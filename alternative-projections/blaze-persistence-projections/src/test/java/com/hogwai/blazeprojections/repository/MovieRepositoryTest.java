package com.hogwai.blazeprojections.repository;

import com.hogwai.blazeprojections.dto.GenreStatDto;
import com.hogwai.blazeprojections.view.MovieTitleView;
import com.hogwai.blazeprojections.view.MovieWithActorsView;
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
    private MovieRepository repository;

    @Test
    void shouldFindTitleById() {
        MovieTitleView movie = repository.findTitleById(1L);
        assertThat(movie).isNotNull();
        assertThat(movie.getTitle()).isEqualTo("The Matrix");
        assertThat(movie.getReleaseYear()).isEqualTo(1999);
        assertThat(movie.getGenre()).isEqualTo("Sci-Fi");
    }

    @Test
    void shouldFindWithActorsById() {
        MovieWithActorsView movie = repository.findWithActorsById(1L);
        assertThat(movie).isNotNull();
        assertThat(movie.getActors()).hasSize(3);
    }

    @Test
    void shouldFindByGenre() {
        List<MovieTitleView> movies = repository.findByGenreAndYear("Crime", null);
        assertThat(movies).hasSize(2);
    }

    @Test
    void shouldFindGenreStats() {
        List<GenreStatDto> stats = repository.findGenreStats();
        assertThat(stats)
                .isNotEmpty()
                .anyMatch(s -> s.genre().equals("Sci-Fi") && s.movieCount() == 2);
    }
}
