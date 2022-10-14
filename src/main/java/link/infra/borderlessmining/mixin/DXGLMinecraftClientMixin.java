package link.infra.borderlessmining.mixin;

import link.infra.borderlessmining.util.DXGLWindowHooks;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.util.Window;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Environment(EnvType.CLIENT)
@Mixin(MinecraftClient.class)
public class DXGLMinecraftClientMixin {
	@Shadow @Final private Window window;

	@Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gl/Framebuffer;draw(II)V"))
	private void draw(Framebuffer instance, int width, int height) {
		((DXGLWindowHooks)(Object)window).dxgl_getOffscreenContext().draw(instance, width, height);
	}
}
