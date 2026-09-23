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

## Objective legibility — Working, 23 September 2026

**Raised from the first live playtest.** Standing beside an authored Pillager
Outpost, the structure read as scenery rather than as *the thing this team is
supposed to defend*. Nothing about it says whose it is, that it is contestable,
or that it belongs to the same system as the team's Fountain. A player who has
not read the design cannot tell an objective from a village.

This is a real design problem, not presentation polish. Objectives are meant to
organise where teams go and what they protect, and an objective nobody
recognises cannot do that work.

**Working direction: a shared visual signature.** Each defensive objective and
the team's Aether Fountain — including the ground immediately around it — carry
the *same* light or glow signature. The shared treatment is the point: it should
read as one system, so that finding an objective tells a player it is connected
to the Fountain they respawn at, without a tutorial or a HUD marker saying so.

Minecraft-native means are preferred over invented UI, consistent with the rest
of this document: emitted light, particles, a vertical beam, coloured glow. A
beacon beam was tried as a throwaway prototype during the same session purely to
judge whether a vertical light signature reads at distance; it is **not** a
proposal to put beacons in objectives.

[OPEN] Everything about the form:

- **whether the signature is team-coloured**, and if so whether an enemy sees
  your colour or only that something is there;
- **whether it is visible at range or only near.** A signature visible across
  the map is a permanent position broadcast and interacts with Exploration's
  value — knowing where things are is supposed to be earned. A Fountain beacon
  visible from the enemy Hinterland gives away more than legibility requires;
- **whether it changes with state.** Defensive Capacity is a finite quantity
  being reduced by four routes; a signature that dims, changes colour or breaks
  as capacity falls would make the siege legible at a glance. It could equally
  be too much information for free;
- **whether the Lair shares it.** The Lair is neutral and conspicuous by
  design, so it may want a *different* signature rather than the team one —
  the contrast may be more useful than the consistency;
- whether the Fountain's surrounding area is marked by the same means as the
  structure, or by a weaker/derived one.

### What the 23 September prototype established

Tried live, in order, at an authored Pillager Outpost:

| Tried | Result |
| --- | --- |
| Beacon + iron pyramid | Beam renders. Pyramid unnecessary -- a lone beacon with sky access beams. Colour needs stained glass above it, so team colour costs world blocks. |
| `block_display`, block = `barrier` | **Nothing renders at all.** |
| Invisible glowing shulker | Shell renders anyway; `Invisible:1b` does not hide it. Shape is a shulker, not a box. |
| `block_display`, block = `glass`, scaled 25x27x25 | Reads as a box: dark frame, clear interior, outline traced. **Chosen direction.** |

**The finding that constrains everything else: the glow is traced from what the
model renders, so there is no invisible-but-outlined form.** "Retexture it away"
defeats the effect it is meant to preserve. The choice is which *visible* body is
acceptable, not whether to have one.

**Working direction for the body: a purpose-built wireframe model, not a
retextured vanilla block.** Retexturing a block changes that block everywhere it
occurs, including where players place it. An `item_display` carrying an item
whose model the pack defines affects nothing else, and the pack pipeline already
writes item models and uses the `minecraft:item_model` component. The model
should be twelve thin edge cuboids rather than a solid cube, so the rendered
body is bars and the glow traces a genuine wireframe.

**Extent:** the box should be larger than the structure, taking in some
surrounding terrain, so it marks a *place* rather than a building.

[RESOLVED] Team colour DOES apply to a `block_display`. The prototype outlined
white because glow colour resolves against the scoreboard the **viewing client**
holds, and the HUD hands every player a private one, so a team registered on the
main board did not exist for them. This was ours, not Minecraft's.

### Ground tint — Working, 23 September 2026

**The signature is the controlled GROUND, not a container around it.** The box
stays the size it is -- the area outside the structure is controlled area and
just as much the thing being defended -- but the glass volume becomes
conceptual, and what the player sees is the terrain inside it wearing the team's
colour. The objective's own blocks keep their appearance.

Minecraft already tints terrain per biome; that is why a swamp does not look
like a jungle. So the recolour swaps the biome rather than inventing a rendering
path. Verified legible in play.

**The two tints are drawn from biomes the map does not contain**, surveyed
around each volume at bind time. Contrast has to be a property of the map rather
than a guess: a tint matching nearby terrain communicates nothing, and which
colours are free depends on what the generated map already looks like. If fewer
than two candidates are free the feature declines rather than picking a
colliding colour. First run chose `cherry_grove` for north and `swamp` for
south.

Constraints, none of them chosen:

