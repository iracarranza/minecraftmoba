# Opportunity bench

**Prototype/test. Built 9 October 2026.** A lab bench for the regenerative systems: watch a
Herd, a Crop Patch or a Swarm manifest, be resolved and recover; see WHY a manifestation can
or cannot happen; force one. It exists because those systems are lifecycles whose interesting
properties are transitions, and the part that touches the world (choosing a locus, spawning,
harvest detection, recovery on the world's own clock) had only ever been unit-tested in its
pure core.

```
/moba lab opportunity           enter
  plot | spawn [radius] | skip | manifest | harvest one|all | base | time | show | report | leave
```

Hotbar, a deck of eleven world buttons, or typed; Leave is in the inventory (`BenchUi`).

## What it drives

The real `Renewables` runtime: sources are registered with `createRuntime`, and the
runtime's own `manifest`, harvest listeners, membership sweep and lifecycle ticker do the
rest. Nothing is simulated beside them. Two shortcuts the real game cannot offer:

- **skip** advances the opportunity's recovery progress to complete at the CURRENT rate, so
  the report still says what day or night would have cost (a recovery is a rate, not a timer).
- **base** builds a plank floor over the region, marked player-placed, to show Development
  displacing a manifestation.

## The venue

A 97 by 101 platform: plains in the west, a mountain biome from x = 15 east, so a
biome- and time-gated swarm can both manifest and be refused. Three plots: Herd (sheep, west),
Crop Patch (wheat, centre), Swarm (mountain ravager, east, daytime only). The viewing
station is at least the 24-block player-exclusion distance from every plot, because a
manifestation is never placed that near a player and a tester standing in a plot would
otherwise make the bench report "no eligible locus" for the wrong reason.

## Why it can say why

`EligibilityReport` walks the same columns as `Eligibility.loci` and `select` and counts what
each was rejected for: unloaded, wrong ground, player-placed, no headroom, then too close to
the previous site or to a player. That is a second copy of the rule, which is the repeated
defect here, so tests assert its counts agree with `Eligibility.loci` and `select` across 25
random terrains; if the rule changes and the report does not, they fail.

## What it found (9 October 2026)

Three defects in the existing runtime, each found by running it:

1. **The "keep a new manifestation away from the last" rule never applied.**
   `Opportunity.memberRemoved` cleared `locus` on resolution without remembering it, and
   `manifested` then set `previousLocus` from that null. `Eligibility.select` therefore never
   had a previous site, and a region could decay into a camp coordinate. **Fixed** on this
   branch (the site becomes the previous one on resolution and is not overwritten), with unit
   tests and a live check (second manifestation 17.9 blocks from the first).
2. **Region and radius disagreed, and the sweeper used the radius.** Eligibility chose a site
   anywhere in the Opportunity Region (a 48-block default half-span), but the membership sweep, harvest
   scan and `membersOf` judged "still in the wild population" by the source's `radius` cube (20 in every
   Alpha source). Members placed outside the cube were swept as "left the region" and depleted the
   opportunity within one 200-tick sample, nobody having harvested anything. About 82% of the Alpha
   region was outside the cube. **Resolved 10 October 2026: the radius is authoritative** (the 48 was a
   default, not an authored extent). A source with no authored region cells now gets its radius as its
   region, pulled in by a placement margin (the larger of a patch's spread and half a herd's cluster, 6
   by default), so a site and its members are placed inside the cube they are judged by. See
   `docs/reconciliation/2026-10-10-opportunity-radius-authoritative.md`. Live: with a radius-10 source
   the site and all five members were placed inside the cube.
3. **`membersOf` and `count` scan the radius cube**, so they miss members that have strolled outside it.
   With the radius authoritative that is now the intended definition, but the bench still does its own
   world-wide membership scan so it can see and clean up a member that wandered out.

Also noted: a Crop Patch spreads past its locus, which the placement margin now accounts for.

**Still open: strolling.** Placement is now inside the cube, but a vanilla sheep wanders up to ten blocks
from where it stands, so a member near the edge of a small cube can walk out and is then revoked. A
radius-4 herd lost members this way in a live run. Whether to confine movement to the radius (a soft home
range, relaxed when a creature is led or aggroed) is a separate design question that interacts with
capture by leading and with unresolved swarm behaviors.

## Verification

Eligibility counts, the drift guard, the lifecycle refusals, the venue, the menu and the
`Opportunity` fix: unit-tested. Live (real server, real plugin): entry and set-up with both
biomes; spawn registering a RECOVERING source; refusals with reasons; skip then a five-sheep
manifestation 43 blocks from the tester; harvest one and all through the runtime's listeners;
a skip refused on a standing manifestation; a second manifestation displaced from the first;
building over the region blocking the next one with the report naming the ground; clearing and
forcing; a wheat patch of eight and a harvest; a ravager by day in the mountain biome, refused
at night with "NOT eligible" and forced by day; the radius/region sweep reproduced; nothing
granted by renewal; and clean leaving. **Not verified:** particles rendering (the eligible-site
display), and anything on a real client.
