# Local handoff continuation / deep-network reach

Bounded analytical correction from `4ec2dc5b10e317f6a0025907333c16d0a8512c73`. No candidate winner or physical readiness is inferred. Local shape is descriptive, not an ordinal quality score. An ordinary local dead_end is ineligible; directed receives exactly the same viability term as branching/junction.

## Derived local horizon and guards

- **horizon**: Upper edge of the next existing operational depth band after the band containing the handoff; exact boundaries enter the next band.
- **open_ended_guard**: Beyond the last finite band, extend by its existing width (350-250), computationally only; no new deep gameplay band.
- **backward_guard**: Fixed start-depth minus one sample diagonal, never reset during traversal.
- **lateral_detour_guard**: Physical graph path budget = twice the derived forward horizon span; bounds sideways floods and long water jumps, not a travel-cost ceiling.
- **core_removal**: Remove the one-sample-diagonal physical path neighborhood around the terminus.
- **branch_evidence**: Residual connected components merge all within-horizon reconvergence. Require existing four-sample feature support and at least three sample spacings of radial extent beyond the removed core, plus one sample of depth gain or a persistent new region.
- **shape**: 0 dead_end, 1 directed, 2 branching, 3+ junction. No ordinal quality or branch bonus.
- **resolution_limit**: Open connected land counts as one choice; sampled paths do not prove legible valleys/passes. Branching beyond the removed core in one connected component is deliberately not separate immediate choices.

The band markers are inherited from Task A (40/70/110/150/210/250/350). They are applied to the existing geometric Homeland Depth field, not to Travel Cost. For example, a handoff in 40–70 is inspected through 110; no arbitrary 64/100/150-block local radius is introduced. The factor-two physical path cap and one-diagonal fixed backward allowance are provisional locality guards, not gameplay distances. Effective extent is the maximum least-cost travel burden within the local induced graph; physical extent is maximum least-length graph distance. Neither is a promised Minecraft walking distance. Exact-boundary discontinuities and open-ended extension are explicit limitations.

## All eight

| Seed | Selected N/S | Unresolved N/S | Local dead_end pool N/S | Changed destination IDs N/S |
|---|---|---|---|---|
| 930005557 | 3/3 | 0/0 | 19/13 | 0/0 |
| 930006815 | 3/3 | 0/0 | 3/5 | 0/0 |
| 930007222 | 3/3 | 0/0 | 15/24 | 0/2 |
| 930010639 | 3/3 | 0/0 | 11/12 | 0/0 |
| 930012642 | 3/3 | 0/0 | 17/17 | 0/0 |
| 930015734 | 3/2 | 0/1 | 16/30 | 0/0 |
| 930016664 | 2/3 | 1/0 | 31/20 | 0/4 |
| 930019528 | 3/3 | 0/0 | 19/10 | 0/0 |

Changed IDs counts additions plus removals, not a quality delta. Pool rejection applies to the retained best anchor for each feature: this bounded pass does not search second-best anchors to rescue a rejected feature. Missing choices therefore remain evidence/local-analyzer/joint-selection limitations, not demonstrated terrain failure.

## Every selected handoff

N1…/S1… use the JSON selected order. Evidence is sampled feature evidence; all physical entrances, crossings and geographic legibility remain provisional. Travel shows the field / finalized corridor cost. Extents are physical-path / effective (terrain-cost) maxima from the terminus, not from home.

