package com.hogwai.dynamodb.clients.repository;

import com.hogwai.dynamodb.clients.setup.TableInitializer;
import com.hogwai.dynamodb.clients.model.Movie;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.List;
import java.util.Map;

@Repository
public class MovieEnhancedRepository {

    private final DynamoDbTable<Movie> table;

    public MovieEnhancedRepository(DynamoDbEnhancedClient enhancedClient) {
        this.table = enhancedClient.table(TableInitializer.TABLE_NAME,
                TableSchema.fromBean(Movie.class));
    }

    public List<Movie> queryByGenre(String genre) {
        return table.query(QueryConditional.keyEqualTo(Key.builder()
                        .partitionValue(genre).build()))
                .items().stream().toList();
    }

    public List<Movie> queryByGenreWithProjection(String genre, List<String> fields) {
        return table.query(QueryEnhancedRequest.builder()
                        .queryConditional(QueryConditional.keyEqualTo(Key.builder()
                                .partitionValue(genre).build()))
                        .attributesToProject(fields)
                        .build())
                .items().stream().toList();
    }

    public Page<Movie> queryByGenrePage(String genre, int limit, Map<String, AttributeValue> lastKey) {
        var requestBuilder = QueryEnhancedRequest.builder()
                .queryConditional(QueryConditional.keyEqualTo(Key.builder()
                        .partitionValue(genre).build()))
                .limit(limit);
        if (lastKey != null && !lastKey.isEmpty()) {
            requestBuilder.exclusiveStartKey(lastKey);
        }
        return table.query(requestBuilder.build()).iterator().next();
    }
}
