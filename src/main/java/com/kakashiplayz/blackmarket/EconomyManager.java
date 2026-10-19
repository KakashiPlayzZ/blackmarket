package com.kakashiplayz.blackmarket;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

public class EconomyManager {

    private Economy econ = null;
    private final BlackMarket plugin;

    public EconomyManager(BlackMarket plugin) {
        this.plugin = plugin;
    }

    public boolean setupEconomy() {
        if (plugin.getServer().getPluginManager().getPlugin("Vault") == null) {
            org.bukkit.Bukkit.getConsoleSender().sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&c[BlackMarket] Vault plugin is missing! Please install Vault."));
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = plugin.getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            org.bukkit.Bukkit.getConsoleSender().sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&c[BlackMarket] Vault is installed, but no Economy provider was found!"));
            org.bukkit.Bukkit.getConsoleSender().sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&c[BlackMarket] You MUST install an economy plugin like EssentialsX, CMI, or EconomyShopGUI."));
            return false;
        }
        econ = rsp.getProvider();
        return econ != null;
    }

    public boolean withdraw(Player player, double amount) {
        if (econ == null) return false;
        
        if (!econ.has(player, amount)) {
            return false;
        }
        
        return econ.withdrawPlayer(player, amount).transactionSuccess();
    }
    
    public Economy getEconomy() {
        return econ;
    }
}
