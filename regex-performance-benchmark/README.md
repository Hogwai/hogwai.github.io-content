# regex-performance-benchmark

This project provides JMH benchmarks to demonstrate the performance impact of different `java.util.regex.Pattern` usage patterns in Java.

## Prerequisites

* **JDK 21** or a later version.
* **Apache Maven** 3.6+ or later.
* An IDE like IntelliJ IDEA or Eclipse (optional, but recommended for easy execution).

## Project Structure

```shell
regex-performance-benchmark/
├── pom.xml
└── src/
    └── main/
        └── java/
            └── com/
                └── hogwai/
                    ├── BenchmarkRunner.java              # Main class to run all benchmarks
                    └── benchmark/
                        ├── PatternCompileBenchmark.java   # Benchmark for Pattern compilation
                        ├── StringMethodsBenchmark.java    # Benchmark for String regex methods
                        ├── IdExtractionBenchmark.java     # Benchmark for ID extraction (substring vs regex)
                        ├── CapturingGroupsBenchmark.java  # Benchmark for capturing vs non-capturing groups
                        ├── PossessiveQuantifierBenchmark.java # Benchmark for greedy/possessive/backtracking
                        ├── PatternQuoteBenchmark.java     # Benchmark for Pattern.quote() overhead
                        ├── ModernPatternAPIBenchmark.java # Benchmark for splitAsStream, asPredicate
                        └── PathMatchingBenchmark.java     # Benchmark for glob vs regex path matching
```

## How to Run the Benchmarks

The easiest way to run the benchmarks is by using the provided `BenchmarkRunner.java` class.

### Step 1: Clone and Build the Project

```bash
git clone https://github.com/Hogwai/hogwai.github.io-content.git

cd regex-performance-benchmark

mvn clean compile
```

### Step 2: Execute the Benchmarks

#### Method 1: From the IDE

1. Open the project in your IDE (IntelliJ, Eclipse, etc.).
2. Navigate to `src/main/java/com/hogwai/BenchmarkRunner.java`.
3. Right-click the `main` method and select **"Run 'BenchmarkRunner.main()'"**.

#### Method 2: From the Command Line

```bash
mvn package
java -jar target/regex-performance-benchmark.jar
```

## Results

Benchmarks run on: JDK 25.0.3 (OpenJDK 64-Bit Server VM), 1 fork, 3 warmup iterations (2s each), 10 measurement iterations (2s each).

### Pattern Compilation

| Benchmark                                             | Mode | Cnt | Score (ns/op) | Error (ns/op) |
|-------------------------------------------------------|------|-----|---------------|---------------|
| `PatternCompileBenchmark.benchmarkCompileInLoop`      | avgt | 10  | ~1,057,404    | ± 81,503      |
| `PatternCompileBenchmark.benchmarkPrecompiledPattern` | avgt | 10  | ~427,757      | ± 141,873     |

**Takeaway:** Compiling inside a loop is ~2.5x slower than reusing a precompiled pattern over 1000 inputs.

### String Regex Methods

| Benchmark                                        | Mode | Cnt | Score (ns/op) | Error (ns/op) |
|--------------------------------------------------|------|-----|---------------|---------------|
| `StringMethodsBenchmark.benchmarkStringMatches`  | avgt | 10  | ~501,527      | ± 67,565      |
| `StringMethodsBenchmark.benchmarkPatternMatcher` | avgt | 10  | ~455,691      | ± 70,202      |

**Takeaway:** `String.matches()` is consistently slower than using a pre-compiled `Pattern`.

### ID Extraction

