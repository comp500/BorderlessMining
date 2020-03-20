package link.infra.borderlessmining.mixin;

import net.minecraft.client.Keyboard;
import net.minecraft.client.MinecraftClient;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * This Mixin fixes a bug in Keyboard that prevents F11 changes from being saved.
 * See: https://bugs.mojang.com/browse/MC-175431
 */
@Mixin(Keyboard.class)
public class F11FixMixin {
	@Shadow @Final
	private MinecraftClient client;

	@Inject(method = "Lnet/minecraft/client/Keyboard;onKey(JIIII)V",
		at = @At(value = "FIELD", target = "Lnet/minecraft/client/options/GameOptions;fullscreen:Z",
			opcode = Opcodes.PUTFIELD, ordinal = 0, shift = At.Shift.BY, by = 1))
	public void keyPressed(long window, int key, int scancode, int i, int j, CallbackInfo ci) {
		this.client.options.write();
	}
}
