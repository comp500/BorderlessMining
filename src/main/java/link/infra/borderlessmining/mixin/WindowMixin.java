package link.infra.borderlessmining.mixin;

import link.infra.borderlessmining.config.ConfigHandler;
import link.infra.borderlessmining.util.DimensionsResolver;
import link.infra.borderlessmining.util.WindowHooks;
import net.minecraft.client.util.MonitorTracker;
import net.minecraft.client.util.Window;
import org.lwjgl.glfw.GLFW;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Window.class)
public abstract class WindowMixin implements WindowHooks {
	@Shadow private int x;
	@Shadow private int y;
	@Shadow private int width;
	@Shadow private int height;

	@Shadow private int windowedX;
	@Shadow private int windowedY;
	@Shadow private int windowedWidth;
	@Shadow private int windowedHeight;

	@Shadow
	private boolean fullscreen;

	@Shadow
	@Final
	private long handle;

	@Shadow
	private boolean videoModeDirty;

	@Shadow
	@Final
	private MonitorTracker monitorTracker;

	@Shadow public abstract void applyVideoMode();

	// Determines if the window *was* in borderless fullscreen (hence the windowed coordinates should not be trusted)
	private boolean borderlessmining_wasEnabled = false;
	private int borderlessmining_oldWindowedX = 0;
	private int borderlessmining_oldWindowedY = 0;
	private int borderlessmining_oldWindowedWidth = 0;
	private int borderlessmining_oldWindowedHeight = 0;

	// Update the window to use borderless fullscreen, when the video/fullscreen mode is changed
	@Inject(method = "updateWindowRegion", at = @At("HEAD"), cancellable = true)
	private void beforeUpdateWindowRegion(CallbackInfo ci) {
		boolean currFullscreen = GLFW.glfwGetWindowMonitor(this.handle) != 0L;
		if (ConfigHandler.getInstance().isEnabled() && fullscreen) {
			if (!currFullscreen && !borderlessmining_wasEnabled) {
				// Currently in windowed mode; save old coordinates
				windowedX = x;
				windowedY = y;
				windowedWidth = width;
				windowedHeight = height;
			}

			// Make non-decorated
			GLFW.glfwSetWindowAttrib(handle, GLFW.GLFW_DECORATED, GLFW.GLFW_FALSE);
			DimensionsResolver res = new DimensionsResolver();
			if (res.resolve((Window) (Object) this, monitorTracker)) {
				// Note: x/y/width/height can change between any GLFW call
				x = res.x;
				y = res.y;
				width = res.width;
				height = res.height;
				// Set dimensions
				GLFW.glfwSetWindowMonitor(handle, 0L, x, y, width, height, GLFW.GLFW_DONT_CARE);

				borderlessmining_wasEnabled = true;
				ci.cancel();
			} else {
				// Reset decorated flag
				GLFW.glfwSetWindowAttrib(handle, GLFW.GLFW_DECORATED, GLFW.GLFW_TRUE);
			}
		} else {
			// Reset decorated flag
			GLFW.glfwSetWindowAttrib(handle, GLFW.GLFW_DECORATED, GLFW.GLFW_TRUE);
		}

		// The rest of this function will reset the windowed coordinates; if going borderless -> fullscreen, need to
		// make sure the old windowed coordinates are preserved
		borderlessmining_oldWindowedX = windowedX;
		borderlessmining_oldWindowedY = windowedY;
		borderlessmining_oldWindowedWidth = windowedWidth;
		borderlessmining_oldWindowedHeight = windowedHeight;
	}

	@Inject(method = "updateWindowRegion", at = @At("RETURN"))
	private void afterUpdateWindowRegion(CallbackInfo ci) {
		if (borderlessmining_wasEnabled) {
			borderlessmining_wasEnabled = false;

			// See above (preserves old windowed coordinates; ignores those from borderless)
			windowedX = borderlessmining_oldWindowedX;
			windowedY = borderlessmining_oldWindowedY;
			windowedWidth = borderlessmining_oldWindowedWidth;
			windowedHeight = borderlessmining_oldWindowedHeight;
		}
	}

	// Pretend to the constructor code (that creates the window) that it is not fullscreen
	@Redirect(method = "<init>",
		at = @At(value = "FIELD", target = "Lnet/minecraft/client/util/Window;fullscreen:Z", opcode = Opcodes.GETFIELD),
		// currentFullscreen still needs to be set correctly
		slice = @Slice(from = @At(value = "FIELD", target = "Lnet/minecraft/client/util/Window;currentFullscreen:Z", opcode = Opcodes.PUTFIELD, ordinal = 1))
	)
	public boolean constructorIsFullscreen(Window window) {
		if (ConfigHandler.getInstance().isEnabled()) {
			return false;
		}
		return fullscreen;
	}

	// Save config and update video mode if fullscreen is toggled
	@Inject(method = "toggleFullscreen", at = @At("HEAD"))
	public void onToggleFullscreen(CallbackInfo info) {
		ConfigHandler.getInstance().saveIfDirty();
	}

	// Save config and update video mode if video mode is applied
	@Inject(method = "applyVideoMode", at = @At("HEAD"))
	private void onApplyVideoMode(CallbackInfo info) {
		ConfigHandler.getInstance().saveIfDirty();
	}

	@Override
	public void borderlessmining_apply() {
		videoModeDirty = true;
		applyVideoMode();
	}
}
