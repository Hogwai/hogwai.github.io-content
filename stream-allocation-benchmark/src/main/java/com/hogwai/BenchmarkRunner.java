package com.hogwai;

import com.hogwai.benchmark.AllMatchBenchmark;
import com.hogwai.benchmark.AnyMatchBenchmark;
import com.hogwai.benchmark.CountBenchmark;
import com.hogwai.benchmark.FindFirstBenchmark;
import com.hogwai.benchmark.NoneMatchBenchmark;
import org.openjdk.jmh.profile.GCProfiler;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

public class BenchmarkRunner {

    static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(AnyMatchBenchmark.class.getSimpleName())
                .include(AllMatchBenchmark.class.getSimpleName())
                .include(NoneMatchBenchmark.class.getSimpleName())
                .include(FindFirstBenchmark.class.getSimpleName())
                .include(CountBenchmark.class.getSimpleName())
                .addProfiler(GCProfiler.class)
                .jvmArgsAppend("-XX:+UseCompactObjectHeaders")
                .build();

        new Runner(opt).run();
    }
}
