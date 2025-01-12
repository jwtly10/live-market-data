package com.jwtly.livemarketdata.domain.port.in;

import com.jwtly.livemarketdata.domain.model.stream.CreateStreamCommand;
import com.jwtly.livemarketdata.domain.model.stream.StreamId;
import com.jwtly.livemarketdata.domain.model.stream.StreamStatus;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface StreamManagementUseCase {
    Mono<StreamStatus> createStream(CreateStreamCommand command);

    Mono<Void> stopStream(StreamId streamId);

    Flux<StreamStatus> getActiveStreams();

    Mono<StreamStatus> getStreamStatus(StreamId streamId);
}
