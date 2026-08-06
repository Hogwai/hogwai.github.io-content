package com.hogwai.dynamodb.clients.repository;

import com.hogwai.dynamodb.clients.setup.TableInitializer;
import com.hogwai.dynamodb.clients.model.Movie;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.util.*;

@Repository
public class MovieRawRepository {

    private static final Logger log = LoggerFactory.getLogger(MovieRawRepository.class);
    public static final String MOVIE_ID = "movieId";
    public static final String GENRE = "genre";
    public static final String GENRE_PARAM = ":genre";
    public static final String GENRE_CONDITION_EXPRESSION = "genre = :genre";
    private final DynamoDbClient client;
    private static final String TABLE = TableInitializer.TABLE_NAME;

    public MovieRawRepository(DynamoDbClient client) {
        this.client = client;
    }

    /**
     * Write a complete item, replacing any existing item with the same key.
     */
    public ConsumedCapacity putItem(Movie movie) {
        var response = client.putItem(PutItemRequest.builder()
                .tableName(TABLE)
                .item(movie.toItemMap())
                .returnConsumedCapacity(ReturnConsumedCapacity.TOTAL)
                .build());
        return response.consumedCapacity();
    }

    /**
     * Write a new item only if the specified condition evaluates to true.
     *
     * @param conditionExpr a DynamoDB condition expression (e.g. {@code attribute_not_exists(movieId)})
     * @param exprValues    expression attribute values, or empty map if none
     * @throws ConditionalCheckFailedException if the condition evaluates to false
     * @see DynamoDbClient#putItem(PutItemRequest)
     */
    public ConsumedCapacity putItemWithCondition(Movie movie, String conditionExpr,
                                                  Map<String, AttributeValue> exprValues) {
        var builder = PutItemRequest.builder()
                .tableName(TABLE)
                .item(movie.toItemMap())
                .conditionExpression(conditionExpr)
                .returnConsumedCapacity(ReturnConsumedCapacity.TOTAL);
        if (exprValues != null && !exprValues.isEmpty()) {
            builder.expressionAttributeValues(exprValues);
        }
        var response = client.putItem(builder.build());
        return response.consumedCapacity();
    }

    /**
     * Retrieve a full item by its composite key.
     * <p>
     * <b>Cost note:</b> This fetches the entire item. When only specific attributes are
     * needed, use {@link #getItemWithProjection(String, String, List)} instead.
     */
    public GetItemResponse getItem(String genre, String movieId) {
        return client.getItem(GetItemRequest.builder()
                .tableName(TABLE)
                .key(Map.of(
                        GENRE, AttributeValue.fromS(genre),
                        MOVIE_ID, AttributeValue.fromS(movieId)))
                .returnConsumedCapacity(ReturnConsumedCapacity.TOTAL)
                .build());
    }

    /**
     * Retrieve only the requested attributes of an item.
     * <p>
     * Projection reduces the data transferred over the wire and avoids deserialising
     * fields that the caller does not need.
     *
     * @param fields attribute names to include in the result (e.g. {@code ["actors"]})
     */
    public GetItemResponse getItemWithProjection(String genre, String movieId, List<String> fields) {
        return client.getItem(GetItemRequest.builder()
                .tableName(TABLE)
                .key(Map.of(
                        GENRE, AttributeValue.fromS(genre),
                        MOVIE_ID, AttributeValue.fromS(movieId)))
                .projectionExpression(String.join(",", fields))
                .returnConsumedCapacity(ReturnConsumedCapacity.TOTAL)
                .build());
    }

    /**
     * Atomically add an element to the {@code actors} string set attribute.
     * <p>
     * This is a single round-trip update with no read-before-write, eliminating
     * the lost-update risk inherent in the getItem → mutate → putItem cycle.
     *
     * @param actorName the actor to add
     */
    public UpdateItemResponse addActor(String genre, String movieId, String actorName) {
        return client.updateItem(UpdateItemRequest.builder()
                .tableName(TABLE)
                .key(Map.of(
                        GENRE, AttributeValue.fromS(genre),
                        MOVIE_ID, AttributeValue.fromS(movieId)))
                .updateExpression("ADD actors :newActor")
                .expressionAttributeValues(Map.of(":newActor", AttributeValue.fromSs(List.of(actorName))))
                .returnConsumedCapacity(ReturnConsumedCapacity.TOTAL)
                .build());
    }

