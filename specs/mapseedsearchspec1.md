# Default Map Seed Search Specification

> **Status:** Active
> **Authority:** Implementation/search specification
> **Related design authority:** `maps.md`
>
> This specification defines how candidate vanilla Minecraft terrain should be
> searched, evaluated, shortlisted, and greyboxed for the Default map.
> `maps.md` remains authoritative for settled game-design decisions if the two
> documents conflict.

---

## 1. Purpose

Find vanilla Minecraft regions that provide an excellent natural substrate for
the Default Minecraft MOBA map using a **fast, inexpensive, staged seed search**.

The search is not expected to find a finished competitive map.

The governing principle is:

> **Seed search selects the natural world. Authorship turns that world into a
> competitive map.**

A candidate should therefore be selected primarily for geographic properties
that are difficult to reproduce convincingly after generation.

Requirements that can be introduced naturally, corrected locally, maintained
by game systems, or explicitly authored later should not unnecessarily reduce
the seed search space.

The target is a world that feels like **Minecraft whose environment has been
unusually well catered to the purposes of a competitive match**, rather than a
conventional MOBA arena decorated with Minecraft terrain.

Vanilla Minecraft remains the governing environmental simulation wherever
practical.

---

# 2. Candidate Definition

A candidate is not merely a Minecraft seed.

A candidate is:

**Seed + playable bounds + logical orientation**

For example:

- Minecraft seed;
- X/Z bounds defining the intended playable area;
- logical North;
- logical South;
- logical West;
- logical East.

The same Minecraft seed may contain multiple viable candidate regions.

Rotation/reflection of the analytical interpretation is allowed. The underlying
Minecraft terrain is not rotated or regenerated; logical map directions are
assigned to the selected region.

All candidate evaluation must concern the intended playable bounds rather than
the effectively infinite surrounding Minecraft world.

---

# 3. Default Map Macro Grammar

The Default map should broadly contain:

- a North/South competitive team axis;
- a viable North homeland region;
- a viable South homeland region;
- a substantial continuous Wilderness between and around them;
- a recognizable central/interior region;
- meaningful expedition depth;
- several legible natural landscape regions;
- increasingly difficult/highland geography generally associated with logical
  West;
- meaningful coast/ocean geography generally associated with logical East;
- multiple natural movement trajectories through the Wilderness;
- meaningful local asymmetry;
- enough natural variation that players must read the generated world rather
  than memorize one exact terrain layout.

This grammar should be recognizable across accepted Default maps without
requiring identical geography.

---

# 4. Orientability

Orientability is an explicit Default-map requirement.

Regional legibility asks:

> **Where am I?**

Orientability asks:

> **Which way am I going?**

Players should be able to develop a rough sense of map direction from the
environment without depending exclusively on coordinates, UI, or memorized
Routes.

## 4.1 West/East Environmental Axis

Logical West should generally be identifiable through combinations of:

- increasing elevation;
- stronger relief;
- mountain systems;
- exposed stone;
- alpine/cold/snow terrain;
- increasingly difficult highland geography.

Logical East should generally be identifiable through combinations of:

- declining/opening terrain;
- coastal geography;
- maritime vegetation/terrain where appropriate;
- bays, beaches, headlands, river mouths, or similar formations;
- eventual ocean.

These are macro gradients, not absolute rules.

An eastern hill or western lake is acceptable. Local geography should remain
natural and irregular.

The test is whether the **overall environmental direction remains
comprehensible**.

## 4.2 North/South Axis

North and South should primarily become legible through:

- clearly separated viable homeland sites;
- eventual authored homeland infrastructure;
- associated Route/infrastructure relationships;
- major natural landmarks where useful.

Seed search must identify and greybox plausible North and South homeland areas.

They need competitive equivalence, not literal terrain symmetry.

## 4.3 Center

The playable area should possess a recognizable central/interior region.

The center does not need to be:

- geometrically centered;
- perfectly flat;
- circular;
- one biome;
- a mandatory objective location.

