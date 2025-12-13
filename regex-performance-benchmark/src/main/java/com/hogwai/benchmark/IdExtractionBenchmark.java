package com.hogwai.benchmark;

import com.hogwai.utils.TextExtractor;
import org.openjdk.jmh.annotations.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(1)
public class IdExtractionBenchmark {

    @Param({"1000", "10000"})
    private int size;

    private List<String> inputList;
    private static final Pattern PATTERN = Pattern.compile(".*_(.*)");

    @Setup(Level.Trial)
    public void setup() {
        inputList = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            inputList.add("facebook_" + i);
        }
    }

    @Benchmark
    public List<String> loopSubstring() {
        List<String> results = new ArrayList<>(size);
        for (String s : inputList) {
            int idx = s.lastIndexOf('_');
            results.add(s.substring(idx + 1));
        }
        return results;
    }

    @Benchmark
    public List<String> streamFastSplit() {
        return inputList.stream()
                        .map(s -> TextExtractor.getAfterLastSeparator(s, "_"))
                        .toList();
    }


    @Benchmark
    public List<String> loopSplit() {
        List<String> results = new ArrayList<>(size);
        for (String s : inputList) {
            results.add(s.split("_")[1]);
        }
        return results;
    }

    @Benchmark
    public List<String> streamRegex() {
        return inputList.stream()
                        .map(s -> {
                            Matcher m = PATTERN.matcher(s);
                            return m.find() ? m.group(1) : "";
                        })
                        .toList();
    }
}