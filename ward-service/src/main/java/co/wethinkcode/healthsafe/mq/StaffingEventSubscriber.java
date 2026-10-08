package co.wethinkcode.healthsafe.mq;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.activemq.ActiveMQConnectionFactory;

import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.Session;
import javax.jms.TextMessage;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Listens to staffing-events-topic and remembers the latest staffing event per ward.
 */
public class StaffingEventSubscriber implements AutoCloseable {

    private final ObjectMapper mapper = new ObjectMapper();
    private final Map<String, JsonNode> latestByWard = new ConcurrentHashMap<>();
    private final Connection connection;

    public StaffingEventSubscriber() throws JMSException {
        ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory(MqConfig.BROKER_URL);
        connection = factory.createConnection();
        Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        MessageConsumer consumer = session.createConsumer(session.createTopic(MqConfig.TOPIC));
        consumer.setMessageListener(this::onMessage);
        connection.start();
    }

    private void onMessage(Message message) {
        try {
            if (message instanceof TextMessage text) {
                JsonNode event = mapper.readTree(text.getText());
                String wardId = event.get("wardId").asText();
                latestByWard.put(wardId, event);
                System.out.println("Received staffing event for " + wardId + ": " + text.getText());
            }
        } catch (Exception e) {
            System.err.println("Could not process staffing event: " + e.getMessage());
        }
    }

    public Optional<JsonNode> latestFor(String wardId) {
        return Optional.ofNullable(latestByWard.get(wardId));
    }

    @Override
    public void close() throws JMSException {
        connection.close();
    }
}
