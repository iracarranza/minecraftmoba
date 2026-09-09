# Recovered Default-map worldgen material

This directory separates reproducible historical code from compact analysis
that carries the latest discussion forward.

## `historical/minecraft_moba_variant_pipeline.py`

Exact source recovered from the 50-variant solution-space bundle. It requires
Python plus NumPy, SciPy, Pillow, and Numba. Its default run writes review
images and full variant output, so direct runs should target an ignored output
directory.

Useful components include topology-family generation, variable coastlines,
Route protection/blending, irregular forest masks, mountain approach carving,
generator/validator separation, diversity filtering, checkpointing, and legal
standing-volume camera selection.

Historical limitations include fixed 672×832 bounds, Java 1.12 numeric block
IDs, abstract mountain `resource_regions`, Euclidean distance proxies, and
validator thresholds that predate the current nonuniform scale/resource
ecology/traversal model.

## `analysis/default_map_analysis.py`

Small dependency-free implementation of the explicitly recovered L1/L3
locomotion arithmetic, unsupported round-trip radius model, and normalized
North/South straight-line accessibility proxy. The distance proxy is a stable
intermediate interface, not the desired final pathfinding model.

## `analysis/resource_contract.py`

Schema-light existence audit for current prototype resource vocabulary. It
does not reconstruct the missing ecology generator's counts, densities, radii,
or placement tuning. A future generator should emit compatible metadata and
add ecological separation, abundance, terrain-aware access, and portfolio
evaluation.

Historical configuration and selected seed fixtures live under
`reference/solution-space/`. P2D terrain evidence lives under
`reference/history/p2d/`.

## Successor Default candidate generator

`generate_default_candidates.py` and `successor/` implement the metadata-first
candidate generator requested after recovery. It does not write Minecraft
worlds and does not choose a final Default map.

The pipeline is intentionally staged:

1. `terrain.py` generates a 5-block analytical surface, western mountain
   structure, variable eastern coast, forest terrain, and connected downhill
   hydrology.
2. `ecology.py` places actual terrain-linked resources, animals, crops,
   formations, structures, caves, and geology. A separately reported guarantee
   correction substage runs before regions are derived from those instances.
3. `routes.py` generates six terrain-adaptive provisional analytical
   centerlines. They have no speed bonus and are not a final visual Route
   vocabulary.
4. `analysis.py` performs terrain-aware shortest-path and reconstructed
   round-trip Hunger analysis, resource-packing/space allocation, and
   opportunity-portfolio diagnostics.
5. `validation.py` keeps hard contract failures, experimental warnings, and
   descriptive metrics in separate namespaces.
6. `pipeline.py` searches more seeds than it saves, preserves topology/metric
   diversity, and writes only a small shortlist. RLE grids retain sufficient
   surface geometry for a later serializer or renderer.

The scale transform is explicit metadata: an approximately 840×1040 block
analytical envelope tests the +25% macro hypothesis while leaving homelands
and feature footprints near their former physical scale. Added area is spent
on regional/deep separation, Route divergence, forest and mountain interiors,
and empty connective territory rather than uniformly enlarging objects.

Run the reproducible search:

```sh
python3 implementation/worldgen/generate_default_candidates.py
```

Run the invariant/regression suite:

```sh
python3 -m unittest discover -s implementation/worldgen/tests -v
```

The implementation uses only the Python standard library. Current analytical
thresholds are prototype tests, not final balance constants or replacements
for visual/playable review.

## Geography greybox v2 search

`generate_default_greyboxes.py` and `greybox/` are the current terrain-first
exploration workflow. They retain the successor envelope, ecology markers,
packing, fairness, and terrain-aware access analysis, but replace its source
terrain and provisional Route geometry. This milestone deliberately writes no
Minecraft worlds, water/lava systems, structures, roads, datapacks, or final
resources.

The terrain generator combines domain-warped multiscale noise, overlapping
topology forms, negative-relief valleys/ravines, irregular mountain entry, and
terrain-responsive landcover. Hydrology is represented by priority-flood
spill relationships and D8 flow accumulation over the unchanged heightfield:
channels, tributaries, basins, minima, and coastal outlets are analytical
opportunities, not finished water. Coast profiles vary boundary, transition
width, and shore character. Forest masks combine multiscale moisture,
topography, clearings, and continuous homeland pressure. Six provisional
Routes use 8-direction terrain-cost search, deterministic guide points, curve
refinement, and resampling; they remain centerlines rather than roads.

Run the deterministic 48-seed search (including regression seeds 920261010
and 920261022):

```sh
python3 implementation/worldgen/generate_default_greyboxes.py
```

