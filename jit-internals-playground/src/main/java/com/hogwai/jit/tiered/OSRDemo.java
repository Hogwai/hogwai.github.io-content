package com.hogwai.jit.tiered;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * On-Stack Replacement (OSR)
 * Demonstrates OSR: a long-running loop is compiled mid-execution by C2,
 * and the JVM swaps out the running interpreted frame for the compiled version
 * without restarting the method.
 * The demo runs a single loop for 200,000 iterations. Halfway through,
 * the OSR compilation kicks in and there's a visible "step change" in
 * per-iteration timing.
 * PrintCompilation shows OSR entries with "%" prefix and "@BCI" suffix:
 *   e.g., "125  48 %     4       OSRDemo::longLoop @ 15 (52 bytes)"
 *   -> "4" = C2 tier, "@ 15" = bytecode index 15 (inside the loop body)
 */
public class OSRDemo {

    private static final Logger LOG = LoggerFactory.getLogger(OSRDemo.class);
    private static final int ITERATIONS = 200_000;

    public static void main(String[] args) {
        LOG.info("=== OSRDemo: On-Stack Replacement Demo ===");
        LOG.info("A single long-running loop :  watch for the OSR compilation.");
        LOG.info("Look for '%' entries with '@N' BCI suffix in PrintCompilation.");

        long startTime = System.nanoTime();
        long lastTimestamp = startTime;
        long sum = 0;

        for (int i = 0; i < ITERATIONS; i++) {
            sum += compute(i);

            // Print progress every 20,000 iterations to show the speedup
            if (i > 0 && i % 20_000 == 0) {
                long now = System.nanoTime();
                long elapsed = now - lastTimestamp;
                lastTimestamp = now;
                double usPerIteration = (double) elapsed / 20_000 / 1000;
                LOG.info("  Iteration %6d :  %.2f us/iteration".formatted(i, usPerIteration));
            }
        }

        long total = System.nanoTime() - startTime;
        LOG.info("Total: {} ms, Result: {}", total / 1_000_000.0, sum);
        LOG.info("Expected: per-iteration timing drops significantly after OSR kicks in");
        LOG.info("(typically around iteration 8,000-15,000).");
    }

    private static long compute(int n) {
        long result = 0;
        for (int i = 0; i < 50; i++) {
            result += (long) (Math.log(n + i + 1.0) * 1000);
        }
        return result;
    }
}
