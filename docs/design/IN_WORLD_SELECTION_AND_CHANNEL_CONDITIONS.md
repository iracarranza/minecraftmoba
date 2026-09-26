# In-World Selection, and the Conditions a Channel Requires

**Status:** Working design direction
**Date:** 2026-09-27
**Scope:** How a player spends a Task allocation or an ability-branch choice
during a match, and the conditions shared by that and by Recall.

Supersedes the chest-menu interaction for reward selection described in
`Rewards.java`. Progression itself is owned by
[`CLASS_PROGRESSION_GROWTH_AND_TASK.md`](CLASS_PROGRESSION_GROWTH_AND_TASK.md).

---

## 1. The principle: levelling requires a space

**Spending a progression choice requires finding or making somewhere to do it.**
Not a menu you can open anywhere, mid-stride, mid-fight.

This is the point of the mechanic rather than a cost bolted onto it. It is
legible, it is Minecraft-native, and it creates real texture from one rule:

- **Mole stops tunnelling and digs out a chamber.** The class is the best in the
  game at manufacturing a level-up space on demand, and nothing grants it that —
  it falls out of what Tunneling already does.
- **A player caught in the open has to find terrain**, or fortify, or retreat.
- **Levelling during a fight means seeking cover first**, which is a decision
  with a position attached.

Levelling gets a *place*, which is how this project treats world state
generally.

---

## 2. The beat: a recurring evaluation, not a channel

While a player has unspent choices, the bossbar that reports them also runs a
**5-second countdown**. At the end of each countdown the player's state is
evaluated. If the conditions hold, the options are summoned. If not, the
countdown simply restarts.

**What matters is state at the end, not conduct across the interval.** Spending
the first four seconds escaping danger, digging a chamber and then crouching on
the final beat is correct play, not an exploit. That is where the skill lives.

This is deliberately **not** a channel. There is no progress to defend, no
interruption, no cancel reason, and therefore none of a channel's failure
modes — no penalty for being nudged by a mob or clipped by knockback. It is also
less machinery than `Recall`, not more.

The bossbar consequently reads as a **rhythm to position against** — "three
seconds, get in the hole" — rather than a bar being protected.

**In combat, the bar shows the combat countdown instead**, and returns to the
level-up countdown when combat ends. The two clocks are sequential and never
concurrent, so nothing has to hold paused progress: combat reaching zero is what
starts a fresh level-up countdown. See
[`COMBAT_STATE.md`](COMBAT_STATE.md#the-bossbar-shows-whichever-clock-is-actually-running).

### Why the bossbar is also the input

Raw key input through a predicate is a **proven dead end** in this project (see
the datapack Chunk 1 pass/fail record), and Swap Offhand is already spent on
ability activation. Making the input a **posture** rather than a keypress
sidesteps both: the player enters a state, and the state is the input. The
draft hall already does the same thing with ghosting.

[OPEN] Whether a confirming input is wanted on top of the posture.

---

## 3. Conditions

Four conditions, evaluated at the beat:

| Condition | Kind | Notes |
| --- | --- | --- |
| Crouching, on solid ground | instantaneous | A posture; rules out doing this airborne |
| Not moving | trailing window | See below |
| Not in combat | trailing window | See below |
| Aimed at eligible space | instantaneous | See §4 |

In practice the first, second and fourth already exclude most real fighting: a
player trading hits is moving, taking knockback, and cannot hold a crouch while
aiming steadily at a fixed piece of the world. **"Not in combat" is retained
anyway**, because the overlap is incidental rather than guaranteed, and because
it is the condition that states the intent.

### Two conditions need a trailing window

If every condition is read instantaneously, two of them stop meaning anything:

- **Not in combat.** A player mid-fight is not being damaged at most individual
  instants; the gap between two sword swings would qualify. This must mean *no
  damage dealt or received within the last N seconds*.
- **Not moving.** Instantaneous velocity is zero at odd moments. This wants a
  short look-back rather than a single sample.

Crouching and aim are genuinely fine as instantaneous reads. So the rule is
**state at the end, where two of the four states are defined over a short
trailing window** — which preserves the design exactly: sprint to safety and
crouch on the beat, but you cannot do it while actively trading hits.

Combat state is defined in [`COMBAT_STATE.md`](COMBAT_STATE.md) — a per-entity
timestamp of last combat involvement, with each consumer choosing its own
threshold. It has **five** consumers, not two, and is still unimplemented.

---

## 4. Eligible space, and what is summoned

**Three slots**, because N is always three — three Task trees, three ability
branches. Derive any width requirement from the slot count rather than fixing a
block span, so a future two- or four-option choice does not silently break the
rule.

Options are placed in **unobstructed space at the aim point**, not against a
flat surface. Requiring a flat face would make the mechanic fail in exactly the
places players most often are — a tunnel, a cave, a slope — while a wall,
open air, and underwater all work under the space rule. The player must still
be looking somewhere sensible; they need not be looking at masonry.

[OPEN] Entity composition. Text displays for the labels with interaction
entities for the hitboxes is the obvious shape. [OPEN] Whether other players see
the labels, a glow or particles, or nothing but the crouch. The project's
standing preference for legibility argues for opponents seeing *something*.

---

## 5. One allocation per summon

A summon spends **one** choice. A player holding four banked allocations needs
four separate safe moments.

This is the lever that decides whether banking costs anything. Spending all
pending choices from a single summon would make hoarding strictly free and the
whole condition set decorative; one per summon means promptness is rewarded and
a hoarder eventually pays for the delay somewhere quiet.

---

## 6. These conditions apply to Recall too

**Decided 27 September 2026.** Two changes to `Recall`, which currently channels
for `channelTicks: 100` with `moveTolerance: 1.5` and cancels on damage:

1. **Add the not-in-combat condition.** Cancelling on damage is not the same as
   requiring disengagement — it permits recalling out of a fight you are winning
   the damage race in. Blocked on the combat-state primitive above.
2. **Remove the movement tolerance.** There should be no movement allowance for
   Recall or for level-up selection.

[TECHNICAL RISK] A literal `moveTolerance: 0.0` compared against
`location.distance(origin)` will cancel on floating-point noise and ordinary
sub-block drift. The robust reading of "no movement" is **the player's block
position is unchanged** — you may shift within your block, and the moment you
leave it the channel ends. That is unambiguous, testable, and immune to float
jitter. Implement it that way rather than by shrinking the tolerance toward
zero.

Note the asymmetry, which is intended: Recall remains a **channel** that
movement and combat interrupt, because leaving is meant to be defensible.
Level-up selection is a **beat** with no interruption, because it is not a
contested action — it only asks where you are standing when the clock strikes.

---

## Explicitly open

Combat-state definition and its trailing window; the "not moving" look-back
length; whether a confirming input sits on top of the posture; entity types and
visual treatment; whether other players see the summoned options; and whether
the 5-second beat is the right period once tested.

This record does not change the pre-match class selection hall
([`PRE_MATCH_SELECTION_FLOW.md`](PRE_MATCH_SELECTION_FLOW.md)), which uses
physical stations and is a different situation: deliberate, out of combat, and
already in a place built for it.
