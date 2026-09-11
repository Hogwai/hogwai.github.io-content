package com.hogwai.nosql.mongodb.repository;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.mongodb.test.autoconfigure.DataMongoTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.mongodb.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import com.hogwai.nosql.mongodb.model.Actor;
import com.hogwai.nosql.mongodb.model.Movie;
import com.hogwai.nosql.mongodb.projection.MovieTitleView;

import static org.assertj.core.api.Assertions.assertThat;

@DataMongoTest
@Testcontainers
class MovieRepositoryTest {

    @Container
    static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:7.0")
            .withExposedPorts(27017);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getConnectionString);
    }

    @Autowired
    private MovieRepository movieRepository;

    @BeforeEach
    void setUp() {
        movieRepository.deleteAll();

        Actor deNiro = new Actor("a1", "Robert", "De Niro");
        Actor pacino = new Actor("a2", "Al", "Pacino");
        Actor bale = new Actor("a3", "Christian", "Bale");
        Actor ledger = new Actor("a4", "Heath", "Ledger");

        movieRepository.save(new Movie("m1", "Goodfellas", 1990, "Crime", List.of(deNiro, pacino)));
        movieRepository.save(new Movie("m2", "The Dark Knight", 2008, "Action", List.of(bale, ledger)));
    }

    @Test
    void findByGenre_returnsOnlyProjectedFields() {
        List<MovieTitleView> results = movieRepository.findByGenre("Crime");

        assertThat(results).hasSize(1);
        assertThat(results.getFirst().getTitle()).isEqualTo("Goodfellas");
        assertThat(results.getFirst().getGenre()).isEqualTo("Crime");
        assertThat(results.getFirst().getId()).isNotNull();
    }

    @Test
    void findTitleAndYearByGenre_returnsTitleAndYear() {
        List<Movie> results = movieRepository.findTitleAndYearByGenre("Crime");

        assertThat(results).hasSize(1);
        assertThat(results.getFirst().getTitle()).isEqualTo("Goodfellas");
        assertThat(results.getFirst().getReleaseYear()).isEqualTo(1990);
    }

    @Test
    void findByGenreDynamic_canSwitchProjectionType() {
        List<MovieTitleView> titles = movieRepository.findByGenre("Crime", MovieTitleView.class);
        assertThat(titles).hasSize(1);
        assertThat(titles.getFirst().getTitle()).isEqualTo("Goodfellas");
    }
}
