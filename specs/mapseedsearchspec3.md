# Default Map Fitting — Post-Task-A Sequence Specification

Status: Working implementation/design sequence
Current state: Task A initial deterministic fitting complete
Current artifacts: implementation/worldgen/results/default_task_a_2026-09-10/
Governing Task A spec: specs/mapseedsearchspec2.md


1. PURPOSE

This specification defines the sequence from the completed first Task A fitting pass through selection of a physical Default-map prototype.

The sequence is:

Task A.0 — Initial analytical fitting
→ Task A.1 — Fitting-model refinement
→ Task A.2 — Comparative validation and finalist selection
→ Task B.0 — Minimal physical greybox
→ Task B.1 — In-world validation
→ Task B.2 — Competitive skeleton revision/freeze
→ Task C — Gameplay-system placement

The purpose of this sequence is to prevent physical map authoring from becoming a substitute for solving the analytical map-fitting problem.

Task B must consume a spatial skeleton that is already sufficiently trusted. It must not become an iterative process of manually searching the world for a better map.


2. CURRENT STATE: TASK A.0

Task A.0 has been completed for the eight existing Default finalists.

It currently produces:

- playable bounds;
- North and South homeland footprints;
- North and South Aether Fountain anchors;
- three Route corridors per team;
- Starter Route sections;
- Starter Route termini;
- center/interior reference;
- regional-depth diagnostics;
- inferred cross-connections;
- inferred shortcuts;
- intersections;
- coast/highland access;
- homeland-quality diagnostics;
- Route-divergence diagnostics;
- Starter construction/legibility diagnostics;
- per-candidate JSON;
- per-candidate SVG greybox;
- aggregate comparison output.

The eight candidates remain regression substrates, not eight maps that must eventually be physically built.

Task A.0 has demonstrated that deterministic fitting is feasible, but it has also exposed analytical ambiguities that should be resolved before physical greyboxing.


3. TASK A.1 — REFINE THE FITTING MODEL

3.1 Goal

Improve the meaning of the fitted competitive skeleton without changing the governing Default-map design or manually tuning individual seeds.

Task A.1 must answer:

"Does the fitter recognize the kinds of spatial relationships the Default actually values?"

It must not answer:

"Which seed do we like best?"

The same eight candidates remain the test set.

No new seed search or world generation occurs.


4. TASK A.1A — CENTRAL / INTERIOR CONNECTIVITY

The current implementation relies too heavily on proximity between Routes and a single center reference.

This is insufficient.

The Default's center is:

a junction or connective geographic area, not necessarily a point destination.

A successful central structure may consist of:

- one major junction;
- several nearby junctions;
- a river-crossing network;
- a valley or basin connecting several approaches;
- a set of natural crossings;
- neighboring Route corridors linked laterally;
- a pass or saddle;
- an open interior through which several approaches interact.

Therefore, replace or supplement center_nearby_routes with a central connective-area analysis.

Evaluate within a defined central/interior area:

- number of Route corridors entering or approaching it;
- number of teams represented;
- number of distinct approaches;
- Route-to-Route connections available through it;
- lateral connectivity;
- size of traversable shared geography;
- whether North/South traversal can interact there;
- whether West/East movement can cross through it;
- whether the area forms one connective cluster or several nearby connected clusters.

Do not require all six Routes to converge.

Do not reward a six-spoke conventional MOBA nexus merely because every Route intersects one point.

The desirable property is:

competitive convergence without mandatory convergence.


5. TASK A.1B — ROUTE DIFFERENTIATION

Current Starter-terminus separation is useful but incomplete.

Two Routes are not meaningfully different merely because their termini are separated.

Evaluate differentiation at several scales.


5.1 Opening differentiation

Measure:

- distinct homeland departures;
- Starter Route overlap;
- Starter terminus separation;
- angular/directional separation where meaningful.


5.2 Early-Wilderness differentiation

After Starter termination, evaluate whether Routes:

- continue through distinct corridors;
- immediately merge;
- enter different regional geography;
- approach different geographic opportunities;
- interact with different formations;
- provide meaningfully different traversal choices.


5.3 Broader-network differentiation

Determine whether two nominally different Routes eventually become functionally the same branch.

A Route pair should be penalized when:

separate homeland departure
→ separate Starter road
→ immediate convergence
→ same Wilderness corridor.

That is effectively one strategic opening presented through two roads.

Three Routes should create three meaningful opening decisions, not merely three pieces of infrastructure.


