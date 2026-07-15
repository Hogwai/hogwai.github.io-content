package com.hogwai.perf.webflux.controller;

import com.hogwai.perf.webflux.model.Product;
import com.hogwai.perf.webflux.repository.ProductRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api")
public class ProductController {

    private final ProductRepository productRepository;

    public ProductController(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @GetMapping("/products")
    @Cacheable(value = "webflux_products", key = "'all'")
    public Flux<Product> getProducts() {
        return productRepository.findAllByOrderByName();
    }

    @GetMapping("/products/{id}")
    @Cacheable(value = "webflux_products", key = "#id")
    public Mono<Product> getProduct(@PathVariable Long id) {
        return productRepository.findById(id);
    }
}
