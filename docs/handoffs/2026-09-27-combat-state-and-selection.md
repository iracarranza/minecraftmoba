# Handoff — combat state, in-world selection, and one unverified fix

**27 September 2026. For the local Claude instance, which can build.**
Follows [`2026-09-26-growth-task-slice.md`](2026-09-26-growth-task-slice.md) and
its [follow-up](2026-09-26-growth-task-followup.md).

Everything here is on main. `22da434..b0aa395` is the range.

Extended later the same day with the Sinkhole redesign and the Task tree
identities — design only, no new code, and the build order is unchanged.

---

## Do this first

```
cd implementation/plugin && ./gradlew test
```

**One code change since your 320-test run is unverified.** `5213671` edits
`MobaPlugin.applyAndSave` and adds `TaskProjectionTest`. The web session still
cannot reach `repo.papermc.io`, so the three-line `MobaPlugin` edit was never
compiled; the projection logic it calls was compiled and exercised standalone
against a stub of `TaskEffects.Domain`, and every assertion passed.

**What it fixes:** `projectTask` ran only inside `PlayerDataCodec.decode`, so a
Task allocation made during a session was persisted correctly and had **no
effect until the player reconnected** — `TaskEffects.tier()` kept returning the
pre-allocation value. Capacity was unaffected, because `sync` recomputes it from
`choices` directly. The fix projects in `applyAndSave` rather than in
`Rewards.click`, so allocation and reload share one path and any future writer
of `choices` is covered without knowing it has to be.

---

## New design, none of it implemented

Two records, written after your run. Read them before building anything in this
area; they change what the reward menu is for.

### `docs/design/COMBAT_STATE.md`

The shared definition five systems already assumed and none defined: level-up
selection, Recall, Health Mastery's out-of-combat Absorption, Shovel
Pathfinding, and Skeleton Crew's Deadline.

- **One state, one duration.** In combat for **7 seconds** [PROTOTYPE] after a
  qualifying action. Not a threshold per consumer — the player has to be able to
  see this, and five windows would be five invisible states. A consumer wanting
  more quiet adds its own delay *on top* of being out of combat.
- Store a **tick, not a flag with a scheduled reset**. Pathfinding polls it
  continuously.
- **Keyed by entity, not player.** Deadline needs a Crew Member's own state.
- **Sets it:** taking damage, dealing attack damage, activating a combat ability.
- **Does not:** moving, environmental damage, a summon's combat, passives, or the
  continuing effects of an action already taken.
- **Actions, not effects.** A 10-second poison is 7 seconds of combat for the
  caster. The victim *is* held, since each tick is damage taken — so a
  damage-over-time denies Recall, which is intended.
- **Abilities are authored combat or not**, at
  `abilities.definitions.<id>.branches.<branch>.combat`. **Require the key; do
  not default it** — the convention `ConfigKeysDefinedTest` exists to enforce.
  Declared per branch because Daredevil's Suplex changes Runway's answer, and
  because Assemble's three branches all reach combat by different routes.
- **Some abilities decide at activation.** Graveyard Shift either Strikes or
  Raises. [OPEN] the seam by which it reports what it did.

### `docs/design/IN_WORLD_SELECTION_AND_CHANNEL_CONDITIONS.md`

Supersedes the chest menu for spending a Task allocation or ability branch.

- Levelling requires **finding or making a space**. Mole stops tunnelling and
  digs a chamber; that is the mechanic, not a cost bolted to it.
- A **5-second recurring beat**, not a channel. **State at the end** is what
  counts, so spending the interval escaping and crouching on the final beat is
  correct play. No interruption, no cancel reason, less machinery than `Recall`.
- Conditions: crouching on solid ground, not moving, not in combat, aimed at
  eligible space. Two need a short trailing window or they mean nothing.
- Options go in **unobstructed space at the aim point**, not against a flat
  surface — a tunnel, cave or slope must work.
- **One allocation per summon.** This is what makes banking cost something.
- The bossbar is also the input: a **posture**, not a keypress. Raw key input via
  predicate is a proven dead end here and Swap Offhand is spent on abilities.
- In combat the bar shows the **combat** countdown instead, returning to the
  level-up countdown when combat ends. Sequential, never concurrent, so nothing
  holds paused progress.

---

## Build order

1. **Run the test suite.** Confirm `5213671`.
2. **Manual walkthrough of the current reward menu** — bossbar at Lv4,
   `/moba rewards`, tree recorded, tier readable *without relogging*. The menu is
   not the end state, but the projection fix needs confirming either way.
