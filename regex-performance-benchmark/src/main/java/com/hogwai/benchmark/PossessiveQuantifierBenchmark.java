package com.hogwai.benchmark;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Thread)
@Warmup(iterations = 3, time = 2, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 10, time = 2, timeUnit = TimeUnit.SECONDS)
@Fork(1)
public class PossessiveQuantifierBenchmark {

    // Suffix-matching patterns
    private static final Pattern GREEDY_SUFFIX = Pattern.compile(".*\\.txt");
    private static final Pattern POSSESSIVE_SUFFIX = Pattern.compile(".*+\\.txt");

    // Catastrophic backtracking patterns
    private static final Pattern CATASTROPHIC = Pattern.compile("(a+)+b");
    private static final Pattern ATOMIC_FIX = Pattern.compile("(?>a+)+b");
    private static final Pattern POSSESSIVE_FIX = Pattern.compile("a++b");

    private String nonMatchingLong;
    private String catastrophicInput;

    @Setup(Level.Trial)
    public void setUp() {
        // Long string that does NOT end in .txt, forces greedy to backtrack
        nonMatchingLong = "a".repeat(500) + ".pdf";

        // Input that triggers catastrophic backtracking for (a+)+b
        // Length 22: ~2 million partition attempts before failing
        catastrophicInput = "a".repeat(22);
    }

    /**
     * Greedy .* backtracks character by character on non-matching input
     */
    @Benchmark
    public void greedySuffixMatching(Blackhole bh) {
        bh.consume(GREEDY_SUFFIX.matcher(nonMatchingLong).matches());
    }

    /**
     * Possessive .*+ fails immediately, no backtracking
     */
    @Benchmark
    public void possessiveSuffixMatching(Blackhole bh) {
        bh.consume(POSSESSIVE_SUFFIX.matcher(nonMatchingLong).matches());
    }

    /**
     * (a+)+b on all-'a' input, exponential backtracking
     */
    @Benchmark
    public void catastrophicBacktracking(Blackhole bh) {
        bh.consume(CATASTROPHIC.matcher(catastrophicInput).matches());
    }

    /**
     * (?>a+)+b, atomic group prevents backtracking into the inner a+
     */
    @Benchmark
    public void atomicGroupFix(Blackhole bh) {
        bh.consume(ATOMIC_FIX.matcher(catastrophicInput).matches());
    }

    /**
     * a++b, possessive quantifier never gives back characters
     */
    @Benchmark
    public void possessiveFix(Blackhole bh) {
        bh.consume(POSSESSIVE_FIX.matcher(catastrophicInput).matches());
    }
}
