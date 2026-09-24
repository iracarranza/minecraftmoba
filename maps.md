# Minecraft MOBA — Maps

## Procedural philosophy — Established

Maps should combine **procedural generation** with distinct **map archetypes**.

A map archetype defines the fundamental geometry and strategic problem. The
seed determines the exact terrain, resources, routes, structures, and
opportunities inside that grammar.

Players should learn Minecraft generation rules and how an archetype behaves
without memorizing fixed resource coordinates. The competitive skill test
includes rapidly reading an unfamiliar Minecraft world and deciding how the
team should transform it.

Minecraft remains the base systemic language. A map should not feel like a
conventional MOBA arena covered in Minecraft textures. The Minecraft world
itself is the strategic board.

Terrain, hunger, mining, construction, caves, food, animals, villages, routes,
logistics, resources, infrastructure, farming, transport, and player
modification of the environment should remain real gameplay.

A useful world-state arc is:

generated Minecraft world
→ discovered
→ traversed
→ extracted
→ constructed
→ developed
→ connected
→ supplied
→ contested

This is an emergent ecosystem rather than a required linear sequence.


## Competitive spatial contract — Established clarification, 22 September 2026

This explicit design clarification supersedes broader homeland-symmetry and
map-wide starter-Route assumptions below. It does not retune existing fixtures.
Source and reconciliation: [spatial doctrine record](docs/reconciliation/2026-09-22-spatial-doctrine.md).

A competitive map has three relevant spatial layers:

| Layer | Contract |
|---|---|
| **Homebase Core** | A relatively small, heavily authored competitive symmetry kernel. North and South receive functionally mirrored geometry: Aether Fountain, spawn/reconstruction space, required immediate defensive geometry, and explicit wilderness-facing interface/exit(s). Natural terrain may be replaced inside the Core. No accidental natural economic jackpot belongs inside it. |
| **Homebase-supported region / hinterland** | Predominantly natural terrain surrounding the Core, selected for clean integration and a viable opening. It is **not mirrored**. Distributed low-richness/basic opportunities support useful opening activity, without a single nearby concentration normally making an immediate full-team expedition obviously dominant. Initial Route support primarily serves exits and this region. |
| **Wilderness** | Natural geography may be substantially asymmetric in canopy, elevation, flat land, caves, resources, biomes and coastline. Strategic opportunities and player-created Infrastructure inhabit it; they do not normalize it. Wilderness may be larger, harsher and less conveniently traversable than earlier prototypes assumed. |

**The symmetry guarantee ends at the Homebase Core/interface.** Functional
opening opportunity beyond it matters; matching terrain statistics does not.
A natural difference is not a competitive deficit without evidence of a relevant
gameplay consequence. Neither lower historical `homeland asymmetry` nor equal
renewable species/counts is a general quality requirement.

### Homebase Socket

A **Homebase Socket** is a candidate natural area capable of accepting the same
standardized Core with low, bounded integration cost. Seedfinding seeks two such
areas, not two naturally mirrored terrain patches. Evaluate:

- physical footprint fit, support and headroom;
- limited cut/fill/clearing requirement;
- viable exit/interface into natural terrain;
- an opening economic floor and an opening opportunity ceiling;
- a smooth transition from supported terrain into unrestricted Wilderness.

Reject a socket that requires major terrain surgery or a large procedural
transition halo. Prefer terrain that permits simple reliable authoring over a
more powerful authorer that forces arbitrary terrain into compliance. The
transition should require only bounded, terrain-conforming work.

**Open:** exact Core footprint, required immediate defensive elements, exit
contract, bounds on integration work, and opening floor/ceiling acceptance
criteria. Existing structure offsets, pad scores and homeland footprints are
Prototype/test evidence, not an implemented standardized Core/Socket contract.
This clarification does not relocate the established team objectives or decide
that all defensive layers must fit inside the Core.

### The Opening Hinterland is small — Canonical, 22 September 2026

An important correction. The Opening Hinterland is a **compact natural
envelope** around the Core. It is **not**:

- the team's half or end of the map;
- everything between the Fountain and the midline;
- a giant radial homeland;
- a fixed-radius circle;
- the region enclosed by the initial Routes.

**Wilderness may exist on every side of it** — toward the midline, east and
west, and **behind the Homebase, toward that team's own N–S pole.** Poleward
Wilderness is the case the earlier vocabulary could not express, because it
assumed a team's end of the map was that team's ground.

```
                 WILDERNESS

             ┌──────────────┐
             │  HINTERLAND  │
  WILDERNESS │     CORE     │ WILDERNESS
             └──────────────┘

                 WILDERNESS
```

The ellipse describes a spatial **role**, not a literal mask or radius.

**Opening floor.** The Hinterland must let a team *begin* every fundamental
verb — Construction, Extraction, Development, Production, Exploration, the
movement and organization Logistics needs, and ordinary Combat/PvE. It is
qualitative: this is deliberately **not** one quota per archetype, and invents
no counts of trees, animals or ore.

> **The Hinterland must permit every fundamental verb, but should not resolve
> any of them.**

**Opening ceiling.** It supplies distributed subsistence, not concentrated
windfalls, and should exclude opportunities that skip meaningful early
progression. Known exclusions:

- **villages**;
- **carrots**;
- **enough accessible iron to equip a player.**

The iron rule is **not** "no iron". Basic Extraction must remain possible; what
the Hinterland should not do is hand over an equipment-sufficient package as an
opening resource. Judging it needs a declared equipment target, which does not
yet exist, so the measurement stays **UNMEASURED** rather than passing by
default.

[IMPLEMENTED for villages, 23 September 2026 — see `terrain_harvest/opening_ceiling.py`.]
The `verify` stage now surveys each Fountain's opening envelope on the
**pristine** world, before authoring, and rejects a map whose opening stands on
built structure. Run against 99887766 it finds 12 sampled columns of village
inside south's envelope — bells, hay blocks, acacia stairs, terracotta — and
**zero** inside north's, which is the asymmetry stated below. **99887766 would
no longer certify.**

The envelope radius is a NON-CANON PROVISIONAL **96 blocks**: doctrine says the
Opening Hinterland is "compact" and gives no number, so it is a parameter with a
declared default rather than a constant pretending to be canon. Raising it makes
the compiler stricter and rejects more maps; that trade is unmeasured.

**Carrots and accessible iron remain unchecked**, and the module says so rather
than staying silent, because silence would look like coverage. The iron rule
still needs a declared equipment target that does not exist.

[Original finding, 23 September 2026] **No compiler stage checked
any of this.** The first live playtest found a savanna village **68 blocks**
from the south Fountain, inside its Opening Hinterland, on a map that passed
physical verification and certified READY. The nearest village to the north
Fountain is **728 blocks** away. So the exclusion above is violated, and it is
violated asymmetrically — one team opens beside a village and the other does
not.

`respect.py` already recognises village blocks, for Routes that must not run
through a house, so the recogniser exists and the ceiling check simply was never
written. Until it is, "serious ceiling violations should reject a socket" is a
statement about what the compiler ought to do and not a description of what it
does. The carrot and iron exclusions are equally unchecked.

### Authored furnishing must respect what is already built — 23 September 2026

Raised in play, on the authored objectives rather than on Routes. The rule that
was written for Routes — *do not run a deck through a village house, fell a tree
whole rather than cutting the part that is in the way* — is **general**, and
only Routes were obeying it.

Three failures, in descending order of seriousness:

[FIXED 23 September 2026 — `terrain_harvest/clearance.py`.] All three below are
now addressed: sites standing on built structure are refused before anything is
written, overhanging canopy is felled whole, and entities inside a footprint are
removed before the blocks land on them.

1. **Structures inside a pad were skipped, not refused.** `clear_and_foundation`
   treats a built block as "not terrain" and leaves it standing, so an objective
   authored over a village house builds *around* the house and embeds it.
   Meanwhile `column_scan` **fails hard** on structures when verifying a site.
   The verifier and the builder therefore disagree about the same condition,
   which is this project's recurring defect — two implementations of one idea.
   The siting stage should refuse a footprint containing built blocks, and then
   neither needs a policy.
[CORRECTED] The felling rule is **not** general, and a commit message claiming
it was generalized from Routes is wrong. `routes.carve` brushes an overhanging
leaf and leaves its tree standing, on measured grounds: felling for a leaf along
a corridor "turned a corridor through a forest into a clear-cut -- 186,226
blocks of one". A pad is a compact disc that is levelled anyway, so a felled
tree reads as clearing and a bisected one as damage; a corridor is a thin line
through standing forest, so felling everything that overhangs destroys the
forest it runs through. **The shared rule is that authoring must look like work
someone did; what that implies differs by the shape of the work.** The
structure-sparing rule IS general -- a corridor routes past a building, a pad
refuses the site.

