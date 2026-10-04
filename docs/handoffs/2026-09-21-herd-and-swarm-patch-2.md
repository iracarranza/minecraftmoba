REGENERATIVE OPPORTUNITY ARCHITECTURE + LIFECYCLE HANDOFF

STATUS / INTENT

Implement the regenerative-resource architecture around the existing canonical
three-level spatial model and the concurrent WORKING lifecycle/timing direction.

Do NOT invent a new fixed-node model.

The relevant canonical distinction already exists:

    Candidate Density
    -> Regenerative Eligibility
    -> current manifestation

Regenerative opportunity is a known recurring world possibility whose current
manifestation must still be sought. Known ecology does not imply exact current
location, readiness, convenient access, ownership, etc.

The previous implementation flattened these concepts into `origin + radius`.
That collapse is rejected.

The purpose of this pass is to establish the architectural boundaries needed for
dynamic manifestations, explicit lifecycle/provenance, and later temporal tuning
WITHOUT silently canonizing unresolved content predicates or balance values.


======================================================================
1. OPPORTUNITY REGION ARCHITECTURE
======================================================================

Use the existing canonical Candidate Density -> Regenerative Eligibility ->
current-manifestation distinction rather than introducing a new node model.

A Regenerative Opportunity Region is a coarse authored ecological search region
derived from the map/worldgen analysis cells that justified the opportunity.

It is NOT:

- origin + radius;
- a visible gameplay zone;
- a prebuilt pen/farm/arena;
- a fixed manifestation point;
- a permanently enumerated set of spawn nodes.

Preserve the optimizer's selected cell geography as the coarse persistent
identity of existing opportunities where the evidence supports doing so.

If an opportunity needs multiple adjacent cells to represent the ecology that
justified it, the Region may consist of multiple cells. Do not assume every
opportunity must be one cell merely for implementation convenience.

Do not "sprinkle nodes" inside the Region merely to satisfy a target density or
percentage. The Region establishes where the ecological opportunity may be
realized; it does not enumerate permanent resource nodes.


======================================================================
2. OWNERSHIP BOUNDARY
======================================================================

MAP / WORLDGEN owns:

- establishing that the regenerative opportunity exists;
- recording its coarse authored Opportunity Region;
- recording its resource/ecological identity;
- preserving the analysis/evidence that justified its selection.

RUNTIME / PLUGIN owns:

- evaluating CURRENT Minecraft terrain inside that Region;
- deriving currently eligible manifestation loci;
- selecting a locus;
- creating the finite current manifestation;
- explicitly tracking membership/provenance;
- tracking manifestation lifecycle state;
- determining when that manifestation has been resolved according to the
  resource-kind completion interface;
- recovery progress;
- querying the current terrain again before each subsequent manifestation.

Do not precompute permanent exact spawn positions.

Player modification of the Minecraft world must be capable of changing which
loci are currently eligible WITHOUT changing the underlying Opportunity
Relationship / authored Region.


======================================================================
3. REQUIRED ARCHITECTURAL PIPELINE
======================================================================

Create an explicit boundary/interface for:

    Opportunity Region
        -> query CURRENT eligible loci
        -> select manifestation locus
        -> create finite manifestation

Conceptually:

    Region
      --query current world-->
    EligibleLoci(t)
      --selection-->
    Locus(t)
      --manifest-->
    Manifestation(t)

After that manifestation is resolved and recovery completes:

    Region
      --query CURRENT, potentially modified world-->
    EligibleLoci(t+1)
      --selection-->
    Locus(t+1)
      --manifest-->
    Manifestation(t+1)

There is deliberately NO requirement that:

    Locus(t) == Locus(t+1)

The recurring thing is the OPPORTUNITY, not necessarily an identical resource
pile at identical coordinates.

Keep exact eligibility predicates and locus-selection weighting configurable /
WORKING unless already canonized elsewhere.

If concrete predicates are necessary to exercise the architecture, use minimal,
clearly labelled Alpha fixtures and tests. Do not promote those fixture rules to
canon.


======================================================================
4. NO-ELIGIBLE-LOCUS FAILURE CASE
======================================================================

If a Region currently contains no eligible manifestation locus:

