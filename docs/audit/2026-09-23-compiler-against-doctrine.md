# The compiler against the intended model

**Date:** 2026-09-23
**Scope:** comparison only, plus two honesty fixes. No redesign, no new
thresholds, no new abstraction.

The doctrine compared against is the intended compiler/foundry hierarchy:

    Map Type / regional shape -> search for viable realization -> team topology
    -> independent Homebase sockets -> Opening Hinterland viability
    -> Wilderness opportunity / traversal / resource measurement
    -> authorability / bounded corrections -> physical verification
    -> readiness certification

---

## 1. The matrix

### Already implemented, and correct

| capability | where |
| --- | --- |
| Map Type / Default regional shape recognition, all 8 orientations | `vanilla_search/evaluate.py`, `recognize` |
| **Window search over a seed, not accept/reject at origin** | `terrain_harvest/window_search.py` (new today) |
| Off-server seed screening on biome facts, fails open | `terrain_harvest/seed_screen.py` |
| Homebase **socket** search independent per team | `homebase` stage, `SOCKET_NOT_INDEPENDENTLY_ACCEPTABLE` |
| Compact Hinterland as an envelope, not a half-map | `hinterland` stage |
| Objective siting under a joint ordinal constraint | `order_constrained` |
| Exactly one Lair, with A_L the single parity gate | `lair_socket.py` |
| Physical verification by column scan and readback | `column_scan.py` |
| VerifiedMap vs READY kept distinct | `readiness.py`, `foundry.py` |
| Opening ceiling: villages | `opening_ceiling.py` (new today) |
| Authoring refuses built sites; evicts entities | `clearance.py` (new today) |

### Implemented but measuring the wrong thing

| what | the problem |
| --- | --- |
| `map_scale` | **Was the literal string `normal` on every realization**, never derived. A hardcoded value wearing the costume of a measurement. **FIXED** to `unmeasured`. |
| `six_corridors_connect` | A straight-line proxy for traversability, already deferred out of regional shape as a known false positive. Correctly parked; still the only traversal gate attempted. |
| Renewable sources on a generated map | **Config's Alpha coordinates were applied to generated terrain.** The world instance is named `alpha_match` whether it holds the template or a claimed pool map, so Alpha's rabbits and carrot patches landed wherever they fell on a different world. Same defect `MapBindings` fixed for Fountains, objectives, Lair and Worksites; renewables were never migrated. **GUARDED** — it now refuses and says why. |

### Measured but not gated

| measurement | where | gated? |
| --- | --- | --- |
| Ore by material, from real region files | `opportunity_map.py` | no |
| Cave volume, depth, `exposed_ore_per_1k_cave` | `caves.py` -> `expedition/manifest.py` | no |
| Travel/expedition cost matrices | `expedition/travel.py` | no |
| Integration cost, levelling volume per column | `column_scan.integration_cost` | **partly** — bounds pad levelling only |
| Objective depth disparity (A_D 0.075..1.355) | measured today | no bound set |

These are not oversights in the same sense: the repo's habit is to measure
first and set a bound from the distribution. But nothing currently **runs** the
ore/cave measurements on a compiled map, so a searched window is optimised for
coastline while its underground is never looked at.

### Designed but not implemented

| doctrine | status |
| --- | --- |
| **Regenerative Eligibility from Strategic Depth / Regional Character** | `maps.md` defines Candidate Density, Regenerative Eligibility and Regenerative Vocabulary, and the "do not sprinkle" rule. `config.yml` states plainly that the plugin "registers supplied sources and manages manifestations, **not a generic Strategic Depth / Regional Character richness policy**". Sources are supplied, never derived from natural occurrence. |
| **Baseline regenerative layer on every viable map** | Not authored. A compiled map's `runtime_bindings` has **no `renewables` key**, and `readiness.REQUIRED` does not ask for one. |
| **Natural resource placement validity** (Diamond near the Hinterland, equipment-sufficient Iron) | Not implemented. `opening_ceiling` covers villages only and says so; carrots and iron are named unchecked. |
| **Opening Hinterland floor** (permits every fundamental verb) | Not implemented at all. Only the ceiling exists. |
| **Opening access certification** (base exits into the Hinterland) | Not implemented. Recorded in `maps.md` as the gap, explicitly *not* a Routes stage. |
| **Bounded authorability as a verdict** | Fragments exist — `integration_cost`, `max_levelling_moved_per_column`, socket viability. Nothing aggregates them into "correctable within doctrine, or reject". |
| **Discovered classification** | Schema exists; both discovered fields are now honestly `unmeasured`. |

### Genuinely unresolved design or calibration

- the **intervention budget** for bounded authorability;
- the **A_D bound** for objective depth;
- **Map Scale's metric** — deliberately not playable area;
- **Resource Density's metric** — must reflect practical opportunity, not ore per area;
- **Regenerative eligibility tables** by kind, richness, depth and region;
- whether **Default regional shape should require all six checks** or five of six.

---

## 2. What the doctrine corrected in my earlier reading

- **Authorability is not a late repair budget.** It belongs early, and its main
  expression is the regenerative layer: inspect natural occurrence, find valid
  manifestation opportunities, author the baseline from them.
- **A deficient map is not relabelled, it is rejected.** `resource-light`
  describes a *valid* map with a light natural economy, not a broken one
  rescued by authoring. My earlier "becomes resource-light rather than
  discarded" was wrong.
- **Renewable baseline is not the Resource Density label.** Every viable map
  gets the baseline; the label describes the *auxiliary natural* economy around
  it.

---

## 3. Minimum missing seams, in dependency order

1. **Run `opportunity_map` and `caves` on the compiled window.** Everything
   below needs a measured realization, and both tools already exist.
2. **Natural resource placement validity** — kind x concentration x
   accessibility x depth, against the opening floor/ceiling. Extends
   `opening_ceiling`.
3. **Regenerative eligibility** derived from Strategic Depth / Regional
   Character, then the baseline layer authored from eligible natural
   occurrences, then `renewables` added to `runtime_bindings` and to
   `readiness.REQUIRED`.
4. **Opening access certification** inside Homebase/Hinterland authoring.
5. **Authorability verdict** aggregating the existing cost metrics.
6. **Discovered classification** last, once there is something to classify.

No thresholds are proposed for any of them.

---

## 4. Test status, separated

- **Passing software tests:** Java 261, worldgen 421, 0 failures.
- **Compiler structural completeness:** incomplete. Six seams above are absent,
  and the regenerative layer is the largest.
- **Empirical competitive validation:** none. No map has been played to a
  conclusion, and nothing here is evidence about balance.