| Seed/team | Destination and evidence | HD | Travel | Local class; gain; branches | Local depth limit; extents | Deep: max HD; regions/types |
|---|---|---|---|---|---|---|
| 930005557/N1 | highland approach: `highland approach:1381` at [-2316, 94, 84]; adjacent sampled surface classifications; connected interface | 36.0 | 64.4/64.371 | directed; 33.7; 1 | 70.0; 67.3/161.3 | 945.5; 90/17 |
| 930005557/N2 | settlement approach: `village:[-152, 15]` at [-2380, 97, 236]; actual_structures village piece bounding boxes projected onto dry surface samples | 55.6 | 69.7/69.74 | branching; 54.1; 2 | 110.0; 107.9/232.2 | 158.8; 4/3 |
| 930005557/N3 | inland-water relationship: `water-3868` at [-2244, 78, 180]; connected sampled water system and dry-bank relationships | 36.0 | 98.8/98.771 | directed; 32.0; 1 | 70.0; 57.9/96.6 | 945.5; 90/17 |
| 930005557/S1 | forest entrance: `forest entrance:6336` at [-1804, 104, 276]; adjacent sampled surface classifications; connected interface | 39.6 | 64.6/64.63 | directed; 28.7; 1 | 70.0; 59.3/130.1 | 787.8; 90/17 |
| 930005557/S2 | highland approach: `highland approach:6909` at [-1940, 95, 188]; adjacent sampled surface classifications; connected interface | 36.0 | 63.9/63.941 | directed; 33.7; 1 | 70.0; 67.3/115.4 | 787.8; 90/17 |
| 930005557/S3 | highland approach: `highland approach:8898` at [-1916, 93, 132]; adjacent sampled surface classifications; connected interface | 49.0 | 64.8/64.827 | branching; 59.9; 2 | 110.0; 120.6/249.1 | 787.8; 90/17 |
| 930006815/N1 | forest entrance: `forest entrance:192` at [316, 88, -284]; adjacent sampled surface classifications; connected interface | 33.0 | 89.9/88.961 | directed; 36.7; 1 | 70.0; 73.9/164.9 | 876.4; 56/15 |
| 930006815/N2 | highland approach: `highland approach:559` at [380, 94, -316]; adjacent sampled surface classifications; connected interface | 44.0 | 66.1/66.1 | directed; 65.7; 1 | 110.0; 131.3/334.0 | 876.4; 56/15 |
| 930006815/N3 | persistent regional threshold: `transition:region-32:region-43` at [356, 80, -172]; boundary between noise-absorbed coherent geographic components | 28.0 | 65.3/65.321 | directed; 41.7; 1 | 70.0; 83.9/175.3 | 876.4; 56/15 |
| 930006815/S1 | forest entrance: `forest entrance:10855` at [-260, 70, -300]; adjacent sampled surface classifications; connected interface | 36.0 | 64.9/64.874 | directed; 33.7; 1 | 70.0; 67.3/131.4 | 674.4; 56/15 |
| 930006815/S2 | persistent regional threshold: `transition:region-10596:region-10600` at [-236, 67, -204]; boundary between noise-absorbed coherent geographic components | 41.0 | 70.3/70.291 | directed; 68.7; 1 | 110.0; 137.9/331.8 | 674.4; 56/15 |
| 930006815/S3 | inland-water relationship: `water-5880` at [-124, 67, -188]; connected sampled water system and dry-bank relationships | 63.6 | 69.8/69.812 | directed; 36.4; 1 | 110.0; 91.9/458.9 | 674.4; 56/15 |
| 930007222/N1 | highland approach: `highland approach:27` at [-2444, 91, 164]; adjacent sampled surface classifications; connected interface | 52.0 | 91.3/91.3 | directed; 56.0; 1 | 110.0; 110.6/258.3 | 934.7; 47/16 |
| 930007222/N2 | slope-foot approach: `slope-foot approach:1235` at [-2404, 80, 148]; adjacent sampled surface classifications; connected interface | 36.0 | 56.8/56.807 | directed; 32.0; 1 | 70.0; 67.3/162.1 | 934.7; 47/16 |
| 930007222/N3 | persistent regional threshold: `transition:region-49:region-56` at [-2468, 72, -12]; boundary between noise-absorbed coherent geographic components | 57.0 | 64.0/64.027 | directed; 52.7; 1 | 110.0; 101.3/233.0 | 268.3; 3/3 |
| 930007222/S1 | forest entrance: `forest entrance:11829` at [-1764, 63, -124]; adjacent sampled surface classifications; connected interface | 28.0 | 64.7/64.657 | directed; 13.0; 1 | 70.0; 83.3/365.7 | 28.0; 0/0 |
| 930007222/S2 | persistent regional threshold: `transition:region-10644:region-11964` at [-1724, 68, -100]; boundary between noise-absorbed coherent geographic components | 52.0 | 73.5/73.456 | branching; 56.9; 2 | 110.0; 115.9/197.8 | 197.4; 6/5 |
| 930007222/S3 | inland-water relationship: `water-12110` at [-1732, 64, -260]; connected sampled water system and dry-bank relationships | 36.0 | 69.4/69.378 | directed; 33.7; 1 | 70.0; 67.3/210.7 | 330.1; 19/8 |
| 930010639/N1 | highland approach: `highland approach:1662` at [1796, 72, -196]; adjacent sampled surface classifications; connected interface | 12.0 | 21.4/21.4 | branching; 57.7; 2 | 70.0; 115.3/476.8 | 992.4; 87/25 |
| 930010639/N2 | coastal access: `water-123` at [1884, 67, -284]; connected sampled water system and dry-bank relationships | 37.7 | 45.4/45.414 | directed; 19.3; 1 | 70.0; 64.6/96.9 | 44.3; 0/0 |
| 930010639/N3 | inland-water relationship: `water-745` at [1732, 64, -212]; connected sampled water system and dry-bank relationships | 44.0 | 64.7/65.255 | directed; 65.7; 1 | 110.0; 131.3/479.5 | 992.4; 86/25 |
| 930010639/S1 | slope-foot approach: `slope-foot approach:11409` at [2316, 79, 60]; adjacent sampled surface classifications; connected interface | 44.0 | 63.1/63.084 | directed; 65.7; 1 | 110.0; 131.9/242.6 | 894.9; 87/25 |
| 930010639/S2 | persistent regional threshold: `transition:region-5500:region-7712` at [2308, 78, -68]; boundary between noise-absorbed coherent geographic components | 13.7 | 64.5/64.5 | directed; 56.0; 1 | 70.0; 112.6/237.4 | 894.9; 87/25 |
| 930010639/S3 | inland-water relationship: `water-10829` at [2452, 78, 84]; connected sampled water system and dry-bank relationships | 96.2 | 127.2/127.183 | directed; 53.5; 1 | 150.0; 106.5/187.8 | 894.9; 86/25 |
| 930012642/N1 | slope-foot approach: `slope-foot approach:1559` at [-2092, 68, -412]; adjacent sampled surface classifications; connected interface | 49.0 | 67.7/67.684 | directed; 60.7; 1 | 110.0; 121.9/278.8 | 1069.0; 68/14 |
| 930012642/N2 | settlement approach: `village:[-133, -24]` at [-2180, 78, -388]; actual_structures village piece bounding boxes projected onto dry surface samples | 36.0 | 64.4/64.371 | directed; 33.7; 1 | 70.0; 67.3/133.7 | 1069.0; 68/14 |
| 930012642/N3 | inland-water relationship: `water-891` at [-2252, 63, -452]; connected sampled water system and dry-bank relationships | 44.0 | 94.6/94.6 | directed; 65.7; 1 | 110.0; 131.9/320.2 | 1069.0; 68/14 |
| 930012642/S1 | forest entrance: `forest entrance:7412` at [-1932, 72, 372]; adjacent sampled surface classifications; connected interface | 60.3 | 105.7/105.707 | directed; 48.6; 1 | 110.0; 99.3/339.3 | 1017.4; 66/14 |
| 930012642/S2 | persistent regional threshold: `transition:region-12466:region-9889` at [-2084, 68, 484]; boundary between noise-absorbed coherent geographic components | 36.3 | 67.5/67.454 | directed; 33.4; 1 | 70.0; 67.3/143.7 | 1017.4; 68/14 |
| 930012642/S3 | inland-water relationship: `water-11721` at [-2004, 68, 364]; connected sampled water system and dry-bank relationships | 28.0 | 65.2/65.171 | directed; 40.3; 1 | 70.0; 83.3/183.6 | 1017.4; 68/14 |
| 930015734/N1 | forest entrance: `forest entrance:51` at [1676, 88, 124]; adjacent sampled surface classifications; connected interface | 49.0 | 64.2/64.227 | directed; 59.9; 1 | 110.0; 120.6/258.3 | 854.7; 74/19 |
| 930015734/N2 | highland approach: `highland approach:53` at [1684, 95, 28]; adjacent sampled surface classifications; connected interface | 41.0 | 63.7/63.664 | directed; 67.3; 1 | 110.0; 137.9/211.7 | 854.7; 74/20 |
| 930015734/N3 | slope-foot approach: `slope-foot approach:3619` at [1844, 95, 76]; adjacent sampled surface classifications; connected interface | 52.0 | 66.7/66.714 | directed; 56.3; 1 | 110.0; 115.9/286.1 | 854.7; 74/20 |
| 930015734/S1 | persistent regional threshold: `transition:region-13529:region-6399` at [2476, 78, 12]; boundary between noise-absorbed coherent geographic components | 17.0 | 60.5/48.175 | branching; 40.0; 2 | 70.0; 104.0/223.2 | 57.0; 2/2 |
| 930015734/S2 | inland-water relationship: `water-1128` at [2380, 67, -100]; connected sampled water system and dry-bank relationships | 33.0 | 65.3/65.289 | directed; 27.0; 1 | 70.0; 70.6/309.6 | 116.3; 2/2 |
| 930016664/N1 | slope-foot approach: `slope-foot approach:1802` at [-164, 79, 2436]; adjacent sampled surface classifications; connected interface | 28.0 | 91.9/91.9 | directed; 41.7; 1 | 70.0; 83.3/198.8 | 211.5; 6/5 |
| 930016664/N2 | inland-water relationship: `water-1605` at [-292, 72, 2460]; connected sampled water system and dry-bank relationships | 29.7 | 74.5/74.494 | branching; 40.0; 2 | 70.0; 80.6/460.8 | 211.5; 6/5 |
| 930016664/S1 | forest entrance: `forest entrance:10324` at [36, 88, 1692]; adjacent sampled surface classifications; connected interface | 36.3 | 65.2/65.177 | directed; 33.4; 1 | 70.0; 64.6/132.9 | 1032.3; 83/20 |
| 930016664/S2 | highland approach: `highland approach:901` at [148, 94, 1612]; adjacent sampled surface classifications; connected interface | 20.0 | 65.8/65.8 | directed; 49.7; 1 | 70.0; 99.9/200.0 | 1032.3; 86/20 |
| 930016664/S3 | inland-water relationship: `water-13872` at [52, 91, 1556]; connected sampled water system and dry-bank relationships | 37.7 | 58.5/58.55 | directed; 30.3; 1 | 70.0; 61.3/90.4 | 37.7; 0/0 |
| 930019528/N1 | forest entrance: `forest entrance:1464` at [-2108, 69, 372]; adjacent sampled surface classifications; connected interface | 36.0 | 59.3/59.314 | directed; 33.7; 1 | 70.0; 67.3/273.5 | 421.4; 21/8 |
| 930019528/N2 | inland-water relationship: `water-1465` at [-2108, 66, 412]; connected sampled water system and dry-bank relationships | 36.0 | 64.0/64.027 | directed; 33.7; 1 | 70.0; 67.3/297.1 | 421.4; 21/8 |
| 930019528/N3 | inland-water relationship: `water-475` at [-1972, 67, 340]; connected sampled water system and dry-bank relationships | 33.0 | 60.7/60.657 | branching; 36.7; 2 | 70.0; 73.9/288.8 | 1033.4; 55/14 |
| 930019528/S1 | highland approach: `highland approach:12560` at [-1876, 93, -436]; adjacent sampled surface classifications; connected interface | 41.0 | 62.7/62.655 | directed; 68.7; 1 | 110.0; 137.9/315.1 | 1174.4; 75/17 |
| 930019528/S2 | slope-foot approach: `slope-foot approach:9965` at [-1836, 86, -260]; adjacent sampled surface classifications; connected interface | 92.0 | 141.3/141.284 | directed; 57.7; 1 | 150.0; 115.9/262.4 | 1174.4; 72/17 |
| 930019528/S3 | persistent regional threshold: `transition:region-11028:region-11888` at [-1732, 87, -420]; boundary between noise-absorbed coherent geographic components | 36.0 | 68.3/68.284 | directed; 33.7; 1 | 70.0; 67.3/101.8 | 1174.4; 75/17 |

