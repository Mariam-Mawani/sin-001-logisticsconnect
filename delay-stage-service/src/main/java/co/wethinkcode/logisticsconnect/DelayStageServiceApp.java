package co.wethinkcode.logisticsconnect;

import io.javalin.Javalin;
import io.javalin.http.HttpStatus;

import javax.jms.JMSException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks the "Transit Delay Stage" (0-8) for each hub, and publishes a message
 * to the package-status-topic every time a stage actually changes.
 */
public class DelayStageServiceApp {

    private static final int MIN_STAGE = 0;
    private static final int MAX_STAGE = 8;
    // In-memory store of the current stage per hub. ConcurrentHashMap because
    // Javalin handles requests on multiple threads, and several clients could
    // read/write stages at the same time.
    private static final Map<String, Integer> stageByHub = new ConcurrentHashMap<>();


    public static void main(String[] args) {
        Javalin app = Javalin.create().start(7052);

        app.get("/health", ctx -> ctx.result("OK"));

        // TODO (Tracks the Transit Delay Stage (0-8, e.g. weather shutdowns).)
        // Add domain endpoints for delay-stage-service here.

        // GET /delay-stage/{hubId} -> the current stage for a hub. A hub we've
        // never received a stage update for defaults to 0 ("no known delay"),
        // since that's a more useful default than 404-ing on every hub that
        // simply hasn't had an incident yet.
        app.get("/delay-stage/{hubId}", ctx -> {
            String hubId = ctx.pathParam("hubId").trim().toUpperCase();
            int stage = stageByHub.getOrDefault(hubId, 0);
            ctx.json(Map.of("hubId", hubId, "stage", stage));
        });

        // POST /delay-stage/{hubId} with a body like {"stage": 3} -> the
        // state-change endpoint. This both updates our own record AND (if the
        // stage actually changed) publishes to package-status-topic so
        // transit-service and alertbot hear about it asynchronously.
        app.post("/delay-stage/{hubId}", ctx -> {
            String hubId = ctx.pathParam("hubId").trim().toUpperCase();

            StageUpdateRequest body = ctx.bodyAsClass(StageUpdateRequest.class);
            if (!isValidStage(body.stage)) {
                ctx.status(HttpStatus.BAD_REQUEST)
                        .json(Map.of("error", "stage must be between " + MIN_STAGE + " and " + MAX_STAGE));
                return;
            }

            Integer previousStage = stageByHub.put(hubId, body.stage);
            boolean stageChanged = hasStageChanged(previousStage, body.stage);

            if (stageChanged) {
                try {
                    publishStageChange(hubId, body.stage);
                } catch (JMSException e) {
                    // We don't want a broker hiccup to fail the HTTP request itself -
                    // the stage is already saved above. We just log it so it's
                    // visible that a downstream consumer may have missed this update.
                    System.out.println("Warning: could not publish stage change to MQ: " + e.getMessage());
                }
            }

            ctx.json(Map.of("hubId", hubId, "stage", body.stage, "published", stageChanged));
        });

    }

    /**
     * True when a stage is within the documented 0-8 range. Pulled out of the
     * route handler so it's testable on its own, with no Javalin involved.
     * No modifier (package-private) so the test class can call it directly.
     */
    static boolean isValidStage(int stage) {
        return stage >= MIN_STAGE && stage <= MAX_STAGE;
    }
}

// MQ TODO: publishes to ActiveMQ topic MqConfig.TOPIC at MqConfig.BROKER_URL (see co.wethinkcode.logisticsconnect.mq.MqConfig)
