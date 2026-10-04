NEXT CLAUDE PASS
VERIFIED MAP → READY INVENTORY → PRE-MATCH → GENERATED-MAP ACTIVE PLAY

Repository:
  iracarranza/minecraftmoba

Branch:
  codex/spatial-cadence-migration

Current head:
  0c739b3

Primary audit:
  docs/audit/2026-09-23-map-compiler-slice.md

Current design decisions:
  Locate the repo copy of the manually added 2026-09-23 document whose
  content/title is:

    "Map, Objective, and Lair Systems — Six Design Decisions"

  The local/source version was also known as:
    92326objupdate.md

Read that document in full before changing:
  - map lifecycle;
  - READY semantics;
  - pre-match behavior;
  - objective toppling;
  - Lair monster rewards/effects;
  - Lair encounters;
  - Opening Hinterland rules.

Treat its six decisions as CURRENT doctrine, not as speculative notes.

======================================================================
MISSION
======================================================================

The map compiler now works.

At current head:

- thirty previously unseen seeds were screened;
- two independently compiled all the way through physical verification;
- both were published to a READY pool;
- the live plugin successfully claimed them distinctly;
- pool exhaustion worked;
- retirement to USED worked;
- Java: 239 passing;
- worldgen: 388 passing.

The compiler has therefore crossed the proof-of-concept threshold.

The principal remaining defect is that:

  a map can satisfy the current physical PlayableMap/VerifiedMap contract
  and enter the pool while its Lair has never actually been manifested or
  bound.

Current generated manifests still have:

  alpha.lair.site: []

and the actual match cadence would therefore reach:

  UNCONFIGURED

on a map the pool had called READY.

Fix that architectural inconsistency and push through the next vertical slice:

  VERIFIED GENERATED WORLD
      ↓
  complete runtime-required authoring/binding
      ↓
  READY inventory
      ↓
  /moba match start
      ↓
  pre-match lifecycle
      ↓
  select/claim compatible READY realization
      ↓
  ACTIVE PLAY
      ↓
  generated-map cadence/objectives/Lair operate
      ↓
  match ends
      ↓
  map retires USED.

TARGET:

Run a disposable-server match lifecycle on an automatically generated,
previously unused map and demonstrate that the generated map can progress
through the real match systems without falling back to Alpha coordinates or
reporting a required system UNCONFIGURED.

This is an implementation pass.

Do not stop at another design/audit-only pass.

======================================================================
1. RECONCILE CURRENT HEAD FIRST
======================================================================

Read:

  docs/audit/2026-09-23-map-compiler-slice.md

Inspect current implementation at 0c739b3.

Verify rather than assume:

- two unseen seeds reached physical verification;
- End Spikes are now authored;
- Outposts use the actual vanilla watchtower NBT;
- Bastions use the current vanilla-NBT-derived retained form;
- objective contracts derive from authored templates;
- column/block readback verification runs;
- footprint overlap is prevented;
- tree clearing is treated as bounded authoring cost;
- Lair sockets are actually searched;
- A_L is actually computed;
- optimizer uses gates + optimization rather than balance_asymmetry;
- foundry publishes provenance;
- MapPool claims atomically;
- READY → IN_USE → USED mechanics exist;
- the cadence migration from 14cb46c remains intact.

The audit currently contains stale statements in its failure section left from
the preceding iteration, including claims that:

- the End Spike mesh remains absent;
- physical authoring/verification has not run end to end.

Those statements conflict with the current §5 result.

Correct the audit so CURRENT STATE is internally consistent while preserving
historical blockers as historical findings where useful.

======================================================================
2. READ AND APPLY THE SIX DESIGN DECISIONS
======================================================================

Read the current repo copy of:

  "Map, Objective, and Lair Systems — Six Design Decisions"

Do not infer its contents from this prompt alone.

It establishes current doctrine in six areas:

1. post-compiler map lifecycle / foundry / draft;
2. meaning of compiler certification;
3. defensive-objective toppling;
4. Lair monster → defensive-objective interaction;
5. Lair encounter design;
6. Opening Hinterland contents.

Reconcile implementation against the actual document.

