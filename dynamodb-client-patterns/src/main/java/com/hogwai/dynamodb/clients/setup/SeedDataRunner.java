package com.hogwai.dynamodb.clients.setup;

import com.hogwai.dynamodb.clients.model.Movie;
import net.datafaker.Faker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.BatchWriteItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.WriteBatch;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanResponse;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Configuration
public class SeedDataRunner {

    private static final Logger log = LoggerFactory.getLogger(SeedDataRunner.class);
    private static final String TABLE_NAME = "movies";

    @Bean
    @Order(2)
    CommandLineRunner seedData(DynamoDbClient dynamoDbClient, DynamoDbEnhancedClient enhancedClient) {
        return _ -> {
            // Check if data already exists
            ScanResponse scanResponse = dynamoDbClient.scan(ScanRequest.builder()
                    .tableName(TABLE_NAME)
                    .limit(1)
                    .build());
            if (!scanResponse.items().isEmpty()) {
                log.info("Movies already seeded, skipping");
                return;
            }

            // Generate movies using datafaker
            Faker faker = new Faker();
            List<String> genres = List.of("Action", "Comedy", "Drama", "Sci-Fi", "Thriller");
            List<Movie> movies = new ArrayList<>();

            for (int i = 0; i < 200; i++) {
                Movie movie = new Movie();
                movie.setGenre(genres.get(faker.random().nextInt(genres.size())));
                movie.setMovieId(UUID.randomUUID().toString());
                movie.setTitle(faker.book().title());
                movie.setAuthor(faker.book().author());
                movie.setDescription(faker.lorem().paragraph(2));
                Set<String> actorSet = new HashSet<>();
                int actorCount = faker.random().nextInt(2, 5);
                for (int j = 0; j < actorCount; j++) {
                    actorSet.add(faker.name().fullName());
                }
                movie.setActors(actorSet);
                movie.setReleaseYear(faker.random().nextInt(1980, 2025));
                movie.setRating(faker.random().nextDouble() * 5.0);
                movies.add(movie);
            }

            // Batch write using the Enhanced Client
            int batchSize = 25;
            DynamoDbTable<Movie> table = enhancedClient.table(TABLE_NAME, TableSchema.fromBean(Movie.class));
            for (int i = 0; i < movies.size(); i += batchSize) {
                List<Movie> batch = movies.subList(i, Math.min(i + batchSize, movies.size()));
                WriteBatch.Builder<Movie> writeBatchBuilder = WriteBatch.builder(Movie.class)
                        .mappedTableResource(table);
                for (Movie movie : batch) {
                    writeBatchBuilder.addPutItem(movie);
                }
                enhancedClient.batchWriteItem(BatchWriteItemEnhancedRequest.builder()
                        .addWriteBatch(writeBatchBuilder.build())
                        .build());
            }

            log.info("Seeded {} movies", movies.size());
        };
    }
}
