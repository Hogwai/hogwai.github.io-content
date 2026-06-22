package com.hogwai.kafka.sync.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;
import java.util.UUID;

/**
 * A synchronous request sent over Kafka.
 *
 * @param requestId unique identifier (auto-generated if null)
 * @param payload   the data to process
 * @param timestamp when the request was created
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record Request(
        String requestId,
        String payload,
        Instant timestamp
) {
    public Request {
        if (requestId == null || requestId.isBlank()) {
            requestId = UUID.randomUUID().toString();
        }
        if (timestamp == null) {
            timestamp = Instant.now();
        }
    }

    public Request(String payload) {
        this(null, payload, null);
    }
}
