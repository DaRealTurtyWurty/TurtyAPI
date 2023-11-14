package dev.turtywurty.turtyapi.games;

import com.api.igdb.apicalypse.APICalypse;
import com.api.igdb.apicalypse.Sort;
import com.api.igdb.exceptions.RequestException;
import com.api.igdb.request.IGDBWrapper;
import com.api.igdb.request.JsonRequestKt;
import com.api.igdb.request.TwitchAuthenticator;
import com.api.igdb.utils.ImageBuilderKt;
import com.api.igdb.utils.TwitchToken;
import dev.turtywurty.turtyapi.Constants;
import dev.turtywurty.turtyapi.TurtyAPI;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

@Getter
public class IGDBConnector {
    private static final ScheduledExecutorService EXECUTOR_SERVICE = Executors.newSingleThreadScheduledExecutor();
    public static final IGDBConnector INSTANCE = new IGDBConnector();

    private final IGDBWrapper wrapper = IGDBWrapper.INSTANCE;
    private final AtomicReference<TwitchToken> twitchToken = new AtomicReference<>(
            TwitchAuthenticator.INSTANCE.requestTwitchToken(
                    TurtyAPI.getTwitchClientId(),
                    TurtyAPI.getTwitchClientSecret()
            ));

    private IGDBConnector() {
        scheduleRefresh();

        Constants.LOGGER.info("Successfully connected to IGDB with client ID: " + TurtyAPI.getTwitchClientId() + ", client secret: " + TurtyAPI.getTwitchClientSecret() + " and access token: " + this.twitchToken.get().getAccess_token());
    }

    private void scheduleRefresh() {
        EXECUTOR_SERVICE.schedule(
                () -> {
                    this.twitchToken.set(TwitchAuthenticator.INSTANCE.requestTwitchToken(
                            TurtyAPI.getTwitchClientId(),
                            TurtyAPI.getTwitchClientSecret()
                    ));

                    this.wrapper.setCredentials(
                            TurtyAPI.getTwitchClientId(),
                            this.twitchToken.get().getAccess_token()
                    );

                    scheduleRefresh();
                },
                this.twitchToken.get().getExpiresUnix() - 1000,
                TimeUnit.MILLISECONDS
        );
    }

    public @Nullable String searchGames(@NotNull String query, int limit) {
        var apiCalypse = new APICalypse()
                .fields("age_ratings,aggregated_rating,aggregated_rating_count,alternative_names,artworks,bundles,category,checksum,collection,cover,created_at,dlcs,expansions,external_games,first_release_date,follows,franchise,franchises,game_engines,game_modes,genres,hypes,involved_companies,keywords,multiplayer_modes,name,parent_game,platforms,player_perspectives,rating,rating_count,release_dates,screenshots,similar_games,slug,standalone_expansions,status,storyline,summary,tags,themes,total_rating,total_rating_count,updated_at,url,version_parent,version_title,videos,websites")
                .search(query)
                .limit(limit);

        try {
            return JsonRequestKt.jsonGames(this.wrapper, apiCalypse);
        } catch (RequestException exception) {
            Constants.LOGGER.error("Failed to search for games!", exception);
            return null;
        }
    }

    public @Nullable String searchGames(@NotNull String query) {
        return searchGames(query, 10);
    }
}
