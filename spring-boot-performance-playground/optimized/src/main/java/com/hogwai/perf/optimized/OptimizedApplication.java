package com.hogwai.perf.optimized;

import com.hogwai.perf.common.model.User;
import com.hogwai.perf.common.model.Product;
import com.hogwai.perf.common.model.Order;
import com.hogwai.perf.common.model.OrderItem;
import com.hogwai.perf.common.repository.UserRepository;
import com.hogwai.perf.common.repository.ProductRepository;
import com.hogwai.perf.common.repository.OrderRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import java.math.BigDecimal;

@SpringBootApplication(scanBasePackages = "com.hogwai.perf")
@EntityScan("com.hogwai.perf.common.model")
@EnableJpaRepositories("com.hogwai.perf.common.repository")
@EnableCaching
public class OptimizedApplication {

    public static void main(String[] args) {
        SpringApplication.run(OptimizedApplication.class, args);
    }

    @Bean
    CommandLineRunner seedData(UserRepository userRepo, ProductRepository productRepo, OrderRepository orderRepo) {
        return args -> {
            if (userRepo.count() > 0) return;
            // Same seed data as baseline -- identical data for fair comparison
            var user = new User("Alice Johnson", "alice@example.com");
            user.setLoyaltyPoints(1500);
            user = userRepo.save(user);

            var user2 = new User("Bob Smith", "bob@example.com");
            user2.setLoyaltyPoints(750);
            userRepo.save(user2);

            var product1 = new Product("Widget A", "A high-quality widget", new BigDecimal("29.99"), 100);
            var product2 = new Product("Gadget B", "A fancy gadget", new BigDecimal("49.99"), 50);
            var product3 = new Product("Doohickey C", "An essential doohickey", new BigDecimal("19.99"), 200);
            product1 = productRepo.save(product1);
            product2 = productRepo.save(product2);
            product3 = productRepo.save(product3);

            var order = new Order();
            order.setUser(user);
            order.setTotalAmount(new BigDecimal("79.98"));
            order.setStatus("COMPLETED");
            order = orderRepo.save(order);

            var item1 = new OrderItem();
            item1.setOrder(order);
            item1.setProduct(product1);
            item1.setQuantity(2);
            item1.setPrice(product1.getPrice());
            order.getItems().add(item1);
            var item2 = new OrderItem();
            item2.setOrder(order);
            item2.setProduct(product2);
            item2.setQuantity(1);
            item2.setPrice(product2.getPrice());
            order.getItems().add(item2);
            orderRepo.save(order);
        };
    }
}
