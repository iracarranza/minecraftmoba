# Map inspection overlay

**Prototype/test. Built 10 October 2026.** The compiler certifies a map against measurements
the player never sees: the two team cost fields C_N and C_S over the analysis cells, the bands
derived from them, and the field points placed inside those bands. This draws them in a
launched lab scoop, so the Opportunity Fields work (`docs/reconciliation/2026-09-29-opportunity-fields.md`)
can be looked at rather than inferred from JSON.

```
/moba lab overlay                      start (a launched scoop with inspection data)
  cells | cost | relation | bands | points | landmarks | team | report | off
```

Reached from the hub's Map overlay pedestal (copper block, needs a launched scoop), the
Benches page, or the command. Everything inside is on the hotbar; Leave is in the inventory.
It is a VIEW onto a real scoop, so there is no deck of buttons.

## What it draws

| Layer | Drawn as | From |
|---|---|---|
| Cell grid | white border particles at the ground | the cell origins and the 64-block size |
| Cost field | a coloured column over each cell, tinted by ONE team's cost, labelled `N 44 \| S 615` | `cell_grid`'s `strategic_depth_cost` |
| Relation | the same columns tinted by who the cell favours | `reach_fields.relation` |
| Band edges | particles on the borders where near / farther / deeper changes for the chosen team, coloured by the band on the cheaper side | `reach_fields.bands`, which reuses `portfolio`'s own rule |
| Field points | a beam and a label per regenerative source, coloured by type | the runtime bindings |
| Landmarks | Fountains, Objectives, the Lair, Worksites | the runtime bindings |

Cost colours are anchored on the map's own opening cost, so "within the opening" (the
compiler's *near*) is always the cheapest colour whatever the map's spread. The buckets beyond
it are multiples of it and a display choice, not a band; the authoritative bands are drawn
separately. Only what is within 192 blocks of the tester is drawn (borders within 80), and a
redraw adds and removes only what entered or left, so a 238-cell map costs about 60 entities
at a time.

## Nothing is recomputed

The overlay reads a sidecar, `inspection.json`, written beside `map.json` at publish time by
`terrain_harvest.inspection`, from the same compilation that certified the map. The Java side
parses and draws; it derives nothing, so a second implementation of "near" cannot drift from
the first. The sidecar is not part of the world, so it does not touch the world fingerprint. A
scoop without one is refused with how to make one. Scoops published before this existed get
theirs from `terrain_harvest.inspection_sidecar`, which recompiles the same harvested candidate,
picks the run matching the published Fountains, and writes the file (it compiles in a temporary
directory and changes neither the world nor `map.json`).

## What it shows about the first three scoops

Reading the sidecars of the three lab scoops (238 cells each) is already informative, and
none of it is a defect; these are labelled non-canon fixtures:

| Seed | "equal" cells | cheap for one team | north band: near / farther / deeper |
|---|---|---|---|
| 910003 | 194 (81%) | 5 north, 3 south | 5 / 116 / 117 |
| 910004 | 119 (50%) | 4 north, 3 south | 4 / 117 / 117 |
| 910006 | 96 (40%) | 2 north, 4 south | 2 / 118 / 118 |

The **near band is 1 to 2% of a map**, because the opening cost (120) is small against costs
that run into the hundreds, so the bands that matter are almost all *farther* and *deeper*.
The **equal-reachability tolerance** (25% of the combined cost) makes a large share of cells
read as equal, up to four in five on 910003. Whether those are the right values is exactly the
kind of question the overlay exists to put in front of a person; it does not answer it.

## Defect found by running it

Non-persistent entities are discarded when their chunk unloads, and a column 150 blocks from
the tester stands in a chunk nobody is near. The first live run drew 58 entities and found 18.
The overlay now holds the chunks its entities stand in (reference-counted, released as cells
leave range), and the live check asserts every drawn entity still exists. The same defect had
already been found and fixed in the legibility bench; it is the third place the pattern has
appeared.

## Verification

Parsing (nulls, missing bands, wrong schema), radius selection, cost and relation colours (every
name is a real material and meanings that differ have distinct colours), cell and band-edge
geometry, the menu and its refusals, and the Python sidecar (rule shared with `reach_fields`,
fingerprint unaffected): unit-tested. Live, on a launched real scoop: the overlay starts with
the hotbar and the parsed data; the cost layer draws a column and label for exactly the 29 cells
in range, all still alive; a column sits where the data says and is tinted by the north cost,
then retinted by the south cost on a team switch; the relation layer tints by relation; field
points draw a beam and label each; landmarks add six; the report gives counts from the data;
and switching layers off, then the overlay off, removes everything and restores the hotbar.
**Not verified:** that the particles (grid and band edges) render, that anything looks like
what it should from a client, and whether the legend is readable. The overlay is for that.
