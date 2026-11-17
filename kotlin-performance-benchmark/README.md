# kotlin-performance-benchmark

Kotlin/JVM counterpart to the Java performance projects in this repository:

* [stream-allocation-benchmark](/stream-allocation-benchmark): eager stream terminal operations allocate 144 to 208 B/op and run 3 to 4 times slower than a loop.
* [collections-issues-benchmark](/collections-issues-benchmark): `List.contains` is O(n), `List.containsAll` is O(n*m).
* [regex-performance-benchmark](/regex-performance-benchmark): recompiling a pattern, capturing groups, backtracking.

The question is which of those pitfalls exist in Kotlin, which ones the language removes, and which new ones it adds.

## What is measured

| Benchmark | Compares | Parameters |
|---|---|---|
| `AllocationBenchmark` | `any {}` inline, indexed loops, `asSequence()`, `stream()`, on `List<Int>`, `Array<Int>` and `IntArray` | `empty`, `first`, `mid`, `last`, `absent` on 1000 elements |
| `CountBenchmark` | `count {}`, `filter {}.size`, sequence and stream `filter().count()` | all match, plus two all-miss methods |
| `ContainsBenchmark` | `List` vs `Set`, `contains` and `containsAll`, `toSet()` conversion, `intersect` | 10, 1 000, 100 000 elements |
| `RegexBenchmark` | `Regex(...)` vs precompiled, lazy `findAll`, `Matcher` loop, `split` with `String` vs `Regex`, capturing groups | a log line and a 500 line log |

## Prerequisites

* **JDK 21** or later. The runner appends `-XX:+UseCompactObjectHeaders`, which requires JDK 25.
* **Apache Maven** 3.6 or later.
* An IDE like IntelliJ IDEA or Eclipse (optional).

## Project Structure

```shell
kotlin-performance-benchmark/
├── pom.xml
└── src/main/kotlin/com/hogwai/
    ├── BenchmarkRunner.kt                    # Main class to run all benchmarks
    └── benchmark/
        ├── AllocationBenchmark.kt            # Inline extensions, Sequence, Stream, loops, arrays
        ├── CountBenchmark.kt                 # filter().count() family
        ├── ContainsBenchmark.kt              # List vs Set membership
        └── RegexBenchmark.kt                 # Recompilation, lazy findAll, split
```

JMH generates its harness in Java. The Kotlin sources therefore go through `kapt` with `jmh-generator-annprocess`, and the Kotlin compile step is bound to `process-sources` so it runs before `javac` compiles the generated harness.

## How to run the benchmarks

### Build the project

```bash
mvn clean package
```

### Execute the benchmarks

#### From the IDE

Run the `main` function in `src/main/kotlin/com/hogwai/BenchmarkRunner.kt`. It selects the four benchmark classes and bakes the settings used for the results below: 3 forks, 5 warmup and 10 measurement iterations of 300 ms, GC profiler, and `-XX:+UseCompactObjectHeaders` on every forked JVM.

#### From the command line

The shaded jar launches `org.openjdk.jmh.Main`, which does not know the options baked into `BenchmarkRunner`. Pass them explicitly:

```bash
java -jar target/kotlin-performance-benchmark.jar -prof gc -f 3 -wi 5 -i 10 -w 300ms -r 300ms \
  -jvmArgsAppend "-XX:+UseCompactObjectHeaders"
```

To run a single class:

```bash
java -jar target/kotlin-performance-benchmark.jar AllocationBenchmark -prof gc
```

## Results

Measured with Eclipse Temurin JDK 25.0.3 on Linux (WSL2), 3 forks, 5 warmup and 10 measurement iterations of 300 ms, `-prof gc`, and `-XX:+UseCompactObjectHeaders` without a heap override. Allocation is `gc.alloc.rate.norm` in B/op, the average bytes allocated by one call. `~0` marks scores at the profiler detection floor.

### AnyMatch

Time in ns/op, lower is better.

| Method | empty | first | mid | last | absent |
|---|---:|---:|---:|---:|---:|
| `list.any {}` | 0.35 | 0.71 | 137.36 | 280.62 | 327.17 |
| indexed `for` on `List` | 0.35 | 0.67 | 143.38 | 281.64 | 297.53 |
| `list.asSequence().any {}` | 0.34 | 0.71 | 137.35 | 332.53 | 364.62 |
| `list.stream().anyMatch {}` | 9.39 | 10.67 | 662.75 | 1 337.48 | 1 154.13 |
| `Array<Int>.any {}` | 0.26 | 0.37 | 57.87 | 184.90 | 118.94 |
| `IntArray.any {}` | **0.26** | 0.36 | 39.04 | 77.99 | 78.14 |
| indexed `for` on `IntArray` | 0.27 | 0.37 | 39.17 | **77.76** | **77.67** |
| `IntArray.asSequence().any {}` | 0.26 | 0.36 | 57.86 | 93.36 | 113.17 |

