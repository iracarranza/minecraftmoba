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

## Authority and source limits

This document owns the shared infrastructure framework. classes.md retains class progression and abilities; objectives.md retains objective rules and the Industrial Enchanter’s independence from recognized Supply Lines; maps.md retains map design. This is Working design canon, not an implementation claim.

Source: World and Match Systems, Infrastructure System New Design Handoff, 10 September 2026, conversation 6aa1edb8-4594-83e9-98cb-fd0870e52b5b. The retrieved handoff contains sections 1–19 and the beginning of section 20; integration is supported independently by the preceding framework discussion and existing canonical documents. [VERIFY RECENCY] The unseen continuation is not claimed as integrated. See [reconciliation](docs/reconciliation/2026-09-10-infrastructure.md).