    /**
     * Atomically remove an element from the {@code actors} string set attribute.
     */
    public UpdateItemResponse removeActor(String genre, String movieId, String actorName) {
        return client.updateItem(UpdateItemRequest.builder()
                .tableName(TABLE)
                .key(Map.of(
                        GENRE, AttributeValue.fromS(genre),
                        MOVIE_ID, AttributeValue.fromS(movieId)))
                .updateExpression("DELETE actors :oldActor")
                .expressionAttributeValues(Map.of(":oldActor", AttributeValue.fromSs(List.of(actorName))))
                .returnConsumedCapacity(ReturnConsumedCapacity.TOTAL)
                .build());
    }

    /**
     * Query all items in a partition.
     */
    public QueryResponse query(String genre) {
        return client.query(QueryRequest.builder()
                .tableName(TABLE)
                .keyConditionExpression(GENRE_CONDITION_EXPRESSION)
                .expressionAttributeValues(Map.of(GENRE_PARAM, AttributeValue.fromS(genre)))
                .returnConsumedCapacity(ReturnConsumedCapacity.TOTAL)
                .build());
    }

    /**
     * Count items in a partition using {@code Select.COUNT}.
     * <p>
     * {@code Select.COUNT} does <em>not</em> reduce RCU — DynamoDB bills reads on the
     * total item size before projection (4 KB increments). For partitions exceeding 1 MB,
     * the count is partial and pagination is required to sum across pages.
     */
    public QueryResponse queryWithSelectCount(String genre) {
        return client.query(QueryRequest.builder()
                .tableName(TABLE)
                .keyConditionExpression(GENRE_CONDITION_EXPRESSION)
                .expressionAttributeValues(Map.of(GENRE_PARAM, AttributeValue.fromS(genre)))
                .select(Select.COUNT)
                .returnConsumedCapacity(ReturnConsumedCapacity.TOTAL)
                .build());
    }

    /**
     * Query items in a partition, applying a post-query filter.
     * <p>
     * DynamoDB applies the filter <em>after</em> reading items from the table, but
     * before returning them. RCU is consumed for every item read, including those
     * filtered out. For predictable performance, prefer a GSI query over a filter.
     */
    public QueryResponse queryWithFilter(String genre, String filterExpr,
                                          Map<String, AttributeValue> exprValues) {
        Map<String, AttributeValue> eav = new HashMap<>();
        eav.put(GENRE_PARAM, AttributeValue.fromS(genre));
        if (exprValues != null) {
            eav.putAll(exprValues);
        }
        return client.query(QueryRequest.builder()
                .tableName(TABLE)
                .keyConditionExpression(GENRE_CONDITION_EXPRESSION)
                .filterExpression(filterExpr)
                .expressionAttributeValues(eav)
                .returnConsumedCapacity(ReturnConsumedCapacity.TOTAL)
                .build());
    }

    /**
     * Query a single page of items within a partition.
     *
     * @param lastKey the exclusive start key from the previous page, or null for the first page
     */
    public QueryResponse queryPaginated(String genre, int limit,
                                         Map<String, AttributeValue> lastKey) {
        var builder = QueryRequest.builder()
                .tableName(TABLE)
                .keyConditionExpression(GENRE_CONDITION_EXPRESSION)
                .expressionAttributeValues(Map.of(GENRE_PARAM, AttributeValue.fromS(genre)))
                .limit(limit)
                .returnConsumedCapacity(ReturnConsumedCapacity.TOTAL);
        if (lastKey != null && !lastKey.isEmpty()) {
            builder.exclusiveStartKey(lastKey);
        }
        return client.query(builder.build());
    }

