package com.jwtly.livemarketdata.domain.model.stream;

import com.jwtly.livemarketdata.domain.model.Broker;

import java.util.List;

public record CreateStreamCommand(Broker broker, List<String> instruments) {
    public CreateStreamCommand {
        if (broker == null) {
            throw new IllegalArgumentException("Broker cannot be null");
        }
        if (instruments == null || instruments.isEmpty()) {
            throw new IllegalArgumentException("Instruments cannot be null or empty");
        }
    }
}
