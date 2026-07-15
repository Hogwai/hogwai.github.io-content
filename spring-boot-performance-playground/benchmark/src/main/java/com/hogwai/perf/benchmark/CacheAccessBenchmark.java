package com.hogwai.perf.benchmark;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Benchmark)
public class CacheAccessBenchmark {

    private final ConcurrentHashMap<String, String> noCache = new ConcurrentHashMap<>();
    private String key = "test-key";
    private String value = "test-value";

    @Benchmark
    public void noCache(Blackhole bh) {
        bh.consume(noCache.get(key));
    }

    @Benchmark
    public void caffeineSimulated(Blackhole bh) {
        // Simulate Caffeine get (concurrent hash map read ~ 30-50ns)
        var result = noCache.get(key);
        if (result == null) {
            noCache.put(key, value);
            result = value;
        }
        bh.consume(result);
    }

    @Benchmark
    public void dbSimulated(Blackhole bh) {
        // Simulate a simple DB query (~ 1-5ms)
        try {
            Thread.sleep(1);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        bh.consume("db_value");
    }
}
