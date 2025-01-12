package com.jwtly.livemarketdata.adapter.out.messaging.kafka;

import com.jwtly.livemarketdata.domain.model.Price;
import com.jwtly.livemarketdata.proto.PriceMessage;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringSerializer;
import reactor.core.publisher.Mono;
import reactor.kafka.sender.KafkaSender;
import reactor.kafka.sender.SenderOptions;
import reactor.kafka.sender.SenderRecord;
import reactor.kafka.sender.SenderResult;

import java.util.HashMap;
import java.util.Map;

@Slf4j
public class KafkaPricePublisher {
    private final KafkaSender<String, byte[]> sender;
    private final String topic;

    public KafkaPricePublisher(KafkaConfig cfg) {
        this.sender = initKafkaSender(cfg);
        this.topic = cfg.getTopic();
    }

    private KafkaSender<String, byte[]> initKafkaSender(KafkaConfig cfg) {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, String.join(",", cfg.getBootstrapServers()));
        props.put(ProducerConfig.CLIENT_ID_CONFIG, cfg.getClientId());
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class);
        props.put(ProducerConfig.RETRIES_CONFIG, cfg.getMaxRetries());
        props.put(ProducerConfig.ACKS_CONFIG, cfg.getRequiredAcks());
        props.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, cfg.getCompression());

        props.put(ProducerConfig.MAX_BLOCK_MS_CONFIG, 3000);
        props.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, 3000);

        return KafkaSender.create(SenderOptions.<String, byte[]>create(props)
                .stopOnError(true));
    }

    public Mono<SenderResult<Void>> sendMessage(String broker, Price price) {
        validateMessage(broker, price);

        var key = String.format("%s:%s", broker, price.instrument());
        PriceMessage protoMessage = PriceProtoHandler.toProto(price);

        ProducerRecord<String, byte[]> producerRecord = new ProducerRecord<>(
                topic,
                null, // partition
                key,
                protoMessage.toByteArray()
        );

        producerRecord.headers().add(new RecordHeader("broker", broker.getBytes()));
        producerRecord.headers().add(new RecordHeader("instrument", price.instrument().getBytes()));

        log.debug("Publishing message - broker: {}, instrument: {}", broker, price.instrument());

        return sender.send(Mono.just(SenderRecord.<String, byte[], Void>create(producerRecord, null)))
                .next()
                .doOnNext(result -> log.debug("Message published - partition: {}, offset: {}, broker: {}, instrument: {}",
                        result.recordMetadata().partition(),
                        result.recordMetadata().offset(),
                        broker,
                        price.instrument()))
                .doOnError(error ->
                        log.error("Failed to publish message to Kafka - broker: {}, instrument: {}, error: {}",
                                broker, price.instrument(), error.getMessage())
                );
    }

    private void validateMessage(String broker, Price price) {
        if (broker == null || broker.isBlank()) {
            throw new IllegalArgumentException("Broker cannot be null or empty");
        }

        if (price == null) {
            throw new IllegalArgumentException("Price cannot be null");
        }
    }

    public void close() {
        sender.close();
    }
}