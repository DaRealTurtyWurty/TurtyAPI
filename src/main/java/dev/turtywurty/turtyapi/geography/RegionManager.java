package dev.turtywurty.turtyapi.geography;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import dev.turtywurty.turtyapi.Constants;
import dev.turtywurty.turtyapi.TurtyAPI;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class RegionManager {
    private static final Map<String, Region> REGIONS = new HashMap<>();

    public static void init() {
        REGIONS.clear();

        try {
            String json = TurtyAPI.getResourceAsString("geography/region_data.json");

            JsonArray array = Constants.GSON.fromJson(json, JsonArray.class);
            for (JsonElement element : array) {
                Region data = Constants.GSON.fromJson(element, Region.class);
                REGIONS.put(data.getCca3(), data);
            }
        } catch (IOException exception) {
            Constants.LOGGER.error("Failed to load region data!", exception);
            return;
        }

        Constants.LOGGER.info("Loaded region data!");
    }

    public static Region getRegion(String cca3) {
        return REGIONS.get(cca3.toUpperCase(Locale.ROOT));
    }

    public static Map<String, Region> getRegions() {
        return REGIONS;
    }

    public static Region getRandomRegion() {
        Map<String, Region> regions = getRegions();
        return regions.values()
                .stream()
                .skip((int) (regions.size() * Math.random()))
                .findFirst()
                .orElse(null);
    }

    public static Map<String, Region> getRegions(boolean excludeTerritories, boolean excludeIslands, boolean excludeCountries, boolean excludeMainland) {
        return REGIONS.entrySet().stream()
                .filter(entry -> {
                    Region region = entry.getValue();
                    if (excludeTerritories && region.isTerritory()) {
                        return false;
                    }

                    if (excludeIslands && region.isIsland()) {
                        return false;
                    }

                    if (excludeCountries && region.isCountry()) {
                        return false;
                    }

                    return !excludeMainland || !region.isMainland();
                })
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public static Region getRandomRegion(boolean excludeTerritories, boolean excludeIslands, boolean excludeCountries, boolean excludeMainland) {
        Map<String, Region> regions = getRegions(excludeTerritories, excludeIslands, excludeCountries, excludeMainland);
        return regions.values()
                .stream()
                .skip((int) (regions.size() * Math.random()))
                .findFirst()
                .orElse(null);
    }
}
