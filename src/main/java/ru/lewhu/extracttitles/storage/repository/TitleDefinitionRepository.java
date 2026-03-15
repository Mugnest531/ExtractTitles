package ru.lewhu.extracttitles.storage.repository;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import ru.lewhu.extracttitles.config.ConfigService;
import ru.lewhu.extracttitles.domain.title.CurrencyType;
import ru.lewhu.extracttitles.domain.title.TitleDefinition;
import ru.lewhu.extracttitles.domain.title.TitleEffectDefinition;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class TitleDefinitionRepository {
    private final ConfigService configService;
    private final Map<String, TitleDefinition> titles = new ConcurrentHashMap<>();
    private final Map<String, List<TitleEffectDefinition>> effectTemplates = new HashMap<>();

    public TitleDefinitionRepository(ConfigService configService) {
        this.configService = configService;
    }

    public void load() {
        titles.clear();
        effectTemplates.clear();
        loadEffectTemplates();

        FileConfiguration cfg = configService.titles();
        ConfigurationSection root = cfg.getConfigurationSection("titles");
        if (root == null) {
            return;
        }

        for (String id : root.getKeys(false)) {
            ConfigurationSection section = root.getConfigurationSection(id);
            if (section == null) {
                continue;
            }

            TitleDefinition title = new TitleDefinition(id);
            title.setDisplayName(section.getString("display-name", id));
            title.setRawFormat(section.getString("raw", id));
            title.setVisibleFormat(section.getString("visible", section.getString("raw", id)));
            title.setDescription(section.getString("description", ""));
            title.setLore(section.getStringList("lore"));
            title.setIcon(parseMaterial(section.getString("icon", "NAME_TAG")));
            title.setCategory(section.getString("category", "default"));
            title.setEnabled(section.getBoolean("enabled", true));
            title.setCurrencyType(CurrencyType.fromString(section.getString("currency", "NONE")));
            title.setPermission(section.getString("permission", ""));
            title.setPurchaseRequirements(section.getStringList("purchase-requirements"));
            title.setAdminOnlyPurchase(section.getBoolean("admin-only-purchase", false));

            loadPurchaseOptions(section, title);
            title.setEffects(readEffects(section));
            titles.put(id.toLowerCase(Locale.ROOT), title);
        }
    }

    private void loadEffectTemplates() {
        ConfigurationSection root = configService.effects().getConfigurationSection("templates");
        if (root == null) {
            return;
        }

        for (String key : root.getKeys(false)) {
            List<Map<?, ?>> list = root.getMapList(key);
            if (list == null || list.isEmpty()) {
                continue;
            }
            effectTemplates.put(key.toLowerCase(Locale.ROOT), parseEffectEntries(list));
        }
    }

    private void loadPurchaseOptions(ConfigurationSection section, TitleDefinition title) {
        ConfigurationSection options = section.getConfigurationSection("purchase-options");
        if (options != null) {
            ConfigurationSection permanent = options.getConfigurationSection("permanent");
            if (permanent != null) {
                title.setPermanentPurchasable(permanent.getBoolean("enabled", false));
                title.setPermanentCost(permanent.getDouble("cost", 0));
            }

            Map<String, Double> temporary = new LinkedHashMap<>();
            ConfigurationSection temporarySec = options.getConfigurationSection("temporary");
            if (temporarySec != null) {
                for (String key : temporarySec.getKeys(false)) {
                    temporary.put(key.toLowerCase(Locale.ROOT), temporarySec.getDouble(key, 0));
                }
            }

            if (title.permanentPurchasable()) {
                title.setTemporaryCosts(Map.of());
            } else if (!temporary.isEmpty()) {
                Map.Entry<String, Double> first = temporary.entrySet().stream()
                        .sorted(Map.Entry.comparingByKey())
                        .findFirst()
                        .orElse(null);
                if (first != null) {
                    title.setTemporaryCosts(Map.of(first.getKey(), first.getValue()));
                } else {
                    title.setTemporaryCosts(Map.of());
                }
            } else {
                title.setTemporaryCosts(Map.of());
            }
            return;
        }

        double legacyCost = section.getDouble("cost", 0);
        String duration = section.getString("purchase-duration", "permanent");
        if (duration.equalsIgnoreCase("permanent")) {
            title.setPermanentPurchasable(legacyCost > 0);
            title.setPermanentCost(legacyCost);
            title.setTemporaryCosts(Map.of());
        } else {
            title.setPermanentPurchasable(false);
            title.setPermanentCost(0);
            title.setTemporaryCosts(Map.of(duration.toLowerCase(Locale.ROOT), legacyCost));
        }
    }

    private Material parseMaterial(String material) {
        if (material == null) {
            return Material.NAME_TAG;
        }
        Material parsed = Material.matchMaterial(material);
        return parsed == null ? Material.NAME_TAG : parsed;
    }

    private List<TitleEffectDefinition> readEffects(ConfigurationSection section) {
        List<TitleEffectDefinition> out = new ArrayList<>();
        List<Map<?, ?>> entries = section.getMapList("effects");
        for (Map<?, ?> entry : entries) {
            String templateName = readTemplateName(entry);
            if (templateName != null) {
                List<TitleEffectDefinition> template = effectTemplates.get(templateName.toLowerCase(Locale.ROOT));
                if (template != null) {
                    out.addAll(template);
                }
                continue;
            }
            TitleEffectDefinition effect = parseEffectEntry(entry);
            if (effect != null) {
                out.add(effect);
            }
        }
        return out;
    }

    private List<TitleEffectDefinition> parseEffectEntries(List<Map<?, ?>> entries) {
        List<TitleEffectDefinition> out = new ArrayList<>();
        for (Map<?, ?> entry : entries) {
            TitleEffectDefinition effect = parseEffectEntry(entry);
            if (effect != null) {
                out.add(effect);
            }
        }
        return out;
    }

    private TitleEffectDefinition parseEffectEntry(Map<?, ?> entry) {
        Object rawType = entry.get("type");
        if (rawType == null) {
            return null;
        }
        String type = String.valueOf(rawType);
        Map<String, Object> options = new HashMap<>();
        for (Map.Entry<?, ?> mapEntry : entry.entrySet()) {
            String key = String.valueOf(mapEntry.getKey());
            if ("type".equalsIgnoreCase(key)) {
                continue;
            }
            options.put(key, mapEntry.getValue());
        }
        return new TitleEffectDefinition(type, options);
    }

    private String readTemplateName(Map<?, ?> entry) {
        Object template = entry.get("template");
        if (template != null) {
            return String.valueOf(template);
        }
        Object preset = entry.get("preset");
        if (preset != null) {
            return String.valueOf(preset);
        }
        return null;
    }

    public Collection<TitleDefinition> all() {
        return titles.values();
    }

    public Optional<TitleDefinition> byId(String id) {
        if (id == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(titles.get(id.toLowerCase(Locale.ROOT)));
    }

    public boolean exists(String id) {
        return id != null && titles.containsKey(id.toLowerCase(Locale.ROOT));
    }

    public void put(TitleDefinition definition) {
        titles.put(definition.id(), definition);
        saveToConfig(definition);
    }

    public void remove(String id) {
        String key = id.toLowerCase(Locale.ROOT);
        titles.remove(key);
        configService.titles().set("titles." + key, null);
        configService.titlesFile().save();
    }

    public void saveToConfig(TitleDefinition title) {
        String path = "titles." + title.id();
        FileConfiguration cfg = configService.titles();
        cfg.set(path + ".display-name", title.displayName());
        cfg.set(path + ".raw", title.rawFormat());
        cfg.set(path + ".visible", title.visibleFormat());
        cfg.set(path + ".description", title.description());
        cfg.set(path + ".lore", title.lore());
        cfg.set(path + ".icon", title.icon().name());
        cfg.set(path + ".category", title.category());
        cfg.set(path + ".enabled", title.enabled());
        cfg.set(path + ".currency", title.currencyType().name());
        cfg.set(path + ".permission", title.permission());
        cfg.set(path + ".purchase-requirements", title.purchaseRequirements());
        cfg.set(path + ".admin-only-purchase", title.adminOnlyPurchase());

        cfg.set(path + ".purchase-options.permanent.enabled", title.permanentPurchasable());
        cfg.set(path + ".purchase-options.permanent.cost", title.permanentCost());
        cfg.set(path + ".purchase-options.temporary", title.temporaryCosts());

        List<Map<String, Object>> effects = new ArrayList<>();
        for (TitleEffectDefinition effect : title.effects()) {
            Map<String, Object> map = new HashMap<>();
            map.put("type", effect.type());
            map.putAll(effect.options());
            effects.add(map);
        }
        cfg.set(path + ".effects", effects);
        configService.titlesFile().save();
    }
}