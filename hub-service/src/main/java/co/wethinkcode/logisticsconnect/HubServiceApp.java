package co.wethinkcode.logisticsconnect;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.Javalin;
import io.javalin.http.HttpStatus;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;

/**
 * hub-service is the place-name source of truth for the rest of LogisticsConnect.
 * It doesn't parse the raw CSV itself - that's ingestion-service's job - it just
 * loads the already-cleaned records over REST and serves them back out.
 */
public class HubServiceApp {

    private static final String INGESTION_URL = "http://localhost:7050/hubs";
    private static final ObjectMapper JSON = new ObjectMapper();
    // The in-memory cache of hubs, loaded from ingestion-service. We keep it in
    // a simple array-backed list rather than a Map<String, Hub> - the dataset is
    // tiny, so a linear scan per lookup is plenty fast and easier to read.
    private static List<Hub> hubCache = new ArrayList<>();


    public static void main(String[] args) {
        hubCache = fetchHubsFromIngestionService();
        System.out.println("hub-service loaded " + hubCache.size() + " hubs from ingestion-service");

        Javalin app = Javalin.create().start(7051);

        app.get("/health", ctx -> ctx.result("OK"));

        // TODO (Serves provinces and sorting centers (place-name source of truth).)
        // Add domain endpoints for hub-service here.

        // GET /hubs -> everything we have cached, straight from ingestion-service.
        app.get("/hubs", ctx -> ctx.json(hubCache));

        // GET /hubs/{hubId} -> the detail transit-service needs for an ETA calc.
        app.get("/hubs/{hubId}", ctx -> {
            String requestedId = ctx.pathParam("hubId").trim().toUpperCase();
            Hub match = findHubById(hubCache, requestedId);

            if (match == null) {
                ctx.status(HttpStatus.NOT_FOUND).json(Map.of("error", "No hub with id " + requestedId));
            } else {
                ctx.json(match);
            }
        });
        // GET /provinces -> the distinct list of provinces we know about. Not
        // asked for explicitly, but it's the kind of small, obviously-useful
        // endpoint you'd expect from a service that calls itself the
        // "place-name source of truth".
        app.get("/provinces", ctx -> ctx.json(distinctProvinces(hubCache)));
        // POST /hubs/refresh -> re-pull the cache from ingestion-service without
        // restarting hub-service. Handy while testing stage 1's cleanup changes.
        app.post("/hubs/refresh", ctx -> {
            hubCache = fetchHubsFromIngestionService();
            ctx.json(Map.of("reloaded", hubCache.size()));
        });
    }

    /**
     * Pure lookup logic, pulled out of the route handler so it can be unit
     * tested without spinning up Javalin or ingestion-service. No modifier
     * (package-private) so the test class in this same package can call it.
     */
    static Hub findHubById(List<Hub> hubs, String hubId) {
        for (Hub hub : hubs) {
            if (hub.hubId.equals(hubId)) {
                return hub;
            }
        }
        return null;
    }

    /**
     * Same reasoning as findHubById: pulled out so a test can check the
     * "distinct provinces" behaviour directly against a small, made-up list.
     */
    static Set<String> distinctProvinces(List<Hub> hubs) {
        Set<String> provinces = new LinkedHashSet<>();
        for (Hub hub : hubs) {
            provinces.add(hub.province);
        }
        return provinces;
    }

    /**
     * Calls ingestion-service's GET /hubs and parses the JSON array into our
     * own Hub objects. Retries a few times with a short pause, since in the
     * normal startup order ingestion-service should already be up - but if
     * someone starts these out of order (or it's still booting), we'd rather
     * wait a moment than fail immediately.
     */
    private static List<Hub> fetchHubsFromIngestionService() throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(INGESTION_URL))
                .timeout(Duration.ofSeconds(5))
                .GET()
                .build();

        int maxAttempts = 5;
        Exception lastError = null;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 200) {
                    Hub[] hubs = JSON.readValue(response.body(), Hub[].class);
                    List<Hub> result = new ArrayList<>();
                    for (Hub hub : hubs) {
                        result.add(hub);
                    }
                    return result;
                }
                lastError = new RuntimeException("ingestion-service returned HTTP " + response.statusCode());
            } catch (Exception e) {
                lastError = e;
            }
            System.out.println("Could not reach ingestion-service on attempt " + attempt
                    + "/" + maxAttempts + " - is it running on port 7050? Retrying...");
            Thread.sleep(1000);
        }
        throw new IllegalStateException(
                "Failed to load hubs from ingestion-service after " + maxAttempts + " attempts. "
                        + "Make sure ingestion-service is running on port 7050 before starting hub-service.",
                lastError);
    }
}
