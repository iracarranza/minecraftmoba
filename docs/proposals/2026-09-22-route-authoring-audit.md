# Route authoring — audit and proposed correction

Status: **audit and proposal.** No generation change made. The Alpha map is not
re-authored or re-frozen.

---

## A. What the algorithm does

All of it is in `implementation/worldgen/terrain_harvest/routes.py`, 240 lines.

**Centreline selection** — `author()`. Uses the terrain-weighted graph that sited
the team structures: `shortest(t.adj, {home: 0})` then `path_to`. This part is
already terrain-aware and is **not** the problem. It returns graph nodes, which
are coarse feature-grid samples, 36–93 nodes per route.

**Densification** — `densify()`. Linearly interpolates between consecutive graph
nodes **in plan view only**, one column per step. Terrain between two nodes is
never consulted, so the corridor is a straight line at sub-node scale.

**Height selection** — `carve()`, and this is the defect. For each of the nine
columns in a 3×3 around every centreline point, it calls `column()` for that
column's own topmost block and writes at that y. **Every column snaps
independently to raw terrain.** There is no profile, no smoothing, no relation
between neighbours.

**Width** — `HALF_WIDTH = 1`, applied as an exact 3×3 at every step. Not
approximate.

**Surfacing** — every non-water column becomes `dirt_path`; every water column
becomes `oak_planks` at the water's surface y.

**Headroom** — `for dy in 1..3: editor.set(cx, y + dy, cz, AIR)`, unconditional,
relative to each column's own top, for all nine columns.

**Obstacles** — there is no obstacle handling. `crossings_of()` only *records*
authored sites the corridor passes through; it never diverts. Trees, cliffs and
holes are not detected at all.

## B. What that produced, measured

Every route column in the frozen map (9,597 of them), comparing each to its
orthogonal route neighbours:

| step between adjacent route columns | pairs | share |
|---|---|---|
| 0 blocks (walkable) | 9,019 | **59.2%** |
| 1 block (requires a jump) | 5,040 | **33.1%** |
| 2 blocks | 675 | 4.4% |
| 3–9 blocks | 455 | 3.0% |
| 10–34 blocks | 35 | 0.2% |

**40.8% of steps along a Route require a jump. 7.6% cannot be climbed at all.**

The tail is the damning part. A 2-block step is impassable without pillaring;
there are 675 of them. There are steps of 16, 18 and 34 blocks — places where the
corridor ran over a cliff edge and laid path blocks down the face, because each
column independently took its own surface height.

So the Route is not merely uneven. It is **discontinuous**, and in places the
authored path is the least traversable line available.

## C. Which rule produced which symptom

| observed | cause |
|---|---|
| parkour-like traversal | per-column independent height snapping in `carve()`; raw terrain noise becomes a jump every ~2.5 columns |
| impassable steps, path down cliffs | same, plus no gap/drop detection anywhere |
| elevated, artificial appearance | exact 3×3 surfacing of *every* column — a continuous replacement surface, not a fitted path |
| trees bulldozed | unconditional 3-block headroom clear over all nine columns, relative to each column's own top |
| broad plank platforms | every water column decked at 3 wide, with no regard to crossing length or shore geometry |
| ambiguity about where the Route goes | the corridor is uniform everywhere, so nothing distinguishes the line from its surroundings except material |

One subtlety worth recording: `dirt_path` is 15/16 of a block tall. It does not
cause the jumps — a 1-block rise needs a jump regardless, since player step
height is 0.6 — but it means a path block is always slightly *sunken* relative
to neighbouring full blocks, which is part of why the corridor reads as a trench
rather than as a trail.

## D. Proposed change, narrowest first

The centreline and the graph stay. `densify` stays. **The change is confined to
`carve()` plus one new pass before it.**

### 1. A step-limited walking profile (fixes 40.8% → near zero)

Instead of nine independent heights per step, compute one profile `y[i]` along
the centreline, constrained so `|y[i+1] − y[i]| ≤ 1`, fitted to the raw surface
by least deviation. Columns then take their height from the profile, not from
themselves.

This is the single change that removes the parkour, and it is about twenty lines.

### 2. Classify each segment by how far the profile sits from raw terrain

This is where the brief's three treatments fall out naturally, from a number
already computed:

| deviation | treatment | action |
|---|---|---|
| 0 | **WORN** | leave the terrain; surface selectively for legibility |
| 1–2 | **ASSIMILATED** | fill or shave locally with a natural material |
| 3+ | **CONSTRUCTED** | bridge, stair or boardwalk — geographically earned |

Today everything is treated identically, which is exactly why constructed-looking
surface is the default representation.

### 3. Surface selectively rather than continuously

Place `dirt_path` at a variable density — heavier where the line would otherwise
be ambiguous, sparse on obvious ground, absent where existing gravel or dirt
already reads as a trail. Vary half-width between 0 and 1 with slow lateral
drift instead of writing an exact 3×3 every step.

