package com.hogwai.dynamodb.clients.service;

import com.hogwai.dynamodb.clients.model.Movie;
import com.hogwai.dynamodb.clients.repository.MovieEnhancedRepository;
import com.hogwai.dynamodb.clients.repository.MovieRawRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;
import software.amazon.awssdk.services.dynamodb.model.ConsumedCapacity;

import java.util.*;

@Service
public class MovieService {

    private static final Logger log = LoggerFactory.getLogger(MovieService.class);
    private static final String METRIC_PREFIX = "[METRIC]";
    public static final String BATCH_TEST = "batch-test";

    private final MovieEnhancedRepository enhanced;
    private final MovieRawRepository raw;

    public MovieService(MovieEnhancedRepository enhanced, MovieRawRepository raw) {
        this.enhanced = enhanced;
        this.raw = raw;
    }

    // ==================== 1. COUNT ====================

    public int countBad(String genre) {
        var response = raw.query(genre);
        int count = response.items().size();
        double rcu = response.consumedCapacity() != null ? response.consumedCapacity().capacityUnits() : 0;
        log.info("{} Count bad (query + .size()): {} items transferred, {} RCU",
                METRIC_PREFIX, count, rcu);
        return count;
    }

    public int countGood(String genre) {
        var response = raw.queryWithSelectCount(genre);
        int count = response.count();
        double rcu = response.consumedCapacity() != null ? response.consumedCapacity().capacityUnits() : 0;
        log.info("{} Count good (Select.COUNT): {} items returned, {} RCU",
                METRIC_PREFIX, count, rcu);
        return count;
    }

    // ==================== 2. CONDITION ====================

    public Movie conditionBad(String genre, String movieId) {
        var getResponse = raw.getItem(genre, movieId);
        double getRcu = getResponse.consumedCapacity() != null
                ? getResponse.consumedCapacity().capacityUnits() : 0;

        if (!getResponse.hasItem()) {
            var movie = createDummyMovie(genre, movieId);
            raw.putItem(movie);
            log.info("{} Condition bad (getItem + putItem): 2 RTs, {} RCU + 1 WCU, race condition window",
                    METRIC_PREFIX, getRcu);
            return movie;
        }
        return null;
    }

    public Movie conditionGood(Movie movie) {
        try {
            var capacity = raw.putItemWithCondition(movie,
                    "attribute_not_exists(movieId)",
                    Map.of());
            double wcu = capacity != null ? capacity.capacityUnits() : 0;
            log.info("{} Condition good (ConditionExpression): 1 RT, {} WCU, atomic",
                    METRIC_PREFIX, wcu);
            return movie;
        } catch (ConditionalCheckFailedException _) {
            log.info("{} Condition good: condition failed (item already exists)", METRIC_PREFIX);
            return null;
        }
    }

    // ==================== 3. BATCH ====================

    public int batchWriteBad(int count) {
        var movies = generateMovies(count);
        int rts = raw.putItemsIndividual(movies);
        log.info("{} Batch write bad ({} putItem calls): {} RTs",
                METRIC_PREFIX, count, rts);
        return rts;
    }

    public int batchWriteGood(int count) {
        var movies = generateMovies(count);
        var response = raw.batchPut(movies);
        var capacities = response.consumedCapacity();
        double wcu = (capacities != null && !capacities.isEmpty())
                ? capacities.stream().mapToDouble(ConsumedCapacity::capacityUnits).sum()
                : 0;
        log.info("{} Batch write good (BatchWriteItem): 1 RT, {} WCU, {} items",
                METRIC_PREFIX, wcu, count);
        return count;
    }

    public int batchReadBad(List<String> movieIds) {
        var keys = movieIds.stream()
                .map(id -> Map.of(
                        "genre", AttributeValue.fromS(BATCH_TEST),
                        "movieId", AttributeValue.fromS(id)))
                .toList();
        raw.getItemsIndividual(keys);
        log.info("{} Batch read bad ({} getItem calls): {} RTs",
                METRIC_PREFIX, movieIds.size(), movieIds.size());
        return movieIds.size();
    }

    public int batchReadGood(List<String> movieIds) {
        var keys = movieIds.stream()
                .map(id -> Map.of(
                        "genre", AttributeValue.fromS(BATCH_TEST),
                        "movieId", AttributeValue.fromS(id)))
                .toList();
        raw.batchGet(keys);
        log.info("{} Batch read good (BatchGetItem): 1 RT", METRIC_PREFIX);
        return movieIds.size();
    }

    // ==================== 4. PROJECTION ====================

    public List<Movie> projectionBad(String genre) {
        var movies = enhanced.queryByGenre(genre);
        long bytes = movies.stream()
                .mapToLong(m -> Movie.estimateItemBytes(m.toItemMap()))
                .sum();
        log.info("{} Projection bad (full item): {} items, ~{} bytes transferred",
                METRIC_PREFIX, movies.size(), bytes);
        return movies;
    }

