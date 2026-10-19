# BlackMarket Plugin

## Overview
BlackMarket is an exclusive, highly customizable GUI-based black market shop plugin for your Minecraft server. It brings a unique dynamic economy experience by offering randomly opening shops or fixed-duration black markets. With built-in purchase tracking and an integrated Admin GUI Editor, managing rare or illegal item sales has never been easier or more immersive!

## Features
- **Dynamic Shop Availability**: Choose between an `always-open` shop, or a thrilling `random-open` system that alerts players when the market appears!
- **Random Pick Mode**: Want a rotating shop? Enable `random-pick` and define how many items to showcase! The plugin will organically select items and display them neatly.
- **Stock Market Mode**: Items are restricted by a global stock limit. If `stock-market` is enabled, an item's price will dynamically double when it completely sells out!
- **Strict Purchase Limits**: Prevent economy abuse by setting a `player-purchase-limit` per opening phase, tracked reliably via SQLite.
- **In-Game GUI Editor**: Admins can easily add, edit, or adjust items (prices, GUI slots, and stocks) directly in-game without touching config files.
- **Advanced GUI Effects**: Smooth custom animations (like `slide` in the config) and stylized UI menus.
- **Immersive Alerts System**: When the market opens, alert your entire server using Titles, Broadcasts, Fireworks, and Sounds.
- **Discord Integration via Webhooks**: Automatically notify your Discord community precisely when the in-game black market opens, complete with custom Bot names and avatars!
- **Robust Economy Integration**: Soft-depends on Vault to securely handle all transactions. Also supports PlaceholderAPI and Citizens!

## Commands
- `/blackmarket` (Aliases: `/nm`) - Opens the BlackMarket GUI (if it is currently open).
- `/blackmarket editor` - Opens the Admin Control Panel GUI to build and edit your shop inventory.
- `/blackmarket reload` - Reloads all configuration files instantly!

## Permissions
- `blackmarket.use` - Allows players to open the shop menu. (Default: true)
- `blackmarket.editor` - Allows opening the admin GUI editor. (Default: op)
- `blackmarket.reload` - Allows reloading the plugin manually. (Default: op)

## Configuration
The plugin generates a pristine `config.yml` on first run:

### `config.yml` Options:
- **Shop Settings**:
  - `always-open` / `random-open` switches the style of your black market.
  - `random-pick`: Set to true to display a rotating shop of random items! Control the quantity with `random-pick-amount`.
  - `stock-market`: Set to true to automatically double an item's price when its stock reaches zero.
  - `open-duration`: Timer indicating how long players have to buy items when it opens (e.g. `30m`).
  - `player-purchase-limit`: Limits the total slots of items a player can buy in one session!
- **GUI & Effects**:
  - `gui-effects` handle animations like sliding items.
  - Toggles for `firework`, `sound`, `title`, and `broadcast` precisely control how the server reacts when the shop goes live.
- **Discord Alerts**:
  - Enter your `webhook-url`, `bot-name`, and `channel-id` to automatically sync notifications to your Discord Server.

## Author Information
- **Developer**: KakashiPlayz!
- **Created By**: KakashiPlayz!
- **Portfolio Website**: https://kakashiplayz.vercel.app
- **Discord Server**: https://discord.gg/QdTN4BFkTv

## Plugin Version
1.0.1

## Compatibility
Works natively with the following Minecraft server versions:
- **1.13.x, 1.14.x, 1.15.x, 1.16.x**
- **1.17.x, 1.18.x, 1.19.x, 1.20.x**
- **1.21.x**
- **26.1, 26.1.1** *(new Minecraft versioning scheme)*

Compatible with Spigot, Paper, Purpur, and Bukkit-based servers.
Requires Vault & a valid Economy plugin to process purchases!

## Installation
1. Ensure you have installed Vault and an Economy Plugin (e.g. EssentialsX)
2. Place the `BlackMarket.jar` file in your server's `plugins` folder
3. Restart your server
4. The plugin will generate configuration files in the `plugins/BlackMarket` folder
5. Configure your settings, MySQL/SQLite database in the files, and you're good to go!

## Support
For issues or questions, join our Discord server: https://discord.gg/QdTN4BFkTv
