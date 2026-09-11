# Minecraft MOBA Design

Design manuscript • Source snapshot: 10 September 2026

Minecraft-first competitive play, persistent world transformation, and specialized contribution

Base repository snapshot: main at fe83bd039f76302665b45640ddc7263f94a8e073; revised with the 10 September Infrastructure System handoff and accompanying canonical reconciliation. This manuscript records design maturity separately from implementation maturity. It is a synthesis for formal review, not an implicit approval of unresolved rules.

## Reading this manuscript

The fourteen chapters proceed from the match’s purpose to its world, economy, infrastructure, objectives, classes, interactions, and implementation. Chapter 14 contains the source register and revision ledger. Source codes in square brackets refer to that register; section references refer to this manuscript unless prefixed by a source code.

**Established** means an explicitly maintained design principle or core, not necessarily implemented or balanced. **Working** means the current developed direction remains revisable. **Prototype-Test** means a trial parameter, experiment, or implementation with limited validation. **Historical-Superseded** preserves earlier reasoning without retaining it as a current rule. **Open** means the rule or decision is not settled.

Flags add specific review meaning: **[OPEN]** missing decision; **[VERIFY RECENCY]** ordering or applicability needs confirmation; **[CONFLICT]** incompatible statements remain in the sources; **[PROTOTYPE]** trial behavior or number; **[HISTORICAL]** obsolete or archival material; **[NEEDS CANONICAL OWNER]** developed material lacks dedicated authoritative ownership. A source note applies to the following subsection unless a more specific note intervenes. An illustrative consequence is explanatory, not an additional rule.

Root classes.md, objectives.md, and maps.md govern their domains. The user-authorized 10 September infrastructure reconciliation adds infrastructure.md as the canonical owner of shared infrastructure rules. Later, explicit revisions within the repository can supersede older passages; the competing passage and reason are recorded rather than silently removed. Chat fills gaps and preserves proposals, but does not silently override those roots. Generated wiki pages, exports, and historical handoffs retain their narrower provenance. The manuscript’s organization and cross-references are editorial; its flags are part of the design record.

# 1. Introduction

## 1.1 What Minecraft MOBA is

**Established. Sources: [R], [C §1], [M: Procedural philosophy], [O §19].** Minecraft MOBA is a competitive team game in which the Minecraft world itself is the strategic board. Terrain, blocks, resources, tools, food, creatures, construction, trade, and transportation remain functional systems. Players gain advantages by reading an unfamiliar world and transforming it into a more useful world for their team.

The competition adds class specialization, a Level 1–30 progression framework, concentrated objectives, opposing defensive structures, and a developing match economy. These systems organize Minecraft play into a contest without reserving ordinary Minecraft actions for particular classes. A player can contribute by finding a valuable place, making it accessible, extracting its resources, producing useful equipment, sustaining renewable production, distributing supplies, or winning a fight.

The useful world-state arc is generated → discovered → traversed → extracted → constructed → developed → connected → supplied → contested. This is an ecosystem of interacting activities, not a mandatory sequence. A team may discover a Worksite before it can exploit it, build an alternate approach during a siege, or develop an economy far from home.

## 1.2 The design thesis

**Established direction. Sources: [C §§1–2], [M], [O §§18–19].** Minecraft labor should remain consequential while its timescale fits a match. Gathering and crafting matter because material availability changes practical options. Roads and tunnels matter because they change access. Combat matters because it protects, interrupts, or captures opportunities. Persistent world changes make earlier decisions visible in later play.

Non-combat specialization must be capable of consequential team contribution. This does not require every activity to produce identical XP or equal spectacle. It requires a team’s preparation, development, and logistics to affect what its players can accomplish. Strong mechanical PvP should remain recognizable; generic progression should not overwhelm it with unbounded numerical advantages. [W: team-size and skill-profile discussion; Working]

## 1.3 Efficiency, potency, and ordinary permission

**Established. Source: [C §1].** Efficiency means accomplishing useful work with less time, input, risk, effort, resource expenditure, or logistical burden. Potency means doing more, doing a stronger or larger version, or achieving something an ordinary player could not accomplish through that system. Classes can express either or both.

Specialization does not grant exclusive permission to fight, travel, mine, build, craft, farm, or carry resources. Progression can instead unlock system recognition of exceptional infrastructure. Building a bridge and designating a Construct are different acts; moving items and recognizing a Supply Line are different layers. Chapter 6 develops this distinction.

## 1.4 Competitive uncertainty and persistent consequences

**Established direction. Sources: [M: Resource philosophy and fairness], [O §§8, 19].** Procedural uncertainty rewards discovery and adaptation. Fundamental resource vocabulary should exist, while its location, concentration, and accessibility remain variable. Fairness concerns competitive opportunity rather than identical terrain.

Objective solutions differ through costs and world consequences. Demolishing a passage, removing defenders, stealing an Allay, and extracting a gold hoard are strategically different accomplishments; there is no universal XP ranking that makes one method always superior. The resulting world state matters beyond a reward notification.

## 1.5 What is mature and what remains unfinished

The Minecraft-first thesis, seven archetypes, Default macro grammar, and objective-method principles are well established. Class kits, progression numbers, infrastructure integration, Worksite designs, and map-fitting criteria are developed working material. Final victory validation, exact XP yields, population ecology, infrastructure boundaries, and many gameplay implementation details remain open.

[VERIFY RECENCY] The root README’s statement that the repository contains no implementation is stale relative to the current implementation folders. Conversely, the presence of generated worlds and a datapack does not establish a playable full match. Chapter 13 separates those claims.

# 2. The Match

## 2.1 Match contract

**Working; [NEEDS CANONICAL OWNER]. Sources: [W: opening and team-size discussions], [C §3], [O §§3, 7, 21].** Opposing teams begin at their Aether Fountains in North and South homelands. Players begin below ordinary vanilla physical capacity and develop toward specialized late-game capabilities. The current class skeleton begins at 9 Health, 9 effective Hunger, and 6 inventory slots; Chapter 5 is the numerical reference.

| Contract element | Current record | Status / limitation |
|---|---|---|
| Team size | 7v7 is the latest discussed working assumption; 5v5 is a comparison case | [VERIFY RECENCY] Chat recommendation carried into subsequent discussion; no final root match contract |
| Duration | Approximately 25–35 minutes | Working target, not an enforced clock or validated result |
| Starting place | Literal Aether Fountain within a usable homeland | Working; dimensions, protection, and provisioning unresolved |
| Inventory on death | KeepInventory ON is the user’s preferred direction | Working; special cargo and MOBA XP require separate decisions |
| Progression | Level 1 through Level 30 | Established range; numerical schedule Working |
| Defensive structure set | Pillager Outpost, Nether Bastion, End Tower, Aether Fountain | Canonical objective set; sequence not fully settled |
| Victory | Disable enemy respawn infrastructure as the final strategic direction | [OPEN] Exact victory transition is not specified |

The manuscript does not turn Fountain disablement into immediate victory or invent a final elimination requirement. Fountain exposure, repair, reactivation, and the fate of living enemies remain objective-system decisions (§7.4).

## 2.2 Seven avenues of play

**Established framework. Source: [C §2].** The seven archetypes first describe useful kinds of work, before they classify individual classes.

| Avenue | Contribution to the match |
|---|---|
| Combat | Confront threats, protect activity, interrupt opponents, and convert control into opportunity |
| Exploration | Discover, interpret, and reach useful places; improve repeated traversal |
| Construction | Turn material into useful geometry and recognized places |
| Extraction | Reach, remove, and acquire useful world materials |
| Production | Transform acquired inputs into useful outputs |
| Development | Improve or mature productive world states |
| Logistics | Make resources available where and when needed |

A class may combine several domains. A team need not select one representative of each, and all players retain the ordinary verbs. Team-size analysis should therefore ask whether players have enough simultaneous consequential work, not whether seven labels fit seven slots.

## 2.3 Opening and division of labor

**Working. Source: [W: constrained dispersal and Level 6 discussion].** Players initially benefit from separating to claim accessible resources and recognizable XP opportunities. The first practical horizon is Level 3’s capacity choice. Nearby development, preparation, resource gathering, and exploration can proceed simultaneously.

Some players can remain close to home while others provision for expeditions. This division is intentional, not a failure to join the “real” game. The emerging team economy should connect local production and Development with outward exploration, extraction, contest, and supply.

The latest chat preference leans against generic proximity XP sharing unless being present matters to the accomplishment. [OPEN] This is not a complete attribution policy. Protection, cooperative extraction, carrier work, objective assists, and specialist enablement still need explicit credit rules (§5.2).

## 2.4 Match rhythm expressed as world state

**Working interpretive phase model. Sources: [W], [C §3].** Early, mid, and late game should describe what players and the world can do, rather than serve as rigid clock gates.

| Landmark | Player / team / world picture |
|---|---|
| Level 1 | Constrained players leave the Fountain, identify accessible work, and manage a small loadout |
| Level 6 | Earlier exploration, building, and preparation can receive first infrastructure recognition; prior work becomes a basis for greater throughput |
| Level 12 | Authored infrastructure role expression differentiates strategic jobs; developed locations and connections become more consequential |
| Levels 16–17 | Universal Inventory, then Health/Hunger, reach their vanilla floors; mobility may condense the effective match geography |
| Level 24 | Mature capacity choices, infrastructure, and class mechanics support larger undertakings and expensive contests |
| Level 30 | A highly realized class reaches terminal progression; its capstone grammar is still open |

These are not guaranteed synchronized team states. Players can progress at different rates, and an advanced player need not imply a fully developed team network.

## 2.5 Provisional timeline retained from the outline

**[PROTOTYPE] [VERIFY RECENCY]. Source: [ORG: manuscript outline].** The prior outline included the following pacing hypotheses. They are retained as a test agenda, not as established event timings or evidence that the progression curve produces them.

| Time hypothesis | Intended activity |
|---|---|
| 0:00–0:30 | Provisioning and local development |
| 0:30–1:00 | Departures |
| Around 1:00 | Serious expeditions become plausible |
| 1:30–2:30 | Plausible early skirmishes |
| 2:30–4:00 | Increasing overlap between opposing activity |
| 4–9 minutes | Claim and Interference |
| 9–15 minutes | Exploitation |
| 15–22 minutes | Contestation |
| 22–30 minutes | Extraction and Denial |
| 30+ minutes | Resolution |

[OPEN] No verified mapping relates these minute bands to level bands, objective availability, day/night duration, or either selected map candidate. “Extraction and Denial” is a phase label, not a rule postponing the Extraction archetype until minute 22.

## 2.6 Pressure, death, and re-engagement

**Working. Sources: [W], [M: Food as geography], [O §18].** Pressure comes from external opportunities, enemy interference, depletion, geography, provisioning, objectives, and the expanding overlap of team economies. Night may create recurring vulnerability windows (§3.5). Teams can choose preparation, concession, defense, or an alternative objective; the design does not require everyone to converge on one timer.

With KeepInventory, death principally loses time, position, pressure, and access. It need not erase an expedition’s ordinary possessions. [OPEN] Respawn delay, restoration of Health/Hunger, protection at the Fountain, special-cargo behavior, and intentional death as a return-transport shortcut require explicit rules. Logistics cannot be balanced on an assumed inventory-drop punishment that the match does not use.

Ordinary building and destruction remain broadly available. [OPEN] Boundaries, objective protection, enemy container access, and interactions with exceptional terrain-changing abilities must still be specified. Universal permission is not a complete implementation contract for every protected state.

# 3. The Minecraft World

## 3.1 World vocabulary and resource function

**Established direction. Sources: [M], [C §8], [H].** Blocks, ores, tools, equipment, food, crops, plants, animals, hostile mobs, villagers, villages, structures, caves, water, biomes, transport, enchantments, and potions form the project’s vocabulary. They should retain recognizable uses while classes promote overlooked interactions into strategically important ones.

Construction materials become geometry. Ores and geology create extraction tasks. Food and renewable ecologies sustain expeditions and development. Infrastructure materials connect places and processes. Exceptional resources support costly tools, equipment, and Worksites. These functional families overlap: iron can be equipment, golem material, or part of a larger strategic investment.

The resource contract is reciprocal. Each class should care about several world resources, and meaningful world resources should ideally matter to at least one class or system. A resource guarantee prevents nonfunctionality; it does not promise abundance or convenient access.

## 3.2 Existence, discovery, and distribution

**Established principle; Working distribution. Source: [M: Resource philosophy].** Discovery uncertainty is valuable; uncertainty about whether a fundamental system-enabling resource exists can be harmful. Baseline homeland access, Wilderness resources, region-linked resources, allocated opportunities, contested opportunities, objective resources, and player-developed resources contribute different forms of availability.

A mountain-linked material can be guaranteed somewhere in the relevant geography without appearing near both Fountains. A shared concentration can be fair because both teams can reasonably find and contest it. The generator must assess the whole opportunity portfolio (§4.9), including renewable access and usable routes.

## 3.3 Living resources and population systems

**Working / Prototype-Test. Sources: [M: Ecology], [W: unfinished population systems], [N].** The Default prototype calls for at least four broad mixed livestock ranges collectively covering cows, sheep, pigs, and chickens, with separate horse presence. Multiple starter occurrences of wheat, carrots, and potatoes belong to baseline vocabulary. These should read as ecology or settlement-related resources, not arbitrary loot stamps.

Herds, Crop Patches, and Mob Swarms also appear in the later world-economy discussion as concentrated, phase-appropriate opportunities. They are intended to enlarge the scale of legitimate Minecraft activity as the economy develops. They are not established as generic XP pickups or automatically as formal Worksites.

[OPEN] [NEEDS CANONICAL OWNER] Their activators, spawning, populations, renewal, depletion, ownership, reward attribution, and temporal behavior are not settled. The night branch explicitly corrected an earlier claim that its full original specifications were available. Do not infer a complete system from that earlier “yes.”

Later extensions under discussion include horse, squid, or mountain-goat groupings and additional crop families such as melons. These are questions about the population framework, not committed species guarantees. Distinguish naturally occurring patches, ordinary player farms, and recognized Development infrastructure (§6.3).

## 3.4 Persistent, contestable, and temporal value

**Editorial organization of supported systems. Sources: [M], [O §§11–14A], [N].** Physical terrain, built access, and generated ore can persist after the event that made them useful. A village, road, or deposit can remain contestable without changing owner through a capture meter. Timed Silo output and potential nighttime opportunities introduce temporal value.

These properties are not exclusive resource classes. A Mining Outpost creates persistent ore whose accessibility is contestable and whose value depends on timing. An Industrial Enchanter provides a finite opportunity determined by successful timed logistics. The manuscript does not assign all resources one universal persistence or reset rule.

## 3.5 Day and night

**Working; [NEEDS CANONICAL OWNER]. Source: [N: final night-economy direction].** Day favors the full effectiveness of established infrastructure. Night reduces the effectiveness of Constructs, Development infrastructure, Routes, and Supply Lines without disabling them. Hostile-mob activity should become more economically valuable, giving Combat a recurring opportunity to convert danger into resources and progression.

This combination makes established positions harder to sustain, supplies less efficient, and reinforcement less comfortable. Raids, ambushes, interception, and siege become more attractive because the world’s relative incentives change. A team can still work, defend, travel, or prepare through night.

Infrastructure progression may mitigate each system’s nighttime weakness in its own way. These resilience paths are candidates; they are not automatically T2 rewards. The discussion’s illustrative 75–90% effectiveness range is not a selected multiplier and is not applied elsewhere in this manuscript.

[OPEN] Cycle duration, start time, number of nights, transitions, sleep behavior, mob composition, spawn rules, reward changes, and which intrinsic or integration effects weaken are undefined. No global nighttime PvP-XP bonus is established. Mob Swarms’ exact relationship to this economy remains open.

## 3.6 Compression of Minecraft timescales

**Working discussion. Source: [W: durability exchanges].** A match must make production, growth, travel, and equipment lifecycle relevant within its duration. Compression should preserve meaningful decisions rather than mechanically accelerate every vanilla process by one factor.

The latest durability exploration favors recurring equipment turnover, possibly with a deliberately uneven material-tier curve: wood temporary, stone disposable, iron dependable, gold an unusually fragile special case, and diamond a durable strategic investment. [VERIFY RECENCY] This direction was discussed after the canonical progression edits and was explicitly not yet canonized. Chapter 5 retains the economy implications; no durability constants are approved.

# 4. Map and Geography

## 4.1 Archetype, seed, and competitive fit

**Established direction. Source: [M].** A map archetype defines the geometry and strategic problem; a seed supplies its particular terrain, resources, structures, and opportunities. Players should learn how Minecraft and the archetype work without memorizing fixed resource coordinates.

Default should begin with actual Minecraft-like Overworld geography, preferably generated by Minecraft and then inspected, selected, constrained, and modestly adjusted. A region requiring extensive artificial correction should be rejected. Later implementation uses official vanilla generation rather than treating the earlier custom terrain generator as the final architectural answer (§13.2).

## 4.2 Default / Valley macro geography

**Working, grounded in established direction. Source: [M: Default].** Team opposition runs North–South. Environmental differentiation runs West–East: principal western highlands and an eastern coast/ocean transition. The orthogonal axes avoid automatically assigning one team mountains and the other the coast.

### 4.2.1 Homelands

Each homeland provides comparatively safe development space with useful access to common terrain, wood, food, baseline ore, caves, soil, water, farming, construction, the Fountain, and appropriate defensive objectives. Equivalence concerns practical starting opportunity, not block-for-block copying. Exact Fountain architecture and its travel time to resources remain open.

### 4.2.2 Continuous Wilderness

Outside the homelands is one shared continuous Wilderness. It is not divided into rigid team-side and center bands. Villages, caves, forests, deposits, difficult terrain, water, animals, plants, minor discoveries, objectives, and player infrastructure should occupy distinct places with connective empty terrain between them.

### 4.2.3 Western highlands and cold ecology

Foothills should develop into increasing elevation, relief, exposed stone, ridges, and deep highland geography. Valleys, saddles, basins, ravines, caves, deposits, and multiple approaches give the region an interior. Difficulty should deepen rather than present one decorative wall.

Variable high-altitude snow/cold ecology is a Working map guarantee. It supports world and class vocabulary, including Golem Master, while reading as real geography. Exact area and location vary by seed.

### 4.2.4 Eastern coast and ocean

The coast is intended to participate in transport, resources, settlement relationships, traversal, and class interaction. It should not be only a decorative boundary. [OPEN] Final aquatic opportunities and access expectations remain less developed than the western geography.

## 4.3 Routes and Starter Routes

**Working; [CONFLICT] [VERIFY RECENCY]. Sources: [M: Authored Routes], [S2 §§8–12], [S3 §§18, 23].** Root maps.md describes authored Routes as physical infrastructure and says visible infrastructure leads toward villages and major POIs. The later Task A specification defines a Route as a designated traversal corridor that need not be physically built, and distinguishes a deliberately supported Starter Route from natural Wilderness continuation.

For the current physical prototype, the later specification governs implementation: three legible departures per homeland receive limited Starter infrastructure; full Wilderness corridors are not paved. The root wording still needs reconciliation before this is treated as a global design replacement. The persistent shared principle is three meaningful opening choices through Minecraft geography, with freedom to leave them.

Starter Routes teach the seed’s geography, support reliable early travel and return, and hand the player into unsupported Wilderness. A Working soft handoff target is about 60–70 effective travel blocks from the homeland edge; nearby alternatives such as 58 or 76 may be preferable when geography supports them. These are neither exact Euclidean lengths nor permission to force terrain into a band.

Corridor quality and construction feasibility are separate. A promising valley connection may still have an unacceptable opening cliff or deep-water crossing. Minor stairs, clearing, path treatment, and bridges can reveal natural traversal logic; heavy terraforming should not manufacture it.

### 4.3.1 Route efficiency prototype

**[PROTOTYPE]. Source: [M: Authored Route test mechanic].** Clean, direct physical geometry is useful by itself. The separate test proposes approximately 10% lower locomotion-generated exhaustion, with no speed bonus. Sprinting’s conceptual 0.10 exhaustion per block becomes 0.09; ordinary jumping’s 0.05 becomes 0.045; the sprint-jump component’s 0.20 becomes 0.18.

This discount does not apply to mining, combat, regeneration, or other non-locomotion costs. The test deliberately gives Routes both geometric and modest artificial efficiency. It is not final player-Route progression and has not been established as implemented in the current physical greyboxes (§13.4).

## 4.4 Villages and points of interest

**Working. Source: [M: Villages and POIs].** Default should contain at least roughly three Wilderness villages; the older 3–5 range is exploratory, not a hard maximum. Multiple settlements make discovery and trade a network problem.

A village is a compound resource portfolio: inhabitants, trades, crops, animals, buildings, materials, and location can make different settlements valuable for different reasons. Their value should not be reduced to a uniform chest. Village development and connections support Merchant’s noncanonical draft (§9.7), but that draft does not establish the global trading rules.

Major and minor POIs can derive value from natural structures, unusual geology, caves, resources, transport, or objective relationships. They need not all be boss arenas. Underused Overworld features—including wells, fossils, igloos, huts, ruined portals, ocean or trail ruins, geodes, mineshafts, and monster rooms—are Working candidates for renewed relevance.

## 4.5 Difficult geography and player transformation

**Established principles. Source: [M: Difficult Wilderness].** A difficult region should offer intentional commitment, valuable opportunity, depth, legibility, contestability, transformability, and logistical consequences. Multiple approaches and recognizable internal geography make it a place to learn and occupy.

Players can tunnel, bridge, clear, build stairs, establish storage, develop food, and create safer repeated access. Roads, harbors, cave entrances, defensive terrain, and relocated animals can change the effective strategic map. These improvements should emerge from Minecraft actions, not an automatic flattening of all inconvenience.

## 4.6 Information and discovery

**Working. Sources: [M], [O §10], [S2].** The macro grammar is learnable; the seed’s precise opportunity geography is not. Discovery has value through practical information and potentially Team First / Match First rewards. Natural landmarks should support naming, communication, and navigation.

