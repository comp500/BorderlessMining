package link.infra.borderlessmining.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import link.infra.borderlessmining.dxgl.DXGLContextManager;
import link.infra.borderlessmining.dxgl.DXGLWindow;
import link.infra.borderlessmining.dxgl.DXGLWindowHelper;
import link.infra.borderlessmining.util.DXGLWindowHooks;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.WindowEventHandler;
import net.minecraft.client.WindowSettings;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.util.MonitorTracker;
import net.minecraft.client.util.Window;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(Window.class)
public abstract class DXGLWindowMixin implements DXGLWindowHooks {
	// TODO: synthetic? prefixed?
	private DXGLWindow dxgl_ctx;
	private long dxgl_origHandle;
	private boolean dxgl_initiallyFullscreen;

	@Override
	public DXGLWindow dxgl_getContext() {
		return dxgl_ctx;
	}

	@Shadow
	private boolean vsync;
	@Mutable @Shadow @Final
	private long handle;
	@Shadow @Final
	private WindowEventHandler eventHandler;
	@Shadow
	private boolean fullscreen;

	@Override
	public void dxgl_attach(DXGLWindow window) {
		if (handle == 0) {
			throw new IllegalStateException("Cannot attach a DXGL context before creating window");
		}
		// Migrate GLFW callbacks, placement settings, input modes
		// note: don't use dxgl_origHandle here, needs to use *last* not original; must be done before hiding window
		DXGLWindowHelper.migrateCoordinates(handle, window.getHandle());
		DXGLWindowHelper.migrateCallbacks(handle, window.getHandle());
		DXGLWindowHelper.migrateInputModes(handle, window.getHandle());
		DXGLWindowHelper.updateTitles(handle, window.getHandle());
		// TODO: migrate attributes?
		// Save the original window handle with OpenGL context
		if (dxgl_origHandle == 0) {
			dxgl_origHandle = handle;
			// Hide window (becomes an offscreen context)
			// Note: origHandle should be non-fullscreen at this point
			GLFW.glfwHideWindow(dxgl_origHandle);
		} else {
			if (dxgl_ctx != null) {
				// Clean up if switching d3d windows (detach does not need to be called)
				if (dxgl_ctx != window) {
					dxgl_ctx.free();
				}
			} else {
				throw new IllegalStateException("Detached DXGL context not properly destroyed");
			}
		}
		// Change the window handle to that of the d3d window in the DXGL context
		handle = window.getHandle();
		dxgl_ctx = window;
		// Fix window icon
		DXGLWindowHelper.fixIcon((Window) (Object) this);
		// TODO: Don't run this on initial attach: MinecraftClient.window is unset
		// Update window focus state (hiding the window makes it become unfocused)
		eventHandler.onWindowFocusChanged(true);
	}

	@Override
	public void dxgl_detach() {
		// Only has effect if a DXGL context was attached; cleans up to behave identically to a non-DXGL window
		if (dxgl_origHandle != 0) {
			// Migrate GLFW callbacks, placement settings, input modes
			DXGLWindowHelper.migrateCoordinates(handle, dxgl_origHandle);
			DXGLWindowHelper.migrateCallbacks(handle, dxgl_origHandle);
			DXGLWindowHelper.migrateInputModes(handle, dxgl_origHandle);
			DXGLWindowHelper.updateTitles(handle, dxgl_origHandle);

			handle = dxgl_origHandle;
			// Show window (becomes an onscreen context)
			// Note: does nothing if handle was fullscreen: making dxgl_origHandle fullscreen shows it
			GLFW.glfwShowWindow(dxgl_origHandle);
			dxgl_origHandle = 0;
		}
		if (dxgl_ctx != null) {
			dxgl_ctx.free();
			dxgl_ctx = null;
		}
		// Update window focus state (hiding the window makes it become unfocused)
		eventHandler.onWindowFocusChanged(true);
		// Fix window icon
		DXGLWindowHelper.fixIcon((Window) (Object) this);
	}

	@Inject(method = "<init>", at = @At(value = "INVOKE", target = "Lorg/lwjgl/glfw/GLFW;glfwCreateWindow(IILjava/lang/CharSequence;JJ)J"))
	private void beforeCreatingWindow(WindowEventHandler eventHandler, MonitorTracker monitorTracker, WindowSettings settings, String videoMode, String title, CallbackInfo ci) {
		// Ensure initial window (GL context) won't be shown if DXGL is enabled; as it will become an offscreen context
		if (DXGLContextManager.enabled()) {
			dxgl_initiallyFullscreen = fullscreen;
			// Fullscreen disabled, as GLFW_VISIBLE has no effect in fullscreen
			fullscreen = false;
			GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE);
		}
	}

	// TODO: force initial window to not be fullscreen - GLFW_VISIBLE has no effect in fullscreen!!

	@SuppressWarnings("ConstantConditions")
	@Inject(method = "<init>", at = @At("TAIL"))
	private void afterConstruction(WindowEventHandler eventHandler, MonitorTracker monitorTracker, WindowSettings settings, String videoMode, String title, CallbackInfo ci) {
		if (DXGLContextManager.enabled()) {
			fullscreen = dxgl_initiallyFullscreen;
			DXGLContextManager.setupContext((Window & DXGLWindowHooks) (Object) this);
		}
	}

	@Redirect(method = "swapBuffers", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;flipFrame(J)V"))
	private void onFlipFrame(long window) {
		GLFW.glfwPollEvents();
		RenderSystem.replayQueue();
		Tessellator.getInstance().getBuffer().clear();
		// Swap buffers using d3d context if active
		if (dxgl_ctx != null) {
			dxgl_ctx.present(vsync);
		} else {
			GLFW.glfwSwapBuffers(handle);
		}
		GLFW.glfwPollEvents();
	}

	@Inject(method = "onFramebufferSizeChanged", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/WindowEventHandler;onResolutionChanged()V"))
	private void onOnFramebufferSizeChanged(long window, int width, int height, CallbackInfo ci) {
		// TODO: use windowresolutionchangewrapper?
		if (dxgl_ctx != null) {
			dxgl_ctx.resize(width, height);
		}
	}

	@Inject(method = "close", at = @At("HEAD"))
	private void onClose(CallbackInfo ci) {
		// Revert handle to original and clean up DXGL context
		if (dxgl_origHandle != 0) {
			handle = dxgl_origHandle;
			dxgl_origHandle = 0;
		}
		if (dxgl_ctx != null) {
			dxgl_ctx.free();
			dxgl_ctx = null;
		}
	}
}
