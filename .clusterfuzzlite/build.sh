#!/bin/bash
set -euo pipefail

# Offline-safe build: no curl/wget in this script. Jazzer is staged via Gradle.
export GRADLE_OPTS="-Dorg.gradle.daemon=false -Dorg.gradle.parallel=true -Xmx2g"

if [ -x "./gradlew" ]; then
  GRADLE="./gradlew"
elif command -v gradle >/dev/null 2>&1; then
  GRADLE="gradle"
else
  echo "error: gradlew or gradle required" >&2
  exit 1
fi

"$GRADLE" --no-daemon compileJava compileFuzzJava fuzzJar stageJazzer -q

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
