package link.infra.borderlessmining.dxgl;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.COM.COMUtils;
import com.sun.jna.platform.win32.Guid;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.ptr.PointerByReference;
import link.infra.borderlessmining.mixin.DXGLWindowAccessor;
import link.infra.dxjni.*;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.util.Monitor;
import net.minecraft.client.util.Window;
import org.lwjgl.glfw.Callbacks;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWNativeWin32;
import org.lwjgl.opengl.GL32C;

import java.util.ArrayList;
import java.util.List;

public class DXGLWindow {
	public D3D12Device d3dDevice;
	public DXGISwapchain3 d3dSwapchain;
	public D3D12CommandQueue d3dCommandQueue;
	public D3D12Fence fence;
	public long fenceValue = 0;
	public WinNT.HANDLE fenceEvent;

	private long handle;
	private final Window parent;
	private DXGLWindowIconState iconState = DXGLWindowIconState.NONE;
	private final DXGLWindowSettings settings;
	private final List<DXGLFramebufferD3D12> framebuffers = new ArrayList<>();

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

	private static void featureTest() {
		int num = GL32C.glGetInteger(GL32C.GL_NUM_EXTENSIONS);
		List<String> exts = new ArrayList<>();
		for (int i = 0; i < num; i++) {
			exts.add(GL32C.glGetStringi(GL32C.GL_EXTENSIONS, i));
		}
		if (!exts.contains("GL_EXT_memory_object")) {
			System.out.println("GL_EXT_memory_object not supported!");
		}
	}

	public void setup(boolean initiallyFullscreen) {
		featureTest();
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

		DXGISwapChainDesc1 desc = new DXGISwapChainDesc1();
		// Width/Height/RefreshRate inferred from window/monitor
		desc.Format.setValue(DXGIModeDesc.DXGI_FORMAT_R8G8B8A8_UNORM);
		// Default sampler mode (no multisampling)
		desc.SampleDesc.Count.setValue(1);

		desc.BufferUsage.setValue(DXGISwapChainDesc.DXGI_USAGE_RENDER_TARGET_OUTPUT);
		desc.BufferCount.setValue(settings.bufferCount());
		desc.SwapEffect.setValue(settings.swapEffect());
		desc.Flags.setValue(settings.swapchainFlags());

		PointerByReference d3dSwapchainRef = new PointerByReference();
		PointerByReference deviceRef = new PointerByReference();

		// TODO: enum adapters by Luid / perf preference?
		COMUtils.checkRC(D3D12Library.INSTANCE.D3D12CreateDevice(
			Pointer.NULL, // No adapter
			D3D12Library.D3D_FEATURE_LEVEL_11_0, // Lowest feature level D3D12 supports
			new Guid.REFIID(D3D12Device.IID_ID3D12Device),
			deviceRef
		));

		d3dDevice = new D3D12Device(deviceRef.getValue());

		PointerByReference factoryRef = new PointerByReference();
		COMUtils.checkRC(DXGILibrary.INSTANCE.CreateDXGIFactory1(new Guid.REFIID(DXGIFactory4.IID_IDXGIFactory4), factoryRef));
		DXGIFactory4 factory = new DXGIFactory4(factoryRef.getValue());

		// Check device LUID/description (using DXGIAdapter GetDesc)
		DXGILUID adapterLuid = d3dDevice.GetAdapterLuid(new DXGILUID());
		PointerByReference adapterRef = new PointerByReference();
		COMUtils.checkRC(factory.EnumAdapterByLuid(adapterLuid.byValue(), new Guid.REFIID(DXGIAdapter.IID_IDXGIAdapter), adapterRef));
		DXGIAdapter adapter = new DXGIAdapter(adapterRef.getValue());
		DXGIAdapterDesc adapterDesc = new DXGIAdapterDesc();
		COMUtils.checkRC(adapter.GetDesc(adapterDesc));
		System.out.println("Created D3D12 device: " + Native.toString(adapterDesc.Description));
		// TODO: verify LUID matches

		PointerByReference commandQueueRef = new PointerByReference();
		COMUtils.checkRC(d3dDevice.CreateCommandQueue(new D3D12CommandQueueDesc(),
			new Guid.REFIID(D3D12CommandQueue.IID_ID3D12CommandQueue), commandQueueRef));
		d3dCommandQueue = new D3D12CommandQueue(commandQueueRef.getValue());

		COMUtils.checkRC(factory.CreateSwapChainForHwnd(
			d3dCommandQueue,
			new WinDef.HWND(new Pointer(hWnd)),
			desc,
			Pointer.NULL, // No fullscreen desc (creating a windowed swapchain)
			Pointer.NULL, // Don't restrict to output
			d3dSwapchainRef
		));
		d3dSwapchain = DXGISwapchain3.fromSwapchain(new DXGISwapchain(d3dSwapchainRef.getValue()));

		for (int i = 0; i < settings.bufferCount(); i++) {
			framebuffers.add(new DXGLFramebufferD3D12(getFramebufferWidth(), getFramebufferHeight(), i, d3dSwapchain, d3dDevice, d3dCommandQueue));
		}
		// TODO: get both buffers, swap

		// Set up d3d resize sync fence
		PointerByReference fenceBuf = new PointerByReference();
		COMUtils.checkRC(d3dDevice.CreateFence(fenceValue, new WinDef.UINT(0), new Guid.REFIID(D3D12Fence.IID_ID3D12Fence), fenceBuf));
		fence = new D3D12Fence(fenceBuf.getValue());

		fenceEvent = Kernel32.INSTANCE.CreateEvent(null, false, false, null);

		// Initialise GL-dependent context (i.e. WGLNVDXInterop); must be run after makeCurrent and GL.createCapabilities
		//d3dDeviceGl = WGLNVDXInterop.wglDXOpenDeviceNV(Pointer.nativeValue(d3dDevice.getPointer()));
		// TODO: this can return 0 (maybe if the d3d+gl adapters don't match?)
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

		// TODO: async frame presentation / D3D resource transitions

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
		COMUtils.checkRC(d3dSwapchain.Present(new WinDef.UINT(syncInterval), new WinDef.UINT(flags)));
	}

