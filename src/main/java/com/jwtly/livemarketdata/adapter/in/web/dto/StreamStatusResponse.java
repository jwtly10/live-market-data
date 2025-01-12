package com.jwtly.livemarketdata.adapter.in.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.jwtly.livemarketdata.domain.model.Broker;
import com.jwtly.livemarketdata.domain.model.stream.StreamError;
import com.jwtly.livemarketdata.domain.model.stream.StreamState;

import java.util.List;

public record StreamStatusResponse(
        @JsonProperty("id") String id,
        @JsonProperty("broker") Broker broker,
        @JsonProperty("state") StreamState state,
        @JsonProperty("instruments") List<String> instruments,
        @JsonProperty("errors") List<StreamError> errors
) {
}
