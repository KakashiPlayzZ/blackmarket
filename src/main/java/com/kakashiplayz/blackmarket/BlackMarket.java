package com.kakashiplayz.blackmarket;

import org.bukkit.ChatColor;
import org.bukkit.plugin.java.JavaPlugin;

public final class BlackMarket extends JavaPlugin {

    private static BlackMarket instance;

    private ConfigManager configManager;
    private DatabaseManager databaseManager;
    private ShopManager shopManager;
    private GuiManager guiManager;
    private CommandManager commandManager;
    private EconomyManager economyManager;
    private EditorManager editorManager;
    private DiscordWebhook discordWebhook;

    @Override
    public void onEnable() {
        instance = this;

        configManager = new ConfigManager(this);
        discordWebhook = new DiscordWebhook(this);
        
        // We need to wait 1 tick to ensure Vault and Economy plugins have loaded if using softdepend
        getServer().getScheduler().runTaskLater(this, () -> {
            economyManager = new EconomyManager(this);
            if (!economyManager.setupEconomy()) {
                org.bukkit.Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', "&c========================================"));
                org.bukkit.Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', "&c[BlackMarket] The plugin will now disable itself safely."));
                org.bukkit.Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', "&c========================================"));
                getServer().getPluginManager().disablePlugin(this);
                return;
            }

            databaseManager = new DatabaseManager(this);
            databaseManager.connect();

            shopManager = new ShopManager(this);
            guiManager = new GuiManager(this);
            commandManager = new CommandManager(this);
            editorManager = new EditorManager(this);

            org.bukkit.Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', "&a[BlackMarket] has been successfully enabled!"));
            org.bukkit.Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', "&a[BlackMarket] Author: KakashiPlayz"));
        }, 1L);
    }

    @Override
    public void onDisable() {
        if (databaseManager != null) {
            databaseManager.disconnect();
        }
        if (shopManager != null && shopManager.isShopOpen()) {
            shopManager.closeShop();
        }
        if (editorManager != null) {
            editorManager.unregister();
        }
        org.bukkit.Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', "&c[BlackMarket] has been successfully disabled!"));
    }

    public static BlackMarket getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public ShopManager getShopManager() {
        return shopManager;
    }

    public GuiManager getGuiManager() {
        return guiManager;
    }

    public EditorManager getEditorManager() {
        return editorManager;
    }

    public EconomyManager getEconomyManager() {
        return economyManager;
    }

    public DiscordWebhook getDiscordWebhook() {
        return discordWebhook;
    }
}