It should instead function primarily as a **geographic junction**.

Multiple useful movements through the map should naturally encounter, cross,
border, or circulate around this interior.

A broad basin, valley system, open plain, river-crossing region, or convergence
of several formations may all satisfy this role.

## 4.4 Redundant Orientation

Strong candidates should preferably provide multiple orientation cues.

For example:

- West = mountains + elevation + snow;
- East = coast + ocean + lower/open terrain;
- North = North homeland + landmarks;
- South = South homeland + landmarks;
- Center = convergent/open geography.

No single landmark should be required for basic orientation.

---

# 5. Playable Bounds

Every shortlisted candidate must have explicit intended playable bounds.

The bounds establish:

- intended map dimensions;
- what terrain is actually being evaluated;
- logical North/South/East/West;
- homeland separation;
- Wilderness depth;
- relationship between center and map edges.

The search should find a suitable **playable window inside vanilla terrain**,
rather than judge an entire seed globally.

The final game boundary does not need to be solved during seed search.

For inspection and greyboxing, however, the intended bounds must be visually
unambiguous.

---

# 6. Terrain Flow

The terrain should gently guide movement without becoming a set of rigid MOBA
lanes.

Preferred natural movement guidance includes:

- valleys;
- passes;
- saddles;
- open-country corridors;
- forest edges;
- ridgelines;
- mountain spurs;
- riverbanks;
- river crossings;
- coastlines;
- gaps between difficult formations;
- gradual elevation changes.

Terrain should generally create **movement fields rather than movement rails**.

A player should often perceive an easier natural route while retaining
Minecraft-native alternatives.

For example:

- valley = easy movement;
- slope = slower/direct movement;
- pass = convenient mountain crossing;
- cave = potentially dangerous shortcut;
- river = barrier or transport opportunity;
- forest = enclosed route, clearing opportunity, or terrain to modify;
- mountain = climb, pass, detour, or tunnel;
- bay = circumnavigate, bridge, or cross by water.

Major barriers should preferably admit multiple Minecraft-native solutions.

---

# 7. Landscape Regions

The Default should contain several substantial and distinguishable landscape
identities.

A useful target vocabulary is:

1. alpine / icy highland;
2. rocky or high-relief mountain;
3. deep / stepped forest;
4. sparse woodland or transitional open terrain;
5. open grassland / interior;
6. coast;
7. ocean.

This is **not** a hard requirement for:

- exactly seven regions;
- exactly seven vanilla biomes;
- seven equal areas;
- seven West-to-East stripes.

Biome combinations and substitutions are acceptable where they produce the
same functional and perceptual role.

Clearly delineated regions are allowed and potentially desirable.

The map may possess recognizable terrain "bands" or broad strategic regions so
long as:

- transitions remain plausible within Minecraft;
- boundaries are naturally irregular where appropriate;
- terrain does not look painted onto a conventional MOBA board;
- the resulting world remains recognizably Minecraft.

A candidate containing six exceptionally strong landscape identities may be
better than one mechanically containing all seven target categories.

---

# 8. Regional Legibility

Major regions should be recognizable from their interiors, not only from an
overhead biome map.

Useful dimensions of regional identity include:

- biome/palette;
- vegetation density;
- canopy;
- openness;
- elevation;
- relief;
- surface material;
- snow coverage;
- water relationship;
- sightline characteristics.

Regions should preferably contain:

**Core → transition → neighboring region**

Example:

**Deep forest core**
- closed canopy;
- enclosed sightlines;
- substantial forest depth.

**Forest edge**
- broken canopy;
- clearings;
- scattered trees.

**Open interior**
- long sightlines;
- sparse tree population;
- visibly different movement/building space.

Open terrain should actually read as open.

A candidate should not receive strong regional-differentiation credit merely
because a nominal plains biome exists beneath nearly continuous tree cover.

Likewise, highland terrain should ideally read as mountain/highland rather than
only as elevated forest.

