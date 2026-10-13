package co.wethinkcode.healthsafe.mq;

import org.apache.activemq.ActiveMQConnectionFactory;

import javax.jms.Connection;
import javax.jms.DeliveryMode;
import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.jms.Session;

public class EquipmentFailurePublisher {

    /**
     * Puts one equipment-failure alert (JSON text) on the equipment-failure-queue.
     * PERSISTENT means the broker writes it to disk, so it survives a broker restart.
     */
    public void publish(String json) throws JMSException {
        ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory(MqConfig.BROKER_URL);
        Connection connection = factory.createConnection();
        try {
            connection.start();
            Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            MessageProducer producer = session.createProducer(session.createQueue(MqConfig.QUEUE));
            producer.setDeliveryMode(DeliveryMode.PERSISTENT);
            producer.send(session.createTextMessage(json));
        } finally {
            connection.close();
        }
    }
}
