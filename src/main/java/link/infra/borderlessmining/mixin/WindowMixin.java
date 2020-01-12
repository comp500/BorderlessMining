package link.infra.borderlessmining.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import link.infra.borderlessmining.config.WIPConfig;
import link.infra.borderlessmining.util.WindowBoundsHolder;
import link.infra.borderlessmining.util.WindowBoundsGetter;
import link.infra.borderlessmining.util.SettingBorderlessFullscreen;
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
	private boolean initialSettingBorderless = false;

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
		this.eventHandler = new WindowEventHandler() {
			@Override
			public void onWindowFocusChanged(boolean focused) {
				prevEventHandler.onWindowFocusChanged(focused);
			}

			@Override
			public void onResolutionChanged() {
				if (!initialSettingBorderless) {
					prevEventHandler.onResolutionChanged();
				}
			}
		};
		if (((SettingBorderlessFullscreen) settings).isBorderlessFullscreen()) {
			initialSettingBorderless = true;
			setBorderlessFullscreen(true);
			initialSettingBorderless = false;
		}
	}

	@Shadow public abstract void swapBuffers();

	@Inject(method = "toggleFullscreen", at = @At("HEAD"), cancellable = true)
	public void onToggleFullscreen(CallbackInfo info) {
		if (!WIPConfig.enabled) {
			// If disabled -> enabled and in fullscreen, take out of exclusive fullscreen
			if (WIPConfig.pendingEnabled) {
				borderlessFullscreen = fullscreen;
				if (fullscreen) {
					fullscreen = false;
					swapBuffers();
				}
				WIPConfig.enabled = true;
			} else {
				borderlessFullscreen = false;
				return;
			}
		}
		// If enabled -> disabled, take out of borderless fullscreen
		if (!WIPConfig.pendingEnabled) {
			fullscreen = !borderlessFullscreen;
			setBorderlessFullscreen(false);
			if (fullscreen) {
				swapBuffers();
			}
			info.cancel();
			WIPConfig.enabled = false;
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

	@Inject(method = "applyVideoMode", at = @At("RETURN"))
	private void onApplyVideoMode(CallbackInfo info) {
		if (WIPConfig.pendingEnabled != WIPConfig.enabled) {
			System.out.println("Pending " + WIPConfig.pendingEnabled);
			if (WIPConfig.pendingEnabled && fullscreen) {
				fullscreen = false;
				swapBuffers();
				setBorderlessFullscreen(true);
			} else if (!WIPConfig.pendingEnabled && borderlessFullscreen) {
				setBorderlessFullscreen(false);
				fullscreen = true;
			}
			// Update fullscreen value
			WIPConfig.enabled = WIPConfig.pendingEnabled;
		}
	}
}
