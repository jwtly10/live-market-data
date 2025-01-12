package com.jwtly.livemarketdata.adapter.out.broker.oanda.stream;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jwtly.livemarketdata.adapter.out.broker.oanda.model.PriceStreamResponse;
import com.jwtly.livemarketdata.adapter.out.broker.oanda.model.Type;
import com.jwtly.livemarketdata.domain.model.Price;
import com.jwtly.livemarketdata.domain.model.stream.StreamError;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Oanda Live Price Streaming Client, Implements /pricing/stream endpoint.
 * See <a href="https://developer.oanda.com/rest-live-v20/pricing-ep/">Docs</a>
 */
@Slf4j
public class OandaPriceStream {
    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final ConcurrentLinkedQueue<StreamError> errors;
    private volatile boolean isHealthy;

    public OandaPriceStream(WebClient webClient, ObjectMapper objectMapper) {
        this.webClient = webClient;
        this.objectMapper = objectMapper;
        this.errors = new ConcurrentLinkedQueue<>();
        this.isHealthy = false;
    }

    public Flux<Price> streamPrices(String apiKey, String accountId, List<String> instruments) {
        return webClient.get()
                .uri(builder -> builder
                        .path("/v3/accounts/{accountId}/pricing/stream")
                        .queryParam("instruments", String.join(",", instruments))
                        .build(accountId))
                .header("Authorization", "Bearer " + apiKey)
                .retrieve()
                .bodyToFlux(String.class)
                .flatMap(line -> {
                    try {
                        PriceStreamResponse response = objectMapper.readValue(line, PriceStreamResponse.class);
                        if (response.type().equals(Type.PRICE)) {
                            return Flux.just(new Price(
                                    response.instrument(),
                                    Double.parseDouble(response.bids().getFirst().price().value()),
                                    Double.parseDouble(response.asks().getFirst().price().value()),
                                    0.0, // TODO: Implement volume calculation
                                    response.time()
                            ));
                        }

                        if (response.type().equals(Type.HEARTBEAT)) {
                            log.info("Received heartbeat: {}", response);
                            return Flux.empty();
                        }
                    } catch (Exception e) {
                        log.error("Error processing line: {}", line, e);
                        return Flux.error(new RuntimeException("Failed to process price data", e));
                    }
                    return Flux.empty();
                })
                .filter(Objects::nonNull)
                .doOnSubscribe(s -> log.info("Starting price stream for instruments: {}", instruments))
                .doOnNext(p -> isHealthy = true)
                .doOnError(e -> {
                    isHealthy = false;
                    errors.offer(new StreamError(e.getMessage(), System.currentTimeMillis()));
                })
                .retryWhen(Retry.backoff(10, Duration.ofSeconds(1))
                        .maxBackoff(Duration.ofSeconds(30))
                        .doBeforeRetry(signal -> log.warn("Retrying after error: {}",
                                signal.failure().getMessage())));
    }

    public boolean isHealthy() {
        return isHealthy;
    }

    public List<StreamError> getRecentErrors() {
        return List.copyOf(errors);
    }
}