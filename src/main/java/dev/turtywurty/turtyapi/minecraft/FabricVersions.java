package dev.turtywurty.turtyapi.minecraft;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import dev.turtywurty.turtyapi.Constants;
import kotlin.Pair;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class FabricVersions {
    private static final String FABRIC_LOADER_META = "https://meta.fabricmc.net/v2/versions/loader";
    private static final ScheduledExecutorService EXECUTOR_SERVICE = Executors.newSingleThreadScheduledExecutor();
    private static final LinkedHashMap<String, Boolean> ALL_FABRIC_VERSIONS = internal_getAllFabricVersions();
    private static final List<Consumer<List<FabricUpdate>>> UPDATE_LISTENERS = new ArrayList<>();

    static {
        EXECUTOR_SERVICE.scheduleAtFixedRate(() -> {
            LinkedHashMap<String, Boolean> newVersions = internal_getAllFabricVersions();
            LinkedHashMap<String, Boolean> oldVersions = ALL_FABRIC_VERSIONS;

            LinkedHashMap<String, Boolean> addedVersions = new LinkedHashMap<>();
            newVersions.forEach((version, stable) -> {
                if (!oldVersions.containsKey(version)) {
                    addedVersions.put(version, stable);
                }
            });

            LinkedHashMap<String, Boolean> removedVersions = new LinkedHashMap<>();
            oldVersions.forEach((version, stable) -> {
                if (!newVersions.containsKey(version)) {
                    removedVersions.put(version, stable);
                }
            });

            ALL_FABRIC_VERSIONS.clear();
            ALL_FABRIC_VERSIONS.putAll(newVersions);

            List<FabricUpdate> updates = new ArrayList<>();
            addedVersions.forEach((version, stable) -> updates.add(new FabricUpdate(version, stable, false)));
            removedVersions.forEach((version, stable) -> updates.add(new FabricUpdate(version, stable, true)));
            UPDATE_LISTENERS.forEach(listener -> listener.accept(updates));
        }, 5, 5, TimeUnit.MINUTES);
    }

    public static Pair<String, String> findLatestFabric() {
        String stable = null;
        String unstable = null;

        for (Map.Entry<String, Boolean> entry : ALL_FABRIC_VERSIONS.entrySet()) {
            if (stable == null && entry.getValue()) {
                stable = entry.getKey();
            } else if (unstable == null && !entry.getValue()) {
                unstable = entry.getKey();
            } else if (stable != null && unstable != null) {
                break;
            }
        }

        return new Pair<>(stable, unstable);
    }

    public static LinkedHashMap<String, Boolean> getAllFabricVersions() {
        return new LinkedHashMap<>(ALL_FABRIC_VERSIONS);
    }

    private static LinkedHashMap<String, Boolean> internal_getAllFabricVersions() {
        try {
            JsonArray json = Constants.GSON.fromJson(new InputStreamReader(new URL(FABRIC_LOADER_META).openStream()),
                    JsonArray.class);

            LinkedHashMap<String, Boolean> versionsMap = new LinkedHashMap<>();
            for (JsonElement jsonElement : json) {
                if (jsonElement.isJsonObject()) {
                    String version = jsonElement.getAsJsonObject().get("version").getAsString();
                    boolean stable = jsonElement.getAsJsonObject().has("stable") && jsonElement.getAsJsonObject()
                            .get("stable").getAsBoolean();
                    versionsMap.put(version, stable);
                }
            }

            return versionsMap;
        } catch (IOException exception) {
            Constants.LOGGER.error("Failed to get Fabric versions!", exception);
            return new LinkedHashMap<>();
        }
    }

    public static void addUpdateListener(Consumer<List<FabricUpdate>> listener) {
        UPDATE_LISTENERS.add(listener);
    }

    public static void removeUpdateListener(Consumer<List<FabricUpdate>> listener) {
        UPDATE_LISTENERS.remove(listener);
    }

    public static void init() {
        // Just to make sure the class is loaded
        Constants.LOGGER.info("Loaded Fabric versions!");
    }

    public record FabricUpdate(String version, boolean stable, boolean removed) {
    }
}
