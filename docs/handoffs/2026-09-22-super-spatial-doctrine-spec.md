NEXT CODEX/CLAUDE PASS
DEFAULT MAP VERTICAL SLICE:
PHYSICAL OBJECTIVES → LAIR → RECOGNIZER → AUTHOR → VERIFIER

Repository:
  iracarranza/minecraftmoba

Current branch/state:
  Previous migration completed at:
    14cb46c
    branch codex/spatial-cadence-migration

  Audit:
    docs/audit/2026-09-22-spatial-cadence-migration.md

MISSION

Push aggressively toward the actual world-generation goal:

  EVERY /moba match start eventually receives a previously unused,
  competitively valid, automatically authored Minecraft MOBA map.

Do NOT stop at another doctrine-only audit or measurement-only pass.

The target for THIS pass is the first serious end-to-end Default-map compiler
vertical slice:

  unseen seed / natural region
      ↓
  Default Map recognition
      ↓
  Homebase socket discovery
      ↓
  compact Hinterland validation
      ↓
  team-objective socket discovery
      ↓
  singular Lair socket discovery
      ↓
  competitive candidate selection
      ↓
  physical authoring
      ↓
  physical verification
      ↓
  PlayableMap OR explicit rejection

Attempt to produce at least ONE newly discovered, non-hard-coded,
automatically authored Default map from an unseen seed that passes the current
physical verification contract.

Do not fake success to meet that target. If a real blocker prevents the
vertical slice, leave the pipeline failing closed and identify the exact
blocker with evidence.

======================================================================
1. START BY READING THE PREVIOUS PASS
======================================================================

Read:
  docs/audit/2026-09-22-spatial-cadence-migration.md

Inspect the implementation produced by 14cb46c.

Treat that pass as current baseline.

Important findings already established:

- There was no actual Overworld/Nether/End runtime state machine.
- Match.elapsed remains the temporal source of truth.
- MatchClock arithmetic drives boundaries.
- tick/skip now share advance().
- Current cadence is:
    Worksite I
    Giant
    Worksite II
    Ghast
    Worksite III
    Dragon.
- Worksites consume a tier rather than an event ordinal.
- Lair lifecycle receives every night transition.
- surviving bosses persist and are replaced at the next Lair event.
- all three team defensive objectives coexist.
- siege advantage exists only as an unresolved seam.
- compare_seeds schema /2 reports contrast rather than weaker_team /
  deficit_kind.
- objective_forms.certify currently FAILS CLOSED intentionally.
- alpha.lair.site is currently empty intentionally.
- map_authoring_optimizer.py still optimizes balance_asymmetry and is now one
  of the primary obsolete implementation targets.
- two test_author_portfolio failures reproduce unchanged at parent 03c5bea;
  do not assume they are regressions from 14cb46c.

======================================================================
2. IMPORTANT PHYSICAL-ASSET SITUATION
======================================================================

The current physical world artifacts are NOT yet the current design.

Do not build the new recognizer around legacy objective geometry.

Current design requires:

PILLAGER OUTPOST
  = Pillager Outpost WATCHTOWER ONLY.
  Peripheral cages/tents/log piles/etc. are not part of the required team
  objective structure.

NETHER BASTION
  = Bridge Bastion rampart/central body.
  Remove the long projecting bridge.
  Do NOT use the older Treasure Room objective.
  Do NOT guess its dimensions: derive/measure the retained form.

END SPIKE
  = vanilla-style Ender Dragon arena obsidian spike
    + End Crystal
    + cage where applicable.
  This replaces the historical End Tower concept.
  Do not merely rename an old end_tower mesh/config and assert compliance.

GIANT MONSTER LAIR
  = does not yet have a canonical physical authored implementation.

This is NOT a reason to stop before recognizer work.

It means physical prototype/certification is STAGE ZERO of this pass.

======================================================================
3. STAGE ZERO — BUILD/MEASURE CURRENT OBJECTIVE FORMS
======================================================================

Create or derive actual physical prototypes/fixtures for the three current
team objective forms.

Prefer Minecraft-native structures/assets/generation data where available
rather than manually approximating vanilla geometry.

For each objective determine the physical contract the socket recognizer
actually needs.

Do NOT reduce every structure to the same bounding-box concept.

