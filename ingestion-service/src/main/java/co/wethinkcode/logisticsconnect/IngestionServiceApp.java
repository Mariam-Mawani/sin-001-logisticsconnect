package co.wethinkcode.logisticsconnect;

import io.javalin.Javalin;
import io.javalin.http.HttpStatus;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

public class IngestionServiceApp {

    public static void main(String[] args) {
        // We clean the CSV once at startup and keep the result in memory. The
        // dataset is small and doesn't change while the service is running, so
        // there's no need to re-read/re-clean it on every request.
        List<Hub> hubs = loadCleanedHubs();
        System.out.println("Loaded and cleaned " + hubs.size() + " hub records from hubs-global.csv");
        Javalin app = Javalin.create().start(7050);

        app.get("/health", ctx -> ctx.result("OK"));

        // TODO: read and clean src/main/resources/hubs-global.csv (hubs, sorting centers, regional districts data —
        // trim whitespace, fix casing, normalize dates/booleans) and expose the
        // cleaned records here for the other services to consume.

        // GET /hubs -> the full cleaned list. This is what hub-service reads on
        // startup to build its own place-name data.
        app.get("/hubs", ctx -> ctx.json(hubs));

        // GET /hubs/{hubId} -> a single cleaned hub, mostly handy for manual
        // testing with curl.
        app.get("/hubs/{hubId}", ctx -> {
            String requestedId = ctx.pathParam("hubId").trim().toUpperCase();

            Hub match = null;
            for (Hub hub : hubs) {
                if (hub.hubId.equals(requestedId)) {
                    match = hub;
                    break;
                }
            }
            if (match == null) {
                ctx.status(HttpStatus.NOT_FOUND).json(Map.of("error", "No hub with id " + requestedId));
            } else {
                ctx.json(match);
            }
        });
    }

    private static List<Hub> loadCleanedHubs() throws Exception {
        // hubs-global.csv sits in src/main/resources, which Maven copies onto
        // the classpath - so we read it as a classpath resource rather than a
        // plain file path (that also keeps it working once packaged into a jar).
        try (InputStream csvInput = IngestionServiceApp.class.getClassLoader()
                .getResourceAsStream("hubs-global.csv")) {

            if (csvInput == null) {
                throw new IllegalStateException("Could not find hubs-global.csv on the classpath");
            }
            return HubCsvCleaner.loadAndClean(csvInput);
        }
    }
}
