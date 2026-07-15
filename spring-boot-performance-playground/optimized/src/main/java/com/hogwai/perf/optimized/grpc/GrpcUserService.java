package com.hogwai.perf.optimized.grpc;

import com.hogwai.perf.common.proto.*;
import com.hogwai.perf.common.repository.OrderRepository;
import com.hogwai.perf.common.repository.UserRepository;
import com.hogwai.perf.optimized.cache.MultiLevelCache;
import io.grpc.stub.StreamObserver;
import org.springframework.grpc.server.service.GrpcService;

import java.util.concurrent.StructuredTaskScope;

@GrpcService
public class GrpcUserService extends UserServiceGrpc.UserServiceImplBase {

    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final MultiLevelCache multiLevelCache;

    public GrpcUserService(UserRepository userRepository, OrderRepository orderRepository,
                           MultiLevelCache multiLevelCache) {
        this.userRepository = userRepository;
        this.orderRepository = orderRepository;
        this.multiLevelCache = multiLevelCache;
    }

    @Override
    public void getUser(GetUserRequest request, StreamObserver<UserResponse> responseObserver) {
        var response = multiLevelCache.get("grpc_user:" + request.getId(), UserResponse.class, () -> {
            var user = userRepository.findById(request.getId()).orElseThrow();
            int orderCount = (int) orderRepository.countByUserId(request.getId());
            return UserResponse.newBuilder()
                    .setId(user.getId())
                    .setName(user.getName())
                    .setEmail(user.getEmail())
                    .setLoyaltyPoints(user.getLoyaltyPoints())
                    .setOrderCount(orderCount)
                    .build();
        });
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void getDashboard(GetDashboardRequest request, StreamObserver<DashboardResponse> responseObserver) {
        try (var scope = StructuredTaskScope.open()) {
            var userFuture = scope.fork(() ->
                    multiLevelCache.get("grpc_user:" + request.getUserId(), UserResponse.class, () -> {
                        var user = userRepository.findById(request.getUserId()).orElseThrow();
                        int orderCount = (int) orderRepository.countByUserId(request.getUserId());
                        return UserResponse.newBuilder()
                                .setId(user.getId())
                                .setName(user.getName())
                                .setEmail(user.getEmail())
                                .setLoyaltyPoints(user.getLoyaltyPoints())
                                .setOrderCount(orderCount)
                                .build();
                    })
            );
            var ordersFuture = scope.fork(() -> {
                var orders = orderRepository.findByUserIdOrderByCreatedAtDesc(request.getUserId());
                var dashResp = DashboardResponse.newBuilder();
                orders.stream().limit(10).forEach(o ->
                        dashResp.addRecentOrders(OrderSummary.newBuilder()
                                .setId(o.getId())
                                .setStatus(o.getStatus())
                                .setTotalAmount(o.getTotalAmount().doubleValue())
                                .build())
                );
                return dashResp;
            });
            var pointsFuture = scope.fork(() ->
                    userRepository.findById(request.getUserId()).orElseThrow().getLoyaltyPoints()
            );
            scope.join();

            var response = ((DashboardResponse.Builder) ordersFuture.get())
                    .setUser((UserResponse) userFuture.get())
                    .setLoyaltyPoints((Integer) pointsFuture.get())
                    .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            responseObserver.onError(e);
        } catch (Exception e) {
            responseObserver.onError(e);
        }
    }
}
