package link.infra.borderlessmining.mixin;

import link.infra.borderlessmining.config.ConfigHandler;
import net.minecraft.client.gui.screen.option.VideoOptionsScreen;
import net.minecraft.client.option.SimpleOption;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

import java.util.function.Consumer;

@Mixin(VideoOptionsScreen.class)
public abstract class FullScreenOptionMixin {
	// Modify the constructor call to add an extra option for Borderless Fullscreen
	@ModifyArgs(method = "init",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/client/option/SimpleOption;<init>(Ljava/lang/String;Lnet/minecraft/client/option/SimpleOption$TooltipFactoryGetter;Lnet/minecraft/client/option/SimpleOption$ValueTextGetter;Lnet/minecraft/client/option/SimpleOption$Callbacks;Ljava/lang/Object;Ljava/util/function/Consumer;)V"))
	private void modifyOption(Args args) {
		if (!ConfigHandler.getInstance().addToVanillaVideoSettings) {
			return;
		}

		// Add one extra option at the end for Borderless Windowed
		SimpleOption.ValidatingIntSliderCallbacks cb = args.get(3);
		int bmOption = cb.maxInclusive() + 1;
		args.set(3, new SimpleOption.ValidatingIntSliderCallbacks(cb.minInclusive(), bmOption));

		// Modify the text getter to show Borderless Mining text
		SimpleOption.ValueTextGetter<Integer> oldTextGetter = args.get(2);
		args.set(2, (SimpleOption.ValueTextGetter<Integer>) (optionText, value) -> {
			if (value == bmOption) {
				return Text.translatable("text.borderlessmining.videomodename");
			}
			return oldTextGetter.toString(optionText, value);
		});

		// Change the default based on the existing option selection
		args.set(4, ConfigHandler.getInstance().isEnabledOrPending() ? bmOption : args.get(4));

		// Update BM settings when the slider is changed
		Consumer<Integer> oldConsumer = args.get(5);
		args.set(5, (Consumer<Integer>) value -> {
			if (value == bmOption) {
				ConfigHandler.getInstance().setEnabledPending(true);
				// Set the actual value to "Current"
				oldConsumer.accept(-1);
			} else {
				ConfigHandler.getInstance().setEnabledPending(false);
				oldConsumer.accept(value);
			}
		});
	}
}
