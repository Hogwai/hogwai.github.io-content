package com.hogwai.jit.inlining;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JIT Inlining - Call Depth Limit
 * Demonstrates MaxInlineLevel (C2 default 15 since JDK 16; C1MaxInlineLevel = 9).
 * Methods beyond the inline level are not inlined - call overhead reappears.
 * Builds a chain: add1() -> add2() -> ... -> add20()
 * Each method increments the value and passes to the next.
 * The last method (base case) returns the value directly.
 * PrintInlining log should show inlining stopping at depth limit.
 * Compare: deep chain (20 levels) vs flat equivalent (direct addition).
 */
public class InliningDepthChains {

    private static final Logger LOG = LoggerFactory.getLogger(InliningDepthChains.class);
    private static final int WARMUP = 30_000;
    private static final int ITERATIONS = 100_000;

    // Deep call chain: 20 levels
    private static int add1(int v)  { return add2(v + 1); }
    private static int add2(int v)  { return add3(v + 1); }
    private static int add3(int v)  { return add4(v + 1); }
    private static int add4(int v)  { return add5(v + 1); }
    private static int add5(int v)  { return add6(v + 1); }
    private static int add6(int v)  { return add7(v + 1); }
    private static int add7(int v)  { return add8(v + 1); }
    private static int add8(int v)  { return add9(v + 1); }
    private static int add9(int v)  { return add10(v + 1); }
    private static int add10(int v) { return add11(v + 1); }
    private static int add11(int v) { return add12(v + 1); }
    private static int add12(int v) { return add13(v + 1); }
    private static int add13(int v) { return add14(v + 1); }
    private static int add14(int v) { return add15(v + 1); }
    private static int add15(int v) { return add16(v + 1); }
    private static int add16(int v) { return add17(v + 1); }
    private static int add17(int v) { return add18(v + 1); }
    private static int add18(int v) { return add19(v + 1); }
    private static int add19(int v) { return add20(v + 1); }
    private static int add20(int v) { return v + 1; }

    // Flat equivalent: direct addition
    private static int flatAdd(int v) {
        return v + 20;
    }

    public static void main(String[] args) {
        LOG.info("=== InliningDepthChains: Call Depth Limit Demo ===");
        LOG.info("MaxInlineLevel   = 15 (C2, since JDK 16)");
        LOG.info("C1MaxInlineLevel = 9");
        LOG.info("Chain: add1 -> add2 -> ... -> add20 (20 levels)");
        LOG.info("Flat:  direct computation (1 level)");

        int sum = 0;

        // Warmup
        for (int i = 0; i < WARMUP; i++) {
            sum += add1(i);
            sum += flatAdd(i);
        }

        // Measure deep chain
        long t1 = System.nanoTime();
        for (int i = 0; i < ITERATIONS; i++) {
            sum += add1(i);
        }
        long t1elapsed = System.nanoTime() - t1;

        // Measure flat
        long t2 = System.nanoTime();
        for (int i = 0; i < ITERATIONS; i++) {
            sum += flatAdd(i);
        }
        long t2elapsed = System.nanoTime() - t2;

        LOG.info("Deep chain (20 calls): %,9d us (%d invocations)".formatted(t1elapsed / 1000, ITERATIONS));
        LOG.info("Flat (1 call):         %,9d us (%d invocations)".formatted(t2elapsed / 1000, ITERATIONS));
        LOG.info("Result (anti-DCE): {}", sum);
        LOG.info("Check PrintInlining for inline depth cutoff:");
        LOG.info("  grep 'add' logs/hotspot_InliningDepthChains.log | head -30");
    }
}
