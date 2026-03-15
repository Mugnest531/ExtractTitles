package ru.lewhu.extracttitles.service;

import org.bukkit.command.CommandSender;
import ru.lewhu.extracttitles.config.ConfigService;
import ru.lewhu.extracttitles.util.Text;

import java.util.List;
import java.util.Map;

public final class MessageService {
    private final ConfigService configService;
    private final PlaceholderService placeholderService;

    public MessageService(ConfigService configService, PlaceholderService placeholderService) {
        this.configService = configService;
        this.placeholderService = placeholderService;
    }

    public void send(CommandSender sender, String key) {
        String rendered = placeholderService.apply(sender, configService.message(key));
        Text.send(sender, rendered);
    }

    public void send(CommandSender sender, String key, Map<String, String> placeholders) {
        String message = configService.message(key);
        String rendered = placeholderService.apply(sender, message, placeholders);
        Text.send(sender, rendered);
    }

    public List<String> list(String key) {
        return configService.messageList(key);
    }

    public String raw(String key) {
        return configService.message(key);
    }

    public String rawOrDefault(String key, String defaultValue) {
        String value = configService.message(key);
        if (value == null || value.startsWith("<red>Missing message key:")) {
            return defaultValue;
        }
        return value;
    }
}