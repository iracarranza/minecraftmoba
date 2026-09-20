# Authored portfolios — the four playtest finalists

Status: **Authored and verified; rescan outstanding**
Date: 20 September 2026

Implements steps 1 and 5 of *Next physical validation* in
[the optimizer run doc](2026-09-20-map-authoring-optimizer-run.md).

## What was authored

`terrain_harvest/author_portfolio.py` takes one finalist from the optimizer's
scenario frontier and writes it into a world. The optimizer has already chosen
which 128×128 regions, with a declared crop per founder patch and species per
renewable, scored as a portfolio — so nothing is re-decided here. This step only
does what the run doc defers to it: choose exact terrain, then report what did
not work rather than substituting.

| Profile | Finalist | Sites | Blocks | Balance asymmetry |
|---|---:|---:|---:|---:|
| Balanced Baseline | rank 0 | 32 | 178,358 | 0.0403 |
| Exploration-Centric | rank 0 | 32 | 178,358 | 0.0265 |
| Consolidative | rank 0 | 32 | 178,358 | 0.0685 |
| Resource-Light | rank 0 | 28 | 156,398 | 0.0182 |

All four author at **rank 0** — the optimizer's own preferred finalist — with
zero skipped sites. Identical block counts across the first three are expected,
not a bug: they place the same mix of site kinds (4 founders, 8 renewables, 10
worksites, 4 POIs, 6 Route targets) from fixed-size templates.

A superseded count-based profile path was removed. It modelled a profile as
overrides on `packing.CONSUMERS`, which is the wrong shape: the optimizer
searches complete portfolios because "a good individual placement can still
create a bad map when combined with other opportunities." Its massing templates
survive as `terrain_harvest/massing.py`.

## Two bugs the verification caught

**Sites stacked on each other.** A portfolio can put several sites in one cell,
and the first version placed them all at the cell centre, so each silently
overwrote the last. The readback found it as wrong blocks at a crop patch. Fixed
by dividing a shared cell into an n×n slot grid sized to the number of sites —
greedy packing from the centre outwards does not work, because the first site
takes the middle and leaves nowhere for the rest.

**Site centres outside the world.** The first apply run failed with
`chunk -145,35 absent; refusing to invent terrain`. The guard was right. The
root cause was upstream: the opportunity map emitted an entire edge row of cells
that are only 25% inside the harvested volume, and reported their *geometric*
centres — at z=560, past the volume edge at z=527.

Fixed in `opportunity_map.py`, which now records `coverage` per cell and a
`sampled_centroid` computed from the columns actually inside the volume. 15 of
63 cells are partial. Before the fix, no Exploration-Centric finalist could be
authored at all and the others needed fallbacks as far down as rank 9; after it,
all four author at rank 0.

That is worth stating plainly: **the fallback machinery was compensating for a
measurement bug.** It is still there, and still correct to have, but it should
now be rare rather than routine.

## Verification

`terrain_harvest/verify_portfolio.py` reads the authored worlds back out of
their region files: **270 assertions across 124 placements, 0 failures**. It
checks the actual block at each site — water at a crop centre, the right crop on
farmland inside vanilla's 4-block hydration range, an open worksite shaft, a lit
POI pillar, a Route cairn — plus that no two placements share a position, which
is the collision bug turned into a standing check.

One earlier failure was in the checker itself: reading `x+2` from the chunk
looked up for `x` silently crosses a chunk boundary. The check now resolves a
chunk per coordinate.

29 unit tests cover allocation, preflight and template selection.

## Not done

- **Routes are targets, not paths.** Only the waypoints are marked.
- **Animals are not written.** They are entities, manifested at runtime.
- **No rescan yet.** This is step 2 — the authored worlds still need their exact
  coordinates and path costs measured, replacing the regional reach
  approximation, whose leave-one-out error the run doc puts at 7–8%.
- **Hostile exposure remains UNRESOLVED**, and the zero-hostile snapshot must
  not become a zero-hazard assumption.
- **Nothing is frozen.**

## One discrepancy to resolve

Re-running the optimizer against the export gives substantially more balanced
configurations than the committed run doc records — 192–395 versus 74–158, at
the same seed and sample count. Same script, so the difference is the input.
Worth confirming which export the documented figures came from before either
set is quoted.
