# Does 930010639's N–S land gap impose an economic cost?

**Status: Working audit; recommended experiment is Prototype/test.** Audited at
`7cae5ae1280617ce95c5cec783e332b5d0785ea0`, continuing the rescue artifacts in
`faa67483eeb685958f540c9a78585e72dd9b21dd`. No balance, authoring, screening,
runtime telemetry, or canonical design changes. No gameplay comparison was run.

## 22 September doctrine reconciliation

The question is now explicitly: **Does this permitted wilderness asymmetry
cause a competitive consequence that the authored-opportunity model fails to
capture?** Natural N/S difference is allowed; the symmetry guarantee ends at the
Homebase Core/interface. Task A/B remains a useful contingent comparison of
opening activity and access burden, not a test of whether terrain statistics
have been equalized. There is no presumption that S is disadvantaged.

The later [fixture report](../../implementation/worldgen/reports/nearmiss_fixture_2026-09-22/REPORT.md)
materializes the near-miss but leaves the Route endpoint check and missing wheat
blocking trials. The [doctrine audit](2026-09-22-spatial-doctrine.md) further
requires identifying the tested Core/interface and supported-region scope before
interpreting opening outcomes. Existing coordinates, counts and task definitions
remain unchanged. Do not run the 16 trials or inject a Task B source in this pass.

## Finding

**The existing evidence cannot establish an actual gameplay disadvantage.** It
can supply task candidates, geological observations, travel-cost estimates and
runtime work accounting. The missing measurement is a timed undertaking with
actual access work and delivered output. A paired human opening-task experiment
is smaller and more informative than implementing the proposed full expedition
simulator or adding a new balance score.

The measurement distinction remains valid: equal weighted travel to placed
opportunities does not describe the natural terrain distribution. Removing that
difference is not an authoring requirement. It also does
not establish that compensation or rejection is necessary. Leave both proposed
design interventions pending evidence. The 991/2,285 qualifying-configuration
ratio measures this search's acceptance frequency, not general authorability or
economic disadvantage in play.

### What 0.3173 actually measures

`terrain_harvest/compare_seeds.py::accessible_land` sums seven named bands from
`land_band_counts`, omitting `deep_core_350_plus` and unreachable samples. Its gap
is `abs(N-S)/(N+S)`, not a 31.73% loss of output, time, or even land relative to N.
`vanilla_search/task_a.py::depth_summary` counts **dry sampled Wilderness,
excluding both homeland footprints**. It does not test every counted sample for
buildability, reachable ore, or useful production. “Near-depth workable land” in
the earlier handoff is shorthand stronger than this implementation warrants.
Depth-band geography and the player's operating cost are distinct in
[maps.md](../../maps.md) (Strategic Depth); [infrastructure.md](../../infrastructure.md)
requires actual transport capability. The original audit only clarified analytical implementation. The later explicit
[spatial reconciliation](../reconciliation/2026-09-22-spatial-doctrine.md) resolves
the broader design interpretation.

## Existing measurements and limits

Paths below are relative to the repository root; Python modules are under
`implementation/worldgen/`, Java classes under
`implementation/plugin/src/main/java/com/minecraftmoba/plugin/`.

