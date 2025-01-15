package com.jwtly.livemarketdata.adapter.out.broker.oanda.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.jwtly.livemarketdata.adapter.out.broker.oanda.OandaMarketDataAdapter;
import com.jwtly.livemarketdata.adapter.out.broker.oanda.stream.OandaPriceStream;
import com.jwtly.livemarketdata.domain.port.out.MarketDataPort;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class OandaConfig {
    @Bean
    public WebClient oandaWebClient(@Value("${oanda.stream-url}") String streamUrl) {
        return WebClient.builder()
                .baseUrl(streamUrl)
                .build();
    }

    @Bean
    @Qualifier("oandaMarketDataAdapter")
    public MarketDataPort oandaMarketDataAdapter(
            @Value("${oanda.api-key}") String apiKey,
            @Value("${oanda.account-id}") String accountId,
            WebClient oandaWebClient
    ) {
        return new OandaMarketDataAdapter(apiKey, accountId, oandaWebClient, objectMapper());
    }

    @Bean
    public OandaPriceStream oandaPriceStream(WebClient oandaWebClient) {
        return new OandaPriceStream(oandaWebClient, objectMapper());
    }

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper().registerModule(new JavaTimeModule());
    }
}
