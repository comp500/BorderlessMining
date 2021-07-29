package link.infra.borderlessmining.config;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.minecraft.client.option.CyclingOption;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.text.LiteralText;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Formatting;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.PointerBuffer;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class ModMenuCompat implements ModMenuApi {
	private static final Logger LOGGER = LogManager.getLogger(ModMenuCompat.class);

	@Override
	public ConfigScreenFactory<?> getModConfigScreenFactory() {
		return parent -> {
			ConfigHandler configHandler = ConfigHandler.getInstance();

			return new ConfigScreen(new TranslatableText("config.borderlessmining.title"), parent) {
				@Override
				public void save() {
					configHandler.save();
				}

				@Override
				public void addElements() {
					addOption(CyclingOption.create("config.borderlessmining.general.enabled",
						new TranslatableText("config.borderlessmining.general.enabled.tooltip"),
						options -> configHandler.isEnabledOrPending(),
						(options, option, value) -> configHandler.setEnabledPending(value)
					));
					addOption(CyclingOption.create("config.borderlessmining.general.videomodeoption",
						new TranslatableText("config.borderlessmining.general.videomodeoption.tooltip"),
						options -> configHandler.addToVanillaVideoSettings,
						(options, option, value) -> configHandler.addToVanillaVideoSettings = value
					));
					addOption(CyclingOption.create("config.borderlessmining.general.enabledmac",
						new TranslatableText("config.borderlessmining.general.enabledmac.tooltip"),
						options -> configHandler.enableMacOS,
						(options, option, value) -> configHandler.enableMacOS = value
					));

					// Get the monitor list
					List<String> monitorNames = new ArrayList<>();
					monitorNames.add(I18n.translate("config.borderlessmining.general.forcemonitor.current"));
					int currentMonitor = configHandler.forceWindowMonitor + 1;
					if (currentMonitor < 0) {
						currentMonitor = 0;
					}
					PointerBuffer monitors = GLFW.glfwGetMonitors();
					if (monitors == null || monitors.limit() < 1) {
						LOGGER.warn("Failed to get a valid monitor list!");
						currentMonitor = 0;
					} else {
						if (configHandler.forceWindowMonitor >= monitors.limit()) {
							LOGGER.warn("Monitor " + configHandler.forceWindowMonitor + " is greater than list size " + monitors.limit() + ", using monitor 0");
							currentMonitor = 0;
						}
						long monitorHandle;
						while (monitors.hasRemaining()) {
							monitorHandle = monitors.get();
							monitorNames.add(GLFW.glfwGetMonitorName(monitorHandle) + " (" + (monitorNames.size() - 1) + ")");
						}
					}

					int finalCurrentMonitor = currentMonitor;
					addOption(CyclingOption.create("config.borderlessmining.general.forcemonitor", monitorNames, LiteralText::new,
						options -> monitorNames.get(finalCurrentMonitor),
						(options, option, value) -> {
							int index = monitorNames.indexOf(value);
							if (index > -1) {
								configHandler.forceWindowMonitor = index - 1;
							}
						}
					));

					addHeading(new TranslatableText("config.borderlessmining.dimensions").formatted(Formatting.BOLD));
					addOption(CyclingOption.create("config.borderlessmining.dimensions.enabled",
						options -> configHandler.customWindowDimensions.enabled,
						(options, option, value) -> configHandler.customWindowDimensions = configHandler.customWindowDimensions.setEnabled(value)
					));
					addOption(CyclingOption.create("config.borderlessmining.dimensions.monitorcoordinates",
						new TranslatableText("config.borderlessmining.dimensions.monitorcoordinates.tooltip"),
						options -> configHandler.customWindowDimensions.useMonitorCoordinates,
						(options, option, value) -> configHandler.customWindowDimensions = configHandler.customWindowDimensions.setUseMonitorCoordinates(value)
					));
					addIntField(new TranslatableText("config.borderlessmining.dimensions.x"),
						() -> configHandler.customWindowDimensions.x,
						value -> configHandler.customWindowDimensions = configHandler.customWindowDimensions.setX(value));
					addIntField(new TranslatableText("config.borderlessmining.dimensions.y"),
						() -> configHandler.customWindowDimensions.y,
						value -> configHandler.customWindowDimensions = configHandler.customWindowDimensions.setY(value));
					addIntField(new TranslatableText("config.borderlessmining.dimensions.width"),
						() -> configHandler.customWindowDimensions.width,
						value -> configHandler.customWindowDimensions = configHandler.customWindowDimensions.setWidth(value));
					addIntField(new TranslatableText("config.borderlessmining.dimensions.height"),
						() -> configHandler.customWindowDimensions.height,
						value -> configHandler.customWindowDimensions = configHandler.customWindowDimensions.setHeight(value));
				}
			};
		};
	}
}
