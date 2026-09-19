# SHADOW economic / Practical Reach revalidation — 18 September 2026

**Working / Prototype-test. No selection decision.** This correction used only Minecraft MOBA; no VWM artifact was imported or propagated.

## Verified corpus and provenance

Repository: `https://github.com/iracarranza/minecraftmoba.git`. Branch: `main`. Starting commit: `be06814fbcbe8bf8e62386ef591bf48e296faf17`. Separately inspected local `origin/main`: `9c85c915599f410cdadf26dbc76de9b7b9b55220` (38 commits ahead; not a claim of remote freshness). The dirty working tree was preserved. Required identity checks were run before analysis: `git remote -v`, `git rev-parse --show-toplevel`, `git status --short --branch`, `git log -5 --oneline`.

[inventory.json](inventory.json) records deterministic file hashes for working-tree and pinned-reference sources. [capabilities.json](capabilities.json) records inspected implementations and their actual scope. Hash inventory membership alone does not establish capability. Discovery included untracked design additions and both seed IDs, rather than relying only on requested filenames. The stale browser wiki is not authority.

- **canonical design:** `maps.md; objectives.md; infrastructure.md; classes.md`. Working-tree authorities exist with local edits; pinned origin/main alternatives hashed separately. No reconciliation or authority edits made.
- **current manuscript:** `docs/manuscript/Minecraft-MOBA-Design.md; docs/manuscript/2026-09-14-Economic-Calibration-Supplement.md; docs/manuscript/2026-09-14-UAU-Work-Stock-and-Practical-Reach.md`. Both Sept 14 supplements exist in pinned origin/main, not this older working tree. Later UAU supplement provides Working 256 WP/UAU and resolves the preceding supplement’s historical missing-UAU boundary. Numerical resource fields remain fixtures, not generation quotas.
- **seed search and fitter:** `implementation/worldgen/vanilla_search/task_a.py; task_a_destinations.py; task_a_local.py; task_a_network.py; specs/mapseedsearchspec4.md`. Frozen oriented grids, homeland-relative fields, destination-first handoffs, local continuation and deep networks. Eight-block terrain; analytical travel cost, not seconds or Hunger.
- **Opening Opportunity Relationship:** `implementation/worldgen/vanilla_search/OPPORTUNITY_SHADOW.md; implementation/worldgen/results/default_opportunity_shadow_2026-09-11/REPORT.md; both seed fit.json files`. Conditional water reach, affordance/payoff chains, set overlap and substitution. Preserved verbatim in measurements.json; not reranked.
- **surface extraction:** `implementation/worldgen/vanilla_search/extract.py`. Anvil palette decoding, 8-block terrain/biome/water/canopy samples, structure starts and section-level cave/water/lava palette presence. Presence is not a resource block count or entrance proof.
- **block decoding:** `implementation/worldgen/serialization/region.py; implementation/worldgen/serialization/nbt.py; implementation/worldgen/serialization/inspect.py`. Existing read_region and VanillaChunk reused. WorldReader also supports block counts but its validation is tied to authored serialization fixtures; it is not evidence for target-seed accessible resources.
- **resource contract:** `implementation/worldgen/analysis/resource_contract.py`. Vocabulary/existence validation only; no abundance, accessibility, depletion or economic field measurement.
- **physical worlds:** `artifacts/worldgen/staged_default_2026-09-09/worlds/Default_930010639_2048_0; artifacts/worldgen/staged_default_2026-09-09/worlds/Default_930012642_-2048_0`. Both worlds exist locally outside tracked corpus. level.dat seed checked, version recorded, consumed region hashes checked before/after. GREYBOX.json says terrain unchanged, but pristine-generation equivalence is not independently established.
- **Task B preservation:** `implementation/worldgen/results/default_task_b_2026-09-10/<seed>/preservation_audit.json`. Separate physical authoring audit exists; do not confuse its edited output with the staged inspection world measured here.
- **new bounded ore observation:** `implementation/worldgen/economic_shadow.py`. Read-only full block-state observations in handoff-centered cuboids, clipped to recorded seed bounds. Counts, material breadth, six-face components, boundary truncation and vertical distribution; no yield, accessibility or time model.

## Method and evidence boundaries

