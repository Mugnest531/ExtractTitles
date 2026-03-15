package ru.lewhu.extracttitles.hook.economy;

import org.bukkit.OfflinePlayer;
import ru.lewhu.extracttitles.domain.title.CurrencyType;

public interface CurrencyProvider {
    CurrencyType type();

    boolean isAvailable();

    double balance(OfflinePlayer player);

    boolean withdraw(OfflinePlayer player, double amount);

    String displayName();
}
