# Map, Objective, and Lair Systems — Six Design Decisions

**Status:** Current design doctrine  
**Date:** 2026-09-23

This document records six connected design questions resolved during the current
map/compiler and objective-system design pass.

The intent is to distinguish settled conceptual design from implementation details
and tuning values that remain open.

---

# 1. What Happens After the Map Compiler Works?

## Question

Once the map compiler can reliably produce valid maps, how should those maps enter
actual matches?

In particular:

- What is the relationship between Map Types, map realizations, and the map draft?
- What information should players see?
- When should a physical map realization be selected?
- Can seeds be reused?
- What does `/moba match start` actually begin?
- How should map Scale and Resource Density relate to Map Type?

## Decision

The map compiler should operate primarily as an **offline foundry** producing a
READY inventory of certified map realizations.

Map generation should not normally occur on the player-critical path.

The broad lifecycle is:

    search viable Map Type candidates
    → compile
    → measure realization
    → classify discovered properties
    → certify
    → READY inventory
    → map draft
    → claim compatible hidden realization
    → instantiate match
    → spawn players

## Map Type

A Map Type defines constitutive geography and traversal character.

Examples include:

- Default
- Archipelago
- Underground
- Desert
- Sky Islands

Properties intrinsic to that geography should remain part of the Map Type rather
than becoming arbitrary orthogonal sliders.

For example:

- Archipelago is inherently water-dominated.
- Underground is inherently subterranean and vertically constrained.
- Sky Islands inherently involve separated elevated landmasses.

A "vertical Archipelago" or "watery Underground" parameter should therefore not
be invented merely to create more configuration axes.

## Scale

Scale is a separate authored/search property.

Working categories:

- Normal
- Large
- Vast

The current compiler work should be understood primarily as proving
**Default · Normal**.

Larger scales should preserve the broad duration and progression of a match rather
than simply multiplying travel distances. Increased scale should make systems such
as Exploration, Routes, and Logistics more consequential without turning a match
into prolonged walking.

A Small scale is not required unless later testing establishes a useful compressed
format.

## Resource Density

Resource Density is also separate from Map Type, but unlike Scale it is primarily
**discovered rather than requested**.

A seed finder should not request:

    Rich Default

and then author a Default map to become rich.

Instead:

    find viable Default candidate
    → compile candidate
    → measure natural economic opportunity
    → classify realization as Light / Normal / Rich

The exact labels remain open.

This means:

- a resource-poor Underground map is possible;
- a resource-rich Archipelago is possible;
- two Default maps may have materially different natural economies.

This variation is desirable map character rather than automatically a balance
failure.

Regenerative Sources and other authored opportunities guarantee the necessary
functional economy. Natural generation is then allowed to provide economic
variation above that floor, subject to progression and opening-ceiling rules.

Resource Density should eventually measure **practical natural economic
opportunity**, not simply raw block counts.

Relevant inputs may include:

- accessible mineral opportunity;
- useful cave access;
- naturally generated structures;
- surface material abundance;
- natural renewable/development opportunity.

Exact classification remains open.

## Map Realizations

A Map Type is not a physical map.

A realization is a particular compiled world arrangement. Its conceptual identity
includes factors such as:

- seed;
- region/origin;
- orientation;
- Map Type and version;
- Scale;
- selected Homebase sockets;
- objective and Lair siting;
- authoring/furnishing plan;
- authoring version;
- discovered Resource Density.

The same seed may therefore support multiple legitimate realizations.

A seed is **not consumed after one match**.

What should be prevented, where useful, is repeated use of the exact same known
realization. The realization space should be sufficiently combinatorial that this
can be handled cheaply without declaring entire seeds unusable.

## Rotation and Draft

The map rotation operates primarily on **Map Types**.

If five Map Types are currently in rotation, each team's presented map options
should collectively represent all five Types.

Multiple options of the same Type are desirable when they differ in meaningful
advertised properties.

