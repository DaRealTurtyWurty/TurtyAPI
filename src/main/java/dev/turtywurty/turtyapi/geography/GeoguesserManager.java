package dev.turtywurty.turtyapi.geography;

import com.google.gson.JsonObject;
import dev.turtywurty.turtyapi.Constants;
import dev.turtywurty.turtyapi.TurtyAPI;
import io.github.coordinates2country.Coordinates2Country;
import kotlin.Pair;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.locationtech.jts.geom.Coordinate;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Optional;

public class GeoguesserManager {
    private static final String STREETVIEW_URL = "https://maps.googleapis.com/maps/api/streetview?location=%s,%s&size=600x400&key=" + TurtyAPI.getGoogleAPIKey();
    private static final String VALIDATE_LOCATION_URL = "https://maps.googleapis.com/maps/api/streetview/metadata?location=%s,%s&key=" + TurtyAPI.getGoogleAPIKey();

    public static Optional<Pair<String, BufferedImage>> requestStaticImage() {
        Coordinate coords = CoordinatePicker.INSTANCE.findRandomLandCoordinate();

        while (!isValidLocation(coords)) {
            coords = CoordinatePicker.INSTANCE.findRandomLandCoordinate();
        }

        BufferedImage image;
        try (Response response = Constants.HTTP_CLIENT.newCall(new Request.Builder()
                        .url(String.format(
                                STREETVIEW_URL,
                                coords.getX(),
                                coords.getY()))
                        .build())
                .execute()) {
            ResponseBody body = response.body();
            if (body == null)
                return Optional.empty();

            image = ImageIO.read(body.source().inputStream());
        } catch (IOException exception) {
            exception.printStackTrace();
            return Optional.empty();
        }

        Optional<String> country = getCountry(coords);

        return Optional.of(new Pair<>(country.get(), image));
    }

    public static boolean isValidLocation(Coordinate coordinates) {
        try (Response response = Constants.HTTP_CLIENT.newCall(new Request.Builder()
                        .url(String.format(
                                VALIDATE_LOCATION_URL,
                                coordinates.getX(),
                                coordinates.getY()))
                        .build())
                .execute()) {
            ResponseBody body = response.body();
            if (body == null) return false;

            JsonObject object = Constants.GSON.fromJson(body.string(), JsonObject.class);
            if (object == null || !object.has("status")) return false;

            String status = object.get("status").getAsString();
            return status.equals("OK");
        } catch (IOException exception) {
            exception.printStackTrace();
            return false;
        }
    }

    public static Optional<String> getCountry(Coordinate coordinate) {
        return Optional.ofNullable(Coordinates2Country.country(coordinate.getX(), coordinate.getY()));
    }
}
