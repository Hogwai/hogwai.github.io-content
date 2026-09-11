package com.hogwai.nosql.cassandra.repository;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.cassandra.CassandraContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import com.hogwai.nosql.cassandra.model.Movie;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
class MovieRepositoryTest {

    @Container
    static CassandraContainer cassandraContainer = new CassandraContainer("cassandra:5.0")
            .withExposedPorts(9042)
            .withInitScript("schema.cql");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.cassandra.contact-points", cassandraContainer::getHost);
        registry.add("spring.data.cassandra.port", () -> cassandraContainer.getMappedPort(9042));
        registry.add("spring.data.cassandra.local-datacenter", () -> "datacenter1");
    }

    @Autowired
    private MovieRepository movieRepository;

    @BeforeEach
    void setUp() {
        movieRepository.deleteAll();

        movieRepository.save(new Movie("m1", "Goodfellas", 1990, "Crime", "a1,a2"));
        movieRepository.save(new Movie("m2", "The Dark Knight", 2008, "Action", "a3,a4"));
    }

    @Test
    void findByGenreProjected_returnsOnlySelectedColumns() {
        List<Movie> results = movieRepository.findByGenreProjected("Crime");

        assertThat(results).hasSize(1);
        assertThat(results.getFirst().getTitle()).isEqualTo("Goodfellas");
        assertThat(results.getFirst().getGenre()).isEqualTo("Crime");
        assertThat(results.getFirst().getId()).isNotNull();
    }

    @Test
    void searchByTitle_returnsMatchingMovies() {
        List<Movie> results = movieRepository.searchByTitle("Goodfellas");

        assertThat(results).hasSize(1);
        assertThat(results.getFirst().getTitle()).isEqualTo("Goodfellas");
    }
}