For example:

    Default · Normal · Light
    Default · Large · Rich
    Archipelago · Normal · Rich
    Underground · Large · Light

This allows players to reason at several strategic levels:

- Map Type;
- Scale;
- natural Resource Density.

The draft does not need to expose exact physical geography.

Players should know broad strategic properties but should not know things such as:

- exact POI locations;
- exact caves;
- exact Homebase sockets;
- exact Lair position;
- exact objective positions;
- exact resource clusters;
- seed.

Thus players draft a **broad map option**, not a known hidden realization.

The final physical realization should normally be claimed only after the map draft
resolves.

Exact ban/counterpick order, final chooser rules, and class/map draft ordering
remain open.

## `/moba match start`

`/moba match start` should begin the **pre-match lifecycle**, not immediately
generate a world or teleport players.

Conceptually:

    /moba match start
    → MATCH CREATED
    → lock participants / teams
    → PRE-MATCH DRAFT
        → class selection (ordering TBD)
        → map option pools
        → bans / counterpicks
        → final map option
    → claim compatible READY realization
    → bind match systems
        → objectives
        → Lair
        → Worksites
        → teams
        → match clock
    → send players to Fountains
    → ACTIVE PLAY

Generation and expensive certification work should therefore happen before this
lifecycle whenever possible.

---

# 2. What Does Compiler Certification Actually Mean?

## Question

When the compiler accepts a map, should that map be called "balanced"?

How should compiler validation relate to actual competitive evidence from human
matches?

## Decision

**The compiler certifies validity, not balance.**

A map passing the compiler means that it satisfies every competitive, spatial,
economic, authoring, and physical requirement that the current compiler knows how
to test.

It does not mean that the map has been empirically proven competitively balanced.

A suitable lifecycle is:

    MapCandidate
    → CompetitiveCandidate
    → AuthoredCandidate
    → VerifiedMap
    → READY

`VerifiedMap` means:

> The realization satisfies the currently encoded map contract.

`READY` means:

> The verified realization is eligible to be claimed for a match.

Terms such as:

- BalancedMap;
- ProvenMap;
- empirically balanced;

should not be used as compiler statuses.

## Empirical Feedback

Actual matches test the assumptions encoded in the compiler.

The feedback loop is:

    VerifiedMap
    → human matches
    → observed strategy / pacing / exploits / asymmetries
    → discover missing competitive constraint
    → update recognizer / author / verifier
    → new compiler version

A playtest discovering a problem does not mean the previous compiler malfunctioned.
It may mean that the previous design contract was incomplete.

Map provenance should therefore preserve enough information to determine which
contract produced a realization, including at minimum the relevant compiler and
authoring versions.

Old match results remain interpretable under the contract used at the time.

## Difference Is Not Deficit

Compiler validity also must not imply homogenization.

A map may be:

- Resource Light;
- Resource Rich;
- geographically asymmetric;
- easier to traverse in one region than another;
- differently shaped around the two teams;

and still be valid.

The compiler's purpose is to determine whether these differences remain within the
rules of a competitive map.

It is not supposed to make every map equivalent.

---

# 3. How Are Defensive Objectives Toppled?

## Question

Each defensive objective supports three broad approaches:

1. signature interaction;
2. combat;
3. structural destruction.

How should these interact?

Should each be an independent binary completion condition? Should they fill an
abstract progress bar? What does "destroying enough of the structure" mean?

## Decision

A defensive objective represents a **finite defensive position protecting the
Aether Fountain**.

What is ultimately toppled is not merely the physical building. It is the
objective's **ability to continue defending the Fountain**.

Each objective therefore possesses a finite defensive capacity/reserve.

Conceptually:

    OBJECTIVE
       │
       ├── Physical Structure
       │       ↓
       │    supports
       │
       └── Defensive Capacity
               ↓
          deploys defenders
               ↓
          defenders die
               ↓
          replacements consume reserve
               ↓
          reserve exhausted
               ↓
             TOPPLED

