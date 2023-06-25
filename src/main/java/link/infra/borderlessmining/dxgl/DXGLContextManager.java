package link.infra.borderlessmining.dxgl;

import link.infra.borderlessmining.dxgl.strategies.dxbackbuffer.RegisterOnce;
import link.infra.borderlessmining.util.DXGLWindowHooks;
import net.minecraft.client.util.Window;
import org.jetbrains.annotations.NotNull;

public class DXGLContextManager {

	public static void setupContext(@NotNull Window window, boolean initiallyFullscreen) {
		// TODO: read config options/etc.
		DXGLWindowSettings settings = new DXGLWindowSettings();
		DXGLWindow dxglCtx = new RegisterOnce(window, settings);
		dxglCtx.setup(initiallyFullscreen);
		((DXGLWindowHooks)(Object)window).dxgl_attach(dxglCtx);
	}

	public static boolean enabled() {
		return true;
	}
}
