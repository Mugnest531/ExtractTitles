package ru.lewhu.extracttitles.gui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import ru.lewhu.extracttitles.config.ConfigService;
import ru.lewhu.extracttitles.domain.player.TitleOwnership;
import ru.lewhu.extracttitles.domain.title.CurrencyType;
import ru.lewhu.extracttitles.domain.title.TitleDefinition;
import ru.lewhu.extracttitles.service.MessageService;
import ru.lewhu.extracttitles.service.PlaceholderService;
import ru.lewhu.extracttitles.service.PlayerTitleService;
import ru.lewhu.extracttitles.service.PurchaseResult;
import ru.lewhu.extracttitles.service.PurchaseService;
import ru.lewhu.extracttitles.service.TitleActivationService;
import ru.lewhu.extracttitles.service.TitleService;
import ru.lewhu.extracttitles.util.Text;
import ru.lewhu.extracttitles.util.TimeFormat;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class TitleMenuService {
    private static final String STATE_NOT_OWNED = "not-owned";
    private static final String STATE_OWNED_INACTIVE = "owned-inactive";
    private static final String STATE_OWNED_ACTIVE = "owned-active";

    private final ConfigService configService;
    private final TitleService titleService;
    private final PlayerTitleService playerTitleService;
    private final PurchaseService purchaseService;
    private final TitleActivationService activationService;
    private final MessageService messageService;
    private final PlaceholderService placeholderService;
    private final NamespacedKey titleIdKey;
    private final NamespacedKey actionKey;

    private final Map<UUID, Integer> pages = new ConcurrentHashMap<>();

    public TitleMenuService(JavaPlugin plugin,
                            ConfigService configService,
                            TitleService titleService,
                            PlayerTitleService playerTitleService,
                            PurchaseService purchaseService,
                            TitleActivationService activationService,
                            MessageService messageService,
                            PlaceholderService placeholderService) {
        this.configService = configService;
        this.titleService = titleService;
        this.playerTitleService = playerTitleService;
        this.purchaseService = purchaseService;
        this.activationService = activationService;
        this.messageService = messageService;
        this.placeholderService = placeholderService;
        this.titleIdKey = new NamespacedKey(plugin, "title_id");
        this.actionKey = new NamespacedKey(plugin, "menu_action");

        Bukkit.getScheduler().runTaskTimer(plugin, this::refreshOpenMenus, 20L, 20L);
    }

    public void open(Player player, int requestedPage) {
        int size = configService.menus().getInt("menu.size", 54);
        MenuHolder holder = new MenuHolder();
        String menuTitle = placeholderService.apply(player, configService.menus().getString("menu.title", "<gradient:#00C6FF:#0072FF><bold>TITLES</bold></gradient>"));
        Inventory inv = Bukkit.createInventory(holder, size, Text.parse(menuTitle));
        holder.inventory = inv;

        int totalPages = totalPages();
        int page = Math.max(0, Math.min(requestedPage, totalPages - 1));

        render(player, inv, page, totalPages);
        pages.put(player.getUniqueId(), page);
        player.openInventory(inv);
    }

    public boolean isMenu(Inventory inventory) {
        return inventory.getHolder() instanceof MenuHolder;
    }

    public void onMenuClosed(Player player) {
        if (isMenu(player.getOpenInventory().getTopInventory())) {
            return;
        }
        pages.remove(player.getUniqueId());
    }

    public void handleClick(Player player, ItemStack clicked) {
        if (clicked == null || !clicked.hasItemMeta()) {
            return;
        }
        ItemMeta meta = clicked.getItemMeta();

        String action = meta.getPersistentDataContainer().get(actionKey, PersistentDataType.STRING);
        if (action != null) {
            int current = pages.getOrDefault(player.getUniqueId(), 0);
            if (action.equals("prev")) {
                open(player, Math.max(0, current - 1));
            }
            if (action.equals("next")) {
                open(player, current + 1);
            }
            return;
        }

        String titleId = meta.getPersistentDataContainer().get(titleIdKey, PersistentDataType.STRING);
        if (titleId == null) {
            return;
        }

        if (playerTitleService.hasTitle(player.getUniqueId(), titleId)) {
            String active = playerTitleService.getActive(player.getUniqueId());
            if (titleId.equalsIgnoreCase(active)) {
                activationService.deactivate(player);
                messageService.send(player, "deactivated", Map.of("title", displayTitle(titleId)));
            } else if (activationService.activate(player, titleId)) {
                messageService.send(player, "activated", Map.of("title", displayTitle(titleId)));
            } else {
                messageService.send(player, "activate-failed", Map.of("title", displayTitle(titleId)));
            }
            open(player, pages.getOrDefault(player.getUniqueId(), 0));
            return;
        }

        PurchaseResult result = purchaseService.buy(player, titleId);
        if (result == PurchaseResult.SUCCESS) {
            messageService.send(player, "buy-success", Map.of(
                    "title", titleId,
                    "duration", ownershipDurationText(player, titleId)
            ));
        } else {
            messageService.send(player, "buy-failed", Map.of("reason", messageService.raw(purchaseReasonKey(result))));
        }
        open(player, pages.getOrDefault(player.getUniqueId(), 0));
    }

    private void refreshOpenMenus() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!player.isOnline()) {
                continue;
            }
            Inventory top = player.getOpenInventory().getTopInventory();
            if (!isMenu(top)) {
                continue;
            }

            int totalPages = totalPages();
            int page = Math.max(0, Math.min(pages.getOrDefault(player.getUniqueId(), 0), totalPages - 1));
            render(player, top, page, totalPages);
        }
    }

    private void render(Player player, Inventory inv, int page, int totalPages) {
        fill(inv, player);
        placeInfo(player, inv);
        placeTitles(player, inv, page);
        placeNavigation(inv, player, page, totalPages);
    }

    private void fill(Inventory inv, Player player) {
        Material fill = Material.matchMaterial(configService.menus().getString("menu.filler.material", "BLACK_STAINED_GLASS_PANE"));
        if (fill == null) {
            fill = Material.BLACK_STAINED_GLASS_PANE;
        }

        ItemStack filler = new ItemStack(fill);
        ItemMeta meta = filler.getItemMeta();
        String name = placeholderService.apply(player, configService.menus().getString("menu.filler.name", " "));
        meta.displayName(nonItalic(Text.parse(name)));
        filler.setItemMeta(meta);

        for (int slot : configService.menus().getIntegerList("menu.filler.slots")) {
            if (slot >= 0 && slot < inv.getSize()) {
                inv.setItem(slot, filler);
            }
        }
    }

    private void placeInfo(Player player, Inventory inv) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        meta.setOwningPlayer(player);

        String infoName = placeholderService.apply(player,
                configService.menus().getString("menu.info.name", "<gradient:#1dd1a1:#10ac84><bold>INFO</bold></gradient>"));
        meta.displayName(nonItalic(Text.parse(infoName)));

        int owned = (int) titleService.all().stream()
                .filter(TitleDefinition::enabled)
                .filter(t -> playerTitleService.hasTitle(player.getUniqueId(), t.id()))
                .count();
        int total = (int) titleService.all().stream().filter(TitleDefinition::enabled).count();

        String activeId = playerTitleService.getActive(player.getUniqueId());
        String active = activeId == null
                ? configService.menus().getString("menu.info.no-active", "<#ff4d4f>none</#ff4d4f>")
                : titleService.find(activeId).map(TitleDefinition::displayName).orElse(activeId);

        String activeExpiry = ownershipDurationText(player, activeId);

        Map<String, String> local = new HashMap<>();
        local.put("player", player.getName());
        local.put("owned", String.valueOf(owned));
        local.put("total", String.valueOf(total));
        local.put("owned_total", owned + "/" + total);
        local.put("active", active);
        local.put("active_expiry", activeExpiry);
        local.put("balance", resolveBalance(player));

        List<String> lore = new ArrayList<>();
        for (String line : configService.menus().getStringList("menu.info.lore")) {
            lore.add(placeholderService.apply(player, line, local));
        }

        meta.lore(lore.stream().map(line -> nonItalic(Text.parse(line))).toList());
        meta.addItemFlags(ItemFlag.values());
        head.setItemMeta(meta);
        inv.setItem(configService.menus().getInt("menu.info.slot", 4), head);
    }

    private void placeTitles(Player player, Inventory inv, int page) {
        List<Integer> slots = configService.menus().getIntegerList("menu.title-slots");
        if (slots.isEmpty()) {
            return;
        }

        List<TitleDefinition> titles = titleService.all().stream()
                .filter(TitleDefinition::enabled)
                .sorted(Comparator.comparing(TitleDefinition::id))
                .toList();

        int pageSize = slots.size();
        int from = page * pageSize;
        int to = Math.min(titles.size(), from + pageSize);

        for (int i = from, index = 0; i < to; i++, index++) {
            TitleDefinition title = titles.get(i);
            boolean owned = playerTitleService.hasTitle(player.getUniqueId(), title.id());
            boolean active = title.id().equalsIgnoreCase(playerTitleService.getActive(player.getUniqueId()));

            String state = !owned ? STATE_NOT_OWNED : (active ? STATE_OWNED_ACTIVE : STATE_OWNED_INACTIVE);
            ItemStack item = createTitleItem(title, state);
            ItemMeta itemMeta = item.getItemMeta();
            // Card title is configured via menu.cards.name.<state>

            Map<String, String> local = new HashMap<>();
            local.put("title_id", title.id());
            local.put("name", title.displayName());
            local.put("description", title.description().isBlank()
                    ? configService.menus().getString("menu.defaults.no-description", "Description is not set.")
                    : title.description());
            local.put("cost", formatPrice(purchaseService.defaultDisplayPrice(title)));
            local.put("purchase_options", purchaseService.optionsText(title));
            local.put("owned_expiry", ownershipDurationText(player, title.id()));
            local.put("line", configService.menus().getString("menu.cards.separator", "<#3a3a3a>--------------</#3a3a3a>"));

            String lorePath;
            if (!owned) {
                lorePath = "menu.cards.not-owned";
            } else if (!active) {
                lorePath = "menu.cards.owned-inactive";
            } else {
                lorePath = "menu.cards.owned-active";
            }
            String namePath = "menu.cards.name." + state;
            String cardName = configService.menus().getString(namePath, "{name}");
            cardName = placeholderService.apply(player, cardName, local);
            itemMeta.displayName(nonItalic(Text.parse(cardName)));


            List<String> lore = new ArrayList<>();
            for (String line : configService.menus().getStringList(lorePath)) {
                lore.add(placeholderService.apply(player, line, local));
            }

            itemMeta.lore(lore.stream().map(line -> nonItalic(Text.parse(line))).toList());
            itemMeta.addItemFlags(ItemFlag.values());
            itemMeta.getPersistentDataContainer().set(titleIdKey, PersistentDataType.STRING, title.id());
            item.setItemMeta(itemMeta);
            inv.setItem(slots.get(index), item);
        }
    }

    private void placeNavigation(Inventory inv, Player player, int page, int totalPages) {
        int prevSlot = configService.menus().getInt("menu.navigation.prev-slot", 45);
        int nextSlot = configService.menus().getInt("menu.navigation.next-slot", 53);
        int pageSlot = configService.menus().getInt("menu.navigation.page-slot", 49);

        if (page > 0) {
            String prev = placeholderService.apply(player,
                    configService.menus().getString("menu.navigation.prev-name", "<#81ecec>Prev</#81ecec>"));
            inv.setItem(prevSlot, navItem("prev", prev));
        }

        if (page < totalPages - 1) {
            String next = placeholderService.apply(player,
                    configService.menus().getString("menu.navigation.next-name", "<#81ecec>Next</#81ecec>"));
            inv.setItem(nextSlot, navItem("next", next));
        }

        ItemStack pageItem = new ItemStack(Material.PAPER);
        ItemMeta pageMeta = pageItem.getItemMeta();
        String pageName = configService.menus().getString("menu.navigation.page-name", "<gradient:#a29bfe:#6c5ce7>Page {page}/{pages}</gradient>");
        pageName = pageName.replace("{page}", String.valueOf(page + 1)).replace("{pages}", String.valueOf(totalPages));
        pageMeta.displayName(nonItalic(Text.parse(placeholderService.apply(player, pageName))));
        pageItem.setItemMeta(pageMeta);
        inv.setItem(pageSlot, pageItem);
    }

    private ItemStack navItem(String action, String name) {
        ItemStack item = new ItemStack(Material.ARROW);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(nonItalic(Text.parse(name)));
        meta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, action);
        item.setItemMeta(meta);
        return item;
    }

    private int totalPages() {
        int pageSize = Math.max(1, configService.menus().getIntegerList("menu.title-slots").size());
        int titleCount = (int) titleService.all().stream().filter(TitleDefinition::enabled).count();
        return Math.max(1, (int) Math.ceil((double) titleCount / pageSize));
    }

    private String ownershipDurationText(Player player, String titleId) {
        if (titleId == null || titleId.isBlank()) {
            return "-";
        }
        TitleOwnership own = playerTitleService.ownership(player.getUniqueId(), titleId);
        if (own == null) {
            return "-";
        }
        if (own.permanent()) {
            return messageService.raw("duration-forever");
        }
        long remaining = playerTitleService.remaining(player.getUniqueId(), titleId);
        return formatDuration(remaining);
    }

    private ItemStack createTitleItem(TitleDefinition title, String state) {
        Material defaultMaterial = STATE_NOT_OWNED.equals(state) ? Material.BARRIER : title.icon();
        Material material = resolveMenuStateMaterial(title.id(), state, defaultMaterial);
        ItemStack item = new ItemStack(material);
        if (resolveMenuStateGlow(title.id(), state)) {
            ItemMeta meta = item.getItemMeta();
            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            item.setItemMeta(meta);
        }
        return item;
    }

    private Material resolveMenuStateMaterial(String titleId, String state, Material fallback) {
        ConfigurationSection stateSection = configService.titles().getConfigurationSection("titles." + titleId + ".menu-icons." + state);
        if (stateSection == null) {
            return fallback;
        }
        Material parsed = Material.matchMaterial(stateSection.getString("material", fallback.name()));
        return parsed == null ? fallback : parsed;
    }

    private boolean resolveMenuStateGlow(String titleId, String state) {
        return configService.titles().getBoolean("titles." + titleId + ".menu-icons." + state + ".glow", false);
    }

    private String resolveBalance(Player player) {
        CurrencyType type = purchaseService.configuredCurrency();
        if (type == CurrencyType.PLAYERPOINTS) {
            String pp = placeholderService.apply(player, "%playerpoints_points%");
            if (!pp.contains("%") && !pp.isBlank()) {
                return pp;
            }
        }

        RegisteredServiceProvider<Economy> rsp = Bukkit.getServicesManager().getRegistration(Economy.class);
        if (rsp != null && rsp.getProvider() != null) {
            double bal = rsp.getProvider().getBalance(player);
            return String.format(Locale.US, "%.2f", bal);
        }

        String value = placeholderService.apply(player, "%vault_eco_balance_formatted%");
        if (!value.contains("%") && !value.isBlank()) {
            return value;
        }

        return placeholderService.apply(player, configService.menus().getString("menu.info.balance-fallback", "0"));
    }

    private String formatPrice(double price) {
        if (Math.floor(price) == price) {
            return String.valueOf((long) price);
        }
        return String.format(Locale.US, "%.2f", price);
    }

    

    private String formatDuration(long millis) {
        return TimeFormat.remaining(
                millis,
                messageService.rawOrDefault("duration-forever", "forever"),
                messageService.rawOrDefault("duration-day-short", "d."),
                messageService.rawOrDefault("duration-hour-short", "h."),
                messageService.rawOrDefault("duration-minute-short", "min."),
                messageService.rawOrDefault("duration-second-short", "sec.")
        );
    }
    private Component nonItalic(Component component) {
        return component.decoration(TextDecoration.ITALIC, false);
    }

    private String displayTitle(String id) {
        if (id == null || id.isBlank()) {
            return id == null ? "" : id;
        }
        return titleService.find(id).map(TitleDefinition::displayName).orElse(id);
    }
    private String purchaseReasonKey(PurchaseResult result) {
        return switch (result) {
            case TITLE_NOT_FOUND -> "reason-title-not-found";
            case DISABLED -> "reason-disabled";
            case NO_PERMISSION -> "reason-no-permission";
            case REQUIREMENT_NOT_MET -> "reason-requirement-not-met";
            case ALREADY_OWNED -> "reason-already-owned";
            case PROVIDER_UNAVAILABLE -> "reason-provider-unavailable";
            case NOT_ENOUGH_FUNDS -> "reason-not-enough-funds";
            case INVALID_DURATION -> "reason-invalid-duration";
            case ERROR -> "reason-error";
            default -> "reason-unknown";
        };
    }

    private static final class MenuHolder implements InventoryHolder {
        private Inventory inventory;

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }
}

