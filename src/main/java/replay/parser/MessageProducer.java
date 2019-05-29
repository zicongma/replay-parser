package replay.parser;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;

import java.util.Properties;
import java.util.concurrent.ExecutionException;

public class MessageProducer {
    private final KafkaProducer<Integer, String> producer;

    public MessageProducer() {
        Properties properties = new Properties();
        properties.put("bootstrap.servers", "localhost:9092");
        properties.put("client.id", "MessageProducer");
        properties.put("key.serializer", "org.apache.kafka.common.serialization.IntegerSerializer");
        properties.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        producer = new KafkaProducer<>(properties);
    }

    public void send(String topic, String message) {
        try {
            producer.send(new ProducerRecord<>(topic, 0, message)).get();
            System.out.println("Sent: " + message);
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }
}
