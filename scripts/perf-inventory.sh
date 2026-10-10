#!/usr/bin/env bash
#
# perf-inventory.sh
#
# Writes `build/perf-inventory.json` next to the build
# artifacts with the current sizes and dependency counts.
# The inventory is the input to perf-budget-check.sh.
#
# Per docs/performance-budget.md:
#   - release artifact size (Android APK / iOS IPA)
#   - per-asset size (companion GLB, ambient audio,
#     background texture, font subset)
#   - first-party dependency count
#   - relative regression vs. previous release

set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
OUTPUT_DIR="${REPO_ROOT}/build"
OUTPUT_FILE="${OUTPUT_DIR}/perf-inventory.json"

mkdir -p "${OUTPUT_DIR}"

COMPOSE_APP_DEPS=0
ANDROID_APP_DEPS=0

# Count first-party dependencies from the gradle
# `dependencies { implementation(project(":...")) }` blocks.
# A first-party dep is one that uses `project(":...")` syntax
# (not a Maven coordinate). We grep the build files.
count_first_party_deps() {
    local file="$1"
    if [ ! -f "$file" ]; then
        echo 0
        return 0
    fi
    # `grep` returns 1 when no match; we ignore that and
    # always emit a number. The `|| true` short-circuits
    # the non-zero exit so `set -e` does not abort.
    local count
    count=$(grep -oE 'project\(":[a-zA-Z0-9_]+"\)' "$file" 2>/dev/null | sort -u | wc -l | tr -d ' ' || true)
    echo "${count:-0}"
}

COMPOSE_APP_DEPS=$(count_first_party_deps "${REPO_ROOT}/composeApp/build.gradle.kts")
ANDROID_APP_DEPS=$(count_first_party_deps "${REPO_ROOT}/androidApp/build.gradle.kts")
TOTAL_DEPS=$((COMPOSE_APP_DEPS + ANDROID_APP_DEPS))

# Probe the Android release APK size if it exists.
APK_BYTES=0
if [ -f "${REPO_ROOT}/androidApp/build/outputs/apk/release/app-release.apk" ]; then
    APK_BYTES=$(stat -f%z "${REPO_ROOT}/androidApp/build/outputs/apk/release/app-release.apk" 2>/dev/null \
        || stat -c%s "${REPO_ROOT}/androidApp/build/outputs/apk/release/app-release.apk" 2>/dev/null \
        || echo 0)
fi

# Probe the iOS framework size if it exists.
KMP_FRAMEWORK_BYTES=0
if [ -d "${REPO_ROOT}/composeApp/build/framework" ]; then
    KMP_FRAMEWORK_BYTES=$(find "${REPO_ROOT}/composeApp/build/framework" -name '*.framework' -type d -print0 \
        | while IFS= read -r -d '' dir; do
            du -sk "$dir" 2>/dev/null | awk '{print $1 * 1024}'
        done \
        | awk '{s+=$1} END {print s+0}')
fi

cat > "${OUTPUT_FILE}" <<EOF
{
  "schemaVersion": 1,
  "timestamp": "$(date -u +%Y-%m-%dT%H:%M:%SZ)",
  "binary": {
    "androidApkBytes": ${APK_BYTES},
    "kmpFrameworkBytes": ${KMP_FRAMEWORK_BYTES}
  },
  "dependencies": {
    "composeAppDirect": ${COMPOSE_APP_DEPS},
    "androidAppDirect": ${ANDROID_APP_DEPS},
    "totalDirect": ${TOTAL_DEPS}
  }
}
EOF

echo "Wrote ${OUTPUT_FILE}"
