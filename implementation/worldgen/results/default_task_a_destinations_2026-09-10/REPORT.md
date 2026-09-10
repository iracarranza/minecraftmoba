# Destination-anchored Starter refit

Analytical / non-authoritative. No winner, freeze, physical edits, new seeds or Task C. Human review must accept the refit before materialization.

Homelands, Fountains, bounds, orientation, departures and deeper Route targets are unchanged. Existing A.0/A.1/B artifacts remain historical evidence, not validation of these new paths.

## Eight-candidate comparison

| Seed | Anchored N/S | Distinct formations N/S | Opportunity kinds N/S | New depth range | Outside preferred band | Diagnostics |
|---|---|---|---|---|---|---|
| 930005557 | 3/3 | 3/3 | 3/2 | 60.0–135.2 | 3 | 0 |
| 930006815 | 1/2 | 1/2 | 1/2 | 76.5–107.3 | 3 | 8 |
| 930007222 | 0/3 | 0/3 | 0/2 | 41.2–73.4 | 1 | 7 |
| 930010639 | 2/2 | 2/2 | 1/1 | 59.6–138.8 | 2 | 7 |
| 930012642 | 2/3 | 2/3 | 2/2 | 52.7–127.6 | 5 | 4 |
| 930015734 | 3/2 | 3/2 | 3/2 | 54.4–94.8 | 2 | 8 |
| 930016664 | 0/3 | 0/3 | 0/3 | 63.1–103.6 | 1 | 5 |
| 930019528 | 3/2 | 3/2 | 3/2 | 59.2–146.8 | 2 | 4 |

## Focused before / after

Coordinates are actual world XYZ; depth is effective blocks from the homeland edge. “Unresolved” means no replacement is asserted; the old spine remains diagnostic only. Feature IDs are connected sampled interfaces, not proof of distinct named water bodies or visually distinct landforms.


### 930010639

| Route | Old terminus / depth | New terminus / depth | Anchor | Weak evidence |
|---|---|---|---|---|
| north-1 | [1828, 72, -172] / 64.3 | [1860, 66, -196] / 73.7 | inland water access:3920 | sampled access edge; physical entrance and visual legibility unverified |
| north-2 | [1852, 66, -204] / 63.5 | [1876, 64, -244] / 59.6 | inland water access:1821 | sampled access edge; physical entrance and visual legibility unverified |
| north-3 | [1836, 65, -316] / 61.3 | Unresolved | No supported destination in bounded search | See path/search exclusions in fit.json; not a seed rejection |
| south-1 | [2316, 78, 28] / 61.6 | [2316, 79, 60] / 106.8 | slope-foot approach:11409 | 32-block flat-to-rising profile; hill scale and human-perceived significance unverified |
| south-2 | [2276, 82, -4] / 61.0 | [2252, 73, 4] / 138.8 | slope-foot approach:10100 | 32-block flat-to-rising profile; hill scale and human-perceived significance unverified |
| south-3 | [2348, 75, -76] / 70.2 | Unresolved | No supported destination in bounded search | See path/search exclusions in fit.json; not a seed rejection |

north: three-choice analytical support True → False; anchored 2/3, 2 interfaces, 1 kinds; joint pair penalty 1.0.
Minimum terminus separation 40.0 → 50.6 blocks; maximum opening overlap 0.00 → 0.00. These geometry metrics include unresolved historical spines and do not establish destination coverage.
Pair-by-pair old/new overlap, separation and continuation-collapse evidence is retained in comparison.json. Full-network geometry is recomputed; no B.1 physical pass is transferred to a revised route.

south: three-choice analytical support True → False; anchored 2/3, 2 interfaces, 1 kinds; joint pair penalty 1.0.
Minimum terminus separation 51.2 → 85.0 blocks; maximum opening overlap 0.00 → 0.00. These geometry metrics include unresolved historical spines and do not establish destination coverage.
Pair-by-pair old/new overlap, separation and continuation-collapse evidence is retained in comparison.json. Full-network geometry is recomputed; no B.1 physical pass is transferred to a revised route.

### 930012642

| Route | Old terminus / depth | New terminus / depth | Anchor | Weak evidence |
|---|---|---|---|---|
| north-1 | [-2212, 75, -420] / 74.4 | Unresolved | No supported destination in bounded search | See path/search exclusions in fit.json; not a seed rejection |
| north-2 | [-2132, 70, -396] / 61.3 | [-2132, 71, -428] / 54.8 | village:[-133, -24] | village entrance, occupancy and unobstructed sightline unverified |
| north-3 | [-2116, 72, -420] / 62.0 | [-2100, 69, -412] / 96.6 | slope-foot approach:1559 | 32-block flat-to-rising profile; hill scale and human-perceived significance unverified |
| south-1 | [-2084, 71, 412] / 55.9 | [-2068, 69, 396] / 52.7 | inland water access:12041 | sampled access edge; physical entrance and visual legibility unverified |
| south-2 | [-2060, 74, 380] / 63.5 | [-2020, 70, 356] / 54.6 | inland water access:11828 | sampled access edge; physical entrance and visual legibility unverified |
| south-3 | [-1972, 69, 396] / 66.2 | [-1924, 71, 380] / 127.6 | forest entrance:11406 | sampled access edge; physical entrance and visual legibility unverified |

north: three-choice analytical support True → False; anchored 2/3, 2 interfaces, 2 kinds; joint pair penalty 0.0.
Minimum terminus separation 28.8 → 35.8 blocks; maximum opening overlap 0.00 → 0.00. These geometry metrics include unresolved historical spines and do not establish destination coverage.
Pair-by-pair old/new overlap, separation and continuation-collapse evidence is retained in comparison.json. Full-network geometry is recomputed; no B.1 physical pass is transferred to a revised route.

