package com.hogwai.dynamodb.clients.setup;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

@Configuration
public class TableInitializer {

    private static final Logger log = LoggerFactory.getLogger(TableInitializer.class);
    public static final String TABLE_NAME = "movies";

    @Bean
    @Order(1)
    CommandLineRunner initializeTable(DynamoDbClient dynamoDbClient) {
        return args -> {
            try {
                dynamoDbClient.describeTable(DescribeTableRequest.builder().tableName(TABLE_NAME).build());
                log.info("Table '{}' already exists, skipping creation", TABLE_NAME);
                return;
            } catch (ResourceNotFoundException _) {
                // table does not exist, create it
            }

            dynamoDbClient.createTable(CreateTableRequest.builder()
                    .tableName(TABLE_NAME)
                    .keySchema(
                            KeySchemaElement.builder().attributeName("genre").keyType(KeyType.HASH).build(),
                            KeySchemaElement.builder().attributeName("movieId").keyType(KeyType.RANGE).build()
                    )
                    .attributeDefinitions(
                            AttributeDefinition.builder().attributeName("genre").attributeType(ScalarAttributeType.S).build(),
                            AttributeDefinition.builder().attributeName("movieId").attributeType(ScalarAttributeType.S).build(),
                            AttributeDefinition.builder().attributeName("author").attributeType(ScalarAttributeType.S).build()
                    )
                    .globalSecondaryIndexes(
                            GlobalSecondaryIndex.builder()
                                    .indexName("author-index")
                                    .keySchema(
                                            KeySchemaElement.builder().attributeName("author").keyType(KeyType.HASH).build()
                                    )
                                    .projection(Projection.builder().projectionType(ProjectionType.ALL).build())
                                    .build()
                    )
                    .billingMode(BillingMode.PAY_PER_REQUEST)
                    .build());

            dynamoDbClient.waiter().waitUntilTableExists(DescribeTableRequest.builder()
                    .tableName(TABLE_NAME)
                    .build());

            log.info("Table '{}' created and ready", TABLE_NAME);

            try {
                dynamoDbClient.updateTimeToLive(UpdateTimeToLiveRequest.builder()
                        .tableName(TABLE_NAME)
                        .timeToLiveSpecification(_ -> TimeToLiveSpecification.builder()
                                .attributeName("expireAt")
                                .enabled(true)
                                .build())
                        .build());
                log.info("TTL enabled on table '{}' with attribute 'expireAt'", TABLE_NAME);
            } catch (Exception e) {
                log.warn("Could not enable TTL (may already be enabled): {}", e.getMessage());
            }
        };
    }
}