Observed local scale across selected handoffs (not desired values): physical_extent 57.9–137.9; effective_extent 90.4–479.5; depth_gain 13.0–68.7.

Class distribution: {"directed": 39, "branching": 7}. The derived band horizon bounds the analysis but does not establish scale stability under perturbations; terrain burden can produce a wide effective extent. Inspect JSON guard_contact and discarded_pockets rather than treating all reported extents as equally confident.

Guard contact among selected modest local analyses: depth_ceiling 44/46, depth_floor 44/46, physical_budget 43/46. A contact means reachable terrain touches an excluded edge, not proof that a useful branch was truncated. Frequent physical-budget contacts mean the provisional lateral-flood guard materially defines locality; horizon stability is still unresolved.

Both remaining unresolved choices (930015734 South and 930016664 North) were already unresolved in the baseline. The selected IDs on those teams are unchanged. Classify these as unresolved destination/joint-fitter capability, not demonstrated terrain failure or a newly lost handoff caused by this local filter.

## Material selection changes from the previous pass

- 930007222/south: {"removed":[{"id":"water-9038","kind":"coastal access","anchor":{"sample":[83,81],"world_xyz":[-1828,64,-140]},"current_local_class":"dead_end"}],"added":[{"id":"forest entrance:11829","kind":"forest entrance","anchor":{"sample":[81,89],"world_xyz":[-1764,63,-124]}}],"retained_anchor_changes":[]}
- 930016664/south: {"removed":[{"id":"highland approach:11611","kind":"highland approach","anchor":{"sample":[52,117],"world_xyz":[12,83,1636]},"current_local_class":"directed"},{"id":"slope-foot approach:13099","kind":"slope-foot approach","anchor":{"sample":[34,122],"world_xyz":[156,96,1596]},"current_local_class":"branching"}],"added":[{"id":"highland approach:901","kind":"highland approach","anchor":{"sample":[35,120],"world_xyz":[148,94,1612]}},{"id":"water-13872","kind":"inland-water relationship","anchor":{"sample":[47,127],"world_xyz":[52,91,1556]}}],"retained_anchor_changes":[]}

