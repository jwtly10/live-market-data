package com.jwtly.livemarketdata.adapter.out.broker.oanda.stream;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jwtly.livemarketdata.adapter.out.broker.common.BaseBrokerStream;
import com.jwtly.livemarketdata.adapter.out.broker.oanda.model.PriceStreamResponse;
import com.jwtly.livemarketdata.adapter.out.broker.oanda.model.Type;
import com.jwtly.livemarketdata.domain.model.Price;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Objects;

/**
 * Oanda Live Price Streaming Client, Implements /pricing/stream endpoint.
 * See <a href="https://developer.oanda.com/rest-live-v20/pricing-ep/">Docs</a>
 */
@Slf4j
public class OandaPriceStream extends BaseBrokerStream {
    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public OandaPriceStream(WebClient webClient, ObjectMapper objectMapper) {
        this.webClient = webClient;
        this.objectMapper = objectMapper;
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
                .flatMap(this::processLine)
                .filter(Objects::nonNull)
                .doOnSubscribe(s -> log.info("Starting Oanda price stream for instruments: {}", instruments))
                .retryWhen(createRetrySpec());
    }

    private Flux<Price> processLine(String line) {
        try {
            PriceStreamResponse response = objectMapper.readValue(line, PriceStreamResponse.class);

            if (response.type().equals(Type.PRICE)) {
                Price price = new Price(
                        response.instrument(),
                        Double.parseDouble(response.bids().getFirst().price()),
                        Double.parseDouble(response.asks().getFirst().price()),
                        0.0,
                        response.time()
                );
                log.debug("Received price: {}", price);
                return Flux.just(price);
            }

            if (response.type().equals(Type.HEARTBEAT)) {
                log.trace("Received heartbeat");
                return Flux.empty();
            }

            return Flux.empty();
        } catch (Exception e) {
            log.error("Error processing line: {}", line, e);
            return Flux.error(new RuntimeException("Failed to process price data", e));
        }
    }
}