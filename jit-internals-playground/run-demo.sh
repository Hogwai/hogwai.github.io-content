#!/bin/bash
set -euo pipefail

QUIET=false

if [ $# -lt 1 ]; then
    echo "Usage: ./run-demo.sh [--quiet] <fully.qualified.ClassName>"
    echo "  --quiet   Skip -XX:+PrintCompilation and -XX:+PrintInlining output"
    echo ""
    echo "Example: ./run-demo.sh com.hogwai.jit.inlining.SmallVsLargeMethod"
    exit 1
fi

# Parse flags
for arg in "$@"; do
    case "$arg" in
        --quiet) QUIET=true ;;
        *)       DEMO_CLASS="$arg" ;;
    esac
done

CLASS_SHORT="${DEMO_CLASS##*.}"

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR"

echo "========================================"
echo " JIT Internals Playground"
echo " Demo: $CLASS_SHORT"
echo "========================================"

# Compile
echo "[1/3] Compiling..."
./mvnw -q compile
if [ $? -ne 0 ]; then
    echo "ERROR: Compilation failed"
    exit 1
fi

# Prepare
mkdir -p logs
LOGFILE="logs/hotspot_${CLASS_SHORT}.log"

# Base JIT observation flags
BASE_FLAGS=(
    -XX:+UnlockDiagnosticVMOptions
    -XX:+LogCompilation
    -XX:LogFile="$LOGFILE"
)

# Verbose flags (skip in quiet mode)
if [ "$QUIET" != "true" ]; then
    BASE_FLAGS+=(-XX:+PrintCompilation)
    BASE_FLAGS+=(-XX:+PrintInlining)
fi

# Category-specific flags
EXTRA_FLAGS=()
if [[ "$DEMO_CLASS" == *".escapeanalysis."* ]]; then
    EXTRA_FLAGS+=(-XX:+PrintEliminateAllocations)
fi
if [[ "$DEMO_CLASS" == *".devirtualization."* ]]; then
    EXTRA_FLAGS+=(-XX:+TraceDeoptimization)
fi
if [[ "$DEMO_CLASS" == *".tiered."* ]]; then
    EXTRA_FLAGS+=(-XX:+PrintGC)
fi

if [ "$QUIET" = "true" ]; then
    echo "[2/3] Running (quiet mode — no verbose JIT output)"
    echo "       Log file: $LOGFILE"
else
    echo "[2/3] Running with JIT observation flags..."
    echo "       Log file: $LOGFILE"
fi
echo ""

# Run the demo
java -cp target/classes \
    "${BASE_FLAGS[@]}" \
    "${EXTRA_FLAGS[@]}" \
    "$DEMO_CLASS"

echo ""
echo "[3/3] Done!"
echo "       JIT log saved to: $LOGFILE"
echo ""
echo "To visualize:"
echo "  JITWatch (desktop): java -jar jitwatch-ui-shaded.jar"
echo "  JITWatch4i (IntelliJ): File → Open JIT log..."
