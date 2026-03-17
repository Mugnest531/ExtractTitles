package ru.lewhu.extracttitles.domain.title;

import org.bukkit.Material;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class TitleDefinition {
    private final String id;
    private String displayName;
    private String rawFormat;
    private String visibleFormat;
    private String description;
    private List<String> lore;
    private Material icon;
    private String category;
    private boolean enabled;
    private CurrencyType currencyType;
    private String permission;
    private List<String> purchaseRequirements;
    private boolean adminOnlyPurchase;
    private List<TitleEffectDefinition> effects;

    private boolean permanentPurchasable;
    private double permanentCost;
    private Map<String, Double> temporaryCosts;

    public TitleDefinition(String id) {
        this.id = id.toLowerCase(Locale.ROOT);
        this.displayName = id;
        this.rawFormat = id;
        this.visibleFormat = id;
        this.description = "";
        this.lore = new ArrayList<>();
        this.icon = Material.NAME_TAG;
        this.category = "default";
        this.enabled = true;
        this.currencyType = CurrencyType.NONE;
        this.permission = "";
        this.purchaseRequirements = new ArrayList<>();
        this.adminOnlyPurchase = false;
        this.effects = new ArrayList<>();

        this.permanentPurchasable = false;
        this.permanentCost = 0;
        this.temporaryCosts = new LinkedHashMap<>();
    }

    public String id() { return id; }
    public String displayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public String rawFormat() { return rawFormat; }
    public void setRawFormat(String rawFormat) { this.rawFormat = rawFormat; }
    public String visibleFormat() { return visibleFormat; }
    public void setVisibleFormat(String visibleFormat) { this.visibleFormat = visibleFormat; }
    public String description() { return description; }
    public void setDescription(String description) { this.description = description; }
    public List<String> lore() { return Collections.unmodifiableList(lore); }
    public void setLore(List<String> lore) { this.lore = lore == null ? new ArrayList<>() : new ArrayList<>(lore); }
    public Material icon() { return icon; }
    public void setIcon(Material icon) { this.icon = icon == null ? Material.NAME_TAG : icon; }
    public String category() { return category; }
    public void setCategory(String category) { this.category = category; }
    public boolean enabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public CurrencyType currencyType() { return currencyType; }
    public void setCurrencyType(CurrencyType currencyType) { this.currencyType = currencyType == null ? CurrencyType.NONE : currencyType; }
    public String permission() { return permission; }
    public void setPermission(String permission) { this.permission = permission == null ? "" : permission; }
    public List<String> purchaseRequirements() { return Collections.unmodifiableList(purchaseRequirements); }
    public void setPurchaseRequirements(List<String> purchaseRequirements) { this.purchaseRequirements = purchaseRequirements == null ? new ArrayList<>() : new ArrayList<>(purchaseRequirements); }
    public boolean adminOnlyPurchase() { return adminOnlyPurchase; }
    public void setAdminOnlyPurchase(boolean adminOnlyPurchase) { this.adminOnlyPurchase = adminOnlyPurchase; }
    public List<TitleEffectDefinition> effects() { return Collections.unmodifiableList(effects); }
    public void setEffects(List<TitleEffectDefinition> effects) { this.effects = effects == null ? new ArrayList<>() : new ArrayList<>(effects); }

    public boolean permanentPurchasable() { return permanentPurchasable; }
    public void setPermanentPurchasable(boolean permanentPurchasable) { this.permanentPurchasable = permanentPurchasable; }
    public double permanentCost() { return permanentCost; }
    public void setPermanentCost(double permanentCost) { this.permanentCost = Math.max(0, permanentCost); }

    public Map<String, Double> temporaryCosts() {
        return Collections.unmodifiableMap(temporaryCosts);
    }

    public void setTemporaryCosts(Map<String, Double> temporaryCosts) {
        LinkedHashMap<String, Double> out = new LinkedHashMap<>();
        if (temporaryCosts != null) {
            for (Map.Entry<String, Double> e : temporaryCosts.entrySet()) {
                if (e.getKey() == null || e.getKey().isBlank()) continue;
                out.put(e.getKey().toLowerCase(Locale.ROOT), Math.max(0, e.getValue() == null ? 0 : e.getValue()));
            }
        }
        this.temporaryCosts = out;
    }

    public boolean hasAnyPurchaseOption() {
        return permanentPurchasable || !temporaryCosts.isEmpty();
    }

    // legacy compatibility
    public boolean purchasable() {
        return hasAnyPurchaseOption();
    }

    public double cost() {
        return permanentCost;
    }

    public String purchaseDuration() {
        return "permanent";
    }
}
