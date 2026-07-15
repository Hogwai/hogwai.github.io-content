# Spring Boot Performance Playground

Two Spring Boot applications side-by-side — one deliberately unoptimized (baseline), one with every performance trick in the book (optimized) — to demonstrate the real-world impact of hardcore Spring Boot optimization.

## Why This Exists

Most Spring Boot optimization advice is fragmented across blog posts, JEPs, and release notes. This project puts everything together in one place so you can see, measure, and compare the techniques in action.

## Techniques Demonstrated

| Technique | Baseline | Optimized | How |
|---|---|---|---|
| Virtual Threads (Loom) | ❌ Platform threads | ✅ Virtual threads | `VirtualThreadConfig` + `spring.threads.virtual.enabled=true` |
| JVM Tuning | ❌ Defaults | ✅ ZGC + pre-touch + compressed OOPs | `-XX:+UseZGC -XX:+AlwaysPreTouch -XX:+UseCompressedOops` |
| Multi-level Caching | ❌ No cache | ✅ Caffeine L1 + Redis L2 | `MultiLevelCache` + `@Cacheable` |
| Zero-Copy I/O | ❌ FileSystemResource | ✅ UrlResource + FileChannel | `FileServiceImpl` (optimized) |
| Structured Concurrency | ❌ Sequential | ✅ `StructuredTaskScope` | `getDashboard()` in `UserServiceImpl` |
| Scoped Values | ❌ ThreadLocal | ✅ `ScopedValue` | `RequestContext` |
| HTTP/2 | ❌ | ✅ Netty + NGINX | `WebServerConfig` + `nginx/default.conf` |
| HikariCP Tuning | ❌ Defaults | ✅ Pool=8, timeout=500ms | `HikariCPConfig` |
| JPA Optimization | ❌ N+1 | ✅ Batch fetching + JOIN FETCH | `spring.jpa.properties.hibernate.jdbc.batch_size=50` |
| gRPC | ❌ | ✅ `spring-boot-starter-grpc-server` + Protocol Buffers | `GrpcUserService`, `GrpcStatsService`, `GrpcProductService` |
| HikariCP Metrics | ❌ None | ✅ Micrometer auto-instrumentation | `HikariCPConfig` + Micrometer `HikariDataSourceMetricsPostProcessor` |
| Graceful Shutdown | ❌ Default (abrupt) | ✅ `server.shutdown=graceful` + `spring.lifecycle.timeout-per-shutdown-phase=30s` | `application.yml` |
| OSIV Disabled | ❌ Enabled (default) | ✅ `spring.jpa.open-in-view=false` | `JpaConfig` |
| Response Compression | ❌ Disabled | ✅ `server.compression.enabled=true` (gzip, min 1KB) | `WebServerConfig` |
| Observability | ❌ None | ✅ Micrometer + Prometheus + JFR | `MetricsConfig` + Actuator |
| Grafana + Prometheus | ❌ None | ✅ Pre-provisioned dashboards | `monitoring/dashboards/` + `monitoring/prometheus.yml` |
| R2DBC / WebFlux | ❌ Not applicable | ✅ Reactive non-blocking stack | `webflux-experiment/` module |

## Project Structure

```
spring-boot-performance-playground/
├── common/                    # Shared entities, DTOs, service interfaces, repositories
│   └── src/main/java/com/hogwai/perf/common/
│       ├── dto/               # UserResponse, StatsResponse, DashboardResponse, ProductResponse
│       ├── model/             # User, Product, Order, OrderItem (JPA entities)
│       ├── service/           # UserService, StatsService, FileService (interfaces)
│       └── repository/        # UserRepository, ProductRepository, OrderRepository
│   └── src/main/proto/        # Protocol Buffers definitions for gRPC services
├── baseline/                  # Unoptimized reference app (Tomcat, N+1, no cache)
│   └── src/main/java/com/hogwai/perf/baseline/
│       ├── config/            # TomcatConfig (limited thread pool)
│       ├── controller/        # UserController, StatsController, FileController, ProductController
│       └── service/           # UserServiceImpl, StatsServiceImpl, FileServiceImpl
├── optimized/                 # Optimized app (virtual threads, caching, gRPC)
│   └── src/main/java/com/hogwai/perf/optimized/
│       ├── cache/             # MultiLevelCache, CacheWarmer
│       ├── concurrent/        # RequestContext (ScopedValue), RequestContextFilter
│       ├── config/            # VirtualThreadConfig, WebServerConfig, HikariCPConfig, JpaConfig
│       ├── controller/        # Same API as baseline, same endpoints
│       ├── grpc/              # gRPC services (GrpcUserService, GrpcStatsService, GrpcProductService)
│       ├── service/           # Optimized implementations with caching + structured concurrency
│       └── observability/     # MetricsConfig (Micrometer)
├── webflux-experiment/        # Reactive WebFlux + R2DBC experiment
│   └── src/main/java/com/hogwai/perf/webflux/
│       ├── controller/        # ProductController (reactive endpoints)
│       ├── model/             # Product (R2DBC entity)
│       └── repository/        # ProductRepository (R2DBC)
├── benchmark/                 # JMH microbenchmarks
│   └── src/main/java/com/hogwai/perf/benchmark/
│       ├── CacheAccessBenchmark.java
│       ├── CacheLayerBenchmark.java
│       ├── VirtualThreadOverheadBenchmark.java
│       ├── StructuredConcurrencyBenchmark.java
│       └── BenchmarkRunner.java
├── k6/                        # Load test scripts
│   ├── config/options.js
│   └── scenarios/             # baseline-smoke, optimized-smoke, ramp-comparison, cache-hit-vs-miss, zero-copy-throughput
├── nginx/                     # HTTP/2 reverse proxy config
├── monitoring/                # Prometheus + Grafana provisioning
│   ├── prometheus.yml         # Prometheus scrape config
│   ├── grafana-datasources.yml
│   └── dashboards/            # Pre-provisioned Grafana dashboards
├── scripts/                   # Benchmark runner utility scripts
│   ├── run-benchmark.sh
│   ├── generate-test-files.sh
├── docker-compose.yml         # PostgreSQL + Redis + Baseline + Optimized + NGINX + Prometheus + Grafana
├── Dockerfile.baseline        # Standard JRE image
├── Dockerfile.optimized       # Multi-stage with ZGC tuning
└── pom.xml                    # Parent POM
```

