# Kafka Synchronous Messaging, Pattern Showcase

A multi-microservice showcase of two approaches to **synchronous request/reply over Apache Kafka** in Spring Boot:

| Approach | Technique | Automatic Correlation |
|----------|-----------|----------------------|
| **Custom** | `CompletableFuture` + `ConcurrentHashMap` | Application-level (by `requestId` in the message body) |
| **ReplyingKafkaTemplate** | Spring Kafka's built-in `ReplyingKafkaTemplate` | Header-level (by `KafkaHeaders.CORRELATION_ID`) |

## Why sync over Kafka?

Kafka is fundamentally asynchronous, producers don't wait for consumers. But real-world workflows often need a synchronous-looking call: *send a request on topic A, block until the result arrives on topic B, and continue with the response.*

Common use cases:
- Cross-service document processing (send a file for processing, wait for the result)
- Orchestration workflows (step A finishes → triggers step B, awaiting confirmation)
- Legacy system integration where the calling code expects a blocking RPC-style API

## Project structure

```
kafka-synchronous-messaging/
├── pom.xml                              # Parent Maven POM (multi-module)
├── docker-compose.yml                   # Run Kafka for manual testing
├── kafka-model/
│   └── src/main/java/.../model/
│       ├── Request.java                 # Request record (requestId, payload, timestamp)
│       └── Response.java                # Response record (requestId, payload, status, processingTimeMs)
├── kafka-sender/
│   └── src/main/java/.../sender/
│       ├── config/KafkaConfig.java      # Kafka beans, ObjectMapper with JavaTimeModule
│       ├── gateway/custom/
│       │   ├── CustomSyncGateway.java   # Hand-made sync Kafka sender
│       │   └── CustomReplyListener.java # Listens for responses, completes futures
│       ├── gateway/replying/
│       │   └── ReplyingSyncGateway.java # ReplyingKafkaTemplate-based sender
│       └── controller/SyncController.java  # POST /api/sync?payload=X&mode=custom|replying
└── kafka-processor/
    └── src/main/java/.../processor/
        ├── config/KafkaConfig.java      # Kafka beans for the processor
        └── service/RequestProcessor.java# @KafkaListener that processes and replies
```

## The two approaches

### 1. Custom: CompletableFuture + ConcurrentHashMap

```java
// Sender side
CompletableFuture<Response> future = new CompletableFuture<>();
pendingFutures.put(request.requestId(), future);    // 1. store future
kafkaTemplate.send(topic, request);                  // 2. send request
return future.get(timeout, SECONDS);                 // 3. block

// Receiver side (@KafkaListener)
CompletableFuture<Response> future = pendingFutures.remove(response.requestId());
if (future != null) future.complete(response);       // 4. complete matching future
```

**Flow:**

```
Sender                    Kafka                      Remote Service
  │                        │                            │
  ├── put(requestId, CF) ──┤                            │
  ├── send(request) ───────┼──────────────────────────► │
  │   .get(timeout) ─┐     │    (results topic)          │
  │                  │     │                             ├── process request
  │◄──────────── complete ◄─┼────────────────────────────┘
  │                        │
```

**What you manage manually:**
- `ConcurrentHashMap` to correlate requests by ID
- `CompletableFuture` lifecycle (create, store, complete on reply, timeout cleanup)
- A separate `@KafkaListener` for the reply topic

**Pros:** Full control, no framework magic, any reply topic shape works.
**Cons:** Boilerplate, error-prone (leaked futures on timeout, race conditions if not careful).

### 2. Spring Kafka: ReplyingKafkaTemplate

```java
ProducerRecord<String, Request> record = new ProducerRecord<>(topic, request);

RequestReplyFuture<String, Request, Response> future =
    replyingTemplate.sendAndReceive(record, Duration.ofSeconds(timeout));

ConsumerRecord<String, Response> response = future.get(timeout, TimeUnit.SECONDS);
return response.value();
```

**Flow:**

```
Sender                               Kafka                       Remote Service
  │                                   │                             │
  ├── sendAndReceive(record) ────────┼───────────────────────────► │
  │   ├── adds REPLY_TOPIC header     │                             ├── reads REPLY_TOPIC header
  │   ├── adds CORRELATION_ID header  │                             ├── copies CORRELATION_ID
  │   └── starts reply consumer      │                             └── sends reply
  │                                   │                             │
  │   .get(timeout) ─┐               │    (reply topic)             │
  │◄──────────────────────────────────┼─────────────────────────────┘
  │                  │               │
  │ template correlates by            │
  │ CORRELATION_ID header             │
```

**What is handled automatically:**
- `REPLY_TOPIC` and `CORRELATION_ID` headers are set by the template
- A dedicated reply consumer subscribes to the reply topic
- Incoming replies are correlated by header and matched to the right future
- No manual `ConcurrentHashMap`, no custom `@KafkaListener`

**Pros:** Clean, framework-native, less code, header-based correlation is robust.
**Cons:** Requires the remote service to honor `REPLY_TOPIC`/`CORRELATION_ID` headers.

## Running the tests

```bash
mvn test
```

The integration tests use **Testcontainers** with `confluentinc/cp-kafka:7.7.1`, Docker is required.

To run a single module:
```bash
mvn test -pl kafka-sender -am
mvn test -pl kafka-processor -am
```

| Module | Tests | Description |
|--------|-------|-------------|
| kafka-sender | 3 | Custom, Replying, Both concurrently |
| kafka-processor | 4 | Custom + Replying patterns |

## Running with Docker Compose

```bash
docker compose up -d                        # Start Kafka + Zookeeper
mvn package -pl kafka-sender -am -DskipTests  # Build sender
mvn package -pl kafka-processor -am -DskipTests  # Build processor
docker compose build                        # Build Docker images
docker compose up                           # Run everything
```

Then:
```bash
# Custom pattern
curl -X POST "http://localhost:8080/api/sync?payload=hello&mode=custom"

# ReplyingKafkaTemplate pattern
curl -X POST "http://localhost:8080/api/sync?payload=world&mode=replying"
```

### Failure scenario (DLQ)

When a request payload starts with `fail`, the processor simulates a failure.
The original `Request` is sent to the `sync-requests-dlq` topic, and a `FAILURE`
response is sent back immediately (instead of the sender timing out):

```bash
curl -s -X POST "http://localhost:8080/api/sync?payload=fail-test&mode=custom" | jq .
# → { "requestId": "...", "payload": "Simulated failure for: fail-test", "status": "FAILURE", "processingTimeMs": 0 }
```

## Topics

| Topic | Purpose |
|-------|---------|
| `sync-requests` | Incoming requests (sender → processor) |
| `sync-results` | Responses for custom pattern |
| `sync-replies` | Responses for replying pattern |
| `sync-requests-dlq` | processor → DLQ (external consumer not implemented). Failed `Request` messages |

## Origin

This showcase presents two approaches to synchronous Kafka request/reply messaging, extracted from a production codebase where the custom pattern was used to send document processing requests and await results synchronously.
