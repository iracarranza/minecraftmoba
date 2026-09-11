SPEC: Default Map Opportunity-Relationship Shadow Analysis
Project: Minecraft MOBA
Scope: Default-map analytical fitter only
Mode: Diagnostic / shadow analysis
Do not alter physical worlds or active route selection

CURRENT STATE

Continue from current origin/main after the destination-first and local-continuation/deep-network work.

Relevant recent architecture:

- Historical Starter Route geometry is no longer a solver input.
- The fitter currently:
  - builds homeland-relative traversal/depth fields;
  - discovers destination candidates;
  - selects three candidate handoffs per homeland where possible;
  - derives departures after destination selection;
  - evaluates local continuation separately from deep-network reach;
  - reports same-team and opponent topology.
- Existing local continuation classes:
  - dead_end
  - directed
  - branching
  - junction
- Existing deep-network analysis remains descriptive and broad.
- Existing selected handoffs should remain exactly as selected during this pass.

This pass introduces a new diagnostic model:

    terrain feature
        ↓
    Handoff
        ↓
    Affordance
        ↓
    Reach
        ↓
    Payoff
        ↓
    Opportunity Relationship

The purpose is to see whether this model correctly explains the existing candidate/selection behavior before it is allowed to influence selection.

DO NOT:
- change current selected routes/handoffs;
- change destination eligibility;
- add a persistence-only eligibility rule;
- retune scoring;
- retune local horizon;
- change map dimensions;
- change homeland placement;
- change region segmentation;
- generate seeds;
- modify/materialize worlds;
- begin Task C;
- add authored POIs/resources to the actual map;
- delete or overwrite previous analytical outputs.

Write all outputs to a new result directory.

--------------------------------------------------
1. DESIGN PRINCIPLE
--------------------------------------------------

A Starter Route should not merely "lead to a destination."

It should provide privileged opening access from the homeland to a meaningful Opportunity Relationship.

The Starter Route ends at a Handoff: the point where pre-established opening infrastructure has done enough to expose an opportunity, but not enough to solve that opportunity for the player.

Canonical shapes:

DIRECT DESTINATION

    Homeland
      ↓
    Starter Route
      ↓
    Village / gameplay-bearing location

GATEWAY

    Homeland
      ↓
    Starter Route
      ↓
    foothill / forest threshold / pass entrance
      ↓
    unsupported geographic access
      ↓
    valuable region/opportunity

TRANSITION

    Homeland
      ↓
    Starter Route
      ↓
    coast / river / crossing interface
      ↓
    different traversal relationship
      ↓
    valuable region / POI

JUNCTION

                             → Opportunity A
    Homeland → Starter → J  → Opportunity B
                             → Opportunity C

The semantic rule is:

    Starter infrastructure should expose an opportunity,
    not complete the expedition.

--------------------------------------------------
2. OPPORTUNITY RELATIONSHIP DATA MODEL
--------------------------------------------------

Add a diagnostic object approximately equivalent to:

OpportunityRelationship {
    relationship_id,
    homeland,
    source_candidate_id,

    origin,
    handoff,
    affordances,
    reach,
    payoff,
    supported_functions,
    completion_state,
    commitment_profile,
    same_team_relationships,
    opponent_relationships,
    evidence_summary
}

Exact field names may follow existing code conventions.

Do not remove existing candidate fields.

--------------------------------------------------
3. ORIGIN
--------------------------------------------------

Represent the homeland-side origin of the relationship.

Conceptually:

origin {
    homeland_id,
    implied_departure,
    perimeter_source,
    opening_direction
}

This must remain derived from the relationship/candidate.

Do not reintroduce historical route departures or historical path geometry.

--------------------------------------------------
4. HANDOFF
--------------------------------------------------

The Handoff is where Starter Route infrastructure can reasonably stop.

Conceptually:

handoff {
    anchor,
    arrival_envelope,
    feature_kind,
    feature_component,
    homeland_depth,
    travel_cost,
    starter_constructability,
    local_context
}

IMPORTANT:

The Handoff should not necessarily be treated as a single exact coordinate.

Add diagnostic support for an `arrival_envelope` where feasible.

An arrival envelope means:

    a set/region of nearby locations from which the same strategic handoff
    is legibly and practically accessed.

