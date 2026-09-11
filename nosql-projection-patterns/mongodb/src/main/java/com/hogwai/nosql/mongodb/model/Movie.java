package com.hogwai.nosql.mongodb.model;

import java.util.List;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document
public class Movie {
    @Id
    private String id;
    private String title;
    private int releaseYear;
    @Indexed
    private String genre;
    private List<Actor> actors;

    public Movie() {}

    public Movie(String id, String title, int releaseYear, String genre, List<Actor> actors) {
        this.id = id;
        this.title = title;
        this.releaseYear = releaseYear;
        this.genre = genre;
        this.actors = actors;
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
    public List<Actor> getActors() { return actors; }
    public void setActors(List<Actor> actors) { this.actors = actors; }
}
