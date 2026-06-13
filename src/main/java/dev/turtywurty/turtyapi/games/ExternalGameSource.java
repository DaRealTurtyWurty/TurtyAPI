package dev.turtywurty.turtyapi.games;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

@Getter
public enum ExternalGameSource {
    STEAM(1),
    GOG(5),
    YOUTUBE(10),
    MICROSOFT(11),
    APPLE(13),
    TWITCH(14),
    ANDROID(15),
    AMAZON_ASIN(20),
    AMAZON_LUNA(22),
    AMAZON_ADG(23),
    EPIC_GAME_STORE(26),
    OCULUS(28),
    UTOMIK(29),
    ITCH_IO(30),
    XBOX_MARKETPLACE(31),
    KARTRIDGE(32),
    PLAYSTATION_STORE_US(36),
    FOCUS_ENTERTAINMENT(37),
    XBOX_GAME_PASS_ULTIMATE_CLOUD(54),
    GAMEJOLT(55);

    private final int id;

    ExternalGameSource(int id) {
        this.id = id;
    }

    public String getName() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static Optional<ExternalGameSource> fromName(@NotNull String name) {
        String normalizedName = name.strip()
                .toUpperCase(Locale.ROOT)
                .replace('-', '_')
                .replace(' ', '_');

        try {
            return Optional.of(valueOf(normalizedName));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

    public static String supportedNames() {
        return Arrays.stream(values())
                .map(ExternalGameSource::getName)
                .collect(Collectors.joining(", "));
    }
}
