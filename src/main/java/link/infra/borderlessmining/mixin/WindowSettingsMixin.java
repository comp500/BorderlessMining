package link.infra.borderlessmining.mixin;

import link.infra.borderlessmining.config.WIPConfig;
import link.infra.borderlessmining.util.SettingBorderlessFullscreen;
import net.minecraft.client.WindowSettings;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.OptionalInt;

@Mixin(WindowSettings.class)
public abstract class WindowSettingsMixin implements SettingBorderlessFullscreen {
	@Shadow
	private boolean fullscreen;

	private boolean borderlessFullscreen;

	@Override
	public boolean isBorderlessFullscreen() {
		return borderlessFullscreen;
	}

	@Inject(at = @At("RETURN"), method = "<init>")
	private void modifyInitialSettings(int width, int height, OptionalInt fullscreenWidth, OptionalInt fullscreenHeight, boolean fullscreen, CallbackInfo info) {
		// If the mod is enabled, set the fullscreen value (from run arguments) to false
		if (WIPConfig.enabled && this.fullscreen) {
			this.fullscreen = false;
			// Tell WindowMixin that the initial state is to enable borderless fullscreen
			this.borderlessFullscreen = true;
		}
	}
}
