# Phase 1 validation status — 2026-09-17

## Environment

- Target selected before first commit: Paper **1.21.11**, Java **21**.
- Official Paper build **132**, commit `c5eb079`.
- Paper download SHA-256: `5ffef465eeeb5f2a3c23a24419d97c51afd7dbb4923ff42df9a3f58bba1ccfba` (verified).
- Runtime: Microsoft OpenJDK 21.0.7, macOS arm64.
- Gradle 8.14.3.
- Source isolation: `codex/phase1-plugin`, based on `fe5b534`.

## Step 1

`gradle wrapper test build --no-daemon`: **PASS**.
Two JUnit tests verify binary round-trip, UUID rejection, omission of transient mode,
capacity growth/caps, choice folding, and reset-to-baseline recomputation.
This is not evidence for any live acceptance test.

## Step 2 — PASS (live)

Paper 1.21.11 build 132, vanilla 1.21.11 client, player `inspiralc` in survival.
The listener attached before `packet_handler` at 11:34:30 server-log time.
Two observed runs each produced one swap control, five `DROP_ITEM`, and five
`DROP_ALL_ITEMS`: **20 drop packets total, all with mainhand=AIR and offhand=AIR**.
The first run was 11:34:41–11:34:49; the second was 11:35:18–11:35:22.
No Bukkit drop event was raised. The probe cancels drop/swap packets before vanilla
processing and never writes inventory. See `empty-hand-drop.log` for the excerpt.

**Answer: yes, the client sends both drop actions with an empty mainhand.**
This clears the prerequisite for steps 3–6; it is not acceptance test 4b, which
requires the eventual ultimate ability to execute.

Mojang's official client bytecode independently supports this result: the drop
method sends the action before inspecting the empty item return value.

## Implementation and automated validation

All six build stages are implemented in separate commits, followed by a preventive
inventory-close hardening commit. Production rewards are empty; the test kit and
all prototype parameters are config. The combined build starts on Paper 1.21.11.
Unit checks cover persistence, capacity/reset, pickup and click safety, close-time
returns, index uniqueness across the full world height, repeated reclamation,
50 duplicate input pairs, mode timeout, and reconstruction of unspent choices.
They are not substituted for the live acceptance suite.

## Provenance measurements — initial sample only

See `provenance-initial-live.csv` and `memory-layout.csv`.

- Observed real-client activity: **8 places, 16 breaks** during an approximately
  11-minute server session (including idle time). This was NOT a 30-minute ordinary
  building session and cannot establish building-speed performance.
- The first place/break cycle rose to **4,504 bytes**, then returned to **0**
  with **4,504 bytes reclaimed**. Seven later placements left a final/peak payload
  of **4,902 bytes in one chunk** at server shutdown. Reclamation does not shrink
  marks for blocks that remain placed.
- Place handlers: **0.315240 ms mean**, **0.573333 ms maximum**, eight samples.
  The first isolated placement took 0.277083 ms.
- Break handlers: **0.242622 ms mean**, **2.236042 ms maximum**, 16 samples.
  These include successful breaks of natural blocks as well as the placed block.
- Default overworld bitset maximum: **12,288 payload bytes per marked chunk**
  (`16 × 16 × 384 / 8`). This is a layout bound, not a benchmark result.
- Java 21 instrumentation measured a 4,504-byte payload array at **4,520 heap bytes**,
  plus **4,544 bytes** for a transient BitSet and its words during modification.
  At maximum height these are **12,304** and **12,328** bytes respectively.
  PDC tag/map/key bookkeeping, extra serialization copies, and chunk memory are
  additional; these figures must not be described as total per-chunk heap cost.
- Reclamation is demonstrated for direct place/break and 100 unit-test cycles.
  Global storage can still grow when players leave marked blocks in new chunks.
  Piston/explosion policy remains OPEN, and can leave stale marks; no stronger
  global bound or production go/no-go conclusion is claimed.

`tools/MemoryProbe.java` uses a premain Instrumentation agent and
`--add-opens java.base/java.util=ALL-UNNAMED` to measure arrays/BitSet objects on
Microsoft OpenJDK 21.0.7. No third-party heap-size estimator is used.

## Live acceptance status

**Debounce.** `duplicateUltInput`: ten rapid drop inputs inside a single mode
window produced exactly **one** ChannelUlt execution. Sequential 50-input runs
already passed; this closes the duplicate-input case.

**SinkholeLite reporting.** The log line now reports `playerPlacedExcluded`
alongside `newlyPlacedSpared`. The former counts player-placed blocks skipped
when the volume is collected and is the provenance demonstration; the latter
counts blocks that become player-placed during the staged collapse, which is
why every earlier cast logged zero while sparing was in fact working.

Evidence files: `protocol-live-results.jsonl` (90 records, run ending 2026-09-17T19:35:25Z),
`protocol-server-excerpt.log` (113 filtered server lines), and the reusable driver under
`protocol/`. The protocol fixture exercises real packet handling, Bukkit events and PDC
updates; per its README it cannot certify vanilla key generation, renderer appearance,
or the 30-minute ordinary-building requirement.

