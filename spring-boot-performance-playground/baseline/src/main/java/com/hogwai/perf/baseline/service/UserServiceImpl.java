package com.hogwai.perf.baseline.service;

import com.hogwai.perf.common.dto.DashboardResponse;
import com.hogwai.perf.common.dto.UserResponse;
import com.hogwai.perf.common.model.User;
import com.hogwai.perf.common.repository.OrderRepository;
import com.hogwai.perf.common.repository.UserRepository;
import com.hogwai.perf.common.service.UserService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final OrderRepository orderRepository;

    public UserServiceImpl(UserRepository userRepository, OrderRepository orderRepository) {
        this.userRepository = userRepository;
        this.orderRepository = orderRepository;
    }

    @Override
    public UserResponse getUserById(Long id) {
        var user = userRepository.findById(id).orElseThrow();
        // N+1: each access to orders triggers a separate query
        int orderCount = (int) orderRepository.countByUserId(id);
        return new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getLoyaltyPoints(), orderCount);
    }

    @Override
    public DashboardResponse getDashboard(Long id) {
        var user = userRepository.findById(id).orElseThrow();
        // Sequential calls -- 3 round-trips
        var orders = orderRepository.findByUserIdOrderByCreatedAtDesc(id);
        var recentOrders = orders.stream()
                .limit(10)
                .map(o -> new DashboardResponse.OrderSummary(o.getId(), o.getStatus(), o.getTotalAmount().doubleValue()))
                .toList();
        return new DashboardResponse(
                new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getLoyaltyPoints(), recentOrders.size()),
                recentOrders,
                user.getLoyaltyPoints()
        );
    }
}
