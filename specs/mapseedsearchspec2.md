# Default Map Fitting — Task A Specification

Status: Working implementation specification  
Scope: Analytical fitting only  
Depends on:
- `maps.md`
- `classes.md`
- `specs/mapseedsearchspec1.md`
- existing staged Default seed-search implementation and results

---

# 1. Purpose

Task A converts an already-accepted vanilla Minecraft terrain candidate into a proposed **Default competitive spatial skeleton**.

The seed-search pipeline has already answered:

> Is this Minecraft geography potentially suitable for Default?

Task A instead answers:

> Given this suitable Minecraft geography, how should an instance of Default fit onto it?

The fitting system must be:

- deterministic;
- analytical;
- terrain-preserving;
- reusable across different accepted seeds;
- explicit about uncertainty and failure;
- suitable for later physical greyboxing.

Task A does **not** modify Minecraft worlds.

Its output becomes the input to Task B, where selected fits may be physically greyboxed in one or more actual Minecraft worlds.

The eight current staged-search finalists are regression substrates for the fitting grammar.

The goal is not to make one candidate win.

The goal is to determine whether the same Default fitting grammar can produce plausible but geographically distinct competitive skeletons across multiple good Minecraft worlds.

---

# 2. Governing Principle

The Default is fitted **onto Minecraft geography**, rather than Minecraft geography being reshaped into a predetermined MOBA arena.

The fitting system should therefore prefer:

> formation → natural corridor → strategic relationship

over:

> predetermined geometry → terrain correction

Useful generated geography should be preserved.

When terrain does not support a desired relationship, the fitter should record that weakness rather than assuming large-scale terrain modification.

---

# 3. Inputs

Run Task A only on the eight existing staged-search finalists.

Reuse existing:

- official vanilla-generated terrain;
- extracted terrain data;
- candidate windows;
- logical orientation;
- terrain classifications;
- analytical utilities and pathfinding where useful.

Do not:

- generate new seeds;
- regenerate official worlds;
- search other windows within a seed;
- modify candidate terrain;
- manually redesign individual candidates.

The candidate window and logical orientation produced by seed search are inputs.

The fitter may refine the exact playable footprint **within** that existing candidate window if needed.

It may not relocate the candidate to another part of the seed.

---

# 4. Required Competitive Skeleton

For each candidate, Task A must propose:

1. exact playable bounds;
2. North homeland footprint;
3. South homeland footprint;
4. North Aether Fountain position;
5. South Aether Fountain position;
6. three distinct Route corridors departing the North homeland;
7. three distinct Route corridors departing the South homeland;
8. the deliberately supported Starter Route portion of each Route;
9. each Starter Route terminus / early-game Wilderness handoff;
10. central/interior reference geography;
11. Route intersections and cross-connections;
12. relevant natural crossings;
13. relevant natural chokepoints;
14. relevant natural shortcuts;
15. geography-derived Key Location candidates relevant to the traversal network;
16. major natural landmarks useful for orientation;
17. operational geographic depth from each homeland.

This is the first **map greybox fit**.

These are proposed spatial relationships and coordinates, not constructed gameplay content.

---

# 5. Playable Bounds

The existing candidate window is the search substrate, not necessarily the final exact playable boundary.

Task A may refine playable bounds within it when doing so produces a better Default fit.

Bounds should preserve the established Default macro grammar:

- North ↔ South = team opposition;
- West = major highland/mountain geography;
- East = coast/ocean geography;
- large continuous Wilderness between team homelands.

Bounds should avoid unnecessary exclusion of useful natural geography.

Do not resize or reposition bounds merely to manufacture symmetry.

Competitive equivalence matters more than geometric equivalence.

Task A does not determine the eventual physical implementation of the map boundary.

---

# 6. Homelands

Each team receives an actual homeland **footprint**, not merely a spawn point.

Homelands should be comparatively safe and usable development regions.

Using available extracted data, evaluate qualities including:

- usable/open ground;
- local relief;
- severe grades;
- water relationship;
- canopy and obstruction;
- development space;
- access into Wilderness;
- access to multiple Route departures;
- severe terrain barriers;
- general spatial usability.

