package com.jwtly.livemarketdata.messaging.kafka;

import com.jwtly.livemarketdata.core.Price;
import com.jwtly.livemarketdata.proto.PriceMessage;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Slf4j
public class KafkaProducer {
    private final KafkaTemplate<String, byte[]> kafkaTemplate;
    private final String topic;
    private final String clientId;

    public KafkaProducer(KafkaConfig cfg) {
        this.kafkaTemplate = initKafkaTemplate(cfg);
        this.topic = cfg.getTopic();
        this.clientId = cfg.getClientId();
    }

    private KafkaTemplate<String, byte[]> initKafkaTemplate(KafkaConfig cfg) {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, cfg.getBootstrapServers());
        props.put(ProducerConfig.CLIENT_ID_CONFIG, cfg.getClientId());
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class);
        props.put(ProducerConfig.RETRIES_CONFIG, cfg.getMaxRetries());
        props.put(ProducerConfig.ACKS_CONFIG, cfg.getRequiredAcks());
        props.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, cfg.getCompression());

        DefaultKafkaProducerFactory<String, byte[]> factory =
                new DefaultKafkaProducerFactory<>(props);
        return new KafkaTemplate<>(factory);
    }


    public CompletableFuture<SendResult<String, byte[]>> sendMessage(
            String broker,
            Price price
    ) {
        validateMessage(broker, price);

        var key = String.format("%s:%s", broker, price.instrument());
        PriceMessage protoMessage = PriceProtoHandler.toProto(price);
        ProducerRecord<String, byte[]> record = new ProducerRecord<>(
                topic,
                null, // partition
                key,
                protoMessage.toByteArray()
        );


        record.headers().add(new RecordHeader("broker", broker.getBytes()));
        record.headers().add(new RecordHeader("instrument", price.instrument().getBytes()));

        log.debug("Publishing market data message: {}", protoMessage);

        return kafkaTemplate.send(record)
                .thenApply(result -> {
                    RecordMetadata metadata = result.getRecordMetadata();
                    log.info("Message published - partition: {}, offset: {}, broker: {}, instrument: {}",
                            metadata.partition(),
                            metadata.offset(),
                            broker,
                            price.instrument());
                    return result;
                });
    }

    private void validateMessage(String broker, Price price) {
        if (broker == null || broker.isBlank()) {
            throw new IllegalArgumentException("Broker cannot be null or empty");
        }

        if (price == null) {
            throw new IllegalArgumentException("Price cannot be null");
        }
    }
}
