package dev.turtywurty.turtyapi;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import okhttp3.OkHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.Random;

public class Constants {
    public static final Path DATA_FOLDER = Path.of(System.getProperty("user.dir"), "src/main/resources");
    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    public static final Logger LOGGER = LoggerFactory.getLogger("TurtyAPI");
    public static final short PORT = 8080;
    public static final Random RANDOM = new Random();
    public static final OkHttpClient HTTP_CLIENT = new OkHttpClient();
}
