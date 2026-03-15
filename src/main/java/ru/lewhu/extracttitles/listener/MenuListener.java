package ru.lewhu.extracttitles.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.ItemStack;
import ru.lewhu.extracttitles.gui.TitleMenuService;

public final class MenuListener implements Listener {
    private final TitleMenuService menuService;

    public MenuListener(TitleMenuService menuService) {
        this.menuService = menuService;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!menuService.isMenu(event.getInventory())) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;
        ItemStack clicked = event.getCurrentItem();
        menuService.handleClick(player, clicked);
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (menuService.isMenu(event.getInventory())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!menuService.isMenu(event.getInventory())) return;
        if (event.getPlayer() instanceof Player player) {
            menuService.onMenuClosed(player);
        }
    }
}
