package link.infra.borderlessmining.dxgl;

/**
 * Determines which icons have been set on a window handle.
 * Icons can only be set when they are visible, so this enum is used to keep track of which icons need to be set.
 */
public enum DXGLWindowIconState {
	NONE,
	/**
	 * Icons for this window were set while it was in fullscreen, so only the taskbar icon was set.
	 * This window should have icons set again when leaving fullscreen.
	 */
	ONLY_TASKBAR,
	ALL
}
