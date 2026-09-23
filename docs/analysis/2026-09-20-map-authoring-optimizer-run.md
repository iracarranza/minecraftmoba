# 2026-09-20 realized-map authoring optimizer run

## Authority

The committed Python optimizer and generated artifacts in docs/analysis/map-authoring-optimizer are now one provenance chain. Earlier divergent handoff/frontier values are superseded. The corrupt candidate-catalog.json.gz is removed and replaced by candidate-catalog.json.

## V2 change: Route spillover enters search

Exact-world rescans showed that a physical Route can improve intended Route-target parity while simultaneously changing renewable, Worksite, founder, and POI access. V2 screens collateral corridor effects during configuration search rather than treating a Route as an isolated edge.

The regional spillover model is a sensitivity layer; exact authored paths and rescans remain authoritative.

## V2 reference run

Seed 20260920; 1200 configurations/profile; working effective-balance filter 0.12.

| Profile | Balanced found | Raw | Spillover | Effective |
|---|---:|---:|---:|---:|
| balanced_baseline | 97 | 0.036159 | 0.034075 | 0.036159 |
| exploration_centric | 80 | 0.075932 | 0.080807 | 0.080807 |
| consolidative | 102 | 0.074903 | 0.046994 | 0.074903 |
| resource_dense | 140 | 0.054456 | 0.039447 | 0.054456 |
| resource_light | 59 | 0.054303 | 0.059896 | 0.059896 |
| contested_core | 95 | 0.061960 | 0.072513 | 0.072513 |
| peripheral_specialization | 98 | 0.048202 | 0.039556 | 0.048202 |
| route_centric | 106 | 0.058306 | 0.040163 | 0.058306 |

Candidate space remains 74 / 110 / 35 / 35 / 32 / 31 for founder / renewable / Worksite / POI / North Route / South Route candidates.

## Prior exact-world evidence retained

Current authored/rescanned worlds were produced from frontier SHA-256 444594cd47198c08354d2829d8e96a2b97243e5c757aa3eb67b226d138699ab5, not this v2 frontier. They remain valid evidence that the old reach proxy understated asymmetry and that Route spillover can help or hurt balance, but they do not validate the new finalists.

Observed old-frontier exact-world asymmetry:

- Balanced Baseline: bare 0.091868 → authored Routes 0.116614 (delta +0.024746)
- Exploration-Centric: 0.145640 → 0.192482 (delta +0.046842)
- Consolidative: 0.156139 → 0.106757 (delta -0.049382)
- Resource-Light: 0.099620 → 0.050488 (delta -0.049133)

## Next validation

Author v2 finalist rank 0 for the four physical test profiles, rescan exact sites and Routes, compare measured spillover against the regional sensitivity, and only then revise the spillover constants or freeze a map.
