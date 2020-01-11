package link.infra.borderlessmining.mixin;

import net.minecraft.client.gui.screen.VideoOptionsScreen;
import net.minecraft.client.gui.widget.AbstractButtonWidget;
import net.minecraft.client.options.DoubleOption;
import net.minecraft.client.options.FullScreenOption;
import net.minecraft.client.options.GameOptions;
import net.minecraft.client.options.Option;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.client.util.Monitor;
import net.minecraft.client.util.VideoMode;
import net.minecraft.client.util.Window;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Optional;

@Mixin(VideoOptionsScreen.class)
public abstract class FullScreenOptionMixin {
	private boolean borderlessFullscreenTest = false;

	// TODO: change this to a invoke redirect on this.list.addSingleOptionEntry(new FullScreenOption...
	@Redirect(at = @At(value = "NEW", target = "net/minecraft/client/options/FullScreenOption"), method = "init")
	public Option constructOption(Window window) {
		Monitor monitor = window.getMonitor();
		double modeCount = 0.0;
		if (monitor != null) {
			modeCount = monitor.getVideoModeCount();
		}

		double finalModeCount = modeCount;

		return new DoubleOption("options.fullscreen.resolution", -1.0, modeCount, 1.0F, (gameOptions) -> {
			if (monitor == null) {
				return borderlessFullscreenTest ? -1.0 : finalModeCount;
			} else {
				Optional<VideoMode> optional = window.getVideoMode();
				return optional.map((videoMode) -> (double)monitor.findClosestVideoModeIndex(videoMode)).orElse(borderlessFullscreenTest ? -1.0 : finalModeCount);
			}
		}, (gameOptions, newValue) -> {
			if (monitor != null) {
				if (newValue == -1.0 || newValue == finalModeCount) {
					window.setVideoMode(Optional.empty());
				} else {
					window.setVideoMode(Optional.of(monitor.getVideoMode(newValue.intValue())));
				}

			}
		}, (gameOptions, doubleOption) -> {
			if (monitor == null) {
				return I18n.translate("options.fullscreen.unavailable");
			} else {
				double newValue = doubleOption.get(gameOptions);
				String prefix = doubleOption.getDisplayPrefix();
				if (newValue == -1.0) {
					return prefix + I18n.translate("options.fullscreen.current");
				} else if (newValue == finalModeCount) {
					return "Borderless Window";
				}
				return monitor.getVideoMode((int)newValue).toString();
			}
		});
		// TODO: override written value, to set to -1 if it is == video mode count?
	}
}
