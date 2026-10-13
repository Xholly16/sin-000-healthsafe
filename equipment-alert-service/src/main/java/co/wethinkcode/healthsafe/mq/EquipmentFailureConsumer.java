package co.wethinkcode.healthsafe.mq;

import org.apache.activemq.ActiveMQConnectionFactory;

import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.Session;
import javax.jms.TextMessage;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Takes equipment-failure alerts off the queue, one at a time.
 * A message is only removed from the queue once we commit the session.
 */
public class EquipmentFailureConsumer implements AutoCloseable {

    private final List<String> received = new CopyOnWriteArrayList<>();
    private final Connection connection;
    private final Session session;

    public EquipmentFailureConsumer() throws JMSException {
        ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory(MqConfig.BROKER_URL);
        connection = factory.createConnection();
        session = connection.createSession(true, Session.SESSION_TRANSACTED);
        MessageConsumer consumer = session.createConsumer(session.createQueue(MqConfig.QUEUE));
        consumer.setMessageListener(this::onMessage);
        connection.start();
    }

    private void onMessage(Message message) {
        try {
            if (message instanceof TextMessage text) {
                received.add(text.getText());
                System.out.println("EQUIPMENT FAILURE ALERT: " + text.getText());
            }
            session.commit(); // handled, so the broker may delete it
        } catch (JMSException e) {
            System.err.println("Could not process alert, it will be redelivered: " + e.getMessage());
            try {
                session.rollback(); // not handled, so the broker keeps it and sends it again
            } catch (JMSException ignored) {
            }
        }
    }

    public List<String> all() {
        return List.copyOf(received);
    }

    @Override
    public void close() throws JMSException {
        connection.close();
    }
}
