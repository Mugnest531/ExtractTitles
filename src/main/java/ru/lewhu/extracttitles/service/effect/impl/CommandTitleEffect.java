package ru.lewhu.extracttitles.service.effect.impl;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import ru.lewhu.extracttitles.domain.title.TitleDefinition;
import ru.lewhu.extracttitles.service.effect.EffectContext;
import ru.lewhu.extracttitles.service.effect.TitleEffect;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class CommandTitleEffect implements TitleEffect {
    @Override
    public String type() {
        return "command";
    }

    @Override
    public void onActivate(EffectContext context, Player player, TitleDefinition title, Map<String, Object> options) {
        if (context.reapply()) {
            return;
        }
        execute(player, readList(options.get("on-activate")));
    }

    @Override
    public void onDeactivate(EffectContext context, Player player, TitleDefinition title, Map<String, Object> options) {
        execute(player, readList(options.get("on-deactivate")));
    }

    private void execute(Player player, List<String> commands) {
        boolean papi = Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null;
        for (String raw : commands) {
            String command = raw.replace("{player}", player.getName())
                    .replace("{uuid}", player.getUniqueId().toString());
            if (papi) {
                command = PlaceholderAPI.setPlaceholders(player, command);
            }
            if (command.startsWith("player:")) {
                player.performCommand(command.substring("player:".length()).trim());
            } else if (command.startsWith("console:")) {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command.substring("console:".length()).trim());
            } else {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command.trim());
            }
        }
    }

    private List<String> readList(Object raw) {
        List<String> out = new ArrayList<>();
        if (raw instanceof Iterable<?> iterable) {
            for (Object val : iterable) {
                out.add(String.valueOf(val));
            }
        }
        return out;
    }
}