package com.jwtly.livemarketdata.domain.exception.stream;

public class StreamCreationException extends RuntimeException {
    public StreamCreationException(String message) {
        super(message);
    }

    public StreamCreationException(String message, Throwable cause) {
        super(message, cause);
    }
}
