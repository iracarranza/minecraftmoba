# Spatial doctrine reconciliation and temporal model migration — 22 September 2026

Branch `codex/spatial-cadence-migration`. Spec: `specs/superspatialdoctrinespec.md`.
Unlike the 03c5bea spatial-doctrine audit, this pass changed executable logic,
configuration, tests and fixtures, because the temporal phase model the runtime
implemented is itself superseded.

---

## A. Temporal model

### Old sources of truth

There was only ever **one** tick counter, `Match.elapsed`, advanced by a 1-tick
scheduled callback. `MatchClock` is arithmetic over it, not a second clock. That
is the good news, and it is why the migration is small.

What the trace found instead of a phase enum was an **absence**: no
Overworld/Nether/End state machine existed in the runtime at all. The obsolete
temporal model lived in doctrine and in two concrete runtime artifacts:

1. **An inferred match length.** `MATCH_MINUTES = 80` was derived from the fact
   that the activation series happened to have four entries — arithmetic
   backwards from a config fixture into a match duration.
2. **A horizon branch.** Past `MATCH_TICKS`, `Match.tick` announced a
   "48-minute analytical horizon" and then incremented `elapsed` a second time
   in order to announce it only once. Any sunset or sunrise landing on the
   skipped tick was **silently lost**, and the message named 48 minutes while
   the constant said 80.

A third defect sat next to them: `Match.tick` and `Match.skipMinutes` were two
copies of the same boundary loop, and had already drifted — only the ticker
carried the horizon branch. This is the defect this codebase keeps producing:
**two implementations of one idea.**

### Dependencies found and how each was classified

| Dependency | Classification | Outcome |
|---|---|---|
| `Match.elapsed` | KEEP | Single source of truth, unchanged. |
| `MatchClock` day/night, `Phase{DAY,NIGHT}` | KEEP | Lighting, not macro stages. Untouched. |
| `MatchClock.MATCH_MINUTES/MATCH_TICKS/pastHorizon` | REMOVE | Deleted. Nothing in canon ends a match on a clock. |
| `Match.tick` / `Match.skipMinutes` | MIGRATE | One shared `advance(ticks)` path. |
| `Worksites.onSunset(ordinal)` | MIGRATE | → `onOpportunityNight(tier)`. |
| `alpha.worksites.activationsPerSunset` | MIGRATE | → `activationsPerTier.{I,II,III}`; legacy key honoured with a deprecation warning. |
| `Contributions.capitalize` first-capitalization award | KEEP | Survives unchanged; activation still never grants capitalization. |
| `Renewables.tickOpportunities`, `Recovery.Rates`, `phaseTicks` | KEEP | `phaseTicks` is DAY/NIGHT duration, not a macro stage. Unrelated semantic use of "phase". |
| `WorkPoints` level-band "phase" vocabulary | KEEP | Level bands, not match time. |
| Fountain/victory predicate (ALPHA-D3) | KEEP | Unchanged. |
| Persistence: memory-only match state, `WorldInstance.load` replacing stale instances | KEEP | No mid-match recovery invented. Lair adds a tagged sweep so a restart cannot strand a boss. |
| Team defensive objective lifecycle | NEW | None existed beyond the Fountain and physical greyboxes. No phase gate was removed, because none was there. |
| Lair | NEW | `LairLifecycle` (pure) + `Lair` (Bukkit binding). |

### Migration performed

`OpportunityCadence` maps the sunset ordinal to the stage — OPENING,
WORKSITE_I, GIANT, WORKSITE_II, GHAST, WORKSITE_III, DRAGON, then UNSCHEDULED.
It derives everything from `MatchClock.sunsetOrdinal`, so `skip` cannot
desynchronize it and there is no second counter to drift.

Six nights fall at 10/30/50/70/90/110 minutes **as a consequence of the vanilla
clock**. That is not a new 120-minute match length, and the tests say so.

`Match.onSunset` now dispatches by stage. The Lair is told the ordinal on
**every** night, including Worksite nights, because "leave the occupant alone"
is a decision its lifecycle has to make rather than one made by not calling it —
that is precisely what lets a surviving Giant persist through Worksite II and
still be replaced when Ghast's night arrives.

Worksites take a **tier**, not an ordinal. Removing the ordinal from the
signature is what makes a Lair night unable to open Worksites: there is no
arithmetic left to get wrong.

### Unresolved seams (temporal)

