It comes from the match
is it stronger to actually identify this as "it comes from rational actors within a match" ? next questions?

Yes. “It comes from rational actors within a match” is stronger and more precise than “it comes from the match.”

The distinction is important because the match does not dynamically assign clay a value. Players do. The systems establish constraints and opportunities; rational actors produce the changing valuation.

SYSTEMS establish:
resource properties
geography
acquisition difficulty
inventory constraints
XP rewards
renewal
team systems
current world state

        ↓

MATCH STATE establishes:
current stocks
current infrastructure
current objectives
known information
player locations
current threats
remaining time

        ↓

RATIONAL ACTORS evaluate:
"What is this opportunity worth to us right now?"

        ↓

PLAYER BEHAVIOR
mine / ignore / mark / cache / contest / transport
So I would actually make this a design doctrine:

Where possible, resource relevance should emerge from rational actors responding to match state rather than from the game explicitly declaring which resources are currently relevant.

That is stronger because it gives us a test for new mechanics. If we find ourselves adding:

"Clay is currently low-value: -75% XP"
we should first ask why rational actors aren't already choosing to ignore it.

If the answer is that our systems make clay irrationally mandatory—too much XP, too dense, zero opportunity cost—that's probably a balance problem upstream, not something requiring a relevance modifier.

It also preserves imperfect/noncompetitive play. A player is still allowed to mine all the clay. The game doesn't invalidate their action. A better player simply recognizes when their time has higher marginal value elsewhere.

That gives us the next questions
I think we've now defined enough qualitative machinery that the next questions should start forcing quantities out of it, in roughly this order:

What exactly constitutes Acquisition Difficulty? We have Search, Reach/Access, Exploitation, and perhaps Hazard. We need to determine which are intrinsic to an opportunity and which emerge from geography/player state. For example, 200 blocks of distance shouldn't necessarily be encoded into a copper vein's XP if the player happened to approach it from elsewhere.

How does Extraction XP recognize that difficulty without omnisciently scoring arbitrary player inconvenience? Search is particularly difficult. If I accidentally expose diamond while digging toward something else, the game can't reasonably know I spent “zero search effort,” whereas another player may have prospected for two minutes. We need a measurable proxy for the accomplishment rather than an invisible evaluation of effort.

Which resources actually need regenerative treatment? We now have enough criteria to complete the matrix rather than assume “more things should regenerate.” Copper probably yes; diamond probably no; clay is interesting; sand may simply be naturally abundant enough that regeneration is unnecessary.

For regenerative resources, what is the relationship between manifestation size, active density, and renewal interval? This is the quantitative version of the clutter problem:

large + rare + slow renewal
vs
small + common + faster renewal
Different resources should occupy different positions.

How much opportunity can a region sustain before rational selection collapses into vacuuming? This is where we can finally calculate Opportunity Saturation for an Extractor rather than reasoning abstractly.

What should the first 6–10 minutes actually contain? Once we know the resource matrix and approximate acquisition profiles, we can generate a synthetic opening region and ask what a Lv1/Lv3/Lv6 player rationally does. That will simultaneously pressure-test food, inventory, regenerative density, XP, depth, and specialization.

I think #1–2 are the immediate blockers. In particular, there's a potentially important distinction hiding inside Acquisition Difficulty:

RESOURCE-INTRINSIC DIFFICULTY
    vein geometry
    hardness/tool requirement
    exposure
    deposit dispersion
    associated hazard

WORLD-POSITION DIFFICULTY
    regional depth
    terrain
    cave access
    distance
    strategic exposure

PLAYER-CONTINGENT DIFFICULTY
    where I happen to be
    what tools I have
    what I know
    my inventory
    enemies currently nearby
My instinct is that Extraction XP should principally recognize the first two, while rational actors internalize all three. Otherwise XP starts trying to compensate a player for every circumstantial inconvenience they personally encounter, which gets both opaque and exploitable.

That is the next distinction I'd resolve before putting actual XP values on the renewable-resource matrix.

yes, 1-2 your distinctions are accurate, and world position is already partially scored by how worldgen is trying to determine early POIs and access as a quantity

Yes. That means we probably shouldn't invent a second spatial-difficulty model for Extraction at all. Worldgen already has—or is developing—the relevant concept: it evaluates how difficult locations/opportunities are to access from the early match state. Extraction should consume that information rather than independently reconstruct it.

I would formalize the split as:

ACQUISITION SIGNIFICANCE

A. Resource-Intrinsic Difficulty
   What makes this manifestation difficult to extract?
   - exposure / concealment
   - vein geometry
   - dispersion
   - required tool tier
   - block-breaking burden
   - associated intrinsic hazards