Examples:
- village edge rather than village center;
- usable stretch of coastline rather than one exact beach sample;
- foothill interface rather than one arbitrary threshold block;
- river access segment rather than one point.

Do not rewrite path fitting around arrival envelopes yet.
Report them diagnostically.

--------------------------------------------------
5. TERRAIN AFFORDANCES
--------------------------------------------------

Separate physical-world affordance from player-facing Handoff function.

Use a compact affordance vocabulary.

Required conceptual affordances:

- passage
- barrier
- crossing
- corridor_access
- network_access
- threshold
- direct_interaction

A candidate may support multiple affordances.

Examples:

highland approach:
    threshold + passage

forest entrance:
    threshold + passage

river access:
    corridor_access
    possibly crossing
    possibly barrier context

coast:
    threshold + network_access

village:
    direct_interaction
    potentially corridor/junction relationships depending geography

Do not infer affordances from feature labels alone when geometry contradicts them.

Store evidence.

Example:

affordances: [
    {
        type: "network_access",
        evidence_state: "supported",
        evidence: {...}
    }
]

--------------------------------------------------
6. PLAYER-FACING HANDOFF FUNCTIONS
--------------------------------------------------

Do not assign exactly one function.

A Handoff may support multiple functions.

Required function vocabulary:

- Gateway
- Junction
- Destination
- Transition

Use evidence states:

- strong
- supported
- weak
- unsupported

These states are evidence/confidence categories, not numerical quality tiers.

Conceptual output:

supported_functions {
    gateway: {
        state,
        evidence
    },
    junction: {
        state,
        evidence
    },
    destination: {
        state,
        evidence
    },
    transition: {
        state,
        evidence
    }
}

Do not use these function results to alter selection in this pass.

--------------------------------------------------
7. GATEWAY EVIDENCE
--------------------------------------------------

A Gateway means:

    crossing the Handoff gives access to a meaningful geographic system
    that was not equivalently accessible before the Handoff.

Gateway evidence should consider:

A. recognizable threshold / entrance;
B. persistence of the entered geography;
C. increasing commitment into that geography rather than immediate exit
   back into equivalent terrain;
D. credible access toward a markedly useful geographic/resource region.

Do not reduce Gateway validation to generic graph reachability.

Examples:

GOOD:
    plain
    → foothill threshold
    → sustained mountain interior

BAD:
    plain
    → 20-block forest strip
    → plain

The entered geography itself should persist as the claimed relationship.

Reuse local continuation and deep reach where useful, but report them as evidence.

Do not add a new universal persistence eligibility rule.

--------------------------------------------------
8. JUNCTION EVIDENCE
--------------------------------------------------

A Junction means:

    reaching the Handoff creates multiple materially different onward
    opportunity relationships.

Raw branch count is not sufficient.

Junction evidence should consider:

- at least two meaningful onward branches;
- branch persistence;
- materially different geographic/opportunity contexts;
- whether branches immediately reconverge;
- whether branches are merely two geometric paths through equivalent terrain.

Examples:

WEAK / NOT A TRUE JUNCTION:
              ↗ open plain
    handoff →
              ↘ open plain

STRONGER:
              → mountain access
    handoff →
              → river corridor

Use existing local branch analysis as input.

Do not make "junction" automatically better than "directed".

--------------------------------------------------
9. DESTINATION EVIDENCE
--------------------------------------------------

A Destination means:

    the thing reached at the Handoff is itself a sufficiently meaningful
    gameplay-bearing reason for the Starter Route to exist.

For current natural-world analysis, be conservative.

Strong natural examples:
- village;
- significant vanilla structure where strategically relevant.

Future authored examples may include:
- Worksite;
- recurring strategic resource site;
- minor POI;
- other deliberately placed strategic systems.

Do NOT treat arbitrary terrain as a Destination merely because it is visually distinct.

Examples:

mountain:
    usually not Destination evidence by itself

beach:
    not Destination evidence by itself

river:
    not Destination evidence by itself

village:
    Destination evidence

If the route can sensibly arrive near but not inside a destination,
use arrival-envelope logic.

--------------------------------------------------
10. TRANSITION EVIDENCE
--------------------------------------------------

A Transition means:

    reaching the Handoff changes the player's available traversal
    relationship.

Transition evidence should require:

A. distinguishable traversal environments;
B. a usable interface between them;
C. meaningful access through/on the second traversal system;
D. a credible downstream payoff relationship.

Examples:

land → coast → water network
land → river access → navigable corridor
bank A → crossing → bank B
surface → cave system, if/when subterranean traversal is analyzed

Do not treat touching water as sufficient.

--------------------------------------------------
11. WATER TOPOLOGY
--------------------------------------------------

Add diagnostic water-affordance analysis.

Do not rely only on labels such as river/lake/ocean.

From the player's perspective, water may function as:

- barrier
- crossing interface
- corridor
- maritime network
- local feature

These may overlap.

Definitions:

BARRIER
    Water meaningfully separates terrestrial spaces.

CROSSING INTERFACE
    A practical location where crossing gives access to materially
    different/opposite-bank geography.

CORRIDOR
    Water supports sustained directional travel along its geometry.

MARITIME NETWORK
    Water supports projection among multiple distinct shoreline,
    island, coastal, or regional access points.

LOCAL FEATURE
    Water exists but does not create meaningful projection/crossing
    relationships at map scale.

Do not make raw water area the primary measure.

A small river connecting major regions may be strategically stronger
than a huge enclosed lake.

--------------------------------------------------
12. TRANSITION PAYOFF REQUIREMENT
--------------------------------------------------

Natural mobility alone is not sufficient to justify a Starter Route.

For a movement-network Transition, require the relationship:

    usable interface
        ↓
    meaningful natural movement network
        ↓
    markedly valuable resource region / POI / strategic opportunity

Conceptually:

    Starter Route → coast → ocean → valuable remote opportunity

may be valid.

But:

    Starter Route → coast → large empty ocean

is not sufficient merely because the player can move far.

Likewise:

    Starter Route → river → meaningful remote resource region

may be stronger than:

    Starter Route → huge lake → nothing strategically distinct.

Do not make the payoff have to be adjacent to the Handoff.

Distance through the natural movement system may be part of the
opportunity.

--------------------------------------------------
13. PAYOFF MODEL
--------------------------------------------------

Represent Payoff in three categories:

payoff {
    observed: [...],
    potential: [...],
    authored: [...]
}

OBSERVED

Actually exists in vanilla/world data.

Examples:
- village;
- relevant structure;
- strong resource-bearing geography;
- strategic regional relationship;
- meaningful connected geographic system.

POTENTIAL

The terrain creates a strong opportunity structure but the actual
gameplay-bearing payoff is not currently present.

Example:

    coast
    → strong maritime network
    → isolated strategically useful landmass
    → no current POI

This may be strong potential for later authored placement.

AUTHORED

Reserved for future strategic placement.

Examples:
- Worksite;
- lapis silo;
- wild crop patch;
- recurring animal zone;
- minor POI;
- other authored strategic feature.

This pass should NOT actually add authored payoff locations.

Only report where terrain appears structurally capable of supporting them.

--------------------------------------------------
14. MARKEDLY VALUABLE PAYOFF
--------------------------------------------------

Do not define payoff value by raw numerical score yet.

Use qualitative evidence dimensions.

A payoff may be strategically differentiated because of:

- scarcity
- concentration
- uniqueness
- projection
- contestability
- development potential
- extraction potential
- structure/POI value

The central comparison is:

    Does this relationship improve access to an opportunity that is
    meaningfully different from or stronger than what is equivalently
    available in the homeland/opening area?

A different biome alone is not automatically a payoff.

A forest is meaningful only if its geography/resources/opportunities
actually matter.

A mountain is meaningful only if it meaningfully changes access,
resources, traversal, or strategic position.

--------------------------------------------------
15. RELATIONSHIP COMPLETION STATE
--------------------------------------------------

Add three diagnostic completion states:

COMPLETE

    Handoff ✓
    Affordance ✓
    Reach ✓
    Payoff ✓

The natural world already provides a coherent opportunity relationship.

STRUCTURALLY_SUPPORTED

    Handoff ✓
    Affordance ✓
    Reach ✓
    Payoff unresolved / potential only

The terrain provides a coherent relationship that could naturally support
future strategic placement.

INCOMPLETE

    One of Handoff / Affordance / Reach fundamentally fails.

