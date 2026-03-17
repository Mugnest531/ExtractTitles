package ru.lewhu.extracttitles.service.effect.impl;

import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import ru.lewhu.extracttitles.domain.title.TitleDefinition;
import ru.lewhu.extracttitles.service.effect.EffectContext;
import ru.lewhu.extracttitles.service.effect.TitleEffect;

import java.util.Locale;
import java.util.Map;

public final class PotionTitleEffect implements TitleEffect {
    @Override
    public String type() {
        return "potion";
    }

    @Override
    public void onActivate(EffectContext context, Player player, TitleDefinition title, Map<String, Object> options) {
        PotionEffectType effectType = parseEffect(options.get("effect"));
        if (effectType == null) return;
        int amplifier = Math.max(0, toInt(options.getOrDefault("amplifier", 0)));
        boolean ambient = toBoolean(options.getOrDefault("ambient", false));
        boolean particles = toBoolean(options.getOrDefault("particles", true));
        boolean icon = toBoolean(options.getOrDefault("icon", true));
        player.addPotionEffect(new PotionEffect(effectType, 20 * 45, amplifier, ambient, particles, icon), true);
    }

    @Override
    public void onDeactivate(EffectContext context, Player player, TitleDefinition title, Map<String, Object> options) {
        PotionEffectType effectType = parseEffect(options.get("effect"));
        if (effectType != null) {
            player.removePotionEffect(effectType);
        }
    }

    @Override
    public void onTick(EffectContext context, Player player, TitleDefinition title, Map<String, Object> options) {
        onActivate(context, player, title, options);
    }

    private PotionEffectType parseEffect(Object raw) {
        if (raw == null) return null;
        return PotionEffectType.getByName(String.valueOf(raw).toUpperCase(Locale.ROOT));
    }

    private int toInt(Object raw) {
        if (raw instanceof Number number) return number.intValue();
        try {
            return Integer.parseInt(String.valueOf(raw));
        } catch (Exception ignored) {
            return 0;
        }
    }

    private boolean toBoolean(Object raw) {
        if (raw instanceof Boolean bool) return bool;
        return Boolean.parseBoolean(String.valueOf(raw));
    }
}