OUTPOST contract should consider:
  - actual retained footprint;
  - height;
  - terrain contact;
  - entrance/access;
  - required clearance;
  - bounded integration needs.

BASTION contract should consider:
  - actual retained Bridge Bastion body/rampart;
  - irregular occupied volume;
  - multilevel/navigation requirements;
  - terrain contact;
  - approach/access;
  - required clearance;
  - bounded integration needs.

END SPIKE contract should consider:
  - small ground footprint;
  - actual radius/variant where relevant;
  - very large vertical clearance;
  - crystal/cage geometry;
  - exposure;
  - approach/access;
  - essentially no interior-navigation requirement.

Update objective_forms certification based on measured/derived reality.

Fail closed where a required fact genuinely cannot be established.

Do not preserve legacy geometry merely to make tests pass.

======================================================================
4. STAGE ZERO — CREATE THE FIRST LAIR PHYSICAL CONTRACT
======================================================================

The Lair has no existing canonical physical artifact.

Do NOT respond by inventing an elaborate bespoke boss arena.

The current design intentionally makes the LAIR SOCKET / NATURAL TERRAIN
VOLUME more important than a giant authored structure.

The Lair is:

- exactly one permanent landmark per map;
- massive/conspicuous as a PLACE;
- a repeated shared encounter site;
- used successively by Giant, Ghast, Ender Dragon;
- modifiable by players;
- terrain-conforming;
- capable of accumulating match history;
- not necessarily flat;
- not necessarily circular;
- not necessarily geometrically central;
- not necessarily biome-neutral.

Its surrounding terrain must provide:

- enough usable ground for Giant/player/PvP combat;
- substantial usable 3D air volume for Ghast and especially Dragon;
- multiple practical approaches;
- building/retreat/staging space;
- reasonable visibility/readability as a landmark;
- no requirement for enormous excavation or flattening.

Build the MINIMUM authored physical manifestation needed to establish the Lair
as a persistent recognizable place.

Examples of acceptable categories of intervention:
  - modest landmark architecture;
  - terrain-conforming markers/ruin-like elements;
  - encounter anchor;
  - spawn/registration anchors;
  - bounded local integration.

Do NOT silently canonize decorative details that have not been designed.

Separate:

  LAIR FUNCTIONAL CONTRACT — canonical enough to implement now

from:

  LAIR ARCHITECTURAL/DECORATIVE TREATMENT — provisional/open.

The limiting encounter volume should assume the Dragon is the demanding case,
but verify actual runtime behavior rather than relying only on theory.

======================================================================
5. SMOKE-TEST THE LAIR LIFECYCLE IN A DISPOSABLE WORLD
======================================================================

Since disposable worlds will already be needed, perform a runtime smoke test
of the cadence/lifecycle.

Verify at least:

  Worksite I
      ↓
  Giant
      ↓
  Worksite II
      ↓
  Ghast
      ↓
  Worksite III
      ↓
  Dragon

Use skip/clock advancement through the real runtime path.

Verify:

- Giant appears on its Lair event.
- surviving Giant persists through the following Worksite event.
- Giant is replaced when Ghast's Lair event arrives.
- killing an occupant leaves the Lair dormant until next Lair event.
- Ghast replacement/cleanup works even if Ghast has moved away from the
  nominal socket.
- Dragon replacement/spawn path executes.
- missing one boss does not stall cadence.

DO NOT invent encounter mechanics merely to make vanilla mobs entertaining.

Known concerns to observe/report:

- vanilla Giant has no meaningful designed AI;
- an Overworld Dragon may carry End-arena assumptions.

Classify findings as:

  LIFECYCLE DEFECT
  ENCOUNTER-DESIGN DEFICIENCY
  LAIR-PHYSICAL-CONTRACT DEFECT
  VANILLA-ENGINE CONSTRAINT

Only the first and third necessarily block this map-compiler pass.

======================================================================
6. REPLACE balance_asymmetry AS THE OPTIMIZER TARGET
======================================================================

map_authoring_optimizer.py must no longer treat generic N/S wilderness
similarity as the thing being optimized.

Preserve useful measurements.
Replace their interpretation.

Do NOT simply replace balance_asymmetry with another arbitrary giant
"balance score."

