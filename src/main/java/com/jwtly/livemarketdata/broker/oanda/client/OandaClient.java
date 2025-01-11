package com.jwtly.livemarketdata.broker.oanda.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jwtly.livemarketdata.broker.common.RetryableStream;
import com.jwtly.livemarketdata.broker.oanda.stream.OandaPriceStream;
import com.jwtly.livemarketdata.core.Price;

import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.util.List;

public class OandaClient {
    private final HttpClient httpClient;
    private final String apiKey;
    private final String streamUrl;
    private final String accountId;
    private final ObjectMapper objectMapper;

    public OandaClient(HttpClient httpClient, String apiKey, String streamUrl, String accountId, ObjectMapper objectMapper) {
        this.httpClient = httpClient;
        this.apiKey = apiKey;
        this.streamUrl = streamUrl;
        this.accountId = accountId;
        this.objectMapper = objectMapper;
    }

    public RetryableStream<Price> createPriceStream(List<String> instruments) throws URISyntaxException {
        return new OandaPriceStream(
                httpClient,
                apiKey,
                streamUrl,
                accountId,
                instruments,
                objectMapper
        );
    }
}