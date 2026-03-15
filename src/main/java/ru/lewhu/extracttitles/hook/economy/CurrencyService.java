package ru.lewhu.extracttitles.hook.economy;

import org.bukkit.OfflinePlayer;
import ru.lewhu.extracttitles.domain.title.CurrencyType;

import java.util.EnumMap;
import java.util.Map;

public final class CurrencyService {
    private final Map<CurrencyType, CurrencyProvider> providers = new EnumMap<>(CurrencyType.class);

    public CurrencyService(CurrencyProvider vault, CurrencyProvider playerPoints) {
        providers.put(CurrencyType.VAULT, vault);
        providers.put(CurrencyType.PLAYERPOINTS, playerPoints);
    }

    public boolean isAvailable(CurrencyType type) {
        CurrencyProvider provider = providers.get(type);
        return provider != null && provider.isAvailable();
    }

    public double balance(CurrencyType type, OfflinePlayer player) {
        CurrencyProvider provider = providers.get(type);
        return provider == null ? 0 : provider.balance(player);
    }

    public boolean withdraw(CurrencyType type, OfflinePlayer player, double amount) {
        CurrencyProvider provider = providers.get(type);
        return provider != null && provider.withdraw(player, amount);
    }

    public String providerName(CurrencyType type) {
        CurrencyProvider provider = providers.get(type);
        return provider == null ? type.name() : provider.displayName();
    }
}
