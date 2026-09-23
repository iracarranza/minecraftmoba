# Regenerative Sources — plugin seam

**Historical seam snapshot; implementation clarification, 22 September 2026.**
The original empty-table/lazy-availability model below records intent at this
commit, not today's entire runtime. Main now supplies Alpha fixture sources,
Opportunity Regions, scheduled lifecycle recovery, terrain-queried manifestations
and membership/provenance. `RenewableKinds` still deliberately carries no
depth/value/region; source capacities/recovery and temporal/eligibility defaults
are fixtures, not a generic Strategic Depth × Regional Character policy.
See [current implementation audit](../audit/2026-09-22-spatial-doctrine.md).

**Working / Prototype-test, 19 September 2026. No content, no balance.**
This follows SPEC §8's rule: provide the seam, not the content. Source tables
ship **empty**, exactly as `rewards.levels` does.

## What canon already settles

From maps.md §"Finite and regenerative world opportunity" and objectives.md §17B:

- **Regeneration restores world opportunity, not player income.** It does not
  place periodic resource payments in inventories or storage. Search, harvest or
  combat, inventory decisions, danger and delivery remain necessary.
- Exploitation reduces local availability; renewal later restores *opportunity*.
- Known ecology does not imply exact current location, readiness, convenient
  access, ownership, or guaranteed successful acquisition.
- Three buckets: crops and plant resources, animal populations, hostile Mob Swarms.
- Do not sprinkle regenerative nodes onto empty cells to satisfy a percentage.
  Identify real candidate opportunities and observe the eligible subset.
- An ordinary replanted farm, a Production process, an objective-generated finite
  deposit and a naturally recurring opportunity remain **distinguishable**.

## The invariant this seam exists to prove

> **Renewal grants nothing to anyone.**

There is no inventory, XP or currency write anywhere in the renewal path, and a
counter asserts it stays zero. This is the difference between a regenerative
world and passive income, and it is the one thing worth proving before any
species table, cadence or value is chosen.

## Model

A `Source` is a located opportunity with an availability state, not a spawner.

```
Source { id, type ∈ {CROP, ANIMAL, SWARM}, world, origin, radius,
         capacity, available, recoveringUntilTick }
```

- **Harvest** is a player action inside the volume on a *natural* target.
  Crops use block provenance to separate a wild patch from a player's farm, which
  is why this depends on the Phase 1 provenance result rather than assuming it.
- Harvest decrements `available`. At zero the source is depleted.
- **Recovery** restores `available` when the clock passes `recoveringUntilTick`.
  It restores availability only: nothing spawns into a player's possession.
- Recovery is evaluated **lazily on access**, so cost scales with interaction
  rather than with the number of sources in the world.

Persistence is the chunk PersistentDataContainer, matching `Provenance`, so state
survives chunk unload and restart without a separate store.

## What ships empty, deliberately

`renewables.sources` is an empty list. The plugin registers sources it is *given*;
it does not generate them. Canon forbids sprinkling nodes to hit a density
target, and the depth gradient, regional tables, species, cadence, XP and drop
scaling are all recorded `[OPEN]`. A generator here would be inventing exactly
the content canon says is unresolved.

## Measurements are the deliverable

Per SPEC §7's framing: bytes per chunk, harvest-event tick cost, recovery
evaluations, depleted-source count over a session, and `grantedByRenewal`, which
must remain zero.

## Explicitly not in this seam

Day/night Swarm composition; spatial depth or value gradients; species tables;
respawn cadence; XP values; drop scaling; ownership or contest rules; whether
exploitation by one team affects another's availability. All `[OPEN]` in canon.
