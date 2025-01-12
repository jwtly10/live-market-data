package com.jwtly.livemarketdata.domain.model.stream;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.Instant;

public record StreamError(
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSZ", timezone = "UTC")
        Instant timestamp,
        String message,
        String context,
        String errorType
) {
}
