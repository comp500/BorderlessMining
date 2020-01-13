package link.infra.borderlessmining.mixin;

import link.infra.borderlessmining.config.WIPConfig;
import net.minecraft.client.options.DoubleOption;
import net.minecraft.client.options.FullScreenOption;
import net.minecraft.client.options.GameOptions;
import net.minecraft.client.util.Monitor;
import net.minecraft.client.util.Window;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

@Mixin(FullScreenOption.class)
public abstract class FullScreenOptionMixin {
	// Modify the superconstructor call in FullScreenOption to add an extra option for Borderless Fullscreen
	@ModifyArgs(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/options/DoubleOption;<init>(Ljava/lang/String;DDFLjava/util/function/Function;Ljava/util/function/BiConsumer;Ljava/util/function/BiFunction;)V"), method = "<init>(Lnet/minecraft/client/util/Window;Lnet/minecraft/client/util/Monitor;)V")
	private static void modifyDoubleOption(Args args, Window window, Monitor monitor) {
		if (!WIPConfig.optionEnabled) {
			return;
		}
		System.out.println("FullScreenOptionMixin start");
		WIPConfig.pendingEnabled = WIPConfig.enabled;

		// Add one extra option at the end for Borderless Fullscreen
		double max = args.<Double>get(2) + 1.0;
		args.set(2, max);

		// Modify the getter/setters to modify Borderless Fullscreen settings when the last option is set
		Function<GameOptions, Double> getter = args.get(4);
		BiConsumer<GameOptions, Double> setter = args.get(5);
		BiFunction<GameOptions, DoubleOption, String> desc = args.get(6);

		args.set(4, (Function<GameOptions, Double>) (opts) -> {
			if (WIPConfig.pendingEnabled) {
				return max;
			}
			return getter.apply(opts);
		});
		args.set(5, (BiConsumer<GameOptions, Double>) (opts, val) -> {
			if (val == max) {
				System.out.println("Pending enabled!!");
				WIPConfig.pendingEnabled = true;
				// Set the actual value to "Current"
				setter.accept(opts, -1.0);
			} else {
				System.out.println("Pending disabled");
				WIPConfig.pendingEnabled = false;
				setter.accept(opts, val);
			}
		});
		args.set(6, (BiFunction<GameOptions, DoubleOption, String>) (opts, val) -> {
			if (val.get(opts) == max) {
				// TODO: i18n
				return "Borderless Fullscreen";
			}
			return desc.apply(opts, val);
		});
		// TODO: apply borderlessfullscreen when invalidating dirty video modes
	}
}
