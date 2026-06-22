package com.hogwai;

import com.hogwai.benchmark.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

public class BenchmarkRunner {

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(PatternCompileBenchmark.class.getSimpleName())
                .include(StringMethodsBenchmark.class.getSimpleName())
                .include(IdExtractionBenchmark.class.getSimpleName())
                .include(CapturingGroupsBenchmark.class.getSimpleName())
                .include(PossessiveQuantifierBenchmark.class.getSimpleName())
                .include(PatternQuoteBenchmark.class.getSimpleName())
                .include(ModernPatternAPIBenchmark.class.getSimpleName())
                .include(PathMatchingBenchmark.class.getSimpleName())
                .build();

        new Runner(opt).run();
    }
}