| Requested outcome | Reusable implementation/evidence | Missing or unsafe inference |
|---|---|---|
| Acquisition / useful output | `expedition/benchmark.py`, `strategy.py`, `vanilla.py`: resource quantity, harvest expectation, mining/overburden seconds; cave sweep sensitivity 150/300/600. `WorkLedger`, `WorkPoints`: ore opportunities, drop units, A(O)/qH, production by output and category. | Benchmark pools the whole manifest, erasing N/S; no executed path, search, base departure, descent, smelting, delivery, inventory trips or elapsed gameplay. Cave exposure is not surface connectivity. Drops are counted at `BlockDropItemEvent`, not pickup or usable stock. Production is craft/furnace-extract accounting, not verified deposited inventory. |
| Travel burden | `expedition/travel.py`: team-homeland to named-site weighted-cost matrix, snapped-node offsets and unreachable pairs. `Routes.nearRoute`; `Vitals`/`HungerDisplay`/`HungerRegen` diagnostics. | Cost units are terrain-weighted blocks; division by vanilla speed is an assumption. Surface graph omits cave descent, actual authored Routes, boats, hazards and cargo. No trajectory, journey time or net food expenditure series. Route status lists geometry, not use. |
| Breaking / placement | Strategy estimates expose `blocks_dug` for branch/quarry. `Provenance` records current placed-block bits and aggregate place/break call counters. `WorkLedger.constructionPlacements` counts credited first placements. | Neither provenance nor ledger is a task action history. Provenance counters are global method calls; current bits lose removed work. Credited placements omit repeats and zero-credit actions. No all-material breaks, access-versus-harvest purpose, bridge cost or retry cost per player. Cave strategy has no actual access excavation. |
| Authored-opportunity utilization | `Worksites.report` gives lifecycle state; `Routes.report` geometry. `Renewables` has source IDs, availability, membership and global CSV harvest/depletion/recovery counts. `simulate_regenerative.py` measures eligible loci and manifestation sequences. | Registration, eligible loci, nearby Exploration credit and harvest totals are not team utilization or delivered yield. Missing source/player/time joins, observed availability windows, actual interaction, capture and output disposition. Eligibility simulation uses a pristine template and config sources, not a played map. |
| Progression / economic tempo | `/moba work <player>` reports level, residual WP, cumulative earned domains and ledger decomposition. `/moba debug <player>` exposes current character state. `MatchClock` supplies elapsed-tick phase semantics. `OpeningBenchmarkTest` replays reported mining/production through shared ledger arithmetic and shipped config. | No timestamped level/stock history. Opening fixture is approximate reported workload, not timed terrain simulation; replaying identical workloads cannot detect geography. WP is work credit, not wealth: extra detour construction can raise WP while delaying useful output. Logistics/Combat have no implemented awards in `WorkPoints`. |

Additional boundaries that matter:

- `economic_shadow.py` and its Sept 18 reports cover both seeds in six
  handoff-centered, full-height ore windows each. They preserve actual geology,
  not accessible yield. Windows can overlap; S3 on 930010639 is clipped. Do not
  sum these into team stocks or treat their coordinates as player knowledge.
- `terrain_harvest/caves.py` → `expedition/manifest.py` now measures exposure,
  depth and cave volume. The Alpha manifest has **66 resource manifestations
  in nine scanned cells**; 930010639 has **63 in nine**, not 63 surveyed cells.
  Both travel matrices have 19 sites and zero unreachable pairs on the coarse
  graph. The manifest explicitly leaves surface connection unproved.
- `terrain_harvest/simulation.py` tests frozen/resumed block drift and envelope
  containment; `navigation/navigate.cjs` tests teleports/dimensions/game modes;
  `contain.cjs` attempts boundary movement. These provide disposable-server and
  protocol scaffolding, not a survival task bot or an economy simulator.
- [Extraction Expedition Calculator](../analysis/extraction-expedition-calculator.md)
  is a Working specification for the larger event pipeline. Its acquired versus
  available clocks are worth reusing; implementing all of it is unnecessary here.
- Runtime diagnostics need care: several `WorkPoints` handlers fill the ledger
  before `award` applies enrollment/Survival/RUNNING-match gates. Restrict trials
  to valid participants and compare ledger deltas with `earnedBy`; do not treat
  arbitrary lobby/creative ledger values as earned WP. Do not fix scoring in
  this experiment. Verify crafts and furnace results against actual inventories.
- `Renewables.report()` calls `available()`, which can settle recovery; the
  scheduled sampler also settles it. Do not add high-frequency status polling and
  assume observation has no lifecycle effects. Preserve normal cadence.

### A small comparison already possible, without new code

Read `cost_matrix[team + '_homeland']` for sites with `kind == 'cave_cell'` in
`reports/expedition_2026-09-20/travel-matrix.json` and
`reports/nearmiss_930010639_2026-09-22/travel-matrix.json`:

