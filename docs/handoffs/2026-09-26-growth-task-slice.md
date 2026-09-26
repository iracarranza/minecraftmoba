# Handoff — Growth clock, per-class curves, Task ledger

**26 September 2026. For the local Claude instance, which can build.**
Merged to main. Implementation report:
[`docs/audit/2026-09-26-growth-task-slice.md`](../audit/2026-09-26-growth-task-slice.md).
Design record:
[`docs/design/CLASS_PROGRESSION_GROWTH_AND_TASK.md`](../design/CLASS_PROGRESSION_GROWTH_AND_TASK.md).

---

## Do this first

```
cd implementation/plugin && ./gradlew test
```

**The build has never been run against these changes.** The web session that
wrote them could not reach `repo.papermc.io` — the environment's network policy
returns 403 on CONNECT — so `paper-api` would not resolve and Gradle never
compiled the plugin. This is the single reason this handoff exists.

If the build is red, fix it before doing anything else in this handoff. The
failure is far more likely to be in the five Bukkit-coupled files listed below
than in the four pure ones, which were compiled and tested directly.

---

## What is already proven

Verified by compiling the four Bukkit-free classes standalone and running the
repository's own test sources under a JUnit console runner from Maven Central.
**23 tests pass:** 21 new, plus the 2 existing `PlayerDataTest` tests still
passing against the rewritten `Capacity`.

| Test | Covers |
| --- | --- |
| `GrowthCurveTest` | Mole's Lv0–30 curve value for value; Lv0 completes no Growth; nothing changes between Growth levels; the Hunger plateau comes from the cap with no Lv15 special case; stepped curves hold flat; unknown/null profile falls back |
| `TaskLedgerTest` | Seven allocations at 4/8/12/16/20/24/28; one per opportunity; repetition advances a tree; 7/0/0, 4/3/0, 3/2/2; budget exhaustion; save round-trip; **exhaustively, no distribution of seven reaches Tier IV twice** |
| `LevelZeroTest` | Fresh players enrol at Lv0; Lv0 survives the save format; pre-Lv0 saves still decode; negative levels rejected |

`Capacity`, `TaskLedger`, `PlayerData` and `PlayerDataCodec` compile with zero
errors.

## What is NOT proven

`Settings`, `RewardCatalog`, `Rewards`, `AbilityInputs`, `MobaPlugin`. They
parse, and every compiler error against them was a missing-`org.bukkit` /
`io.papermc` dependency error rather than syntax or logic — but that is not a
build. Nothing was ever loaded by a server, so **`Settings.load` has never
parsed the new `config.yml` for real.** That is the highest-risk surface: the
stepped-curve and profile parsing is new code reading new config shapes.

---

## Where everything is

**New files**

- `implementation/plugin/src/main/java/com/minecraftmoba/plugin/TaskLedger.java`
- `implementation/plugin/src/test/java/com/minecraftmoba/plugin/{GrowthCurveTest,TaskLedgerTest,LevelZeroTest}.java`
- `docs/audit/2026-09-26-growth-task-slice.md` — the full report
- `docs/design/CLASS_PROGRESSION_GROWTH_AND_TASK.md` — design canon

**Changed Java**

| File | Change |
| --- | --- |
| `Capacity.java` | `Curve` gains optional absolute `steps`; `Settings` holds a fallback profile plus a per-class profile map; `recompute` takes an optional profile id. Old 5-arg constructor and 3-arg `recompute` preserved |
| `PlayerData.java` | `level` starts at **0** |
| `PlayerDataCodec.java` | accepts level ≥ 0 |
| `RewardCatalog.java` | `Option.bonus` no longer mandatory; new nullable `task`; the two are mutually exclusive; new `taskLevels()` |
| `Rewards.java` | records the tree for a Task option, the option id otherwise |
| `Settings.java` | growth levels parsed before curves; `steps` parsing and validation; `capacityProfiles` map; constructs a `TaskLedger`; record gains a 5th component |
| `AbilityInputs.java` | new `definition(String id)` accessor |
| `MobaPlugin.java` | `capacity()` selects the profile via `statGrowthProfile` |

