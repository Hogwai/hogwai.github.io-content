package com.hogwai.kafka.sync.processor.service;

import com.hogwai.kafka.sync.model.Request;
import com.hogwai.kafka.sync.model.Response;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Header;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
public class RequestProcessor {

    private static final Logger log = LoggerFactory.getLogger(RequestProcessor.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${app.kafka.topic.results}")
    private String resultsTopic;

    @Value("${app.kafka.topic.dlq}")
    private String dlqTopic;

    public RequestProcessor(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @KafkaListener(
            topics = "${app.kafka.topic.requests}",
            groupId = "request-processor",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onRequest(ConsumerRecord<String, Request> record) {
        Request request = record.value();
        log.info("=== Received request [{}] payload '{}'", request.requestId(), request.payload());

        try {
            // --- simulate business processing that may fail ---
            if (request.payload() != null && request.payload().startsWith("fail")) {
                throw new RuntimeException("Simulated failure for: %s".formatted(request.payload()));
            }

            Response response = new Response(
                    request.requestId(),
                    "Processed: %s".formatted(request.payload()),
                    "SUCCESS",
                    100L
            );

            sendResponse(record, response);

        } catch (Exception e) {
            log.error("Failed to process request [{}]: {}", request.requestId(), e.getMessage(), e);

            // Send the failed request to the DLQ
            log.info("    → Sending to DLQ topic '{}'", dlqTopic);
            // Fire-and-forget — acceptable for showcase. In production, handle send failure.
            kafkaTemplate.send(dlqTopic, request.requestId(), request);

            // Send a failure response so the sender doesn't time out
            Response errorResponse = new Response(
                    request.requestId(),
                    e.getMessage(),
                    "FAILURE",
                    0L
            );
            sendResponse(record, errorResponse);
        }
    }

    private void sendResponse(ConsumerRecord<String, Request> record, Response response) {
        Header replyTopicHeader = record.headers().lastHeader(KafkaHeaders.REPLY_TOPIC);

        if (replyTopicHeader != null) {
            // --- ReplyingKafkaTemplate flow ---
            String replyTopic = new String(replyTopicHeader.value(), StandardCharsets.UTF_8);
            log.info("    → REPLY_TOPIC found, replying to '{}'", replyTopic);

            var reply = new ProducerRecord<String, Object>(replyTopic, response.requestId(), response);
            Header correlationId = record.headers().lastHeader(KafkaHeaders.CORRELATION_ID);
            if (correlationId != null) {
                reply.headers().add(correlationId);
            }
            kafkaTemplate.send(reply);
        } else {
            // --- Custom approach flow ---
            log.info("    → No REPLY_TOPIC, replying to fixed topic '{}'", resultsTopic);
            kafkaTemplate.send(resultsTopic, response.requestId(), response);
        }
    }
}
