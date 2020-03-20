package link.infra.borderlessmining.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import link.infra.borderlessmining.util.WindowHooks;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;

public class ConfigHandler {
	private static final transient Path configFile = FabricLoader.getInstance().getConfigDirectory().toPath().resolve("borderlessmining.json");
	private static final transient Logger LOGGER = LogManager.getLogger(ConfigHandler.class);

	private ConfigHandler() {}

	private static transient ConfigHandler INSTANCE = null;

	public static ConfigHandler getInstance() {
		if (INSTANCE == null) {
			Gson gson = new Gson();
			try (FileReader reader = new FileReader(configFile.toFile())) {
				INSTANCE = gson.fromJson(reader, ConfigHandler.class);
			} catch (FileNotFoundException ignored) {
				// Do nothing!
			} catch (IOException e) {
				LOGGER.error("Failed to read configuration", e);
			}
			if (INSTANCE == null) {
				INSTANCE = new ConfigHandler();
			}
		}
		return INSTANCE;
	}

	private boolean enableBorderlessFullscreen = true;
	private boolean addToVanillaVideoSettings = true;

	private transient boolean enabledPending = true;
	private transient boolean enabledDirty = false;

	public void setEnabledPending(boolean en) {
		if (enabledPending != en) {
			enabledPending = en;
			enabledDirty = (en != enableBorderlessFullscreen);
		}
	}

	public boolean isEnabledOrPending() {
		return enabledDirty ? enabledPending : enableBorderlessFullscreen;
	}

	public boolean isEnabledDirty() {
		return enabledDirty;
	}

	public boolean isEnabled() {
		return enableBorderlessFullscreen;
	}

	public boolean isOptionEnabled() {
		return addToVanillaVideoSettings;
	}

	public void setOptionEnabled(boolean optionEnabled) {
		this.addToVanillaVideoSettings = optionEnabled;
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
			enableBorderlessFullscreen = enabledPending;
			enabledDirty = false;

			window.borderlessmining_updateEnabledState(enableBorderlessFullscreen, currentState, destFullscreenState);
		}

		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		try (FileWriter writer = new FileWriter(configFile.toFile())) {
			gson.toJson(this, ConfigHandler.class, writer);
		} catch (IOException e) {
			LOGGER.error("Failed to save configuration", e);
		}
	}

	// TODO: config system, initial properties:
	// enableBorderlessFullscreen boolean
	// addToVanillaOptionsMenu boolean (better name?)
	// x/y/height/width and screen?
}
