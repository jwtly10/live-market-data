package com.jwtly.livemarketdata.adapter.out.broker.oanda;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jwtly.livemarketdata.adapter.out.broker.oanda.stream.OandaPriceStream;
import com.jwtly.livemarketdata.domain.model.Price;
import com.jwtly.livemarketdata.domain.port.out.MarketDataPort;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.util.List;

public class OandaMarketDataAdapter implements MarketDataPort {
    private final String apiKey;
    private final String accountId;
    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public OandaMarketDataAdapter(String apiKey, String accountId, WebClient webClient, ObjectMapper objectMapper) {
        this.apiKey = apiKey;
        this.accountId = accountId;
        this.webClient = webClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public Flux<Price> createPriceStream(String streamId, List<String> instruments) {
        return new OandaPriceStream(webClient, objectMapper)
                .streamPrices(apiKey, accountId, instruments);
    }
}