# Realized-map measurement export

Everything in `data/` is measured off the actual generated world for seed
**930012642**, volume `tv_ef56852eda10acc88342e5ee`. None of it is invented
geography. Each file carries its own `schema`, `evidence_state` and
`not_covered` fields — **read `not_covered` before using a file**, because it
says what the measurement does not establish.

Playable bounds (inclusive `xmin, xmax, zmin, zmax`): `[-2480, -1617, -528, 527]`
— 864 × 1056 blocks. Homelands: north `[-2172, -460]`, south `[-2020, 428]`.

## Files

| File | What it is | Grid |
|---|---|---|
| `travel-matrix.json` | Terrain-weighted travel cost between all 19 named sites | 8-block samples |
| `map-manifest.json` | 66 resource manifestations + 8 destinations, calculator §17 schema | 48-block cells |
| `caves.json` | Cave volume, connectivity, exposed/buried ore, per-layer histograms | 48-block cells |
| `opportunity-map.json` | Ore, vegetation, fauna, hostiles, biomes, farmable surface | 128-block cells |
| `packing.json` | Whether 54 demanded consumers fit the map (they do) | 128-block cells |
| `structures-built.json` | The 8 team structures as actually built, block-exact | — |
| `structure-siting.json` | Why those sites, with symmetry gaps | 8-block samples |
| `vanilla-constants.json` | Hardness, tool speeds, break-time formula, Fortune, drops | — |
| `benchmark-182-worked-example.json` | A worked extraction-time run, for cross-checking | — |

## How to join them

- `map-manifest.json` → `travel-matrix.json` via `manifestations[].travel_site_id`,
  which matches `sites[].id`. This is the intended link; do not match on
  coordinates, because the manifest stores cell centres and the travel matrix
  snaps to the nearest 8-block grid sample (`node_offset_blocks` records by how
  much).
- `structures-built.json` → `travel-matrix.json` via `{team}_{structure}`.
- Cave cells and opportunity cells are **different grids** (48 vs 128 blocks)
  and different scans. They are not row-aligned; relate them by world
  coordinates, not by index.

## Units and conventions

- Coordinates are Minecraft world x/z. `y` is vanilla, sea level 63.
- Travel `cost_matrix` is in **terrain-weighted blocks**, not seconds and not
  straight-line distance: the weighting inflates for rise, water, non-buildable
  ground and canopy, with the exact parameters in `cost_model.parameters`.
  Dividing by 4.317 (walk) or 5.612 (sprint) gives a travel-time **proxy**.
  That conversion is an assumption, flagged in the file's `not_covered`.
- Ore counts in `opportunity-map.json` are **sampled every 3rd block and not
  scaled up** (`counts_are` says so). Cave-scan counts in `caves.json` are
  exhaustive within their cells. Do not mix the two as if they were the same
  measurement.
- Cave scan covers y −60..90; opportunity map covers y −64..100.

## Coverage — the honest part

- The cave scan covers **9 cells of 48×48**, not the whole map. Its totals
  describe those cells. Per-cell variation is large (explorable cave volume
  ranges from ~5,000 to ~107,000 blocks), so treat any single cell as a sample.
- The opportunity map covers 63 cells of 128×128 across the playable area.
- **Hostiles read as zero everywhere.** That is a snapshot artefact — entities
  were counted from a freshly generated world — not evidence that the map has no
  hostile pressure. Any mob-related modelling needs a spawn-eligibility
  substrate that does not exist yet.
- Authored crops read as near-zero (`wheat: 1`, `potatoes: 3`) because crop
  patches are authored content that has not been placed yet, not because the
  map lacks farmland. `farmable_surface_samples` per cell is the real signal.
- There are **no measured underground routes**. `route_from` is null in every
  manifestation. The travel matrix is a *surface* graph; it does not model
  descending into a cave, and cave interiors have no traversal model at all.

## What is not in here, and must not be invented

The simulation still needs these, and none of them can be derived from this
bundle:

1. **Cave traversal rate** — how much cave volume a moving player actually
   sweeps per second. Not measured. The worked example treats it as a declared
   parameter and reports results across a 150/300/600 blocks-per-second band
   rather than picking one. Do the same.
2. **Descent time** from surface to a cave's ore-bearing depth.
3. **Search time** to find a deposit that is not yet known.
4. **Hostile interruption** underground — see the zero-hostiles note above.
5. **Worksite activation and capitalization state.**
6. **Contested ground.** Every cost in the travel matrix is the same for both
   teams and assumes nobody is in the way.

If the simulation needs one of these, it should surface as an explicit
parameter with a reported sensitivity band — not as a number folded into a
result.

## Worked example to check against

`benchmark-182-worked-example.json` extracts 182 units using only mining and
in-cave travel. Reproducing its numbers from `caves.json` +
`vanilla-constants.json` is a good check that the join and units are right.
Two results worth knowing before you start:

- Caves beat branch mining for copper and iron across most of the sweep band.
- There is **not enough exposed diamond in the 9 scanned cells to reach 182 by
  cave exploration at all** — the run reports `SHORT BY 102` rather than a
  time. A diamond benchmark currently forces branch mining at 58–102 minutes,
  which exceeds the match. Expect that to matter.

These figures are a **lower bound**: real extraction is slower by exactly the
travel, search and logistics listed above as not covered.
