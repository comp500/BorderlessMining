package link.infra.borderlessmining.dxgl.strategies.dxbackbuffer;

import link.infra.borderlessmining.dxgl.DXGLWindowSettings;
import net.minecraft.client.util.Window;

public class RegisterEveryFrame extends DXBackbuffer {
	public RegisterEveryFrame(Window parent, DXGLWindowSettings settings) {
		super(parent, settings);
	}

	@Override
	protected void registerBackbuffer(int width, int height) {}
	@Override
	protected void unregisterBackbuffer() {}

	@Override
	protected void bindBackbuffer() {
		//super.registerBackbuffer(); TODO
		super.bindBackbuffer();
	}

	@Override
	protected void unbindBackbuffer() {
		super.unbindBackbuffer();
		//super.unregisterBackbuffer();
	}
}