- **only biome-tinted blocks change** -- grass, ferns, leaves, vines, sugar
  cane, water. Dirt, stone, wood and planks have fixed textures and never will,
  which is why the structure keeps its own colour;
- **biome cells are 4x4x4**, so the edge snaps to a four-block grid;
- **biome is not only colour.** It carries mob spawn lists, weather, ambient
  sound and fog, so tinting silently changes what spawns on the tinted ground.
  Choosing absent biomes confines that rather than removing it.

[OPEN] The spawn side effect must be resolved before this is on by default. Two
routes: a datapack biome copying the local one and overriding only the colours,
or sending biome data per viewer so the server's world never changes -- which
would also allow an ally and an enemy to see different colours. Neither is done.

**A glowing box cannot be read from inside it.** Observed in play at the
Fountain, whose box is large enough to stand in. Faces are culled from within
and the outline is traced from rendered faces, so a player standing inside sees
little or nothing of the signature meant to tell them where they are. This is a
constraint on extent, not a bug: **the box must stay small enough that a player
is normally outside it.** It bears directly on marking "some of the surrounding
terrain" -- the more ground the box takes in, the more of the time a defender
standing on that ground cannot see it. [OPEN] Whether a defender should see
their own objective's signature while inside its footprint, and by what
different means, is unresolved.

[DEFERRED, 23 September 2026] **Range is not being decided now.** Values of 6-8
chunks were discussed and the question was explicitly set aside, because it is
really the vision/information question rather than a presentation setting. When
it is taken up: the plugin should own the radius and spawn/despawn per player,
because client view distance and server entity-tracking range are both
configurable and a design number must not be inheritable from a video settings
menu.

Settled: **all players see the glow in the colour of the team that controls the
objective.** Note this presumes control can change; the current design has no
capture, so "controls" presently means "was authored for".

Nothing else is decided, and no brightness, radius or particle is asserted. The
rest of the settled part is only that **objectives and the Fountain should be
recognisable as one team system by looking at them**.

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

## Progression and capability — Working

Worksites are **finite exceptional opportunities that test archetype capability through Minecraft-native activity**. They span and act as gateways across early-mid through late-mid progression, potentially extending into the beginning of lategame, but not endgame. Their requirements must not assume endgame technology. Later opportunities can demand greater capability while retaining ordinary Minecraft participation and meaningful specialist advantage.

The Mining Outpost tests Extraction, supported by Construction that establishes its apparatus. The Industrial Enchanter tests Production, supported by Logistics that realizes a finite delivery opportunity. These are working examples, not a requirement to provide one Worksite for every archetype.

## Nighttime activation — Working, 12 September 2026

**Worksites open only at sunset.** At sunset a limited number of Worksites activate. The number that activate depends on match phase, and which eligible Worksites activate is chosen randomly from the eligible pool.

[HISTORICAL — superseded 23 September 2026] This passage previously continued "at sunrise the active Worksites close." Sunrise closure is superseded: sunset is an activation event, not an availability window, and an activated Worksite remains active for the rest of the match. See §17C, *Worksite activation is permanent*.

This exists to create positive nighttime opportunity rather than a global nighttime stat modifier: scarcity, unpredictable but legible convergence, and a reason not to script the same route every night.

Selection should be constrained by eligibility rather than uniformly random. [OPEN] Candidate eligibility inputs include match phase, Worksite type, prior use or exhaustion, geographic fairness, distance, map distribution, accessibility, and whether the chosen sites cluster near one team. None of this is settled.

Availability and intensity are two independent tuning axes. More active Worksites does **not** automatically produce more contestation — too many simultaneous sites simply let teams split peacefully. A broad candidate pattern is few moderate-value opportunities early, several fronts at mid, and potentially fewer but much more consequential opportunities late. [OPEN] Exact counts per phase, the selection algorithm, phase eligibility, and whether late-game Worksites become more numerous or fewer and more consequential are all unresolved. [OPEN] Whether Worksites themselves receive special mob pressure at night is unresolved.

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

[OPEN — conflict recorded 23 September 2026] The 23 September Worksite economy
handoff states that "the apparatus model is obsolete" and that a Mining Site must
not be an ore dispenser, chest, automatic mine or abstract loot payout, while
still requiring that "a qualifying **Construct** capitalizes the Mining Site."
The three layers below are a Construction-established topology, not a dispenser,
so they satisfy the second requirement and contradict the first only in
vocabulary. The section is therefore retained unchanged and the contradiction is
recorded rather than resolved: whether "apparatus" is merely renamed to
"Construct" or the layered model itself is withdrawn is an unresolved design
decision. Do not silently delete this section on the strength of one word.

