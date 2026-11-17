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

@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
open class AllocationBenchmark {

    @Param("empty", "first", "mid", "last", "absent")
    var scenario: String = "first"

    private lateinit var list: List<Int>
    private lateinit var array: Array<Int>
    private lateinit var intArray: IntArray

    @Setup(Level.Trial)
    open fun setup() {
        val base = (0 until SIZE).toMutableList()
        when (scenario) {
            "first" -> base[0] = TARGET
            "mid" -> base[SIZE / 2] = TARGET
            "last" -> base[SIZE - 1] = TARGET
            "absent" -> Unit
            "empty" -> base.clear()
            else -> throw IllegalArgumentException("unknown scenario: $scenario")
        }

        list = base
        array = base.toTypedArray()
        intArray = base.toIntArray()
    }

    @Benchmark
    open fun anyInlineList(): Boolean = list.any { it == TARGET }

    // Baseline with no iterator and no inline probe: plain indexed access.
    @Benchmark
    open fun anyIndexedList(): Boolean {
        for (i in list.indices) {
            if (list[i] == TARGET) return true
        }
        return false
    }

    // asSequence() adds a wrapper and goes through an Iterator, unlike inline any.
    @Benchmark
    open fun anySequenceList(): Boolean = list.asSequence().any { it == TARGET }

    // java.util.stream builds a pipeline object per call, the cost this benchmark is meant to expose.
    @Benchmark
    open fun anyStreamList(): Boolean = list.stream().anyMatch { it == TARGET }

    // Array<Int> stores boxed values; any is still inline, so only the storage is boxed.
    @Benchmark
    open fun anyInlineArray(): Boolean = array.any { it == TARGET }

    // IntArray keeps primitives unboxed and any is inline, so this should be the cheapest variant.
    @Benchmark
    open fun anyInlineIntArray(): Boolean = intArray.any { it == TARGET }

    @Benchmark
    open fun anyIndexedIntArray(): Boolean {
        for (i in intArray.indices) {
            if (intArray[i] == TARGET) return true
        }
        return false
    }

    // Sequence<Int> boxes every element, unlike the Java IntStream specialisation.
    @Benchmark
    open fun anySequenceIntArray(): Boolean = intArray.asSequence().any { it == TARGET }

    // filter materialises a whole intermediate List before firstOrNull reads it.
    @Benchmark
    @Suppress("kotlin:S6527")
    open fun filterThenFirstList(): Int? = list.filter { it == TARGET }.firstOrNull()

    // Inline and lazy in effect: stops at the first match, allocates nothing.
    @Benchmark
    open fun firstOrNullList(): Int? = list.firstOrNull { it == TARGET }

    @Benchmark
    open fun sequenceFilterFirst(): Int? = list.asSequence().filter { it == TARGET }.firstOrNull()

    @Benchmark
    open fun streamFilterFirst(): Int? = list.stream().filter { it == TARGET }.findFirst().orElse(null)

    private companion object {
        const val SIZE = 1000
        const val TARGET = -1
    }
}
