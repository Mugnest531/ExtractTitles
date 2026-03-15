package ru.lewhu.extracttitles.service;

import net.milkbowl.vault.permission.Permission;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class PermissionGrantService {
    private final Plugin plugin;
    private Permission vaultPermission;
    private final Map<UUID, PermissionAttachment> attachments = new HashMap<>();

    public PermissionGrantService(Plugin plugin) {
        this.plugin = plugin;
        hookVault();
    }

    private void hookVault() {
        RegisteredServiceProvider<Permission> rsp = Bukkit.getServicesManager().getRegistration(Permission.class);
        if (rsp != null) {
            vaultPermission = rsp.getProvider();
        }
    }

    public boolean isVaultAvailable() {
        return vaultPermission != null;
    }

    public void grant(Player player, List<String> nodes) {
        if (nodes.isEmpty()) return;
        if (vaultPermission != null) {
            for (String node : nodes) {
                vaultPermission.playerAdd(player, node);
            }
            return;
        }
        PermissionAttachment attachment = attachments.computeIfAbsent(player.getUniqueId(), id -> player.addAttachment(plugin));
        for (String node : nodes) {
            attachment.setPermission(node, true);
        }
        player.recalculatePermissions();
    }

    public void revoke(Player player, List<String> nodes) {
        if (nodes.isEmpty()) return;
        if (vaultPermission != null) {
            for (String node : nodes) {
                vaultPermission.playerRemove(player, node);
            }
            return;
        }
        PermissionAttachment attachment = attachments.get(player.getUniqueId());
        if (attachment == null) return;
        for (String node : nodes) {
            attachment.unsetPermission(node);
        }
        player.recalculatePermissions();
    }

    public void clear(Player player) {
        PermissionAttachment attachment = attachments.remove(player.getUniqueId());
        if (attachment != null) {
            player.removeAttachment(attachment);
            player.recalculatePermissions();
        }
    }
}
