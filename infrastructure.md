# Infrastructure Design

## Infrastructure Mode and recognition

**Working canon, reconciled 10 September 2026.** At the infrastructure progression breakpoint, currently Level 6, eligible classes use shared **Infrastructure Mode**. Construction recognizes a Construct; Development recognizes a Development Zone; Exploration recognizes a Route; Logistics recognizes a Supply Line. Eligibility remains authored per class, not automatically inherited from every archetype tag. [OPEN] Combat’s corresponding Level 6 economic system remains a nighttime/combat-economy question.

Ordinary building, farming, movement, and transport remain unrestricted. Infrastructure Mode recognizes qualifying present world state or demonstrated capability; the system need not have witnessed its original creation. Build then designate, designate then build, recognize existing modified construction, and expand or repair a designated site are valid workflows. Recognition can generally be retroactive, although a connection still requires proof of its current capability.

### The four infrastructure identities

| Validation type | Extent based and organizes a place | Relation based and organizes a connection |
|---|---|---|
| Constitutive | Construct | Route |
| Facilitative | Development Zone | Supply Line |

Constructs and Development Zones are established primarily through designation and measurement of a spatial extent. Routes and Supply Lines are established primarily through demonstration or proof between locations. All four depend on physical Minecraft world state; tangible versus intangible is not the adopted axis.

Constitutive infrastructure validates its own underlying state or capability: sufficient qualifying construction for a Construct, or a demonstrated traversable connection for a Route. Facilitative infrastructure additionally validates the process and enabling conditions it supports: developmental resources and growth conditions, or transport capability and the state supporting cargo flow.

### Systemic and emergent disruption

Systemic disruption removes an objectively required condition: destroy qualifying construction, sever the only traversable bridge, remove crops or block required growth conditions, or destroy transport apparatus. Emergent disruption reduces usefulness without necessarily invalidating recognition: occupy a Construct, ambush Route users, attack farm workers, or intercept available carriers and cargo.

The system evaluates objective capability, not architectural or strategic quality. Danger, enemy control, exposure, inconvenience, and poor defense do not alone invalidate infrastructure. Recognition does not protect the underlying world from those consequences. Facilitative systems should be re-evaluated regularly; repairing their required conditions should generally restore functionality without consuming an entirely new infrastructure slot. [OPEN] Evaluation frequency, grace periods, partial degradation, and circumstances requiring renewed proof are not specified.

## Constructs

**Working.** A Construct recognizes qualifying construction investment and scale. Its independent benefit remains occupation or sustainment efficiency: the ability to maintain useful presence. Recovery, provisioning, and Hunger/exhaustion remain possible expressions, not selected mechanics. Integration is a separate benefit and cannot replace the need to define intrinsic value.

### Permissive designation

Houses, fortresses, walls, bridges, gates, towers, bunkers, platforms, fortified caves, chokepoint fortifications, and other substantial player construction can qualify. No roof, door, interior volume, enclosure percentage, room layout, predefined shape, or architectural-quality score is required. A wall is not a deficient fortress.

Recognition examines qualifying construction that exists now. [HISTORICAL] A Build Mode that only records blocks placed while active is superseded as the exclusive recognition model. [OPEN] Eligible material, natural versus modified block provenance, minimum investment, ownership, capture, and exact potency calculations remain unresolved. Destruction can reduce or invalidate recognition by removing sufficient qualifying construction; the numerical threshold remains open.

### Slots and spatial metrics

**Construct Slots** limit the number of separately recognized Constructs. **Buildable Scale** limits how much physical extent/material belongs to a recognized Construct and contributes, with qualifying investment, to scale/potency. **Operational Area**, governed by **Operational Scale**, determines where other recognized infrastructure may legally connect for integration.

Operational Area is an infrastructure connection envelope, not a general buff radius. A Development Zone must appropriately fall within or intersect it; a Route connection point and a Supply Line node must be within it. Infrastructure outside remains independently valid but cannot connect to that Construct. [OPEN] Exact intersection, distance, verticality, overlap, and boundary rules remain undefined.

