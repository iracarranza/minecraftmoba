# Worldgen spatial doctrine audit — 22 September 2026

**Doctrine/documentation only.** User clarification is incorporated into canonical
`maps.md`, `objectives.md` and `infrastructure.md`; numerical reports, algorithm
behavior and fixtures are preserved. Audited pushed main **8199c13**, with the
completed Hunger audit **9bb5093** retained in this branch. Source:
[requested spec](../../specs/auditworldgenrecoverspatialmodel.md). Decisions and
supersession are recorded in the [reconciliation](../reconciliation/2026-09-22-spatial-doctrine.md).

## Result

The symmetry contract applies to the **Homebase Core/interface**. A **Socket**
accepts the same standardized Core with bounded integration work. Natural
**supported regions/hinterlands** provide distributed useful opening opportunity,
with both a floor and a concentration ceiling. **Wilderness** is allowed to
differ substantially between teams. None of the current aggregate terrain scores
verifies this complete contract.

**Strategic Depth** is outward relationship to each team's opening space;
**Regional Character** is position within a Map Type's geographic composition.
The Default Ocean→Coast→Open Land→central interior→Forest→Rugged Uplands→Alpine/
Frozen Peaks tendency is not seven stripes or a universal contract for all types.
Neither axis is equivalent to current travel cost after player development.

The live implementation has useful physical and economic seams. It does not yet
implement standardized Core/Socket acceptance, a supported-region boundary,
opening floor/ceiling checks, or a generic two-axis regenerative richness policy.

## Machinery classification

| Machinery / assumption | Classification | Evidence and scope |
|---|---|---|
| Harvested terrain, oriented grids, real resource/cave observations | **Survives unchanged** | `terrain_harvest/opportunity_map.py`, `caves.py`, staged grids and manifests measure geography. They do not assign generic depth tiers or guarantee income. Retain resolution and uncertainty labels. |
| Terrain-weighted paths, Practical Reach, travel matrix, corridor readback, diff/base fingerprints | **Survives unchanged** | `vanilla_search/task_a.py`, `expedition/travel.py`, `routes.py`, `reauthor.py`, `map_diff.py` remain measurement/physical-check seams. Cost units are not actual task time. Preserve all existing checks. |
| `homeland quality`, selected-band accessible land, connected-patch gaps | **Survives but is reinterpreted/re-scoped** | Pad suitability and sampled terrain distribution, not general fairness. `task_a.depth_summary` excludes both homeland footprints. Existing `regional_depth`/`deep_core` keys are legacy analytical labels, not the new Core or Regional Character. |
| Paired structure siting | **Survives but is reinterpreted/re-scoped** | `544747f` / `vanilla_search/structures.py` uses coarse footprint/approach proxies and matched layer scores. It does not build a functionally mirrored Core/interface or measure its complete integration cost. |
| Packing and opportunity-count frontier | **Survives but is reinterpreted/re-scoped** | `bb1feac` / `packing.py` answers a declared prototype demand scenario. Counts/spans are fixtures, not canonical demand or cut/fill cost. N/S usable-area asymmetry alone does not invalidate a packing. |
| Weighted portfolio parity and Route spillover rescans | **Survives but is reinterpreted/re-scoped** | Estimated access to the selected content and collateral corridor effects remain informative. Neither `balance_asymmetry` nor qualifying-search count proves competitive quality, opening viability or general authorability. |
| Source, Kind, availability/depletion, recovery, manifestation, region and provenance/membership | **Survives unchanged** | Runtime `Renewables`, `RenewableKinds`, `OpportunityRegion`, `RegenerativeOpportunity`, `WorldTerrain`; physical manifestation requires eligible current terrain and subsequent human acquisition. Preserve these seams. |
| Whole-Core fit and bounded Socket integration | **Needs later modification** | Existing sampled homeland and separate structure pads are insufficient. Need an explicit shared footprint/interface contract plus exact support/headroom/cut/fill/clearing and transition readback; no large halo authorer. |
| Initial Route target policy | **Needs later modification** | `task_a.fit_routes` regional targets and optimizer's farther-half target selection can project well beyond opening support. Existing spillover models must describe incidental effects, not prescribe deep-map equalization. Define supported scope before changing lengths/targets. |
| Seed-specific reach inputs | **Needs later modification** | `tools/analysis/map_authoring_optimizer.py` has both global `HOMES` and local `homes` in `idw_reach_model` fixed to Alpha. Direct reach and spillover use them. Require explicit actual seed/side origins and provenance before treating a new search as a reliable cross-seed comparison. This pass does not regenerate old scores. |
| Opening economic floor/ceiling and regenerative richness policy | **Needs later modification** | Current founder near/deep selection, team count quotas and source fixture magnitudes do not test distributed usefulness or full-team concentration dominance. A later authoring policy must separate team-relative depth from ecological kinds. |
| “Natural N/S gap is a deficit; compensate or reject it” | **Obsolete/contradictory** | No demonstrated gameplay consequence, and these samples extend outside the controlled Core. Preserve classifier outputs as historical hypotheses; do not use them as established screening instructions. |
| “Lower homeland gap means better map”; automatic POI highways; identical species/count fairness | **Obsolete/contradictory** | Confuses the controlled Core contract with natural geography or legacy profile constraints. Future decisions require contract-specific checks or gameplay evidence. |

