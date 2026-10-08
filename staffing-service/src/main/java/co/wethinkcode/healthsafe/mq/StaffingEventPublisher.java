package co.wethinkcode.healthsafe.mq;

import org.apache.activemq.ActiveMQConnectionFactory;

import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;

public class StaffingEventPublisher {

    /**
     * Broadcasts one message (as JSON text) to the staffing-events-topic.
     * Opens a fresh connection each time: simple, fine for a low-traffic service.
     */
    public void publish(String json) throws JMSException {
        ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory(MqConfig.BROKER_URL);
        Connection connection = factory.createConnection();
        try {
            connection.start();
            Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            MessageProducer producer = session.createProducer(session.createTopic(MqConfig.TOPIC));
            TextMessage message = session.createTextMessage(json);
            producer.send(message);
        } finally {
            connection.close();
        }
    }
}
