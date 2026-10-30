package com.webintegral.catalog_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.webintegral.catalog_service.model.Product;

public interface ProductRepository extends JpaRepository<Product, Long> {
}