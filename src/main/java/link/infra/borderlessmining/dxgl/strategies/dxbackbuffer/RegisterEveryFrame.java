package link.infra.borderlessmining.dxgl.strategies.dxbackbuffer;

import link.infra.borderlessmining.dxgl.DXGLWindowSettings;
import net.minecraft.client.util.Window;

public class RegisterEveryFrame extends DXBackbuffer {
	public RegisterEveryFrame(Window parent, boolean initiallyFullscreen, DXGLWindowSettings settings) {
		super(parent, initiallyFullscreen, settings);
	}

	@Override
	protected void registerBackbuffer() {}
	@Override
	protected void unregisterBackbuffer() {}

	@Override
	protected void bindBackbuffer() {
		super.registerBackbuffer();
		super.bindBackbuffer();
	}

	@Override
	protected void unbindBackbuffer() {
		super.unbindBackbuffer();
		super.unregisterBackbuffer();
	}
}
