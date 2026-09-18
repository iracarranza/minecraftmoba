# Live protocol fixture (not a vanilla client input test)

Dependency: Mineflayer 4.39.0 (lockfile included). Install with `npm ci`.
Run only against the disposable Paper 1.21.11 fixture at `127.0.0.1:25576`.
Use a NEW flat world with spawn-protection=0, peaceful difficulty, and the built
plugin. The fixture uses offline-named player `MobaTest`; it is deliberately bound
to loopback and separate from the authenticated user server. From its console,
run `op MobaTest` after the client connects. Never copy fixture settings into a
public server or use these fixture-authoring commands in a player world.

The protocol driver exercises real packet handling, Bukkit events, and world/PDC
updates. It cannot certify vanilla key generation, renderer appearance, or the
30-minute ordinary-building requirement. The earlier vanilla empty-hand Q log
remains the evidence for the input-generation gate.

Important: Mineflayer's local inventory/cursor prediction can disagree after
cancelled clicks, and its handling of the 1.21.11 velocity packet did not move
this test client despite a correct outgoing velocity. Use server-side `/data get`
inventory/block assertions and ability counters as the authority. Do not claim
client visual acceptance from these checks.

Fixture setup uses vanilla operator commands to provide items, blocks and targets.
Those commands belong to the test fixture and bypass the plugin's preventive event
mechanism by design. They are not production plugin item-management code.

`node driver.cjs` connects and records JSONL results under ignored `results/`.
After granting op, enter a scenario name printed by the driver. Scenarios are
serialized. They require the documented fresh flat-world layout; setup coordinates
for the Sinkhole example assume the player is near (2.5, -59, -5.5), with stone
in hotbar slot 0 and the test class selected. Inspect server assertions/counters
before declaring a scenario passed; the driver does not fabricate a pass status.

The Sept 18 follow-up adds `fiftySinkholes`, `rewardReset`, `entityPackets`,
`entityInteraction`, `quitInMode`, and `reconnectState`. For rewards use both
levels from `implementation/plugin/test-config/rewards-example.yml` only in the
fixture. The entity attack scenario temporarily uses Easy difficulty to summon a
husk; the sheep scenario returns to Peaceful. Run the clean reconnect check under
Peaceful (disconnect with `quitInMode`, restart driver, run `reconnectState`).
The entity interaction check deliberately holds three red dye: white sheep and
unchanged dye count provide observable evidence that vanilla interaction was
cancelled. Test setup commands bypass inventory locking; they are fixture authoring,
not plugin item handling.
