package com.webintegral.composite_gateway.controller;

import com.webintegral.composite_gateway.dto.ProductSummaryResponse;
import com.webintegral.composite_gateway.service.ProductSummaryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/product-summary")
public class ProductSummaryController {

    private final ProductSummaryService productSummaryService;

    public ProductSummaryController(ProductSummaryService productSummaryService) {
        this.productSummaryService = productSummaryService;
    }

    @GetMapping("/{id}")
    public ProductSummaryResponse getProductSummary(@PathVariable Long id) {
        return productSummaryService.getProductSummary(id);
    }
}