The current Construction-established apparatus model, developed for the Mining Outpost, separates three layers. It is not a universal prerequisite for every Worksite; the Industrial Enchanter uses the Logistics gate described in section 14A.

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

For the Mining Outpost, if the functional multiblock is invalidated:

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

The Mining Outpost is the current most-developed Worksite example: an **Extraction challenge generator**, not simply a resource generator.

Its intended flow is:

discover  
→ invest  
→ construct/progress apparatus  
→ prospect/generate/reveal\
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

> The latent opportunity exists before activation; the ore itself does not. Successful apparatus operation generates new physical resource geography, then reveals it.

Its challenge has intrinsic completedness:

> **Generated ore geography: intact → partially excavated/extracted → depleted.**

Physical excavation and depletion express progress without an arbitrary completion bar. Generated ores are normal Minecraft blocks and persist if the apparatus is later disrupted; disruption stops further prospecting/generation, not extraction of an existing deposit.

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

# 14A. Industrial Enchanter — Working Production Worksite

## Central apparatus and child Silos

The current direction is a central **Industrial Enchanter**, positioned relative to child **Lapis Silos** at increasingly difficult logistical distances. The opportunities roughly represent three interphase gamestates across early-mid through late-mid progression, potentially reaching the start of lategame. This is a capability progression, not a finalized count of three Silos or a prescribed technology ladder. Names and whether internal fuel remains lapis-themed are open.

Logistics is the supporting challenge for Production here, as Construction supports Extraction at the Mining Outpost. The Industrial Enchanter must remain fully completable without recognized **Supply Line** infrastructure. It must never require a Supply Line designation, node, multiplier, or recognition check. Ordinary transport infrastructure and class capabilities can help on their own merits.

## Deliberate activation and finite flow

A Silo requires deliberate activation so a player does not accidentally spend an infeasible opportunity merely by passing through its loading zone. Activation announces the activating team, Silo, and remaining timer; the exact activation UI and announcement presentation remain open.

During a finite active window, the Silo gradually outputs transport cargo into suitable available carrying capacity or world-item transport. It does not present the entire payout as an immediate pickup. Players choose how much to wait for, batch, load, and move before the transfer window closes.

Difficulty scales through logistical distance and route difficulty, output schedule, and transfer window. Ordinary running should be viable early and increasingly inefficient later. Mature Logistics solutions should capture much more of the available value without hard technology or class locks. Greater logistical difficulty offers greater potential payout; exact amounts are prototype parameters, not settled balance.

The challenge measures **item-flow performance**, not raw movement speed. Relevant capabilities and costs include:

- batching, loading time, waiting, and available carrying capacity;
- outward travel, return trips, hunger, danger, and human error;
- automation, parallel carriers, and continuity of flow while players do other work;
- routing, handoffs, transfer bottlenecks, and distribution into the Enchanter intake.

A fast runner can help, but speed alone does not solve these combined constraints. Success is expressed by actual cargo delivery; there is no hidden throughput-tier evaluation or prescribed chest-boat → rail → Ender technology sequence.

## Proportional banking, then Production

The preferred outcome is proportional success: successfully delivered cargo banks proportional **inaccessible Worksite fuel/energy**. Partial delivery retains proportional value rather than requiring all-or-nothing completion. Undelivered cargo does not count as banked fuel; its final handling remains open.

After the Logistics transfer phase, banked fuel determines a finite Industrial Enchanter Production opportunity. Production then tests preparation and throughput: players bring prepared enchantable goods and convert them into useful enchanted output within that opportunity. The Worksite provides access to exceptional production, rather than dispensing finished enchanted rewards automatically.

The exact relationship between fuel and operating duration or processing capacity remains open. Exact enchanting rules, vanilla XP requirements, RNG, bookshelves, eligible items, and the treatment of already enchanted goods are also open. A complicated crafting-order challenge is not the current preferred direction.

## Cargo representation — Prototype/test

Worksite fuel must remain economically inaccessible. Ordinary free lapis, coal, kelp blocks, blaze rods, or other usable resources are not the preferred cargo: they create unintended resource payouts and potential collisions with future resource-focused classes. Resource acquisition is the Mining Outpost's purpose; here the transported material is instrumental to the Production opportunity.

The current prototype is the existing vanilla technical **Barrier** item, renamed and remodeled through the resource pack, with the working concept **“Encumbered with Fuel.”** It has no ordinary Survival economy, is stackable, and is not normally placeable in Java Survival. It transiently represents inaccessible Worksite fuel/cargo rather than introducing a new economic resource. This narrow reuse of a technical item does not establish a precedent for proliferating bespoke MOBA items. Its final presentation and implementation still require testing.

