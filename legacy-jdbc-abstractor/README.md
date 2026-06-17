# legacy-jdbc-abstractor

JDBC abstraction patterns using only core Java 8+ features.

The project demonstrates 5 techniques for reducing JDBC boilerplate by composing
`RowMapper`, `ParamBinder`, and `JdbcExecutor` with lambdas and method references,
all within a single compile-time dependency (JSR 331 `javax.sql.DataSource`).

## Prerequisites

- JDK 8 or later.
- Apache Maven 3.6+ or later.

## Project Structure

```
legacy-jdbc-abstractor/
├── pom.xml
└── src/
    ├── main/
    │   └── java/
    │       └── com/
    │           └── hogwai/
    │               └── jdbcabstractor/
    │                   ├── DemoApplication.java          # Runnable demo
    │                   ├── ItemService.java              # 5 JDBC patterns
    │                   ├── dto/
    │                   │   ├── Item.java                 # Result DTO
    │                   │   └── ItemCriteria.java         # Query criteria
    │                   └── persistence/
    │                       ├── JdbcExecutor.java         # Lambda-based executor
    │                       ├── RowMapper.java            # Row mapping interface
    │                       └── SqlUtil.java              # SQL utilities
    └── test/
        └── java/
            └── com/
                └── hogwai/
                    └── jdbcabstractor/
                        ├── ItemServiceTest.java
                        └── persistence/
                            ├── JdbcExecutorTest.java
                            └── SqlUtilTest.java
```

## How to Run the Demo

```bash
git clone https://github.com/Hogwai/hogwai.github.io-content.git
cd legacy-jdbc-abstractor
mvn clean compile exec:java -Dexec.mainClass="com.hogwai.jdbcabstractor.DemoApplication"
```

Or from your IDE: open the project, navigate to `DemoApplication.java`, and run its `main` method.

Expected output:
```
=== Pattern 1: Helper utilities ===
  ITEM-001 | Laptop | $1200.00 | Electronics
  ITEM-002 | Mouse | $25.00 | Electronics
  ITEM-003 | Keyboard | $80.00 | Electronics

=== Pattern 2: Lambda execution ===
  ...
```

All five patterns produce identical results for the same input.

## The 5 Patterns

### 1. Helper utilities

Manual connection acquisition, `SqlUtil` for named-parameter SQL construction,
index-tracked parameter binding, and row mapping via a private method.
Establishes the baseline boilerplate that the remaining patterns reduce.

### 2. Lambda execution

Introduces `JdbcExecutor` which owns the `PreparedStatement` and `ResultSet`
lifecycle via try-with-resources. The call site supplies a `SimpleBinder` lambda
for parameter binding and a `ResultProcessor` lambda for row mapping,
the executor handles resource cleanup.

### 3. Method references

Binder and processor extracted as `static` methods (`bindParams`, `mapResults`).
The call site reads as pure declaration: query, binding strategy, mapping strategy.
Demonstrates how method references (`ItemService::bindParams`) can replace
inline lambdas for readability.

### 4. Fluent ParamBinder

`ParamBinder` wraps `PreparedStatement` with a fluent API that auto-tracks the
parameter index: `.setList(codes).setString(categoryCode)`. Eliminates the
manual `int idx = 1; ps.setString(idx++, ...)` pattern while staying in core Java.

### 5. RowMapper + DataSource-level

`RowMapper<T>` maps a single result row. `JdbcExecutor.toList()` wraps it into a
`ResultProcessor`. The `DataSource`-level `executeQuery` manages the connection
lifecycle, the call site provides only the query, binding, and mapping.
`Item.MAPPER` is a public constant, making the mapping contract visible and reusable.

## Running the Tests

```bash
mvn test
```

## Key Takeaways

1. Lambdas and try-with-resources eliminate JDBC boilerplate : functional
   interfaces (`RowMapper`, `ResultProcessor`) separate resource management from
   business logic without a framework.
2. Small abstractions compound: `ParamBinder` solves index tracking,
   `RowMapper` solves row mapping; together they eliminate entire categories of
   repetitive code.
3. No ORM required: For projects where Hibernate/JPA is not available or
   desirable, this pattern provides most of the ergonomic benefit using only
   `java.sql` and `javax.sql`.
