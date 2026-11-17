package com.hogwai.benchmark

import org.openjdk.jmh.annotations.Benchmark
import org.openjdk.jmh.annotations.BenchmarkMode
import org.openjdk.jmh.annotations.Level
import org.openjdk.jmh.annotations.Mode
import org.openjdk.jmh.annotations.OutputTimeUnit
import org.openjdk.jmh.annotations.Scope
import org.openjdk.jmh.annotations.Setup
import org.openjdk.jmh.annotations.State
import java.util.concurrent.TimeUnit

// The filter().count() family, kept separate from AllocationBenchmark because
// its dataset does not depend on the anyMatch scenario parameter.
@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
open class CountBenchmark {

    private lateinit var allMatchList: List<Int>
    private lateinit var noMatchList: List<Int>

    @Setup(Level.Trial)
    open fun setup() {
        allMatchList = (0 until SIZE).toList()
        noMatchList = (-SIZE until 0).toList()
    }

    // Inline and reads each element without a predicate object.
    @Benchmark
    open fun countInline(): Int = allMatchList.count { it >= 0 }

    // filter allocates a full List only for size to read its length, the classic misuse.
    @Benchmark
    open fun countFilterSize(): Int = allMatchList.filter { it >= 0 }.size

    @Benchmark
    open fun countSequence(): Int = allMatchList.asSequence().filter { it >= 0 }.count()

    @Benchmark
    open fun countStream(): Long = allMatchList.stream().filter { it >= 0 }.count()

    @Benchmark
    open fun countNoMatchInline(): Int = noMatchList.count { it >= 0 }

    @Benchmark
    open fun countNoMatchStream(): Long = noMatchList.stream().filter { it >= 0 }.count()

    private companion object {
        const val SIZE = 1000
    }
}
