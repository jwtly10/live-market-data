package com.jwtly.livemarketdata.domain.port.out;

import com.jwtly.livemarketdata.domain.model.Price;
import reactor.core.publisher.Flux;

import java.util.List;

public interface MarketDataPort {
    Flux<Price> createPriceStream(String streamId, List<String> instruments);
}