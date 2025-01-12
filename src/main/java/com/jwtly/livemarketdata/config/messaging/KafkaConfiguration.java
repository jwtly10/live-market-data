package com.jwtly.livemarketdata.config.messaging;

import com.jwtly.livemarketdata.adapter.out.messaging.kafka.KafkaConfig;
import com.jwtly.livemarketdata.adapter.out.messaging.kafka.KafkaMarketDataPublisher;
import com.jwtly.livemarketdata.domain.port.out.MarketDataPublisherPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.List;

@Configuration
public class KafkaConfiguration {
    @Bean
    public KafkaConfig kafkaConfig(
            @Value("${kafka.bootstrap-servers}") String bootstrapServersString,
            @Value("${kafka.topic}") String topic,
            @Value("${kafka.client-id}") String clientId,
            @Value("${kafka.max-retries:3}") int maxRetries,
            @Value("${kafka.required-acks:all}") String requiredAcks,
            @Value("${kafka.compression:gzip}") String compression
    ) {
        List<String> bootstrapServers = Arrays.asList(bootstrapServersString.split(","));
        return KafkaConfig.builder()
                .bootstrapServers(bootstrapServers)
                .topic(topic)
                .clientId(clientId)
                .maxRetries(maxRetries)
                .requiredAcks(requiredAcks)
                .compression(compression)
                .build();
    }

    @Bean
    public MarketDataPublisherPort marketDataPublisher(KafkaConfig config) {
        return new KafkaMarketDataPublisher(config);
    }
}