DO NOT:

- fall back to the old authored origin;
- force-spawn the resource;
- modify terrain;
- silently expand the Region;
- silently weaken eligibility predicates;
- treat the previous manifestation locus as permanently valid.

Instead preserve explicit lifecycle state and report the lack of current
eligibility for diagnostics.

Distinguish:

    RECOVERING

from:

    READY / UNMANIFESTED / WAITING_FOR_ELIGIBLE_LOCUS
    (exact state name may follow project conventions)

If recovery is incomplete, the Opportunity remains Recovering.

If recovery is complete but there is no valid current locus, the Opportunity is
ready to manifest but remains unmanifested until runtime subsequently finds a
valid locus.

Do not continue accumulating recovery, queue missed generations, or imply that
the Opportunity has multiple manifestations waiting in reserve.


======================================================================
5. MANIFESTATION LIFECYCLE
======================================================================

Recovery belongs to the Regenerative Opportunity / manifestation lifecycle,
NOT to a permanent node or previously selected locus.

Working lifecycle:

    Opportunity exists
        ->
    initial manifestation becomes temporally eligible
        ->
    query current eligible loci
        ->
    select locus
        ->
    finite current manifestation exists
        ->
    manifestation remains available while unresolved
        ->
    full harvest / clear / resource-specific resolution
        ->
    current manifestation ends
        ->
    Opportunity enters recovery
        ->
    recovery completes
        ->
    query CURRENT eligible loci again
        ->
    select locus
        ->
    next finite manifestation

Important:

1. DO NOT top up a partially harvested manifestation.

2. Partial harvest leaves the remaining finite manifestation available and does
   NOT begin recovery.

3. DO NOT accumulate missed manifestations.

4. An available manifestation may remain available indefinitely unless another
   already-established rule says otherwise.

5. Ignoring an available manifestation therefore does not destroy its current
   stock, but prevents the Opportunity from beginning another productive cycle.

6. Recovery begins only after the current manifestation has been fully resolved
   according to that resource kind's completion semantics.

7. Recovery completion authorizes another manifestation attempt. It does NOT
   authorize respawning at the previous locus.

8. Each new manifestation requires a fresh current-terrain eligibility query.


======================================================================
6. MEMBERSHIP AND PROVENANCE
======================================================================

Manifestation membership must be explicit.

Do NOT infer membership merely from:

- entity/resource type;
- presence inside the Region;
- presence inside a radius around the selected locus.

The implementation work already moving toward explicit spawn-time membership,
membership revocation, provenance-correct crop counting, and explicit lifecycle
state is directionally correct and should be preserved.

Nearby player-created, bred, transported, or independently naturally occurring
matching resources must not silently become members of the current
manifestation.

Likewise, resources that cease to satisfy whatever eventual capture/removal
semantics apply must not be mutated, deleted, teleported, or otherwise modified
merely to preserve membership.

Membership revocation should remove system ownership/marking and otherwise leave
the Minecraft object/entity alone unless another explicit rule applies.


======================================================================
7. RESOURCE-SPECIFIC RESOLUTION
======================================================================

The architecture needs a resource-kind-specific interface for determining:

    "Has this finite current manifestation been fully resolved?"

Do NOT silently canonize exact predicates during this architectural pass.

Examples of the problem:

PATCH:
- needs a coherent definition of which finite manifested resources belong to
  the Patch and when those resources have been harvested.

HERD:
- needs a coherent definition of when manifested livestock has been harvested,
  removed, captured, or otherwise ceases to count toward the wild
  manifestation.

SWARM:
- can likely resolve when its explicitly tracked encounter membership has been
  cleared, but preserve existing canon and do not assume details not yet
  settled.

Livestock capture semantics remain unresolved.

In particular, do not accidentally define the system such that one escaped sheep
can prevent recovery forever simply because positional presence/absence is being
used as membership.


======================================================================
8. PATCH MANIFESTATION CLARIFICATION
======================================================================

Wild crop Patches ARE acceptable regenerative manifestations even where that
crop does not ordinarily generate wild in vanilla Minecraft.

"Native" does NOT mean:

    "must occur through vanilla Minecraft world generation."