Cargo should be transport-method agnostic where feasible: player inventories, vehicles and class carriers, dropped-item transport, water, hoppers, minecarts, and other compatible Minecraft item-transfer methods. Stack quantities can represent cargo quantities; the concept does not prescribe one fuel unit per inventory slot or require an inventory-lock mechanic.

## Deliberate asymmetry

Construction gates the Mining Outpost through **creation of previously nonexistent geology**. Once created, that physical Extraction opportunity persists independently of later apparatus disruption.

Logistics gates the Industrial Enchanter through **realizability of increasingly difficult timed item flow**. Actual delivery banks the fuel that makes a finite Production opportunity usable. These gates intentionally differ; do not replace either with a shared arbitrary capability check.

## Open details

- Exact Silo count, layout, distances, timers, output rates, transfer window, and grace period.
- Activation UI and precise loading/intake interactions.
- Barrier presentation, tagging/identification, cleanup, death, enemy interception, cross-team behavior, portals, and buffering/storage behavior.
- Exact banked-fuel conversion into Production duration/capacity and handling of unused fuel.
- Enchanting rules, XP, RNG, bookshelves, and eligible inputs/outputs.
- Final names and whether internal fuel remains lapis-themed.

Enchanting identity is reserved for this Production Worksite. An Enchanting Table should not be a Construction apparatus candidate; sharing that identity would collide with the Industrial Enchanter's role.

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

**Canonical, 22 September 2026.** The escalation below is THEMATIC VOCABULARY,
not the match's temporal state machine. This section previously read as the
top-level temporal organizer, and both doctrine and runtime treated it that way.
It is superseded in that role by the alternating Worksite/Lair cadence in §17C.

What that means concretely: **all three team defensive objectives exist at the
same time, from the opening.** Overworld/Nether/End no longer decide which
objective is present, reachable or attackable. They describe what an encounter
feels like and what materials it speaks in.

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

without requiring literal vanilla dimensional progression, and — **as of 22
September 2026** — without governing availability. See §17C.

---

# 17C. Exceptional Opportunity Cadence — Canonical, 22 September 2026

**This is the top-level temporal model.** It supersedes the phase timeline for
the purpose of deciding what exists and when.

Exceptional opportunity alternates and escalates across successive opportunity
nights:

> **Worksite I → Giant → Worksite II → Ghast → Worksite III → Ender Dragon**

Odd nights are Worksite nights; even nights are Lair nights. At the current
vanilla clock those are nights 1–6, at 10, 30, 50, 70, 90 and 110 minutes. The
canonical requirement is the **alternation and the escalation**, not those
minute values, and the six nights do **not** define a match length: nothing in
canon ends a match on a clock. Victory remains the Fountain predicate.

The two systems are deliberately opposite in shape. A Worksite night pulls teams
toward **distributed** sites; a Lair night concentrates value at the **single
shared landmark**. That contrast is the point — it stops every important night
resolving to "go middle". Worksites are therefore never clustered around the
Lair.

**Post-Dragon cadence is OPEN.** The runtime reports UNSCHEDULED rather than
looping or repeating the last entry.

## Worksite tiers — Working, reconciled 23 September 2026

Each Worksite night carries a paired economic identity: a **Mining Site**
(Construction enables, Extraction pays off) and an **Industrial Factory**
(Logistics enables, Production pays off).

| Tier | Mining Site | Industrial Factory | Economic role |
| --- | --- | --- | --- |
| **I** | Iron + Coal | Blast Furnace + Smoker | bulk material / processing |
| **II** | Diamond + Lapis | Enchanting Table | strategic capital / enhancement |
| **III** | Ancient Debris + Diamond | Smithing Table | apex material / conversion |

Read down the columns, the progression is:

> Mining: **Iron + Coal → Diamond + Lapis → Ancient Debris + Diamond**
> Factory: **PROCESSING → ENHANCEMENT → APEX CONVERSION**

**The escalation is not volume.** Worksite I is higher-volume foundational
material; II is lower-volume, higher-value strategic capital; III is very-low-
volume apex conversion material. A later Mining Site may contain *fewer*
physical ore blocks while being far more consequential. Factory progression is
likewise processing → enhancement → conversion, not fast → faster → fastest.

[HISTORICAL — superseded 23 September 2026] The four-tier economic ladder
**Copper → Iron → Diamond → Ancient Debris**, paired with **Blast Furnace/Smoker
→ TBD intermediate Factory → Enchanting → Smithing**, is superseded by the three
tiers above, because the cadence has three Worksite nights. Specifically:

- the **Copper Worksite tier is removed**. Copper is abundant in the ordinary
  opening economy and its material identity is *replaceable / field-standard
  metal*; it does not need an exceptional injection to establish. This also fits
  Opening Hinterland doctrine, which wants fundamental Minecraft verbs available
  in the opening without the Hinterland resolving later strategic-resource
  questions. Copper's broader role in the world economy is **unchanged**;
- the **TBD intermediate Factory is removed rather than filled**. It was never
  defined, and no workstation is invented to occupy the slot;
- **Diamond + Gold** as the guaranteed Worksite II backbone is superseded by
  **Diamond + Lapis**, because Diamond is equipment capital, Lapis is the
  enchantment input, and the Enchanting Table is the conversion — the paired
  opportunities then speak one economic language without either being sufficient
  alone. Gold remains an ordinary-world resource and is not deleted;
- **Worksite III is no longer OPEN.** It is the constrained Netherite economy;
- the **Anvil** no longer appears in the Worksite II identity. [OPEN] Whether an
  Anvil belongs anywhere in the Factory progression is unresolved; it is not
  asserted here, and its earlier mention is not evidence that it was decided.

**Worksites are not tier permission.** Ordinary Extraction of Iron, Diamond and
Lapis remains possible throughout, and ordinary-world Ancient Debris opportunity
may exist where map/resource doctrine permits. Winning Worksite II must not turn
"we cannot participate in Diamond/enchanting" into "now we can"; it turns scarce
ordinary participation into a substantially stronger concentration of strategic
capital.

Worksite III is deliberately **networked**: Ancient Debris plus the existing Gold
economy plus existing Diamond equipment plus Logistics convergence, through the
Smithing Factory, yields limited Netherite upgrades. A team can hold the Mining
Site without the Factory, the Factory without Debris, or the chain without the
transport to use it. Netherite is premier late-game capital, not a universal
final equipment tier for the whole team.

**Specialization is systemic, not a multiplier.** The same ten physical Diamond
ore yield differently according to a team's Fortune/Yield investment. Do not
implement "Extractor gets +50% Worksite loot" or a "+20% quality" Factory perk:
the opportunity is physically shared, and prior specialization determines how
efficiently it is realized.

[NON-CANON / PROVISIONAL CALIBRATION — not balance, not worldgen quotas] Earlier
sensitivity work suggests ~40–50 physical Iron ore at tier I; ~8–12 Worksite
Diamond against ~14–18 ordinary (≈26 total, about one major Diamond investment
without Yield and approaching two with a strong Yield economy); ~8–12 Ancient
Debris at tier III against ~4–8 ordinary, giving roughly 3–5 Netherite upgrades
since Fortune does not multiply Debris; and ~6–10 secondary Diamond at tier III.
These are hypotheses retained for calibration. They must not become production
constants merely because they are numerically convenient. Exact Iron, Coal,
Diamond, Lapis, Ancient Debris and secondary-Diamond quantities, Factory
throughput, Factory supply thresholds, enchant behaviour, Smithing/Netherite
recipe treatment, whether vanilla Smithing Templates are retained, and the exact
Worksite count per tier all remain **[OPEN]**.

These are anchors, not a reward spec. The existing **Mining Outpost** and
**Industrial Enchanter** work is the specific form and is not replaced by free
grants; the runtime records the tier and its declared economic identity, and
reports the physical package as unresolved.

## Worksite activation is permanent — Canonical, 23 September 2026

**Sunset is an activation event, not an availability window.** This explicitly
supersedes every older rule, comment, config, test and documentation statement
saying that Worksites deactivate, close, reset or become dormant again at
sunrise. Sunrise has **no** deactivation effect.

The lifecycle is not `dormant → sunset → active during night → sunrise
deactivation`. It is:

> dormant → selected at its scheduled Worksite sunset → **ACTIVE** → remains
> active across every subsequent day/night cycle

**Mining Site:** DORMANT → *scheduled sunset activation* → ACTIVE / PROSPECTIVE →
*qualifying Construct* → CAPITALIZED / MANIFESTED → *physical extraction* →
PARTIALLY DEPLETED → DEPLETED. The site remains an active world location until
its finite exceptional opportunity is depleted. Resources left unmined at sunrise
stay there and stay contestable, and the manifestation is **never** regenerated
or replaced merely because another day/night cycle occurred.

**Industrial Factory:** DORMANT → *scheduled sunset activation* → ACTIVE /
EXPOSED → *qualifying Logistics supply* → CAPITALIZED / OPERATIONAL → PERSISTENT
ACTIVE FACTORY. Factories do not deplete like Mining Sites. Distinguish *ACTIVE*
from *currently SUPPLIED / OPERATIONAL*: an activated Factory with insufficient
inputs is still activated, and players can restore the supply relationship later.
No artificial expiration rule is introduced.