Create a working matrix:

  DECISION
  CURRENT IMPLEMENTATION
  GAP
  ACTION THIS PASS
  REMAINS OPEN

Do not silently resolve details the document itself leaves open.

In particular:

- preserve its distinction between physical/systemic certification and
  empirical competitive validation;
- preserve its terminology where explicit;
- do not replace its lifecycle with a simpler one merely because that is
  easier to test.

======================================================================
3. PRESERVE THE VERIFIEDMAP / READY DISTINCTION
======================================================================

Do NOT invent a new MatchReadyMap concept unless the code architecture
absolutely requires one.

Current conceptual lifecycle remains:

  MapCandidate
      ↓
  CompetitiveCandidate
      ↓
  AuthoredCandidate
      ↓
  VerifiedMap
      ↓
  READY

But enforce the meaning correctly.

A VerifiedMap is a compiler result.

READY is an inventory/lifecycle state meaning that the verified realization is
eligible to be claimed for a real match.

Therefore:

  VERIFIED ≠ automatically READY.

A map MUST NOT transition into READY if any required runtime binding is absent.

Examples of READY-blocking defects:

- no Lair runtime site;
- missing Homebase/Fountain binding;
- missing objective binding;
- missing required Worksite portfolio/config;
- incompatible manifest schema;
- authored entity/objective seam required by runtime but absent;
- world artifact missing/corrupt;
- physical verification no longer matches manifest.

Add an explicit readiness certification/check before publication.

Test:

  physically verified + unconfigured Lair
      → NOT READY.

The current state where alpha.lair.site can be empty on a published READY map
must become impossible.

======================================================================
4. MANIFEST THE GIANT MONSTER LAIR
======================================================================

The Lair is now the clearest physical/runtime gap.

Current compiler already:

- searches Lair sockets;
- evaluates functional terrain;
- measures N/S Practical Reach;
- computes A_L;
- selects exactly one socket.

But it does NOT actually establish the Lair there.

Implement the minimum current Lair manifestation required by doctrine.

Preserve:

- exactly one permanent Lair per map;
- permanent location throughout the match;
- same location successively hosts Giant, Ghast, Dragon;
- competitively central rather than necessarily geometrically central;
- surrounding natural 3D terrain is the encounter space;
- players may generally build/destroy there;
- the site can accumulate match history;
- do not flatten a giant arena;
- do not excavate a giant arena;
- do not force circular geometry;
- do not force biome neutrality;
- do not invent final architectural art direction.

The physical manifestation should primarily provide:

  RECOGNIZABILITY
  LOCATION IDENTITY
  RUNTIME ANCHORING
  SPAWN/ENCOUNTER REGISTRATION
  PERSISTENCE.

Use the Six Design Decisions document for any more specific current Lair
requirements.

Where architectural/art treatment remains unspecified, keep it minimal and
explicitly provisional.

Do not turn provisional visual treatment into doctrine.

The generated map manifest must contain the selected Lair's real world
identity/location/configuration.

No Match-eligible map may retain an empty alpha.lair.site equivalent.

======================================================================
5. PHYSICALLY VERIFY THE LAIR
======================================================================

The old recognizer used an eight-block surface grid and therefore approximated
some 3D facts.

Now use generated-world truth where practical.

Verify at least:

- exactly one Lair manifestation exists;
- manifest/runtime location matches physical location;
- usable ground encounter area exists;
- meaningful surrounding air volume exists;
- multiple practical approaches remain;
- Lair does not overlap either compact Hinterland;
- Lair does not overlap team objectives;
- Lair does not overlap Homebase Cores;
- authoring did not destroy the terrain characteristics that caused the socket
  to qualify;
- spawn anchor is physically valid;
- Giant can spawn there;
- Ghast can spawn there;
- Dragon can spawn there.

Do not claim that these checks prove encounter quality.

They prove physical/runtime feasibility.

Competitive encounter quality remains empirical.

======================================================================
6. COMPLETE OBJECTIVE PHYSICAL FORMS
======================================================================

Current intended forms remain:

OUTPOST
  Pillager Outpost watchtower only.

BASTION
  Bridge Bastion retained body/rampart form,
  WITHOUT the projecting bridge.

END SPIKE
  vanilla-style obsidian spike
  + bedrock cap
  + cage where applicable
  + End Crystal.

