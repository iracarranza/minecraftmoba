CODEX SPEC — SPATIAL DOCTRINE RECONCILIATION + OBJECTIVE/LAIR RECOVERY + TEMPORAL MODEL MIGRATION

Repository:
  iracarranza/minecraftmoba

Context:
  This is a substantial reconciliation and implementation pass following the
  spatial-doctrine audit. The previous audit intentionally left executable
  logic/configuration unchanged. That restriction NO LONGER applies.

  Recent design work has exposed two different problems:

  1. Several important older gameplay systems — especially the Giant/Ghast/
     Ender Dragon major-neutral-objective model — were omitted from recent
     map/world-flow reasoning and may be absent or stale in current canonical
     documentation.

  2. More importantly, the CURRENT temporal phase model and the plugin/runtime
     architecture implementing it have now been substantially superseded by a
     new match cadence.

  This is therefore NOT merely a documentation update and NOT merely an
  objectives.html restoration.

  Treat old documents, commits, tests, configs, fixtures, and implementation
  as evidence of prior intent. Recover useful information from them, but do
  not blindly restore obsolete doctrine.

Primary goals:
  A. Reconcile and canonize the current spatial model.
  B. Recover the still-relevant objective/Lair concepts.
  C. Replace the obsolete temporal phase model in doctrine AND runtime with
     the new Worksite/Lair cadence where sufficiently specified.
  D. Audit and update downstream code/config/tests/fixtures accordingly.
  E. Preserve explicit seams for mechanics that remain genuinely unresolved.

======================================================================
0. WORKING METHOD
======================================================================

Before editing:

1. Inspect the current canonical docs, HANDOFF, maps/worldgen docs, objective
   docs, Worksite/nighttime docs, progression docs, relevant configs, plugin
   implementation, tests, fixtures, and recent spatial-doctrine audit.

2. Inspect git history where necessary to distinguish:
     - currently canonical behavior;
     - stale implementation assumptions;
     - useful older concepts accidentally omitted;
     - genuinely superseded concepts.

3. In particular, trace the CURRENT temporal progression implementation from
   its runtime source(s) of truth outward.

   Do not merely grep for the word "phase".

   Determine what actually controls:
     - match temporal progression;
     - Worksite availability/spawning;
     - world opportunity activation;
     - objectives;
     - mobs/encounters;
     - configs/manifests;
     - commands/UI;
     - persistence/recovery;
     - generated fixtures;
     - tests;
     - any other systems whose behavior changes over match time.

4. Produce/maintain a supersession matrix:

     OLD DOCTRINE / IMPLEMENTATION
       → STATUS
       → CURRENT REPLACEMENT
       → AFFECTED DOCS
       → AFFECTED CODE/CONFIG/TESTS

5. Do not preserve a stale abstraction merely because significant code already
   depends on it.

6. Conversely, do not delete unrelated uses of "phase" merely because the
   top-level temporal phase model is obsolete. Distinguish semantic uses.

======================================================================
1. GLOBAL SPATIAL PRINCIPLE
======================================================================

Restore and enforce:

  FUNCTIONAL OPPORTUNITY IS BALANCED; WILDERNESS IS NOT MIRRORED.

And:

  HOMEBASE IS SYMMETRIC SPACE.
  WILDERNESS IS COMPETITIVE SPACE.

Natural N/S terrain differences are NOT automatically competitive deficits.

Do not treat differences in:
  - canopy;
  - elevation;
  - flat land;
  - biome composition;
  - coastline;
  - local resource distribution;
  - wilderness geometry

as defects merely because the opposing team's corresponding terrain differs.

Historical measurements of such differences may remain useful descriptive
measurements. Reinterpret them rather than deleting useful instrumentation.

A competitive deficit exists when geography prevents or severely distorts a
required strategic possibility, not merely when the two sides look different.

This explicitly supersedes interpretations in which N/S wilderness similarity
or "accessible-land asymmetry" itself serves as the balance target.

======================================================================
2. HOMEBASE CORE
======================================================================

The Homebase Core is a COMPACT authored/mirrored structure.

It contains only geometry that genuinely requires team symmetry, including
such things as:
  - Aether Fountain;
  - spawn/reconstruction positions;
  - immediately necessary safe/navigable space;
  - truly base-integral defensive/components if applicable;
  - internal connections;
  - explicit terrain interfaces/exits.

Do NOT define Core as a large radius around the Fountain.

Use this inclusion test:

  Would allowing natural Minecraft terrain generation to determine this
  feature create arbitrary opening combat/defensive differences between
  teams?

If yes, it likely belongs in Core.
If no, prefer leaving it to natural terrain.

