package com.hogwai.nosql.cassandra;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.cassandra.CassandraContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Testcontainers
class CassandraApplicationTest {

    @Container
    static CassandraContainer cassandraContainer = new CassandraContainer("cassandra:5.0")
            .withExposedPorts(9042);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.cassandra.contact-points", cassandraContainer::getHost);
        registry.add("spring.data.cassandra.port", () -> cassandraContainer.getMappedPort(9042));
        registry.add("spring.data.cassandra.local-datacenter", () -> "datacenter1");
    }

    @Test
    void contextLoads() {
        assertTrue(true);
    }
}
