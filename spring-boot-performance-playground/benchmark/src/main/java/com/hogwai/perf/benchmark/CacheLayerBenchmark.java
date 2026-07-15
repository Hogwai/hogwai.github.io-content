package com.hogwai.perf.benchmark;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Thread)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 2)
@Fork(1)
public class CacheLayerBenchmark {

    private static final String KEY = "benchmark:product:1";
    private static final String VALUE = "cached_value";

    private final ConcurrentHashMap<String, String> l1 = new ConcurrentHashMap<>();

    @Setup(Level.Trial)
    public void setup() {
        // Pre-populate L1 with the hot key for hot-key benchmarks
        l1.put(KEY, VALUE);
    }

    @Setup(Level.Invocation)
    public void clearMissCache() {
        // Clear miss cache entries so every invocation does a real miss → L2 → DB path
        l1.remove("miss_" + KEY);
        l1.remove("miss2_" + KEY);
    }

    @Benchmark
    public void l1Only_hit(Blackhole bh) {
        // L1 hit: Caffeine local cache, <1µs
        bh.consume(l1.get(KEY));
    }

    @Benchmark
    public void l1Only_miss_l2Hit(Blackhole bh) {
        // L1 miss → L2 (Redis) hit: ~300µs
        String val = l1.get("miss_" + KEY);
        if (val == null) {
            sleepUs(300); // L2 Redis round-trip
            val = VALUE;
            l1.put("miss_" + KEY, val);
        }
        bh.consume(val);
    }

    @Benchmark
    public void l1Only_miss_l2Miss_dbFetch(Blackhole bh) {
        // L1 miss → L2 miss → DB fetch: ~1300µs
        String val = l1.get("miss2_" + KEY);
        if (val == null) {
            sleepUs(300); // L2 Redis round-trip — miss
            sleepUs(1000); // DB query
            val = VALUE;
            l1.put("miss2_" + KEY, val);
        }
        bh.consume(val);
    }

    @Benchmark
    public void l2Only_hit(Blackhole bh) {
        // Direct Redis hit, no L1: ~300µs
        sleepUs(300);
        bh.consume(VALUE);
    }

    @Benchmark
    public void l2Only_miss_dbFetch(Blackhole bh) {
        // Redis miss → DB: ~1300µs
        sleepUs(300);
        sleepUs(1000);
        bh.consume(VALUE);
    }

    @Benchmark
    public void dbOnly(Blackhole bh) {
        // Direct DB, no caching: ~1000µs
        sleepUs(1000);
        bh.consume("db_value");
    }

    @Benchmark
    public void multiLevel_hotKey(Blackhole bh) {
        // Hot key in L1: <1µs (same as l1Only_hit)
        bh.consume(l1.get(KEY));
    }

    private void sleepUs(long micros) {
        long nanos = micros * 1000;
        try {
            Thread.sleep(nanos / 1_000_000, (int) (nanos % 1_000_000));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