The established homeland resource requirements in `maps.md` remain authoritative, but Task A should evaluate only properties supported by the current extracted data.

Do not invent missing resource/ecology information.

North and South homelands should be **competitively equivalent**, not geometrically mirrored.

A good pair should provide both teams with viable starting geography while allowing local asymmetry.

---

# 7. Aether Fountain Placement

Each homeland receives one proposed Aether Fountain anchor.

The Fountain is the primary authored homeland anchor.

It should be:

- inside the fitted homeland;
- on usable terrain;
- meaningfully integrated with the homeland footprint;
- sensibly related to the three Route departures;
- suitable as the spatial center of the team's initial developed territory.

Do not automatically use the geometric centroid.

Do not build the Fountain during Task A.

Task A outputs only its proposed position and placement diagnostics.

---

# 8. Route Ontology

A **Route** is a persistent designated traversal corridor / map-level connection.

A Route does **not** have to be physically built in order to be a Route.

Physical roads, paths, bridges, stairs and other infrastructure may make a Route legible or easier to traverse, but they do not define its ontology.

Therefore Task A finds **corridor relationships through geography**, not literal road centerlines.

Prefer Minecraft-native formations such as:

- valleys;
- passes;
- saddles;
- riverbanks;
- coastlines;
- forest gaps;
- plains corridors;
- mountain shelves;
- natural crossings;
- other continuous favorable terrain.

The terrain should suggest movement without preventing players from leaving the Route.

---

# 9. Three-Route Structure

Each homeland has three major Route departures.

The three Routes should not merely be three shortest paths toward approximately the same destination.

Collectively they should provide:

- three legible strategic departures;
- meaningful geographic divergence;
- differentiated opening choices;
- access toward different useful portions of the Wilderness;
- opportunities for later cross-connections;
- broader integration into the map traversal network.

Routes may converge later where geography naturally encourages it.

They should not collapse immediately into effectively one corridor.

Routes do not need to reach midpoint.

A Route may pass through largely natural terrain without visible construction beyond its initial Starter Route treatment.

---

# 10. Starter Routes

The homeland-facing beginning of each Route is a **Starter Route**.

Starter Routes are deliberately established initial infrastructure.

Their purpose is not merely aesthetic or navigational.

They exist to make the undeveloped world immediately playable during the opening game.

Starter Routes should provide:

- obvious strategic departures from the homeland;
- reliable early traversal;
- practical access to early Wilderness;
- differentiated opening choices;
- early convergence and interaction opportunities;
- practical return travel;
- introduction to the seed's geography.

Later physical implementation may use Minecraft-native:

- dirt roads;
- footpaths;
- bridges;
- stairs;
- vegetation clearing;
- retaining work;
- other modest traversal infrastructure.

Task A does not construct these features.

It identifies where the supported Starter Route should run.

---

# 11. Starter Route Length

The current working operational calibration is:

- 0–40 effective blocks: homeland fringe / immediate geography;
- 40–70 effective blocks: opening transition / Starter Route space.

Starter Routes should generally hand players into unsupported Wilderness at approximately:

> **60–70 effective travel blocks from the homeland edge**

This is a soft target.

It is not:

- a Euclidean radius;
- a mandatory exact length;
- a reason to ignore better geography.

A terminus at approximately 58 or 76 effective blocks may be preferable when terrain strongly supports it.

Use effective traversal/path distance and terrain cost.

Starter Routes should **not** extend so far that they pre-solve travel into central/deep Wilderness.

The terminus represents a deliberate transition:

> provided opening infrastructure → player-directed Wilderness operation

---

# 12. Starter Route Construction Feasibility

A strong Route corridor does not automatically imply a strong Starter Route.

Evaluate these separately.

For every Route emit at least:

1. **corridor quality**
2. **Starter Route legibility / construction feasibility**

The homeland-facing Starter Route should be capable of becoming unmistakable initial infrastructure without severe terraforming.

Penalize beginnings that immediately require:

- climbing a cliff;
- crossing substantial deep water;
- extreme grades;
- heavy terrain destruction;
- severe dense obstruction;
- implausibly expensive initial road construction.

