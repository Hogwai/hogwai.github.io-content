package com.hogwai.g1;

import java.util.ArrayDeque;
import java.util.logging.Logger;

public final class G1Workload {

    private static final Logger LOG = Logger.getLogger("com.hogwai.g1.G1Workload");

    private volatile int sink;

    G1Workload() {
    }

    public static void main(String[] args) {
        int durationSeconds = args.length > 0 ? Integer.parseInt(args[0]) : 90;
        WorkloadResult result = new G1Workload().run(durationSeconds);
        LOG.info("Iterations: " + result.iterations() + ", Retained objects: " + result.retainedObjects());
    }

    WorkloadResult run(int durationSeconds) {
        LOG.info("Starting G1 workload for " + durationSeconds + " seconds");
        long endTime = System.currentTimeMillis() + durationSeconds * 1000L;

        var retained = new ArrayDeque<byte[]>(1280);
        int iterations = 0;

        while (System.currentTimeMillis() < endTime) {
            for (int i = 0; i < 512; i++) {
                byte[] ephemeral = new byte[64 * 1024];
                sink = System.identityHashCode(ephemeral);
            }

            // Periodic batch cleanup: every 4 iterations release 64 oldest
            // when at capacity (1280), bringing retention back to 1216.
            if (iterations % 4 == 0 && retained.size() >= 1280) {
                for (int i = 0; i < 64; i++) {
                    retained.removeFirst();
                }
            }

            // Add 16 new retained arrays; invariant guaranteed before each addLast.
            for (int i = 0; i < 16; i++) {
                if (retained.size() >= 1280) {
                    retained.removeFirst();
                }
                retained.addLast(new byte[128 * 1024]);
            }

            iterations++;

            try {
                Thread.sleep(20L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        return new WorkloadResult(iterations, retained.size());
    }

    record WorkloadResult(int iterations, int retainedObjects) {
    }
}
