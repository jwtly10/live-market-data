package com.jwtly.livemarketdata.core;

import java.math.BigDecimal;
import java.time.Instant;

public record Price(
        String instrument,
        BigDecimal bid,
        BigDecimal ask,
        BigDecimal volume,
        Instant time
) {
}
