package co.wethinkcode.healthsafe;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.Javalin;
import io.javalin.json.JavalinJackson;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class AlertLevelServiceApp {
    // Holds the current Emergency Status level (0-8). Starts at 0 = normal.
    private static final AtomicInteger currentLevel = new AtomicInteger(0);

    public static void main(String[] args) {
        Javalin app = Javalin.create(config -> {
            config.jsonMapper(new JavalinJackson(new ObjectMapper()));
        }).start(7032);

        app.get("/health", ctx -> ctx.result("OK"));

        // Read the current Emergency Status
        app.get("/alert-level", ctx -> ctx.json(Map.of("level", currentLevel.get())));

        // Change the Emergency Status
        app.post("/alert-level", ctx -> {
            Map<?, ?> body;
            try {
                body = ctx.bodyAsClass(Map.class);
            } catch (Exception e) {
                ctx.status(400).json(Map.of("error", "Body must be valid JSON"));
                return;
            }

            int newLevel;
            try {
                newLevel = AlertLevelValidator.parse(body);
            } catch (IllegalArgumentException e) {
                ctx.status(400).json(Map.of("error", e.getMessage()));
                return;
            }

            currentLevel.set(newLevel);
            ctx.json(Map.of("level", newLevel));
        });
    }
}
