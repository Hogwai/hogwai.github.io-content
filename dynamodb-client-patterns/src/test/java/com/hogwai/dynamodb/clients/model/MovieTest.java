package com.hogwai.dynamodb.clients.model;

import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class MovieTest {

    @Test
    void testToItemMapAndBack() {
        Movie original = new Movie(
                "Sci-Fi",
                "movie-001",
                "Interstellar",
                "Christopher Nolan",
                "A team of astronauts travel through a wormhole.",
                Set.of("Matthew McConaughey", "Anne Hathaway", "Jessica Chastain"),
                2014,
                4.5,
                null,
                null
        );

        Map<String, AttributeValue> item = original.toItemMap();

        Movie restored = Movie.fromItem(item);

        assertEquals(original, restored);
        assertEquals(original.getGenre(), restored.getGenre());
        assertEquals(original.getMovieId(), restored.getMovieId());
        assertEquals(original.getTitle(), restored.getTitle());
        assertEquals(original.getAuthor(), restored.getAuthor());
        assertEquals(original.getDescription(), restored.getDescription());
        assertEquals(original.getActors(), restored.getActors());
        assertEquals(original.getReleaseYear(), restored.getReleaseYear());
        assertEquals(original.getRating(), restored.getRating());
    }

    @Test
    void testNullSafety() {
        Movie movie = new Movie(null, null, null, null, null, null, null, null, null, null);

        // Should not throw
        Map<String, AttributeValue> item = movie.toItemMap();

        assertEquals("", item.get("genre").s());
        assertEquals("", item.get("movieId").s());
        assertEquals("", item.get("title").s());
        assertEquals("", item.get("author").s());
        assertEquals("", item.get("description").s());
        assertTrue(item.get("actors").ss().isEmpty());
        assertEquals("0", item.get("releaseYear").n());
        assertEquals("0.0", item.get("rating").n());
    }

    @Test
    void testFromItemNullSafety() {
        // Empty map
        Map<String, AttributeValue> emptyMap = Map.of();
        Movie movie = Movie.fromItem(emptyMap);

        assertNotNull(movie);
        assertEquals("", movie.getGenre());
        assertEquals("", movie.getMovieId());
        assertEquals("", movie.getTitle());
        assertEquals("", movie.getAuthor());
        assertEquals("", movie.getDescription());
        assertTrue(movie.getActors().isEmpty());
        assertEquals(0, movie.getReleaseYear());
        assertEquals(0.0, movie.getRating());

        // Missing keys map (same behavior via getOrDefault)
        Map<String, AttributeValue> partialMap = Map.of(
                "genre", AttributeValue.fromS("Action")
        );
        Movie partial = Movie.fromItem(partialMap);
        assertEquals("Action", partial.getGenre());
        assertEquals("", partial.getMovieId());
    }
}
