package ru.lewhu.extracttitles.hook.placeholder;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import ru.lewhu.extracttitles.domain.player.PlayerTitleProfile;
import ru.lewhu.extracttitles.domain.player.TitleOwnership;
import ru.lewhu.extracttitles.domain.title.TitleDefinition;
import ru.lewhu.extracttitles.service.PlayerTitleService;
import ru.lewhu.extracttitles.service.TitleService;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

public final class ExtractTitlesExpansion extends PlaceholderExpansion {
    private final TitleService titleService;
    private final PlayerTitleService playerTitleService;
    private final Map<String, Function<OfflinePlayer, String>> staticResolvers;

    public ExtractTitlesExpansion(TitleService titleService, PlayerTitleService playerTitleService) {
        this.titleService = titleService;
        this.playerTitleService = playerTitleService;
        this.staticResolvers = Map.of(
                "active_title", this::activeVisible,
                "active_title_name", this::activeName,
                "active_title_raw", this::activeRaw,
                "owned_count", p -> String.valueOf(playerTitleService.ownedCount(p.getUniqueId())),
                "total_count", p -> String.valueOf(titleService.all().size()),
                "active_title_or_none", this::activeOrNone,
                "active_title_or_blank", this::activeOrBlank
        );
    }

    @Override
    public String getIdentifier() {
        return "extracttitles";
    }

    @Override
    public String getAuthor() {
        return "mugnest";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, String params) {
        if (player == null || params == null || params.isBlank()) return "";

        Function<OfflinePlayer, String> staticResolver = staticResolvers.get(params.toLowerCase());
        if (staticResolver != null) {
            return safe(staticResolver.apply(player));
        }

        PlayerTitleProfile profile = playerTitleService.cachedOrCreate(player.getUniqueId());

        if (params.startsWith("has_title_")) {
            String id = params.substring("has_title_".length());
            return String.valueOf(playerTitleService.hasTitle(player.getUniqueId(), id));
        }
        if (params.startsWith("title_expiry_")) {
            String id = params.substring("title_expiry_".length());
            TitleOwnership own = profile.ownership(id);
            if (own == null) return "none";
            if (own.permanent()) return "permanent";
            return String.valueOf(own.expiresAt());
        }
        if (params.startsWith("title_remaining_")) {
            String id = params.substring("title_remaining_".length());
            long rem = playerTitleService.remaining(player.getUniqueId(), id);
            if (rem < 0) return "none";
            if (rem == Long.MAX_VALUE) return "permanent";
            return String.valueOf(rem / 1000);
        }
        return "";
    }

    private String activeVisible(OfflinePlayer p) {
        return activeTitle(p).map(TitleDefinition::visibleFormat).orElse("");
    }

    private String activeName(OfflinePlayer p) {
        return activeTitle(p).map(TitleDefinition::displayName).orElse("");
    }

    private String activeRaw(OfflinePlayer p) {
        return activeTitle(p).map(TitleDefinition::rawFormat).orElse("");
    }

    private String activeOrNone(OfflinePlayer p) {
        String v = activeVisible(p);
        return v.isBlank() ? "none" : v;
    }

    private String activeOrBlank(OfflinePlayer p) {
        return activeVisible(p);
    }

    private Optional<TitleDefinition> activeTitle(OfflinePlayer player) {
        PlayerTitleProfile profile = playerTitleService.cachedOrCreate(player.getUniqueId());
        String activeId = profile.activeTitleId();
        if (activeId == null || activeId.isBlank()) return Optional.empty();
        if (!playerTitleService.hasTitle(player.getUniqueId(), activeId)) return Optional.empty();
        return titleService.find(activeId);
    }

    private String safe(String v) {
        return v == null ? "" : v;
    }
}
