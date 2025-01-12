package com.jwtly.livemarketdata.adapter.out.broker.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.List;

@Slf4j
public abstract class BaseBrokerStream {
    protected final List<Integer> NON_RETRYABLE_STATUS_CODES = List.of(
            400, 401, 403, 404
    );

    protected Retry createRetrySpec() {
        return Retry.backoff(10, Duration.ofSeconds(1))
                .maxBackoff(Duration.ofSeconds(30))
                .filter(this::isRetryableError);
    }

    protected boolean isRetryableError(Throwable error) {
        if (error instanceof WebClientResponseException wcre) {
            return !NON_RETRYABLE_STATUS_CODES.contains(wcre.getStatusCode().value());
        }
        return true;
    }
}
