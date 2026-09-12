package com.hogwai.jdbiprojections.repository;

import com.hogwai.jdbiprojections.dto.GenreStatDto;
import com.hogwai.jdbiprojections.dto.MovieTitleDto;
import com.hogwai.jdbiprojections.dto.MovieWithActorsDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class MovieRepositoryTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:18-alpine")
            .withDatabaseName("test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private MovieFluentRepository repository;

    @Test
    void shouldFindTitleById() {
        MovieTitleDto movie = repository.findTitleById(1L);
        assertThat(movie).isNotNull();
        assertThat(movie.title()).isEqualTo("The Matrix");
        assertThat(movie.releaseYear()).isEqualTo(1999);
        assertThat(movie.genre()).isEqualTo("Sci-Fi");
    }

    @Test
    void shouldFindByGenre() {
        List<MovieTitleDto> movies = repository.findByGenreAndYear("Crime", null);
        assertThat(movies).hasSize(2);
    }

    @Test
    void shouldFindGenreStats() {
        List<GenreStatDto> stats = repository.findGenreStats();
        assertThat(stats)
                .isNotEmpty()
                .anyMatch(s -> s.genre().equals("Sci-Fi") && s.movieCount() == 2);
    }

    @Test
    void shouldFindWithActorsById() {
        MovieWithActorsDto movie = repository.findWithActorsById(1L);
        assertThat(movie).isNotNull();
        assertThat(movie.actors()).hasSize(3);
    }
}
