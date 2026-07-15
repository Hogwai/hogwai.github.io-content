package com.hogwai.perf.optimized.controller;

import com.hogwai.perf.common.dto.ProductResponse;
import com.hogwai.perf.common.repository.ProductRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class ProductController {

    private final ProductRepository productRepository;

    public ProductController(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @GetMapping("/products")
    @Cacheable(value = "products", key = "'page:' + #page")
    public List<ProductResponse> getProducts(@RequestParam(defaultValue = "0") int page) {
        return productRepository.findAll(PageRequest.of(page, 50)).stream()
                .map(p -> new ProductResponse(p.getId(), p.getName(), p.getDescription(),
                        p.getPrice().doubleValue(), p.getStockQuantity()))
                .toList();
    }
}