**Worksite geography therefore accumulates.** After Worksite III activates the
world may simultaneously contain partially depleted tier I Mining Sites, active
tier I Blast Furnace/Smoker Factories, partially depleted tier II Mining Sites,
active Enchanting Factories, and fresh tier III Debris sites and Smithing
Factories. Tiers are **not** mutually exclusive global phases, and a later
activation never deactivates or supersedes an earlier physical Worksite. Night 1
/ 3 / 5 identify *when* new Worksites activate, not how long they remain
available. Continued relevance is decided by remaining resources, current supply,
geography, player infrastructure, Routes and Practical Reach, strategic demand
and physical control — not by whether it is currently night.

This matters because the match should accumulate an economic history and an
industrial geography rather than replacing one Worksite phase with the next.

## Worksite capitalization and contest — Working

Activation, capitalization and exploitation stay three distinct layers, and
capitalization does not create ownership. Physical Worksite access, remaining
Mining Site resources, and Factory access and supply all remain contestable
after first capitalization.

The current **experimental** first-capitalization reward is **+1 permanent team
Infrastructure Slot**, to the first qualifying team — the first qualifying
Construct at a Mining Site, the first qualifying supply relationship at a
Factory. The intended reading is that bringing a new Worksite into the team's
economy expands the team's organizational capacity. It is permanent and is
separate state from current physical control.

The caveat is load-bearing: the project does **not** have a realized enough
Infrastructure model for the economic value or final semantics of Infrastructure
Slots to be considered settled, and very little Infrastructure gameplay exists
relative to the design architecture. Preserve the reward in documentation,
config and runtime seams; do not build large systems on assumptions about its
final value; treat slot counts, allocation, reassignment, saturation and
licensing semantics as unresolved; leave clean seams. See
[infrastructure.md](infrastructure.md).

## Worksites and the Lair remain distinct

Worksites are distributed, economic, drawn from multiple candidate sites and
activated over the match. The Lair is exactly one permanent conspicuous landmark
whose occupants arrive on schedule and whose defeat produces the paired
objective siege. Do **not** cluster Worksites around the Lair merely because
their nights alternate.

## Lair occupant lifecycle — Canonical

There is exactly **one** Giant Monster Lair in the world: a massive, permanent,
conspicuous landmark, in the same place all match. Its **occupant** changes,
not its location. It is not one Lair per monster, per team, or per night.

- A monster that survives its night **stays**, through dawn and through the
  Worksite night after it, until the next **Lair** night.
- At that Lair night it is **replaced**. Replacement is not a kill and earns no
  siege advantage.
- A monster that is killed leaves the Lair **dormant** — the place remains, the
  occupant does not — until the next scheduled occupant arrives.
- **Missing a monster does not stall the progression.** Players can miss an
  opportunity; the match clock continues.

Team knowledge of the Lair is simply whether players have found it. There is no
scheduled global reveal, and none is needed: every major monster appears there,
so Exploration and later Route investment keep persistent value.

Ordinary Minecraft modification of the Lair — building, mining, approaches,
defences, staging, terrain change — remains valid. Preparation that grants
advantage is strategy; geometry that trivially nullifies an encounter is a
**playtest/balance** concern, and no protections are invented ahead of it.

## Monster ↔ objective pairing — Canonical, effect OPEN

> **Giant → enemy Pillager Outpost · Ghast → enemy Nether Bastion ·
> Ender Dragon → enemy End Spike**

Killing the monster gives the victorious team a meaningful advantage toward
toppling the paired enemy objective. **How that advantage works is OPEN.**
Historical material described literal direct objective damage; that is not
restored. No damage percentage, HP debuff, defender reduction, structural
weakness, timed vulnerability window or signature-verb assistance is asserted.
The only requirement is that it should materially help the siege without making
direct objective interaction irrelevant. The runtime records the siege
opportunity and applies nothing.

---

# 17A. Day and Night Economy

**Working direction, 12 September 2026.** The working distinction is that **day is accumulation and productive time; night is opportunity and contestable time.** Day and night are pressures, not role-locking phases: players can fight in daytime and farm, build or produce at night. The system changes relative economic attractiveness rather than prohibiting actions.

During daytime, established world state compounds most efficiently. Constructs and Development systems operate at full intended efficiency, Routes give their full traversal benefit, Supply Lines their full distribution benefit, and ordinary extraction, production, development, logistics, construction and preparation are comparatively favoured. Teams are rewarded for improving and exploiting controlled world state.

