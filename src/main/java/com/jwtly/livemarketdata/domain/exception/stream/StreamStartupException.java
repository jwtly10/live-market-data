package com.jwtly.livemarketdata.domain.exception.stream;

public class StreamStartupException extends Exception {
    public StreamStartupException(String message) {
        super(message);
    }

    public StreamStartupException(String message, Throwable cause) {
        super(message, cause);
    }
}
