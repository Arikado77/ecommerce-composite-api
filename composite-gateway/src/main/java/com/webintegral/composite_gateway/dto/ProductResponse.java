package com.webintegral.composite_gateway.dto;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ProductResponse(Long id, String name, BigDecimal priceUsd) {
}