Other destination IDs and anchors are unchanged; local classifications and extents are new measurements. The previous deep method is retained, so whole-map matrices change only when selected inputs change.

## Focused before / after

### 930005557


| Team | Prior destination / terminus | Prior broad class; gain; ports | Current state |
|---|---|---|---|
| north | highland approach `highland approach:1381` / [-2316, 94, 84] | junction; 909.5; 9 | retained: local directed, gain 33.7 |
| north | settlement approach `village:[-152, 15]` / [-2380, 97, 236] | junction; 103.2; 2 | retained: local branching, gain 54.1 |
| north | inland-water relationship `water-3868` / [-2244, 78, 180] | junction; 909.5; 20 | retained: local directed, gain 32.0 |

north additions: none. Unresolved: 0. Local classes: directed, branching, directed.

Weak evidence: highland approach: sampled access edge; physical entrance and visual legibility unverified; settlement approach: village entrance, occupancy and unobstructed sightline unverified; inland-water relationship: water depth, ford/bridge viability and navigational significance unavailable.


| Team | Prior destination / terminus | Prior broad class; gain; ports | Current state |
|---|---|---|---|
| south | forest entrance `forest entrance:6336` / [-1804, 104, 276] | junction; 748.2; 4 | retained: local directed, gain 28.7 |
| south | highland approach `highland approach:6909` / [-1940, 95, 188] | junction; 751.8; 8 | retained: local directed, gain 33.7 |
| south | highland approach `highland approach:8898` / [-1916, 93, 132] | junction; 738.8; 21 | retained: local branching, gain 59.9 |

south additions: none. Unresolved: 0. Local classes: directed, directed, branching.

Weak evidence: forest entrance: sampled access edge; physical entrance and visual legibility unverified; highland approach: sampled access edge; physical entrance and visual legibility unverified; highland approach: sampled access edge; physical entrance and visual legibility unverified.

### 930010639


| Team | Prior destination / terminus | Prior broad class; gain; ports | Current state |
|---|---|---|---|
| north | highland approach `highland approach:1662` / [1796, 72, -196] | junction; 980.4; 10 | retained: local branching, gain 57.7 |
| north | coastal access `water-123` / [1884, 67, -284] | dead_end; 6.6; 0 | retained: local directed, gain 19.3 |
| north | inland-water relationship `water-745` / [1732, 64, -212] | junction; 948.4; 4 | retained: local directed, gain 65.7 |

north additions: none. Unresolved: 0. Local classes: branching, directed, directed.