2. **Overhanging canopy was sliced flat at the pad boundary.** Leaves inside the
   disc were brushed to air while the trunk outside it was untouched, so the
   north Outpost stood in a ring of trees cut in half. Fixed: a leaf now fells
   the tree it belongs to, the same call the trunk case already used. A felled
   tree reads as clearing; a bisected one reads as damage.
3. **Entities in the footprint were not moved.** A sheep was suffocated by the
   Bastion. Minor in effect, but the same omission stated again: authoring
   considered blocks and nothing else. They are now deleted rather than
   displaced — displacing means choosing a destination and there is no
   non-arbitrary one — and entities live in their own region files, so they are
   edited there rather than hoped about.

The principle, stated once so it stops being re-derived per subsystem:

> **Authoring should read as work someone did, not as a bug.** Anything that
> was already built is either avoided or refused, never bisected, embedded or
> silently overwritten — and that applies to every authored thing, not to
> Routes alone.

This list is **not exhaustive** — it is a seam. Useful questions for a new
candidate exclusion: does it scale strongly with multiple players, compress
several opening verbs, impose major competitive cost if delayed, or skip an
intended economic step? Serious ceiling violations should **reject a socket**,
before authoring starts deleting natural Minecraft features.

Small **floor** deficiencies may sometimes be corrected with existing authored
opportunity systems. Ceiling violations are a rejection, not a repair.

### The singular Giant Monster Lair — Canonical, 22 September 2026

There is exactly **one** Lair in the world: a massive, permanent, conspicuous
landmark whose occupant succeeds Giant → Ghast → Ender Dragon. See
[objectives.md §17C](objectives.md) for the lifecycle.

A valid Lair socket should supply, in terrain-appropriate form: enough 3D
encounter volume for all three encounters; usable ground for players, building,
retreat and PvP; meaningful vertical volume, especially for Ghast and Dragon;
multiple practical approaches; enough visibility that it reads as a landmark;
terrain already broadly suitable, needing bounded authoring rather than large
excavation; and room for players to modify it over the match.

The Ender Dragon is likely the limiting physical-volume case. That does **not**
make the Lair flat, circular, an artificial stadium, biome-neutral or
geometrically centred. A mountain basin, a frozen amphitheatre, a major clearing
or an unusual valley may all be valid; terrain should shape the encounter.

**Centrality is competitive, not coordinate.** Its W–E/regional position may
vary widely — on Default it might sit near the literal centre, in Forest, in
Rugged Uplands, or even in the alpine extreme — provided team access stays as
near equal as practicable. Evaluate initial Practical Reach from both teams:

> **A_L = |Reach_N − Reach_S| / mean(Reach_N, Reach_S)**

Minimise it. **No acceptable threshold is established**, and none is invented
here. This is the sharp distinction the spatial doctrine turns on:

> asymmetric Wilderness terrain is **allowed**; one team having structurally
> privileged access to the singular shared major objective is a **genuine
> competitive concern**.

**Open:** exact Lair dimensions, the access-parity threshold, and any
anti-cheese rules. No map in this repository has a certified Lair socket; the
runtime reports the site UNCONFIGURED rather than choosing a centre.

### Two independent spatial axes

**Strategic Depth** is the outward relationship to a team's Homebase/opening
space. **Regional Character** is position within a Map Type's geographic
composition. Strategic Depth asks how rich/novel an opportunity may be;
Regional Character asks which kinds make ecological/geographic sense there.
Neither is the player's current travel cost after building Infrastructure.

For the emerging **Default Map Type**, Regional Character tends approximately
from **Ocean → Coast → Open Land → central interior → Forest → Rugged Uplands →
Alpine/Frozen Peaks**. These are tendencies and gradients, not seven rectangular
biome stripes. Central interior is not synonymous with deep Wilderness relative
to either team. The existing east-coast/west-highland orientation is a Working
Default arrangement, not a rule for every Map Type or a guarantee of identical
N/S regional composition.

**Working future pipeline, not implemented Map Types:** Map Type macro-geography
→ team topology → viable Homebase socket pair → wilderness viability →
authorability → physical verification → PlayableMap. A future Default recognizer
should seek its intended Overworld gradient; other Map Types will define their
own macro-geographic search contracts.


# Default map

## Default map thesis — Established direction

Default should remain recognizably based on **actual Minecraft Overworld
generation**, including underused or ordinarily low-value generation quirks.

It is not one fixed map.

The preferred generation philosophy is:

**Let Minecraft generate Minecraft-like geography, then constrain, inspect,
validate, select, and modestly adjust it for competitive play.**

Do not construct a conventional MOBA arena first and disguise it with
Minecraft terrain.

If a generated region requires extensive artificial correction to become
competitively viable, reject it rather than over-authoring it.


## Macro-geography — Working

Teams oppose one another along the:

**North ↔ South**

axis.

The strongest guaranteed environmental gradient is:

**West ↔ East**

This orthogonality is deliberate.

### North / South

- North homeland
- South homeland
- team opposition
- initial authored Routes supporting Core exits and hinterlands

### West

The western side contains the map's major mountain/highland geography.

### East

The eastern side transitions toward coast/ocean geography.

Exact terrain and silhouettes vary by seed.

The macro relationship is guaranteed; the exact geography is not.

This creates strong environmental asymmetry without automatically making one
team "the mountain team" and the other "the ocean team."


## Team homelands — Working

Each team begins in a comparatively safe development region.

Homelands should provide viable baseline access to ordinary Minecraft
development resources, including:

- common terrain materials;
- wood;
- food;
- baseline ore access;
- useful caves;
- soil;
- water;
- farming;
- construction space;
- development space;
- the Aether Fountain;
- appropriate team defensive objectives.

Baseline opening opportunity should be **functionally competitive**, with a
useful distributed floor and a ceiling on dominant opening concentrations.
This resource vocabulary is not a promise of identical species, counts, caves,
or local richness. The Core/interface receives functionally mirrored geometry;
the surrounding supported region remains natural and is not mirrored.

The important requirement is that neither team begins with a structurally
inferior version of the fundamental Minecraft resource vocabulary.

Players may choose to remain near home and specialize in:

- farming;
- Production;
- Logistics;
- infrastructure;
- Development;
- preparation;

while teammates undertake Wilderness expeditions.

This division of labor is desirable.


## Continuous Wilderness — Established direction

Outside the homelands is one large continuous Wilderness.

It is not divided into rigid A-side, center, and B-side bands.

The Wilderness contains the meaningful exploration layer:

- villages;
- major and minor POIs;
- natural structures;
- forests;
- caves;
- ores;
- animals;
- crops;
- unusual geology;
- richer deposits;
- biome-specific resources;
- difficult terrain;
- water;
- transportation opportunities;
- objectives;
- resource sites;
- player-created infrastructure.

Players should meaningfully choose between:

- following authored Routes;
- leaving Routes to explore;
- mining;
- gathering;
- developing infrastructure;
- contesting known opportunities;
- searching for less obvious opportunities;
- remaining closer to home and producing.

The Wilderness must not become a tightly packed theme park in which every
important destination is immediately visible from the previous one.

Meaningful empty connective terrain is part of the map.


## Opening access — Canonical, 23 September 2026

The map-authored thing and the player-created thing are **different objects**,
and the shared word "Route" has been hiding that. The layering is:

```
Homebase Core
     |
authored base exits / opening access
     |
compact Opening Hinterland
     |
natural Wilderness
     |
player-recognized Exploration Routes
```

> The initial map says **"here are sane ways out."**
> Exploration says **"we have learned how to move through this wilderness."**

**The generated map does owe opening access.** This is not optional and is not
satisfied by zero. A team whose Core is walled in by natural terrain has a
dysfunctional opening, and refusing to intervene is not neutrality. What the map
owes is short access **out of the Homebase and through into its compact
Hinterland** — nothing more. Authored access must not become a broad highway
into the Wilderness, and must not connect a team directly to objectives, POIs or
the Lair.

**The requirement is equivalent exit capacity, not identical exit geometry.**
One base may have two broad natural approaches and need no intervention at all;
another may need a small terrain-conforming connection around a cliff. Carving
matching roads through both sides to make a symmetry number come out is the
wrong instinct — Hinterlands are not mirrored, and the gate is whether each team
can get out, not whether they get out the same way.

[OPEN] **Whether authored opening access should carry the formal `Route`
identity at all is unresolved.** It may be a precursor concept that deserves its
own name. Do not assume the two are one object because the Alpha map's authored
features were called Routes.

[HISTORICAL] The compiler gap identified on 23 September was mis-stated as
"generated maps author no Routes, the Alpha map did, so they disagree about what
a map owes." That framing was wrong on both halves: Exploration Routes are
player infrastructure and correctly absent at match start, while opening access
is owed and is genuinely missing. **The gap is opening-access certification, not
Route generation.** A generic `routes` stage must not be added to the compiler
if it would generate formal Exploration Infrastructure before play begins; the
Homebase/Hinterland authoring and verification stages are where each Core's
initial exits into its own Hinterland should be established and checked.