### 4. Headroom only where a player's body is

Clear two blocks at the **profile** height, not three above each column's own
top, and only across the usable width. Leaves and branches above head height
survive. A trunk inside the corridor triggers a lateral nudge of the centreline
before it triggers removal.

### 5. Water by crossing geometry

Measure the crossing before treating it: shallow and short → ford, leave it; short
and deep → a 1-wide bridge; long → a boardwalk. The constructed material then
communicates that *this* obstacle needed construction, which a uniform 3-wide
deck over every puddle does not.

## E. Verification before any re-freeze

The brief requires traversability testing on representative land slope, rough
terrain, forest, and water crossing. The step distribution in §B is the natural
acceptance measure, and it can be computed on the authored world without a
server:

- **no step ≥ 2** anywhere along a Route, ever;
- **1-block steps below a stated share** of adjacent pairs;
- tree removal counted and bounded;
- plank columns counted and bounded.

That turns "does it feel like parkour" into a number that can fail a build. The
same measurement already produced §B, so the harness exists.

## F. Implemented and measured, 22 September

`walkable_profile` now fits a single height sequence to the centreline: the raw
ground is median-filtered over seven columns, then step-limited to one block.
Both halves are load-bearing. Limiting alone still permits a jump at *every*
column, which on ordinary noisy ground is worse than the map has today; the
median removes the wobble and leaves a genuine slope untouched, because a slope
is the median of itself.

`carve` then takes each column's height from the profile rather than from
itself, fills hollows with coarse dirt, shaves protrusions, decks only actual
water, surfaces selectively rather than continuously, drifts its width, and
clears headroom at the walking height instead of three blocks above every
column's own top.

### Against the real map

The same statistic as §B, computed over the actual Route lines, with the new
profile fitted to the **base terrain** the corridors were cut into:

| | adjacent pairs | requires a jump | unclimbable | worst step |
|---|---|---|---|---|
| frozen map today | 1,322 | 35.9% | **6.3%** | **16 blocks** |
| profile-fitted | 1,322 | **23.5%** | **0.0%** | **1 block** |

Unclimbable steps are gone outright, and jumps fall by a third. 23.5% is not
zero and is not meant to be: these transects cross real slopes, and a slope
legitimately steps. The brief's failure condition is repeated interruption, not
elevation change.

### What it costs in terrain

| | mean deviation from natural ground | worst |
|---|---|---|
| old authored | 0.34 blocks | 16 |
| profile-fitted | 0.43 blocks | 9 |

**The corrected Route modifies more ground on average and much less at the
extremes**, which is the intended trade: it stops following cliffs down and
starts making up small hollows. Treatment mix along real lines:

| treatment | share |
|---|---|
| worn — terrain is the Route | **72.7%** |
| assimilated — local fill or shave | 23.5% |
| constructed — genuinely earned | **3.8%** |

Constructed treatment was 100% of the old representation. It is now under four
per cent, which is the point of the priority order.

### Re-authored, 22 September

Authored from the base terrain with the corrected code, as configuration
`consolidative_v2`. Sites reproduced exactly -- 32 placements, 178,358 blocks,
the same as the frozen run -- so the only change is the corridors.

Measured along the centreline, which is the walk a player actually takes and is
now recorded by the authoring itself:

| | steps | requires a jump | unclimbable | worst step |
|---|---|---|---|---|
| frozen map | 15,240 | 40.8% | **7.7%** | **34 blocks** |
| re-authored | 2,816 | 25.1% | **0.0%** | **1 block** |

Corridor work fell from 42,396 blocks to 33,452, and its composition inverted:

| | old | new |
|---|---|---|
| surfaced columns | 24,068 | 13,235 |
| filled / shaved | 0 | 3,938 / 5,484 |
| worn | 0 | 12,654 |
| assimilated | 0 | 8,040 |
| constructed | 0 | 1,448 |

Nearly half as much surfacing, because terrain that was already walkable is now
left as the Route instead of being replaced.

**A caveat about the earlier figures.** §B counted steps between adjacent route
COLUMNS, which over-reports: two corridor segments passing each other on a
hillside are adjacent without anyone stepping between them. On that same basis
the re-authored map is 20.1% jumps and 1.7% unclimbable, better than the frozen
map's 40.8% and 7.7% but not zero. The centreline figure above is the honest
one, and the authoring now records it so it cannot drift unmeasured.

### Still not done

Not re-frozen. `consolidative_v2` is a catalogue entry drawn alongside the
others, and the frozen map stays in the catalogue as a control. Tree avoidance is
reduced to headroom-at-walking-height rather than a lateral nudge of the
centreline, and water treatment does not yet vary by crossing length -- both are
in §D and neither is implemented.