Regenerative manifestations are deliberately game-authored opportunities.

The important distinction is:

    world manifestation = resource occurrence
    player Development  = productive infrastructure

Therefore a Patch should manifest as an irregular, terrain-conforming
concentration of crop/resource blocks.

It should read visually and mechanically as a valuable Minecraft resource
occurrence, NOT as an already-developed player farm.

REJECT as Patch manifestation authoring:

- fences;
- rectangular or highly uniform field layouts;
- central irrigation/water structures;
- broad pre-prepared farmland;
- terrain flattening solely to create a farm;
- other infrastructure implying that someone has already developed the site.

ALLOW:

- irregular clustered crop placement;
- gaps and variable density;
- conformity to existing terrain;
- only the minimum block-state/substrate changes necessary for manifested
  resource blocks to exist;
- sufficient local concentration to read clearly as a valuable resource
  manifestation rather than accidental isolated blocks.

Do NOT use vanilla natural-generation behavior as the test of legitimacy.

The test is:

    Does this read as a Minecraft resource occurrence,
    or as a prebuilt player-production facility?

Game-authored wild crop ecology is valid even where vanilla Minecraft would not
naturally generate that crop in that form.

Do not conflate:

    "not vanilla-natural"

with:

    "not a valid regenerative manifestation."


======================================================================
9. LEGACY HERD / FOUNDER_CROP REPRESENTATIONS
======================================================================

The existing fenced Herd pens and irrigated `founder_crop` fields are rejected
as PHYSICAL manifestations of the intended regenerative system.

This does NOT imply that the underlying selected opportunities are invalid.

Their optimizer-selected cells may remain useful as:

- analysis evidence;
- Opportunity Region seeds;
- coarse authored ecological geography.

For Herds:

- reject the prebuilt fenced-pen representation;
- preserve the underlying Opportunity Region/evidence where otherwise supported;
- future manifestations should arise from current-terrain eligibility rather
  than permanent containment geography.

For the four `founder_crop` opportunities:

- do NOT assume the selected Regions themselves are ecologically invalid merely
  because vanilla Minecraft would not naturally generate those crops there;
- preserve their underlying selected Regions/evidence where otherwise supported;
- reject their CURRENT physical authoring as irrigated player-like fields;
- treat re-authoring them as irregular wild Patches as the intended migration
  direction.

The invalid distinction is therefore:

    CURRENT developed-farm physical representation = rejected

NOT:

    game-authored wild crop opportunity = rejected

An irrigated rectangular field with prepared farmland and a central water column
reads as player Development / productive infrastructure.

An irregular terrain-conforming concentration of game-authored crop/resource
blocks can legitimately read as a world manifestation.


======================================================================
10. MAP MIGRATION / FREEZE SAFETY
======================================================================

Do NOT silently re-author or re-freeze the Alpha template.

Before any freeze-changing work, report exactly:

- which fenced pens would be removed/changed;
- which founder_crop fields would be removed/changed;
- how the founder_crop opportunities would be re-authored as irregular wild
  Patches;
- what minimum substrate/block-state changes those Patches require;
- whether any current terrain must be restored after removal of farm-like
  infrastructure;
- what, if anything, would replace current physical Herd containment;
- which optimizer-selected cells/analysis records remain as Opportunity Region
  evidence;
- which generated/template artifacts would change;
- which map/worldgen validation passes must be rerun;
- which downstream snapshots/tests/artifacts require regeneration.

The four existing founder_crop fields should NOT simply remain as irrigated
fields and be relabelled "wild Patches."

Their underlying Opportunity selections may remain valid.

Their PHYSICAL manifestation needs re-authoring toward irregular,
terrain-conforming resource occurrence.

Until an explicit template migration/re-freeze decision is made, legacy
geography may remain physically present, but must not quietly become the intended
architectural model.


======================================================================
11. TEMPORAL MODEL
======================================================================

Do not make regenerative architecture dependent on hardcoded absolute-minute
values.

Use a Temporal Phase, P, as the design-relative timing unit.

    P = one day OR one night

The current candidate is:

    P ~= 6 minutes

but 6 minutes is WORKING calibration, not structural canon.

This allows the design to test a 10-minute phase or another cadence without
rewriting regenerative-system relationships.