The three approaches attack the same underlying defensive system through different
Minecraft-native verbs.

## Combat

Combat is a fully valid independent toppling method.

Objectives continually replenish defenders as defenders are killed, up to a finite
capacity.

This should resemble the logic of a Minecraft village Raid, but with the player
besieging the defensive position:

    defenders spawn
    → players kill them
    → objective replenishes them
    → replacements consume finite defensive capacity
    → additional waves
    → capacity exhausted
    → objective toppled

Thus "combat toppling" does **not** mean killing the handful of mobs initially
standing at the objective.

It means fighting through enough successive defenders to exhaust the objective's
ability to continue protecting the Fountain.

This should generally be the brute-force and comparatively expensive route.

## Defenders

Current defender identities are:

| Objective | Defender |
|---|---|
| Pillager Outpost | Pillagers |
| Bastion | Piglins |
| End Spike | Endermen |

These use the natural mob identity associated with each structure/dimension rather
than introducing bespoke objective soldiers.

The structures interact differently with their defenders:

- Pillagers benefit from the Outpost's elevation and cover but remain functional
  outside it.
- Piglins make strong use of the Bastion's navigable interior, making structural
  destruction particularly consequential.
- Endermen do not require an interior; the Spike acts as a monumental defended
  focal point.

## Structural Siege

Destroying the structure should reduce its ability to **supply and deploy
defenders**.

This avoids defining structure destruction merely as:

    each block = 1 objective damage

Instead, physical structure is mechanically relevant because it provides the
environment from which the defense operates.

As meaningful structure disappears:

- fewer useful defensive positions remain;
- fewer defenders may be supportable simultaneously;
- replenishment may become less effective;
- total remaining defensive capacity may fall;
- eventually the objective cannot sustain meaningful defense.

Conceptually:

    intact fortress
    → strong defensive supply

    breached fortress
    → weakened defensive supply

    heavily destroyed fortress
    → few viable defenders / little remaining reserve

    ruin
    → no meaningful defense
    → TOPPLED

Implementation may internally use a scalar defensive-capacity value, but player
interaction should remain grounded in visible world state rather than an arbitrary
objective-health interaction.

The system should avoid reducing structural play to finding and breaking a small
set of obvious "spawn blocks." Valid defensive deployment should ideally depend on
the remaining useful structure/geometry.

Exact structural evaluation remains an implementation and testing problem.

## Signature Siege

Each objective also has a signature interaction:

| Objective | Signature interaction |
|---|---|
| Outpost | Steal/remove the Allay |
| Bastion | Extract its Gold |
| End Spike | Break the End Crystal |

Rather than necessarily functioning as instant binary topples, these interactions
can inflict a **very large loss of defensive capacity**.

This lets the signature remain unusually efficient without turning it into a
disguised objective button.

The signatures may be mechanically asymmetric.

### Outpost

Securing/removing the Allay can produce a major one-time disruption.

The exact requirement may involve successfully removing or securing the Allay,
rather than merely opening its enclosure.

### Bastion

Gold naturally supports a more granular extraction model.

Extracting portions of its reserve can progressively weaken the Bastion, with
substantial extraction causing substantial defensive loss.

### End Spike

The Crystal is naturally a singular catastrophic target.

Breaking it can therefore produce one enormous defensive-capacity loss.

## Combined Approaches

The three approaches are not mutually exclusive completion modes.

A siege might proceed as:

    kill first defender wave
    → breach structure
    → reach signature target
    → inflict major signature loss
    → fight weakened replacement wave
    → topple objective

This is desirable.

Players are not selecting "the Combat solution" or "the Extraction solution."
They are conducting a Minecraft siege using whatever combination of:

- fighting;
- mining;
- demolition;
- infiltration;
- construction;
- resource expenditure;

the current world state rewards.

## Core Rule

> A defensive objective is toppled when its finite ability to defend the Aether
> Fountain has been exhausted.

Combat exhausts that ability through repeated defender replacement.

