package dev.turtywurty.turtyapi.minecraft;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
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

public class QuiltVersions {
    private static final String QUILT_LOADER_META = "https://meta.quiltmc.org/v3/versions/loader";
    private static final ScheduledExecutorService EXECUTOR_SERVICE = Executors.newSingleThreadScheduledExecutor();
    private static final LinkedHashMap<String, Boolean> ALL_QUILT_VERSIONS = internal_getAllQuiltVersions();
    private static final List<Consumer<List<QuiltUpdate>>> UPDATE_LISTENERS = new ArrayList<>();

    static {
        EXECUTOR_SERVICE.scheduleAtFixedRate(() -> {
            LinkedHashMap<String, Boolean> newVersions = internal_getAllQuiltVersions();
            LinkedHashMap<String, Boolean> oldVersions = ALL_QUILT_VERSIONS;

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

            ALL_QUILT_VERSIONS.clear();
            ALL_QUILT_VERSIONS.putAll(newVersions);

            List<QuiltUpdate> updates = new ArrayList<>();
            addedVersions.forEach((version, stable) -> updates.add(new QuiltUpdate(version, stable, false)));
            removedVersions.forEach((version, stable) -> updates.add(new QuiltUpdate(version, stable, true)));
            UPDATE_LISTENERS.forEach(listener -> listener.accept(updates));
        }, 5, 5, TimeUnit.MINUTES);
    }

    public static Pair<String, String> findLatestQuilt() {
        String stable = null;
        String unstable = null;

        for (Map.Entry<String, Boolean> entry : ALL_QUILT_VERSIONS.entrySet()) {
            if (stable == null && entry.getValue()) {
                stable = entry.getKey();
            } else if (unstable == null && !entry.getValue()) {
                unstable = entry.getKey();
            }

            if (stable != null && unstable != null) {
                break;
            }
        }

        return new Pair<>(stable, unstable);
    }

    public static LinkedHashMap<String, Boolean> getAllQuiltVersions() {
        return new LinkedHashMap<>(ALL_QUILT_VERSIONS);
    }

    private static LinkedHashMap<String, Boolean> internal_getAllQuiltVersions() {
        try {
            JsonArray json = Constants.GSON.fromJson(new InputStreamReader(new URL(QUILT_LOADER_META).openStream()),
                    JsonArray.class);

            LinkedHashMap<String, Boolean> versionsMap = new LinkedHashMap<>();
            for (JsonElement jsonElement : json) {
                if (jsonElement.isJsonObject()) {
                    JsonObject jsonObject = jsonElement.getAsJsonObject();
                    String version = jsonObject.get("version").getAsString();
                    boolean stable = !(version.contains("beta") || version.contains("alpha") || version.contains("snapshot"));
                    versionsMap.put(version, stable);
                }
            }

            return versionsMap;
        } catch (IOException exception) {
            exception.printStackTrace();
            return new LinkedHashMap<>();
        }
    }

    public record QuiltUpdate(String version, boolean stable, boolean removed) {
    }

    public static void addUpdateListener(Consumer<List<QuiltUpdate>> listener) {
        UPDATE_LISTENERS.add(listener);
    }

    public static void removeUpdateListener(Consumer<List<QuiltUpdate>> listener) {
        UPDATE_LISTENERS.remove(listener);
    }

    public static void init() {
        // Just to make sure the class is loaded
        Constants.LOGGER.info("Loaded Quilt versions!");
    }
}