6. TASK A.1C — CROSS-CONNECTIONS AND SHORTCUTS

The initial fitter produces suspiciously uniform cross-connection counts.

A path existing between two Route corridors is not sufficient to call it a meaningful cross-connection.

Distinguish:


6.1 Incidental traversability

A player can technically walk between two Routes through ordinary terrain.

This is expected and should not automatically become a network feature.


6.2 Useful lateral connection

A reasonably efficient and geographically coherent movement option connects two corridors.

Examples include:

- valley;
- clearing;
- riverbank;
- saddle;
- pass;
- ford;
- beach;
- forest gap;
- ridge shelf.


6.3 Strategic shortcut

A traversal relationship meaningfully changes movement compared with remaining on the established network.

A shortcut may:

- shorten travel;
- bypass a junction;
- connect two otherwise separated Route branches;
- allow flanking;
- provide alternate access to a region;
- become substantially more useful after modest player infrastructure.

Output these separately.

Do not simply select a fixed number of cross-connections per candidate.


7. TASK A.1D — HOMELAND EQUIVALENCE

Retain homeland quality scoring, but do not treat a single quality-difference threshold as a definitive competitive failure.

Report:

- North quality;
- South quality;
- absolute disparity;
- individual component disparities;
- whether each homeland independently exceeds minimum usability requirements.

Classify disparity approximately as:

- Low
- Moderate
- High
- Severe

The exact thresholds should remain explicit and testable.

A moderate scalar difference may be acceptable if the homelands provide competitively equivalent opportunities through different terrain.

A severe difference remains an important fitting failure.

Task A does not yet evaluate resource equivalence.


8. TASK A.1E — REGIONAL / OPERATIONAL DEPTH

Current depth analysis often establishes that usable land exists in a band without proving that meaningful geography exists there.

Separate:


8.1 Traversable depth

There is usable connected land at the operational distance.


8.2 Geographic depth

The band contains a meaningful regional core, formation, landmark relationship, or distinct landscape opportunity.

Use the established bands:

0–40 effective blocks:
Immediate homeland fringe.

40–70:
Starter/opening transition.

70–110:
Secondary-region core.

110–150:
Secondary transition.

150–210:
Tertiary-region core.

210–250:
Deep transition.

250–350+:
Quaternary/deep-region core.

Do not require every band to contain a separate biome.

The desired result is progressive geographic depth, not concentric biome rings.

Evaluate both teams independently.


9. TASK A.1F — STARTER ROUTE EVALUATION

Preserve the current approximately 60–70 effective-block target from homeland edge, with a soft acceptable band rather than exact cutoff.

Retain separate evaluation of:

1. Route corridor quality.
2. Starter construction feasibility / legibility.

Starter Routes must support deliberate Minecraft-native construction such as:

- dirt/path roads;
- footpaths;
- stairs;
- bridges;
- cleared vegetation;
- retaining work;
- modest terrain adaptation.

Penalize starts requiring:

- immediate severe cliff ascent;
- extensive deep-water construction;
- extreme earthwork;
- heavy terrain replacement;
- implausibly circuitous road construction.

Remember:

Starter Routes exist to make the early game happen.

Their fit should therefore evaluate whether the three roads collectively expose players to differentiated useful opening geography, not merely whether each individual road is easy to build.


10. TASK A.1G — NETWORK-LEVEL EVALUATION

After the preceding refinements, evaluate the complete skeleton.

A successful network should provide:

- three meaningful opening choices per team;
- reliable Starter travel;
- differentiated Wilderness continuations;
- eventual opportunities for opposing-team interaction;
- meaningful lateral movement;
- useful central/interior connectivity;
- access to western highland geography;
- access to eastern/coastal geography;
- off-Route alternatives;
- natural shortcuts;
- multiple approaches to contested geography;
- no requirement that every important movement pass through one chokepoint;
- substantial Wilderness not pre-solved by Routes;
- increasing unsupported geographic depth beyond Starter Route termini.

Do not reward conventional MOBA geometry for its own sake.


11. TASK A.1H — PRESERVE UNCERTAINTY

Continue explicitly marking unavailable metrics.

Do not infer unsupported precision for:

- exact block walkability;
- construction volume;
- caves/ravines/overhangs;
- actual sightlines;
- water depth;
- ford viability;
- resource equivalence;
- true mandatory chokepoints;
- Hunger expenditure;
- measured travel time;
- ecology.

Eight-block terrain sampling cannot prove these.