- **Post-Dragon cadence.** `UNSCHEDULED`. Not looped, not repeated.
- **Paired siege advantage.** `LairLifecycle` records a `SiegeOpportunity` with
  the boss, the paired objective and an optionally-unknown team, and applies
  **nothing**. No damage, debuff, defender reduction, structural weakness or
  vulnerability window.
- **Boss XP and objective toppling XP.** Not awarded.
- **Physical toppling validation.** `TeamObjectives.recordValidatedToppling`
  exists and nothing calls it; the evidence adapter is open.
- **Encounter design.** A vanilla Giant has no designed AI and an Overworld
  Ender Dragon carries arena/flight assumptions. No AI, tuning, arena protection
  or XP suppression was invented to hide that. Untested against a live server.
- **Kill attribution.** An unknown killer stays unknown; no team is guessed.

---

## B. Spatial model

Modelled in `implementation/worldgen/terrain_harvest/spatial_contract.py` and
`objective_forms.py`, and canonized in `maps.md` and `objectives.md §17C`.

- **Core** — compact, mirrored, ending at explicit interfaces rather than a
  radius. Inclusion test: would natural generation deciding this feature create
  arbitrary opening combat/defensive differences?
- **Socket** — judged on whether the standardized Core fits with bounded
  integration. North and South need **not** resemble one another; each must be
  independently acceptable. `socket_problems` takes each socket's own viability
  and deliberately never compares them. A socket needing landscape surgery is
  rejected, not repaired.
- **Hinterland** — compact. `hinterland_problems` fails a Hinterland that
  reaches the team end, which is the giant-radial-homeland failure returning
  under a new name.
- **Wilderness** — the residual, on every side including **poleward**. The old
  vocabulary could not express this, because it assumed a team's end of the map
  was that team's ground.
- **Team objectives** — Wilderness structures, ordinal midline → Outpost →
  Bastion → Spike → Fountain, W–E and spacing free. `ordinal_ok` checks the
  order and nothing else.
- **Lair** — exactly one. `lair_problems` rejects zero and rejects more than
  one. `lair_access_asymmetry` computes A_L from both teams and returns
  `acceptable: 'unknown'`.
- **Authored Strategic Depth vs Practical Reach** — preserved as distinct.
  Depth is a spatial-role classification, not distance from the Fountain, since
  geographically nearby terrain can be poleward Wilderness. Initial authored
  Routes shape opening reach; later player infrastructure changes runtime reach
  only.
- **Worksite portfolio** — distributed, and explicitly **not** clustered at the
  Lair. The contrast between a distributed Worksite night and a concentrated
  Lair night is the design intent.

---

## C. Supersession matrix

| Old | Status | Replacement | Docs | Code / config / tests |
|---|---|---|---|---|
| Overworld → Nether → End → Aether as top-level temporal organizer | SUPERSEDED as organizer; RETAINED as thematic vocabulary | Worksite/Lair cadence | `objectives.md` §17, new §17C | No runtime enum existed |
| Sequential objective gating / invulnerability chain (§21 "Unresolved") | RESOLVED | Concurrent objectives; spatial ordinal only | `objectives.md` §21 | `TeamObjectives`, `TeamObjectivesTest` |
| `MATCH_MINUTES = 80`, 48-minute horizon announcement | REMOVED | No timed end; Fountain predicate | this audit | `MatchClock`, `Match.tick`, `MatchClockTest` |
| Worksites open every sunset | SUPERSEDED | Worksite nights I/II/III only | `objectives.md` §17C | `Worksites`, `Match.onSunset` |
| `activationsPerSunset: [2,2,3,3]` | DEPRECATED, still honoured with warning | `activationsPerTier.{I,II,III}` | config comment | `config.yml`, `Worksites.countForTier` |
| 6-minute day / 12-minute cycle / 6-18-30-42 pulses | SUPERSEDED (temporal portion only) | Vanilla clock, six nights | `docs/reconciliation/2026-09-14-…` marked in place | — |
| Tiered XP by toppling method (destruction < elimination < signature verb) | OBSOLETE, not restored | Mixed approaches all valid | `objectives.md` (already rejected) | Nothing implements it |
| Direct boss → objective damage | NOT RESTORED | Pairing recorded; effect OPEN | `objectives.md` §17C | `LairLifecycle.SiegeOpportunity` |
| One Lair per monster / per boss night / per team | SUPERSEDED | One permanent Lair, succeeding occupants | `maps.md`, `objectives.md` §17C | `LairLifecycle`, `lair_problems` |
| `end_tower` greybox as the End objective | HISTORICAL, deliberately NOT renamed | End Spike, unmeasured | `objectives.md` §21 | `objective_forms.HISTORICAL`, `structures.py`, `build_structures.py`, `packing.py` |
| Generic Bastion greybox / Outpost compound | HISTORICAL, unmeasured | Rampart without bridge; watchtower only | `objectives.md` §21 | `objective_forms.FORMS` |
| N/S accessible-land asymmetry as a balance target | SUPERSEDED | Descriptive contrast, consequence unknown | module docstring | `compare_seeds.py`, schema `/2` |
| `weaker_team`, `deficit_kind` | RENAMED and re-scoped | `lower_quality_end`, `contrast_character`, `competitive_consequence: unknown` | — | `compare_seeds.describe_contrast` |
| Hinterland as the team's end of the map | SUPERSEDED | Compact envelope; Wilderness on every side | `maps.md` | `spatial_contract.hinterland_problems` |

