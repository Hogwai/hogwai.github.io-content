package com.hogwai.model;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.serde.annotation.Serdeable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

import java.util.Set;

@Setter
@Getter
@Introspected
@Serdeable
@DynamoDbBean
@AllArgsConstructor
public class Post {

    private String id;
    private String subreddit;
    private Long createdUtc;
    private String author;
    private String title;
    private String selfText;
    private String permalink;
    private Set<String> keywords;

    public Post() {
        // Needed with @DynamoDbBean
    }

    @DynamoDbPartitionKey
    public String getSubreddit() { return subreddit; }

    @DynamoDbSortKey
    public String getId() { return id; }
}
