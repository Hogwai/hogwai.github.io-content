package com.hogwai.blazeprojections.repository;

import com.blazebit.persistence.CriteriaBuilder;
import com.blazebit.persistence.CriteriaBuilderFactory;
import com.blazebit.persistence.view.EntityViewManager;
import com.blazebit.persistence.view.EntityViewSetting;
import com.hogwai.blazeprojections.dto.GenreStatDto;
import com.hogwai.blazeprojections.entity.Movie;
import com.hogwai.blazeprojections.view.MovieTitleView;
import com.hogwai.blazeprojections.view.MovieWithActorsView;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
@Transactional(readOnly = true)
public class MovieRepository {

    private final CriteriaBuilderFactory criteriaBuilderFactory;
    private final EntityViewManager entityViewManager;
    private final EntityManager entityManager;

    public MovieRepository(CriteriaBuilderFactory criteriaBuilderFactory,
                           EntityViewManager entityViewManager,
                           EntityManager entityManager) {
        this.criteriaBuilderFactory = criteriaBuilderFactory;
        this.entityViewManager = entityViewManager;
        this.entityManager = entityManager;
    }

    public MovieTitleView findTitleById(Long id) {
        return entityViewManager.find(entityManager, MovieTitleView.class, id);
    }

    public MovieWithActorsView findWithActorsById(Long id) {
        return entityViewManager.find(entityManager, MovieWithActorsView.class, id);
    }

    public List<MovieTitleView> findByGenreAndYear(String genre, Integer year) {
        CriteriaBuilder<Movie> criteriaBuilder = criteriaBuilderFactory.create(entityManager, Movie.class);

        if (genre != null) {
            criteriaBuilder.where("genre").eq(genre);
        }
        if (year != null) {
            criteriaBuilder.where("releaseYear").eq(year);
        }

        CriteriaBuilder<MovieTitleView> viewCriteriaBuilder = entityViewManager.applySetting(
                EntityViewSetting.create(MovieTitleView.class),
                criteriaBuilder
        );
        return viewCriteriaBuilder.getResultList();
    }

    public List<GenreStatDto> findGenreStats() {
        return entityManager.createQuery(
                """
                        SELECT new com.hogwai.blazeprojections.dto.GenreStatDto(m.genre, COUNT(m.id))
                        FROM Movie m
                        GROUP BY m.genre
                        ORDER BY m.genre
                        """,
                GenreStatDto.class
        ).getResultList();
    }
}
