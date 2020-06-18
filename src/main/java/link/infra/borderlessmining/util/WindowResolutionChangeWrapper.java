package link.infra.borderlessmining.util;

import net.minecraft.client.WindowEventHandler;

/**
 * Utility decorator to disable onResolutionChanged when disabled
 */
public class WindowResolutionChangeWrapper implements WindowEventHandler {
	private boolean enabled = true;
	private final WindowEventHandler child;

	public WindowResolutionChangeWrapper(WindowEventHandler child) {
		this.child = child;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	@Override
	public void onWindowFocusChanged(boolean focused) {
		child.onWindowFocusChanged(focused);
	}

	@Override
	public void onResolutionChanged() {
		if (enabled) {
			child.onResolutionChanged();
		}
	}

	@Override
	public void method_30133() {
		child.method_30133();
	}
}
