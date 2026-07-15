package com.hogwai.perf.webflux.repository;

import com.hogwai.perf.webflux.model.Product;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

public interface ProductRepository extends ReactiveCrudRepository<Product, Long> {
    Flux<Product> findAllByOrderByName();
}
