package dev.turtywurty.turtyapi.words;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.turtywurty.turtyapi.Constants;
import dev.turtywurty.turtyapi.TurtyAPI;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class WordManager {
    private static final String DICTIONARY_API_URL = "https://api.dictionaryapi.dev/api/v2/entries/en/%s";
    private static final String WORD_FREQUENCY_RESOURCE = "words/wordfreq_en.tsv";
    private static final List<String> WORDS = List.of(getAllWordsRaw());
    private static final Map<String, Optional<WordDefinition>> DEFINITION_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, Double> WORD_FREQUENCIES = loadWordFrequencies();

    public static void init() {
        Constants.LOGGER.info("WordManager has been loaded with {} frequency entries!", WORD_FREQUENCIES.size());
    }

    private static String[] getAllWordsRaw() {
        try {
            return TurtyAPI.getResourceAsString("words/all_words.txt").split("\n");
        } catch (IOException exception) {
            Constants.LOGGER.error("Failed to load all words from file!", exception);
            return new String[0];
        }
    }

    public static List<String> getAllWords() {
        return WORDS;
    }

    public static String getRandomWord() {
        return WORDS.get(Constants.RANDOM.nextInt(WORDS.size()));
    }

    public static List<String> getRandomWords(int amount, boolean unique) {
        List<String> words = new ArrayList<>();

        while (words.size() < amount) {
            String word = getRandomWord();

            if (unique && words.contains(word)) {
                continue;
            }

            words.add(word);
        }

        return words;
    }

    public static List<String> getRandomWords(int amount) {
        return getRandomWords(amount, false);
    }

    public static List<String> getAllWords(int length) {
        List<String> words = new ArrayList<>();
        try {
            String[] lines = TurtyAPI.getResourceAsString("words/" + length + "_letter_words.txt").split("\n");
            words.addAll(Arrays.asList(lines));
        } catch (IOException exception) {
            Constants.LOGGER.error("Failed to load {}-letter words!", length, exception);
        }

        return words;
    }

    public static List<String> getAllWords(int minLength, int maxLength) {
        List<String> words = new ArrayList<>();

        for (int length = minLength; length <= maxLength; length++) {
            words.addAll(getAllWords(length));
        }

        return words;
    }

    public static Optional<String> getRandomWord(int length) {
        List<String> words = getAllWords(length);
        if (words.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(words.get(Constants.RANDOM.nextInt(words.size())));
    }

    public static List<String> getRandomWords(int amount, int length, boolean unique) {
        List<String> words = new ArrayList<>();

        while (words.size() < amount) {
            Optional<String> word = getRandomWord(length);

            if (unique && words.contains(word.orElse(""))) {
                continue;
            }

            words.add(word.orElse(""));
        }

        return words;
    }

    public static List<String> getRandomWords(int amount, int length) {
        return getRandomWords(amount, length, false);
    }

    public static List<String> getStartingWith(List<String> words, String start) {
        return words.stream().filter(word -> word.startsWith(start)).toList();
    }

    public static List<String> getWithMaximum(List<String> words, int max) {
        return getWithMaximum(words, max, false);
    }

    public static List<String> getWithMaximum(List<String> words, int max, boolean randomize) {
        max = Math.min(max, words.size());

        if (randomize) {
            List<String> randomized = new ArrayList<>(words);
            Collections.shuffle(randomized);
            return randomized.subList(0, max);
        }

        return words.subList(0, max);
    }

    public static List<String> getCommonWords(List<String> words, int amount, boolean randomize) {
        if (words.isEmpty() || amount <= 0) {
            return List.of();
        }

        List<ScoredWord> scoredWords = words.stream()
                .map(String::toLowerCase)
                .distinct()
                .map(word -> new ScoredWord(word, getPopularityScore(word)))
                .sorted(Comparator.comparingDouble(ScoredWord::score).reversed()
                        .thenComparing(ScoredWord::word))
                .toList();

        int safeAmount = Math.min(amount, scoredWords.size());
        if (!randomize) {
            return scoredWords.subList(0, safeAmount).stream()
                    .map(ScoredWord::word)
                    .toList();
        }

        int poolSize = Math.min(scoredWords.size(), Math.max(safeAmount * 10, safeAmount));
        List<String> pool = new ArrayList<>(scoredWords.subList(0, poolSize).stream()
                .map(ScoredWord::word)
                .toList());
        Collections.shuffle(pool);
        return pool.subList(0, safeAmount);
    }

    public static double getPopularityScore(String word) {
        String normalized = normalizeWord(word);
        if (normalized.isBlank()) {
            return Double.NEGATIVE_INFINITY;
        }

        return WORD_FREQUENCIES.getOrDefault(normalized, Double.NEGATIVE_INFINITY);
    }

    private static Map<String, Double> loadWordFrequencies() {
        try {
            String[] lines = TurtyAPI.getResourceAsString(WORD_FREQUENCY_RESOURCE).split("\n");
            Map<String, Double> frequencies = new HashMap<>();

            for (String line : lines) {
                String trimmed = line.trim();
                if (trimmed.isBlank() || trimmed.startsWith("#")) {
                    continue;
                }

                String[] parts = trimmed.split("\t");
                if (parts.length != 2) {
                    continue;
                }

                String word = normalizeWord(parts[0]);
                if (word.isBlank()) {
                    continue;
                }

                try {
                    frequencies.put(word, Double.parseDouble(parts[1]));
                } catch (NumberFormatException exception) {
                    Constants.LOGGER.debug("Skipping malformed frequency entry: {}", trimmed);
                }
            }

            return Map.copyOf(frequencies);
        } catch (IOException exception) {
            Constants.LOGGER.warn("Failed to load word frequencies from {}", WORD_FREQUENCY_RESOURCE, exception);
            return Map.of();
        }
    }

    public static Optional<WordDefinition> getDefinition(String word) {
        String normalized = normalizeWord(word);
        if (normalized.isBlank()) {
            return Optional.empty();
        }

        return DEFINITION_CACHE.computeIfAbsent(normalized, WordManager::fetchDefinition);
    }

    public static boolean isWord(String word) {
        if (word == null || !word.matches("[a-zA-Z]+") || word.isBlank()) {
            return false;
        }

        return getAllWords(word.length()).contains(word.toLowerCase(Locale.ROOT));
    }

    private static Optional<WordDefinition> fetchDefinition(String word) {
        Request request = new Request.Builder()
                .url(DICTIONARY_API_URL.formatted(URLEncoder.encode(word, StandardCharsets.UTF_8)))
                .header("Accept", "application/json")
                .build();

        try (Response response = Constants.HTTP_CLIENT.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                return Optional.empty();
            }

            ResponseBody body = response.body();
            if (body == null) {
                return Optional.empty();
            }

            JsonElement root = Constants.GSON.fromJson(body.charStream(), JsonElement.class);
            if (!root.isJsonArray()) {
                return Optional.empty();
            }

            JsonArray entries = root.getAsJsonArray();
            for (JsonElement entryElement : entries) {
                if (!entryElement.isJsonObject()) {
                    continue;
                }

                JsonArray meanings = entryElement.getAsJsonObject().getAsJsonArray("meanings");
                if (meanings == null) {
                    continue;
                }

                for (JsonElement meaningElement : meanings) {
                    if (!meaningElement.isJsonObject()) {
                        continue;
                    }

                    JsonObject meaning = meaningElement.getAsJsonObject();
                    String partOfSpeech = meaning.has("partOfSpeech") && !meaning.get("partOfSpeech").isJsonNull()
                            ? meaning.get("partOfSpeech").getAsString()
                            : "";
                    JsonArray definitions = meaning.getAsJsonArray("definitions");
                    if (definitions == null) {
                        continue;
                    }

                    for (JsonElement definitionElement : definitions) {
                        if (!definitionElement.isJsonObject()) {
                            continue;
                        }

                        JsonElement definition = definitionElement.getAsJsonObject().get("definition");
                        if (definition == null || definition.isJsonNull()) {
                            continue;
                        }

                        String cleaned = sanitizeDefinition(definition.getAsString());
                        if (!cleaned.isBlank()) {
                            return Optional.of(new WordDefinition(word, cleaned, partOfSpeech));
                        }
                    }
                }
            }
        } catch (IOException exception) {
            Constants.LOGGER.warn("Failed to fetch definition for word {}", word, exception);
        }

        return Optional.empty();
    }

    private static String sanitizeDefinition(String definition) {
        String cleaned = definition == null ? "" : definition.trim().replaceAll("\\s+", " ");
        if (cleaned.isBlank()) {
            return "";
        }

        if (cleaned.length() > 220) {
            cleaned = cleaned.substring(0, 217) + "...";
        }

        return cleaned;
    }

    private static String normalizeWord(String word) {
        if (word == null) {
            return "";
        }

        String normalized = word.trim().toLowerCase(Locale.ROOT);
        if (!normalized.matches("[a-z]+")) {
            return "";
        }

        return normalized;
    }

    private record ScoredWord(String word, double score) {
    }
}
