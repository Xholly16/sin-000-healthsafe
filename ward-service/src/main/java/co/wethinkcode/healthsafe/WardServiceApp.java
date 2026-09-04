package co.wethinkcode.healthsafe;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.Javalin;
import io.javalin.json.JavalinJackson;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class WardServiceApp {

    public static void main(String[] args) throws Exception {

        ObjectMapper mapper = new ObjectMapper();
        List<WardRecord> wards = fetchWardsFromIngestion(mapper);

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


    }

    private static List<WardRecord> fetchWardsFromIngestion(ObjectMapper mapper) throws Exception{
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:7030/wards"))
                .GET()
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        return mapper.readValue(response.body(),mapper.getTypeFactory()
                .constructCollectionType(List.class, WardRecord.class));

    }
}

