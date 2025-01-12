package com.jwtly.livemarketdata.domain.port.out;

import com.jwtly.livemarketdata.domain.model.Price;
import reactor.core.publisher.Mono;

public interface MarketDataPublisherPort {
    Mono<Void> publishPrice(String broker, Price price);
}
