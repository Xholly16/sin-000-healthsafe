package co.wethinkcode.healthsafe;

import io.javalin.Javalin;

import java.util.List;
import java.io.IOException;
import io.javalin.json.JavalinJackson;
import com.fasterxml.jackson.databind.ObjectMapper;

public class IngestionServiceApp {

    public static void main(String[] args) throws IOException {
        String csvPath =  "wards-outdated.csv";
        List<WardRecord> wards = WardDataCleaner.loadAndClean(csvPath);

        Javalin app = Javalin.create(config -> {
            config.jsonMapper(new JavalinJackson(new ObjectMapper()));
        }).start(7030);

        app.get("/health", ctx -> ctx.result("OK"));
        app.get("/wards",ctx -> ctx.json(wards));

        // TODO: read and clean src/main/resources/wards-outdated.csv (wards, wings, specialist departments data —
        // trim whitespace, fix casing, normalize dates/booleans) and expose the
        // cleaned records here for the other services to consume.
    }
}
