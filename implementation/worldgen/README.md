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
