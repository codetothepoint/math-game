#!/usr/bin/env bash
# Compile and test `core` without Gradle's dependency resolution.
#
# For environments that have a JDK and a Kotlin compiler but no access to Maven Central
# (the normal path is `./gradlew :core:test`). Uses:
#   KOTLINC_HOME  a kotlinc distribution (bin/kotlinc, lib/kotlin-stdlib.jar, lib/kotlin-test*.jar)
#   GRADLE_HOME   a Gradle distribution, for the gson and junit4 jars it bundles
# Tests are written against kotlin.test, so they run on JUnit 4 here and JUnit 5 under Gradle.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
KOTLINC_HOME="${KOTLINC_HOME:?set KOTLINC_HOME}"
GRADLE_HOME="${GRADLE_HOME:?set GRADLE_HOME}"
OUT="${OUT:-$ROOT/build/core-check}"

GSON="$(ls "$GRADLE_HOME"/lib/gson-*.jar | head -1)"
JUNIT4="$(ls "$GRADLE_HOME"/lib/junit-4*.jar | head -1)"
HAMCREST="$(ls "$GRADLE_HOME"/lib/hamcrest-core-*.jar 2>/dev/null | head -1 || true)"
STDLIB="$KOTLINC_HOME/lib/kotlin-stdlib.jar"
KTEST="$KOTLINC_HOME/lib/kotlin-test.jar"
KTEST_JUNIT="$KOTLINC_HOME/lib/kotlin-test-junit.jar"

rm -rf "$OUT" && mkdir -p "$OUT/main" "$OUT/test" "$OUT/resources/curriculum" "$OUT/resources/recipes"
cp "$ROOT/specs/curriculum/generated/curriculum.json" "$OUT/resources/curriculum/"
[ -d "$ROOT/specs/mechanics/recipes/generated" ] && cp "$ROOT"/specs/mechanics/recipes/generated/*.json "$OUT/resources/recipes/" 2>/dev/null || true

echo "compiling core/main"
"$KOTLINC_HOME/bin/kotlinc" -Werror -jvm-target 17 -cp "$GSON" -d "$OUT/main" $(find "$ROOT/core/src/main/kotlin" -name '*.kt')

echo "compiling core/test"
"$KOTLINC_HOME/bin/kotlinc" -Werror -jvm-target 17 -cp "$OUT/main:$GSON:$KTEST:$KTEST_JUNIT:$JUNIT4" -d "$OUT/test" $(find "$ROOT/core/src/test/kotlin" -name '*.kt')

CLASSES=$(cd "$OUT/test" && find . -name '*Test.class' | sed 's#^\./##; s#\.class$##; s#/#.#g' | grep -v '\$' | sort)
echo "running: $(echo "$CLASSES" | wc -l) test classes"
java -cp "$OUT/main:$OUT/test:$OUT/resources:$GSON:$STDLIB:$KTEST:$KTEST_JUNIT:$JUNIT4:$HAMCREST" org.junit.runner.JUnitCore $CLASSES
