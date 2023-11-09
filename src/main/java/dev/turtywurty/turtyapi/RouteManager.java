package dev.turtywurty.turtyapi;

import com.google.gson.JsonArray;
import dev.turtywurty.turtyapi.fun.WouldYouRather;
import dev.turtywurty.turtyapi.fun.WouldYouRatherManager;
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

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

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

        Javalin app = Javalin.create(ctx -> ctx.jsonMapper(gsonMapper));

        app.get("/", ctx -> ctx.result("Hello World!"));

        app.get("/geo/flag", ctx -> {
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
            NaiveRateLimit.requestPerTimeUnit(ctx, 10, TimeUnit.SECONDS);

            Pair<String, String> latest = MinecraftVersions.findLatestMinecraft();
            ctx.contentType(ContentType.JSON).result(JsonBuilder.object().add("release", latest.getFirst()).add("snapshot", latest.getSecond()).toJson());
        });

        app.get("/minecraft/all", ctx -> {
            NaiveRateLimit.requestPerTimeUnit(ctx, 5, TimeUnit.SECONDS);
            LinkedHashMap<String, Boolean> versions = MinecraftVersions.getAllMinecraftVersions();
            JsonBuilder.ArrayBuilder builder = JsonBuilder.array();
            versions.forEach((version, isRelease) -> builder.add(JsonBuilder.object().add("version", version).add("isRelease", isRelease)));
            ctx.contentType(ContentType.JSON).result(builder.toJson());
        });

        app.get("/minecraft/forge/latest", ctx -> {
            NaiveRateLimit.requestPerTimeUnit(ctx, 10, TimeUnit.SECONDS);

            Pair<String, String> latest = ForgeVersions.findLatestForge();
            ctx.contentType(ContentType.JSON).result(JsonBuilder.object().add("recommended", latest.getFirst()).add("latest", latest.getSecond()).toJson());
        });

        app.get("/minecraft/forge/all", ctx -> {
            NaiveRateLimit.requestPerTimeUnit(ctx, 5, TimeUnit.SECONDS);

            LinkedHashMap<String, Boolean> versions = ForgeVersions.getAllForgeVersions();
            JsonBuilder.ArrayBuilder builder = JsonBuilder.array();
            versions.forEach((version, isRecommended) -> builder.add(JsonBuilder.object().add("version", version).add("isRecommended", isRecommended)));
            ctx.contentType(ContentType.JSON).result(builder.toJson());
        });

        app.get("/minecraft/fabric/latest", ctx -> {
            NaiveRateLimit.requestPerTimeUnit(ctx, 10, TimeUnit.SECONDS);

            Pair<String, String> latest = FabricVersions.findLatestFabric();
            ctx.contentType(ContentType.JSON).result(JsonBuilder.object().add("stable", latest.getFirst()).add("unstable", latest.getSecond()).toJson());
        });

        app.get("/minecraft/fabric/all", ctx -> {
            NaiveRateLimit.requestPerTimeUnit(ctx, 5, TimeUnit.SECONDS);

            LinkedHashMap<String, Boolean> versions = FabricVersions.getAllFabricVersions();
            JsonBuilder.ArrayBuilder builder = JsonBuilder.array();
            versions.forEach((version, isStable) -> builder.add(JsonBuilder.object().add("version", version).add("isStable", isStable)));
            ctx.contentType(ContentType.JSON).result(builder.toJson());
        });

        app.get("/minecraft/quilt/latest", ctx -> {
            NaiveRateLimit.requestPerTimeUnit(ctx, 10, TimeUnit.SECONDS);

            Pair<String, String> latest = QuiltVersions.findLatestQuilt();
            ctx.contentType(ContentType.JSON).result(JsonBuilder.object().add("stable", latest.getFirst()).add("unstable", latest.getSecond()).toJson());
        });

        app.get("/minecraft/quilt/all", ctx -> {
            NaiveRateLimit.requestPerTimeUnit(ctx, 5, TimeUnit.SECONDS);

            LinkedHashMap<String, Boolean> versions = QuiltVersions.getAllQuiltVersions();
            JsonBuilder.ArrayBuilder builder = JsonBuilder.array();
            versions.forEach((version, isStable) -> builder.add(JsonBuilder.object().add("version", version).add("isRelease", isStable)));
            ctx.contentType(ContentType.JSON).result(builder.toJson());
        });

        app.get("/minecraft/parchment/latest", ctx -> {
            NaiveRateLimit.requestPerTimeUnit(ctx, 10, TimeUnit.SECONDS);

            String latest = ParchmentVersions.findLatestParchment();
            ctx.contentType(ContentType.JSON).result(JsonBuilder.object().add("version", latest).toJson());
        });

        app.get("/minecraft/parchment/all", ctx -> {
            NaiveRateLimit.requestPerTimeUnit(ctx, 5, TimeUnit.SECONDS);

            LinkedHashMap<String, String> versions = ParchmentVersions.getAllParchmentVersions();
            JsonBuilder.ArrayBuilder builder = JsonBuilder.array();
            versions.values().forEach(version -> builder.add(JsonBuilder.object().add("version", version)));
            ctx.contentType(ContentType.JSON).result(builder.toJson());
        });

        app.get("/minecraft/parchment/{version}", ctx -> {
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
            NaiveRateLimit.requestPerTimeUnit(ctx, 20, TimeUnit.MINUTES);

            Optional<Pornstar> optional = NSFWManager.getRandomPornstarWithPhoto();
            if (optional.isEmpty()) {
                ctx.status(HttpStatus.INTERNAL_SERVER_ERROR).result("Failed to load pornstar!");
                return;
            }

            Pornstar pornstar = optional.get();

            String careerStatus = pornstar.getCareer_status();
            String country = pornstar.getCountry();
            int dayOfBirth = pornstar.getDay_of_birth();;
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
            NaiveRateLimit.requestPerTimeUnit(ctx, 10, TimeUnit.SECONDS);

            boolean includeNSFW = ctx.queryParamAsClass("includeNSFW", Boolean.class).getOrDefault(false);
            boolean nsfw = ctx.queryParamAsClass("nsfw", Boolean.class).getOrDefault(false);

            WouldYouRather wouldYouRather = nsfw ?
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

        RouteManager.app = app.start(Constants.PORT);
    }

    public static void shutdown() {
        app.close();
    }
}
