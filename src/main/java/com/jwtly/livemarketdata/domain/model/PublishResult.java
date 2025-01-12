package com.jwtly.livemarketdata.domain.model;

public record PublishResult(
        String messageId,     // A unique identifier for the published message
        boolean successful,   // True if the message was successfully published, false otherwise
        String error          // Error message if not successful, null otherwise
) {
}
