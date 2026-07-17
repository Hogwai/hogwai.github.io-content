package com.hogwai.jpaprojections.projection;

/**
 * Interface Closed Projection: limits SELECT to id and title only.
 * Spring Data optimizes the query to SELECT m.id, m.title FROM movie m.
 * No proxy overhead for fields not declared here.
 */
public interface MovieTitleView {
    Long getId();
    String getTitle();
}
