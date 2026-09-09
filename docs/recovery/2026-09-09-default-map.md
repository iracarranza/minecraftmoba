# Default-map design/technical recovery — 2026-09-09

This report compares the newest accessible Default-map discussion and failed
or incomplete work against canonical `maps.md`. It intentionally leaves
`maps.md` unchanged so recovered omissions can be reviewed before promotion.

## A. What `maps.md` already captures

`maps.md` is substantially current. It already preserves:

- Minecraft-first generation: generate plausible geography, identify actual
  resources and opportunities, modestly correct, and reject unsuitable seeds.
- North/South teams orthogonal to the west/east environmental gradient.
- Comparatively safe, competitively equivalent homelands without requiring
  literal block mirroring.
- A continuous Wilderness with meaningful empty connective terrain.
- Three physical authored Route branches per team, with visible authored
  infrastructure leading toward villages and major POIs.
- No base Route speed bonus; the current prototype instead tests a 10%
  locomotion-only exhaustion reduction on already cleaner/directer Routes.
- Difficult terrain as inhabitable, legible, contestable, transformable,
  logistically meaningful geography rather than repeated jump taxes.
- A deep western mountain with a gradual-then-stronger westward difficulty
  ramp, internal ridges/valleys/saddles/basins/ravines/caves, several
  approaches, and high-altitude snow ecology.
- An eastern coast/ocean intended to affect transport, resources, settlement,
  class interactions, and regional identity rather than act as decoration.
- Irregular forests with interiors; positive and negative verticality;
  plausible catchments and downhill hydrology; learnable landmarks.
- Villages as compound strategic portfolios and POIs as opportunity geography,
  including the older ~15-second Route-end benchmark correctly labeled
  historical rather than prescriptive.
- Resource treatment categories, functional families, livestock ranges,
  separate horses, wheat/carrot/potato starters, sand/gravel/snow guarantees,
  and physical geological resource situations.
- Food as geography, L1 10-food/L3 12-food test states, zero saturation,
  vanilla 6-food sprint cutoff, and the +50% change in pre-cutoff sprint budget.
- Round-trip expedition evaluation and walking as the underprovisioned failure
  state rather than a hard geographic prohibition.
- The 20–30% scale concern, +25% first hypothesis, ~56% area consequence,
  nonuniform expansion concentrated away from home, and approximate travel
  bands.
- The old 672×832 generator dimensions as historical only, not a new target.
- Opportunity-based fairness, the actual-resource-first generation order,
  movement-aware traversal as the desired successor to straight-line distance,
  and the need for resource-packing analysis.
- The P0–P2D and 50-variant histories, specialist seeds, incomplete first-ten
  ecology run, immediate independent scale-test target, and unresolved Java
  version/world-writing choice.

## B. Later or more-specific material omitted from `maps.md`

The omissions are mostly reproducibility details, not reversals of canonical
direction:

1. The last in-chat scale calculation used a 292-block homeland-to-midpoint
   reference, a 20% operating reserve, two Route jumps/100 blocks, eight
   ordinary-Wilderness jumps/100 blocks, 0.10 exhaustion/block, 0.20 per
   sprint-jump, and 10% Route efficiency. It estimated unsupported sprint-out,
   sprint-back one-way radii of roughly **55–68 blocks at L1** and **83–103 at
   L3**, depending on Route share and jump rate. These are analysis assumptions,
   not map requirements. They are now reproducible in
   `analysis/default_map_analysis.py` for review.
2. The historical accessibility proxy was explicitly
   `(north_cost - south_cost) / (north_cost + south_cost)`. The later fairness
   discussion also called for opportunity portfolios, contestedness,
   discovery-order diversity, clustering penalties, and Route-monopolization
   penalties. `maps.md` captures the factors but not this old formula or the
   distinction between mean bias and mean absolute bias.
3. The space-allocation fixture discussion named measurable parcels more
   explicitly: Route corridors, between-Route Wilderness, deep off-Route
   Wilderness, forest interiors, open terrain, mountain transition/interior,
   coast, POI influence areas, and genuinely empty territory. `maps.md` asks
   the correct resource-packing question but contains no completed packing
   result because none was produced.
4. P2D's quantitative geological/hydrological correction results were not
   carried into `maps.md`. They are preserved under `reference/history/p2d/`.

No later discussion was found that changes the current coast, forest, Route,
resource-guarantee, Hunger, or seed-selection direction already in `maps.md`.

