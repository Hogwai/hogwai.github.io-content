package com.hogwai.benchmark;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Thread)
@Warmup(iterations = 3, time = 2, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 10, time = 2, timeUnit = TimeUnit.SECONDS)
@Fork(1)
public class PatternCompileBenchmark {

    private static final String EMAIL_REGEX = "^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,6}$";

    private static final Pattern PRECOMPILED_PATTERN = Pattern.compile(EMAIL_REGEX, Pattern.CASE_INSENSITIVE);

    private List<String> testData;

    @Setup(Level.Trial)
    public void setUp() {
        testData = IntStream.range(0, 1000)
                            .mapToObj("test.user%d@example.com"::formatted)
                            .toList();
    }

    /**
     * Bad practice: Compiling the pattern at each iteration
     */
    @Benchmark
    public void benchmarkCompileInLoop(Blackhole bh) {
        for (String email : testData) {
            Pattern pattern = Pattern.compile(EMAIL_REGEX, Pattern.CASE_INSENSITIVE);
            boolean matches = pattern.matcher(email).matches();
            bh.consume(matches);
        }
    }

    /**
     * Good practice: Reusing a compiled pattern
     */
    @Benchmark
    public void benchmarkPrecompiledPattern(Blackhole bh) {
        for (String email : testData) {
            boolean matches = PRECOMPILED_PATTERN.matcher(email).matches();
            bh.consume(matches);
        }
    }
}