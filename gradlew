#!/usr/bin/env sh
set -eu

APP_HOME=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
GRADLE_VERSION=8.2.1
GRADLE_HOME="$APP_HOME/.gradle/wrapper/dists/gradle-$GRADLE_VERSION-bin/gradle-$GRADLE_VERSION"
GRADLE_BIN="$GRADLE_HOME/bin/gradle"

if [ ! -x "$GRADLE_BIN" ]; then
  ZIP_DIR="$APP_HOME/.gradle/wrapper/dists"
  ZIP_FILE="$ZIP_DIR/gradle-$GRADLE_VERSION-bin.zip"
  mkdir -p "$ZIP_DIR"

  if [ ! -s "$ZIP_FILE" ]; then
    TMP_ZIP="$ZIP_FILE.tmp"
    rm -f "$TMP_ZIP"
    if command -v curl >/dev/null 2>&1; then
      curl -L --fail -o "$TMP_ZIP" "https://services.gradle.org/distributions/gradle-$GRADLE_VERSION-bin.zip"
    elif command -v wget >/dev/null 2>&1; then
      wget -O "$TMP_ZIP" "https://services.gradle.org/distributions/gradle-$GRADLE_VERSION-bin.zip"
    else
      echo "curl or wget is required to download Gradle $GRADLE_VERSION." >&2
      exit 1
    fi
    mv "$TMP_ZIP" "$ZIP_FILE"
  fi

  if command -v unzip >/dev/null 2>&1; then
    unzip -q -o "$ZIP_FILE" -d "$ZIP_DIR"
  else
    echo "unzip is required to extract Gradle $GRADLE_VERSION." >&2
    exit 1
  fi
fi

exec "$GRADLE_BIN" "$@"