| Map | Seed / volume | N nearest sampled cave cell / cost | S nearest sampled cave cell / cost |
|---|---|---|---|
| Alpha terrain | 930012642 / `tv_ef56852eda10acc88342e5ee` | `cave_-2169_-504` / 53.4 | `cave_-1882_198` / 689.7 |
| Near-miss | 930010639 / `tv_51a79e3b1bee05ea7559e799` | `cave_1640_-153` / 367.3 | `cave_2214_198` / 450.7 |

These are **weighted surface-graph costs to sparse scan centers**, not cave
entrances, seconds, routes in current Alpha, or proof of resource sufficiency.
They show why Alpha must be measured too: an apparently balanced reference can
have an asymmetry on a different existing measurement. Select task destinations
only after verifying their resource and access evidence; do not turn this table
into an economic ranking.

Local runtime evidence was inspected read-only on 22 September: newest observed
`/private/tmp/alpha-server/plugins/MinecraftMoba/measurements/` files
`provenance-1790086578507.csv` and `renewables-1790086578960.csv` each had 277
samples. Headers match the aggregate schemas above; last observed rows had zero
place/break calls and zero harvests respectively. This is an idle-session
observation, not a claim about all historical sessions. There is no player/team,
map configuration or task field in either schema, so these files alone cannot
supply the requested controlled comparison. No live server command was sent.

## Exact recommended first experiment

### 1. Freeze inputs and make the missing comparator concrete

Use a separate disposable Paper **1.21.11 / Java 21** test installation, the
already selected plugin target and existing Anvil world/diff authoring approach.
Do not deploy over the live server or re-freeze Alpha. Snapshot the pinned plugin
jar/config, terrain volume and region hashes, candidate/orientation, siting,
frontier, authored portfolio, route report and configuration diff for each map.
Record N/S homeland world coordinates from each map's own candidate and verify
spawn/fountains agree with runtime config. W/E remains the regional axis.

For the smallest controlled authored comparison choose **`resource_light` on
both seeds**, with the existing best candidate from each pinned frontier. Both
have the same 28-opportunity intervention budget. Label the result “matched
resource_light configurations”; it is not automatically a result for all four
live Alpha configurations. Preserve the current live configuration identity for
later replication; do not compare its random draw to the near-miss's best scalar.

The near-miss `structures-built.json` was synthesized from siting positions;
**faa6748 did not physically build a playable rescue map**. Before trials,
materialize it using existing steps only. `terrain_harvest.reauthor.reauthor`
sequences copy → portfolio → routes → structures → checks → export, but its
fountain verification hard-codes Alpha homeland coordinates even when given a
different candidate. It is **not directly reusable for the near-miss**. Minimum
fixture implementation is an external experiment driver that copies the chosen
base, invokes the existing `author_portfolio`, `routes` and `build_structures`
modules in that order with explicit seed-specific inputs, calls existing
`check_fountains(world, homelands)` with that candidate's homelands plus
`check_routes(report)`, then calls existing `map_diff.export`. Preserve the
wrapper's checks and semantics; do not change its authoring logic or blindly use
its Alpha defaults. Record the exact commands/arguments in the run manifest.
Confirm fountains, route checks, source registration, runtime world coordinates,
base/diff compatibility and observed selected configuration. If a pristine base
or source volume is unavailable, record that prerequisite; a JSON siting stub is
not a substitute. Generated worlds belong under a new `artifacts/worldgen/`
experiment directory; reports under `implementation/worldgen/reports/`; harness
code, if needed, stays under `implementation/plugin/`. This audit builds nothing.

Pin selection using existing `alpha.configurations.force` on the disposable
server. This is test fixture selection, not authoring or balance logic. Record
all changed fixture keys separately; keep gameplay coefficients/features equal.
Restore the same unplayed map and all match/player/source/provenance state before
each independent trial. Check inventories, rewards, WP, source availability and
clock after reset; keep the in-memory ledger before resetting it.

### 2. Two representative tasks, 16 short trials

Use two experienced operators; each performs both tasks on each map from each
team end: **2 operators × 2 maps × 2 sides × 2 tasks = 16 trials**. No opposing
player, cooperative helper or admin action during the timed interval. This is
uncontested economic capability, not combat balance. Counterbalance the four
map/side conditions for each task: operator A uses Alpha-N, near-S, Alpha-S,
near-N; operator B reverses that order. Reset between tasks as well as conditions.

