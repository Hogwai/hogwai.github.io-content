package com.hogwai.benchmark;

import org.openjdk.jmh.annotations.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Thread)
@Warmup(iterations = 2, time = 2, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 3, time = 2, timeUnit = TimeUnit.SECONDS)
@Fork(1)
public class ContainsBenchmark {

    @Param({"10", "1000", "100000"})
    private int size;

    private List<Integer> list;
    private Set<Integer> set;
    private List<Integer> subset;
    private List<Integer> absentSubset;

    @Setup(Level.Trial)
    public void setup() {
        list = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            list.add(i);
        }
        set = new HashSet<>(list);

        // Sous-ensembles de taille 10 % de la collection
        int subSize = Math.max(1, size / 10);
        subset = new ArrayList<>(list.subList(0, subSize));
        absentSubset = new ArrayList<>();
        for (int i = size; i < size + subSize; i++) {
            absentSubset.add(i);
        }
    }

    // contains
    @Benchmark
    public boolean listContainsPresent() {
        return list.contains(size / 2);
    }

    @Benchmark
    public boolean listContainsAbsent() {
        return list.contains(size + 1);
    }

    @Benchmark
    public boolean setContainsPresent() {
        return set.contains(size / 2);
    }

    @Benchmark
    public boolean setContainsAbsent() {
        return set.contains(size + 1);
    }

    // containsAll
    @Benchmark
    public boolean listContainsAllPresent() {
        return list.containsAll(subset);
    }

    @Benchmark
    public boolean listContainsAllAbsent() {
        return list.containsAll(absentSubset);
    }

    @Benchmark
    public boolean listConvertedToSetContainsAllPresent() {
        return new HashSet<>(list).containsAll(subset);
    }

    @Benchmark
    public boolean listConvertedToSetContainsAllAbsent() {
        return new HashSet<>(list).containsAll(absentSubset);
    }

    @Benchmark
    public boolean setContainsAllPresent() {
        return set.containsAll(subset);
    }

    @Benchmark
    public boolean setContainsAllAbsent() {
        return set.containsAll(absentSubset);
    }

}
