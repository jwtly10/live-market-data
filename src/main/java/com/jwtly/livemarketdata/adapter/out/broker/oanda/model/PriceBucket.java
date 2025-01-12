package com.jwtly.livemarketdata.adapter.out.broker.oanda.model;

import com.fasterxml.jackson.annotation.JsonProperty;


/**
 * <a href="https://developer.oanda.com/rest-live-v20/pricing-common-df/#PriceBucket">[Docs]</a>
 */
public record PriceBucket(
        @JsonProperty("price") String price,
        @JsonProperty("liquidity") Long liquidity
) {
}
