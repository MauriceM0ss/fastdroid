#!/usr/bin/env bash
# Run Gradle inside the build container: scripts/gradle.sh assembleDebug
#
# --user matters: without it the container stamps root-owned files into the
# repo (build/, .gradle/) and the next build fails with EACCES.
# The Gradle cache and the debug signing key live in ~/.cache/fastdroid-build.
# The key must persist: a fresh one per build would make `adb install -r`
# refuse to update the app. ~/.android itself is deliberately NOT mounted: it
# also holds adbkey, the private key that authorises USB debugging on your
# phone, and the build runs third-party Gradle plugins and libraries.
set -euo pipefail

repo="$(cd "$(dirname "$0")/.." && pwd)"
# `scripts/gradle.sh lock` regenerates the dependency lockfiles
# (app/gradle.lockfile, buildscript-gradle.lockfile) after a version bump. It runs
# the real build, test and lint tasks: the Kotlin and Android plugins add
# dependencies at build time that `dependencies` alone never resolves.
if [ "${1:-}" = lock ]; then
  set -- buildEnvironment :app:dependencies \
    assembleDebug assembleRelease assembleDebugAndroidTest testDebugUnitTest lintDebug \
    --write-locks
fi
# Tagged by the Dockerfile's hash, so editing the Dockerfile (e.g. a new SDK
# platform) rebuilds the image on the next run instead of silently reusing the old one.
image="fastdroid-build:$(sha256sum "$repo/docker/Dockerfile" | cut -c1-12)"
home_cache="$HOME/.cache/fastdroid-build"

if ! docker image inspect "$image" > /dev/null 2>&1; then
  docker build -t "$image" "$repo/docker" >&2
fi

# Older versions mounted ~/.android at this path, so Docker left it behind as an
# empty root-owned directory; the user owns the parent, so it can be removed.
if [ -d "$home_cache/.android" ] && [ ! -w "$home_cache/.android" ]; then
  rmdir "$home_cache/.android"
fi
mkdir -p "$home_cache/.android"
# Earlier versions of this script kept the key in ~/.android; copy it over once
# so new builds still install over an existing app without losing its data.
if [ ! -f "$home_cache/.android/debug.keystore" ] && [ -f "$HOME/.android/debug.keystore" ]; then
  cp "$HOME/.android/debug.keystore" "$home_cache/.android/debug.keystore"
fi

tty_flag=()
[ -t 1 ] && tty_flag=(-t)

exec docker run --rm "${tty_flag[@]}" \
  --user "$(id -u):$(id -g)" \
  -e HOME=/build-home \
  -e GRADLE_USER_HOME=/build-home/gradle \
  -v "$home_cache:/build-home" \
  -v "$repo:/workspace" \
  -w /workspace \
  "$image" gradle --console=plain "$@"
