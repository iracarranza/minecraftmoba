# Skeleton Crew native acceptance

Runs the complete kit and shared single-link Supply Line foundation on a **new
isolated disposable Paper 1.21.11 server** with Java 21. Reuses only a local runtime,
dependency cache and already accepted EULA. Never edits or launches the user's server.

```sh
JAVA_HOME=/opt/homebrew/opt/openjdk@21 implementation/plugin/gradlew -p implementation/plugin test jar --offline
python3 validation/plugin/skeleton-crew/run.py \
  --runtime /absolute/path/to/existing-paper-runtime \
  --server /private/tmp/new-crew-acceptance \
  --results validation/plugin/skeleton-crew/results/2026-10-11
```

The server directory must not exist. Compilation goes under ignored `build/`;
worlds remain in the disposable directory. The server binds 127.0.0.1:25668 and
stops after the fixture, with a 180-second timeout. Run native fixtures serially.

The fixture uses real NMS player bodies, native mobs/pathfinding, Copper Chest
inventories, sourced damage and projectile impacts. Assignment uses worker commands;
endpoint designation invokes the same production recognition method as its listener.
Physical round trips are observed, not fabricated transport evidence. Reload checks
construct the actual persistence managers from saved sidecars and reconstruct real
ordinary worker bodies; they do not claim a whole-server process restart. Water
recovery places the body in real water, testing native extinguishing and recovery,
not a proof that every obstacle configuration can be navigated. Native cleanup
counts owned workers, excluding unrelated natural mobs.

See [delivery limits](../../../docs/design/SKELETON_CREW_LAB_PASS.md). The skirmish
scenario's schema is validated separately; no launched-scoop run, human client
acceptance, abrupt-crash transaction safety or final balance is claimed.
