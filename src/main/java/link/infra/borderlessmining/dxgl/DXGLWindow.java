package link.infra.borderlessmining.dxgl;

import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.COM.COMUtils;
import com.sun.jna.platform.win32.Guid;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.ptr.PointerByReference;
import link.infra.borderlessmining.mixin.DXGLWindowAccessor;
import link.infra.dxjni.*;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.util.Monitor;
import net.minecraft.client.util.Window;
import org.lwjgl.PointerBuffer;
import org.lwjgl.glfw.Callbacks;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWNativeWin32;
import org.lwjgl.opengl.GL32C;
import org.lwjgl.opengl.WGLNVDXInterop;

public class DXGLWindow {
	public D3D11Texture2D dxColorBuffer;
	public int colorRenderbuffer;
	public int targetFramebuffer;
	public long d3dDevice;
	public long d3dDeviceGl;
	public DXGISwapchain d3dSwapchain;
	public long d3dContext;
	public final PointerBuffer d3dTargets = PointerBuffer.allocateDirect(1);

	private final long handle;
	private final Window parent;
	private DXGLWindowIconState iconState = DXGLWindowIconState.NONE;

	private enum RenderQueueSkipState {
		NONE,
		SKIP_LAST_DRAW_AND_QUEUE,
		SKIP_QUEUE
	}

	private RenderQueueSkipState skipRenderQueue = RenderQueueSkipState.NONE;

	private static void setupWindowHints() {
		// Reset hints; create window with no API
		GLFW.glfwDefaultWindowHints();
		GLFW.glfwWindowHint(GLFW.GLFW_CLIENT_API, GLFW.GLFW_NO_API);
		// TODO: check what these do! (un-inline)
		GLFW.glfwWindowHint(139265, 196609);
		GLFW.glfwWindowHint(139275, 221185);
		GLFW.glfwWindowHint(139266, 3);
		GLFW.glfwWindowHint(139267, 2);
		GLFW.glfwWindowHint(139272, 204801);
		GLFW.glfwWindowHint(139270, 1);
	}

	public DXGLWindow(Window parent, boolean initiallyFullscreen) {
		this.parent = parent;

		setupWindowHints();
		long monitor = 0;
		// True if this is the initial window, and is fullscreen (so no swapping out of fullscreen is necessary)
		if (initiallyFullscreen) {
			Monitor monitorInst = ((DXGLWindowAccessor)(Object)parent).getMonitorTracker().getMonitor(GLFW.glfwGetPrimaryMonitor());
			if (monitorInst != null) {
				monitor = monitorInst.getHandle();
			}
		}
		// Note that this starts in non-fullscreen when we are migrating an existing window (which should be swapped out of fullscreen first)
		handle = GLFW.glfwCreateWindow(parent.getWidth(), parent.getHeight(), "", monitor, 0);

		// Set up d3d in created window
		long hWnd = GLFWNativeWin32.glfwGetWin32Window(handle);

		DXGISwapChainDesc desc = new DXGISwapChainDesc();
		// Width/Height/RefreshRate inferred from window/monitor
		desc.BufferDesc.Format.setValue(DXGIModeDesc.DXGI_FORMAT_R8G8B8A8_UNORM);
		// Default sampler mode (no multisampling)
		desc.SampleDesc.Count.setValue(1);

		desc.BufferUsage.setValue(DXGISwapChainDesc.DXGI_USAGE_RENDER_TARGET_OUTPUT);
		desc.BufferCount.setValue(2);
		desc.OutputWindow.setPointer(new Pointer(hWnd));
		desc.Windowed.setValue(1);
		// TODO: backcompat? FLIP_DISCARD is only w10+
		desc.SwapEffect.setValue(DXGISwapChainDesc.DXGI_SWAP_EFFECT_FLIP_DISCARD);
		// TODO: feature test allow tearing
		desc.Flags.setValue(DXGISwapChainDesc.DXGI_SWAP_CHAIN_FLAG_ALLOW_TEARING);

		PointerByReference d3dSwapchainRef = new PointerByReference();
		PointerByReference deviceRef = new PointerByReference();
		PointerByReference contextRef = new PointerByReference();

		COMUtils.checkRC(D3D11Library.INSTANCE.D3D11CreateDeviceAndSwapChain(
			Pointer.NULL, // No adapter
			new WinDef.UINT(D3D11Library.D3D_DRIVER_TYPE_HARDWARE), new WinDef.HMODULE(), // Use hardware driver (no software DLL)
			new WinDef.UINT(0), //DXJNIShim.D3D11_CREATE_DEVICE_DEBUG, // Debug flag TODO: make dependent on something else
			Pointer.NULL, // Use default feature levels
			new WinDef.UINT(0),
			D3D11Library.D3D11_SDK_VERSION,
			desc,
			d3dSwapchainRef,
			deviceRef,
			new WinDef.UINTByReference(), // No need to get used feature level
			contextRef
		));

		// TODO: wrapper class?
		d3dDevice = Pointer.nativeValue(deviceRef.getValue());
		d3dSwapchain = new DXGISwapchain(d3dSwapchainRef.getValue());
		d3dContext = Pointer.nativeValue(contextRef.getValue());

		PointerByReference colorBufferBuf = new PointerByReference();
		// Get swapchain backbuffer as an ID3D11Texture2D
		COMUtils.checkRC(d3dSwapchain.GetBuffer(
			new WinDef.UINT(0),
			new Guid.REFIID(D3D11Texture2D.IID_ID3D11Texture2D),
			colorBufferBuf
		));
		dxColorBuffer = new D3D11Texture2D(colorBufferBuf.getValue());

		// Initialise GL-dependent context (i.e. WGLNVDXInterop); must be run after makeCurrent and GL.createCapabilities
		d3dDeviceGl = WGLNVDXInterop.wglDXOpenDeviceNV(d3dDevice);
		// TODO: this can return 0 (maybe if the d3d+gl adapters don't match?)

		targetFramebuffer = GL32C.glGenFramebuffers();
		colorRenderbuffer = GL32C.glGenRenderbuffers();
	}

