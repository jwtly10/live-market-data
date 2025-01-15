package com.jwtly.livemarketdata.adapter.out.broker.config;

import com.jwtly.livemarketdata.domain.model.Broker;
import com.jwtly.livemarketdata.domain.port.out.MarketDataPort;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.EnumMap;
import java.util.Map;

@Configuration
public class BrokerConfig {
    @Bean
    public Map<Broker, MarketDataPort> brokerAdapters(
            @Qualifier("oandaMarketDataAdapter") MarketDataPort oandaAdapter
    ) {
        Map<Broker, MarketDataPort> adapters = new EnumMap<>(Broker.class);
        adapters.put(Broker.OANDA, oandaAdapter);
        return adapters;
    }
}
