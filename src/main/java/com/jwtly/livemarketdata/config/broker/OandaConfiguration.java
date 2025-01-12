package com.jwtly.livemarketdata.config.broker;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.jwtly.livemarketdata.adapter.out.broker.oanda.OandaMarketDataAdapter;
import com.jwtly.livemarketdata.adapter.out.broker.oanda.client.OandaClient;
import com.jwtly.livemarketdata.domain.port.out.MarketDataPort;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.http.HttpClient;

@Configuration
public class OandaConfiguration {
    @Bean
    public OandaClient oandaClient(
            @Value("${oanda.api-key}") String apiKey,
            @Value("${oanda.account-id}") String accountId,
            @Value("${oanda.stream-url}") String streamUrl
    ) {
        return new OandaClient(
                HttpClient.newHttpClient(),
                apiKey,
                streamUrl,
                accountId,
                new ObjectMapper().registerModule(new JavaTimeModule())
        );
    }

    @Bean
    @Qualifier("oandaMarketDataAdapter")
    public MarketDataPort oandaMarketDataAdapter(OandaClient client) {
        return new OandaMarketDataAdapter(client);
    }
}
