package com.jwtly.livemarketdata.domain.model.stream;

import com.jwtly.livemarketdata.domain.model.Broker;

import java.util.List;

public record StreamStatus(
        StreamId streamId,
        Broker broker,
        List<String> instruments,
        StreamState state) {
}
