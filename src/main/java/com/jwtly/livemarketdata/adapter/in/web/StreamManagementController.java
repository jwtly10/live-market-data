package com.jwtly.livemarketdata.adapter.in.web;

import com.jwtly.livemarketdata.adapter.in.web.dto.ApiError;
import com.jwtly.livemarketdata.adapter.in.web.dto.StreamCreateRequest;
import com.jwtly.livemarketdata.adapter.in.web.dto.StreamStatusResponse;
import com.jwtly.livemarketdata.adapter.in.web.mapper.StreamDtoMapper;
import com.jwtly.livemarketdata.domain.model.stream.StreamId;
import com.jwtly.livemarketdata.domain.model.stream.StreamStatus;
import com.jwtly.livemarketdata.domain.port.in.StreamManagementUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/stream")
@Tag(name = "Stream Management", description = "APIs for managing market data streams")
public class StreamManagementController {
    private final StreamManagementUseCase streamManagementUseCase;
    private final StreamDtoMapper mapper;

    public StreamManagementController(
            StreamManagementUseCase streamManagementUseCase,
            StreamDtoMapper mapper
    ) {
        this.streamManagementUseCase = streamManagementUseCase;
        this.mapper = mapper;
    }

    @Operation(
            summary = "Create a new stream",
            description = "Creates a new market data stream based on the provided configuration"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Stream created successfully"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Unsupported broker",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiError.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Stream creation failed",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiError.class)
                    )
            )
    })
    @PostMapping
    public StreamStatusResponse createStream(@RequestBody StreamCreateRequest req) {
        StreamStatus status = streamManagementUseCase.createStream(mapper.toCommand(req));
        return mapper.toResponse(status);
    }

    @Operation(
            summary = "Stop a running stream",
            description = "Stops a specific running stream"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Stream status retrieved successfully"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Stream not found",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiError.class)
                    )
            ),
    })
    @PostMapping("/{streamId}/stop")
    public void stopStream(@PathVariable("streamId") String streamId) {
        streamManagementUseCase.stopStream(StreamId.of(streamId));
    }

    @Operation(
            summary = "Get stream status",
            description = "Retrieves the current status of a specific stream"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Stream status retrieved successfully"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Stream not found",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiError.class)
                    )
            )
    })
    @GetMapping
    public List<StreamStatusResponse> getActiveStreams() {
        return streamManagementUseCase.getActiveStreams()
                .stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Operation(
            summary = "Get stream status",
            description = "Retrieves the current status of a specific stream"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Stream status retrieved successfully"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Stream not found",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiError.class)
                    )
            )
    })
    @GetMapping("/{streamId}")
    public StreamStatusResponse getStream(@PathVariable("streamId") String streamId) {
        StreamStatus status = streamManagementUseCase.getStreamStatus(StreamId.of(streamId));
        return mapper.toResponse(status);
    }
}