Current WORKING temporal candidates:

INITIAL AVAILABILITY:
    first regenerative manifestations begin becoming available at
    approximately 0.4P

    Current P=6m equivalent:
    approximately 2:30

BASELINE DAYTIME RECOVERY:
    approximately 0.33P after full resolution

    Current P=6m equivalent:
    approximately 2:00

PEACEFUL / NON-HOSTILE NIGHT RECOVERY:
    current lighter candidate:
    approximately 0.46P effective recovery time

    Current P=6m equivalent:
    approximately 2:45

    Equivalent recovery rate:
    approximately 0.73x daytime baseline

STRONGER NIGHT-SLOWDOWN ALTERNATIVE:
    approximately 0.5P

    Current P=6m equivalent:
    approximately 3:00

    Equivalent recovery rate:
    approximately 0.67x daytime baseline

The 0.46P / ~0.73x candidate is currently preferred for testing over the
stronger 0.5P candidate, but neither should be promoted to canon.

Do not assume every resource kind ultimately shares exactly these values.


======================================================================
12. RECOVERY ACROSS TEMPORAL-PHASE TRANSITIONS
======================================================================

Where practical, represent recovery as progress/rate rather than selecting one
fixed duration at the instant a manifestation is resolved.

This prevents arbitrary behavior such as:

    harvest at 5:59 -> gets full daytime timer
    harvest at 6:01 -> gets full nighttime timer

Instead, recovery can accumulate according to current world state.

Conceptually:

    daytime recovery rate = 1.0 baseline

    peaceful nighttime recovery rate ~= 0.73 baseline
    (WORKING candidate)

Therefore a manifestation resolved shortly before sunset can accumulate some
daytime recovery progress, then continue more slowly after sunset.

Likewise, a recovery spanning sunrise can accelerate for its remaining progress.

Exact implementation should remain configurable and should not make the current
6-minute calibration structurally special.


======================================================================
13. SWARMS AND NIGHT
======================================================================

Do not automatically apply the peaceful-node night model to Swarms.

Current design direction:

- peaceful/resource renewables become somewhat slower to regenerate at night;
- Swarms uniquely become upgraded/changed at night.

The purpose is to shift the relative opportunity landscape:

DAY:
    comparatively higher peaceful renewable throughput

NIGHT:
    comparatively lower peaceful renewable throughput
    + upgraded hostile regenerative opportunities
    + other nighttime strategic changes

Do not silently settle:

- exact Swarm compositions;
- exact night upgrade tables;
- exact reward multipliers;
- whether every Swarm changes;
- exact time-of-day weighting.

Those remain content/balance work.


======================================================================
14. DESIGN PURPOSE: CAMP + SOFT-WAVE ECONOMY
======================================================================

Regenerative opportunities are intended to serve both:

1. CAMP-LIKE FUNCTION

Recurring spatial economic opportunities that reward:

- routing;
- knowledge of ecology;
- repeated exploitation;
- map control;
- invasion/appropriation;
- resource-specific planning.

2. SOFT-WAVE FUNCTION

Recurring local economic obligations where resolving current productive state
creates downtime before the next opportunity.

The wave-like pressure comes from TURNOVER OPPORTUNITY COST.

It does NOT require:

- resources despawning because players were late;
- arbitrary decay;
- global synchronized waves;
- forced attendance;
- stockpiling multiple missed generations;
- fixed pads;
- resources marching toward structures.

If an available manifestation is ignored, its current finite stock remains
available.

But because it has not been resolved:

    recovery has not begun
    ->
    later manifestation has not begun
    ->
    productive turnover has been delayed

This is the intended opportunity cost.

A player can therefore rationally choose to leave a manifestation unresolved,
but doing so sacrifices future production tempo rather than deleting current
wealth.


======================================================================
15. RELATIONSHIP TO THE MACRO TEMPORAL CADENCE
======================================================================

