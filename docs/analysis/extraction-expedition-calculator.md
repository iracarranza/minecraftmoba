# Extraction Expedition Calculator

Status: **Analytical tool / working specification**  
Date: 20 September 2026

## Purpose

This specification consolidates the project's existing Extraction, progression, geography, inventory, endurance, durability, Yield, and Logistics models into one reproducible question:

> Given a current map/resource manifest and a team strategy, how much elapsed match time passes before the team acquires or makes available a benchmark quantity of a resource?

It is a calculator contract, not a new resource-generation or progression system. Inputs unresolved in canon remain explicit parameters rather than hidden assumptions.

Report two clocks:

- **Acquired time** — first time qualifying material is in allied possession.
- **Available time** — first time the benchmark quantity is present in the specified usable team stock/location.

This prevents a remote extractor holding ore from being treated as equivalent to ore already available to Production.

## 1. Core state

For each player p at time t, track position, level, inventory slots, mission/provision/utility slots, Hunger/endurance, movement profile, tool/material, Efficiency, Unbreaking, Fortune, known opportunities, and carried target resource.

For each resource manifestation r, track resource, position/geometry, physical blocks, native drop profile, exposed/cave/buried access state, discovery state, terrain/access profile, excavation burden, hazard/exposure, optional Opportunity Relationship, and optional Worksite state.

A current map may provide exact coordinates/geometry. A coarser analytical manifest may provide measured route distance and explicit access/search estimates. Do not silently convert qualitative labels such as "far" or "difficult" into seconds.

## 2. Benchmark stopping conditions

For benchmark quantity Q*:

[
T_{acquired}=inf\{t:A(t)>=Q*\}
]

[
T_{available}=inf\{t:V(t)>=Q*\}
]

A(t) is cumulative qualifying target material currently acquired by the team; V(t) is material available at the declared destination/stock definition.

Report player-minutes separately:

[
PM=sum_p T_{p,committed}
]

Elapsed time and player-minutes must never be conflated. Parallel labor can reduce wall-clock time without reducing labor.

## 3. Event pipeline

Use deterministic discrete-event simulation:

    spawn
    -> provision / obtain required tool
    -> select opportunity
    -> search / discover if unknown
    -> travel
    -> establish access / excavate
    -> extract
    -> update yield, inventory, Hunger and durability
    -> continue OR return/resupply/replace tool
    -> deliver/deposit
    -> select next opportunity
    -> repeat

Parallel players advance on one match clock. The next event is the earliest scheduled event among all players/world events.

Persistent state carries across six-minute reporting windows. A window boundary does not reset position, inventory, Hunger, tool wear, knowledge, deposits, infrastructure, or opportunity state.

## 4. Travel

For traversed segment s:

[
T_{travel,s}=d_s/v_{p,s}+T_{terrain,s}+T_{interaction,s}
]

and:

[
T_{travel}=sum_s T_{travel,s}
]

Prefer measured traversable paths over straight-line distance. Routes, movement progression, class mobility, roads, boats, etc. modify relevant segment inputs; they do not multiply ore quantity.

The project has used about 4.3 blocks/s walking as a sensitivity reference; it is not a universal canon input. The run must declare movement assumptions.

## 5. Search and discovery

If a manifestation is already known, T_search=0. Otherwise prefer, in order:

1. measured discovery time from generated-map path/search simulation;
2. deterministic search route over candidate cells/regions;
3. explicit sensitivity input.

Never infer search time from ore market value or use Acquisition Difficulty as literal seconds. Acquisition Difficulty is an analytical significance concept; this calculator times actual actions.

Discovery changes persistent team knowledge according to the experiment's information-sharing rules.

## 6. Access and excavation

Separate reaching the opportunity from breaking target ore:

[
T_{access}=T_{approach}+T_{excavation}+T_{setup}
]

If exact obstructing blocks are known:

[
T_{excavation}=sum_b t_{break}(b,tool,Efficiency,effects)
]

Use Minecraft/tool-specific break-time data when available. Otherwise require an explicit excavation-rate sensitivity; do not hide a generic mining-efficiency multiplier.

## 7. Target extraction

For B target blocks:

[
T_{ore}=sum_{i=1}^{B}t_{break}(ore_i,tool,Efficiency,effects)
]

