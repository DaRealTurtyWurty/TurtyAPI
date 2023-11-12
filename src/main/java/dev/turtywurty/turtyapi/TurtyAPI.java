package dev.turtywurty.turtyapi;

import dev.turtywurty.turtyapi.geography.RegionManager;
import dev.turtywurty.turtyapi.image.ImageUtils;
import io.github.cdimascio.dotenv.Dotenv;
import org.apache.commons.io.IOUtils;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Optional;

public class TurtyAPI {
    private static boolean isDev;
    private static final Dotenv ENVIRONMENT = Dotenv.load();

    public static void main(String[] args) {
        isDev = args.length > 0 && args[0].equalsIgnoreCase("dev");
        Constants.LOGGER.info("Starting TurtyAPI!");

        RegionManager.load();
        RouteManager.init();
        Testing.start(args);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            Testing.shutdown();
            RouteManager.shutdown();
            Constants.LOGGER.info("Shutting down!");
        }));

        Constants.LOGGER.info("Started TurtyAPI!");
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
        return Optional.ofNullable(ENVIRONMENT.get(key, null));
    }

    public static String getGoogleAPIKey() {
        return getEnvironmentValue("GOOGLE_API_KEY").orElseThrow(
                () -> new IllegalStateException("No Google API key found!"));
    }

    public static String getAPIKey() {
        return getEnvironmentValue("API_KEY").orElseThrow(
                () -> new IllegalStateException("No API key found!"));
    }
}