[OPEN] The player-facing map, fog-of-war equivalent, visibility tests, shared information, discovery confirmation, and enemy intelligence are not fully specified. Generator opportunity scores are not player information. Analytical markers in a greybox are not final scouting UI.

## 4.7 Scale and operational depth

**Prototype-Test with later Working analytical refinement. Sources: [M: Scale], [S2 §§11–15].** The root’s first scale hypothesis is approximately +25% macro linear Wilderness/resource separation, distributed nonuniformly. Preserve feature scale—homelands, villages, individual structures, patches, trees, and Route width—while adding connective distance, forest interior, mountain depth, and divergence.

A uniform 25% increase in both horizontal dimensions would yield about 56% more surface area, but that arithmetic is not the prescribed transformation. Historical 672 × 832 dimensions and a naive 840 × 1040 equivalent are reference values, not final accepted bounds.

| Later effective-depth band | Working role in Task A |
|---|---|
| 0–40 | Homeland fringe / immediate geography |
| 40–70 | Opening transition / Starter Route |
| About 70–110 | Secondary-region core |
| About 110–150 | Secondary depth / transition |
| About 150–210 | Tertiary-region core |
| About 210–250 | Deep transition |
| About 250–350+ | Quaternary / deep-region core |

[VERIFY RECENCY] Root maps.md retains earlier travel bands (0–40, 40–80, 80–150, 150–250, 250–375+). The later table is the fitting experiment’s calibration, not a silently finalized replacement. Effective depth includes terrain cost and usable paths; neither table mandates concentric rings or a biome per band.

## 4.8 Food as geography

**Established implication; Prototype-Test arithmetic. Source: [M: Traversal].** A village, herd, farm, or crop patch can be a resupply point and expedition staging area. A physically farther opportunity with reliable food may be operationally closer than a nearby one across barren terrain.

Evaluate outbound travel, searching, work, fighting, hauling, return, and reserve. Losing sprint should often impose slower travel and vulnerability rather than prohibit continued travel. The old map experiment’s L1 10-food/zero-saturation and L3 12-food/zero-saturation states remain test fixtures. The newer class progression starts at 9, so the “L1” fixture label no longer describes the current universal starting state (§5.4).

## 4.9 Competitive fairness and validation

**Working. Source: [M: Fairness and validation].** Baseline opportunity should be equivalent. Allocated opportunity can be asymmetric while balanced in overall availability. Shared contested opportunity can be fair through practical access, discoverability, and counterplay.

Validation should measure existence, abundance, concentration, renewability, travel cost, extraction difficulty, Route quality, neighboring resources, defensibility, alternate access, and correction burden. Straight-line distance alone is insufficient. A nominally close deposit behind difficult terrain may be less accessible than a farther exposed one.

The pipeline should identify actual resource instances before deriving resource regions. Do not fabricate strategic geology by drawing resource polygons unsupported by physical blocks. Fairness is a portfolio judgment and cannot be established by a single scalar score.

## 4.10 Seed search, fitting, and physical validation

**Working implementation sequence. Sources: [S1–S3], [B].** Search finds promising vanilla geography. Task A fits bounds, homelands, Fountains, three departures per team, Starter termini, connections, and depth. Comparative review selects candidates. Task B materializes a minimal skeleton and tests it at player scale. Only after bounded correction and a skeleton freeze does Task C place gameplay systems.

Central connectivity can be a junction area or network, not a single center point. Routes may converge later but should not collapse into one opening. Lateral links and shortcuts must exist in physical space, not merely intersect in a diagram.

Current candidates 930010639 and 930012642 are physical experiments, not final Default. The primary has a known block-scale Starter discrepancy; the comparison’s six Starter spines pass conservative checks, but neither has a completed competitive walk-test pass. Details and the current stop state are in §13.3.

## 4.11 Alternate archetypes and historical experiments

**Working concepts. Source: [M: Working map archetypes].** Chasm emphasizes ravines, bridging, cliff routes, tunnels, and vertical transport. Archipelago emphasizes boats, bridges, underwater connections, docks, and later transport. Underdark emphasizes cave reading, lighting, sound, and player-created tunnels. Canopy supports canopy, ground, and underground networks. Wastes emphasizes scarcity, renewable resources, settlements, and concentrated deposits.

These archetypes foreground Minecraft techniques without arbitrary class modifiers. [OPEN] They do not yet have Default’s detailed generation and validation contracts.

[HISTORICAL] P0–P2D, the 50-variant experiment, and seed 920260900 document prior learning. Their dimensions, terrain generation, and incomplete resource audits are not current design authority. The chronology is retained in §14.2.

# 5. Economy and Progression

## 5.1 Economic structure

**Established direction; Working progression. Sources: [C §§3, 9], [O §§9, 18], [W], [X].** Resource acquisition is itself progression, and materials also become practical capability through production, equipment, infrastructure, and objective investment. XP is a parallel advancement layer; it should recognize legitimate Minecraft accomplishments without having to judge every resource’s eventual strategic use.

The economy should accelerate as players become more efficient, gain Yield and capacity, establish infrastructure, and exploit richer opportunities. Rising XP requirements should absorb only part of increased throughput. Progression should change what can be accomplished rather than force an advanced player to repeat opening work indefinitely at an unchanged rate.

Staying home and projecting outward compete for time, labor, and materials. Integrated infrastructure rewards intentional concentration; the world’s opportunities reward dispersion. Neither becomes the universal location of correct play.

## 5.2 XP sources and contribution accounting

**Established resource-attainment principle; other categories Working/Open. Source: [O §§9–10, 18].** Legitimate resource attainment grants XP directly. Candidate additional sources include discoveries, major encounters, objective contributions, eligible productive activity, and other system-recognizable accomplishments. Infrastructure integration modifies legitimate earned XP; mere presence, submission, or existence should not be presumed to generate it.

Team First is the first allied discovery of an eligible opportunity. Match First / Pioneer is the first discovery in the match. [OPEN] Eligible targets, rewards, duplication, visibility, and attribution are undefined. These rewards make early raw movement speed especially sensitive.

[OPEN] The manuscript does not assign XP per block, craft, kill, delivery, farm action, or objective contribution. Source provenance, repeated pickup, transfer between teammates, re-crafting, renewable output, and joint work need a rule that avoids duplicate credit while recognizing useful activity. No complete anti-farming ledger is currently canonical.

[CONFLICT] Older chat preserves flat submission XP plus usage-driven infrastructure XP. Current [O §9] and [C §2] instead reject passive/submission assumptions and specify integration around legitimate activity. The old model is retained as historical discussion, not used in the economy described here.

## 5.3 Current working level spine

**Working. Source: [C §3: Current universal progression skeleton].** The table below preserves the current skeleton. Unspecified numbers remain unspecified; a breakpoint is not proof that its reward has an implemented ability.

| Level | Current progression event |
|---|---|
| 1 | Ability 1; 9 Health, 9 effective Hunger, 6 inventory slots |
| 2 | Ability 2 and universal growth |
| 3 | Capacity specialization I |
| 4 | Efficiency I and universal growth |
| 5 | Ability 1 upgrade |
| 6 | Class-authored infrastructure recognition entry |
| 7 | Yield I and universal growth |
| 8 | Passive scaling and universal growth |
| 9 | Task specialization and universal growth |
| 10 | Ability 2 upgrade |
| 12 | Infrastructure role-expression upgrade |
| 14 | Task advancement and universal growth |
| 15 | Ultimate unlock in the general skeleton |
| 16 | Passive scaling; Inventory floor reaches 36; approximately +10% mobility proposal |
| 17 | Universal Health and effective Hunger floors reach 20 |
| 18 | Capacity specialization II |
| 19 | Task advancement |
| 20 | Advanced class-authored reward I |
| 24 | Capacity specialization III and passive scaling |
| 25 | Advanced class-authored reward II |
| 30 | Capstone; form unresolved |

Levels 11 and 13 also provide universal growth. The complete implemented prototype growth schedule through 17 is 2, 4, 7, 8, 9, 11, 13, 14, 15, 16, 17. [D: progression v7] It corroborates the newer skeleton’s milestones, while not implementing the non-capacity rewards.

[CONFLICT] [C §9] still says growth is withheld whenever a choice occurs and places vanilla completion around Level 19. The newer skeleton explicitly permits task choices to coexist with growth and identifies Levels 16/17. This manuscript presents that newer working model and preserves the conflict in §14.3.

## 5.4 Universal capacity and specialization

**Working numerical model. Sources: [C §3], [D].** Each universal growth unit adds 1 Health, 1 effective Hunger, and 3 inventory slots, with universal floors capped independently at 20, 20, and 36. Health/Hunger specialization is additive above those floors. Inventory has an effective hard maximum of 36.

| Capacity | Level 1 | Automatic unit | Specialization at 3 / 18 / 24 | Universal floor |
|---|---:|---:|---:|---:|
| Health | 9 | +1 | +2 | 20 |
| Effective Hunger | 9 | +1 | +2 | 20 |
| Inventory slots | 6 | +3 | +6, subject to cap | 36 |

At Level 2, the player has 10 Health, 10 effective Hunger, and 9 slots before specialization. Choosing Health at Level 3 raises 10 to 12 (+20%); Hunger raises the simplified pre-sprint-cutoff reserve from 4 to 6 (+50%); Inventory raises 9 slots to 15 (about +67%). These are deliberately different practical effects, not equivalent percentages.

An early Inventory choice reaches 36 at Level 14 rather than 16. The newer skeleton replaces an Inventory choice selected at the cap with +0.5 Health and +0.5 effective Hunger. [CONFLICT] The older capacity section instead says the replacement is unresolved and Inventory should cease to be offered. The current datapack does not document this replacement as implemented. Partial overflow when a choice is made below the cap is not fully specified beyond capping at 36.

Inventory means meaningful loadout categories: tools, food, blocks, utility, supplies, and return haul. The prototype excludes armor and offhand; final treatment of those slots requires confirmation. An early capacity constraint should create choices, not constant item cleanup.

## 5.5 Effective Hunger and operating range

**Working formula. Source: [C §3: Effective Hunger above 20].** Up to 20 effective Hunger, the stat is represented by the intended displayed maximum. Above 20, the visible food bar stays at 20 and additional pre-cutoff reserve is represented by exhaustion efficiency:

**Exhaustion multiplier = 14 / (H − 6), for effective Hunger H > 20.**

The numerator is the reserve from 20 to the 6-food sprint cutoff. The conversion preserves the simplified amount of Hunger-consuming activity before that cutoff. It does not prove exact travel distance in a live match.

| Effective Hunger | Exhaustion multiplier | Reduction |
|---|---:|---:|
| 20 | 1.0000× | 0% |
| 21 | 0.9333× | About 6.67% |
| 22 | 0.8750× | 12.5% |
| 24 | 0.7778× | About 22.22% |
| 26 | 0.7000× | 30% |

A player choosing Hunger at Level 3 reaches 20 effective Hunger at 15, 21 at 16, and 22 at 17 under the working schedule. Further Hunger choices at 18 and 24 produce 24 and 26.

[OPEN] Regeneration-related exhaustion is explicitly unresolved. Stacking with Route discounts, saturation behavior, fractional effective Hunger, eating, and enforcement need implementation-level validation. The current datapack stores surplus Hunger but does not implement this exhaustion conversion (§13.5). The older suggestion of extra visible Hunger rows is not the chosen representation in the newer design skeleton.

For the older 10-food test fixture, the simplified reserve above cutoff is 4 food points, approximately 16 exhaustion; at 12 food it is 6 points, approximately 24 exhaustion. That arithmetic is useful for the map experiment but does not redefine the current 9-Hunger start. Real expeditions also spend time and resources on work, fighting, detours, and return (§4.8).

## 5.6 Task progression and mobility

**Working. Source: [C §3: Task progression].** Efficiency uses the Efficiency enchantment family to increase block-breaking/worldwork rate. Yield combines Fortune and Looting to improve successful acquisition output. Damage combines Sharpness and Power for conventional melee and ranged output. Generic task tiers currently stop at III, leaving room for class-specific amplification.

Efficiency I arrives universally at Level 4; Yield I at Level 7. Task choices occur at 9, 14, and 19. Damage has no initial universal grant in this schedule. [OPEN] Eligibility, application to equipment, replacement, and interaction with ordinary enchanting remain technical and balance decisions.

The approximately +10% universal movement-speed reward at Level 16 is a Working phase-transition proposal, not the superseded early Route-speed bonus. Its purpose is to condense the mid/late match. Test its effects on discovery races, reinforcement, interception, and expedition reach before fixing the value.

## 5.7 XP requirement bands

**Working targets, not final XP amounts. Source: [C §3: Working XP requirement bands]; supporting discussion [X].** Requirements are grouped around changes in productive capability rather than one uninterrupted smooth curve.

| Band | Levels | Relative requirement index | Economic assumption |
|---|---|---:|---|
| I — Bootstrap | 1–6 | About 1.000× | Constrained player and opening Minecraft economy |
| II — Established | 7–12 | About 1.350× | First infrastructure economy, Yield I, task specialization |
| III — Developed | 13–19 | About 1.875× | Mature T1 / emerging T2, task advancement, vanilla capacity transition |
| IV — Advanced | 20–24 | About 2.575× | More mature T2 and advanced class rewards |
| V — Endgame | 25–30 | About 3.250× | Mature late economy and terminal class expression |

The candidate transitions are 6→7, 12→13, 19→20, and 24→25. Requirements remain fixed regardless of whether a team actually built infrastructure. Strong preparation improves the ability to satisfy them; it does not change the requirement dynamically.

The Bootstrap baseline should be calibrated from actual legitimate early activity. Later targets account for task speed, Yield, capacity, class mechanics, world opportunities, and infrastructure together. Integration’s roughly 10–15% targets cannot by themselves justify the entire increase. T1/T2 labels do not automatically map to Levels 6/12 (§6.6).

[PROTOTYPE] The datapack’s flat 100 XP per level is a test constant and not an implementation of these bands. The relation between earned MOBA XP, native XP, and enchanting expenditures remains a separate design decision.

## 5.8 Equipment, crafting, and durability

**Working canonical equipment direction. Source: [C: Kitfighter].** Iron should be the highest combat-equipment tier an ordinary player routinely and sustainably reaches. This should emerge from resource economics, not a ban on stronger equipment. Diamond remains legal and powerful but exceptional; permanent Diamond represents substantial investment competing with tools, Worksite cores, and infrastructure.

Mining Worksites can produce reliable exceptional concentrations. Natural Diamond may remain possible but scarce. If ordinary matches routinely equip everyone in full Diamond, investigate material availability and conversion before imposing an arbitrary restriction.

**Working chat proposal; [NEEDS CANONICAL OWNER]. Source: [W: durability].** Compressed equipment lifetimes could create repeated demand for Production under KeepInventory. Resources become equipment, equipment enables activity, and wear creates replacement or repair demand. Different equipment categories may need different curves, especially armor versus tools.

[OPEN] Removing durability, increasing effective durability, and shortening lifetimes appeared in the discussion. The latest exploration favors shorter, tier-sensitive lifecycles; it does not establish a global multiplier or the illustrative item durability values. The suggested 2–4 heavily used equipment lifecycles per match is an assistant-proposed test target, not a committed requirement. Early wood/stone fragility must be tested against the six-slot starting inventory so provisioning does not become repetitive maintenance.

## 5.9 Trade, enchanting, potions, and advanced rewards

**Working / Open. Sources: [C], [O §14A], [L], [J].** Villagers and trade remain meaningful world systems; Merchant’s draft exploits villager advancement. Enchantments and potions are shared design vocabulary, while the Industrial Enchanter reserves a finite exceptional Production opportunity.

The progression discussion proposes resolving universal effects first, class mechanics next, enchanting-table sets next, and advanced rewards afterward. This is a design-order preference, not a finished enchantment catalog. The already-used generic families are Efficiency, Fortune/Looting, and Sharpness/Power. Intentional overlap can be allowed, but its stacking cannot be assumed.

[OPEN] Ordinary enchanting availability, vanilla XP costs, bookshelves, random selection, Mending, Unbreaking, eligible goods, brewing access, and effect distribution require decisions. Levels 20 and 25 contain class-authored advanced rewards, not automatic infrastructure entitlement. Level 30’s capstone remains unspecified.

## 5.10 Restricted material categories

**Established categories; Working premium. Sources: [C §7], [WI].** Primary Materials are a deliberately restricted set of materials whose expenditure directly represents significant portable player capability. The current members are Iron, Gold, Diamond, Netherite, and Leather. Wood, Stone, Copper, Redstone, Lapis, Coal, Emerald, Quartz, Flint, and String are explicitly outside the category. Exclusion does not imply low economic importance; a material can be central to another system, class, or activity without becoming a Primary Material.

The distinction is directness of conversion. Iron becomes tools, weapons, armour, shields, and buckets with little intermediation, so spending it intensifies an already meaningful scarcity and opportunity cost. Redstone is excluded because its major value emerges through further components, arrangement, and functioning systems rather than direct portable capability. Class relevance alone does not grant Primary Material status, and the category should remain restrictive rather than expanding toward an exhaustive material taxonomy. This supplies the definition Kitfighter's Salvage depends on (§9.6).

Construction Blocks are a separate restricted set whose acquisition and preparation chains are distinct enough that Construction can meaningfully specialize in bringing them into structural use. The current members are Bricks, Mud Bricks, Terracotta, Concrete, and Glass. They are not an exhaustive list of blocks suitable for building, not the best building blocks, not required for valid construction, and not required for Construct recognition. Ordinary blocks remain fully useful and contribute normally to valid Constructs.

Current exclusions include planks, logs, cobblestone and stone, stone bricks, deepslate variants, granite, diorite and andesite with their polished forms, sandstone, slabs, stairs, panes and comparable derivatives, and mineral storage blocks. Status does not propagate to specialized derivatives: Glass may qualify while Glass Panes do not automatically qualify, and Bricks may qualify while Brick Slabs do not.

The category is partly an economic intervention. Wood is already extraordinarily useful and naturally demanded, and polished rock largely adds a crafting step to abundant material, so neither creates new strategic demand. Clay, mud, concrete, and glass support distinctive construction-oriented acquisition and preparation processes whose competitive value would otherwise be weak in a compressed match. The intent is to reward meaningful construction-material activity rather than arbitrary extra crafting clicks.

[OPEN] The XP premium for legitimately incorporating Construction Blocks is a balance target, not canon. Per-recipe Primary Material mapping, derivative handling, and the interaction with anti-farming rules remain unresolved.

## 5.11 Block economy and infrastructure labor

**Working model; non-canonical estimates. Source: [WI].** The block economy must not be modelled as blocks extracted to blocks available to Construct size. At least six quantities matter: bulk or ordinary material supply; Construction Block supply; total physical construction supply; infrastructure labor, meaning the player-time a team can realistically commit to establishing, expanding, repairing, supplying, reorganizing, or dismantling infrastructure; construction efficiency, meaning built-world output per committed unit of labor; and realized construction output, limited by the intersection of available material and efficiency multiplied by committed labor.

Material abundance and infrastructure labor do not necessarily increase together. [PROTOTYPE] For a balanced seven-player team across a representative thirty-minute match, a mature total usable construction supply of roughly 2,500 to 3,000 blocks is the current modelling target, of which perhaps 500 to 900 are intentionally prepared Construction Blocks and 1,800 to 2,400 are ordinary, bulk, or improvised. An economy-specialized team coordinating Extraction, Construction, and Logistics might plausibly reach 4,000 to 5,500 total and 1,000 to 1,600 Construction Blocks. These are modelling targets only and are not balance canon.

Construction specialization should initially alter the composition and effective structural use of the material economy rather than acting as a generic percentage bonus to blocks generated. Extraction pushes front-of-pipeline acquisition; Construction creates disproportionate demand for construction-specific feedstocks and extracts greater structural and progression value from them; Logistics makes geographically separated material economically accessible; Production supports transformation where relevant; Exploration reduces access and travel cost and establishes Routes.

# 6. Infrastructure and Logistics

## 6.1 Infrastructure Mode and recognition

**Working canon, reconciled 10 September 2026. Sources: [I], [WI], [C §2].** At the infrastructure progression breakpoint, currently Level 6, eligible classes use shared **Infrastructure Mode**. Construction recognizes a Construct; Development recognizes a Development Zone; Exploration recognizes a Route; Logistics recognizes a Supply Line. Eligibility remains authored per class, not automatically inherited from every archetype tag. [OPEN] Combat’s corresponding Level 6 economic system remains a nighttime/combat-economy question.

Ordinary building, farming, movement, and transport remain unrestricted. Infrastructure Mode recognizes qualifying present world state or demonstrated capability; the system need not have witnessed its original creation. Build then designate, designate then build, recognize existing modified construction, and expand or repair a designated site are valid workflows. Recognition can generally be retroactive, although a connection still requires proof of its current capability.

### 6.1.1 The four infrastructure identities

| Validation type | Extent based and organizes a place | Relation based and organizes a connection |
|---|---|---|
| Constitutive | Construct | Route |
| Facilitative | Development Zone | Supply Line |

Constructs and Development Zones are established primarily through designation and measurement of a spatial extent. Routes and Supply Lines are established primarily through demonstration or proof between locations. All four depend on physical Minecraft world state; tangible versus intangible is not the adopted axis.

Constitutive infrastructure validates its own underlying state or capability: sufficient qualifying construction for a Construct, or a demonstrated traversable connection for a Route. Facilitative infrastructure additionally validates the process and enabling conditions it supports: developmental resources and growth conditions, or transport capability and the state supporting cargo flow.

### 6.1.2 Systemic and emergent disruption

Systemic disruption removes an objectively required condition: destroy qualifying construction, sever the only traversable bridge, remove crops or block required growth conditions, or destroy transport apparatus. Emergent disruption reduces usefulness without necessarily invalidating recognition: occupy a Construct, ambush Route users, attack farm workers, or intercept available carriers and cargo.

