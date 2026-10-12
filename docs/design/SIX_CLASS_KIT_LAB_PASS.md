# Hollow, Paver, Fungal Assassin, Chauffeur, Bloodmason and Quarryman

11 October 2026. **Prototype/test implementations** for the existing Java 21 /
Paper 1.21.11 combat chamber and skirmish labs. Canonical authority remains
`classes.md` §§20, 23, 25, 26, 28 and 30. Registration does not promote a Proposed
or Working design to Established. Unspecified magnitudes live in `config.yml`;
new class Growth/Task/infrastructure eligibility is not fabricated.

The five requested priorities comprise six classes. Each has its passive,
A1, A2 and ultimate registered; every supplied A1/A2 branch is selectable in a
stable order. Quarryman has no authored branch trees. The shared class roster
feeds both lab selectors without a separate whitelist.

## Hollow

The supplied definitive Heart/body design is incorporated in §30. Body capacity
starts at 1000 displayed Health and gains 110 at every Growth breakpoint through
level 30; Heart capacity is 5/7 of body capacity. Breaking hits have no body
overflow. The external Heart is an attackable owned Slime with a Creaking Heart
block display and explicit remaining Health. Allied authored hits are refused
before the Heart ledger mutates, independently of listener registration order.
A broken Heart returns after 40/32/24/16 seconds at levels 0/8/16/24; full body
Health doubles recovery progress. Relogging saves remaining Heart life/recovery.

Wooden Maw retrieves the Heart, changes body size and Resistance, and repeatedly
thrashes its cavity. Thrashing heals an intact Heart. Recast or expiry redeposits
it. Heartless Maw is smaller/weaker and Slowed without intact-Heart Resistance.
Mangling Branch carries the first caught enemy with a short refreshed Stun;
Eerie Resonance also heals nearby allies; Soresight grows Hollow once per unique
enemy damaging its body during that Maw, including heartless damage. It does
not sample aim rays or mob aggro.

Seeking Heart physically rolls a bounded recorded path and prioritizes the
highest Health fraction. Ordinary recast delays emergence at the Heart. Cursed
Canopy applies Blindness/Weakness; Drawn Together returns along the path over
successive ticks and physically pulls nearby enemies; Creaking Boards permits
wood entry and stuns enemies touching nearby wood on emergence. Heartless Seek
uses a slower delayed emergence toward the highest-fraction enemy player.

Peekaboo uses a forward cone, Maw's radial Frighten followed by inward impulse,
or a successive-tick rush along Seeking Heart's path. Its innate listens for
nearby new Frightened applications and heals body/Heart. Movement still respects
Stun/Root. The sidebar shows Heart Health or return time.

**Prototype limits:** the Heart's collider/visual and wall-slide heuristic,
normal following, cavity geometry, wood-touch search, maximum growth, path cap,
roll/rush speed, and emergence delays are experiments. Burrowing is represented
by hidden, non-collidable delayed emergence rather than Spectator mode. Size
can be capped in config. Recovering a Heart does not itself authorize movement
through a wall. The full networked client feel needs playtesting.

## Paver

The Mixer consumes actual four Sand, four Gravel and one dye while moving,
producing eight matching Concrete. Water/bucket possession advances processing
faster. Full inventory refuses processing; no free material or substitute dye
is introduced.

Placed Concrete records persistent owner/branch metadata. Reinforcement uses a
local, short refreshed mining-fatigue lease; Galvanized strengthens it. Aborting
or leaving the target restores the prior effect. Rebar Chain permits climbing
Concrete; Beveled Edges improves lateral traversal. Pitons maintains exactly
two anchors: initial use creates one, first recast the second; later recasts
choose the farther anchor from the current position. Recasts retain the original
cooldown/lifetime. The bounded safe dash refuses an obstructed route.

Supported Powder receives persistent Chamfered metadata and native gravity is
suppressed. Native hardening converts it to reinforced Concrete; Pozzolan Patch
Roots nearby enemies on that transition. Natural Mix accepts adjacent Grass.
Steep Grading allows sneaking motion over an unsupported edge. A2 arms release
of the next owned connected Chamfered group stepped on; release is bounded and
reschedules native gravity. Placement, breaking, explosions and piston movement
maintain metadata, while actual material checks reject stale ownership cells.

Concrete Shoes consumes carried Concrete and separately borrows nearby owned
reinforced world blocks through shared displacement. Damage/launch scale with
the assembled mass. Borrowed blocks return exactly, including provenance.
Carried material remains physically placed after resolution or cancellation;
occupied final cells drop the corresponding real item rather than overwrite
world state. A zero-material cast refuses. Displays and reservations disappear
on teardown.

**Prototype limits:** reinforcement strength, movement processing cadence,
climbing/dash collision approximation, two-shoe display geometry and bounded
release/gather sizes remain test parameters. Carried material is permanently
committed; borrowed material is a temporary loan. These are distinct resources.

## Fungal Assassin

