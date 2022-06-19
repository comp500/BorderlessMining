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
	private static final transient Path configFile = FabricLoader.getInstance().getConfigDir().resolve("borderlessmining.json");
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
				INSTANCE.save();
			}
			INSTANCE.enabledPending = INSTANCE.enableBorderlessFullscreen;
		}
		return INSTANCE;
	}

	private boolean enableBorderlessFullscreen = true;
	public boolean addToVanillaVideoSettings = true;
	public boolean enableMacOS = false;

	public CustomWindowDimensions customWindowDimensions = CustomWindowDimensions.INITIAL;
	public int forceWindowMonitor = -1;

	public static class CustomWindowDimensions {
		public static transient final CustomWindowDimensions INITIAL = new CustomWindowDimensions();

		public final boolean enabled;
		public final int x;
		public final int y;
		public final int width;
		public final int height;
		public boolean useMonitorCoordinates;

		private CustomWindowDimensions() {
			enabled = false;
			x = 0;
			y = 0;
			width = 0;
			height = 0;
			useMonitorCoordinates = true;
		}

		public CustomWindowDimensions(boolean enabled, int x, int y, int width, int height, boolean useMonitorCoordinates) {
			this.enabled = enabled;
			this.x = x;
			this.y = y;
			this.width = width;
			this.height = height;
			this.useMonitorCoordinates = useMonitorCoordinates;
		}

		public CustomWindowDimensions setEnabled(boolean enabled) {
			return new CustomWindowDimensions(enabled, x, y, width, height, useMonitorCoordinates);
		}

		public CustomWindowDimensions setX(int x) {
			return new CustomWindowDimensions(enabled, x, y, width, height, useMonitorCoordinates);
		}

		public CustomWindowDimensions setY(int y) {
			return new CustomWindowDimensions(enabled, x, y, width, height, useMonitorCoordinates);
		}

		public CustomWindowDimensions setWidth(int width) {
			return new CustomWindowDimensions(enabled, x, y, width, height, useMonitorCoordinates);
		}

		public CustomWindowDimensions setHeight(int height) {
			return new CustomWindowDimensions(enabled, x, y, width, height, useMonitorCoordinates);
		}

		public CustomWindowDimensions setUseMonitorCoordinates(boolean useMonitorCoordinates) {
			return new CustomWindowDimensions(enabled, x, y, width, height, useMonitorCoordinates);
		}
	}

	private transient boolean enabledPending = true;
	private transient boolean enabledDirty = false;

	public void setEnabledPending(boolean en) {
		if (enabledPending != en) {
			enabledPending = en;
			enabledDirty = (en != isEnabled());
		}
	}

	public boolean isEnabledOrPending() {
		return enabledDirty ? enabledPending : isEnabled();
	}

	public boolean isEnabled() {
		return enableBorderlessFullscreen && (!MinecraftClient.IS_SYSTEM_MAC || enableMacOS);
	}

	public void save() {
		if (enabledDirty) {
			//noinspection ConstantConditions
			WindowHooks window = (WindowHooks) (Object) MinecraftClient.getInstance().getWindow();

			// This must be done before changing window mode/pos/size as changing those restarts FullScreenOptionMixin
			enableBorderlessFullscreen = enabledPending;
			enabledDirty = false;

			// Update window state
			window.borderlessmining_apply();
		}

		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		try (FileWriter writer = new FileWriter(configFile.toFile())) {
			gson.toJson(this, ConfigHandler.class, writer);
		} catch (IOException e) {
			LOGGER.error("Failed to save configuration", e);
		}
	}

	public void saveIfDirty() {
		if (enabledDirty) {
			save();
		}
	}
}
