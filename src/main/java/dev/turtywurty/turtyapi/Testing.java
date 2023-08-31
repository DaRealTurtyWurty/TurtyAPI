package dev.turtywurty.turtyapi;

import dev.turtywurty.turtyapi.geography.RegionManager;
import dev.turtywurty.turtyapi.steam.SteamAppCache;

import javax.imageio.ImageIO;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class Testing {
    private static final ScheduledExecutorService EXECUTOR = Executors.newSingleThreadScheduledExecutor();
    private static boolean ENABLED = true;

    private Testing() {
        throw new UnsupportedOperationException("This class cannot be instantiated!");
    }

    public static void start(String[] args) {
        for(String arg : args) {
            if(arg.equalsIgnoreCase("--testmode")) {
                ENABLED = true;
                break;
            }
        }

        run();
    }

    public static void run() {
        if(Testing.isEnabled()) {
            EXECUTOR.schedule(() -> {
                // Test
                try(InputStream stream = new URL("http://localhost:8080").openStream()) {
                    new String(stream.readAllBytes());
                } catch (IOException exception) {
                    Constants.LOGGER.error("Failed to connect to localhost!", exception);
                }

//                AtomicInteger count = new AtomicInteger();
//                RegionManager.getRegions().forEach((cca3, data) -> {
//                    try(InputStream stream = new URL("http://localhost:8080/flag/" + cca3).openStream()) {
//                        ImageIO.read(stream);
//                    } catch (IOException exception) {
//                        Constants.LOGGER.error("Failure (flag): " + cca3, exception);
//                    }
//
//                    try(InputStream stream = new URL("http://localhost:8080/outline/" + cca3).openStream()) {
//                        ImageIO.read(stream);
//                    } catch (IOException exception) {
//                        Constants.LOGGER.error("Failure (outline): " + cca3, exception);
//                    }
//
//                    count.getAndIncrement();
//                });
//
//                Constants.LOGGER.info("Finished testing! {} countries tested!", count.get());


            }, 5, TimeUnit.SECONDS);
        }
    }

    public static void shutdown() {
        EXECUTOR.shutdown();
    }

    public static boolean isEnabled() {
        return ENABLED;
    }
}
