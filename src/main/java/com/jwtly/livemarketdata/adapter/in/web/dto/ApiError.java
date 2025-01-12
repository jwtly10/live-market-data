package com.jwtly.livemarketdata.adapter.in.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Error response containing details about the failure")
public record ApiError(@Schema(
        description = "Detailed error message",
        example = "Stream with ID 'stream-123' not found"
) String message) {
    public ApiError(String message) {
        this.message = message;
    }
}