south: three-choice analytical support True → True; anchored 3/3, 3 interfaces, 2 kinds; joint pair penalty 1.0.
Minimum terminus separation 40.0 → 62.5 blocks; maximum opening overlap 0.00 → 0.00. These geometry metrics include unresolved historical spines and do not establish destination coverage.
Pair-by-pair old/new overlap, separation and continuation-collapse evidence is retained in comparison.json. Full-network geometry is recomputed; no B.1 physical pass is transferred to a revised route.

## All-candidate diagnostics

- 930005557: No current analytical failure diagnostics.
- 930006815: Excessive incidental links: little distinguished lateral geography; north-2: no supported destination; north-3: no supported destination; north: three distinct destination formations not established; north: three meaningful openings not established; south-1: no supported destination; south: three distinct destination formations not established; south: three meaningful openings not established
- 930007222: Excessive incidental links: little distinguished lateral geography; north-1: no supported destination; north-2: no supported destination; north-3: no supported destination; north: homeland below minimum sampled usability; north: three distinct destination formations not established; north: three meaningful openings not established
- 930010639: Under-convergence: no shared central interaction; north-3: no supported destination; north: three distinct destination formations not established; north: three meaningful openings not established; south-3: no supported destination; south: three distinct destination formations not established; south: three meaningful openings not established
- 930012642: Excessive incidental links: little distinguished lateral geography; north-1: no supported destination; north: three distinct destination formations not established; north: three meaningful openings not established
- 930015734: Excessive incidental links: little distinguished lateral geography; south-1: no supported destination; south: deep geographic depth unverified; south: missing connected deep traversable depth; south: missing connected tertiary traversable depth; south: tertiary geographic depth unverified; south: three distinct destination formations not established; south: three meaningful openings not established
- 930016664: north-1: no supported destination; north-2: no supported destination; north-3: no supported destination; north: three distinct destination formations not established; north: three meaningful openings not established
- 930019528: Under-convergence: no shared central interaction; south-1: no supported destination; south: three distinct destination formations not established; south: three meaningful openings not established

## Method and unsupported assumptions

Observable candidates: actual village piece edges; inland dry banks; coast access; lowland-to-highland and forest interfaces; persistent biome interfaces; sampled flat-to-rising slope feet. Ordinary empty terrain is never a successful destination. Passes, valleys, saddles and true junctions are not asserted from insufficient extraction data.

Each Route retains its homeland approach and deeper target. Conservative dry/rise-limited paths search a 32-block neighborhood of its original spine and reconnect to its exact original deeper suffix. Require a loop-free path, sampled feasibility ≥0.5, at least 32 effective blocks of unsupported continuation and at most the existing 1.8 detour factor. An unavailable replacement retains old geometry explicitly marked unresolved, not a destination fit.

Soft distance cost = |depth − 65| / 65. Add construction cost 3×(1−feasibility), evidence cost (1−strength), relative corridor detour and a dry-neighbor handoff cost. Strengths 1 for direct feature interfaces, 0.75 for slope-foot profiles and 0.5 for biome-only interfaces are uncalibrated evidence proxies, not measured gameplay utility. Dry neighboring samples are not proof of distinct branches.

Jointly enumerate retained candidate combinations for each team. Penalize shared feature (3), same opportunity kind (1), fuzzy opening overlap (3×fraction), and separation below the existing 24-block scale (3×normalized deficit). Search top 3 per feature and 12 per Route, retaining each feature’s best option before its additional nearby anchors; this is bounded joint optimization, not a global optimum. Missing routes remain explicit. Different interface IDs can still represent similar geography.

The combined three-choice diagnostic also requires three anchored, distinct interfaces. Fewer than two opportunity kinds emits an explicit prototype narrow-coverage warning, not a failure or required biome portfolio. Different geographic opportunities can share a kind.

The 40–150 search envelope reuses fringe/secondary-transition boundaries as a conservative prototype bound, not a new termination target. It can exclude real destinations beyond 150 or outside the corridor neighborhood; unresolved routes are fitter limitations, not automatic seed rejections. No unsupported destination is manufactured to meet a quota.

Slope-foot detection reuses the 32-block approach scale, 2-block flatness and 8-block relief: flat behind, monotonically rising ahead. Four connected evidence samples are required. This is not proof of mountain scale or visual significance.

- destination_semantics: Opportunity means access to observed geography only; no resources, rewards, ecology or gameplay role inferred.
- destination_detection: 8-block dry-bank, highland-boundary, forest-boundary, slope-foot, coast and persistent biome-interface samples; not verified passes, valleys, saddles, junctions, sightlines or fords.
- destination_search: Bounded 32-block corridor neighborhood, 40–150 effective-depth candidate envelope, top 3 per feature then 12 per Route. These are prototype search restrictions, not balance or exhaustive global optimality.
- destination_tuning: Normalized additive costs and joint duplicate/overlap weights are explicit uncalibrated prototype assumptions. No measured early-game utility is available.
- settlement_access: Existing village piece bounding boxes only; projected sampled edge is not a verified village entrance or occupied settlement. Other structures are not treated as settlements.

Existing Key Locations are regenerated from the revised network by the shared framework. Unverified narrow-neck/open-region references are not promoted into strong destinations merely because they were previously labeled Key Locations.

Block walkability, construction volume, water depth/fords, true chokepoints, visibility, resources, ecology, Hunger and measured travel time remain unavailable. Prior B.1 discrepancy notes remain relevant but were not used for per-seed tuning.

## Stop / next step

Human review of these JSON/SVG fits and especially unresolved/weak handoffs. No physical materialization or Task C is authorized by this run.
