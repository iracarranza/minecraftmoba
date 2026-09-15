# Minecraft MOBA — Objectives

Current design state: September 10, 2026

This document is the authoritative objective-system reference for Minecraft MOBA.

Minecraft already supplies many avenues of progression.

Objectives should concentrate and contest Minecraft play, not replace the sandbox with a collection of bespoke minigames.

---

# 1. Objective Vocabulary

Use the following distinctions consistently.

## Key Location

A place where strategic opportunity exists.

## Objective

An accomplishment available at a Key Location.

## Contribution

An individual action toward accomplishing an Objective.

This prevents every strategically important place from being mislabeled as "an objective."

Broad Key Location categories currently include:

- Team Structures
- Major Encounters
- Worksites
- World Locations
- possible player-established locations

A Key Location may contain multiple possible interactions, contributions, or objectives.

---

# 2. Objective Structure

The current objective world contains several overlapping forms of strategic opportunity rather than requiring one objective for every archetype.

Important categories include:

1. **Team Structures** — persistent enemy structures that create Minecraft-native siege problems.
2. **Major Encounters** — high-stakes neutral combat opportunities.
3. **Worksites** — latent productive opportunities that players capitalize, contest and exploit; Mining Sites deplete while Industrial Factories depend on throughput, inputs and access.
4. **World Locations** — strategically important Minecraft places whose value may derive from resources, geography, infrastructure, inhabitants, or other interactions.
5. **Possible player-established Key Locations** — infrastructure or developed places that may eventually receive systemic recognition.

These categories do not need to correspond one-to-one with the seven archetypes.

Different archetypes and classes can care about the same place for different reasons.

---

# 3. Team Structures

Each team currently has four major defensive structures:

1. **Pillager Outpost**
2. **Nether Bastion**
3. **End Tower**
4. **Aether Fountain**

The first three should support Minecraft-native interaction rather than functioning only as conventional MOBA health bars.

Current broad interaction vocabulary includes:

- structural destruction;
- elimination of defenders;
- objective-specific interaction.

These are strategically different solutions.

They do **not** currently have a universal XP ranking.

In particular, do not use the superseded rule:

> structural destruction < defender elimination < objective-specific interaction

Different methods should instead create different:
- costs;
- risks;
- positioning requirements;
- resource demands;
- tactical consequences;
- strategic consequences.

The objective should recognize what actually happens in the world rather than requiring players to declare an attack method in advance.

Mixed attacks are expected.

---

# 4. Pillager Outpost

The Pillager Outpost is the earliest/currently most accessible defensive structure.

Current interaction vocabulary includes:

## Structural destruction

Players may physically attack and destroy the structure.

The exact threshold for when sufficient destruction defeats/disables the Outpost remains unresolved.

A simple percentage-of-blocks rule may encourage players to remove arbitrary cheap blocks rather than meaningfully topple the structure.

## Defender elimination

Players may fight the defending Pillagers.

The encounter needs enough design that this represents a meaningful combat route rather than merely clearing static vanilla mobs.

## Allay Theft

An Allay powers or is otherwise functionally involved in the Outpost.

A specialized interaction can steal the Allay.

This creates an infiltration/theft problem rather than another health bar.

The Allay's function after capture remains unresolved.

### Current thematic identity

Overworld / occupation / raiding / theft.

---

# 5. Nether Bastion

The Nether Bastion is a tougher defensive structure.

Current interaction vocabulary includes:

## Structural destruction

Players may physically destroy enough of the Bastion to defeat/disable it.

Its construction should make brute-force destruction meaningfully difficult.

Exact structural validation remains unresolved.

## Defender elimination

Players may fight the defending Piglins.

## Gold-Hoard Extraction

The Bastion contains a **physical gold hoard**.

Gold should exist in the actual structure/world rather than being represented only by an abstract reward.

Players can penetrate the Bastion and physically extract/transport that gold.

This creates an extraction and logistics problem in addition to a siege problem.

### Current thematic identity

Nether / fortification / greed / extraction.

---

# 6. End Tower

The End Tower is the final currently defined defensive layer before the Aether Fountain.

Current interaction vocabulary includes:

## Structural destruction

Players may physically attack the tower.

Obsidian or other durable End-related construction can make brute-force destruction difficult.

Exact construction and destruction requirements remain unresolved.

## Defender elimination

Players may fight defending Endermen.

## End Crystal / End Machinery

An End Crystal or related End machinery is functionally involved in the objective.

Reaching and destroying/progressing this machinery provides an objective-specific interaction.

Vertical assault remains an important part of the current concept.

Extra generic loot is not automatically necessary.

### Current thematic identity

End / verticality / durability / assault.

---

# 7. Aether Fountain

The Aether Fountain is the final team structure.

It is connected to enemy respawning.

Current established direction:

> Disabling the Aether Fountain affects the enemy team's ability to respawn.

The Fountain should function as Minecraft infrastructure rather than primarily as another conventional combat health bar.

Earlier concepts included:
- obstructing its source;
- sufficiently destroying the structure.

