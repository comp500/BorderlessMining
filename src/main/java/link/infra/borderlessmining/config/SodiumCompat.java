package link.infra.borderlessmining.config;

import com.google.common.collect.ImmutableList;
import me.jellysquid.mods.sodium.client.gui.options.*;
import me.jellysquid.mods.sodium.client.gui.options.control.Control;
import me.jellysquid.mods.sodium.client.gui.options.control.ControlValueFormatter;
import me.jellysquid.mods.sodium.client.gui.options.control.SliderControl;
import me.jellysquid.mods.sodium.client.gui.options.control.TickBoxControl;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.text.Text;
import org.lwjgl.PointerBuffer;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class SodiumCompat {
    public static final ConfigStorage configStorage = new ConfigStorage();


    public static OptionPage config() {
        List<OptionGroup> groups = new ArrayList<OptionGroup>();

        groups.add(OptionGroup.createBuilder()
                .add(OptionImpl.createBuilder(boolean.class, configStorage)
                        .setName(Text.translatable("config.borderlessmining.general.enabled"))
                        .setTooltip(Text.translatable("config.borderlessmining.general.enabled.tooltip"))
                        .setControl(TickBoxControl::new)
                        .setBinding(ConfigHandler::setEnabledPending, ConfigHandler::isEnabled)
                        .build())
                .add(OptionImpl.createBuilder(boolean.class, configStorage)
                        .setName(Text.translatable("config.borderlessmining.general.videomodeoption"))
                        .setTooltip(Text.translatable("config.borderlessmining.general.videomodeoption.tooltip"))
                        .setControl(TickBoxControl::new)
                        .setBinding((opt, value) -> opt.addToVanillaVideoSettings = value, (opt) -> opt.addToVanillaVideoSettings)
                        .build())
                .add(OptionImpl.createBuilder(boolean.class, configStorage)
                        .setName(Text.translatable("config.borderlessmining.general.enabledmac"))
                        .setTooltip(Text.translatable("config.borderlessmining.general.enabledmac.tooltip"))
                        .setControl(TickBoxControl::new)
                        .setBinding((opt, value) -> opt.enableMacOS = value, (opt) -> opt.enableMacOS)
                        .build())
                .build());

        // monitors are not listed because of the way sodium works. will implement later

        groups.add(OptionGroup.createBuilder()
                .add(OptionImpl.createBuilder(boolean.class, configStorage)
                        .setName(Text.translatable("config.borderlessmining.dimensions"))
                        .setTooltip(Text.empty())
                        .setControl(TickBoxControl::new)
                        .setBinding((opt, val) -> opt.customWindowDimensions = opt.customWindowDimensions.setEnabled(true), (opt) -> opt.customWindowDimensions.enabled)
                        .build())
                .add(OptionImpl.createBuilder(boolean.class, configStorage)
                        .setName(Text.translatable("config.borderlessmining.dimensions.monitorcoordinates"))
                        .setTooltip(Text.translatable("config.borderlessmining.dimensions.monitorcoordinates.tooltip"))
                        .setControl(TickBoxControl::new)
                        .setBinding((opt, val) -> opt.customWindowDimensions = opt.customWindowDimensions.setUseMonitorCoordinates(true), (opt) -> opt.customWindowDimensions.useMonitorCoordinates)
                        .build())
                .add(OptionImpl.createBuilder(int.class, configStorage)
                        .setName(Text.translatable("config.borderlessmining.dimensions.x"))
                        .setTooltip(Text.empty())
                        .setControl(option -> new SliderControl(option, 0, 9999, 1, ControlValueFormatter.number()))
                        .setBinding((opt, val) -> opt.customWindowDimensions = opt.customWindowDimensions.setX(val), (opt) -> opt.customWindowDimensions.x)
                        .build())
                .add(OptionImpl.createBuilder(int.class, configStorage)
                        .setName(Text.translatable("config.borderlessmining.dimensions.y"))
                        .setTooltip(Text.empty())
                        .setControl(option -> new SliderControl(option, 0, 9999, 1, ControlValueFormatter.number()))
                        .setBinding((opt, val) -> opt.customWindowDimensions = opt.customWindowDimensions.setY(val), (opt) -> opt.customWindowDimensions.y)
                        .build())
                .add(OptionImpl.createBuilder(int.class, configStorage)
                        .setName(Text.translatable("config.borderlessmining.dimensions.width"))
                        .setTooltip(Text.empty())
                        .setControl(option -> new SliderControl(option, 0, 9999, 1, ControlValueFormatter.number()))
                        .setBinding((opt, val) -> opt.customWindowDimensions = opt.customWindowDimensions.setWidth(val), (opt) -> opt.customWindowDimensions.width)
                        .build())
                .add(OptionImpl.createBuilder(int.class, configStorage)
                        .setName(Text.translatable("config.borderlessmining.dimensions.height"))
                        .setTooltip(Text.empty())
                        .setControl(option -> new SliderControl(option, 0, 9999, 1, ControlValueFormatter.number()))
                        .setBinding((opt, val) -> opt.customWindowDimensions = opt.customWindowDimensions.setHeight(val), (opt) -> opt.customWindowDimensions.height)
                        .build())
                .build());

        return new OptionPage(Text.translatable("config.borderlessmining.sodium"), ImmutableList.copyOf(groups));
    }
}
