package dev.turtywurty.turtyapi;

import com.google.gson.JsonArray;
import dev.turtywurty.turtyapi.geography.Territory;
import dev.turtywurty.turtyapi.geography.TerritoryManager;
import dev.turtywurty.turtyapi.image.ColorFlagGenerator;
import dev.turtywurty.turtyapi.image.ImageUtils;
import dev.turtywurty.turtyapi.image.LGBTifier;
import dev.turtywurty.turtyapi.json.JsonBuilder;
import dev.turtywurty.turtyapi.minecraft.FabricVersions;
import dev.turtywurty.turtyapi.minecraft.ForgeVersions;
import dev.turtywurty.turtyapi.minecraft.MinecraftVersions;
import dev.turtywurty.turtyapi.minecraft.ParchmentVersions;
import dev.turtywurty.turtyapi.words.WordManager;
import io.javalin.Javalin;
import io.javalin.http.ContentType;
import io.javalin.http.HttpStatus;
import io.javalin.json.JsonMapper;
import kotlin.Pair;
import org.jetbrains.annotations.NotNull;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
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

        app.get("/geo/flag/{cca3}", ctx -> {
            try {
                String cca3 = ctx.pathParam("cca3");
                Territory data = TerritoryManager.getTerritory(cca3);
                Constants.LOGGER.debug("Sending flag for {}!", data);

                InputStream imageStream = Files.newInputStream(Constants.DATA_FOLDER.resolve("geography/flags").resolve(data.getFlag()));
                ctx.contentType(ContentType.IMAGE_PNG).result(imageStream.readAllBytes());
                imageStream.close();
            } catch (IOException | NullPointerException exception) {
                ctx.status(HttpStatus.NOT_FOUND).result("Failed to find territory!");
            }
        });

        app.get("/geo/outline/{cca3}", ctx -> {
            try {
                String cca3 = ctx.pathParam("cca3");
                Territory data = TerritoryManager.getTerritory(cca3);
                Constants.LOGGER.debug("Sending outline for {}!", data);

                InputStream imageStream = Files.newInputStream(Constants.DATA_FOLDER.resolve("geography/outlines").resolve(data.getOutline()));
                ctx.contentType(ContentType.IMAGE_PNG).result(imageStream.readAllBytes());
                imageStream.close();
            } catch (IOException | NullPointerException exception) {
                ctx.status(HttpStatus.NOT_FOUND).result("Failed to find territory!");
            }
        });

        app.get("/geo/data/{cca3}", ctx -> {
            try {
                String cca3 = ctx.pathParam("cca3");
                Territory data = TerritoryManager.getTerritory(cca3);
                Constants.LOGGER.debug("Sending data for {}!", data);

                ctx.contentType(ContentType.JSON).result(Constants.GSON.toJson(data));
            } catch (NullPointerException exception) {
                ctx.status(HttpStatus.NOT_FOUND).result("Failed to find territory!");
            }
        });

        app.get("/words", ctx -> {
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

        app.get("/minecraft/latest", ctx -> {
            Pair<String, String> latest = MinecraftVersions.findLatestMinecraft();
            ctx.contentType(ContentType.JSON).result(JsonBuilder.object().add("release", latest.getFirst()).add("snapshot", latest.getSecond()).toJson());
        });

        app.get("/minecraft/all", ctx -> {
            LinkedHashMap<String, Boolean> versions = MinecraftVersions.getAllMinecraftVersions();
            JsonBuilder.ArrayBuilder builder = JsonBuilder.array();
            versions.forEach((version, isRelease) -> builder.add(JsonBuilder.object().add("version", version).add("isRelease", isRelease)));
            ctx.contentType(ContentType.JSON).result(builder.toJson());
        });

        app.get("/minecraft/forge/latest", ctx -> {
            Pair<String, String> latest = ForgeVersions.findLatestForge();
            ctx.contentType(ContentType.JSON).result(JsonBuilder.object().add("recommended", latest.getFirst()).add("latest", latest.getSecond()).toJson());
        });

        app.get("/minecraft/forge/all", ctx -> {
            LinkedHashMap<String, Boolean> versions = ForgeVersions.getAllForgeVersions();
            JsonBuilder.ArrayBuilder builder = JsonBuilder.array();
            versions.forEach((version, isRecommended) -> builder.add(JsonBuilder.object().add("version", version).add("isRecommended", isRecommended)));
            ctx.contentType(ContentType.JSON).result(builder.toJson());
        });

        app.get("/minecraft/fabric/latest", ctx -> {
            Pair<String, String> latest = FabricVersions.findLatestFabric();
            ctx.contentType(ContentType.JSON).result(JsonBuilder.object().add("loader", latest.getFirst()).add("mappings", latest.getSecond()).toJson());
        });

        app.get("/minecraft/fabric/all", ctx -> {
            LinkedHashMap<String, Boolean> versions = FabricVersions.getAllFabricVersions();
            JsonBuilder.ArrayBuilder builder = JsonBuilder.array();
            versions.forEach((version, isStable) -> builder.add(JsonBuilder.object().add("version", version).add("isStable", isStable)));
            ctx.contentType(ContentType.JSON).result(builder.toJson());
        });

        app.get("/minecraft/parchment/latest", ctx -> {
            String latest = ParchmentVersions.findLatestParchment();
            ctx.contentType(ContentType.JSON).result(JsonBuilder.object().add("version", latest).toJson());
        });

        app.get("/minecraft/parchment/all", ctx -> {
            LinkedHashMap<String, String> versions = ParchmentVersions.getAllParchmentVersions();
            JsonBuilder.ArrayBuilder builder = JsonBuilder.array();
            versions.values().forEach(version -> builder.add(JsonBuilder.object().add("version", version)));
            ctx.contentType(ContentType.JSON).result(builder.toJson());
        });

        app.get("/minecraft/parchment/{version}", ctx -> {
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
                ctx.enableAutomaticPings();

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
                ctx.enableAutomaticPings();

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
            if (!flipStr.equalsIgnoreCase("horizontal") && !flipStr.equalsIgnoreCase("vertical")) {
                ctx.status(HttpStatus.BAD_REQUEST).result("Flip must be horizontal or vertical!");
                return;
            }

            // flip image
            BufferedImage flipped = ImageUtils.getFlipped(image, flipStr.equalsIgnoreCase("horizontal"));

            // send image
            ctx.result(ImageUtils.toBase64(flipped));
        });

        app.get("/image/filter", ctx -> {
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

        RouteManager.app = app.start(Constants.PORT);
    }

    public static void shutdown() {
        app.close();
    }
}