    /**
     * Scan the entire table.
     * <p>
     * Prefer a query over a scan whenever the partition key is known.
     */
    public ScanResponse scanAll() {
        return client.scan(ScanRequest.builder()
                .tableName(TABLE)
                .returnConsumedCapacity(ReturnConsumedCapacity.TOTAL)
                .build());
    }

    /**
     * Retrieve multiple items via individual {@code GetItem} calls.
     * <p>
     * <b>Anti-pattern:</b> N sequential round-trips. For reads that share the same
     * table, prefer {@link #batchGet(List)}.
     */
    public List<GetItemResponse> getItemsIndividual(List<Map<String, AttributeValue>> keys) {
        return keys.stream().map(key ->
                client.getItem(GetItemRequest.builder()
                        .tableName(TABLE)
                        .key(key)
                        .returnConsumedCapacity(ReturnConsumedCapacity.TOTAL)
                        .build()))
                .toList();
    }

    /**
     * Retrieve multiple items in a single batch request.
     * <p>
     * DynamoDB limits a single {@code BatchGetItem} to 100 items or 16 MB.
     * Unprocessed keys should be retried.
     */
    public BatchGetItemResponse batchGet(List<Map<String, AttributeValue>> keys) {
        return client.batchGetItem(BatchGetItemRequest.builder()
                .requestItems(Map.of(TABLE, KeysAndAttributes.builder()
                        .keys(keys)
                        .build()))
                .returnConsumedCapacity(ReturnConsumedCapacity.TOTAL)
                .build());
    }

    /**
     * Write multiple items via individual {@code PutItem} calls.
     * <p>
     * <b>Antipattern:</b> N sequential round-trips. For writes that share the same
     * table, prefer {@link #batchPut(List)}.
     */
    public int putItemsIndividual(List<Movie> movies) {
        for (var movie : movies) {
            client.putItem(PutItemRequest.builder()
                    .tableName(TABLE)
                    .item(movie.toItemMap())
                    .build());
        }
        return movies.size();
    }

    /**
     * Write up to 25 items in a single batch request.
     * <p>
     * DynamoDB limits {@code BatchWriteItem} to 25 items or 16 MB. Batch writes
     * do not support condition expressions and are not atomic. Items returned as
     * {@code UnprocessedItems} are retried up to 3 times with exponential backoff.
     */
    public BatchWriteItemResponse batchPut(List<Movie> movies) {
        var writeRequests = movies.stream()
                .map(m -> WriteRequest.builder()
                        .putRequest(_ -> PutRequest.builder()
                                .item(m.toItemMap()).build())
                        .build())
                .toList();
        var requestItems = Map.of(TABLE, writeRequests);
        var response = client.batchWriteItem(BatchWriteItemRequest.builder()
                .requestItems(requestItems)
                .returnConsumedCapacity(ReturnConsumedCapacity.TOTAL)
                .build());
        var unprocessed = response.unprocessedItems();
        int attempt = 1;
        while (!unprocessed.isEmpty() && attempt < 3) {
            attempt++;
            try {
                Thread.sleep(100L * attempt);
            } catch (InterruptedException _) {
                Thread.currentThread().interrupt();
                break;
            }
            response = client.batchWriteItem(BatchWriteItemRequest.builder()
                    .requestItems(unprocessed)
                    .returnConsumedCapacity(ReturnConsumedCapacity.TOTAL)
                    .build());
            unprocessed = response.unprocessedItems();
        }
        if (!unprocessed.isEmpty()) {
            log.warn("batchPut: {} unprocessed items remain after {} attempts",
                    unprocessed.values().stream().mapToInt(List::size).sum(), attempt);
        }
        return response;
    }

