package dev.turtywurty.turtyapi.geography;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import dev.turtywurty.turtyapi.Constants;
import dev.turtywurty.turtyapi.TurtyAPI;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class TerritoryManager {
    private static final Map<String, Territory> TERRITORIES = new HashMap<>();

    public static void load() {
        try {
            String json = TurtyAPI.getResourceAsString("geography/territory_data.json");

            JsonArray array = Constants.GSON.fromJson(json, JsonArray.class);
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

    public static Territory getRandomTerritory() {
        return TERRITORIES.values().stream().skip((int) (TERRITORIES.size() * Math.random())).findFirst().orElse(null);
    }

    public static List<String> getAllTerritories() {
        return TERRITORIES.keySet().stream().toList();
    }
}
