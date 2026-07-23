package com.hogwai.jit.tiered;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tiered Compilation - Tier Transitions
 * Demonstrates how a hot method progresses through compilation tiers:
 *   L0 - Interpreter
 *   L3 - C1 with full profiling (MethodDataObject collected)
 *   L4 - C2 fully optimized (profile-guided)
 * Uses PrintCompilation to show the tier progression in real time.
 * Each invocation prints the current iteration so you can correlate
 * PrintCompilation entries with application progress.
 * PrintCompilation symbol legend:
 *   %      = On-Stack Replacement (OSR)
 *   s      = synchronized method
 *   !      = method has exception handlers
 *   b      = blocking compilation
 *   n      = native method wrapper
 * Output looks like:
 *   123  45       3       com.hogwai.jit.tiered.TierTransition::hotMethod (42 bytes)
 *   ^    ^        ^       ^--- method + size
 *   |    |        +--- compilation tier (0-4)
 *   |    +--- compile ID (unique counter)
 *   +--- timestamp (ms)
 */
public class TierTransition {

    private static final Logger LOG = LoggerFactory.getLogger(TierTransition.class);

    public static void main(String[] args) {
        LOG.info("=== TierTransition: Compilation Tier Progression Demo ===");
        LOG.info("Observe PrintCompilation output during execution.");
        LOG.info("Look for hotMethod() going through L0 -> L3 -> L4.");
        LOG.info("Symbol legend:");
        LOG.info("  % = OSR  |  s = synchronized  |  ! = exception handlers");
        LOG.info("  b = blocking  |  n = native wrapper");

        // Progress tracker: print every 2000 iterations so you can
        // correlate with PrintCompilation timestamps
        double sum = 0;
        int reportInterval = 2_000;
        int totalIterations = 20_000;

        for (int i = 0; i < totalIterations; i++) {
            sum += hotMethod(i);
            if (i > 0 && i % reportInterval == 0) {
                LOG.info("  Iteration {} :  check PrintCompilation for new entries", i);
            }
        }

        LOG.info("Done. Result (anti-DCE): {}", sum);
        LOG.info("Expected progression in PrintCompilation:");
        LOG.info("  1. hotMethod appears as tier 0 (interpreted)");
        LOG.info("  2. After ~1500 invocations -> tier 3 (C1 with profiling)");
        LOG.info("  3. After ~10000 invocations -> tier 4 (C2 optimized)");
    }

    private static double hotMethod(int n) {
        double result = 0;
        for (int i = 0; i < 100; i++) {
            result += Math.sin(n + (double) i) * Math.cos(n - (double) i);
        }
        return result;
    }
}
