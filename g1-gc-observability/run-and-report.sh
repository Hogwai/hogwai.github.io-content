#!/bin/sh
# run-and-report.sh — exécute le workload G1 GC dans un conteneur Docker
# (eclipse-temurin:25-jdk), échantillonne CPU/RSS/cgroup pendant le run et
# affiche un rapport synthétique.
# Usage: ./run-and-report.sh [durée-secondes]   (défaut: 10)

set -u

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
cd "$SCRIPT_DIR" || exit 1

TARGET_DIR="$SCRIPT_DIR/target"
JAR="$TARGET_DIR/g1-gc-observability-1.0.0.jar"
MARKER="$TARGET_DIR/.run-and-report.start"

fail() {
    echo "error: $1" >&2
    exit 1
}

# --- durée (entier strictement positif, défaut 10 s) ---
DURATION="${1:-10}"
case "$DURATION" in
    ''|*[!0-9]*)
        fail "la durée doit être un entier positif de secondes (reçu: '$DURATION'). Usage: $0 [durée-secondes]"
        ;;
esac
[ "$DURATION" -gt 0 ] 2>/dev/null || fail "la durée doit être strictement positive (reçu: '$DURATION')"

# --- prérequis ---
command -v docker >/dev/null 2>&1 || fail "la commande 'docker' est introuvable sur le PATH."
[ -f "$JAR" ] || fail "JAR introuvable: $JAR. Construisez-le d'abord avec: ./mvnw clean package"

mkdir -p "$TARGET_DIR"

NAME="g1report-$$-$(date +%s)"
: > "$MARKER"

cleanup() {
    docker rm -f "$NAME" >/dev/null 2>&1 || true
    rm -f "$MARKER"
}
trap 'cleanup; exit 130' INT
trap 'cleanup; exit 143' TERM
trap cleanup EXIT

echo "Démarrage du workload G1 (${DURATION}s) dans le conteneur '$NAME'..."
docker run -d --name "$NAME" --memory=512m --cpus=1 \
    -v "$TARGET_DIR:/work/target" \
    -v "$JAR:/work/app.jar:ro" \
    -w /work \
    eclipse-temurin:25-jdk \
    java -Xms256m -Xmx256m -XX:+UseG1GC \
    -Xlog:gc*:file=target/gc.log:time,level,tags:filecount=5,filesize=10m \
    -Xlog:os+container=trace \
    -XX:StartFlightRecording=disk=true,dumponexit=true,filename=target/g1.jfr,settings=profile \
    -jar app.jar "$DURATION" \
    || fail "échec du lancement du conteneur (docker run)."

# --- échantillonnage (au moins 1 mesure / seconde tant que le conteneur tourne) ---
samples=0
rss_max=0
cg_max=0
cpu_max=0

while [ "$samples" -lt "$((DURATION + 60))" ]; do
    running=$(docker inspect -f '{{.State.Running}}' "$NAME" 2>/dev/null || echo false)
    [ "$running" = "true" ] || break

    cpu=$(docker stats --no-stream --format '{{.CPUPerc}}' "$NAME" 2>/dev/null || true)
    rss=$(docker exec "$NAME" awk '/^VmRSS:/ {print $2}' /proc/1/status 2>/dev/null || true)
    cg=$(docker exec "$NAME" cat /sys/fs/cgroup/memory.current 2>/dev/null || true)

    cpu_num=$(printf '%s' "$cpu" | tr -dc '0-9.')
    if [ -n "$cpu_num" ]; then
        cpu_max=$(printf '%s\n%s\n' "$cpu_max" "$cpu_num" \
            | awk '{ v = $1 + 0; if (v > m) m = v } END { printf "%.3f", m + 0 }')
    fi

    rss_kb=$(printf '%s' "$rss" | tr -dc '0-9')
    [ -n "$rss_kb" ] && [ "$rss_kb" -gt "$rss_max" ] 2>/dev/null && rss_max=$rss_kb

    cg_bytes=$(printf '%s' "$cg" | tr -dc '0-9')
    [ -n "$cg_bytes" ] && [ "$cg_bytes" -gt "$cg_max" ] 2>/dev/null && cg_max=$cg_bytes

    samples=$((samples + 1))
done

EXIT_CODE=$(docker inspect -f '{{.State.ExitCode}}' "$NAME" 2>/dev/null || echo 1)
LOG_OUT=$(docker logs "$NAME" 2>&1 || true)

# --- itérations / objets retenus depuis les logs du conteneur ---
ITER="n/d"
RETAINED="n/d"
ITER_LINE=$(printf '%s\n' "$LOG_OUT" | grep 'Iterations:' | tail -1 || true)
if [ -n "$ITER_LINE" ]; then
    ITER=$(printf '%s' "$ITER_LINE" | grep -o '[0-9][0-9]*' | head -1)
    RETAINED=$(printf '%s' "$ITER_LINE" | grep -o '[0-9][0-9]*' | tail -1)
fi

# --- pauses GC depuis target/gc.log* (run courant uniquement) ---
GC_FILES=""
for f in "$TARGET_DIR"/gc.log*; do
    [ -f "$f" ] && [ "$f" -nt "$MARKER" ] && GC_FILES="$GC_FILES $f"
