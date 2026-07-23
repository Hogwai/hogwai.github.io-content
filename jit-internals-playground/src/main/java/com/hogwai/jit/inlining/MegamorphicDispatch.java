package com.hogwai.jit.inlining;

import com.hogwai.jit.impl.payment.PaymentGateway;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JIT Inlining - Megamorphic Dispatch
 * Demonstrates how the JIT handles virtual call sites as more implementations load.
 * Unlike the previous flawed version (which passed one isolated instance per stage),
 * this version uses a single PaymentGateway[] array and cycles through it in a hot
 * loop so that the call site gw.processPayment() sees the actual runtime mix:
 *   Stage 1 (monomorphic): Array has 1 entry (CreditCardGateway).
 *                          Loop: gateways[i % 1] -> always CC -> JIT inlines via CHA.
 *                          PrintInlining: "inline (hot)"
 *   Stage 2 (bimorphic):   Load PayPalGateway. Array now has 2 entries.
 *                          Loop: gateways[i % 2] -> alternates CC/PP.
 *                          JIT uses guarded inlining with a type check.
 *                          PrintInlining: "bimorphic, inline"
 *   Stage 3 (megamorphic): Load BankTransferGateway. Array now has 3 entries.
 *                          Loop: gateways[i % 3] -> cycles CC/PP/BT.
 *                          JIT gives up on inlining -> vtable dispatch.
 *                          PrintInlining: "megamorphic"
 * The timing should visibly degrade across stages as inlining is lost.
 * Uses Class.forName() to load implementations incrementally so that not all
 * three are visible at compile time - only PaymentGateway is imported.
 * Run with -XX:+UnlockDiagnosticVMOptions -XX:+PrintInlining
 */
public class MegamorphicDispatch {

    private static final Logger LOG = LoggerFactory.getLogger(MegamorphicDispatch.class);
    private static final String FMT_WITH_SUM  = "  %-30s : %,9d us (sum=%.0f)";
    private static final String FMT_TIME_ONLY = "  %-30s : %,9d us";
    private static final String FMT_PCT_CHG  = "  %-30s : %+.1f%%";
    private static final int WARMUP = 50_000;
    private static final int ITERATIONS = 200_000;

