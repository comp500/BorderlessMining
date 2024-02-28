package link.infra.borderlessmining.mixin;

import link.infra.borderlessmining.config.SodiumCompat;
import me.jellysquid.mods.sodium.client.gui.SodiumOptionsGUI;
import me.jellysquid.mods.sodium.client.gui.options.OptionPage;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(value = SodiumOptionsGUI.class, remap = false)
public class SodiumOptionsMixin {
    @Shadow
    @Final
    private List<OptionPage> pages;


    @Inject(method = "<init>", at = @At("TAIL"))
    private void init(CallbackInfo info) {
        this.pages.add(SodiumCompat.config());
    }
}
