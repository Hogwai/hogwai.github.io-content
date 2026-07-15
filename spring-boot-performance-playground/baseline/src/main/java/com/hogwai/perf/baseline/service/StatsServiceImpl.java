package com.hogwai.perf.baseline.service;

import com.hogwai.perf.common.dto.StatsResponse;
import com.hogwai.perf.common.repository.OrderRepository;
import com.hogwai.perf.common.repository.ProductRepository;
import com.hogwai.perf.common.repository.UserRepository;
import com.hogwai.perf.common.service.StatsService;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
public class StatsServiceImpl implements StatsService {

    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;

    public StatsServiceImpl(UserRepository userRepository, OrderRepository orderRepository, ProductRepository productRepository) {
        this.userRepository = userRepository;
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
    }

    @Override
    @Cacheable("stats")
    public StatsResponse getStats() {
        long users = userRepository.countUsers();
        long orders = orderRepository.count();
        long products = productRepository.countProducts();
        double totalValue = userRepository.totalOrderValue();
        double avgOrderValue = orders > 0 ? totalValue / orders : 0;
        return new StatsResponse(users, orders, products, avgOrderValue);
    }
}