Move toward:

  HARD / GATING COMPETITIVE CONSTRAINTS
      +
  OPTIMIZATION AMONG VIABLE CANDIDATES.

Candidate viability should increasingly answer concrete questions such as:

- Does the map satisfy Default Map regional structure sufficiently?
- Are there two viable Homebase sockets?
- Can each accept the same Core with bounded intervention?
- Does each support a compact Opening Hinterland?
- Does the opening satisfy its floor?
- Does the opening avoid known ceiling violations?
- Can all six team defensive objective forms be sited?
- Is their team-axis ordering valid?
- Is there one viable Giant Monster Lair socket?
- Is the broader Wilderness structurally playable?
- Can required authored opportunities be supported?

Then optimize among viable combinations using actual meaningful costs such as:

- Homebase integration cost;
- Lair team-access disparity;
- objective authoring/integration cost;
- pathological access disparities with demonstrated competitive consequence;
- total required terrain intervention.

Do not penalize ordinary natural terrain difference merely because it differs.

======================================================================
7. HOMEBASE + COMPACT HINTERLAND
======================================================================

Preserve current doctrine:

HOMEBASE CORE
  = small standardized/mirrored authored competitive geometry.

HOMEBASE SOCKET
  = natural site accepting that Core with bounded integration.

OPENING HINTERLAND
  = compact natural opening envelope around/outside the Core.

The Hinterland is NOT:
  - the team's whole half;
  - everything between Fountain and midline;
  - a giant radial homeland.

Wilderness may exist:
  - toward midline;
  - east/west;
  - behind/poleward of the Homebase.

The conceptual geometry may resemble a smaller ellipse embedded within
Wilderness, but do not require a literal ellipse mask.

The opening floor should permit fundamental verbs without resolving them.

Known opening-ceiling exclusions:

  VILLAGE
  CARROTS
  EQUIPMENT-SUFFICIENT ACCESSIBLE IRON

The iron rule does NOT mean zero iron.

Do not invent rigid per-resource quotas without measurement.

If implementation requires provisional thresholds, follow the empirical rule
in §13 below.

======================================================================
8. TEAM OBJECTIVE SOCKET SEARCH
======================================================================

All three objectives are WILDERNESS structures.

None belong in the Opening Hinterland.

For NORTH, projected from midline toward Fountain:

  MIDLINE
    → OUTPOST
    → BASTION
    → END SPIKE
    → FOUNTAIN.

For SOUTH, the corresponding team-axis ordering reverses spatial direction.

This is ORDINAL spatial ordering, not a straight lane.

Allow:
  - substantial W–E displacement;
  - unequal N–S intervals;
  - different surrounding terrain;
  - different approach geometry.

Do NOT require:
  - mirrored objective coordinates;
  - equal distances;
  - straight lines;
  - identical socket geometry.

Each objective has its own physical socket contract.

Search natural terrain for compatible sites rather than forcing one generic
objective-site template.

The ordering is designed.
The geography between objectives is discovered.

======================================================================
9. SINGULAR LAIR SOCKET SEARCH
======================================================================

Search for exactly ONE Lair socket.

The Lair is the exceptional case where team-access parity genuinely matters
because both teams contest one indivisible shared opportunity.

Measure initial Practical Reach:

  R_N(L)
  R_S(L)

and:

              |R_N(L) - R_S(L)|
  A_L = --------------------------------
        (R_N(L) + R_S(L)) / 2

Use the best existing Practical Reach/path-cost implementation where valid.

Do not use Euclidean distance as a substitute unless only as a cheap
pre-filter.

The Lair may move substantially along Default's W–E regional axis.

It may even occur near an extreme regional character such as frozen/alpine
terrain if:

  - its encounter-volume contract works;
  - both teams have near-equivalent practical initial access;
  - it remains a meaningful shared landmark.

Do not require literal map-center placement.

======================================================================
10. MEASURE A_L, BUT ALSO USE IT
======================================================================

Do not stop after printing A_L distributions.

Measure candidate Lair sites across a meaningful sample of candidate
geography/seeds.

Report:
  - number of sites examined;
  - number physically viable;
  - A_L distribution;
  - best-site distribution per map;
  - relationship between good parity and authoring cost;
  - obvious pathological cases.

If a threshold is needed to continue the vertical slice:

  DO NOT INVENT IT FROM INTUITION.

