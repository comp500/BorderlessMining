package link.infra.borderlessmining.mixin;

import net.minecraft.client.WindowEventHandler;
import net.minecraft.client.WindowSettings;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.util.MonitorTracker;
import net.minecraft.client.util.Window;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Window.class)
public abstract class ExampleMixin {
	@Shadow
	private boolean fullscreen;

	private boolean borderlessFullscreen = false;

	@Inject(method = "toggleFullscreen", at = @At("HEAD"), cancellable = true)
	public void onToggleFullscreen(CallbackInfo info) {
		fullscreen = false;
		info.cancel();
		borderlessFullscreen = !borderlessFullscreen;
		System.out.println("Setting borderless fullscreen to " + borderlessFullscreen);
		// TODO: config system, initial properties:
		// enableBorderlessFullscreen boolean
		// addToVanillaOptionsMenu boolean (better name?)
		// x/y/height/width and screen?
	}

	@Inject(method = "isFullscreen", at = @At("RETURN"), cancellable = true)
	public void onIsFullscreen(CallbackInfoReturnable<Boolean> cir) {
		fullscreen = false;
		cir.setReturnValue(borderlessFullscreen);
	}

	// TODO: windowBoundsHandler to remember the state of windowed mode? how do we use windowedX/Y/Height/Width?

	// TODO: mapping changes:
	// setFullscreen to swapBuffers
	// flipFrame to swapWindowBuffers (RenderSystem)
	// method_4485 to updateFullscreen
	//     vsync as boolean param
	// method_4479 to updateWindowRegion?
	// method_4475 to applyVideoMode
	// field_5177 to currentFullscreen?
}