Task B exists partly to expose these block-level realities later.


12. TASK A.1 EXECUTION

Implement the refinements once in the deterministic fitter.

Then rerun:

- 930005557
- 930006815
- 930007222
- 930010639
- 930012642
- 930015734
- 930016664
- 930019528

No seed-specific exceptions.

No manual Route placement.

No new terrain generation.

Produce revised:

- fit.json;
- greybox.svg;
- aggregate comparison output;
- focused tests;
- short implementation/limitations report.

Keep Task A.0 outputs available for comparison rather than overwriting them if practical.


13. TASK A.2 — COMPARATIVE VALIDATION

Task A.2 contains little or no new map-generation code.

Its purpose is to review whether Task A.1 now expresses the intended Default grammar.

Compare the eight candidates using the revised outputs.

Human review should especially examine:

- whether Route corridors correspond to plausible natural geography;
- whether three opening choices actually feel different;
- whether Starter termini occur at sensible geographic handoffs;
- whether center/interior connectivity is convincing without becoming a MOBA nexus;
- whether lateral links correspond to recognizable formations;
- whether regional depth is genuine;
- whether homeland differences look competitively acceptable;
- whether the SVG abstraction agrees with prior manual world inspection.


14. TASK A.2 — REGRESSION CASES

The current eight candidates already expose useful failure modes.

Preserve these as regression cases even after finalists are selected.

Current examples include:

- Route collapse — multiple Starter Routes becoming one practical opening.
- Homeland inequivalence — one side accepting the Default much more naturally than the other.
- Over-centralization — a clean six-branch graph that may be more conventional-MOBA-like than Minecraft-like.
- Under-convergence — good independent expedition networks that never form sufficient competitive interaction.
- Excessive incidental shortcuts — designated Routes insufficiently distinguished from arbitrary open-country traversal.
- Forced Starter construction — analytical corridor exists but would require excessive physical authoring.

The fitter should continue recognizing these classes of failure.


15. TASK A.2 — SELECTION

After reviewing Task A.1 outputs, select one or two candidates for Task B.

Do not necessarily choose the highest scalar score.

Selection should be based on whether the fitted skeleton:

1. obeys the Default grammar;
2. uses Minecraft terrain rather than fighting it;
3. creates a functional opening game;
4. supports meaningful Wilderness progression;
5. creates competitive interaction without imposing conventional lane geometry;
6. can plausibly be materialized with modest intervention.

Current pre-A.1 candidates of particular interest are:

- 930010639
- 930012642
- 930019528

This is not a locked shortlist.

Task A.1 may change that assessment.


16. GATE BETWEEN TASK A AND TASK B

Do not begin Task B until:

- homeland placement is believable;
- both sides have three meaningful opening choices;
- Starter Route handoffs are plausible;
- obvious Route-collapse failures are absent;
- central/interior connectivity is understood;
- major lateral connections are plausible;
- geographic depth is represented;
- no major fitter diagnostic is suspected to be fundamentally mismeasuring the design;
- a human review selects the candidate.

This is the Task B readiness gate.


17. TASK B.0 — MINIMAL PHYSICAL GREYBOX

Task B begins only after candidate selection.

Its purpose is:

Materialize the accepted analytical skeleton inside the actual vanilla Minecraft world and determine whether it survives block-level reality.

Task B does not redesign the map.

It consumes Task A coordinates and geometry.


18. TASK B.0 — PHYSICAL SCOPE

For the selected candidate, physically represent only the minimum competitive skeleton.


18.1 Homeland

Materialize:

- approximate homeland boundary;
- Aether Fountain location;
- three Route departure points.

Homeland boundaries may use temporary/debug markers rather than final architecture.


18.2 Aether Fountain

Create a simple placeholder/greybox representation.

Do not design the final Fountain structure.


18.3 Starter Routes

Materialize the fitted Starter sections using deliberately recognizable Minecraft-native infrastructure.

Possible temporary vocabulary:

- dirt/path road;
- compacted-looking path treatment;
- stairs;
- simple bridge;
- vegetation clearing;
- modest retaining edge.

The purpose is to make each Route unmistakable.


18.4 Starter termini

Clearly mark the transition where provided early-game infrastructure ends.

The player should be able to experience:

homeland
→ obvious Route
→ Wilderness handoff.


18.5 Optional analytical markers

Where useful, temporary markers may identify:

- center/interior reference;
- candidate Key Locations;
- major lateral connections;
- crossings.

These are debug markers, not final content.


