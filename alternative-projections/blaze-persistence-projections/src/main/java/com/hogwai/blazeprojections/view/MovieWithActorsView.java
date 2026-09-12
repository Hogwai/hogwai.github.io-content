package com.hogwai.blazeprojections.view;

import com.blazebit.persistence.view.EntityView;
import com.blazebit.persistence.view.IdMapping;
import com.hogwai.blazeprojections.entity.Actor;
import com.hogwai.blazeprojections.entity.Movie;

import java.util.Set;

@EntityView(Movie.class)
public interface MovieWithActorsView {

    @IdMapping
    Long getId();

    String getTitle();

    int getReleaseYear();

    String getGenre();

    Set<ActorView> getActors();

    @EntityView(Actor.class)
    interface ActorView {

        @IdMapping
        Long getId();

        String getFirstName();

        String getLastName();
    }
}
