package com.jwtly.livemarketdata.adapter.in.web.mapper;

import com.jwtly.livemarketdata.adapter.in.web.dto.StreamCreateRequest;
import com.jwtly.livemarketdata.adapter.in.web.dto.StreamStatusResponse;
import com.jwtly.livemarketdata.domain.model.stream.CreateStreamCommand;
import com.jwtly.livemarketdata.domain.model.stream.StreamStatus;
import org.springframework.stereotype.Component;

@Component
public class StreamDtoMapper {
    public StreamStatusResponse toResponse(StreamStatus status) {
        return new StreamStatusResponse(
                status.streamId().value(),
                status.broker(),
                status.state(),
                status.instruments()
        );
    }

    public CreateStreamCommand toCommand(StreamCreateRequest req) {
        return new CreateStreamCommand(req.broker(), req.instruments());
    }
}
