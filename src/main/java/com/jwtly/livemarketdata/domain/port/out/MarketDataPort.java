package com.jwtly.livemarketdata.domain.port.out;

import com.jwtly.livemarketdata.domain.model.Price;
import com.jwtly.livemarketdata.domain.model.stream.StreamError;
import reactor.core.publisher.Flux;

import java.util.List;

public interface MarketDataPort {
    Flux<Price> createPriceStream(String streamId, List<String> instruments);

    boolean isStreamHealthy(String streamId);

    List<StreamError> getStreamErrors(String streamId);
}