package com.hogwai.jpaprojections.repository;

import com.hogwai.jpaprojections.projection.ActorNameView;
import com.hogwai.jpaprojections.projection.MovieTitleDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class ActorRepositoryTest {

    @Autowired
    private ActorRepository actorRepository;

    @Test
    void findByLastName_returnsActorNameView() {
        List<ActorNameView> results = actorRepository.findByLastName("Freeman");

        assertThat(results).isNotEmpty();
        assertThat(results.getFirst().getFirstName()).isEqualTo("Morgan");
        assertThat(results.getFirst().getFullName()).isEqualTo("Morgan Freeman");
    }

    @Test
    void findMoviesByActorId_returnsMovieTitleDtos() {
        List<MovieTitleDto> movies = actorRepository.findMoviesByActorId(2L);

        assertThat(movies).isNotEmpty();
        assertThat(movies.getFirst().title()).contains("Shawshank");
    }

    @Test
    void dynamicProjection_returnsActorNameView() {
        List<ActorNameView> results = actorRepository.findByLastName("Brando", ActorNameView.class);

        assertThat(results).isNotEmpty();
        assertThat(results.getFirst().getFirstName()).isEqualTo("Marlon");
    }
}