Weak evidence: highland approach: sampled access edge; physical entrance and visual legibility unverified; coastal access: water depth, ford/bridge viability and navigational significance unavailable; inland-water relationship: water depth, ford/bridge viability and navigational significance unavailable.


| Team | Prior destination / terminus | Prior broad class; gain; ports | Current state |
|---|---|---|---|
| south | slope-foot approach `slope-foot approach:11409` / [2316, 79, 60] | junction; 850.9; 7 | retained: local directed, gain 65.7 |
| south | persistent regional threshold `transition:region-5500:region-7712` / [2308, 78, -68] | junction; 881.2; 8 | retained: local directed, gain 56.0 |
| south | inland-water relationship `water-10829` / [2452, 78, 84] | junction; 798.7; 3 | retained: local directed, gain 53.5 |

south additions: none. Unresolved: 0. Local classes: directed, directed, directed.

Weak evidence: slope-foot approach: 32-block flat-to-rising profile; hill scale and human-perceived significance unverified; persistent regional threshold: component boundary is not a verified landmark or natural pass; inland-water relationship: water depth, ford/bridge viability and navigational significance unavailable.

#### Primary analyzer questions

1. Previous North coastal broad dead_end still selected: yes (local viability must be judged separately from its old broad class).
2. Replacement/additions and North supported count are listed above: 3/3. No forced third.
3. Team local classes are listed above in selected order.
4. The local component rule separates directed (one persistent outward component) from branching/junction (two/three-plus); it does not inherit distant regional ports. Open interconnected terrain can conservatively collapse to directed, and turns/forks beyond the immediate removed neighborhood remain one initial choice. Synthetic tests verify the distinctions; player-perceived formation quality remains unverified.
5. Deep reach is preserved unchanged at identical anchors and still exposes 25 broader region types in the modest proxy, with 33 inland water systems in the unchanged terrain graph. Represented types: alpine/highland/open/gentle/inland, alpine/highland/open/rough/inland, alpine/highland/wooded/gentle/inland, alpine/highland/wooded/rough/inland, alpine/lowland/open/gentle/inland, alpine/lowland/open/rough/inland, alpine/lowland/wooded/gentle/inland, alpine/lowland/wooded/rough/inland, open/highland/open/gentle/inland, open/highland/open/rough/inland, open/lowland/open/gentle/inland, open/lowland/open/rough/inland, shore/lowland/open/gentle/inland, shore/lowland/open/rough/inland, snow/highland/open/gentle/inland, snow/highland/open/rough/inland, snow/highland/wooded/rough/inland, snow/lowland/open/gentle/inland, snow/lowland/open/rough/coastal, snow/lowland/open/rough/inland, snow/lowland/wooded/gentle/inland, snow/lowland/wooded/rough/inland, woodland/highland/wooded/rough/inland, woodland/lowland/wooded/gentle/inland, woodland/lowland/wooded/rough/inland. This retains sampled highland/open-lowland/woodland/coastal reach, not proof of the manually observed river/pass/valley hierarchy or actual crossings. Water identities remain independent evidence. Manual geography is not invalidated by local filtering.
6. Exact physical/effective local extents for all six (or fewer) primary handoffs are in the all-handoff table and JSON; no distances are canonized.

Coastal retention evidence: start HD 37.657, fixed floor 26.343, actual minimum HD 28.000, maximum 56.971; 1 persistent local choice (22 samples), locally reached regions ['region-2224', 'region-3007'], new types []. Small backward detour required: True; natural/modest/major branches 1/1/1. This is the same generic fixed-floor rule used for every feature, not a coast exception. Its old strict-forward deep reach remains limited; local viability does not imply rich eventual network participation.

### 930012642


| Team | Prior destination / terminus | Prior broad class; gain; ports | Current state |
|---|---|---|---|
| north | slope-foot approach `slope-foot approach:1559` / [-2092, 68, -412] | junction; 1020.0; 13 | retained: local directed, gain 60.7 |
| north | settlement approach `village:[-133, -24]` / [-2180, 78, -388] | directed; 1033.0; 1 | retained: local directed, gain 33.7 |
| north | inland-water relationship `water-891` / [-2252, 63, -452] | junction; 1025.0; 17 | retained: local directed, gain 65.7 |

north additions: none. Unresolved: 0. Local classes: directed, directed, directed.

Weak evidence: slope-foot approach: 32-block flat-to-rising profile; hill scale and human-perceived significance unverified; settlement approach: village entrance, occupancy and unobstructed sightline unverified; inland-water relationship: water depth, ford/bridge viability and navigational significance unavailable.


| Team | Prior destination / terminus | Prior broad class; gain; ports | Current state |
|---|---|---|---|
| south | forest entrance `forest entrance:7412` / [-1932, 72, 372] | directed; 957.1; 1 | retained: local directed, gain 48.6 |
| south | persistent regional threshold `transition:region-12466:region-9889` / [-2084, 68, 484] | junction; 981.1; 2 | retained: local directed, gain 33.4 |
| south | inland-water relationship `water-11721` / [-2004, 68, 364] | junction; 989.4; 7 | retained: local directed, gain 40.3 |

south additions: none. Unresolved: 0. Local classes: directed, directed, directed.

Weak evidence: forest entrance: sampled access edge; physical entrance and visual legibility unverified; persistent regional threshold: component boundary is not a verified landmark or natural pass; inland-water relationship: water depth, ford/bridge viability and navigational significance unavailable.

