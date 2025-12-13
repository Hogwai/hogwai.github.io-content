package com.hogwai.repository;

import jakarta.inject.Singleton;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.util.HashMap;
import java.util.Map;

@Singleton
@Slf4j
@RequiredArgsConstructor
public class PostRepository {
    public static final String POSTS = "posts";
    public static final String SUBREDDIT = "subreddit";
    public static final String ID = "id";

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
        return response.hasItem() && !response.item()
                                              .isEmpty();
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

        return response.hasItem() && !response.item()
                                              .isEmpty();
    }

    public boolean hasPostsForSubreddit(String subreddit) {
        Map<String, AttributeValue> values = new HashMap<>();
        values.put(":subVal", AttributeValue.builder()
                                            .s(subreddit)
                                            .build());

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
        values.put(":subVal", AttributeValue.builder()
                                            .s(subreddit)
                                            .build());
        values.put(":idVal", AttributeValue.builder()
                                           .s(id)
                                           .build());
        values.put(":zero", AttributeValue.builder()
                                          .n("0")
                                          .build());

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

    private static void logConsumedCapacity(ConsumedCapacity capacity) {
        log.info("Consumed capacity: {}", capacity);
    }

    private static Map<String, AttributeValue> buildKey(String partitionKey, String sortKey) {
        return Map.of(
                SUBREDDIT, AttributeValue.builder()
                                         .s(partitionKey)
                                         .build(),
                ID, AttributeValue.builder()
                                  .s(sortKey)
                                  .build()
        );
    }
}