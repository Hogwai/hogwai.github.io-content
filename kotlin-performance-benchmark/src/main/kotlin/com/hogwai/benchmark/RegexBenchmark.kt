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
import kotlin.text.Regex

// kotlin.text.Regex is a thin wrapper over java.util.regex.Pattern on the JVM,
// so the classic java.util.regex pitfalls reproduce unchanged. On top of that,
// Regex(...) and String.toRegex() have no compile cache, and findAll() is lazy.
@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
open class RegexBenchmark {

    private lateinit var line: String
    private lateinit var log: String
    private lateinit var csv: String

    private lateinit var idRegex: Regex
    private lateinit var idNonCapturing: Regex
    private lateinit var csvRegex: Regex
    private lateinit var javaPattern: java.util.regex.Pattern

    // Corpus is built once per trial and only read by the benchmarks below.
    @Setup(Level.Trial)
    open fun setup() {
        line = "2026-11-17T10:15:30Z INFO userId=12345 requestId=abcd-9876 status=200"

        val builder = StringBuilder()
        for (i in 1..500) {
            builder.append("2026-11-17T10:15:30Z INFO userId=").append(10000 + i)
            builder.append(" requestId=abcd-").append(9000 + i)
            builder.append(" status=200")
            if (i < 500) builder.append('\n')
        }
        log = builder.toString()

        csv = (1..200).joinToString(",")

        idRegex = Regex("userId=(\\d+)")
        idNonCapturing = Regex("userId=(?:\\d+)")
        csvRegex = Regex(",")
        @Suppress("kotlin:S1192")
        javaPattern = java.util.regex.Pattern.compile("userId=\\d+")
    }

    // Recompiles the pattern on every call, no cache in Regex(...).
    @Benchmark
    open fun compileInLoop(): Boolean = Regex("userId=\\d+").containsMatchIn(line)

    // Same pattern, compiled once in setup.
    @Benchmark
    open fun precompiled(): Boolean = idRegex.containsMatchIn(line)

    // Same trap as compileInLoop, spelled with toRegex().
    @Benchmark
    open fun toRegexInLoop(): Boolean = "userId=\\d+".toRegex().containsMatchIn(line)

    // Baseline: Pattern compiled once, Matcher per call. Kotlin adds only a wrapper.
    @Benchmark
    open fun javaPatternReuse(): Boolean = javaPattern.matcher(line).find()

    // findAll returns a lazy Sequence, so nothing is matched here.
    // This is the silent no-op trap: the sequence is never consumed.
    @Benchmark
    open fun findAllNotConsumed(): Sequence<MatchResult> = idRegex.findAll(log)

    // Terminal operation, so the matching actually runs.
    @Benchmark
    open fun findAllCount(): Int = idRegex.findAll(log).count()

    // Same work plus eager materialization of every match value.
    @Benchmark
    open fun findAllToList(): List<String> = idRegex.findAll(log).map { it.value }.toList()

    // Baseline for findAllCount: same loop, no sequence overhead.
    @Benchmark
    open fun javaMatcherLoop(): Int {
        val matcher = javaPattern.matcher(log)
        var count = 0
        while (matcher.find()) {
            count++
        }
        return count
    }

    // Literal delimiter, the regex engine is never used.
    @Benchmark
    open fun splitLiteral(): List<String> = csv.split(",")

    // Regex.split builds a Matcher internally.
    @Benchmark
    open fun splitRegex(): List<String> = csv.split(csvRegex)

    // Capturing group allocates the group values array on every match.
    @Benchmark
    open fun capturingGroup(): String? = idRegex.find(line)?.groupValues?.get(1)

    // Same find plus value extraction, without the capturing group array.
    @Benchmark
    open fun nonCapturingGroup(): String? = idNonCapturing.find(line)?.value

    // Regex.matches is a whole-string match, not find, so this returns false here.
    @Benchmark
    open fun matchesString(): Boolean = line.matches(idRegex)
}
