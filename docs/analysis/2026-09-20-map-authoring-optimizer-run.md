# Realized-map authored-opportunity search — seed 930012642

**Run date:** 2026-09-20  
**Status:** NEW SENSITIVITY / tooling output. Not canon.

## Critical hazard guard

The export's zero hostile counts are a **fresh-world snapshot artefact**. They are excluded from scoring. Underground hostile exposure remains **UNRESOLVED**, so this pass does not certify natural Extraction balance.

## Candidate space

- 74 founder-stock candidates
- 110 renewable-manifestation candidates
- 35 Worksite candidates
- 35 POI candidates
- 32 North Route targets
- 31 South Route targets

The optimizer searches complete portfolios rather than selecting each opportunity independently.

## Regional reach approximation

Opportunity-cell Practical Reach is reconstructed from the measured terrain-weighted travel sites. Leave-one-out validation:

- North median absolute error: ~9.6 s
- South median absolute error: ~7.5 s
- median relative error: roughly 7–8%

Use this only for regional ranking. Exact authored sites and Route paths must be rescanned.

## Search results

Each profile sampled 1,200 complete configurations.

| Profile | Balanced samples | Preferred balance asymmetry |
|---|---:|---:|
| Balanced Baseline | 114 | 0.047401 |
| Exploration-Centric | 116 | 0.067165 |
| Consolidative | 115 | 0.093518 |
| Resource-Dense | 158 | 0.060442 |
| Resource-Light | 74 | 0.061608 |
| Contested Core | 146 | 0.062782 |
| Peripheral Specialization | 99 | 0.044789 |
| Route-Centric | 122 | 0.060835 |

The current structural-balance filter is `balance_asymmetry <= 0.12`. That threshold, the number of Worksites/renewables in each scenario profile, and profile objective weights are **NON-CANON ANALYTICAL FIXTURES**.

The balance metric is a normalized search measure over reach/labor dimensions. It is not a win probability and must not be described as a percentage advantage.

## Design development

This run changes the map-authoring workflow in five important ways.

1. **Authored resources are optimized as portfolios.** A good individual placement can still create a bad map when combined with other opportunities.
2. **Balance and match character are separated.** Configurations first need acceptable structural parity; among those, the search can preserve exploration-heavy, consolidative, dense, light, contested, peripheral, or Route-centric alternatives.
3. **Generated geography remains authoritative.** Renewables are selected from measured ecology; founder stock can be introduced where the intended economy requires it; Routes change relationships to existing geography.
4. **Exact siting is deferred deliberately.** The optimizer chooses qualifying 128×128 regions. World-authoring tooling chooses exact terrain and must then rescan it.
5. **Missing hazard evidence stays missing.** The entity snapshot cannot silently become a zero-hazard assumption.

## Next physical validation

A useful playtest/authoring set is:

- Balanced Baseline
- Exploration-Centric
- Consolidative
- Resource-Light

These expose meaningfully different economic traffic patterns without requiring different underlying system rules.

For each selected finalist:

1. instantiate exact Worksite pads, renewable manifestations, founder patches, POIs and Routes;
2. rescan exact coordinates and path costs;
3. measure/derive hostile exposure separately;
4. rerun the non-combat 7v7 model with measured values;
5. retain fallbacks when exact terrain invalidates a preferred regional candidate;
6. freeze only after the authored world, rather than its regional approximation, passes validation.
