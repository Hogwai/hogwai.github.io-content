package com.hogwai.jit.escapeanalysis;

import com.hogwai.jit.JITReportCard;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Escape Analysis - Lock Elision
 * Demonstrates how the JIT eliminates lock acquisition on objects
 * that don't escape the current thread. A locally-created object is
 * used with synchronized(this) - since no other thread can access it,
 * the JIT removes the monitor enter/exit entirely.
 * Observation:
 *   - With EliminateLocks (default): lock is elided, no contention, fast
 *   - Without EliminateLocks: real lock acquire/release, slower
 * PrintInlining shows: "eliminate lock" entries.
 */
public class LockElision {

    private static final Logger LOG = LoggerFactory.getLogger(LockElision.class);
    private static final int ITERATIONS = 10_000_000;

    public static void main(String[] args) {
        LOG.info("=== LockElision: Lock Elimination Demo ===");
        LOG.info("synchronized() on a locally-created object in a hot loop.");
        LOG.info("If lock elision works: lock is eliminated, fast execution.");
        LOG.info("If disabled: real lock acquire/release overhead.");

        JITReportCard.report(
                "LockElision",
                () -> {
                    long counter = 0;
                    for (int i = 0; i < ITERATIONS; i++) {
                        Object lock = new Object();
                        //noinspection SynchronizationOnLocalVariableOrMethodParameter
                        synchronized (lock) {
                            counter++;
                        }
                    }
                    LOG.info("  Result (anti-DCE): {}", counter);
                },
                "-XX:-EliminateLocks"
        );

        LOG.info("Check PrintInlining for 'eliminate lock' entries.");
    }
}
