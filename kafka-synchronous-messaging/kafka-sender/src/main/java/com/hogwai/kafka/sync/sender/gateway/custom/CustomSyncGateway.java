package com.hogwai.kafka.sync.sender.gateway.custom;

import com.hogwai.kafka.sync.model.Request;
import com.hogwai.kafka.sync.model.Response;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Synchronous Kafka request/reply using a hand-made {@link CompletableFuture} pattern.
 * <p>
 * A {@link CompletableFuture} is stored in a {@link ConcurrentHashMap} keyed by
 * {@code requestId}. The producer blocks on {@link CompletableFuture#get(long, TimeUnit)}.
 * A separate {@link CustomReplyListener @KafkaListener} receives the response and
 * calls {@link #complete(Response)} to resolve the matching future.
 */
@Service
public class CustomSyncGateway {

    private static final Logger log = LoggerFactory.getLogger(CustomSyncGateway.class);

    private final KafkaTemplate<String, Request> kafkaTemplate;
    private final Map<String, CompletableFuture<Response>> pendingFutures = new ConcurrentHashMap<>();

    @Value("${app.kafka.topic.requests}")
    private String requestTopic;

    @Value("${app.kafka.sync-timeout-seconds:30}")
    private int syncTimeout;

    public CustomSyncGateway(KafkaTemplate<String, Request> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public Response sendSync(Request request)
            throws InterruptedException, ExecutionException, TimeoutException {

        var future = new CompletableFuture<Response>();
        pendingFutures.put(request.requestId(), future);

        log.info(">>> Sending sync request [{}]: awaiting reply...", request.requestId());
        kafkaTemplate.send(new ProducerRecord<>(requestTopic, request.requestId(), request));

        try {
            return future.get(syncTimeout, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            pendingFutures.remove(request.requestId());
            log.warn("Sync request [{}] timed out after {}s", request.requestId(), syncTimeout);
            throw e;
        }
    }

    public void complete(Response response) {
        var future = pendingFutures.remove(response.requestId());
        if (future != null) {
            log.info("<<< Response received for [{}]: completing future", response.requestId());
            future.complete(response);
        } else {
            log.warn("No pending future found for [{}]", response.requestId());
        }
    }

    public int pendingCount() {
        return pendingFutures.size();
    }
}