Ordinary accepted enemy attacks apply Fungal Growth; DoT does not recursively
read as an attack. Bursting Spore consumes the owner's infection and establishes
real Mycelium on eligible soil around the target. Airborne Spread propagates
infection and widens **enemy hits**, keeping the same territory radius. Root
Network pulls distant infected prey toward the originating physical Network.

Shared `DevelopedZones` stores authored physical cells and development counters
with world UUID/coordinates. It is raw ecology support, separate from recognized
Infrastructure or progression rewards. Native fungal spread/growth registers
new terrain. Connected traversal validates current fungal blocks, so destroyed
bridges do not remain invisible links. Infection damage near the originating
Network adds persistent development; bounded extra native growth ticks make
that development productive. Crops receive vetoable growth events. Huge Mushroom
harvests inside a Network guarantee a mushroom drop.

Myceliate uses the player's current directional input to traverse actual
connected fungal cells while hidden/non-collidable. Root/Stun refuses voluntary
traversal. Hardy heals up to a flat effective-Health budget, Hyphae Lashers Roots
nearby enemies, and Nutrient Cycle attempts native mushroom bone meal on exit.
An exit recast is available during the original cooldown.

Destroying Angel is a singular physical mushroom. Maturity in the surrounding
Network can prime it immediately; otherwise it has a visible preparation window.
Unprimed destruction safely disarms it. Primed destruction or recast blooms,
attempts native nearby Huge Mushroom growth, deals enemy damage and reinfects
survivors. Missing/destroyed mushrooms are detected and temporary mushrooms are
removed on teardown; developed terrain remains real world state.

**Prototype limits:** native Vine cannot be waterlogged in this target version;
Submersive uses a bounded native Kelp stalk growing through water above Mycelium
as its aquatic representation. This does not settle or silently replace the
canonical vine fantasy. Eligible soil, connected-cell budget, maturity units,
growth cadence and intangible presentation are experiments. Intangibility
suppresses collision/contact/suffocation, not all incoming damage. Full visual
and aquatic fidelity needs a client playtest.

## Chauffeur

Cargo Boat summons an actual Oak Boat with two native passenger seats and a
separate 27-slot real inventory, opened by sneak-use outside ability mode. Expiry,
destruction or replacement spills cargo exactly once. Muscle Cart summons a
rideable land cart with slower steering correction and more retained momentum.
Their cooldowns and short opportunities are independent.

Exiting a summoned vehicle saves one actual Parking Spot; entering near it grants
a brief boost. Ordinary Boats receive a bounded land-driving boost. Ordinary
Minecart variants can be mounted, retaining their native inventories/fuel/TNT
behavior; Sneak preserves native interactions. Rails accelerate ordinary carts,
and borrowed maximum-speed settings restore when the driver leaves.

Jetski adds velocity in Bubble Columns. Freight Engine checks a full storage
threshold. Front Propeller requires a front impact. Pop the Hopper launches
victims of an actual Hopper Minecart. Diesel Engine reads the actual active
Furnace Minecart fuel, with the last real Coal/Charcoal interaction selecting
its multiplier. Tactical Dismount continues the established trajectory without
the driver, with bonus collision damage; TNT carts detonate at its endpoint.

Racing Line renders a narrow particle line to Parking Spot, marks a wider
physical corridor, boosts driven vehicles heading toward the Spot and accelerates
on hitting marked enemies. It works with ordinary as well as summoned vehicles.
It does not construct a route through obstacles. Glow leases preserve preexisting
glow and remove only this cast's reveal/ultimate state.

**Prototype limits:** server-controlled Boat/Cart handling, drift, Bubble Column
launch, native fuel interactions, collision entry and extreme-speed surfaces
need a connected human client. This pass implements vehicles and their physical
resource behavior. Generalized vehicle Supply Line proof/commitment remains Open;
the existing lab recognition adapter remains Skeleton Crew's. No unearned
Chauffeur Infrastructure packet or automatic delivery is added.

## Bloodmason

Hemostasis starts at 3. A1 lowers it; A2 raises it; manifestations use the
post-cast value. Endpoint 0/6 casts happen before resetting to 1/5 and self-Stunning.
Reaching an endpoint during pathology adds self-damage. From level 5,
stabilization approaches 3 or 4 without crossing that boundary, at the authored
5/4/3-second levels. Native Paper knockback is scaled by current Hemostasis.
The sidebar reports Hemostasis and active Anatomb completion.

Capillary Flow implements the narrow/broad moving four-block segments, faster
P1 and ceiling-truncated P0 eruption. Moving tails restore instead of leaving
an elevator. Nausea Wave drains enemy Hunger and Slows; Bloodtrail records
contacted cells privately and amplifies manifestation without changing cost;
Lifeblood requires exposure and heals each ally once per Flow. A blood floor can
be passed through. Trail writing uses the effective physical footprint as an
explicit test interpretation; the canonical nominal/effective question stays Open.

Viscerwall supplies the permeable P2 floor, rudimentary P3 wall, curved P4,
framed P5 with deliberate passages, and irregular reversed P6 jaw. Render Flesh
changes tissue to systemic Magma and contained lava; Fracture separates major
sections; Adipose uses softer tissue and contact Root. Falling upper teeth hurt
once but cannot land as permanent generated blocks or produce drops.