**Changed config** — `implementation/plugin/src/main/resources/config.yml`:
`capacity.growthLevels`, new `capacityProfiles.mole`, `rewards.levels` populated
with Task options, Mole's `infrastructureProgression` retimed, an
`exhaustionEfficiency` seam that nothing reads.

---

## What needs doing, in order

**1. Run the build. Fix what it finds.** Nothing below matters until this is green.

**2. Load `config.yml` on a real server.** `Settings.load` has never parsed it.
Check specifically that `capacityProfiles.mole.slots.steps` validates at length
11 against 10 growth levels, and that supplying `start` alongside `steps` is
rejected.

**3. Verify Mole in game.** A Lv0 player should have 10 engine HP (5 hearts,
displayed 1,000), 10 hunger, 6 slots. At Lv30: 32 HP, 20 hunger, 24 slots. The
whole curve is in the design record §9.

**4. Project the ledger into `TaskEffects` on load.** `PlayerData.task` is
**not** in the codec, so `TaskEffects` tiers die on restart today. `TaskLedger`
derives tiers from `choices`, which *is* persisted. Recompute `d.task` from
`d.choices` when player data is decoded and the two representations agree again.
This is the most valuable follow-up and it is small.

**5. Check the Task menu end to end.** Hit Lv4, confirm the "Unspent choices"
bossbar appears, open `/moba rewards`, pick a tree, confirm a `ChoiceRecord`
carrying the *tree name* is written and that `TaskLedger.tier` reads it back.

**6. Widen the Growth packet.** See the blocker below.

---

## The blocker for Skeleton Crew

`ClassDefinition.infrastructureProgression` is `Map<level, effect>` — one effect
per level, one string per key, parsed by `AbilityInputs.stringMap`.

**Skeleton Crew cannot be configured at all.** Its Lv6 is Supply Line access
*and* Night Efficiency I; its Lv18 is Night Efficiency II plus crew combat
development. A YAML map cannot hold two values at one key, so there is no
config-level workaround.

**Do not fix this as `Map<level, List<String>>` of infrastructure strings.** A
Growth packet combines infrastructure with class methodology and personal
attributes, so a list of infrastructure effects rebuilds the
Growth-is-Infrastructure confusion one level down. The next step is a real
**class Growth packet** with infrastructure as one effect family inside it.

---

## Do not "fix" these — they are deliberate

- **`Capacity` still hosts the Growth clock** at `capacity.growthLevels`. Growth
  is not a capacity system; capacity is one recipient of a Growth packet. The
  placement is an existing seam kept to keep the slice small, documented in the
  class. Moving it is welcome; treating Capacity as the owner of Growth is not.
- **The `capacity.health/hunger/slots` block is a fallback**, not a roster
  default. Classes share Growth *levels*, never Growth *contents*. Do not
  generalize Mole's numbers to anyone.
- **Capacity specialization is deprecated in place, not deleted.**
  `Capacity.recompute` still reads choice bonuses so existing saves do not change
  meaning. Nothing in config registers one.
- **Task effects for Tiers IV–VII are absent on purpose.** The fractional
  backbone magnitudes and technique Strengths are uncalibrated. Build the seam,
  not the numbers.
- **Exhaustion Efficiency has a cadence and no values.** Lv6/12/21/27 is decided;
  the old 6/12/18/24 percentages were illustrative prototypes, not calibration.
- **The ×100 scale is presentation only.** All simulation and config stay in
  native Minecraft HP. Never move a balance endpoint to make a displayed number
  integral — Skeleton Crew stays at 27.5 HP / 2,750 displayed.

## Known vocabulary mismatch

`TaskEffects.Domain` is `EFFICIENCY, YIELD, DAMAGE`; the design and `TaskLedger`
say **Slaying**. They currently meet at a translation. Renaming the enum changes
persisted keys in `PlayerData.task`, so it needs a migration rather than a
rename — worth doing alongside item 4 above, since that code is already touching
both representations.
