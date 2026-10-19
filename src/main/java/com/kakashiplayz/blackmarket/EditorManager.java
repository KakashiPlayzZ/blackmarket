package com.kakashiplayz.blackmarket;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class EditorManager implements Listener {

    private final BlackMarket plugin;
    // UUID -> Item ID
    private final Map<UUID, String> priceEditMode = new HashMap<>();
    private final Map<UUID, String> slotEditMode = new HashMap<>();
    private final Map<UUID, String> stockEditMode = new HashMap<>();
    private final Map<UUID, String> permEditMode = new HashMap<>();
    
    // UUID -> page
    private final Map<UUID, Integer> viewerPages = new HashMap<>();

    private final List<UUID> durationEditMode = new ArrayList<>();
    private final List<UUID> randomPickEditMode = new ArrayList<>();

    public EditorManager(BlackMarket plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public void openControlPanel(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, ChatColor.translateAlternateColorCodes('&', "&c&lBlackMarket Control Panel"));
        
        FileConfiguration config = plugin.getConfigManager().getConfig();
        boolean alwaysOpen = config.getBoolean("settings.always-open");
        boolean randomOpen = config.getBoolean("settings.random-open");
        boolean randomPick = config.getBoolean("settings.random-pick", false);
        int randomPickAmount = config.getInt("settings.random-pick-amount", 5);
        boolean stockMarket = config.getBoolean("settings.stock-market", false);
        String duration = config.getString("settings.open-duration", "30m");
        boolean shopOpen = plugin.getShopManager().isShopOpen();

        inv.setItem(10, createGuiItem(Material.CHEST, "&a&lEdit Shop Items", "&7Click to open the Items Manager."));
        inv.setItem(11, createGuiItem(Material.DIAMOND, "&e&lEdit Random Pick Amount", "&7Currently: &a" + randomPickAmount, "", "&7Click to change how many items are randomly picked."));
        inv.setItem(12, createGuiItem(Material.LEVER, "&e&lForce Toggle Shop", "&7Currently: " + (shopOpen ? "&aOPEN" : "&cCLOSED"), "", "&7Click to manually turn the shop on or off."));
        inv.setItem(14, createGuiItem(Material.CLOCK, "&b&lAlways Open Mode", "&7Currently: " + (alwaysOpen ? "&aTRUE" : "&cFALSE"), "", "&7If true, the shop never closes."));
        inv.setItem(15, createGuiItem(Material.ENDER_PEARL, "&a&lRandom Pick Mode", "&7Currently: " + (randomPick ? "&aTRUE" : "&cFALSE"), "", "&7If true, the shop only shows random items on open."));
        inv.setItem(16, createGuiItem(Material.COMPASS, "&d&lRandom Open Mode", "&7Currently: " + (randomOpen ? "&aTRUE" : "&cFALSE"), "", "&7If true, the shop opens randomly every 1-2 hours."));
        inv.setItem(21, createGuiItem(Material.GOLD_INGOT, "&c&lStock Market Mode", "&7Currently: " + (stockMarket ? "&aTRUE" : "&cFALSE"), "", "&7If true, an item's price doubles when stock hits 0."));
        inv.setItem(22, createGuiItem(Material.NAME_TAG, "&6&lEdit Open Duration", "&7Currently: &e" + duration, "", "&7Click to change how long the shop stays open."));

        player.openInventory(inv);
    }

    private ItemStack createGuiItem(Material mat, String name, String... loreLines) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
            List<String> lore = new ArrayList<>();
            for (String l : loreLines) {
                lore.add(ChatColor.translateAlternateColorCodes('&', l));
            }
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    public void openItemsManager(Player player, int page) {
        viewerPages.put(player.getUniqueId(), page);
        Inventory inv = Bukkit.createInventory(null, 54, ChatColor.translateAlternateColorCodes('&', "&c&lItems Manager - Page " + (page + 1)));

        ConfigurationSection itemsSection = plugin.getConfigManager().getItems().getConfigurationSection("items");
        List<String> keys = new ArrayList<>();
        if (itemsSection != null) {
            keys.addAll(itemsSection.getKeys(false));
        }

        int itemsPerPage = 45;
        int maxPages = Math.max(1, (int) Math.ceil((double) keys.size() / itemsPerPage));

        int startIndex = page * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, keys.size());

        for (int i = startIndex; i < endIndex; i++) {
            String id = keys.get(i);
            ConfigurationSection sec = itemsSection.getConfigurationSection(id);
            if (sec == null) continue;

            String materialName = sec.getString("material", "STONE");
            Material mat = Material.matchMaterial(materialName);
            if (mat == null) mat = Material.STONE;

            ItemStack item = new ItemStack(mat);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', sec.getString("name", id)));
                List<String> lore = new ArrayList<>();
                for (String l : sec.getStringList("lore")) {
                    lore.add(ChatColor.translateAlternateColorCodes('&', l));
                }

                lore.add(ChatColor.GRAY + "---");
                lore.add(ChatColor.YELLOW + "Price: $" + sec.getDouble("price", 1000.0));
                lore.add(ChatColor.YELLOW + "GUI Slot: " + (sec.getInt("slot", 0) + 1));
                int stock = sec.getInt("stock", -1);
                lore.add(ChatColor.YELLOW + "Stock Limit: " + (stock == -1 ? "Unlimited" : stock));
                lore.add(ChatColor.AQUA + "Click to edit item settings");
                lore.add(ChatColor.DARK_GRAY + "ID: " + id);

                meta.setLore(lore);
                item.setItemMeta(meta);
            }
            inv.setItem(i - startIndex, item);
        }

        // Controls
        if (page > 0) {
            inv.setItem(45, createGuiItem(Material.ARROW, "&ePrevious Page"));
        }
        if (page < maxPages - 1) {
            inv.setItem(53, createGuiItem(Material.ARROW, "&eNext Page"));
        }
        inv.setItem(49, createGuiItem(Material.LIME_DYE, "&a&l+ Add New Item", "&7Click to open the Add Item menu."));
        inv.setItem(48, createGuiItem(Material.BARRIER, "&cBack", "&7Return to Control Panel"));

        player.openInventory(inv);
    }

    public void openAddItemGui(Player player) {
        Inventory inv = Bukkit.createInventory(null, 9, ChatColor.translateAlternateColorCodes('&', "&c&lDrop Item to Add"));
        player.openInventory(inv);
        player.sendMessage(ChatColor.GREEN + "Drop any item from your inventory into the top menu to add it.");
    }

    public void openItemSettings(Player player, String id) {
        FileConfiguration itemsConfig = plugin.getConfigManager().getItems();
        ConfigurationSection sec = itemsConfig.getConfigurationSection("items." + id);
        if (sec == null) {
            openItemsManager(player, 0);
            return;
        }

        Inventory inv = Bukkit.createInventory(null, 27, ChatColor.translateAlternateColorCodes('&', "&c&lItem Settings (" + id + ")"));

        String materialName = sec.getString("material", "STONE");
        Material mat = Material.matchMaterial(materialName);
        if (mat == null) mat = Material.STONE;
        ItemStack baseItem = new ItemStack(mat);
        ItemMeta meta = baseItem.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', sec.getString("name", id)));
            List<String> lore = new ArrayList<>();
            for (String l : sec.getStringList("lore")) {
                lore.add(ChatColor.translateAlternateColorCodes('&', l));
            }
            meta.setLore(lore);
            baseItem.setItemMeta(meta);
        }
        inv.setItem(4, baseItem);

        inv.setItem(10, createGuiItem(Material.GOLD_INGOT, "&eEdit Price", "&7Current: &a$" + sec.getDouble("price", 1000.0), "&eClick to edit!"));
        inv.setItem(12, createGuiItem(Material.ITEM_FRAME, "&eEdit GUI Slot", "&7Current: &a" + (sec.getInt("slot", 0) + 1), "&eClick to edit!"));
        int stock = sec.getInt("stock", -1);
        inv.setItem(14, createGuiItem(Material.CHEST, "&eEdit Stock Limit", "&7Current: &a" + (stock == -1 ? "Unlimited" : stock), "&eClick to edit!"));
        String perm = sec.getString("permission", "None");
        inv.setItem(16, createGuiItem(Material.NAME_TAG, "&eEdit Permission", "&7Current: &a" + (perm.isEmpty() ? "None" : perm), "&eClick to edit!"));

        inv.setItem(22, createGuiItem(Material.REDSTONE_BLOCK, "&c&lDelete Item", "&7Click to permanently remove this item."));
        inv.setItem(26, createGuiItem(Material.BARRIER, "&cBack", "&7Return to Items Manager"));

        player.openInventory(inv);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getCurrentItem() == null && !event.getView().getTitle().contains("Drop Item to Add")) return;
        
        Player player = (Player) event.getWhoClicked();
        String title = event.getView().getTitle();

        if (title.contains("BlackMarket Control Panel")) {
            event.setCancelled(true);
            if (event.getCurrentItem() == null) return;
            int slot = event.getSlot();
            FileConfiguration config = plugin.getConfigManager().getConfig();

            if (slot == 10) {
                openItemsManager(player, 0);
            } else if (slot == 11) {
                randomPickEditMode.add(player.getUniqueId());
                player.closeInventory();
                player.sendMessage(ChatColor.YELLOW + "Type the new random pick amount in chat (e.g. 5), or type 'cancel'.");
            } else if (slot == 12) {
                if (plugin.getShopManager().isShopOpen()) {
                    plugin.getShopManager().closeShop();
                } else {
                    plugin.getShopManager().openShop();
                }
                openControlPanel(player);
            } else if (slot == 14) {
                boolean current = config.getBoolean("settings.always-open");
                config.set("settings.always-open", !current);
                plugin.getConfigManager().saveConfig();
                plugin.getShopManager().reloadTiming();
                openControlPanel(player);
            } else if (slot == 15) {
                boolean current = config.getBoolean("settings.random-pick", false);
                config.set("settings.random-pick", !current);
                plugin.getConfigManager().saveConfig();
                openControlPanel(player);
            } else if (slot == 16) {
                boolean current = config.getBoolean("settings.random-open");
                config.set("settings.random-open", !current);
                plugin.getConfigManager().saveConfig();
                plugin.getShopManager().reloadTiming();
                openControlPanel(player);
            } else if (slot == 21) {
                boolean current = config.getBoolean("settings.stock-market", false);
                config.set("settings.stock-market", !current);
                plugin.getConfigManager().saveConfig();
                openControlPanel(player);
            } else if (slot == 22) {
                durationEditMode.add(player.getUniqueId());
                player.closeInventory();
                player.sendMessage(ChatColor.YELLOW + "Type the new open duration in chat (e.g. 30m, 1h), or type 'cancel'.");
            }
            return;
        }

        if (title.contains("Items Manager")) {
            event.setCancelled(true);
            if (event.getCurrentItem() == null) return;
            int slot = event.getSlot();
            int page = viewerPages.getOrDefault(player.getUniqueId(), 0);

            if (slot == 45 && event.getCurrentItem().getType() != Material.AIR) {
                openItemsManager(player, page - 1);
            } else if (slot == 53 && event.getCurrentItem().getType() != Material.AIR) {
                openItemsManager(player, page + 1);
            } else if (slot == 49) {
                openAddItemGui(player);
            } else if (slot == 48) {
                openControlPanel(player);
            } else if (slot < 45 && event.getCurrentItem().getType() != Material.AIR) {
                ItemMeta meta = event.getCurrentItem().getItemMeta();
                if (meta != null && meta.hasLore()) {
                    for (String l : meta.getLore()) {
                        String uncolored = ChatColor.stripColor(l);
                        if (uncolored.startsWith("ID: ")) {
                            String id = uncolored.substring(4);
                            openItemSettings(player, id);
                            break;
                        }
                    }
                }
            }
            return;
        }

        if (title.contains("Drop Item to Add")) {
            if (event.getClickedInventory() == event.getView().getTopInventory()) {
                if (event.getCursor() != null && event.getCursor().getType() != Material.AIR) {
                    event.setCancelled(true);
                    ItemStack cursorItem = event.getCursor().clone();
                    event.getView().setCursor(new ItemStack(Material.AIR));

                    // Register new item
                    registerNewItem(player, cursorItem);
                } else if (event.getAction().name().contains("HOTBAR")) {
                    event.setCancelled(true);
                    // Deny hotbar swap to top inv to prevent issues
                }
            } else if (event.isShiftClick() && event.getClickedInventory() == event.getView().getBottomInventory()) {
                event.setCancelled(true);
                ItemStack clickedItem = event.getCurrentItem();
                if (clickedItem != null && clickedItem.getType() != Material.AIR) {
                    ItemStack clone = clickedItem.clone();
                    event.setCurrentItem(new ItemStack(Material.AIR));
                    registerNewItem(player, clone);
                }
            }
            return;
        }

        if (title.contains("Item Settings")) {
            event.setCancelled(true);
            if (event.getCurrentItem() == null) return;
            String id = title.substring(title.indexOf("(") + 1, title.indexOf(")"));
            int slot = event.getSlot();

            if (slot == 10) { // Price
                priceEditMode.put(player.getUniqueId(), id);
                player.closeInventory();
                player.sendMessage(ChatColor.YELLOW + "Type the new price in chat, or type 'cancel' to abort.");
            } else if (slot == 12) { // Slot
                slotEditMode.put(player.getUniqueId(), id);
                player.closeInventory();
                player.sendMessage(ChatColor.YELLOW + "Type the new GUI slot (1-54) in chat, or type 'cancel' to abort.");
            } else if (slot == 14) { // Stock
                stockEditMode.put(player.getUniqueId(), id);
                player.closeInventory();
                player.sendMessage(ChatColor.YELLOW + "Type the global stock limit (-1 for unlimited) in chat, or type 'cancel'.");
            } else if (slot == 16) { // Perm
                permEditMode.put(player.getUniqueId(), id);
                player.closeInventory();
                player.sendMessage(ChatColor.YELLOW + "Type the required permission (or 'none') in chat, or type 'cancel'.");
            } else if (slot == 22) { // Delete
                FileConfiguration itemsConfig = plugin.getConfigManager().getItems();
                itemsConfig.set("items." + id, null);
                plugin.getConfigManager().saveItems();
                plugin.getConfigManager().reload();
                player.sendMessage(ChatColor.GREEN + "Item deleted!");
                openItemsManager(player, 0);
            } else if (slot == 26) { // Back
                openItemsManager(player, viewerPages.getOrDefault(player.getUniqueId(), 0));
            }
        }
    }

    private void registerNewItem(Player player, ItemStack item) {
        FileConfiguration itemsConfig = plugin.getConfigManager().getItems();
        ConfigurationSection itemsSec = itemsConfig.getConfigurationSection("items");
        if (itemsSec == null) {
            itemsSec = itemsConfig.createSection("items");
        }
        
        int newIdNum = 1;
        while (itemsSec.contains("item" + newIdNum)) {
            newIdNum++;
        }
        String id = "item" + newIdNum;

        ItemMeta meta = item.getItemMeta();
        String name = item.getType().name();
        List<String> lore = new ArrayList<>();
        if (meta != null) {
            if (meta.hasDisplayName()) {
                name = meta.getDisplayName().replace(ChatColor.COLOR_CHAR, '&');
            }
            if (meta.hasLore()) {
                for (String l : meta.getLore()) {
                    lore.add(l.replace(ChatColor.COLOR_CHAR, '&'));
                }
            }
        }

        // Find next available slot
        int nextSlot = 0;
        List<Integer> takenSlots = new ArrayList<>();
        for (String k : itemsSec.getKeys(false)) {
            takenSlots.add(itemsSec.getInt(k + ".slot"));
        }
        while (takenSlots.contains(nextSlot)) {
            nextSlot++;
        }

        ConfigurationSection sec = itemsSec.createSection(id);
        sec.set("slot", nextSlot);
        sec.set("material", item.getType().name());
        sec.set("name", name);
        sec.set("lore", lore);
        sec.set("price", 1000.0);
        sec.set("stock", -1);
        sec.set("permission", "");

        plugin.getConfigManager().saveItems();
        plugin.getConfigManager().reload();

        player.sendMessage(ChatColor.GREEN + "Item added successfully! Opening settings...");
        openItemSettings(player, id);
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        String msg = event.getMessage();

        if (durationEditMode.contains(player.getUniqueId())) {
            event.setCancelled(true);
            durationEditMode.remove(player.getUniqueId());
            if (msg.equalsIgnoreCase("cancel")) {
                player.sendMessage(ChatColor.RED + "Cancelled.");
                Bukkit.getScheduler().runTask(plugin, () -> openControlPanel(player));
                return;
            }
            if (!msg.endsWith("m") && !msg.endsWith("h") && !msg.endsWith("s")) {
                player.sendMessage(ChatColor.RED + "Invalid format! Use 's', 'm', or 'h'.");
                Bukkit.getScheduler().runTask(plugin, () -> openControlPanel(player));
                return;
            }
            player.sendMessage(ChatColor.GREEN + "Open Duration updated to " + msg);
            Bukkit.getScheduler().runTask(plugin, () -> {
                plugin.getConfigManager().getConfig().set("settings.open-duration", msg);
                plugin.getConfigManager().saveConfig();
                plugin.getShopManager().reloadTiming();
                openControlPanel(player);
            });
            return;
        }

        if (randomPickEditMode.contains(player.getUniqueId())) {
            event.setCancelled(true);
            randomPickEditMode.remove(player.getUniqueId());
            if (msg.equalsIgnoreCase("cancel")) {
                player.sendMessage(ChatColor.RED + "Cancelled.");
                Bukkit.getScheduler().runTask(plugin, () -> openControlPanel(player));
                return;
            }
            try {
                int amount = Integer.parseInt(msg);
                player.sendMessage(ChatColor.GREEN + "Random Pick Amount updated to " + amount);
                Bukkit.getScheduler().runTask(plugin, () -> {
                    plugin.getConfigManager().getConfig().set("settings.random-pick-amount", amount);
                    plugin.getConfigManager().saveConfig();
                    openControlPanel(player);
                });
            } catch (NumberFormatException e) {
                player.sendMessage(ChatColor.RED + "Invalid number format! Use an integer like 5.");
                Bukkit.getScheduler().runTask(plugin, () -> openControlPanel(player));
            }
            return;
        }

        if (handleChatEdit(player, msg, priceEditMode, "price")) {
            event.setCancelled(true);
            return;
        }
        if (handleChatEdit(player, msg, slotEditMode, "slot")) {
            event.setCancelled(true);
            return;
        }
        if (handleChatEdit(player, msg, stockEditMode, "stock")) {
            event.setCancelled(true);
            return;
        }
        if (handleChatEditList(player, msg, permEditMode, "permission")) {
            event.setCancelled(true);
            return;
        }
    }

    private boolean handleChatEdit(Player player, String msg, Map<UUID, String> modeMap, String configKey) {
        if (!modeMap.containsKey(player.getUniqueId())) return false;
        String id = modeMap.remove(player.getUniqueId());
        
        if (msg.equalsIgnoreCase("cancel")) {
            player.sendMessage(ChatColor.RED + "Edit cancelled.");
            Bukkit.getScheduler().runTask(plugin, () -> openItemSettings(player, id));
            return true;
        }

        try {
            double val = Double.parseDouble(msg);
            Bukkit.getScheduler().runTask(plugin, () -> {
                FileConfiguration itemsConfig = plugin.getConfigManager().getItems();
                if (configKey.equals("slot")) {
                    int slotVal = (int) val;
                    if (slotVal < 1 || slotVal > 54) {
                        player.sendMessage(ChatColor.RED + "Slot must be between 1 and 54!");
                        openItemSettings(player, id);
                        return;
                    }
                    itemsConfig.set("items." + id + "." + configKey, slotVal - 1);
                } else if (configKey.equals("stock")) {
                    itemsConfig.set("items." + id + "." + configKey, (int) val);
                } else {
                    itemsConfig.set("items." + id + "." + configKey, val);
                }
                plugin.getConfigManager().saveItems();
                plugin.getConfigManager().reload();
                player.sendMessage(ChatColor.GREEN + configKey.substring(0, 1).toUpperCase() + configKey.substring(1) + " updated!");
                openItemSettings(player, id);
            });
        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + "Invalid number. Cancelled.");
            Bukkit.getScheduler().runTask(plugin, () -> openItemSettings(player, id));
        }
        return true;
    }

    private boolean handleChatEditList(Player player, String msg, Map<UUID, String> modeMap, String configKey) {
        if (!modeMap.containsKey(player.getUniqueId())) return false;
        String id = modeMap.remove(player.getUniqueId());
        
        if (msg.equalsIgnoreCase("cancel")) {
            player.sendMessage(ChatColor.RED + "Edit cancelled.");
            Bukkit.getScheduler().runTask(plugin, () -> openItemSettings(player, id));
            return true;
        }

        Bukkit.getScheduler().runTask(plugin, () -> {
            FileConfiguration itemsConfig = plugin.getConfigManager().getItems();
            if (msg.equalsIgnoreCase("none")) {
                itemsConfig.set("items." + id + "." + configKey, "");
            } else {
                itemsConfig.set("items." + id + "." + configKey, msg);
            }
            plugin.getConfigManager().saveItems();
            plugin.getConfigManager().reload();
            player.sendMessage(ChatColor.GREEN + "Permission updated!");
            openItemSettings(player, id);
        });
        return true;
    }

    public void unregister() {
        for (UUID uuid : viewerPages.keySet()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) p.closeInventory();
        }
    }
}
