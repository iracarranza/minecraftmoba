**Shape review, 13 October:** these are baseline gameplay passes, not a shortlist certified for the new visually sequential island-chain preference. River channels are restored only for geographic island grouping; physical checks retain actual water.

# Qualified archipelago prospects

**Prototype/test geography qualification, Java 1.21.11.** All implemented shared checks passed, with the user-required 175-block horizontal centre spacing. Source worlds are unmodified; no playable-map build or READY publication.

| Seed | Window bounds X / Z | Surface water | Minimum all-anchor spacing | Closest Lair–landmark | Lair access disparity |
|---|---|---:|---:|---:|---:|
| [890001380](seed-890001380-map.png) | -1536…255 / -384…1407 | 56.8% | 186.8 | 921.4 | 14.4% |
| [890001757](seed-890001757-map.png) | -1024…767 / -1536…255 | 60.7% | 178.9 | 197.6 | 8.0% |
| [890001967](seed-890001967-map.png) | -1536…511 / -1536…511 | 77.4% | 241.9 | 241.9 | 14.8% |
| [890001993](seed-890001993-map.png) | -1536…511 / -512…1535 | 73.7% | 193.7 | 362.5 | 1.2% |

[Map visual](qualified-archipelagos.png)
[Full qualification and spacing evidence](qualified-summary.json.gz)

Search: broadened to 1,000 seeds and nine shifted windows per seed; 147 initial coarse prospects; additional island-focused refinement is saved in `chain-refined.json`. Reused four prior vanilla source worlds and generated larger-island shifted prospects. Prototype/test search considers both the original Core bands and wider northern/southern Core positions, while preserving fitted Homebase acceptance. Defensive candidates are distributed across spatial positions and practical-advance bands. An additional search pass reserves a balanced Lair and spaces opposing-team pads during placement. Existing own-team cost half, rival-distance cap, forward corridor, strict defensive ordering and authored footprints remain enforced. Column checks reject unsuitable pads. Candidate diversification changes search coverage, not acceptance thresholds. Exact packed-section counting is accelerated locally, with synthetic, vanilla-section and full-window equivalence evidence saved alongside these reports.

Applied gates: actual Archipelago predicate (surface water >43.1% and more than one disconnected land body); contested-land terrain symmetry <=0.2371; independent Homebase quality >=0.52; compact hinterland <=0.5; strict Outpost/Bastion/Spike ordering; all 36 pair distances across the nine Fountain/Outpost/Bastion/Spike/Lair anchors >=175, including opposing teams; unchanged Lair usable-ground/open-volume/approach proxies and access disparity <=15%; measured objective/Fountain column geometry, levelling <=10 blocks per column, opening ceiling, opening floor and resource validity. Final pair distances independently recomputed from saved evidence.

Limits: Lair air-volume proxy does not certify actual Giant/Ghast/Dragon encounter collision and flight geometry. Equipment-sufficient accessible-iron ceiling and broader territorial acceptance thresholds remain Open. Travel is analytical surface cost, not calibrated boat speed or live Logistics. No authored Lair manifestation or runtime readiness certification.

Scripts: `tools/analysis/search_archipelago_windows.py`, `search_archipelago_layouts.py`, `generate_archipelago_prospects.py`, `report_qualified_archipelagos.py`. Existing reports and worlds are preserved.

Additional Prototype/test search preferences: fitted Fountains on two substantial distinct 8-block surface components (at least half the second-largest component area); each team’s three defensive anchors on distinct geographic components (river channels restored for grouping only); Lair ranked by shortest worst-team straight-line Fountain distance among sites passing physical proxies, spacing and <=15% modeled disparity. Island-aware pooling retains lower-ranked strategic islands; the final local forward search includes alternate routes within 135% of minimum modeled Fountain-to-Fountain cost; Core pairs are ranked by potential island spread before accepted Homebase quality. These preferences are not final balance, and surface components do not prove practical chokepoints or live discovery time.
