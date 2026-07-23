# JIT Internals Playground

Educational demo project showcasing HotSpot JVM JIT optimizations:
JIT Inlining, Devirtualization (CHA), Escape Analysis, and Tiered Compilation.


## Quick Start

```bash
./run-demo.sh com.hogwai.jit.inlining.SmallVsLargeMethod
```

Each demo:
1. Explains the JIT optimization in stdout
2. Runs a warmup + measurement loop
3. Saves a JIT log to `logs/hotspot_<DemoName>.log` for JITWatch

## Concepts

| Concept | Demo | What it shows |
|---|---|---|
| **Inlining — size limits** | `SmallVsLargeMethod` | MaxInlineSize (35 bytes) vs FreqInlineSize (325 bytes) |
| **Inlining — depth limits** | `InliningDepthChains` | MaxInlineLevel (C2=15, C1=9) with 20-level call chain |
| **Inlining — megamorphic** | `MegamorphicDispatch` | Monomorphic → bimorphic → megamorphic dispatch |
| **CHA — monomorphic** | `MonomorphicInlining` | Virtual call inlined when only 1 impl loaded |
| **CHA — bimorphic** | `BimorphicGuard` | Guarded inline for 2 implementations |
| **CHA — deoptimization** | `CHADeoptimization` | New class load → CHA invalidated → deopt |
| **Escape Analysis** | `ScalarReplacement` | Object fields promoted to registers |
| **Escape Analysis** | `LockElision` | Lock eliminated on non-escaping objects |
| **Tiered Compilation** | `TierTransition` | L0 (interpreted) → L3 (C1) → L4 (C2) |
| **On-Stack Replacement** | `OSRDemo` | Loop compiled mid-execution |

## Running Individual Demos

```bash
# Inlining
./run-demo.sh com.hogwai.jit.inlining.SmallVsLargeMethod
./run-demo.sh com.hogwai.jit.inlining.InliningDepthChains
./run-demo.sh com.hogwai.jit.inlining.MegamorphicDispatch

# Devirtualization / CHA
./run-demo.sh com.hogwai.jit.devirtualization.MonomorphicInlining
./run-demo.sh com.hogwai.jit.devirtualization.BimorphicGuard
./run-demo.sh com.hogwai.jit.devirtualization.CHADeoptimization

# Escape Analysis
./run-demo.sh com.hogwai.jit.escapeanalysis.ScalarReplacement
./run-demo.sh com.hogwai.jit.escapeanalysis.LockElision

# Tiered Compilation
./run-demo.sh com.hogwai.jit.tiered.TierTransition
./run-demo.sh com.hogwai.jit.tiered.OSRDemo
```

## JITWatch Setup

### Desktop (JITWatch)

Download the shaded JAR from [AdoptOpenJDK/jitwatch](https://github.com/AdoptOpenJDK/jitwatch/releases):

```bash
java -jar jitwatch-ui-shaded.jar
```

Then:
1. **Config → Sandbox** — add your JDK path and source path
2. **Open Log** → select `logs/hotspot_<DemoName>.log`
3. **Start** — explore the inline tree, chain view, and EA tab

### IntelliJ IDEA (JITWatch4i)

1. Install the [JITWatch4i plugin](https://plugins.jetbrains.com/plugin/26025-jitwatch4i)
2. Right-click on `logs/hotspot_*.log` → **Open with JITWatch**

## Going Further

```bash
# See all inlining-related flags and their defaults
java -XX:+PrintFlagsFinal -version 2>&1 | grep Inline

# Print assembly (requires hsdis disassembler library)
java -XX:+UnlockDiagnosticVMOptions -XX:+PrintAssembly ...

# Override inline level for experimentation
java -XX:MaxInlineLevel=5 com.hogwai.jit.inlining.InliningDepthChains
```

## Build

- **Java 21** required
- **Maven wrapper** included (no system Maven needed)
- **Zero external dependencies** — pure JDK standard library

```bash
./mvnw compile    # compile only (used by run-demo.sh)
./mvnw package    # create fat jar (optional)
```

## Project Structure

```
src/main/java/com/hogwai/jit/
├── JITReportCard.java            ← before/after comparison helper
├── impl/                         ← shared domain code
│   ├── payment/                  ← PaymentGateway hierarchy
│   └── geometry/                 ← Point, Shape, Circle, Square
├── inlining/                     ← 3 demos
├── devirtualization/             ← 3 demos
├── escapeanalysis/               ← 2 demos
└── tiered/                       ← 2 demos
```
