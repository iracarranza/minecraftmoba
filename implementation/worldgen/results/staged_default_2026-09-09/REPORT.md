# Completed staged vanilla Default search

> Prototype/test finalist set. No final map selection, terrain correction or competitive systems are implemented.

## Funnel and measured cost

| Stage | Input → output | Measured time | Throughput |
|---|---:|---:|---:|
| A: native coarse biome screen | 100,000 → 30,897 windows | 24.75 s | 4,041.0 windows/s |
| B: native structural ranking | 30,897 → 19,288 viable proxies → 11 diverse generation selections | 81.18 s | 380.6 windows/s |
| C: official 1.21.11 acquisition | 11 → 11 complete windows | 824.05 s | 0.0133 windows/s |
| C: final ground extraction | 11 complete windows | 92.90 s | 0.118 windows/s |
| C: final actual-terrain analysis | 11 → 8 finalists | 5.59 s | 1.966 windows/s |
| D: final analytical fit | 8 finalists | 1.42 s | 5.630 finalists/s |

A searched 20,000 seeds at five centers each: (0,0), (±2048,0), (0,±2048). Windows are 864×1056 raw blocks. A rejected 69,103 (69.10%); reasons: `{'no_ocean_sample': 15962, 'no_highland_opportunity': 52634, 'mostly_ocean': 507}`. B rejected 11,609 (37.57%); 19,277 viable proxies were not selected for the limited generation budget. They are not claimed to be terrain failures.

Official generation: **11 worlds, 39,204 requested full chunks**, 2648.4 CPU-seconds; serial servers with 3 GB maximum Java heap each. Retained worlds occupy 410.5 MiB. Saved chunk-slot counts, including spawn/halo chunks, are recorded per world in `validation_and_cost.json`; the requested chunk count does not hide that extra generation. No finalist was regenerated. JAR SHA-1: `64bb6d763bed0a9f1d632ec347938594144943ed`.

Times above are measured stage service times, not the elapsed interactive development session. Native compile/setup, presentation rendering, hashing, regression re-extraction and operator review are outside those stage rates. Preliminary extraction/analysis before the snow-on-canopy correction is preserved separately in `pre_ground_scan_analysis_summary.json`; these validation passes incurred parsing work but no extra Minecraft generation. The separate 5,000-window pilot took 5.27 s and is not included in the 100,000-window production counts.

## Selective refinement and review decisions

Overhead review of the initial eight revealed too much broadly snowy coast. A cheap refinement resampled the leading 5,000 existing B survivors in 13.56 s (368.8 windows/s), distinguishing temperate open interior, snowy plains and western peak/slope biomes. It selected exactly three additional official worlds. These are repeated cheap evaluations of existing windows, not an expanded blind generation batch.

Final review retained 8/11 generated candidates. The following are manual/analytical selection exclusions, not hidden full-generation failures:

- 930004953: Overhead review: broadly snowy interior duplicates stronger alpine options; preliminary Route fit had substantial N/S imbalance. Keep as a high-relief cold-climate reference, not a final diverse pick.
- 930011701: Overhead and analytical review: very weak open interior, fragmented dry geography and severely asymmetric coastal access. Replaced by a temperate-interior candidate rather than proposing macro repair.
- 930016657: Overhead and analytical review: frozen maritime composition duplicates other finalists, with a notably weaker north development footprint. Preserve as comparison evidence rather than pad the final shortlist.

All 11 generated worlds remain available: 561.9 MiB and 84,738 saved Overworld chunk slots including spawn/halo/protochunks. Excluded review packages are under `review_excluded/`; their worlds remain clean. Finalist-only disk size is reported separately above.

## Method and metric decisions

A uses 63 stored-quart biome samples at Y96 per window, spacing 128 blocks, to reject absent ocean/highland opportunity and ocean domination. B uses 891 samples at 32-block spacing and four W/E axis assignments. These are biome proxies only. No terrain simulation, structure search, resource inventory or pathfinding runs in the bulk funnel. A soft composition bottleneck rank rewards simultaneous coast, highland, cold, open ground, forest and homeland opportunity; farthest-first composition diversity then operates within the leading 1% (at least 80) of ranked B survivors, with distinct seeds. These prototype ranking scales are not game-design thresholds.

C uses the pinned official Java 1.21.11 dedicated server and existing full-chunk extractor. Real surface water, relief, biome regions, dry connectivity, open substrate, cold highland evidence and homeland footprints determine fit. A 64-block capped downward scan distinguishes snowy tree canopies from ground. Actual open-ground credit requires non-canopied soil/sand/gravel and modest sampled roughness. D alone runs six analytical paths, with uncapped elevation cost to discourage cliff shortcuts, and adds homeland footprints, center, provisional POI nodes, regional adjacency, crossing/steep-step warnings and shared-corridor counts.

Retained: official acquisition, Anvil/NBT reader, eight-way orientation utility, real surface/biome/fluid/structure evidence, compact grids and clean-world inspection setup. Demoted: legacy composite score and warning contract; relative highland column counts; biome-only openness; automatic path connectivity as proof of walkability; exact villages/resources/ecology. Added: cheap A/B, composition diversity, real canopy/open substrate, connected regional relationships, explicit bounds/axes, analytical POIs and footprint/crossing diagnostics. Existing POC code/results remain available.