Authored content should not be considered capable of rescuing this.

Important principle:

    Authored content may complete an existing opportunity relationship.
    It should not manufacture the geographic relationship from nothing.

GOOD authored-completion candidate:

    route
    → river corridor
    → remote junction/region
    → potential Worksite

BAD authored rescue:

    route
    → generic random plains
    → "we can put a POI here"

--------------------------------------------------
16. REACH
--------------------------------------------------

Do not replace existing continuation analysis.

Reframe it.

Reach should collect evidence appropriate to the affordance.

Conceptually:

reach {
    local_continuation,
    geographic_persistence,
    reachable_regions,
    reachable_region_types,
    reachable_features,
    water_reach,
    crossing_reach,
    intervention_dependencies,
    deep_network_reach
}

Examples:

Gateway:
    threshold → entered region → deeper geography

Transition/coast:
    coastal interface → water network → reachable shores/opportunities

Crossing:
    bank/interface → opposite geography

Destination:
    direct_interaction may require little/no onward reach

Do not use one universal Reach rule for every Handoff function.

--------------------------------------------------
17. STARTER ROUTE STOPPING PRINCIPLE
--------------------------------------------------

Add this principle to report/documentation:

    Starter Route infrastructure should stop at the point where further
    pre-established infrastructure would begin solving the opportunity
    rather than merely exposing it.

Examples:

GOOD:
    Starter Route → foothill → player handles mountain expedition

OVER-SOLVED:
    Starter Route climbs all the way to deep mountain resource region

GOOD:
    Starter Route → coast → player decides how to exploit water network

OVER-SOLVED:
    Starter Route follows coast/water path all the way to remote POI

This is diagnostic only in this pass.
Do not alter physical route lengths automatically.

--------------------------------------------------
18. COMMITMENT PROFILE
--------------------------------------------------

Add a qualitative diagnostic commitment profile.

Do not use fixed gameplay-distance bands.

Conceptually:

commitment_profile {
    access_burden,
    exploitation_burden,
    return_burden,
    infrastructure_dependency,
    exposure
}

Suggested qualitative states:
- low
- medium
- high
- unresolved

These are descriptive.

Starter infrastructure should generally reduce access burden without
eliminating the inherent commitment of the downstream opportunity.

Example:

    road → coast

may be low access burden,
while

    coast → distant island payoff

may remain medium/high expedition commitment.

--------------------------------------------------
19. OPENING OPPORTUNITY SET
--------------------------------------------------

For each homeland, analyze the currently selected three handoffs jointly
as an Opening Opportunity Set.

Do not reselect them.

The purpose is to diagnose whether they provide non-redundant reasons
to leave the homeland.

Evaluate diversity across:

A. GEOGRAPHIC DIFFERENTIATION
    different regions / macro relationships

B. AFFORDANCE DIFFERENTIATION
    threshold, corridor, network, crossing, direct interaction, etc.

C. PAYOFF DIFFERENTIATION
    extraction, development, settlement/structure, projection,
    resource access, strategic position, etc.

D. COMMITMENT DIFFERENTIATION
    different access / expedition burden profiles

Do NOT require:
- exactly one west/center/east route;
- exactly one Gateway/Junction/Destination/Transition;
- one route per class/archetype;
- equal payoff;
- equal distance;
- equal combat exposure.

Use a redundancy diagnostic:

    If one relationship disappeared, would the homeland lose a meaningful
    opening opportunity that the other two do not substantially reproduce?

Report:
- redundant
- partially overlapping
- complementary
- strongly differentiated
or equivalent clear states.

--------------------------------------------------
20. SAME-TEAM RELATIONSHIPS
--------------------------------------------------

For relationships within the same homeland, report whether pairs are:

- overlapping
- complementary
- connected
- substitutes

Definitions:

OVERLAPPING
    share much of the same downstream geography/payoff.

COMPLEMENTARY
    different opportunities that interact productively.

CONNECTED
    begin differentiated but naturally intersect/network farther outward.

SUBSTITUTES
    provide largely interchangeable strategic access.

A pair may have more than one label if appropriate.

Do not use this to modify route selection in this pass.

--------------------------------------------------
21. OPPONENT RELATIONSHIPS
--------------------------------------------------

Analyze North/South Opportunity Relationship interactions.

