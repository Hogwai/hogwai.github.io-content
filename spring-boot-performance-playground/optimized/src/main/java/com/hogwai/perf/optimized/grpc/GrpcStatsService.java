package com.hogwai.perf.optimized.grpc;

import com.hogwai.perf.common.proto.*;
import com.hogwai.perf.common.repository.OrderRepository;
import com.hogwai.perf.common.repository.ProductRepository;
import com.hogwai.perf.common.repository.UserRepository;
import com.google.protobuf.Empty;
import io.grpc.stub.StreamObserver;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.grpc.server.service.GrpcService;
import org.springframework.scheduling.annotation.Scheduled;

@GrpcService
public class GrpcStatsService extends StatsServiceGrpc.StatsServiceImplBase {

    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;

    public GrpcStatsService(UserRepository userRepository, OrderRepository orderRepository,
                            ProductRepository productRepository) {
        this.userRepository = userRepository;
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
    }

    @Override
    @Cacheable("grpc_stats")
    public void getStats(Empty request, StreamObserver<StatsResponse> responseObserver) {
        long users = userRepository.countUsers();
        long orders = orderRepository.count();
        long products = productRepository.countProducts();
        double totalValue = userRepository.totalOrderValue();
        double avgOrderValue = orders > 0 ? totalValue / orders : 0;
        var response = StatsResponse.newBuilder()
                .setTotalUsers(users)
                .setTotalOrders(orders)
                .setTotalProducts(products)
                .setAverageOrderValue(avgOrderValue)
                .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Scheduled(fixedRate = 60_000)
    @CacheEvict("grpc_stats")
    public void refreshStats() {}
}
