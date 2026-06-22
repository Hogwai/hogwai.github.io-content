package com.hogwai.kafka.sync.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * The processing result for a {@link Request}.
 *
 * @param requestId        the original request's identifier (used for correlation)
 * @param payload          processed result content
 * @param status           outcome (e.g. SUCCESS, FAILURE)
 * @param processingTimeMs how long the remote processing took
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record Response(
        String requestId,
        String payload,
        String status,
        long processingTimeMs
) {}
