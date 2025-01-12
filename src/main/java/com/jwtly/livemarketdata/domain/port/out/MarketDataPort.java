package com.jwtly.livemarketdata.domain.port.out;

import com.jwtly.livemarketdata.domain.model.Price;

import java.util.List;

public interface MarketDataPort {
    /**
     * Creates a price stream for a list of instruments. Returns error if creating the stream fails.
     * <p>
     * Otherwise errors during the stream are handled by the stream callback itself.
     * The stream will emit prices for the given instruments.
     * </p>
     *
     * @param instruments The list of instruments to create a price stream for
     * @return A stream of prices
     * @throws Exception if an error occurs while creating the stream
     */
    Stream<Price> createPriceStream(List<String> instruments) throws Exception;
}