---

## D. Implementation changes

**New Java:** `OpportunityCadence`, `TeamObjectives`, `LairLifecycle`, `Lair`.
**Changed Java:** `MatchClock` (horizon removed), `Match` (shared `advance`,
cadence dispatch, Lair bind/reset/report), `Worksites` (tier entry point, tier
recorded, legacy count migration), `MobaPlugin` (Lair and TeamObjectives
constructed, registered, swept on disable).

**Config:** `alpha.worksites.activationsPerTier` added; `activationsPerSunset`
deprecated in place; `alpha.lair.site: []` added, **empty by design**.

**New Python:** `terrain_harvest/spatial_contract.py`,
`terrain_harvest/objective_forms.py`.
**Changed Python:** `compare_seeds.py` (schema `/2`), `structures.py`,
`build_structures.py`, `packing.py` (historical-geometry annotations only — no
placement behaviour changed, and no world was regenerated).

**Tests:** new `OpportunityCadenceTest`, `LairLifecycleTest`,
`TeamObjectivesTest`, `tests/test_spatial_contract.py`; migrated
`MatchClockTest` (horizon → six nights) and extended `WorksiteLifecycleTest`.

**Results.** Java: **231 tests, 0 failures.** Worldgen: **346 tests, 2 failures**
— `test_author_portfolio`, both reproduced unchanged at parent commit 03c5bea
and in the optimizer/template path this pass did not touch.

---

## E. Open questions

Only genuinely unresolved items; nothing the spec already settled.

- Exact Giant→Outpost, Ghast→Bastion, Dragon→Spike siege advantage.
- Objective-toppling XP and boss XP.
- What physically constitutes validated toppling for each objective.
- Worksite I/II/III resource and facility packages; Worksite II enchant config.
- Homebase socket cost weights; Strategic Depth thresholds.
- Lair access-parity threshold; Lair dimensions; anti-cheese rules.
- Measured dimensions of the selected Outpost, Bastion and End Spike forms.
- An equipment target against which "equipment-sufficient iron" can be measured.
- Fountain exposure rule; Fountain repair/reactivation.
- Post-Dragon cadence.
- Whether a large accessible-land contrast is ever felt in play.

---

## F. Risks and next empirical work

1. **The boss encounters are untested against a live server.** A vanilla Giant
   has no designed AI; an Overworld Ender Dragon may carry arena and flight
   assumptions. Test entity binding, persistence and replacement in a
   **disposable** world before claiming a playable encounter. `/private/tmp/alpha-server`
   is not a disposable target.
2. **Lair access parity needs seed data.** A_L is computable and has never been
   computed, because no candidate has a Lair socket. This is the highest-value
   next measurement: it is the one parity claim doctrine actually endorses, and
   it would give the balance work a target that survives §1.
3. **Objective certification is blocked on measurement, by design.** Until the
   selected vanilla forms are measured, `objective_forms.certify` fails every
   candidate. That is the intended state, not a bug — but it means no map can be
   certified against the current objective contract today.
4. **Lair modification may enable encounter-nullifying cheese.** Left open
   deliberately; a playtest question, not a protection to invent.
5. **The optimizer still encodes the superseded balance target.** `compare_seeds`
   no longer interprets contrast as a defect, but `tools/analysis/map_authoring_optimizer.py`
   still optimizes `balance_asymmetry`, which the 930010639 rescue test showed
   measures travel-cost equality to *placed* opportunities rather than anything
   about the terrain. Its two pre-existing test failures sit in that same path.
   Reconciling the optimizer to §1 is the natural next migration.
6. **Worksite tier values need gameplay calibration**, and cannot be calibrated
   until the packages exist.
