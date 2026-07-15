package com.hogwai.perf.benchmark;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.*;
import java.util.concurrent.locks.LockSupport;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Benchmark)
public class VirtualThreadOverheadBenchmark {

    @Benchmark
    public void platformThreadCreate(Blackhole bh) throws Exception {
        var thread = new Thread(() -> bh.consume(42));
        thread.start();
        thread.join();
    }

    @Benchmark
    public void virtualThreadCreate(Blackhole bh) throws Exception {
        var thread = Thread.startVirtualThread(() -> bh.consume(42));
        thread.join();
    }

    @Benchmark
    public void virtualThreadPool(Blackhole bh) throws Exception {
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var future = executor.submit(() -> {
                bh.consume(42);
                return 42;
            });
            future.get();
        }
    }
}