The system evaluates objective capability, not architectural or strategic quality. Danger, enemy control, exposure, inconvenience, and poor defense do not alone invalidate infrastructure. Recognition does not protect the underlying world from those consequences. Facilitative systems should be re-evaluated regularly; repairing their required conditions should generally restore functionality without consuming an entirely new infrastructure slot. [OPEN] Evaluation frequency, grace periods, partial degradation, and circumstances requiring renewed proof are not specified.

## 6.2 Constructs

**Working. Sources: [I], [WI], [WB], [C: Construction].** A Construct recognizes qualifying construction investment and scale. Its current preferred intrinsic benefit is Structural Integrity (§6.2.4), under the benefit symmetry principle in §6.15. [HISTORICAL] Occupation or sustainment efficiency — recovery, provisioning, Hunger and exhaustion — is retained as an earlier framing of the same slot, not a competing current answer. Integration is a separate benefit and cannot replace intrinsic value.

### 6.2.1 Permissive designation

Houses, fortresses, walls, bridges, gates, towers, bunkers, platforms, fortified caves, chokepoint fortifications, and other substantial player construction can qualify. No roof, door, interior volume, enclosure percentage, room layout, predefined shape, or architectural-quality score is required. A wall is not a deficient fortress.

Recognition examines qualifying construction that exists now. [HISTORICAL] A Build Mode that only records blocks placed while active is superseded as the exclusive recognition model. [OPEN] Eligible material, natural versus modified block provenance, minimum investment, ownership, capture, and exact potency calculations remain unresolved. Destruction can reduce or invalidate recognition by removing sufficient qualifying construction; the numerical threshold remains open.

### 6.2.2 Slots and spatial metrics

**Construct Slots** limit the number of separately recognized Constructs. **Buildable Scale** limits how much physical extent/material belongs to a recognized Construct and contributes, with qualifying investment, to scale/potency. **Operational Area**, governed by **Operational Scale**, determines where other recognized infrastructure may legally connect for integration.

Operational Area is an infrastructure connection envelope, not a general buff radius. A Development Zone must appropriately fall within or intersect it; a Route connection point and a Supply Line node must be within it. Infrastructure outside remains independently valid but cannot connect to that Construct. [OPEN] Exact intersection, distance, verticality, overlap, and boundary rules remain undefined.

Buildable Scale and Operational Scale are separable progression dimensions. To connect a separated farm, a player can increase Operational Scale or spend buildable capacity, blocks, and labor extending the Construct toward the farm. Recognition limits never forbid ordinary construction beyond those limits.

### 6.2.3 Projection and protection

An exposed wall, slab, block, or bridge can receive full recognized potency and Operational reach if it satisfies the eventual scale requirements. Its exposure is not inherently an exploit. Spending material on projection expands physical extent and integration geography; spending it on protection or centralization creates defended working space, storage, and transport nodes. The world makes exposed players and infrastructure vulnerable.

A team can eventually achieve both projection and protection by paying the material, extraction, transportation, placement-labor, and time costs. The recognition algorithm should not penalize this investment. [OPEN] Construct balance must be calibrated against phase-specific block availability, Extraction efficiency, bulk Logistics, placement speed, class material efficiency, labor, and opportunity cost. At a relevant phase, substantial projection or substantial protection should be attainable without trivially maximizing both; additional team investment should expand that frontier.

[PROTOTYPE] Test structures at 64, 128, 256, 512, and 1024+ qualifying blocks to learn what real construction those investments permit. These are experimental bands, not approved recognition thresholds. Construction specialization must not make material acquisition, transport, labor, or time irrelevant.

### 6.2.4 Structural Integrity

**Working canon. Source: [WB].** A recognized Construct grants Structural Integrity to selected qualifying built and infrastructure world state, increasing its resistance to hostile destruction. Structural Integrity should not reinforce every block inside a Construct or its Operational Area.

It applies to qualifying Construction Blocks incorporated into the Construct, and to explicitly recognized infrastructure-relevant blocks and components within it. Ordinary construction still contributes to Construct recognition, still provides all normal Minecraft physical utility, and still forms walls, bridges, platforms and cover, without automatically receiving Structural Integrity.

This draws an intentional distinction. Ordinary blocks are cheap, immediate, physically useful, and easier to alter or breach. Construction Blocks are deliberately prepared structural materials that are progression-relevant and capable of receiving Structural Integrity when incorporated into a Construct. This gives Construction Blocks a functional reason to exist beyond an XP premium.

The resulting decision is a real one. A team holding 500 total blocks but only 80 qualifying Construction Blocks must choose where hardened structural investment matters: a gate, an inner wall, a bridge support, a bunker, an infrastructure enclosure, or a forward-facing defence.

[OPEN] Exact Structural Integrity strength is unresolved. Do not yet create material-specific Structural Integrity tables; vanilla hardness and blast-resistance differences already distinguish materials and may be sufficient underneath a shared modifier. Whether it affects ordinary mining, explosions, and abilities identically is also unresolved.

### 6.2.5 Protecting infrastructure components

A Construct may also apply Structural Integrity to narrowly defined infrastructure-relevant components located within it: recognized Supply Line nodes and components, Development-enabling infrastructure, infrastructure workstations, and other explicitly recognized functional infrastructure blocks.

Do not automatically treat every chest, furnace, crafting table, crop, or fence inside an Operational Area as reinforced. [OPEN] Infrastructure-relevant block must eventually be defined narrowly.

The strategic consequence is that Construction can physically protect the team's economic machinery. This gives a second reason to co-locate infrastructure with a Construct beyond the integration multiplier: the Construct can make critical integrated infrastructure harder to dismantle. It also preserves raiding and breaching, because ordinary walls and improvised construction remain normal Minecraft terrain while deliberately invested hard points gain additional persistence.

[OPEN] What happens to Structural Integrity when practical control changes, and whether captured Construction Blocks immediately lose former reinforcement, gain occupier reinforcement, or require new recognition, are unresolved.

## 6.3 Development Zones

**Working. Sources: [I], [WI], [C: Development].** Development Zone is the current working name for recognized managed productive/developmental world state within an extent. Designation and measurement recognize existing eligible resources together with the conditions facilitating development. Targeted improvement of eligible processes remains the direction; this is not a generic random-tick-speed increase.

Cultivated crops, managed animals, Crop Patches, Herds, and other renewable biological systems are candidates. [OPEN] Exact eligibility awaits the Crop Patch, Herd, and Mob Swarm foundations. No complete population system is implied by these names.

Validity responds to present conditions. Removing wheat from valid farmland or blocking required light can disrupt the productive state; an empty former farm does not necessarily retain full functionality indefinitely. Replanting or restoring required conditions should generally restore function without a new slot. [OPEN] Exact productive-state thresholds, empty-state behavior, boundaries, improvement rates, and restoration timing remain unsettled. A Development Zone remains independently establishable and useful without a Construct.

## 6.4 Player recognized Routes

**Working. Sources: [I], [WI], [C: Exploration], [O §20].** A Route proves traversal through a physical connection. Terrain, roads, bridges, tunnels, waterways, and other movement infrastructure may support it. Early independent benefits still favor movement efficiency over unconditional speed. Maximum length/reach and branching/network complexity are separate progression dimensions; exact values remain open.

A dangerous, enemy-controlled, monster-infested, or ambush-prone Route remains valid if the objective traversal capability exists. Destruction of the only traversable connection can invalidate it. [OPEN] How altered paths are revalidated, whether replacement traversal requires new proof, submission details, ownership, shared use, width, and progression metrics remain unresolved.

An ordinary road, a map-authored Starter Route (§4.3), and a player-recognized Route remain distinct contexts. A canal, bridge, or tunnel may support both a Route and a Supply Line, but each requires its own proof. Route recognition never automatically grants Supply Line recognition or vice versa.

## 6.5 Supply Lines

**Working canon, reframed. Sources: [WB], [I], [WI], [C: Logistics], [O §20].** A Supply Line is persistent infrastructure produced by demonstrating repeatable resource delivery to an eligible destination. Its origin, path, Flow Weight, Item Rate, and other performance properties are derived from the logistical movement that establishes it rather than being specified beforehand.

The destination is strategically intentional; the origin is primarily descriptive and observed. The real Logistics question is normally how to make resources available here, not what can be done with the contents of a particular chest over there. The origin remains mechanically real and important, but it need not be manually selected before the logistical action begins.

Origin is evidence, not necessarily an instruction. A qualifying logistical action causes actual cargo to begin moving toward a useful destination, and the system can observe where qualifying cargo entered logistical movement, what transport method moved it, what corridor was involved, how long successful delivery took, and where cargo was delivered. Origin is therefore where qualifying cargo began the demonstrated movement, and destination is the eligible location or container where it successfully arrived.

This removes any need for a Logistics class ability to contain an explicit select Supply Line origin step. A Skeleton Crew ability can simply make undead carry actual cargo somewhere; a Merchant Advertisement can simply cause qualifying actors to deliver actual resources somewhere. Classes create unusual ways of moving resources; the system recognizes qualifying resource movement as infrastructure. Do not force class abilities to explicitly create a Supply Line.

Proof is a demonstration of repeatable delivery, not merely that an item can physically reach a destination. A random item or mob accidentally arriving in a container is not sufficient; the movement must constitute intentional qualifying Logistics. Once recognized, the infrastructure extrapolates persistent logistical capability from the demonstrated instance, and the player does not manually reproduce every future delivery.

[HISTORICAL] The earlier sequence — Infrastructure Mode, select source container, select destination container, prove transport — is superseded as the defining model. Directionality is retained: the reverse direction still requires its own demonstrated delivery. Current establishment grammar: the player identifies a useful logistical destination; authors an intended corridor toward it; a qualifying transport method begins carrying actual cargo; the method resolves the intended delivery using its own movement capabilities; the system observes entry point, method, actual movement, Transit Time, and successful destination; delivery demonstrates repeatable capability; origin, destination, corridor, Flow Weight and Item Rate are derived; a Supply Line may be recognized; and it then persistently reproduces the demonstrated capability. [OPEN] The exact input sequence is unresolved. Do not prematurely require marking an origin container first, and do not require every class ability to contain explicit Supply Line interface.

The existing upstream-storage → downstream-storage distribution model remains: actual upstream items gradually become available downstream, allowing team benefit while the Logistics player is elsewhere. The handoff clarifies that physical transport proves the capability and recognized flow may represent its continued operation. It does not require every transferred item to persist as a dropped entity along a path.

### 6.5.1 Transport specific performance

**Transit Time** measures elapsed delivery time for the demonstrated method; ordinary player transport has baseline Flow Weight 1. Flow Weight is an authored gameplay property of a logistical movement method, representing the standardized carrying capacity that method demonstrates. It determines how many items transfer per Supply Line pulse. It is not Item Rate, and it is not derived solely from literal Minecraft inventory slot count.

Item Rate is not an authored property of the carrier in isolation. It is derived from how quickly the actual demonstrated cargo movement reached its destination: observed successful Transit Time yields Item Rate, which sets pulse frequency. Flow Weight therefore determines items per pulse and Item Rate determines how frequently pulses occur, with effective throughput conceptually Flow Weight multiplied by Item Rate. Do not collapse these into a single generic throughput stat in the underlying design. [HISTORICAL] The earlier single Flow Rate proportional to Flow Weight divided by Transit Time is superseded by this pair.

[PROTOTYPE] An Allay illustrates low authored Flow Weight with relatively fast movement, tending toward smaller and more frequent pulses; a Camel illustrates higher authored Flow Weight with slower movement, tending toward larger and less frequent pulses. Final Item Rate still comes from the actual successful demonstration, so distance, terrain, and route quality matter: a long or poor Allay delivery can produce a lower Item Rate than a short easy Camel delivery. Transport archetype influences performance; the demonstrated journey determines the result. Exact values remain balance territory. [OPEN] Exact units, normalization, eligible cargo, and ratings beyond the baseline are not established.

Proof demonstrates that a particular cargo-rated method can move capacity W from A to B in time T. It does not merely measure how quickly any player can reach B. Valid methods may involve players, Logistics-class or summoned carriers, camels, Allays, golems, minecarts, boats, water systems, or other engineered transport. Exceptional mobility improves Logistics only insofar as a valid rated transport method can use it while carrying its rated capacity.

[PROTOTYPE] A fast low-weight Allay, slower high-weight camel, or moderately faster high-weight golem fleet illustrates possible speed/capacity tradeoffs; these properties and numerical ratings are not canonized. Continuity or simultaneous transport may become further dimensions. Node count/network complexity and individual line reach are separate progression dimensions. [OPEN] Their values, branching rules, filtering, capacity, reliability, and future direct allied replenishment remain unspecified.

### 6.5.2 Physical proof and represented flow

Test cargo can demonstrate water transport, a minecart trip can demonstrate rail transport, and a carrier delivery can demonstrate carriage. Subsequent flow may be simulated or represented, while the supporting physical world remains relevant. A severed rail system, obstructed waterway or bubble column, removed required carrier, or invalidated source/destination node can degrade or invalidate the line.

The system should periodically verify the physical conditions necessary for the proved capability. [OPEN] How complex paths and dependencies are remembered, validation frequency and cost, proof renewal, carrier commitment, buffers, inventory accounting, and exact degradation behavior require implementation design. This framework does not establish free item duplication or continued unchanged flow after its physical support is destroyed.

Enemies primarily contest the Minecraft systems that make transport work: nodes, vehicles, carriers, transfer points, rails, waterways, plumbing, storage, and potentially cargo/intermediate buffers. An abstract Supply Line health bar is not the primary contest model. Physical protection of those systems gives fortified Constructs Logistics value even when an exposed Construct has equal Operational reach.

### 6.5.3 Shared geography and unusual transport

A canal can prove player traversal for a Route and independently prove cargo transport for a Supply Line. Neither requires the other. Supply Lines need not follow horizontal walking paths. Gravity-fed delivery can legitimately establish a fast one-way line if it proves valid cargo transport; reverse transport requires separate capability.

[PROTOTYPE] The handoff’s source “1000 blocks above” example expresses this vertical-transport principle, not a selected playable-map height or an approved world-height override. Actual implementation remains constrained by the chosen Minecraft world and transport system.

### 6.5.4 Reconciliation with earlier transfer language

[HISTORICAL] The earlier uncertainty between generic gradual transfer and recognition of physical flow is narrowed by this handoff: proof establishes the rating; persistent supporting world state maintains it; represented ongoing flow need not continuously render every cargo item. Remaining accounting, revalidation, and transport-specific details stay [OPEN]. No mandatory Route, universal continuous ground path, or one required carrier type is introduced. Industrial Enchanter completion remains independent of recognized Supply Lines (§7.6).


### 6.5.5 Player-authored corridor and transport resolution

**Working canon. Source: [WB].** Living and unique carriers have very different movement capabilities and may lack an obvious long-distance delivery vector. The solution separates route intent from transport resolution: the player authors and demonstrates the intended logistical corridor, and the selected transport method then attempts to resolve that delivery using its own movement tools, restrictions, navigation, and pathing. The player authors the route; the transport method authors how that route is traversed logistically.

Demonstration is not literal playback. The carrier need not reproduce the player's exact block-by-block footsteps; the demonstration communicates an intended corridor, movement vector, and delivery path, which the carrier resolves in its own movement vocabulary. A parrot, bat, or Allay may fly, cross gaps unavailable to grounded carriers, and take valid aerial shortcuts. A Camel is grounded and must resolve a valid terrestrial traversal, and cannot reproduce an impossible jump merely because the player demonstrated one. A boat requires valid water traversal, a minecart resolves through rail topology, and water transport resolves through actual flow. One authored corridor may therefore produce different successful paths and performance depending on the method selected.

Player movement abilities do not automatically transfer. If the Logistics player leaps a ravine, climbs exceptionally, briefly flies, teleports, or uses a movement ability while authoring the corridor, the carrier does not inherit that capability. The selected transport method must be able to resolve the demonstrated delivery through its own movement vocabulary. This preserves transport-method identity and prevents Supply Line performance from collapsing into whatever speedrun path the player personally can execute.

Transit Time is measured from the carrier's cargo delivery, not the player's route-authoring traversal. If the player demonstrates the intended corridor in twelve seconds and a loaded Camel resolves it in twenty-seven, the Supply Line Transit Time is twenty-seven seconds. The player demonstrated where and how delivery should be attempted; the carrier demonstrated how well that logistical method can actually perform it.

### 6.5.6 Engineered and living methods

Self-directed engineered transport and guided living carriers do not require separate Supply Line systems. Rail resolves an intended connection through existing rail topology, water through actual flow, a Camel through grounded movement, an Allay through flight and navigation, and a class-created aerial carrier through whatever movement vocabulary that class provides. The common question is whether the method can successfully resolve the intended delivery and deliver qualifying cargo to the destination; if it can, its actual successful movement provides the proof.

[OPEN] Do not yet canonize a generic penalty such as automated methods always having reduced Flow Weight. Engineered systems naturally offer a predictable vector, repeatability, potentially low establishment attention, and infrastructure that may already physically encode the path. Living and guided systems may offer route flexibility, terrain adaptation, aerial traversal, class abilities, and potentially better demonstrated Item Rate or higher authored Flow Weight. An abstract compensatory penalty may be unnecessary if the profiles already differ enough. Compare actual candidate methods before introducing a universal modifier.

### 6.5.7 Persistence of demonstrated capability

Once established, a Supply Line preserves the demonstrated logistical capability. The Logistics player does not repeatedly travel the route, continually recast the establishing ability, repeatedly command the same carrier, or manually perform every delivery, and is free to do other economic or combat work. Infrastructure converts demonstrated player capability into persistent world capability.

Distinguish this from a temporary buff applied to an already existing Supply Line. An effect making existing lines pulse faster for ten seconds should not permanently rewrite their Item Rate. But where an ability is itself part of the logistical method used to establish the line, the successfully demonstrated performance becomes the line's persistent performance.

### 6.5.8 Relation classification, capture, and geography

Supply Lines remain relation-based infrastructure in the four-identity matrix, but not because the player explicitly selects two endpoints. The causality is delivery need and destination, then demonstrated logistical movement, then observed origin, path and performance, then a persistent delivery relationship. The destination is strategically primary; origin and path are mechanically important properties of the demonstrated capability. Revisit the classification later only if necessary.

The delivery model remains compatible with the capture and control framework. Authorship earns the establishing team its historical progression rewards; recognition exists because qualifying delivery capability was demonstrated; ongoing benefit depends on current functional access and team-relative integration. Capturing a destination does not mean the occupying team inherits the defeated team's logistical network, because a Supply Line represents a demonstrated delivery capability including its movement relationship. The occupier may need to establish its own qualifying delivery capability to that destination before the location is fully integrated into its network. This matters more for Supply Lines than for Constructs or Development world state because they are more relational.

The revised model creates a natural but non-mandatory relationship with Exploration. A good Route may improve the path available to grounded logistical carriers, and bridges, tunnels, roads, waterways and other player-created world state can affect logistical resolution. Aerial methods may value geography differently. Neither archetype should mechanically require the other, but they should cooperate naturally.

[OPEN] Long-distance Logistics still raises how a player refers to a distant destination that is not currently visible. This belongs partly to the pending Vision / Detection / Information work and to a Known or Designated Location vocabulary. A Logistics class may eventually reference a previously known destination without line of sight while arbitrary unknown coordinates cannot normally be targeted remotely. Do not build a Logistics node menu into current canon.

[OPEN] How robustly different entities can resolve a player-authored long-distance corridor with their own pathfinding is a technical question, and a datapack cannot be assumed capable of transplanting one mob's navigation onto arbitrary others. Keep the design rule as written: logistical carriers resolve a player-authored corridor according to their own movement vocabulary. Evaluate implementation compromises afterward rather than letting feasibility drive the design.

## 6.6 Infrastructure integration

**Working numerical targets. Sources: [C §2], [O §9].** A Construct can anchor an integrated center. Under the reconciled framework [I], [WI], legal connection is determined by Operational Area as defined in §6.2.2. A Construct alone grants no automatic XP multiplier. Distinct Development infrastructure, a Route, and a Supply Line connected to or established within the relevant area can increase otherwise legitimate activity XP.

| Distinct connected systems | T1 target | T2 target |
|---|---:|---:|
| None: Construct only | 1.000× | 1.000× |
| One | About 1.025× | About 1.050× |
| Two | About 1.055× | About 1.100× |
| Three | About 1.100× | About 1.150× |

The system rewards distinct-system integration, not multiple copies of one flat aura. Each infrastructure type should be able to improve its own contribution, so Construction is not the sole controller of the network’s progression reward. Full T1 and full T2 values are first-pass balance targets.

[OPEN] Mixed T1/T2 combinations, overlapping centers, eligibility of XP-generating activity, attribution to infrastructure creators, disconnection, and exact boundary/intersection calculations are unresolved; Operational Area now owns legal connection eligibility. T1/T2 describe contribution strength; they are not automatic aliases for Levels 6/12 or 20/25.

## 6.7 Infrastructure progression and night operation

**Working. Sources: [C §2], [N].** Level 6 begins class-authored recognition, with one slot as the current starting-capacity idea. Level 12 expresses a role through that specialization rather than simply providing generic “Infrastructure II.” Levels 20 and 25 may extend that same system as part of individual advanced rewards. The earlier universal 6/12/21/27 infrastructure cadence is superseded.

Night may reduce each system’s intrinsic effectiveness and offer later resilience upgrades. [OPEN] It is not yet clear whether integration XP itself is reduced, how mixed resilience works, or which effects change. Chapter 3 owns the shared day/night direction rather than assigning separate incompatible night rules here.

## 6.8 Transport and instrumental cargo

**Working / Prototype-Test. Source: [O §14A].** Players, vehicles, class carriers, dropped-item transport, water, hoppers, and minecarts should remain possible logistics vocabulary where technically compatible. The Industrial Enchanter measures successful item flow, not whether the team used an approved technology ladder.

Its current Barrier-based cargo represents inaccessible fuel, not a new freely usable resource. Stacks can represent cargo quantity; no one-unit-per-slot rule or inventory-lock system is implied. The complete Worksite lifecycle is in §7.6.

