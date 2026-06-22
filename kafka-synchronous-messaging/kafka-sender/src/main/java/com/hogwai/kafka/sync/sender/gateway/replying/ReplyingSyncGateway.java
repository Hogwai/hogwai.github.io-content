package com.hogwai.kafka.sync.sender.gateway.replying;

import com.hogwai.kafka.sync.model.Request;
import com.hogwai.kafka.sync.model.Response;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.requestreply.ReplyingKafkaTemplate;
import org.springframework.kafka.requestreply.RequestReplyFuture;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Synchronous Kafka request/reply using Spring Kafka's built-in
 * {@link ReplyingKafkaTemplate}.
 * <p>
 * The template handles correlation via {@code CORRELATION_ID} header and
 * reply subscription automatically, no manual map or listener needed.
 */
@Service
public class ReplyingSyncGateway {

    private static final Logger log = LoggerFactory.getLogger(ReplyingSyncGateway.class);

    private final ReplyingKafkaTemplate<String, Request, Response> replyingTemplate;

    @Value("${app.kafka.topic.requests}")
    private String requestTopic;

    @Value("${app.kafka.sync-timeout-seconds:30}")
    private int syncTimeout;

    public ReplyingSyncGateway(ReplyingKafkaTemplate<String, Request, Response> replyingTemplate) {
        this.replyingTemplate = replyingTemplate;
    }

    public Response sendSync(Request request)
            throws InterruptedException, ExecutionException, TimeoutException {

        var record = new ProducerRecord<>(requestTopic, request.requestId(), request);

        log.info(">>> Sending sync request [{}] via ReplyingKafkaTemplate, awaiting reply...",
                request.requestId());

        RequestReplyFuture<String, Request, Response> future =
                replyingTemplate.sendAndReceive(record, Duration.ofSeconds(syncTimeout));

        var responseRecord = future.get(syncTimeout, TimeUnit.SECONDS);
        log.info("<<< Response received for [{}] via ReplyingKafkaTemplate", request.requestId());

        return responseRecord.value();
    }
}
