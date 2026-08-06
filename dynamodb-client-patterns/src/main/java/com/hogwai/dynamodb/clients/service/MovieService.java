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
    public static final String HAS_ACTORS = "hasActors";
    public static final String METHOD = "method";
    public static final String ACTORS = "actors";

    private final MovieEnhancedRepository enhanced;
    private final MovieRawRepository raw;

    public MovieService(MovieEnhancedRepository enhanced, MovieRawRepository raw) {
        this.enhanced = enhanced;
        this.raw = raw;
    }


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


    public List<Movie> projectionBad(String genre) {
        var movies = enhanced.queryByGenre(genre);
        long bytes = movies.stream()
                .mapToLong(m -> Movie.estimateItemBytes(m.toItemMap()))
                .sum();
        log.info("{} Projection bad (full item): {} items, ~{} bytes transferred",
                METRIC_PREFIX, movies.size(), bytes);
        return movies;
    }

    /**
     * Query movies with a projection expression.
     * <p>
     * Projection reduces the amount of data transferred over the wire and the memory
     * used for deserialisation. It does <em>not</em> reduce RCU — DynamoDB bills reads
     * on the full item size before projection (4 KB increments per read capacity unit).
     * The byte estimate returned in the log reflects transferred bytes only.
     */
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


    public List<Movie> paginationBad(String genre) {
        var movies = enhanced.queryByGenre(genre);
        log.info("{} Pagination bad (unbounded): {} items all in memory at once",
                METRIC_PREFIX, movies.size());
        return movies;
    }

    /**
     * Query a page of items by page number.
     * <p>
     * Page-number pagination on DynamoDB requires iterating through (page-1) cursors,
     * which makes O(N) round-trips for page N. This is inherent to cursor-based APIs.
     * In production, prefer cursor-based pagination ({@code lastEvaluatedKey}) over
     * page numbers for O(1) round-trips.
     */
    public List<Movie> paginationGood(String genre, int page, int size) {
        Map<String, AttributeValue> lastKey = null;
        for (int i = 1; i < page; i++) {
            var result = enhanced.queryByGenrePage(genre, size, lastKey);
            lastKey = result.lastEvaluatedKey();
            if (lastKey == null || lastKey.isEmpty()) {
                return List.of();
            }
        }
        var result = enhanced.queryByGenrePage(genre, size, lastKey);
        log.info("{} Pagination good (page={}, size={}): {} items, hasMore={}",
                METRIC_PREFIX, page, size, result.items().size(),
                result.lastEvaluatedKey() != null && !result.lastEvaluatedKey().isEmpty());
        return result.items();
    }


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


    public boolean lockingBad(String genre, String movieId, String newTitle) {
        var movie = new Movie();
        movie.setGenre(genre);
        movie.setMovieId(movieId);
        movie.setTitle(newTitle);
        try {
            raw.updateUnconditional(movie);
            log.info("{} Locking bad (unconditional): lost update risk, concurrent writes may overwrite each other",
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


    /**
     * Check whether a movie has actors by loading the <em>entire</em> entity.
     * <p>
     * <b>Anti-pattern:</b> Every attribute of the item is deserialised into a
     * {@link Movie} object, only to inspect a single field. This wastes memory
     * and bandwidth. Prefer {@link #hasActorsGood(String, String)} for a
     * projection-based alternative.
     */
    public Map<String, Object> hasActorsBad(String genre, String movieId) {
        Movie movie = enhanced.getItem(genre, movieId);
        if (movie == null) {
            log.info("{} Load entity bad: no item found for {} / {}", METRIC_PREFIX, genre, movieId);
            return Map.of(HAS_ACTORS, false, METHOD, "full entity (enhanced getItem)", "movie", null);
        }
        long itemBytes = Movie.estimateItemBytes(movie.toItemMap());
        boolean result = movie.getActors() != null && !movie.getActors().isEmpty();
        log.info("{} Load entity bad (full getItem): {} bytes loaded to check actors flag={}",
                METRIC_PREFIX, itemBytes, result);
        return Map.of("movie", movie.getTitle(), HAS_ACTORS, result,
                "bytesLoaded", itemBytes, METHOD, "full entity (enhanced getItem)");
    }

    /**
     * Check whether a movie has actors using a projection expression.
     * <p>
     * Only the {@code actors} attribute is fetched and deserialised, reducing
     * network transfer and client memory compared to loading the full entity.
     */
    public Map<String, Object> hasActorsGood(String genre, String movieId) {
        var response = raw.getItemWithProjection(genre, movieId, List.of(ACTORS));
        boolean result = response.hasItem()
                && response.item().get(ACTORS) != null
                && !response.item().get(ACTORS).ss().isEmpty();
        double rcu = response.consumedCapacity() != null ? response.consumedCapacity().capacityUnits() : 0;
        log.info("{} Load entity good (projected getItem): only actors field, {} RCU, flag={}",
                METRIC_PREFIX, rcu, result);
        return Map.of(HAS_ACTORS, result, METHOD, "projected (getItem with projectionExpression)");
    }

    /**
     * Add an actor using a read-modify-write cycle on the full entity.
     * <p>
     * <b>Anti-pattern:</b> The item is loaded entirely, mutated in memory, then
     * written back unconditionally. This creates a window for lost updates when
     * two clients modify the same item concurrently, and wastes capacity by
     * rewriting every attribute even though only one changed.
     */
    public Map<String, Object> addActorBad(String genre, String movieId, String actorName) {
        Movie movie = enhanced.getItem(genre, movieId);
        long readBytes = Movie.estimateItemBytes(movie.toItemMap());

        Set<String> currentActors = new HashSet<>(movie.getActors());
        currentActors.add(actorName);
        movie.setActors(currentActors);
        movie.setVersion(movie.getVersion() != null ? movie.getVersion() + 1 : 1);

        ConsumedCapacity capacity = raw.putItem(movie);
        long writeBytes = Movie.estimateItemBytes(movie.toItemMap());
        double wcu = capacity != null ? capacity.capacityUnits() : 0;

        log.info("{} Add actor bad (RMW cycle): read {} bytes, wrote {} bytes, {} WCU — lost update risk",
                METRIC_PREFIX, readBytes, writeBytes, wcu);
        return Map.of(METHOD, "read-modify-write (enhanced getItem + putItem)",
                "readBytes", readBytes, "writeBytes", writeBytes, "wcu", wcu);
    }

    /**
     * Add an actor with an atomic update expression.
     * <p>
     * The {@code ADD actors :newActor} update expression performs a single
     * round-trip with no read-before-write, eliminating the lost-update risk
     * and reducing write amplification.
     */
    public Map<String, Object> addActorGood(String genre, String movieId, String actorName) {
        var response = raw.addActor(genre, movieId, actorName);
        double wcu = response.consumedCapacity() != null ? response.consumedCapacity().capacityUnits() : 0;
        log.info("{} Add actor good (updateExpression): 1 RT, {} WCU, atomic, no lost update risk",
                METRIC_PREFIX, wcu);
        return Map.of(METHOD, "atomic update (UpdateItem with ADD actors)",
                "wcu", wcu, "atomic", true);
    }


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
