# Implementation sequence for the six missing compiler seams

**Date:** 2026-09-23
**Status:** PLAN. Nothing implemented in this pass.
**Baseline:** `docs/audit/2026-09-23-compiler-against-doctrine.md`.

Machinery and calibration are separated throughout. **No depth, richness or
species table is proposed.** Where policy is open, the plan says what shape the
seam must have so the policy can arrive without re-architecting.

---

## Part 1 — The regenerative portfolio, traced end to end

### What exists

| piece | state |
| --- | --- |
| `Eligibility.loci(OpportunityRegion, TerrainView, Rules)` | **built and correctly shaped.** A query against the live world, not a table. Returns `Locus(x,y,z)` candidates inside a region. |
| `Eligibility.select(...)` | built. Chooses one locus per manifestation, fresh each time. |
| `OpportunityRegion.Cell(minX,minZ,maxX,maxZ)` | built. An **authored analysis cell**, unbounded in Y. |
| `Renewables.Source` | built: `id, type{CROP,ANIMAL,SWARM}, world, kind, x, y, z, radius, capacity, recoverTicks`. |
| manifestation lifecycle | built: `MANIFESTED / DEPLETED / RECOVERING`, physical placement, recovery. |
| `terrain_harvest/regenerative.py` | built. Runs the same predicate offline against region files, cross-checked against the plugin. |
| `opportunity_map.py`, `caves.py` | built. Ore by material, cave volume/depth/exposure, from real region files. |

### What is missing, exactly

1. **Nothing measures the compiled window.** `opportunity_map` and `caves` are
   never run on a map the compiler produced.
2. **Nothing derives Strategic Depth or Regional Character** as a per-cell
   classification of a realization.
3. **Nothing derives a portfolio.** Sources are authored by hand into
   `config.yml` against the frozen Alpha map.
4. **`runtime_bindings` has no `renewables` key**; `MapBindings` has no reader;
   `readiness.REQUIRED` does not ask for one.
5. `Renewables` loads from config only, and now refuses on a generated map.

### The smallest coherent path

Six steps. Steps 1–3 are new; 4–6 are one-line extensions of existing patterns.

**Step 1 — `characterize` stage.** After `verify`, run the existing
`opportunity_map` and `caves` over the compiled window. Emit into evidence:
ore by material with positions, cave components, exposure, and depth. *Pure
reuse; no new measurement code.*

**Step 2 — depth and character as a cell grid.** Partition the window into
`OpportunityRegion.Cell`s and attach to each:
  - **Strategic Depth** — team-relative cost from each Fountain. Reuse
    `expedition/travel.py`'s cost matrices; do **not** use radial distance,
    which `maps.md` explicitly rejects.
  - **Regional Character** — biome and terrain classification, already carried
    on the candidate.
  *Machinery only: the grid is emitted, nothing is judged.*

**Step 3 — `regenerative_portfolio.py`.** Given the cell grid and the measured
occurrence, produce a list of authored opportunities:
`{id, type, kind, cells, capacity, recoverTicks}`.

  The eligibility question it asks is **"is this characterized geography
  eligible for this kind at this richness"**, not "which vanilla deposit shall I
  convert". A renewable node need not replace an existing occurrence; natural
  occurrence is one input to characterization, not the candidate set.

  The policy it consults is a **declared table, empty by default**. With no
  table, the module derives nothing and says so — it must not fall back to a
  default portfolio. This is the seam; the table is the calibration.

**Step 4 — carry it.** Add `renewables` to the compiler's `bindings()` output,
so it lands in `runtime_bindings` beside `worksites`.

**Step 5 — read it.** `MapBindings.renewables(World)`, mirroring
`MapBindings.worksites(World)` exactly.

**Step 6 — bind and certify.** `Renewables.bind(MapBindings)` loads the
portfolio, replacing today's refusal. `readiness` gains `NO_RENEWABLE_LAYER`.

### Machinery vs calibration

| machinery — buildable now | calibration — open, do not invent |
| --- | --- |
| characterize stage running existing tools | which kinds are eligible at which Strategic Depth |
| cell grid with depth and character attached | richness/capacity by depth |
| portfolio module with an empty policy table | Regional Character → Regenerative Vocabulary |
| `renewables` in bindings, MapBindings, readiness | how many nodes constitute a baseline |
| `Renewables.bind` from manifest | `recoverTicks` per kind |

**On promoting to READY.** Treat it as a future requirement, as directed: build
the binding, the reader and the readiness code now, and promote `renewables`
into `readiness.REQUIRED` when the policy table can actually produce a
portfolio. Promoting it sooner makes every map uncertifiable for a reason that
is ours rather than the map's. Never restore a config fallback.