    public List<Movie> projectionGood(String genre, List<String> fields) {
        var movies = enhanced.queryByGenreWithProjection(genre, fields);
        long bytes = movies.stream()
                .mapToLong(m -> {
                    var projectedItem = new java.util.HashMap<String, AttributeValue>();
                    java.util.Map<String, AttributeValue> fullItem = m.toItemMap();
                    for (String field : fields) {
                        if (fullItem.containsKey(field)) {
                            projectedItem.put(field, fullItem.get(field));
                        }
                    }
                    return Movie.estimateItemBytes(projectedItem);
                })
                .sum();
        log.info("{} Projection good (ProjectionExpression): {} items, ~{} bytes transferred (requested fields: {})",
                METRIC_PREFIX, movies.size(), bytes, fields);
        return movies;
    }

    // ==================== 5. GSI vs FILTER ====================

    public List<Movie> gsiVsFilterBad(String genre, String author) {
        var response = raw.queryWithFilter(genre,
                "author = :author",
                Map.of(":author", AttributeValue.fromS(author)));
        int scanned = response.scannedCount();
        int matched = response.count();
        double rcu = response.consumedCapacity() != null ? response.consumedCapacity().capacityUnits() : 0;
        log.info("{} GSI vs Filter bad (FilterExpression): scanned {} items, matched {} in partition, {} RCU",
                METRIC_PREFIX, scanned, matched, rcu);
        return response.items().stream()
                .map(Movie::fromItem)
                .toList();
    }

    public List<Movie> gsiVsFilterGood(String author) {
        var response = raw.queryByAuthor(author);
        int matched = response.items().size();
        double rcu = response.consumedCapacity() != null ? response.consumedCapacity().capacityUnits() : 0;
        log.info("{} GSI vs Filter good (GSI query): only {} matching items, {} RCU",
                METRIC_PREFIX, matched, rcu);
        return response.items().stream()
                .map(Movie::fromItem)
                .toList();
    }

    // ==================== 6. PAGINATION ====================

    public List<Movie> paginationBad(String genre) {
        var movies = enhanced.queryByGenre(genre);
        log.info("{} Pagination bad (unbounded): {} items all in memory at once",
                METRIC_PREFIX, movies.size());
        return movies;
    }

    public List<Movie> paginationGood(String genre, int page, int size) {
        // Note: page-number pagination on DynamoDB requires iterating through (page-1) cursors,
        // which does O(N) round-trips for page N. This is inherent to cursor-based APIs.
        // In production, accept lastEvaluatedKey from the caller instead of a page number
        // for cursor-based pagination (O(1) round-trips).
        Map<String, AttributeValue> lastKey = null;
        for (int i = 1; i < page; i++) {
            var result = enhanced.queryByGenrePage(genre, size, lastKey);
            lastKey = result.lastEvaluatedKey();
            if (lastKey == null || lastKey.isEmpty()) {
                return List.of();  // no more pages
            }
        }
        var result = enhanced.queryByGenrePage(genre, size, lastKey);
        log.info("{} Pagination good (page={}, size={}): {} items, hasMore={}",
                METRIC_PREFIX, page, size, result.items().size(),
                result.lastEvaluatedKey() != null && !result.lastEvaluatedKey().isEmpty());
        return result.items();
    }

    // ==================== 7. SCAN vs QUERY ====================

    public List<Movie> scanBad() {
        var response = raw.scanAll();
        int count = response.items().size();
        double rcu = response.consumedCapacity() != null ? response.consumedCapacity().capacityUnits() : 0;
        log.info("{} Scan bad (full table scan): {} items across all partitions, {} RCU",
                METRIC_PREFIX, count, rcu);
        return response.items().stream()
                .map(Movie::fromItem)
                .toList();
    }

    public List<Movie> queryGood(String genre) {
        var response = raw.query(genre);
        int count = response.items().size();
        double rcu = response.consumedCapacity() != null ? response.consumedCapacity().capacityUnits() : 0;
        log.info("{} Query good (partition query): {} items, {} RCU",
                METRIC_PREFIX, count, rcu);
        return response.items().stream()
                .map(Movie::fromItem)
                .toList();
    }

    // ==================== 8. TTL (Time-to-Live) ====================

    public int ttlBad(int count) {
        var movies = generateMoviesWithoutTtl(count);
        raw.putItemsIndividual(movies);
        log.info("{} TTL bad (no TTL): {} items written, manual cleanup required", METRIC_PREFIX, count);
        return count;
    }

    public int ttlGood(int count, int ttlSeconds) {
        var movies = generateMoviesWithTtl(count, ttlSeconds);
        raw.putItemsIndividual(movies);
        log.info("{} TTL good (expireAt={}s): {} items, DynamoDB auto-cleanup", METRIC_PREFIX, ttlSeconds, count);
        return count;
    }

