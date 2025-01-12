package com.jwtly.livemarketdata.domain.port.in;

import com.jwtly.livemarketdata.domain.model.stream.CreateStreamCommand;
import com.jwtly.livemarketdata.domain.model.stream.StreamId;
import com.jwtly.livemarketdata.domain.model.stream.StreamStatus;

import java.util.List;

public interface StreamManagementUseCase {
    /**
     * Create a new stream.
     *
     * @param command The command to create a stream.
     * @return The status of the created stream.
     */
    StreamStatus createStream(CreateStreamCommand command);

    /**
     * Stop a stream.
     *
     * @param streamId The ID of the stream to stop.
     */
    void stopStream(StreamId streamId);

    /**
     * Get all active streams.
     *
     * @return A list of active streams.
     */
    List<StreamStatus> getActiveStreams();

    /**
     * Get the status of a stream.
     *
     * @param streamId The ID of the stream to get the status of.
     * @return The status of the stream.
     */
    StreamStatus getStreamStatus(StreamId streamId);
}