Current WORKING pacing hypothesis:

    0 -> ~0.4P
        shared Bootstrap / establishment

    ~0.4P
        first regenerative manifestations begin appearing

    ~0.4P -> 1P
        first renewable economy develops while extraction and other opening
        activity remain incomplete

    1P
        first sunset / macroeconomic world-state transition

    1P -> 2P
        night economy:
        peaceful recovery somewhat slower;
        Swarms upgraded/changed;
        Worksite/contest opportunities and surface risk may change;
        extraction remains available and valuable

    2P
        daylight returns and relative opportunity values shift again

This is NOT intended as rigid game phases.

The design purpose is for independent systems to reach meaningful-but-incomplete
states around the same broad macro transition.

At first sunset, the desired question is NOT:

    "Everyone stop what you're doing and go to the objective."

It is:

    "The relative values of the things you could already be doing have changed.
     Given your current work, location, resources and team needs, should you
     respond?"


======================================================================
16. INITIAL MANIFESTATION TIMING
======================================================================

"Approximately 0.4P" does NOT imply every Opportunity must manifest on the same
global tick.

Preserve architectural room for:

- slight initial staggering;
- resource-kind-specific timing;
- map/ecology-dependent timing;
- other later configurable policies.

Exact staggering policy remains WORKING / unresolved.

Subsequent manifestations should naturally desynchronize because recovery begins
when each individual manifestation is actually resolved.

Do not implement a universal global renewable respawn pulse unless later
explicitly chosen.


======================================================================
17. WORLD MANIFESTATION VS PLAYER DEVELOPMENT
======================================================================

Preserve the conceptual distinction:

    WORLD MANIFESTATION
        = the world presenting a finite resource opportunity

    PLAYER DEVELOPMENT
        = players creating/improving productive infrastructure

This distinction is especially important for Patches.

A wild Patch may require minimal substrate/block-state accommodation so that its
resource blocks can physically exist.

That does NOT make it player Development.

The dividing line is whether the manifestation itself introduces developed
productive infrastructure.

For example:

VALID WORLD-MANIFESTATION DIRECTION:
- irregular crops/resources;
- terrain-conforming placement;
- local substrate changes required for existence;
- no implication of organized cultivation.

PLAYER-DEVELOPMENT-LIKE / REJECTED MANIFESTATION DIRECTION:
- irrigation systems;
- prepared broad fields;
- geometric planting;
- containment structures;
- terrain reshaping whose purpose is productive optimization.

Do not make wild regenerative Patches visually indistinguishable from what a
player is supposed to create through Development.


======================================================================
18. DO NOT ADD
======================================================================

Do not add mechanics merely to force the intended economic behavior.

Specifically, do NOT add:

- arbitrary anti-group mining mechanics;
- mandatory renewable attendance;
- resource decay for missing a timer;
- global synchronized renewable pulses;
- stacked missed manifestations;
- fixed spawn pads;
- invisible refill of partial manifestations;
- forced terrain modification beyond the minimum substrate/block-state changes
  explicitly allowed for valid manifestations;
- fallback spawning at authored origins;
- role/class permission gates around ordinary harvesting;
- arbitrary "Development gets X% more crops" specialization;
- automatic night penalties beyond the explicitly configurable recovery model;
- mechanics whose only purpose is imitating League minion waves literally.

The desired specialization and macro behavior should emerge from the existing
Minecraft world, resource geography, lifecycle, temporal state, class builds,
and competing opportunity values.


======================================================================
19. CURRENT STATUS CLASSIFICATION
======================================================================

CANON / ESTABLISHED:

- Candidate Density -> Regenerative Eligibility -> current-manifestation
  distinction.
- Regenerative opportunity is recurring but its current manifestation must
  still be sought.
- Known ecology does not imply exact current location/readiness/access/
  ownership.
- Do not sprinkle nodes merely to satisfy a target percentage.
- Opportunity Region architecture described above.
- Map/worldgen owns coarse opportunity identity/region/evidence.
- Runtime owns current-terrain eligibility and manifestation selection.
- No permanent exact spawn positions.
- Player world modification can alter current eligibility without changing the
  underlying Opportunity Relationship.
- No fallback to old origin / force-spawn / silent region expansion when no
  eligible locus exists.
- Existing fenced pens and irrigated founder_crop fields are rejected as the
  intended PHYSICAL manifestation model.
- Wild game-authored crop Patches are legitimate regenerative manifestations
  even where vanilla Minecraft would not naturally generate those crops.
