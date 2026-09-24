package co.wethinkcode.logisticsconnect;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.Javalin;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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
    }
}

// MQ TODO: subscribes to ActiveMQ topic MqConfig.TOPIC at MqConfig.BROKER_URL (see co.wethinkcode.logisticsconnect.mq.MqConfig)
