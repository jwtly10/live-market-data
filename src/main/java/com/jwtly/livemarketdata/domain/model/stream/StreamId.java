package com.jwtly.livemarketdata.domain.model.stream;

import java.util.Objects;
import java.util.UUID;

public final class StreamId {
    private final String value;

    private StreamId(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("StreamId must not be null or empty");
        }
        this.value = value;
    }

    public static StreamId generate() {
        return new StreamId(UUID.randomUUID().toString());
    }

    public static StreamId of(String value) {
        return new StreamId(value);
    }

    public String value() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StreamId streamId = (StreamId) o;
        return Objects.equals(value, streamId.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return value;
    }


}
