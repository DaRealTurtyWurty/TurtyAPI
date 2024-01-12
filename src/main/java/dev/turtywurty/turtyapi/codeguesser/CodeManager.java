package dev.turtywurty.turtyapi.codeguesser;

import dev.turtywurty.turtyapi.TurtyAPI;
import org.kohsuke.github.*;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

public class CodeManager {
    private static final List<GHRepository> REPOSITORIES;
    private static final GitHub GITHUB;

    static {
        try {
            // TODO: Switch to OAuth
            GITHUB = new GitHubBuilder().withPassword(TurtyAPI.getGithubUsername(), TurtyAPI.getGithubPassword()).build();
            REPOSITORIES = findRepositories();
            System.out.println("Shortened to " + REPOSITORIES.size() + " repositories!");
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to initialize GitHub!", exception);
        }
    }

    public static void init() {
        // Used to initialize the class
    }

    public static Code findCode() {
        try {
            GHRepository repository = findRepository();

            // Make a request to <Repository URL>/contents to check that it doesn't 404
            HttpURLConnection connection =
                    (HttpURLConnection) new URL(repository.getHtmlUrl().toString().replace("github.com/",
                            "api.github.com/repos/") + "/contents").openConnection();
            if (connection.getResponseCode() == 404)
                return findCode();

            connection.disconnect();

            List<GHContent> files = findFiles(repository, "/");
            while (files.isEmpty()) {
                repository = findRepository();
                files = findFiles(repository, "/");
            }

            files = files.stream().filter(file -> {
                if (!file.isFile()) return false;

                String[] parts = file.getName().split("\\.");
                String extension = parts[parts.length - 1];
                return Code.Language.fromExtension(extension).isPresent();
            }).toList();

            System.out.println("Found " + files.size() + " files!");

            if (files.isEmpty())
                return findCode();

            // Finds a random file in that repository
            GHContent content = findFile(files);

            String[] parts = content.getName().split("\\.");
            String extension = parts[parts.length - 1];
            while (Code.Language.fromExtension(extension).isEmpty()) {
                content = findFile(files);
                parts = content.getName().split("\\.");
                extension = parts[parts.length - 1];
                System.out.println("File extension: " + extension);
            }

            System.out.println("File extension: " + extension);
            System.out.println("File name: " + content.getName());
            String code = readCode(content);
            Code.Language language = Code.Language.fromExtension(extension).orElse(Code.Language.UNKNOWN);
            System.out.println("Language: " + language);

            return new Code(code, language);
        } catch (IOException exception) {
            return findCode();
        }
    }

    private static List<GHRepository> findRepositories() throws IOException {
        Date weekAgo = new Date(Instant.now().minus(1, ChronoUnit.DAYS).toEpochMilli());
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        String weekAgoString = dateFormat.format(weekAgo);
        PagedSearchIterable<GHRepository> searchBuilder = GITHUB.searchRepositories()
                .created(weekAgoString)
                .sort(GHRepositorySearchBuilder.Sort.UPDATED)
                .order(GHDirection.DESC)
                .sort(GHRepositorySearchBuilder.Sort.STARS)
                .visibility(GHRepository.Visibility.PUBLIC)
                .fork(GHFork.PARENT_ONLY)
                .list()
                .withPageSize(1000);
        System.out.println("Found " + searchBuilder.getTotalCount() + " repositories!");
        return searchBuilder.toList();
    }

    private static List<GHContent> findFiles(GHRepository repository, String dir) throws IOException {
        System.out.println("Repository: " + repository.getHtmlUrl() + " | Directory: " + dir);

        List<GHContent> files = new ArrayList<>();

        // List all files in directory
        List<GHContent> found = repository.getDirectoryContent(dir);
        System.out.println("Found " + found.size() + " content(s) in directory!");

        // Loop through all files in the repository
        for (GHContent content : found) {
            System.out.println("Found: " + content.getHtmlUrl());
            if (!content.isFile()) {
                // If the file is a directory, find all files in that directory
                files.addAll(findFiles(repository, content.getPath()));
                continue;
            }

            // If the file is a file, add it to the list
            files.add(content);
        }

        System.out.println(files.size() + " files found in " + repository.getHtmlUrl() + " | Directory: " + dir);

        return files;
    }

    private static GHRepository findRepository() throws IOException {
        return REPOSITORIES.get((int) (Math.random() * REPOSITORIES.size()));
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
