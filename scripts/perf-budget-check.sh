#!/usr/bin/env bash
#
# perf-budget-check.sh
#
# Reads build/perf-inventory.json (written by
# perf-inventory.sh) and compares the current values
# against the [perf-budget](../docs/performance-budget.md)
# hard ceilings. Exits non-zero when a hard ceiling is
# violated.
#
# This is the V1 wire-up of #184. A future polish slice
# adds the relative-regression diff step.

set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
INVENTORY_FILE="${REPO_ROOT}/build/perf-inventory.json"

if [ ! -f "${INVENTORY_FILE}" ]; then
    echo "perf-inventory.json not found at ${INVENTORY_FILE}" >&2
    echo "Run scripts/perf-inventory.sh first." >&2
    exit 2
fi

# Hard ceilings. Mirrored from
# composeApp/src/commonMain/kotlin/app/soine/perf/PerfBudget.kt
# so the source of truth and the CI step stay in sync.
MAX_COMPOSE_APP_DEPS=12
MAX_ANDROID_APP_DEPS=6
MAX_TOTAL_DEPS=18
MAX_ANDROID_APK_BYTES=18874368  # 18 MiB
MAX_KMP_FRAMEWORK_BYTES=12582912  # 12 MiB

VIOLATIONS=0

# Helper: read a JSON field value.
# Usage: read_json <field> where field is the dot-separated
# path inside the JSON document (e.g. "binary.androidApkBytes"
# or "dependencies.composeAppDirect").
read_json() {
    local field="$1"
    local value
    value=$(python3 -c "
import json, sys
with open(sys.argv[1]) as f:
    d = json.load(f)
parts = sys.argv[2].split('.')
v = d
for p in parts:
    v = v[p]
print(int(v) if isinstance(v, (int, float)) else v)
" "$INVENTORY_FILE" "$field")
    if [ -z "$value" ]; then
        echo 0
    else
        echo "$value"
    fi
}

COMPOSE_APP_DEPS=$(read_json 'dependencies.composeAppDirect')
ANDROID_APP_DEPS=$(read_json 'dependencies.androidAppDirect')
TOTAL_DEPS=$(read_json 'dependencies.totalDirect')
APK_BYTES=$(read_json 'binary.androidApkBytes')
KMP_BYTES=$(read_json 'binary.kmpFrameworkBytes')

check() {
    local label="$1"
    local actual="$2"
    local ceiling="$3"
    if [ "${actual}" -gt "${ceiling}" ]; then
        echo "VIOLATION: ${label} = ${actual} > hard ceiling ${ceiling}" >&2
        VIOLATIONS=$((VIOLATIONS + 1))
    else
        echo "OK: ${label} = ${actual} <= hard ceiling ${ceiling}"
    fi
}

check "composeApp first-party deps" "${COMPOSE_APP_DEPS}" "${MAX_COMPOSE_APP_DEPS}"
check "androidApp first-party deps" "${ANDROID_APP_DEPS}" "${MAX_ANDROID_APP_DEPS}"
check "total first-party deps" "${TOTAL_DEPS}" "${MAX_TOTAL_DEPS}"
check "android release APK bytes" "${APK_BYTES}" "${MAX_ANDROID_APK_BYTES}"
check "KMP framework bytes" "${KMP_BYTES}" "${MAX_KMP_FRAMEWORK_BYTES}"

if [ "${VIOLATIONS}" -gt 0 ]; then
    echo "" >&2
    echo "FAIL: ${VIOLATIONS} hard ceiling violation(s) detected." >&2
    exit 1
fi

echo ""
echo "PASS: all hard ceilings satisfied."
