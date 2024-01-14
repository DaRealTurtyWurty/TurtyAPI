package dev.turtywurty.turtyapi.codeguesser;

import dev.turtywurty.turtyapi.Constants;
import dev.turtywurty.turtyapi.TurtyAPI;
import org.kohsuke.github.*;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

public class CodeManager {
    private static final GitHub GITHUB;
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

    static {
        try {
            GITHUB = new GitHubBuilder()
                    .withOAuthToken(TurtyAPI.getGithubToken())
                    .build();
            DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to initialize GitHub!", exception);
        }
    }

    public static void init() {}

    public static Code findCode() {
        try {
            GHRepository repository = findRepository();
            System.out.println("Found repository: " + repository.getFullName());

            // Make a request to <Repository URL>/contents to check that it doesn't 404
            HttpURLConnection connection =
                    (HttpURLConnection) new URL(repository.getHtmlUrl().toString().replace("github.com/",
                            "api.github.com/repos/") + "/contents").openConnection();
            if (connection.getResponseCode() == 404)
                return findCode();

            connection.disconnect();

            List<GHContent> files;
            while ((files = findFiles(repository)).isEmpty()) {
                repository = findRepository();
            }

            System.out.println("Found " + files.size() + " files!");

            if (files.isEmpty())
                return findCode();

            // Finds a random file in that repository
            GHContent content = findFile(files);

            String[] parts = content.getName().split("\\.");
            String extension = parts[parts.length - 1];

            System.out.println("File extension: " + extension);
            System.out.println("File name: " + content.getName());
            String code = readCode(content);
            Code.Language language = Code.Language.fromExtension(extension).orElse(Code.Language.UNKNOWN);
            System.out.println("Language: " + language);

            return new Code(code, language);
        } catch (IOException exception) {
            Constants.LOGGER.error("Failed to find code!", exception);
            return findCode();
        }
    }

    private static Date getYearAgo() {
        return new Date(Instant.now().minus(365, ChronoUnit.DAYS).toEpochMilli());
    }

    private static GHRepository findRepository() {
        String yearAgoString = DATE_FORMAT.format(getYearAgo());

        PagedSearchIterable<GHRepository> searchBuilder = GITHUB.searchRepositories()
                .created(yearAgoString)
                .size(">1000")
                .sort(GHRepositorySearchBuilder.Sort.UPDATED)
                .order(GHDirection.DESC)
                .sort(GHRepositorySearchBuilder.Sort.STARS)
                .visibility(GHRepository.Visibility.PUBLIC)
                .list()
                .withPageSize(100);

        PagedIterator<GHRepository> iterator = searchBuilder.iterator();
        // only get 100 repositories
        List<GHRepository> repositories = new ArrayList<>(iterator.nextPage());
        System.out.println("Found " + repositories.size() + " repositories!");

        return repositories.get((int) (Math.random() * repositories.size()));
    }

    private static List<GHContent> findFiles(GHRepository repository) throws IOException {
        PagedSearchIterable<GHContent> searchBuilder = GITHUB.searchContent()
                .repo(repository.getFullName())
                .q("a")
                .list()
                .withPageSize(25);

        PagedIterator<GHContent> iterator = searchBuilder.iterator();
        List<GHContent> files = new ArrayList<>();
        // keep getting pages until we have 25 files or there are no more pages
        do {
            files.addAll(iterator.nextPage());
            files.removeIf(file -> {
                if(file.isDirectory())
                    return true;

                String[] parts = file.getName().split("\\.");
                String extension = parts[parts.length - 1];
                Code.Language language = Code.Language.fromExtension(extension).orElse(Code.Language.UNKNOWN);
                return language == Code.Language.UNKNOWN;
            });
        } while (files.size() < 25 && iterator.hasNext());


        return files;
    }

    private static String readCode(GHContent file) throws IOException {
        try (InputStream stream = file.read()) {
            return new String(stream.readAllBytes());
        }
    }

    private static GHContent findFile(List<GHContent> files) throws IOException {
        return files.get((int) (Math.random() * files.size()));
    }
}