For the Alpha map's own authored "Routes": read them case by case. Where they
are short base-access features serving the compact Hinterland, they are the
precursor to the opening-access concept above. Where they extend substantially
into Wilderness or toward strategic destinations, that portion does **not**
establish what a generated map owes at match start.

## Authored Routes — Working

Initial/map-authored Routes primarily support Homebase exits and hinterlands.
They are physical Minecraft infrastructure, not invisible MOBA lanes. They should
not routinely create convenient highways deep into unrestricted Wilderness or
directly solve access to distant strategic opportunities. Player-created
Exploration Infrastructure can subsequently extend useful Route networks deeper
into the world.

The older three-branches-per-team layout and promised authored links toward
villages/major POIs are **Historical/prototype assumptions**, not the general
contract. Preserve their measurements and existing fixtures; do not retune Route
lengths in this doctrine pass. Terrain-weighted paths, Practical Reach, physical
corridor quality and Route distance remain useful observations, scoped to the
connection they actually describe rather than a mandate to equalize map-wide
access.

Players remain free to leave the Routes at any time.

Routes should generally remain cleanly traversable.

Avoid arbitrary:

- trees blocking the road;
- pointless jump taxes;
- random walls;
- decorative impassability.

Difficult terrain should make **off-Route traversal** meaningfully more
expensive rather than making designed infrastructure unpleasant.


## Authored Route test mechanic — Prototype

For the next playable Default-map test:

- base authored Routes receive **no movement-speed bonus**;
- their clean/direct geometry is already useful;
- additionally, they provisionally reduce **locomotion-generated exhaustion**
  by approximately 10%.

Conceptual values:

Sprinting:

0.10 exhaustion/block
→ approximately 0.09

Sprint-jumping:

0.20 jump component
→ approximately 0.18

Ordinary jumping:

0.05
→ approximately 0.045

Do not apply this reduction to:

- combat;
- regeneration;
- mining;
- other non-locomotion hunger costs.

This deliberately double-counts Route value for the first prototype:

1. Routes are physically cleaner and more direct.
2. Routes also receive a modest artificial locomotion-efficiency benefit.

This is a **test mechanic**, not final Route progression.

Do not restore the earlier +10% movement-speed proposal as the default.


## Difficult Wilderness — Established principles

A difficult region should be a **place**, not merely an obstacle.

Strong difficult Wilderness should provide:

### Intentionality

Entering or continuing deeper represents a visible commitment.

### Value

Distinctive resources or opportunities justify that commitment.

### Depth

Sustained gameplay can occur inside.

A difficult region should not consist of one cliff, summit, ravine, or jump
obstacle.

### Legibility

Internal geography can be learned, named, communicated, and navigated.

### Contestability

Multiple approaches or portions can become contested.

### Transformability

Minecraft actions can improve the geography.

Examples:

- tunneling;
- bridging;
- roads;
- stairs;
- clearing;
- construction;
- occupation.

### Logistics

Provisioning, reinforcement, extraction, and return travel create meaningful
costs.

Useful supporting patterns include:

- multiple viable approaches;
- internal traversal hierarchy;
- differentiated resources;
- recognizable landmarks;
- reasons to remain inside;
- reinforcement depth;
- opportunities to domesticate initially hostile geography.

Difficulty should not be synonymous with repeated jumping.


## Western mountain/highland system — Working

The west contains the Default map's principal highland/mountain region.

Desired broad gradient:

foothills
→ increasing elevation
→ increasing relief
→ exposed stone
→ longer/larger ridges
→ steeper terrain
→ stronger geological features
→ deep western highlands

The increase in difficulty should initially be gradual and then become more
pronounced toward the deep west.

As off-Route western terrain becomes increasingly expensive to traverse,
player-created western Routes and useful existing starter segments can become
more strategically valuable even before accounting for any explicit Route locomotion-efficiency
mechanic.

The mountain must have **depth**.

It should contain combinations of:

- ridges;
- valleys;
- saddles;
- basins;
- ravines;
- caves;
- exposed deposits;
- landmarks;
- multiple approaches;
- internal resource geography.

It should not read as:

- one wall;
- one ridge;
- one decorative mountain;
- one obstacle between two destinations.


## Snow / cold ecology — Working

Default should guarantee a:

**variable high-altitude cold/snow ecology**

within the western highlands.

Do not add arbitrary snow-resource patches.

Snow should read as actual geography/ecology.

Exact extent and location may vary by seed.

The guarantee is strategically important because snow participates in the
project's class/resource vocabulary.


## Eastern coast / ocean — Working

The eastern side transitions into coast/ocean geography.

Exact coastline geometry varies by seed.

The coast/ocean should eventually participate meaningfully in:

- transportation;
- traversal;
- resource geography;
- settlement relationships;
- class interactions;
- regional identity.

It should not merely function as a decorative map boundary.


## Terrain-generation principles — Established direction

Competitive topology may be mathematically authored internally.

The math should not remain visibly legible.

Avoid:

- obvious ellipses;
- capsules;
- repeated circles;
- gridlike forests;
- mathematical cliffs;
- repeated terrain stamps;
- suspicious micro-symmetry.

Prefer irregular Minecraft-like geography.


### Forests

Prefer:

- irregular stands;
- variable spacing;
- variable density;
- clearings;
- edge variation;
- plausible substrates;
- varied canopy;
- natural understory;
- occasional fallen logs or mushrooms where appropriate.

Forests should be capable of possessing real **interiors**, not merely strips
of trees between destinations.


### Verticality

Use both positive and negative verticality.

Positive examples:

- hills;
- peaks;
- ridges;
- outcrops.

Negative examples:

- gullies;
- basins;
- ravines;
- cuts;
- cave mouths.


### Hydrology

Water should:

- occupy plausible catchments;
- connect where appropriate;
- follow terrain;
- visibly descend where necessary.

Avoid arbitrary closed liquid stamps.


### Landmarks

Large regions need recognizable geography that players can:

- learn;
- name;
- communicate.


## Mirroring and competitive fairness — Working

The full Wilderness does **not** require literal mirroring.

Current preference:

- Core/interface geometry is functionally mirrored; natural hinterlands have a
  viable distributed opening floor and no dominant opening concentration;
- Wilderness terrain may be asymmetric;
- resource opportunity may be asymmetric;
- villages may be asymmetric;
- the generator/validator evaluates strategic opportunity rather than
  identical geometry.

Three useful opportunity categories remain:

1. **Baseline**
   The controlled Core contract plus viable functional opening opportunity in
   natural supported terrain; no terrain-statistic or species/count matching.

2. **Allocated opportunity**
   Independently generated content whose overall strategic availability is
   balanced without requiring matching coordinates.

3. **Contested opportunity**
   A shared opportunity whose fairness comes from comparable practical access,
   discoverability, and contestability rather than duplication.

Players do not see generator value scores.

Fairness should consider:

- access cost;
- quantity;
- quality;
- terrain;
- Route relationships;
- transportation;
- contestability;
- resource portfolio;
- alternate access.

Straight-line distance alone is insufficient.


## Villages — Working

The Wilderness should contain enough villages for discovery to become a
network problem rather than a single lookup.

Current broad target:

**at least roughly 3 Wilderness villages**

with multiple settlement destinations available in a typical Default map.

Earlier work used approximately **3–5** as a useful exploratory range, but this
is not a final hard cap.

Three is useful as a minimum because players can:

- discover settlements in different orders;
- compare their economic value;
- develop different villagers;
- construct different inter-settlement relationships;
- establish competing logistical networks.

Villages should remain sparse enough to feel discovered rather than
ubiquitous.

A village is a **compound resource portfolio**, potentially providing:

- crops;
- livestock;
- villagers;
- workstations;
- beds;
- buildings/materials;
- containers;
- defensible terrain;
- developed space;
- Route value;
- Logistics value.

A village therefore cannot be evaluated merely as a point coordinate.

Its portfolio and practical accessibility matter.


## POIs — Working

The Wilderness includes:

- minor POIs;
- major POIs;
- villages;
- natural/geological opportunities;
- objective-related locations.

A major POI does not necessarily mean:

- a huge structure;
- a boss arena;
- a neutral objective.

It means a strategically important place.

POI placement should interact with:

- terrain;
- Routes;
- villages;
- resources;
- team access;
- contestability;
- regional identity.

### Historical scale benchmark

In an earlier flat greybox, approximately **15 seconds of sprint-jumping from
a Route endpoint to a major POI** felt reasonable.

This is retained only as a historical perceptual benchmark.

It is not a current distance requirement; later map-scale work may place
equivalent opportunities farther apart.


# Resource ecology

## Resource-generation philosophy — Established direction

Core rule:

**Discovery uncertainty is good.
Existence uncertainty is often bad.**

Players should remain uncertain about:

- exact resource location;
- quantity;
- access;
- whether enemies found it first;
- extraction difficulty;
- development potential.

