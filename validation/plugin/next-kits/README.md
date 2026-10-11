# Next class kit native acceptance

Runs Kitfighter, Gardener and Werewolf against a **new disposable Paper server**.
Uses the repository's Java 21/Paper 1.21.11 plugin, real NMS player bodies, Bukkit
inventories, damage/events, attributes, block growth and shared reservations.
The runner never launches or edits the user's server. It reuses only an existing
local Paper runtime/dependency cache and previously accepted EULA.

```sh
JAVA_HOME=/opt/homebrew/opt/openjdk@21 implementation/plugin/gradlew -p implementation/plugin test jar --offline
python3 validation/plugin/next-kits/run.py \
  --runtime /absolute/path/to/existing-paper-runtime \
  --server /private/tmp/new-next-kit-acceptance \
  --results validation/plugin/next-kits/results/2026-10-11
```

`--server` must not exist. Compilation outputs go under the ignored `build/`
directory. The server binds 127.0.0.1:25668 and stops when the fixture completes;
a 180-second timeout stops a hung run. Source and saved validation results remain
separate from compiled artifacts and generated worlds.

The crafting test uses an actual Paper crafting inventory and recipe, dispatches
its CraftItemEvent, then explicitly supplies the post-craft matrix/output. It
checks settlement/real item conservation, not a network client's shift-click.
Projectile pass-through similarly dispatches the native hit event; every
projectile's native follow-through and human offhand client behavior remain
playtest work. No launched-scoop scenario or final balance is claimed by these
isolated tests. See docs/design/NEXT_CLASS_KIT_LAB_PASS.md for Open limits.
