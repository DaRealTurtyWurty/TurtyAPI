package dev.turtywurty.turtyapi.games;

import com.api.igdb.apicalypse.APICalypse;
import com.api.igdb.apicalypse.Sort;
import com.api.igdb.exceptions.RequestException;
import com.api.igdb.request.IGDBWrapper;
import com.api.igdb.request.JsonRequestKt;
import com.api.igdb.request.TwitchAuthenticator;
import com.api.igdb.utils.TwitchToken;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import dev.turtywurty.turtyapi.Constants;
import dev.turtywurty.turtyapi.TurtyAPI;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

@Getter
public class IGDBConnector {
    private static final long DEFAULT_CACHE_TTL_SECONDS = 900;
    private static final int DEFAULT_CACHE_MAX_ENTRIES = 1_000;
    private static final ScheduledExecutorService EXECUTOR_SERVICE = Executors.newSingleThreadScheduledExecutor();
    public static final IGDBConnector INSTANCE = new IGDBConnector();

    private final IGDBWrapper wrapper = IGDBWrapper.INSTANCE;
    private final IGDBResponseCache responseCache = new IGDBResponseCache(
            environmentInt("IGDB_CACHE_MAX_ENTRIES", DEFAULT_CACHE_MAX_ENTRIES),
            Duration.ofSeconds(environmentLong("IGDB_CACHE_TTL_SECONDS", DEFAULT_CACHE_TTL_SECONDS))
    );
    private final AtomicReference<TwitchToken> twitchToken = new AtomicReference<>(
            TwitchAuthenticator.INSTANCE.requestTwitchToken(
                    TurtyAPI.getTwitchClientId(),
                    TurtyAPI.getTwitchClientSecret()
            ));

    private IGDBConnector() {
        TwitchToken token = this.twitchToken.get();

        this.wrapper.setCredentials(
                TurtyAPI.getTwitchClientId(),
                token.getAccess_token()
        );

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
                .where("name ~ *\"" + query + "\"*")
                .sort("rating", Sort.DESCENDING)
                .limit(limit);

        try {
            String cacheKey = cacheKey("games/search", query, limit, fieldsString);
            String jsonString = this.responseCache.getOrLoad(
                    cacheKey,
                    () -> JsonRequestKt.jsonGames(this.wrapper, apiCalypse)
            );
            JsonArray array = Constants.GSON.fromJson(jsonString, JsonArray.class);

            List<Game> games = new ArrayList<>();
            for (JsonElement element : array) {
                games.add(Constants.GSON.fromJson(element, Game.class));
            }

            return games;
        } catch (RequestException exception) {
            throw IGDBRequestException.requestFailed("searching for games", exception);
        } catch (JsonParseException exception) {
            throw IGDBRequestException.invalidResponse("searching for games", exception);
        }
    }

    public @Nullable List<Game> searchGames(@NotNull String query, String... fields) {
        return searchGames(query, 100, fields);
    }

    public @Nullable List<Game> searchGames(@NotNull String query) {
        return searchGames(query, "*");
    }

    public @Nullable Integer findGameIdFromExternalId(
            @NotNull ExternalGameSource source,
            @NotNull String externalId
    ) {
        String escapedExternalId = externalId
                .replace("\\", "\\\\")
                .replace("\"", "\\\"");

        var apiCalypse = new APICalypse()
                .fields("game,uid,name,url,external_game_source")
                .where("external_game_source = " + source.getId() + " & uid = \"" + escapedExternalId + "\"")
                .limit(1);

        try {
            String cacheKey = cacheKey("external-games/source", source.getId(), externalId);
            String jsonString = this.responseCache.getOrLoad(
                    cacheKey,
                    () -> JsonRequestKt.jsonExternalGames(this.wrapper, apiCalypse)
            );
            JsonArray array = Constants.GSON.fromJson(jsonString, JsonArray.class);

            if (array.isEmpty())
                return null;

            JsonObject obj = array.get(0).getAsJsonObject();

            if (!obj.has("game"))
                return null;

            return obj.get("game").getAsInt();
        } catch (RequestException exception) {
            throw IGDBRequestException.requestFailed(
                    "finding a game from the " + source.getName() + " external id",
                    exception
            );
        } catch (JsonParseException exception) {
            throw IGDBRequestException.invalidResponse(
                    "finding a game from the " + source.getName() + " external id",
                    exception
            );
        }
    }