The previous pass correctly replaced greyboxes with Minecraft-derived geometry.

Finish remaining fidelity seams.

----------------------------------------------------------------------
6A. BASTION ASSEMBLY
----------------------------------------------------------------------

The audit records that the current authored Bastion does not reproduce the full
intended rampart assembly because vanilla assembles it through jigsaw pieces.

Investigate the actual vanilla jigsaw data.

Determine which pieces comprise the intended:

  Bridge Bastion rampart/body

and which comprise the excluded:

  projecting bridge.

If this is an engineering reconstruction problem, solve it.

Do not preserve an incomplete Bastion merely because placing one NBT is easier.

Do not restore the projecting bridge.

The same assembled representation must drive:

  socket contract
  authoring
  verification.

Avoid reintroducing separate hand-maintained dimensions.

----------------------------------------------------------------------
6B. END CRYSTAL
----------------------------------------------------------------------

The Spike block geometry is reproduced from measured vanilla generation, but
the End Crystal is an entity and was previously left as an explicit seam.

Close it.

An authored End Spike objective should include its intended End Crystal.

Verify:

- entity exists;
- entity is bound to the correct objective;
- caged variant physically encloses it correctly;
- destroying it can enter the current objective-toppling system as specified by
  the Six Design Decisions document.

Do not invent unresolved objective semantics.

======================================================================
7. COLUMN/BLOCK-LEVEL OBJECTIVE CERTIFICATION
======================================================================

Close the facts the coarse surface grid could not prove.

In particular:

END SPIKE
  - required vertical clearance;
  - obstruction above intended height;
  - physical pillar/cage/crystal correctness.

BASTION
  - vertical/interior fit;
  - intended traversable structure not buried/invalid;
  - actual assembled footprint/contact.

ALL OBJECTIVES
  - no authored-span overlap;
  - no destructive overwrite;
  - no standing-structure collision;
  - no accidental Hinterland placement;
  - correct team association;
  - correct spatial ordering.

Preserve:

  MIDLINE
      → OUTPOST
      → BASTION
      → END SPIKE
      → FOUNTAIN

for each team in the appropriate direction.

This remains geographic ordering, NOT prerequisite locking.

======================================================================
8. IMPLEMENT CURRENT DEFENSIVE CAPACITY MODEL
======================================================================

The Six Design Decisions document resolves the broad toppling model.

Implement what it actually specifies.

The three defensive objectives remain simultaneously present.

Each objective has finite DEFENSIVE CAPACITY.

The three Minecraft-native approaches:

  COMBAT SIEGE
  STRUCTURAL SIEGE
  SIGNATURE SIEGE

must interact with that same underlying objective state rather than being
three unrelated minigames or arbitrary completion buttons.

Preserve the intended signature verbs:

OUTPOST
  steal/free the Allay as specified by current design.

BASTION
  extract/mine its Gold as specified by current design.

END SPIKE
  destroy the End Crystal.

Combat siege:
  defeat the objective's defenders.

Structural siege:
  materially destroy the objective structure.

Implement the shared-capacity architecture and the portions of each route the
document resolves.

Where exact numbers/rates/thresholds remain unspecified:

  DO NOT INVENT FINAL BALANCE VALUES.

If implementation requires provisional Alpha values to exercise the system:

  - derive them from the actual physical structures where possible;
  - label them PROVISIONAL_ALPHA;
  - make them configurable;
  - document sensitivity;
  - do not canonize them as design doctrine.

Critically:

different methods should be capable of contributing to the same toppling
process.

Do not require players to commit exclusively to one route if the document does
not say so.

======================================================================
9. IMPLEMENT CURRENT LAIR → OBJECTIVE ASSAULT MODEL
======================================================================

The old concept of an abstract "siege advantage" is superseded by the Six
Design Decisions document.

Use the actual document wording/details.

Current high-level correspondence remains:

  Giant
      → enemy Outpost

  Ghast
      → enemy Bastion

  Ender Dragon
      → enemy End Spike.

But defeating a Lair monster should lead to the current PHYSICAL ASSAULT model,
not merely:

  +20% objective damage
  objective debuff
  invisible team buff
  arbitrary direct HP subtraction

