#!/usr/bin/env bash

# POSIX-compliant git-aware focused test runner for teddy-fyi-android
# Usage:
#   ./run_focused_tests.sh                        (Smart selection based on git diff)
#   ./run_focused_tests.sh "fyi.teddy.android.*"  (Run tests matching pattern)
#   ./run_focused_tests.sh testFullDebugUnitTest  (Run specific Gradle task)

set -e

# The unit tests are shared by both product flavours (`full` and `grocery`), so one of the two
# variant tasks runs all of them; there is no flavour-less `testDebugUnitTest` task to call
# once the flavours exist. `full` is the one that compiles every surface, todo included.
UNIT_TEST_TASK="testFullDebugUnitTest"

# If arguments are passed, execute targeting those tests or tasks
if [ $# -gt 0 ]; then
  ARG="$1"
  if [ "$ARG" = "test" ] || [ "$ARG" = "$UNIT_TEST_TASK" ] || [ "${ARG#*:}" != "$ARG" ]; then
    echo "==> Executing specified Gradle task: $ARG"
    exec ./gradlew "$@"
  else
    echo "==> Executing focused unit tests for pattern: $ARG"
    exec ./gradlew "$UNIT_TEST_TASK" --tests "$ARG"
  fi
fi

# Determine base branch or target for comparison
TARGET_REF="main"
if ! git rev-parse --verify "$TARGET_REF" >/dev/null 2>&1; then
  if git rev-parse --verify "master" >/dev/null 2>&1; then
    TARGET_REF="master"
  else
    TARGET_REF="HEAD"
  fi
fi

echo "==> Analyzing git changes against target: $TARGET_REF"

# Collect list of changed files (unstaged + staged + unpushed commits compared to target ref)
CHANGED_FILES=$( (git diff --name-only "$TARGET_REF"...HEAD 2>/dev/null || true; git status --porcelain | awk '{print $2}') | sort -u | sed '/^$/d' )

if [ -z "$CHANGED_FILES" ]; then
  echo "==> No modified files detected. Executing default unit test suite."
  exec ./gradlew "$UNIT_TEST_TASK"
fi

echo "==> Modified files detected:"
echo "$CHANGED_FILES" | sed 's/^/  - /'

# Rule 1: Skip tests if ONLY non-code/documentation files are modified
NON_CODE_ONLY=true
while IFS= read -r file; do
  case "$file" in
    *.md|*.txt|*.png|*.jpg|*.jpeg|*.svg|*.webp|.gitignore|LICENSE|AGENTS.md|AI_CONTEXT.md|setup.md|.janitor*|*.json)
      ;;
    *)
      NON_CODE_ONLY=false
      break
      ;;
  esac
done <<< "$CHANGED_FILES"

if [ "$NON_CODE_ONLY" = true ]; then
  echo "==> [Rule 1] Only non-code/documentation files modified. Skipping unit tests."
  exit 0
fi

# Rule 2: Full build & test sweep if build/infra files modified
BUILD_INFRA_TOUCHED=false
while IFS= read -r file; do
  case "$file" in
    *build.gradle*|*settings.gradle*|*gradle.properties*|gradle/*|gradlew*|run_focused_tests.sh|.github/*|config/*)
      BUILD_INFRA_TOUCHED=true
      break
      ;;
  esac
done <<< "$CHANGED_FILES"

if [ "$BUILD_INFRA_TOUCHED" = true ]; then
  echo "==> [Rule 2] Build infrastructure files modified. Executing full unit test suite."
  exec ./gradlew "$UNIT_TEST_TASK"
fi

# Rule 3 & 4: Subsystem/Feature isolation
# Find feature subpackages in changed files
PACKAGES=""
while IFS= read -r file; do
  case "$file" in
    app/src/*/java/fyi/teddy/android/*)
      # Extract relative path after fyi/teddy/android/
      REL_PATH=$(echo "$file" | sed -n 's|.*/fyi/teddy/android/\([^/]*\)/.*|\1|p')
      if [ -n "$REL_PATH" ]; then
        PACKAGES="$PACKAGES $REL_PATH"
      fi
      ;;
  esac
done <<< "$CHANGED_FILES"

UNIQUE_PACKAGES=$(echo "$PACKAGES" | tr ' ' '\n' | sort -u | sed '/^$/d')
if [ -z "$UNIQUE_PACKAGES" ]; then
  PKG_COUNT=0
else
  PKG_COUNT=$(echo "$UNIQUE_PACKAGES" | wc -l | tr -d ' ')
fi

if [ "$PKG_COUNT" -eq 1 ]; then
  PKG=$(echo "$UNIQUE_PACKAGES" | tr -d '\r\n')
  TEST_PATTERN="fyi.teddy.android.${PKG}.*"
  echo "==> [Rule 4] Changes isolated to subsystem '${PKG}'. Executing focused test pattern: ${TEST_PATTERN}"
  exec ./gradlew "$UNIT_TEST_TASK" --tests "$TEST_PATTERN"
else
  echo "==> [Rule 3] Changes affect multiple subsystems or shared components. Executing full unit test suite."
  exec ./gradlew "$UNIT_TEST_TASK"
fi
