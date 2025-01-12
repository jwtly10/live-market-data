package com.jwtly.livemarketdata.domain.exception.stream;

public class StreamNotFoundException extends RuntimeException {
    public StreamNotFoundException(String message) {
        super(message);
    }
}
