# Database-Driven Workflow Engine

A proof-of-concept Spring Boot application demonstrating a database-driven workflow engine built on Spring Batch. Instead of hardcoding step sequences in Java config, the pipeline routing is defined in the database and resolved at runtime by a `JobExecutionDecider`.

## Architecture

```
                  ┌──────────────────────────┐
                  │   ChainConfiguration     │
                  │   Controller (REST)      │
                  └──────────┬───────────────┘
                             │ POST /invoke?config=X&orderId=Y
                             ▼
                  ┌──────────────────────────┐
                  │   ConfigurableChain      │
                  │   (Spring Batch Job)     │
                  └──────────┬───────────────┘
                             │ start
                             ▼
                  ┌──────────────────────────┐
                  │ ChainInformationTasklet  │
                  │ (logs config metadata)   │
                  └──────────┬───────────────┘
                             │ next
                             ▼
                  ┌──────────────────────────┐
                  │   ChainStepDecider       │◄────────────┐
                  │   (reads DB for next     │             │
                  │    step based on result) │             │
                  └──────────┬───────────────┘             │
                             │ route to step               │
                             ▼                             │
                  ┌──────────────────────────┐             │
                  │ OrderProcessingTasklet   │──success───►┤
                  │ (realistic order step)   │──failure───►┤
                  └──────────────────────────┘             │
                                                           │
                  (continues until nextStepOnSuccess=null) │
```

## Domain Model

- **Chain**: logical workflow definition (e.g., ORDER_PROCESSING)
- **Step**: reusable processing unit (e.g., validateOrder, processPayment)
- **ChainConfiguration**: named pipeline variant with its own step ordering
- **ChainStep**: step binding within a config, with `nextStepOnSuccess` / `nextStepOnFailure` routing
- **ChainStatus**: reference status (ACTIVE, SUSPENDED, ENABLED)

## Pre-Seeded Pipelines

| Config | Steps | Notes |
|---|---|---|
| **standard-order** | 8 | validateOrder → checkInventory → processPayment → calculateTax → fulfillOrder → sendConfirmation → updateAccounting → archiveOrder |
| **premium-order** | 9 | Adds applyDiscount after processPayment |
| **flagged-order** | 4 | Short path: validateOrder → checkInventory → escalateOrder → archiveOrder |

## Tech Stack

- Java 25
- Spring Boot 3.5.11
- Spring Batch 5.2.4
- Spring Data JPA / Hibernate
- MapStruct 1.6.3
- H2 (default) / PostgreSQL
- SpringDoc / Swagger

## Running

### Profile 1: H2 (default, zero setup)

```bash
./mvnw spring-boot:run
```

### Profile 2: PostgreSQL

Start a PostgreSQL container:

```bash
docker run -d --name pg-workflow \
  -e POSTGRES_DB=workflow_engine \
  -e POSTGRES_USER=sa \
  -e POSTGRES_PASSWORD=sa \
  -p 5432:5432 postgres:17
```

Run the app:

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=postgresql
```

### Profile 3: Docker (PostgreSQL)

```bash
docker compose up --build
```

This builds the app image and starts both containers (app + PostgreSQL). The app is available at `http://localhost:8080`.

To stop:
```bash
docker compose down
```

To stop and delete the database volume:
```bash
docker compose down -v
```

## API Endpoints

All endpoints are under `/chain-config`.

### List available steps
```bash
curl http://localhost:8080/chain-config/steps
```

### Get pipeline configuration
```bash
curl http://localhost:8080/chain-config/standard-order
curl http://localhost:8080/chain-config/premium-order
curl http://localhost:8080/chain-config/flagged-order
```

### Invoke a pipeline
```bash
curl "http://localhost:8080/chain-config/invoke?config=standard-order&orderId=ORD-001"
curl "http://localhost:8080/chain-config/invoke?config=premium-order&orderId=ORD-002"
curl "http://localhost:8080/chain-config/invoke?config=flagged-order&orderId=ORD-003"
```

### Create a new configuration
```bash
curl -X POST "http://localhost:8080/chain-config/create" \
  -H "Content-Type: application/json" \
  -d '{
    "chainName": "ORDER_PROCESSING",
    "chainConfName": "express-order",
    "chainConfDescription": "Express order with 2-day shipping",
    "chainStepRecords": [
      {"stepName": "validateOrder", "nextStepOnSuccess": "checkInventory", "nextStepOnFailure": "escalateOrder"},
      {"stepName": "checkInventory", "nextStepOnSuccess": "processPayment", "nextStepOnFailure": "escalateOrder"},
      {"stepName": "processPayment", "nextStepOnSuccess": "fulfillOrder", "nextStepOnFailure": "escalateOrder"},
      {"stepName": "fulfillOrder", "nextStepOnSuccess": "archiveOrder", "nextStepOnFailure": "escalateOrder"},
      {"stepName": "archiveOrder", "nextStepOnSuccess": null, "nextStepOnFailure": null}
    ]
  }'
```

### Update an existing configuration
```bash
curl -X PUT "http://localhost:8080/chain-config/update" \
  -H "Content-Type: application/json" \
  -d '{"chainConfName": "standard-order", ...}'
```

Swagger UI is available at `http://localhost:8080/`.

## How It Works

1. A `ChainStepDecider` (implementing `JobExecutionDecider`) queries the database after each step execution to determine the next step.
2. The routing is driven by `nextStepOnSuccess` and `nextStepOnFailure` columns in `ts_chain_step`.
3. When the decider returns a step name matching one of the 10 pre-registered Spring Batch steps, the job routes to that step.
4. When no next step is configured (`NULL`), the job ends with `COMPLETED`.
5. Each `OrderProcessingTasklet` execution simulates real work (~300ms processing + logging).

## Key Files

```
src/main/java/com/hogwai/
├── config/
│   ├── BatchConfig.java                          # Thread pool executor
│   └── ConfigurableChainConfig.java              # Job + step bean wiring
├── controller/
│   └── ChainConfigurationController.java         # REST endpoints
├── decider/
│   └── ChainStepDecider.java                     # Runtime step routing
├── dto/                                          # Record DTOs
├── entity/                                       # JPA entities
├── enums/                                        # StepEnum (10 steps)
├── listener/
│   └── ProcessCompletionListener.java            # Job lifecycle logging
├── mapper/
│   └── ChainConfigurationMapper.java             # MapStruct mapper
├── repository/                                   # Spring Data JPA repos
├── service/                                      # Business logic
└── tasklet/
    ├── ChainInformationTasklet.java              # Config info logging
    └── OrderProcessingTasklet.java               # Realistic order steps
```

## Tests

```bash
./mvnw test
```

The `ChainStepDeciderTest` covers 4 scenarios:
- Successful step → next step on success
- Failed step → next step on failure
- Unknown step name → FAILED
- Null step execution → routes to first configured step
