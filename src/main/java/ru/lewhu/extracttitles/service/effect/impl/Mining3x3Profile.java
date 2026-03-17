package ru.lewhu.extracttitles.service.effect.impl;

import org.bukkit.Material;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public final class Mining3x3Profile {
    private final Set<Material> allowedTools;
    private final Set<String> allowedWorlds;
    private final Set<Material> disabledBlocks;

    public Mining3x3Profile(Set<Material> allowedTools, Set<String> allowedWorlds, Set<Material> disabledBlocks) {
        this.allowedTools = allowedTools;
        this.allowedWorlds = allowedWorlds;
        this.disabledBlocks = disabledBlocks;
    }

    public boolean isToolAllowed(Material material) {
        return allowedTools.isEmpty() || allowedTools.contains(material);
    }

    public boolean isWorldAllowed(String worldName) {
        return allowedWorlds.isEmpty() || allowedWorlds.contains(worldName.toLowerCase(Locale.ROOT));
    }

    public boolean isBlocked(Material material) {
        return disabledBlocks.contains(material);
    }

    public static Set<Material> parseMaterials(Object raw) {
        Set<Material> out = new HashSet<>();
        if (raw instanceof Iterable<?> iterable) {
            for (Object item : iterable) {
                Material material = Material.matchMaterial(String.valueOf(item));
                if (material != null) out.add(material);
            }
        }
        return out;
    }

    public static Set<String> parseWorlds(Object raw) {
        Set<String> out = new HashSet<>();
        if (raw instanceof Iterable<?> iterable) {
            for (Object item : iterable) {
                out.add(String.valueOf(item).toLowerCase(Locale.ROOT));
            }
        }
        return out;
    }
}
