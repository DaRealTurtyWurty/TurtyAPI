package dev.turtywurty.turtyapi.fun;

import dev.turtywurty.turtyapi.Constants;
import dev.turtywurty.turtyapi.TurtyAPI;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

public class CelebrityManager {
    private static final List<URI> IMAGE_LIST = new ArrayList<>();

    private CelebrityManager() {}

    public static void init() {
        // NO-OP
    }

    static {
        try {
            var runningPath = new File(TurtyAPI.class.getProtectionDomain().getCodeSource().getLocation().toURI());

            // read from directory
            if(!runningPath.isFile()) {
                var celebrities = new File(runningPath, "celebrities");
                if(celebrities.exists() && celebrities.isDirectory()) {
                    var files = celebrities.listFiles();
                    if(files != null) {
                        for(var file : files) {
                            if(file.getName().endsWith(".png")) {
                                IMAGE_LIST.add(file.toURI());
                            }
                        }
                    }
                }
            }

            // read from jar
            try(var jarFile = new JarFile(runningPath)) {
                // locate `celebrities` directory
                var entries = jarFile.entries();
                while(entries.hasMoreElements()) {
                    var entry = entries.nextElement();
                    if (entry.getName().equals("celebrities/")) {
                        // read all files in `celebrities` directory
                        var celebrities = jarFile.entries();
                        while(celebrities.hasMoreElements()) {
                            var celebrity = celebrities.nextElement();
                            if (celebrity.getName().startsWith("celebrities/") && celebrity.getName().endsWith(".png")) {
                                IMAGE_LIST.add(new URI("jar:file:" + runningPath + "!/" + celebrity.getName()));
                            }
                        }
                    }
                }
            }
        } catch (IOException | URISyntaxException exception) {
            Constants.LOGGER.error("Failed to load celebrities!", exception);
        }

        Constants.LOGGER.info("Loaded {} celebrities!", IMAGE_LIST.size());
    }

    public static Optional<Celebrity> getRandomCelebrity() {
        var randomIndex = (int) (Math.random() * IMAGE_LIST.size());
        try {
            URI uri = IMAGE_LIST.get(randomIndex);

            if(uri.getScheme().equals("file")) {
                var file = new File(uri);

                String name = file.getName().replace("celebrities/", "").replace(".png", "");
                try(var stream = new FileInputStream(file)) {
                    return Optional.of(new Celebrity(name, stream.readAllBytes()));
                }
            } else if(uri.getScheme().equals("jar")) {
                String path = uri.getPath().split("!")[1];
                try(var jarFile = new JarFile(new File(new URI(uri.getSchemeSpecificPart().split("!")[0])))) {
                    ZipEntry entry = jarFile.getEntry(path);

                    String name = entry.getName().replace("celebrities/", "").replace(".png", "");
                    try (var stream = jarFile.getInputStream(entry)) {
                        return Optional.of(new Celebrity(name, stream.readAllBytes()));
                    }
                }
            }

            return Optional.empty(); // Should never reach here
        }  catch (IOException | URISyntaxException exception) {
            Constants.LOGGER.error("Failed to load celebrity!", exception);
            return Optional.empty();
        }
    }

    public record Celebrity(String name, byte[] image) {
    }
}
