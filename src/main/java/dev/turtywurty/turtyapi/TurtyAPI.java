package dev.turtywurty.turtyapi;

import dev.turtywurty.turtyapi.geography.RegionManager;
import dev.turtywurty.turtyapi.image.ImageUtils;
import org.apache.commons.io.IOUtils;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

public class TurtyAPI {
    private static boolean isDev;

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
}
