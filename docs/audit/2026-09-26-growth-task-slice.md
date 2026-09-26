# Implementation slice — Growth clock, per-class curves, Task ledger

Date: 2026-09-26. Design record:
[`CLASS_PROGRESSION_GROWTH_AND_TASK.md`](../design/CLASS_PROGRESSION_GROWTH_AND_TASK.md).

## 1. Behaviour now proven by tests

Three new test classes, 21 tests, all passing. The two existing tests in
`PlayerDataTest` also still pass against the rewritten `Capacity`.

**`GrowthCurveTest`** — Mole's authored Lv0–30 curve reproduced value for value:
Health 10 → 32 engine HP at +2.2 per Growth, Hunger 10 → 20 by Lv15, Inventory
stepped 6 → 24. Also: Lv0 completes no Growth and reads the start of the curve;
nothing changes between Growth levels; the Hunger plateau is produced by the cap
alone with no Lv15 special case; a stepped curve holds flat across a Growth;
an unknown or null profile falls back rather than throwing; a stepped curve
reads its last value beyond its end.

**`TaskLedgerTest`** — seven allocations at 4/8/12/16/20/24/28; allocation
offered only at Task levels, only at or below the player's level, only for a
real tree; one allocation per opportunity; repeated allocation advances the same
tree; 7/0/0, 4/3/0 and 3/2/2 distributions; the budget exhausted after seven;
tiers recomputed correctly from a save round-trip. And exhaustively across all
36 distributions of seven allocations, **no distribution reaches Tier IV twice** —
the budget enforces the commitment threshold with no exclusivity rule encoded.

**`LevelZeroTest`** — fresh players enrol at Lv0; Lv0 survives the save format;
saves written before Lv0 existed still decode; negative levels are still
rejected.

### What could not be verified

`repo.papermc.io` is blocked by this environment's network policy (403 on
CONNECT), so `paper-api` cannot resolve and **the Gradle build was never run**.
Verification was done by compiling the four Bukkit-free classes standalone and
running the real test sources under a JUnit console runner fetched from Maven
Central.

`Capacity`, `TaskLedger`, `PlayerData` and `PlayerDataCodec` compile with zero
errors. The Bukkit-coupled edits — `Settings`, `RewardCatalog`, `Rewards`,
`AbilityInputs`, `MobaPlugin` — parse, and every compiler error against them is
a missing-`org.bukkit`/`io.papermc` dependency error rather than a syntax or
logic error. That is weaker than a build. **Run `./gradlew test` before
merging.**

## 2. Configuration now expressing current design

- `capacity.growthLevels` is the shared Growth clock: `[3, 6, 9, 12, 15, 18, 21,
  24, 27, 30]`.
- `capacityProfiles.mole` holds Mole's authored curves — health
  `{start: 10, growthUnit: 2.2, cap: 32}`, hunger `{start: 10, growthUnit: 2,
  cap: 20}`, slots `{steps: [6,9,9,12,15,15,18,18,21,21,24], cap: 36}`.
- `capacity.health/hunger/slots` is now the **fallback** curve for a class with
  no authored profile, commented as a bootstrap and explicitly not a
  roster-wide default.
- `rewards.levels` carries the Task allocations at 4/8/12/16/20/24/28, three
  options each, each declaring `task:` and **no** `capacity:`.
- Mole's `infrastructureProgression` is retimed to 18/21/24/27/30.
- `capacityProfiles.mole.exhaustionEfficiency` records the Lv6/12/21/27 cadence
  with an empty `values` list. No code reads it; the magnitudes are unmeasured.

## 3. Architecture seams and stubs

- **`Capacity.Curve` gained optional `steps`.** Absolute values at Lv0 followed
  by one per Growth event, authoritative when present, so `start` is not also
  required and there is one representation of the Lv0 value. `Settings` rejects
  a steps list whose length is not `growthLevels + 1`, that decreases, or that
  is supplied alongside `start`/`growthUnit`.
- **`Capacity.Settings` holds a fallback profile plus a profile map**, selected
  by `ClassDefinition.statGrowthProfile` through a new
  `AbilityInputs.definition(id)`. The old five-argument constructor and
  three-argument `recompute` still work, so existing callers and tests are
  untouched.
- **`RewardCatalog.Option` no longer requires `Capacity.Bonus`.** Both `bonus`
  and the new `task` are nullable and mutually exclusive. `taskLevels()` derives
  the Task clock from the catalogue itself, so the ledger cannot drift from
  configuration.
- **`TaskLedger` is new and pure**, over `PlayerData.ChoiceRecord`. Tiers are
  counted from records that `PlayerDataCodec` already persists, so allocations
  survive a reload with no save-format change.
- **`Rewards` records the tree** for a Task option and the option id otherwise.
- Task effects for Tiers IV–VII are **absent, deliberately**. The magnitudes are
  uncalibrated; the ledger exists without them.

## 4. Where the code forced a semantic compromise

**`Capacity` still owns the Growth clock.** `capacity.growthLevels` is where the
seam already was, and moving it would have enlarged the slice. The design says a
Growth event is a class-authored packet that may touch capacity, infrastructure,
methodology or several at once, and capacity is only one recipient. The class
doc now says so explicitly, and no new API was added that deepens the
association — but the config path still reads as though Capacity owns Growth,
and that is a mismatch to clean up rather than a decision.

**Capacity specialization is deprecated in place, not removed.**
`Capacity.recompute` still reads choice bonuses so an existing save does not
change meaning underfoot. Nothing in current configuration registers one.

**`TaskEffects.Domain` is `EFFICIENCY, YIELD, DAMAGE`**, where the design says
Slaying. `TaskLedger` uses `slaying`, so the two vocabularies meet at a
translation rather than agreeing. Renaming the enum would change persisted keys;
it was left alone.

**`TaskEffects` tiers live in `PlayerData.task`, which the codec does not
persist.** The ledger sidesteps this by deriving tiers from `choices`, which is
persisted, but the two representations now coexist and only one survives a
restart. Projecting the ledger into `d.task` on load is the obvious follow-up.

## 5. Known blocker for the next class

`ClassDefinition.infrastructureProgression` is `Map<level, effect>` — one effect
per level, one string per key. **Skeleton Crew cannot be configured at all**: its
Lv6 is Supply Line access *and* Night Efficiency I, and its Lv18 is Night
Efficiency II plus crew combat development. A YAML map cannot hold two values at
one key, so this is not workable around in configuration.

Do not fix it as `Map<level, List<String>>` of infrastructure strings. A Growth
packet combines infrastructure with class methodology and personal attributes, so
a list of infrastructure effects would rebuild the Growth-is-Infrastructure
confusion one level down. The next step is a real **class Growth packet**, with
infrastructure as one effect family inside it.