Instead:

  1. measure;
  2. inspect distribution;
  3. choose a clearly labeled PROVISIONAL ALPHA threshold supported by the
     sample;
  4. keep it configurable;
  5. document the evidence and sensitivity.

Continue implementation using that provisional value.

Do not halt the entire pass merely because human design has not previously
named a percentage.

======================================================================
11. DEFAULT MAP RECOGNIZER
======================================================================

Push the existing recognizer toward the actual Default contract.

Default regional concept:

  OCEAN
    → COAST
    → OPEN LAND
    → CENTER
    → FOREST
    → RUGGED UPLANDS
    → ALPINE / FROZEN PEAKS.

This is a broad believable Overworld gradient, not literal biome stripes.

W–E = regional axis.
N–S = team axis.

Orientation may reverse where supported.

The recognizer should increasingly distinguish:

  BAD GEOGRAPHY
    → reject.

  GOOD GEOGRAPHY WITH BOUNDED CORRECTABLE OPPORTUNITY DEFICIT
    → author.

Do not make authoring powerful enough to rescue fundamentally bad geography.

======================================================================
12. BUILD A REAL CANDIDATE COMBINATION
======================================================================

Do not evaluate every component only in isolation.

Attempt to select a coherent combination:

  North Homebase socket
  South Homebase socket

  North Outpost
  North Bastion
  North Spike

  South Outpost
  South Bastion
  South Spike

  one Lair

  compact North/South Hinterlands

  initial access assumptions

such that their constraints are jointly satisfiable.

Avoid the trap where:
  "best Homebase pair"
  +
  "best objective pair"
  +
  "best Lair"

cannot coexist spatially.

Use constraint search/optimization appropriate to the existing codebase.

Keep the objective function interpretable.

Record WHY a candidate wins or fails.

======================================================================
13. EMPIRICAL THRESHOLD RULE
======================================================================

This pass is authorized to establish PROVISIONAL ALPHA constants where doing
so is necessary to make the vertical slice run.

Examples:
  - Lair A_L acceptance;
  - socket intervention limits;
  - clearance margins;
  - practical-reach bounds;
  - opening resource concentration bounds.

Rules:

1. Do not choose constants solely from intuition.

2. Measure real generated candidates/sites first.

3. Prefer distributions/sensitivity analysis over one anecdotal map.

4. Mark the resulting value:
     PROVISIONAL_ALPHA
   or equivalent.

5. Keep it configurable where practical.

6. Record:
     sample,
     distribution,
     chosen value,
     consequence of tightening/loosening it.

7. Do NOT turn a provisional Alpha constant into timeless design doctrine.

The goal is to let implementation and generated-world evidence move forward
without pretending early calibration is final balance.

======================================================================
14. AUTHOR A NEW MAP
======================================================================

Once a candidate passes the recognizer, actually author it in a DISPOSABLE
world.

Do not stop at coordinates/JSON.

Use discovered outputs rather than Alpha hard-coded origins wherever this pass
has generalized them.

Attempt to author:

  - North/South Homebase Cores;
  - bounded socket integration;
  - current Outpost forms;
  - current Bastion forms;
  - current End Spikes;
  - singular Lair manifestation/registration;
  - initial access required for the opening;
  - currently supported Worksite portfolio;
  - currently supported renewable manifestations;
  - required manifests/config/runtime bindings.

Do not add large terrain repairs just to save a failing candidate.

If authoring reveals that the recognizer accepted bad terrain:

  FIX THE RECOGNIZER/CERTIFIER,
  reject the candidate,
  and try again.

Do not normalize the terrain until the false positive disappears.

======================================================================
15. PHYSICAL VERIFICATION LOOP
======================================================================

Load the authored disposable world and verify physical reality.

Check at minimum:

HOMEBASE
  - Core exists where intended.
  - Fountain/spawn binding works.
  - terrain integration is bounded.
  - exits/access are usable.

HINTERLAND
  - compact rather than team-half-scale.
  - Wilderness remains around it, including where appropriate behind/poleward.
  - no known ceiling violation has been silently introduced by authoring.

OBJECTIVES
  - correct current physical forms exist.
  - each is physically accessible.
  - ordering metadata is correct.
  - no legacy End Tower/Treasure Bastion artifact is masquerading as current.

