package com.jwtly.livemarketdata.adapter.out.messaging.kafka;

import com.jwtly.livemarketdata.domain.model.Price;
import com.jwtly.livemarketdata.domain.port.out.MarketDataPublisherPort;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
public class KafkaMarketDataPublisher implements MarketDataPublisherPort {
    private final KafkaPricePublisher publisher;

    public KafkaMarketDataPublisher(KafkaPricePublisher publisher) {
        this.publisher = publisher;
    }

    @Override
    public Mono<Void> publishPrice(String broker, Price price) {
        return publisher.sendMessage(broker, price)
                .then();
    }
}

