package com.jwtly.livemarketdata.domain.model.stream;

import com.jwtly.livemarketdata.domain.model.Broker;
import reactor.core.Disposable;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedDeque;

public class StreamHandle {
    private final StreamId id;
    private final Broker broker;
    private final List<String> instruments;
    private volatile Disposable subscription;
    private volatile StreamState state;

    private final Deque<StreamError> recentErrors;
    private static final int MAX_ERROR_HISTORY = 10;

    public StreamHandle(StreamId id, Broker broker, List<String> instruments) {
        this.id = Objects.requireNonNull(id, "id cannot be null");
        this.broker = Objects.requireNonNull(broker, "broker cannot be null");
        this.instruments = List.copyOf(instruments);
        this.state = StreamState.PENDING;
        this.recentErrors = new ConcurrentLinkedDeque<>();
    }

    public void setSubscription(Disposable subscription) {
        this.subscription = Objects.requireNonNull(subscription, "subscription cannot be null");
    }

    public void markRunning() {
        this.state = StreamState.RUNNING;
    }

    public void markError() {
        this.state = StreamState.ERROR;
    }

    public void recordError(Throwable error, String context) {
        StreamError streamError = new StreamError(
                Instant.now(),
                error.getMessage(),
                context,
                error.getClass().getSimpleName()
        );

        recentErrors.addFirst(streamError);
        while (recentErrors.size() > MAX_ERROR_HISTORY) {
            recentErrors.removeLast();
        }

        markError();
    }

    public Broker broker() {
        return broker;
    }

    public List<String> instruments() {
        return instruments;
    }

    public Disposable subscription() {
        return subscription;
    }

    public List<StreamError> getRecentErrors() {
        return new ArrayList<>(recentErrors);
    }

    public StreamStatus toStatus() {
        return new StreamStatus(
                id,
                broker,
                instruments,
                state,
                getRecentErrors()
        );
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StreamHandle that = (StreamHandle) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}