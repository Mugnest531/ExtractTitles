package ru.lewhu.extracttitles.listener;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.util.Vector;
import ru.lewhu.extracttitles.service.effect.impl.Mining3x3Effect;
import ru.lewhu.extracttitles.service.effect.impl.Mining3x3Profile;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class MiningListener implements Listener {
    private final Mining3x3Effect miningEffect;
    private final Set<UUID> guard = new HashSet<>();

    public MiningListener(Mining3x3Effect miningEffect) {
        this.miningEffect = miningEffect;
    }

    @EventHandler(ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (guard.contains(player.getUniqueId())) return;

        Mining3x3Profile profile = miningEffect.profile(player);
        if (profile == null) return;

        ItemStack tool = player.getInventory().getItemInMainHand();
        if (!profile.isToolAllowed(tool.getType())) return;
        if (!profile.isWorldAllowed(player.getWorld().getName())) return;

        Block origin = event.getBlock();
        if (profile.isBlocked(origin.getType())) return;

        guard.add(player.getUniqueId());
        try {
            breakNearby(event, profile, tool);
        } finally {
            guard.remove(player.getUniqueId());
        }
    }

    private void breakNearby(BlockBreakEvent event, Mining3x3Profile profile, ItemStack tool) {
        Player player = event.getPlayer();
        Block origin = event.getBlock();
        BlockFace face = resolveFace(player);

        int[][] offsets = switch (face) {
            case UP, DOWN -> new int[][]{{-1, 0, -1}, {0, 0, -1}, {1, 0, -1}, {-1, 0, 0}, {1, 0, 0}, {-1, 0, 1}, {0, 0, 1}, {1, 0, 1}};
            case NORTH, SOUTH -> new int[][]{{-1, -1, 0}, {0, -1, 0}, {1, -1, 0}, {-1, 0, 0}, {1, 0, 0}, {-1, 1, 0}, {0, 1, 0}, {1, 1, 0}};
            default -> new int[][]{{0, -1, -1}, {0, -1, 0}, {0, -1, 1}, {0, 0, -1}, {0, 0, 1}, {0, 1, -1}, {0, 1, 0}, {0, 1, 1}};
        };

        for (int[] offset : offsets) {
            Block target = origin.getRelative(offset[0], offset[1], offset[2]);
            if (target.getType().isAir()) continue;
            if (profile.isBlocked(target.getType())) continue;

            BlockBreakEvent child = new BlockBreakEvent(target, player);
            Bukkit.getPluginManager().callEvent(child);
            if (child.isCancelled()) continue;

            for (ItemStack drop : target.getDrops(tool, player)) {
                target.getWorld().dropItemNaturally(target.getLocation(), drop);
            }
            target.setType(Material.AIR, false);
            damageTool(player, tool);
        }
    }

    private BlockFace resolveFace(Player player) {
        Vector dir = player.getEyeLocation().getDirection();
        double ax = Math.abs(dir.getX());
        double ay = Math.abs(dir.getY());
        double az = Math.abs(dir.getZ());

        if (ay >= ax && ay >= az) {
            return dir.getY() > 0 ? BlockFace.UP : BlockFace.DOWN;
        }
        if (az >= ax) {
            return dir.getZ() > 0 ? BlockFace.SOUTH : BlockFace.NORTH;
        }
        return dir.getX() > 0 ? BlockFace.EAST : BlockFace.WEST;
    }

    private void damageTool(Player player, ItemStack tool) {
        if (!(tool.getItemMeta() instanceof Damageable damageable)) return;
        if (tool.containsEnchantment(Enchantment.UNBREAKING) && Math.random() < 0.6) return;
        damageable.setDamage(damageable.getDamage() + 1);
        tool.setItemMeta(damageable);
        if (damageable.getDamage() >= tool.getType().getMaxDurability()) {
            player.getInventory().setItemInMainHand(null);
        }
    }
}