Structural siege destroys the physical infrastructure supporting that ability.

Signature play attacks an objective-specific source of defensive strength.

---

# 4. What Does Defeating a Lair Monster Do to an Objective?

## Question

The three Lair monsters correspond to the three defensive objectives:

- Giant → Outpost
- Ghast → Bastion
- Ender Dragon → End Spike

What does the "siege advantage" from defeating one actually mean?

## Decision

Do not create a separate abstract siege-buff system unless later testing requires
one.

Instead:

> Defeating a Lair monster causes that monster to perform a short, large-scale
> physical assault against the opposing paired defensive objective.

The boss uses a legible extension of its Minecraft-native destructive identity.

The attack causes real consequences through the existing objective system:

- physical structure is destroyed;
- defender-supporting geometry/capacity is reduced;
- current defenders may be killed or damaged;
- the objective remains physically altered afterward.

The reward therefore operates through the same siege verbs available to players,
but at exceptional scale.

## Giant → Outpost

The Giant physically assaults the enemy Outpost.

Its behavior should resemble an enormous extension of the Minecraft Zombie's
ability to break through a door or obstruction.

The Giant:

- reaches/appears at the target siege;
- smashes through a substantial portion of the watchtower;
- damages or kills Pillagers in its path;
- leaves the Outpost physically compromised.

The Giant does not need an unrelated "siege spell."

Its identity is simply:

> an enormous Zombie capable of smashing through structures.

## Ghast → Bastion

The Ghast bombards the enemy Bastion with a massive volley of fireballs.

This extends existing Ghast behavior:

- flight;
- ranged fireballs;
- explosions;
- fire;
- terrain destruction.

The bombardment can:

- blow open walls;
- damage floors and routes;
- expose interiors;
- damage or kill Piglins;
- leave fire and structural damage.

This is especially appropriate for the Bastion because its defensive identity
depends heavily on dense navigable structure.

## Ender Dragon → End Spike

The Ender Dragon assaults the enemy End Spike using recognizable Dragon behavior.

It can:

- fly through/around the objective;
- destroy physical structure through its passes;
- attack with Dragon Breath;
- damage or displace Endermen;
- dramatically alter access to the Crystal and remaining Spike.

This does not need to reproduce vanilla Dragon behavior one-to-one. It should remain
legible as an exaggerated application of the Dragon's existing identity as a
large-scale destructive flying creature.

## Not an Automatic Topple

The Lair reward should not normally be defined as:

    kill boss
    → paired objective automatically topples

Instead, it performs substantial **real siege work**.

An intact objective may become badly compromised.

An already weakened objective may be pushed close to collapse.

An objective that has already suffered extensive player siege might actually be
toppled by the boss attack because the physical damage and defender casualties
exhaust its remaining capacity.

Thus the same attack can have different consequences depending on actual world
state.

## Physical, Persistent Result

The result should be embodied in the world:

    current objective state
    + boss assault
    = new objective state

This allows teams to deliberately prepare an objective for a later Lair reward.

For example, a team may breach a Bastion before Night 4 knowing that securing the
Ghast could then devastate the already-compromised structure.

## Boss Travel

The boss does not need to navigate normally from the Lair all the way to the enemy
objective.

Requiring ordinary AI traversal across potentially Large or Vast maps would create
implementation and exploit problems unrelated to the strategic decision.

The siege event may stage the boss at the paired objective after the Lair victory.

Exact presentation remains open.

## Core Rule

> The Lair reward is not abstract objective damage. It is a monster doing to the
> objective, at enormous scale, something players could already accomplish through
> Minecraft's world interactions.

---

# 5. How Should Giant, Ghast, and Dragon Lair Encounters Work?

## Question

The world contains one permanent Giant Monster Lair.

Its scheduled occupants are:

- Night 2 — Giant
- Night 4 — Ghast
- Night 6 — Ender Dragon

How bespoke should these encounters become?

How should the same permanent Lair support three very different monsters?

