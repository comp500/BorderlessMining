package link.infra.borderlessmining.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import link.infra.borderlessmining.config.WIPConfig;
import link.infra.borderlessmining.util.SettingBorderlessFullscreen;
import link.infra.borderlessmining.util.WindowBoundsGetter;
import link.infra.borderlessmining.util.WindowBoundsHolder;
import link.infra.borderlessmining.util.WindowResolutionChangeWrapper;
import net.minecraft.client.WindowEventHandler;
import net.minecraft.client.WindowSettings;
import net.minecraft.client.util.MonitorTracker;
import net.minecraft.client.util.Window;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Window.class)
public abstract class WindowMixin implements WindowBoundsGetter {
	@Shadow
	private boolean fullscreen;

	@Shadow
	@Final
	private long handle;

	@SuppressWarnings("unused")
	@Shadow
	@Final
	@Mutable
	private WindowEventHandler eventHandler;

	@SuppressWarnings("unused")
	@Shadow
	private boolean videoModeDirty;

	private boolean borderlessFullscreen = false;
	private WindowBoundsHolder previousBounds = null;

	private void setBorderlessFullscreen(boolean newValue) {
		RenderSystem.assertThread(RenderSystem::isInInitPhase);
		if (borderlessFullscreen != newValue) {
			if (newValue) {
				// Store previous bounds
				previousBounds = new WindowBoundsHolder(this);
				// TODO: get/set new bounds
				GLFW.glfwSetWindowAttrib(handle, GLFW.GLFW_DECORATED, 0);
				GLFW.glfwSetWindowPos(handle, 100, 100);
				GLFW.glfwSetWindowSize(handle, 1300, 500);
			} else if (previousBounds != null) {
				// Reset to previous bounds
				GLFW.glfwSetWindowAttrib(handle, GLFW.GLFW_DECORATED, 1);
				GLFW.glfwSetWindowPos(handle, previousBounds.getX(), previousBounds.getY());
				GLFW.glfwSetWindowSize(handle, previousBounds.getWidth(), previousBounds.getHeight());
			}
		}
		borderlessFullscreen = newValue;
	}

	@Inject(at = @At("RETURN"), method = "<init>")
	private void onConstruction(WindowEventHandler prevEventHandler, MonitorTracker monitorTracker, WindowSettings settings, String videoMode, String title, CallbackInfo info) {
		// Stop onResolutionChanged from being triggered if borderless is being set in the constructor
		WindowResolutionChangeWrapper eventHandlerWrapper = new WindowResolutionChangeWrapper(prevEventHandler);
		this.eventHandler = eventHandlerWrapper;
		if (((SettingBorderlessFullscreen) settings).isBorderlessFullscreen()) {
			eventHandlerWrapper.setEnabled(false);
			setBorderlessFullscreen(true);
			eventHandlerWrapper.setEnabled(true);
		}
	}

	@Shadow public abstract void swapBuffers();

	@Inject(method = "toggleFullscreen", at = @At("HEAD"), cancellable = true)
	public void onToggleFullscreen(CallbackInfo info) {
		if (!WIPConfig.enabled) {
			// If disabled -> enabled and in fullscreen, take out of exclusive fullscreen
			if (WIPConfig.pendingEnabled) {
				WIPConfig.enabled = true;
				// Set to previous value (will be toggled later)
				borderlessFullscreen = fullscreen;
				if (fullscreen) {
					// Disable exclusive fullscreen
					fullscreen = false;
					swapBuffers();
				}
			} else {
				borderlessFullscreen = false;
				return;
			}
		}
		// If enabled -> disabled, take out of borderless fullscreen
		if (!WIPConfig.pendingEnabled) {
			WIPConfig.enabled = false;
			// Set to new value
			fullscreen = !borderlessFullscreen;
			setBorderlessFullscreen(false);
			if (fullscreen) {
				// Enable exclusive fullscreen (this will probably happen later anyway but this works)
				swapBuffers();
			}
			info.cancel();
			return;
		}
		fullscreen = false;
		info.cancel();
		setBorderlessFullscreen(!borderlessFullscreen);
	}

	@Inject(method = "isFullscreen", at = @At("RETURN"), cancellable = true)
	public void onIsFullscreen(CallbackInfoReturnable<Boolean> cir) {
		// If BM is enabled, return borderlessFullscreen, otherwise defer to normal isFullscreen
		if (!WIPConfig.enabled) {
			borderlessFullscreen = false;
			return;
		}
		// TODO: on config changes (not from FullscreenOptionMixin), go in/out of fullscreen?
		fullscreen = false;
		cir.setReturnValue(borderlessFullscreen);
	}

	@Inject(method = "applyVideoMode", at = @At("HEAD"))
	private void onApplyVideoMode(CallbackInfo info) {
		if (WIPConfig.pendingEnabled != WIPConfig.enabled) {
			// Update enabled state, applying changes if they need to be done
			if (WIPConfig.pendingEnabled && fullscreen) {
				// This must be done before changing window mode/pos/size as changing those restarts FullScreenOptionMixin
				WIPConfig.enabled = true;
				// Disable exclusive fullscreen, swap buffers to apply immediately
				fullscreen = false;
				swapBuffers();
				// Put into borderless fullscreen, and ensure that the video mode isn't changed later - swapBuffers has already done this
				setBorderlessFullscreen(true);
				videoModeDirty = false;
			} else if (!WIPConfig.pendingEnabled && borderlessFullscreen) {
				WIPConfig.enabled = false;
				// Take out of borderless fullscreen
				setBorderlessFullscreen(false);
				// Enable exclusive fullscreen, swap buffers to apply immediately (probably not needed)
				fullscreen = true;
				swapBuffers();
				// Ensure that the video mode isn't changed later - swapBuffers has already done this
				videoModeDirty = false;
			} else {
				// Just update the state
				WIPConfig.enabled = WIPConfig.pendingEnabled;
			}
		}
	}
}
