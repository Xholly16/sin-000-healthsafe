package co.wethinkcode.healthsafe;

import co.wethinkcode.healthsafe.mq.EquipmentFailureConsumer;
import io.javalin.Javalin;

import javax.jms.JMSException;

public class EquipmentAlertServiceApp {

    public static void main(String[] args) {
        EquipmentFailureConsumer consumer = startConsumer();

        Javalin app = Javalin.create().start(7034);

        app.get("/health", ctx -> ctx.result("OK"));

        // Every alert received since this service started (each item is already JSON).
        app.get("/alerts", ctx -> {
            if (consumer == null) {
                ctx.status(503).contentType("application/json")
                        .result("{\"error\":\"Not connected to the message broker\"}");
                return;
            }
            ctx.contentType("application/json")
                    .result("[" + String.join(",", consumer.all()) + "]");
        });
    }

    private static EquipmentFailureConsumer startConsumer() {
        try {
            return new EquipmentFailureConsumer();
        } catch (JMSException e) {
            System.err.println("WARNING: could not connect to the broker, alerts disabled: " + e.getMessage());
            return null;
        }
    }
}

// MQ TODO: consumes ActiveMQ queue MqConfig.QUEUE at MqConfig.BROKER_URL (see co.wethinkcode.healthsafe.mq.MqConfig)
// Producer: ward-service publishes here when it detects an equipment failure on one of its wards.
