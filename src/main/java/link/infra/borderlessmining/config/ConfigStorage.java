package link.infra.borderlessmining.config;

import me.jellysquid.mods.sodium.client.gui.options.storage.OptionStorage;

public class ConfigStorage implements OptionStorage<ConfigHandler> {
    private final ConfigHandler config;

    public ConfigStorage() {
        this.config = ConfigHandler.getInstance();
    }

    @Override
    public ConfigHandler getData() {
        return this.config;
    }


    @Override
    public void save() {
        this.config.save();
    }
}
