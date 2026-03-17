package ru.lewhu.extracttitles.command;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.lewhu.extracttitles.domain.player.OwnershipSource;
import ru.lewhu.extracttitles.gui.TitleMenuService;
import ru.lewhu.extracttitles.service.MessageService;
import ru.lewhu.extracttitles.service.PlayerTitleService;
import ru.lewhu.extracttitles.service.PurchaseResult;
import ru.lewhu.extracttitles.service.PurchaseService;
import ru.lewhu.extracttitles.service.TitleActivationService;
import ru.lewhu.extracttitles.service.TitleService;
import ru.lewhu.extracttitles.util.DurationParser;
import ru.lewhu.extracttitles.util.Text;
import ru.lewhu.extracttitles.util.TimeFormat;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public final class ExtractTitlesCommand implements CommandExecutor, TabCompleter {
    private static final List<String> ROOT = List.of("menu", "list", "activate", "deactivate", "buy", "info", "reload", "give", "remove", "granttemp", "clearactive", "debug");
    private static final String DEFAULT_EXPIRE_FORMAT = "dd.MM.yyyy HH:mm";

    private final TitleService titleService;
    private final PlayerTitleService playerTitleService;
    private final PurchaseService purchaseService;
    private final TitleActivationService activationService;
    private final TitleMenuService menuService;
    private final MessageService messageService;
    private final Runnable reloadAction;

    public ExtractTitlesCommand(TitleService titleService,
                                PlayerTitleService playerTitleService,
                                PurchaseService purchaseService,
                                TitleActivationService activationService,
                                TitleMenuService menuService,
                                MessageService messageService,
                                Runnable reloadAction) {
        this.titleService = titleService;
        this.playerTitleService = playerTitleService;
        this.purchaseService = purchaseService;
        this.activationService = activationService;
        this.menuService = menuService;
        this.messageService = messageService;
        this.reloadAction = reloadAction;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (command.getName().equalsIgnoreCase("titles")) {
            if (!(sender instanceof Player player)) {
                messageService.send(sender, "only-player");
                return true;
            }
            menuService.open(player, 0);
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "menu" -> handleMenu(sender);
            case "list" -> handleList(sender);
            case "activate" -> handleActivate(sender, args);
            case "deactivate" -> handleDeactivate(sender);
            case "buy" -> handleBuy(sender, args);
            case "info" -> handleInfo(sender, args);
            case "reload" -> handleReload(sender);
            case "give" -> handleGive(sender, args);
            case "remove" -> handleRemove(sender, args);
            case "granttemp" -> handleGrantTemp(sender, args);
            case "clearactive" -> handleClearActive(sender, args);
            case "debug" -> handleDebug(sender, args);
            default -> sendHelp(sender);
        }
        return true;
    }

    private void handleMenu(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            messageService.send(sender, "only-player");
            return;
        }
        menuService.open(player, 0);
    }

    private void handleList(CommandSender sender) {
        if (!sender.hasPermission("extracttitles.admin.list")) {
            messageService.send(sender, "no-permission");
            return;
        }

        String titles = titleService.all().stream()
                .map(t -> t.id() + " [" + purchaseService.optionsText(t) + "]")
                .sorted()
                .collect(Collectors.joining(", "));

        if (titles.isBlank()) {
            titles = messageService.rawOrDefault("no-titles", "none");
        }

        messageService.send(sender, "list", Map.of("titles", titles));
    }

    private void handleActivate(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            messageService.send(sender, "only-player");
            return;
        }
        if (args.length < 2) {
            messageService.send(sender, "usage-activate");
            return;
        }

        String activeNow = playerTitleService.getActive(player.getUniqueId());
        if (args[1].equalsIgnoreCase(activeNow)) {
            messageService.send(sender, "already-activated", Map.of("title", displayTitle(args[1])));
            return;
        }

        if (activationService.activate(player, args[1])) {
            messageService.send(sender, "activated", Map.of("title", displayTitle(args[1])));
        } else {
            messageService.send(sender, "activate-failed", Map.of("title", displayTitle(args[1])));
        }
    }

    private void handleDeactivate(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            messageService.send(sender, "only-player");
            return;
        }

        String activeNow = playerTitleService.getActive(player.getUniqueId());
        if (activeNow == null || activeNow.isBlank()) {
            messageService.send(sender, "already-deactivated");
            return;
        }

        activationService.deactivate(player);
        messageService.send(sender, "deactivated", Map.of("title", displayTitle(activeNow)));
    }

    private void handleBuy(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            messageService.send(sender, "only-player");
            return;
        }
        if (args.length < 2) {
            messageService.send(sender, "usage-buy");
            return;
        }

        PurchaseResult result = purchaseService.buy(player, args[1]);
        if (result == PurchaseResult.SUCCESS) {
            String duration = "-";
            var own = playerTitleService.ownership(player.getUniqueId(), args[1]);
            if (own != null) {
                duration = own.permanent()
                        ? messageService.rawOrDefault("duration-forever", "forever")
                        : formatDuration(playerTitleService.remaining(player.getUniqueId(), args[1]));
            }
            messageService.send(sender, "buy-success", Map.of("title", args[1], "duration", duration));
        } else {
            messageService.send(sender, "buy-failed", Map.of("reason", messageService.raw(purchaseReasonKey(result))));
        }
    }

    private void handleInfo(CommandSender sender, String[] args) {
        if (args.length < 2) {
            messageService.send(sender, "usage-info");
            return;
        }
        var opt = titleService.find(args[1]);
        if (opt.isEmpty()) {
            messageService.send(sender, "title-not-found", Map.of("title", displayTitle(args[1])));
            return;
        }
        var title = opt.get();
        messageService.send(sender, "info", Map.of(
                "id", title.id(),
                "display", title.displayName(),
                "raw", title.description(),
                "enabled", String.valueOf(title.enabled()),
                "purchasable", String.valueOf(title.hasAnyPurchaseOption()),
                "cost", formatPrice(purchaseService.defaultDisplayPrice(title)),
                "purchase_options", purchaseService.optionsText(title)
        ));
    }

    private void handleReload(CommandSender sender) {
        if (!sender.hasPermission("extracttitles.admin.edit")) {
            messageService.send(sender, "no-permission");
            return;
        }
        reloadAction.run();
        messageService.send(sender, "reload");
    }

    private void handleGive(CommandSender sender, String[] args) {
        if (!sender.hasPermission("extracttitles.admin.give")) {
            messageService.send(sender, "no-permission");
            return;
        }
        if (args.length < 3) {
            messageService.send(sender, "usage-give");
            return;
        }
        OfflinePlayer target = resolveOffline(args[1]);
        if (target == null) {
            messageService.send(sender, "player-not-found", Map.of("player", args[1]));
            return;
        }
        playerTitleService.load(target.getUniqueId()).thenRun(() -> {
            PurchaseResult result = purchaseService.grant(target, args[2], true, 0, OwnershipSource.ADMIN_GRANT);
            playerTitleService.runSync(() -> {
                if (result == PurchaseResult.SUCCESS) {
                    messageService.send(sender, "give-success", Map.of("player", safeName(target), "title", args[2]));
                } else {
                    messageService.send(sender, "give-failed", Map.of("reason", messageService.raw(purchaseReasonKey(result))));
                }
            });
        });
    }

    private void handleRemove(CommandSender sender, String[] args) {
        if (!sender.hasPermission("extracttitles.admin.remove")) {
            messageService.send(sender, "no-permission");
            return;
        }
        if (args.length < 3) {
            messageService.send(sender, "usage-remove");
            return;
        }
        OfflinePlayer target = resolveOffline(args[1]);
        if (target == null) {
            messageService.send(sender, "player-not-found", Map.of("player", args[1]));
            return;
        }
        playerTitleService.load(target.getUniqueId()).thenRun(() -> {
            playerTitleService.revoke(target.getUniqueId(), args[2]);
            Player online = Bukkit.getPlayer(target.getUniqueId());
            if (online != null) {
                activationService.deactivate(online);
            }
            playerTitleService.save(target.getUniqueId());
            playerTitleService.runSync(() -> messageService.send(sender, "remove-success", Map.of("player", safeName(target), "title", args[2])));
        });
    }

    private void handleGrantTemp(CommandSender sender, String[] args) {
        if (!sender.hasPermission("extracttitles.admin.granttemp")) {
            messageService.send(sender, "no-permission");
            return;
        }
        if (args.length < 4) {
            messageService.send(sender, "usage-granttemp");
            return;
        }
        OfflinePlayer target = resolveOffline(args[1]);
        if (target == null) {
            messageService.send(sender, "player-not-found", Map.of("player", args[1]));
            return;
        }
        DurationParser.DurationResult duration = DurationParser.parse(args[3]);
        if (duration.permanent() || duration.millis() <= 0) {
            messageService.send(sender, "invalid-duration", Map.of("duration", args[3]));
            return;
        }
        playerTitleService.load(target.getUniqueId()).thenRun(() -> {
            PurchaseResult result = purchaseService.grant(target, args[2], false, duration.millis(), OwnershipSource.ADMIN_TEMP_GRANT);
            playerTitleService.runSync(() -> {
                if (result == PurchaseResult.SUCCESS) {
                    long expiresAt = Instant.now().toEpochMilli() + duration.millis();
                    DateTimeFormatter formatter = resolveExpireFormatter();
                    messageService.send(sender, "granttemp-success", Map.of(
                            "player", safeName(target),
                            "title", displayTitle(args[2]),
                            "duration", formatDuration(duration.millis()),
                            "expire_at", formatter.format(Instant.ofEpochMilli(expiresAt))
                    ));
                } else {
                    messageService.send(sender, "give-failed", Map.of("reason", messageService.raw(purchaseReasonKey(result))));
                }
            });
        });
    }

    private void handleClearActive(CommandSender sender, String[] args) {
        if (!sender.hasPermission("extracttitles.admin.clearactive")) {
            messageService.send(sender, "no-permission");
            return;
        }
        if (args.length < 2) {
            messageService.send(sender, "usage-clearactive");
            return;
        }
        OfflinePlayer target = resolveOffline(args[1]);
        if (target == null) {
            messageService.send(sender, "player-not-found", Map.of("player", args[1]));
            return;
        }
        playerTitleService.load(target.getUniqueId()).thenRun(() -> {
            Player online = Bukkit.getPlayer(target.getUniqueId());
            if (online != null) {
                activationService.deactivate(online);
            } else {
                playerTitleService.clearActive(target.getUniqueId());
                playerTitleService.save(target.getUniqueId());
            }
            playerTitleService.runSync(() -> messageService.send(sender, "clearactive-success", Map.of("player", safeName(target))));
        });
    }

    private void handleDebug(CommandSender sender, String[] args) {
        if (!sender.hasPermission("extracttitles.admin.debug")) {
            messageService.send(sender, "no-permission");
            return;
        }
        if (args.length == 1) {
            messageService.send(sender, "debug", Map.of("loaded", String.valueOf(titleService.all().size())));
            return;
        }
        OfflinePlayer target = resolveOffline(args[1]);
        if (target == null) {
            messageService.send(sender, "player-not-found", Map.of("player", args[1]));
            return;
        }
        playerTitleService.load(target.getUniqueId()).thenAccept(profile -> playerTitleService.runSync(() -> {
            String noneWord = messageService.rawOrDefault("none-word", "none");
            messageService.send(sender, "debug-player", Map.of(
                    "player", safeName(target),
                    "owned", String.valueOf(profile.ownerships().size()),
                    "active", profile.activeTitleId() == null ? noneWord : profile.activeTitleId()
            ));
        }));
    }

    
    private String displayTitle(String id) {
        if (id == null || id.isBlank()) {
            return id == null ? "" : id;
        }
        return titleService.find(id).map(t -> t.displayName()).orElse(id);
    }
    private String safeName(OfflinePlayer player) {
        return player.getName() == null ? player.getUniqueId().toString() : player.getName();
    }

    private OfflinePlayer resolveOffline(String input) {
        try {
            UUID uuid = UUID.fromString(input);
            return Bukkit.getOfflinePlayer(uuid);
        } catch (IllegalArgumentException ignored) {
            OfflinePlayer candidate = Bukkit.getOfflinePlayer(input);
            if (candidate.getName() == null && !candidate.hasPlayedBefore()) {
                return null;
            }
            return candidate;
        }
    }

    private void sendHelp(CommandSender sender) {
        boolean admin = hasAnyAdminPermission(sender);
        List<String> lines = messageService.list(admin ? "help-admin" : "help-user");
        if (lines.isEmpty()) {
            lines = admin
                    ? List.of(
                    "<gradient:#00c6ff:#0072ff><bold>ExtractTitles - Admin Commands</bold></gradient>",
                    "<#8be9fd>/extracttitles list</#8be9fd>",
                    "<#8be9fd>/extracttitles reload</#8be9fd>",
                    "<#8be9fd>/extracttitles give <player> <id></#8be9fd>",
                    "<#8be9fd>/extracttitles remove <player> <id></#8be9fd>",
                    "<#8be9fd>/extracttitles granttemp <player> <id> <1d></#8be9fd>",
                    "<#8be9fd>/extracttitles clearactive <player></#8be9fd>",
                    "<#8be9fd>/extracttitles debug [player]</#8be9fd>"
            ) : List.of(
                    "<gradient:#00c6ff:#0072ff><bold>ExtractTitles - Player Commands</bold></gradient>",
                    "<#8be9fd>/titles</#8be9fd>",
                    "<#8be9fd>/extracttitles buy <id></#8be9fd>",
                    "<#8be9fd>/extracttitles info <id></#8be9fd>",
                    "<#8be9fd>/extracttitles activate <id></#8be9fd>",
                    "<#8be9fd>/extracttitles deactivate</#8be9fd>"
            );
        }
        for (String line : lines) {
            sender.sendMessage(Text.parse(line));
        }
    }

    private DateTimeFormatter resolveExpireFormatter() {
        String configured = messageService.rawOrDefault("expire-time-format", DEFAULT_EXPIRE_FORMAT);
        try {
            return DateTimeFormatter.ofPattern(configured).withZone(ZoneId.systemDefault());
        } catch (IllegalArgumentException ignored) {
            return DateTimeFormatter.ofPattern(DEFAULT_EXPIRE_FORMAT).withZone(ZoneId.systemDefault());
        }
    }

    private boolean hasAnyAdminPermission(CommandSender sender) {
        return sender.hasPermission("extracttitles.admin.list")
                || sender.hasPermission("extracttitles.admin.edit")
                || sender.hasPermission("extracttitles.admin.give")
                || sender.hasPermission("extracttitles.admin.remove")
                || sender.hasPermission("extracttitles.admin.granttemp")
                || sender.hasPermission("extracttitles.admin.clearactive")
                || sender.hasPermission("extracttitles.admin.debug");
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

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (command.getName().equalsIgnoreCase("titles")) {
            return List.of();
        }
        if (args.length == 1) {
            return ROOT.stream()
                    .filter(sub -> isSubcommandVisible(sender, sub))
                    .filter(v -> v.startsWith(args[0].toLowerCase(Locale.ROOT)))
                    .toList();
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        if (args.length == 2 && List.of("activate", "buy", "info").contains(sub)) {
            return titleService.all().stream().map(t -> t.id()).sorted().filter(v -> v.startsWith(args[1].toLowerCase(Locale.ROOT))).toList();
        }
        if (args.length == 2 && List.of("give", "remove", "granttemp", "clearactive", "debug").contains(sub)) {
            return Bukkit.getOnlinePlayers().stream().map(Player::getName).filter(v -> v.toLowerCase(Locale.ROOT).startsWith(args[1].toLowerCase(Locale.ROOT))).toList();
        }
        if (args.length == 3 && List.of("give", "remove", "granttemp").contains(sub)) {
            return titleService.all().stream().map(t -> t.id()).sorted().filter(v -> v.startsWith(args[2].toLowerCase(Locale.ROOT))).toList();
        }
        if (args.length == 4 && sub.equals("granttemp")) {
            return Arrays.asList("1h", "12h", "1d", "7d", "30d").stream().filter(v -> v.startsWith(args[3].toLowerCase(Locale.ROOT))).toList();
        }
        return new ArrayList<>();
    }

    private boolean isSubcommandVisible(CommandSender sender, String sub) {
        return switch (sub) {
            case "list" -> sender.hasPermission("extracttitles.admin.list");
            case "reload" -> sender.hasPermission("extracttitles.admin.edit");
            case "give" -> sender.hasPermission("extracttitles.admin.give");
            case "remove" -> sender.hasPermission("extracttitles.admin.remove");
            case "granttemp" -> sender.hasPermission("extracttitles.admin.granttemp");
            case "clearactive" -> sender.hasPermission("extracttitles.admin.clearactive");
            case "debug" -> sender.hasPermission("extracttitles.admin.debug");
            default -> true;
        };
    }
}