Minor Minecraft-native adaptation is acceptable.

The goal is not zero construction.

The goal is a clearly authored Starter Route that still feels like infrastructure built through Minecraft terrain.

---

# 13. Operational Geographic Depth

Regional progression is measured by **effective traversal depth from the homeland**, not concentric Euclidean rings.

Current working calibration:

| Effective depth | Working geographic role |
| ---: | --- |
| 0–40 | homeland fringe / immediate |
| 40–70 | opening transition / Starter Route |
| ~70–110 | secondary-region core |
| ~110–150 | secondary depth / transition |
| ~150–210 | tertiary-region core |
| ~210–250 | deep transition |
| ~250–350+ | quaternary / deep-region core |

These are working analytical bands, not rigid biome bands.

Do not require one unique biome per depth.

A region's effective depth may differ substantially from its Euclidean distance because of:

- relief;
- rivers;
- forests;
- mountains;
- coastlines;
- passes;
- crossings;
- corridor quality;
- other traversal conditions.

---

# 14. Relationship to Player Progression

The geographic-depth calibration reflects the current universal capacity curve in `classes.md`.

Relevant current progression facts include:

- Level 1 begins at 9 Hunger;
- universal capacity increases over progression;
- Level 3 introduces the first capacity specialization;
- Health and Hunger universal floors reach vanilla capacity at Level 17;
- Level 16 currently contains a working universal mobility breakpoint.

The intended relationship is **not**:

> higher Hunger stat hard-unlocks a region.

Players may provision themselves and attempt unusually deep expeditions earlier.

Instead:

> progression changes when deeper geography becomes operationally practical and routine.

Deeper geography should increasingly reward:

- preparation;
- provisioning;
- terrain knowledge;
- Exploration;
- Development;
- Logistics;
- Routes;
- Supply Lines;
- player-built roads;
- bridges;
- tunnels;
- other infrastructure.

Therefore the fitter should evaluate whether meaningful geographic depth exists rather than treating distance as a hard player-level gate.

---

# 15. Regional Progression Evaluation

For each homeland, report whether its reachable geography provides meaningful:

### Secondary depth

Approximately 70–110 effective blocks.

This should be associated with the first meaningful expeditionary expansion beyond the immediate opening area.

### Tertiary depth

Approximately 150–210 effective blocks.

This should represent clearly deeper Wilderness operation rather than simply another feature adjacent to the homeland.

### Deep / quaternary depth

Approximately 250–350+ effective blocks.

At least some important geography should remain meaningfully expeditionary even once universal physical capacity approaches vanilla completeness.

The fitter should diagnose:

- excessive compression of interesting geography near home;
- absence of meaningful secondary depth;
- absence of meaningful tertiary depth;
- absence of meaningful deep geography;
- unusually empty or disconnected depth bands.

Do not maximize biome count.

The goal is **geographic progression**, not biome collection.

---

# 16. Center / Interior

Center is a **junction/reference geography**, not necessarily a destination.

Do not assume:

- a central boss;
- a central objective;
- a central structure;
- a single mandatory central crossing.

Determine the meaningful central/interior area from:

- playable bounds;
- N/S team opposition;
- terrain relationships;
- Route relationships;
- cross-network movement.

Report whether fitted Routes:

- converge near it;
- cross around it;
- bypass it;
- create multiple approaches;
- support lateral movement.

A strong center may be:

- a basin;
- a valley;
- a river crossing/confluence;
- a saddle/gap;
- an open interior;
- another naturally connective formation.

Do not place gameplay content there during Task A.

---

# 17. Key Locations

Task A may identify **geography-derived Key Location candidates**.

A Key Location is:

> a location where geography naturally concentrates, redirects, enables or rewards player movement or activity.

Possible examples, when supported by available extracted data:

- mountain pass;
- saddle;
- river crossing;
- valley junction;
- forest clearing/gap;
- cave mouth;
- bay;
- coastal landing;
- natural bridge site;
- settlement already represented in extracted data.

Key Locations are not automatically POIs.

Task A must not assign:

- objectives;
- rewards;
- resource packages;
- Worksites;
- Mining Outposts;
- production sites;
- authored villages;
- other gameplay content.

