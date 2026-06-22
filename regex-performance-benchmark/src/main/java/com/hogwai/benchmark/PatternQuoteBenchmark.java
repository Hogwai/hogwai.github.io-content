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
public class PatternQuoteBenchmark {

    // Input that does NOT contain regex metacharacters — best case for unquoted
    private static final String SAFE_INPUT = "hello123";

    // Input that contains regex metacharacters — will break the pattern if unquoted
    private static final String DANGEROUS_INPUT = "hello.world (test) [1]";

    // Pre-compiled patterns (compile cost excluded from matching benchmarks)
    private static final Pattern QUOTED_SAFE = Pattern.compile(".*" + Pattern.quote(SAFE_INPUT) + ".*");
    private static final Pattern UNQUOTED_SAFE = Pattern.compile(".*" + SAFE_INPUT + ".*");
    private static final Pattern QUOTED_DANGEROUS = Pattern.compile(".*" + Pattern.quote(DANGEROUS_INPUT) + ".*");

    // Note: UNQUOTED_DANGEROUS would be semantically different (DANGEROUS_INPUT contains regex metacharacters),
    // so we don't benchmark matching with it — the behavior would differ, not just performance.

    private String matchingInput;
    private String nonMatchingInput;

    @Setup(Level.Trial)
    public void setUp() {
        matchingInput = "prefix-hello123-suffix";
        nonMatchingInput = "prefix-HELLO123-suffix";
    }

    // --- Compile-time benchmarks ---

    /**
     * Compiling a pattern with Pattern.quote() for safe input
     */
    @Benchmark
    public void compileWithQuoteSafeInput(Blackhole bh) {
        bh.consume(Pattern.compile(".*" + Pattern.quote(SAFE_INPUT) + ".*"));
    }

    /**
     * Compiling a pattern without quote for safe input
     */
    @Benchmark
    public void compileWithoutQuoteSafeInput(Blackhole bh) {
        bh.consume(Pattern.compile(".*" + SAFE_INPUT + ".*"));
    }

    /**
     * Compiling a pattern with Pattern.quote() for dangerous input (contains metacharacters)
     */
    @Benchmark
    public void compileWithQuoteDangerousInput(Blackhole bh) {
        bh.consume(Pattern.compile(".*" + Pattern.quote(DANGEROUS_INPUT) + ".*"));
    }

    // --- Runtime matching benchmarks (pre-compiled patterns) ---

    /**
     * Matching with a quoted-safe pattern — matching against matching input
     */
    @Benchmark
    public void quotedSafeMatchingMatch(Blackhole bh) {
        bh.consume(QUOTED_SAFE.matcher(matchingInput).matches());
    }

    /**
     * Matching with an unquoted-safe pattern — matching against matching input
     */
    @Benchmark
    public void unquotedSafeMatchingMatch(Blackhole bh) {
        bh.consume(UNQUOTED_SAFE.matcher(matchingInput).matches());
    }

    /**
     * Matching with a quoted-safe pattern — matching against non-matching input
     */
    @Benchmark
    public void quotedSafeMatchingNoMatch(Blackhole bh) {
        bh.consume(QUOTED_SAFE.matcher(nonMatchingInput).matches());
    }

    /**
     * Matching with a quoted-dangerous pattern — matching against input containing dots and parens
     */
    @Benchmark
    public void quotedDangerousMatching(Blackhole bh) {
        String input = "prefix-" + DANGEROUS_INPUT + "-suffix";
        bh.consume(QUOTED_DANGEROUS.matcher(input).matches());
    }
}
