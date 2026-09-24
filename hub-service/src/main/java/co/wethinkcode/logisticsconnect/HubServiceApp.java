package co.wethinkcode.logisticsconnect;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.Javalin;

import java.util.ArrayList;
import java.util.List;

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
        Javalin app = Javalin.create().start(7051);

        app.get("/health", ctx -> ctx.result("OK"));

        // TODO (Serves provinces and sorting centers (place-name source of truth).)
        // Add domain endpoints for hub-service here.
    }
}
