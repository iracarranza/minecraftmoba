# Regional authored-opportunity optimizer — realized map 930012642

This pass searches authored **regional relationships**, not block-exact placement.
Exact placement and exact Route geometry still require world authoring + rescan.

## Critical hazard guard

The export's zero hostile counts are a **fresh-world snapshot artefact**. They are excluded from scoring.
Underground hostile exposure remains **UNRESOLVED**, so this tool does not certify natural Extraction balance.

## Reach proxy

- north: median absolute leave-one-out error 9.6s; median relative error approximately 7–8%.
- south: median absolute leave-one-out error 7.5s; median relative error approximately 7–8%.
- Use: regional ranking only. Large local terrain outliers still require block-exact rescan.

## Scenario finalists

| Profile | Balance asymmetry | Exploration | Consolidation | Resource density | Contest | Route dependence | Peripheral specialization |
|---|---:|---:|---:|---:|---:|---:|---:|
| balanced_baseline | 0.047 | 0.55 target family | moderate | moderate | moderate | moderate | moderate |
| exploration_centric | 0.067 | 0.682 | low | moderate | moderate | moderate | moderate |
| consolidative | 0.094 | moderate | 0.632 | moderate | moderate | moderate | moderate |
| resource_dense | 0.060 | moderate | moderate | 0.777 | moderate | moderate | moderate |
| resource_light | 0.062 | moderate | low | 0.069 | moderate | moderate | moderate |
| contested_core | 0.063 | moderate | moderate | moderate | 0.346 | moderate | moderate |
| peripheral_specialization | 0.045 | moderate | moderate | moderate | lower | moderate | 0.890 |
| route_centric | 0.061 | moderate | moderate | moderate | moderate | 1.000 | moderate |

All scenario resource counts are **NON-CANON ANALYTICAL FIXTURES**. Balance is evaluated as a vector of reach/labor asymmetries; the profile score then selects different strategic characters among structurally viable configurations.

## Claude handoff

Use `claude-authoring-handoff.json`. Each profile should be regenerated/inspected with the committed optimizer and realized-map export. Claude should choose exact terrain within prescribed cells, build paths/pads/patches, then rescan the authored world. Do not improvise a strategic location outside the supplied configuration unless it is returned to the optimizer for evaluation.