---

# 9. Formation Quality

Seed search should prefer strong natural formations that are expensive to
author convincingly.

Examples include:

- mountain complexes;
- ridges;
- valleys;
- passes;
- saddles;
- basins;
- cliffs;
- ravines;
- exposed cave entrances;
- rivers;
- lakes;
- waterfalls;
- bays;
- headlands;
- peninsulas;
- islands;
- river mouths;
- forest clearings;
- isolated peaks;
- other recognizable natural landmarks.

Mountain quality should depend on **internal geography**, not elevation alone.

A useful mountain system may contain:

- multiple approaches;
- lower slopes;
- internal valleys;
- passes;
- ridges;
- exposed rock;
- caves;
- alpine terrain;
- snow;
- neighboring forest/foothills.

---

# 10. Formation Relationships

Formation relationships are at least as important as the presence of the
formations themselves.

The evaluator should prefer candidates where natural features combine into
useful geographic structures.

Examples include:

- open interior → foothills → mountain → alpine;
- forest → foothills → rocky highland;
- mountain → valley/pass → open interior;
- exposed cave entrance → mountain face;
- river → valley;
- river → open interior;
- river → coast;
- forest → clearing;
- forest → open-country edge;
- village-compatible site → natural corridor;
- settlement-compatible site → river crossing;
- mountain pass → settlement-compatible site;
- natural barrier → multiple crossing opportunities;
- interior water → coast/ocean;
- recognizable landmark → important movement corridor.

The search should increasingly evaluate the **network formed by natural
geography**, rather than simply adding independent feature scores.

A mountain, village site, river, and plain that form one coherent geographic
relationship are generally more valuable than the same four features scattered
independently across the map.

---

# 11. Natural Corridors and Future Routes

The terrain should naturally suggest multiple useful movement trajectories.

Final Routes are explicitly authored later.

Seed search should therefore **not require six naturally generated lanes**.

Instead it should ask whether approximately three useful Route relationships
per team could plausibly be fitted into the terrain.

Useful natural Route substrates include:

- valley floors;
- open plains;
- forest edges;
- mountain passes;
- shelves;
- river crossings;
- coastline approaches;
- gaps between formations.

Routes may:

- diverge;
- converge;
- cross regions;
- approach the same region differently;
- use different terrain types;
- differ between North and South.

They do not need literal geometric symmetry.

Terrain should not be rewarded merely because it accidentally forms three
obvious conventional MOBA lanes.

---

# 12. Center as Junction

The central/interior region should generally behave as a junction rather than
a single forced destination.

Several useful trajectories should:

- enter it;
- border it;
- cross it;
- split within it;
- connect through it.

The center may later contain important POIs or objectives, but seed selection
must not assume that every player should always travel to one central point.

Avoid strongly rewarding:

- circular central arenas;
- one mandatory bridge;
- one mandatory mountain gap;
- three suspiciously equal valleys;
- symmetric terrain walls;
- obviously manufactured lane geometry.

---

# 13. What Seed Search Should Select For

Seed search should concentrate computational effort on properties that are
difficult to author afterward without visibly fighting Minecraft terrain.

Highest priorities:

## A. Macro composition / playable fit

- suitable dimensions;
- continuous playable geography;
- N/S homeland opportunities;
- substantial Wilderness;
- expedition depth;
- useful center/interior;
- highland and maritime environmental depth.

## B. Orientability and terrain flow

- comprehensible W/E environmental axis;
- viable N/S competitive axis;
- recognizable center;
- natural movement guidance;
- multiple traversal possibilities.

## C. Formation relationships

- useful interaction between terrain systems;
- natural corridors;
- barriers and crossings;
- connected geographic structure.

## D. Regional legibility

- substantial landscape identities;
- strong regional cores;
- useful transitions;
- genuine open terrain;
- genuine highland identity;
- forests with edges/interiors.

## E. Formation and landmark quality

- strong mountains;
- passes;
- valleys;
- water formations;
- caves/ravines;
- distinctive landmarks.

