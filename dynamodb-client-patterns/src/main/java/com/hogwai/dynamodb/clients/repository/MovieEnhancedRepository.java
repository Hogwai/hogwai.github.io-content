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

    /**
     * Retrieve a single movie by its composite key using the DynamoDB Enhanced Client.
     * <p>
     * <b>Antipattern context:</b> This method loads every mapped attribute of the item
     * into a {@link Movie} object. When only a subset of fields is needed (e.g. checking
     * whether {@link Movie#getActors()} is non-empty), prefer a targeted projection via
     * the low-level client to avoid deserializing the entire entity.
     *
     * @param genre   the partition key value
     * @param movieId the sort key value
     * @return the deserialize Movie, or {@code null} if no item matches the key
     */
    public Movie getItem(String genre, String movieId) {
        return table.getItem(r -> r.key(k -> k.partitionValue(genre).sortValue(movieId)));
    }

    /**
     * Query all movies within a partition.
     *
     * @param genre the partition key value
     * @return all items in the partition, fully deserialize
     */
    public List<Movie> queryByGenre(String genre) {
        return table.query(QueryConditional.keyEqualTo(Key.builder()
                        .partitionValue(genre).build()))
                .items().stream().toList();
    }

    /**
     * Query movies within a partition, retaining only the requested attributes
     * after deserialization. Projection is applied client-side by the Enhanced Client.
     *
     * @param genre  the partition key value
     * @param fields attribute names to project
     * @return items with only the requested fields populated
     */
    public List<Movie> queryByGenreWithProjection(String genre, List<String> fields) {
        return table.query(QueryEnhancedRequest.builder()
                        .queryConditional(QueryConditional.keyEqualTo(Key.builder()
                                .partitionValue(genre).build()))
                        .attributesToProject(fields)
                        .build())
                .items().stream().toList();
    }

    /**
     * Query a single page of movies within a partition using cursor-based pagination.
     *
     * @param genre   the partition key value
     * @param limit   maximum items to return in this page
     * @param lastKey exclusive start key from the previous page, or empty/non-null for the first page
     * @return a single result page
     */
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
