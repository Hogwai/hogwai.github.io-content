#!/bin/bash
# Run all 10 demos sequentially
set -euo pipefail

QUIET_FLAG=""

DEMOS=(
    com.hogwai.jit.inlining.SmallVsLargeMethod
    com.hogwai.jit.inlining.InliningDepthChains
    com.hogwai.jit.inlining.MegamorphicDispatch
    com.hogwai.jit.devirtualization.MonomorphicInlining
    com.hogwai.jit.devirtualization.BimorphicGuard
    com.hogwai.jit.devirtualization.CHADeoptimization
    com.hogwai.jit.escapeanalysis.ScalarReplacement
    com.hogwai.jit.escapeanalysis.LockElision
    com.hogwai.jit.tiered.TierTransition
    com.hogwai.jit.tiered.OSRDemo
)

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR"

# Parse flags
for arg in "$@"; do
    case "$arg" in
        --quiet) QUIET_FLAG="--quiet" ;;
        *)       echo "Unknown flag: $arg"; echo "Usage: ./run-all.sh [--quiet]"; exit 1 ;;
    esac
done

PASSED=0
FAILED=0

if [ -n "$QUIET_FLAG" ]; then
    echo "Running all ${#DEMOS[@]} demos (quiet mode)..."
else
    echo "Running all ${#DEMOS[@]} demos (verbose)..."
fi
echo "================================================"

for demo in "${DEMOS[@]}"; do
    echo ""
    echo ">>> $demo"
    if ./run-demo.sh $QUIET_FLAG "$demo"; then
        ((PASSED++))
        echo "<<< PASSED"
    else
        ((FAILED++))
        echo "<<< FAILED"
    fi
done

echo ""
echo "================================================"
echo "Results: $PASSED passed, $FAILED failed out of ${#DEMOS[@]}"