Night is not simply a combat phase. It changes the relative economic environment: infrastructure becomes less efficient, hostile world pressure increases, Mob Swarms become stronger and more specialized and more valuable, a limited selection of Worksites activates, and teams are encouraged to leave stable economic loops and respond to temporary opportunities. PvP should emerge from overlapping demand for those opportunities and from weaker economic projection.

The conceptual loop is: day accumulates, builds, develops, extracts, produces, connects and distributes; at sunset limited Worksites activate, Mob Swarms transform to nighttime forms, and infrastructure enters its reduced-efficiency state; at night teams evaluate opportunities, provision, project outward, secure, exploit, contest, fight mobs and possibly players, and raid, defend, ambush or siege where worthwhile; at sunrise Swarms revert, infrastructure returns to full efficiency, and teams integrate nighttime gains into the daytime economy. [HISTORICAL — superseded 23 September 2026] This loop previously began the sunrise clause with "Worksites close"; activated Worksites now persist. See §17C.

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

The intent is multiple competing nighttime opportunities rather than one mandatory objective-spawn phase. A Forge activating in the east while a deep mountain Swarm becomes a high-value Stray encounter in the west should force a team to choose between contesting the Worksite, securing the Swarm, splitting, defending infrastructure, ambushing another team, or ignoring both.

---

# 17B. Regenerative Sources

