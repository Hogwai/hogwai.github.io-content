#!/bin/sh
# analyze-g1.sh — analyse les artefacts G1 GC (JFR + logs GC) d'un répertoire.
# Usage: ./analyze-g1.sh [repertoire]   (defaut: target)

set -u

DIR="${1:-target}"

fail() {
    echo "error: $1" >&2
    exit 1
}

command -v jfr >/dev/null 2>&1 \
    || fail "la commande 'jfr' est introuvable sur le PATH. Utilisez un JDK 11+ et exposez son bin/ (ex: JAVA_HOME=... PATH=\$JAVA_HOME/bin:\$PATH)."

[ -d "$DIR" ] || fail "le répertoire '$DIR' n'existe pas. Utilisez: ./analyze-g1.sh [repertoire]"

JFR_FILE="$DIR/g1.jfr"
[ -f "$JFR_FILE" ] || fail "fichier JFR introuvable: $JFR_FILE. Exécutez d'abord le workload avec -XX:StartFlightRecording."

GC_LOGS=$(ls "$DIR"/gc.log* 2>/dev/null || true)
[ -n "$GC_LOGS" ] || fail "aucun log GC ('$DIR/gc.log*') trouvé. Exécutez d'abord le workload avec -Xlog:gc*."

echo "=== jfr summary: $JFR_FILE ==="
jfr summary "$JFR_FILE" || fail "échec de 'jfr summary' sur $JFR_FILE"

echo
echo "=== jdk.GarbageCollection events ==="
jfr print --events jdk.GarbageCollection "$JFR_FILE" || fail "échec de 'jfr print --events jdk.GarbageCollection' sur $JFR_FILE"

echo
echo "=== GC pause lines ==="
total=0
for f in $GC_LOGS; do
    count=$(grep -cE 'GC\([0-9]+\) Pause' "$f" 2>/dev/null || true)
    echo "$f: $count"
    total=$((total + count))
done
echo "total: $total"
