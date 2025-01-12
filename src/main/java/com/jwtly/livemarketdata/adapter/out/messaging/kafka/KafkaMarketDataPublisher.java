package com.jwtly.livemarketdata.adapter.out.messaging.kafka;

import com.jwtly.livemarketdata.domain.model.Price;
import com.jwtly.livemarketdata.domain.model.PublishResult;
import com.jwtly.livemarketdata.domain.port.out.MarketDataPublisherPort;

import java.util.concurrent.CompletableFuture;

public class KafkaMarketDataPublisher implements MarketDataPublisherPort {
    private final KafkaPricePublisher publisher;

    public KafkaMarketDataPublisher(KafkaConfig config) {
        this.publisher = new KafkaPricePublisher(config);
    }

    @Override
    public CompletableFuture<PublishResult> publishPrice(String broker, Price price) {
        return publisher.sendMessage(broker, price)
                .thenApply(result -> new PublishResult(
                        result.getRecordMetadata().offset() + "",
                        true,
                        null
                ))
                .exceptionally(error -> new PublishResult(
                        null,
                        false,
                        error.getMessage()
                ))
                ;
    }
}
