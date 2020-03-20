package link.infra.borderlessmining.config;

import io.github.prospector.modmenu.api.ConfigScreenFactory;
import io.github.prospector.modmenu.api.ModMenuApi;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.resource.language.I18n;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.PointerBuffer;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class ModMenuCompat implements ModMenuApi {
	private static final Logger LOGGER = LogManager.getLogger(ModMenuCompat.class);

	@Override
	public String getModId() {
		return "borderlessmining";
	}

	@Override
	public ConfigScreenFactory<?> getModConfigScreenFactory() {
		return parent -> {
			ConfigHandler configHandler = ConfigHandler.getInstance();

			ConfigBuilder builder = ConfigBuilder.create();
			builder.setParentScreen(parent);
			builder.setTitle("config.borderlessmining.title");
			builder.setSavingRunnable(configHandler::save);

			ConfigEntryBuilder entryBuilder = builder.entryBuilder();
			ConfigCategory general = builder.getOrCreateCategory("config.borderlessmining.general");
			general.addEntry(entryBuilder.startBooleanToggle("config.borderlessmining.general.enabled", configHandler.isEnabled())
					.setDefaultValue(true)
					.setTooltip(I18n.translate("config.borderlessmining.general.enabled.tooltip_1"), I18n.translate("config.borderlessmining.general.enabled.tooltip_2"))
					.setSaveConsumer(configHandler::setEnabledPending)
					.build());
			general.addEntry(entryBuilder.startBooleanToggle("config.borderlessmining.general.videomodeoption", configHandler.addToVanillaVideoSettings)
					.setDefaultValue(true)
					.setTooltip(I18n.translate("config.borderlessmining.general.videomodeoption.tooltip_1"), I18n.translate("config.borderlessmining.general.videomodeoption.tooltip_2"))
					.setSaveConsumer(optionEnabled -> configHandler.addToVanillaVideoSettings = optionEnabled)
					.build());

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
				if (ConfigHandler.getInstance().forceWindowMonitor >= monitors.limit()) {
					LOGGER.warn("Monitor " + ConfigHandler.getInstance().forceWindowMonitor + " is greater than list size " + monitors.limit() + ", using monitor 0");
					currentMonitor = 0;
				}
				long monitorHandle;
				while (monitors.hasRemaining()) {
					monitorHandle = monitors.get();
					monitorNames.add(monitorNames.size() - 1 + ": " + GLFW.glfwGetMonitorName(monitorHandle));
				}
			}

			// Shedaniel said it's ok so I just ignore stupid warning
			//noinspection UnstableApiUsage
			general.addEntry(entryBuilder.startSelector("config.borderlessmining.general.forcemonitor", monitorNames.toArray(new String[0]), monitorNames.get(currentMonitor))
				.setDefaultValue(monitorNames.get(0))
				.setSaveConsumer(monitorName -> {
					int index = monitorNames.indexOf(monitorName);
					if (index > -1) {
						configHandler.forceWindowMonitor = index - 1;
					}
				}).build());
			// TODO: add entries for macOS

			ConfigCategory dimensions = builder.getOrCreateCategory("config.borderlessmining.dimensions");
			dimensions.addEntry(entryBuilder.startBooleanToggle("config.borderlessmining.dimensions.enabled", configHandler.customWindowDimensions.enabled)
				.setDefaultValue(false)
				.setSaveConsumer(enabled -> configHandler.customWindowDimensions = configHandler.customWindowDimensions.setEnabled(enabled))
				.build());
			dimensions.addEntry(entryBuilder.startBooleanToggle("config.borderlessmining.dimensions.monitorcoordinates", configHandler.customWindowDimensions.useMonitorCoordinates)
				.setDefaultValue(true)
				.setTooltip(I18n.translate("config.borderlessmining.dimensions.monitorcoordinates.tooltip_1"), I18n.translate("config.borderlessmining.dimensions.monitorcoordinates.tooltip_2"))
				.setSaveConsumer(useMonitorCoordinates -> configHandler.customWindowDimensions = configHandler.customWindowDimensions.setUseMonitorCoordinates(useMonitorCoordinates))
				.build());
			dimensions.addEntry(entryBuilder.startIntField("config.borderlessmining.dimensions.x", configHandler.customWindowDimensions.x)
				.setDefaultValue(0)
				.setSaveConsumer(x -> configHandler.customWindowDimensions = configHandler.customWindowDimensions.setX(x))
				.build());
			dimensions.addEntry(entryBuilder.startIntField("config.borderlessmining.dimensions.y", configHandler.customWindowDimensions.y)
				.setDefaultValue(0)
				.setSaveConsumer(y -> configHandler.customWindowDimensions = configHandler.customWindowDimensions.setY(y))
				.build());
			dimensions.addEntry(entryBuilder.startIntField("config.borderlessmining.dimensions.width", configHandler.customWindowDimensions.width)
				.setDefaultValue(0)
				.setSaveConsumer(width -> configHandler.customWindowDimensions = configHandler.customWindowDimensions.setWidth(width))
				.build());
			dimensions.addEntry(entryBuilder.startIntField("config.borderlessmining.dimensions.height", configHandler.customWindowDimensions.height)
				.setDefaultValue(0)
				.setSaveConsumer(height -> configHandler.customWindowDimensions = configHandler.customWindowDimensions.setHeight(height))
				.build());

			return builder.build();
		};
	}
}