How much should players be allowed to modify the arena?

## Decision

The Lair should not become an isolated MMO-style raid arena.

The encounters should preserve the monsters' Minecraft identities and let
complexity emerge from:

- terrain;
- preparation;
- construction;
- destruction;
- PvP;
- nighttime conditions;
- resource commitment;
- the accumulated physical history of the permanent Lair.

Bespoke boss mechanics should be added only where native behavior is insufficient.

## Shared Encounter Doctrine

All three encounters should follow these principles:

- The Lair remains part of the ordinary world.
- Enemy players can contest the encounter.
- Normal Minecraft equipment and preparation matter.
- Players can build, mine, dig, fortify, bridge, and otherwise modify the site.
- Earlier Lair fights can leave infrastructure and damage relevant to later ones.
- Difficulty should not rely primarily on enormous health pools.
- Scripted phases are not required merely because these are major objectives.
- Native or native-adjacent behavior should be preferred over invented boss spells.

The three encounters naturally escalate in how much 3D space they demand:

    GIANT
    ground control / melee / structural interaction

        ↓

    GHAST
    ground + air / cover / ranged explosive control

        ↓

    ENDER DRAGON
    full encounter volume / sweeping mobility / persistent area denial

## Giant — Night 2

The Giant is the clearest case where vanilla behavior is insufficient because the
vanilla Giant lacks a meaningful complete encounter behavior set.

The solution should not be to invent an unrelated boss kit.

Instead, treat the Giant as the behavior implied by an enormous Zombie.

It should be able to:

- pursue players;
- make dangerous large-scale melee attacks;
- navigate terrain sufficiently for its scale;
- break through limited obstructing geometry when necessary.

Its obstruction-breaking behavior belongs to the same conceptual family as a
hard-mode Zombie breaking a door, scaled to Giant proportions.

This also teaches the behavior later used in the Giant's Outpost siege.

Avoid adding, without demonstrated need:

- ranged magic attacks;
- arbitrary shockwaves;
- summoned minion phases;
- elaborate enrage phases;
- unrelated boss abilities.

Its encounter complexity should come from being a huge melee/structural threat
inside a PvP-contestable Minecraft environment.

## Ghast — Night 4

The Ghast already possesses most of the required vocabulary:

- flight;
- explosive ranged attacks;
- fireballs;
- projectile reflection;
- terrain damage;
- fire;
- aerial positioning.

The Lair must provide enough usable airspace for those properties to matter.

Alpha tuning may adjust:

- durability/health;
- fire rate;
- movement;
- attack scale;

but should not add fundamentally new verbs without evidence that the encounter
needs them.

Again, the encounter teaches the later siege behavior: the creature players just
fought through aerial bombardment subsequently bombards the Bastion.

## Ender Dragon — Night 6

The Dragon should remain recognizably Minecraft's Ender Dragon.

Its useful existing vocabulary includes:

- sweeping flight;
- charges/passes;
- destructive collision/movement;
- Dragon Breath;
- large-scale aerial repositioning.

The primary engineering challenge is separating whatever vanilla Dragon logic
assumes the End arena from the behavior needed for an Overworld Lair.

The Lair should **not** automatically reproduce the vanilla End fight.

In particular, End Crystals do not need to surround the Lair simply because they
exist in vanilla Dragon combat.

The Dragon is occupying the Lair; the Lair is not becoming the End arena.

## Player Modification

Player modification should generally remain legal.

Useful preparation can include:

- towers;
- tunnels;
- firing positions;
- Ghast cover;
- bridges;
- escape routes;
- barricades;
- staging areas;
- Routes;
- excavated terrain.

These are strategic Minecraft play.

The distinction should be:

> gaining advantage through world modification is allowed;

but:

> trivially nullifying the encounter because the AI cannot function is an
> encounter defect.

For example, if a simple enclosure permanently disables the Giant, the preferred
solution is likely to improve the Giant's ability to handle obstruction rather
than globally prohibit block placement around the Lair.

