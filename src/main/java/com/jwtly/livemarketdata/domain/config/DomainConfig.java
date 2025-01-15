package com.jwtly.livemarketdata.domain.config;

import com.jwtly.livemarketdata.domain.model.Broker;
import com.jwtly.livemarketdata.domain.port.in.StreamManagementUseCase;
import com.jwtly.livemarketdata.domain.port.out.MarketDataPort;
import com.jwtly.livemarketdata.domain.port.out.MarketDataPublisherPort;
import com.jwtly.livemarketdata.domain.service.StreamManagementService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
public class DomainConfig {
    @Bean
    public StreamManagementUseCase streamManagementUseCase(
            Map<Broker, MarketDataPort> brokerAdapters,
            MarketDataPublisherPort publisher
    ) {
        return new StreamManagementService(brokerAdapters, publisher);
    }
}