**Established spatial clarification, 22 September 2026:** Strategic Depth is
Homebase/opening-relative and governs permitted richness/novelty; Regional
Character governs ecologically/geographically sensible kinds. Lower depth
supports less-rich opportunity in quantity/concentration **and** specialization;
deeper Wilderness may support larger concentrations, specialized kinds, or both.
The Homebase-supported region provides distributed useful opening opportunity,
not a nearby dominant full-team regenerative objective. Equal N/S species and
source counts are not required. Exact tiers, richness, species assignment and
recovery curves remain **Open**. See [maps.md](maps.md#competitive-spatial-contract--established-clarification-22-september-2026).

Current runtime Source/Kind/depletion/recovery/manifestation is implemented;
the generic depth/character authoring policy is not. Configured capacities and
recovery magnitudes are analytical fixtures, not that policy. Preserve the
runtime machinery. `RenewableKinds` intentionally has no depth/value/region.

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

**Working direction, 12 September 2026.** The broad mapping between archetypes and world systems is Construction with Extraction to the Mining Outpost; Logistics with Production to an industrial Worksite; Development with Combat to Regenerative Sources; and Exploration to every POI.

This is a mapping of which archetypal verbs each world system most strongly expresses. It is **not** ownership or exclusivity.

**Construction and Extraction — Mining Outpost.** The Mining Outpost represents exceptional physical resource opportunity. Extraction acquires exceptional resources; Construction interacts with, activates, stabilizes and establishes the useful physical site. Activating its machinery should create or reveal a genuine new exceptional deposit or extraction opportunity rather than pointing at ore that already existed and could have been mined accidentally. It remains a Worksite, not Extraction infrastructure.

**Logistics and Production — Forge / Industrial Worksite.** A Production counterpart to the Mining Outpost is wanted: where the Mining Outpost is an exceptional Extraction opportunity, this is an exceptional Production and transformation opportunity. Logistics fits because significant inputs must be supplied, outputs must be distributed, and throughput and transport matter; Production fits because inputs are transformed into unusually valuable outputs. [OPEN] The name is unresolved. "Forge" is attractive but may read as overly metal-specific, which may be acceptable if the Worksite is slightly abstract in edge cases for Production classes working in wood, glass or food. [OPEN] Exact industrial mechanics are unresolved. See also the Industrial Enchanter in section 14A, which is a Working Production Worksite and is not automatically this system.

**Development and Combat — Regenerative Sources.** Development and Combat share the regenerative-resource world system and have parallel but opposite relationships with recurring living systems: Development gains increasing value from nurturing and harvesting renewable living-world opportunities, Combat from overcoming renewable hostile-world opportunities.

**Exploration — every POI.** Exploration is intentionally cross-cutting and its value is not limited to one dedicated POI type. Every meaningful POI creates Exploration value through discovery, navigation, information, route planning, access, first arrival, connection, and knowledge of spatial opportunity. Exploration therefore does not need an exclusive paired Worksite to remain economically legible.

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

**Working canon update, 10 September 2026:** Shared recognition, persistence, physical counterplay, and connection eligibility are governed by [infrastructure.md](infrastructure.md). Objective-specific mechanics remain here. Operational Area defines legal connections for integration; it is not a general buff radius. Industrial Enchanter completion does not require a recognized Supply Line.

# 20. Relationship to Infrastructure

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

## Team Structure sequence — RESOLVED, 22 September 2026

Formerly open: whether Outpost → Bastion → End Tower was strictly sequential,
whether later structures were protected until earlier ones fell, and whether
Minecraft-native approaches could bypass layers.

**Resolved.** The ordering is **spatial, not mechanical**. All three defensive
objectives stand concurrently and each may be attacked at any point, subject
only to actually reaching it. Nothing makes the Bastion invulnerable until the
Outpost falls, or the Spike invulnerable until the Bastion falls.

For a team, moving from the midline toward its own Fountain:

> **midline → Pillager Outpost → Nether Bastion → End Spike → Aether Fountain**

The **ordinal is the invariant**. W–E position and N–S spacing are variable;
no straight lane, equal gaps, mirrored coordinates or matching surrounding
terrain are required. All three are **Wilderness** structures — none belongs
inside the opening Hinterland.

Bypassing an outer defense through alternate terrain, tunnelling, bridging,
Routes, an unusual approach or a coordinated expedition is **valid Minecraft
play**.

Current forms, superseding earlier geometry: the Outpost is the **watchtower
only** (not the cages, tents, log piles or wider compound); the Bastion is the
Bridge Bastion's **rampart/central body** with the projecting bridge removed
(not the older Treasure Room); and the third is the **End Spike** — obsidian
pillar, End Crystal, cage where applicable — **not** an End City tower and not
the generic End Tower geometry the repository currently builds. Exact retained
dimensions must be **measured** from the selected form, and have not been; see
`implementation/worldgen/terrain_harvest/objective_forms.py`, which refuses
certification rather than guessing.

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
- Worksite XP;
- Industrial Enchanter/Silo parameters, cargo behavior, and enchanting rules listed in section 14A.

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
- Mining Outpost being completed simply by submitting a defensible Construction project.
- Mining Outpost directly dispensing rare ores.
- Mining Outpost functioning as a Diamond chest.
- Mining Outpost automatically clearing/excavating valuable material.
- Supply Chain using baseline wheat/carrots/potatoes as unique rewards.
- Supply Chain proving that Supply Lines require physical traced paths.
- Worksites requiring arbitrary level locks when economic/material gating can perform the same function.
- One objective being required for every archetype.
- Player-supplied lapis as the Industrial Enchanter's preferred gate, or ordinary free resource cargo as its preferred transport model.
- A generic powered-production Forge or complicated crafting-order challenge as the preferred Production Worksite.
- Industrial Enchanter dependence on recognized Supply Lines.
- Logistics tiers defined by a prescribed chest-boat → rail → Ender technology ladder, or by hidden throughput-tier evaluation.
- Enchanting Table as a Construction apparatus candidate; enchanting identity belongs to the Production Worksite.

---

# 23. Current Working Summary

> **Team Structures give the enemy Minecraft-native siege problems.**

> **Major Encounters create high-stakes combat contests that can influence larger strategic objectives.**

> **Worksites create finite exceptional opportunities developed, contested, and exploited through Minecraft-native capability: Construction supports Extraction at Mining Outposts; Logistics supports Production at Industrial Enchanters.**

> **World Locations provide strategically valuable Minecraft geography without every important place needing to become a formal objective.**

> **Ordinary Minecraft activity remains a valid source of progression and strategic value between and around objectives.**

> **Objective methods should differ through their costs and consequences, not through a universal specialized-interaction XP hierarchy.**

> **Legitimate resource attainment grants XP directly; the XP system does not need to judge whether every acquired resource was ultimately used well.**

The objective system should concentrate conflict around meaningful Minecraft opportunities while preserving the broader sandbox as part of the competitive game.
## Strategic locations and surrounding traffic

**Established secondary map-design function, 12 September Worldgen and Initial System Balancing branch.** A POI, Worksite, team objective, village or important junction/crossing can act as Q: a reason for recurring entry into or passage through region MNOP. Evaluate the resulting Strategic Exposure along terrain and approach connections as well as inside the objective footprint. Q need not overlap a resource to make its exploitation or remote infrastructure contestable. Exposure is not universally monotonic with Strategic Depth, and low-exposure remote economic settlements remain legitimate. See [maps.md](maps.md#spatial-opportunities-and-initial-balancing).

This function does not make every Q a formal objective or regenerative resource. Existing Mining Outpost finite generated-ore behavior and Industrial Enchanter/Silo activation, finite-window and delivery contracts remain in force. Renewal of world opportunity does not add passive player income, reset Worksites automatically, or dispense objective rewards without their specified human work. Exact traffic modeling is Working; no universal Q radius or visitation rate is selected.
