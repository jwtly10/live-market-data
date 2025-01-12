package com.jwtly.livemarketdata.adapter.out.broker.oanda.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;


/**
 * <a href="https://developer.oanda.com/rest-live-v20/pricing-common-df/#PriceBucket">[Docs]</a>
 */
public record PriceBucket(
        @JsonProperty("price") PriceValue price,
        @JsonProperty("liquidity") BigDecimal liquidity
) {
}
