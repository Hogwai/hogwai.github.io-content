package com.hogwai;

import com.hogwai.benchmark.PatternCompileBenchmark;
import com.hogwai.benchmark.StringMethodsBenchmark;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

public class BenchmarkRunner {

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(PatternCompileBenchmark.class.getSimpleName())
                .include(StringMethodsBenchmark.class.getSimpleName())
                .build();

        new Runner(opt).run();
    }
}