## F. Low macro correction burden

A candidate should already possess the large-scale geography required by the
Default grammar.

---

# 14. What Seed Search Should NOT Strongly Select For

Do not make otherwise excellent geography fail because of:

- exact livestock populations;
- exact livestock spawn positions;
- horses;
- exact crop availability;
- exact crop patch locations;
- exact ore/resource fairness;
- exact village count;
- exact village placement;
- final POI positions;
- final objective positions;
- final Routes;
- exact N/S resource symmetry;
- exact ecological populations at generation time.

Naturally useful examples of these features are bonuses and should be recorded
when available.

They are not worth dramatically shrinking the geographic search space.

---

# 15. Natural Substrate vs Later Authorship

The map should be understood as several layers.

## 15.1 Vanilla Natural Substrate

Seed selection should provide:

- macro terrain;
- mountains;
- valleys;
- ridges;
- coast/ocean;
- waterways;
- biome distribution;
- major forests;
- alpine/snow geography;
- cave systems;
- ravines;
- major natural landmarks.

These are expensive to fabricate convincingly.

## 15.2 Natural Ecology

Suitable selected habitats may later maintain renewable populations such as:

- livestock;
- horses;
- wild crops;
- plants;
- mushrooms/forage;
- other ecological resources;
- nighttime surface hostile mobs;
- cave hostile mobs.

The seed therefore needs suitable **habitat**, not perfect initial entity
placement.

## 15.3 Naturalistic Correction

After terrain selection, limited corrections may establish missing natural
vocabulary while remaining visually/ecologically consistent with Minecraft.

Potential examples:

- livestock ecological ranges;
- horse populations;
- wild crop patches;
- missing plant populations;
- modest resource guarantees;
- village crop portfolios;
- potentially seamless vanilla-style settlement placement;
- small vegetation/access corrections.

Ideally these corrections should look like plausible Minecraft worldgen or
ecology.

## 15.4 Authored MOBA Layer

Explicit competitive authorship later provides:

- North/South homelands;
- Aether Fountains;
- Routes;
- POIs;
- neutral objectives;
- objective structures;
- strategically necessary resource opportunities;
- other competitive infrastructure.

These features may and often should visibly communicate authored gameplay.

## 15.5 Player Transformation

During play, players may further transform the world through Minecraft systems:

- construction;
- roads;
- bridges;
- tunnels;
- stairs;
- clearing;
- lighting;
- farming;
- livestock breeding;
- extraction;
- defenses;
- logistics infrastructure.

The selected terrain should provide interesting problems for these systems to
solve.

---

# 16. Intervention Boundary

The guiding rule is:

> **Do not fight the natural terrain.**

Reject a candidate rather than planning major corrections such as:

- manufacturing its mountain system;
- replacing its coastline;
- creating its primary river network;
- replacing biome-scale forests;
- converting enormous regions between biome identities;
- carving the entire central valley;
- fabricating its alpine region.

Local intervention is acceptable when it preserves the selected geography.

The goal is not an untouched vanilla seed.

The goal is **excellent Minecraft geography that can accept modest naturalistic
correction and a deliberately authored competitive layer**.

---

# 17. Search Architecture

The search must prioritize throughput.

Do not run the official Minecraft server over a large blind seed set.

Use a staged funnel.

---

## Stage A — Very Cheap Coarse Search

Search a large seed/coordinate space using the cheapest reliable information
available.

Possible coarse signals include:

- biome/climate layout;
- land/ocean arrangement;
- broad coast presence;
- broad highland opportunity where cheaply available;
- cold/snow biome opportunity;
- open-vs-forested composition;
- approximate central/interior suitability;
- approximate N/S homeland suitability;
- catastrophic land fragmentation.

The purpose is rejection, not accurate final ranking.

False positives are acceptable.

Avoid expensive false negatives where possible.

Throughput is a primary metric.

---

## Stage B — Cheap Structural Ranking

