package link.infra.borderlessmining.dxgl.strategies.dxbackbuffer;

import link.infra.borderlessmining.dxgl.DXGLWindowSettings;
import net.minecraft.client.util.Window;

public class RegisterOnce extends DXBackbuffer {
	public RegisterOnce(Window parent, boolean initiallyFullscreen, DXGLWindowSettings settings) {
		super(parent, initiallyFullscreen, settings);
	}
}