## Quick Start (Local Dev)

**Prerequisites:** Java 25 (JDK), Maven wrapper (included)

```bash
# Start baseline app (H2 in-memory, port 8080)
./mvnw spring-boot:run -pl baseline

# Start optimized app (H2 in-memory, port 8081)
./mvnw spring-boot:run -pl optimized
```

Then hit the APIs:

```bash
curl http://localhost:8080/api/stats       # Baseline
curl http://localhost:8081/api/stats       # Optimized
curl http://localhost:8081/api/users/1     # Optimized user
curl http://localhost:8081/api/dashboard/1 # Optimized dashboard
```

## Running the Full Stack (Docker Compose)

Full infrastructure including apps, databases, monitoring, and NGINX:

```bash
# Full infrastructure + apps + monitoring
docker compose up -d

# Access:
# - Baseline:       http://localhost:8080/api/stats
# - Optimized:      http://localhost:8081/api/stats
# - NGINX:          https://perf.local/baseline/api/stats (HTTPS with HTTP/2)
# - Grafana:        http://localhost:3000 (admin/perfadmin)
# - Prometheus:     http://localhost:9090

# Stop everything
docker compose down
```

Docker Compose starts:
- **PostgreSQL 17** for persistent data
- **Redis 7** for L2 cache (used by optimized app)
- **Baseline app** on port 8080
- **Optimized app** on port 8081 (with ZGC tuning)
- **NGINX** on port 443 (TLS 1.3 + HTTP/2)
- **Prometheus** on port 9090 (scrapes both apps every 5s)
- **Grafana** on port 3000 (pre-provisioned dashboard, auto-loaded)

## Running JMH Benchmarks

```bash
./mvnw package -pl benchmark
java -jar benchmark/target/benchmarks.jar
```

Or run individual benchmarks:

```bash
java -jar benchmark/target/benchmarks.jar CacheAccess
java -jar benchmark/target/benchmarks.jar CacheLayerBenchmark
java -jar benchmark/target/benchmarks.jar VirtualThread
java -jar benchmark/target/benchmarks.jar StructuredConcurrency
```

The `CacheLayerBenchmark` compares Caffeine L1, Redis L2, and database access latencies side-by-side.

## Running k6 Load Tests