If current extraction data cannot reliably identify a Key Location category, report the limitation rather than inventing one.

---

# 18. Crossings, Chokepoints and Shortcuts

The fitted traversal network should identify geography that affects movement structure.

Relevant relationships include:

- river crossings;
- mountain passes;
- saddles;
- forest gaps;
- narrow coastal approaches;
- natural bridge locations;
- alternate crossings;
- shortcuts;
- bypasses.

Chokepoints are useful but should not generally create one mandatory solution.

Minecraft should permit multiple responses to terrain.

Examples:

river:
- ford;
- bridge;
- boat;
- detour.

mountain:
- pass;
- climb;
- tunnel;
- detour.

forest:
- trail;
- clearing;
- cut through;
- go around.

ravine:
- bridge;
- descend;
- alternate crossing.

The analytical fit should preserve room for later player transformation of the traversal network.

---

# 19. Landmark / Orientability Analysis

Task A should identify major natural landmarks relevant to player orientation.

Examples include:

- dominant mountain;
- isolated snowy peak;
- major bay;
- large lake;
- major river;
- distinctive forest boundary;
- unusual ridge;
- major valley;
- other sufficiently prominent formations represented by available data.

Orientability is distinct from regional identity.

The player should receive redundant directional cues.

The established Default macro grammar already provides:

- West → increasing highland/mountain character;
- East → coast/ocean;
- North/South → team opposition and homeland infrastructure;
- center/interior → convergent geography.

Landmark analysis should reinforce this rather than attempt to replace it.

---

# 20. Network Evaluation

For each candidate, analytically evaluate whether the fitted skeleton supports:

- meaningful Wilderness choices from both teams;
- three differentiated departures per homeland;
- useful Route divergence;
- useful cross-connections;
- lateral movement rather than only N↔S travel;
- meaningful central/interior connectivity;
- western highland expedition access;
- eastern coast/ocean access;
- natural shortcuts;
- deliberate off-Route travel;
- useful chokepoints without one mandatory crossing;
- Starter Route termination before central traversal is fully solved;
- meaningful secondary geographic depth;
- meaningful tertiary geographic depth;
- meaningful deep/quaternary geographic depth.

Do not force successful results.

Weaknesses are valid outputs.

The fitting system exists partly to reveal when otherwise attractive Minecraft geography does not support the complete Default grammar.

---

# 21. Determinism

The fitting system must be deterministic.

Given identical:

- candidate;
- extracted terrain data;
- fitting parameters;

it should produce identical results.

Use explicit:

- scoring;
- penalties;
- thresholds;
- graph/path relationships;
- diagnostics.

Avoid hidden qualitative judgments.

Do not special-case known finalist seeds to improve their results.

If a generic algorithmic issue is found, fix the general algorithm and rerun affected candidates.

---

# 22. Existing Implementation

Prefer extending the current worldgen/search analytical framework.

Reuse existing:

- extracted terrain representations;
- pathfinding;
- orientation handling;
- terrain metrics;
- candidate metadata;
- rendering infrastructure;

where appropriate.

Do not create a parallel world-analysis framework without a concrete technical reason.

Do not regenerate official Minecraft worlds merely to perform Task A.

---

# 23. Per-Candidate JSON Output

Emit one compact machine-readable fitting result for each finalist.

At minimum include:

- seed/candidate identity;
- source candidate bounds;
- logical orientation;
- proposed playable bounds;
- North homeland footprint;
- South homeland footprint;
- North Fountain coordinate;
- South Fountain coordinate;
- six Route corridors;
- six Starter Route portions;
- six Starter Route termini;
- effective Route/path distances;
- regional-depth classifications;
- corridor-quality scores;
- Starter-legibility scores;
- intersections;
- cross-connections;
- detected crossings;
- detected chokepoints;
- detected shortcuts;
- Key Location candidates;
- landmark candidates;
- center/interior reference;
- network metrics;
- uncertainty diagnostics;
- failure diagnostics.

Keep outputs compact enough for inexpensive later inspection.

---

# 24. Per-Candidate SVG Greybox

Emit one SVG greybox for each finalist.

