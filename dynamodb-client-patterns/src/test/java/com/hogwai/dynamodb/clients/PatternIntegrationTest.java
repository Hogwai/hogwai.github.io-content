package com.hogwai.dynamodb.clients;

import com.hogwai.dynamodb.clients.model.Movie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.RestTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class PatternIntegrationTest {
    @Container
    static GenericContainer<?> dynamoDb = new GenericContainer<>("amazon/dynamodb-local:latest")
            .withExposedPorts(8000);

    @LocalServerPort
    private int port;

    private final RestTemplate rest = new RestTemplate();

    @Autowired
    private DynamoDbClient dynamoDbClient;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("aws.dynamodb.endpoint-override",
                () -> "http://localhost:" + dynamoDb.getMappedPort(8000));
    }

    @BeforeEach
    void setUp() {
        createMoviesTableIfNotExists();
        clearTable();
        waitForGsiActive();
    }

    private String url(String path) {
        return "http://localhost:" + port + path;
    }

    private void createMoviesTableIfNotExists() {
        try {
            dynamoDbClient.describeTable(DescribeTableRequest.builder().tableName("movies").build());
            return;
        } catch (ResourceNotFoundException _) {
            // create it
        }
        dynamoDbClient.createTable(CreateTableRequest.builder()
                .tableName("movies")
                .keySchema(
                        KeySchemaElement.builder().attributeName("genre").keyType("HASH").build(),
                        KeySchemaElement.builder().attributeName("movieId").keyType("RANGE").build())
                .attributeDefinitions(
                        AttributeDefinition.builder().attributeName("genre").attributeType("S").build(),
                        AttributeDefinition.builder().attributeName("movieId").attributeType("S").build(),
                        AttributeDefinition.builder().attributeName("author").attributeType("S").build())
                .globalSecondaryIndexes(
                        GlobalSecondaryIndex.builder()
                                .indexName("author-index")
                                .keySchema(KeySchemaElement.builder().attributeName("author").keyType("HASH").build())
                                .projection(p -> p.projectionType(ProjectionType.ALL))
                                .build())
                .billingMode("PAY_PER_REQUEST")
                .build());
    }

    // ---- Pattern 1: Count ----

    @Test
    void pattern1_count() {
        seedMovie("Action", "cnt-" + UUID.randomUUID());
        seedMovie("Action", "cnt-" + UUID.randomUUID());
        seedMovie("Action", "cnt-" + UUID.randomUUID());

        ResponseEntity<Map> bad = rest.getForEntity(url("/api/count?genre=Action&naive=true"), Map.class);
        assertThat(bad.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(bad.getBody()).containsKey("count");
        int naiveCount = (int) bad.getBody().get("count");

        ResponseEntity<Map> good = rest.getForEntity(url("/api/count?genre=Action"), Map.class);
        assertThat(good.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(good.getBody()).containsKey("count");
        int goodCount = (int) good.getBody().get("count");

        // Both should return the same count
        assertThat(naiveCount).isEqualTo(goodCount);
    }

    // ---- Pattern 2: Condition ----

    @Test
    void pattern2_condition() {
        String movieId = "cond-" + UUID.randomUUID().toString().substring(0, 8);

        // Good: ConditionExpression
        Movie movie = new Movie();
        movie.setGenre("Action");
        movie.setMovieId(movieId);
        movie.setTitle("Condition Test");
        movie.setAuthor("Author");
        movie.setReleaseYear(2024);
        movie.setActors(Set.of("Actor"));
        movie.setDescription("Test");
        movie.setRating(4.5);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Movie> request = new HttpEntity<>(movie, headers);

        ResponseEntity<Map> good = rest.exchange(url("/api/movies"), HttpMethod.PUT, request, Map.class);
        assertThat(good.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(good.getBody()).containsEntry("created", true);

        // Second attempt should fail (conditional check)
        ResponseEntity<Map> goodDup = rest.exchange(url("/api/movies"), HttpMethod.PUT, request, Map.class);
        assertThat(goodDup.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(goodDup.getBody()).containsEntry("created", false);

        // Bad: getItem + putItem
        ResponseEntity<Map> bad = rest.exchange(
                url("/api/movies/unsafe?genre=Action&movieId=" + movieId + "-bad"),
                HttpMethod.PUT, null, Map.class);
        assertThat(bad.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(bad.getBody()).containsEntry("created", true);
    }

    // ---- Pattern 3: Batch ----

    @Test
    void pattern3_batch() {
        // Write items via good path (BatchWriteItem)
        ResponseEntity<Map> goodWrite = rest.postForEntity(url("/api/batch/write/good?count=5"), null, Map.class);
        assertThat(goodWrite.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(goodWrite.getBody()).containsEntry("method", "BatchWriteItem");
        assertThat(goodWrite.getBody()).containsEntry("items", 5);

        // Write items via bad path (individual putItem)
        ResponseEntity<Map> badWrite = rest.postForEntity(url("/api/batch/write/naive?count=3"), null, Map.class);
        assertThat(badWrite.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(badWrite.getBody()).containsEntry("method", "individual putItem calls");

        // Write known items for read-back verification
        seedMovie("batch-test", "known-id-1");
        seedMovie("batch-test", "known-id-2");

        // Read back via good path (BatchGetItem)
        ResponseEntity<Map> goodRead = rest.getForEntity(
                url("/api/batch/read/good?ids=known-id-1,known-id-2"), Map.class);
        assertThat(goodRead.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(goodRead.getBody()).containsEntry("method", "BatchGetItem");

        // Read back via bad path (individual GetItem)
        ResponseEntity<Map> badRead = rest.getForEntity(
                url("/api/batch/read/naive?ids=known-id-1,known-id-2"), Map.class);
        assertThat(badRead.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    // ---- Pattern 4: Projection ----

    @Test
    void pattern4_projection() {
        // Seed a movie with a unique description for verification
        String uniqueDesc = "UNIQUE_DESC_" + UUID.randomUUID();
        Map<String, AttributeValue> fullItem = Map.of(
                "genre", AttributeValue.fromS("Action"),
                "movieId", AttributeValue.fromS("proj-" + UUID.randomUUID()),
                "title", AttributeValue.fromS("Projection Test Movie"),
                "author", AttributeValue.fromS("Test Author"),
                "description", AttributeValue.fromS(uniqueDesc),
                "actors", AttributeValue.fromSs(List.of("Actor One", "Actor Two")),
                "releaseYear", AttributeValue.fromN("2024"),
                "rating", AttributeValue.fromN("4.5"));
        dynamoDbClient.putItem(PutItemRequest.builder().tableName("movies").item(fullItem).build());

        // Bad: full item (description should be present)
        ResponseEntity<Map> bad = rest.getForEntity(url("/api/movies?genre=Action"), Map.class);
        assertThat(bad.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(bad.getBody()).containsKey("movies");
        List<Map<String, Object>> badMovies = (List<Map<String, Object>>) bad.getBody().get("movies");
        assertThat(badMovies).isNotEmpty();
        Map<String, Object> firstBad = badMovies.getFirst();
        assertThat(firstBad).containsKeys("genre", "movieId", "title", "author", "description", "actors", "releaseYear", "rating");
        // Full items have non-empty description
        assertThat((String) firstBad.get("description")).isNotEmpty();

        // Good: projected (title, releaseYear only); description should be empty/default
        ResponseEntity<Map> good = rest.getForEntity(url("/api/movies?genre=Action&fields=title,releaseYear"), Map.class);
        assertThat(good.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(good.getBody()).containsKey("movies");
        List<Map<String, Object>> goodMovies = (List<Map<String, Object>>) good.getBody().get("movies");
        assertThat(goodMovies).isNotEmpty();
        Map<String, Object> firstGood = goodMovies.getFirst();
        assertThat(firstGood).containsKeys("title", "releaseYear");
        // Non-projected fields are empty/default when using Enhanced Client
        assertThat((String) firstGood.get("description")).isEmpty();
        assertThat(firstGood.get("actors")).isInstanceOf(List.class);
        assertThat((List<?>) firstGood.get("actors")).isEmpty();
    }

    // ---- Pattern 5: GSI vs Filter ----

    @Test
    void pattern5_gsiVsFilter() {
        seedMovie("Action", "gsi-" + UUID.randomUUID(), "Alice Chen");
        seedMovie("Action", "gsi-" + UUID.randomUUID(), "Alice Chen");
        seedMovie("Comedy", "gsi-" + UUID.randomUUID(), "Alice Chen");

        // Bad: FilterExpression on partition query (requires genre, returns only Alice's Action movies)
        ResponseEntity<Map> bad = rest.getForEntity(
                url("/api/movies/by-author?author=Alice+Chen&genre=Action"), Map.class);
        assertThat(bad.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(bad.getBody()).containsKey("movies");
        List<Map<String, Object>> badMovies = (List<Map<String, Object>>) bad.getBody().get("movies");
        assertThat(badMovies)
                .allMatch(m -> "Action".equals(m.get("genre")))
                .allMatch(m -> "Alice Chen".equals(m.get("author")))
                .hasSize(2);

        // Good: GSI query: direct author lookup, returns all Alice's movies across genres
        ResponseEntity<Map> good = rest.getForEntity(
                url("/api/movies/by-author?author=Alice+Chen"), Map.class);
        assertThat(good.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(good.getBody()).containsKey("movies");
        List<Map<String, Object>> goodMovies = (List<Map<String, Object>>) good.getBody().get("movies");
        assertThat(goodMovies)
                .allMatch(m -> "Alice Chen".equals(m.get("author")))
                .hasSize(3); // 2 Action + 1 Comedy
    }

    // ---- Pattern 6: Pagination ----

    @Test
    void pattern6_pagination() {
        String genre = "Drama";
        for (int i = 0; i < 12; i++) {
            seedMovie(genre, "pag-" + UUID.randomUUID());
        }

        // Bad: unbounded query (page <= 0 required to trigger bad path)
        ResponseEntity<Map> bad = rest.getForEntity(url("/api/movies/paged?genre=" + genre + "&page=0"), Map.class);
        assertThat(bad.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(bad.getBody()).containsKey("movies");
        List<Map<String, Object>> badMovies = (List<Map<String, Object>>) bad.getBody().get("movies");
        assertThat(badMovies).hasSize(12);
        assertThat(bad.getBody()).containsEntry("method", "unbounded query");

        // Good page 1: first 5 items
        ResponseEntity<Map> good = rest.getForEntity(
                url("/api/movies/paged?genre=" + genre + "&page=1&size=5"), Map.class);
        assertThat(good.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<Map<String, Object>> goodMovies = (List<Map<String, Object>>) good.getBody().get("movies");
        assertThat(goodMovies).hasSize(5);
        assertThat(good.getBody()).containsEntry("method", "paginated (Limit + ExclusiveStartKey)");

        // Good page 2: next 5 items (different from page 1)
        ResponseEntity<Map> good2 = rest.getForEntity(
                url("/api/movies/paged?genre=" + genre + "&page=2&size=5"), Map.class);
        assertThat(good2.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<Map<String, Object>> good2Movies = (List<Map<String, Object>>) good2.getBody().get("movies");
        assertThat(good2Movies).hasSize(5).doesNotContainAnyElementsOf(goodMovies);
    }

    // ---- Pattern 7: Scan vs Query ----

    @Test
    void pattern7_scanVsQuery() {
        seedMovie("Sci-Fi", "scan-" + UUID.randomUUID());
        seedMovie("Sci-Fi", "scan-" + UUID.randomUUID());
        seedMovie("Comedy", "scan-" + UUID.randomUUID());

        ResponseEntity<Map> scan = rest.getForEntity(url("/api/movies/scan"), Map.class);
        assertThat(scan.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(scan.getBody()).containsKey("movies");
        int scanCount = ((List<?>) scan.getBody().get("movies")).size();
        assertThat(scan.getBody()).containsEntry("method", "full table scan");

        ResponseEntity<Map> query = rest.getForEntity(url("/api/movies/query?genre=Sci-Fi"), Map.class);
        assertThat(query.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(query.getBody()).containsKey("movies");
        int queryCount = ((List<?>) query.getBody().get("movies")).size();

        // Scan should return more items than a single-partition query
        assertThat(scanCount).isGreaterThan(queryCount);
    }

    // ---- Pattern 8: TTL ----

    @Test
    void pattern8_ttl() {
        // Bad: write without TTL
        ResponseEntity<Map> bad = rest.postForEntity(url("/api/ttl/write/no-ttl?count=3"), null, Map.class);
        assertThat(bad.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(bad.getBody()).containsEntry("method", "write without TTL");
        assertThat(bad.getBody()).containsEntry("items", 3);

        // Good: write with TTL (1 hour)
        ResponseEntity<Map> good = rest.postForEntity(url("/api/ttl/write?count=3&ttlSeconds=3600"), null, Map.class);
        assertThat(good.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(good.getBody()).containsEntry("method", "write with TTL (expireAt)");
        assertThat(good.getBody()).containsEntry("items", 3);
        assertThat(good.getBody()).containsEntry("ttlSeconds", 3600);

        // Verify items are readable (TTL hasn't expired yet)
        var items = dynamoDbClient.scan(ScanRequest.builder()
                .tableName("movies")
                .filterExpression("begins_with(movieId, :prefix)")
                .expressionAttributeValues(Map.of(":prefix", AttributeValue.fromS("ttl-yes-")))
                .build());
        assertThat(items.count()).isEqualTo(3);
        // Verify expireAt attribute exists on TTL items
        for (var item : items.items()) {
            assertThat(item).containsKey("expireAt");
        }

        // Bad items have no expireAt
        var noTtlItems = dynamoDbClient.scan(ScanRequest.builder()
                .tableName("movies")
                .filterExpression("begins_with(movieId, :prefix)")
                .expressionAttributeValues(Map.of(":prefix", AttributeValue.fromS("ttl-no-")))
                .build());
        assertThat(noTtlItems.count()).isEqualTo(3);
        for (var item : noTtlItems.items()) {
            assertThat(item).doesNotContainKey("expireAt");
        }
    }

    // ---- Pattern 9: Optimistic Locking ----

    @Test
    void pattern9_locking() {
        // Seed a movie with version=1
        String movieId = "lock-" + UUID.randomUUID().toString().substring(0, 8);
        seedMovie("Action", movieId, "Lock Author");
        // Manually add version to the item
        dynamoDbClient.updateItem(UpdateItemRequest.builder()
                .tableName("movies")
                .key(Map.of(
                        "genre", AttributeValue.fromS("Action"),
                        "movieId", AttributeValue.fromS(movieId)))
                .updateExpression("SET #ver = :v")
                .expressionAttributeNames(Map.of("#ver", "version"))
                .expressionAttributeValues(Map.of(":v", AttributeValue.fromN("1")))
                .build());

        // Good: update with correct version (should succeed)
        ResponseEntity<Map> good = rest.exchange(
                url("/api/lock/good?genre=Action&movieId=" + movieId + "&title=Updated&expectedVersion=1"),
                HttpMethod.PUT, null, Map.class);
        assertThat(good.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(good.getBody()).containsEntry("updated", true);
        assertThat(good.getBody()).containsEntry("expectedVersion", 1);
        assertThat(good.getBody()).containsEntry("newVersion", 2);

        // Good: update with stale version (should fail, version is now 2)
        ResponseEntity<Map> stale = rest.exchange(
                url("/api/lock/good?genre=Action&movieId=" + movieId + "&title=Stale&expectedVersion=1"),
                HttpMethod.PUT, null, Map.class);
        assertThat(stale.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(stale.getBody()).containsEntry("updated", false);

        // Bad: unconditional update (always succeeds, no conflict detection)
        ResponseEntity<Map> bad = rest.exchange(
                url("/api/lock/bad?genre=Action&movieId=" + movieId + "&title=Overwritten"),
                HttpMethod.PUT, null, Map.class);
        assertThat(bad.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(bad.getBody()).containsEntry("updated", true);
    }

    // ---- Pattern 10: Transactions ----

    @Test
    void pattern10_transactions() {
        // Bad: individual puts (no atomicity)
        ResponseEntity<Map> bad = rest.postForEntity(
                url("/api/transaction/write/bad?count=5"), null, Map.class);
        assertThat(bad.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(bad.getBody()).containsEntry("method", "individual putItem calls (no atomicity)");
        assertThat(bad.getBody()).containsEntry("requested", 5);

        // Good: TransactWriteItems (atomic)
        ResponseEntity<Map> good = rest.postForEntity(
                url("/api/transaction/write/good?count=5"), null, Map.class);
        assertThat(good.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(good.getBody()).containsEntry("method", "TransactWriteItems (ACID)");
        assertThat(good.getBody()).containsEntry("items", 5);

        // Verify all good transaction items exist
        var items = dynamoDbClient.scan(ScanRequest.builder()
                .tableName("movies")
                .filterExpression("begins_with(movieId, :prefix)")
                .expressionAttributeValues(Map.of(":prefix", AttributeValue.fromS("tx-good-")))
                .build());
        assertThat(items.count()).isEqualTo(5);
    }

    // ---- Helpers ----

    private void waitForGsiActive() {
        long deadline = System.currentTimeMillis() + 30_000; // 30s timeout
        while (System.currentTimeMillis() < deadline) {
            try {
                var desc = dynamoDbClient.describeTable(DescribeTableRequest.builder()
                        .tableName("movies").build());
                boolean allActive = desc.table().globalSecondaryIndexes().stream()
                        .allMatch(gsi -> gsi.indexStatus() == IndexStatus.ACTIVE);
                if (allActive) return;
            } catch (ResourceNotFoundException _) {
                // table not ready yet, keep waiting
            }
            try {
                Thread.sleep(500);
            } catch (InterruptedException _) {
                Thread.currentThread().interrupt();
                throw new AssertionError("waitForGsiActive interrupted");
            }
        }
        throw new AssertionError("GSI did not become ACTIVE within 30s timeout (DynamoDB Local may be slow)");
    }

    private void clearTable() {
        Map<String, AttributeValue> lastKey = null;
        do {
            var scan = dynamoDbClient.scan(ScanRequest.builder()
                    .tableName("movies")
                    .exclusiveStartKey(lastKey)
                    .limit(100)
                    .build());
            for (var item : scan.items()) {
                dynamoDbClient.deleteItem(DeleteItemRequest.builder()
                        .tableName("movies")
                        .key(Map.of(
                                "genre", AttributeValue.fromS(item.get("genre").s()),
                                "movieId", AttributeValue.fromS(item.get("movieId").s())))
                        .build());
            }
            lastKey = scan.lastEvaluatedKey();
        } while (lastKey != null && !lastKey.isEmpty());
    }

    private void seedMovie(String genre, String movieId) {
        seedMovie(genre, movieId, "Default Author");
    }

    private void seedMovie(String genre, String movieId, String author) {
        dynamoDbClient.putItem(PutItemRequest.builder()
                .tableName("movies")
                .item(Map.of(
                        "genre", AttributeValue.fromS(genre),
                        "movieId", AttributeValue.fromS(movieId),
                        "title", AttributeValue.fromS("Test Movie"),
                        "author", AttributeValue.fromS(author),
                        "description", AttributeValue.fromS("A test movie"),
                "actors", AttributeValue.fromSs(List.of("Default Actor")),
                "releaseYear", AttributeValue.fromN("2024"),
                "rating", AttributeValue.fromN("4.0")))
                .build());
    }
}
