package integration.com.jwtly.livemarketdata.kafka;

import com.google.protobuf.InvalidProtocolBufferException;
import com.jwtly.livemarketdata.core.Price;
import com.jwtly.livemarketdata.messaging.kafka.KafkaConfig;
import com.jwtly.livemarketdata.messaging.kafka.KafkaProducer;
import com.jwtly.livemarketdata.proto.PriceMessage;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.deser.std.StringDeserializer;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@Testcontainers
public class KafkaProducerIntegrationTest {
    private static final String TOPIC = "test-market-data";
    private static final String CLIENT_ID = "test-producer";

    @Container
    public static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("apache/kafka"));

    private KafkaProducer producer;
    private KafkaConsumer<String, byte[]> consumer;

    @BeforeEach
    void setUp() {
        KafkaConfig cfg = new KafkaConfig();
        cfg.setBootstrapServers(List.of(kafka.getBootstrapServers()));
        cfg.setTopic(TOPIC);
        cfg.setClientId(CLIENT_ID);
        cfg.setMaxRetries(3);
        cfg.setRequiredAcks("all");
        cfg.setCompression("none");

        producer = new KafkaProducer(cfg);

        Properties consumerProps = new Properties();
        consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, "test-group");
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class);

        consumer = new KafkaConsumer<>(consumerProps);
        consumer.subscribe(Collections.singletonList(TOPIC));
    }

    @AfterEach
    void tearDown() {
        if (consumer != null) {
            consumer.close();
        }
    }

    @Test
    public void testCanPublishMessage() throws InvalidProtocolBufferException, ExecutionException, InterruptedException, TimeoutException {
        var broker = "test-broker";
        var now = Instant.now();
        var price = new Price("BTC-USD", BigDecimal.valueOf(10000), BigDecimal.valueOf(10001), BigDecimal.valueOf(10002), now);

        producer.sendMessage(broker, price).get(5, TimeUnit.SECONDS);

        ConsumerRecord<String, byte[]> record = null;
        int maxAttempts = 5;
        int attempt = 0;

        while (record == null && attempt < maxAttempts) {
            var records = consumer.poll(Duration.ofSeconds(1));
            if (!records.isEmpty()) {
                record = records.iterator().next();
            }
            attempt++;
        }

        assertNotNull(record, "Message not received within timeout");
        Assertions.assertEquals(String.format("%s:%s", broker, price.instrument()), record.key());

        PriceMessage receivedMessage = PriceMessage.parseFrom(record.value());
        Assertions.assertEquals(price.instrument(), receivedMessage.getInstrument());
        Assertions.assertEquals(price.ask(), BigDecimal.valueOf(Double.parseDouble(receivedMessage.getAsk())));
        Assertions.assertEquals(price.bid(), BigDecimal.valueOf(Double.parseDouble(receivedMessage.getBid())));
        Assertions.assertEquals(price.volume(), BigDecimal.valueOf(Double.parseDouble(receivedMessage.getVolume())));
        Assertions.assertEquals(price.time().getEpochSecond(), receivedMessage.getTime().getSeconds());

        var headers = record.headers();
        Assertions.assertArrayEquals(broker.getBytes(), headers.lastHeader("broker").value());
        Assertions.assertArrayEquals(price.instrument().getBytes(), headers.lastHeader("instrument").value());
    }

}
