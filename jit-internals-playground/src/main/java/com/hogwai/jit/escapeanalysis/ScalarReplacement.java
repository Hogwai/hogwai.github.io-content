package com.hogwai.jit.escapeanalysis;

import com.hogwai.jit.JITReportCard;
import com.hogwai.jit.impl.geometry.Point;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Escape Analysis - Scalar Replacement
 * Demonstrates how the JIT eliminates object allocation when the object
 * never escapes the method scope. A Point record is created inside a hot loop -
 * the JIT promotes its fields (x, y, z) into local registers instead of
 * allocating on the heap.
 * Observation:
 *   - With EA (default):    0-1 GC collections, tiny allocation pressure
 *   - Without EA (-DoEscapeAnalysis): many GC collections, massive allocation
 * Requires: -XX:+PrintEliminateAllocations (diagnostic flag, available in product builds)
 * LogCompilation XML + JITWatch shows "NoEscape, scalarReplaceable".
 */
public class ScalarReplacement {

    private static final Logger LOG = LoggerFactory.getLogger(ScalarReplacement.class);
    private static final int ITERATIONS = 10_000_000;

    public static void main(String[] args) {
        LOG.info("=== ScalarReplacement: Escape Analysis Demo ===");
        LOG.info("Creating {} Point objects in a hot loop...", ITERATIONS);
        LOG.info("If EA works: no GC, ~0 heap allocations for Point.");
        LOG.info("If EA disabled: massive GC pressure.");

        JITReportCard.report(
                "ScalarReplacement",
                () -> {
                    double sum = 0;
                    for (int i = 0; i < ITERATIONS; i++) {
                        Point p = new Point(i, i + 1.0, i + 2.0);
                        sum += p.distanceFromOrigin();
                    }
                    LOG.info("  Result (anti-DCE): {}", sum);
                },
                "-XX:-DoEscapeAnalysis"
        );

        LOG.info("Check PrintEliminateAllocations output for eliminated allocations.");
        LOG.info("JITWatch EA tab should show 'NoEscape, scalarReplaceable'.");
    }
}