| Benchmark                               | Size  | Mode | Cnt | Score (ns/op) | Error (ns/op) |
|-----------------------------------------|-------|------|-----|---------------|---------------|
| `IdExtractionBenchmark.loopSubstring`   | 1000  | avgt | 5   | 11,066.1      | ± 5,344.0     |
| `IdExtractionBenchmark.loopSubstring`   | 10000 | avgt | 5   | 93,171.7      | ± 2,352.2     |
| `IdExtractionBenchmark.streamFastSplit` | 1000  | avgt | 5   | 9,780.9       | ± 164.5       |
| `IdExtractionBenchmark.streamFastSplit` | 10000 | avgt | 5   | 93,998.1      | ± 925.9       |
| `IdExtractionBenchmark.loopSplit`       | 1000  | avgt | 5   | 26,542.3      | ± 1,340.6     |
| `IdExtractionBenchmark.loopSplit`       | 10000 | avgt | 5   | 270,845.0     | ± 10,456.9    |
| `IdExtractionBenchmark.streamRegex`     | 1000  | avgt | 5   | 32,968.0      | ± 751.6       |
| `IdExtractionBenchmark.streamRegex`     | 10000 | avgt | 5   | 346,222.6     | ± 4,993.4     |

**Takeaway:** Manual `lastIndexOf`/`substring` (streamFastSplit) is the fastest extraction method, ~2.7x faster than `String.split()` and ~3.4x faster than regex. The gap narrows at larger sizes as stream overhead is amortized.

### Capturing Groups

| Benchmark                                             | Mode | Cnt | Score (ns/op) | Error (ns/op) |
|-------------------------------------------------------|------|-----|---------------|---------------|
| `CapturingGroupsBenchmark.capturingUnused`            | avgt | 10  | 24,313.9      | ± 207.7       |
| `CapturingGroupsBenchmark.nonCapturing`               | avgt | 10  | 24,858.1      | ± 100.8       |
| `CapturingGroupsBenchmark.positionalGroupExtraction`  | avgt | 10  | 33,282.3      | ± 610.8       |
| `CapturingGroupsBenchmark.namedGroupExtraction`       | avgt | 10  | 64,973.6      | ± 549.2       |
| `CapturingGroupsBenchmark.nonCapturingWithExtraction` | avgt | 10  | 32,142.5      | ± 219.5       |

**Takeaway:** On modern JDK, unused capturing groups incur almost no overhead, the JIT can optimize them away. However, when extraction is needed, named group access is ~2x slower than positional due to the HashMap lookup. Manual extraction after a non-capturing match performs similarly to positional group extraction.

### Matching Performance & Possessive Quantifiers

| Benchmark                                                | Mode | Cnt | Score (ns/op) | Error (ns/op) |
|----------------------------------------------------------|------|-----|---------------|---------------|
| `PossessiveQuantifierBenchmark.greedySuffixMatching`     | avgt | 10  | 607.2         | ± 5.9         |
| `PossessiveQuantifierBenchmark.possessiveSuffixMatching` | avgt | 10  | 329.7         | ± 3.2         |
| `PossessiveQuantifierBenchmark.catastrophicBacktracking` | avgt | 10  | 1,311.9       | ± 18.2        |
| `PossessiveQuantifierBenchmark.atomicGroupFix`           | avgt | 10  | 23.0          | ± 0.8         |
| `PossessiveQuantifierBenchmark.possessiveFix`            | avgt | 10  | 28.2          | ± 0.4         |

**Takeaway:** Possessive quantifiers are ~1.8x faster for suffix matching on non-matching input. Catastrophic backtracking is ~57x slower than the atomic/possessive fix, and the gap grows exponentially with input length.

### Pattern.quote()

