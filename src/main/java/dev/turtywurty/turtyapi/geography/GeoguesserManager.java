package dev.turtywurty.turtyapi.geography;

import com.google.gson.JsonObject;
import dev.turtywurty.turtyapi.Constants;
import io.github.coordinates2country.Coordinates2Country;
import kotlin.Pair;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Stream;

public class GeoguesserManager {
    private static final String API_KEY = "AIzaSyCTLEo6b9gRsHt3sgv0Nxp53SPOX_5Nc2U"; // TODO: Move to .env
    private static final String STREETVIEW_URL = "https://maps.googleapis.com/maps/api/streetview?location=%s,%s&size=600x400&key=" + API_KEY;
    private static final String RANDOM_GEO_COORDS_URL = "https://api.3geonames.org/.json?randomland=yes";
    private static final String VALIDATE_LOCATION_URL = "https://maps.googleapis.com/maps/api/streetview/metadata?location=%s,%s&key=" + API_KEY;

    public static Optional<Pair<String, BufferedImage>> requestStaticImage() {
        Optional<Pair<Double, Double>> coords = requestRandomGeoCoords();
        if(coords.isEmpty()) return Optional.empty();

        Double latitude = coords.get().getFirst();
        Double longitude = coords.get().getSecond();
        if (latitude == null || longitude == null) return Optional.empty();

        while (!isValidLocation(latitude, longitude)) {
            coords = requestRandomGeoCoords();
            if(coords.isEmpty()) return Optional.empty();

            latitude = coords.get().getFirst();
            longitude = coords.get().getSecond();
            if (latitude == null || longitude == null) return Optional.empty();
        }

        BufferedImage image;

        try(Response response = Constants.HTTP_CLIENT.newCall(new Request.Builder().url(String.format(STREETVIEW_URL, latitude, longitude)).build()).execute()) {
            ResponseBody body = response.body();
            if(body == null) return Optional.empty();

            image = ImageIO.read(body.source().inputStream());
        } catch (IOException exception) {
            exception.printStackTrace();
            return Optional.empty();
        }

        Optional<String> country = getCountry(latitude, longitude);
        while (country.isEmpty()) {
            coords = requestRandomGeoCoords();
            if(coords.isEmpty()) return Optional.empty();

            latitude = coords.get().getFirst();
            longitude = coords.get().getSecond();
            if (latitude == null || longitude == null) return Optional.empty();

            country = getCountry(latitude, longitude);
        }

        return Optional.of(new Pair<>(country.get(), image));
    }

    public static Optional<Pair<Double, Double>> requestRandomGeoCoords() {
        try(Response response = Constants.HTTP_CLIENT.newCall(new Request.Builder().url(RANDOM_GEO_COORDS_URL).build()).execute()) {
            ResponseBody body = response.body();
            if(body == null) return Optional.empty();

            JsonObject object = Constants.GSON.fromJson(body.string(), JsonObject.class);
            if(object == null || !object.has("nearest")) return Optional.empty();

            object = object.getAsJsonObject("nearest");
            if(object == null || !object.has("latt") || !object.has("longt")) return Optional.empty();

            double latitude = Double.parseDouble(object.get("latt").getAsString());
            double longitude = Double.parseDouble(object.get("longt").getAsString());

            return Optional.of(new Pair<>(latitude, longitude));
        } catch (IOException exception) {
            exception.printStackTrace();
            return Optional.empty();
        }
    }

    public static boolean isValidLocation(double latitude, double longitude) {
        try(Response response = Constants.HTTP_CLIENT.newCall(new Request.Builder().url(String.format(VALIDATE_LOCATION_URL, latitude, longitude)).build()).execute()) {
            ResponseBody body = response.body();
            if(body == null) return false;

            JsonObject object = Constants.GSON.fromJson(body.string(), JsonObject.class);
            if(object == null || !object.has("status")) return false;

            String status = object.get("status").getAsString();
            return status.equals("OK");
        } catch (IOException exception) {
            exception.printStackTrace();
            return false;
        }
    }

    public static Optional<String> getCountry(double latitude, double longitude) {
        return Optional.ofNullable(Coordinates2Country.country(latitude, longitude));
    }
}
