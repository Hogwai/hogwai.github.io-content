package com.hogwai.nosql.cassandra.model;

import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;

@Table("movies")
public class Movie {
    @PrimaryKey
    private String id;
    private String title;
    private int releaseYear;
    private String genre;
    private String actorIds; // Denormalized: comma-separated actor IDs

    public Movie() {}

    public Movie(String id, String title, int releaseYear, String genre, String actorIds) {
        this.id = id;
        this.title = title;
        this.releaseYear = releaseYear;
        this.genre = genre;
        this.actorIds = actorIds;
    }

    // getters and setters for all fields
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public int getReleaseYear() { return releaseYear; }
    public void setReleaseYear(int releaseYear) { this.releaseYear = releaseYear; }
    public String getGenre() { return genre; }
    public void setGenre(String genre) { this.genre = genre; }
    public String getActorIds() { return actorIds; }
    public void setActorIds(String actorIds) { this.actorIds = actorIds; }
}
