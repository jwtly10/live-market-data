package com.jwtly.livemarketdata.domain.service;

import com.jwtly.livemarketdata.domain.exception.broker.UnsupportedBrokerException;
import com.jwtly.livemarketdata.domain.exception.stream.StreamCreationException;
import com.jwtly.livemarketdata.domain.exception.stream.StreamNotFoundException;
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
import java.util.concurrent.ConcurrentHashMap;
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
    public StreamStatus createStream(CreateStreamCommand command) {
        MarketDataPort adapter = brokerAdapters.get(command.broker());
        if (adapter == null) {
            throw new UnsupportedBrokerException(
                    String.format("Unsupported broker: %s. Supported brokers: %s",
                            command.broker(),
                            Arrays.toString(Broker.values())
                    )
            );
        }

        var streamId = StreamId.generate();
        try {
            Stream<Price> stream = adapter.createPriceStream(command.instruments());
            ManagedStream managedStream = new ManagedStream(streamId, command.broker(), command.instruments(), stream, publisher);
            streams.put(streamId, managedStream);

            managedStream.start();

            // TODO: Wait for stream to start before returning success
            return managedStream.getStatus();
        } catch (Exception e) {
            throw new StreamCreationException("Failed to create stream", e);
        }
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
