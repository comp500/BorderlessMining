package link.infra.borderlessmining.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import link.infra.borderlessmining.config.ConfigHandler;
import link.infra.borderlessmining.util.SettingBorderlessFullscreen;
import link.infra.borderlessmining.util.WindowHooks;
import link.infra.borderlessmining.util.WindowResolutionChangeWrapper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.WindowEventHandler;
import net.minecraft.client.WindowSettings;
import net.minecraft.client.util.Monitor;
import net.minecraft.client.util.MonitorTracker;
import net.minecraft.client.util.VideoMode;
import net.minecraft.client.util.Window;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.PointerBuffer;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.system.MemoryStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.nio.IntBuffer;

@Mixin(Window.class)
public abstract class WindowMixin implements WindowHooks {
	@Shadow public abstract int getX();
	@Shadow public abstract int getY();
	@Shadow public abstract int getWidth();
	@Shadow public abstract int getHeight();

	@Shadow private int windowedX;
	@Shadow private int windowedY;
	@Shadow private int windowedWidth;
	@Shadow private int windowedHeight;

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
	private static final Logger LOGGER = LogManager.getLogger(WindowMixin.class);

	/**
	 * Enables and disables borderless fullscreen, assuming the current state is windowed
	 * @param newValue The new state of borderless fullscreen to set
	 * @return True if it was successful, false otherwise
	 */
	@SuppressWarnings("UnusedReturnValue")
	private boolean borderlessmining_setBorderlessFullscreen(boolean newValue) {
		RenderSystem.assertOnRenderThreadOrInit();
		if (borderlessFullscreen != newValue) {
			borderlessFullscreen = newValue;
			// Kludge to fix fullscreen option button text not changing on F11
			try {
				MinecraftClient.getInstance().options.getFullscreen().setValue(newValue);
			} catch (Exception ignored) {
				// Whoops something went wrong here!
			}
			if (newValue) {
				// Store previous bounds
				windowedX = getX();
				windowedY = getY();
				windowedWidth = getWidth();
				windowedHeight = getHeight();

				int x;
				int y;
				int width;
				int height;
				if (ConfigHandler.getInstance().customWindowDimensions != null &&
					ConfigHandler.getInstance().customWindowDimensions.enabled &&
					!ConfigHandler.getInstance().customWindowDimensions.useMonitorCoordinates) {
					x = 0;
					y = 0;
					width = 0;
					height = 0;
				} else if (ConfigHandler.getInstance().forceWindowMonitor < 0) {
					Monitor monitor = this.monitorTracker.getMonitor((Window) (Object) this);
					if (monitor == null) {
						LOGGER.error("Failed to get a valid monitor for determining fullscreen size!");
						borderlessFullscreen = false;
						return false;
					}
					VideoMode mode = monitor.getCurrentVideoMode();
					x = monitor.getViewportX();
					y = monitor.getViewportY();
					width = mode.getWidth();
					height = mode.getHeight();
				} else {
					PointerBuffer monitors = GLFW.glfwGetMonitors();
					if (monitors == null || monitors.limit() < 1) {
						LOGGER.error("Failed to get a valid monitor list for determining fullscreen position!");
						borderlessFullscreen = false;
						return false;
					}
					long monitorHandle;
					if (ConfigHandler.getInstance().forceWindowMonitor >= monitors.limit()) {
						LOGGER.warn("Monitor " + ConfigHandler.getInstance().forceWindowMonitor + " is greater than list size " + monitors.limit() + ", using monitor 0");
						monitorHandle = monitors.get(0);
					} else {
						monitorHandle = monitors.get(ConfigHandler.getInstance().forceWindowMonitor);
					}
					try (MemoryStack stack = MemoryStack.stackPush()) {
						IntBuffer xBuf = stack.mallocInt(1);
						IntBuffer yBuf = stack.mallocInt(1);
						GLFW.glfwGetMonitorPos(monitorHandle, xBuf, yBuf);
						x = xBuf.get();
						y = yBuf.get();
					}
					GLFWVidMode mode = GLFW.glfwGetVideoMode(monitorHandle);
					if (mode == null) {
						LOGGER.error("Failed to get a video mode for the current monitor!");
						borderlessFullscreen = false;
						return false;
					} else {
						width = mode.width();
						height = mode.height();
					}
				}

				if (ConfigHandler.getInstance().customWindowDimensions != null) {
					ConfigHandler.CustomWindowDimensions dims = ConfigHandler.getInstance().customWindowDimensions;
					if (dims.enabled) {
						if (dims.useMonitorCoordinates) {
							x += dims.x;
							y += dims.y;
						} else {
							x = dims.x;
							y = dims.y;
						}
						if (dims.width > 0 && dims.height > 0) {
							width = dims.width;
							height = dims.height;
						} else if (!dims.useMonitorCoordinates) {
							LOGGER.error("Both width and height must be > 0 when specifying absolute coordinates!");
							borderlessFullscreen = false;
							return false;
						}
					}
				}

				GLFW.glfwSetWindowAttrib(handle, GLFW.GLFW_DECORATED, GLFW.GLFW_FALSE);
				GLFW.glfwSetWindowPos(handle, x, y);
				GLFW.glfwSetWindowSize(handle, width, height);
			} else {
				// Reset to previous bounds
				GLFW.glfwSetWindowAttrib(handle, GLFW.GLFW_DECORATED, GLFW.GLFW_TRUE);
				GLFW.glfwSetWindowPos(handle, windowedX, windowedY);
				GLFW.glfwSetWindowSize(handle, windowedWidth, windowedHeight);
			}
		}
		return true;
	}

