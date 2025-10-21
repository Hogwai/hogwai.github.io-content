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
                    ├── BenchmarkRunner.java # Main class to run all benchmarks
                    └── benchmark/
                        ├── PatternCompileBenchmark.java # Benchmark for Pattern compilation
                        └── StringMethodsBenchmark.java # Benchmark for String regex methods
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

This is the simplest method.

1. Open the project in your IDE (IntelliJ, Eclipse, etc.).
2. Navigate to the `src/main/java/com/hogwai/BenchmarkRunner.java` file.
3. Right-click anywhere inside the `main` method and select **"Run 'BenchmarkRunner.main()'"**..

#### Method 2: From the Command Line

```bash
mvn package
java -jar regex-performance-benchmark/target/regex-performance-benchmark.jar
```

## Results

Repord generated:

| Benchmark                                              | Mode | Cnt | Score      | Error       | Units |
|--------------------------------------------------------|------|-----|------------|-------------|-------|
| PatternCompileBenchmark.benchmarkCompileInLoop         | avgt | 10  | 1057403.584 | ± 81503.452 | ns/op |
| PatternCompileBenchmark.benchmarkPrecompiledPattern    | avgt | 10  | 427756.571  | ± 141872.720 | ns/op |
| StringMethodsBenchmark.benchmarkPatternMatcher         | avgt | 10  | 455690.725  | ± 70202.242 | ns/op |
| StringMethodsBenchmark.benchmarkStringMatches          | avgt | 10  | 501527.211  | ± 67564.971 | ns/op |

* **Benchmark**: The name of the method that was tested.
* **Mode**: `avgt` stands for "Average Time", meaning the score is the average time taken per operation.
* **Cnt**: The number of iterations used for the measurement.
* **Score**: The average time per operation. **Lower is better.**
* **Error**: The margin of error for the score.
* **Units**: The unit of measurement (here, nanoseconds per operation).

## Key Takeaways

The results consistently demonstrate:

1. **Re-compiling is expensive**: `benchmarkCompileInLoop` is significantly slower than the pre-compiled version.
2. **`String` methods are a trap**: `String.matches()` is slower than using a pre-compiled `Pattern` because it re-compiles the regex on every call.
3. **Pre-compilation is key**: Using a `private static final Pattern` is the most performant approach for repeated regex operations.
