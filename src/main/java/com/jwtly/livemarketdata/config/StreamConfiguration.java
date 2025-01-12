package com.jwtly.livemarketdata.config;

import com.jwtly.livemarketdata.domain.model.Broker;
import com.jwtly.livemarketdata.domain.port.in.StreamManagementUseCase;
import com.jwtly.livemarketdata.domain.port.out.MarketDataPort;
import com.jwtly.livemarketdata.domain.port.out.MarketDataPublisherPort;
import com.jwtly.livemarketdata.domain.service.StreamManagementService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.EnumMap;
import java.util.Map;

@Configuration
public class StreamConfiguration {
    @Bean
    public Map<Broker, MarketDataPort> brokerAdapters(
            @Qualifier("oandaMarketDataAdapter") MarketDataPort oandaAdapter
    ) {
        Map<Broker, MarketDataPort> adapters = new EnumMap<>(Broker.class);
        adapters.put(Broker.OANDA, oandaAdapter);
        return adapters;
    }

    @Bean
    public StreamManagementUseCase streamManagementUseCase(
            Map<Broker, MarketDataPort> brokerAdapters,
            MarketDataPublisherPort publisher
    ) {
        return new StreamManagementService(brokerAdapters, publisher);
    }
}