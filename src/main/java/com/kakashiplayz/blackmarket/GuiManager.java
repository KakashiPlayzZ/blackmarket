package com.kakashiplayz.blackmarket;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class GuiManager implements Listener {

    private final BlackMarket plugin;

    public GuiManager(BlackMarket plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public void openShop(Player player) {
        if (!plugin.getShopManager().isShopOpen()) {
            if (!player.hasPermission("blackmarket.editor") && !player.hasPermission("blackmarket.admin") && !player.isOp()) {
                String msg = plugin.getConfigManager().getMessages().getString("shop-currently-closed");
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', msg));
                return;
            } else {
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&c[BlackMarket] The shop is currently closed to normal players. You bypassed this using admin permissions!"));
            }
        }

        Inventory inv = Bukkit.createInventory(null, 54, ChatColor.translateAlternateColorCodes('&', "&4&lBlackMarket Shop"));

        ConfigurationSection itemsSection = plugin.getConfigManager().getItems().getConfigurationSection("items");
        if (itemsSection != null) {
            List<String> keysToDisplay = new ArrayList<>(itemsSection.getKeys(false));
            if (plugin.getConfigManager().getConfig().getBoolean("settings.random-pick", false)) {
                java.util.Collections.shuffle(keysToDisplay);
                int amount = plugin.getConfigManager().getConfig().getInt("settings.random-pick-amount", 5);
                if (keysToDisplay.size() > amount) {
                    keysToDisplay = keysToDisplay.subList(0, amount);
                }
            }

            int autoSlot = 10;
            for (String key : keysToDisplay) {
                ConfigurationSection itemKey = itemsSection.getConfigurationSection(key);
                int slot = itemKey.getInt("slot");
                if (plugin.getConfigManager().getConfig().getBoolean("settings.random-pick", false)) {
                    slot = autoSlot;
                    autoSlot++;
                    // Skip edges and bottom rows for a cleaner center look if it gets too large, but simple sequential is fine for now
                    if (autoSlot == 17 || autoSlot == 26 || autoSlot == 35 || autoSlot == 44) autoSlot += 2;
                }
                
                String materialName = itemKey.getString("material", "STONE");
                Material mat = Material.matchMaterial(materialName);
                if (mat == null) mat = Material.STONE;

                ItemStack item = new ItemStack(mat);
                ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                    meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', itemKey.getString("name", key)));
                    List<String> lore = new ArrayList<>();
                    for (String l : itemKey.getStringList("lore")) {
                        lore.add(ChatColor.translateAlternateColorCodes('&', l));
                    }
                    
                    double price = itemKey.getDouble("price", 1000.0);
                    int stock = itemKey.getInt("stock", -1);
                    lore.add(ChatColor.translateAlternateColorCodes('&', "&8&m---------------"));
                    lore.add(ChatColor.translateAlternateColorCodes('&', "&a&lPrice: &2$" + price));
                    if (stock != -1) {
                        lore.add(ChatColor.translateAlternateColorCodes('&', "&eStock: &f" + stock));
                    } else {
                        lore.add(ChatColor.translateAlternateColorCodes('&', "&eStock: &fUnlimited"));
                    }
                    lore.add(ChatColor.translateAlternateColorCodes('&', "&eClick to purchase this item!"));
                    
                    // Internal invisible ID to track random picked items
                    lore.add(ChatColor.BLACK + "BlackMarketItemID:" + key);
                    
                    meta.setLore(lore);
                    item.setItemMeta(meta);
                }
                inv.setItem(slot, item);
            }
        }
        
        applyEffects(inv, player);
        player.openInventory(inv);
    }

    private void applyEffects(Inventory inv, Player player) {
        boolean effects = plugin.getConfigManager().getConfig().getBoolean("gui-effects.enabled");
        if (!effects) return;

        int speed = plugin.getConfigManager().getConfig().getInt("gui-effects.speed", 2);

        ItemStack bg = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta bm = bg.getItemMeta();
        if (bm != null) {
            bm.setDisplayName(" ");
            bg.setItemMeta(bm);
        }

        // Keep real items aside, fill entirely with bg first
        List<Integer> itemSlots = new ArrayList<>();
        List<ItemStack> realItems = new ArrayList<>();

        ConfigurationSection itemsSection = plugin.getConfigManager().getItems().getConfigurationSection("items");
        if (itemsSection != null) {
            for (String key : itemsSection.getKeys(false)) {
                ConfigurationSection itemKey = itemsSection.getConfigurationSection(key);
                int slot = itemKey.getInt("slot");
                if (slot >= 0 && slot < inv.getSize()) {
                    ItemStack real = inv.getItem(slot);
                    if (real != null) {
                        itemSlots.add(slot);
                        realItems.add(real.clone());
                    }
                }
            }
        }

        for (int i = 0; i < inv.getSize(); i++) {
            inv.setItem(i, bg.clone());
        }

        // Animation task
        new org.bukkit.scheduler.BukkitRunnable() {
            int step = 0;
            @Override
            public void run() {
                if (!player.isOnline() || !player.getOpenInventory().getTitle().contains("BlackMarket Shop")) {
                    this.cancel();
                    return;
                }
                
                if (step < itemSlots.size()) {
                    int slot = itemSlots.get(step);
                    ItemStack item = realItems.get(step);
                    inv.setItem(slot, item);
                    player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_HAT, 0.5f, 1.5f);
                    step++;
                } else {
                    player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 1.5f);
                    this.cancel();
                }
            }
        }.runTaskTimer(plugin, 5L, speed);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getView().getTitle().contains("BlackMarket Shop")) {
            event.setCancelled(true);
            ItemStack clickedItem = event.getCurrentItem();
            if (clickedItem == null || !clickedItem.hasItemMeta()) return;

            Player player = (Player) event.getWhoClicked();
            
            String idToBuy = null;
            if (clickedItem.getItemMeta().hasLore()) {
                for (String l : clickedItem.getItemMeta().getLore()) {
                    if (l.contains("BlackMarketItemID:")) {
                        idToBuy = ChatColor.stripColor(l).replace("BlackMarketItemID:", "");
                        break;
                    }
                }
            }

            if (idToBuy == null) return; // Background glass or invalid item

            ConfigurationSection iSection = plugin.getConfigManager().getItems().getConfigurationSection("items");
            if (iSection != null) {
                ConfigurationSection itemKey = iSection.getConfigurationSection(idToBuy);
                if (itemKey != null) {
                    handlePurchase(player, idToBuy, itemKey, event.getSlot());
                }
            }
        }
    }

    private void handlePurchase(Player player, String id, ConfigurationSection key, int guiSlot) {
        String perm = key.getString("permission", "");
        if (!perm.isEmpty() && !player.hasPermission(perm)) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getConfigManager().getMessages().getString("no-permission")));
            return;
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            int globalLimit = plugin.getConfigManager().getConfig().getInt("settings.player-purchase-limit", 
                                plugin.getConfigManager().getConfig().getInt("settings.limit-per-player", 2));
            int currentPurchases = 0;

            Connection conn = plugin.getDatabaseManager().getConnection();
            if (conn != null) {
                try (PreparedStatement stmt = conn.prepareStatement("SELECT purchases FROM player_limits WHERE player_uuid = ?")) {
                    stmt.setString(1, player.getUniqueId().toString());
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            currentPurchases = rs.getInt("purchases");
                        }
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }

            if (currentPurchases >= globalLimit) {
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getConfigManager().getMessages().getString("limit-reached")));
                return;
            }

            double price = key.getDouble("price");
            Bukkit.getScheduler().runTask(plugin, () -> {
                int currentStock = key.getInt("stock", -1);
                if (currentStock == 0) {
                    player.sendMessage(ChatColor.RED + "This item is currently out of stock!");
                    return;
                }

                if (plugin.getEconomyManager().withdraw(player, price)) {
                    // Success
                    if (currentStock > 0) {
                        key.set("stock", currentStock - 1);
                        
                        // Stock Market Feature: Double price if stock hits 0
                        if (currentStock - 1 == 0 && plugin.getConfigManager().getConfig().getBoolean("settings.stock-market", false)) {
                            double newPrice = key.getDouble("price", 1000.0) * 2;
                            key.set("price", newPrice);
                        }
                        
                        plugin.getConfigManager().saveItems();
                        
                        // Update inventory lore if they are still looking at the shop
                        if (player.getOpenInventory().getTitle().contains("BlackMarket Shop")) {
                            Inventory top = player.getOpenInventory().getTopInventory();
                            ItemStack shopItem = top.getItem(guiSlot);
                            if (shopItem != null && shopItem.hasItemMeta()) {
                                ItemMeta meta = shopItem.getItemMeta();
                                List<String> lore = meta.getLore();
                                if (lore != null) {
                                    for (int i = 0; i < lore.size(); i++) {
                                        if (ChatColor.stripColor(lore.get(i)).startsWith("Stock: ")) {
                                            lore.set(i, ChatColor.translateAlternateColorCodes('&', "&eStock: &f" + (currentStock - 1)));
                                            break;
                                        }
                                    }
                                    meta.setLore(lore);
                                    shopItem.setItemMeta(meta);
                                    top.setItem(guiSlot, shopItem);
                                }
                            }
                        }
                    }

                    Material mat = Material.matchMaterial(key.getString("material", "STONE"));
                    if (mat == null) mat = Material.STONE;
                    ItemStack item = new ItemStack(mat);
                    ItemMeta meta = item.getItemMeta();
                    if (meta != null) {
                        if (key.contains("name")) {
                            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', key.getString("name", id)));
                        }
                        if (key.contains("lore")) {
                            List<String> itemLore = new ArrayList<>();
                            for (String l : key.getStringList("lore")) {
                                itemLore.add(ChatColor.translateAlternateColorCodes('&', l));
                            }
                            meta.setLore(itemLore);
                        }
                        item.setItemMeta(meta);
                    }
                    player.getInventory().addItem(item);
                    player.sendMessage(ChatColor.GREEN + "You purchased " + key.getString("name", id) + ChatColor.GREEN + " for $" + price);
                    
                    // Log to DB
                    plugin.getDatabaseManager().logPurchase(player.getUniqueId(), player.getName(), id, price);
                    
                    // Increment limit
                    updateLimitAsync(player);

                    plugin.getDiscordWebhook().sendMessage(player.getName() + " bought " + id + " for " + price);
                } else {
                    player.sendMessage(ChatColor.RED + "You don't have enough money.");
                }
            });
        });
    }

    private void updateLimitAsync(Player player) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            Connection conn = plugin.getDatabaseManager().getConnection();
            if (conn != null) {
                try (PreparedStatement stmt = conn.prepareStatement("INSERT INTO player_limits (player_uuid, purchases) VALUES (?, 1) ON CONFLICT(player_uuid) DO UPDATE SET purchases = purchases + 1")) {
                    stmt.setString(1, player.getUniqueId().toString());
                    stmt.executeUpdate();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        });
    }
}
