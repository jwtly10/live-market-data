package com.jwtly.livemarketdata.adapter.out.messaging.kafka;

import com.google.protobuf.InvalidProtocolBufferException;
import com.jwtly.livemarketdata.domain.model.Price;
import com.jwtly.livemarketdata.proto.PriceMessage;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.ConfluentKafkaContainer;
import reactor.core.publisher.Flux;
import reactor.kafka.receiver.KafkaReceiver;
import reactor.kafka.receiver.ReceiverOptions;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

@Testcontainers
public class KafkaPricePublisherIntegrationTest {
    private static final String TOPIC = "test-market-data";
    private static final String CLIENT_ID = "test-producer";
    private static final String GROUP_ID = "test-group";

    @Container
    static ConfluentKafkaContainer kafka = new ConfluentKafkaContainer("confluentinc/cp-kafka:7.4.0");

    private KafkaPricePublisher producer;
    private KafkaReceiver<String, byte[]> receiver;

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
        consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, GROUP_ID);
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class);

        ReceiverOptions<String, byte[]> receiverOptions = ReceiverOptions.<String, byte[]>create(consumerProps)
                .subscription(Collections.singletonList(TOPIC));

        receiver = KafkaReceiver.create(receiverOptions);
    }

    @AfterEach
    void tearDown() {
        producer.close();
    }

    @Test
    void testCanPublishMessage() {
        var broker = "test-broker";
        var now = Instant.now();
        var price = new Price("BTC-USD", 10000, 10001, 10002, now);

        StepVerifier.create(producer.sendMessage(broker, price)
                        .thenMany(receiver.receive()
                                .take(1)
                                .map(record -> {
                                    try {
                                        var receivedMessage = PriceMessage.parseFrom(record.value());
                                        Assertions.assertEquals(String.format("%s:%s", broker, price.instrument()), record.key());
                                        Assertions.assertEquals(price.instrument(), receivedMessage.getInstrument());
                                        Assertions.assertEquals(price.ask(), Double.parseDouble(receivedMessage.getAsk()));
                                        Assertions.assertEquals(price.bid(), Double.parseDouble(receivedMessage.getBid()));
                                        Assertions.assertEquals(price.volume(), Double.parseDouble(receivedMessage.getVolume()));
                                        Assertions.assertEquals(price.time().getEpochSecond(), receivedMessage.getTime().getSeconds());

                                        var headers = record.headers();
                                        Assertions.assertArrayEquals(broker.getBytes(), headers.lastHeader("broker").value());
                                        Assertions.assertArrayEquals(price.instrument().getBytes(), headers.lastHeader("instrument").value());
                                        return true;
                                    } catch (InvalidProtocolBufferException e) {
                                        return false;
                                    }
                                })))
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    void testCanPublishMultipleMessages() {
        var broker = "test-broker";
        var now = Instant.now().truncatedTo(java.time.temporal.ChronoUnit.SECONDS);
        var prices = List.of(
                new Price("BTC-USD", 10000, 10001, 10002, now),
                new Price("ETH-USD", 20000, 20001, 20002, now.plusSeconds(1)),
                new Price("LTC-USD", 30000, 30001, 30002, now.plusSeconds(2))
        );

        StepVerifier.create(Flux.fromIterable(prices)
                        .flatMap(price -> producer.sendMessage(broker, price))
                        .thenMany(receiver.receive()
                                .take(3)
                                .map(record -> {
                                    try {
                                        var receivedMessage = PriceMessage.parseFrom(record.value());
                                        return new Price(
                                                receivedMessage.getInstrument(),
                                                Double.parseDouble(receivedMessage.getBid()),
                                                Double.parseDouble(receivedMessage.getAsk()),
                                                Double.parseDouble(receivedMessage.getVolume()),
                                                Instant.ofEpochSecond(receivedMessage.getTime().getSeconds())
                                        );
                                    } catch (InvalidProtocolBufferException e) {
                                        return null;
                                    }
                                })
                                .collectList())
                        .doOnNext(receivedPrices -> Assertions.assertEquals(prices, receivedPrices)))
                .expectNextCount(1)
                .verifyComplete();
    }
}