Efficiency changes breaking/work rate, not physical ore count. A homogeneous fixture may use T_ore=B/r_break when r_break is declared.

## 8. Yield / Fortune

Current expected Fortune multipliers used by project analysis:

| Fortune | Expected multiplier |
|---|---:|
| 0 | 1.00 |
| I | 1.33 |
| II | 1.75 |
| III | 2.20 |

For resource r with base expected native drop mu_r:

[
E[H_r]=B_r*mu_r*Y_F
]

For Iron-like one-unit native drops, mu=1. For Copper's discussed 2-5 raw native drop, mu=3.5:

| Fortune | Expected raw Copper / physical block |
|---|---:|
| 0 | 3.50 |
| I | 4.67 |
| II | 6.125 |
| III | 7.70 |

Expected physical blocks required:

[
B_{req}=ceil(Q*/(mu_r*Y_F))
]

The simulator caps extraction by actual manifestation quantity and moves to another opportunity when exhausted.

Baseline mode is deterministic expected yield. A later Monte Carlo mode may sample actual Minecraft drop distributions. Results must identify mode.

## 9. Inventory and cargo

[
C_{cargo}=C_{inventory}-C_{mission}-C_{provisions}-C_{utility}
]

There is no ordinary offhand escape hatch. Convert cargo slots to carrying capacity using actual stack size and simultaneously carried outputs.

Return/transfer is triggered by the first binding modeled condition:

[
T_{turn}=min(T_{inventory},T_{endurance},T_{tool},T_{objective/recall},T_{threat})
]

Only include objective/recall/threat timers when specified; otherwise report them omitted.

## 10. Hunger and endurance

Food/endurance is a real expedition constraint. Consume Hunger/exhaustion from actual modeled actions where available. If exact action-level exhaustion is not supplied, accept an explicit endurance budget:

[
E_{remaining}=E_{start}+E_{food}-E_{spent}
]

and schedule return/resupply before invalid state.

Routes may modify locomotion exhaustion if the tested Route rules say so. Do not apply Route modifiers to mining, combat, regeneration, or unrelated exhaustion. Food production and food accessibility are separate.

## 11. Durability and tool replacement

For base durability D and expected Unbreaking multiplier U:

[
D_{eff}=D*U
]

Current expected Unbreaking lifetime approximations used in project analysis: tools/weapons UI about 2x, UII about 3x, UIII about 4x. Armor follows a different curve.

Current non-canon pick/tool durability sensitivity:

| Material | Base uses | UI expected uses |
|---|---:|---:|
| Wood | 16 | ~32 |
| Stone | 36 | ~72 |
| Copper | 60 | ~120 |
| Iron | 96 | ~192 |

These are optional calibration inputs, not canonical values. When remaining capacity cannot complete the next required action, schedule replacement/repair/resupply according to actual equipment availability.

## 12. Progression over the expedition

Player state is time-dependent. Relevant current working landmarks include Lv4 Efficiency I + Unbreaking I; Lv7 Yield I/Fortune I; Lv9/14/19 eligible task advancement choices; Lv16 working ~10% universal movement-speed proposal.

Do not retroactively apply rewards.

Support:

1. **fixed-state mode** — capability held fixed, isolating geography;
2. **progressing mode** — supplied level timeline/progression event stream.

The calculator must not invent XP gain from Extraction to predict its own level timeline unless an authoritative progression-credit model is explicitly supplied; that would create a circular hidden assumption.

## 13. Infrastructure and Logistics

Ordinary roads, caches, storage, boats, rails, etc. may alter actual travel/delivery before recognized Infrastructure.

Recognized systems modify only established effects. Route affects traversal/locomotion. Supply Line affects delivery availability according to Capacity and Item Rate. Construct matters only where actual geometry/protection changes the expedition. Development Zone has no direct ore multiplier.

For Supply Line Capacity C slots/delivery and Item Rate f:

[
Throughput=C*f
]

and Delivered(t) cannot exceed actual upstream stock. Logistics never creates target material.

## 14. Worksites

Mining Site state:

    Dormant
    -> selected at sunset
    -> Activated: resource package communicated; ore concealed
    -> qualifying Construct
    -> Capitalized: special ore manifests
    -> Extraction
    -> Exploited

