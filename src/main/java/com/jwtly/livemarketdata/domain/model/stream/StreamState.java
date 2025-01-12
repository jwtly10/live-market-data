package com.jwtly.livemarketdata.domain.model.stream;

public enum StreamState {
    PENDING,
    CREATED,
    STARTING,
    RUNNING,
    STOPPED,
    COMPLETED,
    ERROR
}
