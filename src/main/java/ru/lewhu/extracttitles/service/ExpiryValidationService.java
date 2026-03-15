package ru.lewhu.extracttitles.service;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import ru.lewhu.extracttitles.service.effect.EffectService;

import java.util.List;
import java.util.Map;

public final class ExpiryValidationService {
    private final JavaPlugin plugin;
    private final PlayerTitleService playerTitleService;
    private final EffectService effectService;
    private final MessageService messageService;

    public ExpiryValidationService(JavaPlugin plugin, PlayerTitleService playerTitleService, EffectService effectService, MessageService messageService) {
        this.plugin = plugin;
        this.playerTitleService = playerTitleService;
        this.effectService = effectService;
        this.messageService = messageService;
    }

    public void start() {
        long period = 20L * 60L;
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                validate(player, true);
            }
        }, period, period);
    }

    public void validate(Player player, boolean notify) {
        List<String> expired = playerTitleService.validateExpiry(player.getUniqueId());
        if (expired.isEmpty()) return;

        String active = playerTitleService.getActive(player.getUniqueId());
        if (active == null || expired.stream().anyMatch(e -> e.equalsIgnoreCase(active))) {
            effectService.clear(player);
            playerTitleService.clearActive(player.getUniqueId());
        }
        playerTitleService.save(player.getUniqueId());

        if (notify) {
            messageService.send(player, "expired", Map.of("count", String.valueOf(expired.size())));
        }
    }
}
