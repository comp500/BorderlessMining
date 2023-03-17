package link.infra.borderlessmining.mixin;

import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(MinecraftClient.class)
public interface DXGLMinecraftClientAccessor {
	@Invoker
	String callGetWindowTitle();
}
