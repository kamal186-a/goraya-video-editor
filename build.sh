#!/usr/bin/env bash
# Goraya Video Edition - one-command build:  bash build.sh
set -euo pipefail
cd "$(dirname "$0")"
ROOT="$(pwd)"
fail() { echo; echo "BUILD FAILED: $*" >&2; exit 1; }

echo "==> [1/5] Checking environment"
command -v java >/dev/null || fail "Java not found. Install JDK 17 (e.g. apt install openjdk-17-jdk)."
JV=$(java -version 2>&1 | head -1 | sed -E 's/.*"([0-9]+).*/\1/')
[ "$JV" -ge 17 ] || fail "JDK 17+ required (found $JV)."
command -v keytool >/dev/null || fail "keytool missing (install a full JDK, not a JRE)."
for c in curl unzip; do command -v $c >/dev/null || fail "'$c' is required."; done

echo "==> [2/5] Android SDK"
SDK="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}"
if [ -z "$SDK" ] && [ -d "$HOME/Android/Sdk" ]; then SDK="$HOME/Android/Sdk"; fi
if [ -z "$SDK" ] || [ ! -d "$SDK" ]; then
  SDK="$ROOT/.android-sdk"
  if [ ! -x "$SDK/cmdline-tools/latest/bin/sdkmanager" ]; then
    echo "SDK not found - downloading command-line tools (Linux only)..."
    [ "$(uname -s)" = "Linux" ] || fail "Set ANDROID_HOME to your Android SDK."
    mkdir -p "$SDK/cmdline-tools"
    curl -fL -o /tmp/cmdtools.zip https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip || fail "Could not download SDK tools."
    unzip -q -o /tmp/cmdtools.zip -d "$SDK/cmdline-tools"
    rm -rf "$SDK/cmdline-tools/latest"; mv "$SDK/cmdline-tools/cmdline-tools" "$SDK/cmdline-tools/latest"
  fi
fi
export ANDROID_HOME="$SDK"
SDKM="$SDK/cmdline-tools/latest/bin/sdkmanager"
if [ -x "$SDKM" ] && { [ ! -d "$SDK/platforms/android-34" ] || [ ! -d "$SDK/build-tools/34.0.0" ]; }; then
  yes | "$SDKM" --licenses >/dev/null 2>&1 || true
  "$SDKM" "platforms;android-34" "build-tools;34.0.0" "platform-tools" || fail "SDK package install failed."
fi
echo "sdk.dir=$SDK" > local.properties

echo "==> [3/5] Signing key and Gradle"
if [ ! -f release.keystore ]; then
  keytool -genkeypair -keystore release.keystore -storepass goraya123 -keypass goraya123 \
    -alias goraya -keyalg RSA -keysize 2048 -validity 10000 \
    -dname "CN=Goraya Video Edition, O=Goraya" >/dev/null 2>&1 || fail "Keystore creation failed."
fi
if [ -x ./gradlew ]; then GR=./gradlew
elif command -v gradle >/dev/null && gradle --version 2>/dev/null | grep -q "Gradle [89]"; then GR=gradle
else
  GV=8.9
  if [ ! -x ".gradle-dist/gradle-$GV/bin/gradle" ]; then
    echo "Downloading Gradle $GV..."
    mkdir -p .gradle-dist
    curl -fL -o /tmp/gradle.zip "https://services.gradle.org/distributions/gradle-$GV-bin.zip" || fail "Could not download Gradle."
    unzip -q -o /tmp/gradle.zip -d .gradle-dist
  fi
  GR=".gradle-dist/gradle-$GV/bin/gradle"
fi

echo "==> [4/5] Building release APK"
$GR --no-daemon :app:assembleRelease || fail "Gradle build failed (see errors above)."

echo "==> [5/5] Collecting APK"
SRC=app/build/outputs/apk/release/app-release.apk
[ -f "$SRC" ] || fail "APK not found at $SRC"
mkdir -p dist && cp "$SRC" dist/GorayaVideoEdition.apk
echo; echo "BUILD SUCCESSFUL"; echo; echo "APK:"; echo "$ROOT/dist/GorayaVideoEdition.apk"
