package com.jwtly.livemarketdata.adapter.in.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.jwtly.livemarketdata.domain.model.Broker;

import java.util.List;

public record StreamCreateRequest(
        @JsonProperty("broker") Broker broker,
        @JsonProperty("instruments") List<String> instruments
) {
}
