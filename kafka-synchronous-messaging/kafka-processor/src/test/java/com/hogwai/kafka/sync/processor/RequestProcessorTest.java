package com.hogwai.kafka.sync.processor;

import com.hogwai.kafka.sync.model.Request;
import com.hogwai.kafka.sync.model.Response;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

@Testcontainers
@SpringBootTest
@Timeout(value = 30, unit = TimeUnit.SECONDS)
class RequestProcessorTest {

    @Container
    static final KafkaContainer kafka = new KafkaContainer(
            DockerImageName.parse("apache/kafka:3.8.0")
    );

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    }

    @Test
    @DisplayName("Custom pattern: request → sync-results response")
    void customPattern_shouldRespondOnResultsTopic() throws Exception {
        var request = new Request("Processor-Test-1");
        kafkaTemplate.send("sync-requests", request.requestId(), request);

        Response response = pollResponse("sync-results", request.requestId(), 15);

        assertAll(
                () -> assertThat(response.requestId()).isEqualTo(request.requestId()),
                () -> assertThat(response.status()).isEqualTo("SUCCESS"),
                () -> assertThat(response.payload()).contains(request.payload()),
                () -> assertThat(response.processingTimeMs()).isPositive()
        );
    }

    @Test
    @DisplayName("Replying pattern: request with REPLY_TOPIC header → response on reply topic")
    void replyingPattern_shouldRespondOnReplyTopic() throws Exception {
        var request = new Request("Processor-Reply-1");
        var replyTopic = "sync-replies";

        var record = new ProducerRecord<String, Object>("sync-requests", request.requestId(), request);
        record.headers().add(new RecordHeader(KafkaHeaders.REPLY_TOPIC, replyTopic.getBytes(StandardCharsets.UTF_8)));
        record.headers().add(new RecordHeader(KafkaHeaders.CORRELATION_ID, request.requestId().getBytes(StandardCharsets.UTF_8)));
        kafkaTemplate.send(record);

        Response response = pollResponse(replyTopic, request.requestId(), 15);

        assertAll(
                () -> assertThat(response.requestId()).isEqualTo(request.requestId()),
                () -> assertThat(response.status()).isEqualTo("SUCCESS"),
                () -> assertThat(response.payload()).contains(request.payload()),
                () -> assertThat(response.processingTimeMs()).isPositive()
        );
    }

    @Test
    @DisplayName("Custom pattern failure: failed request → DLQ + FAILURE response")
    void customPatternFailure_shouldSendToDlqAndReturnFailure() throws Exception {
        var request = new Request("fail-custom-test");
        kafkaTemplate.send("sync-requests", request.requestId(), request);

        Response response = pollResponse("sync-results", request.requestId(), 15);
        Request dlqRequest = pollRequest("sync-requests-dlq", request.requestId(), 5);

        assertAll(
                () -> assertThat(response.requestId()).isEqualTo(request.requestId()),
                () -> assertThat(response.status()).isEqualTo("FAILURE"),
                () -> assertThat(response.payload()).contains("Simulated failure"),
                () -> assertThat(response.processingTimeMs()).isZero(),
                () -> assertThat(dlqRequest.requestId()).isEqualTo(request.requestId()),
                () -> assertThat(dlqRequest.payload()).isEqualTo(request.payload())
        );
    }

    @Test
    @DisplayName("Replying pattern failure: failed request → DLQ + FAILURE response on reply topic")
    void replyingPatternFailure_shouldSendToDlqAndReturnFailure() throws Exception {
        var request = new Request("fail-reply-test");
        var replyTopic = "sync-replies";

        var record = new ProducerRecord<String, Object>("sync-requests", request.requestId(), request);
        record.headers().add(new RecordHeader(KafkaHeaders.REPLY_TOPIC, replyTopic.getBytes(StandardCharsets.UTF_8)));
        record.headers().add(new RecordHeader(KafkaHeaders.CORRELATION_ID, request.requestId().getBytes(StandardCharsets.UTF_8)));
        kafkaTemplate.send(record);

        Response response = pollResponse(replyTopic, request.requestId(), 15);
        Request dlqRequest = pollRequest("sync-requests-dlq", request.requestId(), 5);

        assertAll(
                () -> assertThat(response.requestId()).isEqualTo(request.requestId()),
                () -> assertThat(response.status()).isEqualTo("FAILURE"),
                () -> assertThat(response.payload()).contains("Simulated failure"),
                () -> assertThat(response.processingTimeMs()).isZero(),
                () -> assertThat(dlqRequest.requestId()).isEqualTo(request.requestId()),
                () -> assertThat(dlqRequest.payload()).isEqualTo(request.payload())
        );
    }

    private Response pollResponse(String topic, String requestId, int timeoutSeconds) throws Exception {
        var future = new CompletableFuture<Response>();

        try (var consumer = createResponseConsumer()) {
            consumer.subscribe(List.of(topic));
            long deadline = System.currentTimeMillis() + (timeoutSeconds * 1000L);

            while (System.currentTimeMillis() < deadline && !future.isDone()) {
                var records = consumer.poll(Duration.ofSeconds(1));
                for (var record : records) {
                    var response = record.value();
                    if (response != null && requestId.equals(response.requestId())) {
                        future.complete(response);
                        return future.get(1, TimeUnit.SECONDS);
                    }
                }
            }
        }

        throw new AssertionError("No response received for requestId: " + requestId
                + " within " + timeoutSeconds + " seconds");
    }

    private KafkaConsumer<String, Response> createResponseConsumer() {
        var props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "test-processor-verifier");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class.getName());
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, Response.class.getName());
        return new KafkaConsumer<>(props);
    }

    private KafkaConsumer<String, Request> createRequestConsumer() {
        var props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "test-processor-dlq-verifier");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class.getName());
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, Request.class.getName());
        return new KafkaConsumer<>(props);
    }

    private Request pollRequest(String topic, String requestId, int timeoutSeconds) throws Exception {
        var future = new CompletableFuture<Request>();

        try (var consumer = createRequestConsumer()) {
            consumer.subscribe(List.of(topic));
            long deadline = System.currentTimeMillis() + (timeoutSeconds * 1000L);

            while (System.currentTimeMillis() < deadline && !future.isDone()) {
                var records = consumer.poll(Duration.ofSeconds(1));
                for (var record : records) {
                    var request = record.value();
                    if (request != null && requestId.equals(request.requestId())) {
                        future.complete(request);
                        return future.get(1, TimeUnit.SECONDS);
                    }
                }
            }
        }

        throw new AssertionError("No request received on DLQ for requestId: " + requestId
                + " within " + timeoutSeconds + " seconds");
    }
}