## PvP as Encounter Complexity

The Lair is contested space.

The practical encounter may therefore be:

    TEAM A
       ↘
        BOSS
       ↗
    TEAM B

    + modified terrain
    + nighttime threats
    + constructed positions
    + committed resources
    + retreat routes

This provides substantial strategic complexity without requiring each monster to
be redesigned as a conventional raid boss.

## Core Rule

> Begin with the minimum extensions necessary to make each monster function as a
> contested Minecraft encounter. Add bespoke behavior only in response to
> demonstrated encounter deficiencies.

---

# 6. What Belongs Inside the Opening Hinterland?

## Question

The Opening Hinterland must provide enough opportunity for every fundamental verb
without allowing natural generation to skip major progression.

How should the compiler determine what is acceptable?

Known current exclusions include:

- Villages;
- Carrots;
- enough accessible Iron to equip a player.

How should this generalize without creating an enormous arbitrary blacklist?

## Decision

The central opening rule remains:

> **The Hinterland must permit every fundamental verb, but should not resolve any
> of them.**

The opening ceiling should therefore be based on **strategic consequence**, not
simple resource rarity.

The relevant question is:

> Does immediately receiving this opportunity substantially bypass a cost,
> expedition, Development process, or progression decision that should occur later?

## Working Taxonomy

Opening opportunities can be classified into four broad categories.

### Opening-Safe

Ordinary opportunities necessary to let Minecraft's fundamental verbs function.

Conceptual examples include:

- basic wood;
- ordinary stone;
- modest coal;
- modest basic mineral opportunity;
- ordinary animals;
- basic food opportunity;
- ordinary cave entrances;
- ordinary terrain/material variety.

These provide capability without resolving the associated economic problem.

### Opening-Limited

Resources/opportunities that are acceptable in modest amounts but become
progression-compressing when too concentrated or accessible.

Iron is the clearest current example.

Some accessible Iron means:

> Extraction is available.

Enough immediately accessible Iron to equip a player means:

> an economic/progression step has effectively been skipped.

The compiler should therefore reason about practical accessible concentration,
not merely presence/absence.

### Wilderness Opportunity

Opportunities strategically dense enough that securing them should require leaving
the compact opening envelope.

Known current examples:

- Village;
- Carrot.

Many substantial POIs may eventually belong here.

A Village is the canonical example because it is not merely a single resource. It
packages many opportunities together:

- structures;
- inhabitants;
- crops;
- beds;
- paths;
- economic/trading potential;
- other useful materials.

### Context-Dependent

Some opportunities depend heavily on Map Type and surrounding geography.

This category prevents Default's opening rules from accidentally becoming universal
rules for:

- Underground;
- Archipelago;
- Desert;
- Sky Islands;
- future Map Types.

Something exceptional on Default may be fundamental to another Map Type.

## Judge Opportunities, Not Blocks

The compiler should eventually reason in terms of **practical opportunity**.

A cave entrance is not inherently progression-breaking.

A cave entrance that immediately exposes an enormous mineral network may be.

A lava block is not inherently progression-breaking.

A highly accessible resource configuration that immediately unlocks an otherwise
costly production chain may be.

The conceptual evaluation is therefore:

    world feature
    → practical accessibility
    → quantity / concentration
    → strategic capabilities unlocked
    → opening classification

This is preferable to maintaining a huge blacklist of Minecraft block IDs.

## POI Test

For naturally generated structures and other dense opportunities, retain the
existing test:

> **If this generated immediately outside Homebase, would a knowledgeable team
> materially reorganize its opening around securing or exploiting it?**

If yes, it is probably Wilderness-only.

This clearly excludes Villages.

It clearly does not exclude trivial terrain features such as an ordinary pond.

Other structures—such as ruined portals, shipwrecks, Trial Chambers, mineshafts,
geodes, and similar generation—should be classified when compiler/worldgen work
actually reaches them rather than speculatively canonizing the entire Minecraft
structure set in advance.

