package dev.turtywurty.turtyapi.games;

import com.api.igdb.apicalypse.APICalypse;
import com.api.igdb.exceptions.RequestException;
import com.api.igdb.request.IGDBWrapper;
import com.api.igdb.request.JsonRequestKt;
import com.api.igdb.request.TwitchAuthenticator;
import com.api.igdb.utils.TwitchToken;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import dev.turtywurty.turtyapi.Constants;
import dev.turtywurty.turtyapi.TurtyAPI;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
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

    public static void init() {
        Constants.LOGGER.info("Loaded IGDB!");
    }

    public @Nullable List<Game> searchGames(@NotNull String query, int limit, String... fields) {
        String fieldsString = String.join(",", fields);
        if (fieldsString.isBlank() || fieldsString.equals("null")) {
            fieldsString = "*";
        }

        var apiCalypse = new APICalypse()
                .fields(fieldsString)
                .search(query)
                .limit(limit);

        try {
            String jsonString = JsonRequestKt.jsonGames(this.wrapper, apiCalypse);
            JsonArray array = Constants.GSON.fromJson(jsonString, JsonArray.class);

            List<Game> games = new ArrayList<>();
            for (JsonElement element : array) {
                games.add(Constants.GSON.fromJson(element, Game.class));
            }

            return games;
        } catch (RequestException exception) {
            Constants.LOGGER.error("Failed to search for games!", exception);
            return null;
        }
    }

    public @Nullable List<Game> searchGames(@NotNull String query, String... fields) {
        return searchGames(query, 10, fields);
    }

    public @Nullable List<Game> searchGames(@NotNull String query) {
        return searchGames(query, "*");
    }

    public Artwork findArtwork(int id, String... fields) {
        String fieldsString = String.join(",", fields);
        if (fieldsString.isBlank() || fieldsString.equals("null")) {
            fieldsString = "*";
        }

        var apiCalypse = new APICalypse()
                .fields(fieldsString)
                .where("id = " + id);

        try {
            String jsonString = JsonRequestKt.jsonArtworks(this.wrapper, apiCalypse);
            JsonArray array = Constants.GSON.fromJson(jsonString, JsonArray.class);
            if (array.isEmpty())
                return null;

            return Constants.GSON.fromJson(array.get(0), Artwork.class);
        } catch (RequestException exception) {
            Constants.LOGGER.error("Failed to search for games!", exception);
            return null;
        }
    }

    public Cover findCover(int id, String... fields) {
        String fieldsString = String.join(",", fields);
        if (fieldsString.isBlank() || fieldsString.equals("null")) {
            fieldsString = "*";
        }

        var apiCalypse = new APICalypse()
                .fields(fieldsString)
                .where("id = " + id);

        try {
            String jsonString = JsonRequestKt.jsonCovers(this.wrapper, apiCalypse);
            JsonArray array = Constants.GSON.fromJson(jsonString, JsonArray.class);
            if (array.isEmpty())
                return null;

            return Constants.GSON.fromJson(array.get(0), Cover.class);
        } catch (RequestException exception) {
            Constants.LOGGER.error("Failed to search for games!", exception);
            return null;
        }
    }

    public GamePlatform findPlatform(int id, String... fields) {
        String fieldsString = String.join(",", fields);
        if (fieldsString.isBlank() || fieldsString.equals("null")) {
            fieldsString = "*";
        }

        var apiCalypse = new APICalypse()
                .fields(fieldsString)
                .where("id = " + id);

        try {
            String jsonString = JsonRequestKt.jsonPlatforms(IGDBWrapper.INSTANCE, apiCalypse);
            JsonArray array = Constants.GSON.fromJson(jsonString, JsonArray.class);
            if (array.isEmpty())
                return null;

            return Constants.GSON.fromJson(array.get(0), GamePlatform.class);
        } catch (RequestException exception) {
            Constants.LOGGER.error("Failed to search for games!", exception);
            return null;
        }
    }
}
