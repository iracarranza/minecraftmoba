# Map profiles — authoring the playtest set

Status: **Pipeline complete; profile definitions provisional**
Date: 20 September 2026

## What a profile is

A profile is a variation in *economic demand* over the same map and the same
system rules — how many crop patches, livestock ranges, mining worksites and
POIs, how large, and how far apart. Those are exactly the knobs
`packing.CONSUMERS` already exposes, so a profile is a declared override of that
list plus an id. It is not a new placement algorithm and not new geography.

`terrain_harvest/author_profile.py` takes a profile, runs the packing fit, and
writes the placements into a world as greybox massing, reusing the same
`WorldEditor` that built the team structures — which refuses to invent terrain
rather than generating chunks that do not exist.

## Status of the four definitions

The playtest set is Balanced Baseline, Exploration-Centric, Consolidative and
Resource-Light. **Only the baseline is authoritative.** It is
`packing.CONSUMERS` unmodified. The other three are in `implementation/worldgen/
profiles/` with demand vectors derived from their names, and each declares:

> `CLAUDE INTERPRETATION FROM PROFILE NAME - pending the frontier definitions
> from the optimizer.`

A test asserts every profile declares a source, so a later reader cannot mistake
an interpretation for a finalist. Replacing the `consumers` array with the real
demand vector is the whole integration; the pipeline does not care which numbers
it gets.

## What the four currently produce

| Profile | Fits | Sites | Worksites | Minor POI | Crops | Livestock |
|---|---|---:|---:|---:|---:|---:|
| Balanced Baseline | yes | 44 | 6 | 10 | 12 | 8 |
| Exploration-Centric | **no** | 50 | 10 | 14 | 10 | 8 |
| Consolidative | yes | 32 | 4 | 6 | 8 | 6 |
| Resource-Light | yes | 23 | 3 | 5 | 6 | 4 |

### Exploration-Centric does not fit, and that is a measurement

It asks for 16 minor POIs at 160-block separation and the map places 14. The
reason is reported per instance: *no free cell with role "buildable" outside
this type's own separation radius*. **The map has a dispersion ceiling.** An
864 × 1056 playable area cannot hold arbitrarily spread-out layouts, so an
exploration-centric profile has to buy dispersion by reducing count, or accept
tighter spacing. That ceiling is worth knowing before the optimizer proposes a
frontier point that cannot be built.

The profile was left failing rather than tuned to fit, because tuning it would
have hidden the finding.

## Verification

The baseline was applied to a disposable world copy: **251,220 blocks written**,
then read back out of the region files — 27 assertions across 44 placements,
0 failures. The readback checks the actual block at each placement (water at a
crop patch centre, grass under a pen, dirt path under a village) and that wheat
sits on farmland inside vanilla's 4-block hydration range. Trusting the write
counter alone would not have caught a silently misaddressed write.

## Deliberate scope limits

- **Animals are not written.** They are entities, manifested at runtime by the
  plugin's existing Renewables authoring. Writing entity NBT offline would
  duplicate that with a second, unverified path.
- **Team structures are skipped**, and the report says so per instance. They are
  sited symmetrically by `vanilla_search.structures` and built by
  `build_structures`; authoring them again here would fight that.
- **Surface height is measured, not guessed.** The opportunity map now records
  `mean_surface_y` per cell. Before that it did not, and the authoring path
  refused to place anything rather than assuming a y — the correct failure.
- **Terrain shape within a cell is not modelled.** Packing treats a cell as
  uniform, so a placement can land on a slope that the foundation then flattens.
- **No Routes.** No profile declares them yet.

## Next pass

Rescan the authored worlds and replace approximations with measured values:
the opportunity map and cave scan both run against the authored world rather
than the bare one, `route_from` gets real distances from the travel matrix, and
the benchmark and 7v7 simulation rerun against each profile. The diamond result
from the extraction benchmark is the thing to watch — if 182 diamond is already
unreachable by exploration on the baseline, Resource-Light will be worse.
