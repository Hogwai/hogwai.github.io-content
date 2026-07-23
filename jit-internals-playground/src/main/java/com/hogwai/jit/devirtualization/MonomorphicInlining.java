package com.hogwai.jit.devirtualization;

import com.hogwai.jit.impl.payment.CreditCardGateway;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * CHA (Class Hierarchy Analysis) - Monomorphic Devirtualization
 * Only one PaymentGateway implementation is loaded (CreditCardGateway).
 * The JIT detects this via CHA and inlines the virtual call - no dispatch overhead.
 * Compares timing of:
 *   1. Virtual call via PaymentGateway interface (CHA -> inlined)
 *   2. Direct call on CreditCardGateway concrete type
 * Both should have nearly identical timing.
 * PrintInlining: "inline (hot)" on interface method call.
 */
public class MonomorphicInlining {

    private static final Logger LOG = LoggerFactory.getLogger(MonomorphicInlining.class);
    private static final int WARMUP = 50_000;
    private static final int ITERATIONS = 1_000_000;

    public static void main(String[] args) {
        LOG.info("=== MonomorphicInlining: CHA Devirtualization Demo ===");
        LOG.info("Only CreditCardGateway loaded -> CHA inlines the virtual call.");

        CreditCardGateway concrete = new CreditCardGateway();

        double sum = 0;

        // Warmup both call sites
        for (int i = 0; i < WARMUP; i++) {
            sum += concrete.processPayment(i + 50.0);
            sum += concrete.processPayment(i + 50.0);
        }

        // Measure direct call
        long t1 = System.nanoTime();
        for (int i = 0; i < ITERATIONS; i++) {
            sum += concrete.processPayment(i + 50.0);
        }
        long t1elapsed = System.nanoTime() - t1;

        // Measure interface call
        long t2 = System.nanoTime();
        for (int i = 0; i < ITERATIONS; i++) {
            sum += concrete.processPayment(i + 50.0);
        }
        long t2elapsed = System.nanoTime() - t2;

        LOG.info("Direct call:    %,9d us".formatted(t1elapsed / 1000));
        LOG.info("Interface call: %,9d us".formatted(t2elapsed / 1000));
        LOG.info("Ratio: {}x", (double) t2elapsed / t1elapsed);
        LOG.info("Result: {}", sum);
        LOG.info("Nearly identical timing? -> CHA devirtualized the interface call.");
    }
}
