package ru.lewhu.extracttitles.hook.economy;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import ru.lewhu.extracttitles.domain.title.CurrencyType;

public final class VaultCurrencyProvider implements CurrencyProvider {
    private final JavaPlugin plugin;
    private Economy economy;

    public VaultCurrencyProvider(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    private void hook() {
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
            economy = null;
            return;
        }
        RegisteredServiceProvider<Economy> rsp = Bukkit.getServicesManager().getRegistration(Economy.class);
        economy = rsp == null ? null : rsp.getProvider();
    }

    private Economy economy() {
        if (economy == null) {
            hook();
        }
        return economy;
    }

    @Override
    public CurrencyType type() {
        return CurrencyType.VAULT;
    }

    @Override
    public boolean isAvailable() {
        return economy() != null;
    }

    @Override
    public double balance(OfflinePlayer player) {
        Economy eco = economy();
        return eco == null ? 0 : eco.getBalance(player);
    }

    @Override
    public boolean withdraw(OfflinePlayer player, double amount) {
        Economy eco = economy();
        if (eco == null) return false;
        if (eco.getBalance(player) < amount) return false;
        return eco.withdrawPlayer(player, amount).transactionSuccess();
    }

    @Override
    public String displayName() {
        return plugin.getConfig().getString("currency-names.vault", "Vault");
    }
}
