package com.kakashiplayz.blackmarket;

import org.bukkit.Bukkit;

import javax.net.ssl.HttpsURLConnection;
import java.io.OutputStream;
import java.net.URI;
import java.net.URL;

public class DiscordWebhook {

    private final BlackMarket plugin;

    public DiscordWebhook(BlackMarket plugin) {
        this.plugin = plugin;
    }

    public void sendMessage(String content) {
        if (!plugin.getConfigManager().getConfig().getBoolean("discord.enabled")) {
            return;
        }

        String webhookUrl = plugin.getConfigManager().getConfig().getString("discord.webhook-url");
        if (webhookUrl == null || webhookUrl.isEmpty() || webhookUrl.equalsIgnoreCase("YOUR_WEBHOOK_URL_HERE")) {
            return;
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                URL url = URI.create(webhookUrl).toURL();
                HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
                connection.addRequestProperty("Content-Type", "application/json");
                connection.addRequestProperty("User-Agent", "Java-DiscordWebhook");
                connection.setDoOutput(true);
                connection.setRequestMethod("POST");

                String botName = plugin.getConfigManager().getConfig().getString("discord.bot-name", "BlackMarket");
                String botAvatar = plugin.getConfigManager().getConfig().getString("discord.bot-avatar-url", "");

                StringBuilder jsonBuilder = new StringBuilder();
                jsonBuilder.append("{");
                jsonBuilder.append("\"content\": \"").append(content.replace("\"", "\\\"")).append("\"");
                if (botName != null && !botName.isEmpty()) {
                    jsonBuilder.append(", \"username\": \"").append(botName.replace("\"", "\\\"")).append("\"");
                }
                if (botAvatar != null && !botAvatar.isEmpty()) {
                    jsonBuilder.append(", \"avatar_url\": \"").append(botAvatar.replace("\"", "\\\"")).append("\"");
                }
                jsonBuilder.append("}");

                String json = jsonBuilder.toString();

                try (OutputStream stream = connection.getOutputStream()) {
                    stream.write(json.getBytes());
                    stream.flush();
                }

                connection.getInputStream().close();
                connection.disconnect();

            } catch (Exception e) {
                plugin.getLogger().warning("Failed to send Discord webhook: " + e.getMessage());
            }
        });
    }
}
