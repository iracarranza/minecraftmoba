# Vanilla Default-region seed-search proof of concept

> Prototype/test evidence only. No final Default map is selected and no terrain was modified.

## Method

Worldgen truth is the [official Mojang Minecraft Java **1.21.11** dedicated server](https://www.minecraft.net/en-us/article/minecraft-java-edition-1-21-11), pinned by SHA-1 `64bb6d763bed0a9f1d632ec347938594144943ed`. In the main batch, each seed was started once, the 864×1056-block origin region was generated in twenty <=256-chunk `forceload` batches, and the resulting full Anvil chunks were parsed directly. [Cubiomes](https://github.com/Cubitect/cubiomes) (MIT) was investigated but not adopted: it is useful for biome/structure prefiltering, while its own documentation states that it lacks block-level terrain and reliable surface-height checks. [Chunky](https://github.com/pop4959/Chunky) (GPL-3.0) was also reviewed, but adding a mod/plugin did not avoid full per-seed chunk generation or improve source fidelity over Mojang's server for this small test.

The batch evaluated 10 seeds × 8 analytical orientations. Runtime: setup 3.5s; official-server acquisition 689.5s; parsing/extraction 84.7s; Python evaluation 1.9s; rendering 0.5s; total 794.6s. Acquisition is the bottleneck. A post-review screen correction regenerated seed 920270003 in 78.2s; this is reported separately from the ten-seed batch.

## Retained regions

### Seed 920270010

- Selection role: full functional-screen pass; warnings: none.
- Raw bounds: `[-432, 431, -528, 527]`; orientation: 90° clockwise, east/west reflection=False.
- Western elevation advantage 17.583 Y; highland depth 296 blocks; local-relief P90 14 Y; 9 sampled approach bands; west snow/cold samples 0.
- East/west actual-ocean fractions 65.1%/0.0%; sampled coastline 5528 blocks; boundary variation 215.31 blocks; 34 turns; actual river-water/mouth samples 1536/47; 15 actual surface-water components.
- Largest forest region 28,416 blocks²; forest interior depth 104 blocks; open/buildable land 23.0%.
- Actual structure starts: 37 (buried_treasure, mineshaft, ocean_ruin_cold, ocean_ruin_warm, ruined_portal, ruined_portal_ocean, shipwreck, trial_chambers, village_savanna).
- Homeland developability bias 0.000; all six analytical corridors connected=True; maximum sampled Route step 34 Y.
- Strongest reason to retain: the strongest coast/highland contrast in the sample, with a broad mountainous west and highly articulated ocean edge. Strongest reason to reject: 49.1% surface water, island fragmentation, and a 34-block maximum sampled corridor step make it the most traversal-risky finalist.
- Direct world: `/Users/iracarranza/minecraftmoba/artifacts/worldgen/vanilla_default_poc_2026-09-09/worlds/Minecraft_MOBA_Vanilla_POC_920270010`; creative spawn `-292 73 68`.
- Renders: `shortlist/920270010/`.

### Seed 920270002

- Selection role: full functional-screen pass; warnings: none.
- Raw bounds: `[-432, 431, -528, 527]`; orientation: 90° clockwise, east/west reflection=True.
- Western elevation advantage 8.242 Y; highland depth 344 blocks; local-relief P90 12 Y; 10 sampled approach bands; west snow/cold samples 0.
- East/west actual-ocean fractions 39.9%/0.0%; sampled coastline 6160 blocks; boundary variation 111.68 blocks; 24 turns; actual river-water/mouth samples 1129/17; 68 actual surface-water components.
- Largest forest region 460,992 blocks²; forest interior depth 184 blocks; open/buildable land 39.5%.
- Actual structure starts: 25 (buried_treasure, jungle_pyramid, mineshaft, ocean_ruin_warm, ruined_portal, ruined_portal_jungle, shipwreck, trail_ruins, trial_chambers, village_plains).
- Homeland developability bias 0.099; all six analytical corridors connected=True; maximum sampled Route step 9 Y.
- Strongest reason to retain: the most balanced proof that deep forest, navigable western relief, usable land, real structures, and a clear eastern coast can coexist naturally. Strongest reason to reject: the forest dominates much of the west and may compress open between-Route Wilderness despite the favorable coarse corridor gradients.
- Direct world: `/Users/iracarranza/minecraftmoba/artifacts/worldgen/vanilla_default_poc_2026-09-09/worlds/Minecraft_MOBA_Vanilla_POC_920270002`; creative spawn `-204 69 172`.
- Renders: `shortlist/920270002/`.

### Seed 920270003

- Selection role: one-warning near-miss retained to test the screen boundary; warnings: ocean_gradient_points_east.
- Raw bounds: `[-432, 431, -528, 527]`; orientation: 0° clockwise, east/west reflection=True.
- Western elevation advantage 14.579 Y; highland depth 488 blocks; local-relief P90 14 Y; 7 sampled approach bands; west snow/cold samples 286.
- East/west actual-ocean fractions 10.8%/5.9%; sampled coastline 5632 blocks; boundary variation 80.08 blocks; 21 turns; actual river-water/mouth samples 1792/59; 15 actual surface-water components.
- Largest forest region 41,792 blocks²; forest interior depth 160 blocks; open/buildable land 24.9%.
- Actual structure starts: 27 (mineshaft, ocean_ruin_warm, pillager_outpost, ruined_portal, ruined_portal_ocean, trial_chambers).
- Homeland developability bias 0.123; all six analytical corridors connected=True; maximum sampled Route step 27 Y.
- Strongest reason to retain: strong internal relief, snow/cold evidence, and unusually legible connected vanilla water geography make it a useful stress case. Strongest reason to reject: the selected frame only narrowly reaches ocean and fails the east-over-west ocean-gradient screen.
- Direct world: `/Users/iracarranza/minecraftmoba/artifacts/worldgen/vanilla_default_poc_2026-09-09/worlds/Minecraft_MOBA_Vanilla_POC_920270003`; creative spawn `84 69 -356`.
- Renders: `shortlist/920270003/`.

## Findings

The three-image review confirms that the candidates are actual vanilla geography rather than synthetic approximations. Seed 920270002 visibly combines an eastern ocean, a coastal peninsula, a large western forest, and broken upland relief. Seed 920270010 has the clearest ocean/highland opposition but its island-heavy east makes several analytical corridors cross water. Seed 920270003 is an intentional boundary case: its mountain and inland-water geography are valuable, while the intended eastern-ocean grammar is visibly weak. An initially retained seed, 920270006, was rejected after the review exposed a north/west-ocean orientation false positive; the orientation selector and east-over-west screen were corrected rather than preserving that result.

Metrics that transferred cleanly: oriented west/east contrast, elevation/relief/slope, mountain depth, connected-component area/interior depth, open-space share, homeland developability/equivalence, and terrain-cost corridor feasibility. They now consume actual heightmaps, blocks, biomes, fluids, and structures.

Discarded synthetic assumptions: topology-family labels, generated mountain masks, D8 hydrology, ecology/resource markers, guaranteed snow, synthetic coast profiles, and synthetic POI portfolios. Actual water means sampled vanilla water/ice blocks; river classification additionally requires the vanilla river biome. Cave-water/lava are only section-palette presence in this coarse pass, and ore/entity analysis is deferred.

Naturally searchable in this sample: ocean adjacency, coast shape, water bodies, biome forests, snow/cold terrain, relief/highland depth, open homeland sites, and real structure starts. Likely later corrections/overlays: exact N/S homeland equivalence, six authored Route relationships, guaranteed competitive resource vocabulary, objective placement, and occasional modest access repair.

Scaling estimate: the main run averaged about 69 seconds of official generation and 8.5 seconds of extraction per full region. At that observed serial rate, 1,000 full regions would take roughly 21.5 hours and one million roughly 2.5 years before operational overhead; limited process-level parallelism could reduce elapsed time but not the underlying work. A production search should use a faithful 1.21.11 biome/structure prefilter, generate full chunks only for the small surviving fraction, parallelize isolated server processes, and retain no rejected worlds. Millions of seeds require a native coarse filter; official chunk generation belongs only in fine validation.

Key blockers: no reviewed library found that exposes exact 1.21.11 block terrain/height cheaply; Cubiomes' current mainline does not fully model 1.21.11 block terrain; official generation requires one world/server initialization per seed; full cave/aquifer/lava volumes and structures outside generated chunks cost additional parsing/generation. Natural structures are exact generated starts, but their strategic value is unresolved.

Retained world directories are under `/Users/iracarranza/minecraftmoba/artifacts/worldgen/vanilla_default_poc_2026-09-09/worlds` and are intentionally gitignored. Copy a directory into the Minecraft `saves` folder and open it with Java 1.21.11. The chunks are unmodified vanilla output; only `level.dat` inspection spawn/name plus `INSPECTION.json` and `README_INSPECTION.txt` were added after selection.

Stop condition reached: inspect these candidates before production-scale search, terrain correction, or Route design.