## The eight-commit sequence

| Commit | Preserved evidence | Corrected interpretation |
|---|---|---|
| `544747f` | Pair-scored structure footprints, layer ordering and approach proxies; 8-block grid limitations | Low paired quality gaps are not proof that Core geometry/interfaces are functionally mirrored. Exact defensive elements belonging to the Core remain Open. |
| `bb1feac` | Per-cell packing, declared counts, conflicts and unplaced consumers | Asymmetric natural area is not inherently unusable; counts are a prototype scenario, not a promise that each side receives identical resource geography. |
| `997a319` | Eight candidate comparisons; degenerate all-band statistic honestly exposed | Composition-agnostic measurement is useful. Alpha's 0.0645 homeland gap does not prove “well chosen,” and 930015734's 0.3397 does not prove it unbalanced. Default macro-geography still needs its own future search contract. |
| `1787590` | Accessible and connected gaps, component diagnostics, 28/32/38 opportunity frontier and −0.21 correlation | `opportunity`/`clearable`/`physical` are heuristic labels. Opportunity counts are not integration-work costs; correlation among accepted prototype configurations is not a rescue policy. |
| `faa6748` | 930010639 artifacts; 28-opportunity `resource_light` score 0.0333; best 0.0161; 991 versus 2,285 qualifiers; unchanged 0.3173 | Neither successful scalar “rescue” nor failed terrain “repair” is established. Authoring is not tasked with erasing that terrain difference. Smaller scalar values describe the recorded model, subject to its origin assumptions. |
| `7cae5ae` | Dated handoff of that rescue test | Its compensation-versus-terrain-gate fork is superseded. No such decision is forced by permitted Wilderness asymmetry. |
| `4244b39` | Measurement audit, representative Task A/B verbs and proposed 16-trial design | Retain the question of actual economic consequence; remove the presumption of a harmed S side. No trial results exist. Test the authored-opportunity model's coverage, not whether terrain was normalized. |
| `8199c13` | Physical seed-specific fixture, 28 opportunities, 10 registered sources, 8 Worksites, load/spawn/reset evidence | A loadable prototype is not a certified Core/Socket map or a balance result. Endpoint ice readback fails; Task B has no authored wheat. These blockers remain. |

The **0.3173** value is `abs(N-S)/(N+S)` over selected dry sampled Wilderness
bands, omitting `deep_core_350_plus` and unreachable samples. It is not a 31.73%
output loss or even that percentage of N's land. Canopy, dry land, buildable
terrain and delivered economic output are different measurements. The original
report files now carry prominent interpretation corrections; raw JSON, counts
and experiment inputs remain unchanged. “Near-miss” remains an artifact name,
not a competitive diagnosis.

The same restraint applies to the 991/2,285 ratio: it describes acceptance under
one prototype sampling/filter setup, not generally “harder to author well.” In
addition, the present optimizer retains Alpha origins. Without verified execution
provenance that overrides both origin sites, the historical near-miss scalar must
not be certified as seed-correct merely because the input travel matrix is
seed-specific. Physical fixture readback is a separate claim and survives.

## Regenerative implementation versus doctrine

The [19 September seam proposal](../proposals/2026-09-19-regenerative-sources-seam.md)
historically shipped empty tables and described lazy availability recovery.
Current pushed code supplies 12 Alpha fixture sources, and the near-miss fixture
supplies 10 from its own portfolio. Current `Renewables` has located Sources,
explicit Opportunity Regions (or migrated coarse regions), lifecycle recovery,
current-terrain manifestation queries, finite crop/animal manifestations,
entity membership and block provenance. Scheduled `tickOpportunities` advances
recovery using `ratesFor`; a legacy lazy `available()` path also remains. The
old “ships empty / lazy only” comments are corrected, not the machinery.