Symmetry terminates at explicit terrain interfaces, not at a fixed radius.

======================================================================
3. HOMEBASE SOCKET
======================================================================

A Homebase Socket is a natural-terrain site capable of accepting the same
standardized Core with bounded integration.

North and South sockets DO NOT need to resemble one another.

Each must independently be acceptable.

Conceptual integration cost:

  C_socket =
      footprint intervention
    + interface intervention
    + clearance
    + destruction of meaningful natural terrain/features

Exact weights/thresholds remain OPEN unless already justified empirically.

Reject a socket rather than repairing it if inserting the Core requires major:
  - cliff reconstruction;
  - lake filling;
  - excavation;
  - terrain flattening;
  - transition walls;
  - broad vegetation/feature destruction;
  - other landscape surgery.

Prefer:

  FIND TERRAIN THAT MAKES SIMPLE AUTHORING RELIABLE

over:

  MAKE AUTHORING POWERFUL ENOUGH TO REPAIR BAD TERRAIN.

Map Types define acceptable socket character/search regions, not exact
coordinates.

Hard-coded Alpha origins should ultimately become outputs of socket selection,
not permanent assumptions.

======================================================================
4. OPENING HINTERLAND — IMPORTANT CORRECTION
======================================================================

The Opening Hinterland is SMALL.

It is NOT:
  - the entire team's half/end of the map;
  - the entire section between Fountain and midline;
  - a giant radial homeland;
  - a fixed-radius circle;
  - the full region enclosed by initial Routes.

Conceptually it is its own compact natural envelope/ellipse surrounding the
Core.

WILDERNESS MAY EXIST ON EVERY SIDE OF IT.

That includes Wilderness:
  - toward the midline;
  - east/west of the Homebase;
  - BEHIND the Homebase toward that team's N/S map pole.

Conceptually:

                     WILDERNESS

                 ┌──────────────┐
                 │  HINTERLAND  │
      WILDERNESS │     CORE     │ WILDERNESS
                 └──────────────┘

                     WILDERNESS

Do not implement this as a literal geometric ellipse unless existing tooling
makes that useful. The ellipse describes the spatial role, not necessarily a
hard mask.

The Hinterland is:

  the compact area that is not the authored Aether/Core itself, but has not
  yet transitioned into unrestricted Wilderness.

======================================================================
5. OPENING FLOOR
======================================================================

The Hinterland must permit the team to BEGIN the game's fundamental verbs.

Qualitatively support:
  - ordinary Construction;
  - basic Extraction;
  - basic food/Development;
  - useful early Production;
  - Exploration;
  - movement/material organization relevant to Logistics;
  - ordinary Combat/PvE.

Do NOT convert this into one quota/opportunity per archetype.

Do NOT invent exact counts such as:
  X trees,
  Y cows,
  Z iron,
unless an existing validated design already establishes them.

The general rule:

  THE HINTERLAND MUST PERMIT EVERY FUNDAMENTAL VERB,
  BUT SHOULD NOT RESOLVE ANY OF THEM.

======================================================================
6. OPENING CEILING / PROGRESSION DEFENSE
======================================================================

The Hinterland supplies distributed subsistence, NOT concentrated windfalls.

It should deliberately exclude resources/opportunities that let players skip
meaningful early progression.

KNOWN exclusions include:

  - VILLAGES.
  - CARROTS.
  - ENOUGH ACCESSIBLE IRON TO EQUIP A PLAYER.

The iron rule does NOT mean "no iron whatsoever."

Basic Extraction must remain possible. The Hinterland simply should not hand
a player an equipment-sufficient iron package as an opening resource.

This exclusion list is NOT exhaustive.

Expect additional progression-defying Minecraft resources/opportunities to be
identified later. Preserve a doctrine/configuration seam for this concept
rather than assuming the current list completely defines the ceiling.

Useful ceiling questions include:
  - Does this opportunity scale strongly with multiple players?
  - Does it compress several opening verbs?
  - Does delaying its capture impose major competitive cost?
  - Does it skip an intended economic/progression step?

If strongly yes, it likely belongs in Wilderness.

Socket selection should reject serious ceiling violations before authoring
tries to delete/move natural Minecraft features.

Small floor deficiencies MAY sometimes be corrected using existing authored
opportunity systems.

======================================================================
7. AUTHORED STRATEGIC DEPTH VS RUNTIME PRACTICAL REACH
======================================================================

Preserve the distinction:

  AUTHORED STRATEGIC DEPTH
    = map-authoring classification used for opening economy, renewable
      richness, POI eligibility, objective/world-opportunity siting, etc.

  RUNTIME PRACTICAL REACH
    = actual accessibility during play, which can change through Routes,
      infrastructure, terrain modification, etc.