3. **Combat state.** Everything else waits on it. One class, entity-keyed,
   storing a tick. Add the per-branch `combat` key and fail at load without it.
4. **Recall's two changes**, both decided and neither implemented: add the
   not-in-combat condition, and remove `moveTolerance`.
   [TECHNICAL RISK] A literal `moveTolerance: 0.0` against
   `location.distance(origin)` will cancel on floating-point noise. "No movement"
   should mean **an unchanged block position**.
5. **In-world selection**, replacing the menu.
6. **The Growth packet** — still the gate on Skeleton Crew. `infrastructureProgression`
   is one effect per level and its Lv6 needs two. Do not solve it as a list of
   infrastructure strings.

---

## Added later on 27 September — design only

Two commits after this handoff was first written, `22960c8` and `b0aa395`. No
code. They change what Sinkhole is, and settle what the three Task trees mean.

### Sinkhole is redesigned, and is now implementable

`classes.md` → *Ultimate — Sinkhole*. It **opens and restores** instead of
scarring: target a jagged area including player construction, highlight the
affected blocks to everyone nearby, destroy after a delay, restore exactly after
a further period, No-Build in the opened volume throughout.

**It no longer needs block provenance.** Targeting everything and restoring it
exactly makes the natural-versus-authored distinction moot, so Sinkhole was
removed from §0 of the capability audit — **six systems down to five**. That
dependency was the single most load-bearing unsolved thing in the audit, and
this retires one consumer by design rather than by building it.

Five rules that are not tuning and must survive implementation:

- **Zero drops.** With restoration, any drops make it an infinite duplicator —
  and it can now target whatever the enemy built.
- **Falling blocks must not duplicate.** Remove support under gravel, restore the
  original, and both exist. Suppress falling in the volume or clear the fallen
  entities on restore.
- **No-Build is the restore invariant**, not a separate effect: nothing built
  inside can be destroyed by the restore. One mechanic, not two.
- **Being restored into suffocates.** The threat is the closing, not the opening.
- **Timing is the hardest highlighted block's mining time**, so tool tier and
  Efficiency scale it through vanilla maths with no bespoke rule. **Structural
  Integrity and Waxer's Sealed blocks feed the same hardness**, so fortification
  resists natively and neither system needs a clause written against Sinkhole.

Independent of combat state, so it can be built whenever — but it needs the
per-branch `combat` flag, which arrives with combat state. **`sinkhole_lite` in
config is not this.** It is a radius-3 input-plumbing stub labelled "not class
design or balance canon", and it is why the ultimate currently reads weak.

Still open: block budget, durations, targeting geometry, how the highlight is
drawn, snapshot persistence across chunk unload and restart, and liquids at the
boundary when a hole opens under water or lava.

### What the three Task trees mean

`CLASS_PROGRESSION_GROWTH_AND_TASK.md` → *What each tree means across the
roster*. **Slaying** improves personal combat, trivially. **Yield** is the tempo
tree — harvest XP shifts the power curve left, so breakpoints arrive sooner at
less power per point, peaking mid-match and flattening once the roster caps.

**Efficiency deliberately does not generalize**, and there is a
`[DO NOT SYSTEMATIZE]` marker saying so. Mole's Sinkhole scales with it only
because the ultimate is *implemented as mining*; Skeleton Crew wants Pathfinding
for its artery and nothing about its skeletons scales with Efficiency. An ability
scales with a tree only when it is genuinely implemented as that tree's activity.

### Two rules that look like contradictions and are not

**A technique affecting allied players now also affects their summons and
followed animals.** So crew on a shovel-built path move faster, and the Supply
Line observes a higher Item Rate — which *looks* like Task improving
infrastructure, which `classes.md` §15 forbids. The prohibition was rewritten
rather than the feature:

> Does the Task tier **mention** infrastructure, or does it change something in
> the world that infrastructure then **measures**? The first is forbidden; the
> second is the design.

**Crew movement speed is not an authored Growth axis**, which is why Skeleton
Crew gets Capacity and Reach. Speed divides trip time and multiplies Item Rate,
then multiplies against Capacity — most leverage, least granularity — and mob
speed degrades pathfinding before it gets interesting. Crew are also
dual-purpose, so faster crew are faster defenders. Generalized as:

> **Authored Growth should take the axes with wide, legible tuning ranges. Leave
> movement speed to the world.**

---

## Still true from the previous handoff

Mole in game has never been checked (Lv0 at 10 HP / 10 hunger / 6 slots; Lv30 at
32/20/24). The deliberate compromises listed there — `Capacity` hosting the
Growth clock, the fallback curve not being a roster default, capacity
specialization deprecated rather than deleted, absent IV–VII magnitudes, the
exhaustion cadence without values, ×100 as presentation only — all still stand
and should not be tidied away.

