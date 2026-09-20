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

## Audit against the full authoring handoff

After chat pushed `claude-authoring-handoff.json`, `CLAUDE.md` and the optimizer
report, every authored placement was re-checked against each contract clause
that code can enforce. 19 checks, 1 conflict.

Passed: renewables use only species observed in their own cell, across all four
profiles; founder stock is carrot/potato only, all in farmable regions;
Worksites stay generic with no Mining Site or Industrial Factory binding; no
movement-speed effect anywhere; no module reads hostile counts, so the forbidden
`hostiles == 0 -> safe` inference cannot enter through the authoring path; all
124 placements sit inside their prescribed cells.

Failed, and redone: **Routes.** The contract says *author physical path
quality*, and the design document evaluates Route targets by how path quality
changes Practical Reach. The first pass wrote cairns at the targets and no path,
which is a marker, not a Route.

`terrain_harvest/routes.py` now authors corridors -- 3 wide, `dirt_path` over
land, a plank deck over water, 3 blocks of headroom cleared -- following the
same terrain-weighted graph that sited the team structures, so a Route runs
where the analysis already said the cheapest crossing is. Six routes per
profile, 2,822-3,230 columns, 37,204-42,396 blocks. No speed is granted; what
the Route does to Practical Reach is for the rescan to measure.

A corridor crossing an authored site is recorded rather than diverted. Diverting
would alter the strategic geometry the optimizer selected, and the design
document values POIs partly for the traffic they attract. Three crossings per
profile, listed in each Route report. The first verification pass found one as a
POI foundation replaced by path; it is now declared instead of discovered.

## Verification

`terrain_harvest/verify_portfolio.py` reads both passes back out of the region
files: **346 assertions across four worlds, 0 failures**. A crossing recorded in
a Route report is accepted; an unrecorded one fails.

35 unit tests, including contract clauses expressed as tests.

## Not done
- **Animals are not written.** They are entities, manifested at runtime.
- **No rescan yet.** This is step 2 — the authored worlds still need their exact
  coordinates and path costs measured, replacing the regional reach
  approximation, whose leave-one-out error the run doc puts at 7–8%.
- **Hostile exposure remains UNRESOLVED**, and the zero-hostile snapshot must
  not become a zero-hazard assumption.
- **Nothing is frozen.**

## Provenance: three frontiers, none agreeing

Three artefacts now describe a scenario frontier for this seed, and no two match
on any of the eight profiles. Recorded as data in
`reports/expedition_2026-09-20/frontier-provenance-audit.json`.

| Profile | Handoff `reference_run` | Committed frontier | Committed optimizer |
|---|---:|---:|---:|
| Balanced Baseline | 114 / 0.0474 | 300 / 0.0376 | 282 / 0.0403 |
| Exploration-Centric | 116 / 0.0672 | 259 / 0.0231 | 272 / 0.0265 |
| Consolidative | 115 / 0.0935 | 266 / 0.0231 | 307 / 0.0685 |
| Resource-Light | 74 / 0.0616 | 220 / 0.0334 | 192 / 0.0182 |

The committed frontier settles which is authoritative, in its own
`generator_note`: *"Equivalent regional search generated in-repository handoff
pass; optimizer Python remains authoritative implementation."* It was produced
by a reimplementation and defers to the committed optimizer.

**These worlds were authored from a rerun of the committed optimizer**, which is
what the handoff's own `reproducible_run.commands` instruct. That run is
byte-identical across repeats, and its candidate space matches the handoff's
expected counts exactly (74/110/35/35/32/31) — so the divergence is confined to
scoring, not to candidate construction.

Every portfolio and Route report now records `frontier_sha256` and
`finalist_sha256`. With three disagreeing frontiers in circulation, a world has
to name its own source rather than have it inferred.

### Two defects in the pushed artefacts

- `candidate-catalog.json.gz` **fails CRC**. It decompresses 133,311 bytes, then
  dies mid-object, and the recovered JSON does not parse. The git blob matches
  the file on disk, so it was committed broken. A rerun of the optimizer
  regenerates it correctly.
- `reference_run`, the run doc and `optimizer-report.md` all quote the same
  figures, which no committed code produces. `CLAUDE.md` step 3 asks a reader to
  confirm against them.

Neither is mine to fix: replacing chat's artefacts is chat's call.
