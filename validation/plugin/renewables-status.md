# Regenerative Sources — acceptance results, 19 September 2026

Fixture: Paper 1.21.11, acceptance server port 25576, fixture-only sources
`fixture_patch` (CROP, capacity 5) and `fixture_herd` (ANIMAL, capacity 3) at
the same coordinates, which is the ordinary case for a wild meadow.

| Scenario | Result |
| --- | --- |
| `renewableHarvest` | **Pass.** Three wild wheat breaks took the patch 5/5 to 2/5, `harvests=3`. |
| `renewableFarmNotWild` | **Pass.** Player-placed wheat confirmed present, then confirmed broken, with availability 5/5 unchanged and `harvests` unchanged. |
| `renewableRecovery` | **Pass.** Depleted to 0/5, clock advanced, restored to 5/5. Inventory `[]` before and after; `grantedByRenewal=0`. |
| `renewableAnimal` | **Pass.** Player-killed cow took the herd 3/3 to 2/3; a zombie killed in the same volume changed nothing. |

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