| Spec test | Status / evidence still required |
|---|---|
| 1 | Pending full real-client path matrix; cancellation unit tests pass. |
| 2 | Level commands observed live; immediate usability needs client confirmation. |
| 3 | **Live PASS (protocol).** `rewardGui`: reset to level 1, setlevel 3, 45-slot menu opened, click recorded `ChoiceRecord[level=2, choiceId=fixture_health]`, and the inventory readback contained no reward item. Fixture-only reward levels; production `rewards.levels` remains empty. |
| 4 | **Partial live PASS.** The earlier "abilities do not fire" report is superseded: 50 protocol M1 inputs raised Lunge 2 -> 52 (exactly +50) and 50 empty-hand ult inputs raised ChannelUlt 1 -> 51. SinkholeLite remains at 1 execution, so 50-inputs-per-ability is met for 2 of 3 abilities. Protocol fixture only; not a vanilla-client key-generation test. |
| 4b | **Live PASS (protocol).** Empty-hand drop packets already PASS; ChannelUlt now observed executing, counter 1 -> 51 -> 52. Vanilla-client key generation remains evidenced only by the earlier empty-hand Q log. |
| 5 | **Live PASS (protocol).** Mode timeout observed live in the same run; counters were unchanged across the timeout (lunge held at 52, sinkhole_lite at 1, channel_ult at 1), confirming silent clearing with no ability consumption. |
| 6 | **Live PASS (protocol).** `deathInMode`: died with mode=true, mode read false afterwards rather than sticking; level and choices unchanged; a following M1 still executed (lunge incremented). Quit-in-mode was already recorded. |
| 7 | Pending real-client attempts to move/drop/store/frame the map. |
| 8 | **Partial.** `mobTargeted` ran M1 and M2 against a summoned NoAI zombie and lunge incremented, but the Health readback did not return usable data, so damage is unconfirmed. Assertion needs fixing. |
| 9 | **Live PASS (protocol) for provenance sparing.** `sinkholeProvenance`: three player-placed blocks inside the blast volume, `playerPlacedExcluded=3`, all three asserted surviving, 25 natural blocks removed with three asserted individually as air. A real-client built structure is still untested; do not mark platform go/no-go. |
| 10 | Pending 30-minute ordinary-building trial. Initial small sample only. |

## Decisions and spec limitations

- User approved refusal of enrollment when the offhand is occupied; the player must
  empty it themselves. A fresh map is issued only into an empty offhand.
- User approved the `test` class assignments (Lunge/SinkholeLite/ChannelUlt) and
  explicitly labeled configurable prototype defaults. No branch trees are invented.
- Vanilla keep-inventory semantics preserve exact inventory slots on enrolled-player
  death, satisfying the stated inventory-untouched requirement without restoring items.
- Shift insertion, double-click collection, cursor-return hazards, and temporary
  crafting/trading menu returns require more prevention than §4's sketch shows.
  Partial capacity therefore disables unsafe operations/temporary menu inputs;
  there is no relocation fallback. Admin reduction/reset requires empty cursor and
  temporary slots. This is a usability limitation for review, not hidden item repair.
- InventoryMoveItemEvent exposes a destination inventory, not a destination slot.
  Partial-capacity player destinations are conservatively cancelled. Vanilla hoppers
  do not feed player inventories; the handler also covers custom transfer events.
- Commands or other plugins that write directly to inventories bypass these events;
  no cancellation-only implementation can enforce against arbitrary external writes.
- §2 comments refer to rewards/specialization in §7; their actual seam is §8.
- The vanilla offhand map rendering claim in §5/§11 is not a guarantee of a useful
  persistent minimap in every pose/view. Only the requested placeholder ships.
- Commands target online players; setlevel clears XP toward the next level. Choices
  remain historical on a level reduction; reset clears them. Unknown classes have
  no kit. Adding reward config applies retroactively to reached unspent levels.
- Pending choices are derived from level, choices and config. The GUI creates icons
  only in its own menu and cancels all transfer actions; it never gives item rewards.
- Structural protocol constants (36 storage slots, 20 displayed hunger, 16-wide
  chunks, 8 bits/byte, GUI rows) and serialization version numbers are not balance
  values. Progression, abilities, reward values, and sampling cadence are config.
- Provenance tracks successful BlockPlaceEvent/BlockMultiPlaceEvent and break events.
  Covering retains marks; piston/explosion interactions are measured without policy.
  Fluid/entity placement outside those events is not assigned a new policy.

The user explicitly requested pushing the in-progress implementation to main and
continuing with small checkpoints. Merge/push does not certify live acceptance.

## Ability dispatch regression — reproduced and fixed

The test server retained the first scaffold config, without `abilities.classes`.
Bukkit returned scalar defaults but `getKeys(false)` omitted default-only class
entries, yielding an empty ability registry: mode feedback worked and casts silently
resolved no ability. A regression using a legacy config reproduced zero executions
across 50 input pairs. Config loading now enables copying defaults before enumerating
registries and persists the merged config after validation, preserving explicit values.
This fixes the reproduced server-side cause; a fresh real-client retest is still needed.

The fix was pushed to main as `87469cb`. All 11 regression tests pass, including the
legacy-config reproduction now executing exactly 50 Lunges. The local server then
started cleanly, saved the merged config, and contained the test class assignments.
Fresh-launch computer control still exposed only Minecraft Launcher, so the corrected
abilities need a real-client retest before any additional acceptance row can pass.
Startup logs and `/moba debug` now explicitly show whether ability kits are registered.

## Protocol fixture follow-up

A separate loopback-only Paper fixture on port 25576 uses Mineflayer 4.39.0 and a
new disposable flat world. It leaves the authenticated player server on 25575
unchanged. The fixed registry executed empty-hand ChannelUlt exactly once and
reported channel completion, and executed Lunge from an arm-animation packet.
Initial live inventory checks retained all six filled L1 slots while cancelling
pickup overflow and attempted insertion into hotbar slot 6. These are protocol-client
tests, not assertions about vanilla key generation or rendered feedback.

This fixture also exposed peaceful hunger regeneration bypassing FoodLevelChangeEvent.
Capacity now enforces the hunger/saturation cap on a configurable tick cadence as
well as food-change events. No item state is inspected or modified by that task.
