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
import org.lwjgl.glfw.Callbacks;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWNativeWin32;
import org.lwjgl.opengl.WGLNVDXInterop;

public abstract class DXGLWindow {
	public D3D11Texture2D dxColorBackbuffer;
	public long d3dDevice;
	public long d3dDeviceGl;
	public DXGISwapchain d3dSwapchain;
	public long d3dContext;

	private long handle;
	private final Window parent;
	private DXGLWindowIconState iconState = DXGLWindowIconState.NONE;
	private final DXGLWindowSettings settings;

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

	public DXGLWindow(Window parent, DXGLWindowSettings settings) {
		this.parent = parent;
		this.settings = settings;
	}

	public void setup(boolean initiallyFullscreen) {
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
		desc.BufferCount.setValue(settings.bufferCount());
		desc.OutputWindow.setPointer(new Pointer(hWnd));
		desc.Windowed.setValue(1);
		desc.SwapEffect.setValue(settings.swapEffect());
		desc.Flags.setValue(settings.swapchainFlags());

		PointerByReference d3dSwapchainRef = new PointerByReference();
		PointerByReference deviceRef = new PointerByReference();
		PointerByReference contextRef = new PointerByReference();

		COMUtils.checkRC(D3D11Library.INSTANCE.D3D11CreateDeviceAndSwapChain(
			Pointer.NULL, // No adapter
			new WinDef.UINT(D3D11Library.D3D_DRIVER_TYPE_HARDWARE), new WinDef.HMODULE(), // Use hardware driver (no software DLL)
			new WinDef.UINT(settings.debug() ? D3D11Library.D3D11_CREATE_DEVICE_DEBUG : 0), // Debug flag
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
		dxColorBackbuffer = new D3D11Texture2D(colorBufferBuf.getValue());

		// Initialise GL-dependent context (i.e. WGLNVDXInterop); must be run after makeCurrent and GL.createCapabilities
		d3dDeviceGl = WGLNVDXInterop.wglDXOpenDeviceNV(d3dDevice);
		// TODO: this can return 0 (maybe if the d3d+gl adapters don't match?)

		setupGlBuffers();
		registerBackbuffer();
	}

	public void updateIcon() {
		if (iconState != DXGLWindowIconState.ALL) {
			DXGLWindowHelper.updateIcon(parent);
			iconState = parent.isFullscreen() ? DXGLWindowIconState.ONLY_TASKBAR : DXGLWindowIconState.ALL;
		}
	}

	public void present(boolean vsync) {
		// DXGI_PRESENT_ALLOW_TEARING can only be used with sync interval 0. It is recommended to always pass this
		// tearing flag when using sync interval 0 if CheckFeatureSupport reports that tearing is supported and the app
		// is in a windowed mode - including border-less fullscreen mode. Refer to the DXGI_PRESENT constants for more details.
		// TODO: could use blt model if unthrottled framerate is desired and tearing is not supported

		// TODO: adaptive vsync by detecting when a frame is skipped (using present stats) and presenting newest frame?
		// TODO: could look into Special K's Always Present Newest Frame
		// TODO: https://developer.nvidia.com/dx12-dos-and-donts#swapchains

		// Present frame (using DXGI instead of OpenGL)
		int syncInterval = vsync ? settings.vsyncSyncInterval() : 0;
		int flags = vsync ? settings.presentFlagsVsync() : settings.presentFlags();
		if (skipRenderQueue == RenderQueueSkipState.SKIP_LAST_DRAW_AND_QUEUE) {
			// Don't present (force new draw)
			// TODO: does this work with framerate limiter?
			return;
		} else if (skipRenderQueue == RenderQueueSkipState.SKIP_QUEUE) {
			// Discard stale queued presents (before resize)
			flags |= DXJNIShim.DXGI_PRESENT_RESTART;
			skipRenderQueue = RenderQueueSkipState.NONE;
		}
		d3dSwapchain.Present(new WinDef.UINT(syncInterval), new WinDef.UINT(flags));
	}

	public void resize(int width, int height) {
		unregisterBackbuffer();
		// TODO: use WindowResolutionChangeWrapper?
		dxColorBackbuffer.Release();
		COMUtils.checkRC(d3dSwapchain.ResizeBuffers(
			new WinDef.UINT(settings.bufferCount()),
			new WinDef.UINT(width),
			new WinDef.UINT(height),
			new WinDef.UINT(DXGIModeDesc.DXGI_FORMAT_R8G8B8A8_UNORM),
			new WinDef.UINT(settings.swapchainFlags())
		));

		PointerByReference colorBufferBuf = new PointerByReference();
		// Get swapchain backbuffer as an ID3D11Texture2D
		COMUtils.checkRC(d3dSwapchain.GetBuffer(
			new WinDef.UINT(0),
			new Guid.REFIID(D3D11Texture2D.IID_ID3D11Texture2D),
			colorBufferBuf
		));
		dxColorBackbuffer = new D3D11Texture2D(colorBufferBuf.getValue());
		registerBackbuffer();

		skipRenderQueue = RenderQueueSkipState.SKIP_LAST_DRAW_AND_QUEUE;
	}

	public void draw(Framebuffer instance, int width, int height) {
		// TODO: waitable object?
		bindBackbuffer();
		// Draw frame
		instance.draw(width, height);
		unbindBackbuffer();

		if (skipRenderQueue == RenderQueueSkipState.SKIP_LAST_DRAW_AND_QUEUE) {
			// Have drawn since last skip; current backbuffer is valid
			skipRenderQueue = RenderQueueSkipState.SKIP_QUEUE;
		}
	}

	protected abstract void setupGlBuffers();
	protected abstract void freeGlBuffers();
	protected abstract void registerBackbuffer();
	protected abstract void unregisterBackbuffer();
	protected abstract void bindBackbuffer();
	protected abstract void unbindBackbuffer();

	public long getHandle() {
		return handle;
	}

	public void free() {
		unregisterBackbuffer();
		freeGlBuffers();
		// TODO: teardown more d3d context
		Callbacks.glfwFreeCallbacks(handle);
		GLFW.glfwDestroyWindow(handle);
	}
}
