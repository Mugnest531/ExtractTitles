package ru.lewhu.extracttitles.service;

import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import ru.lewhu.extracttitles.config.ConfigService;
import ru.lewhu.extracttitles.domain.player.OwnershipSource;
import ru.lewhu.extracttitles.domain.title.CurrencyType;
import ru.lewhu.extracttitles.domain.title.TitleDefinition;
import ru.lewhu.extracttitles.hook.economy.CurrencyService;
import ru.lewhu.extracttitles.util.DurationParser;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class PurchaseService {
    private record PurchaseChoice(boolean permanent, long durationMillis, double cost, String displayDuration) {}

    private final TitleService titleService;
    private final PlayerTitleService playerTitleService;
    private final CurrencyService currencyService;
    private final ConfigService configService;

    public PurchaseService(TitleService titleService,
                           PlayerTitleService playerTitleService,
                           CurrencyService currencyService,
                           ConfigService configService) {
        this.titleService = titleService;
        this.playerTitleService = playerTitleService;
        this.currencyService = currencyService;
        this.configService = configService;
    }

    public PurchaseResult buy(Player player, String titleId) {
        return buy(player, titleId, null);
    }

    public PurchaseResult buy(Player player, String titleId, String ignoredOptionKey) {
        var opt = titleService.find(titleId);
        if (opt.isEmpty()) return PurchaseResult.TITLE_NOT_FOUND;
        TitleDefinition title = opt.get();

        if (!title.enabled()) return PurchaseResult.DISABLED;
        if (playerTitleService.hasTitle(player.getUniqueId(), title.id())) return PurchaseResult.ALREADY_OWNED;

        PurchaseChoice choice = resolveChoice(title);
        if (choice == null) return PurchaseResult.INVALID_DURATION;

        CurrencyType providerType = configuredCurrency();
        if (choice.cost > 0 && providerType != CurrencyType.NONE) {
            if (!currencyService.isAvailable(providerType)) return PurchaseResult.PROVIDER_UNAVAILABLE;
            if (currencyService.balance(providerType, player) < choice.cost) return PurchaseResult.NOT_ENOUGH_FUNDS;
            if (!currencyService.withdraw(providerType, player, choice.cost)) return PurchaseResult.NOT_ENOUGH_FUNDS;
        }

        long expiresAt = choice.permanent ? 0L : Instant.now().toEpochMilli() + choice.durationMillis;
        playerTitleService.grant(player.getUniqueId(), title.id(), choice.permanent, expiresAt, OwnershipSource.PURCHASE);
        playerTitleService.save(player.getUniqueId());
        return PurchaseResult.SUCCESS;
    }

    public PurchaseResult grant(OfflinePlayer offlinePlayer, String titleId, boolean permanent, long durationMillis, OwnershipSource source) {
        var opt = titleService.find(titleId);
        if (opt.isEmpty()) return PurchaseResult.TITLE_NOT_FOUND;

        long expiresAt = permanent ? 0L : Instant.now().toEpochMilli() + durationMillis;
        playerTitleService.grant(offlinePlayer.getUniqueId(), titleId, permanent, expiresAt, source);
        playerTitleService.save(offlinePlayer.getUniqueId());
        return PurchaseResult.SUCCESS;
    }

    public CurrencyType configuredCurrency() {
        return CurrencyType.fromString(configService.config().getString("purchase.currency-provider", "VAULT"));
    }

    public List<String> optionKeys(TitleDefinition title) {
        return List.of();
    }

    public List<String> optionKeys(String titleId) {
        return List.of();
    }

    public String optionsText(TitleDefinition title) {
        PurchaseChoice choice = resolveChoice(title);
        if (choice == null) return "\u043d\u0435\u0442";
        return choice.displayDuration;
    }

    public double defaultDisplayPrice(TitleDefinition title) {
        PurchaseChoice choice = resolveChoice(title);
        return choice == null ? 0.0 : choice.cost;
    }

    private PurchaseChoice resolveChoice(TitleDefinition title) {
        if (!title.hasAnyPurchaseOption()) return null;

        if (title.permanentPurchasable()) {
            return new PurchaseChoice(true, 0L, title.permanentCost(), "\u043d\u0430\u0432\u0441\u0435\u0433\u0434\u0430");
        }

        Map.Entry<String, Double> first = title.temporaryCosts().entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.naturalOrder()))
                .findFirst()
                .orElse(null);
        if (first == null) return null;

        DurationParser.DurationResult parsed = DurationParser.parse(first.getKey());
        if (parsed.permanent() || parsed.millis() <= 0) return null;
        return new PurchaseChoice(false, parsed.millis(), first.getValue(), localizeDurationKey(first.getKey()));
    }

    private String localizeDurationKey(String key) {
        DurationParser.DurationResult parsed = DurationParser.parse(key);
        if (parsed.permanent()) return "\u043d\u0430\u0432\u0441\u0435\u0433\u0434\u0430";
        long hours = parsed.millis() / 1000L / 60L / 60L;
        if (hours % 24L == 0) return (hours / 24L) + "\u0434";
        return hours + "\u0447";
    }

    private String formatPrice(double price) {
        if (Math.floor(price) == price) return String.valueOf((long) price);
        return String.format(Locale.US, "%.2f", price);
    }
}
