package co.wethinkcode.healthsafe;

import io.javalin.Javalin;
import io.javalin.json.JavalinJackson;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class AlertLevelServiceApp {
    // Holds the current Emergency Status level (0-8). Starts at 0 = normal.
    private static final AtomicInteger currentLevel = new AtomicInteger(0);

    public static void main(String[] args) {
        Javalin app = Javalin.create(config ->{
            config.jsonMapper(new JavalinJackson(new com.fasterxml.jackson.databind.ObjectMapper()));
        } ).start(7032);

        app.get("/health", ctx -> ctx.result("OK"));

        //Read the current Emergency Status
        app.get("/alert-level", ctx-> {
            ctx.json(Map.of("level", currentLevel.get()));
        });

        app.post("/alert-level", ctx ->{
            Map<?, ?> body = ctx.bodyAsClass(Map.class);
            int newLevel = ((Number) body.get("level")).intValue();

            if (newLevel < 0 || newLevel > 8) {
                ctx.status(400).json(Map.of("error", "level must be between 0 and 8"));
                return;
            }
            currentLevel.set(newLevel);
            ctx.json(Map.of("level", currentLevel.get()));
        });
        // TODO (Tracks the hospital Emergency Status (0-8, 8 = full Code Blue).)
        // Add domain endpoints for alert-level-service here.
    }
}
