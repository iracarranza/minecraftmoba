# Handoff — combat state, in-world selection, and one unverified fix

**27 September 2026. For the local Claude instance, which can build.**
Follows [`2026-09-26-growth-task-slice.md`](2026-09-26-growth-task-slice.md) and
its [follow-up](2026-09-26-growth-task-followup.md).

Everything here is on main. `22da434..3258610` is the range.

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

## Still true from the previous handoff

Mole in game has never been checked (Lv0 at 10 HP / 10 hunger / 6 slots; Lv30 at
32/20/24). The deliberate compromises listed there — `Capacity` hosting the
Growth clock, the fallback curve not being a roster default, capacity
specialization deprecated rather than deleted, absent IV–VII magnitudes, the
exhaustion cadence without values, ×100 as presentation only — all still stand
and should not be tidied away.