[OPEN] Cargo behavior on death, interruption, enemy pickup, storage, portals, disconnects, or late delivery remains undefined. KeepInventory for ordinary possessions does not silently decide these cases.

## 6.9 Infrastructure payback horizon

**Established principle. Source: [WI].** Persistent infrastructure has a payback horizon. Progression can increase infrastructure potency, but advancing match state reduces remaining match time, uncontested labor, and the time available for an investment to repay its establishment cost. An early Route may repay across much of the match; a late Route needs a much more immediate tactical purpose. The same applies to Supply Lines, Development Zones, and Constructs. Late infrastructure must establish faster, produce more immediate value, repurpose existing world state, solve an immediate tactical need, or otherwise justify its shortened horizon.

## 6.10 Control windows and infrastructure labor

**Established match-flow principle. Source: [WI].** Mid- and late-game infrastructure time is not given by the match clock. It is won through control. Objectives and strategic contests create temporary windows of access, space, safety, and labor availability during which a team can establish or expand infrastructure.

Infrastructure and contestation form a reciprocal cycle. Infrastructure improves projection, economic position, and objective control; that helps win strategic contests; winning control creates a development window; the team converts some combat presence into infrastructure labor; the infrastructure becomes more valuable; the stakes of the next contest rise; the enemy attacks or disrupts; the cycle repeats.

Progression and material abundance rise throughout the match, but discretionary infrastructure time need not. Enemy contact increases, objective contests matter more, existing infrastructure needs defending, players must rotate, and deaths remove presence. Early infrastructure is therefore more likely to be material-constrained and late infrastructure more likely to be labor- and attention-constrained. [PROTOTYPE] A working qualitative curve places scarcity at Levels 1 to 6, a construction window at 7 to 12, the likely peak intersection for large new infrastructure formation at 13 to 18, collapsing labor under still-rising throughput at 19 to 24, and reactive repair, modification, fortification, demolition and reconnection at 25 to 30. These bands are not timing canon until progression pacing is settled. §2.4 owns the shared phase model.

Economic players hold simultaneous combat roles. A Construction player may be the team's tank, a Development player an assassin, a Logistics player a support. Infrastructure work must therefore be naturally interruptible. The foundational interruption test for any persistent economic activity is what happens when that player suddenly needs to fulfil their combat role. The preferred answer is that the activity pauses, meaningful persistent progress remains, the player fights, and the activity can later resume. Avoid systems where leaving to help the team wipes minutes of progress or requires starting over.

Large infrastructure should be able to emerge across multiple successive control windows rather than requiring one uninterrupted construction period. Partially developed infrastructure is normal world state, not failed infrastructure: a half-built fortification already provides cover and terrain, represents invested material and labor, may support a smaller recognized Construct, can be attacked or defended, and can later be resumed. A late-game Construct can therefore record accumulated territorial success over time, and by late match the map should visibly contain the history of previous development and conflict.

Winning an initial strategic fight does not entitle a team to finish its infrastructure safely. Starting major infrastructure while the opponent retains the capability to contest it converts current territorial advantage and player labor into future advantage while exposing that investment to contest. An opponent may legitimately concede the first development window, progress elsewhere, improve equipment, attack another location, wait for labor to divide, and siege after the investment has been made. Infrastructure creates value and stakes simultaneously.

A class-design consequence follows: late Construction potency should increasingly help solve having material but very little time, not merely permit a bigger structure.

## 6.11 Authorship, recognition, and control

**Established distinction. Source: [WI].** Three concepts previously conflated should be kept separate. Authorship is who performed the action that created or developed something, and governs historical progression rewards: Construction XP, any Construction Block placement premium, XP for establishing infrastructure, and class triggers tied to performing the work. Authorship does not transfer because territory changes hands. Recognition is which infrastructure relationships currently exist, based on qualifying world state and capability. Control is which team can currently use, operate, and connect the infrastructure.

Most ongoing infrastructure benefits should depend more heavily on current functional access and control than on permanent authorship. World value can be stolen; historical progression earned by doing the work cannot.

## 6.12 Capture, local value, and network value

**Working direction. Source: [WI].** Do not adopt a simplistic rule in which enemy presence automatically invalidates infrastructure, one won fight changes formal ownership of every block, or captured infrastructure grants the occupier the creator's historical progression. Physical utility is substantially owner-agnostic: a capturing team can immediately benefit from walls, cover, bridges, tunnels, terrain modification, accessible storage and interactables where Minecraft rules permit, farms, and other surviving world state. Capturing a developed position may therefore let a team skip some physical infrastructure creation, and that is intentional. It prevents infrastructure from being risk-free economic accumulation.

A captured hub should not transfer as a perfectly functioning integrated economic machine. This follows the established extent and relation axis: extent-based local value is more directly capturable, while relation-based network value generally requires re-establishment by the occupier. A Construct is highly physically capturable and the built world continues to exist. A Development Zone is fairly capturable in practical terms where the occupier actually performs the relevant activity. A Route's physical path remains useful, but team-specific projection and network recognition may need to be re-proved. A Supply Line is the least automatically capturable, because its defining feature is an active demonstrated relationship between nodes; capturing a destination does not cause the occupier's cargo to flow through the defeated team's network. The extent/relation axis is defined in §6.1.1 and owned by [I].

[OPEN] The conditions under which a captured Construct's intrinsic benefit applies to the occupier, team-relative Route re-proof, Development Zone capture and use, and Supply Line interruption and re-establishment all require specification. Do not introduce a universal capture bar or automatic infrastructure conversion system before these physical and access-based rules have been tested.

Contested positions should accumulate physical history rather than reset. A late-game objective approach may contain original walls, a breach, later fortifications, repaired sections, competing or abandoned structures, tunnels, replacement bridges, severed and reconnected Routes, abandoned storage, Development Zones that changed practical control, new Supply Line approaches, and terrain destroyed by repeated combat. The history of territorial control should become physically legible in the map. Do not introduce a universal reset-on-capture mechanic unless later playtesting demonstrates a need.

## 6.13 Team-relative integration

**Working direction extending §6.6. Source: [WI].** Integration should not be evaluated only as whether a hub is fully integrated, but as how integrated it is for each team right now. The existing T1/T2 multiplier should follow active team-relative integration rather than permanent historical authorship.

Worked example. Before a siege, Team A has authored and functionally uses a Construct, a Development Zone, a Route, and a Supply Line, so A holds full integration and B none. Immediately after B wins the siege, B may physically occupy and use the fortress and exploit local productive state; A's Supply Line may cease functioning through interruption or lost access; A's Route may still physically exist without granting B its team-specific projection benefit; and B has not automatically established its own cargo flow. B therefore receives substantial local value but only partial integration, and B's infrastructure players now have meaningful work reconnecting the captured position into B's network.

This produces a deliberate intermediate outcome between capturing everything with instant full integration and gaining nothing while rebuilding the position from zero. Authorship determines progression rewards for creating infrastructure; current functional access and connection determine most ongoing infrastructure benefits.

## 6.14 Construct scale gating

**Working direction. Sources: [WI], §6.2.2.** Construct scale should be jointly resource- and progression-gated. Physical construction itself is unrestricted: if a team invests enough resources and labor to create an enormous fortress early, the fortress physically exists and provides all Minecraft-native physical utility. Infrastructure designation remains retroactive. Progression governs how much exceptional systemic Construct value can be extracted from that investment, but should not make physically existing construction unreal.

[OPEN] Do not finalize recognized-scale thresholds yet. The infrastructure-labor and control-window findings materially affect scale design. Before assigning any threshold series, establish the realistic player-minutes required to place it, how construction efficiency changes that, what fraction of total material supply can realistically be concentrated in one project, how many control windows a large project is expected to span, what recognized scale actually provides, and how late-game Construction compresses labor requirements. Earlier numerical thresholds are exploratory only.

## 6.15 Infrastructure benefit symmetry

**Working canon. Source: [WB].** The intrinsic benefit of each recognized infrastructure type should directly improve the vocabulary of its corresponding archetype rather than acting as an arbitrary generic buff. A Development Zone improves Development within a productive place; a Route improves traversal and projection through an established corridor; a Supply Line improves delivery of resources to a strategically useful place; a Construct should improve Construction and the persistence of built world.

This rules out making a Construct's primary intrinsic effect an XP bonus for placing blocks nearby. That is progression amplification rather than an improvement to built-world capability, and it creates an undesirable feedback loop in which a Construct exists to make building near it more rewarding, which encourages building near it. The current preferred intrinsic Construct vocabulary is Structural Integrity.

## 6.16 Logistics as delivery

**Working canon. Source: [WB].** Logistics materially changes how items, resources, supplies, and other strategic assets are delivered and made available where they are needed. The foundational verb is delivery and availability, not connection. A connection may result from Logistics, but connection is not what defines the archetype.

This separates Logistics from Exploration more cleanly. Exploration improves how players traverse, discover, access, and project through geography. Logistics improves how resources are delivered through that geography and made available at useful destinations.

## 6.17 Designation and evidence

**Working canon. Source: [WC].** Infrastructure Mode is not responsible for inferring arbitrary infrastructure from the world. It allows a player to **designate Minecraft-native evidence** of infrastructure, and the system then recognizes and extrapolates infrastructure properties from that evidence. This produces different recognition grammars for the four systems rather than forcing all of them through one submission mechanic.

Infrastructure is the systemic recognition, automation, and amplification of behaviour that already exists in Minecraft or through the player's class. Archetype membership does not by itself grant recognized infrastructure, and ordinary Construction, Development, Exploration and Logistics behaviour remains available before recognition is unlocked. The chain remains player action, demonstrated capability, persistent world capability.

### 6.17.1 Place and connection infrastructure

The four systems divide into two structural categories, and this should be embraced rather than normalized away. **Place infrastructure** covers Constructs and Development Zones; **connection infrastructure** covers Routes and Supply Lines. Place infrastructure asks what Minecraft evidence establishes that an area is meaningfully built or developed. Connection infrastructure asks what Minecraft objects establish the endpoints and what player behaviour demonstrates the connection between them.

In each system's own voice: a Construct says these authored blocks constitute one useful place; a Development Zone says these renewable productive resources constitute one productive place; a Route says these two marked places are meaningfully traversable; a Supply Line says resources can meaningfully move between these two storage nodes. Place and connection map onto the existing extent and relation axis in the four-identity table; they are an organizing frame for that axis, not a replacement for it.

### 6.17.2 Archetype-native anchors

| Archetype | Native anchor / evidence | Infrastructure |
|---|---|---|
| Exploration | Banners | Route |
| Logistics | Copper Chests | Supply Line |
| Construction | Construction Blocks | Construct |
| Development | Developable resources and populations | Development Zone |

Each archetype perceives a particular ordinary Minecraft object as the anchor or raw material of its infrastructure. This keeps infrastructure legible in the world and requires participation in the ordinary resource economy: an archetype does not merely press an infrastructure button, it needs the Minecraft things through which its infrastructure exists.

It also gives ordinary resources new economic significance without inventing currencies. Banners create relevance for sheep, wool, dyes and banner crafting, and already semantically communicate a marked or important location. Copper Chests make a logistics node visible to allies and enemies alike: an ordinary chest says items are stored here, while a Copper Chest says this storage participates in Logistics infrastructure and is a possible disruption target.

## 6.18 Designating a Construct

**Working. Source: [WC].** Construction uses Construction Blocks as its own infrastructure evidence. The grammar is to enter Infrastructure Mode, designate qualifying authored Construction Blocks or a bounded region containing them, have those materials recognized as belonging to a Construct, optionally designate further nearby regions or components, and let the system evaluate the aggregate recognized build.

The system does not need to decide whether something is objectively a house, a fortress or a bridge; that is fuzzy and unnecessary. The player declares that these authored blocks are one Construct, and the system evaluates objective properties of that declaration: block investment or mass, spatial extent, and possibly internal coherence, meaning how spatially separated the selected regions are. Coherence may remain an internal validation concept rather than a visible player statistic.

The core relationship is that more legitimate constructed material allows a player to claim and support more operational space. Insufficient block investment should constrain maximum legitimate extent, which prevents placing four blocks at four distant corners and claiming an enormous fortress area. Conversely, many blocks in a compact region can represent a dense high-investment Construct without requiring a large operational footprint.

Repeated region selection is preferable to requiring one mathematically contiguous mass, because legitimate Minecraft structures routinely contain gaps, air, separate walls, fence components, bridge supports, nearby defensive works, vertical separation and disconnected authored components. [OPEN] Exact region-selection representation is unresolved. Prefer player-assisted bounded selection over automatic recursive flood-fill of arbitrary world construction; do not continuously ask the implementation to infer enormous connected structures.

## 6.19 Development Weight and dynamic capacity

**Working. Source: [WC].** Development Zones are the simplest place infrastructure. The player enters Infrastructure Mode, designates something recognized as developable, and the system recognizes and evaluates the surrounding qualifying productive region. Further nearby developmental components may be added to the same zone. Developable evidence may include crops, trees and saplings, livestock, bees and hives, and other renewable productive Minecraft systems.

Raw counts cannot be compared across developmental types: ten wheat blocks, ten cows and ten saplings are not equivalent. An internal **Development Weight** normalizes productive value per recognized resource type, with Development Capacity conceptually the sum of qualifying weights. [OPEN] Exact weights are unresolved. As with Constructs, spatial extent and density matter, so that productive investment must justify operational area and one cow at each corner of an enormous field does not produce an enormous zone.

Block-based and entity-based development should not use identical normalization. Qualifying resource blocks can be counted with density and area limits preventing pathological layouts. Raw population count is dangerous on its own: a hundred cows crammed into a single block should not read as vastly greater legitimate Development than a functioning pasture. Population, occupied viable area, and a local density ceiling or diminishing return are candidate factors. [OPEN] Exact formula unresolved. Mixed zones combining crops, livestock, bees and trees should be allowed to constitute one productive area rather than four unrelated zones.

Unlike a Construct, a Development Zone should be periodically re-evaluated. A Construct derives legitimacy primarily from authored construction that exists; a Development Zone derives it from productive resources and populations that continue to exist and develop. If livestock disappear, crops are destroyed, hives vanish or productive area collapses, Development Capacity should be able to fall. This is preferable to permanently certifying a zone based on whatever was present at the moment of designation. [OPEN] Recalculation frequency unresolved.

## 6.20 Banner endpoints

**Working. Source: [WC].** Banners become Exploration's infrastructure anchor. The establishment grammar is to place or find Banner A, enter Infrastructure Mode and designate it as an endpoint, travel to another location, place or find Banner B, and designate it as the other endpoint, after which the system recognizes and evaluates the demonstrated connection. The Banner says this location matters; the player's traversal says a viable connection between these locations has been demonstrated.

The persistent Route need not be defined as an exact block-by-block replay of the player's recorded footsteps. The endpoints are the durable semantic identity of the Route; the demonstrated traversal is evidence used to establish and evaluate it. This gives Exploration an internal resource requirement, so it is no longer only about finding interesting things but also about provisioning itself to create navigational infrastructure. [OPEN] Whether Routes are inherently bidirectional, how much demonstrated path geometry is retained, how Route effect is calculated, whether alternate demonstrations can improve an existing Route, and the exact designate input all remain unresolved.

## 6.21 Copper Chest anchors and Supply Line execution

**Working. Source: [WC].** Supply Lines use **Copper Chests** as their infrastructure anchors rather than generic storage. This is primarily a legibility decision and should read to both allies and enemies: seeing an enemy Copper Chest should immediately communicate a strategically significant logistics node, a possible Supply Line endpoint, and a possible disruption or denial target.

**Resolved, late September 2026. Source: [WR].** Explicit start is reinstated. The grammar is: enter Infrastructure Mode, designate Copper Chest A as Source, perform the actual logistical transport, designate Copper Chest B as Destination. This intentionally reverses the 11 September instruction not to require marking an origin container first. That instruction existed mainly because origin-first designation created an unsolved interaction problem — how the player tells the game that an arbitrary storage container begins the infrastructure being established — and the archetype-native anchor model answers it. Designation is now legible, intentional, deterministic, comparatively easy to implement, and structurally parallel to the Banner grammar for Routes. It also bounds what the system must observe: the first endpoint says establishment begins here, the second says it ends here. Retrospective inference of which container was the origin is no longer preferred.

A Supply Line is therefore **persistent infrastructure that simulates repeated resource deliveries by the logistical method that established a connection between two designated Copper Chests.** Its Flow Weight represents that method's carrying efficiency and capacity; its Item Rate represents the frequency with which that method can deliver across the established connection under its relevant movement conditions.

Reinstating explicit Source designation does not put Supply Line vocabulary into class abilities; the firewall in §6.21.5 is unaffected.

[OPEN] The exact designate interaction is unresolved. Do not prematurely canonize right-click; a generic designate-targeted-block action may be cleaner than detecting arbitrary vanilla right-click interactions. Actual item withdrawal or deposit could form part of establishment, but reliable inventory-transfer detection should be feasibility-tested before becoming a design dependency.

### 6.21.1 Self-directed logistical entities

Any summoned or self-directed entity performing Logistics operates on a **Source to Destination assignment**. The player determines logistical intent; the entity determines how it physically resolves that assignment with its own movement capabilities. A walking worker pathfinds over traversable terrain, a future flying carrier may resolve the connection through flight, and an aquatic carrier through swimming.

**Resolved, late September 2026. Source: [WR].** Endpoints are sufficient to express player intent **for self-directed logistical methods**, which therefore need no player-authored corridor. This is not a universal answer, and corridor information is not globally deleted from Supply Lines. Two broad categories exist. **Self-directed methods** — a Skeleton Crew worker, a hypothetical bat or parrot carrier, a future aquatic carrier, other autonomous summoned workers — receive Source and Destination and answer for themselves how to get there, walking, flying or swimming as their movement vocabulary allows. **Intrinsically directional, path-authored methods** — rails, water channels and other physically directional transport — carry the corridor as part of the logistical method itself, because the construction already encodes movement direction and topology. Different methods may establish their connection differently; the system normalizes their output through Flow Weight and Item Rate rather than forcing identical topology. The 11 September player-authored corridor model remains canon for player-guided and path-authored methods and no longer applies to self-directed carriers.

This resolves the earlier problem where self-directed systems appeared directionless beside intrinsically directional systems such as rails and water channels. Directional infrastructure already encodes movement through its physical construction; self-directed entities instead receive strategic endpoints and independently resolve traversal.

### 6.21.2 Physical Logistics before recognition

Physical logistical labour and recognized Supply Lines are not the same thing. Before recognition, an actual carrier physically collects and moves actual items between storage: it travels, can be intercepted, carries cargo, can die, is affected by pathing, and must repeat the trip. That is already legitimate Logistics gameplay.

After the capability is recognized, the persistent infrastructure can reproduce the demonstrated resource flow without requiring the original physical carrier to make every future trip. The progression is manual physical Minecraft behaviour, then demonstrated logistical capability, then recognized automated infrastructure. Infrastructure should free the player or entity to establish new value rather than requiring permanent repetition of already-proven labour.

### 6.21.3 Derived Flow Weight and Item Rate

**Resolved, late September 2026. Source: [WR].** Neither quantity is an infrastructure stat the player authors, and the two do not share an origin. The establishment journey is not a benchmark run from which the system estimates both.

**Flow Weight is a property of the logistical vehicle or method**, representing its carrying efficiency and capacity, and determining items per pulse. A basic walking carrier, an improved or elite carrier, a minecart system and a future flying carrier each have their own. It is not inferred from a single observed cargo sample and is not a player-selected infrastructure stat. [OPEN] Exact values unresolved.

**Item Rate emerges from the method's actual movement behaviour over the established connection**, depending on carrier movement speed, connection length, traversability, terrain, rails, paths, bridges, tunnels, shortcuts, movement-enhancing infrastructure and method-specific traversal abilities. Player-authored Item Rate is removed: the player never declares that a line has Item Rate 5.

Environmental improvement should therefore improve Item Rate naturally. A Crew Member crossing difficult terrain makes a slower repeated trip and yields a lower Item Rate; a better path built for it yields a higher one, with no rule reading "+20% Supply Line Item Rate." A poor rail system yields a slower minecart and a lower rate; an improved or powered rail a higher one. Supply Lines inherit the consequences of actual Minecraft improvements rather than receiving disconnected numerical bonuses.

Distance is not eliminated, but matters indirectly: it changes how long the method takes to perform the connection, so a short connection delivers more frequently than a long one for an otherwise identical carrier. Because the method already experiences distance and traversal conditions, no separate arbitrary distance penalty is needed.

Do not derive Item Rate primarily from geometric source-to-destination distance. Distance already affects actual demonstrated delivery time, so building a bridge, laying rails, creating a shortcut, using flight, tunnelling through a mountain, or using a faster carrier improves the demonstration itself. This preserves player action, demonstrated capability, persistent capability. Different methods need not share movement mechanics; they become comparable because the system ultimately asks how much a system successfully moved and how quickly and reliably it moved it.

### 6.21.4 Recognized Supply Line execution

**Direct inventory transfer is the current preferred execution model.** Once a Supply Line has a source Copper Chest, a destination Copper Chest, a Flow Weight and an Item Rate, the recognized line periodically transfers up to Flow Weight eligible actual items from the source inventory into the destination inventory at the derived interval. The items are real, the inventories are real, the endpoints are physical and attackable, and the transfer is deterministic. This should be the first model prototyped.

At each simulated delivery pulse the line transfers up to the method's Flow Weight from Source to Destination, and the pulse interval represents Item Rate. The game does not continue physically simulating every individual carrier trip after recognition: physical logistical behaviour establishes the connection, and the system then simulates repeated performance of that behaviour. This is the automation payoff of infrastructure.

Two alternatives were considered and are not preferred. Invisible Copper Golems and other invisible carrier entities used to physically simulate the calculated rate makes mob AI the system's clock and brings pathfinding failure, entity overhead, obstruction, and chunk-loading questions; keep it only as a possible presentation experiment, not canonical behaviour. Physical item packets travelling the world at each pulse recreate a logistical simulation after the player has already earned infrastructure abstraction; revisit only if in-transit interception proves essential to counterplay.

