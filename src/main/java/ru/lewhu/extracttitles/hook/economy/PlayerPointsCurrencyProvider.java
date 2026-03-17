package ru.lewhu.extracttitles.hook.economy;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.Plugin;
import ru.lewhu.extracttitles.domain.title.CurrencyType;

import java.lang.reflect.Method;
import java.util.UUID;

public final class PlayerPointsCurrencyProvider implements CurrencyProvider {
    private Object api;
    private Method lookMethod;
    private Method takeMethod;

    public PlayerPointsCurrencyProvider() {
        hook();
    }

    private void hook() {
        Plugin plugin = Bukkit.getPluginManager().getPlugin("PlayerPoints");
        if (plugin == null) return;
        try {
            Method getApi = plugin.getClass().getMethod("getAPI");
            api = getApi.invoke(plugin);
            lookMethod = api.getClass().getMethod("look", UUID.class);
            takeMethod = api.getClass().getMethod("take", UUID.class, int.class);
        } catch (Exception ignored) {
            api = null;
            lookMethod = null;
            takeMethod = null;
        }
    }

    @Override
    public CurrencyType type() {
        return CurrencyType.PLAYERPOINTS;
    }

    @Override
    public boolean isAvailable() {
        return api != null && lookMethod != null && takeMethod != null;
    }

    @Override
    public double balance(OfflinePlayer player) {
        if (!isAvailable()) return 0;
        try {
            Object result = lookMethod.invoke(api, player.getUniqueId());
            if (result instanceof Number number) {
                return number.doubleValue();
            }
            return 0;
        } catch (Exception ignored) {
            return 0;
        }
    }

    @Override
    public boolean withdraw(OfflinePlayer player, double amount) {
        if (!isAvailable()) return false;
        int rounded = (int) Math.ceil(amount);
        try {
            if (balance(player) < rounded) return false;
            Object result = takeMethod.invoke(api, player.getUniqueId(), rounded);
            return (result instanceof Boolean b) && b;
        } catch (Exception ignored) {
            return false;
        }
    }

    @Override
    public String displayName() {
        return "PlayerPoints";
    }
}