Thus Worksite ore cannot begin before activation wait plus capitalization. Industrial Factory is not an ore source unless a benchmark explicitly asks for downstream transformed output. First-capture shared Infrastructure is future capacity, not immediate ore.

## 15. Opportunity selection / rational team policy

Support at least:

- **nearest-known**;
- **highest-yield-known**;
- **minimum-delivered-time**;
- **scripted** player-to-opportunity sequence;
- **oracle**, where all manifestations are known at t=0, as a lower-bound geography benchmark.

Primary rational benchmark: minimum predicted delivered time among currently known opportunities. Normal policy cannot inspect undiscovered map data.

## 16. Parallel team allocation

Each Extraction-assigned player has an independent event queue but shares discovered opportunities, remaining physical blocks, team stock, infrastructure, caches/destinations, and world events.

Two players cannot mine the same physical block. Reserve opportunity shares when extraction events are scheduled.

Report:

[
T_{elapsed}, PM_{total}, PM_{search}, PM_{travel}, PM_{access}, PM_{extract}, PM_{return/delivery}
]

This exposes whether a faster benchmark comes from productivity or simply more committed team labor.

## 17. Serializable run input

Suggested schema:

    benchmark:
      resource: copper
      quantity: 182
      availability_destination: team_base

    mode:
      yield: expected
      progression: fixed | supplied_timeline
      knowledge: normal | oracle
      policy: minimum_delivered_time

    team:
      players:
        - id: extractor_1
          start_position: [...]
          level: ...
          inventory_slots: ...
          mission_slots: ...
          provision_slots: ...
          utility_slots: ...
          tool: ...
          efficiency: ...
          unbreaking: ...
          fortune: ...
          food/endurance: ...

    map:
      destinations: [...]
      manifestations:
        - id: copper_01
          resource: copper
          position: [...]
          physical_blocks: ...
          native_drop_mean: 3.5
          discovery: ...
          access: ...
          path: ...
      infrastructure: [...]
      worksites: [...]

    assumptions:
      omitted_interruptions: [...]
      explicit_sensitivities: [...]

Every unresolved parameter belongs under assumptions/sensitivities rather than being silently supplied.

## 18. Required outputs

Return benchmark quantity; acquired time; available time; physical ore blocks consumed; expected harvest; manifestations depleted/partial; trips; player-minutes; time/player-minutes by provisioning, search, travel, access/excavation, target extraction, return/delivery; durability/replacements; food/endurance; cargo use; progression events; infrastructure used; binding constraints; unresolved assumptions.

Also report remaining target material in player inventories, caches, transit, and destination stock at stopping time.

## 19. Validation invariants

Automated tests should enforce:

1. Conservation: delivered + carried + cached + in-transit target material cannot exceed realized harvest minus legitimate consumption.
2. No duplicate depletion.
3. No future knowledge in normal policy.
4. Fortune changes harvest, not physical manifestation count.
5. Efficiency changes break time, not ore count/yield.
6. Logistics changes availability time, not harvest.
7. Team elapsed time is event-clock time, not summed labor.
8. Reporting windows do not reset state.
9. Concealed Mining Site ore cannot be extracted before capitalization.
10. Missing required data produces unresolved output/error, never inferred measurement.

## 20. Canon versus sensitivity

Tag every parameter source:

- **CANON / WORKING DESIGN**
- **MAP MEASUREMENT**
- **EXPERIMENT INPUT**
- **SENSITIVITY / NON-CANON**

The tool exists to make balance questions reproducible without promoting convenient fixture values into canon.

## 21. Immediate use

Once current worldgen/resource work exports manifestations and paths:

    load map manifest
    load team fixture
    set benchmark resource + quantity
    select knowledge/policy/progression mode
    run event simulation
    compare acquired-time, available-time and player-minute breakdown

Useful first comparisons: 182 Copper gross combat-tier benchmark; Iron operational maturity; Diamond major investment; oracle vs normal discovery; one vs multiple extractors; actual Route/Supply Line state; Fortune 0/I/II/III; fixed-state vs supplied progression.

Kitfighter Salvage belongs downstream as reduced net Production cost, not fake Extraction yield.

The central output is not merely ore/minute. It is the causal decomposition of **where match time went**.
