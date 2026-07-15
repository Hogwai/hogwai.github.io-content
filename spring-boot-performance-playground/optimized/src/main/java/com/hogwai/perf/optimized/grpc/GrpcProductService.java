package com.hogwai.perf.optimized.grpc;

import com.hogwai.perf.common.proto.*;
import com.hogwai.perf.common.repository.ProductRepository;
import io.grpc.stub.StreamObserver;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.grpc.server.service.GrpcService;

@GrpcService
public class GrpcProductService extends ProductServiceGrpc.ProductServiceImplBase {

    private final ProductRepository productRepository;

    public GrpcProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Override
    @Cacheable(value = "grpc_products", key = "'page:' + #request.page")
    public void getProducts(GetProductsRequest request, StreamObserver<ProductListResponse> responseObserver) {
        var page = PageRequest.of(request.getPage(), request.getPageSize() > 0 ? request.getPageSize() : 50);
        var products = productRepository.findAll(page);
        var response = ProductListResponse.newBuilder();
        products.forEach(p ->
                response.addProducts(ProductResponse.newBuilder()
                        .setId(p.getId())
                        .setName(p.getName())
                        .setDescription(p.getDescription())
                        .setPrice(p.getPrice().doubleValue())
                        .setStockQuantity(p.getStockQuantity())
                        .build())
        );
        responseObserver.onNext(response.build());
        responseObserver.onCompleted();
    }
}
