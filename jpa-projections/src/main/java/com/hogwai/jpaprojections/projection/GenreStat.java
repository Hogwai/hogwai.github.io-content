package com.hogwai.jpaprojections.projection;

/**
 * Record DTO for an aggregated projection (JPQL GROUP BY).
 * Demonstrates that custom JPQL with constructor expressions works with records.
 */
public record GenreStat(String genre, long movieCount) {
}
