# Alternative Projections

A showcase of JPA projections alternatives in Spring Boot.

## Projects

| Project | Library | Key Concept |
|---|---|---|
| `jdbctemplate-projections` | JdbcTemplate | JDBC with RowMapper |
| `spring-data-jdbc-projections` | Spring Data JDBC | Spring Data without JPA |
| `jdbi-projections` | JDBI | Lightweight SQL with SQL Object API |
| `querydsl-projections` | QueryDSL SQL | Type-safe SQL DSL |
| `blaze-persistence-projections` | Blaze-Persistence | Entity Views on top of JPA |
| `jooq-projections` | jOOQ | Type-safe SQL with code generation |

## Prerequisites

- Java 25
- Docker (for Testcontainers)
- Maven (or use `./mvnw` wrapper)

## Running a Project

```bash
cd jdbctemplate-projections
./mvnw spring-boot:run
```

## Running Tests

```bash
cd jdbctemplate-projections
./mvnw test
```

## API Endpoints

| Endpoint | Description |
|---|---|
| `GET /api/movies/{id}` | Movie by ID (lightweight projection) |
| `GET /api/movies/{id}/full` | Movie with actors (where applicable) |
| `GET /api/movies?genre=...&year=...` | Dynamic query with filters |
| `GET /api/movies/stats` | Aggregation by genre |