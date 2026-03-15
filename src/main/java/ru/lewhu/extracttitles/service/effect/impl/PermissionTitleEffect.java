package ru.lewhu.extracttitles.service.effect.impl;

import org.bukkit.entity.Player;
import ru.lewhu.extracttitles.domain.title.TitleDefinition;
import ru.lewhu.extracttitles.service.effect.EffectContext;
import ru.lewhu.extracttitles.service.effect.TitleEffect;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class PermissionTitleEffect implements TitleEffect {
    @Override
    public String type() {
        return "permission";
    }

    @Override
    public void onActivate(EffectContext context, Player player, TitleDefinition title, Map<String, Object> options) {
        context.permissionGrantService().grant(player, readNodes(options.get("nodes")));
    }

    @Override
    public void onDeactivate(EffectContext context, Player player, TitleDefinition title, Map<String, Object> options) {
        context.permissionGrantService().revoke(player, readNodes(options.get("nodes")));
    }

    private List<String> readNodes(Object raw) {
        List<String> out = new ArrayList<>();
        if (raw instanceof Iterable<?> iterable) {
            for (Object value : iterable) {
                String node = String.valueOf(value).trim();
                if (!node.isBlank()) out.add(node);
            }
        }
        return out;
    }
}
