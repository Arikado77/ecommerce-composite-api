package com.webintegral.composite_gateway.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ErrorResponse(
        @JsonProperty("status") String status,
        @JsonProperty("message") String message,
        @JsonProperty("service") String service) {
}
