# Plan : Les différents mappers de résultats dans Hibernate ORM

## Article — objectif

Montrer les **9 RowTransformer implementations** de Hibernate ORM : comment chaque type de requête JPQL/SQL natif produit un résultat Java différent, et comment Hibernate choisit le bon transformer.

---

## Structure de l'article

### 1. Pipeline interne — ResultSet → Objet Java

Le flux en 5 étapes :

```
SQL ResultSet
  → RowProcessingState (valeurs JDBC brutes)
    → DomainResultAssembler[] (extraction Java par colonne)
      → Object[] row (tableau de valeurs assemblées)
        → RowTransformer<T>.transformRow(Object[] row) → T
          → ResultsConsumer (collecte en List ou ScrollableResults)
```

- Le SPI `RowTransformer<T>` : 2 méthodes
  - `T transformRow(Object[] row)` — la transformation
  - `int determineNumberOfResultElements(int rawElementCount)` — combien d'éléments dans le résultat
- Point clé : les Assemblers produisent un `Object[]` uniforme, c'est le Transformer qui décide de la forme finale

### 2. Logique de décision — quel transformer est choisi ?

L'arbre dans `ConcreteSqmSelectQueryPlan.determineRowTransformer()` :

```
TupleTransformer user-set ?
├── OUI → RowTransformerTupleTransformerAdapter
└── NON
    ├── Pas de resultClass → StandardImpl
    └── resultClass spécifié
        ├── 1 seul élément sélectionné
        │   ├── Sélection = resultType → SingularReturnImpl
        │   ├── resultType = array → ArrayImpl
        │   ├── resultType = List.class → ListImpl
        │   ├── resultType = Tuple.class → JpaTupleImpl
        │   ├── resultType = Map.class → MapImpl
        │   ├── resultType = classe/record → ConstructorImpl
        │   │   └── InstantiationException → CheckingImpl (fallback)
        │   └── sinon → CheckingImpl
        └── Multi-sélection
            ├── resultType = array → ArrayImpl
            ├── resultType = List.class → ListImpl
            ├── resultType = Tuple.class → JpaTupleImpl
            ├── resultType = Map.class → MapImpl
            ├── resultType = classe/record → ConstructorImpl
            └── sinon → QueryTypeMismatchException
```

### 3. Les 8 transformers — un par sous-section

Chaque sous-section : description + requête JPQL + code utilisateur + résultat attendu.

| # | Transformer | Transforme | Requête JPQL | Type résultat |
|---|-------------|------------|--------------|---------------|
| 3.1 | `SingularReturnImpl` | `Object[]` → `row[0]` | `SELECT b FROM Book b` | `Book.class` |
| 3.2 | `ConstructorImpl` | `Object[]` → `constructor.newInstance(row)` | `SELECT new BookSummaryDTO(b.title, b.author.name) FROM Book b` | `BookSummaryDTO.class` |
| 3.3 | `JpaTupleImpl` | `Object[]` → `Tuple` | `SELECT b.title, b.pages FROM Book b` | `Tuple.class` |
| 3.4 | `MapImpl` | `Object[]` → `Map<String,Object>` | `SELECT b.title AS title, b.pages AS pages FROM Book b` | `Map.class` |
| 3.5 | `ArrayImpl` | `Object[]` → `Object[]` (identité) | `SELECT b.title, b.pages FROM Book b` | `Object[].class` |
| 3.6 | `ListImpl` | `Object[]` → `List.of(row)` | `SELECT b.title, b.pages FROM Book b` | `List.class` |
| 3.7 | `TupleTransformerAdapter` | `Object[]` → delegate user `TupleTransformer` | `.setTupleTransformer((tuple, aliases) -> ...)` | custom |
| 3.8 | `StandardImpl` + `CheckingImpl` | Fallback | Pas de type ou type non instanciable | `Object.class` |

### 4. Native SQL

- Les `NativeQuery*Transformer` (Tuple, Map, List, Array)
- Différence d'alias : lowercased en natif vs preserved en JPQL
- `@SqlResultSetMapping` annoté vs `setTupleTransformer` programmatique
- Exemple avec `createNativeQuery(..., Book.class)` → passe par `NativeQueryTupleTransformer`

### 5. Perf & optimisations

- L'optimisation singleton dans `StandardRowReader` : 3 transformers (Standard, SingularReturn, Array) sont mis à `null` quand possible → pas de call de méthode
- Les 6 autres transformers sont instanciés à chaque requête
- `RowTransformerConstructorImpl` réutilise le `Constructor` réflexif (pas de lookup à chaque row)
- Impact négligeable en pratique — le vrai cost est le SQL + le network, pas le transformer

---

## Projet demo — `row-transformers`

### Structure

```
hibernate-row-transformers/
├── pom.xml                          (Spring Boot 4.1, Testcontainers, PostgreSQL 18, JUnit 5)
├── mvnw
├── README.md
└── src/
    ├── main/
    │   ├── java/com/hogwai/rowtransformers/
    │   │   ├── RowTransformersApplication.java
    │   │   ├── entity/
    │   │   │   ├── Book.java
    │   │   │   └── Author.java
    │   │   └── dto/
    │   │       ├── BookSummaryDTO.java
    │   │       └── BookStatsDTO.java
    │   └── resources/
    │       ├── application.properties
    │       └── data.sql
    └── test/java/com/hogwai/rowtransformers/
        ├── RowTransformersApplicationTest.java
        └── transformer/
            ├── SingularReturnTest.java
            ├── ConstructorTest.java
            ├── TupleTest.java
            ├── MapTest.java
            ├── ArrayTest.java
            ├── ListTest.java
            ├── CustomTransformerTest.java
            ├── StandardFallbackTest.java
            └── NativeSqlTest.java
```

