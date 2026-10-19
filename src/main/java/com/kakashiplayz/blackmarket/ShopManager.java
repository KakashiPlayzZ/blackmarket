package com.kakashiplayz.blackmarket;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.Random;

public class ShopManager {

    private final BlackMarket plugin;
    private boolean shopOpen = false;
    private int currentTaskId = -1;
    private long nextOpenTime = -1;

    public ShopManager(BlackMarket plugin) {
        this.plugin = plugin;
        initTiming();
    }

    public void reloadTiming() {
        if (currentTaskId != -1) {
            Bukkit.getScheduler().cancelTask(currentTaskId);
            currentTaskId = -1;
        }
        initTiming();
    }

    private void initTiming() {
        boolean alwaysOpen = plugin.getConfigManager().getConfig().getBoolean("settings.always-open");
        boolean randomOpen = plugin.getConfigManager().getConfig().getBoolean("settings.random-open");
        
        if (alwaysOpen) {
            if (!shopOpen) {
                openShop();
            }
            return;
        }

        if (randomOpen && !shopOpen) {
            scheduleRandomOpen();
        } else if (!randomOpen && shopOpen && !alwaysOpen) {
            closeShop();
        } else if (!randomOpen) {
            nextOpenTime = -1;
        }
    }

    private void scheduleRandomOpen() {
        int delayTicks = (new Random().nextInt(72000) + 72000); // 1-2 hours
        nextOpenTime = System.currentTimeMillis() + (delayTicks * 50L);
        currentTaskId = Bukkit.getScheduler().runTaskLater(plugin, this::openShop, delayTicks).getTaskId();
    }

    public void openShop() {
        if (currentTaskId != -1) {
            Bukkit.getScheduler().cancelTask(currentTaskId);
            currentTaskId = -1;
        }

        if (shopOpen) return;
        shopOpen = true;
        nextOpenTime = -1;

        if (plugin.getConfigManager().getConfig().getBoolean("effects.broadcast")) {
            String msg = ChatColor.translateAlternateColorCodes('&', plugin.getConfigManager().getMessages().getString("shop-open"));
            Bukkit.broadcastMessage(msg);
        }

        for (Player p : Bukkit.getOnlinePlayers()) {
            if (plugin.getConfigManager().getConfig().getBoolean("effects.sound")) {
                p.playSound(p.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 1.0f);
            }
            if (plugin.getConfigManager().getConfig().getBoolean("effects.title")) {
                String title = ChatColor.translateAlternateColorCodes('&', "&4&lBlackMarket");
                String sub = ChatColor.translateAlternateColorCodes('&', "&cShop is now open!");
                p.sendTitle(title, sub, 10, 70, 20);
            }
        }

        plugin.getDatabaseManager().resetLimits();
        plugin.getDiscordWebhook().sendMessage("BlackMarket shop is now OPEN!");

        boolean alwaysOpen = plugin.getConfigManager().getConfig().getBoolean("settings.always-open");
        if (!alwaysOpen) {
            String durationStr = plugin.getConfigManager().getConfig().getString("settings.open-duration", "30m");
            long durationTicks = parseDuration(durationStr);
            currentTaskId = Bukkit.getScheduler().runTaskLater(plugin, this::closeShop, durationTicks).getTaskId();
        }
    }

    public void closeShop() {
        if (currentTaskId != -1) {
            Bukkit.getScheduler().cancelTask(currentTaskId);
            currentTaskId = -1;
        }

        if (!shopOpen) return;
        shopOpen = false;

        if (plugin.getConfigManager().getConfig().getBoolean("effects.broadcast")) {
            String msg = ChatColor.translateAlternateColorCodes('&', plugin.getConfigManager().getMessages().getString("shop-close"));
            Bukkit.broadcastMessage(msg);
        }

        plugin.getDiscordWebhook().sendMessage("BlackMarket shop is now CLOSED!");
        
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.getOpenInventory().getTitle().contains("BlackMarket")) {
                p.closeInventory();
            }
        }

        if (plugin.getConfigManager().getConfig().getBoolean("settings.random-open")) {
            scheduleRandomOpen();
        }
    }

    public boolean isShopOpen() {
        return shopOpen;
    }

    public long getNextOpenTime() {
        return nextOpenTime;
    }

    private long parseDuration(String s) {
        if (s == null || s.isEmpty()) return 36000;
        char unit = s.charAt(s.length()-1);
        int val = Integer.parseInt(s.substring(0, s.length()-1));
        if (unit == 'm') return val * 1200L;
        if (unit == 'h') return val * 72000L;
        if (unit == 's') return val * 20L;
        return val * 1200L;
    }
}