[OPEN] Arbitrary-container inventory manipulation in Java 1.21.9 deserves a technical prototype before this is declared solved. Carrier entities, particles, sounds, or visible activation at Copper Chests may later serve as presentation without being responsible for the actual transfer.

### 6.21.5 Class ability and infrastructure firewall

**Working. Source: [WR].** A normal class ability should generally not say "create a Supply Line." Classes create unusual physical Minecraft behaviour, and the system recognizes that behaviour as infrastructure when appropriate. A Skeleton Crew ability knows "move cargo from this Source to this Destination"; it does not know "create Supply Line #4 with these Item Rate and Flow Weight values." The system independently knows that a logistical method has established a valid connection between infrastructure-eligible nodes, and when progression allows recognition it may recognize and simulate that behaviour as a Supply Line. Keep **class creates behaviour, then the system recognizes infrastructure**, violating the firewall only intentionally for a class whose exceptional fantasy is manipulating already-recognized infrastructure.

The Skeleton Crew consequence follows. Crew Members are self-directed logistical workers with an authored Flow Weight plus actual movement and pathfinding; a later upgrade producing a stronger worker need not say "+Supply Line Flow Weight" or "+Supply Line Item Rate," because greater cargo capacity, faster movement and better traversal produce those results naturally.

### 6.21.6 Shared connection grammar

**Working. Source: [WR].** Routes and Supply Lines are both connection infrastructure and share one grammar: start, then actual world behaviour or journey, then end, then a recognized connection. A Route runs Banner Start, player traversal, Banner End, Route. A Supply Line runs Copper Chest Source, actual logistical transport, Copper Chest Destination, Supply Line. The player is not drawing an abstract line; they designate meaningful Minecraft endpoints and establish a connection through actual gameplay. The journey is neither a minigame nor stat assignment, but the behaviour whose repeated operation the infrastructure represents.

The core principle is preserved: player action, actual Minecraft capability, recognized infrastructure, persistent simulation and amplification of that capability. This is preferable to a player drawing an abstract connection, assigning a numerical rate, and receiving resources.

[OPEN] Banner endpoints do not automatically eliminate all information about a traversed Route corridor. The Banner model improves endpoint legibility, establishment interaction and resource grounding; how much traversal geometry a Route retains is a separate question, and is not settled merely because self-directed logistical entities need only Source and Destination intent.

## 6.22 Implementation feasibility notes

**Working. Source: [WC].** Conceptually safe for datapack implementation: Infrastructure Mode state via scoreboard or tag; designating a targeted or nearby known block type; identifying Banners and Copper Chests; storing endpoint coordinates; counting and inspecting bounded areas after explicit player action; storing aggregate infrastructure properties; scoreboard-based pulse timers; entity ownership via tags or scores; and class-controlled source and destination state.

[OPEN] Needs prototyping: arbitrary-container item transfer; inventory-origin proof during Supply Line demonstration; performant region representation for Constructs and Development Zones; dynamic Development Zone scanning; entity-population normalization; and the chosen designate interaction. Avoid making design dependent on arbitrary vanilla right-click detection until tested, on continuous flood-fill or large world scans, on mob AI as the authoritative timing mechanism for recognized Supply Lines, or on exact replay of a player's route when endpoint and demonstration data suffice.

# 7. Objectives and Strategic Locations

## 7.1 Vocabulary and organizing principle

**Established distinctions. Source: [O §§1–2].** A Key Location is a place where strategic opportunity exists. An Objective is an accomplishment available there. A Contribution is an individual action toward that accomplishment. A place can support several objectives or valuable interactions.

The categories are Team Structures, Major Encounters, Worksites, World Locations, and possible player-established Key Locations. They need not map one-to-one to archetypes. A Worksite can overlap a Construct, developed area, Route, and Supply Line without becoming the same system.

## 7.2 Team defensive structures

**Canonical set; Working interactions. Source: [O §§3–6].** The first three defensive structures support destruction, defender elimination, and objective-specific interaction. Mixed attacks should be recognized from what happens in the world, not a declared method chosen in advance.

### 7.2.1 Pillager Outpost

The earliest/currently most accessible structure uses Overworld occupation, raiding, and theft. Players can attack the structure, fight defending Pillagers, or steal the functionally involved Allay. [OPEN] Meaningful destruction, defender behavior, theft requirements, and the Allay’s post-capture function remain unsettled.

### 7.2.2 Nether Bastion

The tougher fortification uses Piglin defenders and a physical gold hoard. Extracting and transporting the actual gold makes it an Extraction/Logistics problem as well as a siege problem. [OPEN] Hoard completion, structural validation, defender design, and the consequences of partial extraction need definition.

### 7.2.3 End Tower

The final defined defensive layer before the Fountain uses durable End-related construction, Endermen, and an End Crystal or machinery interaction. Vertical assault is central. [OPEN] Construction, machinery, completion, and defender details are unresolved. Extra generic loot is not automatically required.

## 7.3 Methods, rewards, and structural defeat

**Established principle. Source: [O §§8, 19].** There is no universal ordering of destruction < defender elimination < specialized interaction for XP. Methods should differ in tools, time, risk, access, physical reward, and persistent consequences.

Demolition can create permanent access; defenders can be cleared to establish control; Allay theft can alter function; hoard extraction moves actual wealth; machinery interaction can progress the objective. [OPEN] These examples do not settle reward values or exact state transitions.

Pure percentage-of-block destruction risks rewarding removal of cheap, irrelevant blocks. The project requires a meaningful structural or functional validation rule; none is finalized. Ability-driven destruction, tunneling, and repair must be evaluated against that rule, not independently invented exceptions.

## 7.4 Aether Fountain, exposure, and victory

**Established direction; major rules Open. Source: [O §§7, 21].** The Fountain is final team infrastructure tied to respawning. Disabling it affects the enemy’s ability to respawn. It should function as infrastructure rather than only a health bar.

[OPEN] Whether previous structures form a strict sequence, protection layers, or bypassable challenges is undecided. Fountain exposure may depend on some or all of them, but no choice is canonized. Obstructing a source and substantial destruction are earlier concepts, not sufficient finalized triggers.

[OPEN] Required disable duration, validation, repair, reactivation, treatment of already-living enemies, and the transition to victory must be resolved together. A single unnoticed block placement is explicitly not an assumed valid equivalent of destroying a Nexus.

## 7.5 Mining Outpost

**Working canonical design, revised 10 September. Sources: [O §§11–14], [RW].** The Mining Outpost is an Extraction challenge generator. The sequence is discover → invest → construct/progress apparatus → prospect/generate/reveal → excavate → extract → transport/use.

### 7.5.1 Core, functional multiblock, and facility

A phase-expensive Core concentrates economic gating. A recognizable functional topology around it establishes the apparatus without requiring a giant fixed schematic. The surrounding facility is flexible: shelter, storage, defense, access, Routes, and logistics can adapt to terrain.

This Construction-established apparatus belongs to the Mining Outpost. It is not a universal prerequisite for all Worksites or for ordinary Extraction. Economic/material gating is preferred where it can avoid arbitrary level locks.

### 7.5.2 Generate, then reveal

The site contains latent opportunity, but the exceptional ore deposit does not physically exist before successful operation. Prospecting creates phase-appropriate resource geography within eligible surrounding geology, then reveals enough information to pursue it. This prevents pre-mining from bypassing the Worksite’s development requirements.

After generation, resources are physical Minecraft blocks. Players must approach, excavate, acquire, transport, and use them. The apparatus does not automatically clear terrain, mine, dispense ore, or award a finished Diamond chest.

### 7.5.3 Depletion, interruption, and specialist advantage

Progress is intrinsic: intact deposit → partly excavated/extracted → depleted deposit. If the functional multiblock becomes invalid, further activation/prospecting/generation pauses, but already-generated ore and revealed information persist.

Generic players can exploit the opportunity. Extraction specialists should excel at interpreting the site, choosing approaches, tunneling, searching, and acquiring resources. The reveal must not solve so much of the task that Mole’s toolkit becomes irrelevant. [OPEN] The direct relationship between Sifth Sense’s player detection and Worksite detection requires clarification; the manuscript does not grant it ore sensing.

[OPEN] Core recipes, apparatus topology, activation, generation placement, reveal precision, finite generation budget, enemy operation, and XP remain undefined. “Depletion” is the intended physical lifecycle, not permission to assume infinite repeated deposit generation.

## 7.6 Industrial Enchanter and Lapis Silos

**Working canonical design; cargo Prototype-Test. Sources: [O §14A], [RW], [W].** A central Industrial Enchanter is associated with child Lapis Silos at increasingly difficult logistical distances. Logistics determines how much finite opportunity is realized; Production determines how useful the resulting output becomes.

### 7.6.1 Deliberate activation and gradual output

A team deliberately activates a Silo. Clear contest information should announce activation and remaining time; accidental entry into a loading zone is not the selected activation model. During the active period, cargo is released gradually.

Gradual output creates batching choices. A courier can leave early with less cargo or wait for a larger load and sacrifice transfer time. Capacity, hunger, loading, unloading, cooperation, terrain, safety, return trips, automation, and execution affect item flow. Raw speed can help, but is not the only performance measure.

### 7.6.2 Distance as a soft capability test

The conceptual progression spans early-mid through late-mid capability, potentially the beginning of lategame, not endgame. Near opportunities can be meaningfully attempted by ordinary carriage; more difficult distances reward developed logistical solutions. Exact Silo count remains open despite the three-stage conceptual framing.

No recognized Supply Line is required. No chest-boat → rail → Ender technology ladder, hidden Logistics-tier check, or approved transport recipe defines success. Teams can attempt difficult opportunities and achieve partial value through actual delivery.

### 7.6.3 Banked fuel and finite Production

Delivered cargo becomes proportional inaccessible banked fuel. After the transfer phase, that fuel determines a finite Production opportunity. Players bring prepared enchantable goods and convert the opportunity into useful enchanted output.

The prepared inventory does not make the Worksite arbitrarily large. Logistics establishes a finite budget; Production spends it effectively. This is the counterpart to depleting a finite ore deposit, while deliberately using a different gating method.

[OPEN] Fuel-to-duration or fuel-to-capacity conversion, leftover fuel, eligible items, pre-enchanted goods, XP, RNG, bookshelves, and normal enchanting outside the Worksite are unresolved.

### 7.6.4 Inaccessible fuel and Barrier cargo

Ordinary lapis, coal, kelp blocks, or blaze rods would introduce an unintended independent resource payout. The current prototype instead reuses the vanilla technical Barrier item with renamed/resource-pack presentation, working concept “Encumbered with Fuel.” It represents temporary cargo for inaccessible fuel rather than an ordinary economic material.

[PROTOTYPE] Its suitability for stacking, Survival non-placement, transfer methods, tagging, and presentation requires testing in the selected environment. This narrow reuse does not authorize adding a broad bespoke item economy. Final names, fuel theme, cleanup, death, interception, cross-team delivery, portals, buffering, and grace periods remain open.

### 7.6.5 Deliberate Worksite asymmetry

Construction gates Mining Outpost opportunity through creation of previously nonexistent geology. Logistics gates Industrial Enchanter opportunity through realizable timed flow. Once created, ore persists after apparatus disruption; the Enchanter instead consumes a banked finite productive opportunity. These are specific Worksite relationships, not universal dependencies between the four archetypes.

## 7.7 Supply Chain and other World Locations

**Open. Source: [O §15].** Supply Chain remains a recognized concept with unresolved advanced Development resources, location, completion, and reward. Wheat, carrots, and potatoes are baseline resources and must not become its unique reward vocabulary.

The older inaccessible crop zone requiring a constructed supply route is historical. It does not prove that Supply Lines require traced paths. Ordinary valuable World Locations can matter without a formal objective completion rule.

## 7.8 Major neutral encounters

**Working, with historical association. Source: [O §§16–17].** Giant Zombie, Ghast, and Ender Dragon form the neutral escalation. Prior design associated them respectively with Pillager Outpost, Nether Bastion, and End Tower, granting substantial XP and indirect siege advancement.

[OPEN] Spawn timing, encounter behavior, respawn, contest rules, XP, and the amount/form of siege advancement remain unresolved. Too much advancement would replace native siege; too little would make the neutral investment strategically weak. Do not infer numerical structure damage from the correspondence.

The Overworld → Nether → End → Aether thematic escalation does not require literal vanilla dimension progression.

## 7.9 Contribution and open objective contract

**Open. Source: [O §21].** Objective accounting must recognize mixed attacks and potentially Combat, resource, construction, and logistical contributions. No exact allocation is established. The unresolved match contract comprises structure sequence, exposure, disablement, repair, final victory, and recognition of meaningful defeat.

Worksite completion must follow actual finite opportunities rather than arbitrary class checks. Objective availability should coexist with ordinary progression; it is sometimes rational to concede one opportunity to develop or attack elsewhere.

# 8. Combat

## 8.1 Combat as economic contribution

**Established archetype; Working economic role. Sources: [C: Combat], [W], [N].** Combat exceptionally manipulates damage, survival, mitigation, sustain, and control. Its strategic value also includes protection, interception, denial, and creating uncontested time. A won fight matters when the team can exploit the position or opportunity it secures.

Combat should remain recognizable Minecraft play enhanced by class expression and progression. The team-size discussion emphasizes room for players who fortify, supply, scout, or maintain escape routes during a battle without performing the same combat rotation. These are possible contributions, not prescribed combat roles or credit rules.

## 8.2 Early skirmishing and information

**Working interpretation. Sources: [ORG], [M], [W].** Opposing expeditions can intersect at Route approaches, villages, mountain access, coastlines, caves, or resource/animal concentrations. Early fights therefore grow out of useful work and information rather than requiring a separate lane minigame.

[OPEN] The outline’s minute-level skirmish timings are hypotheses (§2.5). Damage, equipment availability, early Health, escape ability, and food access must be tested together before asserting when typical encounters happen.

## 8.3 Night combat and siege

**Working. Source: [N].** Richer hostile-mob opportunity and weaker infrastructure can make night favorable for combat-oriented activity. Players may hunt, raid, ambush, defend, disrupt distribution, or pressure a siege. This is a recurring incentive window, not a mandatory combat phase.

Chapter 3 owns the unresolved cycle and mob parameters. No automatic night kill multiplier is assumed. Advanced infrastructure resilience and anti-static-map pressure need joint testing so night remains a strategic opportunity rather than an irritating economy shutdown.

## 8.4 Terrain, protection, and persistent damage

**Established world consequences; Open global protection. Sources: [C: Mole and Golem Master], [O §§3–7].** Tunneling, construction, and real block destruction can change combat geometry. Mole’s Sinkhole is persistent destruction; Golem Master’s Wither Golem is explicitly non-destructive to blocks. Those individual statements must be retained rather than replaced with one generic ultimate-terrain rule.

[OPEN] Protected terrain, objective machinery, Construct vulnerability, friendly obstruction, enemy storage, fall damage interactions, and block-damage persistence require shared rules. Later waxing and durability ideas do not establish global infrastructure invulnerability (§9.8).

## 8.5 Death and recovery

**Working. Source: [W]; objective authority [O §7].** KeepInventory retains ordinary possessions but death still removes a player from the current position and exposes the team’s work. Supply, reinforcement routes, and remaining defenders determine whether an advantage can be held.

[OPEN] Respawn time, recovery values, spawn protection, combat XP, kill/assist attribution, and special cargo are not specified. The Fountain connects combat to match resolution, but its final state machine remains unfinished (§7.4).

# 9. Classes

## 9.1 Class philosophy and kit grammar

**Established philosophy; Working grammar. Sources: [C §§1–3], [CC].** Classes amplify recognizable Minecraft systems. Their strongest designs produce several uses from a coherent rule, allowing the state of the world to become relevant class state. Ordinary verbs remain universal, while specialists obtain exceptional efficiency, potency, or interaction.

The current draft grammar is hook/core idea, archetypes, passive, Ability 1 and upgrades, Ability 2 and upgrades, ultimate, optional build directions, world dependencies, and open questions. This is a documentation pattern, not a requirement that all classes have identical branching. Upgrade themes should support mixed builds rather than force unrelated subclasses.

Archetypes identify the systems manipulated; strategic roles identify the resulting team contribution. “Support” is a role, not an eighth archetype. Infrastructure eligibility and advanced rewards are authored for individual classes rather than automatically inherited from every archetype tag.

## 9.2 Roster and authority

**Sources: [C §§4–7, 11], [J], [ORG].** The canonical root documents four classes. The September 8 class-library export contains six, adding Merchant and Lightfooted. The export was accepted as the site’s class library, but does not supersede root class authority for this manuscript.

| Class | Domain classification | Maturity and provenance |
|---|---|---|
| Mole | Extraction primary; Exploration/Combat/Logistics secondary possibilities | Established core; unresolved classification and balance; canonical root |
| Gardener | Development primary; Exploration and Support/Combat possibilities | Established core, Working upgrade themes; canonical root |
| Golem Master | Construction / Combat | Working draft; canonical root |
| Kitfighter | Combat / Production | Current documented direction; canonical root |
| Merchant | Exploration primary in export; Production/Logistics tags | Working noncanonical draft; [NEEDS CANONICAL OWNER] |
| Lightfooted | Exploration primary in export; Combat/Development possibilities | Incomplete noncanonical draft; [NEEDS CANONICAL OWNER] |

[CONFLICT] The Organization Site preview’s summary identifies Merchant and Lightfooted differently from their actual exported primary fields. The table preserves the export fields and leaves root-absent classifications provisional. Mole’s and Gardener’s secondary tags in the site are not evidence that the root’s unresolved classifications have been finalized.

## 9.3 Mole

**Conceptually settled. Sources: [C §4], [WC].** The world beneath and inside terrain is as navigable to Mole as its surface is to everyone else. Sand, gravel, caves, geology, and excavation opportunities are its world dependencies. Primary archetype Extraction; secondary Combat and Exploration. Logistics is not a Mole archetype.

### 9.3.1 Passive — Sifth Sense

While actively digging, Mole senses nearby sand and gravel. Excavating sand or gravel can sift or clear connected falling material substantially more effectively instead of producing normal collapse behaviour. [HISTORICAL] The earlier player-detection formulation, scaling through radius, rate and location specificity, is superseded. [OPEN] Sense radius, sift volume, relationship to vanilla falling-block behaviour, progression scaling, and Worksite interaction are undefined.

### 9.3.2 Ability 1 — Tunneling

A toggled digging mode amplifies the current digging tool’s speed. Mole cannot attack or build while active. Moving into a wall automatically excavates a 1×2 player passage; shifting directs digging downward and jumping directs it upward.

Branches are Bore (increased tunnelling speed), Gallery (approximately 3×2 useful tunnels), and Dig In (substantially harder to displace while actively excavating). [HISTORICAL] The earlier Speed / Width / Stability labels are superseded. [OPEN] Amplification, tool/material compatibility, resource handling, and protection interactions require definition. Generic Efficiency progression must be tested with the amplification rather than balanced independently.

### 9.3.3 Ability 2 — Drill Rush

Mole enters a wall or floor, then reactivates to emerge and deal damage in its facing direction. Armored Emergence grants major damage reduction on emergence. Undermine stuns enemies when emerging from the floor. Burrow Chain grants speed on wall exit and resets the cooldown if a new wall section is entered within a short window; otherwise the normal cooldown occurs.

[OPEN] Timing, range, damage, cooldown, “new section,” collision, and counterplay remain unsettled.

### 9.3.4 Ultimate — Sinkhole

Mole senses and targets an unstable natural-terrain region and incites a delayed collapse. Natural terrain only; collapse occurs downward in stages; destroyed terrain produces only partial drops; the area becomes No-Build while actively collapsing; a permanent jagged sinkhole remains. Player-authored construction survives rather than being indiscriminately erased. No ultimate upgrade branch is specified. [OPEN] Delay, staging cadence, region size, instability criteria, drop fraction, No-Build duration, and counterplay remain unresolved.

Extraction/route creation, information, bruiser/engage, and control are possible build directions, not fixed subclasses. Mining Outposts should leave enough approach and excavation work for Mole to excel while remaining usable by generic players (§7.5).

## 9.4 Gardener

**Conceptually settled. Sources: [C §5], [WC].** Everyone else sees vegetation primarily as things to harvest. Gardener sees a landscape of productive sources at different stages of development and is constantly deciding which ones are worth harvesting, preserving, or reinvesting into. Primary archetype Development; secondary archetypes deliberately unassigned pending descriptive evaluation. [HISTORICAL] The earlier Spread Vines / Flowerpot / Glistening Greenhouse kit, the repeated-planting passive, and the Backyard / Exotic / Generalist tendencies are superseded by the Plant Material and Cultivar model below.

Plant Material is a stacking proc/meter, not an inventory item; at threshold it is automatically consumed to grow a Cultivar nearby. A Cultivar is a special developed plant created through Gardener's class systems, and Cultivars accelerate adjacent plant growth.

### 9.4.1 Passive — Plant Material

Accumulate Plant Material. At threshold it is automatically consumed to grow the currently available Cultivar nearby, and Cultivars accelerate adjacent plant growth. [OPEN] Threshold value, accumulation rates, Cultivar placement, persistence, stacking limits, and enemy counterplay remain undefined.

### 9.4.2 Ability 1 — Clip

Clip a living plant without destroying or resetting it to generate Plant Material. Growing crops are eligible; mature plants and crops provide substantially greater value. Branches are Maturity / Specimen (rewards individually mature, high-value specimens), Proliferation / Population (rewards clipping where many plants of the same family are locally established), and Diversity / Collection (rewards varied species and families, with repeated clipping of the same kinds becoming less valuable).

[OPEN] Branch names are conceptual labels, not finalized titles. Values, eligibility, and scaling need specification.

### 9.4.3 Ability 2 — Cultivate

Consume eligible real plant items from inventory to generate Plant Material. This is less efficient than Clip but converts harvested plant inventory back into development. The chosen branch determines Gardener's normal Cultivar: Torchflower (protection and sanctuary, light and Absorption-related value), Sweet Berry Bush (fortification and sustain, provisioning value and a hostile thicket effect), or Giant Bamboo (expansive Development, whose original produces empowered bamboo offspring that themselves project growth acceleration).

