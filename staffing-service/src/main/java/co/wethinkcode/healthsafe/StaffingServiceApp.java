package co.wethinkcode.healthsafe;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.Javalin;
import io.javalin.json.JavalinJackson;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;

public class StaffingServiceApp {

    private static final String WARD_SERVICE = "http://localhost:7031";
    private static final String ALERT_LEVEL_SERVICE = "http://localhost:7032";

    private static final ObjectMapper mapper = new ObjectMapper();
    private static final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(2))
            .build();

    public static void main(String[] args) {
        Javalin app = Javalin.create(config -> {
            config.jsonMapper(new JavalinJackson(mapper));
        }).start(7033);

        app.get("/health", ctx -> ctx.result("OK"));

        app.get("/staffing/{wardId}", ctx -> {
            String wardId = ctx.pathParam("wardId");
            try {
                // Question 1: does the ward exist?
                String encodedId = URLEncoder.encode(wardId, StandardCharsets.UTF_8);
                HttpResponse<String> wardResponse = get(WARD_SERVICE + "/wards/" + encodedId);
                if (wardResponse.statusCode() == 404) {
                    ctx.status(404).json(Map.of("error", "Ward not found: " + wardId));
                    return;
                }
                if (wardResponse.statusCode() != 200) {
                    ctx.status(502).json(Map.of("error", "ward-service returned " + wardResponse.statusCode()));
                    return;
                }

                // Question 2: what is the current emergency level?
                HttpResponse<String> levelResponse = get(ALERT_LEVEL_SERVICE + "/alert-level");
                if (levelResponse.statusCode() != 200) {
                    ctx.status(502).json(Map.of("error", "alert-level-service returned " + levelResponse.statusCode()));
                    return;
                }
                int level = mapper.readTree(levelResponse.body()).get("level").asInt();

                // Question 3: how many doctors does that level need?
                int doctors = StaffingScheduler.doctorsRequired(level);

                ctx.json(Map.of(
                        "wardId", wardId,
                        "alertLevel", level,
                        "doctorsRequired", doctors));

            } catch (IOException e) {
                ctx.status(503).json(Map.of("error", "A downstream service is unavailable"));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                ctx.status(503).json(Map.of("error", "Request was interrupted"));
            }
        });
    }

    private static HttpResponse<String> get(String url) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(3))
                .GET()
                .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }
}

// MQ TODO: publishes to ActiveMQ topic MqConfig.TOPIC at MqConfig.BROKER_URL (see co.wethinkcode.healthsafe.mq.MqConfig)