| Benchmark                                              | Mode | Cnt | Score (ns/op) | Error (ns/op) |
|--------------------------------------------------------|------|-----|---------------|---------------|
| `PatternQuoteBenchmark.compileWithQuoteSafeInput`      | avgt | 10  | 76.5          | ± 0.7         |
| `PatternQuoteBenchmark.compileWithoutQuoteSafeInput`   | avgt | 10  | 55.4          | ± 7.5         |
| `PatternQuoteBenchmark.compileWithQuoteDangerousInput` | avgt | 10  | 131.6         | ± 6.3         |
| `PatternQuoteBenchmark.quotedSafeMatchingMatch`        | avgt | 10  | 65.2          | ± 0.5         |
| `PatternQuoteBenchmark.unquotedSafeMatchingMatch`      | avgt | 10  | 64.7          | ± 0.3         |
| `PatternQuoteBenchmark.quotedSafeMatchingNoMatch`      | avgt | 10  | 34.5          | ± 0.4         |
| `PatternQuoteBenchmark.quotedDangerousMatching`        | avgt | 10  | 112.0         | ± 1.1         |

**Takeaway:** `Pattern.quote()` adds ~21 ns of compile-time overhead for safe input (~38%) and ~76 ns for input with metacharacters. At runtime, there is no measurable difference between quoted and unquoted matching. The safety benefit far outweighs the tiny compile cost.

### Modern Pattern API

| Benchmark                                        | Mode | Cnt | Score (ns/op) | Error (ns/op) |
|--------------------------------------------------|------|-----|---------------|---------------|
| `ModernPatternAPIBenchmark.splitToArray`         | avgt | 10  | 11,264.3      | ± 263.1       |
| `ModernPatternAPIBenchmark.splitToStream`        | avgt | 10  | 13,409.2      | ± 226.7       |
| `ModernPatternAPIBenchmark.splitThenArrayStream` | avgt | 10  | 11,477.2      | ± 718.4       |
| `ModernPatternAPIBenchmark.lambdaMatch`          | avgt | 10  | 8,676.5       | ± 166.3       |
| `ModernPatternAPIBenchmark.asMatchPredicate`     | avgt | 10  | 9,780.0       | ± 53.8        |
| `ModernPatternAPIBenchmark.asPredicateFind`      | avgt | 10  | 14,391.9      | ± 199.8       |

**Takeaway:** `splitAsStream()` is slightly slower than `split()` when consuming all tokens, the stream overhead offsets the lazy allocation benefit. `asMatchPredicate()` is slightly slower than a raw lambda (stream wrapping overhead). These methods are about **convenience and clarity**, not raw speed.

### Globbing vs Regex Path Matching

| Benchmark                                 | Mode | Cnt | Score (ns/op) | Error (ns/op) |
|-------------------------------------------|------|-----|---------------|---------------|
| `PathMatchingBenchmark.globPathMatching`  | avgt | 10  | 130,398.8     | ± 2,434.9     |
| `PathMatchingBenchmark.regexPathMatching` | avgt | 10  | 71,750.3      | ± 3,205.3     |

**Takeaway:** Glob matching is ~1.8x slower than regex when both go through `FileSystem.getPathMatcher()`. This is because glob patterns are converted to regex internally, adding overhead. Choose glob for **readability and simplicity**, not performance.

## Key Takeaways

1. **Re-compiling is expensive**: `benchmarkCompileInLoop` is ~2.5x slower than the pre-compiled version.
2. **`String` methods are a trap**: `String.matches()` is slower than using a pre-compiled `Pattern`.
3. **Pre-compilation is key**: Using a `private static final Pattern` is the most performant approach.
4. **Non-capturing groups matter when extraction is used**: Named group extraction is ~2x slower than positional due to HashMap lookup.
5. **Possessive quantifiers eliminate backtracking**: `.*+` is ~1.8x faster than `.*` on non-matching input. The gap grows with input length.
6. **Catastrophic backtracking is real and dangerous**: The atomic/possessive fix is ~57x faster on a 22-character input. At 30+ characters the difference becomes astronomical.
7. **`Pattern.quote()` has negligible overhead**: ~21 ns at compile time, zero at runtime. No excuse to skip it.
8. **Modern Pattern API methods are convenient, not faster**: `splitAsStream()` and `asPredicate()` improve readability, not raw throughput.
9. **Use glob for readability, regex for speed**: Glob is ~1.8x slower for path matching via `PathMatcher`.
