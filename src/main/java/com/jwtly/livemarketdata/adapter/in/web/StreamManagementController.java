package com.jwtly.livemarketdata.adapter.in.web;

import com.jwtly.livemarketdata.adapter.in.web.dto.ApiError;
import com.jwtly.livemarketdata.adapter.in.web.dto.StreamCreateRequest;
import com.jwtly.livemarketdata.adapter.in.web.dto.StreamStatusResponse;
import com.jwtly.livemarketdata.adapter.in.web.mapper.StreamDtoMapper;
import com.jwtly.livemarketdata.domain.model.stream.StreamId;
import com.jwtly.livemarketdata.domain.port.in.StreamManagementUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/stream")
@Tag(name = "Stream Management", description = "APIs for managing market data streams")
public class StreamManagementController {
    private final StreamManagementUseCase streamManagement;
    private final StreamDtoMapper mapper;

    public StreamManagementController(
            StreamManagementUseCase streamManagementUseCase,
            StreamDtoMapper mapper
    ) {
        this.streamManagement = streamManagementUseCase;
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
    public Mono<ResponseEntity<StreamStatusResponse>> createStream(@RequestBody StreamCreateRequest req) {
        return streamManagement.createStream(mapper.toCommand(req))
                .map(res -> ResponseEntity.ok(mapper.toResponse(res)));
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
    @DeleteMapping("/{streamId}")
    public Mono<ResponseEntity<Void>> stopStream(@PathVariable String streamId) {
        return streamManagement.stopStream(StreamId.of(streamId))
                .then(Mono.just(ResponseEntity.ok().build()));
    }

    @Operation(
            summary = "Get all active streams",
            description = "Retrieves a list of all currently active streams"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Stream status's retrieved successfully"
            ),
    })
    @GetMapping
    public Flux<StreamStatusResponse> getActiveStreams() {
        return streamManagement.getActiveStreams()
                .map(mapper::toResponse);
    }

    @Operation(
            summary = "Get specific stream status",
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
    public Mono<ResponseEntity<StreamStatusResponse>> getStreamStatus(@PathVariable String streamId) {
        return streamManagement.getStreamStatus(StreamId.of(streamId))
                .map(res -> ResponseEntity.ok(mapper.toResponse(res)));
    }
}