package com.jwtly.livemarketdata.adapter.out.broker.oanda;

import com.jwtly.livemarketdata.adapter.out.broker.oanda.client.OandaClient;
import com.jwtly.livemarketdata.domain.model.Price;
import com.jwtly.livemarketdata.domain.port.out.MarketDataPort;
import com.jwtly.livemarketdata.domain.port.out.Stream;

import java.net.URISyntaxException;
import java.util.List;

public class OandaMarketDataAdapter implements MarketDataPort {
    private final OandaClient client;

    public OandaMarketDataAdapter(OandaClient client) {
        this.client = client;
    }

    @Override
    public Stream<Price> createPriceStream(List<String> instruments) throws URISyntaxException {
        return client.createPriceStream(instruments);
    }
}