---

## Part 2 — The other five seams, by dependency

| # | seam | depends on | reuses existing measurement? |
| --- | --- | --- | --- |
| 1 | **Natural resource placement validity** | Step 1 + Step 2 | **yes, fully.** `opportunity_map` gives kind and position; `caves` gives accessibility; the cell grid gives depth. Extends `opening_ceiling`. *Open:* the rules — "Diamond near the Hinterland", "equipment-sufficient Iron" — need an equipment target that does not exist. |
| 2 | **Opening Hinterland floor** | Step 1 + Step 2 | **partly.** Ore and cave measurement exist; nothing measures whether each fundamental verb is *possible*. Construction/Extraction are reachable from existing data; Logistics, Production, Combat are not. *Open:* what evidence counts as "permits a verb". |
| 3 | **BUILT 24 Sep 2026 — `terrain_harvest/opening_access.py`.** Measures exit capacity per team at the edge of the opening, as connected frontier components over the same `Terrain`/`shortest` graph. Found that exit COUNT is near-degenerate (1 for almost every team at every cost, since an unobstructed frontier is one ring) and that WIDTH carries the signal: seed 930006815 at cost 240 has a north frontier of 78 nodes against the south's 206. No bound set. **Opening access certification** | Step 2 (shares the terrain graph) | **yes, more than any other seam.** `routes.author` already walks `shortest(t.adj, {home: 0})` from each homeland, carves a corridor fitted to a walkable profile, spares built structures, fells trunks whole, and records `skipped` with "no path from homeland in the terrain graph" -- a binary reachability verdict. `rescan.measure` then reports per-team reach **in seconds** off the authored world. *Missing:* it runs on authored configurations rather than compiled maps, its targets are strategic destinations, and there is no equivalent-exit-capacity comparison between teams. |
| 4 | **Practical traversability** | Step 2 | **yes.** `expedition/travel.py` already produces cost matrices. *Open:* what bound, and the standing warning that equal travel time is not the target. |
| 5 | **Bounded authorability verdict** | 1–4, and the portfolio | **yes, fragments.** `integration_cost`, `max_levelling_moved_per_column`, socket viability. Needs aggregation into "correctable within doctrine, or reject". *Open:* the intervention budget. |
| 6 | **Discovered classification** | everything above | n/a — it classifies what the others measured. *Open:* both metrics. Currently honest at `unmeasured`. |

**All five reuse measurement that already exists**, and 1, 2, 4 and 5 are
blocked on the same two prerequisites -- the characterize stage and the cell
grid. Those two pieces unblock four seams, which is why they come first. Seam 3
shares the terrain graph with them rather than standing apart, so it is not
independent either.

### The real characterisation of this repo's state

`caves.py`, `opportunity_map.py`, `rescan.py` and `routes.py` are all built,
all proven against the Alpha map, and **none is reachable from a compiled
realization.** The work is not building measurement. It is wiring measurement
that exists to a compiler that was developed separately from it. That is a much
smaller estimate than "six missing seams" implies, and it explains why the same
pattern -- measured, good, unconnected -- keeps surfacing.

### What seam 3 must NOT reuse

`routes.author` takes `route_targets` from a configuration: **strategic
destinations**. That is precisely the extension into Wilderness that generated
maps do not owe. Retargeting it at the Hinterland edge is a change of inputs,
not of mechanism -- and the carving, sparing and felling behaviour transfers
unchanged. The targeting model in code is still the old one; the correction
lives only in `maps.md`.

---

## Part 3 — Where cubiomes stops

Every seam above is **downstream of generation**. Ore, caves, exposure,
buildability, water and traversability are carver and feature output, and none
is a pure function of the seed. The off-server layer finishes when the window is
chosen:

    cubiomes    Map Type -> window search          cheap, repeatable, off-server
    ----------  generate the selected window  -----------------------------------
    region files characterize -> validate -> derive -> author -> verify -> classify

This is a clean boundary, not a limitation: throughput work and correctness work
are now separable, and no seam in Part 1 or 2 should be attempted off-server.

---

## Part 4 — What this plan deliberately does not do

- does not propose a depth, richness or species table;
- does not propose an intervention budget, an A_D bound, a Map Scale metric or
  a Resource Density metric;
- does not make North/South wilderness similarity or raw resource parity a
  target anywhere;
- does not add a compiler Routes stage;
- does not restore any config fallback for renewables.
