#!/usr/bin/env bash
# Minimal Android command-line SDK under ./sdk (japanglify path, Linux host).
# Domain tests need no SDK — only :app:assemble* does.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
SDK_ROOT="${ANDROID_SDK_ROOT:-${ANDROID_HOME:-$ROOT/sdk}}"
mkdir -p "$SDK_ROOT/cmdline-tools"

if [[ -z "${JAVA_HOME:-}" ]]; then
  JAVA_BIN="$(readlink -f "$(command -v java)")"
  export JAVA_HOME="$(dirname "$(dirname "$JAVA_BIN")")"
fi

PLATFORM=linux
CMDLINE_ZIP="commandlinetools-${PLATFORM}-11076708_latest.zip"
CMDLINE_URL="https://dl.google.com/android/repository/${CMDLINE_ZIP}"
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT

if [[ ! -f "$SDK_ROOT/cmdline-tools/latest/bin/sdkmanager" ]]; then
  echo "Downloading Android command-line tools ..."
  curl -fL --retry 3 -o "$TMP/cmdtools.zip" "$CMDLINE_URL"
  unzip -q "$TMP/cmdtools.zip" -d "$TMP"
  rm -rf "$SDK_ROOT/cmdline-tools/latest"
  mkdir -p "$SDK_ROOT/cmdline-tools"
  if [[ -d "$TMP/cmdline-tools/bin" ]]; then
    mv "$TMP/cmdline-tools" "$SDK_ROOT/cmdline-tools/latest"
  elif [[ -d "$TMP/cmdline-tools/latest" ]]; then
    mv "$TMP/cmdline-tools/latest" "$SDK_ROOT/cmdline-tools/latest"
  else
    mv "$TMP/cmdline-tools" "$SDK_ROOT/cmdline-tools/latest"
  fi
fi

export ANDROID_HOME="$SDK_ROOT"
export ANDROID_SDK_ROOT="$SDK_ROOT"
SDKMANAGER="$SDK_ROOT/cmdline-tools/latest/bin/sdkmanager"
chmod +x "$SDKMANAGER" "$SDK_ROOT/cmdline-tools/latest/bin/"* || true

feed_yes() {
  for _ in $(seq 1 300); do printf 'y\n'; done
}

echo "Accepting licenses and installing platform-tools, android-35, build-tools;35.0.0 ..."
feed_yes | "$SDKMANAGER" --sdk_root="$SDK_ROOT" --licenses >/dev/null || true
feed_yes | "$SDKMANAGER" --sdk_root="$SDK_ROOT" \
  "platform-tools" "platforms;android-35" "build-tools;35.0.0"

printf 'sdk.dir=%s\n' "$SDK_ROOT" > "$ROOT/local.properties"
echo "Wrote $ROOT/local.properties"
echo "SDK ready at $SDK_ROOT"
echo "Build APK: ./gradlew :app:assembleDebug"
