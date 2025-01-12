package com.jwtly.livemarketdata.adapter.out.messaging.kafka;

import com.google.protobuf.InvalidProtocolBufferException;
import com.jwtly.livemarketdata.domain.model.Price;
import com.jwtly.livemarketdata.proto.PriceMessage;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.ConfluentKafkaContainer;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@Testcontainers
public class KafkaPricePublisherIntegrationTest {
    private static final String TOPIC = "test-market-data";
    private static final String CLIENT_ID = "test-producer";

    @Container
    static ConfluentKafkaContainer kafka = new ConfluentKafkaContainer("confluentinc/cp-kafka:7.4.0");

    private KafkaPricePublisher producer;
    private KafkaConsumer<String, byte[]> consumer;

    @BeforeEach
    void setUp() {
        KafkaConfig cfg = KafkaConfig.builder()
                .bootstrapServers(List.of(kafka.getBootstrapServers()))
                .topic(TOPIC)
                .clientId(CLIENT_ID)
                .maxRetries(3)
                .requiredAcks("all")
                .compression("none")
                .build();

        producer = new KafkaPricePublisher(cfg);

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
        var price = new Price("BTC-USD", 10000, 10001, 10002, now);

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
        Assertions.assertEquals(price.ask(), Double.parseDouble(receivedMessage.getAsk()));
        Assertions.assertEquals(price.bid(), Double.parseDouble(receivedMessage.getBid()));
        Assertions.assertEquals(price.volume(), Double.parseDouble(receivedMessage.getVolume()));
        Assertions.assertEquals(price.time().getEpochSecond(), receivedMessage.getTime().getSeconds());

        var headers = record.headers();
        Assertions.assertArrayEquals(broker.getBytes(), headers.lastHeader("broker").value());
        Assertions.assertArrayEquals(price.instrument().getBytes(), headers.lastHeader("instrument").value());
    }

    @Test
    public void testCanPublishMultipleMessages() throws Exception {
        var broker = "test-broker";
        var now = Instant.now();
        var prices = List.of(
                new Price("BTC-USD", 10000, 10001, 10002, Instant.ofEpochSecond(now.getEpochSecond())),
                new Price("ETH-USD", 20000, 20001, 20002, Instant.ofEpochSecond(now.plus(Duration.ofSeconds(1)).getEpochSecond())),
                new Price("LTC-USD", 30000, 30001, 30002, Instant.ofEpochSecond(now.plus(Duration.ofSeconds(2)).getEpochSecond())
                )
        );

        for (Price price : prices) {
            producer.sendMessage(broker, price).get(5, TimeUnit.SECONDS);
        }

        var records = consumer.poll(Duration.ofSeconds(5));
        Assertions.assertEquals(3, records.count());

        var gotPrices = new ArrayList<Price>();
        for (ConsumerRecord<String, byte[]> record : records) {
            PriceMessage receivedMessage = PriceMessage.parseFrom(record.value());
            gotPrices.add(new Price(
                    receivedMessage.getInstrument(),
                    Double.parseDouble(receivedMessage.getBid()),
                    Double.parseDouble(receivedMessage.getAsk()),
                    Double.parseDouble(receivedMessage.getVolume()),
                    Instant.ofEpochSecond(receivedMessage.getTime().getSeconds())
            ));
        }

        Assertions.assertEquals(prices, gotPrices);
    }
}