Common Prototype/test fixture: level 1, same class and zero specializations,
full identical vitals, unenchanted stone pickaxe and axe, 16 bread, no target
materials, no prebuilt access works. Keep normal progression and use a written
identical reward-choice policy at each earned level; record capability changes.
Preflight the chosen class and kit against current inventory rules, then freeze
its exact item/character snapshot before timed trials. Keep normal server
mechanics, difficulty, phase, food/regen rules and source cadence identical;
record stochastic manifestation state rather than silently rerolling bad starts.

Give both operators the same kind of information: homeland and authored source
locations visible through the existing UI, no hidden ore coordinates. A uniform
untimed map briefing before both sides reduces learning-order effects. Search,
failed approaches and discovering that a source is unavailable count as time.
Do not force straight-line travel or the same absolute coordinates across maps.

| Task | Start and explicit useful-output condition | Why this verb |
|---|---|---|
| A: first metal utility expedition | From homeland, acquire wood/stone/fuel and at least six raw iron; process and craft one iron pickaxe plus one bucket; bring both back to the declared homeland stock point. Initial kit and food are excluded from output. Record first raw iron, six acquired, first usable tool, both crafted, and both delivered. | Search, mining, excavation, fuel, processing and return are a small real opening chain already represented by `OpeningBenchmarkTest`, without treating its approximate bulk workload as a target quota. |
| B: provision and establish a small food plot | From homeland, obtain wheat/seed stock from a registered authored wheat opportunity; deliver three newly crafted bread and establish four hydrated, planted wheat crop blocks at homeland with a traversable access path. Craft a wooden hoe if needed; supply one identical water bucket in this task's fixture only to avoid repeating A. Record source arrival, actual harvest/pickup, bread delivered, and plot planted. | Measures an authored resource actually used and the clearing/placement burden of useful organization. Four plants are an experimental task size, not a Development recognition rule or mature yield claim. |

Preflight B's wheat-source presence in both fixed configurations without changing
placements. If either lacks one, B is **not runnable as specified**; report the
content mismatch before collecting data and choose a shared authored resource
and equivalent output explicitly. Do not silently substitute village hay or
inject a source. Plot placement proves setup, not mature food throughput.

Cap each trial at **20 minutes elapsed wall time**, recording elapsed server
ticks too. Record every five-minute stock/WP checkpoint and every completion
milestone. A failed/timed-out goal is censored at the cap with partial output,
reason and last phase; never drop it or report the cap as completion time.
Retain deaths and recovery time as observed outcomes. These caps and quantities
are experiment parameters, not balance thresholds.

### 3. Measurements, without a new score

Per task record outbound/search, access work, acquisition, processing, return,
and total elapsed time. Processing can overlap travel: store event intervals and
milestones, not a falsely additive decomposition. Store acquired, crafted and
**delivered** quantities separately, plus remaining provisions and tools.

Record all successful break/place actions by material and position. Annotate
purpose from task phase/video: resource harvest, clearing/entry, traversal
bridge/steps, plot/processing setup, or uncertain. Preserve repeats and failed
attempt time; keep both action count and distinct positions. Do not infer intent
from material (stone can be target stock or overburden).

Travel: actual time, sampled horizontal/vertical distance, ascent/descent,
return/resupply trips, food consumed, damage/deaths, tool durability and time
inside a recognized Route. Mark teleport/Recall/death discontinuities separately;
never count teleport displacement as walking. Net hunger alone is misleading
with regen/Route relief; record food, saturation/exhaustion and relevant vitals.

Utilization: per registered source ID, visited, unavailable when attempted,
actually harvested, output acquired and used/delivered. Record source kind,
locus and initial availability; show counts against the specified opportunities
available during the task, not all theoretical loci. Use existing entity PDC
membership for animals and the current crop/provenance source attribution for
crops. Worksite proximity is discovery only; capitalize/exploit events and
outputs would be separate if a later task uses Worksites.

