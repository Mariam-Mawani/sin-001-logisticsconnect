package co.wethinkcode.logisticsconnect;

import io.javalin.Javalin;

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
    }
}

// MQ TODO: publishes to ActiveMQ topic MqConfig.TOPIC at MqConfig.BROKER_URL (see co.wethinkcode.logisticsconnect.mq.MqConfig)