However, exact:
- exposure;
- disable condition;
- obstruction behavior;
- validation;
- reactivation;
- repair;
- relationship to preceding objectives

remain unresolved.

Do not assume that a single unnoticed placed block should instantly produce the equivalent of destroying a conventional MOBA Nexus.

The exact transition from a disabled Fountain to final match victory also remains subject to objective-system development.

---

# 8. Objective Methods and XP

The previous objective-method XP hierarchy is superseded.

Do **not** assume:

| Method | XP |
| --- | --- |
| Structural destruction | Lowest |
| Defender elimination | Medium |
| Objective-specific interaction | Highest |

The current rule is:

> Objective solution methods should create strategically different consequences rather than being globally ranked through XP multipliers.

For example:
- demolishing a wall may consume tools and time but create permanent access;
- eliminating defenders may secure temporary control;
- stealing an Allay may alter the structure's function;
- extracting a gold hoard physically removes valuable material;
- destroying End machinery may directly progress the objective.

Those consequences can make methods meaningfully different without requiring one to universally pay more XP than another.

Exact objective XP values and contribution accounting remain unresolved.

---

# 9. XP Philosophy

XP should represent broad Minecraft-native contribution rather than functioning primarily as a combat reward.

Prefer:
- objective;
- legible;
- system-recognizable

evidence of contribution over subjective judgments about whether a player's action was strategically intelligent.

## Resource attainment

Legitimate resource attainment grants XP directly.

The XP system does not need to determine whether each acquired resource was ultimately used well.

Usefulness is presumed through the resource economy.

This avoids requiring the game to trace every resource through its eventual strategic use.

## XP calibration is downstream of the day/night economy

**Working direction, 12 September 2026.** Do not derive XP numbers from the day/night model. The dependency runs day/night system, then nighttime Worksite availability, then Mob Swarm availability, composition and value, then expected Combat economic throughput, then expected teamwide XP throughput, then XP band calibration.

XP curve calibration should eventually account for how much legitimate XP activity is available during each night, how much Combat progression changes exploitation rate, how much the nighttime infrastructure slowdown offsets other economic throughput, how Worksites inject phase-appropriate opportunities, and how spatial depth changes reward density. The XP branch is intentionally downstream of these decisions; see section 17A.

## Other XP sources under consideration

Examples may include:

- exploration discoveries;
- major neutral encounters;
- objective contribution;
- infrastructure interaction/use;
- other recognizable Minecraft accomplishments.

## Infrastructure and XP

Infrastructure should not generally generate XP merely because it was submitted, exists, or has a player standing nearby.

The current infrastructure-integration direction instead modifies otherwise legitimate XP-generating activity.

A recognized Construct alone does not automatically grant an XP multiplier.

When distinct allied infrastructure systems become connected to or established around a Construct, they may contribute toward an **infrastructure-integration XP multiplier** affecting legitimate XP earned through activity in the relevant area.

Current contributing infrastructure candidates are:

- Development infrastructure;
- Routes;
- Supply Lines.

The underlying accomplishment remains the XP source.

For example:

- legitimate resource attainment remains resource-attainment XP;
- recognized productive activity remains the relevant productive XP;
- other eligible accomplishments retain their normal identity.

Infrastructure integration can increase the value of that legitimate activity without creating passive XP.

This serves a strategic purpose:

> World opportunities encourage players to disperse, while connected infrastructure rewards teams for intentionally concentrating some useful activity around places they have developed.

The goal is not to require XP-efficient play to occur at a permanent home base.

A remote Worksite, forward position, objective approach, bridgehead, or other strategically useful location may be worth formalizing and connecting later in the match.

Multiple infrastructure slots and expanding network capacity should allow teams to develop additional centers rather than being punished for having already invested in an earlier base.

Exact multiplier values remain subject to balance testing.

Current first-pass targets are approximately:

- fully integrated T1 infrastructure: ~1.10×;
- fully integrated T2 infrastructure: ~1.15×.

T1 and T2 currently describe infrastructure-integration strength for balance discussion. They are not yet mapped to specific player levels or universal progression rewards.

In particular, do not assume:

- T1 is automatically granted at Level 6;
- T2 is automatically granted at Level 12;
- or Levels 20/25 automatically advance infrastructure integration tier.

The current XP-band model expects T1 infrastructure to emerge and mature during earlier infrastructure-enabled progression, with T2 infrastructure emerging later and becoming increasingly mature in advanced progression. However, the exact class-authored rewards, levels, or upgrade paths by which individual infrastructure systems advance from T1 to T2 remain unresolved.

Intermediate values should reward distinct-system integration rather than simply making every infrastructure element an independent flat XP aura.

Exact XP values remain unresolved.

---

# 10. Exploration XP

Existing exploration-XP concepts include:

## Team First

Reward for the first allied discovery of a location/opportunity.

## Match First / Pioneer

Reward for the first discovery in the match.

Exact:
- eligible discoveries;
- reward amounts;
- duplication rules;
- visibility requirements;
- contribution accounting

remain unresolved.