The default output is
`implementation/worldgen/results/default_greyboxes_2026-09-09/`. It contains
a compact JSON search summary, six shortlisted metadata packages, two
regression packages, three consistent PNG renders per retained seed, and a
phone-scrollable `SHORTLIST.md`. Override the batch with `--seed`,
`--attempts`, `--shortlist`, or `--out`; the selected range must retain both
regression seeds. The implementation uses only the Python standard library.

Hard design/resource invariants and experimental visible-defect screens remain
separate. The search rejects hard failures, applies experimental geography
screens, then preserves topology and metric diversity; it never chooses the
top candidates solely by one aggregate score.

## Minecraft Java 1.21.11 serialization milestone

`serialize_default_worlds.py` and `serialization/` translate finalists
920261010 and 920261022 through one deterministic block-writing pipeline. The
output is Minecraft Java 1.21.11 (DataVersion 4671), using direct modern
Anvil/NBT region writing and a `level.dat` template bootstrapped by the matching
official Mojang server. This keeps the analytical heightfield and feature
coordinates authoritative; it does not feed the seed back into vanilla terrain
generation.

Version 1.21.11 was selected because the locally installed official client,
Java 21 runtime, version metadata, and server were available for an end-to-end
format check. It avoids the historical Java 1.12 numeric-ID format while making
the precise palette, entity-region, and `level.dat` schemas independently
testable. The build rejects any server JAR whose SHA-1 is not
`64bb6d763bed0a9f1d632ec347938594144943ed`.

This section documents the earlier serialization milestone and remains useful
for diagnosing its block-level findings. It is not the next step for the v2
greybox shortlist; review the geography renders before serializing any new
candidate.

Download that server and run the full reproducible build (review the Minecraft
EULA before passing `--accept-eula`):

```sh
curl -L \
  https://piston-data.mojang.com/v1/objects/64bb6d763bed0a9f1d632ec347938594144943ed/server.jar \
  -o /tmp/minecraft-server-1.21.11.jar

python3 implementation/worldgen/serialize_default_worlds.py \
  --server-jar /tmp/minecraft-server-1.21.11.jar \
  --java /path/to/a/Java-21/bin/java \
  --accept-eula \
  --server-check
```

The command writes independently playable folders and reproducible ZIPs under
`artifacts/worldgen/default_serialized_2026-09-09/`. Bulk worlds and packages
are intentionally gitignored; compact validation evidence, comparison text,
and renders are retained in
`implementation/worldgen/reports/serialized_default_2026-09-09/`.

Both worlds start in creative mode at the north homeland, with commands and
flight enabled. `INSPECTION.json` inside each world records homelands, Routes,
hydrology endpoints, villages, POIs, and mountain markers. Extract a package
into the Minecraft `saves` directory, open it in Java 1.21.11, and use creative
flight or `/gamemode spectator` for inspection. These builds intentionally have
no Route speed mechanic, Hunger datapack, objective system, or final Route
architecture.

For a faster repeat of readback validation, rendering, server boot, and
packaging after chunks already exist, append `--reuse-existing`. Do not use that
flag after changing block-generation code or candidate metadata.

## Vanilla Default-region proof of concept

`search_vanilla_default_regions.py` and `vanilla_search/` test the newer
Minecraft-first architecture: the official Minecraft Java 1.21.11 dedicated
server generates real chunks, the repository's Anvil/NBT reader extracts
heightmaps, biome palettes, surface blocks, fluids, and structure starts, and
Python evaluates eight rotations/reflections without changing the terrain.

The default experiment generates ten 864×1056-block origin regions, screens
their functional western-highland/eastern-coast fit, and retains three review
worlds. It requires the pinned official server JAR and Java 21. Review the
Minecraft EULA before passing `--accept-eula`:

```sh
python3 implementation/worldgen/search_vanilla_default_regions.py \
  --server-jar /tmp/minecraft-server-1.21.11.jar \
  --java "/path/to/java-21/bin/java" \
  --accept-eula
```

Compact committed metadata, findings, and renders are written to
`implementation/worldgen/results/vanilla_default_poc_2026-09-09/`. Only the
retained, unmodified vanilla chunk worlds are preserved locally under
`artifacts/worldgen/vanilla_default_poc_2026-09-09/worlds/`; that bulk artifact
directory remains gitignored. The pipeline changes only `level.dat` name,
creative inspection spawn, and command permission after selection, then adds
`INSPECTION.json` and `README_INSPECTION.txt`. It does not rewrite region or
entity chunks.

This proof of concept deliberately defers production-scale seed prefiltering,
ore volumes, entity/ecology analysis, exhaustive cave/aquifer analysis,
terrain correction, physical Routes, objectives, and custom serialization.
