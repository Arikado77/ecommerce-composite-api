package com.webintegral.catalog_service.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.webintegral.catalog_service.model.Product;
import com.webintegral.catalog_service.repository.ProductRepository;

import java.util.List;

@RestController
@RequestMapping("/microservices/products")
public class ProductController {

    private final ProductRepository repository;

    public ProductController(ProductRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/{id}")
    public ResponseEntity<Product> getProduct(@PathVariable Long id) {
        return repository.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping
    public List<Product> getAllProducts() {
        return repository.findAll();
    }

    @PostMapping
    public ResponseEntity<Product> createProduct(@RequestBody Product product) {
        Product saved = repository.save(product);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }
}