19. TASK B.0 — EXPLICIT EXCLUSIONS

Do not yet build:

- final objectives;
- Mining Outposts;
- production Worksites;
- final Key Location gameplay content;
- Supply Chains;
- crop systems;
- animal ecology;
- hostile ecology;
- resource portfolios;
- authored villages;
- final defensive structures;
- finished homeland architecture;
- final Aether Fountain;
- final roads;
- final map boundary solution.

Task B.0 is a spatial greybox.


20. TASK B.1 — IN-WORLD VALIDATION

Load the physical greybox in Minecraft.

Validation should be performed primarily through actual player-scale traversal, not aerial screenshots alone.

Test each homeland separately.


21. OPENING-GAME WALK TEST

From each Fountain:

1. Identify the three Starter Routes without external map knowledge.
2. Choose each Route.
3. Travel it normally.
4. Observe terrain and landmarks encountered.
5. Reach its terminus.
6. Continue into unsupported Wilderness.

Ask:

- Are the three choices obvious?
- Do they feel different?
- Is travel initially reliable?
- Does each road teach something about the world?
- Does the road end at a meaningful point?
- Does unsupported Wilderness begin at the right time?
- Does the player have an understandable next decision after the terminus?

This is the first major Task B test.


22. ROUTE CONSTRUCTION REALITY TEST

Compare analytical Starter-legibility estimates against actual block geometry.

Record:

- unexpected cliffs;
- tree-clearing burden;
- water depth;
- bridge requirements;
- ugly switchbacks;
- terrain cuts;
- excessive stairs;
- awkward road grades;
- block-level obstacles hidden by eight-block sampling.

The goal is not zero construction.

The goal is:

Does modest Minecraft-native infrastructure reveal the terrain's existing traversal logic, or must the map author manufacture a Route that the terrain does not support?


23. WILDERNESS CONTINUATION TEST

After each Starter terminus, continue traveling without following debug overlays.

Determine whether the natural corridor remains understandable through:

- valleys;
- passes;
- clearings;
- riverbanks;
- forest edges;
- slopes;
- coastlines;
- landmarks.

The Route itself may remain designated abstractly, but the world should not require a painted road through the entire Wilderness to explain traversal.


24. COMPETITIVE INTERACTION TEST

Travel from both teams toward:

- center/interior;
- highland;
- coast;
- major inferred Key Locations;
- major cross-connections.

Determine whether:

- opposing approaches plausibly interact;
- lateral movement is understandable;
- one crossing accidentally becomes mandatory;
- shortcuts actually exist;
- apparent SVG intersections are real;
- geography supports ambush, avoidance and alternate traversal;
- terrain permits player-created alternatives.


25. GEOGRAPHIC DEPTH TEST

Walk outward from each homeland.

Evaluate whether the established operational progression is perceptible:

immediate
→ opening
→ secondary
→ tertiary
→ deep Wilderness.

This is qualitative at Task B.1.

Do not pretend Minecraft Hunger alone creates these bands.

Evaluate combined:

- travel effort;
- terrain;
- provisioning;
- navigational uncertainty;
- distance from homeland;
- ease of return;
- future infrastructure value.


26. TASK B.1 OUTPUTS

For each physical candidate, record:

- screenshots;
- traversal notes;
- Route-specific failures;
- actual Starter construction burden;
- mistaken analytical assumptions;
- useful natural features omitted by Task A;
- block-level barriers;
- unexpected connections;
- candidate Key Locations revealed by walking;
- whether the map should proceed.

Do not immediately edit the world after every discovered problem.

Complete the validation pass first.


27. TASK B.2 — COMPETITIVE SKELETON REVISION

After the full Task B.1 walk test, make a bounded revision pass.

Allowed changes include:

- modest homeland-footprint adjustment;
- modest Fountain relocation;
- Route departure adjustment;
- Route-corridor adjustment;
- Starter terminus adjustment;
- choosing a nearby better natural crossing;
- recognizing an overlooked natural lateral connection;
- abandoning an analytically inferred connection that does not exist physically.

Do not perform macro terrain repair.

If the candidate requires:

- manufacturing a mountain pass;
- relocating a coastline;
- carving a major valley;
- replacing biome-scale terrain;
- extensive terrain flattening;
- wholesale Route reconstruction;

then reconsider the candidate rather than forcing it.


28. TASK B.2 — REFIT VS LOCAL CORRECTION

Distinguish two failure types.


28.1 Local physical discrepancy

