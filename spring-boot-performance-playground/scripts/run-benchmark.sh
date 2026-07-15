#!/bin/bash
# Full benchmark run: starts both apps, runs k6 scenarios, produces comparison report

set -e

echo "=== Building apps ==="
./mvnw package -pl baseline -am -DskipTests
./mvnw package -pl optimized -am -DskipTests

echo "=== Starting apps ==="
java -jar baseline/target/*.jar &
BASELINE_PID=$!
java -jar optimized/target/*.jar &
OPTIMIZED_PID=$!

echo "=== Waiting for readiness ==="
for i in $(seq 1 30); do
    if curl -sf http://localhost:8080/actuator/health > /dev/null 2>&1 && \
       curl -sf http://localhost:8081/actuator/health > /dev/null 2>&1; then
        break
    fi
    sleep 1
done

echo "=== Running k6 scenarios ==="
k6 run k6/scenarios/ramp-comparison.js --out json=/tmp/k6-results.json

echo "=== Generating report ==="
# Process k6 results into comparison table
# (k6 JSON output parsed to produce Markdown)

echo "=== Stopping apps ==="
kill $BASELINE_PID $OPTIMIZED_PID 2>/dev/null || true
wait 2>/dev/null || true

echo "=== Done ==="
