package com.jwtly.livemarketdata.domain.service;

import com.jwtly.livemarketdata.domain.exception.stream.StreamStartupException;
import com.jwtly.livemarketdata.domain.model.Broker;
import com.jwtly.livemarketdata.domain.model.Price;
import com.jwtly.livemarketdata.domain.model.stream.StreamId;
import com.jwtly.livemarketdata.domain.model.stream.StreamState;
import com.jwtly.livemarketdata.domain.model.stream.StreamStatus;
import com.jwtly.livemarketdata.domain.port.out.MarketDataPublisherPort;
import com.jwtly.livemarketdata.domain.port.out.Stream;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Slf4j
public class ManagedStream {
    @Getter
    private final StreamId id;
    private final Broker broker;
    private final List<String> instruments;
    private final Stream<Price> stream;
    private final MarketDataPublisherPort publisher;
    @Getter
    private volatile StreamStatus status;

    public ManagedStream(StreamId id,
                         Broker broker,
                         List<String> instruments,
                         Stream<Price> stream,
                         MarketDataPublisherPort publisher
    ) {
        this.id = id;
        this.broker = broker;
        this.stream = stream;
        this.instruments = instruments;
        this.publisher = publisher;
        this.status = new StreamStatus(id, broker, instruments, StreamState.CREATED);
    }

    /**
     * Since our retryable stream implementation is async, we need to return a CompletableFuture
     * to signal when the stream has actually successfully started up.
     */
    public CompletableFuture<Void> start() {
        CompletableFuture<Void> startupFuture = new CompletableFuture<>();
        Stream.StreamCallback<Price> cb = new Stream.StreamCallback<>() {
            @Override
            public void onData(Price price) {
                // We know its started up if we get any price
                startupFuture.complete(null);
                status = new StreamStatus(id, broker, instruments, StreamState.RUNNING);

                publisher.publishPrice(broker.name(), price)
                        .whenComplete((result, error) -> {
                            if (error != null) {
                                log.error("Failed to publish price", error);
                            }
                        })
                ;
            }

            @Override
            public void onHeartbeat() {
                // We know its started up if we get any heartbeat
                startupFuture.complete(null);
                status = new StreamStatus(id, broker, instruments, StreamState.RUNNING);
            }

            @Override
            public void onError(Exception e) {
                if (e instanceof StreamStartupException) {
                    startupFuture.completeExceptionally(e);
                }
                // TODO: Append to a list of errors with timestamps for easy debugging
            }

            @Override
            public void onComplete() {
                log.info("Stream completed");
                status = new StreamStatus(id, broker, instruments, StreamState.COMPLETED);
            }
        };

        status = new StreamStatus(id, broker, instruments, StreamState.STARTING);
        stream.start(cb);
        return startupFuture;
    }

    public void stop() {
        stream.close();
        status = new StreamStatus(id, broker, instruments, StreamState.STOPPED);
    }
}