Buildable Scale and Operational Scale are separable progression dimensions. To connect a separated farm, a player can increase Operational Scale or spend buildable capacity, blocks, and labor extending the Construct toward the farm. Recognition limits never forbid ordinary construction beyond those limits.

### Projection and protection

An exposed wall, slab, block, or bridge can receive full recognized potency and Operational reach if it satisfies the eventual scale requirements. Its exposure is not inherently an exploit. Spending material on projection expands physical extent and integration geography; spending it on protection or centralization creates defended working space, storage, and transport nodes. The world makes exposed players and infrastructure vulnerable.

A team can eventually achieve both projection and protection by paying the material, extraction, transportation, placement-labor, and time costs. The recognition algorithm should not penalize this investment. [OPEN] Construct balance must be calibrated against phase-specific block availability, Extraction efficiency, bulk Logistics, placement speed, class material efficiency, labor, and opportunity cost. At a relevant phase, substantial projection or substantial protection should be attainable without trivially maximizing both; additional team investment should expand that frontier.

[PROTOTYPE] Test structures at 64, 128, 256, 512, and 1024+ qualifying blocks to learn what real construction those investments permit. These are experimental bands, not approved recognition thresholds. Construction specialization must not make material acquisition, transport, labor, or time irrelevant.

## Development Zones

**Working.** Development Zone is the current working name for recognized managed productive/developmental world state within an extent. Designation and measurement recognize existing eligible resources together with the conditions facilitating development. Targeted improvement of eligible processes remains the direction; this is not a generic random-tick-speed increase.

Cultivated crops, managed animals, Crop Patches, Herds, and other renewable biological systems are candidates. [OPEN] Exact eligibility awaits the Crop Patch, Herd, and Mob Swarm foundations. No complete population system is implied by these names.

Validity responds to present conditions. Removing wheat from valid farmland or blocking required light can disrupt the productive state; an empty former farm does not necessarily retain full functionality indefinitely. Replanting or restoring required conditions should generally restore function without a new slot. [OPEN] Exact productive-state thresholds, empty-state behavior, boundaries, improvement rates, and restoration timing remain unsettled. A Development Zone remains independently establishable and useful without a Construct.

## Player recognized Routes

**Working.** A Route proves traversal through a physical connection. Terrain, roads, bridges, tunnels, waterways, and other movement infrastructure may support it. Early independent benefits still favor movement efficiency over unconditional speed. Maximum length/reach and branching/network complexity are separate progression dimensions; exact values remain open.

A dangerous, enemy-controlled, monster-infested, or ambush-prone Route remains valid if the objective traversal capability exists. Destruction of the only traversable connection can invalidate it. [OPEN] How altered paths are revalidated, whether replacement traversal requires new proof, submission details, ownership, shared use, width, and progression metrics remain unresolved.

An ordinary road, a map-authored Starter Route (maps.md), and a player-recognized Route remain distinct contexts. A canal, bridge, or tunnel may support both a Route and a Supply Line, but each requires its own proof. Route recognition never automatically grants Supply Line recognition or vice versa.

## Supply Lines

**Working.** A Supply Line is a directional logistical relationship between eligible container nodes, with performance established by demonstrated cargo transport. The sequence is Infrastructure Mode → select source container → select destination container → prove valid cargo transport → establish rated performance. Merely selecting two containers is insufficient. The reverse direction requires its own proof.

The existing upstream-storage → downstream-storage distribution model remains: actual upstream items gradually become available downstream, allowing team benefit while the Logistics player is elsewhere. The handoff clarifies that physical transport proves the capability and recognized flow may represent its continued operation. It does not require every transferred item to persist as a dropped entity along a path.

### Transport specific performance

**Transit Time** measures elapsed delivery time for the demonstrated method. **Flow Weight** standardizes its logistical capacity; ordinary player transport has baseline Flow Weight 1. Flow Weight is not simply inventory item count. Conceptually, Flow Rate is proportional to Flow Weight divided by Transit Time. [OPEN] The exact formula, units, normalization, eligible cargo, and ratings beyond the baseline are not established.