    public static void main(String[] args) throws Exception {
        LOG.info("=== Megamorphic Dispatch: Virtual Call Inlining Demo ===");

        // --- Stage 1: Monomorphic ------------------------------------------
        LOG.info("--- Stage 1: Monomorphic (1 implementation) ---");

        Class<?> ccClass = Class.forName("com.hogwai.jit.impl.payment.CreditCardGateway");
        PaymentGateway cc = (PaymentGateway) ccClass.getDeclaredConstructor().newInstance();

        PaymentGateway[] gateways = new PaymentGateway[]{cc};

        double sum = 0;

        // Warmup
        for (int i = 0; i < WARMUP; i++) {
            sum += gateways[0].processPayment(i + 100.0);
        }

        // Measure
        long t1 = System.nanoTime();
        for (int i = 0; i < ITERATIONS; i++) {
            sum += gateways[0].processPayment(i + 100.0);
        }
        long elapsed1 = System.nanoTime() - t1;
        LOG.info(FMT_WITH_SUM.formatted("Monomorphic (CreditCard only)", elapsed1 / 1000, sum));

        // --- Stage 2: Bimorphic --------------------------------------------
        LOG.info("--- Stage 2: Bimorphic (2 implementations) ---");

        Class<?> ppClass = Class.forName("com.hogwai.jit.impl.payment.PayPalGateway");
        PaymentGateway pp = (PaymentGateway) ppClass.getDeclaredConstructor().newInstance();

        gateways = new PaymentGateway[]{cc, pp};

        // Warmup (bimorphic call site)
        for (int i = 0; i < WARMUP; i++) {
            sum += gateways[i % 2].processPayment(i + 100.0);
        }

        // Measure
        t1 = System.nanoTime();
        for (int i = 0; i < ITERATIONS; i++) {
            sum += gateways[i % 2].processPayment(i + 100.0);
        }
        long elapsed2 = System.nanoTime() - t1;
        LOG.info(FMT_WITH_SUM.formatted("Bimorphic (CC + PayPal)", elapsed2 / 1000, sum));

        // --- Stage 3: Megamorphic ------------------------------------------
        LOG.info("--- Stage 3: Megamorphic (3 implementations) ---");

        Class<?> btClass = Class.forName("com.hogwai.jit.impl.payment.BankTransferGateway");
        PaymentGateway bt = (PaymentGateway) btClass.getDeclaredConstructor().newInstance();

        gateways = new PaymentGateway[]{cc, pp, bt};

        // Warmup (megamorphic call site)
        for (int i = 0; i < WARMUP; i++) {
            sum += gateways[i % 3].processPayment(i + 100.0);
        }

        // Measure
        t1 = System.nanoTime();
        for (int i = 0; i < ITERATIONS; i++) {
            sum += gateways[i % 3].processPayment(i + 100.0);
        }
        long elapsed3 = System.nanoTime() - t1;
        LOG.info(FMT_WITH_SUM.formatted("Megamorphic (CC + PP + BT)", elapsed3 / 1000, sum));

        // --- Comparison table ----------------------------------------------
        LOG.info("=== Comparison Table ===");
        LOG.info(FMT_TIME_ONLY.formatted("Monomorphic", elapsed1 / 1000));
        LOG.info(FMT_TIME_ONLY.formatted("Bimorphic", elapsed2 / 1000));
        LOG.info(FMT_TIME_ONLY.formatted("Megamorphic", elapsed3 / 1000));
        LOG.info(FMT_PCT_CHG.formatted("Mono -> Bi change",
                ((double) elapsed2 / elapsed1 - 1.0) * 100.0));
        LOG.info(FMT_PCT_CHG.formatted("Bi -> Mega change",
                ((double) elapsed3 / elapsed2 - 1.0) * 100.0));
        LOG.info(FMT_PCT_CHG.formatted("Mono -> Mega change",
                ((double) elapsed3 / elapsed1 - 1.0) * 100.0));

        // --- PrintInlining guidance ----------------------------------------
        LOG.info("What to look for in PrintInlining output:");
        LOG.info("  Run: java -XX:+UnlockDiagnosticVMOptions -XX:+PrintInlining \\");
        LOG.info("       -cp target/classes com.hogwai.jit.inlining.MegamorphicDispatch");
        LOG.info("  Stage 1 (monomorphic) -> look for:");
        LOG.info("    \"com.hogwai.jit.impl.payment.CreditCardGateway::processPayment (24 bytes)\"");
        LOG.info("      @ 11   ...PaymentGateway::processPayment (5 bytes)   inline (hot)");
        LOG.info("  Stage 2 (bimorphic) -> look for:");
        LOG.info("    \"com.hogwai.jit.impl.payment.CreditCardGateway::processPayment (24 bytes)\"");
        LOG.info("      @ 11   ...PaymentGateway::processPayment (5 bytes)   bimorphic, inline");
        LOG.info("    \"com.hogwai.jit.impl.payment.PayPalGateway::processPayment (24 bytes)\"");
        LOG.info("      @ 11   ...PaymentGateway::processPayment (5 bytes)   bimorphic, inline");
        LOG.info("  Stage 3 (megamorphic) -> look for:");
        LOG.info("    ~processPayment~ @ 11   ...PaymentGateway::processPayment (5 bytes)   megamorphic");
        LOG.info("    (no inlining happens - the ~ tilde prefix means \"not inlineable\")");
        LOG.info("  Also visible in -XX:+LogCompilation output opened in JITWatch.");
        LOG.info("  Anti-DCE sum (not meaningful, just prevents elimination): {}", sum);
    }
}
