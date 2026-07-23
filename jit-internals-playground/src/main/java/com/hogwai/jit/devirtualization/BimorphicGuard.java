package com.hogwai.jit.devirtualization;

import com.hogwai.jit.impl.payment.CreditCardGateway;
import com.hogwai.jit.impl.payment.PayPalGateway;
import com.hogwai.jit.impl.payment.PaymentGateway;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * CHA - Bimorphic Guarded Inlining
 * Two PaymentGateway implementations loaded (CreditCardGateway, PayPalGateway).
 * The JIT generates a guarded inline: type check -> inline CreditCard's method,
 * else type check -> inline PayPal's method, else -> fallback to vtable dispatch.
 * Still faster than megamorphic, but slightly slower than monomorphic due to
 * the type-guard overhead.
 * PrintInlining: "bimorphic, inline" with both implementations listed.
 */
public class BimorphicGuard {

    private static final Logger LOG = LoggerFactory.getLogger(BimorphicGuard.class);
    private static final int WARMUP = 50_000;
    private static final int ITERATIONS = 1_000_000;

    public static void main(String[] args) {
        LOG.info("=== BimorphicGuard: Bimorphic Inlining Demo ===");
        LOG.info("Two implementations loaded -> JIT uses guarded inlining.");

        PaymentGateway cc = new CreditCardGateway();
        PaymentGateway pp = new PayPalGateway();

        double sum = 0;

        // Warmup both receivers
        for (int i = 0; i < WARMUP; i++) {
            sum += cc.processPayment(i + 50.0);
            sum += pp.processPayment(i + 50.0);
        }

        // Measure CreditCard call
        long t1 = System.nanoTime();
        for (int i = 0; i < ITERATIONS; i++) {
            sum += cc.processPayment(i + 50.0);
        }
        long t1elapsed = System.nanoTime() - t1;

        // Measure PayPal call
        long t2 = System.nanoTime();
        for (int i = 0; i < ITERATIONS; i++) {
            sum += pp.processPayment(i + 50.0);
        }
        long t2elapsed = System.nanoTime() - t2;

        // Measure alternating call (worst for prediction)
        long t3 = System.nanoTime();
        for (int i = 0; i < ITERATIONS; i++) {
            PaymentGateway gw = (i % 2 == 0) ? cc : pp;
            sum += gw.processPayment(i + 50.0);
        }
        long t3elapsed = System.nanoTime() - t3;

        LOG.info("CreditCard (always):    %,9d us".formatted(t1elapsed / 1000));
        LOG.info("PayPal (always):        %,9d us".formatted(t2elapsed / 1000));
        LOG.info("Alternating CC/PP:      %,9d us".formatted(t3elapsed / 1000));
        LOG.info("Result: {}", sum);
        LOG.info("Check PrintInlining for 'bimorphic, inline' entries.");
    }
}
