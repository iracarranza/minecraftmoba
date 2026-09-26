# Combat State

**Status:** Working design direction
**Date:** 2026-09-27
**Scope:** The shared definition of "in combat", which five separate systems
now depend on and none of them defines.

---

## Why this exists

Nothing in the plugin defines combat state. `Recall` cancels on
`EntityDamageEvent` and that is the whole of it. Meanwhile five designed systems
already assume the concept:

| Consumer | Uses it for | Recorded in |
| --- | --- | --- |
| Level-up selection | gating the beat that summons progression choices | [`IN_WORLD_SELECTION_AND_CHANNEL_CONDITIONS.md`](IN_WORLD_SELECTION_AND_CHANNEL_CONDITIONS.md) |
| Recall | gating the channel, on top of its existing damage cancel | same |
| Health Mastery | regenerating out-of-combat Absorption | [`2026-09-12-capacity-curve.md`](../proposals/2026-09-12-capacity-curve.md) |
| Shovel **Pathfinding** | out-of-combat movement speed on path blocks | [`CLASS_PROGRESSION_GROWTH_AND_TASK.md`](CLASS_PROGRESSION_GROWTH_AND_TASK.md) |
| Skeleton Crew **Deadline** | burning Crew in combat long enough explode | [`classes.md`](../../classes.md) §15 |

Five consumers, no definition, and each would otherwise grow its own. Define it
once.

---

## 1. Shape: a timestamp, not a boolean

**Record the tick of last combat involvement. Do not ship an `inCombat` flag.**

Consumers ask *"has it been at least N since?"* and choose their own N:

    inCombat(entity, threshold) := currentTick - lastCombatTick(entity) < threshold

One definition of **what counts**; many thresholds. This matters because the
consumers genuinely want different windows — spending a progression choice is
routine admin, while regenerating Absorption is a real reward and should demand
a longer quiet period. A shared boolean would force one window on all five and
be wrong for at least three.

It is also the cheapest thing to poll, which Pathfinding needs: it is evaluated
continuously while a player moves, not at an event.

## 2. It applies to any living entity, not only players

**Deadline is the reason.** Its threshold is about a **Crew Member** being in
combat, not the commander — "Crew that remain in combat while burning long
enough explode", and "an enemy can disengage to prevent the explosion."

So the primitive is keyed by entity, not by player. A player-only implementation
would have to be rebuilt the moment Skeleton Crew is implemented.

---

## 3. What counts

Combat involvement is **damage dealt or received, traced to a living source.**

**Sets combat state:**

- damage received from a living entity, directly or via its projectile
- damage dealt to a living entity

**Does not set combat state:**

- **Environmental damage** — fall, fire, lava, drowning, suffocation, starvation,
  cactus, the void. The concept is combat, not harm. Burning in lava should not
  block a Recall; the lava is already the problem.
- **Damage dealt or received by an entity's summons.** A commander's combat state
  is their own.

### The summon exclusion is load-bearing

Skeleton Crew's crew fights more or less continuously, and its recruitment loop
*is* fighting, nocturnally, as the class's defining activity. If crew combat set
the commander's state, a Skeleton Crew player could never level up, never
Recall, and never hold an out-of-combat effect during the exact play the class
exists to perform.

The class's own design already answers the friction: **the crew keeps fighting
while the commander steps back.** That is the fantasy working, not a loophole —
redirecting labour into military presence is precisely what Skeleton Crew does,
and the commander being free to do something else while it happens is the point.

Golem Master inherits the same treatment for the same reason.

### Hostile mobs count

All living-entity damage counts, not only player-versus-player. A PvE exemption
would make a mob swarm a free Recall zone, and would permit spending a
progression choice while a zombie is actively chewing on the player, which reads
as absurd in a game whose whole argument is Minecraft-native legibility.

This is a real cost for classes that farm mobs — Skeleton Crew above all, and
Mole fighting through a cave. The cost is correct: they must actually disengage.
The summon exclusion is what keeps it payable.

---

## 4. The player must be able to see it

A gate the player cannot observe fails silently, and the level-up beat has four
conditions evaluated on a five-second cycle. A player who fails the beat and is
told nothing will conclude the feature is broken.

**On a failed beat, say which condition failed.** Combat state should also be
visible while it is suppressing something the player is waiting on. `Recall`
already sets the precedent with its "Recall interrupted" action bar; this is the
same courtesy applied to a condition that blocks *before* anything starts rather
than cancelling midway.

[OPEN] The surface. The action bar is the obvious candidate; the HUD already has
a mode line.

---

## Explicitly open

- **Every threshold.** No magnitude is chosen here. The one existing datum is
  Deadline's "[OPEN] Threshold, conceptually around three seconds" in
  `classes.md`, which is a hint at scale for one consumer and not a default for
  the rest. Recall's existing `channelTicks: 100` is a channel length, not a
  combat window, and should not be borrowed as one.
- **Whether non-damage events set combat state** — using an ability, being
  targeted, being aggroed by a hostile mob. Damage-only is the narrow, testable
  starting definition; it will let a player Recall while a creeper is mid-fuse.
- **Whether damage dealt to a passive animal counts.** Punching a cow for Looting
  is technically combat and probably harmless to include, but it briefly gates a
  Yield build out of its own progression.
- **Whether a killing blow should clear the state early**, so that winning a
  fight decisively is rewarded rather than treated the same as fleeing one.
