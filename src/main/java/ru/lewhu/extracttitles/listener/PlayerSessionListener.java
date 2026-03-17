package ru.lewhu.extracttitles.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import ru.lewhu.extracttitles.service.MessageService;
import ru.lewhu.extracttitles.service.PlayerTitleService;
import ru.lewhu.extracttitles.service.effect.EffectService;

import java.util.Map;

public final class PlayerSessionListener implements Listener {
    private final PlayerTitleService playerTitleService;
    private final EffectService effectService;
    private final MessageService messageService;

    public PlayerSessionListener(PlayerTitleService playerTitleService, EffectService effectService, MessageService messageService) {
        this.playerTitleService = playerTitleService;
        this.effectService = effectService;
        this.messageService = messageService;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        playerTitleService.load(player.getUniqueId()).thenAccept(profile -> {
            playerTitleService.runSync(() -> {
                var expired = playerTitleService.validateExpiry(player.getUniqueId());
                if (!expired.isEmpty()) {
                    messageService.send(player, "expired", Map.of("count", String.valueOf(expired.size())));
                }
                String active = profile.activeTitleId();
                if (active != null && playerTitleService.hasTitle(player.getUniqueId(), active)) {
                    effectService.reapplyFor(player, active);
                }
                playerTitleService.save(player.getUniqueId());
            });
        });
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        effectService.clear(player);
        playerTitleService.unload(player.getUniqueId());
    }
}
