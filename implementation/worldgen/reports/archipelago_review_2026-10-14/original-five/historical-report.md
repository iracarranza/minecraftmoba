# Qualified archipelago prospects

**Prototype/test geography qualification, Java 1.21.11.** All implemented shared checks passed, with the user-required 175-block horizontal centre spacing. Source worlds are unmodified; no playable-map build or READY publication.

| Seed | Window bounds X / Z | Surface water | Minimum all-anchor spacing | Closest Lair–landmark | Lair access disparity |
|---|---|---:|---:|---:|---:|
| [890000020](seed-890000020-map.png) | -1024…1023 / -1024…1023 | 61.2% | 214.7 | 407.4 | 5.0% |
| [890001347](seed-890001347-map.png) | -1536…511 / -512…1535 | 70.6% | 177.6 | 697.6 | 3.1% |
| [890001592](seed-890001592-map.png) | -1536…511 / -1024…1023 | 62.8% | 181.4 | 1292.4 | 0.2% |
| [890001687](seed-890001687-map.png) | -512…1535 / -1536…511 | 57.3% | 179.8 | 967.5 | 7.4% |
| [890001809](seed-890001809-map.png) | -512…1535 / -1024…1023 | 55.1% | 218.4 | 897.2 | 6.1% |

[Map visual](qualified-archipelagos.png)
[Full qualification and spacing evidence](qualified-summary.json.gz)

Search: broadened to 1,000 seeds and nine shifted windows per seed; 147 coarse prospects, 16 fine-refined windows. Reused four prior vanilla source worlds and generated larger-island shifted prospects. Prototype/test search considers both the original Core bands and wider northern/southern Core positions, while preserving fitted Homebase acceptance. Defensive candidates are distributed across spatial positions and practical-advance bands. An additional search pass reserves a balanced Lair and spaces opposing-team pads during placement. Existing own-team cost half, rival-distance cap, forward corridor, strict defensive ordering and authored footprints remain enforced. Column checks reject unsuitable pads. Candidate diversification changes search coverage, not acceptance thresholds. Exact packed-section counting is accelerated locally, with synthetic, vanilla-section and full-window equivalence evidence saved alongside these reports.

Applied gates: actual Archipelago predicate (surface water >43.1% and more than one disconnected land body); contested-land terrain symmetry <=0.2371; independent Homebase quality >=0.52; compact hinterland <=0.5; strict Outpost/Bastion/Spike ordering; all 36 pair distances across the nine Fountain/Outpost/Bastion/Spike/Lair anchors >=175, including opposing teams; unchanged Lair usable-ground/open-volume/approach proxies and access disparity <=15%; measured objective/Fountain column geometry, levelling <=10 blocks per column, opening ceiling, opening floor and resource validity. Final pair distances independently recomputed from saved evidence.

Limits: Lair air-volume proxy does not certify actual Giant/Ghast/Dragon encounter collision and flight geometry. Equipment-sufficient accessible-iron ceiling and broader territorial acceptance thresholds remain Open. Travel is analytical surface cost, not calibrated boat speed or live Logistics. No authored Lair manifestation or runtime readiness certification.

Scripts: `tools/analysis/search_archipelago_windows.py`, `search_archipelago_layouts.py`, `generate_archipelago_prospects.py`, `report_qualified_archipelagos.py`. Existing reports and worlds are preserved.

## Subsequent user design review

**890000020 and 890001592 are rejected on design review.** Their basic-filter
passes are retained for traceability. The remaining three are acceptable
prospects, with 890001809 closest to the desired island-chain direction.
The visuals above show the original filter bank, not a design-approved shortlist.
New Type-specific topology and absolute Lair reach checks remain unimplemented.
See [design review](design-review.json) and the
[canonical reconciliation](../../../../../docs/reconciliation/2026-10-12-archipelago-balancing.md).