unless the document explicitly requires some such component.

Implement the resolved physical/systemic behavior from the document.

The effect should participate in the same Defensive Capacity/toppling system,
not create a second unrelated objective-health model.

Ensure the effect can resolve against objectives whose positions came from the
generated map manifest.

No Alpha coordinates.

======================================================================
10. LAIR ENCOUNTERS
======================================================================

Apply the Six Design Decisions document's encounter doctrine.

The goal is Minecraft-native contested PvPvE, not bespoke MMO boss fights.

Preserve ordinary Minecraft systemic verbs where possible:

- movement;
- terrain;
- building;
- destruction;
- cover;
- ranged attacks;
- melee;
- mob interaction;
- preparation;
- retreat;
- enemy-player interference.

Implement only the minimum behavioral extensions the document actually calls
for.

GIANT:
  The vanilla Giant's lack of useful designed AI is a known real deficiency.
  Implement the minimum current behavior required by the design document.

GHAST:
  Preserve recognizable Ghast behavior unless the document specifies a minimal
  correction necessary for the Lair encounter.

DRAGON:
  Preserve recognizable Dragon behavior and account for the fact that the
  encounter occurs outside the End.

Do not turn all three into custom scripted raid bosses.

The point is that:
  Giant encounter
  Ghast encounter
  Dragon encounter

should feel different because they are different Minecraft entities interacting
with the same persistent terrain volume.

======================================================================
11. GENERATED-MAP RUNTIME BINDINGS
======================================================================

Audit runtime code for assumptions that the map is Alpha or manually known.

For a generated VerifiedMap eligible for READY, derive runtime bindings from
its manifest/provenance.

At minimum:

WORLD
  - world identity;
  - map type;
  - orientation;
  - compiler/authoring version.

NORTH
  - Homebase Core;
  - Aether Fountain;
  - spawn/reconstruction;
  - Outpost;
  - Bastion;
  - End Spike.

SOUTH
  - Homebase Core;
  - Aether Fountain;
  - spawn/reconstruction;
  - Outpost;
  - Bastion;
  - End Spike.

SHARED
  - singular Lair;
  - Worksite portfolio/sites;
  - regenerative source registration;
  - initial authored access/Routes where runtime needs them.

Search for:

  alpha.*
  fixed coordinates
  end_tower
  Treasure Bastion
  hard-coded Fountain coordinates
  fixed Lair assumptions
  fixed Worksite origins
  default-world assumptions
  fixture-specific objective ids.

Classify each occurrence:

  REFERENCE FIXTURE
  HISTORICAL
  CURRENT RUNTIME BUG
  STILL-VALID GENERIC DEFAULT.

Generalize runtime bugs.

Do not destroy useful Alpha fixtures.

======================================================================
12. READY INVENTORY
======================================================================

The foundry already exists.

Strengthen it into a producer of ACTUALLY CLAIMABLE match realizations.

Pipeline:

  seed
      ↓
  cheap screening where available
      ↓
  recognize
      ↓
  competitive candidate
      ↓
  author
      ↓
  verify
      ↓
  readiness certification
      ↓
  READY inventory.

A READY entry must carry enough provenance/configuration to instantiate a match
without rediscovering the map.

Preserve at least:

- seed;
- map type;
- candidate identity;
- compiler version;
- authoring version;
- provisional constants;
- world fingerprint;
- physical verification evidence;
- runtime binding manifest;
- known reproduction/fidelity caveats;
- READY/IN_USE/USED state.

Keep atomic claiming.

Keep concurrency tests.

======================================================================
13. BEGIN FOUNDRY INDUSTRIALIZATION
======================================================================

Current measured yield is:

  2 / 30 unseen seeds
  = 6.7%.

The dominant rejection is:

  NO_DEFAULT_REGIONAL_SHAPE
  24 / 30.

This strongly suggests a cheap pre-generation screen could save substantial
machine time.

Investigate whether Default regional-shape screening can be moved earlier using
cheap biome/climate/worldgen information WITHOUT generating the expensive full
candidate world.

If defensible, implement it.

Measure:

  seeds screened cheaply;
  seeds sent to expensive generation;
  false-negative risk if measurable;
  false-positive rate;
  machine time saved.