Fundamental resource categories generally should not disappear entirely from a
map by random chance.

If a resource becomes fundamental to:

- a class;
- an objective;
- a recipe;
- baseline progression;
- core logistics;
- traversal;

it should be promoted into **guaranteed map vocabulary**.

Guaranteed does not mean:

- mirrored;
- abundant;
- fixed;
- near spawn.

It means that the map cannot accidentally omit the system.


## Resource-treatment categories

Resources can be treated as:

1. **Baseline / Homeland-guaranteed**
2. **Natural Wilderness**
3. **Region / Map-guaranteed**
4. **Allocated Wilderness Opportunity**
5. **Objective-guaranteed / Objective-activated**
6. **Player-developed**

Also distinguish whether a resource is:

- finite / depletable;
- renewable / replantable;
- reproductive / mobile;
- naturally regenerating;
- objective-generated;
- objective-activated;
- player-produced.


## Spatial opportunities and initial balancing

**CANON / ESTABLISHED principles — Worldgen and Initial System Balancing branch, incorporated 12 September 2026.** Worldgen generates spatial opportunities. Player income and throughput emerge from current world state and human activity. This section incorporates the explicit branch corrections; it does not select numerical balance or claim implementation. The quantitative specification is [the opportunity-field model](docs/worldgen-initial-balancing.md). Source and conflicts are recorded in [the reconciliation](docs/reconciliation/2026-09-12-worldgen-balancing.md).

### Candidate Density, Regenerative Eligibility, and Regenerative Vocabulary

**Candidate Density** is how many candidate resource opportunities the geography supports per stated spatial unit. Ecology, geology, biome, terrain, and actual resource instances determine the candidate population. **Regenerative Eligibility** identifies which of those opportunities can participate in regeneration. **Regenerative Vocabulary** is the set of resource or encounter kinds eligible to recur in that geography. Vocabulary eligibility does not promise every eligible kind appears in every region.

Strategic Depth increases both regenerative vocabulary and the fraction of candidate opportunities eligible to regenerate. In the branch's symbolic comparison, shallow A has X/T and deeper Z has Y/T, where Y/T > X/T. X and Y describe eligible subsets, not quotas the generator must fill. The result emerges from candidate resource density, ecology/geology, and eligibility conditions. Identify real candidate opportunities, evaluate their regional conditions, and observe the eligible subset; do not sprinkle enough regenerative nodes onto empty cells to satisfy a percentage.

T is not held equal between real regions. For an implementation using opportunity counts, report X_A/T_A and Y_Z/T_Z with a consistent candidate unit. A depth band is an analytical grouping, not an authored resource polygon. The intended gradient does not require every stochastic region at a greater depth to exceed every shallower region; the comparison scope and acceptance tolerance remain Open. Sparse deep geography may have a high eligible fraction and very few actual recurring sites. At matching spatial units, eligible density is candidate density times the observed eligible fraction; currently available density additionally depends on depletion and renewal state.

[CONFLICT — measurement terminology] Earlier branch prose also called T “total resource value.” Counting opportunities and weighting their value are different measurements. This incorporation uses candidate-opportunity share for X/T, as explicitly requested. A value-weighted regenerative share may be reported separately under an explicit valuation rule; it must not silently replace the count statistic or become an authored value budget.

### The depth gradient remains additive

The established reasons for depth and difficulty remain: travel and round-trip commitment, Hunger/provisioning, difficult terrain, danger, uncertainty, search, extraction and return burden, limited immediate access to homeland stores, logistics, infrastructure investment, and specialized regional resource vocabulary. Broader regenerative eligibility and vocabulary add recurring potential to this existing exchange. They do not replace finite rewards, simplify all costs to distance, or assign higher drops automatically.

Strategic Depth is not simply radial distance from a Fountain. Existing effective-distance factors include terrain, elevation, usable paths, food, Hunger, movement methods, cargo, classes, and infrastructure. Keep the team-relative Strategic Depth descriptor, Regional Character and the player's changing operating cost separate; whether eligibility itself is recalculated after development remains Open.

Raw candidate density, actual resource density, Strategic Exposure, and defensibility are **not monotonic with depth**. Their causes differ. Ecology/geology may produce a sparse mountain, dense forest, or economically weak connective terrain at the same depth. Empty or weak deep terrain is legitimate; each deep region need not contain a standardized reward package. Existing map resource guarantees remain vocabulary/functionality guarantees, not blanket abundance or regenerative quotas.

### Finite and regenerative world opportunity

Both finite and regenerative resources are spatial from the beginning. Finite opportunity is sought, discovered, accessed, and exploited; local remaining value decreases and does not naturally restore under that opportunity's rule. Depletion can push activity toward other deposits or new geography, without requiring a strictly outward radial frontier.

Regenerative opportunity is a known recurring world possibility whose current manifestation must still be sought, accessed, and exploited by humans. Exploitation reduces local availability; renewal or ecological conditions later restore opportunity. Known ecology does not imply exact current location, readiness, convenient access, ownership, or guaranteed successful acquisition. Crop Patches, Herds, and Mob Swarms illustrate the recurring-opportunity concept without settling their complete population rules.

**Regeneration restores world opportunity, not player income.** It does not place periodic resource payments in inventories or storage. Search, harvest or combat, mixed drops, inventory decisions, danger, and delivery remain necessary. A repeatable Production process consuming acquired inputs, an ordinary replanted farm, an objective-generated finite deposit, and a naturally recurring opportunity remain distinguishable behaviors; this section does not make every resource regenerative.

**CURRENT WORKING DIRECTION.** Availability cycles and current manifestations give recurring opportunities temporal variation. Day/night variants, especially Mob Swarm composition, belong in the future model, with exact species, quantities, timing, and any phase gating Open. Cooldowns can encourage circulation: exploit one place, pursue another task or opportunity, then revisit. This is a possible incentive, not a rule that forbids camping or settlement; multiple opportunities, safety, travel cost, and demand may support sustained local activity.

### MNOP and Q generate Strategic Exposure

**CANON / ESTABLISHED distinction.** A depth-Z region MNOP may contain strategic location Q, or lie on approaches used to reach Q. Q may be a POI, active Worksite, team objective, village, junction, crossing, or another strategically relevant destination. Q gives other players reasons to enter or traverse the surrounding geography independently of the resource being evaluated. This creates Strategic Exposure and potentially recurring contest, raids, interference, or interception.

Q need not sit on the resource. Its traffic field follows geography and connectivity: passes, basins, crossings, approach alternatives, tunnels, waterways, and developed connections. A circle around Q cannot by itself describe that field. Multiple Q locations, their activity, and team access may combine or redirect traffic. The exposure model is Working; no universal radius or traffic rate is established.

**Logistical remoteness** concerns the difficulty of reaching, provisioning, exploiting, and moving cargo from a place. **Strategic Exposure** concerns other players' reasons and ability to pass through or contest it. **Security** additionally depends on control, information, defenses, and player response. Exposure is neither automatic defeat nor a synonym for difficult terrain or defensibility.

Infrastructure and ordinary world development can conquer logistical remoteness without necessarily eliminating contestability. A well-supplied deep base may remain unsafe because Q continues attracting traffic. Bridges, tunnels, roads, storage, recognized Routes, Supply Lines, and defensive construction can substantially reduce costs and improve security; none automatically removes enemy reasons to visit. Conversely, low-exposure remote economic settlements are valid outcomes. Do not place Q everywhere to prohibit them or require every deep region to be heavily contested.

POIs, Worksites, and objectives therefore have a secondary map-design function: they generate traffic through neighboring geography as well as hosting their primary activity. Evaluate approach corridors and surrounding opportunities, not just the objective footprint. Changing infrastructure may redirect that traffic; exposure is not frozen merely because its generating geography was authored first.


## Resource functional families

These functional families are useful for generation and resource-packing
analysis.

### Construction

- soil/dirt;
- stone/cobble;
- wood;
- sand;
- gravel;
- clay-like materials.

### Extraction

- coal/fuel;
- iron;
- copper;
- gold;
- redstone-like utility ores;
- rare ores;
- exposed/deep deposits.

### Food / Development

- animals;
- crops;
- seeds;
- mushrooms/forage;
- farmland;
- water.

### Ecological / Specialized

- flowers;
- vines;
- pumpkins;
- unusual plants/crops;
- biome/ecology-specific resources.

### Infrastructure

- wood;
- stone;
- iron;
- gravel;
- sand;
- water/ice;
- usable terrain.

### Exceptional / Objective

- unusual concentrations;
- exceptional extraction opportunities;
- objective-embedded resources such as Bastion gold;
- other phase-appropriate objective-activated opportunities.


## Livestock ecology — Prototype contract

Current first prototype contract guarantees map-wide presence of:

- cows;
- sheep;
- pigs;
- chickens.

Use at least:

**four broad mixed livestock ecological ranges/locales**

These are not fixed herd coordinates.

Each represents:

