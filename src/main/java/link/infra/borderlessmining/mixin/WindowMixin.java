package link.infra.borderlessmining.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import link.infra.borderlessmining.config.WIPConfig;
import link.infra.borderlessmining.util.*;
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
public abstract class WindowMixin implements WindowBoundsGetter, WindowHooks {
	@Shadow public abstract int getX();
	@Shadow public abstract int getY();
	@Shadow public abstract int getWidth();
	@Shadow public abstract int getHeight();

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

	private void borderlessmining_setBorderlessFullscreen(boolean newValue) {
		RenderSystem.assertThread(RenderSystem::isInInitPhase);
		if (borderlessFullscreen != newValue) {
			borderlessFullscreen = newValue;
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
	}

	@Inject(at = @At("RETURN"), method = "<init>")
	private void onConstruction(WindowEventHandler prevEventHandler, MonitorTracker monitorTracker, WindowSettings settings, String videoMode, String title, CallbackInfo info) {
		if (WIPConfig.getInstance().enabled) {
			// Stop onResolutionChanged from being triggered if borderless is being set in the constructor
			WindowResolutionChangeWrapper eventHandlerWrapper = new WindowResolutionChangeWrapper(prevEventHandler);
			this.eventHandler = eventHandlerWrapper;
			if (((SettingBorderlessFullscreen) settings).isBorderlessFullscreen()) {
				eventHandlerWrapper.setEnabled(false);
				borderlessmining_setBorderlessFullscreen(true);
				eventHandlerWrapper.setEnabled(true);
			}
		}
	}

	@Shadow public abstract void swapBuffers();

	@Shadow private boolean currentFullscreen;

	@Inject(method = "toggleFullscreen", at = @At("HEAD"), cancellable = true)
	public void onToggleFullscreen(CallbackInfo info) {
		if (WIPConfig.getInstance().isEnabledDirty()) {
			WIPConfig.getInstance().save(!borderlessmining_getFullscreenState());
			info.cancel();
			return;
		}
//		if (!WIPConfig.getInstance().enabled) {
//			// If disabled -> enabled and in fullscreen, take out of exclusive fullscreen
//			if (WIPConfig.getInstance().pendingEnabled) {
//				WIPConfig.getInstance().enabled = true;
//				// Set to previous value (will be toggled later)
//				borderlessFullscreen = fullscreen;
//				if (fullscreen) {
//					// Disable exclusive fullscreen
//					fullscreen = false;
//					swapBuffers();
//				}
//			} else {
//				borderlessFullscreen = false;
//				return;
//			}
//		}
//		// If enabled -> disabled, take out of borderless fullscreen
//		if (!WIPConfig.getInstance().pendingEnabled) {
//			WIPConfig.getInstance().enabled = false;
//			// Set to new value
//			fullscreen = !borderlessFullscreen;
//			borderlessmining_setBorderlessFullscreen(false);
//			if (fullscreen) {
//				// Enable exclusive fullscreen (this will probably happen later anyway but this works)
//				swapBuffers();
//			}
//			info.cancel();
//			return;
//		}
		if (WIPConfig.getInstance().enabled) {
			fullscreen = false;
			info.cancel();
			borderlessmining_setBorderlessFullscreen(!borderlessFullscreen);
		}
	}

	@Inject(method = "isFullscreen", at = @At("RETURN"), cancellable = true)
	public void onIsFullscreen(CallbackInfoReturnable<Boolean> cir) {
//		// If BM is enabled, return borderlessFullscreen, otherwise defer to normal isFullscreen
//		if (!WIPConfig.getInstance().enabled) {
//			borderlessFullscreen = false;
//			return;
//		}
//		// TODO: on config changes (not from FullscreenOptionMixin), go in/out of fullscreen?
//		// TODO: check f11 with fullscreen: on/off setting
//		fullscreen = false;
//		cir.setReturnValue(borderlessFullscreen);
		//if (WIPConfig.getInstance().isEnabledOrPending()) {
		if (WIPConfig.getInstance().enabled) {
			cir.setReturnValue(borderlessFullscreen);
		}
	}

	@Inject(method = "applyVideoMode", at = @At("HEAD"))
	private void onApplyVideoMode(CallbackInfo info) {
		if (WIPConfig.getInstance().isEnabledDirty()) {
			WIPConfig.getInstance().save();
			// Ensure that the video mode isn't changed later - updateEnabledState has already done this
			videoModeDirty = false;
		}
//		if (WIPConfig.getInstance().pendingEnabled != WIPConfig.getInstance().enabled) {
//			// Update enabled state, applying changes if they need to be done
//			if (WIPConfig.getInstance().pendingEnabled && fullscreen) {
//				// This must be done before changing window mode/pos/size as changing those restarts FullScreenOptionMixin
//				WIPConfig.getInstance().enabled = true;
//				// Disable exclusive fullscreen, swap buffers to apply immediately
//				fullscreen = false;
//				swapBuffers();
//				// Put into borderless fullscreen, and ensure that the video mode isn't changed later - swapBuffers has already done this
//				borderlessmining_setBorderlessFullscreen(true);
//				videoModeDirty = false;
//			} else if (!WIPConfig.getInstance().pendingEnabled && borderlessFullscreen) {
//				WIPConfig.getInstance().enabled = false;
//				// Take out of borderless fullscreen
//				borderlessmining_setBorderlessFullscreen(false);
//				// Enable exclusive fullscreen, swap buffers to apply immediately (probably not needed)
//				fullscreen = true;
//				swapBuffers();
//				// Ensure that the video mode isn't changed later - swapBuffers has already done this
//				videoModeDirty = false;
//			} else {
//				// Just update the state
//				WIPConfig.getInstance().enabled = WIPConfig.getInstance().pendingEnabled;
//			}
//		}
	}

	/**
	 * Updates the state of the game to the new value of the enabled configuration option
	 */
	public boolean borderlessmining_getFullscreenState() {
		return WIPConfig.getInstance().enabled ? borderlessFullscreen : fullscreen;
	}

	/**
	 * Updates the state of the game to the new value of the enabled configuration option, called only when it changes
	 * @param destEnabledState The desired destination enabling state - if this is true the previous enable state was false, etc.
	 * @param destFullscreenState The desired destination fullscreen state, after applying this change
	 */
	public void borderlessmining_updateEnabledState(boolean destEnabledState, boolean currentFullscreenState, boolean destFullscreenState) {
		System.out.println("Updating enabled state: Curr/DestF/DestE " + currentFullscreenState + "/" + destFullscreenState + "/" + destEnabledState);
		// Update enabled state, applying changes if they need to be done
		if (destEnabledState) {
			// Disabled -> Enabled
			if (currentFullscreenState) {
				// Disable exclusive fullscreen, swap buffers to apply immediately
				fullscreen = false;
				if (destFullscreenState) {
					swapBuffers();
				}
			}
			if (destFullscreenState) {
				// Put into borderless fullscreen
				borderlessmining_setBorderlessFullscreen(true);
			}
		} else {
			// Enabled -> Disabled
			if (currentFullscreenState) {
				// Take out of borderless fullscreen
				borderlessmining_setBorderlessFullscreen(false);
			}
			if (destFullscreenState) {
				// Enable exclusive fullscreen
				fullscreen = true;
			}
		}
	}
}