Proof demonstrates that a particular cargo-rated method can move capacity W from A to B in time T. It does not merely measure how quickly any player can reach B. Valid methods may involve players, Logistics-class or summoned carriers, camels, Allays, golems, minecarts, boats, water systems, or other engineered transport. Exceptional mobility improves Logistics only insofar as a valid rated transport method can use it while carrying its rated capacity.

[PROTOTYPE] A fast low-weight Allay, slower high-weight camel, or moderately faster high-weight golem fleet illustrates possible speed/capacity tradeoffs; these properties and numerical ratings are not canonized. Continuity or simultaneous transport may become further dimensions. Node count/network complexity and individual line reach are separate progression dimensions. [OPEN] Their values, branching rules, filtering, capacity, reliability, and future direct allied replenishment remain unspecified.

### Physical proof and represented flow

Test cargo can demonstrate water transport, a minecart trip can demonstrate rail transport, and a carrier delivery can demonstrate carriage. Subsequent flow may be simulated or represented, while the supporting physical world remains relevant. A severed rail system, obstructed waterway or bubble column, removed required carrier, or invalidated source/destination node can degrade or invalidate the line.

The system should periodically verify the physical conditions necessary for the proved capability. [OPEN] How complex paths and dependencies are remembered, validation frequency and cost, proof renewal, carrier commitment, buffers, inventory accounting, and exact degradation behavior require implementation design. This framework does not establish free item duplication or continued unchanged flow after its physical support is destroyed.

Enemies primarily contest the Minecraft systems that make transport work: nodes, vehicles, carriers, transfer points, rails, waterways, plumbing, storage, and potentially cargo/intermediate buffers. An abstract Supply Line health bar is not the primary contest model. Physical protection of those systems gives fortified Constructs Logistics value even when an exposed Construct has equal Operational reach.

### Shared geography and unusual transport

A canal can prove player traversal for a Route and independently prove cargo transport for a Supply Line. Neither requires the other. Supply Lines need not follow horizontal walking paths. Gravity-fed delivery can legitimately establish a fast one-way line if it proves valid cargo transport; reverse transport requires separate capability.

[PROTOTYPE] The handoff’s source “1000 blocks above” example expresses this vertical-transport principle, not a selected playable-map height or an approved world-height override. Actual implementation remains constrained by the chosen Minecraft world and transport system.

### Reconciliation with earlier transfer language

[HISTORICAL] The earlier uncertainty between generic gradual transfer and recognition of physical flow is narrowed by this handoff: proof establishes the rating; persistent supporting world state maintains it; represented ongoing flow need not continuously render every cargo item. Remaining accounting, revalidation, and transport-specific details stay [OPEN]. No mandatory Route, universal continuous ground path, or one required carrier type is introduced. Industrial Enchanter completion remains independent of recognized Supply Lines (objectives.md section 14A).


## Infrastructure integration and progression

