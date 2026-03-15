package ru.lewhu.extracttitles;

import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import ru.lewhu.extracttitles.command.ExtractTitlesCommand;
import ru.lewhu.extracttitles.config.ConfigService;
import ru.lewhu.extracttitles.gui.TitleMenuService;
import ru.lewhu.extracttitles.hook.economy.CurrencyService;
import ru.lewhu.extracttitles.hook.economy.PlayerPointsCurrencyProvider;
import ru.lewhu.extracttitles.hook.economy.VaultCurrencyProvider;
import ru.lewhu.extracttitles.hook.placeholder.ExtractTitlesExpansion;
import ru.lewhu.extracttitles.listener.MenuListener;
import ru.lewhu.extracttitles.listener.MiningListener;
import ru.lewhu.extracttitles.listener.PlayerSessionListener;
import ru.lewhu.extracttitles.service.ExpiryValidationService;
import ru.lewhu.extracttitles.service.MessageService;
import ru.lewhu.extracttitles.service.PermissionGrantService;
import ru.lewhu.extracttitles.service.PlaceholderService;
import ru.lewhu.extracttitles.service.PlayerTitleService;
import ru.lewhu.extracttitles.service.PurchaseService;
import ru.lewhu.extracttitles.service.TitleActivationService;
import ru.lewhu.extracttitles.service.TitleService;
import ru.lewhu.extracttitles.service.effect.EffectService;
import ru.lewhu.extracttitles.storage.mysql.MySqlPlayerTitleRepository;
import ru.lewhu.extracttitles.storage.mysql.MySqlStorage;
import ru.lewhu.extracttitles.storage.repository.PlayerTitleRepository;
import ru.lewhu.extracttitles.storage.repository.TitleDefinitionRepository;
import ru.lewhu.extracttitles.storage.sqlite.SQLitePlayerTitleRepository;
import ru.lewhu.extracttitles.storage.sqlite.SQLiteStorage;

import java.sql.SQLException;
import java.util.Locale;

public final class ExtractTitlesPlugin extends JavaPlugin {
    private ConfigService configService;
    private SQLiteStorage sqliteStorage;
    private MySqlStorage mySqlStorage;
    private PlayerTitleRepository playerRepository;
    private TitleDefinitionRepository titleRepository;

    private TitleService titleService;
    private PlayerTitleService playerTitleService;
    private PurchaseService purchaseService;
    private TitleActivationService activationService;
    private EffectService effectService;
    private ExpiryValidationService expiryValidationService;
    private TitleMenuService menuService;
    private MessageService messageService;
    private PlaceholderService placeholderService;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        configService = new ConfigService(this);
        configService.loadAll();
        placeholderService = new PlaceholderService(this);
        messageService = new MessageService(configService, placeholderService);

        try {
            initStorage();
        } catch (SQLException e) {
            getLogger().severe("Failed to initialize storage: " + e.getMessage());
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        titleRepository = new TitleDefinitionRepository(configService);
        titleService = new TitleService(titleRepository);
        titleService.load();

        playerTitleService = new PlayerTitleService(this, playerRepository);

        var vaultProvider = new VaultCurrencyProvider(this);
        var ppProvider = new PlayerPointsCurrencyProvider();
        CurrencyService currencyService = new CurrencyService(vaultProvider, ppProvider);

        purchaseService = new PurchaseService(titleService, playerTitleService, currencyService, configService);
        PermissionGrantService permissionGrantService = new PermissionGrantService(this);
        effectService = new EffectService(this, titleService, permissionGrantService);
        activationService = new TitleActivationService(titleService, playerTitleService, effectService);

        menuService = new TitleMenuService(this, configService, titleService, playerTitleService, purchaseService, activationService, messageService, placeholderService);
        expiryValidationService = new ExpiryValidationService(this, playerTitleService, effectService, messageService);

        registerCommands();
        registerListeners();

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new ExtractTitlesExpansion(titleService, playerTitleService).register();
            getLogger().info("PlaceholderAPI expansion registered.");
        }

        for (Player online : Bukkit.getOnlinePlayers()) {
            playerTitleService.load(online.getUniqueId()).thenRun(() -> playerTitleService.runSync(() -> {
                expiryValidationService.validate(online, false);
                String active = playerTitleService.getActive(online.getUniqueId());
                if (active != null && playerTitleService.hasTitle(online.getUniqueId(), active)) {
                    effectService.reapplyFor(online, active);
                }
            }));
        }

        effectService.startTicker();
        expiryValidationService.start();
        getLogger().info("ExtractTitles enabled.");
    }

    private void initStorage() throws SQLException {
        String type = configService.storage().getString("storage.type", "SQLITE").toUpperCase(Locale.ROOT);
        if ("MYSQL".equals(type)) {
            try {
                mySqlStorage = new MySqlStorage(
                        this,
                        configService.storage().getString("storage.mysql.host", "127.0.0.1"),
                        configService.storage().getInt("storage.mysql.port", 3306),
                        configService.storage().getString("storage.mysql.database", "extracttitles"),
                        configService.storage().getString("storage.mysql.username", "root"),
                        configService.storage().getString("storage.mysql.password", "password")
                );
                mySqlStorage.init();
                playerRepository = new MySqlPlayerTitleRepository(mySqlStorage);
                getLogger().info("Using MySQL storage.");
                return;
            } catch (SQLException ex) {
                getLogger().warning("MySQL unavailable, fallback to SQLite: " + ex.getMessage());
                if (mySqlStorage != null) {
                    mySqlStorage.close();
                    mySqlStorage = null;
                }
            }
        }

        sqliteStorage = new SQLiteStorage(this, configService.storage().getString("storage.sqlite.file", "extracttitles.db"));
        sqliteStorage.init();
        playerRepository = new SQLitePlayerTitleRepository(sqliteStorage);
        getLogger().info("Using SQLite storage.");
    }

    @Override
    public void onDisable() {
        if (playerTitleService != null) {
            playerTitleService.saveAll().join();
        }
        if (playerRepository != null) {
            playerRepository.shutdown();
        }
        if (sqliteStorage != null) {
            sqliteStorage.close();
        }
        if (mySqlStorage != null) {
            mySqlStorage.close();
        }
    }

    private void registerCommands() {
        ExtractTitlesCommand handler = new ExtractTitlesCommand(
                titleService,
                playerTitleService,
                purchaseService,
                activationService,
                menuService,
                messageService,
                this::reloadPlugin
        );

        PluginCommand root = getCommand("extracttitles");
        if (root != null) {
            root.setExecutor(handler);
            root.setTabCompleter(handler);
        }
        PluginCommand alias = getCommand("titles");
        if (alias != null) {
            alias.setExecutor(handler);
            alias.setTabCompleter(handler);
        }
    }

    private void registerListeners() {
        Bukkit.getPluginManager().registerEvents(new PlayerSessionListener(playerTitleService, effectService, messageService), this);
        Bukkit.getPluginManager().registerEvents(new MenuListener(menuService), this);
        Bukkit.getPluginManager().registerEvents(new MiningListener(effectService.mining3x3Effect()), this);
    }

    private void reloadPlugin() {
        configService.reloadAll();
        titleService.load();
        for (Player player : Bukkit.getOnlinePlayers()) {
            expiryValidationService.validate(player, false);
            String active = playerTitleService.getActive(player.getUniqueId());
            if (active == null) {
                effectService.clear(player);
            } else if (playerTitleService.hasTitle(player.getUniqueId(), active)) {
                effectService.reapplyFor(player, active);
            } else {
                effectService.clear(player);
                playerTitleService.clearActive(player.getUniqueId());
            }
        }
    }
}