    /**
     * Update an item with optimistic locking via a version condition expression.
     * <p>
     * The condition {@code version = :expectedVer OR attribute_not_exists(version)}
     * allows first-time creation but creates a theoretical window where two concurrent
     * first-writers could both succeed. For strict atomicity from the first write,
     * create items with version=1 via a separate conditional {@code PutItem}, then
     * use this method for subsequent writes only.
     *
     * @param expectedVersion the version the caller last read; the server increments it atomically
     */
    public UpdateItemResponse updateWithVersion(Movie movie, int expectedVersion) {
        var item = movie.toItemMap();
        item.put("version", AttributeValue.fromN(String.valueOf(expectedVersion + 1)));

        var key = Map.of(
                GENRE, AttributeValue.fromS(movie.getGenre()),
                MOVIE_ID, AttributeValue.fromS(movie.getMovieId()));

        return client.updateItem(UpdateItemRequest.builder()
                .tableName(TABLE)
                .key(key)
                .updateExpression("SET title = :title, #ver = :newVer")
                .conditionExpression("version = :expectedVer OR attribute_not_exists(version)")
                .expressionAttributeValues(Map.of(
                        ":title", AttributeValue.fromS(movie.getTitle()),
                        ":newVer", AttributeValue.fromN(String.valueOf(expectedVersion + 1)),
                        ":expectedVer", AttributeValue.fromN(String.valueOf(expectedVersion))))
                .expressionAttributeNames(Map.of("#ver", "version"))
                .returnConsumedCapacity(ReturnConsumedCapacity.TOTAL)
                .build());
    }

    /**
     * Update an item unconditionally.
     * <p>
     * <b>Antipattern:</b> No version check means concurrent updates can silently
     * overwrite each other (lost update). Prefer {@link #updateWithVersion(Movie, int)}.
     */
    public UpdateItemResponse updateUnconditional(Movie movie) {
        var key = Map.of(
                GENRE, AttributeValue.fromS(movie.getGenre()),
                MOVIE_ID, AttributeValue.fromS(movie.getMovieId()));

        return client.updateItem(UpdateItemRequest.builder()
                .tableName(TABLE)
                .key(key)
                .updateExpression("SET title = :title")
                .expressionAttributeValues(Map.of(":title", AttributeValue.fromS(movie.getTitle())))
                .returnConsumedCapacity(ReturnConsumedCapacity.TOTAL)
                .build());
    }

    /**
     * Write items atomically within a single transaction.
     * <p>
     * All items are written or none are — there is no partial failure.
     * DynamoDB transactions have a 100-item / 4 MB limit.
     */
    public void transactWriteGood(List<Movie> movies) {
        var transactItems = movies.stream()
                .map(m -> TransactWriteItem.builder()
                        .put(_ -> Put.builder()
                                .tableName(TABLE)
                                .item(m.toItemMap())
                                .build())
                        .build())
                .toList();

        client.transactWriteItems(TransactWriteItemsRequest.builder()
                .transactItems(transactItems)
                .returnConsumedCapacity(ReturnConsumedCapacity.TOTAL)
                .build());
    }

    /**
     * Write items individually without transaction guarantees.
     * <p>
     * <b>Antipattern:</b> Some items may succeed while others fail, leaving the
     * dataset in a partially written state.
     *
     * @return a list of 1 (success) or 0 (failure) per item
     */
    public List<Integer> transactWriteBad(List<Movie> movies) {
        List<Integer> results = new ArrayList<>();
        for (var movie : movies) {
            try {
                client.putItem(PutItemRequest.builder()
                        .tableName(TABLE)
                        .item(movie.toItemMap())
                        .build());
                results.add(1);
            } catch (DynamoDbException e) {
                log.warn("transactWriteBad: putItem failed for {}: {}", movie.getMovieId(), e.getMessage());
                results.add(0);
            }
        }
        return results;
    }

    /**
     * Query the {@code author-index} GSI for all movies by a given author.
     */
    public QueryResponse queryByAuthor(String author) {
        return client.query(QueryRequest.builder()
                .tableName(TABLE)
                .indexName("author-index")
                .keyConditionExpression("author = :author")
                .expressionAttributeValues(Map.of(":author", AttributeValue.fromS(author)))
                .returnConsumedCapacity(ReturnConsumedCapacity.TOTAL)
                .build());
    }
}