Player-created Routes must NOT retroactively turn a deep renewable/resource
into a low-depth one.

IMPORTANT NEW CORRECTION:

Do NOT reduce Authored Strategic Depth to:

  distance/practical travel cost from Fountain.

Because the Hinterland is a compact bounded opening envelope and Wilderness
can exist BEHIND or BESIDE it, geographically nearby terrain may still be
Wilderness.

Practical Reach is an INPUT to spatial classification and balance evaluation,
not the sole classifier.

Initial map-authored access/Routes may influence authored opening reach because
they exist at match start.

Later player infrastructure changes runtime reach only.

======================================================================
8. INITIAL ACCESS / ROUTES
======================================================================

Initial map-authored access exists to:
  - make Core exits sane;
  - support a viable compact Hinterland;
  - prevent immediate structural traps.

It should NOT:
  - solve deep Wilderness traversal;
  - connect players automatically to every major POI;
  - pre-build Exploration's strategic work;
  - function as long highways to Worksites/Lair/objectives merely for parity.

Distinguish Homebase access from player-created recognized Routes where useful.

Exploration should extend connectivity beyond what the opening map gives away.

======================================================================
9. DEFAULT MAP TYPE
======================================================================

Preserve the Default regional concept:

  OCEAN
    → COAST
    → OPEN LAND
    → CENTER
    → FOREST
    → RUGGED UPLANDS
    → ALPINE / FROZEN PEAKS

This is a broad Minecraft geographic gradient, NOT seven artificial biome
stripes.

The W–E axis expresses REGIONAL CHARACTER.

The N–S axis expresses TEAM RELATIONSHIP.

The two axes must not be conflated.

A team's North/South wilderness need not match the opponent's.

The Lair, objectives, resources, Worksites, etc. may occur in very different
regional characters from seed to seed.

Default should feel like a compressed believable Overworld continent whose
natural geography has been competitively selected and lightly authored, not a
MOBA board manufactured out of Minecraft blocks.

======================================================================
10. TEAM OBJECTIVES — RECOVER + UPDATE
======================================================================

All three defensive objectives exist AT THE SAME TIME.

They are NOT temporal replacements for one another.

They may each be toppled at any point, subject to actually reaching and
successfully attacking them.

The current forms are:

  1. PILLAGER OUTPOST
     - Pillager Outpost WATCHTOWER ONLY.
     - Do not include cages/tents/log piles/peripheral compound pieces as part
       of its required objective footprint.

  2. NETHER BASTION
     - Bridge Bastion RAMPART/CENTRAL BODY.
     - Remove the long projecting bridge.
     - Do NOT revert to the older Treasure Room concept.
     - Exact retained dimensions should be measured from the selected form,
       not guessed.

  3. END SPIKE
     - Vanilla-style Ender Dragon arena End Spike:
         obsidian pillar
         + End Crystal
         + cage where applicable.
     - NOT an End City tower.
     - NOT the obsolete generic "End Tower" geometry.

  4. AETHER FOUNTAIN
     - final Homebase objective / respawn structure;
     - retain currently canonical Fountain behavior unless specifically
       superseded elsewhere.

======================================================================
11. TEAM OBJECTIVE TOPOLOGY
======================================================================

The three defensive objectives are all WILDERNESS structures.

NONE belong inside the Opening Hinterland.

Their N–S/team-axis ordering is mandatory.

For a team, moving from MIDLINE toward its Fountain:

  MIDLINE
    → PILLAGER OUTPOST
    → NETHER BASTION
    → END SPIKE
    → AETHER FOUNTAIN

The End Spike may therefore be the defensive objective closest to the outer
Hinterland, but it remains in Wilderness.

This ordering is SPATIAL, not a mechanical prerequisite chain.

Do NOT make Bastion invulnerable until Outpost falls.
Do NOT make Spike invulnerable until Bastion falls merely to enforce order.

Bypassing an outer defense through:
  - alternate terrain;
  - tunneling;
  - bridging;
  - Routes;
  - unusual approach;
  - coordinated expedition

is valid Minecraft play unless another currently canonical mechanic explicitly
says otherwise.

W–E ordering is VARIABLE.

Exact N–S spacing is VARIABLE.

Do NOT require:
  - a straight objective lane;
  - equal gaps;
  - mirrored coordinates;
  - identical surrounding terrain.

The invariant is ordinal:

  MIDLINE → OUTPOST → BASTION → SPIKE → FOUNTAIN.

Let natural terrain determine the actual defensive shape within that rule.

