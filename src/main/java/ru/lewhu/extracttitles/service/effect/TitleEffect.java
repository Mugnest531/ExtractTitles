package ru.lewhu.extracttitles.service.effect;

import org.bukkit.entity.Player;
import ru.lewhu.extracttitles.domain.title.TitleDefinition;

import java.util.Map;

public interface TitleEffect {
    String type();

    void onActivate(EffectContext context, Player player, TitleDefinition title, Map<String, Object> options);

    void onDeactivate(EffectContext context, Player player, TitleDefinition title, Map<String, Object> options);

    default void onTick(EffectContext context, Player player, TitleDefinition title, Map<String, Object> options) {
    }
}
