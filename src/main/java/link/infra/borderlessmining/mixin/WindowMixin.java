package link.infra.borderlessmining.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import link.infra.borderlessmining.config.WIPConfig;
import link.infra.borderlessmining.util.WindowBoundsHolder;
import link.infra.borderlessmining.util.WindowBoundsGetter;
import link.infra.borderlessmining.util.SettingBorderlessFullscreen;
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
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Window.class)
public abstract class WindowMixin implements WindowBoundsGetter {
	@Shadow
	private boolean fullscreen;
	@Shadow
	private boolean vsync;
	@Shadow
	@Final
	private long handle;
	@Shadow
	@Final
	@Mutable
	private WindowEventHandler eventHandler;
	@Shadow
	private boolean videoModeDirty;

	// TODO: move from here if it's not used elsewhere
	private WindowResolutionChangeWrapper eventHandlerWrapper;

	@Accessor("x")
	public abstract void setX(int x);
	@Accessor("y")
	public abstract void setY(int y);
	@Accessor("width")
	public abstract void setWidth(int width);
	@Accessor("height")
	public abstract void setHeight(int height);

	private boolean borderlessFullscreen = false;
	private WindowBoundsHolder previousBounds = null;

	private void setBorderlessFullscreen(boolean newValue) {
		System.out.println("Setting borderless fullscreen to " + newValue);
		RenderSystem.assertThread(RenderSystem::isInInitPhase);
		if (borderlessFullscreen != newValue) {
			if (newValue) {
				// Store previous bounds
				previousBounds = new WindowBoundsHolder(this);
				// TODO: set undecorated?
				// TODO: set new bounds
				GLFW.glfwSetWindowAttrib(handle, GLFW.GLFW_DECORATED, 0);
				GLFW.glfwSetWindowPos(handle, 100, 100);
				GLFW.glfwSetWindowSize(handle, 1300, 500);
				System.out.println(getX());
				System.out.println(getY());
			} else if (previousBounds != null) {
				// Reset to previous bounds
				// TODO: set decorated?
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
		this.eventHandler = eventHandlerWrapper = new WindowResolutionChangeWrapper(prevEventHandler);
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
			System.out.println("toggling: disabled");
			// If disabled -> enabled and in fullscreen, take out of exclusive fullscreen
			if (WIPConfig.pendingEnabled) {
				WIPConfig.enabled = true;
				System.out.println("toggling: disabled -> enabled");
				System.out.println("toggling: fullscreen = " + fullscreen);
				System.out.println("toggling: bFullscreen = " + borderlessFullscreen);
				borderlessFullscreen = fullscreen;
				if (fullscreen) {
					System.out.println("toggling: disabling fullscreen, swapping buffers");
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
			System.out.println("toggling: enabled -> disabled");
			System.out.println("toggling: fullscreen = " + fullscreen);
			System.out.println("toggling: bFullscreen = " + borderlessFullscreen);
			fullscreen = !borderlessFullscreen;
			System.out.println("toggling: disabling bFullscreen");
			setBorderlessFullscreen(false);
			if (fullscreen) {
				System.out.println("toggling: swapping buffers");
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
		System.out.println("applying: " + WIPConfig.enabled + " enabled, " + WIPConfig.pendingEnabled + " pending");
		if (WIPConfig.pendingEnabled != WIPConfig.enabled) {
			System.out.println("Pending " + WIPConfig.pendingEnabled);
			// Update enabled state, applying changes if they need to be done
			if (WIPConfig.pendingEnabled && fullscreen) {
				// This must be done before changing window mode/pos/size as changing those restarts FullScreenOptionMixin
				WIPConfig.enabled = true;
				System.out.println("applying: disabling fullscreen, swapping buffers");
				fullscreen = false;
				swapBuffers();
				System.out.println("applying: enabling bFullscreen");
				setBorderlessFullscreen(true);
				videoModeDirty = false;
			} else if (!WIPConfig.pendingEnabled && borderlessFullscreen) {
				WIPConfig.enabled = false;
				System.out.println("applying: disabling bFullscreen, setting fullscreen to true");
				setBorderlessFullscreen(false);
				fullscreen = true;
				swapBuffers();
				videoModeDirty = false;
			} else {
				WIPConfig.enabled = WIPConfig.pendingEnabled;
			}
		}
	}
}
