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
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

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
        StreamId streamId = StreamId.generate();
        MarketDataPort adapter = validateAndGetAdapter(command.broker());

        StreamHandle handle = new StreamHandle(streamId, command.broker(), command.instruments());

        Flux<Price> priceStream = adapter.createPriceStream(streamId.toString(), command.instruments())
                .flatMap(price -> publisher.publishPrice(command.broker().name(), price)
                        .doOnSuccess(__ -> handle.markRunning())
                        .doOnError(error -> {
                            log.error("Failed to publish price for broker {}",
                                    command.broker(),
                                    error
                            );
                            handle.recordError(error, "price_publish");
                        })
                        .thenReturn(price)
                )
                .doOnError(error -> {
                    if (error instanceof WebClientResponseException wcre) {
                        log.error("Stream error from broker {}: {} {}",
                                command.broker(),
                                wcre.getStatusCode(),
                                wcre.getMessage()
                        );
                    } else {
                        log.error("Unexpected stream error from broker {}",
                                command.broker(),
                                error
                        );
                    }
                    handle.recordError(error, "stream_error");
                })
                .subscribeOn(Schedulers.boundedElastic())
                .share();

        handle.setSubscription(priceStream.subscribe());
        activeStreams.put(streamId, handle);

        return Mono.just(handle.toStatus());
    }

    @Override
    public Mono<Void> stopStream(StreamId streamId) {
        log.debug("Stopping stream: {}", streamId);
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
        log.debug("Retrieving active streams");
        return Flux.fromIterable(activeStreams.values())
                .map(StreamHandle::toStatus);
    }

    @Override
    public Mono<StreamStatus> getStreamStatus(StreamId streamId) {
        log.debug("Retrieving stream status: {}", streamId);
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