A Development Zone appropriately within/intersecting Operational Area, a Route connection point within it, and a Supply Line node within it may legally connect to the Construct. Distinct connected systems contribute to the existing integration XP model in [classes.md](classes.md#infrastructure-recognition-and-integration) and [objectives.md](objectives.md#infrastructure-and-xp). A Construct alone creates neither passive XP nor an automatic XP multiplier; otherwise legitimate activity must grant XP. World opportunities encourage dispersion while integration rewards intentional concentration.

Existing T1/T2 numerical targets and class-authored progression remain unchanged. Level 6 is the current recognition entry; Level 12 expresses the class role. No automatic T1/T2 level mapping is introduced. [OPEN] Exact overlap, mixed-tier contributions, attribution, eligible activity, and night behavior remain unresolved.

## Infrastructure payback horizon

**Working canon, 10 September 2026.** Persistent infrastructure has a payback horizon. Progression can increase infrastructure potency, but advancing match state reduces remaining match time, uncontested labor, and the time available for an investment to repay its establishment cost. An early Route may repay across much of the match; a late Route needs a much more immediate tactical purpose. The same applies to Supply Lines, Development Zones, and Constructs. Late infrastructure must establish faster, produce more immediate value, repurpose existing world state, solve an immediate tactical need, or otherwise justify its shortened horizon.

## Control windows and infrastructure labor

Mid- and late-game infrastructure time is not given by the match clock. It is won through control. Objectives and strategic contests create temporary windows of access, space, safety, and labor availability during which a team can establish or expand infrastructure.

Infrastructure and contestation form a reciprocal cycle. Infrastructure improves projection, economic position, and objective control; that helps win strategic contests; winning control creates a development window; the team converts some combat presence into infrastructure labor; the infrastructure becomes more valuable; the stakes of the next contest rise; the enemy attacks or disrupts; the cycle repeats.

Progression and material abundance rise throughout the match, but discretionary infrastructure time need not. Enemy contact increases, objective contests matter more, existing infrastructure needs defending, players must rotate, and deaths remove presence. Early infrastructure is therefore more likely to be material-constrained and late infrastructure more likely to be labor- and attention-constrained. [PROTOTYPE] A working qualitative curve places scarcity at Levels 1 to 6, a construction window at 7 to 12, the likely peak intersection for large new infrastructure formation at 13 to 18, collapsing labor under still-rising throughput at 19 to 24, and reactive repair, modification, fortification, demolition and reconnection at 25 to 30. These bands are not timing canon until progression pacing is settled.

Economic players hold simultaneous combat roles. A Construction player may be the team's tank, a Development player an assassin, a Logistics player a support. Infrastructure work must therefore be naturally interruptible. The foundational interruption test for any persistent economic activity is what happens when that player suddenly needs to fulfil their combat role. The preferred answer is that the activity pauses, meaningful persistent progress remains, the player fights, and the activity can later resume. Avoid systems where leaving to help the team wipes minutes of progress or requires starting over.

Large infrastructure should be able to emerge across multiple successive control windows rather than requiring one uninterrupted construction period. Partially developed infrastructure is normal world state, not failed infrastructure: a half-built fortification already provides cover and terrain, represents invested material and labor, may support a smaller recognized Construct, can be attacked or defended, and can later be resumed. A late-game Construct can therefore record accumulated territorial success over time, and by late match the map should visibly contain the history of previous development and conflict.

Winning an initial strategic fight does not entitle a team to finish its infrastructure safely. Starting major infrastructure while the opponent retains the capability to contest it converts current territorial advantage and player labor into future advantage while exposing that investment to contest. An opponent may legitimately concede the first development window, progress elsewhere, improve equipment, attack another location, wait for labor to divide, and siege after the investment has been made. Infrastructure creates value and stakes simultaneously.

A class-design consequence follows: late Construction potency should increasingly help solve having material but very little time, not merely permit a bigger structure.

## Authorship, recognition, and control

Three concepts previously conflated should be kept separate. Authorship is who performed the action that created or developed something, and governs historical progression rewards: Construction XP, any Construction Block placement premium, XP for establishing infrastructure, and class triggers tied to performing the work. Authorship does not transfer because territory changes hands. Recognition is which infrastructure relationships currently exist, based on qualifying world state and capability. Control is which team can currently use, operate, and connect the infrastructure.

Most ongoing infrastructure benefits should depend more heavily on current functional access and control than on permanent authorship. World value can be stolen; historical progression earned by doing the work cannot.

## Capture, local value, and network value

Do not adopt a simplistic rule in which enemy presence automatically invalidates infrastructure, one won fight changes formal ownership of every block, or captured infrastructure grants the occupier the creator's historical progression. Physical utility is substantially owner-agnostic: a capturing team can immediately benefit from walls, cover, bridges, tunnels, terrain modification, accessible storage and interactables where Minecraft rules permit, farms, and other surviving world state. Capturing a developed position may therefore let a team skip some physical infrastructure creation, and that is intentional. It prevents infrastructure from being risk-free economic accumulation.

A captured hub should not transfer as a perfectly functioning integrated economic machine. This follows the established extent and relation axis: extent-based local value is more directly capturable, while relation-based network value generally requires re-establishment by the occupier. A Construct is highly physically capturable and the built world continues to exist. A Development Zone is fairly capturable in practical terms where the occupier actually performs the relevant activity. A Route's physical path remains useful, but team-specific projection and network recognition may need to be re-proved. A Supply Line is the least automatically capturable, because its defining feature is an active demonstrated relationship between nodes; capturing a destination does not cause the occupier's cargo to flow through the defeated team's network.

[OPEN] The conditions under which a captured Construct's intrinsic benefit applies to the occupier, team-relative Route re-proof, Development Zone capture and use, and Supply Line interruption and re-establishment all require specification. Do not introduce a universal capture bar or automatic infrastructure conversion system before these physical and access-based rules have been tested.

Contested positions should accumulate physical history rather than reset. A late-game objective approach may contain original walls, a breach, later fortifications, repaired sections, competing or abandoned structures, tunnels, replacement bridges, severed and reconnected Routes, abandoned storage, Development Zones that changed practical control, new Supply Line approaches, and terrain destroyed by repeated combat. The history of territorial control should become physically legible in the map. Do not introduce a universal reset-on-capture mechanic unless later playtesting demonstrates a need.

## Team-relative integration

Integration should not be evaluated only as whether a hub is fully integrated, but as how integrated it is for each team right now. The existing T1/T2 multiplier should follow active team-relative integration rather than permanent historical authorship.

Worked example. Before a siege, Team A has authored and functionally uses a Construct, a Development Zone, a Route, and a Supply Line, so A holds full integration and B none. Immediately after B wins the siege, B may physically occupy and use the fortress and exploit local productive state; A's Supply Line may cease functioning through interruption or lost access; A's Route may still physically exist without granting B its team-specific projection benefit; and B has not automatically established its own cargo flow. B therefore receives substantial local value but only partial integration, and B's infrastructure players now have meaningful work reconnecting the captured position into B's network.

This produces a deliberate intermediate outcome between capturing everything with instant full integration and gaining nothing while rebuilding the position from zero. Authorship determines progression rewards for creating infrastructure; current functional access and connection determine most ongoing infrastructure benefits.

## Construct scale gating

Construct scale should be jointly resource- and progression-gated. Physical construction itself is unrestricted: if a team invests enough resources and labor to create an enormous fortress early, the fortress physically exists and provides all Minecraft-native physical utility. Infrastructure designation remains retroactive. Progression governs how much exceptional systemic Construct value can be extracted from that investment, but should not make physically existing construction unreal.

[OPEN] Do not finalize recognized-scale thresholds yet. The infrastructure-labor and control-window findings materially affect scale design. Before assigning any threshold series, establish the realistic player-minutes required to place it, how construction efficiency changes that, what fraction of total material supply can realistically be concentrated in one project, how many control windows a large project is expected to span, what recognized scale actually provides, and how late-game Construction compresses labor requirements. Earlier numerical thresholds are exploratory only.

## Authority and source limits

This document owns the shared infrastructure framework. classes.md retains class progression and abilities; objectives.md retains objective rules and the Industrial Enchanter’s independence from recognized Supply Lines; maps.md retains map design. This is Working design canon, not an implementation claim.

Source: World and Match Systems, Infrastructure System New Design Handoff, 10 September 2026, conversation 6aa1edb8-4594-83e9-98cb-fd0870e52b5b. The retrieved handoff contains sections 1–19 and the beginning of section 20; integration is supported independently by the preceding framework discussion and existing canonical documents. A second, separately supplied handoff (World / Match Systems: Infrastructure Economy, Material Categories, Territorial Contest, sections 1-17) was received complete and is integrated above; whether it is the same document as the earlier truncated retrieval is [OPEN] and should be confirmed by the owner. See [infrastructure reconciliation](docs/reconciliation/2026-09-10-infrastructure.md) and [world systems reconciliation](docs/reconciliation/2026-09-10-worldsystems.md).
