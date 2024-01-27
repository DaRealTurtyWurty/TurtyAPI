package dev.turtywurty.turtyapi.minecraft;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.turtywurty.turtyapi.Constants;
import kotlin.Pair;
import org.apache.commons.io.IOUtils;
import org.json.XML;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class NeoforgeVersions {
    private static final String NEOFORGE_PROMOS = "https://maven.neoforged.net/net/neoforged/neoforge/maven-metadata.xml";
    private static final ScheduledExecutorService EXECUTOR_SERVICE = Executors.newSingleThreadScheduledExecutor();
    private static final LinkedHashMap<String, Boolean> ALL_NEOFORGE_VERSIONS = internal_getAllNeoforgeVersions();
    private static final List<Consumer<List<NeoforgeUpdate>>> UPDATE_LISTENERS = new ArrayList<>();

    static {
        EXECUTOR_SERVICE.scheduleAtFixedRate(() -> {
            LinkedHashMap<String, Boolean> newVersions = internal_getAllNeoforgeVersions();
            LinkedHashMap<String, Boolean> oldVersions = ALL_NEOFORGE_VERSIONS;

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

            ALL_NEOFORGE_VERSIONS.clear();
            ALL_NEOFORGE_VERSIONS.putAll(newVersions);

            List<NeoforgeUpdate> updates = new ArrayList<>();
            addedVersions.forEach((version, release) -> updates.add(new NeoforgeUpdate(version, release, false)));
            removedVersions.forEach((version, release) -> updates.add(new NeoforgeUpdate(version, release, true)));
            UPDATE_LISTENERS.forEach(listener -> listener.accept(updates));
        }, 5, 5, TimeUnit.MINUTES);
    }

    public static Pair<String, String> findLatestNeoforge() {
        String unstable = null;
        String recommended = null;

        for (Map.Entry<String, Boolean> entry : ALL_NEOFORGE_VERSIONS.entrySet()) {
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

    public static LinkedHashMap<String, Boolean> getAllNeoforgeVersions() {
        return new LinkedHashMap<>(ALL_NEOFORGE_VERSIONS);
    }

    private static LinkedHashMap<String, Boolean> internal_getAllNeoforgeVersions() {
        try {
            String content = IOUtils.toString(new InputStreamReader(new URL(NEOFORGE_PROMOS).openStream(), StandardCharsets.UTF_8));
            String xmlJsonStr = XML.toJSONObject(content).toString(1);

            JsonObject xmlJson = Constants.GSON.fromJson(xmlJsonStr, JsonObject.class);
            JsonObject versioning = xmlJson.getAsJsonObject("metadata").getAsJsonObject("versioning");
            JsonArray versionsArray = versioning.getAsJsonObject("versions").getAsJsonArray("version");

            final LinkedHashMap<String, Boolean> versionsMap = new LinkedHashMap<>();
            versionsArray.forEach(element -> {
                String version = element.getAsString();
                boolean isRelease = !version.endsWith("-beta");
                versionsMap.put(version, isRelease);
            });

            // sort by version
            return versionsMap.entrySet().stream().sorted((entry0, entry1) -> {
                String version0 = entry0.getKey().replace("-beta", "");
                String version1 = entry1.getKey().replace("-beta", "");

                String[] split0 = version0.split("\\.");
                String[] split1 = version1.split("\\.");

                for (int i = 0; i < Math.min(split0.length, split1.length); i++) {
                    int num0 = Integer.parseInt(split0[i]);
                    int num1 = Integer.parseInt(split1[i]);

                    if (num0 != num1) {
                        return Integer.compare(num1, num0);
                    }
                }

                return Integer.compare(split1.length, split0.length);
            }).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (entry0, entry1) -> entry0, LinkedHashMap::new));
        } catch (IOException exception) {
            exception.printStackTrace();
            return new LinkedHashMap<>();
        }
    }

    public static void addUpdateListener(Consumer<List<NeoforgeUpdate>> listener) {
        UPDATE_LISTENERS.add(listener);
    }

    public static void removeUpdateListener(Consumer<List<NeoforgeUpdate>> listener) {
        UPDATE_LISTENERS.remove(listener);
    }

    public static void init() {
        // Just to make sure the class is loaded
    }

    public record NeoforgeUpdate(String version, boolean recommended, boolean removed) {
    }
}
