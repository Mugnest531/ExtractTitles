package ru.lewhu.extracttitles.service.effect.impl;

import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import ru.lewhu.extracttitles.domain.title.TitleDefinition;
import ru.lewhu.extracttitles.service.effect.EffectContext;
import ru.lewhu.extracttitles.service.effect.TitleEffect;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class MaxHealthEffect implements TitleEffect {
    private final Map<UUID, Double> baseHealth = new HashMap<>();

    @Override
    public String type() {
        return "max_health";
    }

    @Override
    public void onActivate(EffectContext context, Player player, TitleDefinition title, Map<String, Object> options) {
        var attr = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (attr == null) return;
        double amount = toDouble(options.getOrDefault("amount", 0));
        if (amount <= 0) return;
        baseHealth.putIfAbsent(player.getUniqueId(), attr.getBaseValue());
        attr.setBaseValue(baseHealth.get(player.getUniqueId()) + amount);
    }

    @Override
    public void onDeactivate(EffectContext context, Player player, TitleDefinition title, Map<String, Object> options) {
        var attr = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (attr == null) return;
        Double base = baseHealth.remove(player.getUniqueId());
        if (base != null) {
            attr.setBaseValue(base);
            if (player.getHealth() > base) {
                player.setHealth(base);
            }
        }
    }

    private double toDouble(Object raw) {
        if (raw instanceof Number number) return number.doubleValue();
        try {
            return Double.parseDouble(String.valueOf(raw));
        } catch (Exception ignored) {
            return 0;
        }
    }
}

