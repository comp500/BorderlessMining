package link.infra.borderlessmining.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import link.infra.borderlessmining.util.DXGLHandles;
import link.infra.dxjni.DXJNIShim;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.WindowEventHandler;
import net.minecraft.client.WindowSettings;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.util.MonitorTracker;
import net.minecraft.client.util.Window;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWNativeWin32;
import org.lwjgl.opengl.GL32C;
import org.lwjgl.opengl.WGLNVDXInterop;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.nio.ByteBuffer;
import java.nio.LongBuffer;

@Environment(EnvType.CLIENT)
@Mixin(Window.class)
public abstract class DXGLWindowMixin {
	@Shadow
	@Final
	private long handle;

	@Shadow private boolean vsync;

	@Shadow private int width;

	@Shadow public abstract int getFramebufferWidth();

	@Shadow public abstract int getFramebufferHeight();

	@Inject(method = "<init>", at = @At("TAIL"))
	private void afterConstruction(WindowEventHandler eventHandler, MonitorTracker monitorTracker, WindowSettings settings, String videoMode, String title, CallbackInfo ci) {
		DXGLHandles.d3dDeviceGl = WGLNVDXInterop.wglDXOpenDeviceNV(DXGLHandles.d3dDevice);
		// TODO: this can return 0 (maybe if the d3d+gl adapters don't match?)

		DXGLHandles.targetFramebuffer = GL32C.glGenFramebuffers();
		DXGLHandles.colorRenderbuffer = GL32C.glGenRenderbuffers();
	}

