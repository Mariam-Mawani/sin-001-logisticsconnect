package co.wethinkcode.logisticsconnect;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.Javalin;
import io.javalin.http.HttpStatus;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Calculates an ETA window for a hub, combining:
 *   - hub location data, fetched synchronously from hub-service (stage 2 REST call)
 *   - the hub's current delay stage, learned from package-status-topic instead
 *     of calling delay-stage-service directly (stage 3 MQ decoupling)
 */
public class TransitServiceApp {

    private static final String HUB_SERVICE_URL = "http://localhost:7051/hubs/";
    private static final ObjectMapper JSON = new ObjectMapper();
    // Delay stage per hub, learned entirely from MQ messages. A hub with no
    // entry here simply hasn't had a stage change reported since this service
    // started listening - see the /eta handler for how that's treated.
    private static final Map<String, Integer> delayStageByHub = new ConcurrentHashMap<>();


    public static void main(String[] args) {
        Javalin app = Javalin.create().start(7053);

        app.get("/health", ctx -> ctx.result("OK"));

        // TODO (Calculates estimated arrival windows based on hub and delay stage.)
        // Add domain endpoints for transit-service here.
        // GET /eta/{hubId} -> an estimated arrival window for that hub.
        app.get("/eta/{hubId}", ctx -> {
            String hubId = ctx.pathParam("hubId").trim().toUpperCase();

            Hub hub = fetchHub(hubId);
            if (hub == null) {
                ctx.status(HttpStatus.NOT_FOUND).json(Map.of("error", "No hub with id " + hubId));
                return;
            }

            // No message received yet for this hub -> assume no known delay (stage 0),
            // rather than blocking the ETA on a stage we simply haven't heard about.
            int delayStage = delayStageByHub.getOrDefault(hubId, 0);

            EtaResult eta = calculateEta(hub, delayStage);
            ctx.json(eta);
        });
    }
}

// MQ TODO: subscribes to ActiveMQ topic MqConfig.TOPIC at MqConfig.BROKER_URL (see co.wethinkcode.logisticsconnect.mq.MqConfig)
