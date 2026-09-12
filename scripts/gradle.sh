#!/usr/bin/env bash
# Run Gradle inside the build container: scripts/gradle.sh assembleDebug
#
# --user matters: without it the container stamps root-owned files into the
# repo (build/, .gradle/) and the next build fails with EACCES.
# The Gradle cache lives in ~/.cache/fastdroid-build, and ~/.android is mounted
# so the debug keystore persists — a fresh key per build would make
# `adb install -r` refuse to update the app.
set -euo pipefail

repo="$(cd "$(dirname "$0")/.." && pwd)"
# Tagged by the Dockerfile's hash, so editing the Dockerfile (e.g. a new SDK
# platform) rebuilds the image on the next run instead of silently reusing the old one.
image="fastdroid-build:$(sha256sum "$repo/docker/Dockerfile" | cut -c1-12)"
home_cache="$HOME/.cache/fastdroid-build"

if ! docker image inspect "$image" > /dev/null 2>&1; then
  docker build -t "$image" "$repo/docker" >&2
fi

mkdir -p "$home_cache" "$HOME/.android"

tty_flag=()
[ -t 1 ] && tty_flag=(-t)

exec docker run --rm "${tty_flag[@]}" \
  --user "$(id -u):$(id -g)" \
  -e HOME=/build-home \
  -e GRADLE_USER_HOME=/build-home/gradle \
  -v "$home_cache:/build-home" \
  -v "$HOME/.android:/build-home/.android" \
  -v "$repo:/workspace" \
  -w /workspace \
  "$image" gradle --console=plain "$@"
