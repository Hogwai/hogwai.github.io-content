package com.hogwai.dynamodb.clients.model;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@DynamoDbBean
public class Movie {

    private String genre;
    private String movieId;
    private String title;
    private String author;
    private String description;
    private Set<String> actors;
    private Integer releaseYear;
    private Double rating;
    private Long expireAt;
    private Integer version;

    public Movie() {
        this("", "", "", "", "", Set.of(), 0, 0.0, null, null);
    }

    public Movie(String genre, String movieId, String title, String author,
                 String description, Set<String> actors, Integer releaseYear, Double rating, Long expireAt, Integer version) {
        this.genre = genre;
        this.movieId = movieId;
        this.title = title;
        this.author = author;
        this.description = description;
        this.actors = actors;
        this.releaseYear = releaseYear;
        this.rating = rating;
        this.expireAt = expireAt;
        this.version = version;
    }

    @DynamoDbPartitionKey
    public String getGenre() { return genre; }
    public void setGenre(String genre) { this.genre = genre; }

    @DynamoDbSortKey
    public String getMovieId() { return movieId; }
    public void setMovieId(String movieId) { this.movieId = movieId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Set<String> getActors() { return actors; }
    public void setActors(Set<String> actors) { this.actors = actors; }

    public Integer getReleaseYear() { return releaseYear; }
    public void setReleaseYear(Integer releaseYear) { this.releaseYear = releaseYear; }

    public Double getRating() { return rating; }
    public void setRating(Double rating) { this.rating = rating; }

    public Long getExpireAt() { return expireAt; }
    public void setExpireAt(Long expireAt) { this.expireAt = expireAt; }

    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }

    public Map<String, AttributeValue> toItemMap() {
        Map<String, AttributeValue> item = new HashMap<>();
        item.put("genre", AttributeValue.fromS(genre != null ? genre : ""));
        item.put("movieId", AttributeValue.fromS(movieId != null ? movieId : ""));
        item.put("title", AttributeValue.fromS(title != null ? title : ""));
        item.put("author", AttributeValue.fromS(author != null ? author : ""));
        item.put("description", AttributeValue.fromS(description != null ? description : ""));
        Set<String> safeActors = actors != null ? actors : Set.of();
        item.put("actors", AttributeValue.fromSs(new ArrayList<>(safeActors)));
        item.put("releaseYear", AttributeValue.fromN(releaseYear != null ? String.valueOf(releaseYear) : "0"));
        item.put("rating", AttributeValue.fromN(rating != null ? String.valueOf(rating) : "0.0"));
        if (expireAt != null) {
            item.put("expireAt", AttributeValue.fromN(String.valueOf(expireAt)));
        }
        if (version != null) {
            item.put("version", AttributeValue.fromN(String.valueOf(version)));
        }
        return item;
    }

    public static Movie fromItem(Map<String, AttributeValue> item) {
        Movie movie = new Movie();
        movie.setGenre(item.getOrDefault("genre", AttributeValue.fromS("")).s());
        movie.setMovieId(item.getOrDefault("movieId", AttributeValue.fromS("")).s());
        movie.setTitle(item.getOrDefault("title", AttributeValue.fromS("")).s());
        movie.setAuthor(item.getOrDefault("author", AttributeValue.fromS("")).s());
        movie.setDescription(item.getOrDefault("description", AttributeValue.fromS("")).s());

        AttributeValue actorsAv = item.get("actors");
        if (actorsAv != null && actorsAv.ss() != null) {
            movie.setActors(new HashSet<>(actorsAv.ss()));
        } else {
            movie.setActors(Set.of());
        }

        AttributeValue yearAv = item.get("releaseYear");
        if (yearAv != null && yearAv.n() != null && !yearAv.n().isEmpty()) {
            movie.setReleaseYear(Integer.parseInt(yearAv.n()));
        } else {
            movie.setReleaseYear(0);
        }

        AttributeValue ratingAv = item.get("rating");
        if (ratingAv != null && ratingAv.n() != null && !ratingAv.n().isEmpty()) {
            movie.setRating(Double.parseDouble(ratingAv.n()));
        } else {
            movie.setRating(0.0);
        }

        AttributeValue expAv = item.get("expireAt");
        if (expAv != null && expAv.n() != null && !expAv.n().isEmpty()) {
            movie.setExpireAt(Long.parseLong(expAv.n()));
        } else {
            movie.setExpireAt(null);
        }

        AttributeValue verAv = item.get("version");
        if (verAv != null && verAv.n() != null && !verAv.n().isEmpty()) {
            movie.setVersion(Integer.parseInt(verAv.n()));
        } else {
            movie.setVersion(null);
        }

        return movie;
    }

    /**
     * Estimate the byte size of a DynamoDB item (attribute names + values).
     */
    public static long estimateItemBytes(Map<String, AttributeValue> item) {
        long bytes = 0;
        for (var entry : item.entrySet()) {
            bytes += entry.getKey().length();
            var av = entry.getValue();
            if (av.s() != null) bytes += av.s().length();
            if (av.n() != null) bytes += av.n().length();
            if (av.ss() != null) {
                for (String s : av.ss()) bytes += s.length();
            }
        }
        return bytes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Movie movie = (Movie) o;
        return Objects.equals(genre, movie.genre)
                && Objects.equals(movieId, movie.movieId)
                && Objects.equals(title, movie.title)
                && Objects.equals(author, movie.author)
                && Objects.equals(description, movie.description)
                && Objects.equals(actors, movie.actors)
                && Objects.equals(releaseYear, movie.releaseYear)
                && Objects.equals(rating, movie.rating)
                && Objects.equals(expireAt, movie.expireAt)
                && Objects.equals(version, movie.version);
    }

    @Override
    public int hashCode() {
        return Objects.hash(genre, movieId, title, author, description, actors, releaseYear, rating, expireAt, version);
    }

    @Override
    public String toString() {
        return "Movie{" +
                "genre='" + genre + '\'' +
                ", movieId='" + movieId + '\'' +
                ", title='" + title + '\'' +
                ", author='" + author + '\'' +
                ", description='" + description + '\'' +
                ", actors=" + actors +
                ", releaseYear=" + releaseYear +
                ", rating=" + rating +
                ", expireAt=" + expireAt +
                ", version=" + version +
                '}';
    }
}