## All-eight topology (unchanged deep-network method)

Matrices use selected-order N1…/S1…. Same-team cells are minimax shared Homeland Depth; opposing cells show North/South depths. These remain deep-network diagnostics and never determine local class. The report shows conditional modest graphs; JSON retains all three sensitivities.

### 930005557


north lateral

| | N1 | N2 | N3 |
|---|---|---|---|
| N1 | — | 55.6 | 283.2 |
| N2 | 55.6 | — | 283.2 |
| N3 | 283.2 | 283.2 | — |

Opening exclusivity: [{"destination":"highland approach:1381","exclusive_depth_gain":19.598,"first_same_team_connection_depth":55.598},{"destination":"village:[-152, 15]","exclusive_depth_gain":0,"first_same_team_connection_depth":55.598},{"destination":"water-3868","exclusive_depth_gain":247.245,"first_same_team_connection_depth":283.245}]

south lateral

| | S1 | S2 | S3 |
|---|---|---|---|
| S1 | — | 87.6 | 87.6 |
| S2 | 87.6 | — | 49.0 |
| S3 | 87.6 | 49.0 | — |

Opening exclusivity: [{"destination":"forest entrance:6336","exclusive_depth_gain":48.0,"first_same_team_connection_depth":87.598},{"destination":"highland approach:6909","exclusive_depth_gain":12.971,"first_same_team_connection_depth":48.971},{"destination":"highland approach:8898","exclusive_depth_gain":0,"first_same_team_connection_depth":48.971}]

North/South convergence

| | S1 | S2 | S3 |
|---|---|---|---|
| N1 | 220.9/217.5 | 220.9/217.5 | 220.9/217.5 |
| N2 | 60.3/460.0 | 60.3/460.0 | 60.3/460.0 |
| N3 | 188.0/188.0 | 188.0/188.0 | 188.0/188.0 |

Earliest opposing depth: 188.0/188.0; modest pair coverage 1.00; broader shared regions 91; isolated {"north": [], "south": []}; major-only pairs [].

Diagnostics: No unresolved analytical corridor choices; not a physical pass.
### 930006815


north lateral

| | N1 | N2 | N3 |
|---|---|---|---|
| N1 | — | 44.0 | 60.0 |
| N2 | 44.0 | — | 60.0 |
| N3 | 60.0 | 60.0 | — |

Opening exclusivity: [{"destination":"forest entrance:192","exclusive_depth_gain":11.029,"first_same_team_connection_depth":44.0},{"destination":"highland approach:559","exclusive_depth_gain":0,"first_same_team_connection_depth":44.0},{"destination":"transition:region-32:region-43","exclusive_depth_gain":32.0,"first_same_team_connection_depth":60.0}]

south lateral

| | S1 | S2 | S3 |
|---|---|---|---|
| S1 | — | 50.9 | 69.7 |
| S2 | 50.9 | — | 69.7 |
| S3 | 69.7 | 69.7 | — |

Opening exclusivity: [{"destination":"forest entrance:10855","exclusive_depth_gain":14.912,"first_same_team_connection_depth":50.912},{"destination":"transition:region-10596:region-10600","exclusive_depth_gain":9.941,"first_same_team_connection_depth":50.912},{"destination":"water-5880","exclusive_depth_gain":6.059,"first_same_team_connection_depth":69.657}]

North/South convergence

| | S1 | S2 | S3 |
|---|---|---|---|
| N1 | 244.0/252.0 | 244.0/252.0 | 244.0/252.0 |
| N2 | 244.0/252.0 | 244.0/252.0 | 244.0/252.0 |
| N3 | 244.0/252.0 | 244.0/252.0 | 244.0/252.0 |

Earliest opposing depth: 244.0/252.0; modest pair coverage 1.00; broader shared regions 57; isolated {"north": [], "south": []}; major-only pairs [].

Diagnostics: No unresolved analytical corridor choices; not a physical pass.
### 930007222


north lateral

| | N1 | N2 | N3 |
|---|---|---|---|
| N1 | — | 52.0 | — |
| N2 | 52.0 | — | — |
| N3 | — | — | — |

Opening exclusivity: [{"destination":"highland approach:27","exclusive_depth_gain":0,"first_same_team_connection_depth":52.0},{"destination":"slope-foot approach:1235","exclusive_depth_gain":16.0,"first_same_team_connection_depth":52.0},{"destination":"transition:region-49:region-56","exclusive_depth_gain":null,"first_same_team_connection_depth":null}]

south lateral

| | S1 | S2 | S3 |
|---|---|---|---|
| S1 | — | — | — |
| S2 | — | — | 52.0 |
| S3 | — | 52.0 | — |

Opening exclusivity: [{"destination":"forest entrance:11829","exclusive_depth_gain":null,"first_same_team_connection_depth":null},{"destination":"transition:region-10644:region-11964","exclusive_depth_gain":0,"first_same_team_connection_depth":52.0},{"destination":"water-12110","exclusive_depth_gain":16.0,"first_same_team_connection_depth":52.0}]

North/South convergence

| | S1 | S2 | S3 |
|---|---|---|---|
| N1 | — | — | — |
| N2 | — | — | — |
| N3 | — | — | — |

