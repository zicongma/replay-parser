package replay.parser;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;

import java.util.Properties;
import java.util.concurrent.ExecutionException;

public class UpdateProducer {
    private final KafkaProducer<Integer, String> producer;

    public UpdateProducer() {
        Properties properties = new Properties();
        properties.put("bootstrap.servers", "localhost:9092");
        properties.put("client.id", "UpdateProducer");
        properties.put("key.serializer", "org.apache.kafka.common.serialization.IntegerSerializer");
        properties.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        producer = new KafkaProducer<>(properties);
    }

    public void send(String message) {
        try {
            producer.send(new ProducerRecord<>("update", 0, message)).get();
            System.out.println("Sent: " + message);
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }
}