	@Inject(method = "<init>", at = @At(value = "INVOKE", target = "Lorg/lwjgl/glfw/GLFW;glfwCreateWindow(IILjava/lang/CharSequence;JJ)J"))
	private void beforeCreatingWindow(WindowEventHandler eventHandler, MonitorTracker monitorTracker, WindowSettings settings, String videoMode, String title, CallbackInfo ci) {
		// Create invisible window with existing configured hints
		GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE);
		DXGLHandles.offscreenContext = GLFW.glfwCreateWindow(640, 480, "DXGL offscreen GL context", 0, 0);
		// Reset hints; make primary window created with no API
		GLFW.glfwDefaultWindowHints();
		GLFW.glfwWindowHint(GLFW.GLFW_CLIENT_API, GLFW.GLFW_NO_API);
	}

	private void checkResult(long result) {
		if (result != 0) {
			throw new RuntimeException("D3D error code " + result);
		}
	}

	@Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lorg/lwjgl/glfw/GLFW;glfwMakeContextCurrent(J)V"))
	private void onMakeContextCurrent(long window) {
		GLFW.glfwDefaultWindowHints();
		// TODO: check what these do!
//		GLFW.glfwWindowHint(139265, 196609);
//		GLFW.glfwWindowHint(139275, 221185);
//		GLFW.glfwWindowHint(139266, 3);
//		GLFW.glfwWindowHint(139267, 2);
//		GLFW.glfwWindowHint(139272, 204801);
//		GLFW.glfwWindowHint(139270, 1);
		GLFW.glfwMakeContextCurrent(DXGLHandles.offscreenContext);

		// Set up d3d in onscreen context
		long hWnd = GLFWNativeWin32.glfwGetWin32Window(window);
		try (MemoryStack stack = MemoryStack.stackPush()) {
			LongBuffer deviceBuf = stack.callocLong(1);
			LongBuffer swapchainBuf = stack.callocLong(1);
			LongBuffer contextBuf = stack.callocLong(1);
			LongBuffer colorBufferBuf = stack.callocLong(1);

			// TODO: backcompat? FLIP_DISCARD is only w10+

			// Fill swapchain description
			ByteBuffer swapchainDesc = stack.calloc(72);
			// DXGI_MODE_DESC (28)
			swapchainDesc.putInt(0); // Width (4)
			swapchainDesc.putInt(0); // Height (4)
			swapchainDesc.putLong(0); // RefreshRate (8)
			swapchainDesc.putInt(DXJNIShim.DXGI_FORMAT_R8G8B8A8_UNORM); // Format (4)
			swapchainDesc.putInt(0); // ScanlineOrdering (4)
			swapchainDesc.putInt(0); // Scaling (4)
			// DXGI_SAMPLE_DESC (8)
			swapchainDesc.putInt(1); // Count (4)
			swapchainDesc.putInt(0); // Quality (4)
			// --
			swapchainDesc.putInt(DXJNIShim.DXGI_USAGE_RENDER_TARGET_OUTPUT); // BufferUsage (4)
			swapchainDesc.putInt(2); // BufferCount (4)
			swapchainDesc.putInt(0); // 4 bytes padding
			swapchainDesc.putLong(hWnd); // OutputWindow (8)
			swapchainDesc.putInt(1); // Windowed (4)
			swapchainDesc.putInt(DXJNIShim.DXGI_SWAP_EFFECT_FLIP_DISCARD); // SwapEffect (4)
			swapchainDesc.putInt(0); // Flags (4)
			swapchainDesc.putInt(0); // 4 bytes padding
			swapchainDesc.flip();

			checkResult(DXJNIShim.D3D11CreateDeviceAndSwapChain(
				0, // No adapter
				DXJNIShim.D3D_DRIVER_TYPE_HARDWARE, 0, // Use hardware driver (no software DLL)
				0,//DXJNIShim.D3D11_CREATE_DEVICE_DEBUG, // Debug flag TODO: make dependent on something else
				0, 0, // Use default feature levels
				MemoryUtil.memAddress(swapchainDesc),
				MemoryUtil.memAddress(swapchainBuf),
				MemoryUtil.memAddress(deviceBuf),
				0, // No need to get used feature level
				MemoryUtil.memAddress(contextBuf)
			));

			DXGLHandles.d3dDevice = deviceBuf.get();
			DXGLHandles.d3dSwapchain = swapchainBuf.get();
			DXGLHandles.d3dContext = contextBuf.get();

			// Get swapchain backbuffer as an ID3D11Texture2D
			checkResult(DXJNIShim.DXGISwapChainGetBufferTexture2D(DXGLHandles.d3dSwapchain, 0,
				MemoryUtil.memAddress(colorBufferBuf)));

			DXGLHandles.dxColorBuffer = colorBufferBuf.get();
		}
	}

	@Redirect(method = "swapBuffers", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;flipFrame(J)V"))
	private void onFlipFrame(long window) {
		GLFW.glfwPollEvents();
		RenderSystem.replayQueue();
		Tessellator.getInstance().getBuffer().clear();

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
		DXJNIShim.DXGISwapChainPresent(DXGLHandles.d3dSwapchain, vsync ? 1 : 0, 0);

		GLFW.glfwPollEvents();
	}

	@Inject(method = "onFramebufferSizeChanged", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/WindowEventHandler;onResolutionChanged()V"))
	private void onOnFramebufferSizeChanged(long window, int width, int height, CallbackInfo ci) {
		// TODO: use WindowResolutionChangeWrapper?
		checkResult(DXJNIShim.ID3D11Texture2DRelease(DXGLHandles.dxColorBuffer));
		checkResult(DXJNIShim.DXGISwapChainResizeBuffers(DXGLHandles.d3dSwapchain, 2, width, height, 0, 0));
		try (MemoryStack stack = MemoryStack.stackPush()) {
			LongBuffer colorBufferBuf = stack.callocLong(1);

			// Get swapchain backbuffer as an ID3D11Texture2D
			checkResult(DXJNIShim.DXGISwapChainGetBufferTexture2D(DXGLHandles.d3dSwapchain, 0,
				MemoryUtil.memAddress(colorBufferBuf)));

			DXGLHandles.dxColorBuffer = colorBufferBuf.get();
		}
	}

	// TODO: teardown
}
