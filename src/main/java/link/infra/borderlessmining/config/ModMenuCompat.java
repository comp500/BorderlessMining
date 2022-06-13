package link.infra.borderlessmining.config;

import com.mojang.serialization.Codec;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.minecraft.client.option.SimpleOption;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.PointerBuffer;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class ModMenuCompat implements ModMenuApi {
	private static final Logger LOGGER = LogManager.getLogger(ModMenuCompat.class);

	@Override
	public ConfigScreenFactory<?> getModConfigScreenFactory() {
		return parent -> {
			ConfigHandler configHandler = ConfigHandler.getInstance();

			return new ConfigScreen(Text.translatable("config.borderlessmining.title"), parent) {
				@Override
				public void save() {
					configHandler.save();
				}

				@Override
				public void addElements() {
					addOption(SimpleOption.ofBoolean("config.borderlessmining.general.enabled",
						SimpleOption.constantTooltip(Text.translatable("config.borderlessmining.general.enabled.tooltip")),
						configHandler.isEnabledOrPending(),
						configHandler::setEnabledPending));
					addOption(SimpleOption.ofBoolean("config.borderlessmining.general.videomodeoption",
						SimpleOption.constantTooltip(Text.translatable("config.borderlessmining.general.videomodeoption.tooltip")),
						configHandler.addToVanillaVideoSettings,
						value -> configHandler.addToVanillaVideoSettings = value));
					addOption(SimpleOption.ofBoolean("config.borderlessmining.general.enabledmac",
						SimpleOption.constantTooltip(Text.translatable("config.borderlessmining.general.enabledmac.tooltip")),
						configHandler.enableMacOS,
						value -> configHandler.enableMacOS = value));

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

					addOption(new SimpleOption<>("config.borderlessmining.general.forcemonitor",
						SimpleOption.emptyTooltip(),
						(k, i) -> Text.of(monitorNames.get(i)),
						new SimpleOption.PotentialValuesBasedCallbacks<>(
							IntStream.range(0, monitorNames.size()).boxed().collect(Collectors.toList()),
							Codec.INT
						),
						currentMonitor,
						i -> configHandler.forceWindowMonitor = i));

					addHeading(Text.translatable("config.borderlessmining.dimensions").formatted(Formatting.BOLD));
					addOption(SimpleOption.ofBoolean("config.borderlessmining.dimensions.enabled",
						configHandler.customWindowDimensions.enabled,
						value -> configHandler.customWindowDimensions = configHandler.customWindowDimensions.setEnabled(value)));
					addOption(SimpleOption.ofBoolean("config.borderlessmining.dimensions.monitorcoordinates",
						SimpleOption.constantTooltip(Text.translatable("config.borderlessmining.dimensions.monitorcoordinates.tooltip")),
						configHandler.customWindowDimensions.useMonitorCoordinates,
						value -> configHandler.customWindowDimensions = configHandler.customWindowDimensions.setUseMonitorCoordinates(value)));
					addIntField(Text.translatable("config.borderlessmining.dimensions.x"),
						() -> configHandler.customWindowDimensions.x,
						value -> configHandler.customWindowDimensions = configHandler.customWindowDimensions.setX(value));
					addIntField(Text.translatable("config.borderlessmining.dimensions.y"),
						() -> configHandler.customWindowDimensions.y,
						value -> configHandler.customWindowDimensions = configHandler.customWindowDimensions.setY(value));
					addIntField(Text.translatable("config.borderlessmining.dimensions.width"),
						() -> configHandler.customWindowDimensions.width,
						value -> configHandler.customWindowDimensions = configHandler.customWindowDimensions.setWidth(value));
					addIntField(Text.translatable("config.borderlessmining.dimensions.height"),
						() -> configHandler.customWindowDimensions.height,
						value -> configHandler.customWindowDimensions = configHandler.customWindowDimensions.setHeight(value));
				}
			};
		};
	}
}
