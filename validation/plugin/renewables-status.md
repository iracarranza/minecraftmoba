# Regenerative Sources — acceptance results, 19 September 2026

Fixture: Paper 1.21.11, acceptance server port 25576, fixture-only sources
`fixture_patch` (CROP, capacity 5) and `fixture_herd` (ANIMAL, capacity 3) at
the same coordinates, which is the ordinary case for a wild meadow.

| Scenario | Result |
| --- | --- |
| `renewableHarvest` | **Pass.** Three wild wheat breaks took the patch 5/5 to 2/5, `harvests=3`. |
| `renewableFarmNotWild` | **Pass.** Player-placed wheat confirmed present, then confirmed broken, with availability 5/5 unchanged and `harvests` unchanged. |
| `renewableRecovery` | **Pass.** Depleted to 0/5, clock advanced, restored to 5/5. Inventory `[]` before and after; `grantedByRenewal=0`. |
| `renewableAnimal` | **Partly vacuous as first reported.** The cow half was real: a player-killed cow took the herd 3/3 to 2/3. The zombie half was not — the fixture runs on Peaceful, so `summon minecraft:zombie` failed and the "did not count" result was measured against an entity that never existed. Corrected below. |

## Correction, later the same day

The `renewableAnimal` row above was reported as a clean pass. Its negative half
was vacuous: hostiles cannot be summoned on Peaceful difficulty, the summon
failed silently, and an availability figure that did not move was read as proof
that a hostile does not count against an ANIMAL source. Nothing was tested.

This surfaced only because the later SWARM scenario asserted its target was
present and that marker did not print. An availability number alone cannot
distinguish "the rule held" from "the event never happened", so both scenarios
now set `difficulty easy` and assert `ANIMAL_TARGET_PRESENT` /
`MONSTER_TARGET_PRESENT` before killing anything.

**Treat a summon as an action that can fail for reasons outside the plugin.**
Any scenario summoning an entity needs a presence assertion, or its negative
results are worthless.

## Re-measured results

| Scenario | Result |
| --- | --- |
| `renewableSwarm` | **Pass, non-vacuous.** Target confirmed present, then confirmed dead. Player-attributed kill took the swarm 2/2 to 1/2. A second zombie killed by `/kill`, with no killer attribution, left it at 1/2: unattributed death is attrition, not harvest. |
| `renewableAnimal`, re-run | **Pass on the positive case.** `ANIMAL_TARGET_PRESENT` confirmed, cow kill took the herd 1/3 to 0/3. `MONSTER_TARGET_PRESENT` confirmed, and the zombie kill took the **swarm** 1/2 to 0/2 while the herd stayed 0/3. |

### The hostile-versus-animal negative is still weak

The re-run shows a zombie decrementing the SWARM source rather than the ANIMAL
source, which is real evidence that routing by type works. But the herd was
already at 0/3 when the zombie died, so "the zombie did not decrement the herd"
could not have been observed either way: nothing can go below zero.

A clean negative needs a hostile killed inside an ANIMAL source that still has
availability, with no SWARM source present to absorb it. That case is **not yet
covered**. The positive routing evidence is what currently carries this claim.

## The invariant

`grantedByRenewal` stayed 0 across every run, and the recovery scenario prints
the player inventory on both sides of a recovery: empty before, empty after.
Availability returned; the player received nothing. That is the difference
between a regenerative world and passive income, now demonstrated rather than
asserted.

## Two real defects these scenarios found

**Event ordering made every farm look wild.** `Provenance` clears a block's
placed mark in its own `MONITOR` handler and is registered first, so a `MONITOR`
read in `Renewables` saw every player-placed block as unmarked. The farm
scenario failed with the patch draining 2/5 to 0/5 on the player's own crops.
Fixed by reading at `EventPriority.HIGHEST`, before `Provenance` reclaims.

This is a cross-cutting constraint, not a local fix: **any system that asks
whether a block was player-placed during a break must run before `Provenance`
reclaims the mark.** Five documented systems depend on that question.

**Overlapping sources shadowed each other.** The lookup found the first source
in a volume and then filtered by type, so a crop patch registered first hid a
co-located animal population entirely. Found by writing the fixture, not by
reading the code. Fixed by matching type during the search.

## Scenario defects worth recording separately

Three runs produced no usable evidence and none of them were plugin faults: a
mineflayer chat race when scenario names were queued before the bot finished
spawning, farmland trampled back to dirt by the player walking on it so seeds
would not place, and a keepalive timeout during an over-long idle settle that
left later steps executing against a dead connection. That last one surfaced as
`must be holding an item to place`, which reads like a scenario bug and is not.

An earlier run reported `grantedByRenewal=0` with **zero sources loaded**,
because a config edit silently no-opped and reported success anyway. An
invariant that cannot be violated because nothing is running is not evidence.
The fixture writer now asserts both that its target exists and that its write
took effect.

## Not covered

Persistence across server restart is untested. Day/night Swarm composition,
depth and value gradients, species tables, respawn cadence, XP and drop scaling,
ownership and contest rules are all content and remain `[OPEN]` in canon. The
`SWARM` type has no scenario: no fixture source of that type exists yet.
