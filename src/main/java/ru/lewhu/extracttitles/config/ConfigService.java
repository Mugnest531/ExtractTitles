package ru.lewhu.extracttitles.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public final class ConfigService {
    private final ConfigFile config;
    private final ConfigFile titles;
    private final ConfigFile menus;
    private final ConfigFile messages;
    private final ConfigFile effects;
    private final ConfigFile storage;

    public ConfigService(JavaPlugin plugin) {
        this.config = new ConfigFile(plugin, "config.yml");
        this.titles = new ConfigFile(plugin, "titles.yml");
        this.menus = new ConfigFile(plugin, "menus.yml");
        this.messages = new ConfigFile(plugin, "messages.yml");
        this.effects = new ConfigFile(plugin, "effects.yml");
        this.storage = new ConfigFile(plugin, "storage.yml");
    }

    public void loadAll() {
        config.load();
        titles.load();
        menus.load();
        messages.load();
        effects.load();
        storage.load();
    }

    public void reloadAll() {
        config.reload();
        titles.reload();
        menus.reload();
        messages.reload();
        effects.reload();
        storage.reload();
    }

    public FileConfiguration config() {
        return config.config();
    }

    public FileConfiguration titles() {
        return titles.config();
    }

    public FileConfiguration menus() {
        return menus.config();
    }

    public FileConfiguration messages() {
        return messages.config();
    }

    public FileConfiguration effects() {
        return effects.config();
    }

    public FileConfiguration storage() {
        return storage.config();
    }

    public ConfigFile titlesFile() {
        return titles;
    }

    public String message(String key) {
        return messages.config().getString("messages." + key, "<red>Missing message key: " + key + "</red>");
    }

    public List<String> messageList(String key) {
        return messages.config().getStringList("messages." + key);
    }
}
