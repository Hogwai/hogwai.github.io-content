# stream-allocation-benchmark

This project provides JMH benchmarks to test the claims from Don Raab's article ["Allocation Hungry Any/All/None, FindFirst, and Count Methods on Java Stream"](https://donraab.medium.com/allocation-hungry-any-all-none-findfirst-and-count-methods-on-java-stream-e3ca6d66b265?sk=84629376cedf192618276bfc6668e97a): 
eager stream terminal operations (`anyMatch`, `allMatch`, `noneMatch`, `filter().findFirst()`, `filter().count()`) allocate objects such as the stream head, `MatchOps`/`FindOps`/`ReduceOps` instances and closures 
while equivalent plain loops or Eclipse Collections traversals stay allocation free.

## Implementations compared

Each benchmark exposes the same four implementations:

* `jdkStream`: the raw eager stream operation
* `jdkStreamGuarded`: the same call behind an `isEmpty()` guard to isolate the cost of an early return on an empty input.
* `helperLoop`: using `com.hogwai.util.Iterables`, a plain serial for-loop over the `Iterable`.
* `eclipseCollections`: using `org.eclipse.collections.impl.utility.Iterate`, the Eclipse Collections eager utility methods.

## Benchmarks

* `AnyMatchBenchmark`: `anyMatch` / `anySatisfy`.
* `AllMatchBenchmark`: `allMatch` / `allSatisfy`.
* `NoneMatchBenchmark`: `noneMatch` / `noneSatisfy`.
* `FindFirstBenchmark`: `filter().findFirst()`, `Iterables.findFirst`, `Iterables.detect`, `Iterate.detect`.
* `CountBenchmark`: `filter().count()` / `Iterate.count`.

The match benchmarks use a `scenario` parameter (`empty`, `first`, `mid`, `last`, `absent`).
The count benchmark uses `empty`, `none`, `half`, `all`.

## Prerequisites

* JDK >= 25
* Apache Maven >= 3.6

## Project Structure

```shell
stream-allocation-benchmark/
├── pom.xml
└── src/
    └── main/
        └── java/
            └── com/
                └── hogwai/
                    ├── BenchmarkRunner.java # Main class to run all benchmarks
                    ├── benchmark/
                    │   ├── AllMatchBenchmark.java
                    │   ├── AnyMatchBenchmark.java
                    │   ├── CountBenchmark.java
                    │   ├── FindFirstBenchmark.java
                    │   └── NoneMatchBenchmark.java
                    └── util/
                        └── Iterables.java # Plain for-loop baseline
```

## How to run the benchmarks

### Build the project

```bash
mvn clean package
```

### Execute the benchmarks

There are two ways to launch the suite, and they differ in where the JMH options come from.

#### From the IDE

1. Open the project in your IDE (IntelliJ, Eclipse, etc.).
2. Navigate to the `src/main/java/com/hogwai/BenchmarkRunner.java` file.
3. Run the `main` method.

This class builds its own JMH options: it selects the five benchmark classes, enables the JMH GC profiler and appends `-XX:+UseCompactObjectHeaders` to every forked JVM. Nothing else to pass.

#### MFrom the command line

The shaded jar launches `org.openjdk.jmh.Main`, which does not know the options baked into `BenchmarkRunner`. 
Pass them explicitly:

```bash
java -jar target/stream-allocation-benchmark.jar -prof gc -f 3 -jvmArgsAppend "-XX:+UseCompactObjectHeaders"
```

To reproduce the run that produced the Results section below:

```bash
java -jar target/stream-allocation-benchmark.jar "AnyMatchBenchmark" -prof gc -f 3 -jvmArgsAppend "-XX:+UseCompactObjectHeaders" -jvmArgsAppend "-Xmx25M"
```

Repeat per class (`AnyMatchBenchmark`, `AllMatchBenchmark`, `NoneMatchBenchmark`, `FindFirstBenchmark`, `CountBenchmark`) or omit the class name to run everything. 
The full suite at 3 forks takes roughly 40 to 50 minutes.

