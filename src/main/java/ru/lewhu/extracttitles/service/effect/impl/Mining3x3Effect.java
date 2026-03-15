package ru.lewhu.extracttitles.service.effect.impl;

import org.bukkit.entity.Player;
import ru.lewhu.extracttitles.domain.title.TitleDefinition;
import ru.lewhu.extracttitles.service.effect.EffectContext;
import ru.lewhu.extracttitles.service.effect.TitleEffect;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class Mining3x3Effect implements TitleEffect {
    private final Map<UUID, Mining3x3Profile> profiles = new ConcurrentHashMap<>();

    @Override
    public String type() {
        return "mining_3x3";
    }

    @Override
    public void onActivate(EffectContext context, Player player, TitleDefinition title, Map<String, Object> options) {
        profiles.put(player.getUniqueId(), new Mining3x3Profile(
                Mining3x3Profile.parseMaterials(options.get("tools")),
                Mining3x3Profile.parseWorlds(options.get("worlds")),
                Mining3x3Profile.parseMaterials(options.get("disabled-blocks"))
        ));
    }

    @Override
    public void onDeactivate(EffectContext context, Player player, TitleDefinition title, Map<String, Object> options) {
        profiles.remove(player.getUniqueId());
    }

    public Mining3x3Profile profile(Player player) {
        return profiles.get(player.getUniqueId());
    }
}
