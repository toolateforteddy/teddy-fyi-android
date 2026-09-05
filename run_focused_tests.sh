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

# Determine base branch or target for comparison.
#
# A CI checkout is detached with no local branches, so a bare "main" does not
# resolve there and the remote-tracking ref has to be tried too. Without that
# the comparison silently falls back to HEAD, every diff comes back empty, and
# the selection below degrades to "always run everything". Remote-tracking refs
# come first because a local branch can be stale; GITHUB_BASE_REF names the
# branch the pull request actually targets, when it is set.
TARGET_REF=""
for CANDIDATE in ${GITHUB_BASE_REF:+"origin/$GITHUB_BASE_REF"} origin/main origin/master main master; do
  if git rev-parse --verify --quiet "$CANDIDATE" >/dev/null 2>&1; then
    TARGET_REF="$CANDIDATE"
    break
  fi
done

if [ -z "$TARGET_REF" ]; then
  echo "==> No base branch ref found (tried origin/main, origin/master, main, master)."
  echo "==> Falling back to the full unit test suite."
  exec ./gradlew "$UNIT_TEST_TASK"
fi

echo "==> Analyzing git changes against target: $TARGET_REF"

# Collect the changed files: unpushed commits against the target ref, plus the
# working tree.
#
# `git status --porcelain | awk '{print $2}'` used to stand in for the working
# tree, but it reported the pre-rename path for renames and truncated any path
# containing a space. The plumbing below needs no parsing instead: `diff HEAD`
# covers staged and unstaged tracked edits, `ls-files --others` the untracked.
CHANGED_FILES=$( (
  git diff --name-only "$TARGET_REF"...HEAD 2>/dev/null || true
  git diff --name-only HEAD 2>/dev/null || true
  git ls-files --others --exclude-standard 2>/dev/null || true
) | sort -u | sed '/^$/d' )

if [ -z "$CHANGED_FILES" ]; then
  echo "==> No modified files detected. Executing default unit test suite."
  exec ./gradlew "$UNIT_TEST_TASK"
fi

echo "==> Modified files detected:"
echo "$CHANGED_FILES" | sed 's/^/  - /'

# Rule 1: Skip tests if ONLY non-code/documentation files are modified.
#
# Anything not listed here counts as code, so the list stays narrow and
# explicit. `*.json` in particular is deliberately absent: the Room schema
# exports under app/schemas/ and the migration fixtures under
# app/src/androidTest/assets/ are JSON, and a change to either is a database
# change the tests have to run against. Agent state files that happen to be
# JSON are named individually instead.
NON_CODE_ONLY=true
while IFS= read -r file; do
  case "$file" in
    # Prose and images, at any depth.
    *.md|*.txt|*.png|*.jpg|*.jpeg|*.svg|*.webp)
      ;;
    # Repo metadata.
    .gitignore|*/.gitignore|.gitattributes|*/.gitattributes|LICENSE|*/LICENSE)
      ;;
    # IDE settings and agent tooling config/state/scratch, none of which is on
    # the test classpath: .idea/ and .run/ are Android Studio's, .claude/ and
    # .artifacts/ are agent working files, and .janitor-state.json is the Code
    # Janitor action's own bookkeeping.
    .idea/*|.run/*|.claude/*|.artifacts/*|.janitor*)
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

# No feature package matched: the change is code, but it lives outside
# app/src/**/java/fyi/teddy/android/*. `wc -l` counts the newline echo adds to
# an empty string, so this has to be checked before the count -- otherwise an
# empty package list reads as "exactly one package" and builds the unmatchable
# filter "fyi.teddy.android..*", which selects no tests at all.
if [ -z "$UNIQUE_PACKAGES" ]; then
  echo "==> [Rule 3] Changes touch no feature package. Executing full unit test suite."
  exec ./gradlew "$UNIT_TEST_TASK"
fi

PKG_COUNT=$(echo "$UNIQUE_PACKAGES" | wc -l | tr -d ' ')

if [ "$PKG_COUNT" -eq 1 ]; then
  PKG=$(echo "$UNIQUE_PACKAGES" | tr -d '\r\n')
  TEST_PATTERN="fyi.teddy.android.${PKG}.*"
  echo "==> [Rule 4] Changes isolated to subsystem '${PKG}'. Executing focused test pattern: ${TEST_PATTERN}"
  exec ./gradlew "$UNIT_TEST_TASK" --tests "$TEST_PATTERN"
else
  echo "==> [Rule 3] Changes affect multiple subsystems or shared components. Executing full unit test suite."
  exec ./gradlew "$UNIT_TEST_TASK"
fi
