# DynamoDB Client Patterns

A Spring Boot demo showing good vs bad usage of native DynamoDB clients (low-level `DynamoDbClient` and Enhanced Client `DynamoDbEnhancedClient`). 
Each pattern exposes two endpoints: naive (costly) and good (efficient), with metrics logged via SLF4J.

## Prerequisites

- Java 25
- Docker (for DynamoDB Local via Testcontainers)

## Quick Start

```sh
# Run all tests (16 tests, starts DynamoDB Local automatically)
./mvnw test

# Start the application (requires DynamoDB Local running on port 8000)
docker compose up -d

./mvnw spring-boot:run
```

## Tests

16 tests: 3 unit (Movie model) + 13 integration (DynamoDB Local via Testcontainers):

| Test class               | Type        | What it covers                                    |
|--------------------------|-------------|---------------------------------------------------|
| `MovieTest`              | Unit        | Round-trip toItemMap/fromItem, null safety        |
| `DemoApplicationTest`    | Integration | Context load, DynamoDB connection, table creation |
| `PatternIntegrationTest` | Integration | All 10 controller endpoints with real RCU metrics |

Run: `./mvnw test` (Docker required)

The app seeds 200 movies on first startup and the table is created automatically.

## The 10 Patterns

### Count

| Endpoint                                 | Method                 | Description                          |
|------------------------------------------|------------------------|--------------------------------------|
| `GET /api/count?genre=Action&naive=true` | full query + `.size()` | Fetches all items, counts in memory  |
| `GET /api/count?genre=Action`            | `Select.COUNT`         | Returns only the count from DynamoDB |

Note: Select.COUNT does not reduce RCU consumption. For partitions with more than 1 MB of data, pagination (ExclusiveStartKey) is needed to sum counts across pages.

### Condition

| Endpoint                                         | Method                | Description                         |
|--------------------------------------------------|-----------------------|-------------------------------------|
| `PUT /api/movies/unsafe?genre=Action&movieId=m1` | GetItem + PutItem     | Two requests, race condition window |
| `PUT /api/movies` (body: Movie JSON)             | `ConditionExpression` | Atomic create-if-not-exists         |

### Batch

| Endpoint                                 | Method           | Description                  |
|------------------------------------------|------------------|------------------------------|
| `POST /api/batch/write/naive?count=10`   | N x PutItem      | One request per item         |
| `POST /api/batch/write/good?count=10`    | `BatchWriteItem` | Single request for all items |
| `GET /api/batch/read/naive?ids=m1,m2,m3` | N x GetItem      | One request per key          |
| `GET /api/batch/read/good?ids=m1,m2,m3`  | `BatchGetItem`   | Single request for all keys  |

Note: BatchWriteItem is limited to 25 items / 16 MB total. The response may contain UnprocessedItems (throttled) that require retry. BatchWriteItem is not atomic and does not support condition expressions.

### Projection

| Endpoint                                                | Method                 | Description                   |
|---------------------------------------------------------|------------------------|-------------------------------|
| `GET /api/movies?genre=Action`                          | Full item              | Returns all attributes        |
| `GET /api/movies?genre=Action&fields=title,releaseYear` | `ProjectionExpression` | Returns only requested fields |

Note: ProjectionExpression reduces network transfer and client-side memory, but does NOT reduce RCU. DynamoDB bills reads on the full item size before projection (up to 4 KB per read capacity unit).

### GSI vs Filter

| Endpoint                                                   | Method             | Description                              |
|------------------------------------------------------------|--------------------|------------------------------------------|
| `GET /api/movies/by-author?author=Alice+Chen&genre=Action` | `FilterExpression` | Scans entire partition before filtering  |
| `GET /api/movies/by-author?author=Alice+Chen`              | GSI query          | Direct index lookup, only matching items |

### Pagination

| Endpoint                                            | Method                        | Description        |
|-----------------------------------------------------|-------------------------------|--------------------|
| `GET /api/movies/paged?genre=Action&page=0`         | Unbounded query               | All items at once  |
| `GET /api/movies/paged?genre=Action&page=1&size=10` | `Limit` + `ExclusiveStartKey` | One page at a time |

### Scan vs Query

| Endpoint                             | Method  | Description                   |
|--------------------------------------|---------|-------------------------------|
| `GET /api/movies/scan`               | `Scan`  | Reads every item in the table |
| `GET /api/movies/query?genre=Action` | `Query` | Reads only one partition      |

### 8. TTL (Time-to-Live)

| Endpoint                                      | Method                | Description                         |
|-----------------------------------------------|-----------------------|-------------------------------------|
| `POST /api/ttl/write/no-ttl?count=5`          | Write without TTL     | Items accumulate forever            |
| `POST /api/ttl/write?count=5&ttlSeconds=3600` | Write with `expireAt` | Items auto-deleted after TTL expiry |

Note: TTL deletion is eventual. DynamoDB typically removes expired items within 48 hours. DynamoDB Local does not auto-delete expired items (testable via Streams or manual verification of the expireAt attribute).

