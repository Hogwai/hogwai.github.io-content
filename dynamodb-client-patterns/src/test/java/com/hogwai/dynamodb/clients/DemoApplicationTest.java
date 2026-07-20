package com.hogwai.dynamodb.clients;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeDefinition;
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest;
import software.amazon.awssdk.services.dynamodb.model.DescribeTableRequest;
import software.amazon.awssdk.services.dynamodb.model.DescribeTableResponse;
import software.amazon.awssdk.services.dynamodb.model.GlobalSecondaryIndex;
import software.amazon.awssdk.services.dynamodb.model.KeySchemaElement;
import software.amazon.awssdk.services.dynamodb.model.ListTablesResponse;
import software.amazon.awssdk.services.dynamodb.model.ProjectionType;
import software.amazon.awssdk.services.dynamodb.model.ResourceNotFoundException;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
class DemoApplicationTest {

    @Container
    static GenericContainer<?> dynamoDb = new GenericContainer<>("amazon/dynamodb-local:latest")
            .withExposedPorts(8000);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("aws.dynamodb.endpoint-override",
                () -> "http://localhost:" + dynamoDb.getMappedPort(8000));
    }

    @Autowired
    private DynamoDbClient dynamoDbClient;

    @BeforeEach
    void setUp() {
        createMoviesTableIfNotExists();
    }

    private void createMoviesTableIfNotExists() {
        try {
            DescribeTableResponse describe = dynamoDbClient.describeTable(
                    DescribeTableRequest.builder().tableName("movies").build());
            if (describe.table() != null) {
                return; // table already exists
            }
        } catch (ResourceNotFoundException _) {
            // table does not exist, create it
        }

        dynamoDbClient.createTable(CreateTableRequest.builder()
                .tableName("movies")
                .keySchema(
                        KeySchemaElement.builder()
                                .attributeName("genre").keyType("HASH").build(),
                        KeySchemaElement.builder()
                                .attributeName("movieId").keyType("RANGE").build()
                )
                .attributeDefinitions(
                        AttributeDefinition.builder()
                                .attributeName("genre").attributeType("S").build(),
                        AttributeDefinition.builder()
                                .attributeName("movieId").attributeType("S").build(),
                        AttributeDefinition.builder()
                                .attributeName("author").attributeType("S").build()
                )
                .globalSecondaryIndexes(
                        GlobalSecondaryIndex.builder()
                                .indexName("author-index")
                                .keySchema(
                                        KeySchemaElement.builder()
                                                .attributeName("author").keyType("HASH").build()
                                )
                                .projection(p -> p.projectionType(ProjectionType.ALL))
                                .build()
                )
                .billingMode("PAY_PER_REQUEST")
                .build());
    }

    @Test
    void contextLoads() {
        assertThat(dynamoDbClient).isNotNull();
    }

    @Test
    void connectionWorks() {
        ListTablesResponse response = dynamoDbClient.listTables();
        assertThat(response.tableNames()).isNotNull();
    }

    @Test
    void moviesTableExists() {
        DescribeTableResponse response = dynamoDbClient.describeTable(
                DescribeTableRequest.builder().tableName("movies").build());
        assertThat(response.table()).isNotNull();
        assertThat(response.table().tableName()).isEqualTo("movies");
        assertThat(response.table().keySchema()).hasSize(2);
        assertThat(response.table().globalSecondaryIndexes()).hasSize(1);
        assertThat(response.table().globalSecondaryIndexes().getFirst().indexName())
                .isEqualTo("author-index");
    }
}