Allocation in B/op, lower is better.

| Method | empty | first | mid | last | absent |
|---|---:|---:|---:|---:|---:|
| `list.any {}` | ~0 | ~0 | ~0 | ~0 | ~0 |
| indexed `for` on `List` | ~0 | ~0 | ~0 | ~0 | ~0 |
| `list.asSequence().any {}` | ~0 | ~0 | ~0 | 24.01 | 24.01 |
| `list.stream().anyMatch {}` | 152.00 | 152.00 | 152.01 | 138.69 | 152.02 |
| `Array<Int>.any {}` | ~0 | ~0 | ~0 | ~0 | ~0 |
| `IntArray.any {}` | ~0 | ~0 | ~0 | ~0 | ~0 |
| indexed `for` on `IntArray` | ~0 | ~0 | ~0 | ~0 | ~0 |
| `IntArray.asSequence().any {}` | ~0 | ~0 | ~0 | ~0 | ~0 |

`stream().anyMatch {}` is the only variant that allocates on every call, early exit included, and it is 4 to 5 times slower on a full scan. The inline extensions allocate nothing measurable: the `ArrayList` iterator is scalar replaced by escape analysis, so `list.any {}` matches the indexed loop at 280 ns against 282 ns.

### FindFirst

Time in ns/op, lower is better.

| Method | empty | first | mid | last | absent |
|---|---:|---:|---:|---:|---:|
| `list.firstOrNull {}` | 0.36 | **0.72** | 138.37 | 281.74 | 280.76 |
| `list.filter {}.firstOrNull()` | 0.36 | 325.35 | 323.16 | 325.77 | 319.14 |
| `list.asSequence().filter {}.firstOrNull()` | 0.36 | 0.68 | 137.48 | 281.56 | 279.67 |
| `list.stream().filter {}.findFirst()` | 11.06 | 13.90 | 633.04 | 1 244.76 | 1 231.60 |

Allocation in B/op, lower is better.

| Method | empty | first | mid | last | absent |
|---|---:|---:|---:|---:|---:|
| `list.firstOrNull {}` | ~0 | ~0 | ~0 | ~0 | ~0 |
| `list.filter {}.firstOrNull()` | ~0 | 56.01 | 56.01 | 56.01 | 24.01 |
| `list.asSequence().filter {}.firstOrNull()` | ~0 | ~0 | ~0 | ~0 | ~0 |
| `list.stream().filter {}.findFirst()` | 176.00 | 192.00 | 192.01 | 192.03 | 176.03 |

`firstOrNull {}` does exactly what `filter().findFirst()` does, without the intermediate list: 0.72 ns against 325 ns on the first hit. `filter {}.firstOrNull()` pays for a full `ArrayList` of matches before looking at the first one.

### Count

| Method | ns/op | B/op |
|---|---:|---:|
| `list.count {}` | **326.49** | **~0** |
| `list.filter {}.size` | 3 190.05 | 14 936.07 |
| `list.asSequence().filter {}.count()` | 529.44 | 40.01 |
| `list.stream().filter {}.count()` | 787.23 | 216.02 |
| `list.count {}` on an all-miss list | 325.66 | ~0 |
| `list.stream().filter {}.count()` on an all-miss list | 629.13 | 200.01 |

`filter {}.size` is the Kotlin translation of the Java `filter().count()` habit, and it is the worst variant: it builds the full result list, a 1000 element `ArrayList` at 14 936 B/op, to read its size, for about 10 times the time.

### Contains

Time in µs/op, lower is better.

| Method | 10 | 1 000 | 100 000 |
|---|---:|---:|---:|
| `list.contains(present)` | 0.004 | 0.162 | 16.797 |
| `list.contains(absent)` | 0.006 | 0.311 | 31.870 |
| `present in list` | 0.004 | 0.162 | 16.816 |
| `set.contains(present)` | 0.001 | 0.002 | **0.002** |
| `set.contains(absent)` | 0.001 | 0.001 | **0.001** |
| `list.containsAll(present)` | 0.003 | 1.355 | 16 896.449 |
| `list.containsAll(absent)` | 0.005 | 0.328 | 32.292 |
| `list.toSet().containsAll(present)` | 0.069 | 6.572 | 1 788.622 |
| `list.toSet().containsAll(absent)` | 0.068 | 6.458 | 1 737.858 |
| `set.containsAll(present)` | 0.004 | 0.032 | 3.330 |
| `set.containsAll(absent)` | 0.003 | 0.003 | 0.003 |
| `list.intersect(present).isEmpty()` | 0.031 | 4.178 | 51 116.660 |

