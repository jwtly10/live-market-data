package com.jwtly.livemarketdata.domain.model;

import com.jwtly.livemarketdata.domain.exception.broker.UnsupportedBrokerException;
import lombok.Getter;

import java.util.Arrays;

@Getter
public enum Broker {
    OANDA("OANDA");

    private final String identifier;

    Broker(String identifier) {
        this.identifier = identifier;
    }

    public static Broker fromString(String identifier) {
        return Arrays.stream(values())
                .filter(b -> b.getIdentifier().equalsIgnoreCase(identifier))
                .findFirst()
                .orElseThrow(() -> new UnsupportedBrokerException("Unsupported broker: " + identifier));
    }
}