Example:

The fitted road hits a six-block cliff, but a natural saddle exists 20 blocks west.

Correct locally.


28.2 Structural fit failure

Example:

Two Routes believed to be separate actually share the same valley for their entire useful length.

Return to the fitting decision or candidate selection.

Do not disguise structural failure with construction.


29. SKELETON FREEZE

Once the selected candidate passes physical validation, freeze the first Default competitive skeleton.

Freeze:

- playable footprint;
- logical orientation;
- homeland footprints;
- Fountain anchors;
- three opening Route relationships per team;
- Starter Route termini;
- major Wilderness Route relationships;
- central/interior connective geography;
- major crossings;
- major lateral connections;
- core geographic-depth structure.

This is not a finished map.

It is the stable geographic framework onto which gameplay systems can now be placed.


30. TASK C — GAMEPLAY-SYSTEM PLACEMENT

Only after the skeleton is stable should unresolved gameplay content begin occupying it.

Task C includes later work on:

- objectives;
- Key Location gameplay roles;
- Mining Outposts;
- production Worksites;
- resource opportunities;
- villages;
- ecology;
- animal/crop systems;
- hostile pressure;
- Supply Chains;
- defensive geography;
- objective relationships;
- infrastructure economy.

These systems should respond to the accepted geography rather than determine the seed retrospectively.


31. TASK C PRINCIPLE

Task C should generally follow:

geography
→ opportunity
→ gameplay role

rather than:

gameplay role
→ force geography to accommodate it.

For example:

mountain foot
+ cave system
+ defensible shelf
→ valuable geographic opportunity
→ possible Extraction-oriented Key Location

rather than:

need Mining Outpost
→ place Mining Outpost at arbitrary symmetric coordinate.


32. LATER PHYSICAL AUTHORING

Only after gameplay-system placement should the project proceed toward polished map authoring:

- final homeland architecture;
- final Aether Fountains;
- final Starter Route construction;
- authored structures;
- objective structures;
- local terrain corrections;
- ecological setup;
- resource corrections;
- final landmarks/signposting;
- boundary treatment;
- presentation polish.

The world should remain recognizably the vanilla seed underneath those interventions.


33. FULL DEVELOPMENT SEQUENCE

VANILLA TERRAIN SEARCH
    ↓
Stage A — cheap candidate filtering
    ↓
Stage B — structural terrain ranking
    ↓
Stage C — official vanilla generation + extraction
    ↓
Stage D — initial analytical competitive fit
    ↓
8 accepted regression candidates
    ↓
TASK A.0 — deterministic Default fitting
    ↓
TASK A.1 — refine fitting semantics
    ├─ central connective areas
    ├─ Route differentiation
    ├─ meaningful cross-connections
    ├─ homeland equivalence
    ├─ operational geographic depth
    └─ network-level evaluation
    ↓
rerun same 8 candidates
    ↓
TASK A.2 — human comparative validation
    ↓
select 1–2 physical finalists
    ↓
TASK B.0 — minimal in-world greybox
    ├─ homeland
    ├─ Fountain placeholder
    ├─ three Starter Routes/team
    └─ termini/debug markers
    ↓
TASK B.1 — player-scale traversal validation
    ├─ opening-game test
    ├─ construction-reality test
    ├─ Wilderness-continuation test
    ├─ competitive-interaction test
    └─ geographic-depth test
    ↓
TASK B.2 — bounded spatial correction
    ↓
DEFAULT COMPETITIVE SKELETON FREEZE
    ↓
TASK C — gameplay-system placement
    ├─ Key Location roles
    ├─ objectives
    ├─ Worksites
    ├─ resources
    ├─ ecology
    ├─ villages
    └─ infrastructure relationships
    ↓
PHYSICAL AUTHORING / ITERATION
    ↓
PLAYTEST
    ↓
competitive corrections
    ↓
DEFAULT MAP INSTANCE


34. GOVERNING RULE

At every stage, preserve the distinction between discovering what the Minecraft world already provides and authoring what the MOBA requires.

The pipeline should progressively reduce uncertainty:

Seed search finds good Minecraft geography.

Task A determines whether the Default can fit that geography.

Task B determines whether the analytical fit survives actual Minecraft space.

Task C gives that accepted geography gameplay content.

Later authoring makes the resulting map playable and legible without erasing the vanilla world underneath it.

This sequence prevents roads, objectives, structures or terrain edits from being used prematurely to solve problems that should have been caught at the geography/fitting layer.