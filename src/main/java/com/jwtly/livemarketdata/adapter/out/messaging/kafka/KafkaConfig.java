package com.jwtly.livemarketdata.adapter.out.messaging.kafka;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Builder
@Getter
public class KafkaConfig {
    private List<String> bootstrapServers;
    private String topic;
    private String clientId;
    private int maxRetries = 5;
    private String requiredAcks = "all";
    private String compression = "none";
}