	//@Inject(at = @At("RETURN"), method = "onWindowFocusChanged(JZ)V")
	private void onWindowFocusChanged(long window, boolean focused, CallbackInfo ci) {
		if (window == handle) {
			// This seems to be buggy when switching between borderless fullscreen and a standard window
			// and when clicking directly to another application from fullscreen (if it doesn't cover the screen)
			// so for now it is disabled.
			if (ConfigHandler.getInstance().isEnabled() && this.borderlessFullscreen && focused) {
				GLFW.glfwSetWindowAttrib(handle, GLFW.GLFW_FLOATING, GLFW.GLFW_TRUE);
			} else {
				GLFW.glfwSetWindowAttrib(handle, GLFW.GLFW_FLOATING, GLFW.GLFW_FALSE);
			}
		}
	}

	@Inject(at = @At("RETURN"), method = "<init>")
	private void onConstruction(WindowEventHandler prevEventHandler, MonitorTracker monitorTracker, WindowSettings settings, String videoMode, String title, CallbackInfo info) {
		if (ConfigHandler.getInstance().isEnabled()) {
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
		if (ConfigHandler.getInstance().isEnabledDirty()) {
			ConfigHandler.getInstance().save(!borderlessmining_getFullscreenState());
			info.cancel();
			return;
		}
		if (ConfigHandler.getInstance().isEnabled()) {
			fullscreen = false;
			info.cancel();
			borderlessmining_setBorderlessFullscreen(!borderlessFullscreen);
		}
	}

	@Inject(method = "isFullscreen", at = @At("RETURN"), cancellable = true)
	public void onIsFullscreen(CallbackInfoReturnable<Boolean> cir) {
		// If BM is enabled, return borderlessFullscreen, otherwise defer to normal isFullscreen
		if (ConfigHandler.getInstance().isEnabled()) {
			cir.setReturnValue(borderlessFullscreen);
		}
	}

	@Inject(method = "applyVideoMode", at = @At("HEAD"))
	private void onApplyVideoMode(CallbackInfo info) {
		if (ConfigHandler.getInstance().isEnabledDirty()) {
			ConfigHandler.getInstance().save();
			// Ensure that the video mode isn't changed later - updateEnabledState has already done this
			videoModeDirty = false;
		}
	}

	/**
	 * Updates the state of the game to the new value of the enabled configuration option
	 */
	public boolean borderlessmining_getFullscreenState() {
		return ConfigHandler.getInstance().isEnabled() ? borderlessFullscreen : fullscreen;
	}

	/**
	 * Updates the state of the game to the new value of the enabled configuration option, called only when it changes
	 * @param destEnabledState The desired destination enabling state - if this is true the previous enable state was false, etc.
	 * @param destFullscreenState The desired destination fullscreen state, after applying this change
	 */
	public void borderlessmining_updateEnabledState(boolean destEnabledState, boolean currentFullscreenState, boolean destFullscreenState) {
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
