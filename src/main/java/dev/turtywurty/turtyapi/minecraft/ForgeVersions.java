package dev.turtywurty.turtyapi.minecraft;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.turtywurty.turtyapi.Constants;
import kotlin.Pair;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class ForgeVersions {
    private static final String FORGE_PROMOS = "https://files.minecraftforge.net/net/minecraftforge/forge/promotions_slim.json";
    private static final ScheduledExecutorService EXECUTOR_SERVICE = Executors.newSingleThreadScheduledExecutor();
    private static final LinkedHashMap<String, Boolean> ALL_FORGE_VERSIONS = internal_getAllForgeVersions();
    private static final List<Consumer<List<ForgeVersions.ForgeUpdate>>> UPDATE_LISTENERS = new ArrayList<>();

    static {
        EXECUTOR_SERVICE.scheduleAtFixedRate(() -> {
            LinkedHashMap<String, Boolean> newVersions = internal_getAllForgeVersions();
            LinkedHashMap<String, Boolean> oldVersions = ALL_FORGE_VERSIONS;

            LinkedHashMap<String, Boolean> addedVersions = new LinkedHashMap<>();
            newVersions.forEach((version, release) -> {
                if (!oldVersions.containsKey(version)) {
                    addedVersions.put(version, release);
                }
            });

            LinkedHashMap<String, Boolean> removedVersions = new LinkedHashMap<>();
            oldVersions.forEach((version, release) -> {
                if (!newVersions.containsKey(version)) {
                    removedVersions.put(version, release);
                }
            });

            ALL_FORGE_VERSIONS.clear();
            ALL_FORGE_VERSIONS.putAll(newVersions);

            List<ForgeVersions.ForgeUpdate> updates = new ArrayList<>();
            addedVersions.forEach((version, release) -> updates.add(new ForgeVersions.ForgeUpdate(version, release, false)));
            removedVersions.forEach((version, release) -> updates.add(new ForgeVersions.ForgeUpdate(version, release, true)));
            UPDATE_LISTENERS.forEach(listener -> listener.accept(updates));
        }, 5, 5, TimeUnit.MINUTES);
    }

    public static Pair<String, String> findLatestForge() {
        String unstable = null;
        String recommended = null;

        for (Map.Entry<String, Boolean> entry : ALL_FORGE_VERSIONS.entrySet()) {
            if (unstable == null && entry.getValue()) {
                unstable = entry.getKey();
            } else if (recommended == null && !entry.getValue()) {
                recommended = entry.getKey();
            } else if (unstable != null && recommended != null) {
                break;
            }
        }

        return new Pair<>(unstable, recommended);
    }

    public static LinkedHashMap<String, Boolean> getAllForgeVersions() {
        return new LinkedHashMap<>(ALL_FORGE_VERSIONS);
    }

    private static LinkedHashMap<String, Boolean> internal_getAllForgeVersions() {
        try {
            JsonObject promos = Constants.GSON.fromJson(new InputStreamReader(new URL(FORGE_PROMOS).openStream()),
                    JsonObject.class).getAsJsonObject("promos");

            List<Map.Entry<String, JsonElement>> elements = new ArrayList<>(promos.entrySet().stream()
                    .sorted(Comparator.comparingInt(
                            entry -> Integer.parseInt(entry.getKey().split("\\.")[1].split("-")[0]))).toList());
            Collections.reverse(elements);

            LinkedHashMap<String, Boolean> versionsMap = new LinkedHashMap<>();
            for (Map.Entry<String, JsonElement> entry : elements) {
                String version = entry.getKey() + "-" + entry.getValue().getAsString();
                if (version.contains("-recommended")) {
                    version = version.replace("-recommended", "");
                } else if (version.contains("-latest")) {
                    version = version.replace("-latest", "");
                }

                versionsMap.put(version, entry.getKey().contains("-recommended"));
            }

            return versionsMap;
        } catch (IOException exception) {
            exception.printStackTrace();
            return new LinkedHashMap<>();
        }
    }

    public record ForgeUpdate(String version, boolean recommended, boolean removed) {
    }

    public static void addUpdateListener(Consumer<List<ForgeUpdate>> listener) {
        UPDATE_LISTENERS.add(listener);
    }

    public static void removeUpdateListener(Consumer<List<ForgeUpdate>> listener) {
        UPDATE_LISTENERS.remove(listener);
    }

    public static void init() {
        // Just to make sure the class is loaded
        Constants.LOGGER.info("Loaded Forge versions!");
    }
}