This system is one reason unconditional early movement speed is strategically dangerous.

Movement speed can affect not only traversal but also who reaches discovery rewards and strategic opportunities first.

---

# 11. Worksites

**Established, reconciled 15 September 2026.** There are exactly two Worksite types: **Mining Site** and **Industrial Factory**. Worksites are latent productive opportunities, not free sunset resource drops or mandatory permission to participate in a resource tier. Their value comes from scarcity, concentration, timing, resource composition and phase-appropriate productive capability.

## Sunset selection and phase assignment

The world contains a **surplus pool of dormant Mining Sites and Industrial Factories**: more locations than regular match activation guarantees to use. At sunsets **6m, 18m, 30m and 42m**, some eligible dormant Worksites are selected and **Activated**, receiving that phase's opportunity package. No particular site is guaranteed to activate. Any dormant Worksite can potentially receive any phase/tier package appropriate to its type until selected; tier/resource packages belong to the activation phase, not permanently to the location. Scouting and preparing a site therefore do not guarantee its future tier or activation.

| Sunset | Phase-economic verb | Industrial Factory workstation package |
| --- | --- | --- |
| 6m | Establish | Blast Furnace + Smoker |
| 18m | Specialize | **TBD / OPEN** |
| 30m | Capitalize | Enchanting Table |
| 42m | Resolve | Smithing Table |

Mining resource compositions and quantities require separate phase balancing; existing quantitative sensitivity fixtures remain non-canon. The phase verb **Capitalize (30m)** does not restrict the capitalization lifecycle step to that sunset.

The existing Working nighttime availability direction remains: selected sites open at sunset and close at sunrise. Closure does not erase exposed physical ore or revoke earned Infrastructure Slots. Exact closure effects on unfinished capitalization and Factory operation, reuse/exhaustion, selection algorithm, geographic fairness, eligibility constraints, active counts and special mob pressure remain **OPEN**. Eligibility must not silently assign a permanent tier to a dormant location. Availability and intensity are separate tuning axes.

## Shared lifecycle

**Dormant -> Activated -> Capitalized -> Exploited**

- **Dormant:** a latent site of one of the two types; its eventual phase/tier is unresolved.
- **Activated:** the sunset/world event selects the site and assigns its package. This is distinct from player work.
- **Capitalized:** qualifying player work unlocks productive payoff under the type-specific gate.
- **Exploited:** players perform Extraction or Production to realize value. This can be partial; it does not assert depletion of a Factory.

Exploration discovers and reaches both types; Combat can secure, contest or deny them. Neither receives its own Worksite type. Construction/Extraction and Logistics/Production are activity relationships, not four archetype-specific Worksite categories.

# 12. First Capture and Infrastructure Slots

**Established, reconciled 15 September 2026.** At **Lv6**, each team member gains **1 Infrastructure Slot**, offering the ability to create/support one eligible Infrastructure. Eligible infrastructure includes **Construct, Route, Supply Line, Development Zone**, and the **Working Mob Slayer** concept using otherwise unused/relevant weapon enchantment specializations such as Bane of Arthropods and Smite. Mob Slayer's exact effects, recognition, eligibility and enchantment rules remain **OPEN**. The four developed recognition grammars below do not exhaust future eligible infrastructure.

Normal character/class progression increases player Infrastructure Slots; exact later level slot cadence remains **OPEN**. Authored class eligibility and role-expression branches still apply and do not establish a universal numerical slot schedule. Separately, the **first team to capitalize each activated Worksite earns +1 team Infrastructure Slot**. Player progression-granted capacity and team-earned Worksite capacity are distinct. Allocation, assignment, sharing, reassignment and interaction of team slots with player capacity remain **OPEN**.

**First Capture != Exclusive Control.** The first qualifying Construct at an activated Mining Site or the first qualifying Logistics supply/enabling work at an activated Industrial Factory earns that site's one-time team slot. Subsequent enemy use or loss of practical control does not revoke or transfer the earned slot. Capture does not award a flat XP payout for the slot.

The Mining Site capture trigger is the first team establishing its qualifying Construct; the Industrial Factory trigger is the first team satisfying its qualifying Logistics supply/power/enabling requirement. Merely arriving, sunset activation, finding a workstation, or beginning work is not capture. Every activated Worksite offers this reward once, to its first capitalizing team.

# 13. Mining Site

**Construction capitalization -> Extraction opportunity.** Activation communicates the prospective resource composition/opportunity but **does not expose the special ores**. The exact communication UI/mechanism remains **OPEN**.

