package dev.turtywurty.turtyapi;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import dev.turtywurty.turtyapi.games.IGDBConnector;
import dev.turtywurty.turtyapi.geography.RegionManager;
import dev.turtywurty.turtyapi.image.ImageUtils;
import io.github.cdimascio.dotenv.Dotenv;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import io.javalin.http.util.NaiveRateLimit;
import org.apache.commons.cli.*;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class TurtyAPI {
    private static final Map<String, KeyData> API_KEYS = new HashMap<>();

    private static boolean isDev;
    private static Dotenv environment;
    private static Path keysPath;

    public static void main(String[] args) {
        Constants.LOGGER.info("Starting TurtyAPI!");

        parseCLIArguments(args);
        loadKeyData();

        RouteManager.init();
        Testing.start(args);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            Testing.shutdown();
            RouteManager.shutdown();
            Constants.LOGGER.info("Shutting down!");
        }));

        Constants.LOGGER.info("Started TurtyAPI!");
    }

    private static void parseCLIArguments(String[] args) {
        var options = new Options();

        var devOption = new Option("dev", false, "Run the API in development mode");
        devOption.setRequired(false);
        options.addOption(devOption);

        var envOption = new Option("env", true, "Path to the environment file");
        envOption.setRequired(false);
        options.addOption(envOption);

        var keysOption = new Option("keys", true, "Path to the API keys file");
        keysOption.setRequired(false);
        options.addOption(keysOption);

        var parser = new DefaultParser();
        var formatter = new HelpFormatter();
        CommandLine cmd;
        try {
            cmd = parser.parse(options, args);
        } catch (ParseException exception) {
            Constants.LOGGER.error("Failed to parse command line arguments!", exception);
            formatter.printHelp("TurtyAPI", options);

            System.exit(1);
            return;
        }

        if (cmd.hasOption("dev")) {
            TurtyAPI.isDev = true;
        }

        if (cmd.hasOption("env")) {
            String envPath = cmd.getOptionValue("env");
            TurtyAPI.environment = Dotenv.configure().directory(envPath).load();
        } else {
            TurtyAPI.environment = Dotenv.load();
        }

        if (cmd.hasOption("keys")) {
            String keysPath = cmd.getOptionValue("keys");
            TurtyAPI.keysPath = Path.of(keysPath);

            if (!Files.exists(TurtyAPI.keysPath)) {
                Constants.LOGGER.error("API keys file does not exist!");
                System.exit(1);
            }
        } else {
            TurtyAPI.keysPath = Path.of("api.keys");
        }
    }

    public static boolean isDev() {
        return isDev;
    }

    public static InputStream getResource(String location) throws NullPointerException {
        return TurtyAPI.class.getClassLoader().getResourceAsStream(location);
    }

    public static Optional<BufferedImage> getResourceAsImage(String location) throws NullPointerException {
        return ImageUtils.loadImage(getResource(location));
    }

    public static String getResourceAsString(String location) throws IOException {
        return new String(getResourceAsBytes(location));
    }

    public static byte[] getResourceAsBytes(String location) throws IOException {
        return IOUtils.toByteArray(getResource(location));
    }

    public static URL getResourceAsURL(String location) throws NullPointerException {
        return TurtyAPI.class.getClassLoader().getResource(location);
    }

    public static Optional<String> getEnvironmentValue(String key) {
        return Optional.ofNullable(environment.get(key, null));
    }

    public static String getGoogleAPIKey() {
        return getEnvironmentValue("GOOGLE_API_KEY").orElseThrow(
                () -> new IllegalStateException("No Google API key found!"));
    }

    public static String getTwitchClientId() {
        return getEnvironmentValue("TWITCH_CLIENT_ID").orElseThrow(
                () -> new IllegalStateException("No Twitch client ID found!"));
    }

    public static String getTwitchClientSecret() {
        return getEnvironmentValue("TWITCH_CLIENT_SECRET").orElseThrow(
                () -> new IllegalStateException("No Twitch client secret found!"));
    }

    public static String getGithubToken() {
        return getEnvironmentValue("GITHUB_TOKEN").orElseThrow(
                () -> new IllegalStateException("No GitHub token found!"));
    }

    private static void loadKeyData() {
        try {
            if(Files.notExists(keysPath)) {
                Constants.LOGGER.error("API keys file does not exist at path: {}", keysPath.toAbsolutePath());
                System.exit(1);

                return;
            }

            String content = Files.readString(keysPath);
            JsonArray keys = Constants.GSON.fromJson(content, JsonArray.class);
            for (JsonElement element : keys) {
                if (!element.isJsonObject())
                    continue;

                JsonObject obj = element.getAsJsonObject();
                JsonElement keyElement = obj.get("key");
                if (keyElement == null || !keyElement.isJsonPrimitive())
                    continue;

                String key = keyElement.getAsString();
                String comment = obj.has("comment") ? obj.get("comment").getAsString() : null;
                JsonArray allowedIPs = obj.has("allowedIPs") ? obj.getAsJsonArray("allowedIPs") : new JsonArray();
                JsonArray allowedEndpoints = obj.has("allowedEndpoints") ? obj.getAsJsonArray("allowedEndpoints") : new JsonArray();

                List<String> ipList = Constants.GSON.fromJson(allowedIPs, new TypeToken<List<String>>() {}.getType());
                List<String> endpointList = Constants.GSON.fromJson(allowedEndpoints, new TypeToken<List<String>>() {}.getType());
                API_KEYS.put(key, new KeyData(key, comment, ipList, endpointList));
            }
        } catch (IOException exception) {
            Constants.LOGGER.error("Failed to read API keys file!", exception);
        }
    }

    public static boolean isValidApiKey(String key) {
        return API_KEYS.containsKey(key);
    }

    public static boolean isAllowedIP(String key, String ip) {
        KeyData data = API_KEYS.get(key);
        return data != null && (data.allowedIPs.isEmpty() || data.allowedIPs.contains(ip));
    }

    public static boolean isAllowedEndpoint(String key, String endpoint) {
        KeyData data = API_KEYS.get(key);
        return data != null && (data.allowedEndpoints.isEmpty() || data.allowedEndpoints.contains(endpoint.substring(1)));
    }

    public static boolean validateRequest(Context ctx, int rateLimit) {
        String apiKey = ctx.queryParam("apiKey");
        if (apiKey == null || apiKey.isBlank()) {
            ctx.status(HttpStatus.UNAUTHORIZED).result("You must specify an API key!");
            return false;
        }

        if (!isValidApiKey(apiKey)) {
            ctx.status(HttpStatus.UNAUTHORIZED).result("Invalid API key!");
            return false;
        }

        String ip = ctx.ip();
        if (!isAllowedIP(apiKey, ip)) {
            ctx.status(HttpStatus.FORBIDDEN).result("Your IP is not allowed to use this API key!");
            return false;
        }

        String endpoint = ctx.path();
        if (!isAllowedEndpoint(apiKey, endpoint)) {
            ctx.status(HttpStatus.FORBIDDEN).result("This endpoint is not allowed for this API key!");
            return false;
        }

        NaiveRateLimit.requestPerTimeUnit(ctx, rateLimit, TimeUnit.SECONDS);
        return true;
    }

    public record KeyData(String key, String comment, List<String> allowedIPs, List<String> allowedEndpoints) {}
}
