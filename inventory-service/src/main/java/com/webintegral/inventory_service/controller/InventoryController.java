package com.webintegral.inventory_service.controller;

import com.webintegral.inventory_service.model.Inventory;
import com.webintegral.inventory_service.repository.InventoryRepository;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/microservices/inventory")
public class InventoryController {

    private final InventoryRepository repository;

    public InventoryController(InventoryRepository repository) {
        super();
        this.repository = repository;
    }

    @GetMapping("/{productId}")
    public ResponseEntity<Inventory> getInventory(
            @PathVariable Long productId) {

        return repository.findById(productId)
                .map(ResponseEntity::ok)
                .orElseGet(() ->
                        ResponseEntity.notFound().build());
    }

    @GetMapping
    public List<Inventory> getAllInventory() {
        return repository.findAll();
    }

    @PostMapping
    public ResponseEntity<Inventory> createInventory(
            @RequestBody Inventory inventory) {

        if (inventory.getProductId() == null
                || inventory.getAvailableUnits() == null
                || inventory.getAvailableUnits() < 0) {
            return ResponseEntity.badRequest().build();
        }

        inventory.setStockStatus(
                inventory.getAvailableUnits() > 0
                        ? "IN_STOCK"
                        : "OUT_OF_STOCK"
        );

        Inventory saved = repository.save(inventory);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(saved);
    }
}