======================================================================
12. OBJECTIVE TOPPLING — PRESERVE VERBS, DROP OLD XP HIERARCHY
======================================================================

Recover the still-useful three-route objective grammar.

OUTPOST:
  - sufficient structural destruction;
  - defender elimination;
  - steal/remove the Allay powering the objective.

BASTION:
  - sufficient structural destruction;
  - defender elimination;
  - penetrate/extract the gold hoard.

END SPIKE:
  - sufficient structural destruction;
  - defender elimination;
  - destroy the End Crystal.

Mixed approaches are valid.

Example:
  breaking through a Bastion wall while fighting defenders in order to reach
  its gold is not an invalid hybrid route.

IMPORTANT:

The old doctrine:

  structural destruction < defender elimination < objective-specific action

as a tiered XP reward hierarchy is OBSOLETE.

Do NOT restore it.
Do NOT infer that the game should tell players one valid Minecraft solution is
the "correct/highest-value" solution merely because an old objective document
did so.

Exact XP treatment is outside this pass unless current canonical systems
already determine it.

======================================================================
13. PERMANENT GIANT MONSTER LAIR
======================================================================

There is exactly ONE Giant Monster Lair in the world.

It is NOT:
  - one Lair per monster;
  - a randomly selected new site each boss night;
  - one Lair per team;
  - an ordinary Worksite.

It is a massive, permanent, conspicuous world landmark.

It should read immediately as a boss Lair / major strategic place.

The physical Lair remains at the same location for the match.

Its occupant changes over time:

  GIANT
    → GHAST
    → ENDER DRAGON.

======================================================================
14. LAIR SOCKET
======================================================================

The map generator/recognizer should search for ONE exceptional Lair socket.

A valid socket should provide, in terrain-appropriate form:

  - sufficient 3D encounter volume for all three encounters;
  - enough usable ground space for players, building, retreat, and PvP;
  - meaningful vertical volume, especially for Ghast/Dragon;
  - multiple practical approaches;
  - enough visibility/readability that the Lair is a conspicuous landmark;
  - terrain that is already broadly suitable;
  - bounded authoring rather than huge excavation/flattening;
  - capacity for player modification over the course of the match.

The Ender Dragon is likely the limiting physical-volume case.

Do NOT infer from that that the Lair must be:
  - flat;
  - circular;
  - an artificial stadium;
  - biome-neutral;
  - geometrically centered.

Natural terrain should meaningfully shape the encounter.

A mountain basin, frozen amphitheater, major clearing, unusual valley, etc.
may all be valid depending on Map Type and encounter viability.

======================================================================
15. COMPETITIVE CENTRALITY OF THE LAIR
======================================================================

The Lair should be in a particularly central/conspicuous strategic part of
the map.

"Central" means COMPETITIVELY CENTRAL, not necessarily coordinate-center.

Its W–E/regional position may vary substantially.

On Default it could:
  - be near literal center;
  - be displaced into Forest;
  - sit in Rugged Uplands;
  - even occur in the icy/alpine extreme,

provided TEAM ACCESS remains as close to true equal as practicable.

This is one of the places where parity genuinely matters.

Evaluate initial Practical Reach from both teams to the single shared Lair.

Conceptually:

  A_L =
      |Reach_N - Reach_S|
      -------------------
      mean(Reach_N, Reach_S)

Minimize this disparity.

Do NOT invent a canonical acceptable percentage threshold in this pass unless
existing empirical work supports one.

This differs fundamentally from wilderness-terrain parity:

  asymmetric wilderness terrain = allowed;
  one team having structurally privileged access to the singular shared major
  objective = genuine competitive concern.

======================================================================
16. LAIR DISCOVERY
======================================================================

The Lair location does NOT need a special scheduled global reveal.

It is a permanent place.

Relevant team knowledge is simply whether players have found/scouted it.

Once discovered, its location remains useful knowledge because every major
monster appears there.

This gives Exploration and later Route/infrastructure investment persistent
strategic value.

Do not invent a quest-marker/reveal system unless existing mechanics require
one.

======================================================================
17. LAIR MODIFICATION
======================================================================

Normal Minecraft modification of the Lair should initially remain broadly
valid.

Players may:
  - build;
  - mine;
  - create approaches;
  - create defenses;
  - create staging positions;
  - alter terrain;
  - establish infrastructure.

The accumulated physical history of Giant/Ghast/Dragon contests is desirable.

Do NOT preemptively make the Lair immutable.

Potential cheese such as trivially suffocating/nullifying an encounter should
be treated as a PLAYTEST/BALANCE concern.

