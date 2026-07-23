package com.hogwai.jit.devirtualization;

import com.hogwai.jit.impl.payment.PaymentGateway;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * CHA - Deoptimization on New Class Load
 * Demonstrates CHA invalidation:
 *   1. Warm up with a single implementation (CreditCardGateway)
 *      -> JIT inlines the virtual call
 *   2. Load a NEW implementation (PayPalGateway) at runtime
 *      -> CHA dependency is invalidated
 *      -> Compiled method becomes "made not entrant"
 *      -> Next invocation goes through interpreter -> recompiles without the inline
 * Relies on: -XX:+TraceDeoptimization to show the deoptimization event.
 * The standard PrintInlining log will show the inline in step 1,
 * and PrintCompilation will show "made not entrant" after step 2.
 */
public class CHADeoptimization {

    private static final Logger LOG = LoggerFactory.getLogger(CHADeoptimization.class);
    private static final int WARMUP = 50_000;
    private static final int ITERATIONS = 200_000;

    public static void main(String[] args) throws Exception {
        LOG.info("=== CHADeoptimization: Class Loading -> Deoptimization Demo ===");

        // Step 1: Load and warm up with CreditCardGateway
        LOG.info("Step 1: Warming up with CreditCardGateway...");
        Class<?> ccClass = Class.forName("com.hogwai.jit.impl.payment.CreditCardGateway");
        PaymentGateway cc = (PaymentGateway) ccClass.getDeclaredConstructor().newInstance();

        double sum = 0;
        for (int i = 0; i < WARMUP; i++) {
            sum += cc.processPayment(i + 100.0);
        }

        // Measure after warmup - should be inlined
        long t1 = System.nanoTime();
        for (int i = 0; i < ITERATIONS; i++) {
            sum += cc.processPayment(i + 100.0);
        }
        long t1elapsed = System.nanoTime() - t1;
        LOG.info("  After warmup (inlined): %,9d us".formatted(t1elapsed / 1000));

        // Step 2: Load a new implementation - CHA invalidated!
        LOG.info("Step 2: Loading PayPalGateway -> CHA invalidated -> deoptimization!");
        Class<?> ppClass = Class.forName("com.hogwai.jit.impl.payment.PayPalGateway");
        PaymentGateway pp = (PaymentGateway) ppClass.getDeclaredConstructor().newInstance();

        // Access the new impl to force the JIT to notice
        sum += pp.processPayment(100.0);

        // Step 3: Remeasure the original call - now deoptimized, recompiled without inline
        LOG.info("Step 3: Remeasuring CreditCardGateway after deoptimization...");
        long t2 = System.nanoTime();
        for (int i = 0; i < ITERATIONS; i++) {
            sum += cc.processPayment(i + 100.0);
        }
        long t2elapsed = System.nanoTime() - t2;
        LOG.info("  After deopt (recompiled): %,9d us".formatted(t2elapsed / 1000));
        LOG.info("  Ratio: {}x", (double) t2elapsed / t1elapsed);
        LOG.info("  Result: {}", sum);
        LOG.info("Check TraceDeoptimization output for 'Uncommon trap' events.");
        LOG.info("Check PrintCompilation for 'made not entrant' entries.");
    }
}
