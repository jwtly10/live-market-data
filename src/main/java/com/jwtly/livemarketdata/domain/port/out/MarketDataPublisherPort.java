package com.jwtly.livemarketdata.domain.port.out;

import com.jwtly.livemarketdata.domain.model.Price;
import com.jwtly.livemarketdata.domain.model.PublishResult;

import java.util.concurrent.CompletableFuture;

public interface MarketDataPublisherPort {
    /**
     * Publishes a price update for a specific broker.
     *
     * @param broker The source broker identifier
     * @param price  The price information to publish
     * @return A future that completes when the message is published
     * @throws IllegalArgumentException if broker is null/empty or price is null
     */
    CompletableFuture<PublishResult> publishPrice(String broker, Price price);
}