General distinction:

  preparation that grants advantage = potentially valid Minecraft strategy;
  trivial geometry that completely nullifies the encounter = possible future
  anti-cheese case.

Do not invent protections until necessary.

======================================================================
18. MONSTER ↔ TEAM OBJECTIVE PAIRING
======================================================================

Recover and preserve:

  GIANT
    → paired with enemy PILLAGER OUTPOST.

  GHAST
    → paired with enemy NETHER BASTION.

  ENDER DRAGON
    → paired with enemy END SPIKE.

Killing the monster gives the victorious team a meaningful advantage toward
toppling its corresponding enemy objective.

HOW THIS ADVANTAGE WORKS IS CURRENTLY OPEN.

Historical material described literal direct objective damage.

Do NOT automatically restore that implementation.

Do NOT invent:
  - a damage percentage;
  - an HP debuff;
  - defender reduction;
  - structural weakness;
  - a timed vulnerability window;
  - signature-verb assistance;
  - any other exact effect.

Preserve an explicit runtime/design seam for:

  paired siege advantage: UNRESOLVED.

The only current requirement is that it should materially help the siege
without making direct objective interaction irrelevant.

======================================================================
19. NEW TEMPORAL MODEL — MAJOR SUPERSESSION
======================================================================

THIS IS NOT MERELY A DOCUMENTATION CHANGE.

The existing temporal phase timeline AND the plugin/runtime model built around
it are substantially superseded.

Do not preserve the current phase architecture as the top-level world
progression model merely because it exists in code.

The new exceptional-opportunity progression is:

  WORKSITE I
      ↓
  GIANT LAIR
      ↓
  WORKSITE II
      ↓
  GHAST LAIR
      ↓
  WORKSITE III
      ↓
  ENDER DRAGON LAIR

Assuming the current day/night cadence maps these to successive relevant
nights, this is conceptually:

  odd opportunity night  → Worksite
  even opportunity night → Lair encounter

Verify the current night/event timing implementation before changing exact
night numbers.

Do not silently assume calendar numbering if the plugin uses another temporal
representation.

The key canonical requirement is ALTERNATION and ESCALATION.

======================================================================
20. WORKSITE TIERS
======================================================================

Existing work on growing Worksite tiers should be recovered and reconciled.

Current working anchors:

WORKSITE I
  - iron/coal-scale mining opportunity;
  - Blast Furnace;
  - Smoker.

WORKSITE II
  - second-tier mining/resource opportunity;
  - Enchanting Table;
  - Anvil;
  - exact/set enchant behavior is NOT important to settle now.

WORKSITE III
  - third/higher-tier Worksite opportunity;
  - exact resource/facility package remains open unless more recent canonical
    work already establishes it.

Do NOT treat these bullets as a complete new reward spec if the repository
contains newer, more detailed Worksite-tier work.

Recover that work first.

The important new doctrine is:

  progressively stronger Worksites alternate with progressively stronger
  Lair encounters.

======================================================================
21. WORKSITE VS LAIR SPATIAL MODEL
======================================================================

WORKSITES:
  - various potential sites;
  - distributed through eligible world geography;
  - temporary exceptional economic/productive opportunities;
  - may activate at different locations.

LAIR:
  - exactly one site;
  - permanent landmark;
  - shared;
  - repeated combat focus;
  - changing occupant.

Do NOT preferentially cluster Worksites around the Lair.

These systems should create contrasting spatial rhythms:

  WORKSITE NIGHT
    → value can pull teams toward distributed world locations.

  LAIR NIGHT
    → value concentrates at the known singular shared landmark.

This prevents every important night from reducing to "go middle."

======================================================================
22. LAIR OCCUPANT LIFECYCLE
======================================================================

The monster does NOT despawn merely because its initial night ends.

Example:

  Giant appears on its scheduled Lair night.

If Giant survives:
  - Giant remains alive through subsequent time;
  - it remains until the next scheduled LAIR night;
  - when Ghast's Lair night begins, Giant is REPLACED by Ghast.

If Giant is killed:
  - the permanent Lair remains;
  - the Lair is dormant/empty;
  - it remains dormant until the next scheduled Lair occupant arrives.

Same principle for Ghast → Dragon.

Do NOT let failure to kill one monster stall the temporal progression.

The players can MISS an opportunity.

The match clock continues.

Treat replacement as authored encounter-state succession, not ordinary random
mob despawning.

======================================================================
23. TEMPORAL PHASE MODEL SUPERSESSION
======================================================================

Historical/current doctrine may describe something like:

  OVERWORLD → NETHER → END → AETHER

or:
  early phase → middle phase → late phase

as the top-level temporal organization of the match.

That is now substantially superseded.

