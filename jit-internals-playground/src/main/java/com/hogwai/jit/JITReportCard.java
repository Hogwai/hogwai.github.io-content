package com.hogwai.jit;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple single-run JIT reporter.
 * Runs a benchmark once and prints elapsed time in us plus GC collection count.
 * The rerunHint tells the user which flag to add to disable a specific
 * optimization for comparison.
 */
public final class JITReportCard {

    private static final Logger LOG = LoggerFactory.getLogger(JITReportCard.class);

    private JITReportCard() {}

    public static void report(String title, Runnable benchmark, String rerunHint) {
        LOG.info("=== JIT Report Card: {} ===", title);

        resetGCStats();
        long t1 = System.nanoTime();
        benchmark.run();
        long elapsed = System.nanoTime() - t1;
        long gc = getTotalGCCollections();

        LOG.info("  Elapsed: %5d GC, %,8d us".formatted(gc, elapsed / 1000));
        LOG.info("  To compare without {}:", title);
        LOG.info("    Re-run with: {}", rerunHint);
    }

    private static void resetGCStats() {
        for (GarbageCollectorMXBean bean : ManagementFactory.getGarbageCollectorMXBeans()) {
            bean.getCollectionCount();
        }
    }

    private static long getTotalGCCollections() {
        List<GarbageCollectorMXBean> beans = ManagementFactory.getGarbageCollectorMXBeans();
        return beans.stream().mapToLong(GarbageCollectorMXBean::getCollectionCount).sum();
    }
}
