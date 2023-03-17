package link.infra.borderlessmining.util;

import link.infra.borderlessmining.dxgl.DXGLWindow;
import org.jetbrains.annotations.Nullable;

public interface DXGLWindowHooks {
	@Nullable DXGLWindow dxgl_getContext();
	void dxgl_attach(DXGLWindow window);
	void dxgl_detach();
}
