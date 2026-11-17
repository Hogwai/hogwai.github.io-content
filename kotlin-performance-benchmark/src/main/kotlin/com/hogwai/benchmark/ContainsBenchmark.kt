package com.hogwai.benchmark

import org.openjdk.jmh.annotations.Benchmark
import org.openjdk.jmh.annotations.BenchmarkMode
import org.openjdk.jmh.annotations.Level
import org.openjdk.jmh.annotations.Mode
import org.openjdk.jmh.annotations.OutputTimeUnit
import org.openjdk.jmh.annotations.Param
import org.openjdk.jmh.annotations.Scope
import org.openjdk.jmh.annotations.Setup
import org.openjdk.jmh.annotations.State
import java.util.concurrent.TimeUnit

/**
 * Cost of membership tests on Kotlin collections.
 *
 * Kotlin adds no algorithmic change here: List.contains is a linear scan, so it is O(n).
 * List.containsAll walks the argument and scans the receiver for each element, so it is O(n*m).
 * The idiomatic surface hides this, since `in`, `listOf` and `toSet()` all read the same at a glance.
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
open class ContainsBenchmark {

    @Param("10", "1000", "100000")
    var size: Int = 1000

    private lateinit var list: List<Int>
    private lateinit var set: Set<Int>
    private lateinit var presentList: List<Int>
    private lateinit var absentList: List<Int>
    private var probePresent: Int = 0
    private var probeAbsent: Int = 0

    @Setup(Level.Trial)
    fun setUp() {
        list = (0 until size).toList()
        set = list.toSet()
        // Subset of list, so containsAll can only succeed after scanning the receiver.
        presentList = list.filter { it % 100 == 0 }
        absentList = (size until size * 2).toList()
        probePresent = size / 2
        probeAbsent = -1
    }

    @Benchmark
    open fun listContainsPresent(): Boolean = list.contains(probePresent)

    @Benchmark
    open fun listContainsAbsent(): Boolean = list.contains(probeAbsent)

    // The `in` operator compiles to List.contains, so this is the same O(n) scan.
    @Benchmark
    open fun listInOperator(): Boolean = probePresent in list

    @Benchmark
    open fun setContainsPresent(): Boolean = set.contains(probePresent)

    @Benchmark
    open fun setContainsAbsent(): Boolean = set.contains(probeAbsent)

    @Benchmark
    open fun listContainsAllPresent(): Boolean = list.containsAll(presentList)

    @Benchmark
    open fun listContainsAllAbsent(): Boolean = list.containsAll(absentList)

    // Conversion measured inside the operation, as an on-the-fly idiomatic fix would do it.
    @Benchmark
    open fun listToSetContainsAllPresent(): Boolean = list.toSet().containsAll(presentList)

    @Benchmark
    open fun listToSetContainsAllAbsent(): Boolean = list.toSet().containsAll(absentList)

    @Benchmark
    open fun setContainsAllPresent(): Boolean = set.containsAll(presentList)

    @Benchmark
    open fun setContainsAllAbsent(): Boolean = set.containsAll(absentList)

    // Idiomatic Kotlin that looks smart: intersect allocates a Set of the smaller side, then isEmpty.
    @Benchmark
    open fun listIntersectIsEmpty(): Boolean = list.intersect(presentList).isEmpty()
}
