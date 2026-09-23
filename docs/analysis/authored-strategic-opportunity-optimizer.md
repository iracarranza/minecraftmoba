# Authored Strategic Opportunity Optimizer

**22 September scope clarification:** preserve Practical Reach, Route spillover
and physical rescans as measurements. Initial Routes primarily serve Core exits
and hinterlands; distant target selection and full-map parity objectives are
prototype assumptions requiring later re-scoping, not guarantees of deep
Wilderness convenience. Profile counts and weighted travel parity do not prove
Core/Socket compliance, opening floor/ceiling, or competitive equivalence. See
[spatial doctrine audit](../audit/2026-09-22-spatial-doctrine.md).

**Status:** Working analysis/tooling. Strategic-profile counts and optimization weights are **NON-CANON ANALYTICAL FIXTURES** unless separately promoted.

## Purpose

Map balancing now closes with an authored strategic-opportunity reconciliation pass rather than stopping after world generation. The optimizer treats the realized Minecraft world as the substrate, generates many plausible authored opportunities, evaluates complete combinations, and preserves multiple balanced strategic characters rather than selecting a single mirrored layout.

The balancing object is a **Strategic Opportunity Portfolio**, not equal resource counts.

```text
generated Minecraft world
→ measured terrain / geology / ecology / structures
→ natural Opportunity Relationships
→ candidate authored opportunities
→ complete authored-map configurations
→ 7v7 non-combat / reach-labor evaluation
→ structural balance filter
→ strategic-character comparison / Pareto-style frontier
→ block-exact authoring
→ rescan
→ validation
```

## Critical hostile-data rule

The realized-map export's zero-hostile result is a fresh-world/entity-snapshot artefact. It is **not evidence that underground space is safe**.

Therefore:

- hostile snapshot counts are excluded from opportunity scoring;
- `hostiles == 0 → underground is safe` is a forbidden inference;
- underground hostile exposure remains **UNRESOLVED**;
- natural Extraction balance must not be certified until hostile exposure is measured or tested as an explicit sensitivity.

A future measurement should prefer spawn-eligible substrate, light/spawn-rule context, connected cave volume/topology, entrance/descent exposure, and operation duration over transient entity counts.

## Candidate generation

The optimizer generates many regional candidates rather than one prescribed location per resource.

Current realized-map search space:

| Candidate family | Count |
|---|---:|
| Founder-stock candidates | 74 |
| Renewable-manifestation candidates | 110 |
| Worksite candidates | 35 |
| POI candidates | 35 |
| North Route targets | 32 |
| South Route targets | 31 |

Candidates remain provenance-tagged. A qualifying region is not the same thing as an exact authored location.

### Renewables

Generated ecology supplies eligibility evidence. Strategic regenerative manifestations are selected from ecologically coherent regions rather than sprinkled to satisfy quotas.

### Founder stock

Carrots/potatoes may be authored introductions because vanilla incidence does not supply the intended founder economy. Their candidate value includes propagation, Development suitability, return burden, nearby food-chain relationships, and exploration depth.

### Worksites

Dormant Worksite sites remain generic before activation. The optimizer balances the **candidate-site portfolio**, not predetermined Mining/Factory identities.

### Routes

Route targets are evaluated by how physical path quality changes Practical Reach. No movement-speed bonus is implied. Exact Route geometry remains an authoring/rescan problem.

### POIs

POIs are evaluated partly through the opportunity relationships they create: traffic, expedition bundling, sunk travel, infrastructure leverage, and contestability.

## Configuration search

A map configuration contains a portfolio of Routes, POIs, dormant Worksite candidates, renewable manifestations, founder stock, and other strategic manifestations.

Structural balance is evaluated before strategic character. Current balance components include:

- opening founder reach;
- deep founder reach;
- private renewable labor/reach;
- Worksite-pool mean reach;
- favored-side Worksite reach;
- POI portfolio reach;
- Route-target reach.

The current normalized balance-asymmetry metric is an internal search measure, **not a win probability or percentage team advantage**.