## Results

Measured with:
- Eclipse Temurin JDK 25.0.3 on Linux (WSL2)
- `-XX:+UseCompactObjectHeaders`, `-Xmx25M`, 
- 3 forks, 15 samples per benchmark and the JMH GC profiler.
- Allocation is `gc.alloc.rate.norm` in B/op, the average bytes allocated by one call. 
- `~0` marks scores at the profiler detection floor.

### AnyMatch

#### Time (ns/op, lower is better)

| Method                    |     empty |     first |          mid |         last |       absent |
|---------------------------|----------:|----------:|-------------:|-------------:|-------------:|
| `Stream.anyMatch`         |     12.08 |     13.57 |     65022.22 |    124726.69 |    115280.56 |
| `guarded Stream.anyMatch` | **0.361** |     13.81 |     66031.29 |    128021.76 |    130399.62 |
| `Iterables.anyMatch`      |     0.423 |     0.743 | **15479.47** | **31096.35** | **30234.64** |
| `Iterate.anySatisfy`      |     0.383 | **0.740** |     16612.28 |     40736.52 |     46140.81 |

#### Allocation (B/op, lower is better)

| Method                    |  empty |  first |       mid |      last |    absent |
|---------------------------|-------:|-------:|----------:|----------:|----------:|
| `Stream.anyMatch`         | 144.00 | 144.00 |    143.59 |    131.39 |     98.33 |
| `guarded Stream.anyMatch` | **~0** | 144.00 |    143.58 |    132.31 |     82.81 |
| `Iterables.anyMatch`      | **~0** | **~0** | **0.098** | **0.197** | **0.192** |
| `Iterate.anySatisfy`      | **~0** | **~0** |     0.105 |     0.258 |     0.311 |

### AllMatch

#### Time (ns/op, lower is better)

| Method                    |     empty |     first |          mid |         last |       absent |
|---------------------------|----------:|----------:|-------------:|-------------:|-------------:|
| `Stream.allMatch`         |     13.17 |     13.99 |     67382.91 |    124991.24 |    116349.65 |
| `guarded Stream.allMatch` | **0.357** |     14.13 |     68351.66 |    125698.54 |    130865.15 |
| `Iterables.allMatch`      |     0.393 | **0.694** | **14811.47** | **30099.53** | **31289.08** |
| `Iterate.allSatisfy`      |     0.382 |     0.742 |     16042.11 |     44003.74 |     44389.83 |

#### Allocation (B/op, lower is better)

| Method                    |  empty |  first |       mid |      last |    absent |
|---------------------------|-------:|-------:|----------:|----------:|----------:|
| `Stream.allMatch`         | 144.00 | 144.00 |    136.30 |    130.91 |     98.42 |
| `guarded Stream.allMatch` | **~0** | 144.00 |    136.47 |    131.42 |     82.62 |
| `Iterables.allMatch`      | **~0** | **~0** | **0.094** | **0.191** | **0.198** |
| `Iterate.allSatisfy`      | **~0** | **~0** |     0.101 |     0.279 |     0.281 |

### NoneMatch

#### Time (ns/op, lower is better)

| Method                     |     empty |     first |          mid |         last |       absent |
|----------------------------|----------:|----------:|-------------:|-------------:|-------------:|
| `Stream.noneMatch`         |     12.20 |     13.65 |     67547.33 |    124416.94 |    115951.16 |
| `guarded Stream.noneMatch` | **0.360** |     13.96 |     68346.99 |    128971.91 |    129895.88 |
| `Iterables.noneMatch`      |     0.396 | **0.688** | **14718.21** | **30191.97** | **31304.02** |
| `Iterate.noneSatisfy`      |     0.379 |     0.735 |     16413.21 |     42259.22 |     46026.17 |

#### Allocation (B/op, lower is better)

