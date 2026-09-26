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

## 1. One state, one duration

**An entity is in combat for 7 seconds after a qualifying action.** One number,
one state, for every consumer.

    inCombat(entity) := currentTick - lastCombatTick(entity) < 7s

[PROTOTYPE] 7 seconds is a first value, not calibration.

A single duration is preferred over per-consumer windows specifically because
**the player has to be able to see this** (§5). Five consumers with five
thresholds would mean five invisible states, and no indicator could honestly
report "you are in combat" when the answer differs per system. One state is one
thing on screen.

A consumer that wants more quiet than combat state provides should add its own
delay **on top** of being out of combat — "out of combat, then a further N
before Absorption begins" stays explicable in a way that a private 12-second
combat window never would.

Implement it as a stored tick rather than a flag with a scheduled reset: it is
the cheapest thing to poll, which Pathfinding needs since it is evaluated
continuously while a player moves rather than at an event.

## 2. It applies to any living entity, not only players

**Deadline is the reason.** Its threshold is about a **Crew Member** being in
combat, not the commander — "Crew that remain in combat while burning long
enough explode", and "an enemy can disengage to prevent the explosion."

So the primitive is keyed by entity, not by player. A player-only implementation
would have to be rebuilt the moment Skeleton Crew is implemented.

---

## 3. What counts

**Sets combat state:**

- **taking damage** from a living entity, directly or via its projectile
- **dealing attack damage** to a living entity
- **activating a combat ability** (§3.1)

**Does not set combat state:**

- **Moving.** Travel is not combat, however fast or evasive.
- **Environmental damage** — fall, fire, lava, drowning, suffocation, starvation,
  cactus, the void. The concept is combat, not harm. Burning in lava should not
  block a Recall; the lava is already the problem.
- **Damage dealt or received by an entity's summons.** A commander's combat state
  is their own; summons carry their own (§3.3).
- **The continuing effects of an action already taken** (§3.2).
- **Passives.** A passive is not an activation. Lightfooted's Animal Senses and
  Skeleton Crew's Undead Affinity are always on; if they set combat state, those
  classes would never leave it.

### 3.1 Abilities are authored combat or non-combat

Combat-ness cannot be inferred from an ability's existence, because several
abilities are the opposite of fighting: Mole's **Tunneling** is a digging mode
and a toggle, Gardener's **Clip** harvests a plant, Merchant's **Work** employs
villagers, Golem Master's **Assemble** builds a wall, Skeleton Crew **Raises** a
worker. A blanket rule would lock each of those classes out of its own
progression while it performed its defining activity.

The working line: **an ability is combat when it acts on a combatant's capacity
to fight** — dealing damage, healing, mitigating, buffing, granting speed — and
non-combat when it acts on the world or the economy.

**Classify by effect on combatants, not by the medium.** Mole's **Sinkhole** is
combat, and not because it breaks blocks: it slows enemies and denies them the
ability to build, which is crowd control by another name. Mole's **Tunneling**
touches the same medium and is not combat, because it excavates and does nothing
to anybody's capacity to fight. An ability that moves terrain can be either; what
it does to the people standing on that terrain is the question.

Declare it as `abilities.definitions.<id>.combat`, beside the existing
`cooldownTicks`. **Require the key; do not default it.** This repository has
already paid for the alternative: `ConfigKeysDefinedTest` exists because six
features shipped silently disabled when their keys were absent. A forgotten flag
defaulting to non-combat is a silent exploit, and one defaulting to combat is a
silent lockout that reads as a broken feature. Failing at load is the convention
here.

Two cases a single static flag cannot express:

- **A branch can change combat-ness.** Daredevil's **Runway** is pure mobility,
  but its **Suplex** branch subjects an enemy to the launch. The declaration
  therefore needs to be overridable per branch, which
  `abilities.definitions.<id>.branches.<branch>` already supports structurally.
- **Some abilities decide at activation.** Skeleton Crew's **Graveyard Shift** is
  context-sensitive by design: against a valid target it Strikes, otherwise it
  Raises a Crew Member. One activation, two outcomes, only one of which is
  combat. Such an ability must report what it actually did rather than carry a
  fixed answer.

So the declared flag is the ability's normal case, and a context-sensitive
ability signals combat at execution. [OPEN] The exact seam.

### 3.2 Actions, not effects

