package dev.turtywurty.turtyapi.geography;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import dev.turtywurty.turtyapi.Constants;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class TerritoryManager {
    private static final Map<String, Territory> TERRITORIES = new HashMap<>();

    public static void load() {
        Path path = Constants.DATA_FOLDER.resolve("geography/territory_data.json");

        if(Files.notExists(path)) {
            Constants.LOGGER.error("Failed to find territory data file!");
            return;
        }

        try {
            JsonArray array = Constants.GSON.fromJson(Files.readString(path), JsonArray.class);
            for (JsonElement element : array) {
                Territory data = Constants.GSON.fromJson(element, Territory.class);
                TERRITORIES.put(data.getCca3(), data);
            }
        } catch (IOException exception) {
            Constants.LOGGER.error("Failed to load territory data!", exception);
        }
    }

    public static Territory getTerritory(String cca3) {
        return TERRITORIES.get(cca3.toUpperCase(Locale.ROOT));
    }

    public static Map<String, Territory> getTerritories() {
        return TERRITORIES;
    }
}
