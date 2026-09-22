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

# alpha.templatePath is repo-relative in config.yml, which is right for the
# repository and unresolvable from a server that lives outside it -- and the
# frozen template is not in git (worlds are gitignored), so it exists only in
# the main checkout, never in a worktree. Rewrite the key to an absolute path
# on the way out rather than committing a machine-specific one.
#
# This matters more than it looks: with a wrong template path, materializing
# fails, and until 2026-09-21 load() silently skipped materializing whenever an
# instance directory already existed. The two defects cancelled into "the world
# never resets", which is what was actually reported.
MAIN_CHECKOUT="$(dirname "$(git rev-parse --path-format=absolute --git-common-dir)")"
TEMPLATE="$MAIN_CHECKOUT/artifacts/worldgen/alpha-0.1/base-terrain"
MAPS="$MAIN_CHECKOUT/artifacts/worldgen/alpha-0.1/maps"
if [ ! -d "$TEMPLATE" ]; then
    echo "REFUSED: no base terrain at $TEMPLATE" >&2
    echo "Every match open and every reset would fail. Build or restore it first." >&2
    exit 1
fi
if [ ! -d "$MAPS" ]; then
    echo "REFUSED: no map configurations at $MAPS" >&2
    echo "Export them with terrain_harvest.map_diff first." >&2
    exit 1
fi
# Map configurations are small enough to live beside the server rather than be
# searched for, and they must travel with the jar that reads them.
mkdir -p "$SERVER/maps" && cp "$MAPS"/*.json.gz "$SERVER/maps/" 2>/dev/null
echo "  maps -> $(ls "$SERVER/maps" | tr '\n' ' ')"
sed "s|^  templatePath:.*|  templatePath: \"$TEMPLATE\"|" \
    src/main/resources/config.yml > "$SERVER/plugins/MinecraftMoba/config.yml"
grep -q "templatePath: \"$TEMPLATE\"" "$SERVER/plugins/MinecraftMoba/config.yml" || {
    echo "REFUSED: could not rewrite alpha.templatePath; check its key in config.yml" >&2
    exit 1
}
# Build, zip and hash the resource pack, and write the hash into the config we
# just deployed. The pack and its hash must ship together: clients cache by
# hash, so a rebuilt pack behind a stale hash is silently not applied, and the
# symptom is indistinguishable from the pack not working at all.
PACK_DIR="$SERVER/pack"
python3 "$(dirname "$0")/../resourcepack/publish.py" \
    --serve-dir "$PACK_DIR" \
    --config "$SERVER/plugins/MinecraftMoba/config.yml" | sed 's/^/  /'

# The pack has to be reachable over HTTP for the server to hand it out.
if ! lsof -ti :25580 >/dev/null 2>&1; then
    (cd "$PACK_DIR" && nohup python3 -m http.server 25580 --bind 127.0.0.1 \
        > "$PACK_DIR/http.log" 2>&1 &)
    sleep 1
    echo "  started the pack host on 127.0.0.1:25580"
else
    echo "  pack host already running on 127.0.0.1:25580"
fi

echo "deployed $(basename "$JAR") and config.yml to $SERVER"
echo "  template -> $TEMPLATE"
