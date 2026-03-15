package ru.lewhu.extracttitles.service.effect;

import org.bukkit.plugin.java.JavaPlugin;
import ru.lewhu.extracttitles.service.PermissionGrantService;

public record EffectContext(JavaPlugin plugin, PermissionGrantService permissionGrantService, boolean reapply) {
}