package com.jwtly.livemarketdata.domain.service;

import com.jwtly.livemarketdata.domain.exception.broker.UnsupportedBrokerException;
import com.jwtly.livemarketdata.domain.exception.stream.StreamCreationException;
import com.jwtly.livemarketdata.domain.exception.stream.StreamNotFoundException;
import com.jwtly.livemarketdata.domain.exception.stream.StreamStartupException;
import com.jwtly.livemarketdata.domain.model.Broker;
import com.jwtly.livemarketdata.domain.model.Price;
import com.jwtly.livemarketdata.domain.model.stream.CreateStreamCommand;
import com.jwtly.livemarketdata.domain.model.stream.StreamId;
import com.jwtly.livemarketdata.domain.model.stream.StreamStatus;
import com.jwtly.livemarketdata.domain.port.in.StreamManagementUseCase;
import com.jwtly.livemarketdata.domain.port.out.MarketDataPort;
import com.jwtly.livemarketdata.domain.port.out.MarketDataPublisherPort;
import com.jwtly.livemarketdata.domain.port.out.Stream;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
public class StreamManagementService implements StreamManagementUseCase {
    private final Map<StreamId, ManagedStream> streams = new ConcurrentHashMap<>();
    private final Map<Broker, MarketDataPort> brokerAdapters;
    private final MarketDataPublisherPort publisher;

    public StreamManagementService(Map<Broker, MarketDataPort> brokerAdapters, MarketDataPublisherPort publisher) {
        this.brokerAdapters = brokerAdapters;
        this.publisher = publisher;
    }

    @Override
    public StreamStatus createStream(CreateStreamCommand command) throws StreamCreationException {
        MarketDataPort adapter = validateAndGetAdapter(command.broker());
        var streamId = StreamId.generate();
        try {
            ManagedStream managedStream = initializeStream(streamId, command, adapter);
            return startAndMonitorStream(streamId, managedStream);
        } catch (Exception e) {
            throw new StreamCreationException(String.format("Failed to create stream for broker: %s (%s)", command.broker(), e.getMessage()), e);
        }
    }

    private MarketDataPort validateAndGetAdapter(Broker broker) {
        MarketDataPort adapter = brokerAdapters.get(broker);
        if (adapter == null) {
            throw new UnsupportedBrokerException(
                    String.format("Unsupported broker: %s. Supported brokers: %s",
                            broker,
                            Arrays.toString(Broker.values())
                    )
            );
        }
        return adapter;
    }

    private ManagedStream initializeStream(StreamId streamId, CreateStreamCommand command, MarketDataPort adapter) throws Exception {
        Stream<Price> stream = adapter.createPriceStream(command.instruments());
        ManagedStream managedStream = new ManagedStream(
                streamId,
                command.broker(),
                command.instruments(),
                stream,
                publisher
        );
        streams.put(streamId, managedStream);
        return managedStream;
    }

    private StreamStatus startAndMonitorStream(StreamId streamId, ManagedStream managedStream) throws Exception {
        try {
            managedStream.start()
                    .orTimeout(10, TimeUnit.SECONDS)
                    .join();
            return managedStream.getStatus();
        } catch (Exception e) {
            handleFailedStartup(streamId, managedStream, e);
            throw translateStartupException(e);
        }
    }

    private void handleFailedStartup(StreamId streamId, ManagedStream managedStream, Exception e) {
        streams.remove(streamId);
        managedStream.stop();
    }

    private Exception translateStartupException(Exception e) {
        if (e instanceof CompletionException && e.getCause() instanceof StreamStartupException) {
            return (StreamStartupException) e.getCause();
        }
        return new StreamCreationException("Stream failed to start within timeout", e);
    }

    @Override
    public void stopStream(StreamId streamId) {
        var stream = streams.get(streamId);
        if (stream == null) {
            throw new StreamNotFoundException("Stream not found: " + streamId);
        }

        stream.stop();
        streams.remove(streamId);
    }

    @Override
    public List<StreamStatus> getActiveStreams() {
        return streams.values().stream()
                .map(ManagedStream::getStatus)
                .collect(Collectors.toList());
    }

    @Override
    public StreamStatus getStreamStatus(StreamId streamId) {
        var stream = streams.get(streamId);
        if (stream == null) {
            throw new StreamNotFoundException("Stream not found: " + streamId);
        }

        return stream.getStatus();
    }
}