| Method                     |  empty |  first |       mid |      last |    absent |
|----------------------------|-------:|-------:|----------:|----------:|----------:|
| `Stream.noneMatch`         | 144.00 | 144.00 |    136.48 |    131.72 |     98.22 |
| `guarded Stream.noneMatch` | **~0** | 144.00 |    136.63 |    132.37 |     83.50 |
| `Iterables.noneMatch`      | **~0** | **~0** | **0.093** | **0.191** | **0.198** |
| `Iterate.noneSatisfy`      | **~0** | **~0** |     0.104 |     0.268 |     0.292 |

### FindFirst

#### Time (ns/op, lower is better)

| Method                                |     empty |     first |          mid |         last |       absent |
|---------------------------------------|----------:|----------:|-------------:|-------------:|-------------:|
| `Stream.filter().findFirst()`         |     14.97 |     17.61 |     60210.32 |    125820.39 |    118251.70 |
| `guarded Stream.filter().findFirst()` | **0.359** |     17.60 |     60745.89 |    125174.22 |    117942.84 |
| `Iterables.findFirst`                 |     0.408 |      1.74 |     18398.38 |     45708.38 | **30994.93** |
| `Iterables.detect`                    |     0.414 | **0.706** | **15398.21** | **30989.74** |     31010.92 |
| `Iterate.detect`                      |     0.381 |     0.739 |     16741.82 |     40767.43 |     45399.61 |

#### Allocation (B/op, lower is better)

| Method                                |  empty |  first |       mid |      last |    absent |
|---------------------------------------|-------:|-------:|----------:|----------:|----------:|
| `Stream.filter().findFirst()`         | 168.00 | 184.00 |    184.38 |    184.80 |    168.75 |
| `guarded Stream.filter().findFirst()` | **~0** | 184.00 |    184.39 |    184.79 |    168.75 |
| `Iterables.findFirst`                 | **~0** |  16.00 |     16.12 |     16.29 | **0.196** |
| `Iterables.detect`                    | **~0** | **~0** | **0.097** | **0.196** | **0.196** |
| `Iterate.detect`                      | **~0** | **~0** |     0.106 |     0.258 |     0.288 |

### Count

#### Time (ns/op, lower is better)

| Method                            |     empty |         none |         half |          all |
|-----------------------------------|----------:|-------------:|-------------:|-------------:|
| `Stream.filter().count()`         |     14.34 |     58871.75 |     72139.11 |     75908.07 |
| `guarded Stream.filter().count()` | **0.354** |     61915.40 |     76259.46 |     79184.47 |
| `Iterables.count`                 |     0.411 | **31062.44** | **40120.17** | **29900.15** |
| `Iterate.count`                   |     0.378 |     46491.24 |     61630.73 |     45381.66 |

#### Allocation (B/op, lower is better)

| Method                            |  empty |      none |      half |       all |
|-----------------------------------|-------:|----------:|----------:|----------:|
| `Stream.filter().count()`         | 176.00 |    192.18 |    207.99 |    208.00 |
| `guarded Stream.filter().count()` | **~0** |    192.19 |    206.97 |    208.00 |
| `Iterables.count`                 | **~0** | **0.197** | **0.254** | **0.189** |
| `Iterate.count`                   | **~0** |     0.351 |     0.466 |     0.287 |

### Conclusion

#### `Iterables` and Eclipse Collections allocate essentially nothing

They are the fastest implementations on long scans. 
The only minor allocation is `Iterables.findFirst` which wraps a hit in a 16-byte `Optional`.

#### The JDK Stream operations allocate on every call
It's 144 B/op for `anyMatch`/`allMatch`/`noneMatch`, 168 to 184 B/op for `filter().findFirst()` and 176 to 208 B/op for `filter().count()`. 
They allocate even on an empty collection and on a full scan, they run about 3 to 4 times slower than the loop or Eclipse Collections. 

#### The `isEmpty()` guard only removes the allocation in the empty case

On non-empty input the guarded stream allocates the same as the raw stream.
