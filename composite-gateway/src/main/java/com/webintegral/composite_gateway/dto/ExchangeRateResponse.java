package com.webintegral.composite_gateway.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ExchangeRateResponse(String base, LocalDate date, Map<String, BigDecimal> rates) {
}