At 100 000 elements, `List.contains` is about 8 400 times slower than `Set.contains`, and `List.containsAll` reaches 16.9 ms. This is the JDK code, not a Kotlin implementation: `in` compiles to `List.contains`, and nothing in the language discourages it.

The `toSet()` conversion measured inside the call costs about 1.8 ms at 100 000 elements, so it only pays off when the same collection is queried many times. The `intersect` one-liner is not a fix either: it is the slowest variant in the table. Its implementation walks the receiver and calls `contains` on the argument, so passing a `List` makes it O(n*m), exactly the trap it looks like it avoids.

### Regex

Time in ns/op and allocation in B/op.

| Method | ns/op | B/op |
|---|---:|---:|
| `Regex("userId=\\d+")` in the benchmark | 102.0 | 1 240.00 |
| `"userId=\\d+".toRegex()` in the benchmark | 102.0 | 1 240.00 |
| precompiled `Regex("userId=(\\d+)")` | 39.0 | 136.00 |
| precompiled `java.util.regex.Pattern` | **31.0** | 128.00 |
| `findAll(log)` never consumed | **2.0** | 40.00 |
| `findAll(log).count()` | 29 409.0 | 88 200.62 |
| `findAll(log).map { it.value }.toList()` | 32 473.0 | 118 936.69 |
| `Matcher.find()` loop | 20 770.0 | 128.44 |
| `csv.split(",")` | 1 694.0 | 11 016.04 |
| `csv.split(Regex(","))` | 2 055.0 | 11 072.04 |
| `find(line)?.groupValues?.get(1)` | 46.0 | 240.00 |
| non capturing `find(line)?.value` | 47.0 | 216.00 |
| `line.matches(regex)` | 8.0 | 64.00 |

Kotlin runs the same `java.util.regex` engine, so the ranking is the same as in the Java project. Three Kotlin-specific observations:

* `Regex(...)` and `toRegex()` have no compile cache: 102 ns against 39 ns for a precompiled pattern, on a 13 character source string. The gap grows with pattern complexity.
* `findAll` returns a lazy `Sequence`. `findAllNotConsumed` looks free at 2 ns because it does nothing. You get 29 µs only once a terminal operation is called.
* Once consumed, `findAll` allocates 88 KB per call on 500 matches, about 176 B per match, against 128 B total for the `Matcher` loop. Each step of the sequence allocates its own iterator plus a `Matcher` and a `MatchResult` per occurrence.

`matchesString` is the `String.matches` trap rather than a performance comparison: it is a whole string match, so it returns `false` here and exits on the first character.

## Key Takeaways

1. **The Java stream allocation problem does not reproduce.** `any {}`, `all {}`, `none {}`, `count {}` and `firstOrNull {}` are `inline` and compile to the loop you would write by hand. They allocate nothing measurable, against 152 B/op for `stream().anyMatch {}`, which is also 4 to 5 times slower on a full scan.
2. **`Sequence` is not the Kotlin `Stream`, and it is not free.** It adds up to 24 B/op on a `List` and makes full scans 11 to 45 percent slower, depending on the source type. Its `Int` sequences box at the API level; escape analysis removes that in these microbenchmarks, but not once values are retained.
3. **The equivalent of `filter().count()` is `count {}`.** `filter {}.size` builds the whole result list: 14 936 B/op and 10 times the time.
4. **Boxing is where Kotlin costs more than Java.** `IntArray.any {}` scans 1000 elements in 78 ns; `List<Int>` and `Array<Int>` take 281 and 185 ns. There is no `IntStream` equivalent on `Sequence`, so the performance answer is primitive arrays plus inline extensions.
5. **Collections are untouched.** `contains` and `containsAll` are JDK implementations behind a Kotlin facade, with the same O(n) and O(n*m) behaviour. `in`, `listOf` and `toSet()` make the trap easier to write, not harder.
6. **Regex is the same engine, with two extra traps:** no compile cache in `Regex(...)`/`toRegex()`, and a lazy `findAll()` that silently does nothing without a terminal operation while allocating a `Matcher` and a `MatchResult` per match.

## Notes and limits

* `@Param` applies to the whole class, which is why `AllocationBenchmark` runs its four `findFirst` variants for every scenario. The `count` family lives in its own class to avoid repeating five times a dataset that does not depend on the scenario.
* `Stream` results are measured through Kotlin's mapped `java.util.List.stream()`. The 152 B/op against the 144 B/op of the Java project is within the usual variation between JDK builds and harnesses.
* No `parallel()` equivalent is measured: `Sequence` has none, and parallel collection processing in Kotlin means coroutines or an explicit `java.util.stream`.
* Kotlin/Native and Kotlin/JS have different collection implementations. Everything here is Kotlin/JVM.
