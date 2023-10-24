package dev.turtywurty.turtyapi.minecraft;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.turtywurty.turtyapi.Constants;
import org.apache.commons.io.IOUtils;
import org.json.XML;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ParchmentVersions {
    private static final String PARCHMENT_MAVEN_META = "https://ldtteam.jfrog.io/artifactory/parchmentmc-public/org/parchmentmc/data/parchment-%s/maven-metadata.xml";
    private static final ScheduledExecutorService EXECUTOR_SERVICE = Executors.newSingleThreadScheduledExecutor();
    private static final LinkedHashMap<String, String> ALL_PARCHMENT_VERSIONS = internal_getAllParchmentVersions();
    private static final List<Consumer<List<ParchmentUpdate>>> UPDATE_LISTENERS = new ArrayList<>();

    static {
        EXECUTOR_SERVICE.scheduleAtFixedRate(() -> {
            LinkedHashMap<String, String> newVersions = internal_getAllParchmentVersions();
            LinkedHashMap<String, String> oldVersions = ALL_PARCHMENT_VERSIONS;

            LinkedHashMap<String, String> addedVersions = new LinkedHashMap<>();
            newVersions.forEach((mcVersion, parchmentVersion) -> {
                if (!oldVersions.containsKey(mcVersion)) {
                    addedVersions.put(mcVersion, parchmentVersion);
                }
            });

            LinkedHashMap<String, String> removedVersions = new LinkedHashMap<>();
            oldVersions.forEach((mcVersion, parchmentVersion) -> {
                if (!newVersions.containsKey(mcVersion)) {
                    removedVersions.put(mcVersion, parchmentVersion);
                }
            });

            ALL_PARCHMENT_VERSIONS.clear();
            ALL_PARCHMENT_VERSIONS.putAll(newVersions);

            List<ParchmentUpdate> updates = new ArrayList<>();
            addedVersions.forEach((mcVersion, parchmentVersion) -> updates.add(new ParchmentUpdate(mcVersion, parchmentVersion, false)));
            removedVersions.forEach((mcVersion, parchmentVersion) -> updates.add(new ParchmentUpdate(mcVersion, parchmentVersion, true)));
            UPDATE_LISTENERS.forEach(listener -> listener.accept(updates));
        }, 5, 5, TimeUnit.MINUTES);
    }

    public static String getParchmentVersion(String mcVersion) {
        if (ALL_PARCHMENT_VERSIONS == null || ALL_PARCHMENT_VERSIONS.isEmpty())
            return internal_getParchmentVersion(mcVersion);

        return ALL_PARCHMENT_VERSIONS.getOrDefault(mcVersion, internal_getParchmentVersion(mcVersion));
    }

    public static String findLatestParchment(String minecraftVersion) {
        String latestVersion = getParchmentVersion(minecraftVersion);

        int increment = 0;
        List<String> versions = MinecraftVersions.getAllMinecraftVersions().entrySet().stream().filter(Map.Entry::getValue).map(Map.Entry::getKey).toList();
        while (latestVersion.equals("Unknown")) {
            minecraftVersion = versions.stream().skip(increment++).findFirst().orElse(null);
            if (minecraftVersion == null) {
                break;
            }

            latestVersion = getParchmentVersion(minecraftVersion);
        }

        return latestVersion + "-" + minecraftVersion;
    }

    public static String findLatestParchment() {
        return findLatestParchment(MinecraftVersions.findLatestMinecraft().getFirst());
    }

    public static LinkedHashMap<String, String> getAllParchmentVersions() {
        return new LinkedHashMap<>(ALL_PARCHMENT_VERSIONS);
    }

    private static String internal_getParchmentVersion(String mcVersion) {
        try {
            String parchmentUrl = String.format(PARCHMENT_MAVEN_META, mcVersion);
            String content = IOUtils.toString(new InputStreamReader(new URL(parchmentUrl).openStream(),
                    StandardCharsets.UTF_8));
            final String xmlJsonStr = XML.toJSONObject(content).toString(1);

            final JsonObject xmlJson = Constants.GSON.fromJson(xmlJsonStr, JsonObject.class);
            final JsonObject versioning = xmlJson.getAsJsonObject("metadata").getAsJsonObject("versioning");
            final JsonArray versionsArray = versioning.getAsJsonObject("versions").getAsJsonArray("version");

            List<String> results = new ArrayList<>();
            for (final JsonElement element : versionsArray) {
                final String version = element.getAsString();
                if (!Pattern.matches("\\d+\\.\\d+(\\.\\d+)?", version)) continue;

                results.add(version);
            }

            results.sort(Comparator.comparingInt(entry -> {
                String[] split = entry.split("\\.");
                return Integer.parseInt(split[0]) * 10000 + Integer.parseInt(
                        split[1]) * 100 + (split.length > 2 ? Integer.parseInt(split[2]) : 0);
            }));

            return results.get(results.size() - 1);
        } catch (IOException exception) {
            return "Unknown";
        }
    }

    private static LinkedHashMap<String, String> internal_getAllParchmentVersions() {
        List<String> minecraftReleases = MinecraftVersions.getAllMinecraftVersions().entrySet().stream().filter(Map.Entry::getValue).map(Map.Entry::getKey).toList();
        List<String> versions = new ArrayList<>();
        for (String minecraftRelease : minecraftReleases) {
            String version = getParchmentVersion(minecraftRelease);
            if (!version.equals("Unknown")) {
                versions.add(version + "-" + minecraftRelease);
            }
        }

        return versions.stream().collect(Collectors.toMap(
                version -> version.split("-")[1],
                version -> version.split("-")[0],
                (a, b) -> b, LinkedHashMap::new));
    }

    public record ParchmentUpdate(String minecraftVersion, String parchmentVersion, boolean removed) {
    }

    public static void addUpdateListener(Consumer<List<ParchmentUpdate>> listener) {
        UPDATE_LISTENERS.add(listener);
    }

    public static void removeUpdateListener(Consumer<List<ParchmentUpdate>> listener) {
        UPDATE_LISTENERS.remove(listener);
    }
}
