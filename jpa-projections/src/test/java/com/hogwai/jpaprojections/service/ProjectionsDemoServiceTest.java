package com.hogwai.jpaprojections.service;

import com.hogwai.jpaprojections.projection.ActorWithMoviesDto;
import com.hogwai.jpaprojections.projection.MovieDetailDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class ProjectionsDemoServiceTest {

    @Autowired
    private ProjectionsDemoService service;

    @Test
    void getMovieDetail_returnsDtoWithActors() {
        MovieDetailDto dto = service.getMovieDetail(1L);

        assertThat(dto).isNotNull();
        assertThat(dto.title()).isEqualTo("The Shawshank Redemption");
        assertThat(dto.actors()).isNotEmpty();
        assertThat(dto.actors()).hasSize(2);
        assertThat(dto.actors().getFirst().firstName()).isNotBlank();
    }

    @Test
    void getMovieDetail_returnsNullForMissingMovie() {
        MovieDetailDto dto = service.getMovieDetail(999L);

        assertThat(dto).isNull();
    }

    @Test
    void getActorWithMovies_returnsDtoWithMovies() {
        ActorWithMoviesDto dto = service.getActorWithMovies(2L);

        assertThat(dto).isNotNull();
        assertThat(dto.firstName()).isEqualTo("Morgan");
        assertThat(dto.movies()).isNotEmpty();
        assertThat(dto.movies().getFirst().title()).contains("Shawshank");
    }

    @Test
    void getActorWithMovies_returnsNullForMissingActor() {
        ActorWithMoviesDto dto = service.getActorWithMovies(999L);

        assertThat(dto).isNull();
    }
}