LAIR
  - exactly one exists.
  - manifestation is recognizable.
  - encounter volume is physically plausible.
  - approaches exist.
  - runtime Lair binding points to it.

WORLD OPPORTUNITIES
  - Worksite site registration is valid.
  - renewable manifestations remain terrain-conforming.
  - initial access/Routes do not pre-solve deep Wilderness.

If verification fails because analysis was wrong:

  feed the failure back into recognizer/certifier logic.

Iterate where practical.

======================================================================
16. TARGET: ONE UNSEEN-SEED VERTICAL SLICE
======================================================================

The principal success target is:

  Produce at least ONE map from an unseen/non-hard-coded seed/region such that:

  SEED
    → Default recognition
    → Homebase discovery
    → Hinterland validation
    → objective socket discovery
    → Lair discovery
    → candidate selection
    → authoring
    → physical verification
    → PLAYABLE MAP.

"Playable" here means:

  passes the current explicit authored competitive/physical contract.

It does NOT mean:

  empirically proven perfectly balanced through human competitive play.

If no map succeeds:

  do not fake one.

Instead report the deepest stage reached and the measured rejection causes.

The pass is still useful if it transforms an unknown failure into a precise,
measured compiler blocker.

======================================================================
17. HISTORICAL /1 ANALYTICS
======================================================================

Preserve historical schema /1 output as historical evidence.

Do not rewrite old JSON merely to make history agree with current doctrine.

Current /2+ analytics should not emit weaker_team/deficit_kind for generic
natural asymmetry unless an actual competitive consequence is established.

It is valid to say:

  contrast exists;
  competitive consequence unknown.

======================================================================
18. test_author_portfolio FAILURES
======================================================================

Investigate the two existing failures.

Remember:
  they reproduce unchanged at parent 03c5bea.

Do not blindly modify new code until the old tests pass.

For each failure classify:

  A. genuine implementation defect still relevant under current doctrine;
  B. stale assertion tied to balance_asymmetry or another superseded model;
  C. unrelated pre-existing bug.

If B:
  migrate the test to assert the new competitive contract.

If A or C:
  fix where appropriate.

Document the classification.

======================================================================
19. DO NOT OVERREACH INTO OPEN DESIGN
======================================================================

Still unresolved:

  - exact Giant→Outpost siege advantage;
  - exact Ghast→Bastion siege advantage;
  - exact Dragon→Spike siege advantage;
  - boss/toppling XP;
  - final Worksite tier reward packages;
  - final Giant AI/encounter design;
  - final Ghast encounter modifications;
  - final Dragon encounter modifications;
  - final Lair architectural art direction;
  - final competitive thresholds after Alpha testing.

Do not invent these to complete the map compiler.

A vanilla mob behaving badly should be documented accurately rather than
hidden behind an invented combat redesign.

======================================================================
20. DO NOT PRESERVE ALPHA SPECIAL CASES WITHOUT REASON
======================================================================

Search specifically for:
  - hard-coded Alpha origins;
  - Alpha-only Homebase coordinates;
  - fixed objective coordinates;
  - assumed Lair coordinates;
  - fixed N/S repair targets;
  - resource-packing assumptions tied to one map;
  - authoring steps that only work because the Alpha map was manually known.

Generalize them where required for the vertical slice.

Where an Alpha fixture remains useful as a regression specimen, keep it.

Distinguish:

  FIXTURE / REFERENCE MAP

from:

  RUNTIME WORLDGEN ASSUMPTION.

======================================================================
21. MAP COMPILER STATE MODEL
======================================================================

Where compatible with existing architecture, make the pipeline legible as:

  MapCandidate
      ↓
  CompetitiveCandidate
      ↓
  AuthoredCandidate
      ↓
  PlayableMap.

Do not force new classes merely for terminology if existing structures already
represent these states cleanly.

But make failures attributable to stages.

A rejected seed/site should have machine-readable reasons where practical.

Examples:

  NO_DEFAULT_REGIONAL_SHAPE
  NO_HOMEBASE_SOCKET_PAIR
  OPENING_CEILING_VIOLATION
  NO_ORDERED_OBJECTIVE_LAYOUT
  NO_LAIR_VOLUME
  LAIR_ACCESS_DISPARITY
  AUTHORING_COST_EXCEEDED
  PHYSICAL_VERIFICATION_FAILED