Do not weaken geographic doctrine merely to increase yield.

Also provide a foundry job mode capable of:

  target READY depth = N

and continuing until:
  - N READY maps exist;
  - an explicit resource/time/attempt bound is reached;
  - or a real failure occurs.

Do not generate synchronously during match start.

======================================================================
14. MAP TYPE / SCALE / RESOURCE-DENSITY IDENTITY
======================================================================

The Six Design Decisions document places map choice inside a broader pre-match
identity that may include:

  MAP TYPE
  SCALE
  discovered RESOURCE DENSITY / related map characteristics.

Do not invent a complete map-draft metagame if those option-generation rules
remain open.

But ensure the READY inventory model can expose the properties required for a
future draft.

A READY map should be queryable by the relevant authored/discovered
characteristics rather than being an opaque world id.

If only Default exists today, that is fine.

Do not fabricate additional Map Types merely to exercise the API.

======================================================================
15. /moba match start IS A PRE-MATCH ENTRY POINT
======================================================================

Do NOT implement:

  /moba match start
      → immediately claim arbitrary READY map
      → immediately teleport everyone.

The Six Design Decisions document establishes a PRE-MATCH lifecycle.

Conceptually:

  /moba match start
      ↓
  MATCH CREATED
      ↓
  participants/teams locked
      ↓
  PRE-MATCH
      ↓
  class/map decision process
      ↓
  final compatible map option
      ↓
  claim corresponding READY realization
      ↓
  bind generated map
      ↓
  send players to Fountains
      ↓
  ACTIVE PLAY.

Implement the lifecycle architecture needed for this.

Do NOT invent unresolved ordering such as:
  - exact ban order;
  - exact counterpick order;
  - whether class or map selection occurs first;
  - exact number of map options;
  - exact UI presentation

unless the document explicitly resolves them.

Instead provide explicit seams/state transitions for the unresolved draft
details.

======================================================================
16. PROVIDE A TEST/DEBUG PRE-MATCH RESOLUTION PATH
======================================================================

We still need an end-to-end integration test before the final draft rules exist.

Therefore provide a TEST/DEBUG mechanism that can resolve the pre-match map
choice deterministically.

For example, conceptually:

  create match
      ↓
  enter PRE_MATCH
      ↓
  test harness selects a known compatible READY option
      ↓
  claim it
      ↓
  proceed to ACTIVE PLAY.

This must be clearly a test/debug path.

Do not silently make "first READY map wins" the production draft rule.

======================================================================
17. RUN A REAL GENERATED-MAP MATCH LIFECYCLE
======================================================================

On a disposable server:

1. Ensure foundry/fixtures provide at least one genuinely generated READY map.

2. Invoke the real match creation/start path.

3. Enter PRE_MATCH.

4. Resolve map selection through the explicit test/debug mechanism.

5. Atomically claim the selected READY realization.

6. Bind its actual generated world.

7. Verify both teams resolve to their generated:
     Homebases
     Fountains
     spawn/reconstruction positions.

8. Verify all six team objectives coexist and register.

9. Verify the singular generated Lair registers.

10. Verify Worksite sites/config register.

11. Transition to ACTIVE PLAY.

Then drive the actual MatchClock/advance path:

NIGHT 1
  Worksite I.

NIGHT 2
  Giant at generated Lair.

NIGHT 3
  Worksite II.
  Surviving Giant persists.

NIGHT 4
  Ghast replaces Giant.

NIGHT 5
  Worksite III.

NIGHT 6
  Dragon replaces Ghast.

At NO point should a required generated-map system report:

  UNCONFIGURED.

Do not call the integration successful merely because the map was claimed.

======================================================================
18. EXERCISE DEFENSIVE OBJECTIVES ON THE GENERATED MAP
======================================================================

During the disposable integration run, exercise enough of the new Defensive
Capacity architecture to prove that generated objective bindings are real.

At minimum, for one or more objectives:

- combat-siege contribution reaches the correct generated objective;
- structural destruction is recognized against the correct generated
  structure;
- signature interaction is recognized against the correct generated objective;
- mixed contribution affects one shared Defensive Capacity state;
- toppling transition occurs when the current test threshold is reached.