---

## Done on the local instance, 27 September 2026

The build order above is complete through item 6. Both suites green: 372 plugin
tests (from 324) and 591 worldgen.

**1. The unverified commit is verified.** `repo.papermc.io` is reachable from
the local instance, so `5213671` compiles and `TaskProjectionTest` passes. A
Task allocation now takes effect without relogging.

**2. Combat state exists.** `CombatState` — a stored tick, keyed by entity,
7 seconds [PROTOTYPE], swept from the plugin's timer. `AbilityCombat` requires
a per-branch `combat` declaration and fails at load naming every definition and
branch that did not declare. The four existing abilities are classified;
`sinkhole_lite` is combat and `tunneling` is not, which is the pair the design
names as same-medium-opposite-answers.

**3. Recall has both decided changes.** Gated on combat state in addition to the
damage cancel, and `moveTolerance` is gone — replaced by an unchanged **block
position**, not a literal `0.0` against a distance, which the handoff correctly
flagged would cancel on float noise. A test asserts the key stays absent.

**4. In-world selection is built and SHIPPED OFF** (`selection.enabled: false`).
The beat, the four conditions with trailing windows on two of them, the bossbar
clock swap, one-choice-per-summon, and Text Display + Interaction options placed
in unobstructed space. The arithmetic is in `SelectionBeat` and
`SelectionLayout` and is tested without a server; the entity work is not,
following `LobbyHall`'s split. It is off because the beat period and the
condition set are exactly what wants playing, and enabling it would make a
design experiment the default experience. The menu still works.

**5. The Growth packet exists**, and `CLASS_PROGRESSION_GROWTH_AND_TASK.md` is
updated where it recorded the blocker. `skeleton_crew` is in config with both
two-effect levels, as proof the data model holds the class. Its effect names and
tiers are [WORKING] placeholders and balance nothing.

### Not done, and why

**Sinkhole.** Buildable now in principle, but block budget, durations,
targeting geometry, how the highlight is drawn, snapshot persistence across
chunk unload and restart, and liquids at the boundary are all still open. Five
rules that must survive implementation are recorded above; none of the numbers
are. Building it would mean inventing them.

**The manual walkthrough (item 2).** Needs a running server and a player.

**`codex/lightfooted-from-phase1` is PORTED, not merged** (see below for what
was carried and what was left). The branch itself stays unmerged.

**`codex/lightfooted-from-phase1` is deliberately NOT merged.** Five commits,
and merging them regresses main. It is a parallel implementation built on a
20 September base that reads its branch from a single global config string
(`abilities.lightfooted.branch`) rather than per player from
`PlayerData.classState`, which is incompatible with the draft and Task ledger
main has since built. Taking its side of the conflicts would delete channel
cancellation (`isAbilityActive`/`cancelAbilities`), the draft's `ids()` and
`definition()`, the per-branch HUD suffix, and per-player branch selection.

What is genuinely unique on it and worth porting rather than merging:
`LightfootedMechanics` is the **only implementation of a passive hook that
exists anywhere** — `passiveHook` is declared on every class and dispatched by
nothing. Its Animal Senses (wolf damage reduction, cat fall reduction, fox
speed), its Bounding second ability, and its lunge sweep with per-target hit
dedup have no equivalent on main. Port them onto main's ClassDefinition-driven
architecture; do not merge the branch.

**Two stale doc branches** are also unmerged and probably should stay that way.
`origin/docs/canonical-worksites-infrastructure` and
`origin/docs/resource-opportunity-significance` fork from 14 and 12 September
and are 320 and 329 commits behind. Their `classes.md` deltas are pre-Sinkhole,
pre-Growth-packet text that would fight every canonical doc. One file on them is
genuinely absent from main —
`docs/reconciliation/2026-09-13-extraction-opportunities.md`, 38 lines — and
could be cherry-picked on its own if wanted.

### Next

Sinkhole's open numbers, the Lightfooted port, and playing the selection beat
to decide whether 5 seconds and the four conditions are right.


---

## Addendum — the port and the cherry-pick, same day

**The Lightfooted port is done.** `Passives` dispatches `passiveHook`, which
until now was declared on every class and read by nothing. `AnimalSenses`
carries the wolf/cat/fox benefits with a **capped** species count — the source
floored `1 - wolves * 0.02` at zero, so fifty wolves was literal damage
immunity, and animals are breedable and deliberately stocked by the compiler.
`BoundingAbility` replaces `sinkhole_lite` as Lightfooted's a2. Lunge now
sweeps along the leap with per-target dedup instead of ray-tracing once at cast
time before the player had moved.

