package com.kakashiplayz.blackmarket;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class CommandManager implements CommandExecutor, TabCompleter {

    private final BlackMarket plugin;

    public CommandManager(BlackMarket plugin) {
        this.plugin = plugin;
        plugin.getCommand("blackmarket").setExecutor(this);
        plugin.getCommand("blackmarket").setTabCompleter(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        // No args — open the shop
        if (args.length == 0) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "Only players can open the shop. Use /blackmarket help for commands.");
                return true;
            }
            Player p = (Player) sender;
            if (!p.hasPermission("blackmarket.use")) {
                p.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getConfigManager().getMessages().getString("no-permission")));
                return true;
            }
            plugin.getGuiManager().openShop(p);
            return true;
        }

        // /blackmarket help
        if (args[0].equalsIgnoreCase("help")) {
            sendHelpMenu(sender);
            return true;
        }

        // /blackmarket editor
        if (args[0].equalsIgnoreCase("editor")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "Only players can use the editor.");
                return true;
            }
            Player p = (Player) sender;
            if (!p.hasPermission("blackmarket.editor")) {
                p.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getConfigManager().getMessages().getString("no-permission")));
                return true;
            }
            plugin.getEditorManager().openControlPanel(p);
            return true;
        }

        // /blackmarket reload
        if (args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("blackmarket.reload")) {
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getConfigManager().getMessages().getString("no-permission")));
                return true;
            }
            plugin.getConfigManager().reload();
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    "&8[&4BlackMarket&8] &aConfiguration reloaded successfully!"));
            return true;
        }

        // Unknown sub-command — show help
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                "&8[&4BlackMarket&8] &cUnknown sub-command. Use &f/blackmarket help &cfor a list of commands."));
        return true;
    }

    private void sendHelpMenu(CommandSender sender) {
        String line   = ChatColor.translateAlternateColorCodes('&', "&8&m----------------------------------------");
        String header = ChatColor.translateAlternateColorCodes('&', "        &4&lBlack&c&lMarket &8» &7Help Menu");
        String footer = ChatColor.translateAlternateColorCodes('&', "  &8Developed by &cKakashiPlayz");

        sender.sendMessage(line);
        sender.sendMessage(header);
        sender.sendMessage(line);
        sender.sendMessage("");

        // General commands
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', " &c&l▶ &fGeneral Commands"));
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                "  &4/blackmarket       &8» &7Opens the Black Market shop GUI."));
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                "                     &8(Only available when the market is open!)"));
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                "  &4/blackmarket help  &8» &7Shows this help menu."));

        // Admin commands (only show to players with appropriate perms)
        boolean hasEditor = sender.hasPermission("blackmarket.editor") || sender.isOp();
        boolean hasReload = sender.hasPermission("blackmarket.reload") || sender.isOp();

        if (hasEditor || hasReload) {
            sender.sendMessage("");
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', " &4&l▶ &fAdmin Commands"));

            if (hasEditor) {
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                        "  &4/blackmarket editor  &8» &7Opens the Admin Control Panel GUI."));
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                        "                        &8Manage items, prices, slots & stock."));
            }
            if (hasReload) {
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                        "  &4/blackmarket reload  &8» &7Reloads all config files instantly."));
            }
        }

        sender.sendMessage("");

        // Permissions info
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', " &c&l▶ &fPermissions"));
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                "  &4blackmarket.use     &8» &7Open the shop (default: all players)"));
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                "  &4blackmarket.editor  &8» &7Access the admin GUI editor (default: op)"));
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                "  &4blackmarket.reload  &8» &7Reload plugin configuration (default: op)"));

        sender.sendMessage("");

        // How it works
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', " &c&l▶ &fHow It Works"));
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                "  &7The Black Market opens at &crandom intervals &7(1–2 hours apart)"));
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                "  &7or stays permanently open if &calways-open &7is enabled."));
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                "  &7When the market opens, the whole server is notified via"));
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                "  &7broadcast, title, sound & Discord webhook."));
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                "  &7Each player has a purchase limit per session (set in config)."));

        sender.sendMessage("");
        sender.sendMessage(footer);
        sender.sendMessage(line);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        // Suppress namespaced commands (e.g. blackmarket:blackmarket)
        if (alias.contains(":")) {
            return new ArrayList<>();
        }

        if (args.length == 1) {
            List<String> completions = new ArrayList<>(Arrays.asList("help"));

            if (sender instanceof Player) {
                Player p = (Player) sender;
                if (p.hasPermission("blackmarket.use")) {
                    // base command — no extra sub-command needed for opening
                }
            }
            if (sender.hasPermission("blackmarket.editor")) {
                completions.add("editor");
            }
            if (sender.hasPermission("blackmarket.reload")) {
                completions.add("reload");
            }

            return completions.stream()
                    .filter(s -> s.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        return new ArrayList<>();
    }
}
