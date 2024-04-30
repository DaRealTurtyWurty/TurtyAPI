package dev.turtywurty.turtyapi;

import com.api.igdb.utils.ImageSize;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.turtywurty.turtyapi.codeguesser.Code;
import dev.turtywurty.turtyapi.codeguesser.CodeManager;
import dev.turtywurty.turtyapi.fun.CelebrityManager;
import dev.turtywurty.turtyapi.fun.WouldYouRatherManager;
import dev.turtywurty.turtyapi.games.*;
import dev.turtywurty.turtyapi.geography.CoordinatePicker;
import dev.turtywurty.turtyapi.geography.GeoguesserManager;
import dev.turtywurty.turtyapi.geography.Region;
import dev.turtywurty.turtyapi.geography.RegionManager;
import dev.turtywurty.turtyapi.image.ColorFlagGenerator;
import dev.turtywurty.turtyapi.image.FlipType;
import dev.turtywurty.turtyapi.image.ImageUtils;
import dev.turtywurty.turtyapi.image.LGBTifier;
import dev.turtywurty.turtyapi.json.JsonBuilder;
import dev.turtywurty.turtyapi.minecraft.*;
import dev.turtywurty.turtyapi.nsfw.NSFWManager;
import dev.turtywurty.turtyapi.nsfw.Photo;
import dev.turtywurty.turtyapi.nsfw.Pornstar;
import dev.turtywurty.turtyapi.words.WordManager;
import io.javalin.Javalin;
import io.javalin.http.ContentType;
import io.javalin.http.HttpStatus;
import io.javalin.http.util.NaiveRateLimit;
import io.javalin.json.JsonMapper;
import kotlin.Pair;
import org.jetbrains.annotations.NotNull;
import org.locationtech.jts.geom.Coordinate;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class RouteManager {
    private static Javalin app;

    public static void init() {
        if (RouteManager.app != null) {
            throw new IllegalStateException("RouteManager has already been initialized!");
        }

        var gsonMapper = new JsonMapper() {
            @NotNull
            @Override
            public <T> T fromJsonString(@NotNull String json, @NotNull Type targetType) {
                return Constants.GSON.fromJson(json, targetType);
            }

            @NotNull
            @Override
            public String toJsonString(@NotNull Object obj, @NotNull Type type) {
                return Constants.GSON.toJson(obj, type);
            }
        };

        MinecraftVersions.init();
        ForgeVersions.init();
        NeoforgeVersions.init();
        FabricVersions.init();
        QuiltVersions.init();
        ParchmentVersions.init();
        CodeManager.init();
        CelebrityManager.init();

        Javalin app = Javalin.create(ctx -> ctx.jsonMapper(gsonMapper));

        app.get("/", ctx -> ctx.result("Hello World!"));

        app.get("/geo/flag", ctx -> {
            // check for api key
            String apiKey = ctx.queryParam("apiKey");
            if (apiKey == null || apiKey.isBlank()) {
                ctx.status(HttpStatus.UNAUTHORIZED).result("You must specify an API key!");
                return;
            }

            // check for valid api key
            if (!apiKey.equals(TurtyAPI.getAPIKey())) {
                ctx.status(HttpStatus.UNAUTHORIZED).result("Invalid API key!");
                return;
            }

            NaiveRateLimit.requestPerTimeUnit(ctx, 10, TimeUnit.SECONDS);

            try {
                String cca3 = ctx.queryParam("cca3");
                if (cca3 == null) {
                    ctx.status(HttpStatus.BAD_REQUEST).result("Missing cca3 query parameter!");
                    return;
                }

                Region data = RegionManager.getRegion(cca3);
                Constants.LOGGER.debug("Sending flag for {}!", data);

                InputStream imageStream = TurtyAPI.getResource("geography/flags/" + data.getFlag());
                ctx.contentType(ContentType.IMAGE_PNG).result(imageStream.readAllBytes());
                imageStream.close();
            } catch (IOException | NullPointerException exception) {
                ctx.status(HttpStatus.NOT_FOUND).result("Failed to find region!");
            }
        });

        app.get("/geo/flag/random", ctx -> {
            // check for api key
            String apiKey = ctx.queryParam("apiKey");
            if (apiKey == null || apiKey.isBlank()) {
                ctx.status(HttpStatus.UNAUTHORIZED).result("You must specify an API key!");
                return;
            }

            // check for valid api key
            if (!apiKey.equals(TurtyAPI.getAPIKey())) {
                ctx.status(HttpStatus.UNAUTHORIZED).result("Invalid API key!");
                return;
            }

            NaiveRateLimit.requestPerTimeUnit(ctx, 10, TimeUnit.SECONDS);

            try {
                String toExclude = ctx.queryParam("exclude");
                String[] parts = toExclude == null ? new String[0] : (toExclude.split(",").length == 0 ? new String[]{toExclude} : toExclude.split(","));
                List<String> exclude = Arrays.stream(toExclude == null ? new String[0] : parts)
                        .map(String::toLowerCase)
                        .map(String::trim)
                        .toList();

                boolean excludeTerritories = exclude.contains("territories");
                boolean excludeIslands = exclude.contains("islands");
                boolean excludeCountries = exclude.contains("countries");
                boolean excludeMainland = exclude.contains("mainland");

                Region data = RegionManager.getRandomRegion(excludeTerritories, excludeIslands, excludeCountries, excludeMainland);
                Constants.LOGGER.debug("Sending flag for {}!", data);

                InputStream imageStream = TurtyAPI.getResource("geography/flags/" + data.getFlag());
                BufferedImage image = ImageIO.read(imageStream);
                String base64 = ImageUtils.toBase64(image);
                imageStream.close();

                ctx.contentType(ContentType.JSON).result(new JsonBuilder.ObjectBuilder().add("region", data).add("image", base64).toJson());
            } catch (IOException | NullPointerException exception) {
                ctx.status(HttpStatus.NOT_FOUND).result("Failed to find region!");
            }
        });

        app.get("/geo/outline", ctx -> {
            // check for api key
            String apiKey = ctx.queryParam("apiKey");
            if (apiKey == null || apiKey.isBlank()) {
                ctx.status(HttpStatus.UNAUTHORIZED).result("You must specify an API key!");
                return;
            }

            // check for valid api key
            if (!apiKey.equals(TurtyAPI.getAPIKey())) {
                ctx.status(HttpStatus.UNAUTHORIZED).result("Invalid API key!");
                return;
            }

            NaiveRateLimit.requestPerTimeUnit(ctx, 10, TimeUnit.SECONDS);

            try {
                String cca3 = ctx.queryParam("cca3");
                if (cca3 == null) {
                    ctx.status(HttpStatus.BAD_REQUEST).result("Missing cca3 query parameter!");
                    return;
                }

                Region data = RegionManager.getRegion(cca3);
                Constants.LOGGER.debug("Sending outline for {}!", data);

                InputStream imageStream = TurtyAPI.getResource("geography/outlines/" + data.getOutline());
                ctx.contentType(ContentType.IMAGE_PNG).result(imageStream.readAllBytes());
                imageStream.close();
            } catch (IOException | NullPointerException exception) {
                ctx.status(HttpStatus.NOT_FOUND).result("Failed to find region!");
            }
        });

        app.get("/geo/outline/random", ctx -> {
            // check for api key
            String apiKey = ctx.queryParam("apiKey");
            if (apiKey == null || apiKey.isBlank()) {
                ctx.status(HttpStatus.UNAUTHORIZED).result("You must specify an API key!");
                return;
            }

            // check for valid api key
            if (!apiKey.equals(TurtyAPI.getAPIKey())) {
                ctx.status(HttpStatus.UNAUTHORIZED).result("Invalid API key!");
                return;
            }

            NaiveRateLimit.requestPerTimeUnit(ctx, 10, TimeUnit.SECONDS);

            try {
                String toExclude = ctx.queryParam("exclude");
                String[] parts = toExclude == null ? new String[0] : (toExclude.split(",").length == 0 ? new String[]{toExclude} : toExclude.split(","));
                List<String> exclude = Arrays.stream(toExclude == null ? new String[0] : parts)
                        .map(String::toLowerCase)
                        .map(String::trim)
                        .toList();

                boolean excludeTerritories = exclude.contains("territories");
                boolean excludeIslands = exclude.contains("islands");
                boolean excludeCountries = exclude.contains("countries");
                boolean excludeMainland = exclude.contains("mainland");

                Region data = RegionManager.getRandomRegion(excludeTerritories, excludeIslands, excludeCountries, excludeMainland);
                Constants.LOGGER.debug("Sending outline for {}!", data);

                InputStream imageStream = TurtyAPI.getResource("geography/outlines/" + data.getOutline());
                BufferedImage image = ImageIO.read(imageStream);
                String base64 = ImageUtils.toBase64(image);
                imageStream.close();

                ctx.contentType(ContentType.JSON).result(new JsonBuilder.ObjectBuilder().add("region", data).add("image", base64).toJson());
            } catch (IOException | NullPointerException exception) {
                ctx.status(HttpStatus.NOT_FOUND).result("Failed to find region!");
            }
        });

        app.get("/geo/data", ctx -> {
            // check for api key
            String apiKey = ctx.queryParam("apiKey");
            if (apiKey == null || apiKey.isBlank()) {
                ctx.status(HttpStatus.UNAUTHORIZED).result("You must specify an API key!");
                return;
            }

            // check for valid api key
            if (!apiKey.equals(TurtyAPI.getAPIKey())) {
                ctx.status(HttpStatus.UNAUTHORIZED).result("Invalid API key!");
                return;
            }

            NaiveRateLimit.requestPerTimeUnit(ctx, 10, TimeUnit.SECONDS);

            try {
                String cca3 = ctx.queryParam("cca3");
                if (cca3 == null) {
                    ctx.status(HttpStatus.BAD_REQUEST).result("Missing cca3 query parameter!");
                    return;
                }

                Region data = RegionManager.getRegion(cca3);
                Constants.LOGGER.debug("Sending data for {}!", data);

                ctx.contentType(ContentType.JSON).result(Constants.GSON.toJson(data));
            } catch (NullPointerException exception) {
                ctx.status(HttpStatus.NOT_FOUND).result("Failed to find region!");
            }
        });

        app.get("/geo/data/random", ctx -> {
            // check for api key
            String apiKey = ctx.queryParam("apiKey");
            if (apiKey == null || apiKey.isBlank()) {
                ctx.status(HttpStatus.UNAUTHORIZED).result("You must specify an API key!");
                return;
            }

            // check for valid api key
            if (!apiKey.equals(TurtyAPI.getAPIKey())) {
                ctx.status(HttpStatus.UNAUTHORIZED).result("Invalid API key!");
                return;
            }

            NaiveRateLimit.requestPerTimeUnit(ctx, 10, TimeUnit.SECONDS);

            try {
                String toExclude = ctx.queryParam("exclude");
                String[] parts = toExclude == null ? new String[0] : (toExclude.split(",").length == 0 ? new String[]{toExclude} : toExclude.split(","));
                List<String> exclude = Arrays.stream(toExclude == null ? new String[0] : parts)
                        .map(String::toLowerCase)
                        .map(String::trim)
                        .toList();

                boolean excludeTerritories = exclude.contains("territories");
                boolean excludeIslands = exclude.contains("islands");
                boolean excludeCountries = exclude.contains("countries");
                boolean excludeMainland = exclude.contains("mainland");

                Region data = RegionManager.getRandomRegion(excludeTerritories, excludeIslands, excludeCountries, excludeMainland);
                Constants.LOGGER.debug("Sending data for {}!", data);

                ctx.contentType(ContentType.JSON).result(Constants.GSON.toJson(data));
            } catch (NullPointerException exception) {
                ctx.status(HttpStatus.NOT_FOUND).result("Failed to find region!");
            }
        });

        app.get("/geo/data/all", ctx -> {
            // check for api key
            String apiKey = ctx.queryParam("apiKey");
            if (apiKey == null || apiKey.isBlank()) {
                ctx.status(HttpStatus.UNAUTHORIZED).result("You must specify an API key!");
                return;
            }

            // check for valid api key
            if (!apiKey.equals(TurtyAPI.getAPIKey())) {
                ctx.status(HttpStatus.UNAUTHORIZED).result("Invalid API key!");
                return;
            }

            NaiveRateLimit.requestPerTimeUnit(ctx, 10, TimeUnit.SECONDS);

            try {
                Set<String> regions = RegionManager.getRegions().keySet();

                JsonBuilder.ArrayBuilder arrayBuilder = new JsonBuilder.ArrayBuilder();
                for (String region : regions) {
                    arrayBuilder.add(RegionManager.getRegion(region));
                }

                ctx.contentType(ContentType.JSON).result(arrayBuilder.toJson());
            } catch (NullPointerException exception) {
                ctx.status(HttpStatus.NOT_FOUND).result("Failed to find region!");
            }
        });

        app.get("/words", ctx -> {
            // check for api key
            String apiKey = ctx.queryParam("apiKey");
            if (apiKey == null || apiKey.isBlank()) {
                ctx.status(HttpStatus.UNAUTHORIZED).result("You must specify an API key!");
                return;
            }

            // check for valid api key
            if (!apiKey.equals(TurtyAPI.getAPIKey())) {
                ctx.status(HttpStatus.UNAUTHORIZED).result("Invalid API key!");
                return;
            }

            NaiveRateLimit.requestPerTimeUnit(ctx, 5, TimeUnit.SECONDS);

            int length = ctx.queryParamAsClass("length", Integer.class).getOrDefault(-1);
            if (length <= 0 && length != -1) {
                ctx.status(HttpStatus.BAD_REQUEST).result("You must request a word length of at least 1!");
                return;
            }

            List<String> words = length == -1 ? WordManager.getAllWords() : WordManager.getAllWords(length);

            String startsWith = ctx.queryParamAsClass("startsWith", String.class).getOrDefault("");
            if (!startsWith.isBlank()) {
                words = WordManager.getStartingWith(words, startsWith);
            }

            int amount = ctx.queryParamAsClass("amount", Integer.class).getOrDefault(1);
            if (amount <= 0) {
                ctx.status(HttpStatus.BAD_REQUEST).result("You must request at least 1 word!");
                return;
            }
            words = WordManager.getWithMaximum(words, amount);

            var array = new JsonArray();
            words.forEach(array::add);
            ctx.contentType(ContentType.JSON).result(Constants.GSON.toJson(array));
        });

        app.get("/words/random", ctx -> {
            // check for api key
            String apiKey = ctx.queryParam("apiKey");
            if (apiKey == null || apiKey.isBlank()) {
                ctx.status(HttpStatus.UNAUTHORIZED).result("You must specify an API key!");
                return;
            }

            // check for valid api key
            if (!apiKey.equals(TurtyAPI.getAPIKey())) {
                ctx.status(HttpStatus.UNAUTHORIZED).result("Invalid API key!");
                return;
            }

            NaiveRateLimit.requestPerTimeUnit(ctx, 5, TimeUnit.SECONDS);

            int length = ctx.queryParamAsClass("length", Integer.class).getOrDefault(-1);
            if (length <= 0 && length != -1) {
                ctx.status(HttpStatus.BAD_REQUEST).result("You must request a word with a length greater than 0!");
                return;
            }

            int minLength = ctx.queryParamAsClass("minLength", Integer.class).getOrDefault(length == -1 ? 1 : length);
            int maxLength = ctx.queryParamAsClass("maxLength", Integer.class).getOrDefault(length == -1 ? 14 : length);
            if (minLength > maxLength) {
                ctx.status(HttpStatus.BAD_REQUEST).result("The minimum length must be less than or equal to the maximum length!");
                return;
            }

            List<String> words = WordManager.getAllWords(minLength, maxLength);

            String startsWith = ctx.queryParamAsClass("startsWith", String.class).getOrDefault("");
            if (!startsWith.isBlank()) {
                words = WordManager.getStartingWith(words, startsWith);
            }

            int amount = ctx.queryParamAsClass("amount", Integer.class).getOrDefault(1);
            if (amount <= 0) {
                ctx.status(HttpStatus.BAD_REQUEST).result("You must request at least 1 word!");
                return;
            }

            words = WordManager.getWithMaximum(words, amount, true);

            var array = new JsonArray();
            words.forEach(array::add);
            ctx.contentType(ContentType.JSON).result(Constants.GSON.toJson(array));
        });

        app.get("/words/validate", ctx -> {
            // check for api key
            String apiKey = ctx.queryParam("apiKey");
            if (apiKey == null || apiKey.isBlank()) {
                ctx.status(HttpStatus.UNAUTHORIZED).result("You must specify an API key!");
                return;
            }

            // check for valid api key
            if (!apiKey.equals(TurtyAPI.getAPIKey())) {
                ctx.status(HttpStatus.UNAUTHORIZED).result("Invalid API key!");
                return;
            }

            NaiveRateLimit.requestPerTimeUnit(ctx, 5, TimeUnit.SECONDS);

            String word = ctx.queryParam("word");
            if (word == null) {
                ctx.status(HttpStatus.BAD_REQUEST).result("You must specify a word!");
                return;
            }

            boolean valid = WordManager.isWord(word);
            ctx.contentType(ContentType.JSON).result(JsonBuilder.object().add("valid", valid).toJson());
        });

        app.get("/minecraft/latest", ctx -> {
            // check for api key
            String apiKey = ctx.queryParam("apiKey");
            if (apiKey == null || apiKey.isBlank()) {
                ctx.status(HttpStatus.UNAUTHORIZED).result("You must specify an API key!");
                return;
            }

            // check for valid api key
            if (!apiKey.equals(TurtyAPI.getAPIKey())) {
                ctx.status(HttpStatus.UNAUTHORIZED).result("Invalid API key!");
                return;
            }

            NaiveRateLimit.requestPerTimeUnit(ctx, 10, TimeUnit.SECONDS);

            Pair<String, String> latest = MinecraftVersions.findLatestMinecraft();
            ctx.contentType(ContentType.JSON).result(JsonBuilder.object().add("release", latest.getFirst()).add("snapshot", latest.getSecond()).toJson());
        });

        app.get("/minecraft/all", ctx -> {
            // check for api key
            String apiKey = ctx.queryParam("apiKey");
            if (apiKey == null || apiKey.isBlank()) {
                ctx.status(HttpStatus.UNAUTHORIZED).result("You must specify an API key!");
                return;
            }

            // check for valid api key
            if (!apiKey.equals(TurtyAPI.getAPIKey())) {
                ctx.status(HttpStatus.UNAUTHORIZED).result("Invalid API key!");
                return;
            }

            NaiveRateLimit.requestPerTimeUnit(ctx, 5, TimeUnit.SECONDS);

            LinkedHashMap<String, Boolean> versions = MinecraftVersions.getAllMinecraftVersions();
            JsonBuilder.ArrayBuilder builder = JsonBuilder.array();
            versions.forEach((version, isRelease) -> builder.add(JsonBuilder.object().add("version", version).add("isRelease", isRelease)));
            ctx.contentType(ContentType.JSON).result(builder.toJson());
        });

        app.get("/minecraft/forge/latest", ctx -> {
            // check for api key
            String apiKey = ctx.queryParam("apiKey");
            if (apiKey == null || apiKey.isBlank()) {
                ctx.status(HttpStatus.UNAUTHORIZED).result("You must specify an API key!");
                return;
            }

            // check for valid api key
            if (!apiKey.equals(TurtyAPI.getAPIKey())) {
                ctx.status(HttpStatus.UNAUTHORIZED).result("Invalid API key!");
                return;
            }

            NaiveRateLimit.requestPerTimeUnit(ctx, 10, TimeUnit.SECONDS);

            Pair<String, String> latest = ForgeVersions.findLatestForge();
            ctx.contentType(ContentType.JSON).result(JsonBuilder.object().add("stable", latest.getFirst()).add("latest", latest.getSecond()).toJson());
        });

        app.get("/minecraft/forge/all", ctx -> {
            // check for api key
            String apiKey = ctx.queryParam("apiKey");
            if (apiKey == null || apiKey.isBlank()) {
                ctx.status(HttpStatus.UNAUTHORIZED).result("You must specify an API key!");
                return;
            }

            // check for valid api key
            if (!apiKey.equals(TurtyAPI.getAPIKey())) {
                ctx.status(HttpStatus.UNAUTHORIZED).result("Invalid API key!");
                return;
            }

            NaiveRateLimit.requestPerTimeUnit(ctx, 5, TimeUnit.SECONDS);

            LinkedHashMap<String, Boolean> versions = ForgeVersions.getAllForgeVersions();
            JsonBuilder.ArrayBuilder builder = JsonBuilder.array();
            versions.forEach((version, isStable) -> builder.add(JsonBuilder.object().add("version", version).add("isStable", isStable)));
            ctx.contentType(ContentType.JSON).result(builder.toJson());
        });

        app.get("/minecraft/neoforge/latest", ctx -> {
            // check for api key
            String apiKey = ctx.queryParam("apiKey");
            if (apiKey == null || apiKey.isBlank()) {
                ctx.status(HttpStatus.UNAUTHORIZED).result("You must specify an API key!");
                return;
            }

            // check for valid api key
            if (!apiKey.equals(TurtyAPI.getAPIKey())) {
                ctx.status(HttpStatus.UNAUTHORIZED).result("Invalid API key!");
                return;
            }

            NaiveRateLimit.requestPerTimeUnit(ctx, 10, TimeUnit.SECONDS);

            Pair<String, String> latest = NeoforgeVersions.findLatestNeoforge();
            ctx.contentType(ContentType.JSON).result(JsonBuilder.object().add("stable", latest.getFirst()).add("latest", latest.getSecond()).toJson());
        });

        app.get("/minecraft/neoforge/all", ctx -> {
            // check for api key
            String apiKey = ctx.queryParam("apiKey");
            if (apiKey == null || apiKey.isBlank()) {
                ctx.status(HttpStatus.UNAUTHORIZED).result("You must specify an API key!");
                return;
            }

            // check for valid api key
            if (!apiKey.equals(TurtyAPI.getAPIKey())) {
                ctx.status(HttpStatus.UNAUTHORIZED).result("Invalid API key!");
                return;
            }

            NaiveRateLimit.requestPerTimeUnit(ctx, 5, TimeUnit.SECONDS);

            LinkedHashMap<String, Boolean> versions = NeoforgeVersions.getAllNeoforgeVersions();
            JsonBuilder.ArrayBuilder builder = JsonBuilder.array();
            versions.forEach((version, isStable) -> builder.add(JsonBuilder.object().add("version", version).add("isStable", isStable)));
            ctx.contentType(ContentType.JSON).result(builder.toJson());
        });

        app.get("/minecraft/fabric/latest", ctx -> {
            // check for api key
            String apiKey = ctx.queryParam("apiKey");
            if (apiKey == null || apiKey.isBlank()) {
                ctx.status(HttpStatus.UNAUTHORIZED).result("You must specify an API key!");
                return;
            }

            // check for valid api key
            if (!apiKey.equals(TurtyAPI.getAPIKey())) {
                ctx.status(HttpStatus.UNAUTHORIZED).result("Invalid API key!");
                return;
            }

            NaiveRateLimit.requestPerTimeUnit(ctx, 10, TimeUnit.SECONDS);

            Pair<String, String> latest = FabricVersions.findLatestFabric();
            ctx.contentType(ContentType.JSON).result(JsonBuilder.object().add("stable", latest.getFirst()).add("unstable", latest.getSecond()).toJson());
        });

        app.get("/minecraft/fabric/all", ctx -> {
            // check for api key
            String apiKey = ctx.queryParam("apiKey");
            if (apiKey == null || apiKey.isBlank()) {
                ctx.status(HttpStatus.UNAUTHORIZED).result("You must specify an API key!");
                return;
            }

            // check for valid api key
            if (!apiKey.equals(TurtyAPI.getAPIKey())) {
                ctx.status(HttpStatus.UNAUTHORIZED).result("Invalid API key!");
                return;
            }

            NaiveRateLimit.requestPerTimeUnit(ctx, 5, TimeUnit.SECONDS);

            LinkedHashMap<String, Boolean> versions = FabricVersions.getAllFabricVersions();
            JsonBuilder.ArrayBuilder builder = JsonBuilder.array();
            versions.forEach((version, isStable) -> builder.add(JsonBuilder.object().add("version", version).add("isStable", isStable)));
            ctx.contentType(ContentType.JSON).result(builder.toJson());
        });

        app.get("/minecraft/quilt/latest", ctx -> {
            // check for api key
            String apiKey = ctx.queryParam("apiKey");
            if (apiKey == null || apiKey.isBlank()) {
                ctx.status(HttpStatus.UNAUTHORIZED).result("You must specify an API key!");
                return;
            }

            // check for valid api key
            if (!apiKey.equals(TurtyAPI.getAPIKey())) {
                ctx.status(HttpStatus.UNAUTHORIZED).result("Invalid API key!");
                return;
            }

            NaiveRateLimit.requestPerTimeUnit(ctx, 10, TimeUnit.SECONDS);

            Pair<String, String> latest = QuiltVersions.findLatestQuilt();
            ctx.contentType(ContentType.JSON).result(JsonBuilder.object().add("stable", latest.getFirst()).add("unstable", latest.getSecond()).toJson());
        });

        app.get("/minecraft/quilt/all", ctx -> {
            // check for api key
            String apiKey = ctx.queryParam("apiKey");
            if (apiKey == null || apiKey.isBlank()) {
                ctx.status(HttpStatus.UNAUTHORIZED).result("You must specify an API key!");
                return;
            }

            // check for valid api key
            if (!apiKey.equals(TurtyAPI.getAPIKey())) {
                ctx.status(HttpStatus.UNAUTHORIZED).result("Invalid API key!");
                return;
            }

            NaiveRateLimit.requestPerTimeUnit(ctx, 5, TimeUnit.SECONDS);

            LinkedHashMap<String, Boolean> versions = QuiltVersions.getAllQuiltVersions();
            JsonBuilder.ArrayBuilder builder = JsonBuilder.array();
            versions.forEach((version, isStable) -> builder.add(JsonBuilder.object().add("version", version).add("isRelease", isStable)));
            ctx.contentType(ContentType.JSON).result(builder.toJson());
        });

        app.get("/minecraft/parchment/latest", ctx -> {
            // check for api key
            String apiKey = ctx.queryParam("apiKey");
            if (apiKey == null || apiKey.isBlank()) {
                ctx.status(HttpStatus.UNAUTHORIZED).result("You must specify an API key!");
                return;
            }

            // check for valid api key
            if (!apiKey.equals(TurtyAPI.getAPIKey())) {
                ctx.status(HttpStatus.UNAUTHORIZED).result("Invalid API key!");
                return;
            }

            NaiveRateLimit.requestPerTimeUnit(ctx, 10, TimeUnit.SECONDS);

            String latest = ParchmentVersions.findLatestParchment();
            ctx.contentType(ContentType.JSON).result(JsonBuilder.object().add("version", latest).toJson());
        });

        app.get("/minecraft/parchment/all", ctx -> {
            // check for api key
            String apiKey = ctx.queryParam("apiKey");
            if (apiKey == null || apiKey.isBlank()) {
                ctx.status(HttpStatus.UNAUTHORIZED).result("You must specify an API key!");
                return;
            }

            // check for valid api key
            if (!apiKey.equals(TurtyAPI.getAPIKey())) {
                ctx.status(HttpStatus.UNAUTHORIZED).result("Invalid API key!");
                return;
            }

            NaiveRateLimit.requestPerTimeUnit(ctx, 5, TimeUnit.SECONDS);

            LinkedHashMap<String, String> versions = ParchmentVersions.getAllParchmentVersions();
            JsonBuilder.ArrayBuilder builder = JsonBuilder.array();
            versions.values().forEach(version -> builder.add(JsonBuilder.object().add("version", version)));
            ctx.contentType(ContentType.JSON).result(builder.toJson());
        });

        app.get("/minecraft/parchment/{version}", ctx -> {
            // check for api key
            String apiKey = ctx.queryParam("apiKey");
            if (apiKey == null || apiKey.isBlank()) {
                ctx.status(HttpStatus.UNAUTHORIZED).result("You must specify an API key!");
                return;
            }

            // check for valid api key
            if (!apiKey.equals(TurtyAPI.getAPIKey())) {
                ctx.status(HttpStatus.UNAUTHORIZED).result("Invalid API key!");
                return;
            }

            NaiveRateLimit.requestPerTimeUnit(ctx, 10, TimeUnit.SECONDS);

            String version = ParchmentVersions.getParchmentVersion(ctx.pathParam("version"));
            if (version.equalsIgnoreCase("unknown")) {
                ctx.status(HttpStatus.NOT_FOUND).result("Failed to find Parchment version!");
                return;
            }

            ctx.contentType(ContentType.JSON).result(JsonBuilder.object().add("version", version).toJson());
        });

        app.ws("/minecraft", wsConfig -> {
            wsConfig.onConnect(ctx -> {
                // check for api key
                String apiKey = ctx.queryParam("apiKey");
                if (apiKey == null || apiKey.isBlank()) {
                    ctx.closeSession();
                    return;
                }

                // check for valid api key
                if (!apiKey.equals(TurtyAPI.getAPIKey())) {
                    ctx.closeSession();
                    return;
                }

                ctx.enableAutomaticPings();

                Consumer<List<MinecraftVersions.MinecraftUpdate>> updateListener = updates -> {
                    JsonBuilder.ObjectBuilder builder = JsonBuilder.object().add("type", "minecraft");

                    JsonBuilder.ArrayBuilder added = JsonBuilder.array();
                    updates.stream().filter(update -> !update.removed()).forEach(update -> added.add(JsonBuilder.object().add("version", update.version()).add("isRelease", update.release())));

                    JsonBuilder.ArrayBuilder removed = JsonBuilder.array();
                    updates.stream().filter(MinecraftVersions.MinecraftUpdate::removed).forEach(update -> removed.add(JsonBuilder.object().add("version", update.version()).add("isRelease", update.release())));

                    builder.add("added", added).add("removed", removed);
                    ctx.send(builder.toJson());
                };

                MinecraftVersions.addUpdateListener(updateListener);

                wsConfig.onClose(context -> MinecraftVersions.removeUpdateListener(updateListener));
            });
        });

        app.ws("/forge", wsConfig -> {
            wsConfig.onConnect(ctx -> {
                // check for api key
                String apiKey = ctx.queryParam("apiKey");
                if (apiKey == null || apiKey.isBlank()) {
                    ctx.closeSession();
                    return;
                }

                // check for valid api key
                if (!apiKey.equals(TurtyAPI.getAPIKey())) {
                    ctx.closeSession();
                    return;
                }

                ctx.enableAutomaticPings(10, TimeUnit.SECONDS);

                Consumer<List<ForgeVersions.ForgeUpdate>> updateListener = updates -> {
                    JsonBuilder.ObjectBuilder builder = JsonBuilder.object().add("type", "forge");

                    JsonBuilder.ArrayBuilder added = JsonBuilder.array();
                    updates.stream().filter(update -> !update.removed()).forEach(update -> added.add(JsonBuilder.object().add("version", update.version()).add("isRecommended", update.recommended())));

                    JsonBuilder.ArrayBuilder removed = JsonBuilder.array();
                    updates.stream().filter(ForgeVersions.ForgeUpdate::removed).forEach(update -> removed.add(JsonBuilder.object().add("version", update.version()).add("isRecommended", update.recommended())));

                    builder.add("added", added).add("removed", removed);
                    ctx.send(builder.toJson());
                };

                ForgeVersions.addUpdateListener(updateListener);

                wsConfig.onClose(context -> ForgeVersions.removeUpdateListener(updateListener));
            });
        });

        app.ws("/fabric", wsConfig -> {
            wsConfig.onConnect(ctx -> {
                // check for api key
                String apiKey = ctx.queryParam("apiKey");
                if (apiKey == null || apiKey.isBlank()) {
                    ctx.closeSession();
                    return;
                }

                // check for valid api key
                if (!apiKey.equals(TurtyAPI.getAPIKey())) {
                    ctx.closeSession();
                    return;
                }

                ctx.enableAutomaticPings(10, TimeUnit.SECONDS);

                Consumer<List<FabricVersions.FabricUpdate>> updateListener = updates -> {
                    JsonBuilder.ObjectBuilder builder = JsonBuilder.object().add("type", "fabric");

                    JsonBuilder.ArrayBuilder added = JsonBuilder.array();
                    updates.stream().filter(update -> !update.removed()).forEach(update -> added.add(JsonBuilder.object().add("version", update.version()).add("isStable", update.stable())));

                    JsonBuilder.ArrayBuilder removed = JsonBuilder.array();
                    updates.stream().filter(FabricVersions.FabricUpdate::removed).forEach(update -> removed.add(JsonBuilder.object().add("version", update.version()).add("isStable", update.stable())));

                    builder.add("added", added).add("removed", removed);
                    ctx.send(builder.toJson());
                };

                FabricVersions.addUpdateListener(updateListener);

                wsConfig.onClose(context -> FabricVersions.removeUpdateListener(updateListener));
            });
        });

        app.ws("/parchment", wsConfig -> {
            wsConfig.onConnect(ctx -> {
                // check for api key
                String apiKey = ctx.queryParam("apiKey");
                if (apiKey == null || apiKey.isBlank()) {
                    ctx.closeSession();
                    return;
                }

                // check for valid api key
                if (!apiKey.equals(TurtyAPI.getAPIKey())) {
                    ctx.closeSession();
                    return;
                }

                ctx.enableAutomaticPings();

                Consumer<List<ParchmentVersions.ParchmentUpdate>> updateListener = updates -> {
                    JsonBuilder.ObjectBuilder builder = JsonBuilder.object().add("type", "parchment");

                    JsonBuilder.ArrayBuilder added = JsonBuilder.array();
                    updates.stream().filter(update -> !update.removed()).forEach(update -> added.add(JsonBuilder.object().add("version", update.parchmentVersion())));

                    JsonBuilder.ArrayBuilder removed = JsonBuilder.array();
                    updates.stream().filter(ParchmentVersions.ParchmentUpdate::removed).forEach(update -> removed.add(JsonBuilder.object().add("version", update.parchmentVersion())));

                    builder.add("added", added).add("removed", removed);
                    ctx.send(builder.toJson());
                };

                ParchmentVersions.addUpdateListener(updateListener);

                wsConfig.onClose(context -> ParchmentVersions.removeUpdateListener(updateListener));
            });
        });

        app.get("image/resize", ctx -> {
            // check for api key
            String apiKey = ctx.queryParam("apiKey");
            if (apiKey == null || apiKey.isBlank()) {
                ctx.status(HttpStatus.UNAUTHORIZED).result("You must specify an API key!");
                return;
            }

            // check for valid api key
            if (!apiKey.equals(TurtyAPI.getAPIKey())) {
                ctx.status(HttpStatus.UNAUTHORIZED).result("Invalid API key!");
                return;
            }

            NaiveRateLimit.requestPerTimeUnit(ctx, 10, TimeUnit.SECONDS);

            String urlStr = ctx.queryParam("url");

            // validate image
            BufferedImage image;
            Optional<BufferedImage> optional = ImageUtils.validateURL(urlStr, Optional.of(ctx));
            if (optional.isEmpty()) {
                ctx.status(HttpStatus.BAD_REQUEST).result("Failed to load image!");
                return;
            }

            image = optional.get();

            // get width and height
            int width = ctx.queryParamAsClass("width", Integer.class).getOrDefault(image.getWidth());
            int height = ctx.queryParamAsClass("height", Integer.class).getOrDefault(image.getHeight());

            // validate width and height
            if (width <= 0 || height <= 0) {
                ctx.status(HttpStatus.BAD_REQUEST).result("Width and height must be greater than 0!");
                return;
            }

            if (width == image.getWidth() && height == image.getHeight()) {
                ctx.result(ImageUtils.toBase64(image));
                return;
            }

            // resize image
            BufferedImage resized = ImageUtils.getResized(image, width, height);

            // send image
            ctx.result(ImageUtils.toBase64(resized));
        });

        app.get("image/rotate", ctx -> {
            // check for api key
            String apiKey = ctx.queryParam("apiKey");
            if (apiKey == null || apiKey.isBlank()) {
                ctx.status(HttpStatus.UNAUTHORIZED).result("You must specify an API key!");
                return;
            }

            // check for valid api key
            if (!apiKey.equals(TurtyAPI.getAPIKey())) {
                ctx.status(HttpStatus.UNAUTHORIZED).result("Invalid API key!");
                return;
            }

            NaiveRateLimit.requestPerTimeUnit(ctx, 10, TimeUnit.SECONDS);

            String urlStr = ctx.queryParam("url");

            // validate image
            BufferedImage image;
            Optional<BufferedImage> optional = ImageUtils.validateURL(urlStr, Optional.of(ctx));
            if (optional.isEmpty()) {
                ctx.status(HttpStatus.BAD_REQUEST).result("Failed to load image!");
                return;
            }

            image = optional.get();

            // get angle
            int angle = ctx.queryParamAsClass("angle", Integer.class).getOrDefault(0);

            // validate angle
            if (angle % 90 != 0) {
                ctx.status(HttpStatus.BAD_REQUEST).result("Angle must be a multiple of 90!");
                return;
            }

            if (angle == 0) {
                ctx.result(ImageUtils.toBase64(image));
                return;
            }

            // rotate image
            BufferedImage rotated = ImageUtils.getRotated(image, angle);

            // send image
            ctx.result(ImageUtils.toBase64(rotated));
        });

        app.get("image/flip", ctx -> {
            // check for api key
            String apiKey = ctx.queryParam("apiKey");
            if (apiKey == null || apiKey.isBlank()) {
                ctx.status(HttpStatus.UNAUTHORIZED).result("You must specify an API key!");
                return;
            }

            // check for valid api key
            if (!apiKey.equals(TurtyAPI.getAPIKey())) {
                ctx.status(HttpStatus.UNAUTHORIZED).result("Invalid API key!");
                return;
            }

            NaiveRateLimit.requestPerTimeUnit(ctx, 10, TimeUnit.SECONDS);

            String urlStr = ctx.queryParam("url");

            // validate image
            BufferedImage image;
            Optional<BufferedImage> optional = ImageUtils.validateURL(urlStr, Optional.of(ctx));
            if (optional.isEmpty()) {
                ctx.status(HttpStatus.BAD_REQUEST).result("Failed to load image!");
                return;
            }

            image = optional.get();

            // get flip
            String flipStr = ctx.queryParam("flip");
            if (flipStr == null) {
                ctx.status(HttpStatus.BAD_REQUEST).result("Flip must be specified!");
                return;
            }

            // validate flip
            if (!flipStr.equalsIgnoreCase("horizontal") && !flipStr.equalsIgnoreCase("vertical") && !flipStr.equalsIgnoreCase("both")) {
                ctx.status(HttpStatus.BAD_REQUEST).result("Flip must be horizontal, vertical or both!");
                return;
            }

            // flip image
            BufferedImage flipped = ImageUtils.getFlipped(image, FlipType.valueOf(flipStr.toUpperCase()));

            // send image
            ctx.result(ImageUtils.toBase64(flipped));
        });

        app.get("/image/filter", ctx -> {
            // check for api key
            String apiKey = ctx.queryParam("apiKey");
            if (apiKey == null || apiKey.isBlank()) {
                ctx.status(HttpStatus.UNAUTHORIZED).result("You must specify an API key!");
                return;
            }

            // check for valid api key
            if (!apiKey.equals(TurtyAPI.getAPIKey())) {
                ctx.status(HttpStatus.UNAUTHORIZED).result("Invalid API key!");
                return;
            }

            NaiveRateLimit.requestPerTimeUnit(ctx, 10, TimeUnit.SECONDS);

            String urlStr = ctx.queryParam("url");

            // validate image
            BufferedImage image;
            Optional<BufferedImage> optional = ImageUtils.validateURL(urlStr, Optional.of(ctx));
            if (optional.isEmpty()) {
                ctx.status(HttpStatus.BAD_REQUEST).result("Failed to load image!");
                return;
            }

            image = optional.get();

            // get filter
            String filterStr = ctx.queryParam("filter");
            if (filterStr == null || filterStr.isBlank()) {
                ctx.status(HttpStatus.BAD_REQUEST).result("Filter must be specified!");
                return;
            }

            // validate filter
            if (!ImageUtils.isValidFilter(filterStr)) {
                ctx.status(HttpStatus.BAD_REQUEST).result("Filter must be valid! For a list of filters. Visit /filters");
                return;
            }

            Optional<BufferedImage> filtered = ImageUtils.handleFiltering(image, filterStr, ctx);
            if (filtered.isEmpty()) {
                ctx.status(HttpStatus.BAD_REQUEST).result("Failed to filter image!");
                return;
            }

            // send image
            ctx.result(ImageUtils.toBase64(filtered.get()));
        });

        app.get("/image/flag", ctx -> {
            // check for api key
            String apiKey = ctx.queryParam("apiKey");
            if (apiKey == null || apiKey.isBlank()) {
                ctx.status(HttpStatus.UNAUTHORIZED).result("You must specify an API key!");
                return;
            }

            // check for valid api key
            if (!apiKey.equals(TurtyAPI.getAPIKey())) {
                ctx.status(HttpStatus.UNAUTHORIZED).result("Invalid API key!");
                return;
            }

            NaiveRateLimit.requestPerTimeUnit(ctx, 10, TimeUnit.SECONDS);

            String urlStr = ctx.queryParam("url");
            int colors = ctx.queryParamAsClass("colors", Integer.class).getOrDefault(5);

            // validate image
            BufferedImage image;
            Optional<BufferedImage> optional = ImageUtils.validateURL(urlStr, Optional.of(ctx));
            if (optional.isEmpty()) {
                ctx.status(HttpStatus.BAD_REQUEST).result("Failed to load image!");
                return;
            }

            image = optional.get();

            // validate colors
            if (colors < 1 || colors > 10) {
                ctx.status(HttpStatus.BAD_REQUEST).result("Colors must be between 1 and 10!");
                return;
            }

            // flag image
            BufferedImage flag = ColorFlagGenerator.create(image, colors);

            // send image
            ctx.result(ImageUtils.toBase64(flag));
        });

        app.get("/image/lgbt", ctx -> {
            // check for api key
            String apiKey = ctx.queryParam("apiKey");
            if (apiKey == null || apiKey.isBlank()) {
                ctx.status(HttpStatus.UNAUTHORIZED).result("You must specify an API key!");
                return;
            }

            // check for valid api key
            if (!apiKey.equals(TurtyAPI.getAPIKey())) {
                ctx.status(HttpStatus.UNAUTHORIZED).result("Invalid API key!");
                return;
            }

            NaiveRateLimit.requestPerTimeUnit(ctx, 10, TimeUnit.SECONDS);

            String urlStr = ctx.queryParam("url");

            // validate image
            BufferedImage image;
            Optional<BufferedImage> optional = ImageUtils.validateURL(urlStr, Optional.of(ctx));
            if (optional.isEmpty()) {
                ctx.status(HttpStatus.BAD_REQUEST).result("Failed to load image!");
                return;
            }

            image = optional.get();

            // lgbt image
            BufferedImage lgbt = LGBTifier.lgbtify(image);

            // send image
            ctx.result(ImageUtils.toBase64(lgbt));
        });

        app.get("/geo/guesser", ctx -> {
            // check for api key
            String apiKey = ctx.queryParam("apiKey");
            if (apiKey == null || apiKey.isBlank()) {
                ctx.status(HttpStatus.UNAUTHORIZED).result("You must specify an API key!");
                return;
            }

            // check for valid api key
            if (!apiKey.equals(TurtyAPI.getAPIKey())) {
                ctx.status(HttpStatus.UNAUTHORIZED).result("Invalid API key!");
                return;
            }

            NaiveRateLimit.requestPerTimeUnit(ctx, 10, TimeUnit.MINUTES);

            Optional<Pair<String, BufferedImage>> optional = GeoguesserManager.requestStaticImage();
            if (optional.isEmpty()) {
                ctx.status(HttpStatus.INTERNAL_SERVER_ERROR).result("Failed to load image!");
                return;
            }

            Pair<String, BufferedImage> pair = optional.get();
            String country = pair.getFirst();
            BufferedImage image = pair.getSecond();

            ctx.contentType(ContentType.JSON).result(new JsonBuilder.ObjectBuilder().add("country", country).add("image", ImageUtils.toBase64(image)).toJson());
        });

        app.get("/nsfw/pornstar", ctx -> {
            // check for api key
            String apiKey = ctx.queryParam("apiKey");
            if (apiKey == null || apiKey.isBlank()) {
                ctx.status(HttpStatus.UNAUTHORIZED).result("You must specify an API key!");
                return;
            }

            // check for valid api key
            if (!apiKey.equals(TurtyAPI.getAPIKey())) {
                ctx.status(HttpStatus.UNAUTHORIZED).result("Invalid API key!");
                return;
            }

            NaiveRateLimit.requestPerTimeUnit(ctx, 20, TimeUnit.MINUTES);

            Optional<Pornstar> optional = NSFWManager.getRandomPornstarWithPhoto();
            if (optional.isEmpty()) {
                ctx.status(HttpStatus.INTERNAL_SERVER_ERROR).result("Failed to load pornstar!");
                return;
            }

            Pornstar pornstar = optional.get();

            String careerStatus = pornstar.getCareer_status();
            String country = pornstar.getCountry();
            int dayOfBirth = pornstar.getDay_of_birth();
            String monthOfBirth = pornstar.getMonth_of_birth();
            int yearOfBirth = pornstar.getYear_of_birth();
            String gender = pornstar.getGender();
            String id = pornstar.getId();
            String name = pornstar.getName();
            String profession = pornstar.getProfession();
            List<String> nicknames = pornstar.getNicknames();
            List<String> photos = pornstar.getPhotos().stream().map(Photo::getFull).toList();

            JsonBuilder.ArrayBuilder nicknamesBuilder = new JsonBuilder.ArrayBuilder();
            nicknames.forEach(nicknamesBuilder::add);

            JsonBuilder.ArrayBuilder photosBuilder = new JsonBuilder.ArrayBuilder();
            photos.forEach(photosBuilder::add);

            ctx.contentType(ContentType.JSON).result(
                    new JsonBuilder.ObjectBuilder()
                            .add("careerStatus", careerStatus)
                            .add("country", country)
                            .add("dayOfBirth", dayOfBirth)
                            .add("monthOfBirth", monthOfBirth)
                            .add("yearOfBirth", yearOfBirth)
                            .add("gender", gender)
                            .add("id", id)
                            .add("name", name)
                            .add("profession", profession)
                            .add("nicknames", nicknamesBuilder)
                            .add("photos", photosBuilder)
                            .toJson());
        });

        app.get("/fun/wyr/random", ctx -> {
            // check for api key
            String apiKey = ctx.queryParam("apiKey");
            if (apiKey == null || apiKey.isBlank()) {
                ctx.status(HttpStatus.UNAUTHORIZED).result("You must specify an API key!");
                return;
            }

            // check for valid api key
            if (!apiKey.equals(TurtyAPI.getAPIKey())) {
                ctx.status(HttpStatus.UNAUTHORIZED).result("Invalid API key!");
                return;
            }

            NaiveRateLimit.requestPerTimeUnit(ctx, 10, TimeUnit.SECONDS);

            boolean includeNSFW = ctx.queryParamAsClass("includeNSFW", Boolean.class).getOrDefault(false);
            boolean nsfw = ctx.queryParamAsClass("nsfw", Boolean.class).getOrDefault(false);

            WouldYouRatherManager.WouldYouRather wouldYouRather = nsfw ?
                    WouldYouRatherManager.getRandomNSFWWouldYouRather() :
                    WouldYouRatherManager.getRandomWouldYouRather(includeNSFW);

            if (wouldYouRather == null) {
                ctx.status(HttpStatus.INTERNAL_SERVER_ERROR).result("Failed to load would you rather!");
                return;
            }

            ctx.contentType(ContentType.JSON).result(
                    new JsonBuilder.ObjectBuilder()
                            .add("optionA", wouldYouRather.optionA())
                            .add("optionB", wouldYouRather.optionB())
                            .toJson());
        });

        app.get("/geo/coordinate", ctx -> {
            // check for api key
            String apiKey = ctx.queryParam("apiKey");
            if (apiKey == null || apiKey.isBlank()) {
                ctx.status(HttpStatus.UNAUTHORIZED).result("You must specify an API key!");
                return;
            }

            // check for valid api key
            if (!apiKey.equals(TurtyAPI.getAPIKey())) {
                ctx.status(HttpStatus.UNAUTHORIZED).result("Invalid API key!");
                return;
            }

            NaiveRateLimit.requestPerTimeUnit(ctx, 10, TimeUnit.SECONDS);

            boolean land = ctx.queryParamAsClass("land", Boolean.class).getOrDefault(false);
            if (!land) {
                ctx.status(HttpStatus.BAD_REQUEST).result("You must specify land=true!");
                return;
            }

            Coordinate coordinate = CoordinatePicker.INSTANCE.findRandomLandCoordinate();
            if (coordinate == null) {
                ctx.status(HttpStatus.INTERNAL_SERVER_ERROR).result("Failed to find coordinate!");
                return;
            }

            ctx.contentType(ContentType.JSON).result(
                    new JsonBuilder.ObjectBuilder()
                            .add("longitude", coordinate.getX())
                            .add("latitude", coordinate.getY())
                            .toJson());
        });

        app.get("/games/search", ctx -> {
            String apiKey = ctx.queryParam("apiKey");
            if (apiKey == null || apiKey.isBlank()) {
                ctx.status(HttpStatus.UNAUTHORIZED).result("You must specify an API key!");
                return;
            }

            if (!apiKey.equals(TurtyAPI.getAPIKey())) {
                ctx.status(HttpStatus.UNAUTHORIZED).result("Invalid API key!");
                return;
            }

            NaiveRateLimit.requestPerTimeUnit(ctx, 10, TimeUnit.SECONDS);

            String query = ctx.queryParam("query");
            if (query == null || query.isBlank()) {
                ctx.status(HttpStatus.BAD_REQUEST).result("You must specify a query!");
                return;
            }

            String fields = ctx.queryParam("fields");
            if (fields == null || fields.isBlank()) {
                fields = "*";
            }

            final List<String> fieldsList = Arrays.asList(fields.split(","));
            if (fieldsList.isEmpty()) {
                fieldsList.add("*");
            }

            List<Game> results = IGDBConnector.INSTANCE.searchGames(query, fieldsList.toArray(new String[0]));
            if (results == null) {
                ctx.status(HttpStatus.INTERNAL_SERVER_ERROR).result("Failed to search for games!");
                return;
            }

            var array = new JsonArray();
            for (Game game : results) {
                var object = new JsonObject();
                Constants.GSON.toJsonTree(game)
                        .getAsJsonObject()
                        .entrySet()
                        .stream()
                        .filter(entry -> fieldsList.contains("*") || fieldsList.contains(entry.getKey()))
                        .filter(entry -> !entry.getValue().isJsonPrimitive() || !entry.getValue().getAsJsonPrimitive().isNumber() || entry.getValue().getAsNumber().doubleValue() != -1)
                        .forEach(entry -> object.add(entry.getKey(), entry.getValue()));
                array.add(object);
            }

            ctx.contentType(ContentType.JSON).result(Constants.GSON.toJson(array));
        });

        app.get("/games/artwork", ctx -> {
            String apiKey = ctx.queryParam("apiKey");
            if (apiKey == null || apiKey.isBlank()) {
                ctx.status(HttpStatus.UNAUTHORIZED).result("You must specify an API key!");
                return;
            }

            if (!apiKey.equals(TurtyAPI.getAPIKey())) {
                ctx.status(HttpStatus.UNAUTHORIZED).result("Invalid API key!");
                return;
            }

            NaiveRateLimit.requestPerTimeUnit(ctx, 10, TimeUnit.SECONDS);

            String id = ctx.queryParam("id");
            if (id == null || id.isBlank()) {
                ctx.status(HttpStatus.BAD_REQUEST).result("You must specify an id!");
                return;
            }

            int intId;
            try {
                intId = Integer.parseInt(id);
            } catch (NumberFormatException e) {
                ctx.status(HttpStatus.BAD_REQUEST).result("You must specify a valid id!");
                return;
            }

            String fields = ctx.queryParam("fields");
            if (fields == null || fields.isBlank()) {
                fields = "*";
            }

            final List<String> fieldsList = Arrays.asList(fields.split(","));
            if (fieldsList.isEmpty()) {
                fieldsList.add("*");
            }

            Artwork artwork = IGDBConnector.INSTANCE.findArtwork(intId, fieldsList.toArray(new String[0]));
            if (artwork == null) {
                ctx.status(HttpStatus.INTERNAL_SERVER_ERROR).result("Failed to find artwork!");
                return;
            }

            if (fields.contains("*") || fields.contains("url")) {
                String value = artwork.getUrl().replace("//images.igdb.com/", "https://images.igdb.com/");

                String imageSize = ctx.queryParam("imageSize");
                if (imageSize == null || imageSize.isBlank()) {
                    imageSize = ImageSize.THUMB.getTSize();
                } else {
                    try {
                        imageSize = ImageSize.valueOf(imageSize.toUpperCase(Locale.ROOT)).getTSize();
                    } catch (IllegalArgumentException e) {
                        ctx.status(HttpStatus.BAD_REQUEST).result("You must specify a valid image size! The options are: " + Arrays.stream(ImageSize.values())
                                .map(ImageSize::name)
                                .collect(Collectors.joining(", ")));
                        return;
                    }
                }

                value = value.replace("t_thumb", imageSize);
                artwork.setUrl(value);
            }

            var object = new JsonObject();
            Constants.GSON.toJsonTree(artwork)
                    .getAsJsonObject()
                    .entrySet()
                    .stream()
                    .filter(entry -> fieldsList.contains("*") || fieldsList.contains(entry.getKey()))
                    .filter(entry -> !entry.getValue().isJsonPrimitive() || !entry.getValue().getAsJsonPrimitive().isNumber() || entry.getValue().getAsNumber().doubleValue() != -1)
                    .forEach(entry -> object.add(entry.getKey(), entry.getValue()));

            ctx.contentType(ContentType.JSON).result(Constants.GSON.toJson(object));
        });

        app.get("/games/cover", ctx -> {
            String apiKey = ctx.queryParam("apiKey");
            if (apiKey == null || apiKey.isBlank()) {
                ctx.status(HttpStatus.UNAUTHORIZED).result("You must specify an API key!");
                return;
            }

            if (!apiKey.equals(TurtyAPI.getAPIKey())) {
                ctx.status(HttpStatus.UNAUTHORIZED).result("Invalid API key!");
                return;
            }

            NaiveRateLimit.requestPerTimeUnit(ctx, 10, TimeUnit.SECONDS);

            String id = ctx.queryParam("id");
            if (id == null || id.isBlank()) {
                ctx.status(HttpStatus.BAD_REQUEST).result("You must specify an id!");
                return;
            }

            int intId;
            try {
                intId = Integer.parseInt(id);
            } catch (NumberFormatException e) {
                ctx.status(HttpStatus.BAD_REQUEST).result("You must specify a valid id!");
                return;
            }

            String fields = ctx.queryParam("fields");
            if (fields == null || fields.isBlank()) {
                fields = "*";
            }

            final List<String> fieldsList = Arrays.asList(fields.split(","));
            if (fieldsList.isEmpty()) {
                fieldsList.add("*");
            }

            Cover cover = IGDBConnector.INSTANCE.findCover(intId, fieldsList.toArray(new String[0]));
            if (cover == null) {
                ctx.status(HttpStatus.INTERNAL_SERVER_ERROR).result("Failed to find cover!");
                return;
            }

            if (fields.contains("*") || fields.contains("url")) {
                String value = cover.getUrl().replace("//images.igdb.com/", "https://images.igdb.com/");

                String imageSize = ctx.queryParam("imageSize");
                if (imageSize == null || imageSize.isBlank()) {
                    imageSize = ImageSize.THUMB.getTSize();
                } else {
                    try {
                        imageSize = ImageSize.valueOf(imageSize.toUpperCase(Locale.ROOT)).getTSize();
                    } catch (IllegalArgumentException e) {
                        ctx.status(HttpStatus.BAD_REQUEST).result("You must specify a valid image size! The options are: " + Arrays.stream(ImageSize.values())
                                .map(ImageSize::name)
                                .collect(Collectors.joining(", ")));
                        return;
                    }
                }

                value = value.replace("t_thumb", imageSize);
                cover.setUrl(value);
            }

            var object = new JsonObject();
            Constants.GSON.toJsonTree(cover)
                    .getAsJsonObject()
                    .entrySet()
                    .stream()
                    .filter(entry -> fieldsList.contains("*") || fieldsList.contains(entry.getKey()))
                    .filter(entry -> !entry.getValue().isJsonPrimitive() || !entry.getValue().getAsJsonPrimitive().isNumber() || entry.getValue().getAsNumber().doubleValue() != -1)
                    .forEach(entry -> object.add(entry.getKey(), entry.getValue()));

            ctx.contentType(ContentType.JSON).result(Constants.GSON.toJson(object));
        });

        app.get("/games/platform", ctx -> {
            String apiKey = ctx.queryParam("apiKey");
            if (apiKey == null || apiKey.isBlank()) {
                ctx.status(HttpStatus.UNAUTHORIZED).result("You must specify an API key!");
                return;
            }

            if (!apiKey.equals(TurtyAPI.getAPIKey())) {
                ctx.status(HttpStatus.UNAUTHORIZED).result("Invalid API key!");
                return;
            }

            NaiveRateLimit.requestPerTimeUnit(ctx, 10, TimeUnit.SECONDS);

            String id = ctx.queryParam("id");
            if (id == null || id.isBlank()) {
                ctx.status(HttpStatus.BAD_REQUEST).result("You must specify an id!");
                return;
            }

            int intId;
            try {
                intId = Integer.parseInt(id);
            } catch (NumberFormatException e) {
                ctx.status(HttpStatus.BAD_REQUEST).result("You must specify a valid id!");
                return;
            }

            String fields = ctx.queryParam("fields");
            if (fields == null || fields.isBlank()) {
                fields = "*";
            }

            final List<String> fieldsList = Arrays.asList(fields.split(","));
            if (fieldsList.isEmpty()) {
                fieldsList.add("*");
            }

            GamePlatform platform = IGDBConnector.INSTANCE.findPlatform(intId, fieldsList.toArray(new String[0]));
            if (platform == null) {
                ctx.status(HttpStatus.INTERNAL_SERVER_ERROR).result("Failed to find platform!");
                return;
            }

            var object = new JsonObject();
            Constants.GSON.toJsonTree(platform)
                    .getAsJsonObject()
                    .entrySet()
                    .stream()
                    .filter(entry -> fieldsList.contains("*") || fieldsList.contains(entry.getKey()))
                    .filter(entry -> !entry.getValue().isJsonPrimitive() || !entry.getValue().getAsJsonPrimitive().isNumber() || entry.getValue().getAsNumber().doubleValue() != -1)
                    .forEach(entry -> object.add(entry.getKey(), entry.getValue()));

            ctx.contentType(ContentType.JSON).result(Constants.GSON.toJson(object));
        });

        app.get("/code/guesser", ctx -> {
            String apiKey = ctx.queryParam("apiKey");
            if (apiKey == null || apiKey.isBlank()) {
                ctx.status(HttpStatus.UNAUTHORIZED).result("You must specify an API key!");
                return;
            }

            if (!apiKey.equals(TurtyAPI.getAPIKey())) {
                ctx.status(HttpStatus.UNAUTHORIZED).result("Invalid API key!");
                return;
            }

            NaiveRateLimit.requestPerTimeUnit(ctx, 10, TimeUnit.SECONDS);

            Code code = CodeManager.findCode();
            if (code.code() == null || code.code().isBlank() || code.language() == null) {
                ctx.status(HttpStatus.INTERNAL_SERVER_ERROR).result("Failed to find code!");
                return;
            }

            // turn code into a json serializable string
            String codeStr = URLEncoder.encode(code.code(), StandardCharsets.UTF_8);

            ctx.contentType(ContentType.JSON).result(
                    new JsonBuilder.ObjectBuilder()
                            .add("code", codeStr)
                            .add("language", new JsonBuilder.ObjectBuilder()
                                    .add("name", code.language().getName())
                                    .add("extension", code.language().getExtension()))
                            .toJson());
        });

        app.get("/fun/celebrity/random", ctx -> {
            String apiKey = ctx.queryParam("apiKey");
            if (apiKey == null || apiKey.isBlank()) {
                ctx.status(HttpStatus.UNAUTHORIZED).result("You must specify an API key!");
                return;
            }

            if (!apiKey.equals(TurtyAPI.getAPIKey())) {
                ctx.status(HttpStatus.UNAUTHORIZED).result("Invalid API key!");
                return;
            }

            NaiveRateLimit.requestPerTimeUnit(ctx, 10, TimeUnit.SECONDS);

            Optional<CelebrityManager.Celebrity> celebrityOpt = CelebrityManager.getRandomCelebrity();
            if (celebrityOpt.isEmpty()) {
                ctx.status(HttpStatus.INTERNAL_SERVER_ERROR).result("Failed to find celebrity!");
                return;
            }

            CelebrityManager.Celebrity celebrity = celebrityOpt.get();

            ctx.contentType(ContentType.JSON).result(
                    new JsonBuilder.ObjectBuilder()
                            .add("name", celebrity.name())
                            .add("image", Base64.getEncoder().encodeToString(celebrity.image()))
                            .toJson());
        });

        RouteManager.app = app.start(Constants.PORT);
    }

    public static void shutdown() {
        app.stop();
    }
}
