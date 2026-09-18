# Phase 1 implementation and validation — 2026-09-18

All six implementation stages are built. **14 automated tests pass**, and the
live server protocol checks below pass. This is not an unqualified claim that
all requested human-client/ordinary-play acceptance is complete: the 30-minute
measurement was a controlled protocol-client building workload, and rendered
client feedback has not been retested through computer control.

## Environment and artifacts

- Paper **1.21.11**, official build **132**, commit `c5eb079`; available and tested.
- Paper SHA-256: `5ffef465eeeb5f2a3c23a24419d97c51afd7dbb4923ff42df9a3f58bba1ccfba`.
- Microsoft OpenJDK **21.0.7**, macOS arm64; Gradle **8.14.3**.
- Main jar: `implementation/plugin/build/libs/minecraft-moba-0.1.0-SNAPSHOT.jar`.
- Build: `implementation/plugin/gradlew -p implementation/plugin clean test build`.
- Production rewards remain **empty**. GUI examples and event harness are test-only.
- Live protocol clients: pinned Mineflayer **4.39.0**, disposable loopback worlds.
- Claude's three pushed commits were preserved and incorporated on `codex/phase1-plugin`.
  The user authorized small checkpoints pushed to main; pushing is not acceptance certification.

## Step 2: empty-mainhand drop — confirmed on the vanilla client

**Yes.** Vanilla Minecraft 1.21.11, player `inspiralc`, sent both `DROP_ITEM` and
`DROP_ALL_ITEMS` with an empty mainhand. Two runs captured **20 drop packets**,
all mainhand/offhand AIR, without a Bukkit drop event. This was verified before
building the dependent systems. Evidence: `empty-hand-drop.log`.

## Live acceptance matrix

“Protocol” means real Paper packet processing, Bukkit events, and server-side
inventory/world/PDC assertions. It does not establish vanilla rendering or mouse/key
mapping beyond the separately observed empty-hand drop gate.

| Test | Evidence / status |
|---|---|
| 1: slot locking | **PASS, protocol + event harness.** 23 inventory cases include locked hotbar/main insertion, drag, chest shift/hotkey/offhand transfer, full-capacity cursor returns, crafting returns, locked-stack double-click collection, overflow pickup, armor returns, creative insertion, and map operations. Server snapshots matched and no unexpected ground items appeared. Real InventoryMoveItemEvent dispatch cancels partial player destinations and map transfers; full capacity with room remains usable. Vanilla hoppers cannot feed a player inventory directly. |
| 2: unlock immediately | **PASS, protocol.** Waiting dirt was rejected at six full slots, then entered slot 6 immediately at L2 without relog. |
| 3: reset choices | **PASS, protocol.** Two GUI choices changed capacity to 11/11/9; reset cleared choices and restored exactly 9/9/6 with unchanged storage. |
| 4: 50 each | **PASS, protocol.** Final zero-gap F→input run produced exactly 50 Lunges, 50 Sinkholes, and 50 ultimates. Earlier spaced-input runs also passed. |
| 4b: empty-hand Q | **PASS.** Vanilla empty-hand packet generation was observed; the final protocol run selected an empty slot and sent Q plus Ctrl-Q per entry, resolving exactly 50 ultimates, not 100. |
| 5: timeout | **PASS, protocol.** Mode cleared after timeout with unchanged execution counters. Timeout exit code is silent and touches no items. |
| 6: death/logout | **PASS, protocol.** Death cleared active mode and preserved storage/map. A clean quit-in-mode/reconnect preserved exact storage and tagged offhand map with mode false. A full server restart also preserved level, XP, class and choices. |
| 7: map locking | **PASS, protocol.** Left/right/shift/number-key/drop/stack-drop/creative-clone/deletion/offhand-hotkey attempts were blocked. Interact and interact-at could not insert the map into item frames or armor stands. F entry/exit preserved the same map. |
| 8: entity targets | **PASS, protocol.** Actual entity attack triggered one Lunge and left husk health at 20.0. Entity right-click triggered Sinkhole while a white sheep stayed white and all three red dye remained. |
| 9: provenance sparing | **PASS, protocol.** A seven-block, three-high arch survived while supporting terrain collapsed. After a clean server restart, all seven survived another collapse with placeCount=0, proving persisted marks were used. |
| 10: 30-minute measurement | **Controlled live trial complete.** 585 placements and 585 breaks over 1,803.823 seconds; results below. This repetitive paced workload is not a human ordinary-play session. The user's requested ordinary-play interpretation remains an explicit acceptance caveat. |

Evidence: `inventory-matrix-final-2026-09-18.jsonl`, `map-entities-2026-09-18.jsonl`,
`rapid-input-2026-09-18.jsonl`, `server-followup-2026-09-18.jsonl`,
`server-assertions-2026-09-18.log`, and earlier checked-in protocol/death logs.

## Provenance measurements

Controlled survival construction and teardown of a 13-block wall, paced with
normal placement/dig packets. **45 complete cycles**, 585 placements, 585 breaks,
**30 minutes 3.823 seconds**. Operator commands supplied materials and a foundation;
they did not create placement marks. No other player placed/broke blocks on that
measurement server during the run. Other validation ran on a separate server.

