package com.jwtly.livemarketdata.domain.model.stream;

public enum StreamState {
    CREATED,
    STARTING,
    RUNNING,
    ACTIVE,
    ERROR,
    STOPPED,
    COMPLETED
}
