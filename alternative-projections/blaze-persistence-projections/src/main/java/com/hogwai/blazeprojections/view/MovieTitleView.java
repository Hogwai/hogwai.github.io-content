package com.hogwai.blazeprojections.view;

import com.blazebit.persistence.view.EntityView;
import com.blazebit.persistence.view.IdMapping;
import com.hogwai.blazeprojections.entity.Movie;

@EntityView(Movie.class)
public interface MovieTitleView {

    @IdMapping
    Long getId();

    String getTitle();

    int getReleaseYear();

    String getGenre();
}
