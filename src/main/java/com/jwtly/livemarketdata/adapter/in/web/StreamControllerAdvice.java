package com.jwtly.livemarketdata.adapter.in.web;

import com.jwtly.livemarketdata.adapter.in.web.dto.ApiError;
import com.jwtly.livemarketdata.domain.exception.broker.UnsupportedBrokerException;
import com.jwtly.livemarketdata.domain.exception.stream.StreamCreationException;
import com.jwtly.livemarketdata.domain.exception.stream.StreamNotFoundException;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class StreamControllerAdvice {

    @ExceptionHandler(StreamNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ApiResponse(
            responseCode = "404",
            description = "Stream not found",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ApiError.class)
            )
    )
    public ApiError handleStreamNotFound(StreamNotFoundException ex) {
        log.error("Stream not found", ex);
        return new ApiError(ex.getMessage());
    }

    @ExceptionHandler(UnsupportedBrokerException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ApiResponse(
            responseCode = "400",
            description = "Unsupported broker",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ApiError.class)
            )
    )
    public ApiError handleUnsupportedBroker(UnsupportedBrokerException ex) {
        log.error("Unsupported broker", ex);
        return new ApiError(ex.getMessage());
    }

    @ExceptionHandler(StreamCreationException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ApiResponse(
            responseCode = "500",
            description = "Stream creation failed",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ApiError.class)
            )
    )
    public ApiError handleStreamCreation(StreamCreationException ex) {
        log.error("Stream creation failed", ex);
        return new ApiError(ex.getMessage());
    }

    @ExceptionHandler(RuntimeException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ApiResponse(
            responseCode = "500",
            description = "Unexpected error occurred",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ApiError.class)
            )
    )
    public ApiError handleUnexpectedException(RuntimeException ex) {
        log.error("Unexpected error occurred", ex);
        return new ApiError("An unexpected error occurred");
    }
}