Raw observations: current-world ore block identities and world coordinates in twelve separate JSON exports, linked and hashed in [measurements.json](measurements.json). No world/server generation or writing occurred. Java 1.21.11 is recorded by the original candidates; runtime world metadata is included in the artifact. These are current inspection snapshots, not a proof that no player or simulation has ever altered them.

Derived measurements: each frozen Handoff anchors an inclusive ±32-block horizontal square, from Y=-64 through 319, clipped to the candidate’s recorded region. This is a **sampling fixture**, not a Practical Reach radius. Required chunks must be fully generated; missing/non-full chunks cannot become zero resources. Initial scanning extended 930010639/S3 past the recorded eastern boundary; the final scan clips it to X=2479, yielding 60×65 columns rather than 65×65. Other windows are 65×65. No extrapolation or area-normalized economic comparison is made. Windows may overlap; do not sum them into seed or team stocks.

Counted vocabulary: coal, copper, iron, gold, redstone, lapis, diamond and emerald ores, merging stone/deepslate variants by material while preserving exact identities in raw exports. Six-face same-material components describe observed deposit structure; diagonally adjacent ore remains separate. Boundary components can continue outside the window. Component fraction of observed stock is a finite-stock concentration/depletion proxy only: removing a component would remove that fraction of this snapshot’s counted ore, not that fraction of an accessible economic field. Vertical 32-block bands are descriptive. No elapsed depletion, exposed-face, cave access or recoverability measurements are asserted.

Mixed-resource breadth is seven observed ore materials in each window. It does not measure economic breadth capacity, food, wood, fuel preparation, crops, animals, inventory or operational portfolio. Zero emerald means no emerald block observed inside that bounded snapshot, not absence from the seed.

## Concrete measurements

HD is inherited shortest horizontal graph length from outward Homeland edges, independent of rise/canopy penalties. Travel is inherited terrain-weighted graph cost. Optimistic water/major-grade links are conditional; neither field is measured time, a level gate or utility. All ore numbers below are physical blocks throughout the sampled vertical column, including buried and currently inaccessible deposits.

| Seed / Handoff | HD / Travel | Copper | Iron | Coal | Diamond | Ore materials |
|---|---:|---:|---:|---:|---:|---:|
| 930010639 / N1 | 12.000 / 21.400 | 1727 | 1215 | 1980 | 394 | 7 |
| 930010639 / N2 | 37.657 / 45.414 | 1297 | 1140 | 1026 | 399 | 7 |
| 930010639 / N3 | 44.000 / 64.727 | 1452 | 1274 | 1750 | 488 | 7 |
| 930010639 / S1 | 44.000 / 63.084 | 1617 | 1174 | 2087 | 378 | 7 |
| 930010639 / S2 | 13.657 / 64.500 | 1720 | 1284 | 1762 | 481 | 7 |
| 930010639 / S3 | 96.167 / 127.183 | 1397 | 1143 | 1638 | 348 | 7 |
| 930012642 / N1 | 48.971 / 67.684 | 1371 | 1210 | 1841 | 412 | 7 |
| 930012642 / N2 | 36.000 / 64.371 | 1742 | 1389 | 2045 | 389 | 7 |
| 930012642 / N3 | 44.000 / 94.600 | 1427 | 1350 | 1583 | 374 | 7 |
| 930012642 / S1 | 60.284 / 105.707 | 1683 | 1244 | 1828 | 401 | 7 |
| 930012642 / S2 | 36.284 / 67.454 | 1520 | 1209 | 1579 | 433 | 7 |
| 930012642 / S3 | 28.000 / 65.171 | 1749 | 1350 | 1500 | 458 | 7 |

All eight material counts, component counts/largest sizes, clipped components, vertical bands, exact bounds, and prior opportunity relationships are machine-readable. These large full-depth ore totals cannot be compared directly to the supplement’s ordinary economically relevant Copper 45–55 or Iron 130–140 fixtures: physical existence is not rational acquisition.

Existing sampled geographic differences also remain valid as proxies:

| Seed | Surface-water fraction | Largest water body, sampled blocks² | Largest forest, sampled blocks² | Highland depth, blocks |
|---|---:|---:|---:|---:|
| 930010639 | 0.3878 | 308096 | 163968 | 416 |
| 930012642 | 0.2168 | 80256 | 84864 | 472 |

## Preserved opening relationships