Empowered offspring do not recursively create further empowered generations without limit; no exponential recursion. [OPEN] Conversion efficiency, eligible plant items, Cultivar effect magnitudes, and the offspring generation cap remain unresolved.

### 9.4.4 Ultimate — Grafter's Handbook

At the appropriate progression point, currently Level 15 in design discussion, activation allows Gardener to grow both of the other upgraded Cultivar forms sequentially, one at a time. This is not a respec or cycling system; it represents mastery over the other Cultivar forms. [HISTORICAL] Glistening Greenhouse is superseded. [OPEN] Unlock level, sequencing, duration, and whether granted Cultivars persist after the ultimate ends remain unresolved.

Gardener must function without a Construct, Construction teammate, or predefined Development zone. World plants, crops, water, farmland, and ecological variety should matter alongside ability-created vegetation. Synergy with recognized infrastructure does not become a prerequisite.

## 9.5 Golem Master

**Working. Source: [C §6].** Golem Master converts terrain into golems, then uses golem labor and carried materials to construct useful structures. Animate and Assemble forms are independently selected, supporting mixed builds.

### 9.5.1 Passive and Animate

The unnamed passive periodically produces nearby pumpkins and increases command capacity with level. Animate consumes targeted terrain at a tool-dependent rate. After enough material has been gathered, reactivation consumes a Pumpkin and forms a temporary golem. Amount and relevant vanilla block properties—potentially hardness, explosion resistance, and preferred tool—inform its properties.

Iron is more expensive/less efficient to form, with extra Health and attack knockback. Snow uses slowing ranged projectiles. Copper is cheaper/more efficient, smaller and faster, can wield different weapons, and is best at intentional block destruction and collection. Other golems can incidentally destroy and collect blocks.

Copper golems should behave as cheap worker drones outside combat and as swarm/harass/scatter units in combat. The core command principle is that the player chooses intent while the golem type determines execution; commands under consideration include Follow, Combat, Work, and Rally/Guard. [OPEN] The Wither Golem ultimate and generic Construction progression retain their existing questions.

### 9.5.2 Assemble

Nearby golems consume actual inventory blocks to build a targeted wall. Speed scales with golem Health and level. Iron Wall is reinforced and grants nearby Resistance, stronger for golems. Snow Wall adds Slowness to projectiles fired from behind/near it, including relevant player and golem attacks. Copper Wall builds faster, gives allied golems Speed, and gives nearby allied players Haste.

### 9.5.3 Ultimate and modularity

Wither Golem creates a temporary golem explicitly non-destructive to blocks. Its behavior and duration are open. Iron, Snow, and Copper Animate choices can combine with any of the three wall choices. These combinations offer defensive, ranged-control, mobile, and workforce uses without becoming fixed subclasses.

[OPEN] Pumpkin rates, command capacity, material thresholds, block-property formulas, golem duration, collected-material handling, wall dimensions, targeting, cost, permanence, and effects are undefined. Class-specific construction must coexist with generic Construct recognition without requiring all Construction classes to use golems or Block Efficiency.

## 9.6 Kitfighter

**Current documented direction. Source: [C §7].** Kitfighter uses crafted equipment and deployable utility as a flexible combat kit. Salvage makes material-to-equipment conversion more efficient; Covered With Diamonds temporarily exceeds the sustainable equipment curve.

### 9.6.1 Passive — Salvage

Crafting equipment using at least three units of a Primary Material grants one material-specific Salvage. Reaching the current threshold consumes that Salvage to recover two units of the same Primary Material.

| Listed level threshold | Salvage required to recover 2 material units |
|---|---:|
| 0 | 6 |
| 8 | 5 |
| 16 | 4 |
| 24 | 3 |

[VERIFY RECENCY] The passive’s table begins at 0 while matches begin at 1. Preserve this as a class-table convention pending clarification, not a global Level 0. Its 0/8/16/24 cadence is class-specific and does not define universal progression.

### 9.6.2 Offhander and Hotswap

Offhander equips a bow in the offhand on right-click. Upgrade alternatives replace it with a crossbow, fishing rod, or Golden Head. Hotswap deploys a short-lived web at the targeted block. Its alternatives are a much longer-lasting Cobweb, Lava, or TNT.

[OPEN] Actual input mapping, resource/ammunition costs, deployment rules, durations, and cooldowns remain undefined. The prototype’s offhand sentinel creates a direct implementation collision with Offhander; the kit must not silently be rewritten around the test harness (§13.4).

### 9.6.3 Covered With Diamonds

Temporarily upgrades armor/tools to Diamond, then reverts them. Current overdrive bonuses are knockback resistance and movement speed, with upgraded escalation including a Notch Apple and an Ender Crystal. It should not inject a permanent free Diamond set into the economy.

[CONFLICT] The newer general skeleton places ultimate unlock at Level 15, while the class’s own section says its exact unlock remains unresolved. Treat 15 as the general schedule and Kitfighter’s assignment as needing confirmation. Do not infer Level 16 from the older vanilla-capability landmark.

[OPEN] Duration, scaling, enchantments, durability, prior equipment, material definitions, and crafting-validation edge cases remain unresolved. Production is expressed through Salvage, but Production as a whole is not a refund mechanic.

## 9.7 Merchant and Lightfooted

**Working noncanonical drafts; [NEEDS CANONICAL OWNER]. Source: [J], exported 8 September 2026 at 06:08:22 UTC.** These are developed context absent from root classes.md, not additional finalized roster entries.

[HISTORICAL] This earlier Merchant draft is **superseded** by the active design in §9.11. It framed the hook as discovering, developing, exploiting and connecting villages and economic locations, with a Star Trading passive granting bonus XP as villagers advanced through trade levels, and a Silk Road ultimate granting speed along established routes and transmitting allied enchantments and potion effects along the route. The name Silk Road survives as an alternative title for Procession; the route-effect-transmission mechanic does not.

Lightfooted is developed further in §9.12. Its hook uses animal properties for stealthy/quick traversal with combat and animal-propagation applications. The export retains placeholders for the passive, both abilities, upgrades, and ultimate. Those placeholders are missing design, not rules to be completed through invention. [VERIFY RECENCY] A later full kit, if one exists outside the retrieved material, needs reconciliation before promotion.

## 9.8 New concepts and the Waxer branch

**Working concepts; [NEEDS CANONICAL OWNER]. Sources: [CC], [CB].** Recent concept development explores coherent rule-based identities and explicitly distinguishes surviving premises from complete kits.

| Concept | Preserved direction; not an authored full kit |
|---|---|
| Kiln | Production creates persistent heat |
| Paver | Prepare loose construction, then commit/set it; powder and water create territorial reactions |
| Amber / Wax / Waxer | Preserve state, absorb change, and explore hostile sealing |
| Composter | Convert accumulated Development into immediate tempo |
| Mushroom Assassin | Develop hostile territory into assassination infrastructure |
| Invasive Moss | Development becomes excavation |
| Reinforcer | Existing construction becomes materially harder |
| Shulker | Players/resources become protected contents |
| Avalancher | Make structures require physical support |
| Quarryman | Treat recognizable geography as extractable/movable material |
| Sonar / Sculk Recon | Infer hidden activity through physical vibration |
| Lodestar | Derive Exploration value from dangerous round trips |
| Grafter | Transfer properties between living things |
| Hollow | Apparently solid blocks contain hidden state |
| Elytra Explorer | Temporary ultimate-scale access to aerial Minecraft |
| Camel / Caravan | Logistics embodied in vulnerable moving herds |
| Allay Logistics | Delegate physical retrieval and delivery |

The worksheet branch preserves blank hook/passive/A1/A2/ultimate fields for many survivors. Their existence does not authorize filling them with invented abilities. Great Smelt was discussed as losing to or being absorbed into Kiln; other rejected concepts retain possible useful interactions (§14.5).

Waxer has the most recent detailed exploration. A proposed crafting passive fills otherwise-empty recipe slots with wax to apply a consumable Waxed layer. The later user suggestion frames wax as absorbing eligible change before the object changes or loses durability. A removal/stripping ability and an ultimate involving complete amber stasis are directions, not finished procedures.

The latest assistant proposal, Seal, would preserve a functional block’s state or contents—for example interrupting transfer without destroying a chest or hopper. [OPEN] This is not an accepted universal “wax disables everything” rule. Eligibility, application, protection consumption, visible counterplay, ordinary removal, active abilities, and ultimate method remain undeveloped.

The user explicitly questioned whether global infrastructure breaking rules were needed first. No enemy-block immunity was established. Do not create that global rule merely to make Waxer work. The concept should be evaluated against later shared state-change rules, and its implementation feasibility remains unproven.

## 9.9 Coverage and role vocabulary

**Working analytical framework. Sources: [C], [ORG], [CC].** Combat-role vocabulary includes Assassin, Duelist/Skirmisher, Marksman, Nuker, Hypercarry, Bruiser/Juggernaut, Tank, Vanguard/Initiator, Warden/Peeler, Disruptor, Controller, Trapper/Ambusher, Siege, Support/Enabler, Sustain, Scout/Recon, Summoner/Commander, and Splitpusher/Dismantler. This is a descriptive review vocabulary, not a finalized role assignment matrix.

Canonical world dependencies are strongest for Mole’s geology, Gardener’s plants, Golem Master’s iron/snow/copper/pumpkins, and Kitfighter’s material economy. Review coverage in both directions: resources without meaningful uses and classes vulnerable to resource omission. Aquatic and other biome-oriented class families remain prospective, not justification for arbitrary map buffs.

## 9.10 Construction's material agency

**Working design direction. Sources: [C §2], [WI].** Construction may hold exceptional agency over the acquisition, preparation, recovery, and structural use of specifically construction-oriented materials without becoming generic Extraction or generic Production. Extraction remains exceptional removal, access, acquisition, detection, and excavation of world materials generally. Production remains exceptional transformation of acquired inputs into useful outputs generally. Construction is exceptional creation and manipulation of persistent built world, and may possess class mechanics that improve material handling precisely because the material is destined for structural construction.

The governing principle is bounded on both sides. Construction may outperform Production at a transformation when the output's principal purpose is structural construction, while Production remains superior at transformation as a general capability; and Construction may hold advantages involving construction-specific feedstocks without becoming superior to Extraction at acquisition generally. These are legitimate class-design territories, not universal passive bonuses for every Construction player. Candidate expressions include construction-material yield, bulk preparation, on-site preparation, rapid hardening or conversion, construction recovery and recycling, efficient demolition and rebuilding, and structural material reuse.

This creates a real decision when preferred Construction Blocks are unavailable. Building immediately from ordinary or improvised material brings the structure online sooner and keeps the Construct valid while sacrificing the Construction Block progression premium; spending time obtaining and preparing Construction Blocks delays the structure but is more progression-efficient; coordinating with Extraction, Logistics, or Production lets those archetypes support the material chain while Construction spends more labor actually constructing. The correct choice should vary with strategic pressure rather than having a universally optimal answer. Construction Blocks are defined in §5.10.

## 9.11 Merchant — active design

**ACTIVE DESIGN — not settled.** Merchant is the current active class-design subject. Structure below is recorded at the confidence level it actually holds: strong current structure, [FAINT CAUTION] for mechanics worth preserving but not conceptually settled, and [OPEN] for unresolved implementation, balance, or system questions.

Emergent archetype: **Production**. Archetypes are descriptive, not generative — do not assign Logistics, Exploration, or others until the finished behaviour is evaluated.

**Core fantasy.** A fabulously wealthy travelling benefactor and opportunist whose economic success becomes spectacle. The presentation is fairytale and lavish: showering villagers with money, gaudy fanfare, pomp, patronage, prosperity, an entourage, procession, finery, and royal treatment.

Merchant is feast-or-famine, potentially enormous in both economic and combat utility, and an aloof, unserious, largely self-serving aristocrat. Merchant helps the team because the team happens to benefit from Merchant getting disgustingly rich.

Progression fantasy: a poor Merchant still has to get a job — farm, cut wood, gather requested resources, walk normally, personally craft. A successful Merchant increasingly escapes ordinary labour through capital and employees.

**Economic model.** **Reputation** determines commercial relationship and willingness to work. Hiring should specifically require sufficient Trading Reputation / commercial relationship rather than merely any positive vanilla Reputation, and continued willingness to Work may depend on sufficiently positive overall Reputation. This lets curing and gratitude differ from actually establishing a business relationship, and means Swindle can eventually damage an employee relationship. [OPEN] Exact thresholds and vanilla-gossip implementation.

**Mastery** is the villager's vanilla-style progression: Novice, Apprentice, Journeyman, Expert, Master. Mastery determines productive capability and later Procession strength. Work itself advances Mastery.

**Payroll** is the Emerald cost/obligation of employed villagers. It is not a new currency.

**Employment**: sufficient commercial Reputation makes a villager willing to Work; Merchant then hires them onto Payroll. Do not automatically force every eligible villager onto Payroll. [OPEN] Firing/rehiring anti-exploit behaviour.

**Retinue** is the subset of employed workers physically mobilized around Merchant. Workforce is not the same as Retinue.

**Passive — Work.** Villagers with sufficient commercial Trading Reputation toward Merchant become willing to Work for Emeralds. Merchant may employ willing villagers, placing them on Payroll. Performing Work advances Mastery, and higher Mastery makes Work more capable and materially efficient.

Work is a **Production** system: resources plus Emerald labour cost produce products. Merchant supplies the actual resources; workers do **not** generate missing raw resources.

Work is **not** profession-specific. Villager professions remain important to their ordinary vanilla trading relationships, but Work itself should use a universal paid-production abstraction.

Preferred interaction model: avoid a bespoke Merchant production UI or specialized input family if possible. Work should occur periodically and automatically from eligible supplied materials. Some RNG is acceptable and may be desirable. Available materials constrain the possible output pool — Primary Materials, Construction Blocks, and secondary materials — and the worker selects a valid vanilla-grounded transformation within their Mastery and Production Reach.

Initial obvious output families are equipment and processed/construction products. Do not permanently define Work as only tools and blocks; specialized products may later expand the vocabulary. Use vanilla recipes and transformations as the authoritative semantic vocabulary rather than mathematically inferring every possible product from arbitrary material categories.

Output RNG principle: uncertainty may determine **which** useful valid production occurs, while the system remains legible through the materials Merchant supplies.

**Mastery and the production curve.** Work is **not** required to be economically superior at low Mastery. Poor employees may genuinely lose Merchant material. Mastery can provide a continuous production-efficiency curve rather than requiring a new mechanic at every tier.

| Mastery | Approximate conceptual behaviour |
| --- | --- |
| Novice | Potentially actively wasteful; may consume more material than equivalent ordinary crafting |
| Apprentice | Likely the realistic floor of Merchant employment, because the trading needed to establish a commercial relationship may already advance many villagers past Novice. Still inefficient or near-ordinary |
| Journeyman | Production becomes genuinely efficient |
| Expert | Meaningful preserved inputs, leftovers, or surplus |
| Master | Exceptional material utilization: primary craft, useful leftover material, and potentially additional secondary products from the same budget |

[OPEN] Tier breakpoints are not finalized. Do not spend equal design budget forcing Novice to matter; Novice employment may be a legitimate but uncommon edge case.

Mastery has two related dimensions. **Production Reach** is what transformations the worker can perform; **Production Efficiency** is how effectively they convert the supplied material budget. Production Reach should represent productive sophistication, depth, and complexity — not simply Novice equals wood and Master equals diamond. [OPEN] The exact Production Reach model.

**Emerald substitution.** [FAINT CAUTION] Merchant may substitute an intentionally **exorbitant** quantity of Emeralds for some or all goods requested by a villager in a trade.

A poor Merchant must satisfy trade requests normally. An extremely rich Merchant increasingly decides whether acquiring a requested material is worth their time or whether to throw money at the problem. The exchange rate must be intentionally bad, and this should not make trading obsolete — trading remains important for commercial relationships and Reputation, villager progression, liquidity, Swindle opportunities, and workforce development.

Emerald substitution competes with Payroll for the same liquid capital. [OPEN] Placement in the passive or another system is unresolved.

**Ability 1 — Swindle.** After trading with a villager, Merchant can strike them to Swindle them, converting or sacrificing some Reputation for bonus Emerald value from the completed trade. Once per villager per restock cycle; the villager is marked Swindled until a successful restock.

The economic tension is that Emeralds are liquid capital and Reputation is social/commercial capital: Merchant can finance their economy by damaging the relationships that allow the economy to exist.

**Invisible Hand** — strong, near-settled. Swindling one eligible villager also Swindles other qualifying nearby villagers without physically hitting them, extracting bonus Emerald value from each while concentrating Reputation loss onto the struck villager. Nearby villagers must independently be eligible, traded, and not already Swindled.

**Credit Line** — [FAINT CAUTION] Commercial leverage. The intended question is whether Merchant can consume future relationship value now for immediate liquidity and convert that liquidity into enough acceleration before the exhausted relationship matters again. Exact mechanic **not locked**.

**Community Chest** — [FAINT CAUTION] Economic circulation and distributed value. The intended question is whether Merchant can keep enough value circulating through Work and commerce that redistribution beats retaining personal liquidity. Exact mechanic **not locked**.

Branch principle: every branch should be an economically irresponsible business model that becomes brilliant if Merchant correctly understands the economy they built. The failure state is "I built my business model around an economy that doesn't exist."

**Ability 2 — Retinue.** Nearby employed workers respond to Merchant and accompany or follow them. Retinue mobilizes the existing workforce; it does not independently create employees.

Because Work has evolved away from profession-specific resource gathering, any older Retinue text assuming workers simply gather profession-specific materials should be re-evaluated.

**Seize the Means** — [FAINT CAUTION / STRONG] Retinue sacrifices or pauses ordinary productive Work to target a designated area and **disable** enemy workstations and infrastructure — disable, not necessarily destroy. This naturally creates Splitpusher, Dismantler, Siege, and Disruptor behaviour if retained.

**Toll Patrol** — [FAINT CAUTION / STRONG] Retinue patrols and operates around Merchant in contested space. Ordinary productive Work is sacrificed or paused. Enemy presence and activity offset Payroll rather than directly generating arbitrary Emeralds.

**Overtime** — [FAINT CAUTION] Pay more or intensify labour to accelerate ordinary Work and/or Mastery. This is the **weakest** current branch concept and still needs to prove it creates a distinct playstyle rather than merely numbers going up.

**Ultimate — Procession / Silk Road.** Merchant begins a moving procession in a chosen direction or toward a destination with nearby Retinue workers. Merchant leads by remaining within the procession radius and continuing to finance the workers. If Merchant abandons the procession or cannot meet its Emerald/Payroll requirement, the ultimate ends.

Do **not** force Supply Line or Route clauses into the ability; infrastructure interaction should occur naturally through generic WAMS rules where appropriate.

Retinue size and Mastery directly determine power. **Retinue size** determines Work proc frequency and cadence. **Mastery** determines the cumulative Procession Work package:

| Mastery | Cumulative package |
| --- | --- |
| Novice | I |
| Apprentice | I + II |
| Journeyman | I + II + III |
| Expert | I–IV |
| Master | I–V |

This cumulative structure is intentional. Current candidate effects are I Saturation, II Speed, III Regeneration, IV Absorption, V Instant Damage AoE.

[FAINT CAUTION] The exact effect lineup is not settled. Instant Damage is not conceptually forbidden; the primary balance concern is proc frequency with many Master workers.

Procession should probably represent **maximum expenditure** rather than discounted Work: workers stop running Merchant's ordinary economy and redirect their productive capacity into the procession. The fed-state payoff scales accordingly — a small low-Mastery workforce gives a modest procession, a large low-Mastery workforce frequent weaker Work, a small elite workforce infrequent but powerful Work, and a large Master workforce a deliberately obscene feast-state payoff.

## 9.12 Unsettled class drafts

Preserved as drafts, not settled classes. Missing slots are missing design, not gaps to fill by invention.

**Skeleton Crew.** **Draft — not settled.** Likely emergent archetypes are Logistics and Combat, but these should be re-evaluated after completion.

Core rule: hostile undead become logistical labour.

**Passive — Undead Affinity.** Zombies and Skeletons become compatible with the class and largely non-hostile, per the current draft.

**Ability 1.** An empowered interaction or attack against Zombies and Skeletons banks the mob. Arise raises the banked crew. Raised crew can be sent toward a destination, physically pick up dropped items, use actual inventories and cargo, and be redirected; killing or dismissing them spills cargo.

**Ability 2.** Set crew ablaze for dramatically increased speed and combat effectiveness, at the cost of continuous HP loss and risk of cargo spill.

**Ultimate — Getting A-Head.** A Headless Horseman leads the horde. An alternative under consideration has the player ride the horse while the Horseman independently fights.

Missing conceptual slots: A1 branches, A2 branches, exact command structure, Horseman resolution, and possible simplification of A1 complexity.

**Waxer.** **Draft — not settled.** Primary Production; Combat currently plausible.

Core rule: Honeycomb can be repeatedly invested into existing products to preserve them against change; enough preservation becomes obstruction.

**Passive — Waxed Recipes.** Fill otherwise-empty crafting slots with Honeycomb to produce a Waxed version or output.

**Ability 1 — Wax-On.** Apply Waxed stacks to existing objects. Enough Wax on interactive blocks makes them Sealed; Sealed blocks cannot normally change or use their interactive state. The first Wax application to damaged unwaxed equipment restores durability. Branch concepts: **Amber** (faster or stronger sealing), **Restorative** (stronger initial restoration), **Sticky** (interaction with Waxed armour can punish attackers, for example Mining Fatigue while consuming Wax).

**Ability 2 — Wax-Off.** A thrown Honey Solution. The base ability needs to provide a repeatable route to Honeycomb acquisition. Branch seeds — **Enzymatic**, **Preserving**, **Floral** — are not equally developed and remain draft.

**Ultimate — Amber.** Complete preservation and stasis; delivery mechanism unresolved.

