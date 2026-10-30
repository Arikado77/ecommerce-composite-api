package com.webintegral.composite_gateway.dto;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ProductSummaryResponse(
        @JsonProperty("product_id") Long productId,
        @JsonProperty("title") String title,
        @JsonProperty("price_usd") BigDecimal priceUsd,
        @JsonProperty("price_mxn") BigDecimal priceMxn,
        @JsonProperty("stock_status") String stockStatus,
        @JsonProperty("available_units") Integer availableUnits,
        @JsonProperty("currency_conversion_source") String currencyConversionSource,
        @JsonProperty("exchange_rate") BigDecimal exchangeRate,
        @JsonProperty("status") String status) {
}
