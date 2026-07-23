package com.hogwai.dynamodb.clients.demo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.retry.RetryMode;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.http.crt.AwsCrtHttpClient;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.ReturnConsumedCapacity;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanResponse;

import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
@Profile("client-demo")
public class ClientComparisonRunner {

    private static final Logger log = LoggerFactory.getLogger(ClientComparisonRunner.class);
    private static final String TABLE_NAME = "movies";
    public static final String DUMMY = "dummy";

    @Bean
    @Order(3)
    CommandLineRunner compareClients() {
        return _ -> {
            final int WARMUP = 20;
            final int MEASURE = 1000;

            log.info("============================================");
            log.info("  DynamoDB HTTP Client Comparison Demo");
            log.info("============================================");
            log.info("Methodology: {} items, {} warmup (discarded), single measurement run", MEASURE, WARMUP);
            log.info("");

            /*
             Phase 0: Scan the table to retrieve existing movie keys
             */
            List<Map<String, AttributeValue>> allItems = scanAllMovies();

            log.info("Retrieved {} movie keys from table '{}'", allItems.size(), TABLE_NAME);
            log.info("");

            if (allItems.size() < 200) {
                log.warn("Need at least 200 movies but found {}. Run SeedDataRunner first.", allItems.size());
                return;
            }

            // Use exactly the first 200 keys, cycle through them for MEASURE iterations
            List<Map.Entry<String, String>> movieKeys = allItems.stream()
                    .limit(200)
                    .map(item -> Map.entry(
                            item.get("genre").s(),
                            item.get("movieId").s()))
                    .toList();

            /*
            Build 3 HTTP clients (raw instantiation, not Spring beans)
             */
            SdkHttpClient urlConnectionClient = UrlConnectionHttpClient.builder()
                    .connectionTimeout(Duration.ofMillis(5000))
                    .socketTimeout(Duration.ofMillis(10000))
                    .build();

            SdkHttpClient apacheClient = ApacheHttpClient.builder()
                    .connectionTimeout(Duration.ofMillis(5000))
                    .socketTimeout(Duration.ofMillis(10000))
                    .build();

            SdkHttpClient crtClient = AwsCrtHttpClient.builder()
                    .connectionTimeout(Duration.ofMillis(5000))
                    .build();

            long urlMs;
            long apacheMs;
            long crtMs;
            double urlRcuTotal;

            try (DynamoDbClient urlDdbClient = buildDynamoDbClient(urlConnectionClient);
                 DynamoDbClient apacheDdbClient = buildDynamoDbClient(apacheClient);
                 DynamoDbClient crtDdbClient = buildDynamoDbClient(crtClient)) {

                /*
                Scenario A: URL Connection (One TCP connection per request, no pooling)
                 */
                log.info("Scenario A: UrlConnectionHttpClient (sequential baseline)");
                ScenarioResult urlResult = runSequential(urlDdbClient, movieKeys, WARMUP, MEASURE, "url connection");
                urlMs = urlResult.elapsedMs();
                urlRcuTotal = urlResult.totalRcu();

                /*
                Scenario B: Apache (Connection pooling reduces latency on repeated sequential calls)
                 */
                log.info("Scenario B: ApacheHttpClient (sequential pooled)");
                ScenarioResult apacheResult = runSequential(apacheDdbClient, movieKeys, WARMUP, MEASURE, "apache");
                apacheMs = apacheResult.elapsedMs();

                /*
                Scenario C: CRT (Non-blocking I/O + virtual threads crush concurrent workloads)
                 */
                log.info("Scenario C: AwsCrtHttpClient (concurrent non blocking)");
                ScenarioResult crtResult = runConcurrent(crtDdbClient, movieKeys, WARMUP, MEASURE, "crt");
                crtMs = crtResult.elapsedMs();

                // Recap
                double apacheFactor = (double) urlMs / apacheMs;
                double crtFactor = (double) urlMs / crtMs;

                log.info("[METRIC] comparison recap:");
                log.info("[METRIC]   url connection: {}ms ({}ms/item), baseline, simple, zero deps",
                        urlMs, "%.1f".formatted((double) urlMs / MEASURE));
                log.info("[METRIC]   apache: {}ms ({}ms/item), {}x faster with connection pooling",
                        apacheMs, "%.1f".formatted((double) apacheMs / MEASURE), String.format("%.1f", apacheFactor));
                log.info("[METRIC]   crt: {}ms ({}ms/item), {}x faster with non blocking I/O",
                        crtMs, "%.1f".formatted((double) crtMs / MEASURE), String.format("%.1f", crtFactor));
                log.info("[METRIC]   same work, same RCU: {}, different HTTP transport",
                        String.format("%.1f", urlRcuTotal));

            }
        };
    }