[OPEN] The earlier Seal proposal, eligibility, application, protection consumption, visible counterplay, ordinary removal, and implementation feasibility remain undeveloped. Do not create a global enemy-block-immunity rule merely to make Waxer work.

**Lightfooted.** **Draft — substantially incomplete.** Primary Exploration currently plausible; Combat and Development consequences possible but not finalized.

Core area: animal traversal, animal following, and movement/combat interactions.

**Passive — Rabbit's Foot.** Massively reduced fall damage. Lured or following animals share movement and following benefits.

**Ability 1 — Lunge.** Leap toward the cursor and deal damage. Known branches: **Swarming Bite** (wolf count affects or reduces cooldown) and **Thieving Swipe** (charge recovery / offhand disruption concept).

Missing conceptual slots: the third A1 branch, Ability 2 entirely, the A2 branch tree, and the ultimate. These gaps are missing design, not invitations to invent.

**Daredevil.** **Draft — substantially incomplete.** Primary Exploration currently plausible; Extraction and Combat only tentative consequences.

**Passive — Skydiver.** Remaining airborne for more than one second grants extreme Speed while airborne. Traversal and routes created while Skydiving interact with this movement concept according to existing global infrastructure rules.

**Ability 1 — Runway.** Requires speed and momentum. Converts forward momentum into forward or upward traversal and updraft, and negates fall damage. Branches: **Cannon Jump** (TNT-assisted boost), **Trampoline** (slime-based impacts and bouncing), **Mach Headbutt** (high-speed knockback collision).

Missing conceptual slots: Ability 2 entirely, the A2 branch tree, and the ultimate. Do not invent them.

# 10. System Interactions

## 10.1 Reading the game through dependencies

**Synthesis of [C], [M], [O], [W], and [N].** The following interactions explain existing design relationships. They do not add prerequisites or numerical effects. A system can be independently useful while becoming more valuable in combination.

| Interaction | Consequence | Rule owner / cross-reference |
|---|---|---|
| Food × terrain × Inventory | Provisioning and return haul change operational distance | §4.8 and §5.5 |
| Efficiency × Mole Tunneling | Multiplication can compress excavation time excessively | §5.6 and §9.3 |
| Yield × deposits × XP | More acquired resources can accelerate both material and level economies | §5.2 and §5.7 |
| Construct × other infrastructure | Connection modifies legitimate local XP without passive generation | §6.6 |
| Logistics × Industrial Enchanter | Delivered flow establishes a finite Production budget | §7.6 |
| Construction × Mining Outpost | Apparatus brings physical extraction opportunity into existence | §7.5 |
| KeepInventory × durability | Activity-driven wear may create recurring equipment demand | §5.8 |
| Night × infrastructure × Combat | Temporary vulnerability makes contest relatively attractive | §3.5 and §8.3 |
| Objective destruction × terrain abilities | Persistent geography must coexist with meaningful defeat validation | §7.3 and §8.4 |

## 10.2 Opportunity creation versus exploitation

Mining Outpost and Industrial Enchanter demonstrate different gates. One creates a deposit that did not exist; the other banks fuel through actual delivery. Extraction and Production then exploit finite opportunities. Their support roles should not be generalized into “Extraction requires Construction” or “Production requires Logistics.”

World opportunities also need not have formal Worksite status. A discovered cave, ordinary herd, village, or useful crossing can justify investment. Infrastructure formalizes selected work; it does not make unrecognized work worthless.

## 10.3 Dispersion versus concentration

Distributed opportunities encourage players to leave one another and specialize locally. Integrated centers reward deliberately bringing useful activity together. Routes and Supply Lines reduce the cost of connecting distant work; Constructs and Development organize places.

This creates a strategic choice about where to concentrate rather than a rule that all efficient play occurs at the home base. A forward bridge can be useful with a Construct and Route even if it never gains Development. [OPEN] XP eligibility and creator attribution must preserve that distinction without rewarding empty integration.

## 10.4 Resource sinks, progression, and finite opportunity

Materials can be spent on equipment, infrastructure, and objective development. Diamond competes with other investments. If durability turnover is adopted, replacement and repair add activity-driven demand. A finite enchanting opportunity makes preparation and allocation consequential.

[OPEN] Salvage, Yield, generic task effects, enchanting, and potential durability changes may multiply economic returns. No final stacking order or duplication-prevention policy exists. Test the combined chain instead of validating each bonus only in isolation.

## 10.5 Specialist advantage without compulsory composition

Everyone can mine generated ore; Mole excels at access and excavation. Everyone can transport Silo cargo; Logistics specialists may improve flow. Everyone can build useful geometry; authored recognition grants exceptional Construct benefits. Gardener functions outside recognized Development infrastructure.

These examples define the composition principle. [OPEN] Team size, class coverage, XP credit, and infrastructure eligibility must be tested together to determine whether some supposedly optional specialist becomes practically compulsory.

## 10.6 Cross-system conflict checkpoints

The most consequential unsettled interfaces are effective Hunger versus regeneration and Route discounts; Offhander versus the input sentinel; KeepInventory versus timed cargo and death transport; functional-block sealing versus shared storage behavior; and terrain-changing abilities versus protected objectives. Each requires an explicit shared contract. Individual class or prototype decisions cannot silently own the whole interface.

# 11. Terminology

## 11.1 Registry and authority

**Sources: [O §1], [C], [M], [T], [W].** Stable terms should distinguish recognized objects, activities, and states. Ordinary Minecraft terms do not all need replacements. The terminology chat is useful context but retains an obsolete Construct-as-XP-aura definition; the canonical integration distinction takes precedence.

| Term | Meaning in this manuscript | Status / note |
|---|---|---|
| Archetype | Domain of exceptional Minecraft-system manipulation | Established; seven domains |
| Strategic role | How capability becomes useful team contribution | Working vocabulary; separate from archetype |
| Efficiency | Useful work for less cost/time/risk | Established |
| Potency | More, stronger, larger, or exceptional capability | Established |
| Key Location | Place where strategic opportunity exists | Established objective vocabulary |
| Objective | Accomplishment available at a Key Location | Established |
| Contribution | Individual action toward an Objective | Established |
| Worksite | Finite exceptional opportunity developed and exploited through Minecraft activity | Working specifics; not automatically infrastructure |
| Core | Phase-expensive central component of Mining Outpost apparatus | Working |
| Functional multiblock | Required recognizable topology of that apparatus | Working |
| Facility | Flexible supporting player-authored environment | Working |
| Infrastructure Recognition | System recognition/empowerment beyond ordinary activity | Established distinction |
| Designation | Player process identifying world state for recognition | Working process |
| Construct | Recognized built world state with an intended occupation/sustainment benefit | Working mechanics; no standalone XP aura |
| Development Zone | Designated developmental resources and their enabling conditions | Working name; eligibility and rates Open |
| Route | Repeated-traversal connection; map and player-recognition contexts differ | [VERIFY RECENCY] Root/spec ontology issue |
| Starter Route | Provided opening infrastructure before Wilderness handoff | Working later map-spec term |
| Supply Line | Directional container relationship rated through cargo-transport proof | Working; formula and validation details Open |
| Supply Chain | Separate unresolved objective/world concept | [OPEN]; not synonymous with Supply Line |
| Integration | Additional value from connecting distinct infrastructure systems | Working |
| T1 / T2 | Strength of infrastructure integration contribution | Not automatic level labels |
| XP band | Range of levels with a working requirement category | Not an infrastructure tier |
| Effective Hunger | Progression/endurance stat, including values above visible food limit | Working formula in §5.5 |
| Yield | Fortune + Looting task domain | Working |
| Primary Material | Recipe material eligible for Kitfighter Salvage | Definition incomplete |
| Salvage | Material-specific Kitfighter crafting credit | Class-specific |
| Industrial Enchanter | Production Worksite fueled by successful Logistics | Working name and design |
| Lapis Silo | Child timed cargo source for the Enchanter | Working name; count/layout open |
| Encumbered with Fuel | Proposed presentation of instrumental Barrier cargo | Prototype-Test |

## 11.2 Emerging terms and protected distinctions

Buildable Scale and Operational Scale are distinct working progression dimensions. Operational Area defines legal infrastructure connection eligibility, not a general buff radius. Infrastructure Mode is the shared recognition interface; Transit Time and Flow Weight rate demonstrated logistical performance, with ordinary player Flow Weight 1. Extent/relation and constitutive/facilitative define the four infrastructure identities (§6.1.1). These working terms do not specify a final UI. Occupation, occupational, and sustainment efficiency refer to the unresolved intrinsic Construct benefit. Herds, Crop Patches, and Mob Swarms name unfinished world-population concepts; they do not carry a complete spawn or reward contract.

“Development choices” should not be the generic term for progression choices because Development is an archetype. “Support” remains a role. “Extraction” is useful removal/access/acquisition, not arbitrary block destruction. “Production” is transformation, not universally Salvage. A “Route” is not automatically a Supply Line, a road, or a speed lane.

Primary Materials and Construction Blocks are deliberately restricted categories, not exhaustive material taxonomies; exclusion does not imply low economic value. Infrastructure Labor names committed player-time and is distinct from material supply. A Control Window is a period of usable access won through play, not a clock phase. Payback Horizon names the shrinking time an investment has to repay its establishment cost. Authorship, Recognition, and Control are three separate questions and should not be collapsed into a single notion of ownership.

Structural Integrity names the current preferred intrinsic Construct benefit: added resistance to hostile destruction for qualifying Construction Blocks and narrowly defined infrastructure-relevant components. Occupation and sustainment efficiency are retained as [HISTORICAL] framings of that same unresolved slot, not as a competing current answer. Flow Weight is authored by the transport method and sets items per pulse; Item Rate is derived from demonstrated Transit Time and sets pulse frequency; the earlier Flow Rate formulation is superseded by that pair. Origin is observed evidence; Destination is strategically intentional.

## 11.3 Status and concept-ledger tags

The manuscript’s Established / Working / Prototype-Test / Historical-Superseded / Open labels describe design maturity. Concept-ledger tags describe strengths, missing pieces, or risks and are not substitutes for those statuses.

The detailed tag families are preserved with the concept-development record in §14.5. Strength, development need, design risk, and disposition should remain separate dimensions; a strong ultimate does not by itself establish a complete class.

# 12. Balance and Validation

## 12.1 What validation must establish

**Working principles. Sources: [M], [C], [O], [S3].** Balance is functional: players with different specialties should have consequential work, teams should have reasonable opportunity portfolios, and ordinary Minecraft capability should remain relevant. Numerical symmetry alone cannot establish this.

The following is a proposed validation agenda derived from explicit unresolved dependencies. It is not a report of tests performed for this manuscript. Existing implementation evidence is separately identified in Chapter 13.

## 12.2 Opening and progression tests

Measure Fountain-to-first-resource travel, usable loadouts at six slots, early lethality at 9 Health, Hunger/saturation behavior, first capacity choices, and meaningful work per player. Compare plausible 5v5 and 7v7 compositions for simultaneous useful jobs, forced hybrids, readability, and unused systemic domains.

Calibrate Bootstrap XP from actual activity before choosing final requirements. Measure legitimate throughput through Efficiency, Yield, task choices, capacities, infrastructure, and richer opportunities. Track time spent progressing and the practical new options each breakpoint enables. Verify that later requirements do not simply cancel every acceleration reward.

## 12.3 Geography and expedition tests

Test each Starter Route from each Fountain at player scale. Record legibility, construction burden, unsupported continuation, return travel, water depth, cliffs, vegetation, and alternative approaches. Test central interaction and lateral connections without relying on overlays to navigate.

Evaluate complete expeditions: provision, travel, locate, work, potentially fight, haul, and return. Compare route and off-route journeys, capacity choices, mounts, food availability, and the Level 16 mobility proposal. Preserve the distinction between analytical effective distance and observed player performance.

## 12.4 Infrastructure and economic tests

Test independent usefulness before integration. A wall, bridge, remote Construct, standalone Development area, Route, and Supply Line should each have a reason to exist. Test retrospective recognition, destruction, relocation, overlapping areas, empty productive sites, source exhaustion, and interrupted connections.

For integration, measure whether the modest target bonuses reward meaningful concentration without making remote activity irrational. Test duplicate-system counting, mixed tiers, attribution, and exploit loops. For Supply Lines, test item conservation, destination availability, filtering, interruption, and restoration under the eventual transfer contract.

## 12.5 Worksite, combat, and equipment tests

Mining Outpost tests must demonstrate no preexisting exceptional ore, real generation and excavation, persistence after apparatus disruption, finite value, and generic participation with specialist advantage. Industrial Enchanter tests must measure gradual output, batching, proportional delivery, varied transport methods, hostile interference, and finite useful Production.

Test night as a meaningful choice rather than mandatory combat or disabled infrastructure. Test siege methods for distinct costs and consequences. Validate that neutral advancement neither replaces structures nor becomes irrelevant.

If equipment turnover is adopted, measure lifetimes in actual activity by equipment category, material demand, repair frequency, inventory burden, and Production relevance. Assess Diamond availability and the combined effect of Salvage, Yield, enchanting, and durability.

## 12.6 Readiness and unresolved decisions

| Priority interface | Evidence needed before treating it as settled |
|---|---|
| Match resolution | Explicit structure/Fountain/victory state transitions |
| Economy | XP sources, attribution, duplication rules, measured yields |
| Capacity | Reconciled schedule and functional effective-Hunger implementation |
| Infrastructure | Recognition, intrinsic benefits, boundaries, transfer, disruption |
| World population | Spawn, renewal, scaling, depletion, night relationship |
| Map skeleton | Complete player-scale B.1 validation and accepted B.2 outcome |
| Class runtime | Ability inputs compatible with kits and reliable world interaction |

These are design/readiness dependencies, not reasons to declare the existing work invalid. A successful bounded prototype answers its own question; it does not automatically pass the entire game.

[OPEN] Further readiness dependencies identified on 10 September: the Construction Block XP premium and its interaction with anti-farming rules; the Construct intrinsic benefit and its relationship to recognized scale and Operational Area; the conditions under which a captured Construct's intrinsic benefit applies to an occupier; team-relative Route re-proof, Development Zone capture behaviour, and Supply Line re-establishment; Construct scale thresholds pending the labor and control-window modelling; late-game Construction mechanics that compress labor requirements; mid- and late-objective design that deliberately creates infrastructure development windows; and whether any formal territorial-control mechanic is needed at all, as against functional access emerging from physical play.

[OPEN] Readiness dependencies added on 11 September: Structural Integrity strength and whether mining, explosions and abilities are affected differently; a narrow definition of infrastructure-relevant block; Structural Integrity behaviour when practical control changes; the exact input for authoring a logistical corridor; qualifying cargo movement; derivation of pulse interval from Transit Time; authored Flow Weight values and whether they vary by cargo, carrier or upgrade; branching and rerouting; permitted carrier deviation from the authored corridor; failure to resolve; obstruction after establishment; interaction with Routes; capture and re-establishment; whether engineered transport needs any balancing disadvantage; long-distance destination targeting; and datapack feasibility for entity navigation.

# 13. Technical Design and Feasibility

## 13.1 Implementation maturity and version scope

**Prototype-Test evidence. Sources: [I], [D], [B].** Current implementation includes recovered historical generation, analytical candidate tools, modern world serialization, official-vanilla seed search, physical greybox experiments, and input/progression datapack work. These supersede the root README’s old “design only/no implementation” inventory statement.

World-generation work pins Minecraft Java 1.21.11 and Java 21 for its recorded builds. The input prototype describes Java 1.21.9+ with pack_format 88 and testing on 1.21.9. [VERIFY RECENCY] These are milestone-specific environments, not proof of one validated final game target across every component. The root map document’s unresolved Java-version note is historical relative to the specific worldgen milestone, but a unified production compatibility contract still needs ownership.

## 13.2 World-generation architecture

**Recorded implementation. Source: [I].** Earlier historical code used custom terrain and Java 1.12-era numeric block IDs. Successor tools separated terrain, ecology, resource-instance derivation, Routes, analysis, and validation. A modern serializer wrote Anvil/NBT worlds for 1.21.11 from analytical candidates.

The later preferred search uses official Minecraft generation for actual chunks, reads heightmaps/biomes/blocks/fluids/structures, and evaluates candidate bounds and logical orientation. Rotation/reflection is analytical interpretation; the world’s chunks are not rotated. A candidate is seed + bounds + orientation, not merely a seed.

The staged search uses a pinned Cubiomes biome proxy for cheap filtering and official generation for selected survivors. [PROTOTYPE] Cubiomes’ version enumeration does not explicitly guarantee 1.21.11; the repository documents it as a proxy. It must not substitute for block-level validation. The recorded staged run retained eight finalists from eleven full generations after refinement and review exclusions.

The specifications sequence cheap search, structural ranking, official generation, analytical fitting, human selection, physical validation, then gameplay placement. Search-stage A/B/C/D and later Task A/B/C are different labels; do not confuse their completion states.

## 13.3 Current physical map checkpoint

**Recorded results, not retested here. Source: [B], at snapshot fe83bd0.** The user selected 930010639 as primary and 930012642 as comparison/fallback, then accepted the remaining analytical readiness questions. The older blocked-gate report is historical. B.0 is complete for both independent world copies; B.1 is only partially complete.

| Recorded measurement | 930010639 | 930012642 |
|---|---:|---:|
| Planned physical block edits | 1,797 | 1,928 |
| Vegetation/overhead blocks cleared | 259 | 379 |
| Unexpected states at planned edit positions | 0 | 0 |
| Starter spines passing conservative block checks | 5/6 | 6/6 |
| Fountain anchors with dry support and headroom | 2/2 | 2/2 |
| New/removed Overworld chunks | 0/0 | 0/0 |

The primary’s North-2 Starter crosses an eight-block dip and subsequent large rises hidden by coarse sampling. A nearby dry alternative is identified but not applied. The comparison’s Starter checks pass, while unsupported South-3 continuation includes substantial rises and deep water. Neither result establishes a competitive pass or automatic candidate promotion.

Matching dedicated servers loaded both worlds. Normal graphical-client traversal was not completed. The block renders are not in-game screenshots or walk-test evidence. All-chunk preservation audits also retain 26 and 6 unexplained unplanned state differences respectively, separate from expected transient changes; clean source worlds remain untouched.

B.2 corrections have not occurred, no competitive skeleton is frozen, and Task C has not begun. The next evidence is the full B.1 walk and route-specific review, not immediate construction to hide discrepancies. Bulk worlds are local/gitignored; compact reports and reproduction plans are committed.

## 13.4 Ability input prototype

**Prototype-Test, reported tests dated 10 September. Source: [D].** The input experiment repurposes Swap Offhand: F prints A1, Sneak+F prints A2, and Sprint+F begins an approximately two-second ultimate channel. Completion/cancellation and a Quick Actions dialog path are tested. This is an input-feasibility harness, not a finished class implementation.

The repository reports item-preserving sentinel interception and one resolved ability per input, with visible hand jitter at high tapping speed. Sneaking-state predicates work. Attempts to read raw key input failed in the tested shapes; the fallback reads sprinting movement state. Consequently, the ultimate cannot start while stationary, rooted, or submerged in that prototype.

[OPEN] A robust modifier/input design remains unresolved. Do not describe failed predicates as a proof that all possible vanilla approaches are impossible. Kitfighter’s genuine offhand equipment and the sentinel compete for the same space; this conflict needs explicit resolution.

Official Minecraft documentation confirms content-defined dialogs and the Quick Actions entry point as a limited UI mechanism, not a general custom in-game HUD. [V] The successful local dialog test establishes the tested dialog→trigger→function path, not every proposed class or inventory UI.

## 13.5 Progression and capacity prototype

**Prototype-Test. Source: [D: Chunk 2 v7].** The datapack owns MOBA XP and levels in scoreboards and uses the native XP display as output. Capacity is recalculated from authoritative state, allowing reset/respec without repeated mutation drift. The tested universal schedule reproduces the newer Level 16 Inventory and Level 17 Health/Hunger floors.

Flat 100 XP per level is temporary. Excess awards carry across levels; the cap’s display is filled. These behaviors do not implement the final XP bands or define enchanting expenditure. Only the capacity portion of the breakpoint table is implemented; abilities, task rewards, infrastructure, mobility, and capstone remain outside that chunk.

### 13.5.1 Hunger enforcement

The prototype reads food level and applies a high Hunger effect when it exceeds the intended cap. Its documented consequences include a delay after eating, saturation draining at the cap, and a brief effect icon. This is an enforcement workaround, not a native maximum-Hunger attribute.

Effective Hunger above 20 remains stored separately but provides no implemented extra reserve in this version. The newer design’s exhaustion conversion has not been realized by this prototype. An extended visual row alone would not supply the missing endurance mechanic.

### 13.5.2 Inventory and Health

Inventory limitation uses per-tick eviction: contents of unavailable slots are copied to a ground item, successful copying is checked, then the slot is cleared. Locked slots remain visible. This is not a native locked-slot UI and can produce item-management friction.

The prototype excludes armor and offhand. Health is set through the attribute base; another system writing that base could conflict with recalculation. The final Inventory-at-cap replacement and fractional Health/Hunger effects require verification rather than being assumed from the design table.

## 13.6 Exhaustion, cargo, and world-state constraints

**Working design, Open implementation. Sources: [I], [WI].** Infrastructure requires current-state measurement for Constructs/Development Zones and capability proof for Routes/Supply Lines. Transport dependencies must remain physically contestable even when flow is represented rather than rendered item by item. No validation algorithm or performance budget has been proved. Tests must cover node destruction, rail severing, water obstruction, carrier loss, restoration without a new slot, and the distinction between physical invalidity and strategic danger.

**Open feasibility interfaces. Sources: [M: Datapack limitation], [D], [O §14A], [CC].** The Route proposal discounts only locomotion exhaustion. The source documents explicitly treat a datapack estimate/compensation approach as an approximation unless exact control is demonstrated. Do not claim that such a harness literally edits vanilla exhaustion constants.