	private void awaitQueue() {
		// Set fence to wait for GPU to complete in-flight tasks
		System.out.println("Original fence " + fenceValue);
		fenceValue++;
		long targetFence = fenceValue;
		System.out.println("Signalling fence " + targetFence);
		COMUtils.checkRC(d3dCommandQueue.Signal(fence, targetFence));
		// Wait for fence to be reached
		if (fence.GetCompletedValue().longValue() < targetFence) {
			COMUtils.checkRC(fence.SetEventOnCompletion(new WinDef.ULONGLONG(targetFence), fenceEvent));
			System.out.println("Waiting for fence " + targetFence);
			Kernel32.INSTANCE.WaitForSingleObject(fenceEvent, Kernel32.INFINITE);
			System.out.println("Fence " + targetFence + " reached");
		} else {
			System.out.println("Fence " + targetFence + " already reached");
		}
	}

	public void resize(int width, int height) {
		// TODO: use WindowResolutionChangeWrapper?
		awaitQueue();

		for (DXGLFramebufferD3D12 buf : framebuffers) {
			buf.free();
		}
		framebuffers.clear();

		COMUtils.checkRC(d3dSwapchain.ResizeBuffers(
			new WinDef.UINT(settings.bufferCount()),
			new WinDef.UINT(width),
			new WinDef.UINT(height),
			new WinDef.UINT(DXGIModeDesc.DXGI_FORMAT_R8G8B8A8_UNORM),
			new WinDef.UINT(settings.swapchainFlags())
		));

		for (int i = 0; i < settings.bufferCount(); i++) {
			framebuffers.add(new DXGLFramebufferD3D12(getFramebufferWidth(), getFramebufferHeight(), i, d3dSwapchain, d3dDevice, d3dCommandQueue));
		}

		skipRenderQueue = RenderQueueSkipState.SKIP_LAST_DRAW_AND_QUEUE;
	}

	public void draw(Framebuffer instance, int width, int height) {
		// TODO: waitable object?
		int bufIdx = d3dSwapchain.GetCurrentBackBufferIndex().intValue();
		framebuffers.get(bufIdx).bind();
		// Draw frame
		instance.draw(width, height);
		framebuffers.get(bufIdx).unbind();

		if (skipRenderQueue == RenderQueueSkipState.SKIP_LAST_DRAW_AND_QUEUE) {
			// Have drawn since last skip; current backbuffer is valid
			skipRenderQueue = RenderQueueSkipState.SKIP_QUEUE;
		}
	}

	public long getHandle() {
		return handle;
	}

	public int getFramebufferWidth() {
		return parent.getFramebufferWidth();
	}

	public int getFramebufferHeight() {
		return parent.getFramebufferHeight();
	}

	public void free() {
		awaitQueue();
		for (DXGLFramebufferD3D12 buf : framebuffers) {
			buf.free();
		}
		// TODO: teardown more d3d context
		Callbacks.glfwFreeCallbacks(handle);
		GLFW.glfwDestroyWindow(handle);
	}
}