Useful labels:

- exclusive
- parallel
- shared
- contested

Also distinguish:

SPATIAL CONVERGENCE
    traversal/reachable geographic components intersect.

OPPORTUNITY CONVERGENCE
    two relationships lead toward the same strategic region,
    movement system, resource opportunity, or POI.

Opportunity convergence may occur before literal route/path overlap.

Examples:

North coast
    ↓
shared eastern maritime system
    ↑
South coast

North mountain gateway
    ↓
deep western resource region
    ↑
South mountain gateway

North interior
    ↓
central village
    ↑
South interior

Report both spatial and opportunity convergence where supportable.

--------------------------------------------------
22. TOPOLOGY PHASES
--------------------------------------------------

Relate the Opportunity Set diagnostically to the existing phases:

1. DIFFERENTIATION
2. NETWORKING
3. CONTESTATION

Interpretation:

DIFFERENTIATION
    opening opportunities are meaningfully distinct near the homeland.

NETWORKING
    same-team branches begin to connect through natural geography or
    plausible future infrastructure.

CONTESTATION
    North/South relationships begin to concern the same strategic
    geography/opportunity systems.

Do not invent target distances.

Report observed topology only.

--------------------------------------------------
23. MAP-SCALE DIAGNOSTIC
--------------------------------------------------

Do not change dimensions.

Use the Opportunity Relationship analysis to report whether current
playable scale appears to spatially support:

    Homeland
        ↓
    Opening differentiation
        ↓
    same-team networking
        ↓
    opponent opportunity convergence
        ↓
    deeper strategic/resource space

Flag possible:

- compressed topology
- healthy separation
- overly diffuse topology
- unresolved

Do not turn this into a numerical recommendation unless the evidence is
exceptionally clear.

--------------------------------------------------
24. NATURAL VS AUTHORED RESPONSIBILITY
--------------------------------------------------

The report must explicitly distinguish:

NATURAL WORLD RESPONSIBILITY
- terrain affordances;
- water systems;
- thresholds;
- natural corridors;
- geographic components;
- villages/vanilla structures;
- traversal topology.

AUTHORED MAP RESPONSIBILITY
- Worksites;
- recurring animals;
- wild crop patches;
- lapis silos;
- minor POIs;
- other strategic resource placement.

The seed does not need to contain every finished gameplay opportunity.

It should provide strong opportunity structure that authored systems can
complete without fighting the terrain.

--------------------------------------------------
25. SHADOW MODE REQUIREMENT
--------------------------------------------------

This is critical.

The new model MUST NOT influence current route/candidate selection.

For every currently selected handoff, attach a shadow Opportunity
Relationship analysis.

Also analyze a useful sample of rejected candidates where feasible,
especially candidates that were:
- locally valid but strategically suspicious;
- dead ends;
- strong water features;
- villages;
- highland/forest gateways;
- redundant with another selected candidate.

Then compare:

    current destination-selection logic
vs
    Opportunity Relationship interpretation

Identify disagreements.

Do not automatically "fix" them.

--------------------------------------------------
26. FOCUS CASE: 930010639
--------------------------------------------------

Give a detailed Opportunity Relationship interpretation of all six
currently selected handoffs.

Especially North:

N1 highland approach
N2 coastal access
N3 inland-water relationship

For N1, answer:

- supported affordances;
- Gateway evidence;
- downstream payoff evidence;
- completion state;
- commitment profile.

For N2 coastal access, explicitly answer:

1. Is the coastline a usable water interface?
2. Does it provide corridor or maritime-network access?
3. What distinct shoreline/region/feature access does that water network
   provide?
4. Is there an observed markedly valuable downstream payoff?
5. If not, is there credible strategic-placement potential?
6. Is this relationship:
   - complete,
   - structurally_supported,
   - incomplete?
7. Does the previous low/zero terrestrial deep reach actually matter once
   water-network Reach is considered?
8. Is the route currently explainable as a Transition for the right reason,
   or was the previous fitter merely accepting a coastline label?

For N3 inland-water, determine whether it behaves as:
- corridor access;
- crossing;
- network access;
- local feature;
- combination.

Do not assume "inland water" is automatically useful.

Then analyze the North Opportunity Set:
- redundancy;
- complementarity;
- geographic/affordance/payoff diversity;
- likely Differentiation → Networking behavior.