Overworld / Nether / End may remain useful as:
  - thematic escalation;
  - material/encounter identity;
  - difficulty vocabulary.

They are NOT the top-level match-state machine controlling whether Outpost,
Bastion, Spike, etc. exist.

ALL THREE TEAM DEFENSIVE OBJECTIVES EXIST CONCURRENTLY.

The temporal escalation now occurs primarily through:

  WORKSITE I
    → GIANT
    → WORKSITE II
    → GHAST
    → WORKSITE III
    → DRAGON.

Audit the current plugin for assumptions tied to the obsolete temporal model.

======================================================================
24. TEMPORAL RUNTIME MIGRATION
======================================================================

Trace the current temporal system from code outward.

Identify:
  - source of truth for current phase/time progression;
  - state enums/classes;
  - timers/night counters;
  - Worksite spawning;
  - objective activation;
  - mob/encounter activation;
  - reward scaling;
  - configs;
  - manifests;
  - commands;
  - UI/status output;
  - save/reload/recovery;
  - generated test fixtures;
  - tests;
  - any world-authoring logic coupled to phase.

Classify each dependency:

  KEEP
  REINTERPRET
  MIGRATE
  REMOVE
  OPEN / NEEDS DESIGN

Then implement the smallest coherent migration that makes the runtime agree
with the new model.

Do not leave two competing temporal sources of truth.

If exact mechanics required for full migration are unresolved, create a clean
explicit seam rather than inventing design.

======================================================================
25. RENEWABLE SOURCES
======================================================================

Preserve existing Renewable Source runtime semantics where possible.

Do NOT put Strategic Depth, region, or intrinsic value directly into a
Renewable Kind merely because the authoring system now needs them.

Use higher-level authoring policy:

  Map Type
    + Regional Character
    + Authored Strategic Depth
      → eligible renewable kinds
      → richness/novelty range
      → source scale
      → manifestation.

Two independent axes:

  STRATEGIC DEPTH
    = how strategically rich/novel the opportunity may be.

  REGIONAL CHARACTER
    = what kinds of opportunities make geographic/ecological sense here.

Farther/deeper opportunities should generally support increasing:
  - quantity and/or
  - novelty/specialization.

Do NOT assume recovery cadence must also increase.

The current source runtime already supports manifestation/recovery and should
not be rewritten gratuitously.

======================================================================
26. REGENERATIVE MANIFESTATION VISUAL DOCTRINE
======================================================================

Wild crop Patches are valid authored regenerative manifestations even where
the crop does not ordinarily generate wild in vanilla Minecraft.

"Native" does NOT mean "must occur through vanilla world generation."

Preserve:

  WORLD MANIFESTATION = RESOURCE OCCURRENCE
  PLAYER DEVELOPMENT = PRODUCTIVE INFRASTRUCTURE

A Patch should be:
  - irregular;
  - terrain-conforming;
  - recognizably a concentration of a resource/crop.

Reject:
  - fences;
  - rectangular farm layouts;
  - central irrigation;
  - broad pre-prepared farmland;
  - terrain flattening solely to make a farm;
  - other infrastructure implying the site has already been Developed.

======================================================================
27. AUTHORING PHILOSOPHY
======================================================================

Preserve the world-generation hierarchy:

  vanilla generation
    → scan/score
    → select promising seed/region
    → bounded competitive corrections
    → MOBA-specific authoring.

Do NOT procedurally manufacture an idealized MOBA map from bad geography.

Bad underlying geography:
  REJECT.

Good geography with a correctable opportunity deficit:
  AUTHOR MODESTLY.

Worksites, regenerative manifestations, Routes, objective structures, and
other game-authored opportunities can solve functional deficits without
flattening natural geography into parity.

======================================================================
28. UPDATED MAP PIPELINE
======================================================================

Reconcile the pipeline toward something conceptually like:

  Map Type shape
      ↓
  team topology
      ↓
  Homebase search regions
      ↓
  candidate Homebase sockets
      ↓
  Core physical-fit / integration filter
      ↓
  compact Opening Hinterland viability
      ↓
  team-objective socket search
      ↓
  enforce:
    midline → Outpost → Bastion → Spike → Fountain
      ↓
  singular Lair socket search
      ↓
  Lair competitive-access evaluation
      ↓
  initial access / Route layout
      ↓
  Authored Strategic Depth / spatial-role classification
      ↓
  opening-floor evaluation
      ↓
  opening-ceiling evaluation
      ↓
  Worksite-site portfolio
      ↓
  renewable / POI / opportunity authoring
      ↓
  Wilderness viability
      ↓
  physical authoring verification
      ↓
  PlayableMap