Use names appropriate to the existing project conventions.

======================================================================
22. OUTPUT DATA FOR THE NEXT STEP
======================================================================

Even if the vertical slice succeeds, retain useful experimental output.

Report at least:

- seeds/regions screened;
- rejection rate by stage;
- Homebase socket counts;
- objective socket counts;
- Lair candidate counts;
- A_L distributions;
- selected candidate's relevant costs;
- authoring interventions performed;
- physical verification failures/retries;
- elapsed processing cost where measurable.

We need to know not merely whether ONE map can compile, but what will constrain
turning the compiler into a background map foundry.

======================================================================
23. RELATION TO FINAL PRODUCT GOAL
======================================================================

Do NOT implement a fake synchronous:

  /moba match start
      → search thousands of seeds
      → wait
      → author world.

The intended eventual architecture remains:

  MAP FOUNDRY
      ↓
  READY MAP POOL
      ↓
  /moba match start
      ↓
  claim unused READY map
      ↓
  IN_USE
      ↓
  USED.

This pass proves/generalizes the COMPILER.

A subsequent pass can industrialize it into the foundry/ready-pool workflow.

If existing architecture makes safe progress toward the pool cheap, that is
welcome, but do not sacrifice the vertical slice to build queue machinery
before one unseen map can actually compile.

======================================================================
24. TESTING
======================================================================

Run all relevant:

  - Java/plugin tests;
  - worldgen tests;
  - objective-form tests;
  - optimizer tests;
  - authoring tests;
  - physical verification tests.

Add focused tests for newly generalized behavior.

Tests should cover properties, not one magic Alpha coordinate.

Examples:

  - objective ordering survives lateral displacement;
  - Wilderness asymmetry alone does not create a loser;
  - Lair search evaluates both team reaches;
  - exactly one Lair is selected;
  - Hinterland does not implicitly extend to map pole;
  - objective certifier rejects legacy/noncompliant geometry;
  - author consumes discovered sockets rather than fixed origins.

Where world generation makes exact fixtures expensive, use the smallest
deterministic fixture that still tests the actual contract.

======================================================================
25. COMMIT / REPORT
======================================================================

Commit and push the coherent pass.

Produce/update an audit document describing:

1. PHYSICAL OBJECTIVE FORMS
   - what was derived/built;
   - measured contracts;
   - remaining uncertainty.

2. LAIR
   - first physical manifestation;
   - functional socket contract;
   - lifecycle smoke-test results;
   - vanilla mob constraints found.

3. OPTIMIZER MIGRATION
   - what balance_asymmetry used to do;
   - what replaced it;
   - which measurements were retained.

4. RECOGNIZER
   - current candidate pipeline;
   - constraints;
   - provisional thresholds and empirical basis.

5. VERTICAL SLICE
   - unseen seed used;
   - stages passed;
   - authored output;
   - physical verification result.

6. FAILURES
   - rejection/failure distribution;
   - false positives found and corrected;
   - remaining blockers.

7. TESTS
   - exact results;
   - treatment of the two pre-existing portfolio failures.

8. NEXT STEP
   - what remains between the current compiler and:
       MAP FOUNDRY
       → READY MAP POOL
       → /moba match start.

Final response should include:

  - commit hash;
  - branch;
  - audit path;
  - tests;
  - whether an unseen-seed PlayableMap was actually produced;
  - if yes, its seed/candidate/provenance and verification result;
  - if no, deepest completed compiler stage and exact blocker;
  - provisional constants established and their evidence;
  - remaining hard-coded Alpha assumptions;
  - recommended next pass.

======================================================================
26. OPERATING PRINCIPLE
======================================================================

Be aggressive about ENGINEERING.

Do not stop for human confirmation merely because:
  - a dimension needs measuring;
  - a distribution needs sampling;
  - an old test is wrong;
  - a legacy structure must be replaced;
  - a provisional Alpha threshold is needed;
  - a disposable world needs authoring;
  - a false-positive recognizer rule needs iteration.

Stop/invent nothing only when the missing information is genuinely a DESIGN
choice rather than an engineering/measurement question.

The objective of this pass is no longer to describe the map compiler.

BUILD THE FIRST GENERALIZED VERTICAL SLICE OF IT.