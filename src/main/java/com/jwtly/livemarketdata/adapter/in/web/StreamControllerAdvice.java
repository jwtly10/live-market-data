package com.jwtly.livemarketdata.adapter.in.web;

import com.jwtly.livemarketdata.adapter.in.web.dto.ApiError;
import com.jwtly.livemarketdata.domain.exception.broker.UnsupportedBrokerException;
import com.jwtly.livemarketdata.domain.exception.stream.StreamNotFoundException;
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
    public ApiError handleStreamNotFound(StreamNotFoundException ex) {
        log.error("Stream not found", ex);
        return new ApiError(ex.getMessage());
    }

    @ExceptionHandler(UnsupportedBrokerException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleUnsupportedBroker(UnsupportedBrokerException ex) {
        log.error("Unsupported broker", ex);
        return new ApiError(ex.getMessage());
    }

    @ExceptionHandler(RuntimeException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiError handleUnexpectedException(RuntimeException ex) {
        log.error("Unexpected error occurred", ex);
        return new ApiError("An unexpected error occurred");
    }
}