Barrier cargo needs transport, provenance, cleanup, death, storage, enemy interaction, and resource-pack tests. State-preservation concepts such as Waxer need defined eligible changes and reliable intervention points. Massive terrain movement, persistent block damage, specialized golems, and recognition over large regions remain unproven class/system capabilities rather than guaranteed datapack features.

[OPEN] Performance budgets, scan intervals, loaded-chunk behavior, multiplayer races, persistence/recovery, world reset, compatibility, and observable diagnostics need a shared technical owner. A mod/plugin remains an option if exact behavior is necessary and vanilla proves insufficient; no such platform switch is made by this manuscript.

## 13.7 Technical documentation boundaries

Implementation should record its target version, implemented behavior, approximation, observed tests, and remaining gaps separately. “Can load,” “matches planned block edits,” “walkable,” “competitively viable,” and “complete match” are different claims. Preserve them as such.

The immediate technical priorities follow the current evidence: finish the selected map’s player-scale validation, resolve input/kit compatibility, implement or test effective Hunger’s intended mechanics, and define shared infrastructure/XP contracts before treating isolated prototypes as a full game runtime.

# 14. Development Record

## 14.1 Snapshot, authority, and coverage

This edition uses the GitHub default branch verified at commit **fe83bd039f76302665b45640ddc7263f94a8e073**, dated 10 September 2026, 10:45:15 UTC. The initial manuscript used that commit. This revision integrates the 10 September Infrastructure System handoff into infrastructure.md, classes.md, objectives.md, and this manuscript through an explicit reconciliation record. Unrelated system coverage retains the initial snapshot; this is not a fresh audit of all later implementation work.

Root classes.md, objectives.md, and maps.md retain domain authority; infrastructure.md now owns shared infrastructure recognition, persistence, spatial metrics, and transport proof. Repository reconciliation records explain explicit promotions or supersession. Search/fitting specifications govern their experiments while retaining maps.md’s settled design authority. Chat proposals fill gaps with visible provenance. The September 8 handoff and site export are contextual snapshots, not global overrides.

The available project conversations were inspected, including the complete retrieved World and Match Systems and Nighttime and Combat Economy histories, recent progression, class-concept, class-design, map, organization, and terminology material, and the retrieved XP-curve branch. Some longer histories were sampled through relevant recent and older pages rather than exhaustively transcribed. [VERIFY RECENCY] Unretrieved older class/map details may contain useful context; no unseen attachments or later full kits are claimed as incorporated.

The requested manuscript hierarchy is preserved. Prose, cross-references, consolidated tables, and the validation agenda are editorial organization; they do not promote proposals into settled mechanics. Numerical examples retain the status of their source. The explicit gaps are part of the formal design record.

## 14.2 Development chronology

| Period / milestone | Retained significance | Present interpretation |
|---|---|---|
| P0 | Basic North/South layout, Routes, villages, POIs, mountain/coast placeholders | Historical scale/visibility evidence |
| P1 | Traversal-friction proxy and western-village accessibility | Historical analysis, not current movement validation |
| P2A | Improved Route fan; criticized as flat/conservative | Historical |
| P2B | More verticality and terrain features; unnatural forms and fluids | Historical failure modes |
| P2C | Irregular forests, pools, foothill transitions, preserved Routes | Historical refinement |
| P2D FINAL | Geology and hydrology improvements | Historical reference; “FINAL” does not mean final Default |
| 50-variant experiment | Multiple accepted topology families | Procedural variants, not 50 playable Java saves |
| Early resource audit | First ten of fifty completed; 920260900 performed strongly | Incomplete sample, not canonical seed selection |
| 8 September reconciliation | Root authority and stale-wiki distinctions documented | Inventory statements later superseded by implementation |
| 9–10 September progression edits | Authored infrastructure, XP bands, capacity and Hunger refinements | Current Working material with residual duplicate sections |
| 10 September infrastructure reconciliation | Shared Infrastructure Mode, matrix, Construct metrics, Development Zones, transport proof and physical persistence | Working canon promoted with explicit open parameters |
| 10 September Worksite reconciliation | Generate-then-reveal and Industrial Enchanter integrated | Explicit current objective revision |
| Current Task B | Two minimal physical candidates; B.1 incomplete | No frozen competitive skeleton or Task C |
| Latest systems/concept chats | Durability turnover, Construct scales, Waxer | Working discussion; not silently canonized |

Earlier worlds were programmatically generated Java/Anvil saves, not WorldPainter-authored worlds. The historical dimensions and method do not dictate the modern official-vanilla approach.

10 September 2026, second integration: the World and Match Systems handoff was supplied in full and integrated, adding restricted material categories, the revised block economy and infrastructure-labor model, Construction's material agency, and the payback horizon, control-window, authorship/recognition/control, capture, and team-relative integration material. The numerical estimates and phase bands recorded there are modelling targets, not balance canon.

11 September 2026: a WAMS handoff on infrastructure benefits and Logistics redefinition was integrated. Structural Integrity becomes the current preferred intrinsic Construct benefit, and the Supply Line model is reframed from origin-first endpoint selection to demonstrated destination-oriented delivery. Flow Weight and Item Rate are separated, with the earlier combined Flow Rate formulation superseded. Flow Weight, Transit Time, persistent operation, physical disruption, directionality, and team-relative integration are retained and reconciled rather than replaced.

Late September 2026: a WAMS infrastructure clarification handoff from the Skeleton Crew and Logistics discussion introduced the designation-and-evidence model, the place versus connection organizing frame, archetype-native anchors (Banners, Copper Chests, Construction Blocks, developable resources), Construct block-investment and extent evaluation, Development Weight with dynamic recalculation, the source-to-destination rule for self-directed logistical entities, the distinction between pre-infrastructure physical Logistics and recognized Supply Lines, and direct Copper Chest inventory transfer as the preferred execution model. Four tensions with 10 and 11 September canon were recorded as [CONFLICT] rather than resolved. A subsequent infrastructure reconciliation handoff [WR] then decided them: explicit Copper Chest Source designation is reinstated; Flow Weight belongs to the logistical method while Item Rate emerges from that method's actual movement over the connection, with player-authored Item Rate removed; endpoints suffice only for self-directed carriers, leaving corridors meaningful for path-authored methods; and the class-ability firewall is reaffirmed. Route corridor retention remains [OPEN].

## 14.3 Conflict and recency ledger

| ID | Collision | Treatment in this manuscript |
|---|---|---|
| CR-01 | Root README says no implementation; current folders contain tools, reports, worlds-related plans, and datapacks | README inventory flagged stale; bounded evidence in Chapter 13 |
| CR-02 | classes.md newer §3 completes floors at 16/17; older §9 says about 19 | Newer skeleton shown; older text retained as explicit conflict |
| CR-03 | Older choice rule withholds growth for every choice; newer task choices coexist with growth | Explicit schedule used; no silent generalized rule |
| CR-04 | Inventory-at-cap replacement now +0.5 Health/+0.5 effective Hunger; older §9 leaves replacement open | Newer proposal recorded; implementation/overflow unresolved |
| CR-05 | General ultimate unlock 15; Kitfighter-specific unlock still open | General schedule retained; class assignment flagged |
| CR-06 | maps.md physical authored Routes versus later corridor/Starter ontology | Later implementation scope described; root design reconciliation still needed |
| CR-07 | Map “L1” fixture 10 food versus current class start 9 | Fixture remains Prototype-Test, not universal start |
| CR-08 | Older travel bands and scale target versus later effective-depth fitting calibration | Both identified; neither made final map geometry |
| CR-09 | Old Construct XP aura/submission model versus current integration-only multiplier | Current canonical model governs; old model historical |
| CR-10 | Root gradual Supply Line transfer versus chat recognition of physical flow | Reconciled by [I], [WI]: physical proof, represented ongoing flow, persistent physical dependencies; detailed validation/accounting Open |
| CR-11 | Site/export roster and primary tags versus canonical four-class roster | Noncanonical additions separately documented |
| CR-12 | Hunger surplus design versus datapack’s stored but mechanically inactive surplus | Design formula separated from implementation gap |
| CR-13 | Input sentinel versus Kitfighter offhand kit | Explicit technical collision; kit not rewritten |
| CR-14 | Old blocked Task B gate versus later accepted gate and B.0 completion | Later report governs; B.1 still incomplete |
| CR-15 | Old unresolved Java version versus version-specific modern milestones | Milestone choices recorded; unified runtime target still needs confirmation |
| CR-16 | Durability removal/reduced consumption versus latest shorter-lifecycle discussion | Latest direction Working, exact rule still Open |

## 14.4 Canonical ownership and decision register

The following ownership recommendations are editorial proposals, not newly created canonical files. Existing root content remains authoritative until deliberately moved or reconciled.

| Domain needing ownership | Developed material to preserve | Principal open decisions |
|---|---|---|
| Match and world systems | Team size, KeepInventory, phase model, night economy, durability | Victory contract, respawn, cycle, turnover |
| Universal progression/economy | Capacity schedule, task progression, XP bands, attribution philosophy | Conflicting passages, real XP yields, enchanting relation |
| Infrastructure | Constructs, Development, Routes, Supply Lines, integration | Recognition, intrinsic benefits, areas, transfer, disruption |
| Resource populations | Herds, Crop Patches, Mob Swarms and extensions | Spawn, renewal, depletion, scaling, rewards |
| Terminology | Stable distinctions and new working names | Development noun, Construct scales, cargo naming |
| Technical contract | Version targets, inputs, state ownership, approximations | Runtime compatibility, effective Hunger, performance |
| Expanded roster | Merchant, Lightfooted, surviving concepts, Waxer | Canonical promotion and actual kit authorship |

Priority unresolved decisions include the Fountain-to-victory transition; attribution and duplicate XP prevention; Hunger/Route/regeneration interaction; Construct intrinsic effects and spatial rules; Supply Line transfer versus recognition; Development/population semantics; and completion of the physical map validation. These are visible dependencies rather than concealed assumptions.

## 14.5 Superseded directions and preserved concepts

**Historical-Superseded. Sources: [RC], [RW], [C], [O §22], [M], [CC].** Do not restore the objective-method XP hierarchy, Mining Outpost ore dispenser, pre-minable exceptional Worksite deposit, baseline crops as Supply Chain’s unique reward, generic Forge as the preferred Production Worksite, recognized Supply Line requirement for the Enchanter, or prescribed Logistics technology ladder.

Do not restore a default Route speed bonus, literal full Wilderness mirroring, uniform enlargement of every feature, P2D as final Default, 920260900 as canonical, or the old 1,300 × 1,000 immediate prototype footprint. Do not automatically map archetypes to infrastructure unlocks or T1/T2 to player levels.

Concept rejection need not destroy useful material. The class ledger preserves strengths, risks, donor mechanics, and incomplete hooks. Great Smelt’s relationship to Kiln and the sparse survivor worksheets illustrate that process. Their retention is development history, not roster inclusion.

The concept discussions distinguish SOLID CONCEPT, SOLID RULE, SOLID LOOP, SOLID ULT, SOLID HOOK, SOLID MODE, and SOLID INTERACTION; NEEDS BONES, NEEDS FUN, NEEDS LOOP, NEEDS RULE, NEEDS HOOK, NEEDS MODES, NEEDS PAYOFF, NEEDS COST, NEEDS COUNTERPLAY, and NEEDS VOCABULARY; and targeted implementation, balance, legibility, complexity, performance, griefing, scaling, tempo, or dependency risks. [CC]

Design-smell tags include BENDER RISK, JOB SKIN, MOB COSPLAY, GENERIC MOBA, THREE-MECHANIC, ARCHETYPE LAUNDERING, and NO DECISION INVERSION. Relationship tags such as COLLISION, ABSORB, SYSTEM?, MODE?, ULT DONOR, and MECHANIC DONOR preserve useful material even when a class is rejected. These are editorial review tools, not player-facing game mechanics.

## 14.6 Source register

Repository links below are pinned to the reviewed commit for reproducibility. Bracketed codes throughout the manuscript refer to these entries. Source section names are provided in local notes where the source is large.

**[R] Repository README.** Source hierarchy, thesis, and historical inventory. [Read snapshot](https://github.com/iracarranza/minecraftmoba/blob/fe83bd039f76302665b45640ddc7263f94a8e073/README.md).

**[C] classes.md.** Canonical class reference; current progression skeleton, infrastructure model, XP bands, four class drafts, and residual older progression sections. Its September 8 header does not capture all later edits. [Read snapshot](https://github.com/iracarranza/minecraftmoba/blob/fe83bd039f76302665b45640ddc7263f94a8e073/classes.md).

**[O] objectives.md.** Canonical objectives, contribution vocabulary, Worksites, integration, and unresolved victory rules; current design date September 10. [Read snapshot](https://github.com/iracarranza/minecraftmoba/blob/fe83bd039f76302665b45640ddc7263f94a8e073/objectives.md).

**[M] maps.md.** Canonical Default philosophy, geography, resource vocabulary, prototype parameters, and historical experiments. [Read snapshot](https://github.com/iracarranza/minecraftmoba/blob/fe83bd039f76302665b45640ddc7263f94a8e073/maps.md).

**[RC] Repository reconciliation, 8 September.** Authority and historical/working classification; its inventory predates implementation. [Read record](https://github.com/iracarranza/minecraftmoba/blob/fe83bd039f76302665b45640ddc7263f94a8e073/docs/reconciliation/2026-09-08.md).

**[RW] Worksite reconciliation, 10 September.** Explicit promotion of the Mining Outpost corrections and Industrial Enchanter; associated commit 7b17ca2. [Read record](https://github.com/iracarranza/minecraftmoba/blob/fe83bd039f76302665b45640ddc7263f94a8e073/docs/reconciliation/2026-09-10-worksites.md).

**[S1] MapSeedSearch specification 1.** Staged vanilla search and candidate definition. [Read specification](https://github.com/iracarranza/minecraftmoba/blob/fe83bd039f76302665b45640ddc7263f94a8e073/specs/mapseedsearchspec1.md).

**[S2] MapSeedSearch specification 2.** Task A competitive fitting, Route ontology, Starter handoffs, and effective depth. [Read specification](https://github.com/iracarranza/minecraftmoba/blob/fe83bd039f76302665b45640ddc7263f94a8e073/specs/mapseedsearchspec2.md).

**[S3] MapSeedSearch specification 3.** Refinement, human selection, physical validation, bounded correction, freeze, and gameplay placement. [Read specification](https://github.com/iracarranza/minecraftmoba/blob/fe83bd039f76302665b45640ddc7263f94a8e073/specs/mapseedsearchspec3.md).

**[I] Worldgen implementation README.** Historical recovery, successor generators, serialization, official vanilla search, runtime pins, and limits. [Read implementation record](https://github.com/iracarranza/minecraftmoba/blob/fe83bd039f76302665b45640ddc7263f94a8e073/implementation/worldgen/README.md).

**[B] Task B physical greyboxes report, 10 September.** Current completed/incomplete stages, block checks, unexplained preservation differences, and next validation. [Read report](https://github.com/iracarranza/minecraftmoba/blob/fe83bd039f76302665b45640ddc7263f94a8e073/implementation/worldgen/results/default_task_b_2026-09-10/REPORT.md).

**[D] Datapack README.** Input feasibility and progression v7, reported tests and limitations. [Read prototype record](https://github.com/iracarranza/minecraftmoba/blob/fe83bd039f76302665b45640ddc7263f94a8e073/implementation/datapacks/README.md).

**[H] chathandoff.txt, 8 September.** Retrieved attachment from Levels, Objectives, Progression; contextual snapshot. The current root tree does not contain the README-linked handoff file. It preserves earlier principles and tests but does not override later canonical revisions.

**[J] minecraft-moba-classes (1).json.** Organization Site attachment, class-library version 2, exported 2026-09-08T06:08:22.656Z. Six site records; used for the absent Merchant and Lightfooted drafts with explicit limitations.

**[ORG] Organization Site.** Referenced manuscript hierarchy, provisional timing outline, site provenance, and export discussion. Conversation ID 6a9f0e11-cf54-83ea-a932-78ea4c568eb2. [Open conversation](https://chatgpt.com/c/6a9f0e11-cf54-83ea-a932-78ea4c568eb2).

**[W] World and Match Systems.** Match structure, team-size discussion, KeepInventory, economy, Worksite evolution, durability, and latest Construct recognition/scales. Conversation ID 6aa1edb8-4594-83e9-98cb-fd0870e52b5b; reviewed through 10 September. [Open conversation](https://chatgpt.com/c/6aa1edb8-4594-83e9-98cb-fd0870e52b5b).

**[N] WaMS Branch - Nighttime and Combat Economy.** Integration revisions, night direction, and explicit missing population-system detail. Conversation ID 6aa22fcc-17ac-83ea-88e8-bbe6bcc74ffc. [Open conversation](https://chatgpt.com/c/6aa22fcc-17ac-83ea-88e8-bbe6bcc74ffc).

**[X] WaMS Branch - XP curves.** XP bands and the separation of integration tiers from level rewards. Conversation ID 6aa22adc-6ed8-83ea-a07b-575f5a7afba5. [Open conversation](https://chatgpt.com/c/6aa22adc-6ed8-83ea-a07b-575f5a7afba5).

**[L] Levels, Objectives, Progression.** Capacity, task/enchantment allocation, infrastructure breakpoints, and Hunger revision discussions. Conversation ID 6aa02d6b-7e6c-83ea-acf2-2a715ebe5753. [Open conversation](https://chatgpt.com/c/6aa02d6b-7e6c-83ea-acf2-2a715ebe5753).

**[T] Terminology Systems Comparison.** Registry proposal and vocabulary; its Construct XP definition is stale. Conversation ID 6aa22ce9-292c-83e9-b112-d810f00346c0. [Open conversation](https://chatgpt.com/c/6aa22ce9-292c-83e9-b112-d810f00346c0).

**[CC] Class Design Branch - Concepts.** Concept ledger, surviving rule-based hooks, and Waxer development. Conversation ID 6aa2348f-b550-83ea-ac9a-358f54283e22. [Open conversation](https://chatgpt.com/c/6aa2348f-b550-83ea-ac9a-358f54283e22).

**[CB] Branch · Class Design Branch - Concepts.** Survivor discussion and sparse worksheets. Conversation ID 6aa294fb-822c-83ea-889e-d33b8d7a633c. [Open conversation](https://chatgpt.com/c/6aa294fb-822c-83ea-889e-d33b8d7a633c).

Additional contextual inspection included Class Design (6a9f46d8-837c-83e9-8172-5e31cf296d81), Map Types, Worldgen (6a9f5718-f650-83e9-bbc4-2a38bd5dde7b), Datapack Prompting (6aa0b5d8-f37c-83ea-8b72-267b40de66cc), and Design volleyball MOBA (6a9dd1ff-ad94-83ea-834e-f72b8276a426). Their older summaries were not used to override the pinned roots or later explicit reports.

**[V] Mojang, Minecraft Java Edition 1.21.6 release notes.** External technical reference for dialogs and Quick Actions’ limited UI purpose; consulted 10 September 2026. It establishes the platform feature, not project implementation success. [Read official release notes](https://www.minecraft.net/en-us/article/minecraft-java-edition-1-21-6).

## 14.7 Manuscript maintenance

Future editions should record the new repository commit and chat cutoff, close ledger entries through explicit decisions, and preserve superseded reasoning in the development record. Promote a proposal only when its owner and status are clear. Reconcile repeated passages in canonical sources before relying on a heading’s location or an old file date as proof of recency.

Keep design, implementation, and validation status separate. Update cross-references when moving ownership, and retain the unresolved interfaces until their behavior is specified and tested. This manuscript can then become a maintained design reference rather than another untracked snapshot.


**[I] Infrastructure Design and reconciliation.** Canonical infrastructure.md, introduced in this revision; docs/reconciliation/2026-09-10-infrastructure.md records scope and supersession. [Infrastructure source](https://github.com/iracarranza/minecraftmoba/blob/main/infrastructure.md). This main-branch link identifies the living source; the base source snapshot above remains pinned.

**[WC] Class Documentation Handoff.** 11 September 2026. Settles Mole and Gardener, records Merchant as active design, and preserves Skeleton Crew, Waxer, Lightfooted and Daredevil as drafts. Supersedes the earlier Gardener kit, Mole passive and Tunneling branches, and the Star Trading / route-effect Merchant. See docs/reconciliation/2026-09-11-classes.md.

**[WC] WAMS Infrastructure Clarification Handoff.** Late September 2026, from the Skeleton Crew / Logistics discussion. Introduces the designation-and-evidence model, place/connection framing, archetype-native anchors, and direct Copper Chest inventory transfer. Four tensions with [WB] and [WI] were recorded as [CONFLICT]; all four are decided by [WR]. See docs/reconciliation/2026-09-11-wams-clarification.md.

**[WR] Minecraft MOBA Infrastructure Reconciliation / Correction Handoff.** September 2026. Resolves the four [WC]/[WB] conflicts, reinstates explicit Supply Line start, corrects Flow Weight and Item Rate authorship, splits self-directed from path-authored logistical methods, reaffirms the class-ability firewall, and states the shared start/journey/end connection grammar. Retains [WC] where it is not corrected; does not supersede [WB] wholesale. See docs/reconciliation/2026-09-11-infrastructure-reconciliation.md.

**[WB] WAMS Handoff: Infrastructure Benefits and Logistics / Supply Line Redefinition.** 11 September 2026, working-canon handoff, supplied complete. Supersedes the origin-first Supply Line formulation and selects Structural Integrity as the preferred intrinsic Construct benefit. See docs/reconciliation/2026-09-11-logistics.md.

**[WI] Infrastructure System New Design Handoff.** World and Match Systems, 10 September 2026, working-canon handoff. Retrieved text covers sections 1–19 and begins section 20; the retrieval truncates there. Integration context was independently available in the preceding framework discussion and canonical class/objective text. [VERIFY RECENCY] Any unseen continuation beyond that retrieval is not claimed as incorporated. [Source conversation](https://chatgpt.com/c/6aa1edb8-4594-83e9-98cb-fd0870e52b5b).