Earliest opposing depth: unresolved; modest pair coverage 0.00; broader shared regions 0; isolated {"north": ["highland approach:27", "slope-foot approach:1235", "transition:region-49:region-56"], "south": ["forest entrance:11829", "transition:region-10644:region-11964", "water-12110"]}; major-only pairs [[0, 0], [0, 1], [0, 2], [1, 0], [1, 1], [1, 2], [2, 0], [2, 1], [2, 2]].

Diagnostics: No unresolved analytical corridor choices; not a physical pass.
### 930010639


north lateral

| | N1 | N2 | N3 |
|---|---|---|---|
| N1 | — | 148.3 | 148.3 |
| N2 | 148.3 | — | — |
| N3 | 148.3 | — | — |

Opening exclusivity: [{"destination":"highland approach:1662","exclusive_depth_gain":136.284,"first_same_team_connection_depth":148.284},{"destination":"water-123","exclusive_depth_gain":110.627,"first_same_team_connection_depth":148.284},{"destination":"water-745","exclusive_depth_gain":104.284,"first_same_team_connection_depth":148.284}]

south lateral

| | S1 | S2 | S3 |
|---|---|---|---|
| S1 | — | 44.0 | 96.2 |
| S2 | 44.0 | — | 96.2 |
| S3 | 96.2 | 96.2 | — |

Opening exclusivity: [{"destination":"slope-foot approach:11409","exclusive_depth_gain":0,"first_same_team_connection_depth":44.0},{"destination":"transition:region-5500:region-7712","exclusive_depth_gain":30.343,"first_same_team_connection_depth":44.0},{"destination":"water-10829","exclusive_depth_gain":0,"first_same_team_connection_depth":96.167}]

North/South convergence

| | S1 | S2 | S3 |
|---|---|---|---|
| N1 | 588.0/637.3 | 588.0/637.3 | 588.0/637.3 |
| N2 | 37.7/699.6 | 37.7/699.6 | 37.7/699.6 |
| N3 | 588.0/637.3 | 588.0/637.3 | 588.0/637.3 |

Earliest opposing depth: 588.0/637.3; modest pair coverage 1.00; broader shared regions 88; isolated {"north": [], "south": []}; major-only pairs [].

Diagnostics: No unresolved analytical corridor choices; not a physical pass.
### 930012642


north lateral

| | N1 | N2 | N3 |
|---|---|---|---|
| N1 | — | 49.0 | 71.6 |
| N2 | 49.0 | — | 65.0 |
| N3 | 71.6 | 65.0 | — |

Opening exclusivity: [{"destination":"slope-foot approach:1559","exclusive_depth_gain":0,"first_same_team_connection_depth":48.971},{"destination":"village:[-133, -24]","exclusive_depth_gain":12.971,"first_same_team_connection_depth":48.971},{"destination":"water-891","exclusive_depth_gain":20.971,"first_same_team_connection_depth":64.971}]

south lateral

| | S1 | S2 | S3 |
|---|---|---|---|
| S1 | — | 77.9 | 60.3 |
| S2 | 77.9 | — | 77.9 |
| S3 | 60.3 | 77.9 | — |

Opening exclusivity: [{"destination":"forest entrance:7412","exclusive_depth_gain":0,"first_same_team_connection_depth":60.284},{"destination":"transition:region-12466:region-9889","exclusive_depth_gain":41.657,"first_same_team_connection_depth":77.941},{"destination":"water-11721","exclusive_depth_gain":32.284,"first_same_team_connection_depth":60.284}]

North/South convergence

| | S1 | S2 | S3 |
|---|---|---|---|
| N1 | 426.9/427.2 | 426.9/427.2 | 426.9/427.2 |
| N2 | 426.9/427.2 | 426.9/427.2 | 426.9/427.2 |
| N3 | 426.9/427.2 | 426.9/427.2 | 426.9/427.2 |

Earliest opposing depth: 426.9/427.2; modest pair coverage 1.00; broader shared regions 69; isolated {"north": [], "south": []}; major-only pairs [].

Diagnostics: No unresolved analytical corridor choices; not a physical pass.
### 930015734


north lateral

| | N1 | N2 | N3 |
|---|---|---|---|
| N1 | — | 49.0 | 55.6 |
| N2 | 49.0 | — | 52.0 |
| N3 | 55.6 | 52.0 | — |

Opening exclusivity: [{"destination":"forest entrance:51","exclusive_depth_gain":0,"first_same_team_connection_depth":48.971},{"destination":"highland approach:53","exclusive_depth_gain":8.0,"first_same_team_connection_depth":48.971},{"destination":"slope-foot approach:3619","exclusive_depth_gain":0,"first_same_team_connection_depth":52.0}]

south lateral

| | S1 | S2 |
|---|---|---|
| S1 | — | — |
| S2 | — | — |

Opening exclusivity: [{"destination":"transition:region-13529:region-6399","exclusive_depth_gain":null,"first_same_team_connection_depth":null},{"destination":"water-1128","exclusive_depth_gain":null,"first_same_team_connection_depth":null}]

North/South convergence

| | S1 | S2 |
|---|---|---|
| N1 | — | — |
| N2 | — | — |
| N3 | — | — |

Earliest opposing depth: unresolved; modest pair coverage 0.00; broader shared regions 0; isolated {"north": ["forest entrance:51", "highland approach:53", "slope-foot approach:3619"], "south": ["transition:region-13529:region-6399", "water-1128"]}; major-only pairs [[0, 0], [0, 1], [1, 0], [1, 1], [2, 0], [2, 1]].