    // ==================== 9. OPTIMISTIC LOCKING ====================

    public boolean lockingBad(String genre, String movieId, String newTitle) {
        var movie = new Movie();
        movie.setGenre(genre);
        movie.setMovieId(movieId);
        movie.setTitle(newTitle);
        try {
            raw.updateUnconditional(movie);
            log.info("{} Locking bad (unconditional): lost update risk — concurrent writes may overwrite each other",
                    METRIC_PREFIX);
            return true;
        } catch (Exception e) {
            log.warn("{} Locking bad failed: {}", METRIC_PREFIX, e.getMessage());
            return false;
        }
    }

    public boolean lockingGood(String genre, String movieId, String newTitle, int expectedVersion) {
        var movie = new Movie();
        movie.setGenre(genre);
        movie.setMovieId(movieId);
        movie.setTitle(newTitle);
        try {
            raw.updateWithVersion(movie, expectedVersion);
            log.info("{} Locking good (version={}): update succeeded, version now {}",
                    METRIC_PREFIX, expectedVersion, expectedVersion + 1);
            return true;
        } catch (ConditionalCheckFailedException _) {
            log.info("{} Locking good: version conflict (expected={}, actual differs)", METRIC_PREFIX, expectedVersion);
            return false;
        }
    }

    // ==================== 10. TRANSACTIONS ====================

    public int transactionBad(int count) {
        var movies = generateMoviesForTransaction(count, "tx-bad-");
        var results = raw.transactWriteBad(movies);
        long succeeded = results.stream().filter(r -> r == 1).count();
        log.info("{} Transaction bad (individual puts): {}/{} items written, no atomicity",
                METRIC_PREFIX, succeeded, count);
        return (int) succeeded;
    }

    public int transactionGood(int count) {
        var movies = generateMoviesForTransaction(count, "tx-good-");
        raw.transactWriteGood(movies);
        log.info("{} Transaction good (TransactWriteItems): {} items atomically written",
                METRIC_PREFIX, count);
        return count;
    }

    // ==================== HELPERS ====================

    private Movie createDummyMovie(String genre, String movieId) {
        var movie = new Movie();
        movie.setGenre(genre);
        movie.setMovieId(movieId);
        movie.setTitle("Test Movie");
        movie.setReleaseYear(2024);
        movie.setActors(Set.of("Test Actor"));
        movie.setDescription("Test");
        movie.setRating(4.5);
        movie.setAuthor("Test Author");
        return movie;
    }

    private List<Movie> generateMovies(int count) {
        var movies = new ArrayList<Movie>(count);
        for (int i = 0; i < count; i++) {
            movies.add(createDummyMovie(BATCH_TEST, "batch-" + UUID.randomUUID()));
        }
        return movies;
    }

    private List<Movie> generateMoviesWithoutTtl(int count) {
        var movies = new ArrayList<Movie>(count);
        for (int i = 0; i < count; i++) {
            Movie movie = new Movie();
            movie.setGenre("ttl-test");
            movie.setMovieId("ttl-no-" + UUID.randomUUID());
            movie.setTitle("No TTL Movie " + i);
            movie.setAuthor("TTL Author");
            movie.setDescription("No expiry set");
            movie.setActors(Set.of("TTL Actor"));
            movie.setReleaseYear(2024);
            movie.setRating(3.0);
            movie.setExpireAt(null);
            movies.add(movie);
        }
        return movies;
    }

    private List<Movie> generateMoviesForTransaction(int count, String prefix) {
        var movies = new ArrayList<Movie>(count);
        for (int i = 0; i < count; i++) {
            Movie movie = new Movie();
            movie.setGenre("tx-test");
            movie.setMovieId(prefix + UUID.randomUUID());
            movie.setTitle("Transaction Movie " + i);
            movie.setAuthor("Transaction Author");
            movie.setDescription("Part of atomic batch");
            movie.setActors(Set.of("Tx Actor"));
            movie.setReleaseYear(2024);
            movie.setRating(3.5);
            movies.add(movie);
        }
        return movies;
    }

    private List<Movie> generateMoviesWithTtl(int count, int ttlSeconds) {
        var movies = new ArrayList<Movie>(count);
        long now = System.currentTimeMillis() / 1000;
        for (int i = 0; i < count; i++) {
            Movie movie = new Movie();
            movie.setGenre("ttl-test");
            movie.setMovieId("ttl-yes-" + UUID.randomUUID());
            movie.setTitle("TTL Movie " + i);
            movie.setAuthor("TTL Author");
            movie.setDescription("Will auto-expire");
            movie.setActors(Set.of("TTL Actor"));
            movie.setReleaseYear(2024);
            movie.setRating(3.0);
            movie.setExpireAt(now + ttlSeconds);
            movies.add(movie);
        }
        return movies;
    }
}
