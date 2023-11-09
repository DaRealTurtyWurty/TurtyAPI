package dev.turtywurty.turtyapi.fun;

import dev.turtywurty.turtyapi.Constants;
import dev.turtywurty.turtyapi.TurtyAPI;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class WouldYouRatherManager {
    private WouldYouRatherManager() {}

    private static final List<WouldYouRather> WOULD_YOU_RATHERS = new ArrayList<>();

    static {
        try {
            String content = TurtyAPI.getResourceAsString("wyr/would_you_rathers.csv");
            String[] lines = content.split("\n");
            for (int index = 1; index < lines.length; index++) {
                String line = lines[index];
                if (line.isBlank())
                    continue;

                final String[] parts = line.split(",");
                if (parts.length < 4)
                    continue;

                StringBuilder optionA = new StringBuilder(parts[0]);
                int nextIndex = 1;
                boolean isOptionB = false;
                while (!isOptionB) {
                    try {
                        Integer.parseInt(parts[nextIndex].trim());
                        isOptionB = true;
                    } catch (NumberFormatException exception) {
                        optionA.append(",").append(parts[nextIndex++]);
                    }
                }

                nextIndex++;

                StringBuilder optionB = new StringBuilder(parts[nextIndex++]);
                while (true) {
                    try {
                        Integer.parseInt(parts[nextIndex].trim());
                        break;
                    } catch (NumberFormatException exception) {
                        optionB.append(",").append(parts[nextIndex++]);
                    } catch (ArrayIndexOutOfBoundsException exception) {
                        break;
                    }
                }

                WOULD_YOU_RATHERS.add(new WouldYouRather(
                        optionA.toString().trim().replace("\"", "\\\""),
                        optionB.toString().trim().replace("\"", "\\\"")));
            }
        } catch (IOException exception) {
            Constants.LOGGER.error("Failed to load would you rather questions!", exception);
        }
    }

    public static WouldYouRather getRandomWouldYouRather() {
        return WOULD_YOU_RATHERS.get(Constants.RANDOM.nextInt(WOULD_YOU_RATHERS.size()));
    }
}
