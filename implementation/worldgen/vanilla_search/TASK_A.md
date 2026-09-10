# Default Task A analytical fitting

Authority: `specs/mapseedsearchspec2.md`, with `maps.md` authoritative for settled
design. These are Working analytical fits, not recommended seeds or playable
maps. No Task B content or world writing is included.

## Run and inspect

From the repository root:

```sh
python3 -m unittest discover -s implementation/worldgen/tests -v
python3 implementation/worldgen/fit_default_task_a.py
```

The runner reads exactly the eight identities in
`implementation/worldgen/results/staged_default_2026-09-09/search_summary.json`.
It consumes their committed `candidate.json` feature grids and existing
`05_actual_ground_canopy.png` backgrounds. It does not acquire seeds, generate
chunks, read worlds for terrain analysis, or use prior authored POI assignments.

Outputs: `implementation/worldgen/results/default_task_a_2026-09-10/`:

- `<seed>/fit.json`: compact spatial fit, scores, evidence and failures.
- `<seed>/greybox.svg`: self-contained terrain overlay with proposed geometry.
- `comparison.json`: comparable metrics, ordered by seed, without a winner.
- `verification.json`: source/output SHA-256 hashes and before/after world-file
  immutability verification. World reads are limited to hashing existing files.

## Deterministic model

The fitter reuses the existing oriented RLE feature grid, coordinate transform,
nine-by-nine homeland search and connected-region/distance helpers. Dijkstra
extends the previous point-to-point pathfinding because edge-origin distance
fields and network distances are required. SVG is a new presentation layer over
the existing terrain image, not another terrain-analysis pipeline.

Bounds and orientation remain exactly those of the input window. Homelands use
the existing terrain-scored 72-by-72-block footprint. A Fountain must be dry,
buildable, uncanopied and have sampled local grade at most two blocks. Among
valid samples it minimizes total distance to the footprint perimeter plus a
grade penalty; it is not automatically the centroid. Missing valid space is a
failure, not an invented anchor.

Eight-neighbor movement costs are horizontal distance multiplied by:

```text
1 + 0.5 * absolute height difference
  + 0.8 * absolute height difference when the difference exceeds 8
  + 10 * either-end-water
  + 0.35 * mean nonbuildability
  + 0.5 * mean canopy
```

These are transparent analytical effective-block weights, not calibrated travel
time, Hunger consumption, player movement rules or level gates. Water and rough
terrain carry finite opportunity costs, not proven traversal or bridge plans.
Symmetric costs permit explicit return/round-trip reporting. Homeland-edge depth
starts halfway along edges crossing the footprint boundary, not at the Fountain.

The three departures seek western highland, interior open/forest-gap and eastern
coast geography. Targets are representatives of connected eligible regions,
ranked by effective path cost and area. Departures are separated; a reuse penalty
discourages early corridor collapse. That penalty affects path choice only, not
reported physical/effective distances. Corridor spines are designated reference
connections, not literal construction centerlines.

Starter termini prefer 65 effective blocks, search the soft 58–76 range and
penalize water/canopy at the handoff. If no sampled handoff exists in that range,
the nearest available sample is retained with a failure diagnostic. The full
supported path from Fountain to terminus is included separately from its
outside-homeland portion. Corridor efficiency and Starter construction/legibility
are separate scores; water, canopy and severe sampled rises reduce the latter.

Depth bands follow the specification exactly, including a separate 350+ code.
Dry Wilderness statistics exclude both homelands. Connected land patches of at
least 12 samples indicate area availability, not proven interesting content.
Existing extracted regional references provide a separate geographic-interest
diagnostic. Their absence does not establish an empty landscape.

Network joins include shared samples and crossing diagonal midpoints. Pairwise
terrain-cost cross-connections between early/middle corridor anchors supply
lateral-connection and surface-bypass diagnostics. A bypass must cost less than
85% of the existing network alternative. Network components describe the six
Route spines before proposed extra connections; all joins remain unverified at
block resolution. Center is selected in the middle third by nearby corridor
count, open ground and grade, with bypass and nearby-connection reporting. It
receives no gameplay role.

Sampled water runs and narrow favorable approaches are crossing/chokepoint
candidates only. Network-adjacent extracted regions provide additional Key
Location candidates. Existing geographic region references provide landmarks;
the five largest are labeled in SVG for readability, not ranked as design value.
West-highland/east-coast and N/S homeland labels retain the directional grammar.

Thresholds are generic across all eight seeds and recorded in each result.
Failure diagnostics include missing anchors/targets, insufficient opening
divergence (24-block terminus separation and less than 50% opening overlap),
Starter range/construction failures, insufficient depth area, compressed regional
references, weak interior connectivity and homeland terrain-score disparity over
0.2. These are analytical diagnostics, not final balance judgments.

## Source-data limitations

The eight-block grid cannot establish exact collision clearance, construction
volume, water depth/ford suitability, cave entrances, tunnels, ravines, natural
bridge geometry, mandatory chokepoints or grade-separated connectivity. Region
representatives do not prove landmark visibility, prominence or rich content
throughout a depth band. Resource/ecology portfolios and competitive resource
equivalence are unavailable. Actual travel time, Hunger and progression access
are not measured. These limitations are emitted in every fit and the aggregate;
unsupported metrics remain null or explicitly unverified. Nothing is repaired by
generating worlds or inventing gameplay mechanics.