Diagnostics: south: 1 unresolved choices; local viability/evidence/constructability/joint differentiation or analyzer capability, not a seed rejection
### 930016664


north lateral

| | N1 | N2 |
|---|---|---|
| N1 | — | 109.7 |
| N2 | 109.7 | — |

Opening exclusivity: [{"destination":"slope-foot approach:1802","exclusive_depth_gain":81.657,"first_same_team_connection_depth":109.657},{"destination":"water-1605","exclusive_depth_gain":80.0,"first_same_team_connection_depth":109.657}]

south lateral

| | S1 | S2 | S3 |
|---|---|---|---|
| S1 | — | 36.3 | — |
| S2 | 36.3 | — | — |
| S3 | — | — | — |

Opening exclusivity: [{"destination":"forest entrance:10324","exclusive_depth_gain":0,"first_same_team_connection_depth":36.284},{"destination":"highland approach:901","exclusive_depth_gain":16.284,"first_same_team_connection_depth":36.284},{"destination":"water-13872","exclusive_depth_gain":null,"first_same_team_connection_depth":null}]

North/South convergence

| | S1 | S2 | S3 |
|---|---|---|---|
| N1 | — | — | — |
| N2 | — | — | — |

Earliest opposing depth: unresolved; modest pair coverage 0.00; broader shared regions 0; isolated {"north": ["slope-foot approach:1802", "water-1605"], "south": ["forest entrance:10324", "highland approach:901", "water-13872"]}; major-only pairs [[0, 0], [0, 1], [0, 2], [1, 0], [1, 1], [1, 2]].

Diagnostics: north: 1 unresolved choices; local viability/evidence/constructability/joint differentiation or analyzer capability, not a seed rejection
### 930019528


north lateral

| | N1 | N2 | N3 |
|---|---|---|---|
| N1 | — | 36.0 | — |
| N2 | 36.0 | — | — |
| N3 | — | — | — |

Opening exclusivity: [{"destination":"forest entrance:1464","exclusive_depth_gain":0,"first_same_team_connection_depth":36.0},{"destination":"water-1465","exclusive_depth_gain":0,"first_same_team_connection_depth":36.0},{"destination":"water-475","exclusive_depth_gain":null,"first_same_team_connection_depth":null}]

south lateral

| | S1 | S2 | S3 |
|---|---|---|---|
| S1 | — | 92.0 | 41.0 |
| S2 | 92.0 | — | 92.0 |
| S3 | 41.0 | 92.0 | — |

Opening exclusivity: [{"destination":"highland approach:12560","exclusive_depth_gain":0,"first_same_team_connection_depth":40.971},{"destination":"slope-foot approach:9965","exclusive_depth_gain":0,"first_same_team_connection_depth":92.0},{"destination":"transition:region-11028:region-11888","exclusive_depth_gain":4.971,"first_same_team_connection_depth":40.971}]

North/South convergence

| | S1 | S2 | S3 |
|---|---|---|---|
| N1 | 41.0/794.4 | 41.0/794.4 | 41.0/794.4 |
| N2 | 41.0/794.4 | 41.0/794.4 | 41.0/794.4 |
| N3 | 404.4/412.3 | 404.4/412.3 | 404.4/412.3 |

Earliest opposing depth: 404.4/412.3; modest pair coverage 1.00; broader shared regions 76; isolated {"north": [], "south": []}; major-only pairs [].

Diagnostics: No unresolved analytical corridor choices; not a physical pass.

## Limitations and scope verification

- Local natural/modest/major counts are cumulative sensitivity graphs. Adding edges can merge branches, so counts are not additive or necessarily monotonic. Major includes unverified water transport; it is not proof that large terrain edits are required.
- Backtracking dependency distinguishes a necessary allowed small backward detour (strict-forward has no branch) from viability available only below the fixed local floor. The latter remains locally dead_end/ineligible. Additional reach can require backtracking even when a viable no-backtracking choice already exists; this is separately counted.
- No second-best feature anchor rescue, segmentation redesign, destination-weight tuning, map resizing or desired topology target changes. Immediate core/persistence and derived-band sensitivity remain analyzer limitations, not seed failure evidence.
- No new seeds generated: runner consumes exactly the existing eight-finalist manifest; no search/generation entry point is called.
- No Minecraft worlds changed/materialized: before/after hashes for all available finalist and Task B world files are in verification.json. Unavailable worlds, if any, are explicitly listed there.
- No Task C: only analyzer, tests, analytical JSON/SVG and documentation outputs.
- No historical Route dependency: analyze accepts only terrain, fixed homelands and parameters. Deletion/scrambling and mocked-old-solver regression tests are retained.
- Prior analytical outputs preserved: verification.json includes hashes of all previous Task A/refit/network/Task B result files and input candidates.
- Test count/result: **107 tests passed** in 55.971 seconds; see TEST_RESULTS.md.

Reproduce: `python3 implementation/worldgen/fit_default_task_a_network.py`; `python3 -m unittest discover -s implementation/worldgen/tests -q`.

Stop: analytical human review only. No new physical greybox, Task C, skeleton freeze, or seed rejection is authorized by this pass.
