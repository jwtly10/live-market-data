package com.jwtly.livemarketdata.domain.model;

import java.time.Instant;

public record Price(
        String instrument,
        double bid,
        double ask,
        double volume,
        Instant time
) {
}
