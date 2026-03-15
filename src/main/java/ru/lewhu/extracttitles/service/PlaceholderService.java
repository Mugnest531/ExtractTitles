package ru.lewhu.extracttitles.service;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;

public final class PlaceholderService {
    private final JavaPlugin plugin;

    public PlaceholderService(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public String apply(CommandSender sender, String input) {
        if (sender instanceof Player player) {
            return apply(player, input);
        }
        return input == null ? "" : input;
    }

    public String apply(CommandSender sender, String input, Map<String, String> local) {
        return apply(sender, applyLocal(input, local));
    }

    public String apply(OfflinePlayer player, String input) {
        if (input == null) return "";
        if (player == null || !isPapiEnabled()) return input;
        return PlaceholderAPI.setPlaceholders(player, input);
    }

    public String apply(Player player, String input) {
        if (input == null) return "";
        if (player == null || !isPapiEnabled()) return input;
        return PlaceholderAPI.setPlaceholders(player, input);
    }

    public String apply(Player player, String input, Map<String, String> local) {
        return apply(player, applyLocal(input, local));
    }

    private String applyLocal(String input, Map<String, String> local) {
        if (input == null) return "";
        if (local == null || local.isEmpty()) return input;

        String result = input;
        for (var entry : local.entrySet()) {
            result = result.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return result;
    }

    private boolean isPapiEnabled() {
        return Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")
                && plugin.getServer().getPluginManager().getPlugin("PlaceholderAPI") != null;
    }
}
