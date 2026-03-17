package ru.lewhu.extracttitles.service.effect;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import ru.lewhu.extracttitles.domain.title.TitleDefinition;
import ru.lewhu.extracttitles.domain.title.TitleEffectDefinition;
import ru.lewhu.extracttitles.service.PermissionGrantService;
import ru.lewhu.extracttitles.service.TitleService;
import ru.lewhu.extracttitles.service.effect.impl.CommandTitleEffect;
import ru.lewhu.extracttitles.service.effect.impl.MaxHealthEffect;
import ru.lewhu.extracttitles.service.effect.impl.Mining3x3Effect;
import ru.lewhu.extracttitles.service.effect.impl.PermissionTitleEffect;
import ru.lewhu.extracttitles.service.effect.impl.PotionTitleEffect;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class EffectService {
    private final JavaPlugin plugin;
    private final TitleService titleService;
    private final PermissionGrantService permissionGrantService;
    private final Map<String, TitleEffect> handlers = new HashMap<>();
    private final Map<UUID, List<AppliedEffect>> activeEffects = new ConcurrentHashMap<>();
    private final Mining3x3Effect mining3x3Effect;

    public EffectService(JavaPlugin plugin, TitleService titleService, PermissionGrantService permissionGrantService) {
        this.plugin = plugin;
        this.titleService = titleService;
        this.permissionGrantService = permissionGrantService;
        register(new MaxHealthEffect());
        register(new PotionTitleEffect());
        register(new PermissionTitleEffect());
        register(new CommandTitleEffect());
        this.mining3x3Effect = new Mining3x3Effect();
        register(mining3x3Effect);
    }

    private void register(TitleEffect effect) {
        handlers.put(effect.type().toLowerCase(Locale.ROOT), effect);
    }

    public void startTicker() {
        long period = 20L * 20L;
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                tick(player);
            }
        }, period, period);
    }

    public void applyTitle(Player player, TitleDefinition title) {
        applyTitle(player, title, false);
    }

    public void applyTitle(Player player, TitleDefinition title, boolean reapply) {
        clear(player);
        EffectContext context = new EffectContext(plugin, permissionGrantService, reapply);

        List<AppliedEffect> applied = new ArrayList<>();
        for (TitleEffectDefinition effectDef : title.effects()) {
            TitleEffect handler = handlers.get(effectDef.type().toLowerCase(Locale.ROOT));
            if (handler == null) {
                continue;
            }
            handler.onActivate(context, player, title, effectDef.options());
            applied.add(new AppliedEffect(title.id(), title, handler, effectDef.options()));
        }
        activeEffects.put(player.getUniqueId(), applied);
    }

    public void clear(Player player) {
        List<AppliedEffect> effects = activeEffects.remove(player.getUniqueId());
        if (effects == null) {
            return;
        }
        EffectContext context = new EffectContext(plugin, permissionGrantService, false);
        for (AppliedEffect effect : effects) {
            effect.effect().onDeactivate(context, player, effect.title(), effect.options());
        }
    }

    public void tick(Player player) {
        List<AppliedEffect> effects = activeEffects.get(player.getUniqueId());
        if (effects == null) {
            return;
        }
        EffectContext context = new EffectContext(plugin, permissionGrantService, false);
        for (AppliedEffect effect : effects) {
            effect.effect().onTick(context, player, effect.title(), effect.options());
        }
    }

    public void reapplyFor(Player player, String titleId) {
        titleService.find(titleId).ifPresent(title -> applyTitle(player, title, true));
    }

    public Mining3x3Effect mining3x3Effect() {
        return mining3x3Effect;
    }
}