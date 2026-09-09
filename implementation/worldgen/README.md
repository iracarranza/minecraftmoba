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
