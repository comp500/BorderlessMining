package link.infra.borderlessmining.config;

import link.infra.borderlessmining.util.WindowHooks;
import net.minecraft.client.MinecraftClient;

public class WIPConfig {
	private WIPConfig() {

	}

	private static WIPConfig INSTANCE = null;

	public static WIPConfig getInstance() {
		if (INSTANCE == null) {
			INSTANCE = new WIPConfig();
		}
		return INSTANCE;
	}

	// TODO: convert to getter/setter so get only!
	public boolean enabled = true;
	public boolean optionEnabled = true;

	private boolean enabledPending = true;
	private boolean enabledDirty = false;

	public void setEnabledPending(boolean en) {
		if (enabledPending != en) {
			enabledPending = en;
			enabledDirty = (en != enabled);
		}
	}

	public boolean isEnabledOrPending() {
		return enabledDirty ? enabledPending : enabled;
	}

	public boolean isEnabledDirty() {
		return enabledDirty;
	}

	public void save() {
		//noinspection ConstantConditions
		WindowHooks window = (WindowHooks) (Object) MinecraftClient.getInstance().getWindow();
		save(window.borderlessmining_getFullscreenState());
	}

	public void save(boolean destFullscreenState) {
		if (enabledDirty) {
			//noinspection ConstantConditions
			WindowHooks window = (WindowHooks) (Object) MinecraftClient.getInstance().getWindow();
			boolean currentState = window.borderlessmining_getFullscreenState();

			// This must be done before changing window mode/pos/size as changing those restarts FullScreenOptionMixin
			enabled = enabledPending;
			enabledDirty = false;

			window.borderlessmining_updateEnabledState(enabled, currentState, destFullscreenState);
		}

		// TODO: saving/loading to/from file
	}

	// TODO: config system, initial properties:
	// enableBorderlessFullscreen boolean
	// addToVanillaOptionsMenu boolean (better name?)
	// x/y/height/width and screen?
}
