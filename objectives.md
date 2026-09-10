# Minecraft MOBA — Objectives

Current design state: September 8, 2026

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
3. **Worksites** — exceptional resource opportunities that players develop, contest, exploit, and eventually deplete.
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

Worksites are Key Locations offering:

> exceptional resource opportunities

rather than simply "rare materials."

Their value may derive from:

- scarcity;
- concentration;
- timing;
- local scarcity;
- unusual combinations;
- phase-specific opportunity.

## Desired lifecycle

Dormant opportunity  
→ developed opportunity  
→ contested resource site  
→ depleted opportunity

A Worksite should become valuable because players materially develop and exploit it.

Players should not receive its full value merely by:

- camping nearby;
- mining randomly;
- enclosing it with trivial walls;
- submitting an arbitrary construction.

Development should materially change access to the opportunity.

Ownership should not be absolute.

Enemy players may:
- attack;
- contest;
- enter;
- exploit;
- interfere with

a developed Worksite.

---

# 12. Worksite Apparatus

The current stronger Worksite model separates three layers.

## Core

A phase-expensive crafted object or block.

Most of the economic gating should probably reside here.

## Functional multiblock

A recognizable required topology surrounding or incorporating the Core.

This establishes functional infrastructure without requiring a giant rigid blueprint.

## Facility

The flexible player-authored environment surrounding the functional apparatus.

This may include:

- storage;
- shelter;
- defenses;
- access;
- roads;
- Routes;
- logistics.

The preferred principle is:

> recognizable functional topology + terrain adaptation

rather than:

> reproduce a giant predetermined schematic.

If the functional multiblock is invalidated:

- ongoing activation, prospecting, or generation of new Worksite opportunities should pause;
- opportunities already generated by successful Worksite operation should remain physically present;
- already revealed information about those opportunities should remain revealed.

A Worksite's exceptional opportunity does not necessarily need to exist in exploitable form before the Worksite is developed.

Where appropriate, successful Worksite operation may cause a new physical opportunity to be generated in the world.

The important constraint is:

> A Worksite creates access to an exceptional Minecraft-native opportunity rather than directly dispensing its finished reward.

The exact form of this generation is Worksite-specific.

Where possible, prefer economic/material gating over arbitrary level locks.

---

# 13. Mining Outpost

The Mining Outpost is the current most-developed Worksite example.

Its intended flow is:

discover  
→ invest  
→ construct/progress apparatus  
→ prospect/reveal  
→ excavate  
→ extract  
→ transport/use

## Apparatus role

The Mining Outpost apparatus:

> prospects, generates, and reveals an exceptional extraction opportunity.

Players:

> excavate and extract it.

The Mining Outpost site represents a **latent resource opportunity**.

Its exceptional resource deposit does **not** physically exist underground before successful operation of the Worksite apparatus.

Successful prospecting causes phase-appropriate resource geography to be generated within the Worksite's eligible surrounding geology. The apparatus then provides players with information sufficient to locate and pursue that opportunity.

This distinction is deliberate.

If the exceptional deposit existed physically before Worksite activation, players could accidentally or deliberately discover and extract the future Worksite reward through ordinary pre-mining. That would allow the Mining Outpost's development requirements to be bypassed.

The Worksite therefore gates the **creation of the exceptional opportunity**, not permission to mine blocks that were already present.

Once generated, however, the opportunity becomes ordinary physical Minecraft world state.

The valuable resources:

- exist as physical blocks in the world;
- occupy actual resource geography;
- must be reached through the surrounding terrain;
- must be physically excavated;
- must be collected;
- must be transported and used normally.

The apparatus should not become:

- an ore dispenser;
- a Diamond chest;
- automatic block clearing;
- an automatic mining system;
- a substitute for actual excavation and extraction.

Its reward category remains:

> phase-appropriate exceptional extraction opportunities.

These opportunities may derive their value from:

- scarcity;
- concentration;
- timing;
- local scarcity;
- unusual resource combinations.

The key distinction is:

> The resource opportunity does not exist before successful Worksite operation, but after generation it exists as real Minecraft resource geography.

---

# 14. Mining Outpost and Extraction Specialists

A generic player must be able to exploit a Mining Outpost.

An Extraction specialist should nevertheless substantially outperform a generic player in areas such as:

- search;
- excavation;
- route selection;
- extraction efficiency.

The Worksite should therefore expose an opportunity without completely solving the extraction problem.

For example, successful Mining Outpost prospecting may generate and reveal valuable geology while leaving the actual:

- interpretation of the revealed opportunity;
- approach;
- excavation;
- tunnel creation;
- resource collection;
- transport

to players.

The apparatus creates the exceptional opportunity and makes it discoverable.

It does not perform the Extraction player's work.

A generic player must still be capable of exploiting the generated deposit, while an Extraction specialist can substantially outperform that player in finding the best approach, reaching the deposit, excavating it efficiently, extracting its resources, and moving the resulting material into the wider economy.

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
- Mining Outpost opportunities are physical geology;
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

O# 20. Relationship to Infrastructure

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

Open:
- Core recipes/cost;
- apparatus topology;
- activation;
- prospecting;
- depletion;
- enemy interaction;
- resource generation/revelation;
- Worksite XP.

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

# 22. Superseded Objective Ideas

Do not reintroduce the following as current rules without new discussion:

- Stronghold / Excavation / Monument / Expedition / Siege as the active objective-family model.
- Structural destruction necessarily granting the least XP.
- Defender elimination necessarily granting medium XP.
- Objective-specific interaction necessarily granting the most XP.
- Specialized objective interaction being globally more valuable in XP than combat/destruction.
- Mining Outpost being completed simply by submitting a defensible Construction project.
- Mining Outpost directly dispensing rare ores.
- Mining Outpost functioning as a Diamond chest.
- Mining Outpost automatically clearing/excavating valuable material.
- Supply Chain using baseline wheat/carrots/potatoes as unique rewards.
- Supply Chain proving that Supply Lines require physical traced paths.
- Worksites requiring arbitrary level locks when economic/material gating can perform the same function.
- One objective being required for every archetype.

---

# 23. Current Working Summary

> **Team Structures give the enemy Minecraft-native siege problems.**

> **Major Encounters create high-stakes combat contests that can influence larger strategic objectives.**

> **Worksites create exceptional resource opportunities that must be developed, contested, excavated, and exploited in the physical world.**

> **World Locations provide strategically valuable Minecraft geography without every important place needing to become a formal objective.**

> **Ordinary Minecraft activity remains a valid source of progression and strategic value between and around objectives.**

> **Objective methods should differ through their costs and consequences, not through a universal specialized-interaction XP hierarchy.**

> **Legitimate resource attainment grants XP directly; the XP system does not need to judge whether every acquired resource was ultimately used well.**

The objective system should concentrate conflict around meaningful Minecraft opportunities while preserving the broader sandbox as part of the competitive game.