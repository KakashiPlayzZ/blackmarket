package com.kakashiplayz.blackmarket;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;

public class ConfigManager {

    private BlackMarket plugin;

    private FileConfiguration config;
    private FileConfiguration messages;
    private FileConfiguration items;

    private File configFile;
    private File messagesFile;
    private File itemsFile;

    public ConfigManager(BlackMarket plugin) {
        this.plugin = plugin;
        setup();
    }

    public void setup() {
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdir();
        }

        configFile = new File(plugin.getDataFolder(), "config.yml");
        messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        itemsFile = new File(plugin.getDataFolder(), "items.yml");

        if (!configFile.exists()) {
            plugin.saveResource("config.yml", false);
        }
        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        if (!itemsFile.exists()) {
            plugin.saveResource("items.yml", false);
        }

        load();
    }

    public void load() {
        config = YamlConfiguration.loadConfiguration(configFile);
        messages = YamlConfiguration.loadConfiguration(messagesFile);
        items = YamlConfiguration.loadConfiguration(itemsFile);
    }

    public void reload() {
        load();
    }

    public void saveItems() {
        try {
            items.save(itemsFile);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void saveConfig() {
        try {
            config.save(configFile);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public FileConfiguration getConfig() {
        return config;
    }

    public FileConfiguration getMessages() {
        return messages;
    }

    public FileConfiguration getItems() {
        return items;
    }
}
