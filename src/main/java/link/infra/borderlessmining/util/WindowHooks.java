package link.infra.borderlessmining.util;

public interface WindowHooks {
	boolean borderlessmining_getFullscreenState();
	void borderlessmining_updateEnabledState(boolean destEnabledState, boolean currentFullscreenState, boolean destFullscreenState);
}
