package com.jwtly.livemarketdata.domain.service;

import com.jwtly.livemarketdata.domain.exception.broker.UnsupportedBrokerException;
import com.jwtly.livemarketdata.domain.exception.stream.StreamNotFoundException;
import com.jwtly.livemarketdata.domain.model.Broker;
import com.jwtly.livemarketdata.domain.model.Price;
import com.jwtly.livemarketdata.domain.model.stream.CreateStreamCommand;
import com.jwtly.livemarketdata.domain.model.stream.StreamHandle;
import com.jwtly.livemarketdata.domain.model.stream.StreamId;
import com.jwtly.livemarketdata.domain.model.stream.StreamStatus;
import com.jwtly.livemarketdata.domain.port.in.StreamManagementUseCase;
import com.jwtly.livemarketdata.domain.port.out.MarketDataPort;
import com.jwtly.livemarketdata.domain.port.out.MarketDataPublisherPort;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public class StreamManagementService implements StreamManagementUseCase {
    private final Map<Broker, MarketDataPort> brokerAdapters;
    private final MarketDataPublisherPort publisher;
    private final Map<StreamId, StreamHandle> activeStreams = new ConcurrentHashMap<>();

    public StreamManagementService(Map<Broker, MarketDataPort> brokerAdapters, MarketDataPublisherPort publisher) {
        this.brokerAdapters = brokerAdapters;
        this.publisher = publisher;
    }

    @Override
    public Mono<StreamStatus> createStream(CreateStreamCommand command) {
        MarketDataPort adapter = validateAndGetAdapter(command.broker());
        StreamId streamId = StreamId.generate();

        return Mono.defer(() -> {
            AtomicBoolean isHealthy = new AtomicBoolean(false);

            Flux<Price> priceStream = adapter.createPriceStream(streamId.toString(), command.instruments())
                    .doOnNext(price -> {
                        isHealthy.set(true);
                        publisher.publishPrice(command.broker().name(), price)
                                .whenComplete((result, error) -> {
                                    if (error != null) {
                                        log.error("Failed to publish price", error);
                                    }
                                });
                    })
                    .doOnError(e -> isHealthy.set(false))
                    .retry(10)
                    .subscribeOn(Schedulers.boundedElastic());

            return Mono.just(priceStream.subscribe())
                    .map(subscription -> {
                        StreamHandle handle = new StreamHandle(
                                streamId,
                                command.broker(),
                                command.instruments(),
                                subscription,
                                isHealthy
                        );
                        activeStreams.put(streamId, handle);
                        return handle.toStatus();
                    })
                    .timeout(Duration.ofSeconds(10))
                    .doOnError(e -> {
                        log.error("Failed to start stream", e);
                        Optional.ofNullable(activeStreams.remove(streamId))
                                .ifPresent(handle -> handle.subscription().dispose());
                    });
        });
    }

    @Override
    public Mono<Void> stopStream(StreamId streamId) {
        return Mono.fromRunnable(() -> {
            StreamHandle handle = activeStreams.remove(streamId);
            if (handle == null) {
                throw new StreamNotFoundException("Stream not found: " + streamId);
            }
            handle.subscription().dispose();
        });
    }

    @Override
    public Flux<StreamStatus> getActiveStreams() {
        return Flux.fromIterable(activeStreams.values())
                .map(StreamHandle::toStatus);
    }

    @Override
    public Mono<StreamStatus> getStreamStatus(StreamId streamId) {
        return Mono.justOrEmpty(activeStreams.get(streamId))
                .map(StreamHandle::toStatus)
                .switchIfEmpty(Mono.error(new StreamNotFoundException("Stream not found: " + streamId)));
    }

    private MarketDataPort validateAndGetAdapter(Broker broker) {
        return Optional.ofNullable(brokerAdapters.get(broker))
                .orElseThrow(() -> new UnsupportedBrokerException(
                        String.format("Unsupported broker: %s. Supported brokers: %s",
                                broker, Arrays.toString(Broker.values()))
                ));
    }
}
