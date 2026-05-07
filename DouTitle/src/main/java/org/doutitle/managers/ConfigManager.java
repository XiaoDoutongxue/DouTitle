package org.doutitle.managers;

import org.doutitle.DouTitle;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;

public class ConfigManager {

    private final DouTitle plugin;
    private YamlConfiguration config;
    private YamlConfiguration shopConfig;
    private YamlConfiguration openConfig;

    public ConfigManager(DouTitle plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        plugin.reloadConfig();
        config = (YamlConfiguration) plugin.getConfig();

        File shopFile = new File(plugin.getDataFolder(), "shop.yml");
        if (!shopFile.exists()) {
            plugin.saveResource("shop.yml", false);
        }
        shopConfig = YamlConfiguration.loadConfiguration(shopFile);

        File openFile = new File(plugin.getDataFolder(), "open.yml");
        if (!openFile.exists()) {
            plugin.saveResource("open.yml", false);
        }
        openConfig = YamlConfiguration.loadConfiguration(openFile);
    }

    public boolean isChatEnabled() {
        return config.getBoolean("ischat", true);
    }

    public int getCoolingTime() {
        return config.getInt("Coolingtime", 1);
    }

    public YamlConfiguration getShopConfig() {
        return shopConfig;
    }

    public YamlConfiguration getOpenConfig() {
        return openConfig;
    }
}