#!/bin/bash
set -euo pipefail

# Fast ClusterFuzzLite build — single compile pass, fat fuzz jar.
export GRADLE_OPTS="-Dorg.gradle.daemon=false -Dorg.gradle.parallel=true -Xmx2g"
./gradlew --no-daemon compileJava compileFuzzJava fuzzJar -q

OUT="${OUT:-$PWD/out}"
mkdir -p "$OUT"

JAZzer_VERSION="0.24.0"
JAZzer_JAR="${JAZzer_JAR:-jazzer-standalone-${JAZzer_VERSION}.jar}"
if [ ! -f "$JAZzer_JAR" ]; then
  curl -fsSL -o "$JAZzer_JAR" \
    "https://repo1.maven.org/maven2/com/code-intelligence/jazzer/jazzer-standalone/${JAZzer_VERSION}/jazzer-standalone-${JAZzer_VERSION}.jar"
fi

FUZZ_CP="build/libs/veridian-fuzz.jar"

for target in BatchFuzzer RouterFuzzer SessionFuzzer FixFuzzer SwiftFuzzer; do
  cat > "$OUT/${target}" <<EOF
#!/bin/bash
exec java -XX:-UsePerfData -Xmx512m -XX:ActiveProcessorCount=2 \\
  -Djazzer.max_len=8192 \\
  -Djazzer.keep_going=0 \\
  -cp "${JAZzer_JAR}:${FUZZ_CP}" \\
  com.code_intelligence.jazzer.Jazzer \\
  --target_class=com.veridian.collateral.fuzz.${target} \\
  --reproducer_path=\${REPRODUCERS:-./reproducers}/${target} \\
  "\$@"
EOF
  chmod +x "$OUT/${target}"
done

echo "Built 5 fuzz targets in ${OUT}"
