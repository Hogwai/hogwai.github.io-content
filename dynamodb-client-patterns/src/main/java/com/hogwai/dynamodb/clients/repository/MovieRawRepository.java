package com.hogwai.dynamodb.clients.repository;

import com.hogwai.dynamodb.clients.setup.TableInitializer;
import com.hogwai.dynamodb.clients.model.Movie;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.util.*;

@Repository
public class MovieRawRepository {

    public static final String MOVIE_ID = "movieId";
    public static final String GENRE = "genre";
    public static final String GENRE_PARAM = ":genre";
    public static final String GENRE_CONDITION_EXPRESSION = "genre = :genre";
    private final DynamoDbClient client;
    private static final String TABLE = TableInitializer.TABLE_NAME;

    public MovieRawRepository(DynamoDbClient client) {
        this.client = client;
    }

    public ConsumedCapacity putItem(Movie movie) {
        var response = client.putItem(PutItemRequest.builder()
                .tableName(TABLE)
                .item(movie.toItemMap())
                .returnConsumedCapacity(ReturnConsumedCapacity.TOTAL)
                .build());
        return response.consumedCapacity();
    }

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

    public GetItemResponse getItem(String genre, String movieId) {
        return client.getItem(GetItemRequest.builder()
                .tableName(TABLE)
                .key(Map.of(
                        GENRE, AttributeValue.fromS(genre),
                        MOVIE_ID, AttributeValue.fromS(movieId)))
                .returnConsumedCapacity(ReturnConsumedCapacity.TOTAL)
                .build());
    }

    public QueryResponse query(String genre) {
        return client.query(QueryRequest.builder()
                .tableName(TABLE)
                .keyConditionExpression(GENRE_CONDITION_EXPRESSION)
                .expressionAttributeValues(Map.of(GENRE_PARAM, AttributeValue.fromS(genre)))
                .returnConsumedCapacity(ReturnConsumedCapacity.TOTAL)
                .build());
    }

    /**
     * Count items in a partition using Select.COUNT.
     * Note: For partitions with >1 MB of data, the count is partial and pagination
     * (ExclusiveStartKey) is needed to sum results across pages.
     * Also, Select.COUNT does NOT reduce RCU (it is charged on the total read item size).
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

    public ScanResponse scanAll() {
        return client.scan(ScanRequest.builder()
                .tableName(TABLE)
                .returnConsumedCapacity(ReturnConsumedCapacity.TOTAL)
                .build());
    }

    public List<GetItemResponse> getItemsIndividual(List<Map<String, AttributeValue>> keys) {
        return keys.stream().map(key ->
                client.getItem(GetItemRequest.builder()
                        .tableName(TABLE)
                        .key(key)
                        .returnConsumedCapacity(ReturnConsumedCapacity.TOTAL)
                        .build()))
                .toList();
    }

    public BatchGetItemResponse batchGet(List<Map<String, AttributeValue>> keys) {
        return client.batchGetItem(BatchGetItemRequest.builder()
                .requestItems(Map.of(TABLE, KeysAndAttributes.builder()
                        .keys(keys)
                        .build()))
                .returnConsumedCapacity(ReturnConsumedCapacity.TOTAL)
                .build());
    }

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
     * Batch write up to 25 items in a single request.
     * Note: DynamoDB limits BatchWriteItem to 25 items / 16 MB total.
     * The response may contain UnprocessedItems (throttled) that require retry.
     * BatchWriteItem does NOT support condition expressions and is NOT atomic.
     */
    public BatchWriteItemResponse batchPut(List<Movie> movies) {
        var writeRequests = movies.stream()
                .map(m -> WriteRequest.builder()
                        .putRequest(_ ->PutRequest.builder()
                                .item(m.toItemMap()).build())
                        .build())
                .toList();
        return client.batchWriteItem(BatchWriteItemRequest.builder()
                .requestItems(Map.of(TABLE, writeRequests))
                .returnConsumedCapacity(ReturnConsumedCapacity.TOTAL)
                .build());
    }

    /**
     * Update item with optimistic locking via version condition.
     * The OR attribute_not_exists(version) clause allows first-time creation,
     * but creates a theoretical window where two concurrent first-writers
     * could both succeed. For strict atomicity from the first write,
     * create items with version=1 in a separate PutItem with attribute_not_exists,
     * then use this UpdateItem for subsequent writes only.
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

    public List<Integer> transactWriteBad(List<Movie> movies) {
        List<Integer> results = new ArrayList<>();
        for (var movie : movies) {
            try {
                client.putItem(PutItemRequest.builder()
                        .tableName(TABLE)
                        .item(movie.toItemMap())
                        .build());
                results.add(1); // success
            } catch (Exception _) {
                results.add(0); // failure
            }
        }
        return results;
    }

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
