package com.hogwai.jdbiprojections.repository;

import com.hogwai.jdbiprojections.dto.GenreStatDto;
import com.hogwai.jdbiprojections.dto.MovieTitleDto;
import org.jdbi.v3.sqlobject.config.RegisterConstructorMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.spring.JdbiRepository;

import java.util.List;

@JdbiRepository
@RegisterConstructorMapper(MovieTitleDto.class)
public interface MovieSqlObject {

    @SqlQuery("SELECT id, title, release_year, genre FROM movies WHERE id = :id")
    MovieTitleDto findTitleById(@Bind("id") Long id);

    @SqlQuery("SELECT id, title, release_year, genre FROM movies WHERE (:genre IS NULL OR genre = :genre) AND (:year IS NULL OR release_year = :year)")
    List<MovieTitleDto> findByGenreAndYear(@Bind("genre") String genre, @Bind("year") Integer year);

    @SqlQuery("SELECT genre, COUNT(*) as movie_count FROM movies GROUP BY genre")
    @RegisterConstructorMapper(GenreStatDto.class)
    List<GenreStatDto> findGenreStats();
}