B. World-Position Difficulty
   What makes this manifestation difficult to reach?
   - regional depth
   - path distance
   - elevation / terrain impedance
   - cave accessibility
   - water / verticality
   - strategic exposure
   - POI/access relationships

C. Player-Contingent Difficulty
   What makes it difficult for THIS player RIGHT NOW?
   - current position
   - tools
   - Hunger
   - inventory
   - information
   - enemy presence
   - class
   - current mission

XP can recognize A + appropriate portions of B.
Rational actors evaluate A + B + C.
The important consequence for worldgen
If worldgen already assigns some notion of access quantity/value to early POIs and opportunities, then renewable resources should probably participate in that same spatial evaluation.

That gives us something much stronger than:

copper = 10 XP
iron = 8 XP
We can have:

Resource Opportunity
    ├── material/economic profile
    ├── intrinsic acquisition profile
    └── worldgen access profile
and derive its expected Extraction significance from the combination.

A copper vein at regional depth A beside a Route shouldn't be progression-equivalent to a comparable copper vein embedded in difficult depth-C mountain geography. Yet we don't need to observe what the player actually did to reach either one. The world already knows what kind of opportunity it generated.

That seems like the correct abstraction boundary.

The search problem becomes easier too
I was worried about measuring “the player spent two minutes prospecting.” We probably shouldn't.

Search difficulty can mostly be encoded in the opportunity's generated discoverability:

high exposure
→ low search difficulty

partially exposed / strong environmental clues
→ moderate

concealed but prospectable
→ high

requires significant cave/geological investigation
→ higher
Then:

XP recognizes the difficulty of the opportunity the player successfully resolved, not a stopwatch recording how inefficiently the player personally searched for it.

So two players can resolve the same difficult deposit differently:

Player A:
excellent knowledge/pathing
→ finds it in 35 sec

Player B:
wanders
→ finds it in 2:40

same opportunity significance
→ approximately same underlying XP value
That's desirable. Efficiency itself is rewarded by XP/hour.

Player A gets the same accomplishment faster and can go do another thing. We don't need an explicit efficiency multiplier.

That is particularly appropriate for Extraction:

Extraction skill is partly the ability to realize a fixed world opportunity more efficiently than another player.

One subtlety: world-position difficulty should probably be frozen to generation, not continuously recalculated
Suppose a difficult depth-C copper region initially requires a substantial mountain approach. Then Construction builds a bridge and Logistics establishes a Route.

If Extraction XP continuously evaluates current accessibility:

team improves infrastructure
→ copper becomes easier to reach
→ copper gives less XP
That creates a weird negative externality: teammates improving the world reduce the Extractor's progression reward.

I'd instead distinguish:

GENERATED ACCESS DIFFICULTY
= property of the opportunity/world geography

REALIZED ACCESS DIFFICULTY
= what rational actors actually experience after
  infrastructure, knowledge, position, etc.
XP can use generated access difficulty.

Player decision-making uses realized access difficulty.

Then infrastructure does what it should:

same valuable Extraction opportunity
+ better Route/access
→ same approximate accomplishment value
+ less time required
→ greater Extraction throughput
→ greater XP/hour
The Route has made the Extractor stronger economically without the game needing to give the Route an explicit “+15% Extraction XP” modifier.

That's an excellent example of our emergent-organization/integration philosophy.

This also gives renewable nodes a very clean generation constraint
When worldgen considers creating a renewable manifestation, it isn't merely asking:

Is copper eligible here?
It can ask:

1. Is copper economically/ecologically eligible here?

2. What acquisition profile would this location create?

3. Does the region currently need an opportunity in that
   acquisition/significance band?

4. Would adding it exceed active Opportunity Saturation?
So a region need not randomly produce ten trivial copper veins merely because copper is regenerative.

It might maintain a distribution such as:

LOW acquisition significance
████

MEDIUM
███

HIGH
█

VERY HIGH
rare / conditional
Those proportions are illustrative, not proposed values.

This also suggests regeneration eligibility itself can require a minimum acquisition significance for some resource classes.

For example:

COPPER:
may regenerate as low/medium/high acquisition opportunities

IRON:
perhaps medium/high favored

GOLD:
only sufficiently deep/difficult opportunities qualify

DIAMOND:
does not ordinarily enter regenerative pool
Whereas bulk Construction resources don't need that rule:

CLAY:
easy deposits are perfectly legitimate
because material demand, not Extraction progression,
is the primary reason to seek them.
That prevents us from making every renewable material artificially hard to obtain.

