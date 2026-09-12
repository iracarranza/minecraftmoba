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
- authored Routes projecting from each homeland into the Wilderness

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

Baseline homeland opportunity should be **competitively equivalent**.

Literal block-for-block mirroring is not required.

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


## Authored Routes — Working

Each team has **three major authored Route branches** projecting from its
homeland.

These are physical Minecraft infrastructure, not invisible MOBA lanes.

Villages and major POIs have visible authored Route infrastructure leading
toward them.

A destination does not necessarily sit directly on a Route, and players remain
free to approach it off-route, but the Route relationship is an intentional
part of Default's generated infrastructure rather than merely likely
proximity.

Routes may also lead toward other strategically useful Wilderness areas.

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
authored western Routes should become correspondingly more strategically
valuable even before accounting for any explicit Route locomotion-efficiency
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

- baseline homeland opportunity is competitively equivalent;
- Wilderness terrain may be asymmetric;
- resource opportunity may be asymmetric;
- villages may be asymmetric;
- the generator/validator evaluates strategic opportunity rather than
  identical geometry.

Three useful opportunity categories remain:

1. **Baseline**
   Fundamental starting access whose competitive value is deliberately
   equivalent.

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

Regenerating resources increase in **economic specificity** with distance from the midpoint and base regions. This does not mean farther equals more XP, or the same resource in larger stacks. Near and core resources solve broad universal needs; deeper resources are newer, rarer, more specialized or composition-dependent, and support narrower but stronger strategies. Distance increases specialization, niche utility, strategic value and sometimes challenge.

### Crops and plant resources

Near, basic and staple candidates are wheat, carrots, potatoes and beetroot, serving food, the basic renewable economy, and common Development opportunity. Intermediate and specialized candidates include pumpkin, melon, sugar cane, cocoa beans and sweet berries, which begin to carry more specific downstream value through recipes, class interactions, utility, biome identity and production inputs. Deep and niche candidates include bamboo, cactus, nether wart, glow berries, torchflower and pitcher plant.

These lists are neither canon nor exhaustive. Deep resources should only matter if they have real match utility: do not populate deep plant sites with vanilla resources nobody has reason to value. Deep regenerative resources are a good home for narrower class-specific demand.

Do not automatically treat all renewable vegetation as crop sites. Trees, logs and flowers enter this bucket only where they function as deliberately valuable renewable plant resources.

### Animal populations

"Herds" remains useful player-facing language, but the underlying system is better treated internally as **animal populations**, which also covers schools, colonies, biome-specific populations and aquatic populations.

Near and common candidates are cows, pigs, sheep and chickens, solving general needs such as food, leather, wool, feathers and eggs — consistent with the existing livestock prototype contract above. Intermediate and specialized candidates include rabbits, bees, horses, donkeys and goats, providing mobility, transport, honey and wax, class interactions and specialized drops. Deep and niche candidates include llamas, squid, glow squid, turtles, frogs, axolotls and other biome-specific or unusual populations.

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

1. Generate terrain.
2. Locate/classify natural POIs and structures.
3. Identify/place competitively equivalent homelands.
4. Identify actual resource instances.
5. Derive resource regions from those instances.
6. Build/select authored Route relationships.
7. Evaluate strategic opportunity.
8. Modestly adjust resources, ecology, loot, or opportunity where useful.
9. Integrate competitive objectives.
10. Validate.
11. Accept or reject the seed.

If extensive correction is required, reject the seed.


## Validation — Working

Validation should measure strategic opportunity rather than geometry alone.

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
- homeland equivalence;
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

## Minecraft_MOBA_Default_Scale_Test — Immediate implementation target

The next intended artifact is a **new independent Minecraft Java Default-map
prototype**.

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

After or alongside the first +25% scale prototype, perform a **resource-packing
analysis**.

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


# Working map archetypes

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