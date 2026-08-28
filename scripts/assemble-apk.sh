#!/usr/bin/env bash
# Build the debug APK after bootstrap-android-sdk.sh (japanglify path).
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"
if [[ -z "${JAVA_HOME:-}" ]]; then
  JAVA_BIN="$(readlink -f "$(command -v java)")"
  export JAVA_HOME="$(dirname "$(dirname "$JAVA_BIN")")"
fi
export ANDROID_SDK_ROOT="${ANDROID_SDK_ROOT:-${ANDROID_HOME:-$ROOT/sdk}}"
export ANDROID_HOME="$ANDROID_SDK_ROOT"
if [[ ! -f "$ROOT/local.properties" ]]; then
  bash "$ROOT/scripts/bootstrap-android-sdk.sh"
fi
exec ./gradlew :domain:test :app:assembleDebug --no-daemon "$@"
