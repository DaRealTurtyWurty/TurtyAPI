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

public class MinecraftVersions {
    private static final String MINECRAFT_PISTON_META = "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json";
    private static final ScheduledExecutorService EXECUTOR_SERVICE = Executors.newSingleThreadScheduledExecutor();
    private static final LinkedHashMap<String, Boolean> ALL_MINECRAFT_VERSIONS = internal_getAllMinecraftVersions();
    private static final List<Consumer<List<MinecraftUpdate>>> UPDATE_LISTENERS = new ArrayList<>();

    static {
        EXECUTOR_SERVICE.scheduleAtFixedRate(() -> {
            LinkedHashMap<String, Boolean> newVersions = internal_getAllMinecraftVersions();
            LinkedHashMap<String, Boolean> oldVersions = ALL_MINECRAFT_VERSIONS;

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

            ALL_MINECRAFT_VERSIONS.clear();
            ALL_MINECRAFT_VERSIONS.putAll(newVersions);

            List<MinecraftUpdate> updates = new ArrayList<>();
            addedVersions.forEach((version, release) -> updates.add(new MinecraftUpdate(version, release, false)));
            removedVersions.forEach((version, release) -> updates.add(new MinecraftUpdate(version, release, true)));
            UPDATE_LISTENERS.forEach(listener -> listener.accept(updates));
        }, 5, 5, TimeUnit.MINUTES);
    }

    public static Pair<String, String> findLatestMinecraft() {
        String release = null;
        String snapshot = null;

        for (Map.Entry<String, Boolean> entry : ALL_MINECRAFT_VERSIONS.entrySet()) {
            if (release == null && entry.getValue()) {
                release = entry.getKey();
            } else if(snapshot == null && !entry.getValue()) {
                snapshot = entry.getKey();
            } else if (release != null && snapshot != null) {
                break;
            }
        }

        return new Pair<>(release, snapshot);
    }

    private static LinkedHashMap<String, Boolean> internal_getAllMinecraftVersions() {
        try {
            JsonObject json = Constants.GSON.fromJson(
                    new InputStreamReader(new URL(MINECRAFT_PISTON_META).openStream()), JsonObject.class);

            JsonArray versions = json.getAsJsonArray("versions");
            LinkedHashMap<String, Boolean> versionsMap = new LinkedHashMap<>();
            for (JsonElement version : versions) {
                JsonObject versionObject = version.getAsJsonObject();
                versionsMap.put(versionObject.get("id").getAsString(),
                        versionObject.get("type").getAsString().equals("release"));
            }

            return versionsMap;
        } catch (IOException exception) {
            exception.printStackTrace();
            return new LinkedHashMap<>();
        }
    }

    public static LinkedHashMap<String, Boolean> getAllMinecraftVersions() {
        return new LinkedHashMap<>(ALL_MINECRAFT_VERSIONS);
    }

    public record MinecraftUpdate(String version, boolean release, boolean removed) {
    }

    public static void addUpdateListener(Consumer<List<MinecraftUpdate>> listener) {
        UPDATE_LISTENERS.add(listener);
    }

    public static void removeUpdateListener(Consumer<List<MinecraftUpdate>> listener) {
        UPDATE_LISTENERS.remove(listener);
    }

    public static void init() {
        // Just to make sure the class is loaded
        Constants.LOGGER.info("Loaded Minecraft versions!");
    }
}