## Renewables Are Distinct

Natural generation, regenerative manifestation, and player Development are three
different things:

    natural generation
        ≠
    regenerative manifestation
        ≠
    player Development

Regenerative Sources are deliberate game-authored world opportunities.

A regenerative crop Patch may therefore exist even where that crop does not
normally generate wild in vanilla Minecraft.

However, an explicit opening-ceiling restriction should still be respected by
authoring unless the resource doctrine intentionally changes it. For example, if
Carrots are currently excluded from the Opening Hinterland because of their
progression significance, the compiler should not simply author a carrot Patch
there to bypass that rule.

A Patch must remain a world manifestation rather than pre-developed
infrastructure:

    world manifestation = resource occurrence
    player Development = productive infrastructure

Thus a Patch should be irregular and terrain-conforming rather than resembling a
finished farm.

## Relationship to Resource Density

The opening floor and ceiling make discovered Resource Density possible.

The compiler guarantees:

- sufficient opening functionality;
- required authored regenerative opportunities;
- progression integrity.

Natural generation is then allowed to provide different levels of economic
opportunity within those constraints.

Thus both:

    Default · Normal · Light

and:

    Default · Normal · Rich

may be legitimate certified maps.

The Rich map does not need to be normalized downward merely because another
Default realization is poorer.

The Light map does not need to be artificially filled until its natural economy
matches the Rich one.

This extends the broader doctrine:

> **Difference is not deficit.**

## Current Explicit Default Exclusions

The known explicit Opening Hinterland ceiling currently includes:

1. **Villages**
2. **Carrots**
3. **Enough practically accessible Iron to equip a player**

This list is intentionally extensible.

Other opportunities should be classified as actual compiler/worldgen work
encounters them.

## Core Rule

> Reject or relocate natural opportunities whose immediately practical value
> substantially bypasses an intended expedition, progression step, Development
> process, or strategic cost.

Evaluate those opportunities by:

- accessibility;
- concentration;
- capabilities unlocked;

rather than block identity alone.

---

# Consolidated Doctrine

These six decisions establish a connected map-to-match system.

## Map Foundry

The compiler searches for naturally suitable geography, minimally authors the
necessary competitive/game-specific systems, measures the resulting realization,
and places certified realizations into READY inventory.

It certifies compliance with the current design contract, not empirical balance.

## Map Draft

Players choose between broad strategic map options defined primarily by:

- Map Type;
- Scale;
- discovered Resource Density.

Exact geography remains hidden until the match begins.

## Defensive Objectives

Outpost, Bastion, and End Spike are finite defensive positions protecting the
Aether Fountain.

They can be besieged through combinations of:

- combat;
- structural destruction;
- signature interaction.

All three methods attack the objective's finite ability to continue defending.

## Lair Rewards

Giant, Ghast, and Ender Dragon do not grant abstract siege buffs.

They physically assault their paired enemy objectives:

- Giant → Outpost;
- Ghast → Bastion;
- Ender Dragon → End Spike.

Their attacks perform large-scale versions of the same destructive actions players
can already perform.

## Lair Encounters

The permanent Lair remains mutable Minecraft terrain rather than a protected raid
arena.

Boss complexity should emerge primarily from:

- native monster behavior;
- terrain;
- player construction/destruction;
- preparation;
- PvP;
- accumulated world state.

Bespoke mechanics should be introduced only when testing establishes that native
behavior is insufficient.

## Opening Economy

The compact Opening Hinterland guarantees access to every fundamental verb without
solving those verbs for the player.

The compiler enforces a floor of functionality and a ceiling against progression
compression.

Within those bounds, natural economic variation is legitimate map character.

Together these systems preserve the broader map-authoring principle:

> **Homebase is controlled space. Wilderness is competitive space.**

and the broader competitive principle:

> **Functional opportunity is balanced; the world itself does not need to be
> mirrored or homogenized.**