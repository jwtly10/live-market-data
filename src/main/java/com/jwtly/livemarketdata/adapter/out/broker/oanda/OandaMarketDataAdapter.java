package com.jwtly.livemarketdata.adapter.out.broker.oanda;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jwtly.livemarketdata.adapter.out.broker.oanda.stream.OandaPriceStream;
import com.jwtly.livemarketdata.domain.model.Price;
import com.jwtly.livemarketdata.domain.model.stream.StreamError;
import com.jwtly.livemarketdata.domain.port.out.MarketDataPort;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class OandaMarketDataAdapter implements MarketDataPort {
    private final String apiKey;
    private final String accountId;

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final Map<String, OandaPriceStream> activeStreams;

    public OandaMarketDataAdapter(String apiKey, String accountId, WebClient webClient, ObjectMapper objectMapper) {
        this.apiKey = apiKey;
        this.accountId = accountId;
        this.webClient = webClient;
        this.objectMapper = objectMapper;
        this.activeStreams = new ConcurrentHashMap<>();
    }

    @Override
    public Flux<Price> createPriceStream(String streamId, List<String> instruments) {
        var stream = new OandaPriceStream(webClient, objectMapper);
        activeStreams.put(streamId, stream);

        return stream.streamPrices(apiKey, accountId, instruments)
                .doFinally(signal -> activeStreams.remove(streamId));
    }

    @Override
    public boolean isStreamHealthy(String streamId) {
        var stream = activeStreams.get(streamId);
        return stream != null && stream.isHealthy();
    }

    @Override
    public List<StreamError> getStreamErrors(String streamId) {
        var stream = activeStreams.get(streamId);
        return stream != null ? stream.getRecentErrors() : List.of();
    }
}