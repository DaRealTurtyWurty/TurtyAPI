package dev.turtywurty.turtyapi.words;

import dev.turtywurty.turtyapi.Constants;
import dev.turtywurty.turtyapi.TurtyAPI;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class WordManager {
    private static final List<String> WORDS = List.of(getAllWordsRaw());

    private static String[] getAllWordsRaw() {
        try {
            return TurtyAPI.getResourceAsString("words/all_words.txt").split("\n");
        } catch (IOException exception) {
            exception.printStackTrace();
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
            exception.printStackTrace();
        }

        return words;
    }

    public static List<String> getAllWords(int minLength, int maxLength) {
        List<String> words = new ArrayList<>();

        for(int length = minLength; length <= maxLength; length++) {
            words.addAll(getAllWords(length));
        }

        return words;
    }

    public static Optional<String> getRandomWord(int length) {
        List<String> words = getAllWords(length);
        if(words.isEmpty()) {
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

        if(randomize) {
            List<String> randomized = new ArrayList<>(words);
            Collections.shuffle(randomized);
            return randomized.subList(0, max);
        }

        return words.subList(0, max);
    }

    public static boolean isWord(String word) {
        if(word == null || !word.matches("[a-zA-Z]+") || word.isBlank())
            return false;

        return getAllWords(word.length()).contains(word.toLowerCase());
    }
}