The SVG should present the **proposed map fit**, not merely debugging metrics.

Overlay the fitting result on the available terrain representation.

Show:

- playable bounds;
- logical N/S/E/W;
- North homeland footprint;
- South homeland footprint;
- Aether Fountains;
- six Route corridors;
- visually distinguishable Starter Route portions;
- Starter Route termini;
- center/interior reference;
- Route intersections/cross-connections;
- detected Key Locations;
- major landmarks;
- regional-depth information where readable.

The result should make it possible to understand the proposed Default skeleton at a glance before Task B.

Avoid excessive diagnostic clutter.

---

# 25. Aggregate Output

Emit one compact aggregate comparison JSON covering all eight finalists.

Include comparable metrics for:

- homeland quality;
- North/South homeland equivalence;
- Route divergence;
- Route-network quality;
- Starter Route legibility;
- secondary-depth access;
- tertiary-depth access;
- deep/quaternary access;
- cross-network connectivity;
- center/interior connectivity;
- highland access;
- coast access;
- important failures;
- important uncertainties.

Do not declare a winning seed.

If implementation architecture requires an aggregate numeric score for sorting/debugging, label it clearly:

> analytical / non-authoritative

It must not be presented as a design recommendation.

---

# 26. Tests

Add focused automated tests for at least:

- deterministic fitting;
- homeland fitting;
- Fountain placement inside valid homeland space;
- North/South handling;
- Route divergence;
- effective traversal distance;
- regional-depth classification;
- Starter Route termination;
- Starter Route feasibility scoring;
- Route intersections/cross-connections;
- relevant network calculations.

Where possible, test general properties rather than expected outputs for specific finalist seeds.

---

# 27. Execution

Run the completed fitter on the eight existing finalists only.

Do not:

- generate additional candidates;
- regenerate the eleven official worlds;
- manually inspect every candidate and alter its fit;
- special-case individual seeds;
- physically modify any Minecraft world.

If a generic implementation bug appears, correct the fitter and rerun affected candidates.

---

# 28. Explicitly Deferred Systems

Task A does **not** fit, author or implement:

- final objectives;
- defensive objective structures;
- Mining Outposts;
- production Worksites;
- other final Worksites;
- final POIs;
- resource portfolios;
- ore guarantees;
- crop placement;
- crop ecological zones;
- animal populations;
- animal ecological zones;
- hostile-mob zones;
- authored villages;
- Supply Chains;
- detailed infrastructure placement;
- physical Route construction;
- final map boundaries/walls;
- terrain correction.

Existing natural features may be detected where relevant to traversal.

They should not be assigned unresolved gameplay roles.

---

# 29. Relationship to Task B

Task B begins only after human review of Task A outputs.

Task B will take one or more selected fitted candidates and physically materialize an initial greybox in the actual Minecraft world.

Expected Task B inputs from this specification include:

- exact playable bounds;
- homeland footprints;
- Aether Fountain coordinates;
- Starter Route paths;
- Starter Route termini;
- other selected spatial markers.

Task B should therefore not need to independently redesign or rediscover the basic spatial skeleton.

Task A must not proceed into Task B automatically.

---

# 30. Completion Criteria

Task A is complete when:

1. a deterministic Default fitting implementation exists;
2. focused tests pass;
3. all eight existing finalists have fitting JSON outputs;
4. all eight have SVG greyboxes;
5. aggregate comparison output exists;
6. limitations caused by missing extraction data are documented;
7. no Minecraft world has been modified.

At completion:

- report files changed;
- report tests/run commands;
- report generated output paths;
- briefly report unavailable metrics caused by source-data limitations;
- commit and push the coherent implementation;
- return the commit SHA.

Keep the completion report concise.

---

# 31. Non-Goals

Do not use Task A to answer:

> Which seed should ship?

Do not use Task A to produce a finished Default map.

Do not use Task A to solve unresolved gameplay systems.

Do not use Task A to optimize Minecraft terrain into conventional MOBA geometry.

Task A exists to answer:

> Can the established Default spatial grammar be consistently and transparently fitted onto this piece of good vanilla Minecraft geography, and what would that fit look like?