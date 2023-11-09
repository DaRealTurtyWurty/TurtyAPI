package dev.turtywurty.turtyapi.fun;

import dev.turtywurty.turtyapi.Constants;
import dev.turtywurty.turtyapi.TurtyAPI;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class WouldYouRatherManager {
    private WouldYouRatherManager() {}

    private static final List<WouldYouRather> WOULD_YOU_RATHERS = new ArrayList<>();
    private static final List<WouldYouRather> NSFW_WOULD_YOU_RATHERS = new ArrayList<>();

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

        final List<String> badWordList = List.of(
                "penis",
                "vagina",
                "pussy",
                "cock",
                "dick",
                "cum",
                "semen",
                "fuck",
                "sex",
                "testicles",
                "shit",
                "piss",
                "blowjob",
                "handjob",
                "genital",
                "genitals",
                "genitalia",
                "orgy",
                "glory hole",
                "masturbate",
                "jerk off",
                "jack off",
                "tits",
                "boobs",
                "boobies",
                "titties",
                "ass",
                "asshole",
                "anal",
                "missionary",
                "cowgirl",
                "sidefuck",
                "doggy style",
                "sexual",
                "cocaine",
                "weed",
                "majorana",
                "meth",
                "methamphetamine",
                "lsd",
                "mushrooms",
                "ketamine",
                "cloud 9",
                "ecstasy",
                "mdma",
                "heroin",
                "crack",
                "ghb",
                "salvia",
                "opium",
                "boobjob",
                "porn",
                "nigger",
                "nigga",
                "wank",
                "whore",
                "slut",
                "cunt",
                "bollocks",
                "beastiality",
                "bbc",
                "2g1c",
                "2 girls 1 cup",
                "boner",
                "erection",
                "bbw",
                "bdsm",
                "bondage",
                "bangbros",
                "brazzers",
                "pornhub",
                "camel toe",
                "onlyfans",
                "clit",
                "clitoris",
                "creampie",
                "cumshot",
                "cumming",
                "rape",
                "deep throat",
                "dildo",
                "vibrator",
                "ejaculation",
                "dominatrix",
                "erotic",
                "faggot",
                "tranny",
                "squirting",
                "fingering",
                "fisting",
                "fellatio",
                "footjob",
                "futa",
                "yuri",
                "oppai",
                "yiff",
                "hentai",
                "gangbang",
                "orgasm",
                "orgasmic",
                "incest",
                "intercourse",
                "masturbate",
                "playboy",
                "queef",
                "pubes",
                "raping",
                "rapist",
                "scissoring",
                "strip club",
                "stripping",
                "threesome",
                "viagra",
                "wet dream",
                "xxx",
                "tubgirl",
                "tub girl",
                "2k1s",
                "2 kids 1 sandbox",
                "1g1j",
                "1 guy 1 jar",
                "rule34",
                "rule 34",
                "sperm",
                "period"
        );

        List<WouldYouRather> wouldYouRatherList = new ArrayList<>(WOULD_YOU_RATHERS);
        for (WouldYouRather wouldYouRather : wouldYouRatherList) {
            String optionsConcat = (wouldYouRather.optionA() + " " + wouldYouRather.optionB()).toLowerCase(Locale.ROOT).trim();
            for (String badWord : badWordList) {
                if(optionsConcat.contains(badWord)) {
                    NSFW_WOULD_YOU_RATHERS.add(wouldYouRather);
                    WOULD_YOU_RATHERS.remove(wouldYouRather);
                }
            }
        }

        Constants.LOGGER.info("Loaded " + WOULD_YOU_RATHERS.size() + " would you rather questions!");
        Constants.LOGGER.info("Loaded " + NSFW_WOULD_YOU_RATHERS.size() + " nsfw would you rather questions!");
    }

    public static WouldYouRather getRandomWouldYouRather(boolean includeNSFW) {
        if(includeNSFW) {
            return Constants.RANDOM.nextBoolean() ?
                    getRandomWouldYouRather() :
                    getRandomNSFWWouldYouRather();
        }

        return getRandomWouldYouRather();
    }

    public static WouldYouRather getRandomWouldYouRather() {
        return WOULD_YOU_RATHERS.get(Constants.RANDOM.nextInt(WOULD_YOU_RATHERS.size()));
    }

    public static WouldYouRather getRandomNSFWWouldYouRather() {
        return NSFW_WOULD_YOU_RATHERS.get(Constants.RANDOM.nextInt(NSFW_WOULD_YOU_RATHERS.size()));
    }
}