done

if [ -n "$GC_FILES" ]; then
    PAUSE_STATS=$(awk '
        /GC\([0-9]+\) Pause/ {
            t = $NF
            if (t ~ /us$/)          { sub(/us$/, "", t); v = t / 1000 }
            else if (t ~ /ms$/)     { sub(/ms$/, "", t); v = t + 0 }
            else if (t ~ /s$/)      { sub(/s$/, "", t); v = t * 1000 }
            else                    { next }
            count++
            sum += v
            if (v > max) max = v
        }
        END {
            avg = (count > 0) ? sum / count : 0
            printf "%d %.3f %.3f %.3f\n", count, sum, avg, max
        }
    ' $GC_FILES)
    GC_COUNT=$(printf '%s' "$PAUSE_STATS" | awk '{ print $1 }')
    GC_SUM=$(printf '%s' "$PAUSE_STATS" | awk '{ print $2 }')
    GC_AVG=$(printf '%s' "$PAUSE_STATS" | awk '{ print $3 }')
    GC_MAX=$(printf '%s' "$PAUSE_STATS" | awk '{ print $4 }')
else
    GC_COUNT="n/d"; GC_SUM="n/d"; GC_AVG="n/d"; GC_MAX="n/d"
fi

# --- événements jdk.GarbageCollection depuis le JFR (si l'outil jfr est disponible) ---
JFR_FILE="$TARGET_DIR/g1.jfr"
JFR_EVENTS=""
if command -v jfr >/dev/null 2>&1; then
    # Robustesse : le JFR est finalisé à la fermeture du process et le fichier
    # écrit par le conteneur peut mettre un instant à être visible côté hôte
    # (montages bind/WSL). On réessaie quelques fois en cas d'échec.
    retries=0
    while [ -z "$JFR_EVENTS" ] && [ "$retries" -lt 8 ]; do
        [ -f "$JFR_FILE" ] || { sleep 1; retries=$((retries + 1)); continue; }
        JFR_EVENTS=$(jfr summary "$JFR_FILE" 2>/dev/null \
            | awk '/jdk\.GarbageCollection/ { gsub(/[^0-9]/, "", $2); if ($2 != "") { print $2; exit } }')
        [ -n "$JFR_EVENTS" ] || { sleep 1; retries=$((retries + 1)); }
    done
fi
[ -n "$JFR_EVENTS" ] || JFR_EVENTS="n/d"

# --- allocation estimée : 34 MiB par itération ---
if [ "$ITER" != "n/d" ]; then
    ALLOC=$(awk -v i="$ITER" -v d="$DURATION" 'BEGIN { printf "%.1f", 34 * i / d }')
else
    ALLOC="n/d"
fi

# --- conversions d'unités ---
rss_mib=$(awk -v kb="$rss_max" 'BEGIN { printf "%.1f", kb / 1024 }')
cg_mib=$(awk -v b="$cg_max" 'BEGIN { printf "%.1f", b / 1024 / 1024 }')
cpu_pct=$(printf '%.1f' "$cpu_max")

# --- rapport ---
echo
echo "=== Rapport G1 GC — run de ${DURATION} s (échantillonnage conteneur) ==="
printf '%-28s : %s\n' "Durée demandée" "${DURATION} s"
printf '%-28s : %s\n' "Itérations" "$ITER"
printf '%-28s : %s\n' "Objets retenus" "$RETAINED"
printf '%-28s : %s MiB/s\n' "Allocation estimée" "$ALLOC"
printf '%-28s : %s\n' "Pauses GC" "$GC_COUNT"
printf '%-28s : %s ms\n' "  durée totale" "$GC_SUM"
printf '%-28s : %s ms\n' "  durée moyenne" "$GC_AVG"
printf '%-28s : %s ms\n' "  durée max" "$GC_MAX"
printf '%-28s : %s\n' "Événements jdk.GarbageCollection" "$JFR_EVENTS"
printf '%-28s : %s MiB\n' "RSS JVM max" "$rss_mib"
printf '%-28s : %s MiB\n' "Mémoire cgroup max" "$cg_mib"
printf '%-28s : %s %%\n' "CPU max" "$cpu_pct"
printf '%-28s : %s\n' "Échantillons" "$samples"
echo "Artefacts:"
echo "  JAR : $JAR"
echo "  Logs: $TARGET_DIR/gc.log (+ rotations gc.log.*)"
echo "  JFR : $JFR_FILE"

if [ "$samples" -eq 0 ]; then
    echo "note: aucun échantillon collecté — le conteneur s'est terminé avant la première mesure." >&2
fi

# --- code de sortie ---
status=0
if [ "$EXIT_CODE" != "0" ]; then
    echo "error: le workload s'est terminé avec le code de sortie $EXIT_CODE (dernières lignes du conteneur):" >&2
    printf '%s\n' "$LOG_OUT" | tail -5 >&2
    status=1
elif [ -z "$GC_FILES" ]; then
    echo "error: aucun log GC (target/gc.log*) produit pour ce run." >&2
    status=1
elif [ "$ITER" = "n/d" ]; then
    echo "error: la ligne 'Iterations:' est absente des logs du conteneur." >&2
    status=1
fi
exit "$status"
