# Visually sequential archipelago search — 13 October 2026

Prototype/test search for the Working pattern in maps.md: large Fountain islands at opposite ends, smaller chain islands generally growing toward a shared center, and interspersed larger structure islands. Geographic proximity alone is insufficient.

Expanded the existing 1,000-seed census by 5,000 seeds (890002001–890007000), with nine shifted 2048-block windows per seed. The conventional coarse archipelago screen retained 661 prospects from the new range. A separate morphology-first screen also reviews all nine cached windows, rather than accepting the conventional screen's one preferred window per seed. Morphology screening uses approximate native biome grids; it does not certify actual land/water, physical pads, symmetry, spacing, or gameplay suitability.

The morphology ranking considers substantial endpoint islands, inward projected order, small first stops, growth, lateral scatter, long gaps, and sharp turns. These are search preferences, not adopted numerical balance rules. Curved sequences still require visual review; scores cannot establish design approval or an unbiased prevalence estimate.

Files:

- `all-window-coarse.json`: best shape-ranked window per cached seed; no gameplay gates.
- `all-window-fine.json`: 16-block approximate refinement of the all 225 coarse shapes, with approximate height-symmetry screening recorded separately.
- `fine-sequence-prospects.png`: shape prospects that pass the approximate symmetry screen; no gameplay qualification.
- `two-sequence-prospects.png`: the two selected vanilla verification targets, 890002226 and 890002716. Lines suggest island order, not placed objectives.

Actual verification uses the official unmodified Java 1.21.11 server, then the existing shared compiler stages and all-nine-anchor 175-block spacing. River channels are restored only when grouping geographic islands; physical fit uses actual water. The conservative three-distinct-defensive-islands trial remains a Prototype/test search preference. Actual boss encounter volumes, calibrated boat/discovery time, and broader territorial balance remain unverified. No worlds are authored or installed as playable maps.

## Completed verification results

All 225 morphology-screened coarse windows were refined at 16-block spacing. 70 retained a two-sided sequence under the Prototype ranking; 22 also passed the approximate height-symmetry screen. These counts describe this biased screening method, not the prevalence of the desired design.

The actual vanilla targets 890002226 and 890002716 did not yield a qualifying endpoint-constrained Fountain pair. A broader candidate search removed center-grade/canopy restrictions while retaining the shared footprint quality, valid Fountain selection and exact physical checks; the missing endpoint candidates remained. Thus neither reached a full 175-block gameplay layout in this trial. Search failure is not a proof of impossibility. See `verification-results.json` and `actual-sequence-targets.png`. Approximate small islands sometimes became connected shoreline or tiny patches in actual terrain. No seed in this batch is certified for the full new shape and gameplay requirements.

## Four-candidate bank and follow-up trial

The requested four-candidate bank contains 890001380, 890001757, 890001967 and 890001993. Each passes the implemented shared stages and an independent all-nine-anchor 175-block audit. See `../archipelago_chain_search_2026-10-12/qualified-summary.json` and `independent-geographic-audit.json`. These are basic gameplay-filter candidates; they do not have sequential-shape or Type-specific balance approval. In particular, Fountain-to-Lair straight distances are about 1056/1300, 594/560, 928/898 and 1263/1260 blocks respectively, and absolute discovery/boat time remains uncalibrated.

Four further source worlds were verified: 890002156, 890006350, 890004593 and 890002221. None yielded a qualifying endpoint-constrained layout in this finite trial. The three-distinct-defensive-islands preference is a conservative search experiment, not an adopted universal balance gate. All source generation and checks have ended.
