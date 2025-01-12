package com.jwtly.livemarketdata.domain.model.stream;

import com.jwtly.livemarketdata.domain.model.Broker;
import reactor.core.Disposable;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public record StreamHandle(
        StreamId id,
        Broker broker,
        List<String> instruments,
        Disposable subscription,
        AtomicBoolean isHealthy
) {
    public StreamStatus toStatus() {
        return new StreamStatus(
                id,
                broker,
                instruments,
                isHealthy.get() ? StreamState.RUNNING : StreamState.ERROR
        );
    }
}
