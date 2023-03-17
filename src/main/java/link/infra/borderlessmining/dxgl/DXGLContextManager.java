package link.infra.borderlessmining.dxgl;

import link.infra.borderlessmining.util.DXGLWindowHooks;
import net.minecraft.client.util.Window;
import org.jetbrains.annotations.NotNull;

public class DXGLContextManager {
	public static <T extends Window & DXGLWindowHooks> void setupContext(@NotNull T window) {
		// TODO: read config options/etc.
		window.dxgl_attach(new DXGLWindow(window));
	}

	public static boolean enabled() {
		return true;
	}
}
