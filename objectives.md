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

- ongoing activation/prospecting should pause;
- already revealed opportunities should remain revealed.

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

> prospects/reveals.

Players:

> excavate.

The apparatus should not become:

- an ore dispenser;
- a Diamond chest;
- automatic block clearing;
- a substitute for actual mining.

Its reward category is:

> phase-appropriate exceptional extraction opportunities.

These opportunities may derive their value from:

- scarcity;
- concentration;
- timing;
- local scarcity;
- unusual resource combinations.

The valuable material should continue to exist as physical Minecraft resource geography.

---

# 14. Mining Outpost and Extraction Specialists

A generic player must be able to exploit a Mining Outpost.

An Extraction specialist should nevertheless substantially outperform a generic player in areas such as:

- search;
- excavation;
- route selection;
- extraction efficiency.

The Worksite should therefore expose an opportunity without completely solving the extraction problem.

For example, a prospecting apparatus may reveal where valuable geology exists while leaving the actual:

- approach;
- excavation;
- tunnel creation;
- resource collection;
- transport

to players.

This preserves meaningful class advantage without making an Extraction class mandatory.

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

Objectives may interact with infrastructure systems, but those systems should remain conceptually distinct.

## Constructs

Constructs are recognized Construction infrastructure.

They do not currently have a mandatory settled XP-multiplier mechanic.

Do not require a Worksite facility to be a Construct unless that relationship is deliberately established later.

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