    /**
     * Scans the movies table to retrieve all items (used for discovering real keys).
     */
    private static List<Map<String, AttributeValue>> scanAllMovies() {
        SdkHttpClient scanHttpClient = UrlConnectionHttpClient.builder()
                .connectionTimeout(Duration.ofMillis(5000))
                .socketTimeout(Duration.ofMillis(10000))
                .build();

        try (DynamoDbClient scanClient = DynamoDbClient.builder()
                .endpointOverride(URI.create("http://localhost:8000"))
                .region(Region.of("eu-west-3"))
                .httpClient(scanHttpClient)
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(DUMMY, DUMMY)))
                .overrideConfiguration(c -> c
                        .apiCallTimeout(Duration.ofSeconds(20))
                        .apiCallAttemptTimeout(Duration.ofSeconds(5))
                        .retryStrategy(RetryMode.STANDARD))
                .build()) {
            List<Map<String, AttributeValue>> allItems = new ArrayList<>();
            Map<String, AttributeValue> exclusiveStartKey = null;

            do {
                ScanRequest.Builder scanBuilder = ScanRequest.builder()
                        .tableName(TABLE_NAME)
                        .limit(100);

                if (exclusiveStartKey != null && !exclusiveStartKey.isEmpty()) {
                    scanBuilder.exclusiveStartKey(exclusiveStartKey);
                }

                ScanResponse scanResult = scanClient.scan(scanBuilder.build());
                allItems.addAll(scanResult.items());
                exclusiveStartKey = scanResult.lastEvaluatedKey();
            } while (exclusiveStartKey != null && !exclusiveStartKey.isEmpty());

            return allItems;
        }
    }

    /**
     * Builds a DynamoDbClient with the given HTTP client, pointing to DynamoDB Local.
     */
    private static DynamoDbClient buildDynamoDbClient(SdkHttpClient httpClient) {
        return DynamoDbClient.builder()
                .endpointOverride(URI.create("http://localhost:8000"))
                .region(Region.of("eu-west-3"))
                .httpClient(httpClient)
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(DUMMY, DUMMY)))
                .overrideConfiguration(c -> c
                        .apiCallTimeout(Duration.ofSeconds(20))
                        .apiCallAttemptTimeout(Duration.ofSeconds(5))
                        .retryStrategy(RetryMode.STANDARD))
                .build();
    }

    /**
     * Builds a GetItemRequest for the movies table with consumed capacity requested.
     */
    private static GetItemRequest buildGetRequest(String genre, String movieId) {
        return GetItemRequest.builder()
                .tableName(TABLE_NAME)
                .key(Map.of(
                        "genre", AttributeValue.builder().s(genre).build(),
                        "movieId", AttributeValue.builder().s(movieId).build()))
                .returnConsumedCapacity(ReturnConsumedCapacity.TOTAL)
                .build();
    }

    /**
     * Extracts consumed read capacity units from a GetItem response.
     * DynamoDB Local may return null consumed capacity, hence the guard.
     */
    private static double extractRcu(GetItemResponse response) {
        return response.consumedCapacity() != null ? response.consumedCapacity().capacityUnits() : 0.0;
    }

    private record ScenarioResult(long elapsedMs, double totalRcu) {}

    private static ScenarioResult runSequential(DynamoDbClient client, List<Map.Entry<String, String>> keys,
                                                 int warmup, int measure, String label) {
        for (int i = 0; i < warmup; i++) {
            Map.Entry<String, String> key = keys.get(i % keys.size());
            client.getItem(buildGetRequest(key.getKey(), key.getValue()));
        }

        double totalRcu = 0.0;
        long start = System.currentTimeMillis();
        for (int i = 0; i < measure; i++) {
            Map.Entry<String, String> key = keys.get(i % keys.size());
            GetItemResponse response = client.getItem(buildGetRequest(key.getKey(), key.getValue()));
            totalRcu += extractRcu(response);
        }
        long elapsed = System.currentTimeMillis() - start;

        log.info("[METRIC] {} sequential: {} items in {}ms ({}ms/item), {} RCU total",
                label, measure, elapsed, "%.1f".formatted((double) elapsed / measure), "%.1f".formatted(totalRcu));
        log.info("");

        return new ScenarioResult(elapsed, totalRcu);
    }

    private static ScenarioResult runConcurrent(DynamoDbClient client, List<Map.Entry<String, String>> keys,
                                                 int warmup, int measure, String label) {
        try (ExecutorService warmupExec = Executors.newVirtualThreadPerTaskExecutor()) {
            List<CompletableFuture<Void>> warmupFutures = new ArrayList<>();
            for (int i = 0; i < warmup; i++) {
                Map.Entry<String, String> key = keys.get(i % keys.size());
                warmupFutures.add(CompletableFuture.supplyAsync(() -> {
                    client.getItem(buildGetRequest(key.getKey(), key.getValue()));
                    return null;
                }, warmupExec));
            }
            CompletableFuture.allOf(warmupFutures.toArray(new CompletableFuture[0])).join();
        }

        double totalRcu;
        long elapsed;
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<CompletableFuture<GetItemResponse>> futures = new ArrayList<>();
            long start = System.currentTimeMillis();
            for (int i = 0; i < measure; i++) {
                Map.Entry<String, String> key = keys.get(i % keys.size());
                futures.add(CompletableFuture.supplyAsync(() ->
                        client.getItem(buildGetRequest(key.getKey(), key.getValue())), executor));
            }
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
            elapsed = System.currentTimeMillis() - start;
            totalRcu = futures.stream()
                    .mapToDouble(f -> extractRcu(f.join()))
                    .sum();
        }

        log.info("[METRIC] {} concurrent: {} items in {}ms ({}ms/item), {} RCU total",
                label, measure, elapsed, "%.1f".formatted((double) elapsed / measure), "%.1f".formatted(totalRcu));
        log.info("");

        return new ScenarioResult(elapsed, totalRcu);
    }
}