Not carried: the branch's global `abilities.lightfooted.branch` string, its
teleport-driven lunge path, and `AbilityCooldowns`. The first contradicts
per-player branch selection; the second fights knockback and blocks; the third
duplicates the cooldown map `AbilityInputs` already holds, and porting it would
have meant rewriting the HUD line for no mechanical gain. The branch's
cooldown-remaining HUD readout ("READY / 1.2s" rather than grey/white) is the
one thing there still worth taking, and is not taken.

**`docs/reconciliation/2026-09-13-extraction-opportunities.md` is cherry-picked**
from `origin/docs/resource-opportunity-significance`. Both doc branches remain
unmerged and should stay that way.

**The HUD question, answered.** The continuous health/hunger bar exists as
**two** commits, not one, and neither is a fixed-size proportional fill:

- `d7cc16f` repaints the ten native heart/drumstick sprites in the **resource
  pack** (`bar_segment()` in `implementation/resourcepack/build_pack.py`) so
  they tile into one bar in the HUD's own position. The client still owns the
  fill, and its quantum is the half-sprite — `registry.json` still defines
  `half_left` and `half_right`, so half-steps are still exposed, just drawn as
  a half-filled rectangle rather than half a heart.
- `52f44b4` (`VitalsScaling`, enabled) makes the bar **always twenty points for
  every player**, with Capacity deciding what a point is worth. That solves the
  fixed-size half of the problem, by a different route than rendering — and it
  makes `d7cc16f`'s "this CANNOT fix the bar's length" note stale.

So: fixed size yes, continuous appearance yes, **proportional fill no** — still
twenty discrete steps. True smooth fill means taking the row away from the
client, which is what the retired `HungerDisplay` bossbar-glyph readout did, at
the cost of a placeholder pipeline, a permanent boss bar and an evening of
"hunger is fully invisible". It is `enabled: false`, kept rather than deleted.

**Next up with the user: Sinkhole's open numbers.**


---

## The vitals bar is built — 27 September 2026

A fixed-size health and hunger bar with a **proportional** internal fill, which
is what was actually asked for and what neither existing commit delivered.

**The bar is one unit glyph, repeated.** A bitmap glyph cannot be stretched, so
a bar that grows horizontally is a bar built from a repeated unit and its
length is the repeat count. It is always 64 units wide and only how many are
**lit** changes — so the fixed size is structural, not a property that separate
images have to agree about. Four glyphs (`vitals_cap_left`, `vitals_unit_on`,
`vitals_unit_off`, `vitals_cap_right`).

The first attempt enumerated one whole-bar image per fill level — 130 textures
for two bars. Repetition replaced it, and was the better call for a reason
beyond elegance: the width invariant became a loop in a test instead of an
inspection of 130 images.

**The advance correction is the one assumption.** Minecraft advances a bitmap
glyph by its bounding box plus one pixel of spacing, so each one-pixel unit
advances two and is followed by `U+F001` (-1) for a net of exactly one. If that
is wrong the bar collapses to a sliver rather than drifting — loud, not subtle.

**A latent defect surfaced and is now guarded.** Every other glyph takes its
codepoint from its position in `registry.json` while the plugin hard-codes the
resulting Java literal, so inserting one glyph shifts every later one with
nothing to say so. The bar units declare an explicit base at `U+E100`; a pack
test asserts the bar block starts above every sequential glyph, and a plugin
test pins the base against the registry. **The underlying fragility is not
fixed** — `HungerDisplay`'s four literals are still position-dependent.

**Both switches ship off, in that order.** `features.vitalsBar.enabled: false`
and `hideNativeRows: false`. Without the pack the codepoints are tofu, and the
previous glyph readout shipped with the vanilla row hidden and the replacement
illegible — "hunger is fully invisible". Turn the bar on, confirm it draws,
then hide what it duplicates; `build_pack.py --hide-native-rows` is the second
half and needs a pack rebuild and republish, not a live toggle.

**Not verified against a client.** Verified by decoding the generated PNGs and
composing the bar from real glyph pixels. The things a client would settle:
whether the advance correction is right, whether 64px is a sensible width on a
bossbar line, and whether the colours read.

### Also still open here

The action bar has **26 writers across 13 files** with no arbiter, so ability
casts, Recall ticks and selection-beat reasons already overwrite each other
today. That is why the bar went on a bossbar. Worth fixing on its own merits.
