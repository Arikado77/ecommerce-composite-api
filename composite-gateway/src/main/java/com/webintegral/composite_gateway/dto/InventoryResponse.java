package com.webintegral.composite_gateway.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record InventoryResponse(Long productId, Integer availableUnits, String stockStatus) {
}
