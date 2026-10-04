# Scenario harness and progression simulation

**Scoped 3 October 2026. Prototype/test — nothing here is an established
decision.** Scoped against the lab room, `LabMaps` and `LabAuthoring` introduced
on 2 October, which were uncommitted at the time of writing.

## The problem this exists for

A fourteen-player game cannot be exercised by one person, and the obvious
reading of that — "we need thirteen bots" — is the expensive reading. It is
three problems wearing one coat.

| | What it asks | Needs intelligence? |
|---|---|---|
| **A. Load** | Do fourteen players' simultaneous casts, block writes, reservations, HUD updates and combat states hold up? | **No.** Needs bodies. |
| **B. Interaction correctness** | Does friendly fire respect teams? Does a Stun refuse a cast? Does Buffet restore a hotbar on disconnect? | **No.** Needs engineered situations. |
| **C. Flow and balance** | Is the levelling pace right? Does the win condition produce good games? Is this fun? | **Yes**, and no bot will supply it. |

**A and B are most of the testing pain, and neither needs a bot that can
play.** C is the one that does, which is why chasing it first is how this ends
up as an AI project instead of a testing project.

**The gap is already costing us.** Two live defects reached a running server
and were invisible to what is now 1,238 unit tests, because both were
integration facts rather than logic errors:

- Yield multiplied by tier twice, so a Yield-3 player averaged 3.25 items from
  every natural block.
- Health regeneration stood down at food ≥ 18 in favour of a gamerule a match
  deliberately holds off, so the healthiest players healed not at all.

Neither was findable without a server, a player and an action. That is exactly
what this harness supplies.

---

# Part 1 — The scenario harness

## Bodies: server-side fake players

Construct a `ServerPlayer` with a synthetic profile and a dummy connection, and
add it to the world. Carpet's `/player` command does this on Fabric and several
Paper plugins do the equivalent.

**The decisive property is not convenience. They are real `Player` objects.**
So `plugin.enrolled(p)`, `PlayerData`, `Capacity`, `AbilityInputs`, team
assignment, `Provenance` attribution, `CombatState` and `Stun` all work
**unchanged**.

That matters more than it first appears: the alternative — an NPC or a mock
that merely resembles a player — forces an `if (isBot)` branch into the game
code, and those branches multiply until the harness is testing a special case
of the game rather than the game.

[TECHNICAL RISK] This is reflective NMS and it will break on Paper updates.
`NativePacketQueue` already establishes that pattern here, so the project has
both the precedent and the scar tissue. The ongoing cost is real and should be
accepted deliberately rather than discovered.

### What was considered and not chosen

**Mineflayer headless clients** exercise the real network path, which is
genuinely valuable. Rejected for now on three counts: its 1.21.11 protocol
support needs verifying before anything is built on it; fourteen Node processes
is a heavier operational story than one plugin; and input semantics would be
re-implemented outside the plugin that defines them. Worth revisiting
specifically for protocol-level questions.

**Citizens-style NPCs** are not `Player` instances, so precisely the code paths
worth exercising would not run. Rejected.

## The number is four, not fourteen

**Most team-team questions are 2v2 or 3v3 at a contested point.** Whether Suplex
throws an ally, whether two Pavers can reserve the same position, whether a Stun
lands through a Bloodmason channel — none needs more than four bodies.

Fourteen is a **load** number, and load needs no intelligence at all: fourteen
bots walking in circles and casting on a timer answer problem A completely.

This is the single largest cost saving available here, and it should shape the
harness: **build for four, scale to fourteen for load.**

## Lifecycle, against the lab

The lab already supplies isolation, disposability and refusal-to-collide. The
harness should add nothing to that and borrow all of it.

1. **`/moba lab start`** — existing. Admin, idle controller, lab room.
2. **Select scoop, class, level, team** — existing, via pedestals or commands.
3. **`/moba lab scenario <id>`** — *new*. Loads a scenario definition, and
   refuses if the selection it requires is absent.
4. **Launch** — existing `play()`, which copies pristine terrain into
   `moba_lab_match` and calls `match().startForTest()`.
5. **Spawn** — *new*. Fake players are created **after** the match is running,
   for the same reason `/moba lab` itself adds participants after the start:
   joining before the world exists skips the spawn path.
6. **Run** — *new*. A tick-driven timeline drives actions and evaluates
   assertions.
7. **Report** — *new*. Pass/fail per assertion, written to the lab session log.
8. **`/moba lab end`** — existing. Teardown must despawn every fake player
   **before** the world unloads.

### Teardown is the part that will break

Four failure paths, all of which leave a fake player alive in a world that no
longer exists:

- The scenario throws midway.
- A fake player dies and respawns.
- The admin calls `/moba lab end` during a run.
- The server stops with a scenario active.

**Despawn must therefore be idempotent and run from a single place**, the same
discipline `DrillRushAbility.cancel` uses for burrowed invulnerability and
`DisplacementLedger.release` uses for reservations. The harness should hold its
roster in one structure and tear down from one method, with the world-unload
path calling it unconditionally.

[OPEN] Whether a fake player's `PlayerData` should persist. It almost certainly
should **not** — a scenario roster writing to the real player store would
pollute progression data with synthetic players, and the codec has a format
version that would then carry them forever.

## Scenario format

Declarative, in the plugin's own config idiom rather than a new language, so
the existing config-key guard (`ConfigKeysDefinedTest`) keeps applying.