A team must establish a **Construct-compliant building at the site**, using the ordinary Construct recognition contract in [infrastructure.md](infrastructure.md#constructs). Player-designated Construction Blocks, the minimum recognized threshold, bounded recognition region and maximum recognizable block count governed by infrastructure investment apply; exact unsettled parameters are not invented here. This capitalization exposes the ore manifestation and makes Extraction possible. A generic shelter or arbitrary submitted build is insufficient unless it meets the Construct contract. No separate Core, multiblock or prospecting system is required.

The exposed opportunity is physical resource geography: players approach, excavate, collect, transport and use it. Exposure does not automatically clear terrain or dispense finished resources. Opponents can also contest and extract once exposed; the first capture slot is retained by the first team. Exposed ore remains physical world state after later disruption. Mining Sites are **finite-opportunity/depletion oriented**: intact -> partially extracted -> depleted. No infinite repeated generation or automatic renewal is established. Exact manifestation placement, finite resource budgets and later disruption handling beyond exposed-ore persistence remain **OPEN**.

# 14. Mining Site and Extraction Specialists

Generic players can exploit exposed ore. Extraction specialists should retain advantages in interpreting geography, selecting approaches, searching, tunneling and efficient extraction. Revealing the prospective composition must not solve the whole excavation task or invent ore-sensing capabilities for a class.

Preserve **WP_X = A(O) + qH**, with **H measured in normalized Harvest Units**. Fortune increases realized qualifying harvest and therefore Extraction progression naturally; it does not multiply physical geology or create a separate flat reward. UAU, Work Stock and Practical Reach retain their current meanings and authority. Exact XP yields and quantitative sensitivity figures are not canonized by this Worksite revision.

# 14A. Industrial Factory

**Logistics capitalization -> Production opportunity.** Sunset activation physically exposes the assigned special workstation(s), but they remain unusable/inaccessible **for productive use** until sufficient qualifying Logistics work supplies, powers or otherwise enables them. Use the phase table in section 11; **18m is TBD / OPEN**, and Enchanting Table belongs to 30m.

The first team to satisfy that Logistics requirement captures the site and earns its +1 team Infrastructure Slot. Opponents may later establish sufficient qualifying Logistics and use the workstations too. First capture does not confer exclusive access. A recognized Supply Line can support qualifying work, but is not itself a mandatory designation check; actual Logistics must satisfy the requirement.

Factories are **throughput/input/access oriented**. Logistics enables the special productive capability; Production transforms supplied inputs into useful outputs. Activation alone does not grant usable production or finished goods. Exact supply/power thresholds, ongoing requirements, input/output rules, operating windows and workstation-specific parameters remain **OPEN**. Earlier Silo, Barrier-cargo, finite fuel-bank and enchanting-only contracts are superseded, not default Factory requirements.

## Economic acceleration

Evaluate capitalization work, exploitation work, downstream **Work Stock**, and persistent acceleration from the earned **+1 Infrastructure Slot** together. Productive structures, material, transport, knowledge and networks can expand **Practical Reach** and future useful work. The slot is persistent capacity, not a flat XP payout. Worksites remain optional accelerators within an ordinary viable economy; teams can capitalize and exploit, defer, partially exploit, contest or concede them. UAU-based valuation must account for legitimate work without turning sensitivity estimates into canonical reward schedules.

---

# 15. Supply Chain

Supply Chain remains a known objective/world-interaction concept, but its advanced development-resource contents are intentionally unresolved.

Do **not** use:

- wheat;
- carrots;
- potatoes

as its unique reward vocabulary.

These crops are now baseline Default world vocabulary and are guaranteed independently.

Earlier versions described Supply Chain as a difficult-to-access development zone containing unique crops and requiring players to construct a traversable supply route.

That implementation should **not** be treated as current established design.

Its exact:
- location;
- resources;
- completion conditions;
- Development interaction;
- Logistics interaction;
- infrastructure requirements;
- strategic reward

remain open.

Supply Chain also should not be used as evidence that Supply Lines require physical traced paths.

The current generic Supply Line concept is separate:
- start storage;
- end storage;
- resource distribution/replenishment;
- intervening topology unresolved.

---

# 16. Major Neutral Encounters

The existing objective design includes a combat-focused neutral escalation:

1. **Giant Zombie**
2. **Ghast**
3. **Ender Dragon**

The previous design associated these encounters with:

| Neutral encounter | Corresponding enemy structure |
| --- | --- |
| Giant Zombie | Pillager Outpost |
| Ghast | Nether Bastion |
| Ender Dragon | End Tower |

Defeating a neutral encounter was intended to:
- grant substantial XP;
- indirectly advance the corresponding enemy siege.

The exact amount and form of that advancement remain unresolved.

The design problem remains:

Too much direct objective damage can make Minecraft-native interaction with the enemy structure irrelevant.

Too little can make the major neutral encounter strategically secondary.

The neutral encounter should create a meaningful alternative strategic investment without replacing the corresponding siege.

Exact neutral-boss XP is unresolved.

---

# 17. Match Escalation and Flavor

The current objective set supports an implicit thematic escalation.

## Early — Pillager / Giant Zombie

- Overworld vocabulary;
- accessible materials;
- conventional enemies;
- theft/infiltration.

## Mid — Nether / Ghast

- stronger fortification;
- environmental complication;
- gold extraction;
- ranged threat.

## Late — End / Ender Dragon

- extreme durability;
- vertical assault;
- End machinery;
- strongest currently defined neutral combat encounter.

## Final — Aether Fountain

The contest shifts toward disabling the enemy team's respawn infrastructure.

This produces a Minecraft-flavored progression:

> Overworld → Nether → End → Aether

without requiring literal vanilla dimensional progression.

---

# 17A. Day and Night Economy

**Working direction, 12 September 2026.** The working distinction is that **day is accumulation and productive time; night is opportunity and contestable time.** Day and night are pressures, not role-locking phases: players can fight in daytime and farm, build or produce at night. The system changes relative economic attractiveness rather than prohibiting actions.

During daytime, established world state compounds most efficiently. Constructs and Development systems operate at full intended efficiency, Routes give their full traversal benefit, Supply Lines their full distribution benefit, and ordinary extraction, production, development, logistics, construction and preparation are comparatively favoured. Teams are rewarded for improving and exploiting controlled world state.

Night is not simply a combat phase. It changes the relative economic environment: infrastructure becomes less efficient, hostile world pressure increases, Mob Swarms become stronger and more specialized and more valuable, a limited selection of Worksites activates, and teams are encouraged to leave stable economic loops and respond to temporary opportunities. PvP should emerge from overlapping demand for those opportunities and from weaker economic projection.

The conceptual loop is: day accumulates, builds, develops, extracts, produces, connects and distributes; at sunset limited Worksites activate, Mob Swarms transform to nighttime forms, and infrastructure enters its reduced-efficiency state; at night teams evaluate opportunities, provision, project outward, secure, exploit, contest, fight mobs and possibly players, and raid, defend, ambush or siege where worthwhile; at sunrise Worksites close, Swarms revert, infrastructure returns to full efficiency, and teams integrate nighttime gains into the daytime economy.

[OPEN] Exact day and night duration is unresolved.

## Infrastructure at night

Nighttime infrastructure penalties are owned by [infrastructure.md](infrastructure.md#infrastructure-at-night). In summary: all four recognized infrastructure types become less effective at night, none disables completely, and the penalty should probably differ by infrastructure type rather than being a uniform percentage. Night temporarily compresses some of the advantage of highly developed infrastructure without introducing an explicit comeback mechanic, creating windows where raids, sieges and ambushes matter more, isolated infrastructure is more vulnerable, and forward expeditions need active protection.

Infrastructure vulnerability alone is **not** considered sufficient to create nighttime PvP. The positive nighttime opportunity economy is the stronger driver.

## Combat's economic role

**Working direction, 12 September 2026.** Combat should not require a dedicated Combat infrastructure system. Combat is **securing value under threat**: the archetype that lets a team economically operate where danger or opposition would otherwise make operating inefficient or impossible. This refines the earlier phrasing that Combat converts threats and contestation into value.

Combat can generate economic value by defeating hostile mobs, securing Mob Swarms, protecting expeditions, contesting Worksites, defending infrastructure, breaking enemy control, escorting valuable resources, raiding, ambushing, sieging, and denying the opponent access to temporary opportunities.

Avoid flat "+X% XP for PvP at night," arbitrary nighttime kill multipliers, and explicit instructions that force players to attack enemy structures at sunset. The preferred model is that nighttime produces richer and more dangerous opportunities, limited Worksites create scarcity, infrastructure projection weakens, teams collide over valuable opportunities, and PvP emerges from game theory.

## Why a direct nighttime PvP bonus may be unnecessary

**Working direction, 12 September 2026.** Do not add an explicit nighttime PvP XP bonus yet. Night already creates three pressures: infrastructure is weaker, Mob Swarms are richer and more dangerous, and limited Worksites activate. Players therefore leave safe economic patterns, movement converges, valuable locations become contested, defence and reinforcement weaken, and PvP becomes more likely without a rule saying kills are worth more after sunset. PvP should happen because the thing we want overlaps with the thing they want. [OPEN] Whether any direct nighttime PvP incentive is still needed after testing is unresolved.

## Two distinct nighttime opportunity systems

At sunset two systems change and they should remain distinct. **Worksites** are discrete, scarce, temporarily active, known strategic destinations. **Regenerative hostile sources** are distributed living-world opportunities whose composition changes with time of day and which depend on location and biome.

The intent is multiple competing nighttime opportunities rather than one mandatory objective-spawn phase. An Industrial Factory activating in the east while a deep mountain Swarm becomes a high-value Stray encounter in the west should force a team to choose between contesting the Worksite, securing the Swarm, splitting, defending infrastructure, ambushing another team, or ignoring both.

---

# 17B. Regenerative Sources

**Working direction, 12 September 2026.** The regenerative-resource system has three top-level buckets: **crops and plant resources**, **animal populations**, and **hostile Mob Swarms**. They are generated with a spatial depth and value gradient; the spatial rules belong to [maps.md](maps.md#regenerative-source-depth-gradient--working) and the economic role belongs here.

The core rule is that regenerating resources increase in **economic specificity** with distance from the midpoint and base regions. Do not read this as farther meaning simply more XP, or the same resource in larger stacks. Near and core resources solve broad universal needs; deeper resources are newer, rarer, more specialized or composition-dependent, and support narrower but stronger strategies. Distance therefore increases specialization, niche utility, strategic value and sometimes challenge.

## Mob Swarms

Mob Swarms are the Combat-facing regenerative resource. They follow the same spatial principle, but add a second major axis: **time of day**. Swarm composition depends on spatial depth, regional and biome identity, and day/night state, so a Swarm is not generated from a difficulty tier alone.

The night encounter does not need to be the day encounter multiplied numerically. Night may change mob type, encounter geometry, status effects, ranged pressure, terrain interaction, density, rare drops, and the tactics required. This is preferred over the same swarm with more health.

Near and core Swarms should be relatively ordinary: zombies, skeletons, spiders, basic mixed groups. Intermediate Swarms may include more specialized threats such as creepers, pillagers, witches, husks, strays, cave spiders, slimes, drowned, and mixed contextual groups. Deep Swarms may include ravagers, charged creepers, large specialized swarms, dense pillager groups, guardian groups in appropriate ocean regions, and multi-mob compositions with tactical synergy.

Do not casually use boss or quasi-boss mobs such as the Warden as ordinary regenerative Swarms. Regenerative Mob Swarms remain distinct from major objectives, bosses, and one-off encounter content.

Nighttime Combat should become more economical primarily because the world presents harder and richer combat work — larger concentrations, stronger variants, more specialized compositions, rare drops, higher-value hostile opportunities, and more difficult territory to operate in. A harder and richer encounter produces more legitimate output and therefore more legitimate XP value, which keeps XP grounded in actual activity rather than in a global nighttime multiplier.

[OPEN] Exact Mob Swarm compositions, day-to-night Swarm transformations, respawn cadence, regional tables, XP values, and drop or reward scaling are unresolved. [OPEN] Whether all nighttime Combat value can be embodied in encounter composition is unresolved.

## Distance and time of day as an opportunity field

Spatial depth and time of day form a conceptual two-dimensional field rather than a numeric tier table: near-day play is routine and near-night play is dangerous; intermediate depth is valuable by day and high-value by night; deep regions are specialized by day and exceptional by night. Deep-night play can become some of the most valuable Combat activity on the map.

This interacts strongly with infrastructure. Deep opportunities are already far from bases, and night weakens Routes, Supply Lines and Constructs, so deep-night expeditions create strong Exploration, Logistics and Combat interdependence.

---

# 17C. Archetype and World-System Mapping

**Established, reconciled 15 September 2026.** Mining Site = Construction capitalization -> Extraction opportunity. Industrial Factory = Logistics capitalization -> Production opportunity. These are exactly two Worksite types. Development and Combat have nurturing/overcoming relationships with Regenerative Sources; Exploration discovers, accesses and connects all meaningful POIs. Combat secures value under threat and can deny either Worksite. These relationships do not assign exclusive ownership or create additional Worksite categories.

## Broader archetype economy

Extraction acquires resources. Production transforms acquired resources. Development matures and improves renewable productive world state. Construction establishes useful built places. Logistics distributes resources. Combat secures value under threat. Exploration discovers, accesses and connects the opportunity graph.

---

# 18. Relationship to Ordinary Progression

Objectives are not the sole source of XP, resources, or strategic value.

Ordinary Minecraft activity remains relevant between and around objective contests.

In particular:

> Legitimate resource attainment grants XP directly.

Other recognized progression sources may include:
- exploration;
- neutral encounters;
- objective contributions;
- infrastructure interaction/use;
- combat and other recognized accomplishments where appropriate.

Objectives therefore answer:

> Where is unusually valuable or strategically concentrated work available right now?

They do not answer:

> What is the only thing worth doing right now?

It may be rational to concede or delay an objective in order to:

- acquire resources;
- explore;
- develop infrastructure;
- establish production;
- prepare logistics;
- attack elsewhere;
- exploit another opportunity.

Multiple strategic priorities should be capable of existing simultaneously.

## XP requirement tiers and productive economy

The current progression direction does not assume that Level 1–30 XP requirements must follow one uninterrupted smooth curve.

Instead, ranges of levels may belong to distinct **XP requirement tiers** corresponding to changes in the productive economy available to players.

Current working model:

> Levels 1–6 form the base XP tier.


The current level-reward curve gives this boundary a specific economic interpretation:

- Level 4 grants Efficiency I, accelerating ordinary block-breaking and worldwork;
- Level 6 introduces the current infrastructure-recognition breakpoint;
- Level 7 grants Yield I through Fortune I + Looting I, increasing resource-acquisition productivity.

The Level 6 → 7 boundary therefore marks a deliberate transition from the constrained opening economy toward a faster productive economy. A player entering the next XP-requirement tier can have access both to recognized infrastructure and, immediately thereafter, improved resource yield.

These rewards are among the reasons later XP requirements can increase without assuming that infrastructure integration alone must account for the increase.

This tier assumes primarily early Minecraft activity without a mature recognized-infrastructure economy.

Progression around the end of this tier can unlock new ways of increasing useful throughput, including infrastructure recognition and integration.

The following XP tier can therefore require substantially more XP per level because players have access to qualitatively stronger economic tools.

This should not be implemented as dynamic XP requirements based on whether a particular team actually built infrastructure.

Level requirements remain fixed.

A team that prepared and integrated its economy well can satisfy the new requirements more efficiently; a team that did not remains capable of progressing but must work harder, establish the missing economy later, or exploit other valuable opportunities.

The infrastructure XP multiplier is only one source of increased progression throughput.

Tier calibration should also account for progression effects such as:

- task efficiency;
- yield improvements;
- increased fundamental Minecraft capacity;
- Development productivity;
- better traversal;
- better resource distribution;
- larger or more sophisticated infrastructure;
- phase-appropriate Worksite opportunities;
- other increasingly valuable world opportunities.

Therefore XP requirement tiers should ultimately be calibrated against:

> expected legitimate XP throughput in the corresponding match/progression phase

rather than simply increasing XP requirements by the same percentage as the infrastructure multiplier.

---

# 19. Objective Design Principles

## Minecraft-native interaction

Objectives should use recognizable Minecraft verbs and world systems wherever possible.

Examples include:

- break;
- kill;
- steal;
- extract;
- climb;
- obstruct;
- build;
- excavate;
- transport;
- develop.

## Distinct solutions without class locks

No objective should simply say:

> bring the correct archetype.

Different classes should be able to contribute through different Minecraft systems.

Specialists can outperform generic players without becoming mandatory.

## Physical rewards and consequences

Where practical, objective value should exist in the world.

Examples:

- Bastion gold is physical gold;
- Mining Site opportunities are physical geology;
- terrain modification persists;
- constructed facilities remain;
- extracted resources must be transported.

Avoid turning Minecraft-native objectives into abstract reward menus where the physical world could carry the consequence instead.

## Persistent strategic consequences

Objective interactions should change:
- access;
- geography;
- resources;
- infrastructure;
- respawning;
- defensive conditions;
- strategic options

rather than merely paying currency and resetting.

## Multiple simultaneous priorities

The match should support concurrent:
- siege;
- neutral encounters;
- Worksite development;
- resource acquisition;
- infrastructure;
- development;
- exploration;
- logistics;
- harassment;
- combat.

---

# 20. Relationship to Infrastructure

**Working canon update, 10 September 2026:** Shared recognition, persistence, physical counterplay, and connection eligibility are governed by [infrastructure.md](infrastructure.md). Objective-specific mechanics remain here. Operational Area defines legal connections for integration; it is not a general buff radius. Industrial Factory completion does not require a recognized Supply Line.

Objectives and infrastructure can overlap spatially and strategically without becoming the same system.

A Worksite facility, objective structure, road, farm, or storage network is not automatically recognized infrastructure merely because it exists.

Likewise, recognized infrastructure should remain useful outside formal Objectives.

## Constructs

Constructs are recognized Construction infrastructure.

Ordinary building remains unrestricted.

A Construct's intrinsic purpose is currently framed as improving **occupation or sustainment efficiency**:

> How efficiently can the team maintain useful presence at this place?

Exact intrinsic mechanics remain unresolved.

A Construct alone does not automatically grant an XP multiplier.

Instead, it can serve as an anchor for infrastructure integration.

## Development infrastructure

Development infrastructure improves eligible persistent productive or maturing world processes.

It remains independently useful and does not require a Construct.

When appropriately colocated with a Construct, it can also contribute toward that location's infrastructure-integration state.

## Routes

Routes concern repeated player traversal.

Their early systemic direction is traversal/sprint efficiency rather than unconditional movement speed.

Current player-established Route concept:

- physically traverse the desired path;
- traversal defines the Route.

A Route connected to a Construct can contribute toward infrastructure integration while retaining its independent traversal value.

## Supply Lines

Supply Lines concern actual resource distribution.

Current establishment concept:

- start storage;
- end storage;
- real resources transfer/replenish downstream.

The intervening topology remains unresolved.

A Supply Line connected to a Construct can contribute toward infrastructure integration while retaining its independent Logistics value.

## Integration

These systems should remain independently useful.

Their combination creates additional value because the team has created a location that is simultaneously:

- occupiable;
- productive;
- accessible;
- supplied.

The current working direction is to recognize that integration through a modest multiplier on otherwise legitimate XP-generating activity within the integrated Construct area.

Infrastructure progression may improve each participating system's contribution to that multiplier.

Do not collapse:

- Constructs;
- Development infrastructure;
- Routes;
- Supply Lines;
- Worksite access;
- objective progress

into a single infrastructure mechanic.

Physical overlap and strategic synergy do not imply identical system ownership or prerequisites.

## Routes

Routes concern repeated player traversal.

Current player-established Route concept:
- physically traverse the desired path;
- traversal defines the Route.

## Supply Lines

Supply Lines concern resource distribution.

Current establishment concept:
- start storage;
- end storage.

The exact intervening topology remains unresolved.

Do not collapse:
- Routes;
- Supply Lines;
- Worksite access;
- Supply Chain

into one generic connectivity system.

---

## Objectives create infrastructure time

**Working canon, 10 September 2026.** Mid- and late-game infrastructure time is not given by the match clock. It is won through control. Objectives and strategic contests create temporary windows of access, space, safety, and labor availability during which a team can establish or expand infrastructure.

Objective and encounter design should therefore be evaluated partly on whether winning creates a usable development window. See [infrastructure.md](infrastructure.md#control-windows-and-infrastructure-labor) for the labor model, the interruption test, and capture behaviour.

# 21. Key Risks / Open Questions

## Team Structure sequence

Is:

Pillager Outpost  
→ Nether Bastion  
→ End Tower

strictly sequential?

Are later structures merely protected until earlier ones fall?

Can difficult Minecraft-native approaches bypass layers?

Unresolved.

## Aether exposure

Does the Fountain become vulnerable:
- after any defensive objective falls;
- progressively as structures fall;
- only after all three;
- through some other rule?

Unresolved.

## Aether disablement

What constitutes a legitimate disable?

How long must obstruction persist?

Can defenders repair/reactivate the Fountain?

How does the system avoid a trivial unnoticed block placement deciding the match?

Unresolved.

## Structural destruction

What constitutes meaningful structural defeat?

Pure percentage destruction risks rewarding arbitrary cheap-block removal.

A robust structural/functionality model remains needed.

## Defenders

Pillagers, Piglins, and Endermen require enough encounter design that defender elimination is a meaningful solution.

## Objective-specific interactions

Exact:
- Allay function;
- Gold-Hoard completion;
- End machinery;
- Aether machinery

need further definition.

## Major neutral encounters

Open:
- exact XP;
- spawn/timing;
- encounter design;
- objective damage/advancement;
- contest structure;
- respawn behavior.

## Worksites

Open: phase selection counts/eligibility and fairness; exact Mining resource packages, manifestation placement and communication mechanism; Factory supply/power thresholds and operating/input rules; **18m Factory workstation**; team-slot allocation; exact legitimate-work XP values. First-capture triggers and the +1 team slot are established, not open.

## Supply Chain

Its advanced resource vocabulary and actual objective interaction remain unresolved.

## XP economy

Open:
- objective XP values;
- contribution accounting;
- neutral encounter XP;
- Worksite XP;
- exploration Team First / Pioneer rewards;
- relationship between MOBA levels and vanilla Minecraft XP/enchanting.

---

## Control windows and captured infrastructure

Open: whether any formal territorial-control mechanic is needed at all, as against functional access emerging from physical play; how mid- and late-objective design deliberately creates development windows; and the capture questions owned by [infrastructure.md](infrastructure.md#capture-local-value-and-network-value).

---

# 22. Superseded Objective Ideas

Do not reintroduce the following as current rules without new discussion:

- Stronghold / Excavation / Monument / Expedition / Siege as the active objective-family model.
- Structural destruction necessarily granting the least XP.
- Defender elimination necessarily granting medium XP.
- Objective-specific interaction necessarily granting the most XP.
- Specialized objective interaction being globally more valuable in XP than combat/destruction.
- Treating a merely defensible building as sufficient without Construct compliance, or treating capitalization as completed Extraction.
- Mining Outpost directly dispensing rare ores.
- Mining Outpost functioning as a Diamond chest.
- Mining Outpost automatically clearing/excavating valuable material.
- Supply Chain using baseline wheat/carrots/potatoes as unique rewards.
- Supply Chain proving that Supply Lines require physical traced paths.
- Worksites requiring arbitrary level locks when economic/material gating can perform the same function.
- One objective being required for every archetype.
- Player-supplied lapis as the Industrial Enchanter's preferred gate, or ordinary free resource cargo as its preferred transport model.
- The earlier rejection of Logistics-powered Factory production is superseded by section 14A; no complicated crafting-order challenge is mandated.
- Industrial Enchanter dependence on recognized Supply Lines.
- Logistics tiers defined by a prescribed chest-boat → rail → Ender technology ladder, or by hidden throughput-tier evaluation.
- Apparatus as a live system, including Core/functional-multiblock prerequisites. Enchanting Table is the 30m Industrial Factory workstation.

---

# 23. Current Working Summary

> **Team Structures give the enemy Minecraft-native siege problems.**

> **Major Encounters create high-stakes combat contests that can influence larger strategic objectives.**

> **Worksites are latent productive opportunities: Mining Sites require Construction capitalization before Extraction; Industrial Factories require Logistics capitalization before Production. Every first capture earns +1 team Infrastructure Slot without exclusive control.**

> **World Locations provide strategically valuable Minecraft geography without every important place needing to become a formal objective.**

> **Ordinary Minecraft activity remains a valid source of progression and strategic value between and around objectives.**

> **Objective methods should differ through their costs and consequences, not through a universal specialized-interaction XP hierarchy.**

> **Legitimate resource attainment grants XP directly; the XP system does not need to judge whether every acquired resource was ultimately used well.**

The objective system should concentrate conflict around meaningful Minecraft opportunities while preserving the broader sandbox as part of the competitive game.