Do not treat this exact order as sacred if implementation dependencies require
iteration.

The important point is that Homebase, objectives, Lair, Hinterland,
Wilderness, Worksites, and opportunities are all now first-class inputs to
candidate viability.

======================================================================
29. READY-MAP PIPELINE
======================================================================

Preserve the broader target:

  MAP FOUNDRY
    → READY MAP POOL
    → /moba match start claims unused map
    → instantiate/bind/spawn
    → READY → IN_USE → USED.

Preserve useful provenance such as:
  - match id;
  - seed;
  - candidate id;
  - Map Type;
  - authoring version;
  - config id;
  - plugin version;
  - manifest hash;
  - players/result.

Do not require expensive seed search/world authoring synchronously inside
/moba match start if the current architecture already supports/prepares the
ready-map direction.

======================================================================
30. HISTORICAL ANALYTICS — DO NOT THROW AWAY USEFUL WORK
======================================================================

Review at least the conceptual consequences of the recent spatial/worldgen
history, including the work associated with:

  544747f
  bb1feac
  997a319
  1787590
  faa6748
  7cae5ae
  4244b39
  8199c13
  03c5bea

Do not mechanically revert these commits.

Measurements/tools may remain useful even when their old interpretation is
wrong.

Example:

  "North and South accessible-land values differ"

may remain useful descriptive information.

What is superseded is:

  "therefore the map is competitively deficient and authoring must equalize
   them."

Similarly, existing Route/Practical Reach tooling should be reused where it
now answers better questions:
  - socket opening viability;
  - Lair access parity;
  - objective accessibility;
  - Hinterland practical reach;
  - runtime infrastructure effects.

======================================================================
31. OPEN QUESTIONS — DO NOT INVENT ANSWERS
======================================================================

Explicitly leave open unless newer canonical repo material already resolves:

  - exact Giant → Outpost advantage;
  - exact Ghast → Bastion advantage;
  - exact Dragon → Spike advantage;
  - literal objective-damage amounts;
  - exact XP rewards for toppling objectives;
  - exact boss XP;
  - exact Worksite resource quantities;
  - exact Worksite II enchant configuration;
  - exact Worksite III reward package;
  - exact Homebase socket-cost weights;
  - exact Strategic Depth thresholds;
  - exact Lair access-parity threshold;
  - exact Lair dimensions;
  - anti-cheese/protected-block rules for the Lair;
  - exact objective socket dimensions until measured;
  - exact renewable richness curve;
  - exact Fountain exposure rule;
  - Fountain repair/reactivation;
  - any other balance constant unsupported by testing/current canon.

When implementation needs one of these:
  - expose/configure the seam;
  - preserve current harmless behavior if possible;
  - mark the unresolved design dependency;
  - do NOT silently make a design decision.

======================================================================
32. TESTING / VALIDATION
======================================================================

Update or add tests for doctrine that is concrete enough to test.

At minimum investigate coverage for:

  TEMPORAL
  - Worksite/Lair alternation.
  - Giant → Ghast → Dragon occupant progression.
  - killed monster leaves dormant Lair.
  - surviving monster persists until next Lair night.
  - surviving prior monster is replaced at next Lair transition.
  - missed monster does not stall progression.

  OBJECTIVES
  - all three defensive objectives coexist.
  - no obsolete phase-gating prevents direct attack.
  - spatial ordering metadata/config is:
      midline → Outpost → Bastion → Spike → Fountain.

  MAP
  - Lair is singular.
  - Lair access can be evaluated from both teams.
  - Homebase socket selection does not demand N/S terrain similarity.
  - Hinterland classification does not automatically consume the entire team
    end of the map.
  - Wilderness can exist poleward/behind the Homebase.
  - defensive objectives are classified/sited in Wilderness, not Hinterland.

  OPENING ECONOMY
  - where practical, opening ceiling recognizes known exclusions:
      villages;
      carrots;
      equipment-sufficient iron concentration.
  - do not invent brittle exact resource quotas merely to write a test.

Preserve existing tests that remain semantically valid.

Rewrite/delete tests only when they encode genuinely superseded doctrine.

======================================================================
33. DOCUMENTATION OUTPUT
======================================================================

Update canonical docs so a future contributor can understand the current
system WITHOUT reconstructing it from chat history.

The resulting documentation should clearly distinguish:

  CANONICAL / CURRENT
  WORKING BUT NOT BALANCE-FINAL
  OPEN
  HISTORICAL / SUPERSEDED

Do not leave old contradictory prose appearing equally canonical.

