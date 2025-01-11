package com.jwtly.livemarketdata.core;

import java.time.Instant;

public record Price(
        String instrument,
        double bid,
        double ask,
        double volume,
        Instant time
) {
}
