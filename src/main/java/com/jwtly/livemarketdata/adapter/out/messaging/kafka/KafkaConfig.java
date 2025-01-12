package com.jwtly.livemarketdata.adapter.out.messaging.kafka;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "kafka")
@Data
public class KafkaConfig {
    private List<String> bootstrapServers;
    private String topic;
    private String clientId;
    private int maxRetries = 5;
    private String requiredAcks = "all";
    private String compression = "none";
}