```yaml
scenarios:
  friendly_fire:
    description: "Authored damage does not cross teams; systemic damage does."
    roster:
      - { id: north_a, team: NORTH, class: lightfooted, level: 12, at: [10, 70, 0] }
      - { id: north_b, team: NORTH, class: toolbox,     level: 12, at: [12, 70, 0] }
      - { id: south_a, team: SOUTH, class: mole,        level: 12, at: [20, 70, 0] }
    timeline:
      - { tick: 0,  actor: north_a, do: cast, input: LEFT_CLICK, target: north_b }
      - { tick: 5,  assert: health_unchanged, of: north_b }
      - { tick: 10, actor: north_a, do: cast, input: LEFT_CLICK, target: south_a }
      - { tick: 15, assert: health_below, of: south_a, than: max }
      - { tick: 20, actor: north_a, do: place, block: TNT, at: [11, 70, 0] }
      - { tick: 40, assert: health_below, of: north_b, than: max,
          because: "TNT is systemic, and systemic damage crosses teams by design" }
```

Four properties worth holding:

- **Ticks are absolute, not relative.** A relative timeline makes a scenario's
  meaning depend on how long the previous step took, and a step that silently
  takes longer turns an assertion into a race.
- **`because` is part of the assertion**, not a comment. A failing assertion
  should say what rule it believed in, the way the plugin's own refusals do.
- **Actions name an actor.** There is no implicit "the player", because the
  whole point is more than one.
- **Scenarios are data.** Adding one is not a code change, which is what keeps
  them cheap enough to actually write.

[OPEN] Whether assertions evaluate at a tick or over a window. A window
(`between: [10, 20]`) is more forgiving of scheduling jitter and almost
certainly needed for anything involving a projectile.

## The first three scenarios

Chosen because each one covers a rule that is **currently believed rather than
demonstrated**, and each would have caught a real class of bug.

### 1. Friendly fire — authored versus systemic

`TeamDamage` allows an authored list of causes and refuses them across teams,
while systemic damage — TNT, suffocation, fall, fire — crosses teams
deliberately. That distinction is load-bearing for Toolbox, whose entire kill
method is systemic, and it is currently asserted only in unit tests against the
cause list, not against a running fight.

Asserts: an ally takes nothing from an authored ability; an enemy does; an ally
**does** take damage from the caster's own TNT.

### 2. Stun and Root, enforced

Root was declared and enforced nowhere from the Utility Belt shipping until
29 September. The enforcement is now four days old, applies to every player in
every match, and **nobody has ever seen it act**.

Asserts: a rooted player's position does not change while their yaw does; a
rooted player **can** still attack; a stunned player cannot attack, place,
break or cast, and receives the `STUNNED` notice; both expire on time; a Stun
applied over a longer Stun does not shorten it.

### 3. Displacement reservation across two players

The registry has three intended callers and none is built, so its guards have
never been exercised against a second player trying to occupy a reserved
position.

Asserts: a reserved position refuses a block placement by *another* player with
the `RESERVED` notice; fluid does not flow into it; the block returns to its
exact state with its `Provenance` mark intact; and — the one that matters — a
scenario aborted mid-run leaves **no** reserved position behind.

## Cost and sequencing

| Piece | Rough size | Note |
|---|---|---|
| Fake player spawn/despawn | Small, fiddly, version-sensitive | The only NMS in it |
| Roster and teardown discipline | Small | Where the bugs will be |
| Scenario loading and timeline | Medium | Ordinary config and scheduling |
| Assertion vocabulary | Grows per scenario | Start with five or six |
| First three scenarios | Small each, once the above exists | |

Build in that order. The first scenario is worth nothing until teardown is
trustworthy, because an untrustworthy teardown produces failures that are the
harness's fault and will be read as the game's.

---

# Part 2 — Progression simulation

**A different instrument, answering questions bots cannot, and it should exist
regardless of whether the harness does.**

Model the economy and progression in plain code with no Minecraft at all: XP
rates, the level curve, Worksite output, resource accrual, objective timings,
Growth and Task effects — driven by parameterised policies such as a greedy
farmer, an aggressive ganker, an objective-focused player. Run ten thousand
matches in seconds.

This is already idiomatic here. The worldgen side screened 265,280 candidate
windows to certify three maps and carries `expedition/benchmark.py` and
`test_terrain_simulation.py`; doing the same for progression surprises nobody.

**What it finds:** runaway leads, dead levels nobody wants, curve
discontinuities, compounding economies, a specialization that dominates every
policy, a Task domain that never pays.

**What it cannot find, and must not be asked to:** whether a fight feels good,
whether players discover the intended strategy, whether a class is legible.

> **A simulation's value depends on the model being right, and the model is
> largely the thing being validated.** It is reliable for *internal
> inconsistency* and silent about *emergent quality*.

Stated plainly so nobody later cites a simulation run as evidence of balance:

> The simulation is a design instrument, not a test. The harness is a test
> harness, not a design instrument.

---

# What remains human

Problem **C** needs people, and no amount of either instrument changes that.

What they do change is what a scarce playtest is *spent* on. Sessions with
thirteen other people are the most expensive resource this project has, and
they should not be spent discovering that friendly fire is broken, that Root
does nothing, or that a reservation leaked a hole in the map.

**The harness exists so the human playtest can be about the game.**
