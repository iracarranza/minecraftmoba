# Combat chamber

**Scoped 8 October 2026. Prototype/test. Nothing here is an established
decision.** Decisions the user stated are marked *(stated)*; everything else is
proposal. This is a design record, not an implementation.

## What it is

A place in the lab to exercise **abilities**, as the terrain chamber
(`implementation/plugin/CHAMBERS.md`) exercises **placements**. It is a separate
chamber, reached from the lab setup menu, not a mode of the terrain chamber
*(stated)*.

It has two independent axes. They should be built and tested separately.

| Axis | Choices |
|---|---|
| **Venue**: where | blank slab, dojo, platform stage, scoop |
| **Role**: who acts | Operator, Recipient, Observer |

## The dummy *(all stated)*

- **A real `Player`.** Several abilities act only on players (Brawling Slash's
  leap, team damage, `Targetability`, `Stun`'s move handling). A zombie or armor
  stand would silently skip those branches, which is the same class of false pass
  `Bodies.connected()` exists to prevent.
- **Passive.** It has no behavior of its own beyond a deliberate script (see
  *Scripting the dummy*). It does not act on its own initiative.
- **Measurable.** It records what happens to it.
- **A team.** Its team is a setting. An enemy dummy receives damage and control;
  an ally dummy receives heals and buffs. Recipient testing of a support ability
  needs it.

### What "measurable" means

| Record | Notes |
|---|---|
| Each hit: raw damage, damage after armor and reduction, source ability and branch | The raw/after pair is how armor and Damage Reduction are checked. |
| Damage in a rolling window, and time to kill from full | Resets on command. |
| Active statuses and their remaining time | Stun, Root, Weakness, and the rest. |
| Health, and any state a class carries | e.g. a Werewolf's form, Hemostasis. |

Measurement runs in **both directions**, because Recipient has two modes (below):
damage the tester takes, and damage the tester deals to a buffed dummy. The
accumulator is plain logic and is tested without a server. Only the event
listener feeding it needs one.

## The pre-entry phase *(stated)*

Like the terrain chamber, the combat chamber has a phase before you enter, and
the choices made there shape what is offered. The flow is a small state machine:

```
class  ->  role  ->  (Recipient / Observer) slot and output
       ->  dummy behavior  ->  modes  ->  enter
```

1. **Choose the class to examine first.** Nothing else is offered until then.
2. **Operator is always available.** You enter as that class. For Werewolf this
   includes entering and leaving its states.
3. **Recipient and Observer cannot be chosen until a class is.** Once it is, the
   player sees icons for **passive, A1, A2 and ultimate**, with each option
   available or unavailable according to the chosen class's actual effects and
   outputs.
4. **Passives.** Some cannot be received at all, only **operated** (which names no
   ability) or **observed** (which needs a scripted usage decided somehow; see
   *Scripting the dummy*).
5. **Modes** are chosen here, not mid-session. Cooldown waiving is one *(stated)*:
   it changes what is being measured, so it is set before entry and shown in
   every report. Others: time of day (Werewolf's Unleashed depends on it), the
   dummy's team, gear, level, and the venue.

### What decides availability

The right source is declared data, not a hand-kept table.

- **What exists.** `AbilityOutput(id, TargetForm, InputForm)`
  (`Ability.outputs()`). `TargetForm` is `SELF`, `UNIT`, `AREA`, `DIRECTION`,
  `VECTOR`. `InputForm` is `PASSIVE`, `INSTANT`, `CHANNELED`, `CHARGED`.
  `InputForm.PASSIVE` already marks a passive. Only 9 abilities declare outputs
  today, and `ability-skeleton.yml` (`ABILITY_SKELETON.md`) holds an 18-class by
  4-slot manifest with `?` for unanswered cells.
- **The gap.** `TargetForm` says how an ability is **aimed**, not **who it
  affects**. Crash Landing is declared `SELF` yet damages nearby enemies, so
  "Recipient needs a unit target" would wrongly exclude it.
- **The fix, requested.** An `affects` value per output: `NONE` (self only),
  `ENEMIES`, `ALLIES`, `BOTH`, or `?`. It maps onto the dummy-team setting and is
  legal as `?` only while an ability is merely designed. It was requested of the
  session building the ability skeleton on 8 October 2026; **whether it has been
  added is unconfirmed**. Until it lands, availability for abilities without it is
  an authored table with a test that every ability has an entry.
- **A pure function.** Given a role, a slot and the declared outputs, "is this
  option available" can be computed and tested for every class without a server.

## Roles

**Operator.** You take the class and use it on the dummy. Needs a level and class
(the lab already offers both), a dummy, and a way to reset it.

**Recipient** *(stated)*. The **dummy casts on you, on your command.** It is not an
injected effect. It is really **two modes**, because the ability decides which
applies:

| Mode | Example | You do | Measured |
|---|---|---|---|
| **Receive** | Tank one of A1's strikes | Stand in the effect | Damage and statuses **you** take |
| **Counter** | Werewolf's Bloodhide (A2) | Strike the dummy before and after it casts | Damage **you deal** to the buffed dummy |

An output that affects others is Receive; one that buffs the caster is Counter.
This is another reason `affects` is needed.

**Observer** *(stated)*. Its purpose is **to visually examine a cast**, not to study
its effects broadly. It uses the same commanded caster but is about the camera:
repeat the cast, see it from the enemy's side, choose a viewpoint. Measurement is
secondary.

Recipient and Observer share the mechanism and differ in purpose, so they differ
in controls, not in code.

## Scripting the dummy

Two layers, so neither has to carry the other.

**Script: what the dummy does, and when.**

- **Preset.** Cast once; cast every N seconds; hold a charged or channelled output
  for a duration; delay before the first cast.
- **Recorded (a ghost).** The tester performs the actions as Operator, the system
  records them, and the dummy repeats them. This is the preferred way to script
  anything granular, and it also answers how an **observed passive** gets a
  scripted usage: whoever authors it records it once.

**Aim policy: where the dummy points.** *(first three stated)*

| Policy | Meaning |
|---|---|
| At the player | Resolves to the tester's current position |
| Straight ahead | Along the dummy's facing |
| Random direction | A fresh direction per cast |
| At a marked point | The tester places a marker; area outputs resolve to it |
| As recorded | The aim captured in the take |

Other granular settings: the branch, gear, distance, and facing. Settings that
only apply to some outputs (hold duration for `CHARGED`/`CHANNELED`) appear only
for those.

### The ghost: record inputs, not positions

The dummy is a real player, so replaying **inputs** keeps every game rule in
force: its casts obey cooldowns, Stun and Root, and a movement ability's own
physics. Replaying positions would teleport past all of that. It also means
counterplay is real: stun the ghost and its next cast is refused.

- Recording needs a hook where inputs enter `AbilityInputs`.
- A replay is **approximate**. Terrain, timing and randomness mean it will not
  match exactly, and the report should say so.
- A recording is **open-loop**: it does not react to the tester. That is why it is
  consistent with the dummy being passive.
- One person cannot drive both sides live. The nearest thing is a **tag-swap**:
  record yourself, then switch to the other role.

### Authored reactions *(stated)*

The tester can author an action for the ghost **that they must react to**.
Lightfooted's ultimate landing on a chosen location is the model case: place a
marker, the ghost casts to it, the tester reacts. The reaction can be measured
(damage taken; whether the area was left in time).

### Interference *(stated)*

If the ghost is **struck, displaced or otherwise interfered with** during its
scripted actions, it becomes a **passive dummy until it leaves combat**, then
returns to its scripting loop.

```
SCRIPTING  --struck / displaced / Stunned / Rooted-->  INTERFERED (passive)
INTERFERED --leaves combat (CombatState)----------->   SCRIPTING
```

Details proposed, not stated:

- On interference, cancel any channel or charge the ghost was holding.
- On resuming, restart the loop from the beginning, so each presentation of the
  telegraph is clean, rather than resuming mid-sequence.
- "Displaced" means displaced by something other than its own ability.
  `TunnelingAbility.externallyDisplaced` already draws that line.
- While interfered it is still measurable.
- A manual reset forces the loop to resume.
- Observer may want an **ignore interference** setting, since its point is the
  visuals.
- The state machine is pure and tested without a server.

## Commanding the dummy in the chamber

The terrain chamber already solved "a hotbar that is a menu" (`ChamberMenu`,
`ChamberHotbar`, `ChamberController`). The combat chamber should reuse that
shape, not invent a second:

```
root      Cast A1 | Cast A2 | Cast Q | Dummy setup | Measure | Reset
setup     Class | Level | Armor | Team | Go back
measure   Show log | Clear | Window
```

`ChamberMenu` enforces eight entries per page, a fixed back slot, and refusals
with reasons. It would need generalising from the chamber's pages, not copying.

## Venues

- **Blank slab.** Flat, measured, known blocks and distances: the **control**, in
  the sense `TestBed` uses the word. Terrain-reading classes (Mole, Quarryman,
  Paver, Gardener, Chauffeur, Daredevil) need ground, water, height or paths, so
  the slab carries `TestBed`-style strips rather than being empty.
- **Dojo.** A designed variation: cover, walls, height. Because it is hand-shaped
  it cannot be a control. [OPEN] What it varies.
- **Platform stage.** Mostly tests displacement (Runway, Bounding, knockback, void
  kills). Valuable, but later. [OPEN] Whether "balanced stage" is a design
  artifact or just a bounded arena.
- **Scoop.** Already exists.

Each venue is a world launched from the setup menu, the way a scoop is. Room,
dojo and stage can be generated by code, with their geometry as pure layout
tested without a server, the way `ChamberPlatform` is.

## Critical path

1. **`Bodies`.** A connected fake player, by reflective NMS. This is the
   prerequisite for Recipient, Observer and the ghost, and the harness doc
   (`SCENARIO_HARNESS.md`) already scoped it. **[TECHNICAL RISK]** It is
   version-sensitive and will break on Paper updates; `NativePacketQueue` is the
   precedent. It has not been built.
2. **Casting for a body.** `Ability.execute(Player, AbilityContext)` takes a
   player, so a body can be passed. What a body lacks is *input*: it has no look
   direction, and held and double-press abilities read `AbilityInputs`. Casting
   for it means setting its position and rotation toward the target first, and
   synthesising the inputs a held ability waits for.
3. **Measurement.** The damage accumulator, then the listener.
4. **The `affects` axis**, and the availability function built on it.
5. **Pre-entry flow**, as a pure state machine over the availability function.
6. **Venue builders**, starting with the slab.
7. **Input recording**, then replay, then the interference state machine.
8. **Lab setup menu** entry for the combat chamber.

Steps 3 to 6 do not need `Bodies` and can be built and tested first. That gives a
usable **Operator** mode against a measurable target before the NMS work.

## Cautions

- **An ability that needs its caster to move** (Lunge, Tunneling, Bounding) cannot
  rely on `PlayerMoveEvent` from a body that sends no packets. A passive dummy
  that only stands and casts is fine. A dummy asked to cast a movement ability
  needs the body to be `connected()`, or the test refuses rather than passing
  vacuously.
- **Cooldown waiving** changes what is measured. It is a pre-entry mode and is
  stated in every report.
- **Time-dependent kits.** Werewolf's Unleashed depends on time of day, so the
  chamber needs to set the world clock for those.
- **Replay fidelity** is approximate and must be labelled so.

## Suggested order

1. Measurement accumulator, the `affects` axis, the availability function, and the
   blank slab, with Operator mode and a **temporary non-player target**, clearly
   labelled, so damage numbers can be read before `Bodies` exists. It must not be
   mistaken for the real dummy.
2. `Bodies`.
3. Commanded casting, which unlocks Recipient and Observer.
4. Input recording and replay, with the interference state machine.
5. Dojo and stage.

## Status, 9 October 2026

Slice 1 is built and verified live: availability, the pre-entry flow, the damage log,
the blank slab, the `/moba lab combat` commands, cooldown waiving, and Operator mode
against a real `NmsBodies` dummy (`docs/audit/2026-10-09-combat-chamber-acceptance.md`,
19 of 19). Recipient and Observer go through the pre-entry flow but are refused at
entry, because the dummy cannot cast on command yet.

Two statements above were corrected by running it:

- **"No scoop and no match needed" was incomplete.** `LobbySafety` cancels all damage to
  anyone not in a running match, so the chamber needs an explicit exemption for its
  tester and dummy. It has one (`CombatChamber.occupies`).
- **A measured final can be below raw with no armor**, because vanilla damage-immunity
  frames apply only the excess of a second hit. The report states this.

The temporary non-player target proposed in the suggested order was not needed: the
real dummy was available first.

## Open questions

- Should the temporary non-player target in step 1 be built, or should nothing
  ship until `Bodies` exists?
- Should Observer have its own controls (repeat, freeze, camera), or is that a
  later layer?
- Does the `affects` request land in the skeleton as specified?
- Resume point after interference: restart (proposed) or mid-sequence?