In particular make explicit:

  - all three team defensive objectives coexist;
  - their N–S spatial ordering;
  - their W–E/spacing variability;
  - all are Wilderness structures;
  - compact Hinterland surrounded by Wilderness;
  - one permanent Lair;
  - Giant/Ghast/Dragon succession;
  - Worksite/Lair alternating cadence;
  - temporal phase model supersession;
  - objective-specific monster pairings;
  - exact monster siege effect remains open;
  - old tiered XP-by-toppling-method model is obsolete;
  - natural wilderness asymmetry is permitted;
  - singular Lair access parity is a genuine competitive constraint.

======================================================================
34. REQUIRED AUDIT / REPORT
======================================================================

Create or update an audit document for this pass containing:

A. TEMPORAL MODEL
   - old runtime source(s) of truth;
   - dependencies found;
   - migration performed;
   - unresolved seams.

B. SPATIAL MODEL
   - Core;
   - Socket;
   - compact Hinterland;
   - Wilderness;
   - team-objective topology;
   - Lair;
   - Worksite-site portfolio;
   - Strategic Depth vs Practical Reach.

C. SUPERSESSION MATRIX
   For every significant stale doctrine discovered:
     old concept
     → status
     → replacement
     → affected files/code/tests.

D. IMPLEMENTATION CHANGES
   - code changed;
   - config changed;
   - fixtures changed;
   - tests changed;
   - docs changed.

E. OPEN QUESTIONS
   - only genuinely unresolved design questions;
   - do not re-list questions resolved by this spec.

F. RISKS / NEXT EMPIRICAL WORK
   Especially identify where:
     - Lair access parity needs seed data;
     - socket integration needs physical testing;
     - opening floor/ceiling needs resource measurements;
     - Lair modification may enable encounter-nullifying cheese;
     - Worksite tier values need gameplay calibration.

======================================================================
35. SCOPE / EXECUTION RULE
======================================================================

Unlike the previous spatial-doctrine audit:

  EXECUTABLE LOGIC MAY CHANGE.
  CONFIGURATION MAY CHANGE.
  TESTS MAY CHANGE.
  FIXTURES MAY CHANGE.

because the existing temporal phase model/plugin behavior is itself being
superseded.

However:

DO NOT:
  - invent unresolved balance constants;
  - regenerate every world merely because terminology changed;
  - run expensive gameplay trials unless required to verify the migration;
  - implement unrelated class/combat systems;
  - invent new Map Types;
  - force natural terrain parity;
  - over-author terrain;
  - create three separate Lairs;
  - turn objectives into mechanically prerequisite turret tiers;
  - restore tiered XP rewards for objective toppling methods;
  - assume old "direct boss damage" is still canonical;
  - convert the Hinterland into a giant radial homeland.

Prefer the smallest coherent implementation that:
  1. removes obsolete temporal assumptions;
  2. establishes the new runtime cadence;
  3. makes the spatial data model capable of representing the current design;
  4. preserves existing working systems where their semantics remain valid.

======================================================================
36. COMPLETION CRITERIA
======================================================================

This pass is complete when:

1. The repository has one coherent documented spatial doctrine.

2. The plugin no longer relies on the superseded Overworld/Nether/End-style
   temporal phase model as the top-level organizer of world/objective
   availability.

3. All three team defensive objectives can coexist in the model/runtime.

4. The runtime can represent the alternating progression:

     Worksite I
       → Giant
       → Worksite II
       → Ghast
       → Worksite III
       → Dragon.

5. One permanent Lair can progress through Giant/Ghast/Dragon, including
   persistence/replacement/dormancy semantics.

6. Worksites remain distributed opportunities rather than being collapsed
   into the permanent Lair.

7. The spatial model can represent:
     compact Core
       + compact Hinterland
       + surrounding Wilderness
       + ordered Wilderness team objectives
       + singular competitively-central Lair.

8. Map analysis no longer interprets ordinary N/S wilderness terrain
   difference as a defect by default.

9. Known opening-ceiling exclusions are represented in doctrine and, where
   appropriate/currently feasible, candidate analysis:
     village
     carrot
     equipment-sufficient opening iron.

10. Historical useful measurements/tooling are preserved or reinterpreted
    rather than discarded unnecessarily.

11. All relevant tests pass, with obsolete tests migrated to the new model.

12. The audit clearly states:
      what changed,
      what was superseded,
      what implementation was migrated,
      what remains open,
      and what should be done next.

Commit the completed coherent pass and push it to the working branch.

In the final report provide:
  - commit hash;
  - branch;
  - concise summary of doctrine changes;
  - concise summary of runtime/config/test migration;
  - test results;
  - audit document path;
  - explicit list of unresolved seams;
  - recommended next implementation/design step.