I think 1–2 are therefore resolved enough to advance
The doctrine coming out of them is:

Extraction progression values the generated significance of successfully acquiring a world resource: principally the resource manifestation's intrinsic extraction challenge and its world-generated access challenge. It does not reward player-specific inconvenience. Player efficiency is rewarded naturally because resolving equivalent opportunities faster increases productive throughput.

And separately:

Infrastructure and changing match state alter the realized cost and current economic value of an opportunity, not necessarily its underlying progression significance. Rational actors decide whether that opportunity remains worth pursuing.

That gives us a pretty stable basis for the next question: which resources actually belong in the regenerative economy at all.

I think we can now finish the full resource matrix—crops, animals, trees/plants, minerals, bulk geology, aquatic resources, and likely Nether/End materials—and for each make an initial call of finite / naturally player-renewable / world-regenerative / special-location regenerative / probably unnecessary to regenerate, plus the reason. That should reveal what the regenerative world actually contains before we calculate densities.

yes, but before moving on, check repo/docs recent pushes for that worldgen behavior/calcs/etc


Worked for 49s
I checked the recent worldgen pushes, especially the immutable Opportunity Relationship shadow analysis and its governing spec. The good news is that the repo already has a stronger spatial/access framework than I was implicitly reconstructing.

What the current worldgen actually measures
The current analytical fitter already represents a handoff with fields conceptually including:

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
and the Opportunity Relationship model explicitly separates:

terrain feature
→ Handoff
→ Affordance
→ Reach
→ Payoff
→ Opportunity Relationship
That is significant for our Extraction discussion. The existing worldgen isn't just measuring Euclidean distance. It is already trying to distinguish access structure, continuation, what the terrain enables, and whether the resulting geography actually leads to a meaningful payoff.

The shadow analysis is diagnostic only right now—it does not alter candidate selection—but it already tracks the kind of information we'd want Extraction opportunities to inherit later.

The most relevant existing concept is the Commitment Profile
The spec already defines:

commitment_profile {
    access_burden,
    exploitation_burden,
    return_burden,
    infrastructure_dependency,
    exposure
}
with low / medium / high / unresolved states. Crucially, it explicitly says not to reduce this to fixed gameplay-distance bands.

That maps almost perfectly onto what we were just calling World-Position Difficulty, except it is broader and better structured.

So I would revise our terminology slightly:

RESOURCE-INTRINSIC ACQUISITION DIFFICULTY
+
WORLDGEN COMMITMENT PROFILE
=
generated acquisition significance
rather than inventing a parallel “world-position difficulty” score.

Existing commitment dimensions map cleanly
Existing worldgen quantity	Extraction interpretation
access burden	difficulty reaching the opportunity
exploitation burden	how much commitment remains after arrival
return burden	difficulty bringing value back / continuing safely
infrastructure dependency	degree to which roads, Routes, bridges, boats, etc. matter
exposure	vulnerability to interruption/contest
That is almost exactly what we need.

There is also already a strong “access is not payoff” distinction
This is particularly important.

The recent shadow analysis explicitly found that a locally coherent route/access relationship can still be strategically unexplained if it does not lead to meaningful payoff. The report calls out relationships that have structural access but no established natural payoff.

And the spec's core rule is:

Starter infrastructure should expose an opportunity, not complete the expedition.

It also requires movement transitions to lead toward a markedly valuable resource region / POI / strategic opportunity rather than merely providing movement for movement's sake.

That matters for resource generation because we shouldn't say:

difficult-to-reach place
→ therefore good Extraction opportunity
The actual chain should be:

meaningful access burden
+
actual resource payoff
+
resource-intrinsic extraction task
→ meaningful Extraction opportunity
So the worldgen model already prevents us from confusing difficulty with value.

The repo already has another useful axis: payoff differentiation
The current payoff model considers things like:

scarcity
concentration
uniqueness
projection
contestability
development potential
extraction potential
structure/POI value
and specifically asks whether an opportunity is meaningfully different from or stronger than what is equivalently available near the homeland.

That means regenerative-resource placement could eventually plug into an already-existing question:

Does putting this copper opportunity here create
meaningfully differentiated Extraction access?
rather than simply:

Is there enough copper in this map?
That is much stronger.

One correction to what I said before
I suggested that generated access difficulty might be “frozen” and then used directly for Extraction XP.

I still think the principle is right, but the repo suggests we should be more careful about what is frozen.

The shadow model's current commitment fields are descriptive and qualitative, and the repo explicitly warns against turning provisional quantities into weights, quotas, eligibility gates, or automatic authored placements yet.

So right now we should not say:

worldgen access score = 0.63
therefore copper vein = 18 XP
That would be ahead of what the fitter actually supports.

A better current design statement is:

Extraction XP should eventually inherit validated worldgen access/commitment information, but current Opportunity Relationship and Commitment Profile outputs are diagnostic evidence, not yet canonical numerical scoring inputs.

That preserves the direction without prematurely treating the current shadow analysis as a production formula.

Another useful repo concept: arrival envelopes
The current fitter deliberately avoids treating every strategic opportunity as one exact point. A Handoff can have an arrival envelope: a nearby region from which the same opportunity is practically accessible.

That is potentially very useful for regenerative resources.

A copper-bearing mountain opportunity probably shouldn't be:

exact block coordinate = opportunity
It could be:

resource-bearing geological component / envelope
        ↓
one or more current manifestations
Likewise:

riverbank clay system
forest timber system
grazing/sheep ecology
coastal kelp ecology
This supports our earlier idea that a regenerating resource should usually renew within an appropriate opportunity region, not refill the exact same physical node.

The worldgen currently distinguishes local continuation from deep reach
This is another thing we should inherit rather than reinvent.

The repo already separates:

local continuation
from:

deep-network reach
and uses local classes such as:

dead_end
directed
branching
junction
while broader deep reach remains separately descriptive.

So for resources, we can eventually distinguish:

LOCAL ACQUISITION STRUCTURE
"What does reaching this node immediately require?"

from

REGIONAL RESOURCE RELATIONSHIP
"What larger resource-bearing system does this opportunity open?"
Example:

Starter Route
→ foothill handoff
→ copper-bearing highlands
→ several possible copper manifestations deeper inside
The first copper vein is not necessarily the whole opportunity. The highland resource relationship may be the opportunity; individual veins are manifestations inside it.

That distinction is probably going to matter a lot for regen.

Current worldgen doctrine that directly affects our renewable-resource design
There are three repo principles I think we should carry forward intact.

1. Infrastructure exposes rather than solves
Starter Route
→ gives privileged access to opportunity
→ player still performs expedition
For Extraction:

Mining Outpost / Route / information
→ reduces one component of acquisition burden
→ player still performs actual resource acquisition
2. Geography must have actual payoff
interesting terrain alone ≠ valuable opportunity
Likewise:

hard-to-access geology alone ≠ valuable Extraction opportunity
There needs to be real material/systemic payoff.

3. Authored content should complete existing geographic relationships, not manufacture them from nothing
The spec states this explicitly for authored strategic content.

That strongly suggests regenerative authored resources should preferably sit in:

already coherent resource/geographic systems
rather than:

generic plains
+ spawn copper because Logistics needs copper
That is a very useful constraint for the matrix.

Revised Acquisition model after checking the repo
I would now write it as:

GENERATED EXTRACTION SIGNIFICANCE
=
Resource-Intrinsic Acquisition Profile
+
Worldgen Opportunity/Commitment Evidence
where:

Resource-intrinsic
discoverability of manifestation
vein/deposit geometry
dispersion
tool requirement
block-breaking burden
intrinsic hazard
Worldgen-provided
homeland depth
travel/access cost
access burden
exploitation burden
return burden
infrastructure dependency
exposure
Reach / continuation context
actual payoff differentiation
Explicitly NOT part of base XP significance
player happened to start nearby
player wandered inefficiently
current Hunger
current inventory
current tools beyond ordinary eligibility
current team demand
temporary enemy positioning
current infrastructure convenience
Those belong to the rational actor's realized decision, not to the generated opportunity's underlying significance.

The biggest practical implication
I think we should stop thinking of regeneration primarily as:

resource nodes
and instead think:

RENEWABLE OPPORTUNITY RELATIONSHIPS
        ↓
resource-bearing geography/ecology
        ↓
current manifestations
For example:

western highland relationship
→ copper-bearing geology
→ current copper veins

river/wetland relationship
→ sediment ecology
→ current clay beds

grassland relationship
→ livestock ecology
→ current sheep herd

deep agricultural/ecological relationship
→ founder-crop eligibility
→ current carrot patch
That fits the repo's current worldgen architecture much better than adding a free-standing resource-node spawner.

And it gives us the right next move: when we finish the resource matrix, we should add a column for what kind of Opportunity Relationship / geographic system can host this resource, rather than assigning only a generic “depth C” or “mountain” tag.

The recent pushes therefore strengthen the direction we were taking, but they also constrain us: reuse the fitter's Commitment/Reach/Payoff concepts, and do not prematurely turn the current diagnostic shadow metrics into hard XP formulas or generation weights.