Install [k6](https://k6.io/docs/get-started/installation/) first, then:

```bash
# Smoke test: baseline vs optimized (requires apps running)
k6 run k6/scenarios/baseline-smoke.js
k6 run k6/scenarios/optimized-smoke.js

# Full comparison: both apps simultaneously under ramping load
k6 run k6/scenarios/ramp-comparison.js

# Cache hit ratio test (optimized only)
k6 run k6/scenarios/cache-hit-vs-miss.js

# Zero-copy throughput test (optimized only, requires test files)
bash scripts/generate-test-files.sh
k6 run k6/scenarios/zero-copy-throughput.js

# Full automated benchmark suite
bash scripts/run-benchmark.sh
```

## JVM Flags Reference

| Flag | Purpose |
|---|---|
| `-XX:+UseZGC` | Ultra-low-pause garbage collector (<1ms typical) |
| `-XX:MaxGCPauseMillis=100` | Target max GC pause |
| `-Xms2G -Xmx2G` | Fixed heap (no resize overhead) |
| `-XX:+AlwaysPreTouch` | Pre-zero and commit heap at startup (faster at runtime) |
| `-XX:+UseCompressedOops` | 32-bit object references (less memory, better cache) |
| `-XX:+UseZGC` | Z Garbage Collector for low latency |
| `--enable-preview` | Enable preview features (needed for some APIs) |

## Build Profiles

| Profile | Command | Description |
|---|---|---|
| dev (default) | `./mvnw spring-boot:run` | H2 in-memory, no optimization |
| docker | Set via `SPRING_PROFILES_ACTIVE=docker` | PostgreSQL + Redis |

## Grafana Dashboards

The project includes a pre-provisioned Grafana instance with dashboards for both baseline and optimized apps. Dashboards are auto-loaded on startup via the provisioning directory at `monitoring/dashboards/`.

- **URL:** http://localhost:3000
- **Credentials:** `admin` / `perfadmin`
- **Default dashboard:** Spring Boot Overview (JVM metrics, request rates, cache hit ratios)

### Dashboard Highlights

| Panel | Source | What it shows |
|---|---|---|
| JVM Heap / GC | Micrometer + Prometheus | ZGC pause times, heap usage, GC cycles |
| Request Rate | `http.server.requests` | RPS, latency distribution (p50/p95/p99) |
| Thread State | `jvm.threads.*` | Virtual vs platform thread counts |
| Cache Hit Ratio | `cache.gets.{hit,miss}` | Caffeine L1 + Redis L2 hit rates |
| Connection Pool | `hikaricp.connections.*` | Active/idle/pending connections |

## R2DBC / WebFlux Experiment

An experimental module demonstrating Spring WebFlux with R2DBC for a fully reactive, non-blocking alternative to the traditional servlet stack:

```bash
# Start the reactive app
./mvnw spring-boot:run -pl webflux-experiment

# Test endpoints (port 8082)
curl http://localhost:8082/api/products
curl http://localhost:8082/api/products/1
```

- Uses Spring WebFlux (Netty) instead of Spring MVC (Tomcat)
- R2DBC for non-blocking database access instead of JPA/Hibernate
- Fully reactive end-to-end: controller → repository → database
- Same PostgreSQL database, different access pattern

## Results Template

| Metric | Baseline | Optimized | Improvement |
|---|---|---|---|
| p50 response time | __ms | __ms | __x |
| p95 response time | __ms | __ms | __x |
| p99 response time | __ms | __ms | __x |
| Max throughput (req/s) | __ | __ | __x |
| Startup time | __s | __s | __x |
| Memory (RSS) | __MB | __MB | __x |
| Cache hit ratio (L1) | N/A | __% | — |
| Cache hit ratio (L2) | N/A | __% | — |

Fill in results after running `scripts/run-benchmark.sh` and the JMH benchmarks.

## Architecture (ASCII)

```
┌─────────────────────────────────────────────────────────────┐
│                         NGINX (443)                         │
│                     TLS 1.3 + HTTP/2                        │
│              /baseline/* → baseline:8080                    │
│             /optimized/* → optimized:8081                   │
└──────┬──────────────────────────────┬───────────────────────┘
       │                              │
       ▼                              ▼
┌──────────────┐            ┌──────────────────┐
│  BASELINE    │            │   OPTIMIZED      │
│  Tomcat      │            │  Netty + HTTP/2  │
│  Thread-per  │            │  Virtual Threads │
│  No cache    │            │  Structured Conc │
│  N+1 queries │            │  Scoped Values   │
│  :8080       │            │  L1+L2 Cache   │
└──────┬───────┘            └────┬─────────────┘
       │                         │
       ▼                         ▼
┌──────────────────────────────────────────────┐
│              PostgreSQL (5432)                │
│              perfdb / perfuser               │
└──────────────────────┬───────────────────────┘
                       │
                       ▼
              ┌──────────────┐
              │   Redis (7)  │
              │  L2 cache    │
              └──────────────┘
```

## Prerequisites

- **Java 25 JDK** (e.g., Eclipse Temurin)
- **Docker** (for full-stack Compose setup)
- **k6** (for load testing)


## Benchmark Results

| Scenario | Latency | Notes |
|---|---|---|
| Baseline REST | ~57ms avg | Tomcat thread-pool bound, N+1 queries |
| Optimized REST | ~9ms avg | Virtual threads, Redis cache, JPA batching |
| Optimized gRPC | ~3ms avg | Binary protocol, HTTP/2, same backend optimizations |
| WebFlux R2DBC | (profile) | Reactive non-blocking alternative |
| L1 Cache (Caffeine) | ~0.05ms | Local JVM, no network |
| L2 Cache (Redis) | ~0.3ms | Network round-trip |
| DB (PostgreSQL) | ~1ms | Full query execution |
