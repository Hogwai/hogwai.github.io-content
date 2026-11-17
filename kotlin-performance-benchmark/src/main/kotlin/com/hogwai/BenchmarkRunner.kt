package com.hogwai

import com.hogwai.benchmark.AllocationBenchmark
import com.hogwai.benchmark.ContainsBenchmark
import com.hogwai.benchmark.CountBenchmark
import com.hogwai.benchmark.RegexBenchmark
import org.openjdk.jmh.profile.GCProfiler
import org.openjdk.jmh.runner.Runner
import org.openjdk.jmh.runner.options.OptionsBuilder
import org.openjdk.jmh.runner.options.TimeValue

fun main() {
    val options = OptionsBuilder()
        .include(AllocationBenchmark::class.java.simpleName)
        .include(CountBenchmark::class.java.simpleName)
        .include(ContainsBenchmark::class.java.simpleName)
        .include(RegexBenchmark::class.java.simpleName)
        .forks(3)
        .warmupIterations(5)
        .measurementIterations(10)
        .warmupTime(TimeValue.milliseconds(300))
        .measurementTime(TimeValue.milliseconds(300))
        .addProfiler(GCProfiler::class.java)
        .jvmArgsAppend("-XX:+UseCompactObjectHeaders")
        .build()

    Runner(options).run()
}
