package com.hogwai.dynamodb.clients.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.retry.RetryMode;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.http.crt.AwsCrtHttpClient;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.net.URI;
import java.time.Duration;

@Configuration
public class DynamoDbConfig {

    private static final Logger log = LoggerFactory.getLogger(DynamoDbConfig.class);

    @Bean
    public SdkHttpClient sdkHttpClient(
            @Value("${aws.dynamodb.http-client:url-connection}") String httpClientType,
            @Value("${aws.dynamodb.connection-timeout:5000}") int connectionTimeout,
            @Value("${aws.dynamodb.socket-timeout:10000}") int socketTimeout) {

        log.info("Configuring DynamoDB HTTP client: type={}, connectionTimeout={}ms, socketTimeout={}ms",
                httpClientType, connectionTimeout, socketTimeout);

        return switch (httpClientType) {
            case "apache" -> ApacheHttpClient.builder()
                    .connectionTimeout(Duration.ofMillis(connectionTimeout))
                    .socketTimeout(Duration.ofMillis(socketTimeout))
                    .build();
            case "crt" -> AwsCrtHttpClient.builder()
                    .connectionTimeout(Duration.ofMillis(connectionTimeout))
                    // NOTE: AwsCrtHttpClient does not expose socketTimeout;
                    // connection timeout is the primary tuning knob.
                    .build();
            case "url-connection" -> UrlConnectionHttpClient.builder()
                    .connectionTimeout(Duration.ofMillis(connectionTimeout))
                    .socketTimeout(Duration.ofMillis(socketTimeout))
                    .build();
            default -> {
                log.warn("Unknown http-client type '{}', falling back to UrlConnectionHttpClient", httpClientType);
                yield UrlConnectionHttpClient.builder()
                        .connectionTimeout(Duration.ofMillis(connectionTimeout))
                        .socketTimeout(Duration.ofMillis(socketTimeout))
                        .build();
            }
        };
    }

    @Bean
    public DynamoDbClient dynamoDbClient(
            SdkHttpClient sdkHttpClient,
            @Value("${aws.dynamodb.endpoint-override}") String endpoint,
            @Value("${aws.dynamodb.region}") String region) {
        return DynamoDbClient.builder()
                .endpointOverride(URI.create(endpoint))
                .region(Region.of(region))
                .httpClient(sdkHttpClient)
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create("dummy", "dummy")))
                .overrideConfiguration(c -> c
                        .apiCallTimeout(Duration.ofSeconds(20))
                        .apiCallAttemptTimeout(Duration.ofSeconds(5))
                        .retryStrategy(RetryMode.STANDARD))
                .build();
    }

    @Bean
    public DynamoDbEnhancedClient dynamoDbEnhancedClient(DynamoDbClient dynamoDbClient) {
        return DynamoDbEnhancedClient.builder()
                .dynamoDbClient(dynamoDbClient)
                .build();
    }
}