Do not require a polished human encounter.

This is an integration proof.

Verify that no path references historical Alpha objective coordinates.

======================================================================
19. EXERCISE ONE LAIR → OBJECTIVE ASSAULT
======================================================================

If the Six Design Decisions document resolves enough detail to implement it,
exercise at least one complete chain:

  defeat Lair monster
      ↓
  determine paired enemy objective
      ↓
  physical assault manifests
      ↓
  assault affects that generated objective
      ↓
  Defensive Capacity/toppling system recognizes it.

Prefer Giant → Outpost for the first integration proof if that is the simplest
resolved case.

Then test the generic mapping for:
  Ghast → Bastion
  Dragon → End Spike

as far as current implementation permits.

Do not invent missing final combat balance merely to produce spectacle.

======================================================================
20. MATCH END AND MAP RETIREMENT
======================================================================

Complete the generated-map lifecycle.

On normal match termination:

  IN_USE
      ↓
  USED.

Verify:

- the map cannot be claimed again;
- its provenance remains available;
- retirement does not leak inventory;
- claim artifacts do not make USED appear IN_USE;
- pool exhaustion behaves correctly.

Inspect restart/crash semantics.

At minimum, ensure state is diagnosable.

If a map can become stranded permanently after a crash, either fix the simple
case or document the exact recovery seam.

Do not build a huge distributed recovery system unless actually necessary.

======================================================================
21. OPENING HINTERLAND — APPLY THE SIXTH DECISION
======================================================================

Reconcile the compiler's current opening checks against the Six Design
Decisions document.

Preserve the core principle:

  the Hinterland must permit fundamental verbs
  without resolving them.

Preserve explicit known exclusions unless the document changes them:

  VILLAGES
  CARROTS
  EQUIPMENT-SUFFICIENT ACCESSIBLE IRON.

Do not turn these three examples into the entire opening ceiling.

Use the document's strategic-consequence framing.

Audit whether current compiler checks actually represent:

- basic Construction opportunity;
- basic Extraction opportunity;
- basic Development/renewable opportunity;
- enough Production input for useful opening play without equipment shortcut;
- Exploration opportunity;
- Logistics relationships between useful locations;
- ordinary Combat/PvE opportunity.

Do not invent arbitrary quotas simply to make every verb a numeric gate.

Where a useful measurement can be derived from generated worlds, report it.

======================================================================
22. DO NOT CALL PHYSICAL CERTIFICATION "BALANCE"
======================================================================

The Six Design Decisions document explicitly separates compiler certification
from empirical competitive validation.

Preserve that distinction everywhere.

The compiler can establish things such as:

- required structures exist;
- sockets satisfy authored constraints;
- both teams can reach required shared opportunities;
- opening floor/ceiling constraints hold;
- objective ordering holds;
- Lair access parity falls inside the provisional bound;
- authoring stayed bounded;
- runtime systems can bind.

It CANNOT establish from those facts alone that:

  "this map is competitively balanced."

Use terminology such as:

  VerifiedMap
  verified realization
  competitively admissible under current Alpha constraints

where appropriate.

Do not introduce:
  BalancedMap
or equivalent claims.

All current thresholds remain PROVISIONAL_ALPHA until play produces evidence.

======================================================================
23. PREPARE FOR EMPIRICAL PLAYTESTING
======================================================================

Nothing has yet been competitively played.

Now that generated-map ACTIVE PLAY should become possible, add lightweight
instrumentation that will make the first real playtests useful.

Do not build a giant analytics platform.

Capture high-value facts already implied by the systems, such as:

- map id / seed / realization;
- selected Map Type / Scale / exposed draft properties;
- Homebase sockets;
- objective sites;
- Lair A_L;
- initial travel times;
- first arrival to Worksites/Lair/objectives where available;
- objective Defensive Capacity changes by source:
    combat
    structural
    signature
    Lair assault;
- objective topple times;
- Lair contest/kill times;
- match duration;
- winner;
- major runtime errors/UNCONFIGURED events.

The purpose is to compare:
  compiler predictions
against:
  actual play.

Do not automatically retune thresholds from one match.