### Optimistic Locking

| Endpoint                                                                 | Method                       | Description                             |
|--------------------------------------------------------------------------|------------------------------|-----------------------------------------|
| `PUT /api/lock/bad?genre=Action&movieId=m1&title=New`                    | Unconditional update         | Lost update risk, no conflict detection |
| `PUT /api/lock/good?genre=Action&movieId=m1&title=New&expectedVersion=1` | Version condition expression | Atomic update with conflict detection   |

### Transactions

| Endpoint                                   | Method               | Description                 |
|--------------------------------------------|----------------------|-----------------------------|
| `POST /api/transaction/write/bad?count=5`  | Individual putItem   | Partial success possible    |
| `POST /api/transaction/write/good?count=5` | `TransactWriteItems` | All-or-nothing atomic write |

## Per-pattern comparison

Values measured against DynamoDB Local with the application's seed data (200 movies, 5 genres).

| #  | Pattern           | Naive approach                         | Cost (measured)                         | Good approach                        | Cost (measured)                          | Impact                                          |
|----|-------------------|----------------------------------------|-----------------------------------------|--------------------------------------|------------------------------------------|-------------------------------------------------|
| 1  | **Count**         | `Query` + `.size()`                    | 0.5 RCU, 3 items transferred            | `Select.COUNT`                       | 0.5 RCU, count only, no items            | Eliminates data transfer for count-only queries. Note: requires pagination for partitions >1 MB. |
| 2  | **Condition**     | `GetItem` + `PutItem`                  | 2 RT, 0.5 RCU + 1 WCU                   | `PutItem` with `ConditionExpression` | 1 RT, 2.0 WCU                            | Half the round-trips, no race condition         |
| 3  | **Batch write**   | 3x `PutItem`                           | 3 RT                                    | `BatchWriteItem`                     | 1 RT, 10.0 WCU, 5 items                  | Single round-trip, up to 25 items / 16 MB max  |
| 3  | **Batch read**    | 2x `GetItem`                           | 2 RT                                    | `BatchGetItem`                       | 1 RT                                     | Single round-trip for multiple keys             |
| 4  | **Projection**    | Full item (8 attributes)               | ~209 B/item                             | `ProjectionExpression` (2 fields)    | ~41 B/item                               | 80% less data transferred (network only, not RCU) |
| 5  | **GSI vs Filter** | Partition `Query` + `FilterExpression` | scanned 2 items, 0.5 RCU                | GSI `author-index` query             | 3 matching items, 0.5 RCU                | Same RCU, way less data transferred             |
| 6  | **Pagination**    | Unbounded query                        | 12 items all in memory at once          | `Limit` + `ExclusiveStartKey`        | 5 items/page, `hasMore` flag             | Bounded memory, cursor-based                    |
| 7  | **Scan vs Query** | Full table `Scan`                      | 3 items all partitions, 0.5 RCU         | Partition `Query`                    | 2 items, single partition, 0.5 RCU       | Targeted read vs full scan                      |
| 8  | **TTL**           | Write without TTL                      | 3 items, manual cleanup required        | Write with `expireAt`                | 3 items, DynamoDB auto-cleanup (eventual)| No custom cleanup code needed (48h window)     |
| 9  | **Locking**       | Unconditional update                   | Lost update risk, no conflict detection | Update with version condition        | Atomic update, version conflict detected | Prevents lost concurrent updates                |
| 10 | **Transactions**  | Individual putItem calls               | 5/5 items written, no atomicity         | `TransactWriteItems`                 | 5 items atomically written               | ACID across multiple items                      |

> **Note:** DynamoDB Local returns simulated capacity. Real AWS values differ, but the relative ratios between naive and good approaches are representative.

## Project Structure

```
src/main/java/com/hogwai/dynamodb/clients/
├── DynamoDbClientPatternsApplication.java
├── config/
│   └── DynamoDbConfig.java
├── model/
│   └── Movie.java               # @DynamoDbBean with toItemMap/fromItem
├── repository/
│   ├── MovieEnhancedRepository.java  # Enhanced Client abstraction
│   └── MovieRawRepository.java       # Raw DynamoDbClient
├── service/
│   └── MovieService.java         # All 16+ pattern methods + metrics
├── controller/                   # One controller per pattern (10 total)
│   ├── CountController.java
│   ├── ConditionController.java
│   ├── BatchController.java
│   ├── ProjectionController.java
│   ├── GsiVsFilterController.java
│   ├── PaginationController.java
│   ├── ScanController.java
│   ├── TtlController.java
│   ├── LockingController.java
│   └── TransactionController.java
└── setup/
    ├── TableInitializer.java     # Creates movies table + enables TTL on startup
    └── SeedDataRunner.java       # Seeds 200 movies with datafaker
```

## Tech Stack

- **Java 25** with Maven wrapper
- **Spring Boot 4.1.0**
- **AWS SDK v2** (dynamodb 2.31.26, dynamodb-enhanced, url-connection-client)
- **Testcontainers** (DynamoDB Local)
- **datafaker** (seed data generation)
