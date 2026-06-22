package com.hogwai.benchmark;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Thread)
@Warmup(iterations = 3, time = 2, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 10, time = 2, timeUnit = TimeUnit.SECONDS)
@Fork(1)
public class PathMatchingBenchmark {

    // Glob matcher for Java files
    private static final PathMatcher GLOB_MATCHER = FileSystems.getDefault()
            .getPathMatcher("glob:**/*.java");

    // Equivalent regex matcher
    private static final PathMatcher REGEX_MATCHER = FileSystems.getDefault()
            .getPathMatcher("regex:.*\\.java");

    // Mixed paths: some match (.java), some don't
    private List<Path> paths;

    @Setup(Level.Trial)
    public void setUp() {
        paths = IntStream.range(0, 1000)
                .mapToObj(i -> {
                    String ext = i % 3 == 0 ? ".java" : (i % 3 == 1 ? ".class" : ".xml");
                    return Paths.get("src/main/java/com/example/Class" + i + ext);
                })
                .toList();
    }

    /**
     * Path matching with glob syntax
     */
    @Benchmark
    public void globPathMatching(Blackhole bh) {
        for (Path p : paths) {
            bh.consume(GLOB_MATCHER.matches(p));
        }
    }

    /**
     * Path matching with regex syntax (equivalent pattern)
     */
    @Benchmark
    public void regexPathMatching(Blackhole bh) {
        for (Path p : paths) {
            bh.consume(REGEX_MATCHER.matches(p));
        }
    }
}
