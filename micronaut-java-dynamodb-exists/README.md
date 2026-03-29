# DynamoDB Lightweight "Exists" Patterns (Java SDK v2)

Efficient patterns for checking item existence in Amazon DynamoDB without retrieving full item payloads. Emulates SQL-like `EXISTS` behavior using the **AWS SDK for Java v2**.

Companion project for the article: [Replicating the SQL exists statement behavior for DynamoDB](https://hogwai.github.io/posts/replicating-exists-statement-for-java-dynamodb/)

## Prerequisites

* Java 21+
* Micronaut 4.x
* AWS SDK for Java v2 (`software.amazon.awssdk:dynamodb`)
* Docker (for local DynamoDB)

## Strategies Implemented

### 1. `existsByProjection`

Uses `GetItem` with a `ProjectionExpression` to return only the partition key attribute instead of the whole item. This reduces network bandwidth and deserialization cost, but does **not** reduce RCU consumption because DynamoDB reads the full item from disk before applying the projection.

```java
GetItemRequest request = GetItemRequest.builder()
        .tableName(POSTS)
        .key(key)
        .projectionExpression(SUBREDDIT)
        .build();

GetItemResponse response = dynamoDbClient.getItem(request);
return response.hasItem();
```

### 2. `existsByGetItem`

Standard `GetItem` without projection. Serves as a baseline for comparison with the other strategies.

### 3. `batchExists`

Uses `BatchGetItem` with projection to check up to 100 items in a single request. Missing items are simply omitted from the response rather than returned as `null`. When DynamoDB is under load, the response may be partial: the implementation handles `UnprocessedKeys` via a retry loop with exponential backoff.

```java
Map<String, Boolean> result = new HashMap<>();
ids.forEach(id -> result.put(id, false));

int attempts = 0;

do {
    attempts++;
    BatchGetItemResponse response = dynamoDbClient.batchGetItem(request);

    var foundItems = response.responses().getOrDefault(POSTS, List.of());
    foundItems.forEach(item -> result.put(item.get(ID).s(), true));

    if (response.hasUnprocessedKeys() && !response.unprocessedKeys().isEmpty()) {
        request = request.toBuilder()
                         .requestItems(response.unprocessedKeys())
                         .build();
        backoff(attempts);
    } else {
        break;
    }
} while (attempts < 5);
```

### 4. `hasKeywordsByGetItem`

Uses `GetItem` to fetch only the `keywords` attribute and checks whether it exists and contains data. Sets `consistentRead(false)` to halve the RCU cost.

```java
GetItemRequest request = GetItemRequest.builder()
        .tableName(POSTS)
        .key(buildKey(subreddit, id))
        .projectionExpression(KEYWORDS)
        .consistentRead(false)
        .build();

GetItemResponse response = dynamoDbClient.getItem(request);

if (!response.hasItem()) return false;

AttributeValue keywords = response.item().get(KEYWORDS);
return keywords != null && keywords.hasL() && !keywords.l().isEmpty();
```

### 5. `hasKeywords`

Alternative to `hasKeywordsByGetItem` using a `Query` with a `FilterExpression` (`size(keywords) > 0`). More flexible for complex conditions, but the filter is applied **after** the read, so RCU consumption is the same.

```java
QueryRequest request = QueryRequest.builder()
        .tableName(POSTS)
        .keyConditionExpression("subreddit = :subVal AND id = :idVal")
        .filterExpression("size(keywords) > :zero")
        .expressionAttributeValues(values)
        .projectionExpression("id")
        .limit(1)
        .build();

QueryResponse response = dynamoDbClient.query(request);
return response.count() > 0;
```

### 6. `hasPostsForSubreddit`

Uses a `Query` with `Limit(1)` to check if any item exists in a given partition. This avoids reading the full list of items for that partition key.

```java
QueryRequest request = QueryRequest.builder()
        .tableName(POSTS)
        .keyConditionExpression("subreddit = :subVal")
        .expressionAttributeValues(values)
        .projectionExpression(SUBREDDIT)
        .limit(1)
        .build();

QueryResponse response = dynamoDbClient.query(request);
return response.count() > 0;
```

## Performance Cheatsheet

| Technique                | Saves Bandwidth? |   Saves RCU?   | Best For                                    |
|:-------------------------|:----------------:|:--------------:|:------------------------------------------  |
| **ProjectionExpression** |       Yes        |       No       | Large items, reducing network latency.      |
| **Eventual Consistency** |        No        | **Yes (-50%)** | Existence checks where delay is acceptable. |
| **Keys-Only GSI**        |       Yes        | **Yes (-90%)** | Heavy existence checks on very large items. |

## Running Locally

1. Start DynamoDB Local:

```bash
docker compose up -d
```

2. Create the table:

```bash
./create-table.sh
```

3. Run the application:

```bash
./gradlew run
```

The application starts on port `8082`. Set `app.data-generator.enabled=true` in `application.properties` to generate sample data on startup.

## API Endpoints

| Method | Path                                             | Description                               |
|:-------|:-------------------------------------------------|:------------------------------------------|
| `GET`  | `/post/projection/{subreddit}/exists?id=`        | Existence check with projection           |
| `GET`  | `/post/get-item/{subreddit}/exists?id=`          | Existence check with full GetItem         |
| `POST` | `/post/batch-exists/{subreddit}`                 | Batch existence check (body: list of ids) |
| `GET`  | `/post/projection/{subreddit}/has-posts`         | Check if subreddit has any posts          |
| `GET`  | `/post/{id}/has-keywords?subreddit=`             | Check keywords via Query                  |
| `GET`  | `/post/{id}/has-keywords-by-get-item?subreddit=` | Check keywords via GetItem                |
