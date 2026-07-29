#!/bin/bash
set -euo pipefail

# Offline-safe build: no curl/wget in this script. Jazzer is staged via Gradle.
export GRADLE_OPTS="-Dorg.gradle.daemon=false -Dorg.gradle.parallel=true -Xmx2g"

run_gradle() {
  if [ -f "./gradlew" ]; then
    # Fenrir checkouts may not preserve the git executable bit; bash does not require +x.
    bash ./gradlew "$@"
  elif command -v gradle >/dev/null 2>&1; then
    gradle "$@"
  else
    echo "error: gradlew or gradle required (missing ./gradlew and system gradle)" >&2
    ls -la . 2>/dev/null || true
    exit 1
  fi
}

run_gradle --no-daemon compileJava compileFuzzJava fuzzJar stageJazzer -q

OUT="${OUT:-$PWD/out}"
mkdir -p "$OUT"

JAZZER_JAR="build/jazzer/jazzer-standalone.jar"
if [ ! -f "$JAZZER_JAR" ]; then
  echo "error: missing staged Jazzer jar at ${JAZZER_JAR}" >&2
  exit 1
fi

FUZZ_CP="build/libs/veridian-fuzz.jar"
if [ ! -f "$FUZZ_CP" ]; then
  echo "error: missing fuzz jar at ${FUZZ_CP}" >&2
  exit 1
fi

for target in BatchFuzzer RouterFuzzer SessionFuzzer FixFuzzer SwiftFuzzer; do
  cat > "$OUT/${target}" <<EOF
#!/bin/bash
exec java -XX:-UsePerfData -Xmx512m -XX:ActiveProcessorCount=2 \\
  -Djazzer.max_len=8192 \\
  -Djazzer.keep_going=0 \\
  -cp "${JAZZER_JAR}:${FUZZ_CP}" \\
  com.code_intelligence.jazzer.Jazzer \\
  --target_class=com.veridian.collateral.fuzz.${target} \\
  --reproducer_path=\${REPRODUCERS:-./reproducers}/${target} \\
  "\$@"
EOF
  chmod +x "$OUT/${target}"
done

echo "Built 5 fuzz targets in ${OUT}"