Do the equivalent, more concise analysis for South.

--------------------------------------------------
27. FOCUS CASE: 930012642
--------------------------------------------------

Analyze all six current selections.

This seed previously had all selected local continuations classified as
directed.

Use it to verify that the Opportunity Relationship model does NOT punish
one-direction continuations merely for lacking branches.

A directed Gateway or directed Transition can be fully coherent.

Report:
- supported functions;
- affordances;
- Reach;
- payoff/completion;
- Opportunity Set diversity;
- major overlaps/redundancies.

--------------------------------------------------
28. FOCUS CASE: 930005557
--------------------------------------------------

Analyze all six.

Particularly inspect:

- North settlement relationship;
- highland relationships;
- inland-water relationship;
- South's multiple highland-like opportunities.

Check whether apparently different feature candidates are actually
strategic substitutes.

--------------------------------------------------
29. FOCUS CASE: 930007222
--------------------------------------------------

Inspect the previously suspicious South forest entrance.

Do not ask only whether it continues far.

Ask:

- Is there a real forest threshold?
- Does the entered forest persist as a meaningful geography?
- What useful opportunity does entering it expose?
- Is there observed payoff?
- Is there authored-placement potential grounded in terrain?
- Is it Complete / Structurally Supported / Incomplete?

This case should test whether the new model handles a locally directed
but potentially strategically empty Gateway correctly.

--------------------------------------------------
30. ALL-EIGHT SUMMARY
--------------------------------------------------

Rerun exactly the existing eight finalists:

- 930010639
- 930015734
- 930012642
- 930006815
- 930019528
- 930005557
- 930016664
- 930007222

Do not generate any additional seeds.

For every selected Handoff, report:

- seed
- team
- slot/current selected ID
- feature kind
- Handoff Depth
- Travel Cost
- local continuation class
- supported affordances
- Gateway state
- Junction state
- Destination state
- Transition state
- completion state
- observed payoff summary
- potential payoff summary
- commitment profile
- concise Opportunity Relationship description

For each homeland also report:

- Opportunity Set diversity;
- redundancies/substitutes;
- same-team connectivity;
- projection coverage;
- whether there is at least one credible pathway toward eventual
  opponent contestation;
- unresolved authored-placement dependencies.

--------------------------------------------------
31. DO NOT CREATE HARD QUOTAS
--------------------------------------------------

This pass must NOT introduce hard rules such as:

- one Gateway per team;
- one Transition per team;
- one Destination per team;
- one water route per team;
- one western / one central / one eastern route;
- exactly three different payoff categories;
- equal commitment tiers.

Functional diversity is a set-level diagnostic, not a lane template.

--------------------------------------------------
32. DO NOT CONFUSE AFFORDANCE WITH VALUE
--------------------------------------------------

Explicitly guard against these false equivalences:

    water exists
    != useful Transition

    terrain continues
    != useful Gateway

    branch count >= 2
    != useful Junction

    visually notable terrain
    != Destination

    mobility
    != strategic value

Connectivity derives strategic value from what it makes accessible.

--------------------------------------------------
33. DOCUMENTATION
--------------------------------------------------

Create a concise design/implementation document in the appropriate
worldgen/spec/results location explaining:

- Opportunity Relationship;
- Handoff;
- Affordance;
- Reach;
- Payoff;
- completion states;
- Gateway/Junction/Destination/Transition;
- Opening Opportunity Set;
- spatial vs opportunity convergence;
- natural vs authored responsibility;
- Starter Route stopping principle.

Keep terminology consistent with existing project docs.

Do not silently canonize speculative implementation thresholds.

Mark provisional analytical heuristics clearly.

--------------------------------------------------
34. TESTS
--------------------------------------------------

Add focused tests where practical.

At minimum test conceptual behavior for:

A. COAST WITH NO PAYOFF

    usable coast
    → large water
    → no meaningful downstream opportunity

Expected:
    Transition affordance may be supported
    payoff insufficient
    relationship not naturally Complete

B. COAST WITH DOWNSTREAM PAYOFF

    coast
    → navigable water network
    → distinct valuable shore/POI

Expected:
    Transition supported/strong
    coherent Reach
    Complete if payoff observed