C hard filters are only absent eastern ocean, reversed ocean axis, absent western highland, catastrophic dry fragmentation, or lack of plausible development sites. No exact animal/crop/ore/village count is used. Dimensions and 72×72 homeland footprints are prototype tests, not settled scale decisions. Rotations can exchange logical width/height; no terrain is rotated. N/S reversal and reflection are equivalent duplicate interpretations before teams are assigned.

## Finalists

| Seed | Raw center | Inclusive X / Z bounds | Logical N / E | Open ground / west highland | Review |
|---|---|---|---|---|---|
| 930010639 | [2048, 0] | 1616…2479 / -528…527 | -X / -Z | 38.7% / 38.8% | [fit and inspection](finalists/930010639/REVIEW.md) |
| 930015734 | [2048, 0] | 1616…2479 / -528…527 | -X / -Z | 16.8% / 66.3% | [fit and inspection](finalists/930015734/REVIEW.md) |
| 930012642 | [-2048, 0] | -2480…-1617 / -528…527 | -Z / +X | 45.3% / 40.3% | [fit and inspection](finalists/930012642/REVIEW.md) |
| 930006815 | [0, 0] | -432…431 / -528…527 | +X / +Z | 43.4% / 41.2% | [fit and inspection](finalists/930006815/REVIEW.md) |
| 930019528 | [-2048, 0] | -2480…-1617 / -528…527 | +Z / -X | 25.7% / 34.9% | [fit and inspection](finalists/930019528/REVIEW.md) |
| 930005557 | [-2048, 0] | -2480…-1617 / -528…527 | -X / -Z | 41.3% / 56.9% | [fit and inspection](finalists/930005557/REVIEW.md) |
| 930016664 | [0, 2048] | -432…431 / 1520…2575 | +Z / -X | 27.7% / 41.5% | [fit and inspection](finalists/930016664/REVIEW.md) |
| 930007222 | [-2048, 0] | -2480…-1617 / -528…527 | -X / -Z | 28.4% / 56.9% | [fit and inspection](finalists/930007222/REVIEW.md) |

[Visual comparison](COMPARISON.svg). Pale green = sampled open ground, dark green = canopy, grey = exposed rock, white = snow/ice, blue = actual water. The per-finalist biome and height maps provide complementary evidence. Each `REVIEW.md` records landscape regions/relationships, center, principal weakness, correction burden, homeland and Route fit, POIs and world path.

## Regression references and evidence boundaries

| Region | Actual open ground | Actual canopy | Eastern ocean | Western cold highland samples |
|---|---:|---:|---:|---:|
| 920270002 | 28.9% | 40.6% | 39.9% | 0 |
| 920270003 | 46.4% | 7.3% | 10.8% | 277 |

The references were re-extracted from their existing official worlds in their historical chosen orientations, not regenerated or silently reoriented. The specification supplies the qualitative interpretation: 002 has useful directionality but weaker differentiation; 003 has rich formations/open country but weaker W/E arrangement. Numeric canopy is sampled cover, not an attempt to override those interior visual judgments. New finalists should be compared for the combination of traits rather than one scalar score.

Cubiomes revision `e61f90580cbdd883214a8054670dacae655e59c0` ([upstream](https://github.com/Cubitect/cubiomes)) advertises MC_1_21 Winter Drop, not explicit 1.21.11 fidelity. Same-coordinate quart-palette spot checks matched 5977/5980 samples across references and finalists. This is empirical compatibility evidence, not a complete generator equivalence proof. Initial block-Voronoi comparisons were replaced with the appropriate stored-quart comparison.

Evidence layers: A/B are cheap proxies; C measurements and raster images come from actual vanilla chunks; D boxes, corridors, region labels and POI nodes are analytical hypotheses; interior legibility and final playability remain manual. No Minecraft first-person playtest has been performed in this task. See `VISUAL_REVIEW.md` for explicit overhead-image judgment.

## Limitations and handoff

Eight-block sampling misses narrow barriers, caves, small passes and individual obstructions. The ground scan can stop at structure roofs or unusual unclassified blocks and has a 64-block cap. It is not a complete block collision model. The region graph describes surface adjacency, not verified traversal. A* has finite cost everywhere; six paths do not prove six independent or balanced Routes. Shared paths and rough/wet segments are exposed for review. Homeland flatness is a local opportunity proxy, not resource fairness or a guarantee of construction-ready 72×72 floors.

Manual/later: regional interiors, sightlines, landmark quality, internal mountain passes/valleys, cave/ravine entrances, alternative crossings, actual bridge/stair/clearing burden, expedition timing, homeland resources/defenses, competitive equivalence, authored village network and ecological/resource guarantees. No macro repair is proposed. Reject a finalist if manual inspection would require mountain/coast/river/biome replacement.

Clean worlds: `artifacts/worldgen/staged_default_2026-09-09/worlds/` at repository root. They are local, gitignored bulk artifacts, not included in the Git push. Each folder opens in Java 1.21.11 and includes `INSPECTION.json`, `GREYBOX.json` and chunk hashes. Committed artifacts include the comparison, detailed reviews, labeled SVGs, actual rasters, candidate metadata, all B survivors, queue, counts, acquisition timings and validation results.

Recommended next step: manually fly the strongest open-interior candidates and the contrasting alpine/coastal options using the labeled bounds; inspect both homeland footprints, western approaches and flagged crossings. Choose one substrate only after this review, then plan modest authored infrastructure and resource/ecology work. Do not start macro terrain repair.