	public void updateIcon() {
		if (iconState != DXGLWindowIconState.ALL) {
			DXGLWindowHelper.updateIcon(parent);
			iconState = parent.isFullscreen() ? DXGLWindowIconState.ONLY_TASKBAR : DXGLWindowIconState.ALL;
		}
	}

	public void present(boolean vsync) {
		// TODO: option for >1 vsync
		// TODO: tearing
		// DXGI_PRESENT_ALLOW_TEARING can only be used with sync interval 0. It is recommended to always pass this
		// tearing flag when using sync interval 0 if CheckFeatureSupport reports that tearing is supported and the app
		// is in a windowed mode - including border-less fullscreen mode. Refer to the DXGI_PRESENT constants for more details.
		// TODO: could use blt model if unthrottled framerate is desired and tearing is not supported

		// TODO: adaptive vsync by detecting when a frame is skipped (using present stats) and presenting newest frame?
		// TODO: could look into Special K's Always Present Newest Frame
		// TODO: https://developer.nvidia.com/dx12-dos-and-donts#swapchains

		// Present frame (using DXGI instead of OpenGL)
		int syncInterval = vsync ? 1 : 0;
		int flags = vsync ? 0 : DXJNIShim.DXGI_PRESENT_ALLOW_TEARING;
		if (skipRenderQueue == RenderQueueSkipState.SKIP_LAST_DRAW_AND_QUEUE) {
			// Don't present (force new draw)
			// TODO: does this work with framerate limiter?
			return;
		} else if (skipRenderQueue == RenderQueueSkipState.SKIP_QUEUE) {
			// Discard stale queued presents (before resize)
			flags |= DXJNIShim.DXGI_PRESENT_RESTART;
			skipRenderQueue = RenderQueueSkipState.NONE;
		}
		// TODO: feature test allow tearing
		d3dSwapchain.Present(new WinDef.UINT(syncInterval), new WinDef.UINT(flags));
	}

	public void resize(int width, int height) {
		// TODO: use WindowResolutionChangeWrapper?
		dxColorBuffer.Release();
		COMUtils.checkRC(d3dSwapchain.ResizeBuffers(
			// TODO: configurable buffer count?
			new WinDef.UINT(2),
			new WinDef.UINT(width),
			new WinDef.UINT(height),
			new WinDef.UINT(DXGIModeDesc.DXGI_FORMAT_R8G8B8A8_UNORM),
			// TODO: feature test allow tearing
			new WinDef.UINT(DXGISwapChainDesc.DXGI_SWAP_CHAIN_FLAG_ALLOW_TEARING)
		));

		PointerByReference colorBufferBuf = new PointerByReference();
		// Get swapchain backbuffer as an ID3D11Texture2D
		COMUtils.checkRC(d3dSwapchain.GetBuffer(
			new WinDef.UINT(0),
			new Guid.REFIID(D3D11Texture2D.IID_ID3D11Texture2D),
			colorBufferBuf
		));
		dxColorBuffer = new D3D11Texture2D(colorBufferBuf.getValue());

		skipRenderQueue = RenderQueueSkipState.SKIP_LAST_DRAW_AND_QUEUE;
	}

	// TODO: move to some sort of swapchain class?
	public void draw(Framebuffer instance, int width, int height) {
		// TODO: waitable object?

		// Register d3d backbuffer as an OpenGL renderbuffer
		// TODO: try registering fewer times; rolling buffer to mimic swapchain behaviour? check usage flags and how they change?
		d3dTargets.put(WGLNVDXInterop.wglDXRegisterObjectNV(
			d3dDeviceGl, Pointer.nativeValue(dxColorBuffer.getPointer()), colorRenderbuffer,
			GL32C.GL_RENDERBUFFER, WGLNVDXInterop.WGL_ACCESS_WRITE_DISCARD_NV));
		d3dTargets.flip();

		// Set up framebuffer and attach d3d backbuffer
		WGLNVDXInterop.wglDXLockObjectsNV(d3dDeviceGl, d3dTargets);
		GL32C.glBindFramebuffer(GL32C.GL_FRAMEBUFFER, targetFramebuffer);
		GL32C.glFramebufferRenderbuffer(GL32C.GL_FRAMEBUFFER, GL32C.GL_COLOR_ATTACHMENT0, GL32C.GL_RENDERBUFFER, colorRenderbuffer);

		// Draw frame
		instance.draw(width, height);

		// Unwind: unbind the framebuffer, unlock and unregister backbuffer
		GL32C.glBindFramebuffer(GL32C.GL_FRAMEBUFFER, 0);
		WGLNVDXInterop.wglDXUnlockObjectsNV(d3dDeviceGl, d3dTargets);
		WGLNVDXInterop.wglDXUnregisterObjectNV(d3dDeviceGl, d3dTargets.get());
		d3dTargets.flip();

		if (skipRenderQueue == RenderQueueSkipState.SKIP_LAST_DRAW_AND_QUEUE) {
			// Have drawn since last skip; current backbuffer is valid
			skipRenderQueue = RenderQueueSkipState.SKIP_QUEUE;
		}
	}

	public long getHandle() {
		return handle;
	}

	public void free() {
		// TODO: teardown d3d context
		Callbacks.glfwFreeCallbacks(handle);
		GLFW.glfwDestroyWindow(handle);
	}
}
