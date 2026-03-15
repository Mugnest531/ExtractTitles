package ru.lewhu.extracttitles.service;

import org.bukkit.entity.Player;
import ru.lewhu.extracttitles.domain.title.TitleDefinition;
import ru.lewhu.extracttitles.service.effect.EffectService;

import java.util.Optional;

public final class TitleActivationService {
    private final TitleService titleService;
    private final PlayerTitleService playerTitleService;
    private final EffectService effectService;

    public TitleActivationService(TitleService titleService, PlayerTitleService playerTitleService, EffectService effectService) {
        this.titleService = titleService;
        this.playerTitleService = playerTitleService;
        this.effectService = effectService;
    }

    public boolean activate(Player player, String titleId) {
        Optional<TitleDefinition> opt = titleService.find(titleId);
        if (opt.isEmpty()) return false;
        TitleDefinition title = opt.get();
        if (!title.enabled()) return false;
        if (!playerTitleService.hasTitle(player.getUniqueId(), titleId)) return false;

        effectService.clear(player);
        playerTitleService.setActive(player.getUniqueId(), titleId);
        effectService.applyTitle(player, title);
        playerTitleService.save(player.getUniqueId());
        return true;
    }

    public void deactivate(Player player) {
        effectService.clear(player);
        playerTitleService.clearActive(player.getUniqueId());
        playerTitleService.save(player.getUniqueId());
    }
}