C. STRUCTURALLY SUPPORTED MARITIME CASE

    coast
    → strong network
    → strategically distinct remote region
    → no current POI

Expected:
    strong structural relationship
    payoff potential
    Structurally Supported

D. FOREST STRIP

    threshold
    → tiny forest
    → equivalent terrain

Expected:
    weak/unsupported Gateway evidence

E. PERSISTENT FOREST GATEWAY

    threshold
    → sustained forest interior
    → useful resource/geographic opportunity

Expected:
    Gateway supported

F. FALSE JUNCTION

    two geometric branches
    → same terrain
    → rapid reconvergence

Expected:
    not strong Junction evidence

G. REAL JUNCTION

    branch 1 → mountain system
    branch 2 → river corridor

Expected:
    Junction supported

H. DESTINATION

    route arrival envelope
    → village

Expected:
    Destination supported/strong

I. AUTHORED RESCUE REJECTION

    generic terrain
    → no meaningful affordance/reach
    → hypothetical POI

Expected:
    Incomplete, not Structurally Supported

J. HISTORICAL ROUTE INDEPENDENCE

Scrambling/deleting historical Route geometry must not alter Opportunity
Relationship results except fields explicitly derived from current
destination-first geometry.

Retain all existing tests.

--------------------------------------------------
35. IMPLEMENTATION CAUTION
--------------------------------------------------

Do not overfit this pass.

The goal is to represent and inspect the model, not finalize balance
numbers.

If implementation requires provisional thresholds for:
- water-network extent;
- region persistence;
- branch distinction;
- arrival-envelope clustering;
- payoff comparison;
- commitment classification;

then:

1. derive them from existing sampling/worldgen scales where possible;
2. keep them diagnostic;
3. document them explicitly;
4. do not promote them to gameplay-design constants;
5. include sensitivity concerns in the report.

--------------------------------------------------
36. REQUIRED REPORT QUESTIONS
--------------------------------------------------

The final report must answer:

1. Does Opportunity Relationship analysis explain the previously
   suspicious selections better than the destination-only model?

2. Does 930010639 North coastal access have a defensible Transition
   relationship once water-network Reach and payoff are considered?

3. Are there selected candidates that currently look locally valid but
   strategically empty?

4. Are there rejected candidates that look strategically promising once
   opportunity structure is considered?

5. How often are selected candidates:
   - Complete
   - Structurally Supported
   - Incomplete

6. How much of the current route set depends on future authored payoff
   placement?

7. Are any homeland sets strongly redundant?

8. Do the eight seeds differ meaningfully in Opportunity Set quality even
   when they all satisfy current geometric selection?

9. Does the current map scale generally preserve:
   Differentiation → Networking → Contestation?

10. Which analytical signals appear useful enough to eventually influence
    selection, and which remain too noisy/provisional?

Do not change selection based on the answers.

--------------------------------------------------
37. VERIFICATION
--------------------------------------------------

Explicitly verify:

- no new seeds generated;
- no Minecraft worlds modified;
- no Minecraft worlds materialized;
- no Task C;
- map dimensions unchanged;
- current selected Handoffs unchanged;
- current destination scoring/eligibility unchanged;
- local continuation logic unchanged;
- deep-network logic unchanged except additive diagnostic use;
- terrain-region segmentation unchanged;
- historical Route geometry remains absent from solver inputs;
- previous analytical outputs preserved;
- new outputs written separately;
- full test count and result.

--------------------------------------------------
38. OUTPUTS
--------------------------------------------------

Produce:

1. implementation;
2. tests;
3. new all-eight shadow-analysis results;
4. machine-readable Opportunity Relationship diagnostics;
5. human-readable report;
6. concise terminology/design note;
7. commit;
8. push.

Return:

- pushed commit SHA;
- files changed;
- tests/result;
- new result directory;
- concise summary of major findings;
- explicit answer on whether another design pass is needed before allowing
  Opportunity Relationships to influence selection.

STOP THERE.

Do not:
- reselect routes;
- build physical greyboxes;
- alter map dimensions;
- add authored POIs;
- proceed to Task C.

NEXT DECISION AFTER THIS PASS

We will review the shadow analysis manually.

Only after that review should we decide whether Opportunity Relationship
evidence replaces or augments the current destination-set selection logic.