- World manifestation and player Development are distinct: resource occurrence
  versus productive infrastructure.
- No silent Alpha-template re-freeze.

IMPLEMENTED / DIRECTIONALLY CORRECT FROM CURRENT WORK:

- explicit manifestation membership at spawn;
- membership revocation without mutating the underlying entity/object;
- one manifestation at a time;
- provenance-correct crop counting;
- explicit lifecycle state.

WORKING DESIGN DIRECTION FROM CONCURRENT SYSTEMS WORK:

- full resolution -> recovery -> fresh eligibility query -> next manifestation;
- partial manifestations do not top up;
- unresolved manifestations do not accumulate later generations;
- Temporal Phase P as the relative timing unit;
- first manifestations around 0.4P;
- daytime recovery around 0.33P;
- slower peaceful recovery at night;
- current lighter night candidate ~0.46P / ~0.73x recovery rate;
- stronger alternative ~0.5P / ~0.67x;
- recovery progress responds continuously to day/night state where practical;
- Swarms uniquely receive nighttime upgrades/changes;
- regenerative economy intended to function as both camps and soft waves;
- turnover opportunity cost rather than decay creates wave-like pressure;
- founder_crop migration direction is irregular terrain-conforming wild Patch,
  not deletion of the underlying regenerative opportunity.

UNRESOLVED / DO NOT SILENTLY SETTLE:

- exact terrain eligibility predicates by resource kind;
- exact locus-selection weighting;
- exact geometry details beyond the coarse authored Region architecture;
- exact initial-manifestation staggering;
- exact final phase duration (6m vs 10m etc.);
- exact recovery values;
- exact peaceful nighttime slowdown;
- exact Swarm nighttime behavior;
- exact Patch completion predicates;
- exact minimum substrate transformations allowed for each Patch resource;
- exact Herd capture/removal semantics;
- exact manifestation displacement rules;
- exact population/capacity/species tables;
- exact Development interaction with regenerative recovery/productivity;
- whether Development affects recovery speed, manifestation quantity, maturation,
  eligibility, or another aspect;
- exact Worksite interaction/timing;
- exact Alpha-template re-authoring.


======================================================================
20. IMMEDIATE IMPLEMENTATION TARGET
======================================================================

Implement only enough to establish and test the architecture:

    authored Opportunity Region
        ->
    runtime current-terrain eligibility query
        ->
    selected current locus
        ->
    explicitly tracked finite manifestation
        ->
    explicit resolution boundary
        ->
    recovery state/progress
        ->
    fresh current-terrain eligibility query
        ->
    next manifestation

Use minimal Alpha fixtures wherever unresolved content rules are required.

Tests should establish at minimum:

- Region and manifestation locus are distinct concepts.
- A manifestation can occur at a different valid locus on a later generation.
- World modification can invalidate one locus and make another eligible.
- No eligible locus produces explicit unmanifested state, not fallback spawning.
- Partial harvest does not refill.
- Partial harvest does not begin recovery.
- Full resolution begins recovery.
- Recovery completion does not itself force manifestation when no locus is
  eligible.
- No missed-generation backlog accumulates.
- Non-member matching entities/resources do not count toward manifestation
  membership.
- Membership revocation does not mutate/delete the underlying Minecraft entity.
- A Patch can validly manifest a game-authored crop even where vanilla natural
  generation would not produce that crop.
- Patch fixture geometry is irregular / terrain-conforming rather than a
  pre-developed rectangular farm.
- Patch manifestation does not introduce fences, irrigation infrastructure,
  broad prepared farmland, or terrain flattening merely to create a farm.
- Only minimum substrate/block-state accommodation required for the manifested
  resource is permitted by the fixture.
- Recovery parameters are expressed relative to/configurable from Temporal Phase
  rather than hardcoding the current six-minute hypothesis.
- Changing temporal state during recovery can alter remaining recovery rate if
  the progress/rate implementation is included in this pass.
- Swarm versus peaceful-resource temporal behavior can be configured separately,
  without yet canonizing exact values.

Before touching frozen Alpha geography, stop and report the required map changes
and validation/re-freeze consequences.