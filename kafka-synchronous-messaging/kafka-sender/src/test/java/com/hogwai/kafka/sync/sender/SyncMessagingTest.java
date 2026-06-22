package com.hogwai.kafka.sync.sender;

import com.hogwai.kafka.sync.model.Request;
import com.hogwai.kafka.sync.model.Response;
import com.hogwai.kafka.sync.sender.gateway.custom.CustomSyncGateway;
import com.hogwai.kafka.sync.sender.gateway.replying.ReplyingSyncGateway;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Header;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.test.annotation.DirtiesContext;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

@SpringBootTest(properties = {
        "app.kafka.sync-timeout-seconds=10"
})
@Testcontainers
@Import(SyncMessagingTest.TestRequestHandler.class)
@DirtiesContext
class SyncMessagingTest {

    @Container
    static KafkaContainer kafka = new KafkaContainer(
            DockerImageName.parse("apache/kafka:3.8.0")
    );

    @Autowired
    private CustomSyncGateway customGateway;

    @Autowired
    private ReplyingSyncGateway replyingGateway;

    @org.springframework.test.context.DynamicPropertySource
    static void overrideProperties(
            org.springframework.test.context.DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    }

    @Test
    @DisplayName("Custom: CompletableFuture + ConcurrentHashMap")
    void customApproach_shouldReceiveSyncResponse() throws Exception {
        var request = new Request("Invoice-123");

        Response response = customGateway.sendSync(request);

        assertAll(
                () -> assertThat(response.requestId()).isEqualTo(request.requestId()),
                () -> assertThat(response.status()).isEqualTo("SUCCESS"),
                () -> assertThat(response.payload()).contains("Invoice-123"),
                () -> assertThat(customGateway.pendingCount()).isZero()
        );
    }

    @Test
    @DisplayName("ReplyingKafkaTemplate: Spring Kafka built-in request/reply")
    void replyingKafkaTemplate_shouldReceiveSyncResponse() throws Exception {
        var request = new Request("CreditNote-789");

        Response response = replyingGateway.sendSync(request);

        assertAll(
                () -> assertThat(response.requestId()).isEqualTo(request.requestId()),
                () -> assertThat(response.status()).isEqualTo("SUCCESS"),
                () -> assertThat(response.payload()).contains("CreditNote-789")
        );
    }

    @Test
    @DisplayName("Both patterns concurrently without interference")
    void bothPatterns_shouldWorkConcurrently() throws Exception {
        var customRequest = new Request("Concurrent-Custom-1");
        var replyingRequest = new Request("Concurrent-Replying-2");

        var customFuture = CompletableFuture.supplyAsync(() -> {
            try {
                return customGateway.sendSync(customRequest);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        var replyingFuture = CompletableFuture.supplyAsync(() -> {
            try {
                return replyingGateway.sendSync(replyingRequest);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        CompletableFuture.allOf(customFuture, replyingFuture).get(15, TimeUnit.SECONDS);

        assertAll(
                () -> assertThat(customFuture.get().requestId()).isEqualTo(customRequest.requestId()),
                () -> assertThat(customFuture.get().status()).isEqualTo("SUCCESS"),
                () -> assertThat(replyingFuture.get().requestId()).isEqualTo(replyingRequest.requestId()),
                () -> assertThat(replyingFuture.get().status()).isEqualTo("SUCCESS"),
                () -> assertThat(customGateway.pendingCount()).isZero()
        );
    }

    /**
     * Test-only handler that simulates the remote processor service.
     * Listens on the requests topic and sends responses back via the
     * appropriate reply topic (custom or replying pattern).
     */
    @TestComponent
    static class TestRequestHandler {

        private final KafkaTemplate<String, Object> kafkaTemplate;

        @Value("${app.kafka.topic.results}")
        private String resultsTopic;

        TestRequestHandler(KafkaTemplate<String, Object> kafkaTemplate) {
            this.kafkaTemplate = kafkaTemplate;
        }

        @KafkaListener(
                topics = "${app.kafka.topic.requests}",
                groupId = "test-processor",
                containerFactory = "kafkaListenerContainerFactory"
        )
        void handle(ConsumerRecord<String, Request> consumerRecord) {
            Request request = consumerRecord.value();
            Response response = new Response(
                    request.requestId(),
                    "Processed: %s".formatted(request.payload()),
                    "SUCCESS",
                    100L
            );

            Header replyTopic = consumerRecord.headers().lastHeader(KafkaHeaders.REPLY_TOPIC);

            if (replyTopic != null) {
                var reply = new ProducerRecord<String, Object>(
                        new String(replyTopic.value(), StandardCharsets.UTF_8),
                        response.requestId(), response);
                Header correlationId = consumerRecord.headers().lastHeader(KafkaHeaders.CORRELATION_ID);
                if (correlationId != null) {
                    reply.headers().add(correlationId);
                }
                kafkaTemplate.send(reply);
            } else {
                kafkaTemplate.send(resultsTopic, response.requestId(), response);
            }
        }
    }
}
