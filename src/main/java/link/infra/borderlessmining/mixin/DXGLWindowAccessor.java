package link.infra.borderlessmining.mixin;

import net.minecraft.client.util.Window;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Window.class)
public interface DXGLWindowAccessor {
	@Invoker
	void callOnWindowPosChanged(long window, int x, int y);
	@Invoker
	void callOnFramebufferSizeChanged(long window, int width, int height);
	@Invoker
	void callOnWindowSizeChanged(long window, int width, int height);
	@Invoker
	void callOnWindowFocusChanged(long window, boolean focussed);
	@Invoker
	void callOnCursorEnterChanged(long window, boolean entered);
}
