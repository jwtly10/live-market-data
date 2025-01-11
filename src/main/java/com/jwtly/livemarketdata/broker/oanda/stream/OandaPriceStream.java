package com.jwtly.livemarketdata.broker.oanda.stream;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jwtly.livemarketdata.broker.common.RetryableStream;
import com.jwtly.livemarketdata.broker.oanda.model.PriceStreamResponse;
import com.jwtly.livemarketdata.broker.oanda.model.Type;
import com.jwtly.livemarketdata.core.Price;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.util.List;

/**
 * Oanda Live Price Streaming Client, Implements /pricing/stream endpoint.
 * See <a href="https://developer.oanda.com/rest-live-v20/pricing-ep/">Docs</a>
 */
@Slf4j
public class OandaPriceStream extends RetryableStream<Price> {
    public OandaPriceStream(HttpClient client, String apiKey, String streamUrl, String accountId, List<String> instruments, ObjectMapper objectMapper) throws URISyntaxException {
        super(client, buildRequest(apiKey, streamUrl, accountId, instruments), objectMapper);
    }

    private static HttpRequest buildRequest(String apiKey, String streamUrl, String accountId, List<String> instruments) throws URISyntaxException {
        var instrumentParam = String.join(",", instruments);
        var uri = new URI(String.format("%s/v3/accounts/%s/pricing/stream?instruments=%s", streamUrl, accountId, instrumentParam));

        return HttpRequest.newBuilder()
                .uri(uri)
                .header("Authorization", "Bearer " + apiKey)
                .GET()
                .build();
    }

    /**
     * Processes the line from the price stream into a Tick object
     *
     * @param line     the line from the price stream
     * @param callback the callback to trigger when a price is received
     * @throws Exception if there is an error in the price stream
     */
    @Override
    protected void processLine(String line, StreamCallback<Price> callback) throws Exception {
        if (line.contains("error")) {
            throw new IOException("Error in price stream: " + line);
        }

        try {
            PriceStreamResponse res = objectMapper.readValue(line, PriceStreamResponse.class);

            if (res.type().equals(Type.HEARTBEAT)) {
                log.debug("Heartbeat received. TODO: Add metrics");
            }

            if (res.type().equals(Type.PRICE)) {
                var price = new Price(
                        res.instrument(),
                        Double.parseDouble(res.bids().getFirst().price().value()),
                        Double.parseDouble(res.asks().getFirst().price().value()),
                        0.0, // TODO: Confirm how OANDA handle volume
                        res.time()
                );
                callback.onData(price);
            }
        } catch (Exception e) {
            log.error("Error processing price stream line: {}", line, e);
            throw e;
        }
    }
}