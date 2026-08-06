# G1 GC Observability

Demonstration workload that allocates ephemeral and retained byte arrays to exercise the G1 garbage collector under JDK 25. Generates unified GC logs and JDK Flight Recorder (JFR) recordings for analysis.

## Prerequisites

- **JDK 25** (Temurin recommended)
- **Maven Wrapper inclus** (`./mvnw`, Maven 3.9.9) — aucun Maven système requis
- **Docker** (optionnel, pour les exécutions sous contraintes cgroup)

## Build

```bash
./mvnw clean package
```

Produces `target/g1-gc-observability-1.0.0.jar` (executable JAR, main class `com.hogwai.g1.G1Workload`).

## Tests

```bash
./mvnw test
```

`G1WorkloadTest` couvre le smoke test à durée nulle (logging du point d'entrée)
et un run d'une seconde vérifiant les invariants de rétention : itérations positives,
rétention plafonnée à 1280 objets et multiple de 16.

## Run locally with G1

```bash
java \
  -Xms256m -Xmx256m \
  -XX:+UseG1GC \
  -Xlog:gc*:file=target/gc.log:time,level,tags:filecount=5,filesize=10m \
  -XX:StartFlightRecording=disk=true,dumponexit=true,filename=target/g1.jfr,settings=profile \
  -jar target/g1-gc-observability-1.0.0.jar \
  90
```

- `-Xms256m -Xmx256m` — fixed heap at 256 MB.
- `-XX:+UseG1GC` — explicitly selects the G1 collector (default since JDK 9, explicit for clarity).
- `-Xlog:gc*` — unified GC logging to `target/gc.log` (rotating, 5 files of 10 MB each).
- `-XX:StartFlightRecording` — JFR recording dumped to `target/g1.jfr` on exit.
- The numeric argument (`90`) is the duration in seconds (default: 90 if omitted).

## Run inside a Docker container

Docker is **optional** for local runs — the workload runs fine with a plain JDK 25 —
but it is the only way to reproduce cgroup-constrained measurements (`--memory`, `--cpus`).

### Via Docker Compose (optionnel)

```bash
./mvnw clean package          # required: the Compose service mounts the built JAR
docker compose run --rm g1-workload
```

`docker-compose.yml` reproduces the exact flags below (G1, GC logs, container-awareness
logging, JFR) for a 90-second run, with `mem_limit: 512m` and `cpus: 1`. The JAR is mounted
read-only as `/work/app.jar` and `target/` is mounted at `/work/target` so logs and JFR
survive the container lifecycle. The JAR must be built before running.

### Manual `docker run`

```bash
docker run --rm --name g1-gc-observability \
  --memory=512m --cpus=1 \
  -v "$PWD/target:/work/target" \
  -v "$PWD/target/g1-gc-observability-1.0.0.jar:/work/app.jar:ro" \
  -w /work \
  eclipse-temurin:25-jdk \
  java -Xms256m -Xmx256m -XX:+UseG1GC \
    -Xlog:gc*:file=target/gc.log:time,level,tags:filecount=5,filesize=10m \
    -Xlog:os+container=trace \
    -XX:StartFlightRecording=disk=true,dumponexit=true,filename=target/g1.jfr,settings=profile \
    -jar app.jar 90
```

- `--memory=512m --cpus=1` — container resource limits.
- `--name g1-gc-observability` — names the container so it can be inspected during execution (required by the `docker exec` commands below).
- `-v "$PWD/target:/work/target"` — mounts the local `target/` directory so GC logs and JFR files survive the container lifecycle.
- `-v "$PWD/target/g1-gc-observability-1.0.0.jar:/work/app.jar:ro"` — mounts the JAR read-only inside the container.
- `-w /work` — sets the working directory so logs and JFR are written into the mounted `target/` directory.
- `-Xlog:os+container=trace` — prints container-awareness details (cgroup limits, available CPUs/memory).

### Observing memory during a run

Open a second terminal while the container is still running (before it exits) and run any of the following.

**JVM process resident memory**
```bash
docker exec g1-gc-observability sh -c "grep '^VmRSS:' /proc/1/status"
```
`/proc/1/status` is the JVM process because the JVM runs as PID 1 inside the container.
`VmRSS` is the **resident set size** of the process — physical pages mapped into the JVM's address space.
It is **not** the JVM committed heap (`-Xmx` is an upper bound on the Java heap, not a measure of
physical pages in use). Resident memory and committed heap diverge because of page faults,
swap, GC region fragmentation, and pages shared with other processes (e.g. mapped CDS archives).

**Cgroup memory charged to the container**
```bash
docker exec g1-gc-observability cat /sys/fs/cgroup/memory.current
```
This is the memory **charged to the cgroup** by the kernel — the metric the OOM killer uses.
It includes the JVM RSS plus page cache, shared library pages, and kernel slab allocations that
the container's cgroup is billed for. It is typically larger than VmRSS.

**Container-level metric from the host**
```bash
docker stats g1-gc-observability --no-stream
```
`docker stats` reports a cgroup-based MEM USAGE that may be adjusted for cache reclaimable by the kernel.
It is a separate metric from `memory.current`; the two can differ depending on the kernel version and
the storage driver.

**All three numbers above measure different things.** None of them equals the `-Xmx` heap setting.
`-Xmx` is only a bound on the Java heap; no static addition of `-Xmx` + metaspace + code cache +
stack + native buffers can predict RSS, because the operating system manages physical pages
independently of JVM internal accounting (committed vs. resident, huge pages, page fault,
transparent page sharing).

### Investigating native memory

For a deeper breakdown of JVM-native allocations (heap, class, thread, code, GC, compiler, etc.),
restart the JVM with NMT enabled:

```bash
-XX:NativeMemoryTracking=summary
```

Then, while the process is running:

```bash
docker exec g1-gc-observability jcmd 1 VM.native_memory summary
```

NMT must be enabled at JVM start — it cannot be turned on retroactively. The output shows
committed and reserved memory per JVM subsystem. Note that NMT committed memory is the JVM's
own accounting and does not match RSS or cgroup memory either.

## Rapport de métriques

`./run-and-report.sh` exécute le workload G1 dans un conteneur Docker, échantillonne
CPU, RSS JVM et mémoire cgroup pendant l'exécution, puis affiche un rapport synthétique :

```bash
./run-and-report.sh 10          # run de 10 secondes
./run-and-report.sh             # défaut : 10 secondes
./run-and-report.sh 3           # run court, à des fins de test
```

Le script est **Docker-backed** : il lance directement
`docker run -d --name <unique> --memory=512m --cpus=1` avec l'image `eclipse-temurin:25-jdk`,
reprend les flags G1 / GC log / `os+container` / JFR de la section Docker ci-dessus et passe
la durée en argument au workload. Il exige Docker et le JAR construit (`./mvnw clean package`).
Il valide la durée (entier strictement positif), supprime le conteneur même en cas d'erreur
(`trap`) et sort avec un code non nul si le workload échoue ou si les artefacts manquent.

Sortie indicative (valeurs d'exemple) :

```
=== Rapport G1 GC — run de 10 s (échantillonnage conteneur) ===
Durée demandée             : 10 s
Itérations                 : 498
Objets retenus             : 1216
Allocation estimée         : 1693.2 MiB/s
Pauses GC                  : 312
  durée totale             : 1520.540 ms
  durée moyenne            : 4.873 ms
  durée max                : 21.402 ms
Événements jdk.GarbageCollection : 310
RSS JVM max                : 301.2 MiB
Mémoire cgroup max         : 288.9 MiB
CPU max                    : 91.7 %
Échantillons               : 9
Artefacts:
  JAR : .../target/g1-gc-observability-1.0.0.jar
  Logs: .../target/gc.log (+ rotations gc.log.*)
  JFR : .../target/g1.jfr
```

### Signification des métriques

- **Durée demandée / Itérations / Objets retenus** — paramètres du run et résultats du workload, extraits des logs du conteneur.
- **Allocation estimée** — ~34 MiB alloués par itération (512 × 64 KiB éphémères + 16 × 128 KiB retenus), rapportés à la seconde. Estimation théorique, pas une mesure.
- **Pauses GC** — nombre et durées (totale, moyenne, max) des pauses G1 extraites des lignes `GC(n) Pause ... Nms` de `target/gc.log*`.
- **Événements jdk.GarbageCollection** — compteur des événements du JFR, si l'outil `jfr` du JDK est sur le `PATH`.
- **RSS JVM max** — `VmRSS` du process JVM (PID 1 du conteneur), lu via `docker exec ... /proc/1/status`.
- **Mémoire cgroup max** — `memory.current` lu dans le conteneur : mémoire facturée au cgroup par le noyau, la métrique de l'OOM killer.
- **CPU max** — pourcentage CPU du conteneur lu via `docker stats`, plafonné à 100 % par `--cpus=1`.

### `-Xmx`, RSS JVM et `memory.current` : trois valeurs distinctes

- `-Xmx256m` borne uniquement le **heap Java** : ce n'est ni une mesure de mémoire physique ni une limite de conteneur.
- **RSS JVM** mesure les pages physiques du process (heap + metaspace + code cache + stacks + native).
- **memory.current** mesure la charge du cgroup (RSS + page cache + pages partagées + slab), généralement supérieure au RSS.

Aucune de ces valeurs ne se déduit des autres : ne les additionnez pas et ne les comparez pas directement.

### Limite : un échantillonnage, pas une télémétrie continue

Le rapport échantillonne au plus une fois par seconde. C'est un **échantillonnage** :
il peut sous-estimer les pics brefs (pause GC < 1 s, pic d'allocation native, etc.).
Pour une analyse fine et événementielle, utilisez `target/g1.jfr` (JMC, outil `jfr`) et les logs GC.

## Analyse des artefacts

`./analyze-g1.sh` analyse les artefacts produits par un run local ou Docker :

```bash
export JAVA_HOME="$HOME/.sdkman/candidates/java/25.0.3-tem"
export PATH="$JAVA_HOME/bin:$PATH"
./analyze-g1.sh            # analyse target/ (défaut)
./analyze-g1.sh /chemin/vers/repertoire
```

Le script :
- exige la commande JDK `jfr` sur le `PATH` ;
- exige `<repertoire>/g1.jfr` et au moins un `<repertoire>/gc.log*` ;
- affiche `jfr summary`, les événements `jdk.GarbageCollection` et un comptage
  des lignes de pause GC (`GC(n) Pause ...`) dans tous les logs GC ;
- sort avec un code non nul et un message clair si un prérequis ou un artefact manque.

## Artefacts

| Artefact | Description |
|---|---|
| `target/g1-gc-observability-1.0.0.jar` | Executable JAR |
| `target/gc.log` (and `.0`, `.1`, …) | Unified GC logs (rotating) |
| `target/g1.jfr` | JFR recording (open with JDK Mission Control or `jfr` tool) |

## Notes

Docker is **optional** for local runs but required for cgroup-constrained measurements:
the `--memory=512m` / `mem_limit: 512m` flag caps the **container** memory (cgroup limit),
not just the JVM heap.
The JVM's total RSS footprint includes the heap (`-Xmx256m`), Metaspace, code cache, stack, native memory, and the GC overhead.
As a result, the JVM may be restricted or killed by the cgroup memory limit even when `-Xmx` is well below 512 MB.

### Example configuration

The Docker command above uses `-Xms256m -Xmx256m` with `--memory=512m`. This is a
**configuration to measure**, not a budget or a guaranteed safe margin. Run the workload,
then observe `VmRSS`, `memory.current`, and `docker stats` as described above to understand
where the JVM's memory goes under G1. Adjust `-Xmx` or the cgroup limit based on actual
observations, not on a static component estimate.
