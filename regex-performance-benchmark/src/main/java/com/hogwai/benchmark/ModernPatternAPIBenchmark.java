package com.hogwai.benchmark;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Thread)
@Warmup(iterations = 3, time = 2, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 10, time = 2, timeUnit = TimeUnit.SECONDS)
@Fork(1)
public class ModernPatternAPIBenchmark {

    private static final Pattern COMMA = Pattern.compile(",");
    private static final Pattern DIGITS = Pattern.compile("\\d+");

    private String csvInput;
    private List<String> mixedInputs;

    @Setup(Level.Trial)
    public void setUp() {
        csvInput = IntStream.range(0, 1000)
                .mapToObj(String::valueOf)
                .collect(Collectors.joining(","));

        mixedInputs = IntStream.range(0, 1000)
                .mapToObj(i -> i % 2 == 0 ? String.valueOf(i) : "abc" + i)
                .toList();
    }

    // --- split vs splitAsStream ---

    /**
     * split() into array then iterate
     */
    @Benchmark
    public void splitToArray(Blackhole bh) {
        for (String s : COMMA.split(csvInput)) {
            bh.consume(s);
        }
    }

    /**
     * splitAsStream() — lazy, no intermediate array
     */
    @Benchmark
    public void splitToStream(Blackhole bh) {
        COMMA.splitAsStream(csvInput).forEach(bh::consume);
    }

    /**
     * split() + Arrays.stream() — the "old way" to get a stream
     */
    @Benchmark
    public void splitThenArrayStream(Blackhole bh) {
        Arrays.stream(COMMA.split(csvInput)).forEach(bh::consume);
    }

    // --- asPredicate vs lambda ---

    /**
     * Lambda wrapping Pattern.matcher().matches()
     */
    @Benchmark
    public void lambdaMatch(Blackhole bh) {
        long count = mixedInputs.stream()
                .filter(s -> DIGITS.matcher(s).matches())
                .count();
        bh.consume(count);
    }

    /**
     * asMatchPredicate() — full-string match predicate (Java 11+)
     */
    @Benchmark
    public void asMatchPredicate(Blackhole bh) {
        long count = mixedInputs.stream()
                .filter(DIGITS.asMatchPredicate())
                .count();
        bh.consume(count);
    }

    /**
     * asPredicate() — substring match predicate (find, not matches).
     * Semantic differs from the above — included for completeness.
     */
    @Benchmark
    public void asPredicateFind(Blackhole bh) {
        long count = mixedInputs.stream()
                .filter(DIGITS.asPredicate())
                .count();
        bh.consume(count);
    }
}
