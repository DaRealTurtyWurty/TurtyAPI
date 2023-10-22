package dev.turtywurty.turtyapi.nsfw;

import dev.turtywurty.turtyapi.Constants;
import dev.turtywurty.turtyapi.TurtyAPI;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class NSFWManager {
    public static Optional<Pornstar> getRandomPornstarWithPhoto() {
        try {
            String json = TurtyAPI.getResourceAsString("nsfw/female_pornstars.json");
            Pornstar[] pornstars = Constants.GSON.fromJson(json, Pornstar[].class);
            List<Pornstar> available = Arrays.stream(pornstars).filter(Pornstar::hasPhoto).toList();
            return Optional.of(available.get(Constants.RANDOM.nextInt(available.size())));
        } catch (IOException exception) {
            Constants.LOGGER.error("Failed to load female pornstars!", exception);
            return Optional.empty();
        }
    }
}