`RenewableKinds.Kind` is exactly identity, type, block targets and entity targets.
Its comment explicitly rejects depth/value/region, and its vocabulary must stay
that way. The goat mapping added by `8199c13` is identity coverage, not a gradient.
Source region support is spatial runtime state; it is not an encoded Map Type
Regional Character or team-relative depth policy.

Shipped config marks animal capacity 6 / recoverTicks 2400 and crop capacity 24 /
recoverTicks 1200 as **NON-CANON ANALYTICAL FIXTURES**. Temporal phase fractions,
night rates and eligibility values are separately marked Working calibration /
minimal Alpha fixtures; they drive current lifecycle behavior and must not be
mistaken for depth-dependent recovery curves. This pass changes no values.
`author_portfolio` currently records founder/renewable opportunities with empty
block templates instead of prebuilding pens/irrigated fields. Runtime selection
and manifestation do not establish the missing richness/novelty policy.

The clarified authoring direction permits greater richness in **quantity/
concentration and novelty/specialization** with depth. Regional Character selects
plausible kinds. It neither forces density to rise in every deeper patch nor
requires matching team species/counts. No exact tiers, values, species mapping
or recovery curve is chosen here.

## Smallest later implementation, in dependency order

1. **Decide the Core/Socket contract first.** Specify the shared Core footprint,
   immediate defensive elements and exit/interface geometry, then what counts as
   bounded integration and a viable distributed opening. Keep exact limits Open
   until chosen; do not expand the authorer's clearing halo to pass difficult sites.
2. **Add an explicit spatial contract record at the existing siting seam.** Keep
   Core footprint/interface, supported-region evidence, team identity/orientation
   and natural geography separately addressable. Do not rename historical
   `homeland`/`regional_depth` JSON silently. Reuse real block data to report
   support/headroom and changed-block categories (cut, fill, clearing) per Socket;
   reject out-of-contract proposals and verify the completed interfaces.
3. **Remove remaining Alpha origin assumptions before new searches.** Supply
   actual seed-specific origins to every optimizer reach/direct/spillover use,
   distinguishing candidate centers from built Fountain/spawn coordinates.
   Preserve old outputs as historical and validate coordinate provenance; no new
   balance objective is needed for this input repair.
4. **Re-scope initial Route selection after supported scope is defined.** Reuse
   existing path authoring and exact checks; constrain intended targets to opening
   support rather than automatically selecting distant/high-reach opportunities.
   Retain collateral reach measurements and later player network growth. No
   numerical Route retuning is part of this pass.
5. **Add floor/ceiling evidence before a generic richness authorer.** Assess useful
   opening verbs and distributed opportunity versus a concentrated full-team
   opening incentive. A later policy can combine depth-based permitted richness
   with geography-based kinds and emit existing Source/region inputs. Keep Kind
   identity and runtime lifecycle unchanged; don't replace this with equal counts.

Future Map Types may then organize these seams as macro-geography → team topology
→ Socket pair → Wilderness viability → authorability → physical verification →
PlayableMap. Implementing a Default recognizer or other Map Types is not the next
step before the Core/Socket and opening contract are defined.

## Task A/B and remaining fixture blockers

Retain the [paired task protocol](2026-09-22-terrain-gameplay-comparison.md) as a
**Prototype/test proposal** answering:

> Does this permitted wilderness asymmetry cause a competitive consequence that
> the authored-opportunity model fails to capture?

Interpret effects in delivered output, time, travel and access work, for either
side; distinguish opening contract failures from permitted natural differences
with no demonstrated consequence. A positive result for two tasks does not
identify 0.3173 as causal or prescribe normalization; a null result does not
certify full-match fairness. Clarify tested Core/interface and supported-region
scope when resuming. Existing fixtures can remain historical controls even if
they do not meet a future finalized contract.

Do not proceed now: the fixture still has its north destination ice readback
conflict; Task B's registered wheat is absent; Task A has no completed accepted-
fixture rehearsal. A future explicit equivalent Task B revision must use real
shared content, not inject wheat or silently substitute hay. No 16-trial run,
new telemetry, world regeneration or alteration of 930010639 occurred here.

## Validation of this pass

Only Markdown, source comments/docstrings and YAML comments changed. Python
executable ASTs after removing docstrings, Java source outside comments, and
parsed YAML values all match the parent commit. New local link targets and
heading links pass; no tracked numerical/fixture artifact is changed. No worldgen, server or gameplay
tests are appropriate or run for these documentation-only edits.