| Measurement | Result |
|---|---:|
| Marked chunk payload at a completed wall | **4,148 bytes** |
| Test chunk after each complete teardown | **0 payload bytes; key removed** |
| Net payload growth across the session | **0 bytes** |
| Cumulative payload reclaimed | **186,660 bytes** |
| Place handler mean, baseline-subtracted | **0.176570 ms** |
| Break handler mean | **0.212108 ms** |
| Break handler maximum | **2.235583 ms** |
| Place handler maximum | **≤4.340583 ms**, a cumulative upper bound including earlier fixture activity |
| Sampled Paper rolling mean tick time | median **1.469322 ms**, mean **1.543279 ms**, max **5.751500 ms** |

The preexisting unrelated chunk held 185 bytes throughout: whole observed payload
alternated between 185 and 4,333 bytes and ended at 185. Place averages subtract
three preexisting samples; the cumulative maximum cannot isolate the trial's own
maximum. The 273 tick samples are rolling averages, **not per-tick percentiles**.
These are measurements on a developer machine, not an isolated production load test.
The timed mark/unmark implementation was unchanged by the later pre-handler
canBuild check or the unrelated input-queue fix.

Raw results: `building-session-2026-09-18.jsonl`, `provenance-controlled-30min.csv`.
The earlier human-client sample remains in `provenance-initial-live.csv`: eight
places, sixteen breaks over approximately eleven minutes including idle time.

### Heap footprint, beyond serialized payload

Java Instrumentation measured the exact Paper `DirtyCraftPersistentDataContainer`
used by chunks and its reachable raw-map graph (table/nodes/key/tag/byte array).
It excludes the shared type registry, enclosing Minecraft chunk, the plugin's
separate observation ledger, and transient event allocations.

| Payload | PDC + raw-map graph |
|---:|---:|
| 185 bytes | 488 bytes |
| 4,148 bytes (trial peak/chunk) | **4,448 bytes** |
| 4,504 bytes | 4,800 bytes |
| 4,902 bytes | 5,200 bytes |
| 12,288 bytes (384-height layout maximum) | **12,584 bytes** |

An empty fresh container/map graph was **80 bytes**; after removing its only entry,
**160 bytes** remained because HashMap retains its allocated table. Payload/tag/key
objects were no longer reachable from it. Therefore “zero payload” is not “zero
heap.” BitSet and modification-array allocations are transient; the earlier
`memory-layout.csv` measures those separately. Reproduce the Paper measurement
with `tools/paper_memory_probe.py`; see `tools/README.md`.

**Conclusion:** direct place/break reclamation bounds this repeated working set,
and costs were small at the measured building rate. Storage still grows with
retained builds in additional chunks. Piston/explosion policy remains OPEN and
can leave stale marks; no global production bound or unconditional platform go/no-go
is claimed from this controlled workload.

## Bugs found and fixed

- Legacy scaffold configs lacked the default-only class registry: mode feedback
  worked while casts did nothing. Missing defaults now merge before registry
  enumeration and are persisted without overwriting explicit configuration.
- Peaceful regeneration bypassed FoodLevelChangeEvent. Configurable tick enforcement
  now caps hunger/saturation as well; actual foodLevel 9 was observed at L1.
- Bukkit scheduling of F raced vanilla click processing: **0/50 zero-gap M1 casts**.
  Swap/drop now use Paper 1.21.11's native PacketProcessor queue, preserving order
  with clicks/movement. Unconsumed drops execute once in place through vanilla.
  Final results were 50/50 for all three abilities. No item movement was added.
- Provenance now ignores canBuild=false placements, even when not cancelled.
  Unit and live event-dispatch regressions pass.

The rapid-input archive also preserves failed intermediate fixture runs. A block
interaction target was at the survival reach edge, the reused platform collapsed,
and forced client aiming had not necessarily been sent. The final run uses a
fresh verified platform, targets within reach, and awaits aiming before sending
zero-gap F/click. An initial transfer harness attempted nested level commands;
those were deferred, so the corrected harness changes level before dispatching
each test event. Failed fixture runs are not counted as passes.

## Decisions, limitations, and open seams

- User-approved: refuse enrollment until a conflicting offhand is emptied by the
  player; only create a fresh map into an empty offhand.
- User-approved: one `test` kit and labeled configurable prototype ability numbers.
- No built-in rewards or branch trees; examples are separate test configuration.
- Capacity is recomputed from level/choices/config. Choices remain historical when
  lowering level; reset clears them. Setlevel clears XP; commands target online players.
- Pending choices are derived and reconstructed on reconnect; adding a reward
  configuration applies retroactively to reached, unspent levels.
- Vanilla keep-inventory semantics preserve exact slots on enrolled-player death.
- Partial capacity conservatively disables unsafe shift insertion, collection,
  cursor swaps, and temporary crafting/trading inputs because close-time returns
  bypass cancellable transfer events. Reduction/reset requires an empty cursor and
  temporary menu. There is no repair, stash, relocation, or drop fallback.
- Direct inventory writes by commands/other plugins bypass cancellable events;
  enforcing against arbitrary external writes would conflict with the prevention-only rule.
- The observation ledger covers loaded/touched chunks, not all unvisited disk data.
- Piston/explosion events are counted without transfer/reclamation policy. Covering
  retains marks; other fluid/entity placement paths remain undesigned.
- §2's references to reward/specialization §7 actually point to the seam in §8.
- The vanilla map's visibility depends on pose/view; Phase 1 ships only the requested stub.
- Protocol/layout constants (36 slots, chunk width, bits/byte, GUI rows, serialization
  version) are structural; progression, ability, reward, and sampling values are config.
- Computer control exposes the launcher but not the Java game window. No new visual
  client acceptance is claimed. The outstanding ordinary-play distinction above
  must be resolved before declaring all eleven acceptance rows unconditionally done.