    public @Nullable Game findGameById(int id, String... fields) {
        String fieldsString = String.join(",", fields);
        if (fieldsString.isBlank()) {
            fieldsString = "*";
        }

        var apiCalypse = new APICalypse()
                .fields(fieldsString)
                .where("id = " + id)
                .limit(1);

        try {
            String cacheKey = cacheKey("games/id", id, fieldsString);
            String jsonString = this.responseCache.getOrLoad(
                    cacheKey,
                    () -> JsonRequestKt.jsonGames(this.wrapper, apiCalypse)
            );
            JsonArray array = Constants.GSON.fromJson(jsonString, JsonArray.class);

            if (array.isEmpty())
                return null;

            return Constants.GSON.fromJson(array.get(0), Game.class);
        } catch (RequestException exception) {
            throw IGDBRequestException.requestFailed("finding a game", exception);
        } catch (JsonParseException exception) {
            throw IGDBRequestException.invalidResponse("finding a game", exception);
        }
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
            String cacheKey = cacheKey("artworks/id", id, fieldsString);
            String jsonString = this.responseCache.getOrLoad(
                    cacheKey,
                    () -> JsonRequestKt.jsonArtworks(this.wrapper, apiCalypse)
            );
            JsonArray array = Constants.GSON.fromJson(jsonString, JsonArray.class);
            if (array.isEmpty())
                return null;

            return Constants.GSON.fromJson(array.get(0), Artwork.class);
        } catch (RequestException exception) {
            throw IGDBRequestException.requestFailed("finding artwork", exception);
        } catch (JsonParseException exception) {
            throw IGDBRequestException.invalidResponse("finding artwork", exception);
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
            String cacheKey = cacheKey("covers/id", id, fieldsString);
            String jsonString = this.responseCache.getOrLoad(
                    cacheKey,
                    () -> JsonRequestKt.jsonCovers(this.wrapper, apiCalypse)
            );
            JsonArray array = Constants.GSON.fromJson(jsonString, JsonArray.class);
            if (array.isEmpty())
                return null;

            return Constants.GSON.fromJson(array.get(0), Cover.class);
        } catch (RequestException exception) {
            throw IGDBRequestException.requestFailed("finding a cover", exception);
        } catch (JsonParseException exception) {
            throw IGDBRequestException.invalidResponse("finding a cover", exception);
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
            String cacheKey = cacheKey("platforms/id", id, fieldsString);
            String jsonString = this.responseCache.getOrLoad(
                    cacheKey,
                    () -> JsonRequestKt.jsonPlatforms(this.wrapper, apiCalypse)
            );
            JsonArray array = Constants.GSON.fromJson(jsonString, JsonArray.class);
            if (array.isEmpty())
                return null;

            return Constants.GSON.fromJson(array.get(0), GamePlatform.class);
        } catch (RequestException exception) {
            throw IGDBRequestException.requestFailed("finding a platform", exception);
        } catch (JsonParseException exception) {
            throw IGDBRequestException.invalidResponse("finding a platform", exception);
        }
    }

    public void clearCache() {
        this.responseCache.clear();
    }

    public CacheStats getCacheStats() {
        IGDBResponseCache.CacheStats stats = this.responseCache.stats();
        return new CacheStats(stats.hits(), stats.misses(), stats.evictions(), stats.size());
    }

    private static String cacheKey(String endpoint, Object... parts) {
        var builder = new StringBuilder(endpoint);
        for (Object part : parts) {
            String value = String.valueOf(part);
            builder.append('|').append(value.length()).append(':').append(value);
        }

        return builder.toString();
    }

    private static int environmentInt(String key, int defaultValue) {
        long value = environmentLong(key, defaultValue);
        if (value < 0 || value > Integer.MAX_VALUE) {
            Constants.LOGGER.warn("{} must be between 0 and {}; using {}", key, Integer.MAX_VALUE, defaultValue);
            return defaultValue;
        }

        return (int) value;
    }

    private static long environmentLong(String key, long defaultValue) {
        String configuredValue = TurtyAPI.getEnvironmentValue(key).orElse(null);
        if (configuredValue == null || configuredValue.isBlank()) {
            return defaultValue;
        }

        try {
            return Long.parseLong(configuredValue);
        } catch (NumberFormatException exception) {
            Constants.LOGGER.warn("{} must be an integer; using {}", key, defaultValue);
            return defaultValue;
        }
    }

    public record CacheStats(long hits, long misses, long evictions, int size) {
    }
}