### Entités

**`Author`**
```java
@Entity
public class Author {
    @Id @GeneratedValue
    private Long id;
    private String name;
    private String nationality;
    // getters, setters
}
```

**`Book`**
```java
@Entity
public class Book {
    @Id @GeneratedValue
    private Long id;
    private String title;
    private int pages;
    @ManyToOne
    private Author author;
    // getters, setters
}
```

### DTOs / Records

**`BookSummaryDTO`** — pour ConstructorImpl
```java
public record BookSummaryDTO(String title, String authorName) {}
```

**`BookStatsDTO`** — pour ConstructorImpl (stats agrégées)
```java
public record BookStatsDTO(String title, int pageCount) {}
```

### Données initiales (`data.sql`)

```sql
INSERT INTO author (id, name, nationality) VALUES (1, 'Tolkien', 'British');
INSERT INTO author (id, name, nationality) VALUES (2, 'Asimov', 'American');
INSERT INTO book (id, title, pages, author_id) VALUES (1, 'The Hobbit', 310, 1);
INSERT INTO book (id, title, pages, author_id) VALUES (2, 'Foundation', 255, 2);
INSERT INTO book (id, title, pages, author_id) VALUES (3, 'The Lord of the Rings', 1178, 1);
```

### Tests — un par transformer

Chaque test utilise `@DataJpaTest` + Testcontainers (PostgreSQL 18) et vérifie le type de retour + le contenu.

**Configuration Testcontainers :**
- `@TestConfiguration` avec un bean `PostgreSQLContainer<?>` statique et réutilisable
- `@DynamicPropertySource` pour injecter l'URL/JDBC dynamiquement
- Image : `postgres:18`
- Docker requis pour lancer les tests

| Test | Transformer | Ce qu'on vérifie |
|------|-------------|------------------|
| `SingularReturnTest` | `SingularReturnImpl` | `query.getResultList()` retourne `List<Book>`, premier élément est `instanceof Book` |
| `ConstructorTest` | `ConstructorImpl` | `SELECT new BookSummaryDTO(...)` retourne `List<BookSummaryDTO>`, champs corrects |
| `TupleTest` | `JpaTupleImpl` | `.getResultList()` retourne `List<Tuple>`, `tuple.get(0)` = title, `tuple.get("title")` = title |
| `MapTest` | `MapImpl` | `.getResultList()` retourne `List<Map>`, clés = alias (title, pages) |
| `ArrayTest` | `ArrayImpl` | `.getResultList()` retourne `List<Object[]>`, `array[0]` = title, `array[1]` = pages |
| `ListTest` | `ListImpl` | `.getResultList()` retourne `List<List>`, `list.get(0)` = title |
| `CustomTransformerTest` | `TupleTransformerAdapter` | `.setTupleTransformer(...)` retourne des objets custom |
| `StandardFallbackTest` | `StandardImpl` | Pas de type → retourne `Object[]` |
| `NativeSqlTest` | Native SQL | `createNativeQuery(sql, Book.class)` retourne `List<Book>` |

### Application properties

```properties
# Testcontainers injecte l'URL dynamiquement via @DynamicPropertySource
# Ces props sont pour le profile dev/demo hors tests
spring.datasource.url=jdbc:postgresql://localhost:5432/hibernate_row_transformers
spring.datasource.username=postgres
spring.datasource.password=postgres
spring.jpa.hibernate.ddl-auto=create-drop
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
```

### Testcontainers config (dans chaque test ou abstract base)

```java
@TestConfiguration
static class TestcontainersConfig {
    static final PostgreSQLContainer<?> postgres =
        new PostgreSQLContainer<>("postgres:18")
            .withDatabaseName("hibernate_row_transformers")
            .withUsername("test")
            .withPassword("test");

    static {
        postgres.start();
    }

    @Bean
    DataSource dataSource() {
        HikariDataSource ds = new HikariDataSource();
        ds.setJdbcUrl(postgres.getJdbcUrl());
        ds.setUsername(postgres.getUsername());
        ds.setPassword(postgres.getPassword());
        return ds;
    }
}
```

### Dépendances pom.xml (ajouts clés)

```xml
<!-- Spring Boot Data JPA -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>

<!-- PostgreSQL driver -->
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <scope>runtime</scope>
</dependency>

<!-- Testcontainers -->
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>postgresql</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>junit-jupiter</artifactId>
    <scope>test</scope>
</dependency>

<!-- HikariCP (included via spring-boot-starter-data-jpa) -->
```

---

## Validation

- [ ] `./mvnw test` passe tous les tests
- [ ] Chaque test montre le même résultat obtenu via une approche différente
- [ ] Le README contient un tableau récapitulatif des transformers
- [ ] Docker requis (Testcontainers + PostgreSQL 18)
- [ ] Compatible Java 21+ (comme les autres projets du repo)