**Combat state tracks what an entity did, not how long its effects last.**

Poisoning an enemy puts the caster in combat for 7 seconds. If the poison runs
for 10, the caster is not in combat for the last 3 — the cast was the action,
and the damage after it is the consequence.

This generalizes well beyond poison: Mole's Sinkhole collapsing in stages,
Skeleton Crew's Deadline explosion, Waxer's Enzymatic durability wear, a
Gardener thicket that hurts somebody minutes later. Without the rule, any class
whose output is persistent or infrastructural would be permanently in combat —
which would punish exactly the classes this project insists must stay viable.

**The asymmetry is intended.** The victim of a damage-over-time *is* held in
combat, because each tick is damage taken and refreshes their 7 seconds. Taking
damage is the clearest possible signal of being in a fight. The consequence is
that **a damage-over-time denies Recall**, which gives such abilities a real
strategic role beyond their damage: poison someone and walk away, and they
cannot leave until it ends.

### 3.3 Summons carry their own combat state

Summoned entities have combat state, by these same rules, keyed to themselves.
Skeleton Crew's **Deadline** requires exactly this — its threshold is about a
Crew Member being in combat, not the commander, and an enemy disengaging is what
prevents the explosion.

A summon's combat state does **not** propagate to its owner.

#### The summon exclusion is load-bearing

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

### 3.4 Hostile mobs count

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

Of those four conditions, three are things the player **chooses** — crouch,
stand still, look at eligible space — and one is **imposed** on them. The
imposed one is the one that needs displaying, because it is the only one the
player cannot see coming.

### The bossbar shows whichever clock is actually running

While a player holds unspent choices, entering combat **replaces the level-up
countdown with an in-combat countdown**. When combat ends the bar returns to the
level-up countdown, and the beat resumes from there.

The two clocks are therefore sequential and never concurrent, and no paused
progress has to be held anywhere: combat state reaching zero *is* the signal to
start a fresh level-up countdown. A player who takes a hit sees the bar flip,
watches combat run out, and watches the level-up clock start — which reads as a
single continuous explanation of why they cannot level up yet.

This also answers most of the feedback problem above at no cost. It does not
answer all of it: a player who is out of combat and still fails the beat needs
to know it was the crouch, the movement or the aim.

[OPEN] **A player with no unspent choices still needs to see combat state**,
because it also gates Recall, Absorption regeneration and Pathfinding's speed.
The bossbar swap covers the level-up case specifically; combat state wants a
quieter persistent surface as well. The action bar is the obvious candidate and
the HUD already has a mode line.

[OPEN] Whether the returning level-up countdown restarts or resumes where it
stopped. Under state-at-end it changes only how soon the next evaluation
happens, so this is a feel question rather than a mechanical one.

**The two durations stay independently tuned.** That combat is 7 seconds and the
level-up beat is 5 is a coincidence of first values, not a relationship. Showing
them in the same bar must not become a reason to collapse them into one number.

---

## Explicitly open

- **The 7-second duration** is [PROTOTYPE] and wants testing. Deadline's
  "conceptually around three seconds" in `classes.md` is a separate number — how
  long burning Crew must *remain* in combat — and neither should be derived from
  the other. Recall's `channelTicks: 100` is a channel length, not a combat
  window, and must not be borrowed as one.
- **The per-ability combat classification itself**, once the roster's abilities
  are implemented. The working line decides most of them. Mole's **Sinkhole** is
  settled as **combat** — it is crowd control, slowing enemies and denying them
  building, and the terrain is only its medium. The next case the rule has to
  answer is Golem Master's **Assemble**: a wall is Construction methodology, but
  the **Snow Wall** branch explicitly slows and displaces enemies, and any wall
  raised mid-fight is mitigation. Likely combat, probably per branch, not yet
  decided.
- **The seam by which a context-sensitive ability reports what it did.**
- **Whether damage dealt to a passive animal counts.** Punching a cow for Looting
  is technically an attack, and under this definition it is combat for 7 seconds
  — near-continuous while a Yield build farms, gating that build out of its own
  progression. The likeliest exception, and the one most worth testing.
- **Whether being targeted or aggroed counts.** Currently it does not, so a
  player may Recall with a creeper mid-fuse.
- **Whether a killing blow should clear the state early**, so that winning a
  fight decisively is rewarded rather than treated the same as fleeing one.
