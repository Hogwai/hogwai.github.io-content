package com.hogwai.benchmark;

import com.hogwai.util.Iterables;
import org.eclipse.collections.impl.utility.Iterate;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.eclipse.collections.api.block.predicate.Predicate;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Thread)
@Warmup(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(1)
public class CountBenchmark {

    private static final int NON_EMPTY_SIZE = 100_000;

    @Param({"empty", "none", "half", "all"})
    private String scenario;

    private List<Integer> values;
    private Predicate<Integer> predicate;

    @Setup(Level.Trial)
    public void setup() {
        int size = "empty".equals(scenario) ? 0 : NON_EMPTY_SIZE;
        List<Integer> list = new ArrayList<>(size);
        for (int i = 1; i <= size; i++) {
            list.add(i);
        }
        values = List.copyOf(list);
        int bound = switch (scenario) {
            case "half" -> size / 2;
            case "all" -> size;
            default -> 0;
        };
        predicate = value -> value <= bound;
    }

    @Benchmark
    public long jdkStream() {
        return values.stream().filter(predicate).count();
    }

    @Benchmark
    public long jdkStreamGuarded() {
        if (values.isEmpty()) {
            return 0L;
        }
        return values.stream().filter(predicate).count();
    }

    @Benchmark
    public long helperLoop() {
        return Iterables.count(values, predicate);
    }

    @Benchmark
    public int eclipseCollections() {
        return Iterate.count(values, predicate);
    }
}