Apply stronger analysis to Stage A survivors where practical without full
official-server generation.

Potential measures/proxies include:

- regional differentiation;
- broad W/E environmental progression;
- center/junction potential;
- highland depth;
- coast quality;
- land continuity;
- forest dominance;
- open-terrain continuity;
- approximate formation relationships;
- approximate natural corridors;
- approximate homeland connectivity.

Do not invent expensive metrics merely because they appear in this
specification.

If a property cannot be inferred cheaply and reliably, defer it.

---

## Stage C — Actual Vanilla Generation

Use the pinned official Minecraft version to generate real chunks only for a
small survivor set.

At this stage evaluate actual:

- surface terrain;
- relief;
- forest boundaries;
- waterways;
- coast;
- mountains;
- valleys;
- passes;
- structures;
- cave/ravine opportunities where extractable;
- formation relationships;
- homeland viability;
- central/interior quality;
- orientability;
- correction burden.

Actual vanilla terrain is ground truth.

Do not return to synthetic terrain generation as a substitute.

---

## Stage D — Competitive Greybox Fit

Perform a cheap analytical competitive fit only for the strongest fully
generated candidates.

Do **not** modify the Minecraft terrain yet.

Greybox:

- intended playable boundary;
- logical N/S/E/W;
- North homeland;
- South homeland;
- center/interior;
- approximately three plausible Route corridors per team;
- candidate major/minor POI nodes;
- important natural crossings/chokepoints;
- major landscape regions/formations.

The greybox answers:

> **Can this terrain readily accept a compelling competitive map topology
> without major geographic correction?**

Local asymmetry is allowed and desirable where it creates interesting
strategic differences.

---

# 18. Greybox Presentation

Every finalist should be easy to inspect both as Minecraft terrain and as a
potential competitive map.

Provide a reference/overlay that clearly identifies:

- playable bounds;
- North;
- South;
- East;
- West;
- North homeland;
- South homeland;
- center;
- provisional Route corridors;
- provisional POI locations;
- major natural regions/formations.

The playable boundary should make intended map dimensions immediately clear.

Prefer preserving a clean vanilla inspection world alongside analytical
greybox/reference material.

Do not clutter the actual world with unnecessary permanent debugging
construction.

---

# 19. Search Throughput and Computational Budget

The search should be designed to make **breadth cheap and depth selective**.

Before scaling an expensive stage, measure:

- candidates/second;
- rejection rate;
- survivor count;
- time per candidate;
- full vanilla-generation cost;
- expected total cost of the funnel.

If a supposedly cheap filter becomes a computational bottleneck, simplify,
replace, or move it later in the funnel.

The desired shape is conceptually:

**very many cheap candidates**
→ **far fewer structural survivors**
→ **small full-generation set**
→ **approximately 5–10 manual finalists**

Exact counts should follow measured throughput rather than arbitrary quotas.

Do not manually reason through candidates individually during bulk search.

---

# 20. Scoring Philosophy

Do not encode this specification as dozens of arbitrary hard thresholds.

Separate evaluation into:

### Hard viability filters

Only properties whose absence makes the candidate fundamentally unsuitable.

### Ranking metrics

Properties where more/less strongly changes candidate quality.

### Relationship metrics

Properties derived from interactions between natural features rather than
independent inventory.

### Manual-finalist criteria

Properties that cannot be measured cheaply or reliably enough to justify
automated rejection.

A candidate with unusual but compelling geography should be allowed to survive
if it satisfies the Default's strategic grammar.

---

# 21. Regression References

Preserve prior candidates as useful conceptual references.

## `920270002`

Useful properties:

- relatively strong macro directional composition;
- meaningful coast/highland relationship;
- useful scale/directionality.

Weak properties:

- forest/jungle dominance;
- weak landscape differentiation;
- nominally open areas often read as reduced forest rather than genuinely open
  country;
- highland identity is partially obscured by vegetation.

Use as a reference for:

