package ru.lewhu.extracttitles.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.CommandSender;

public final class Text {
    private static final MiniMessage MINI = MiniMessage.miniMessage();
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();

    private Text() {
    }

    public static Component parse(String input) {
        if (input == null) return Component.empty();
        try {
            return MINI.deserialize(input);
        } catch (Exception ignored) {
            return LEGACY.deserialize(input);
        }
    }

    public static String plainFormatted(String input) {
        return LEGACY.serialize(parse(input));
    }

    public static void send(CommandSender sender, String message) {
        sender.sendMessage(parse(message));
    }
}
