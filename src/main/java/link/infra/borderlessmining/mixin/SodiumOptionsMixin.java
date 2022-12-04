package link.infra.borderlessmining.mixin;

import link.infra.borderlessmining.config.ConfigHandler;
import me.jellysquid.mods.sodium.client.gui.SodiumGameOptionPages;
import me.jellysquid.mods.sodium.client.gui.options.OptionGroup;
import me.jellysquid.mods.sodium.client.gui.options.OptionImpl;
import me.jellysquid.mods.sodium.client.gui.options.OptionPage;
import me.jellysquid.mods.sodium.client.gui.options.control.TickBoxControl;
import me.jellysquid.mods.sodium.client.gui.options.storage.MinecraftOptionsStorage;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.List;

/**
 * This adds a Borderless-Option to the Sodium Options-Menu if present.
 *
 * @author ChloeCDN
 */
@Mixin(SodiumGameOptionPages.class)
public class SodiumOptionsMixin {

    @Shadow
    @Final
    private static MinecraftOptionsStorage vanillaOpts;

    @Inject(method = "general", at = @At(value = "INVOKE", target = "Lme/jellysquid/mods/sodium/client/gui/options/OptionGroup;createBuilder()Lme/jellysquid/mods/sodium/client/gui/options/OptionGroup$Builder;", ordinal = 2, shift = At.Shift.BEFORE), locals = LocalCapture.CAPTURE_FAILSOFT, remap = false)
    private static void addBorderlessOption(CallbackInfoReturnable<OptionPage> cir, List<OptionGroup> groups) {
        groups.add(OptionGroup.createBuilder().add(OptionImpl.createBuilder(Boolean.TYPE, vanillaOpts).setName(Text.translatable("config.borderlessmining.general.enabled")).setTooltip(Text.translatable("config.borderlessmining.general.enabled.tooltip")).setControl(TickBoxControl::new).setBinding((opts, value) -> {
            ConfigHandler.getInstance().setEnabledPending(value);
            ConfigHandler.getInstance().save();
        }, (opts) -> ConfigHandler.getInstance().isEnabledOrPending()).build()).build());
    }
}