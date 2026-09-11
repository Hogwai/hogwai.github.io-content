# nosql-projection-patterns

Demonstrates lightweight data retrieval patterns across NoSQL databases (MongoDB, Cassandra). Each database has its own mechanism for selecting only the needed fields. 1000 movies are generated on startup for realistic pagination and aggregation demos.

## Prerequisites

- Java 25
- Docker

## Quick Start

```bash
# Start databases
docker compose up -d

# Run MongoDB module (port 8080)
./mvnw spring-boot:run -pl mongodb

# Run Cassandra module (port 8081)
./mvnw spring-boot:run -pl cassandra
```

## Projection Patterns Comparison

| Pattern | MongoDB | Cassandra |
|---|---|---|
| Interface closed projection | Auto-narrowing `{ 'title': 1 }` | Explicit `SELECT id, title, genre` |
| @Query field selection | `fields` attribute in `@Query` | CQL `SELECT` clause |
| Aggregation | `$project` in `@Aggregation` | CQL `GROUP BY` + `COUNT` |
| Dynamic projection | `<T> List<T> findByGenre(Class<T>)` | N/A (no auto-narrowing) |
| Embedded docs | Native nested documents | Denormalized actor IDs |
| Pagination | `Slice<T>` / `Page<T>` | `Slice<T>` + `CassandraPageRequest` |
| Indexing | `@Indexed` on `genre` | Secondary index on `genre`, `title` |

## API Endpoints

### MongoDB (port 8080)

| Endpoint | Description |
|---|---|
| `GET /api/mongo/movies?genre=` | Interface closed projection (id, title, genre) |
| `GET /api/mongo/movies/search?title=` | @Query fields (id, title, releaseYear) |
| `GET /api/mongo/movies/stats/genre` | Aggregation $project |
| `GET /api/mongo/movies/dynamic?genre=&type=MovieTitleView\|MovieDetailDto` | Dynamic projection |
| `GET /api/mongo/movies/paged/slice?genre=&page=&size=` | Slice pagination (no count) |
| `GET /api/mongo/movies/paged/page?genre=&page=&size=` | Page pagination (with total count) |
| `GET /api/mongo/movies/{id}/detail` | Full detail with embedded actors |

### Cassandra (port 8081)

| Endpoint | Description |
|---|---|
| `GET /api/cassandra/movies?genre=` | CQL SELECT projection (id, title, genre) |
| `GET /api/cassandra/movies/search?title=` | CQL search (id, title, release_year) |
| `GET /api/cassandra/movies/stats/genre` | CQL GROUP BY + COUNT aggregation |
| `GET /api/cassandra/movies/paged?genre=&size=` | Slice pagination with CassandraPageRequest |
| `GET /api/cassandra/movies/{id}/detail` | Full detail (separate actor query) |

## Architecture

Each module follows Controller -> Service -> Repository pattern.

```
mongodb/                     cassandra/
  MongoApplication             CassandraApplication
  service/
    MongoProjectionsService      CassandraProjectionsService
  controller/
    MongoProjectionsController   CassandraProjectionsController
  repository/
    MovieRepository              MovieRepository
  model/
    Movie (@Document)            Movie (@Table)
    Actor (@Document)            Actor (@Table)
  projection/
    MovieTitleView               MovieTitleView
    MovieDetailDto               MovieDetailDto
                                GenreStat
```

## Key Differences (Blog Post Angle)

| Aspect | MongoDB | Cassandra |
|---|---|---|
| Auto field narrowing | Yes (interface projections) | No (must SELECT explicitly) |
| @Query field selection | Yes (`fields` attribute) | CQL SELECT clause |
| Aggregation | `$project` in `@Aggregation` | CQL aggregates |
| Embedded documents | Native support | Denormalize or separate table |
| Dynamic projection | `<T> List<T> findBy(Class<T>)` | Same API but manual SELECT |
| Performance impact | Reduces BSON deserialization | Reduces SSTable I/O |
| Pagination | Spring Data Pageable | CassandraPageRequest (keyset) |

## Testing

```bash
# MongoDB tests
./mvnw test -pl mongodb

# Cassandra tests
./mvnw test -pl cassandra
```
