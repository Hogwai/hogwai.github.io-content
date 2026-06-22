package com.hogwai.benchmark;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Thread)
@Warmup(iterations = 3, time = 2, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 10, time = 2, timeUnit = TimeUnit.SECONDS)
@Fork(1)
public class CapturingGroupsBenchmark {

    // Pattern using capturing groups that we never extract
    private static final Pattern CAPTURING_UNUSED = Pattern.compile("(\\w+)-(\\w+)");
    // Pattern using non-capturing groups
    private static final Pattern NON_CAPTURING = Pattern.compile("(?:\\w+)-(?:\\w+)");

    // Patterns for extraction benchmarks
    private static final Pattern POSITIONAL = Pattern.compile("(\\w+)-(\\d+)");
    private static final Pattern NAMED = Pattern.compile("(?<name>\\w+)-(?<id>\\d+)");

    private List<String> inputs;

    @Setup(Level.Trial)
    public void setUp() {
        inputs = IntStream.range(0, 1000)
                .mapToObj(i -> "user-" + i)
                .toList();
    }

    /**
     * Capturing groups that we never extract — the engine still tracks them
     */
    @Benchmark
    public void capturingUnused(Blackhole bh) {
        for (String s : inputs) {
            bh.consume(CAPTURING_UNUSED.matcher(s).matches());
        }
    }

    /**
     * Non-capturing groups — engine skips capture bookkeeping
     */
    @Benchmark
    public void nonCapturing(Blackhole bh) {
        for (String s : inputs) {
            bh.consume(NON_CAPTURING.matcher(s).matches());
        }
    }

    /**
     * Extraction via positional group(1), group(2)
     */
    @Benchmark
    public void positionalGroupExtraction(Blackhole bh) {
        for (String s : inputs) {
            Matcher m = POSITIONAL.matcher(s);
            if (m.matches()) {
                bh.consume(m.group(1));
                bh.consume(m.group(2));
            }
        }
    }

    /**
     * Extraction via named group("name"), group("id") — same internal storage,
     * minor HashMap lookup overhead
     */
    @Benchmark
    public void namedGroupExtraction(Blackhole bh) {
        for (String s : inputs) {
            Matcher m = NAMED.matcher(s);
            if (m.matches()) {
                bh.consume(m.group("name"));
                bh.consume(m.group("id"));
            }
        }
    }

    /**
     * Both matches() and extraction — isolates the full cost of captures
     */
    @Benchmark
    public void nonCapturingWithExtraction(Blackhole bh) {
        for (String s : inputs) {
            // Extract using non-capturing groups + manual split
            if (NON_CAPTURING.matcher(s).matches()) {
                int dash = s.indexOf('-');
                bh.consume(s.substring(0, dash));
                bh.consume(s.substring(dash + 1));
            }
        }
    }
}
