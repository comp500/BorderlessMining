package link.infra.borderlessmining.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import link.infra.borderlessmining.config.WIPConfig;
import link.infra.borderlessmining.util.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.WindowEventHandler;
import net.minecraft.client.WindowSettings;
import net.minecraft.client.util.Monitor;
import net.minecraft.client.util.MonitorTracker;
import net.minecraft.client.util.VideoMode;
import net.minecraft.client.util.Window;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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

	@Shadow
	@Final
	private MonitorTracker monitorTracker;

	private boolean borderlessFullscreen = false;
	private WindowBoundsHolder previousBounds = null;
	private static final Logger LOGGER = LogManager.getLogger(WindowMixin.class);

	/**
	 * Enables and disables borderless fullscreen, assuming the current state is windowed
	 * @param newValue The new state of borderless fullscreen to set
	 * @return True if it was successful, false otherwise
	 */
	private boolean borderlessmining_setBorderlessFullscreen(boolean newValue) {
		RenderSystem.assertThread(RenderSystem::isInInitPhase);
		if (borderlessFullscreen != newValue) {
			borderlessFullscreen = newValue;
			// Kludge to fix fullscreen option not changing on F11
			try {
				MinecraftClient.getInstance().options.fullscreen = newValue;
			} catch (Exception ignored) {
				// Whoops something went wrong here!
			}
			if (newValue) {
				// Store previous bounds
				previousBounds = new WindowBoundsHolder(this);
				// TODO: make this configurable, so you can specify monitor/bounds
				Monitor monitor = this.monitorTracker.getMonitor((Window) (Object) this);
				if (monitor == null) {
					LOGGER.warn("Failed to get a valid monitor for determining fullscreen size!");
					borderlessFullscreen = false;
					return false;
				}
				VideoMode mode = monitor.getCurrentVideoMode();
				GLFW.glfwSetWindowAttrib(handle, GLFW.GLFW_DECORATED, GLFW.GLFW_FALSE);
				GLFW.glfwSetWindowPos(handle, monitor.getViewportX(), monitor.getViewportY());
				GLFW.glfwSetWindowSize(handle, mode.getWidth(), mode.getHeight());
			} else if (previousBounds != null) {
				// Reset to previous bounds
				GLFW.glfwSetWindowAttrib(handle, GLFW.GLFW_DECORATED, GLFW.GLFW_TRUE);
				GLFW.glfwSetWindowPos(handle, previousBounds.getX(), previousBounds.getY());
				GLFW.glfwSetWindowSize(handle, previousBounds.getWidth(), previousBounds.getHeight());
			}
		}
		return true;
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
				// Don't do anything if it fails!
				eventHandlerWrapper.setEnabled(true);
			}
		}
	}

	@Shadow public abstract void swapBuffers();

	@Inject(method = "toggleFullscreen", at = @At("HEAD"), cancellable = true)
	public void onToggleFullscreen(CallbackInfo info) {
		if (WIPConfig.getInstance().isEnabledDirty()) {
			WIPConfig.getInstance().save(!borderlessmining_getFullscreenState());
			info.cancel();
			return;
		}
		if (WIPConfig.getInstance().enabled) {
			fullscreen = false;
			info.cancel();
			borderlessmining_setBorderlessFullscreen(!borderlessFullscreen);
		}
	}

	@Inject(method = "isFullscreen", at = @At("RETURN"), cancellable = true)
	public void onIsFullscreen(CallbackInfoReturnable<Boolean> cir) {
		// TODO: check f11 with fullscreen: on/off setting
		// If BM is enabled, return borderlessFullscreen, otherwise defer to normal isFullscreen
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
				// Don't do anything if it fails!
			}
		} else {
			// Enabled -> Disabled
			if (currentFullscreenState) {
				// Take out of borderless fullscreen
				borderlessmining_setBorderlessFullscreen(false);
				// Don't do anything if it fails!
			}
			if (destFullscreenState) {
				// Enable exclusive fullscreen
				fullscreen = true;
			}
		}
	}
}
