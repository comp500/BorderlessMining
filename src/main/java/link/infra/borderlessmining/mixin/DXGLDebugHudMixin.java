package link.infra.borderlessmining.mixin;

import link.infra.borderlessmining.dxgl.DXGLWindow;
import link.infra.borderlessmining.util.DXGLWindowHooks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.DebugHud;
import net.minecraft.util.Formatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.List;

@Mixin(DebugHud.class)
public class DXGLDebugHudMixin {
	@Inject(method = "getRightText", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/MinecraftClient;hasReducedDebugInfo()Z"), locals = LocalCapture.CAPTURE_FAILSOFT)
	protected void appendRightText(CallbackInfoReturnable<List<String>> cir, long l, long m, long n, long o, List<String> list) {
		DXGLWindow ctx = ((DXGLWindowHooks)(Object)MinecraftClient.getInstance().getWindow()).dxgl_getContext();
		list.add("");
		if (ctx == null) {
			list.add("DXGL " + Formatting.RED + "disabled");
		} else {
			list.add("DXGL " + Formatting.GREEN + "enabled");
			// TODO: more debug info
		}
	}
}
