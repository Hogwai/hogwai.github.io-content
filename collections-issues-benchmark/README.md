# collections-issues-benchmark

This project provides JMH benchmarks to demonstrate the performance impact of different `java.util.regex.Pattern` usage patterns in Java.

## Prerequisites

* **JDK 21** or a later version.
* **Apache Maven** 3.6+ or later.
* An IDE like IntelliJ IDEA or Eclipse (optional, but recommended for easy execution).

## Project Structure

```shell
collections-issues-benchmark/
├── pom.xml
└── src/
    └── main/
        └── java/
            └── com/
                └── hogwai/
                    ├── BenchmarkRunner.java # Main class to run all benchmarks
                    └── benchmark/
                        ├── ContainsBenchmark.java # Benchmark for contains/containsAll operations
```

## How to Run the Benchmarks

The easiest way to run the benchmarks is by using the provided `BenchmarkRunner.java` class.

### Step 1: Clone and Build the Project

```bash
git clone https://github.com/Hogwai/hogwai.github.io-content.git

cd collections-issues-benchmark

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
java -jar collections-issues-benchmark/target/collections-issues-benchmark.jar
```

## Results

| Method                                   | Size    | Score (ns/op)  |
| ---------------------------------------- | ------- | -------------- |
| **listContainsPresent**                  | 10      | 10,071         |
|                                          | 1 000   | 443,124        |
|                                          | 100 000 | 49 151,385     |
| **listContainsAbsent**                   | 10      | 14,004         |
|                                          | 1 000   | 920,734        |
|                                          | 100 000 | 155 958,292    |
| **setContainsPresent**                   | 10      | 3,924          |
|                                          | 1 000   | 6,357          |
|                                          | 100 000 | 5,843          |
| **setContainsAbsent**                    | 10      | 3,568          |
|                                          | 1 000   | 4,150          |
|                                          | 100 000 | 4,129          |
| **listContainsAllPresent**               | 10      | 11,176         |
|                                          | 1 000   | 4 104,207      |
|                                          | 100 000 | 53 596 009,045 |
| **listContainsAllAbsent**                | 10      | 15,339         |
|                                          | 1 000   | 949,681        |
|                                          | 100 000 | 168 152,208    |
| **listConvertedToSetContainsAllPresent** | 10      | 197,473        |
|                                          | 1 000   | 22 531,443     |
|                                          | 100 000 | 2 526 157,383  |
| **listConvertedToSetContainsAllAbsent**  | 10      | 195,065        |
|                                          | 1 000   | 21 106,696     |
|                                          | 100 000 | 2 746 630,271  |
| **setContainsAllPresent**                | 10      | 12,628         |
|                                          | 1 000   | 1 049,815      |
|                                          | 100 000 | 101 134,422    |
| **setContainsAllAbsent**                 | 10      | 15,811         |
|                                          | 1 000   | 19,705         |
|                                          | 100 000 | 10,368         |


* **Benchmark**: The name of the method that was tested.
* **Mode**: `avgt` stands for "Average Time", meaning the score is the average time taken per operation.
* **Cnt**: The number of iterations used for the measurement.
* **Score**: The average time per operation. **Lower is better.**
* **Error**: The margin of error for the score.
* **Units**: The unit of measurement (here, nanoseconds per operation).

## Key Takeaways

The results demonstrate:

1. **`List.contains()` scales linearly**: the performance degrades sharply with size (`O(n)`).
2. **`HashSet.contains()` is effectively constant**: it stays in single-digit nanoseconds even at 100K elements.
3. **`List.containsAll()` is dangerous**: the performance collapses to tens of milliseconds at large scales (`O(n × m)`).
4. **Converting a List to a Set first (`new HashSet<>(list)`) is a major win**: it costs more initially, but drastically faster for large collections.
5. **`Set.containsAll()` is the most stable**: it remains efficient and predictable even for 100K elements.

**Rule of thumb:**

* For occasional lookups on small lists → `List.contains()` is fine.
* For repeated checks or large datasets → build or maintain a `HashSet`.
* For bulk comparisons (`containsAll`) → **always** use a `Set`.
