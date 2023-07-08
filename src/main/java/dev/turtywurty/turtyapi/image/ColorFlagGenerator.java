package dev.turtywurty.turtyapi.image;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.*;

public final class ColorFlagGenerator {
    private ColorFlagGenerator() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    public static BufferedImage create(BufferedImage image, int n) {
        List<Color> colors = extractColors(image);
        Map<Color, Integer> colorOccurrences = countColorOccurrences(colors);
        List<Color> predominantColors = findPredominantColors(colorOccurrences, n, 100D);

        return createFlag(700, 400, predominantColors);
    }

    private static List<Color> extractColors(BufferedImage image) {
        List<Color> colors = new ArrayList<>();
        int width = image.getWidth();
        int height = image.getHeight();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = image.getRGB(x, y);
                Color color = new Color(rgb);
                colors.add(color);
            }
        }

        return colors;
    }

    private static Map<Color, Integer> countColorOccurrences(List<Color> colors) {
        Map<Color, Integer> colorOccurrences = new HashMap<>();

        for (Color color : colors) {
            colorOccurrences.put(color, colorOccurrences.getOrDefault(color, 0) + 1);
        }

        return colorOccurrences;
    }

    private static List<Color> findPredominantColors(Map<Color, Integer> colorOccurrences, int numOfColors, double similarityThreshold) {
        List<Color> predominantColors = new ArrayList<>();

        // Sort colors by occurrence count
        List<Map.Entry<Color, Integer>> sortedColors = new ArrayList<>(colorOccurrences.entrySet());
        sortedColors.sort(Map.Entry.comparingByValue(Comparator.reverseOrder()));

        // Extract the most predominant colors, filtering out similar colors
        for (Map.Entry<Color, Integer> entry : sortedColors) {
            Color color = entry.getKey();

            if (!isSimilarColor(color, predominantColors, similarityThreshold)) {
                predominantColors.add(color);
            }

            if (predominantColors.size() >= numOfColors) {
                break;
            }
        }

        return predominantColors;
    }

    private static List<Color> findLeastPredominantColors(Map<Color, Integer> colorOccurrences, int numOfColors, double similarityThreshold) {
        List<Color> predominantColors = new ArrayList<>();

        // Sort colors by occurrence count
        List<Map.Entry<Color, Integer>> sortedColors = new ArrayList<>(colorOccurrences.entrySet());
        sortedColors.sort(Map.Entry.comparingByValue());

        // Extract the most predominant colors, filtering out similar colors
        for (Map.Entry<Color, Integer> entry : sortedColors) {
            Color color = entry.getKey();

            if (!isSimilarColor(color, predominantColors, similarityThreshold)) {
                predominantColors.add(color);
            }

            if (predominantColors.size() >= numOfColors) {
                break;
            }
        }

        return predominantColors;
    }

    private static boolean isSimilarColor(Color color, List<Color> colors, double similarityThreshold) {
        for (Color existingColor : colors) {
            double distance = calculateColorDistance(color, existingColor);

            if (distance <= similarityThreshold) {
                return true;
            }
        }

        return false;
    }

    private static double calculateColorDistance(Color color1, Color color2) {
        double redDiff = color1.getRed() - color2.getRed();
        double greenDiff = color1.getGreen() - color2.getGreen();
        double blueDiff = color1.getBlue() - color2.getBlue();

        return Math.sqrt(redDiff * redDiff + greenDiff * greenDiff + blueDiff * blueDiff);
    }

    private static BufferedImage createFlag(int width, int height, List<Color> colors) {
        BufferedImage flag = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = flag.createGraphics();

        int stripeHeight = (int) Math.ceil((float) height / colors.size());
        int stripeIndex = 0;

        // Sort colors based on similarity to a reference color
        Color referenceColor = colors.get(0);
        colors.sort((color1, color2) -> {
            double distance1 = calculateColorDistance(color1, referenceColor);
            double distance2 = calculateColorDistance(color2, referenceColor);
            return Double.compare(distance1, distance2);
        });

        for (int y = 0; y < height; y += stripeHeight) {
            Color stripeColor = colors.get(stripeIndex);
            g2d.setColor(stripeColor);
            g2d.fillRect(0, y, width, stripeHeight);

            stripeIndex = (stripeIndex + 1) % colors.size();
        }

        g2d.dispose();
        return flag;
    }
}