Tempo: timestamp actual level crossings, domain WP and ledger terms, inventory
and useful stock at 5/10/15/20 minutes. `/moba work <player>` and `/moba debug
<player>` provide snapshots now. Report delivered output and task time first,
WP/min and UAU/min only as secondary work-accounting rates using existing rules.

## Minimum implementation, in order

1. **No telemetry code for the initial feasibility rehearsal.** Record video with
   coordinates and a visible clock, task milestones and inventories; capture
   existing work/debug reports at start, five-minute checkpoints and finish.
   Export normal renewable/provenance CSVs and the exact run/config manifest.
   An observer can annotate block actions and source IDs in these short tasks.
   This is enough to establish whether the tasks run and locate the missing
   burden. Do not claim precise distance from an unaudited video estimate.
2. **If manual recording is insufficient for the 16 trials, add only a bounded
   task recorder**, recommended as `TaskTrialRecorder.java` plus wiring in
   `MobaPlugin`, and a small offline report script. Explicit begin/phase/end
   markers scope recording to one participant and RUNNING match. Append JSONL
   events with run/task/map/config/player/team IDs, monotonic wall time, match
   tick, world/position and event payload. Sample position/vitals/inventory at
   1-second cadence (label distance an approximation); record successful block
   actions and acquisition/output milestones as events. Preserve cancellation
   and provenance semantics; a before-clear provenance observation must not
   become a counted action if the action is subsequently cancelled.
3. **Read existing results; never duplicate economic rules.** Snapshot
   `earnedBy`, `ledger`, player state and the match clock. Record confirmed
   inventory/stock changes as evidence of pickup, crafting and delivery; event
   drops alone are insufficient. Attribute renewable interactions at the
   existing membership/source-resolution seam, without changing harvest or
   recovery behavior. No global high-frequency world scans, alternate source
   detection, auto-pathfinder, crafting agent or new progression calculator.
   If attribution needs a narrow observational callback, document that seam;
   the initial manual protocol can supply the source ID instead.
4. Before relying on the recorder, verify a disposable fixture: cancelled break
   counts zero, repeated place/break counts every successful action, dropped
   versus picked-up ore differs, partial/full-inventory craft counts match stock,
   another player's activity is excluded, reset cannot leak counts, teleport
   is excluded from distance, and a timeout remains incomplete. Compare one
   video-labelled task to the recorded events and WP snapshots. Test observation
   leaves output/awards unchanged. Do not modify scoring to make logs agree.

No recorder is implemented by this audit: reliable identity, cancellation,
inventory confirmation and lifecycle attribution are more than trivial logging.

## Analysis and next decision

For each operator and task show N, S and **S minus N** in ordinary units for each
map; then show `(S-N)_930010639 - (S-N)_Alpha`. Keep raw trials, completion count,
paired differences and their spread; two operators are a feasibility pilot,
not a statistical fairness certification. Separate resource stock, travel,
access work, output, and WP—no combined penalty score.

A repeatable side-specific delay (either N or S) in delivery/setup, explained by more travel or
access work and persisting across operators, supports a disadvantage **for these
configurations and tasks**. Extra WP with slower delivery can still be economic
harm. Different terrain with similar output times and burdens supplies no harm
evidence for these tasks, but does not prove equivalence or full-match fairness.
Do not adopt a pass/fail tolerance after seeing results.

Even a positive result does not identify 0.3173 as the causal variable: seed,
geology and placement co-vary. If the pilot finds a repeatable delay, repeat the
affected task on a second pinned existing profile on both seeds. A bare-terrain
or fixed-site access replay is a later diagnostic to distinguish authoring from
terrain, with its own changed-fixture label. Only then consider compensation,
screening or broader contested/team playtesting as explicit design decisions.

## Audit validation

Read source and tracked report schemas at the pinned commit; inspected current
local CSV headers/tail rows read-only; calculated the nearest-scan-center table
from existing matrices without regenerating inputs. Reviewed canonical regional
depth and transport distinctions. No runtime probe, build, deployment, new
world, simulator run or gameplay outcome is claimed. Documentation links and
`git diff --check` are the appropriate checks for this documentation-only change.
