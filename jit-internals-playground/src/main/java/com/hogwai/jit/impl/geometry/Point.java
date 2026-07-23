package com.hogwai.jit.impl.geometry;

/**
 * Simple record used by ScalarReplacement demo.
 * JIT replaces allocation with local registers when Point doesn't escape.
 */
public record Point(double x, double y, double z) {

    public double distanceFromOrigin() {
        return Math.sqrt(x * x + y * y + z * z);
    }
}