======================================================================
24. REVISIT A_L WITH THE EXPANDED SAMPLE
======================================================================

Current:

  max_lair_access_asymmetry = 0.15
  PROVISIONAL_ALPHA.

It was originally chosen from a small eight-finalist distribution.

There are now thirty additional unseen-seed trials.

Recompute/report the evidence available at current head.

Do not change 0.15 merely because more data exists.

Change it only if the expanded evidence materially undermines the current
choice.

If changed:

- show old/new distribution;
- show candidate-yield consequence;
- keep it PROVISIONAL_ALPHA.

Remember:
  A_L is one of the few intentional N/S parity metrics.

Do not generalize its parity requirement to wilderness geography.

======================================================================
25. PRESERVE NATURAL ASYMMETRY
======================================================================

Do not regress while integrating runtime systems.

Still true:

  terrain inequality != strategic inequality.

Homebase Core:
  controlled/symmetric.

Compact Hinterland:
  constrained opening economy.

Wilderness:
  natural/asymmetric competitive space.

Objective ordering:
  designed.

Geography between objectives:
  discovered.

Lair:
  singular shared exception where initial team access parity matters.

Do not reintroduce:
  weaker_team
  generic deficit_kind
  balance_asymmetry optimization
  mirrored wilderness
  equal objective intervals
  N/S biome equality
  equal resource counts.

======================================================================
26. FOUNDRY PERFORMANCE
======================================================================

Measure current cost rather than guessing.

The previous audit estimated roughly:
  6.7% final yield
  ~15 generated worlds per READY map
  ~2 minutes/world
  ~30 machine-minutes/map.

After cheap screening / new readiness requirements, report:

- cheap-screen throughput;
- expensive-generation count;
- compile yield;
- physical-verification yield;
- readiness yield;
- average/median time per READY map;
- dominant rejection codes.

Do not optimize tiny stages while 80% of expensive work is still being thrown
away at the regional-shape gate.

======================================================================
27. TESTS
======================================================================

Run the full relevant suite.

Preserve current baseline:
  Java 239 / 0
  worldgen 388 / 0

and add tests for the new contracts.

At minimum test:

READY:
  - physically verified but Lair-unconfigured map cannot publish READY;
  - fully configured map can;
  - claim remains atomic;
  - USED cannot be reclaimed.

LAIR:
  - exactly one manifestation;
  - manifest binding exists;
  - all three occupants use same generated site;
  - mobile boss cleanup remains correct.

OBJECTIVES:
  - real Bastion assembled form;
  - End Crystal exists/binds;
  - generated objective ids/locations used;
  - Defensive Capacity shared across siege methods;
  - toppling transition.

LIFECYCLE:
  - /moba match start enters PRE_MATCH;
  - production path does not silently auto-pick a map;
  - test/debug path can resolve a READY choice;
  - selected realization becomes IN_USE;
  - ACTIVE PLAY uses generated bindings;
  - match end retires USED.

CADENCE:
  - Worksite I;
  - Giant;
  - Worksite II;
  - Ghast;
  - Worksite III;
  - Dragon;
  on a generated map without UNCONFIGURED.

COMPILER:
  - existing two unseen successful seeds remain regression specimens where
    appropriate;
  - do not hard-code logic specifically for those seeds.

======================================================================
28. LIVE/DISPOSABLE SERVER VERIFICATION
======================================================================

Do not consider unit tests sufficient.

Run a disposable server integration.

Required proof:

  generated map
      ↓
  VerifiedMap
      ↓
  readiness certification
      ↓
  READY
      ↓
  match creation
      ↓
  PRE_MATCH
      ↓
  test selection
      ↓
  claim
      ↓
  IN_USE
      ↓
  generated world bound
      ↓
  ACTIVE PLAY
      ↓
  Worksite/Lair cadence executes
      ↓
  objective system recognizes generated structures
      ↓
  match terminates
      ↓
  USED.

Record logs/evidence sufficient to distinguish an actual end-to-end run from a
mocked lifecycle.

======================================================================
29. FAILURE POLICY
======================================================================

Be aggressive about engineering.

If running the system exposes:

- stale fixture;
- false-positive recognizer;
- incorrect manifest assumption;
- structure drift;
- missing runtime binding;
- concurrency defect;
- world-load bug;
- author/verifier mismatch;
- cheap-screen inefficiency;
- lifecycle transition bug;

fix it generally and rerun.

Do not stop merely because implementation is difficult.

Do NOT invent when the missing piece is a genuine unresolved design decision.

If a design decision blocks execution:

1. identify the exact unresolved choice;
2. preserve a clear seam;
3. use a test-only mechanism if needed to exercise downstream architecture;
4. continue every independent part of the pass.

======================================================================
30. SUCCESS LEVELS
======================================================================

Report the deepest level genuinely achieved.

LEVEL A — COMPILER
  unseen map physically verifies.

Already achieved at current head.

LEVEL B — READY
  generated map passes strengthened runtime-readiness certification and enters
  READY with Lair/objectives/etc. actually configured.

LEVEL C — MATCH BIND
  pre-match selects and claims that generated realization and all systems bind.

LEVEL D — ACTIVE RUNTIME
  generated map reaches ACTIVE PLAY and survives all six cadence nights without
  required systems becoming UNCONFIGURED.

LEVEL E — SYSTEM INTEGRATION
  generated objective Defensive Capacity/toppling and at least one Lair assault
  operate against generated-map structures.

LEVEL F — COMPLETE LIFECYCLE
  match ends and generated realization transitions correctly to USED.

Target:
  LEVEL F.

If Level F cannot be reached, do not fake it.

Report:
  deepest level;
  exact blocker;
  evidence;
  whether blocker is engineering or unresolved design.

======================================================================
31. AUDIT OUTPUT
======================================================================

Update/create an audit for this pass.

Include:

1. CURRENT PIPELINE
   - actual lifecycle after this pass.

2. SIX DECISIONS RECONCILIATION
   - each decision;
   - implementation state;
   - remaining open seams.

3. LAIR
   - physical manifestation;
   - verification;
   - runtime binding;
   - encounter behavior.

4. OBJECTIVES
   - final authored forms;
   - Bastion reconstruction;
   - End Crystal;
   - Defensive Capacity;
   - toppling methods.

5. LAIR ASSAULT
   - implemented physical assault behavior;
   - generated-map binding;
   - unresolved balance values.

6. READY CONTRACT
   - what now prevents an invalid map entering READY.

7. PRE-MATCH
   - implemented lifecycle;
   - unresolved draft rules;
   - test/debug selection path.

8. LIVE MATCH TEST
   - exact generated map;
   - provenance;
   - state transitions;
   - cadence;
   - objective/Lair evidence;
   - termination/retirement.

9. FOUNDRY
   - yield;
   - cheap screening;
   - throughput;
   - inventory behavior.

10. EMPIRICAL INSTRUMENTATION
    - what first human playtest will record.

11. TESTS
    - exact Java/worldgen/integration results.

12. OPEN DESIGN
    - only genuinely unresolved choices.

13. NEXT STEP
    - what remains between this state and:
        "every real match receives a unique generated verified map through the
         intended draft/foundry lifecycle."

======================================================================
32. COMMIT / PUSH
======================================================================

Commit and push the coherent pass.

Final report must include:

- branch;
- commit hash;
- audit path;
- tests;
- live/disposable-server result;
- deepest SUCCESS LEVEL reached (A–F);
- generated seed/map used;
- whether Lair is now physically manifested;
- whether all required runtime bindings exist;
- whether generated map entered READY;
- whether /moba match start entered PRE_MATCH;
- whether generated map reached ACTIVE PLAY;
- whether all six cadence nights executed;
- whether generated objective toppling was exercised;
- whether a Lair→objective assault was exercised;
- whether map retired to USED;
- foundry yield/performance;
- remaining PROVISIONAL_ALPHA constants;
- remaining Alpha-only runtime assumptions;
- remaining genuine design questions.

======================================================================
OPERATING PRINCIPLE
======================================================================

The last pass proved:

  THE COMPILER CAN MAKE MAPS.

This pass should prove:

  THE GAME CAN ACTUALLY USE THEM.

Do not spend the pass polishing the compiler while a READY map can still reach
the real match runtime with its Lair UNCONFIGURED.

Close the generated-world → real-match loop first.