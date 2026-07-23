package com.hogwai.jit.inlining;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JIT Inlining - Method Size Thresholds
 * Demonstrates:
 *   - MaxInlineSize (~35 bytes): methods smaller than this are ALWAYS inlined
 *   - FreqInlineSize (~325 bytes): "hot" methods up to this size get inlined too
 * smallPrice() is tiny (~10 bytes of bytecode) - always inlined.
 * largePrice() is ~60 bytes - only inlined when very hot (C2, freaky frequency).
 * Look for in PrintInlining output:
 *   - "inline (hot)" for smallPrice
 *   - "too big" or "hot method too big" for largePrice (or "inline (hot)" after many invocations)
 */
public class SmallVsLargeMethod {

    private static final Logger LOG = LoggerFactory.getLogger(SmallVsLargeMethod.class);
    private static final int WARMUP = 50_000;
    private static final int ITERATIONS = 1_000_000;

    // Tiny method: ~2 bytecode instructions, well under MaxInlineSize
    private static double smallPrice(double amount) {
        return amount * 1.1;
    }

    // Larger method: multiple arithmetic operations, ~20 bytecode instructions
    // Above MaxInlineSize (35 bytes of bytecode, not source lines)
    // Below FreqInlineSize (325 bytes)
    private static double largePrice(double amount) {
        double vat = amount * 0.2;
        double service = amount * 0.05;
        double subtotal = amount + vat + service;
        double discount = subtotal > 100 ? subtotal * 0.02 : 0;
        double afterDiscount = subtotal - discount;

        return Math.round(afterDiscount * 100.0) / 100.0;
    }

    public static void main(String[] args) {
        LOG.info("=== SmallVsLargeMethod: Inlining Threshold Demo ===");
        LOG.info("MaxInlineSize  = 35 bytes  (always inlined)");
        LOG.info("FreqInlineSize = 325 bytes (inlined when very hot)");

        double sum = 0;

        // Warmup: let JIT profile
        for (int i = 0; i < WARMUP; i++) {
            sum += smallPrice(i);
            sum += largePrice(i * 2.0);
        }

        // Measure: small vs large
        long t1 = System.nanoTime();
        for (int i = 0; i < ITERATIONS; i++) {
            sum += smallPrice(i);
        }
        long t1elapsed = System.nanoTime() - t1;

        long t2 = System.nanoTime();
        for (int i = 0; i < ITERATIONS; i++) {
            sum += largePrice(i * 2.0);
        }
        long t2elapsed = System.nanoTime() - t2;

        LOG.info("smallPrice:  %,9d us (%d invocations)".formatted(t1elapsed / 1000, ITERATIONS));
        LOG.info("largePrice:  %,9d us (%d invocations)".formatted(t2elapsed / 1000, ITERATIONS));
        LOG.info("Result (anti-DCE): {}", sum);
        LOG.info("Now check the JIT log with JITWatch:");
        LOG.info("  java -jar jitwatch-ui-shaded.jar logs/hotspot_SmallVsLargeMethod.log");
        LOG.info("  -> Look for smallPrice() and largePrice() in the Inline Tree");
    }
}