- suitable broad ecology;
- variable herd location;
- variable composition;
- physical animals.

All four baseline livestock species should exist somewhere map-wide.

Normal/random Minecraft animals may also spawn.

Long-term replenishment should preferably behave as population/ecology
maintenance rather than periodically spawning identical herds at fixed
coordinates.

Exact replenishment behavior remains unresolved.


## Regenerative source depth gradient — Working

**Working direction, 12 September 2026.** The three regenerative buckets — crops and plant resources, animal populations, and hostile Mob Swarms — should be generated with a spatial depth and value gradient. The economic framing belongs to [objectives.md](objectives.md#17b-regenerative-sources); the spatial rule is recorded here.

**Established clarification, 22 September 2026:** Strategic Depth answers
**how rich/novel may an opportunity be?** Regional Character answers **which
kinds fit this geography/ecology?** Lower Strategic Depth generally supports
less-rich regenerative opportunities in both quantity/concentration and
novelty/specialization. Deeper Wilderness may support larger concentrations,
more specialized kinds, or both. This is not a monotonic guarantee of actual
resource density in every region, nor an instruction to fill a quota.

The supported region supplies distributed useful subsistence/opportunity without
a nearby renewable normally becoming an obvious full-team opening objective.
North and South need neither identical species nor identical source counts.
Functional opening opportunity is the concern. The former "distance from the
midpoint" rule conflated Regional Character with Strategic Depth and is
**superseded**. Exact depth tiers, richness values, species tables and recovery
curves remain **Open**.

**Implementation status, 22 September 2026:** runtime Source/Kind,
depletion/recovery and manifestation machinery exists. `RenewableKinds`
deliberately carries identity/type/harvest vocabulary, **no depth, value or
region**. Current source capacities/recoverTicks and temporal/eligibility values
are analytical fixtures or Working calibration, not a generic depth-gradient
implementation. Portfolio species selection and equal-count profile constraints
also remain prototype heuristics, not this doctrine implemented. See the
[audit](docs/audit/2026-09-22-spatial-doctrine.md).

### Crops and plant resources

Near, basic and staple candidates are wheat, carrots, potatoes and beetroot, serving food, the basic renewable economy, and common Development opportunity. Intermediate and specialized candidates include pumpkin, melon, sugar cane, cocoa beans and sweet berries, which begin to carry more specific downstream value through recipes, class interactions, utility, biome identity and production inputs. Deep and niche candidates include bamboo, cactus, nether wart, glow berries, torchflower and pitcher plant.

These inherited lists are **Historical illustrative candidates**, neither canon
nor exhaustive; their near/intermediate/deep grouping is not a species table. Deep resources should only matter if they have real match utility: do not populate deep plant sites with vanilla resources nobody has reason to value. Deep regenerative resources are a good home for narrower class-specific demand.

Do not automatically treat all renewable vegetation as crop sites. Trees, logs and flowers enter this bucket only where they function as deliberately valuable renewable plant resources.

### Animal populations

"Herds" remains useful player-facing language, but the underlying system is better treated internally as **animal populations**, which also covers schools, colonies, biome-specific populations and aquatic populations.

**Historical illustrative candidates, not assigned tiers:** Near and common candidates are cows, pigs, sheep and chickens, solving general needs such as food, leather, wool, feathers and eggs — consistent with the existing livestock prototype contract above. Intermediate and specialized candidates include rabbits, bees, horses, donkeys and goats, providing mobility, transport, honey and wax, class interactions and specialized drops. Deep and niche candidates include llamas, squid, glow squid, turtles, frogs, axolotls and other biome-specific or unusual populations.

[OPEN] Exact placement and distance tier for each species is not settled. The system should support region-specific populations such as ocean and coast resources, mountain-specific animals, and biome-specific utility species.

### Mob Swarms

Mob Swarm composition depends on spatial depth, regional and biome identity, and day/night state rather than on a difficulty tier alone. Composition rules and the day/night axis belong to [objectives.md](objectives.md#mob-swarms); generation only needs to supply the regional and depth identity those rules read from.

[OPEN] Exact crop, animal and Mob Swarm distance tables and regional resource tables are unresolved.


## Horses — Prototype contract

Horses are separately map-guaranteed.

They function primarily as a transportation/logistics resource.

They do not need to occupy one of the four baseline livestock ranges.

Exact horse distribution remains open.


## Baseline crops — Prototype contract

Default guarantees multiple starter occurrences of:

- wheat;
- carrots;
- potatoes.

These should appear plausibly through:

- wild/naturalized patches;
- settlement adjacency;
- farmland-related contexts.

Do not make them read as arbitrary loot stamps.

Because these crops are baseline map vocabulary, they should not become the
unique reward vocabulary of Supply Chain.


## Sand, gravel, and snow — Prototype contract

Meaningful map-level access should be guaranteed to:

- sand;
- gravel;
- snow.

Sand and gravel should occur as actual surface/geological formations.

Snow is provided through the high-altitude western cold ecology.


## Ore / geological geography — Working

Relevant ore vocabulary includes:

- coal;
- iron;
- copper;
- gold;
- redstone;
- lapis;
- diamond;
- emerald.

Resources should exist as spatial geological situations:

- buried;
- exposed;
- cave-linked;
- mountain-linked;
- Route-adjacent;
- deep Wilderness.

Strategic value can depend on:

- abundance;
- concentration;
- exposure;
- travel;
- excavation cost;
- local scarcity;
- Route relationship;
- defensibility;
- transport.

Resource regions should be **derived from actual resource instances**, not
created first as arbitrary labeled polygons.


## Food as geography — Established implication

Food changes effective map distance.

A:

- village;
- herd;
- crop patch;
- farm;

may become:

- resupply;
- expedition staging;
- return-trip insurance;
- a contested logistical waypoint.

Resource geography and traversal geography therefore cannot be designed
independently.

A farther destination with reliable food along the journey may be
operationally closer than a physically nearer destination through barren
terrain.


# Traversal and map scale

## Hunger as expedition capacity — Prototype context

The current map prototype uses:

### L1 test state

- 5 hunger icons;
- 10 food points;
- 0 / effectively 0 saturation.

### L3 Hunger test state

- 6 hunger icons;
- 12 food points;
- 0 / effectively 0 saturation.

These are **prototype test settings**, not finalized universal progression
rules.

Vanilla sprint cutoff remains:

- 3 hunger icons;
- 6 food points.

No custom sprint threshold is required.

At L1, the visible sprint-capable reserve is approximately:

10 food
→ 6-food cutoff
= 4 food points
≈ 16 exhaustion.

At L3 Hunger:

12 food
→ 6-food cutoff
= 6 food points
≈ 24 exhaustion.

The +1-icon prototype therefore represents approximately:

- +20% total displayed Hunger;
- +50% pre-cutoff sprint-capable reserve.

This should be tested as a meaningful early expedition/logistics choice.


## Walking as the failure state

Once sprint is lost, ordinary walking does not aggressively drain Hunger.

Underprovisioning should therefore often mean:

- slower return;
- greater vulnerability;
- lost tempo;
- reduced tactical flexibility;

rather than:

"The game physically prohibits you from traveling farther."

Players can trade **time for range**.

Food, Routes, mounts, infrastructure, class mobility, and planning can extend
operating range further.


## Expeditions must be evaluated round-trip

Do not evaluate accessibility using one-way travel alone.

A complete expedition budget includes:

- outbound travel;
- searching;
- gathering/mining;
- possible fighting;
- hauling;
- return travel;
- safety reserve.

Being able to reach a place once does not mean a player can comfortably
operate there.


## Map-scale problem — Working

Default may require approximately:

**20–30% more linear macro scale**

than the procedural solution-space prototypes.

The purpose is not merely longer travel.

The map needs enough physical room for:

- villages to feel distinct;
- forests to possess interiors;
- mountains to possess depth;
- livestock to occupy separate ecological ranges;
- resources to occupy distinct geography;
- Routes to diverge;
- POIs to possess surrounding terrain;
- exploration to contain genuine travel;
- Logistics to emerge naturally.

Do not manufacture geographic distance primarily through:

- tiny Hunger capacity;
- arbitrary blockers;
- obnoxious terrain;
- visibly nearby but artificially inaccessible destinations.

Real physical geography should create expedition scale before artificial
gating does.


## +25% first scale test — Prototype

Current first test:

**approximately +25% macro linear Wilderness/resource separation**

This is **not** a uniform ×1.25 transformation.

Preserve approximately the same physical scale for:

- homelands;
- villages;
- individual POIs;
- Route width;
- trees;
- individual structures;
- individual resource patches/formations.

Spend the added area primarily on:

- connective Wilderness;
- village separation;
- resource separation;
- Route divergence;
- mountain depth;
- forest depth;
- regional identity;
- meaningful empty terrain.

A +25% increase in both horizontal dimensions produces approximately:

**56% more surface area**

which gives resource/ecological systems substantially more room without
requiring individual features to become oversized.


## Nonuniform expansion — Prototype

Expansion should accumulate with depth from the homeland.

Conceptually:

Home
→ short transition
→ Near Wilderness
→ Local opportunities
→ Regional Wilderness
→ Deep Wilderness / midmap

Approximate design vocabulary:

- Near Wilderness: ~1.0–1.1×
- Local → regional spacing: ~1.2–1.3×
- Deep Wilderness: ~1.3–1.4×
- Mountain internal depth: potentially ~1.3×+
- Overall macro dimensions: approximately ~1.25×

These are design guides rather than rigid formulas.


## Travel-band hypothesis — Prototype

Measured roughly from homeland edge / developed territory:

### Immediate outskirts

~0–40 blocks

Essentially local.

### Near Wilderness

~40–80 blocks

Comfortable normal excursion.

### Local opportunities

~80–150 blocks

Meaningful travel/Hunger choice.

### Regional Wilderness

~150–250 blocks

Provisioning and infrastructure become increasingly relevant.

### Deep Wilderness

~250–375+ blocks

Genuine expedition territory.

These are not literal circular rings.

Effective distance depends on:

- terrain;
- Route geometry;
- elevation;
- food;
- Hunger;
- walking;
- sprinting;
- classes;
- mounts;
- combat;
- haul load;
- infrastructure.


## Earlier generator dimensions — Historical reference

The 50-variant procedural generator used approximately:

X:
-336 .. 335
≈ 672 blocks

Z:
-416 .. 415
≈ 832 blocks

Sea level:
64

Working Y range:
approximately 48–111

A naive +25% rectangular equivalent would be approximately:

X ≈ 840 blocks
Z ≈ 1040 blocks

roughly:

X:
-420 .. 419

Z:
-520 .. 519

This is only a dimensional reference.

The intended scale prototype should use **nonuniform expansion**, not simple
coordinate multiplication.

Earlier exploratory targets around **1,300 × 1,000 blocks** and Aether
Fountains roughly **900–1,100 blocks apart** are superseded as the immediate
prototype specification.

Final scale remains unresolved pending actual traversal and resource-packing
tests.


# Seed selection and validation

## Seed-selection philosophy — Established direction

A useful precedent is Minecraft speedrunning seed filtering:

**let Minecraft generate Minecraft, then inspect whether the generated
seed/region is suitable for competitive play.**

Different map archetypes may eventually function partly as different
generation/seed-selection contracts rather than wholly separate handcrafted
terrain generators.


## Preferred Default generation pipeline — Working

Use the future pipeline stated in the competitive spatial contract:

1. Recognize the selected Map Type's macro-geography.
2. Evaluate its team topology.
3. Find two viable Homebase Sockets for the same Core/interface contract.
4. Evaluate natural Wilderness viability, actual resources/POIs and geographic character.
5. Establish authorability with bounded Core integration, supported-region Route
   scope, and functional opening opportunity; derive resource regions from real
   instances rather than statistical mirroring.
6. Physically verify the authored result and its interfaces.
7. Produce a PlayableMap only with declared checks and remaining uncertainties.

These are future responsibilities, not new recognizers or gates implemented by
this document. Reject unsuitable sockets rather than expanding terrain surgery.


## Validation — Working

Validation should distinguish the controlled Core/Socket contract from natural
terrain observations and demonstrated gameplay consequences. Geometry checks
inside the Core are binding; a Wilderness N/S terrain gap alone is not a failed
competitive test. Preserve raw terrain, travel and opportunity measurements.

Potential dimensions include:

- required-resource existence;
- abundance;
- concentration;
- renewable access;
- livestock distribution;
- crop distribution;
- village portfolios;
- geological situations;
- team travel cost;
- Route relationships;
- terrain difficulty;
- forest depth;
- mountain depth;
- POI accessibility;
- contestability;
- Core/interface equivalence and socket integration bounds;
- supported-region opening floor and concentration ceiling;
- artificial correction required.

Long-term, straight-line distance should be replaced by movement-aware
traversal cost.

Eventually evaluate approximate vanilla-player movement through actual
terrain, including:

- sprinting;
- jumping;
- slopes;
- water;
- obstacles;
- elevation;
- detours;
- Route surfaces;
- potentially Hunger cost.


## Resource fairness — Working

Fairness does not require:

"the same resource at the same distance for both teams."

Instead ask:

**Does each team have a competitively reasonable portfolio of opportunities,
and are asymmetric opportunities contestable?**

Relevant factors include:

- primary access;
- alternate access;
- abundance;
- Route quality;
- terrain;
- timing;
- local scarcity;
- extraction difficulty;
- transportation;
- nearby defenses;
- neighboring resources.


# Player transformation of the map

Terrain difficulty should create opportunities for development rather than
merely permanent taxes.

Players may:

- build roads;
- improve Routes;
- bridge gaps;
- tunnel through mountains;
- establish storage;
- farm;
- breed animals;
- create extraction infrastructure;
- clear terrain;
- establish supply systems;
- create safer movement.

The effective strategic map should therefore change over a match.


# Previous Default-map generation experiments

## Greybox sequence — Historical

### P0

`Minecraft_MOBA_Greybox_P0.zip`

Basic macro-layout test containing:

- North/South bases;
- Routes;
- villages;
- POIs;
- western mountain placeholder;
- eastern coast;
- cave markers.

At render distance 12, the opposing Route was only marginally visible from
one Route endpoint. This scale relationship was liked.

### P1

`Minecraft_MOBA_Greybox_P1.zip`

Added:

- traversal-friction model;
- simple A* surface proxy;
- corrected western-village accessibility.

Also generated:

`Minecraft_MOBA_Greybox_P1_travel_matrix.csv`

### P2A

`Minecraft_MOBA_Greybox_P2A.zip`

Improved Route fan geometry.

Main criticism:

- too flat;
- too conservative.

### P2B

`Minecraft_MOBA_Greybox_P2B.zip`

Added:

- stronger verticality;
- forests;
- pools;
- spires;
- boulders.

Problems included:

- gridlike trees;
- visible capsule-like terrain;
- shallow fluids;
- unnatural substrate relationships;
- excessive authored appearance.

### P2C

`Minecraft_MOBA_Greybox_P2C.zip`

Improved:

- forest irregularity;
- tree scale;
- fallen logs;
- mushrooms;
- pools;
- foothill gradient;
- Route preservation.

### P2D FINAL

`Minecraft_MOBA_Greybox_P2D_FINAL.zip`

Improved:

- mountain geology;
- hydrology;
- mountain elevation roughly Y60–109;
- naturalized transitions;
- connected water;
- Route preservation.

P2D remains a useful terrain-generation reference.

It is not final Default.


## Previous generation method — Historical

Earlier playable greyboxes were generated programmatically as Minecraft
Java/Anvil saves.

They were **not** WorldPainter-authored worlds.

WorldPainter may still be useful in future, but the resulting playable
geography matters more than the authoring application.


## 50-variant solution-space experiment — Historical

A procedural generator produced:

**50 accepted Default-map variants**

to:

- demonstrate a broad valid solution space;
- avoid overfitting Default to one geography;
- test mountain/coast/Route/POI relationships;
- establish validation-driven generation.

Topology families included:

- twin_massif
- split_spine
- horseshoe
- broken_peaks
- long_ridge
- highland_ravines
- offset_basin
- sawtooth_valleys

These were procedural map variants, not 50 playable Java saves.

Representative seeds included:

- Variant03 — 920260902 — offset_basin — baseline
- Variant47 — 920260965 — highland_ravines — traversal stress
- Variant49 — 920260970 — horseshoe — opportunity complexity
- Variant44 — 920260959 — fairness specialist
- Variant45 — 920260960 — space-allocation laboratory
- Variant11 — 920260911 — older resource-geography laboratory
- 920260900 — strongest result in an incomplete first-10 resource ecology audit

None is automatically canonical Default.


## Resource ecology prototype — Historical

A second-stage generator previously existed:

`Minecraft_MOBA_resource_ecology_pipeline.py`

It preserved accepted terrain seeds while overlaying/auditing resource
geography.

It included:

- four livestock ranges;
- cow/sheep/pig/chicken coverage;
- three starter patches each for wheat/carrot/potato;
- sand/gravel formations;
- concrete ore clusters;
- variable village portfolios;
- North/South accessibility proxies;
- resource-fairness metrics;
- contract-gap reporting.

Its counts, radii, densities, populations, and ore totals were prototype
parameters rather than balance decisions.

The early version explicitly lacked snow.

That design gap is now addressed conceptually through the western
high-altitude cold ecology, although implementation remains required.

A full 50-seed resource pass timed out.

The first ten completed.

Seed `920260900` performed strongly within that incomplete sample.

It is not canonical.


# Next Default-map prototype

## Minecraft_MOBA_Default_Scale_Test — Historical prototype target

The historical target below was a **new independent Minecraft Java Default-map
prototype**. Its dimensions, counts and Route reach remain prototype context,
not instructions to override the clarified Core/Socket contract.

Do not modify P2D in place.

The exact target Java version remains unresolved and should be chosen before
generation. A modern Java version is preferable if an embedded datapack is
used natively, but this is not yet a finalized version requirement.


### Macro structure

Include:

- North homeland;
- South homeland;
- three authored Routes per team;
- large shared Wilderness;
- western mountain/highlands;
- eastern coast/ocean.


### Western region

Include:

- deep highland geography;
- ridges;
- valleys;
- basins;
- caves;
- integrated high-altitude cold/snow ecology.


### Ecology

Include:

- forests;
- at least four mixed livestock ranges;
- cows;
- sheep;
- pigs;
- chickens;
- separate horse presence;
- multiple wheat patches;
- multiple carrot patches;
- multiple potato patches.


### Geology

Include:

- sand;
- gravel;
- caves;
- coal;
- iron;
- copper;
- gold;
- redstone;
- lapis;
- diamond;
- emerald.


### Strategic geography

Include:

- villages;
- major POIs;
- minor POIs;
- meaningful empty connective Wilderness.


### Scale

Use approximately:

**+25% macro linear Wilderness/resource separation**

implemented **nonuniformly**.

Do not uniformly enlarge every feature.

Most additional area should appear:

- between meaningful resources and places;
- inside deep Wilderness;
- within mountain/forest depth;
- between major Route/opportunity systems.


### Test harness

If technically practical, the prototype should support:

L1 test:
- food = 10;
- saturation = 0.

L3 Hunger test:
- food = 12;
- saturation = 0.

Control:
- normal/unlimited food if useful.

Retain vanilla sprint cutoff at:
- 6 food / 3 icons.

Authored Routes:
- no speed bonus;
- provisional ~10% locomotion-exhaustion efficiency.

Useful journey instrumentation includes:

- journey start/end;
- elapsed time;
- start/end coordinates;
- distance;
- Route distance/time;
- off-Route distance/time;
- starting/current food;
- food consumed;
- sprint-loss state;
- current test mode.

Vanilla datapacks do not expose a simple way to multiply only
locomotion-generated exhaustion by 0.9.

Any datapack implementation of that behavior should therefore be documented as
an approximation unless a more exact implementation method is found.


# Next spatial analysis

**22 September 2026 scope correction:** the packing list below is a historical
prototype demand scenario. Core, supported-region and Wilderness requirements
are distinct; asymmetric usable terrain is not itself a packing failure.

After or alongside the first +25% scale prototype, perform a **resource-packing
analysis**.

**12 September clarification:** The historical counts below belong to that prototype. For initial economic balancing, use the spatial-opportunity principles above and the opportunity-field model in docs/worldgen-initial-balancing.md. Resource packing must not become a regenerative-node quota or a promise of income.

Consider the territorial requirements and desired separation of:

- homelands;
- six authored Route branches;
- villages;
- four livestock ranges;
- horses;
- crop patches;
- forests;
- sand;
- gravel;
- ordinary geology;
- exceptional geology;
- major POIs;
- minor POIs;
- snow ecology;
- mountain interior;
- coast;
- caves;
- connective empty Wilderness.

The important question is not simply:

"Can all of these objects physically fit?"

It is:

"Can players encounter them as distinct geographic and strategic decisions
rather than as one compressed resource buffet?"


# Underused Overworld generation — Working direction

Default should particularly value Minecraft features that ordinary survival
often underuses or quickly outgrows.

Candidates include:

- desert wells;
- fossils;
- igloos;
- swamp huts;
- ruined portals;
- ocean ruins;
- trail ruins;
- geodes;
- mineshafts;
- monster rooms;
- unusual cave/aquifer formations;
- biome-specific terrain quirks.

These should retain their Minecraft identity.

Classes and systems can create additional reasons to care about them rather
than replacing them with generic MOBA camps or tokens.


# Map Type doctrine — Working, 24 September 2026

These are future **Map Types** (earlier terminology: archetypes), not implemented
recognizers. Default's regional gradient is its own search contract, not a
universal composition constraint.

## What a Map Type is

**A Map Type is a world hypothesis, not a biome preset.** "Desert" is weak by
itself; *what if an otherwise viable competitive world developed around an
enormous arid region where biological abundance is rare* is a premise. The
type is the strategic distortion, and the biome is how the world happens to
express it.

Each Type should be summarisable in one **geographic thesis** sentence. If the
sentence cannot be written, the proposal is not differentiated enough to be a
Type.

- Default: opportunity is distributed through a diverse Minecraft wilderness.
- Archipelago: opportunity is fragmented across land separated by cheap
  maritime movement.
- Underground: opportunity is abundant but hidden inside a rewritable network.
- Arid: productive abundance is rare and geographically concentrated.
- Pale Forest: information is scarce around a highly concentrated central prize.
- Sky Islands: valuable geography exists above a barrier that breaks ordinary
  horizontal expansion.

### Prefer extreme vanilla phenomena over invented terrain

Minecraft occasionally produces floating terrain, enormous caves, isolated
mountains, huge oceans, cliff faces, ravines, enclosed valleys and structure
clusters. A compiler can search **for** those outliers rather than rejecting
them, then bind the competitive requirements onto what it finds. The aim is a
map that feels like an extraordinary Minecraft seed, not a Minecraft-themed
arena.

### A Type is a (feature, location) pair, not a feature

Pale Forest works because the Mansion is **central**. The same Mansion at the
scoop edge is a different map. Location is co-equal with presence, and a Type
definition that names only a feature will match worlds that do not play like
it. This is the same correction that turned the fixed 864x1056 window into a
searched one: *what* is present was never the whole question.

### Types are not a partition

A scoop may match several Types or none. The unmatched case matters more than
the overlapping one: it is where a Type nobody has defined shows up, and it
has to survive measurement rather than be discarded. See "describe, then
label" below.

## The two axes, and why they must not be one score

**Extremeness serves interest, novelty and legibility. Symmetry serves
competitive viability.** They are separate axes and must never be collapsed
into a single score, because they are in direct measured tension.

Over 1,073 scoops on 8 seeds, mirror deviation scales with relief almost
proportionally:

| relief quartile | relief | deviation | deviation/relief |
|---|---|---|---|
| flattest 25% | 7.0 | 1.23 | 0.175 |
| 2nd | 17.0 | 3.81 | 0.224 |
| 3rd | 37.0 | 7.16 | 0.194 |
| most extreme 25% | 60.0 | 14.52 | 0.242 |

So **ranking on raw symmetry is ranking on flatness**, and the flattest places
are water. A blind symmetry search on a seed that is 31% ocean returned scoops
that were 97-99% water in its top three and no landmass scoop at all in its
top eight. That is the same sink as the superseded "more ocean is better"
window scoring, reached from the opposite direction by a measure that never
mentions ocean.

**Symmetry is therefore gated on the ratio, and relief is recorded but not
ranked.** Two seeds with the same valley shape, one 10 blocks tall and one
100, are equally symmetric at equal ratio and are equally worth evaluating;
the magnitude difference is an aesthetic and legibility factor identified
downstream. Ranked on raw deviation the 100-block valley never surfaces, since
a deviation catastrophic on a 10-block feature is excellent on a 100-block one.

### Symmetry is measured over contested ground

A scoop half covered in ocean gets half its mirrored pairs for free, because
water matches water exactly. Measured over the whole scoop the ratio *falls*
as water rises (0.246 driest quartile to 0.182 wettest), which reads as a
water map tolerating more asymmetry. It does not; it is being flattered.
Restricted to pairs where both ends are contested land, the ordering reverses
and the spread nearly closes: 0.269, 0.276, 0.279, 0.296.

**[OPEN]** Most of the apparent case for per-Type symmetry bounds was this
denominator error rather than a property of the Types. A ~10% residual spread
remains unexplained. Writing per-Type bounds before it is explained would
encode a measurement artefact. Extreme Types are expected to need better
tutoring and filtering rather than looser symmetry.

**[OPEN]** Whether forest canopy is contested ground. Surface height reads
tree tops as terrain, which would distort every wooded Type.

## Symmetry first, then distortion

Filtering runs **hardest-to-author first**. Whole-scoop symmetry is the
hardest constraint and therefore the first gate: a Type that cannot be
satisfied symmetrically is not a Map Type. That every candidate Type below is
centre- or axis-symmetric — barrier in the middle, Mansion in the middle,
basin in the middle — is the constraint showing through, not a coincidence.

Homebase sockets and the opening Hinterland are immediately downstream;
Worksite, Lair and objective siting downstream of those; and what the map
uniquely offers — terrain features, structures, POIs — after that.

## Every distortion needs a corresponding opportunity

Poor visibility needs concealed concentrated value. Resource poverty needs a
destination worth leaving home for. Ocean fragmentation needs fast boats and
distributed islands. Underground food scarcity needs mineral abundance.
Extreme verticality needs valuable vertical destinations.

Compensation need not be mathematically equal, but **a map must not simply
make Minecraft harder. Constraint should produce strategy.**

This is the standing bound and the automatic filter against gimmick maps: a
map that is only harder fails it by construction. It is a **relation between
two measured quantities at two locations** — poverty here, concentration there
— not a threshold on any single metric, which is why it can reject a scoop
without a per-Type parameter.

## Maps alter the solution space, not the destination

The same player should make different decisions on different maps while still
recognising the same macro-game: establish, explore, specialise/invest,
contest, escalate, resolve. A Type changes what players value, how they move,
what information they hold and where they invest. It does not change the
match lifecycle or expected tempo.

## Strategic variables a Type may distort

A Type may primarily distort abundance, information, connectivity, movement
cost, value concentration, verticality, route topology or exposure, and may
combine two or three. This is a **description vocabulary computed for every
scoop**, not a set of gates; a Type is a region in that space.

Measurable today, against what exists:

| variable | status |
|---|---|
| verticality | measured (`prominence`, both signs; `scoop` relief) |
| connectivity | partial (`scoop.water_structure` component analysis) |
| route topology | partial (`cell_grid`, `shortest`) |
| movement cost | partial (`task_a` traversal cost) |
| abundance | not measured |
| information | not measured (`task_a` lists visibility as UNAVAILABLE) |
| value concentration | not measured |
| exposure | not measured |

**The vocabulary must be Type-neutral or it will only ever find Default.**
Seven of the 25 existing metrics are one-sided: `highland_fraction_west`
returns a number about the wrong place for an eastern highland rather than a
low number. Any bound built on those can only recognise Default, whatever
order it is evaluated in.

### Describe, then label

Bounds are not the problem; **bounds applied to a fixed window** were. Default
rejected 82% of seeds while rejecting most of them for being in the wrong
*place*. Bounds plus search is a different thing, and telling the search what
several Types look like is strictly more informative than telling it about one.

Type definitions therefore **tutor** the search: budget is spent per Type so a
scarce kind competes against its own kind rather than being outranked by a
thousand symmetric plains. Tutoring is required, not optional, because an
untutored ranking collapses to the flattest and emptiest scoop available.

A Type definition must steer **without becoming the discard rule**. Every
scoop keeps its full measured vector whatever it matched; unmatched scoops are
measured and returned; a Type matching nothing is reported as matching
nothing; and a search lacking an input says which input it lacked rather than
reporting an absence.

This is already canonical for Resource Density, which the compiler discovers
and measures rather than being asked to produce.

## Cost classes

Not uniform, and it determines what is buildable now.

- **Scannable off-seed (~1s per 8192-block square).** Elevation, biome, land
  and water arrangement, and structure placement. Most Types sit here.
- **Requires generation.** Underground and Karst. cubiomes models biome noise
  and approximate surface height; caves and ravines are carvers it does not
  generate. `dripstone_caves` and `lush_caves` exist only as biomes, so their
  locations are findable and their geometry is not.

Approximate height is trusted to rank symmetry and find flat ground, not to
certify a build site: 81% of samples within 5 blocks, 93% within 10.

## Legibility, in light of the draft

Players already know **Map Type, Scale and Resource Density** before the match
(see `docs/design/MATCH_LIFECYCLE_OBJECTIVES_AND_OPENING_DECISIONS.md`); the
information boundary is that players know the broad classification and
discover the realization. They also arrive at it deliberately: the draft is
**class-first, then map ban/counterpick across the Types in rotation**, so a
Type is chosen against known compositions and its broad character is
information both teams have acted on. Legibility therefore does **not** carry the
classification load. A player in a Pale Forest map already knows it is one.

What legibility carries is the **realization**: reading local value and
navigation inside a map whose Type is known. That splits cleanly:

- **Biome-signalled** — free, supplied by the seed. Pale oak says visibility
  is poor; an oasis says fertile ground is unusual here.
- **Topological** — an elevated rim does not announce that the low centre is
  rich. This needs authoring, and it is the same problem the objective
  legibility prototypes hit and did not solve. **[OPEN]**

## Scale

Map Scale is an independent axis already established in the lifecycle design.
The compiler need not derive it as a setting: **scoop area and Homebase
separation** are the two scalars that distinguish Normal from Large from Vast,
and both fall out of a scoop for free. Where they cut into named Scales is a
labelling decision and is deliberately not fixed here.

Scale interacts with every Type — an Archipelago at Normal is a different game
from one at Vast — so a Type may legitimately appear in a draft more than once
at different Scales.

## Candidate Types

**Working.** Some of these will collapse into families rather than deserving
separate formal archetypes; Sky Islands, Great River/Canyon, Crater/Basin and
Mountain Range are all peak-or-pit prominence plus a location term, and
prominence is the axis they collapse along. Naming them is the exercise that
reveals the dimensions.

| Map Type | Worldgen premise | Strategic distortion |
|---|---|---|
| Default / Overworld | Diverse, readable Overworld terrain | Baseline mixed economy and navigation |
| Archipelago | Islands separated by substantial ocean | Water becomes connective terrain; land and resources fragmented |
| Underground | Huge interconnected cave system | Ore-rich, food/renewables-poor; topology replaces surface distance |
| Arid / Dunes | Vast desert/badlands with rare fertile pockets | Common biological resources become strategically concentrated |
| Pale Forest / Mansion | Dense pale forest surrounding a central Mansion | Low information and resource poverty outside; extreme central concentration |
| Sky Islands | Impassable or very costly surface divide with naturally generated floating terrain spanning it | Vertical exploration and aerial connectivity become the viable crossing |
| Mountain Range | Extreme peaks and ridges divide the world | Vertical travel, passes, tunnels and overlooks determine movement |
| Great River / Canyon | Enormous river or ravine forms the primary axis | Crossings become strategic; movement along the axis is easy, across it costly |
| Swamplands | Large swamp/mangrove complex with waterways and broken sightlines | Movement technically open but awkward; local navigation and visibility matter |
| Frozen | Snowy peaks, groves, frozen rivers and ocean, taiga | Ice creates very fast natural routes while snow and vertical terrain complicate off-route movement |
| Badlands / Mesa | Terraced badlands with exposed mineshafts, poorer surrounds | Extreme mineral readability; vertical shelves create natural territorial layers |
| Jungle / Lost City | Extremely dense jungle around temples, villages and valuable clearings | Information and traversal are expensive; discovered clearings become anchors |
| Karst / Sinkholes | Surface punctured by enormous cave mouths, ravines and underground connections | Surface and underground become two overlapping route networks |
| Shattered Coast | Cliffs, coves, peninsulas and offshore islands rather than full archipelago | Land and sea are both viable networks and repeatedly intersect |
| Crater / Basin | Teams begin around elevated exterior terrain with an unusually rich low central basin | Entering the centre is easy; leaving while carrying value is the commitment |
| Highlands / Valley | Valuable lowland corridor surrounded by resource-bearing mountains | Economic movement concentrates in the valley while extraction pulls outward and upward |

### Landmarks organise economically without becoming artificial objectives

Players should care about a Woodland Mansion because world generation placed
unusual opportunity there, not because the game says "capture Woodland
Mansion." The Mansion's Minecraft identity should matter: enormous,
compartmentalised, defensible, dangerous, hard to understand from outside,
many entrances and floors. Its grounds being lucrative is what prevents the
map collapsing into a fight inside one building.

## Valley / Default

Balanced baseline.

Ordinary Overworld terrain and generation.

All avenues broadly viable; strategic variation comes from the seed rather
than a fixed arena.

Default's current specific grammar includes:

- North/South team opposition;
- western highlands;
- eastern coast/ocean;
- shared Wilderness;
- authored Routes;
- Minecraft-first resource ecology.


## Chasm

Vertical ravine/cavern geometry emphasizing:

- bridging;
- cliff Routes;
- tunneling;
- vertical transportation.

Problems should admit multiple Minecraft solutions:

- across;
- around;
- through;
- down.


## Archipelago

Water-separated resources and objectives.

Potential techniques include:

- boats;
- bridges;
- underwater tunnels;
- docks;
- ice Routes;
- later advanced transportation.

Logistics becomes unusually central.


## Underdark

Predominantly underground and initially unknown.

Emphasizes:

- cave reading;
- tunneling;
- lighting;
- sound;
- verticality.

Player-created tunnels may effectively become the match's Routes.


## Canopy

Large forest/jungle with:

- canopy;
- ground;
- underground layers.

Encourages three-dimensional Route networks.


## Wastes

Scarcity-focused desert/badlands/volcanic family.

Emphasizes:

- renewable resources;
- settlement expansion;
- wood/food access;
- concentrated deposits.

Map archetypes should foreground different Minecraft traversal/building
techniques without arbitrary class modifiers such as:

"+20% Mole power on cave map."