## C. Useful historical implementation components

- The exact 50-variant pipeline is recovered under
  `implementation/worldgen/historical/`. Its topology families, variable
  coastline, Route masks and aprons, entrance carving, irregular forest fields,
  validator separation, diversity filtering, checkpoints, and legal camera
  placement remain reusable.
- The historical configuration is separated into a small JSON file, and seven
  important seed roles are preserved without retaining 50 worlds or renders.
- P2D's embedded audit/readme and recovered correction results preserve the
  strongest available geology/hydrology/transition evidence.
- The compact analysis scripts preserve current traversal arithmetic,
  normalized accessibility output, and resource-existence checks without
  depending on the historical renderer.

## D. Successful components superseded by later thinking

- The generator's 672×832 rectangular bounds and uniform coordinate model are
  not the next map dimensions. The successor needs nonuniform depth expansion.
- Abstract mountain `resource_regions` are superseded by actual resource
  instances from which regions and opportunity portfolios are derived.
- Euclidean distance is only a stable proxy. Terrain-aware path cost must
  include slopes, jumps, water, obstacles, detours, Route surfaces, Hunger,
  and eventually hauling/return conditions.
- The old structural gates are useful cheap rejection checks, not proof of
  fairness, accessibility, ecology, opportunity equivalence, or gameplay.
- The historical contract's phrase that live Routes may add a speed advantage
  is superseded for the immediate test by **no speed bonus** and provisional
  locomotion efficiency.
- Java 1.12 numeric IDs and review rendering are historical implementation
  details, not a reason to choose the next playable world's format.
- P2D's silhouette, basin, exact approaches, ore quantities, and authored world
  are evidence, not a base to extend.

## E. Incomplete/failed work with the newest relevant content

- The resource-ecology pipeline was later than the 50-variant generator. It
  overlaid four livestock ranges, cow/sheep/pig/chicken coverage, three patches
  per baseline crop, sand/gravel formations, ore clusters, variable village
  portfolios, North/South access proxies, fairness metrics, and explicit
  contract gaps. The full 50-seed run timed out after ten; seed 920260900 was
  strong only within that incomplete sample. Snow was explicitly unimplemented.
- The complete historical ecology source is not recoverable from the repository,
  Git history, attached bundle, or accessible conversation export. Only a
  truncated prefix survives in conversation history. It was not committed as
  a misleading partial program, and missing tuning was not reconstructed.
- The +25% independent playable world and datapack never reached artifact
  creation. What advanced during the failed handoff was the nonuniform scaling
  model and traversal arithmetic, now captured by the analysis helper.
- No completed resource-packing analysis exists. It remains the next required
  quantitative step rather than a historical result to recover.
- No P2D generator source survives in accessible material. The world, embedded
  audits, and reported correction results are the recovery boundary.

## F. Recommended technical basis of the next generator

1. Treat `maps.md` as design authority and this report as review material.
2. Separate terrain generation, resource-instance generation, analysis,
   correction, validation, serialization, and rendering.
3. Reuse the 50-variant pipeline's macro topology families, coast variation,
   Route protection/blending, irregular field generation, approach carving,
   diversity filtering, and checkpointing—but replace its fixed geometry with
   a nonuniform homeland-depth transform.
4. Reimplement P2D's terrain vocabulary: broad irregular transition bands,
   stepped/broken slopes, exceptional rather than planar cliffs, slope-aware
   scree, internal mountain geography, and catchment/spill hydrology.
5. Generate actual terrain-linked plants, animals, formations, ores, caves,
   structures, and food waypoints; derive resource regions and village/POI
   portfolios afterward. Preserve existence guarantees without fixing exact
   discovery coordinates.
6. Evaluate required existence first, then territorial separation, ecological
   plausibility, movement-aware access, contestedness, opportunity portfolios,
   empty-space depth, and amount of artificial correction. Reject candidates
   that need extensive repair.
7. Use the selected fixture seeds as regression/stress cases, not as candidates
   that must survive a changed generator unchanged.
8. Resolve the target Minecraft Java version and world-writing/export path
   before building a playable save. Keep gameplay test instrumentation separate
   from terrain synthesis, and label any Route-exhaustion compensation as an
   approximation unless the implementation is source-accurate.

The immediate engineering milestone should be a metadata-only successor
candidate generator and evaluator. Run resource packing and terrain-aware
round-trip analysis before serializing or rendering a new playable world.
