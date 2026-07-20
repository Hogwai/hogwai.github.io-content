package com.hogwai.jpaprojections.repository;

import com.hogwai.jpaprojections.entity.Movie;
import com.hogwai.jpaprojections.projection.GenreStat;
import com.hogwai.jpaprojections.projection.MovieTitleDto;
import com.hogwai.jpaprojections.projection.MovieTitleView;
import com.hogwai.jpaprojections.projection.MovieWithActorsView;
import com.hogwai.jpaprojections.projection.MovieWithLabelView;
import com.hogwai.jpaprojections.projection.MovieTitleGenreDto;
import com.hogwai.jpaprojections.projection.GenreStatDto;
import jakarta.persistence.Tuple;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class MovieRepositoryTest {

    @Autowired
    private MovieRepository movieRepository;

    @Test
    void findByGenre_returnsInterfaceClosedProjection() {
        List<MovieTitleView> results = movieRepository.findByGenre("Sci-Fi");

        assertThat(results).isNotEmpty();
        assertThat(results.getFirst().getId()).isNotNull();
        assertThat(results.getFirst().getTitle()).isNotBlank();
        assertThat(results.getFirst().getGenre()).isPresent().hasValueSatisfying(
                genre -> assertThat(genre).isEqualTo("Sci-Fi"));
    }

    @Test
    void findByGenrePaged_returnsPageWithExpectedContent() {
        Page<MovieTitleView> page = movieRepository.findByGenre("Sci-Fi", PageRequest.of(0, 2));

        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getTotalElements()).isEqualTo(3);
        assertThat(page.getTotalPages()).isEqualTo(2);
    }

    @Test
    void findByGenreWithActors_returnsNestedProjection() {
        List<MovieWithActorsView> results = movieRepository.findByGenreWithActors("Sci-Fi");

        assertThat(results).isNotEmpty();
        assertThat(results.getFirst().getActors()).isNotEmpty();
        MovieWithActorsView.ActorView firstActor = results.getFirst().getActors().iterator().next();
        assertThat(firstActor.getId()).isNotNull();
        assertThat(firstActor.getFirstName()).isNotBlank();
    }

    @Test
    void findByGenreIgnoreCase_withEntityGraph_returnsNestedProjection() {
        List<MovieWithActorsView> results = movieRepository.findByGenreIgnoreCase("sci-fi");

        assertThat(results).isNotEmpty();
        assertThat(results.getFirst().getActors()).isNotEmpty();
    }

    @Test
    void nPlusOne_returnsMoviesWithActorsButTriggersExtraQueries() {
        List<MovieWithActorsView> results = movieRepository.findByGenreWithActorsNPlusOne("Sci-Fi");

        assertThat(results).isNotEmpty();
        assertThat(results.getFirst().getActors()).isNotEmpty();
    }

    @Test
    void findMoviesReleasedAfter_returnsRecordDtos() {
        List<MovieTitleDto> results = movieRepository.findMoviesReleasedAfter(2000);

        assertThat(results).isNotEmpty();
        assertThat(results.getFirst().id()).isNotNull();
        assertThat(results.getFirst().title()).isNotBlank();
        assertThat(results).allMatch(dto -> dto.title() != null);
    }

    @Test
    void findByTitleContaining_returnsRecordDtos() {
        List<MovieTitleDto> results = movieRepository.findByTitleContaining("The");

        assertThat(results).isNotEmpty();
        assertThat(results.getFirst().id()).isNotNull();
    }

    @Test
    void countByGenre_returnsAggregatedStats() {
        List<GenreStat> stats = movieRepository.countByGenre();

        assertThat(stats).isNotEmpty();
        GenreStat sciFi = stats.stream()
                .filter(s -> s.genre().equals("Sci-Fi"))
                .findFirst().orElseThrow();
        assertThat(sciFi.movieCount()).isEqualTo(3);
    }

    @Test
    void countByGenreDto_usesPersistenceCreatorConstructor() {
        List<GenreStatDto> stats = movieRepository.countByGenreDto();

        assertThat(stats).isNotEmpty();
        GenreStatDto sciFi = stats.stream()
                .filter(s -> s.getGenre().equals("Sci-Fi"))
                .findFirst().orElseThrow();
        assertThat(sciFi.getMovieCount()).isEqualTo(3);
    }

    @Test
    void findByGenreNative_returnsInterfaceProjection() {
        List<MovieTitleView> results = movieRepository.findByGenreNative("Sci-Fi");

        assertThat(results).isNotEmpty();
        assertThat(results.getFirst().getId()).isNotNull();
        assertThat(results.getFirst().getTitle()).isNotBlank();
    }

    @Test
    void findByGenreNativeDto_returnsRecordDtos() {
        List<MovieTitleDto> results = movieRepository.findByGenreNativeDto("Sci-Fi");

        assertThat(results).isNotEmpty();
        assertThat(results.getFirst().id()).isNotNull();
        assertThat(results.getFirst().title()).isNotBlank();
    }

    @Test
    void findTupleByGenre_returnsTuples() {
        List<Tuple> results = movieRepository.findTupleByGenre("Sci-Fi");

        assertThat(results).isNotEmpty();
        assertThat(results.getFirst().get("id", Long.class)).isNotNull();
        assertThat(results.getFirst().get("title", String.class)).isNotBlank();
        assertThat(results.getFirst().get("genre", String.class)).isEqualTo("Sci-Fi");
    }

    @Test
    void dynamicProjection_returnsInterfaceWhenRequested() {
        List<MovieTitleView> results = movieRepository.findByGenre("Drama", MovieTitleView.class);

        assertThat(results).isNotEmpty();
        assertThat(results.getFirst().getTitle()).isNotBlank();
    }

    @Test
    void dynamicProjection_returnsFullEntityWhenRequested() {
        List<Movie> results = movieRepository.findByGenre("Drama", Movie.class);

        assertThat(results).isNotEmpty();
        assertThat(results.getFirst().getReleaseYear()).isPositive();
        assertThat(results.getFirst().getGenre()).isEqualTo("Drama");
    }

    @Test
    void dynamicProjection_returnsRecordDtoWhenRequested() {
        List<MovieTitleDto> results = movieRepository.findByGenre("Crime", MovieTitleDto.class);

        assertThat(results).isNotEmpty();
        assertThat(results.getFirst().title()).isNotBlank();
    }

    @Test
    void findWithLabelByGenre_returnsOpenProjectionWithComputedLabel() {
        List<MovieWithLabelView> results = movieRepository.findWithLabelByGenre("Sci-Fi");

        assertThat(results).isNotEmpty();
        assertThat(results.getFirst().getTitle()).isNotBlank();
        assertThat(results.getFirst().getGenreLabel()).contains("[");
        assertThat(results.getFirst().getGenreLabel()).endsWith("]");
    }

    @Test
    void findTitleAndGenreByGenre_returnsMultiSelectRewrittenQuery() {
        List<MovieTitleGenreDto> results = movieRepository.findTitleAndGenreByGenre("Sci-Fi");

        assertThat(results).isNotEmpty();
        assertThat(results.getFirst().title()).isNotBlank();
        assertThat(results.getFirst().genre()).isEqualTo("Sci-Fi");
    }
}
