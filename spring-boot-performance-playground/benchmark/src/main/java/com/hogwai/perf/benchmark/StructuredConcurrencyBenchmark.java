package com.hogwai.perf.benchmark;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.StructuredTaskScope;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
public class StructuredConcurrencyBenchmark {

    private static final int TASKS = 3;
    private static final long SIMULATED_WORK_MS = 10;

    @Benchmark
    public void sequential(Blackhole bh) throws Exception {
        for (int i = 0; i < TASKS; i++) {
            LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(SIMULATED_WORK_MS));
            bh.consume(i);
        }
    }

    @Benchmark
    public void structuredConcurrency(Blackhole bh) throws Exception {
        try (var scope = StructuredTaskScope.open()) {
            var f1 = scope.fork(() -> { LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(SIMULATED_WORK_MS)); bh.consume(1); return 1; });
            var f2 = scope.fork(() -> { LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(SIMULATED_WORK_MS)); bh.consume(2); return 2; });
            var f3 = scope.fork(() -> { LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(SIMULATED_WORK_MS)); bh.consume(3); return 3; });
            scope.join();
            bh.consume(f1.get() + f2.get() + f3.get());
        }
    }
}
