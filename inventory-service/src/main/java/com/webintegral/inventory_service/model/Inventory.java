package com.webintegral.inventory_service.model;

import jakarta.persistence.*;

@Entity
@Table(name = "inventory")
public class Inventory {

    @Id
    private Long productId;

    private Integer availableUnits;
    private String warehouseLocation;
    private Integer estimatedDeliveryDays;
    private String stockStatus;

    public Inventory() {}

    public Inventory(Long productId, Integer availableUnits,
                     String warehouseLocation,
                     Integer estimatedDeliveryDays,
                     String stockStatus) {
        this.productId = productId;
        this.availableUnits = availableUnits;
        this.warehouseLocation = warehouseLocation;
        this.estimatedDeliveryDays = estimatedDeliveryDays;
        this.stockStatus = stockStatus;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public Integer getAvailableUnits() {
        return availableUnits;
    }

    public void setAvailableUnits(Integer availableUnits) {
        this.availableUnits = availableUnits;
    }

    public String getWarehouseLocation() {
        return warehouseLocation;
    }

    public void setWarehouseLocation(String warehouseLocation) {
        this.warehouseLocation = warehouseLocation;
    }

    public Integer getEstimatedDeliveryDays() {
        return estimatedDeliveryDays;
    }

    public void setEstimatedDeliveryDays(Integer estimatedDeliveryDays) {
        this.estimatedDeliveryDays = estimatedDeliveryDays;
    }

    public String getStockStatus() {
        return stockStatus;
    }

    public void setStockStatus(String stockStatus) {
        this.stockStatus = stockStatus;
    }
}