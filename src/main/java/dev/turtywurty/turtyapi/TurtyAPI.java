package dev.turtywurty.turtyapi;

import dev.turtywurty.turtyapi.geography.TerritoryManager;

public class TurtyAPI {
    public static void main(String[] args) {
        Constants.LOGGER.info("Starting TurtyAPI!");

        TerritoryManager.load();
        RouteManager.init();
        Testing.start(args);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            Testing.shutdown();
            RouteManager.shutdown();
            Constants.LOGGER.info("Shutting down!");
        }));

        Constants.LOGGER.info("Started TurtyAPI!");
    }
}