930010639 N1 remains the highland approach at [1796,72,-196]: structurally supported, with immediate modest entry unresolved. N2 at [1884,67,-284] and N3 at [1732,64,-212] retain conditional sampled water-network-122 access and village payoff. N2 has an 824-block water path extent; N3 has 1216, neither a navigability/time claim. The shared village identity is `village:[103, -9]`; N2’s reported water-to-village shore path is 480 blocks versus alternative homeland modest-land cost 124.069, incomparable units for speed claims. N2 is interpreted as a barrier interface, N3 as a crossing interface. Their overlapping/substitute relationship remains a question of practical access and payoff, not settled interchangeability.

New N2/N3 local ore totals differ, but their neighborhoods do not establish either road’s privileged access to those deposits. Buried ore beneath a water Handoff does not make it a mining destination. Thus the new geology neither resolves substitution nor invalidates prior water relationships. 930012642 retains its north village Destination and separate unresolved southern relationships. Prior completion counts remain 2 Complete / 1 Structurally Supported / 3 Incomplete for 930010639 and 1 / 1 / 4 for 930012642; these are evidence categories, not scores.

## Practical Reach and analytical states

Preserve the causal sequence: **generated geography → Character constraints → intelligent vanilla organization → recognized MOBA infrastructure → integrated-system effects**. This scan adds current physical snapshot evidence to the geography layer; it does not populate the later layers by assumption.

- **Opening:** geography and frozen opening routes are evidenced; actual loadout, food, tools, access and threats are UNRESOLVED.
- **Informed:** exact ore coordinates are analyst knowledge, not observed player knowledge. Discovery effort and accessible deposits are UNRESOLVED.
- **Organized:** prepared tunnels, bridges, caches, processing and intelligent vanilla teamwork require measured state; benefits are UNRESOLVED.
- **Recognized:** compare the same physical organization with and without valid recognition. No measured recognition event or throughput delta exists here; UNRESOLVED.
- **Mature:** integrated transport/production/renewal, stock, character and threat state are UNRESOLVED; no phase-productivity ratio or UAU/min is derived.

The manuscript’s 256 WP/UAU and progression ratios are Working calibration hypotheses. They do not price these ore counts or imply elapsed progression. No constant-rate single-resource miner, Fortune material multiplier, class power, static seed Reach score, overall winner or change of fitter authority is introduced.

## Discrimination and smallest next measurement

**Yes, the evidence distinguishes local geology and previously sampled geography. No, it does not yet discriminate economically realizable opportunity or establish a preferred seed.** Resource distributions and deposit components now have actual block evidence; accessibility, exposure, discovery cost, exploitable finite fields, mixed operational resources, mining/processing/return time, character capacity, persistent stock, UAU flow, and Practical Reach remain UNRESOLVED.

A generic exporter is no longer the missing capability: this pass implements a bounded read-only ore export using existing codecs. The smallest next step is a read-only block/air/fluid halo and surface-to-deposit access audit around **930010639 N2 and N3**, retaining these world hashes and frozen Handoffs. Establish exposed ore, water-column/embarkation clearance, connected cave approaches and the first actual access work required. Then measure one explicit loadout/undertaking with outbound, acquisition, processing and return burdens. Compare equal Character/physical states before attributing benefits to knowledge, vanilla organization or formal recognition. Full-depth counts alone justify neither economy balancing nor a playable-map build.

## Files and validation

Added `implementation/worldgen/economic_shadow.py`, `implementation/worldgen/tests/test_economic_shadow.py`, and this report directory (inventory, capability catalog, measurements and twelve raw ore exports). All prior authorities, fitter implementations, selections and historical outputs were left untouched. Pre-existing local changes remain.

Reproduce from repository root:

```sh
PYTHONPATH=implementation/worldgen python3 -m unittest discover -s implementation/worldgen/tests -p test_economic_shadow.py
python3 implementation/worldgen/economic_shadow.py --output implementation/worldgen/reports/economic_shadow_2026-09-18
PYTHONPATH=implementation/worldgen python3 -m unittest discover -s implementation/worldgen/tests -p test_task_a_opportunities.py
```

See [VALIDATION.md](VALIDATION.md) for actual results, repeatability and preservation checks. Test fixtures are synthetic and excluded from seed observations.
