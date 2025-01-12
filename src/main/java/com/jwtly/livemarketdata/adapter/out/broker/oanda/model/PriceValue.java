package com.jwtly.livemarketdata.adapter.out.broker.oanda.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;


/**
 * <a href="https://developer.oanda.com/rest-live-v20/pricing-common-df/#PriceValue">[Docs]</a>
 */
public record PriceValue(
        @JsonValue
        String value
) {
    @JsonCreator
    public static PriceValue create(String value) {
        return new PriceValue(value);
    }
}