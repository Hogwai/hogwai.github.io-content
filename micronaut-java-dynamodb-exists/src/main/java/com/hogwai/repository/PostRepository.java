package com.hogwai.repository;

import jakarta.inject.Singleton;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.util.*;
import java.util.concurrent.TimeUnit;

@Singleton
@Slf4j
@RequiredArgsConstructor
public class PostRepository {
    public static final String POSTS = "posts";
    public static final String SUBREDDIT = "subreddit";
    public static final String ID = "id";
    public static final String KEYWORDS = "keywords";

    private final DynamoDbClient dynamoDbClient;

    public boolean existsByProjection(String subreddit, String id) {
        Map<String, AttributeValue> key = buildKey(subreddit, id);

        GetItemRequest request = GetItemRequest.builder()
                                               .tableName(POSTS)
                                               .key(key)
                                               .projectionExpression(SUBREDDIT)
                                               .returnConsumedCapacity(ReturnConsumedCapacity.INDEXES)
                                               .build();

        GetItemResponse response = dynamoDbClient.getItem(request);

        logConsumedCapacity(response.consumedCapacity());
        return response.hasItem();
    }

    public boolean existsByGetItem(String subreddit, String id) {
        Map<String, AttributeValue> key = buildKey(subreddit, id);

        GetItemRequest request = GetItemRequest.builder()
                                               .tableName(POSTS)
                                               .key(key)
                                               .returnConsumedCapacity(ReturnConsumedCapacity.INDEXES)
                                               .build();

        GetItemResponse response = dynamoDbClient.getItem(request);

        logConsumedCapacity(response.consumedCapacity());

        return response.hasItem();
    }

    public Map<String, Boolean> batchExists(String subreddit, List<String> ids) {
        List<Map<String, AttributeValue>> keys = ids.stream()
                                                    .map(id -> buildKey(subreddit, id))
                                                    .toList();

        Map<String, KeysAndAttributes> requestItems = new HashMap<>();
        requestItems.put(POSTS, KeysAndAttributes.builder()
                                                 .keys(keys)
                                                 .projectionExpression(ID)
                                                 .build());

        BatchGetItemRequest request = BatchGetItemRequest.builder()
                                                         .requestItems(requestItems)
                                                         .returnConsumedCapacity(ReturnConsumedCapacity.INDEXES)
                                                         .build();

        Map<String, Boolean> result = new HashMap<>();
        ids.forEach(id -> result.put(id, false));

        int attempts = 0;

        do {
            attempts++;
            BatchGetItemResponse response = dynamoDbClient.batchGetItem(request);

            var foundItems = response.responses().getOrDefault(POSTS, List.of());
            foundItems.forEach(item -> result.put(item.get(ID).s(), true));

            logConsumedCapacity(response.consumedCapacity().stream().findFirst().orElse(null));

            if (response.hasUnprocessedKeys() && !response.unprocessedKeys().isEmpty()) {
                request = request.toBuilder()
                                 .requestItems(response.unprocessedKeys())
                                 .build();
                backoff(attempts);
            } else {
                break;
            }
        } while (attempts < 5);

        return result;
    }

    public boolean hasPostsForSubreddit(String subreddit) {
        Map<String, AttributeValue> values = new HashMap<>();
        values.put(":subVal", AttributeValue.fromS(subreddit));

        QueryRequest request = QueryRequest.builder()
                                           .tableName(POSTS)
                                           .keyConditionExpression("subreddit = :subVal")
                                           .expressionAttributeValues(values)
                                           .projectionExpression(SUBREDDIT)
                                           .limit(1)
                                           .build();

        QueryResponse response = dynamoDbClient.query(request);
        logConsumedCapacity(response.consumedCapacity());
        return response.count() > 0;
    }

    public boolean hasKeywords(String subreddit, String id) {
        Map<String, AttributeValue> values = new HashMap<>();
        values.put(":subVal", AttributeValue.fromS(subreddit));
        values.put(":idVal", AttributeValue.fromS(id));
        values.put(":zero", AttributeValue.fromN("0"));

        QueryRequest request = QueryRequest.builder()
                                           .tableName(POSTS)
                                           .keyConditionExpression("subreddit = :subVal AND id = :idVal")
                                           .filterExpression("size(keywords) > :zero")
                                           .expressionAttributeValues(values)
                                           .projectionExpression("id")
                                           .limit(1)
                                           .build();

        QueryResponse response = dynamoDbClient.query(request);
        logConsumedCapacity(response.consumedCapacity());
        return response.count() > 0;
    }

    public boolean hasKeywordsByGetItem(String subreddit, String id) {
        GetItemRequest request = GetItemRequest.builder()
                                               .tableName(POSTS)
                                               .key(buildKey(subreddit, id))
                                               .projectionExpression(KEYWORDS)
                                               .consistentRead(false)
                                               .returnConsumedCapacity(ReturnConsumedCapacity.INDEXES)
                                               .build();

        GetItemResponse response = dynamoDbClient.getItem(request);
        logConsumedCapacity(response.consumedCapacity());

        if (!response.hasItem()) {
            return false;
        }

        AttributeValue keywords = response.item().get(KEYWORDS);
        return keywords != null && keywords.hasL() && !keywords.l().isEmpty();
    }

    private static void backoff(int attempt) {
        try {
            TimeUnit.MILLISECONDS.sleep((long) Math.pow(2, attempt) * 100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static void logConsumedCapacity(ConsumedCapacity capacity) {
        log.info("Consumed capacity: {}", capacity);
    }

    private static Map<String, AttributeValue> buildKey(String partitionKey, String sortKey) {
        return Map.of(
                SUBREDDIT, AttributeValue.fromS(partitionKey),
                ID, AttributeValue.fromS(sortKey)
        );
    }
}