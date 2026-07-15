package com.hogwai.perf.optimized.service;

import com.hogwai.perf.common.dto.DashboardResponse;
import com.hogwai.perf.common.dto.UserResponse;
import com.hogwai.perf.common.repository.OrderRepository;
import com.hogwai.perf.common.repository.UserRepository;
import com.hogwai.perf.common.service.UserService;
import com.hogwai.perf.optimized.cache.MultiLevelCache;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.StructuredTaskScope;

@Service
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final MultiLevelCache multiLevelCache;

    public UserServiceImpl(UserRepository userRepository, OrderRepository orderRepository,
                           MultiLevelCache multiLevelCache) {
        this.userRepository = userRepository;
        this.orderRepository = orderRepository;
        this.multiLevelCache = multiLevelCache;
    }

    @Override
    @Cacheable(value = "users", key = "#id")
    public UserResponse getUserById(Long id) {
        return multiLevelCache.get("user:" + id, UserResponse.class, () -> {
            var user = userRepository.findById(id).orElseThrow();
            // Uses a dedicated count query instead of triggering N+1 on the collection
            int orderCount = (int) orderRepository.countByUserId(id);
            return new UserResponse(user.getId(), user.getName(), user.getEmail(),
                    user.getLoyaltyPoints(), orderCount);
        });
    }

    @Override
    public DashboardResponse getDashboard(Long id) {
        try (var scope = StructuredTaskScope.<Object>open()) {
            // All three calls execute in parallel on virtual threads
            var userSubtask = scope.fork(() -> getUserById(id));

            var ordersSubtask = scope.fork(() -> {
                var orders = orderRepository.findByUserIdOrderByCreatedAtDesc(id);
                return orders.stream()
                        .limit(10)
                        .map(o -> new DashboardResponse.OrderSummary(
                                o.getId(), o.getStatus(), o.getTotalAmount().doubleValue()))
                        .toList();
            });

            var pointsSubtask = scope.fork(() ->
                    userRepository.findById(id).orElseThrow().getLoyaltyPoints()
            );

            scope.join();

            return new DashboardResponse(
                    (UserResponse) userSubtask.get(),
                    (List<DashboardResponse.OrderSummary>) ordersSubtask.get(),
                    (Integer) pointsSubtask.get()
            );
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Dashboard request interrupted", e);
        }
    }
}
