#!/usr/bin/env bash
# Deploy the plugin jar to a test server, refusing to do it to a RUNNING one.
#
# Copying over a jar that a live JVM has open corrupts that server in a way
# that looks like a game bug rather than a deploy mistake. Paper opens the jar
# lazily: classes already loaded keep working, and every class NOT yet loaded
# becomes permanently unloadable, so the server keeps running and starts
# throwing NoClassDefFoundError from whichever code path is touched first. It
# did exactly that on 2026-09-21, and the symptom reported was "locked out of
# respawning, mobs still attacking me" -- no part of which points at a file
# copy.
#
# So this refuses rather than warning. A refused deploy costs a restart; an
# accepted one costs a corrupted playtest and a false bug report.
set -euo pipefail

SERVER="${1:-/private/tmp/alpha-server}"
JAR="$(ls build/libs/*.jar | head -1)"
TARGET="$SERVER/plugins/$(basename "$JAR")"

if lsof -t "$TARGET" >/dev/null 2>&1; then
    echo "REFUSED: a running server has $TARGET open (pid $(lsof -t "$TARGET" | tr '\n' ' '))." >&2
    echo "Stop the server, then re-run. Hot-swapping the jar corrupts it silently." >&2
    exit 1
fi
if pgrep -f "paper.jar" >/dev/null 2>&1; then
    echo "REFUSED: a paper server process is running." >&2
    echo "It may not have the jar open yet, but it will load classes from it later." >&2
    echo "Stop it first, or pass --force if you are certain it is a different server." >&2
    [ "${2:-}" = "--force" ] || exit 1
fi

cp "$JAR" "$SERVER/plugins/"
cp src/main/resources/config.yml "$SERVER/plugins/MinecraftMoba/config.yml"
echo "deployed $(basename "$JAR") and config.yml to $SERVER"