Anatomb chooses the largest clear stage that fits, widening upward with physical
rooms/floors and a central ascent opening. Its shell starts without reinforcement.
Allied damage consumes physical shell cells to heal allies; enemy damage inside
heals Bloodmason. Held A1/A2 use shared continuous-input support and pulse every
half-second: both spend Health and change Hemostasis, while Hemorrhage drives
combat upward and Teratoma repairs/grows tissue inward. Both add to completion;
the final enemy Stun scales with missing Health. Death/expiry/teardown restores
all leased air and removes falling teeth.

**Prototype limits:** block/material anatomy is a bounded geometric interpretation,
not a final Vitruvian visual. Powder Snow supplies permeable Flow/floor physics;
Magma remains systemic and does not gain ally safety. P6 lava is contained by
reservation guards. Exposure, costs, pathological duration, completion target,
Stun scaling and stage proportions remain test parameters. Shared temporary
restoration has the repository's existing abrupt-crash limitation; ordinary
shutdown and class/lab teardown restore correctly.

## Quarryman

Quotas track Stage, current progress, previous completedness (LQC) and cumulative
completed quotas (CQ) separately. Only natural accepted native breaks credit the
quota; provenance is witnessed before break listeners clear it. Expiry records
partial failure or success; success alone adds CQ. Strike pauses/clears the
current quota without a free Stage decrease, and refresh does not begin a new
quota before the scheduled deadline. CQ/LQC, Stage, remaining time and cleared/
Strike state survive class teardown. The sidebar makes all performance inputs
and the current deadline readable.

Ceaseless Swing raises a held tool's harvest tier after ten seconds of continued
mining without replacing that item or changing its native mining-speed tier.
Native upgraded-tool drops support, for example, Iron harvesting Obsidian.
Scan lasts 30 seconds, using `ceil(CQ/2 + 3)` cube width anchored to the caster's
block and the supplied additive reduction. It advances break progress with a
positive hardness floor, rather than assigning negative hardness. The physical
cube applies to other miners too. Stage/current progress do not alter potency.

Strike uses the strongest carried pickaxe tier, supplied CQ exponential/cap and
native charge curve; there is no minimum combat floor. Strongest means the
highest base vanilla damage tier in storage in this prototype.

Eternity Mountain first marks a cube, then permits release after a telegraph.
The real player moves to the mountain peak and enemy players still inside are
captured at its base; allies remain in the real world. An owned, publicly glowing
World Quarryman body remains attackable at the cast point. Damage to either ends
the domain, with world-body damage deducted from the same owner Health pool
without being swallowed by return-teleport immunity. Friendly authored hits do
not end it. Return uses recorded positions, with a bounded safe alternative
when needed.

Holding a pickaxe on the Bedrock target advances custom work using native Stone
break speed as an equipment/Task/condition reference. CQ affects cube size, not
the Bedrock rate. Softer eligible source blocks are progressively removed;
corresponding mountain material disappears as progress increases. Interrupted
excavation stays removed. Containers, shared reservations, unbreakable blocks and
portal/objective structural materials are protected. Excavation does not fabricate
quota progress, loot or objective completion. Mountain/benchmark blocks and body
are removed on exit; bot world transfers send the normal client-loaded handshake.

**Prototype limits:** quota demand/timer and Stage bounds, ceil/anchoring/width cap,
channel-and-recast release, sampled Bedrock work, hardness ordering, proxy body,
return fallback and mountain geometry are explicitly Open test choices. The
native domain is an isolated generated runtime world using the existing Bukkit
Void authoring approach; it is not a new playable map release. Ordinary platforming,
mining, bridging and class mobility solve the mountain. Human domain capture,
movement, mining cadence and armor synchronization need playtesting.

## Shared behavior and verification

All six use the shared input, cooldown, ability damage, team, Root/Stun/Frightened,
provenance and temporary-terrain machinery. Retained ultimates use public glow
leases; this does not claim implementation of the still-Open per-viewer Tier-2
empowerment packets. New footprint/structure persistence is in separately named
plugin YAML files, while entity/cast opportunities remain temporary.

The unit and native evidence is recorded under `validation/plugin/six-kits/`.
Native fixtures use real Paper bodies/entities/blocks and vetoable events, and
clearly label controls that call production state directly. They do not certify
human client handling or production balance. See the validation README/report
for exact results and artifact hashes. No browser wiki resynchronization is implied.

### Accepted evidence

All **1,248 unit tests** passed after integrating the concurrent main changes. Native Paper evidence passed **152 checks**:
54 six-kit checks in `merged-native`, 25 earlier-kit checks, 36 Kitfighter/Werewolf/
Gardener checks, and 37 Skeleton Crew/transport checks. Exact reports and the
plugin SHA-256 are recorded in
[verification.json](../../validation/plugin/six-kits/results/verification.json).
Exploratory failures remain retained separately; the accepted six-kit run has
zero failures. Human client handling and production balance remain playtests.

The pre-merge 1,226-test / `native-5` evidence is retained separately in that record.