> **useful macro structure with insufficient regional differentiation**

## `920270003`

Useful properties:

- strong regional differentiation;
- genuinely open interior;
- clear forest edges;
- strong alpine/mountain formations;
- natural valleys/passes;
- exposed cave opportunities;
- complex water geography;
- recognizable landmarks;
- useful relationships between mountains, open country, settlements, water,
  and forest.

Weak properties:

- poorer fit to the intended Default W/E macro arrangement;
- strong landscape vocabulary is not arranged ideally for the Default grammar.

Use as a reference for:

> **strong landscape and formation relationships with weaker macro directional
> fit**

The search target is approximately:

> **`002`'s useful macro directionality + `003`'s landscape and formation
> richness**

without requiring either candidate's literal geometry.

---

# 22. Finalist Selection

Produce a small and diverse finalist set suitable for manual Minecraft
inspection.

Prefer roughly **5–10 strong candidates** over dozens of minor variations.

Diversity matters.

Do not allow the shortlist to collapse into multiple nearly identical examples
of one topology merely because that topology scores slightly higher.

For each finalist record:

- seed;
- source coordinates;
- playable bounds;
- logical orientation;
- concise survival rationale;
- macro composition;
- orientability;
- major landscape regions;
- notable formations;
- notable formation relationships;
- central/junction quality;
- principal weakness;
- estimated correction burden;
- greyboxed homeland fit;
- greyboxed Route fit;
- greyboxed POI opportunities;
- path to actual generated inspection world.

---

# 23. Current Non-Goals

This search iteration should not implement final:

- Routes;
- villages;
- objectives;
- POIs;
- resource balancing;
- animal ecology;
- crop ecology;
- day/night ecology;
- hostile-mob ecology;
- terrain corrections;
- homeland construction;
- competitive infrastructure.

Analytical greyboxing is allowed.

These systems should influence whether the selected geography can support the
game, but they should not expand this seed-search task into implementation of
the entire map.

---

# 24. Required Implementation Review

Before modifying the search:

1. inspect the existing vanilla Default-region search implementation;
2. preserve useful extraction/search work;
3. identify which existing metrics remain useful;
4. identify which metrics should be demoted or removed;
5. identify which new requirements can actually be measured cheaply;
6. explicitly defer requirements that need full chunks or manual review.

Do not rebuild working infrastructure without reason.

---

# 25. Required Search Report

After implementing and running the search, report:

- search architecture used;
- cheap-prefilter method;
- candidates searched;
- throughput by stage;
- rejection/survival rates;
- full-generation count;
- full-generation cost;
- metrics retained;
- metrics demoted/removed;
- metrics added;
- properties remaining manual-only;
- finalist list;
- comparison against `920270002` and `920270003`;
- greybox-fit results;
- output paths;
- important limitations;
- recommended next step.

The report should make clear which conclusions come from:

- cheap proxies;
- actual generated vanilla terrain;
- analytical greyboxing;
- manual judgment.

---

# 26. Governing Principles

When implementation details are ambiguous, prefer the following principles in
order:

1. **Actual vanilla Minecraft terrain is ground truth.**
2. **Search for geography that is expensive to author convincingly.**
3. **Do not make seed search solve systems that can be authored or maintained
   later.**
4. **Formation relationships matter at least as much as feature inventory.**
5. **The Default should be geographically orientable.**
6. **Terrain should guide movement without reducing the Wilderness to rigid
   lanes.**
7. **Clearly delineated strategic regions are acceptable when they remain
   plausible Minecraft geography.**
8. **Competitive equivalence does not require literal symmetry.**
9. **Local naturalistic correction is acceptable; major geographic repair means
   the seed is wrong.**
10. **Optimize the search funnel for throughput. Spend expensive computation
    only on survivors.**
11. **Preserve Minecraft's capacity for emergent play rather than scripting every
    useful interaction into the terrain.**
12. **The final map should read simultaneously as a Minecraft world and as a
    deliberately selected competitive map.**