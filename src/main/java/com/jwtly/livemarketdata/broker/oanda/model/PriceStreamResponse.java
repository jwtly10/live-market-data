package com.jwtly.livemarketdata.broker.oanda.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.List;


/**
 * <a href="https://developer.oanda.com/rest-live-v20/pricing-df/#ClientPrice">[Docs]</a>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record PriceStreamResponse(
        @JsonProperty("type") Type type,
        @JsonProperty("time") Instant time,
        @JsonProperty("instrument") String instrument,
        @JsonProperty("tradeable") Boolean tradeable,
        @JsonProperty("bids") List<PriceBucket> bids,
        @JsonProperty("asks") List<PriceBucket> asks,
        @JsonProperty("closeoutBid") PriceValue closeoutBid,
        @JsonProperty("closeoutAsk") PriceValue closeoutAsk
) {
}