## Strategic-character dimensions

Balanced configurations can still produce very different matches. The tool separately characterizes:

- Opportunity / resource density;
- Exploration pressure;
- Consolidation pressure;
- Contest pressure;
- Decentralization;
- Route dependence;
- Peripheral specialization;
- volatility from opportunity distribution.

Current search profiles:

1. Balanced Baseline
2. Exploration-Centric
3. Consolidative
4. Resource-Dense
5. Resource-Light
6. Contested Core
7. Peripheral Specialization
8. Route-Centric

These are scenario families, not rankings. The intended output is a set of viable alternatives with different strategic characters.

## Current realized-map search

The first reproducible pass sampled **1,200 complete configurations per profile**.

| Profile | Balanced samples under current filter | Preferred balance asymmetry |
|---|---:|---:|
| Balanced Baseline | 114 | 0.0474 |
| Exploration-Centric | 116 | 0.0672 |
| Consolidative | 115 | 0.0935 |
| Resource-Dense | 158 | 0.0604 |
| Resource-Light | 74 | 0.0616 |
| Contested Core | 146 | 0.0628 |
| Peripheral Specialization | 99 | 0.0448 |
| Route-Centric | 122 | 0.0608 |

The current filter is a search fixture, not canon.

## Reach-model limitation

The current regional pass reconstructs Practical Reach for opportunity cells from the measured terrain-weighted travel sites using an inverse-distance-weighted terrain-inflation proxy.

Leave-one-out median absolute error is approximately:

- North: 9.6 seconds;
- South: 7.5 seconds.

This is adequate for regional ranking, not block-exact certification. Local terrain outliers can be much larger. Every authored finalist must therefore be rescanned after exact placement.

## Proxy reconstruction warning

The first implementation can reconstruct cell proxies whose totals match the packing export:

- farmable fraction ≥ 0.35 → 37 Development-like cells;
- sampled ore total ≥ 750 → 54 Extraction-like cells;
- water fraction ≤ 0.39 → 48 Buildable-like cells.

These are **count-calibrated proxies**. Matching the totals does not prove these were the original packing criteria.

## Claude / world-authoring handoff

The analysis layer should choose regional strategic configurations. World-authoring tooling should choose exact terrain within those regions.

A handoff should provide:

- preferred complete configuration;
- several already-simulated fallback configurations;
- exact candidate-region bounds;
- measured and derived provenance;
- intended strategic role;
- reach/relationship validation bands;
- ecological constraints;
- overlap exclusions;
- explicit unresolved hazards.

If exact terrain makes a preferred location incoherent, use a pre-simulated fallback rather than improvising a strategically untested location.

After authoring:

```text
exact blocks / paths / pads / patches
→ rescan
→ replace regional reach proxies with measurements
→ rerun non-combat 7v7 balance
→ only then freeze map candidate
```

## Current conclusion

The map-balancing pipeline can now terminate in **Authored Strategic Opportunity Reconciliation**. The goal is not to make both halves equivalent. It is to use coherent authored relationships around generated geography so both teams have viable, distinct, interactable economic paths across the match.

Combat resolution remains intentionally outside this pass.


## Route spillover after exact-world validation

The first block-exact authored worlds established that a Route is not only an edge to its selected target. Surfacing, clearing and decking a corridor changes Practical Reach to every opportunity that the corridor passes or makes cheaper to approach. In the old-frontier rescans, Route-target parity could improve while renewable or Worksite parity worsened.

The optimizer therefore now carries a **regional Route-spillover sensitivity** during configuration search. It estimates collateral reach changes for selected opportunities near plausible homeland-to-target corridors and filters on the worse of raw regional asymmetry and spillover-sensitive asymmetry. The radius, path-stretch floor and scale are analysis fixtures, not canon.

This does not replace block-exact authoring. Physical Routes still follow real terrain; every crossing must be recorded and every authored world rescanned. The sensitivity exists to reject fragile portfolios before expensive authoring.
