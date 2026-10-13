package co.wethinkcode.healthsafe;

import co.wethinkcode.healthsafe.mq.EquipmentFailurePublisher;
import co.wethinkcode.healthsafe.mq.StaffingEventSubscriber;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.Javalin;
import io.javalin.json.JavalinJackson;

import javax.jms.JMSException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class WardServiceApp {

    public static void main(String[] args) throws Exception {

        ObjectMapper mapper = new ObjectMapper();
        List<WardRecord> wards = fetchWardsFromIngestion(mapper);
        StaffingEventSubscriber staffingEvents = startSubscriber();
        EquipmentFailurePublisher failurePublisher = new EquipmentFailurePublisher();

        Javalin app = Javalin.create(config -> {
            config.jsonMapper(new JavalinJackson(mapper));
        }).start(7031);

        app.get("/health", ctx -> ctx.result("OK"));
        app.get("/wards", ctx -> ctx.json(wards));
        app.get("/wards/{id}", ctx -> {
            String id = ctx.pathParam("id");
            Optional<WardRecord> match = WardLookup.findById(wards, id);

            if (match.isPresent()) {
                ctx.json(match.get());
            } else {
                ctx.status(404).json(Map.of("error", "Ward not found: " + id));
            }
        });

        // Latest staffing event this service heard on staffing-events-topic.
        app.get("/wards/{id}/staffing", ctx -> {
            String id = ctx.pathParam("id");
            if (staffingEvents == null) {
                ctx.status(503).json(Map.of("error", "Not connected to the message broker"));
                return;
            }
            Optional<JsonNode> latest = staffingEvents.latestFor(id);
            if (latest.isPresent()) {
                ctx.json(latest.get());
            } else {
                ctx.status(404).json(Map.of("error", "No staffing event received yet for ward: " + id));
            }
        });

        // Report a broken piece of equipment: the alert goes on the equipment-failure-queue.
        app.post("/wards/{id}/equipment-failure", ctx -> {
            String id = ctx.pathParam("id");
            if (WardLookup.findById(wards, id).isEmpty()) {
                ctx.status(404).json(Map.of("error", "Ward not found: " + id));
                return;
            }

            JsonNode body;
            try {
                body = mapper.readTree(ctx.body());
            } catch (Exception e) {
                ctx.status(400).json(Map.of("error", "Body must be valid JSON"));
                return;
            }
            String equipment = body.path("equipment").asText("").trim();
            if (equipment.isEmpty()) {
                ctx.status(400).json(Map.of("error", "Field 'equipment' is required"));
                return;
            }

            Map<String, Object> event = new LinkedHashMap<>();
            event.put("event", "EQUIPMENT_FAILURE");
            event.put("wardId", id);
            event.put("equipment", equipment);
            event.put("details", body.path("details").asText(""));
            event.put("reportedAt", Instant.now().toString());

            try {
                failurePublisher.publish(mapper.writeValueAsString(event));
                ctx.status(202).json(event);
            } catch (JMSException e) {
                ctx.status(503).json(Map.of("error", "Message broker unavailable"));
            }
        });
    }

    private static StaffingEventSubscriber startSubscriber() {
        try {
            return new StaffingEventSubscriber();
        } catch (JMSException e) {
            System.err.println("WARNING: could not connect to the broker, staffing events disabled: " + e.getMessage());
            return null;
        }
    }

    private static List<WardRecord> fetchWardsFromIngestion(ObjectMapper mapper) throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:7030/wards"))
                .GET()
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        return mapper.readValue(response.body(), mapper.getTypeFactory()
                .constructCollectionType(List.class, WardRecord.class));
    }
}
