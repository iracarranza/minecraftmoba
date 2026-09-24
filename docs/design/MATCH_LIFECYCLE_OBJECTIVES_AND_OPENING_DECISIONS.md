# Match Lifecycle and Objective-System Decisions

**Status:** Current design decisions  
**Date:** 2026-09-23  
**Scope:** Six design questions resolved in parallel with the Default-map compiler work.

This document records the questions and answers reached for the post-compiler map lifecycle, map certification, defensive-objective toppling, Lair siege rewards, Lair encounters, and the Opening Hinterland ceiling. It is a decision record rather than an implementation specification: tuning values and implementation details explicitly left open below remain open.

---

## 1. What happens after a map compiles?

### Question

How should compiled maps enter matches? What constitutes map uniqueness? What information should players receive before play, and how should future map drafting interact with the map foundry?

### Decision

A **seed is not consumed by use in a match**. Map uniqueness is the combination of the natural world and the authored/furnished realization built from it. The exact same seed may therefore support multiple valid realizations, including different regions, orientations, Map Types, or authored configurations.

The useful hierarchy is:

- **Seed:** Minecraft world-generation input.
- **Map Type:** the geographic/topological grammar a candidate must satisfy, such as Default, Archipelago, Underground, or a future Desert type.
- **Map Scale:** a broad spatial class. Current future-facing candidates are Normal, Large, and Vast; Small is not required merely to complete the scale.
- **Resource Density:** a **discovered property**, not an authoring target. A valid natural realization is measured and labeled according to its practical natural economic opportunity.
- **Map Realization:** seed + selected region/origin + orientation + Map Type/version + sockets + authored/furnished contents + authoring/compiler provenance.
- **Match Instance:** one match using a claimed realization.

The system should prevent known exact-realization reuse where desired, but it should not maintain a list of permanently consumed seeds. Combinatorially, exact realization collision should already be rare.

### Map Type, Scale, and Resource Density are distinct

Map Type defines the kind of geography. It is not synonymous with economic richness.

Therefore:

- a resource-poor Underground map is valid;
- a resource-rich Archipelago is valid;
- a watery Underground or a vertically defined Archipelago should not be produced merely by treating water/verticality as independent sliders, because those characteristics belong to the geographic grammar of the Type.

**Resource Density is not requested from the author.** The seed finder/compiler discovers a valid realization and measures its natural economic character. Two Default maps can both satisfy the competitive contract while one is resource-light and another resource-rich because of differences such as villages, practical cave access, natural ore concentrations, and other useful natural opportunities.

Game-authored regenerative sources do not erase this variation. They establish required renewable/world opportunities. The compiler guarantees a functional floor and progression integrity; it does not normalize natural abundance above that floor.

Resource Density should eventually measure **practical economic opportunity**, not merely count raw blocks. Accessibility, concentration, cave access, useful structures, and capabilities unlocked matter.

Map Scale is also independent, but Large/Vast recognition does not need to interrupt the current Normal Default compiler. Later scale support must preserve broad match flow rather than merely multiply coordinates and walking distances.

### What players draft

The long-term system may support an arbitrarily large catalog of Map Types (for example, roughly twenty), of which a subset is in the current **rotation**.

All in-rotation Map Types should be represented among the teams' offered map picks. A Type may appear more than once with different discovered/scale characteristics. For example, the same draft can legitimately contain:

- Default · Normal · Resource Light
- Default · Large · Resource Rich
- Underground · Normal · Resource Light
- Archipelago · Large · Resource Rich

Duplicate Types are useful rather than redundant because their broad strategic character can differ.

Players should know the **Map Type, Scale, and Resource Density**. They should not receive the particular seed, exact geography, POIs, Lair location, objective surroundings, renewable-source locations, or other realization-specific discoveries.

The information boundary is:

> **Players know the broad map classification; they discover the realization.**

No additional advertised property is currently needed. Candidate properties such as traversal, verticality, water prevalence, openness, and ruggedness are substantially consequences of Map Type and/or Scale. POI and specific opportunity information would reveal too much of the realization.

### Draft and match lifecycle

The future draft is a **Map Type/map-option draft**, not a draft of visible generated worlds. A possible mature format is:

1. A larger catalog exists.
2. A subset of Map Types is in rotation.
3. Each team receives a set of options in which all in-rotation Types are represented.
4. Teams ban/counterpick until a small finalist set remains.
5. One team ultimately selects the played option.
6. Only then is a compatible hidden READY realization claimed.

**Class-first, then map — Working, 24 September 2026.** The draft order is settled: each team drafts its classes, and only then do teams ban and counterpick across the Map Types currently in rotation. This supersedes the previous open choice between class-first, map-first and interleaved.

The consequence is that map bans are made with **both** compositions known, so a Type is banned or counterpicked for how it suits the classes already drafted rather than in the abstract. Map drafting therefore reads as a response to composition, not a separate contest.

Still open: exact ban order, overlap between the teams' option sets, and final-choice rights.

`/moba match start` should therefore **begin the match and its pre-match process**, not immediately teleport players to their Fountains. The broad lifecycle is:

```
/moba match start
    -> create/lock match and teams
    -> class draft
    -> map ban/counterpick across the Types in rotation
    -> map option selected
    -> claim compatible READY realization
    -> instantiate/bind match systems
    -> send players to Fountains
    -> start active play / match clock
```

The eventual foundry should maintain enough hidden READY realizations to support the currently draftable Types/options. Initially it can discover valid maps, classify them, and inventory what exists rather than demanding an equal Cartesian stock of every Type × Scale × Resource Density combination. Later it may preferentially search for underrepresented categories without authoring a naturally poor map into a rich one.

Core principle:

> **The compiler guarantees playability. The seed supplies character. The author preserves that character wherever it does not violate the competitive contract.**

---

## 2. What does compiler acceptance mean: PlayableMap, VerifiedMap, or “balanced”?

### Question

When the compiler accepts a map, should the project call that map balanced? How should later human playtesting relate to compiler certification?

### Decision

**The compiler certifies validity, not demonstrated balance.**

A map that satisfies every currently encoded competitive, spatial, economic, authoring, and physical requirement is a **VerifiedMap** and may become **READY** for match use.

A useful pipeline is:

```
MapCandidate
    -> CompetitiveCandidate
    -> AuthoredCandidate
    -> VerifiedMap
    -> READY
```

“Balanced” should not be a per-map compiler status. It would imply empirical certainty that the compiler cannot establish.

Human matches test the competitive contract itself:

```
VerifiedMap
    -> human play
    -> pacing / strategy / exploit / outcome evidence
    -> discover missing or incorrect constraint
    -> update recognizer / author / verifier
    -> new compiler version
```

Every realization should retain compiler/authoring version and provenance so an old match remains interpretable under the contract that certified it.

Do not add statuses such as BALANCED, PROVEN, or TESTED merely because a map passed automated validation.

---

## 3. How do defensive objectives topple?

### Question

All three defensive objectives support a signature verb, combat, and structural destruction. Are those three unrelated completion buttons, contributions to an abstract progress bar, or parts of one coherent siege system?

### Decision

A defensive objective is toppled when its **finite ability to defend the Aether Fountain has been exhausted**.

The objective is a finite defensive position. It can replenish defenders as they die, up to its remaining capacity, in a manner conceptually analogous to fighting through the waves of a village raid. Combat is therefore a true independent toppling method: a team may win a prolonged siege by repeatedly defeating replacements until the objective can no longer furnish meaningful defense.

The three approaches attack the same functional system:

### Combat

Defenders respawn/replenish as they are killed, consuming the objective's finite defensive supply. Fighting through enough waves can exhaust the objective and topple it without requiring the signature interaction.

This must be substantially harder than merely killing the defenders initially present.

Current defender identities:

- **Outpost:** Pillagers
- **Bastion:** Piglins
- **End Spike:** Endermen

These reinforce the structures' distinct spatial identities: Pillagers exploit elevation/cover; Piglins strongly use the Bastion's navigable interior; Endermen defend an exposed monumental focal point without requiring an interior.

### Structural siege

The physical structure supports the objective's ability to furnish and deploy defenders.

Destroying meaningful structure therefore reduces its defensive capability. Sufficient destruction can leave:

- fewer valid defensive positions;
- fewer simultaneous defenders;
- slower or impossible replenishment;
- less remaining defensive supply;
- ultimately too little defensible structure to continue protecting the Fountain.

This is intentionally a health-like system **by proxy**, not “every block deals one objective damage.” Decorative/random block breaking should not be equivalent to destroying usable defensive infrastructure.

The implementation should prefer the remaining physical geometry and usable defensive positions over a hidden percentage of total original blocks where feasible. It should also avoid reducing the solution to breaking a handful of known “spawn-pad” blocks.

### Signature siege

The signature interaction is an exceptionally efficient attack on the same defensive system: a **massive hit to defensive capability**, not necessarily an unrelated instant-win button.

The signatures remain physically distinct:

- **Outpost:** steal/remove the Allay.
- **Bastion:** extract its Gold; this may naturally support progressive weakening as more of the reserve is removed.
- **End Spike:** break the End Crystal; naturally a singular catastrophic event.

The three signatures need not have identical magnitudes or interaction shapes.

### Combined siege

The strongest property of the model is that the approaches combine.

A team might:

- fight through an initial wave;
- demolish a wall or floor to reduce the structure's defensive ability;
- reach and compromise the signature target;
- finish the weakened remaining defenders.

Players are therefore conducting a Minecraft siege rather than choosing one of three separate objective minigames.

The internal implementation may use a scalar defensive capacity/reserve if useful, but the player-facing system should be expressed primarily through world state, defenders, replenishment, physical damage, and signature state rather than an arbitrary objective-HP UI.

---

## 4. What advantage does each Lair monster grant against its paired objective?

### Question

After defeating Giant, Ghast, or Ender Dragon, how should the victory weaken the paired enemy objective? Earlier material left this as an abstract “advantage/weakening” seam and historical designs included direct objective damage.

### Decision

Do **not** create a separate percentage siege buff or arbitrary direct-damage effect.

Each defeated monster performs a brief, large-scale version of the same Minecraft-native structural/combat siege that players can already perform.

- **Giant -> enemy Outpost:** the Giant smashes through a substantial portion of the watchtower, analogous in legibility to a hard-mode Zombie breaking a door but at Giant scale. The assault can destroy real blocks and strike/kill Pillager defenders.
- **Ghast -> enemy Bastion:** the Ghast launches a major fireball bombardment, physically breaching/damaging the Bastion and damaging Piglins caught in the attack.
- **Ender Dragon -> enemy End Spike:** the Dragon makes destructive passes through/around the Spike and uses dragon breath, damaging physical structure/access and attacking or displacing Endermen.

The behavior need not be a literal 1:1 copy of vanilla AI. It should be a legible scaling of the monster's Minecraft identity.

The result is persistent world state:

```
current objective geometry
+ current defenders
+ monster siege action
= damaged objective geometry and defensive state
```

The monster attack should normally be **massive progress, not an automatic scripted topple**. However, because it acts on the objective's real current state, it may topple an objective that players have already weakened.

This permits deliberate setup: players can damage an objective before a Lair night and then seek the paired monster so its siege assault can capitalize on that damage.

The monster does not need to path normally from the Lair across the entire map to the objective. The siege event may stage/manifest the defeated monster at the paired objective; exact presentation remains an implementation/art-direction question.

Core principle:

> **The Lair reward is not abstract objective damage. It is a monster doing to the objective, at enormous scale, something players could already accomplish through Minecraft's world interactions.**

---

## 5. What should the Giant, Ghast, and Dragon Lair encounters actually be?

### Question

Vanilla Giant lacks meaningful designed AI, and an Overworld Dragon may depend on End-arena assumptions. How bespoke should the Lair encounters become?

### Decision

Start with **minimal extensions of Minecraft-native monster identities**. Do not begin by designing three MMO raid bosses.

The permanent Lair is a shared, modifiable world location. Encounter complexity comes from:

- the monster;
- natural 3D terrain;
- enemy-team PvP;
- nighttime threats;
- equipment/resource commitments;
- routes and staging;
- player construction and destruction;
- modifications accumulated during earlier Lair fights.

Player modification is generally legitimate preparation. Towers, tunnels, cover, firing positions, bridges, escape routes, and staging areas should remain allowed unless testing shows a construction pattern **nullifies the encounter** rather than merely creating an advantage. If boxing a boss switches the encounter off, prefer fixing boss interaction/geometry over globally prohibiting building.

### Giant — Night 2

Treat the Giant as an enormous Zombie.

It should at minimum:

- pursue players;
- make dangerous large-scale melee attacks;
- navigate terrain adequately for its size;
- break through limited obstructing world/player geometry in the conceptual family of a hard-mode Zombie breaking a door.

That destructive vocabulary also teaches the behavior later used in its Outpost siege.

Do not initially add ranged attacks, magic shockwaves, summon phases, or other unrelated boss mechanics unless playtesting demonstrates a need.

### Ghast — Night 4

The vanilla vocabulary is already strong:

- flight;
- explosive ranged fireballs;
- projectile reflection;
- terrain/fire consequences;
- aerial positioning.

The Lair must provide enough usable airspace for those behaviors. Tuning such as health, firing cadence, or attack scale can be adjusted for the encounter, but fundamentally new verbs are not initially required.

Its later Bastion bombardment is a scaled expression of the same behavior players just fought.

### Ender Dragon — Night 6

Keep the Dragon recognizably Minecraft's Dragon:

- sweeping flight;
- charges/passes;
- collision/destructive movement;
- dragon breath;
- large-scale aerial repositioning.

The engineering problem is to make the Dragon operate correctly in an Overworld Lair without assuming a vanilla End arena. The Lair does **not** need to recreate the End fight or surround the Dragon with healing End Crystals merely because vanilla does so.

The Dragon naturally supplies the final encounter's full-volume escalation.

### Encounter escalation

The three bosses progressively demand more of the same Lair:

```
Giant
  ground control / melee / obstruction

Ghast
  ground + air / cover / ranged bombardment

Dragon
  full 3D volume / mobility / destructive passes / area denial
```

PvP is a major part of the encounter's strategic depth. The actual event can be both teams contesting the same monster while also fighting each other in terrain they have previously modified. This is a reason to avoid overloading the monsters themselves with bespoke mechanics.

---

## 6. What belongs below or above the Opening Hinterland's progression ceiling?

### Question

The Hinterland must let players perform fundamental Minecraft verbs without resolving those verbs for them. How should the compiler distinguish acceptable opening resources from opportunities that skip intended progression?

### Decision

Retain the core rule:

> **The Opening Hinterland must permit every fundamental verb, but should not resolve any of them.**

The ceiling is not fundamentally a blacklist of rare blocks. The compiler should ask:

> **Does immediately practical access to this natural opportunity substantially bypass an intended expedition, progression step, Development process, or strategic cost?**

Evaluate an opportunity using:

- practical accessibility;
- quantity/concentration;
- the capabilities it immediately unlocks;
- whether it compresses several opening decisions/verbs;
- whether knowledgeable teams would reorganize the opening around securing it.

### Working taxonomy

#### Opening-safe

Ordinary opportunities needed for Minecraft's fundamental verbs without resolving them, such as basic wood/stone, modest basic extraction, ordinary animals/food opportunity, and ordinary terrain/cave access that does not itself deliver exceptional value.

#### Opening-limited

Resources that may be present but become progression-skipping at sufficient accessible concentration.

**Iron is the canonical case:** some accessible iron permits Extraction; enough immediately accessible iron to equip a player resolves too much of the opening economy.

The compiler should therefore reason about practical concentration rather than merely presence/absence.

#### Wilderness opportunity

An opportunity strategically dense or valuable enough that finding, reaching, and contesting it should require leaving the compact opening envelope.

Known explicit Default cases:

- **Village**
- **Carrot**

The existing POI test remains useful:

> **If this generated immediately outside Homebase, would a knowledgeable team materially reorganize its opening around securing/exploiting it?**

If yes, it is probably a Wilderness opportunity.

Do not prematurely canonize every Minecraft structure into this category without examining the actual game version and its interaction with MOBA systems.

#### Context-dependent

An opportunity whose role depends strongly on Map Type or surrounding generation. This prevents Default's opening ceiling from accidentally becoming a universal law for future Archipelago, Underground, or other Map Types.

### Opportunities, not block IDs

The eventual classifier should reason approximately as:

```
world feature
    -> practical accessibility
    -> quantity / concentration
    -> strategic capabilities unlocked
    -> opening classification
```

A cave entrance is not inherently too valuable; one providing immediate access to an extraordinary exposed mineral network may be. A single lava source is not automatically progression-breaking; a conveniently accessible combination that collapses an intended production/progression cost may be.

### Renewables

Keep these concepts distinct:

```
natural generation
!= regenerative manifestation
!= player Development
```

Regenerative Sources are game-authored world opportunities and may exist even where a resource does not naturally generate in vanilla. They establish functional renewable opportunity without turning the site into developed infrastructure.

However, the opening ceiling still applies to authored opportunities: if carrots are currently an explicit opening exclusion, authoring a carrot Patch inside that same compact Hinterland would defeat the purpose of the rule unless the resource doctrine is deliberately changed.

This relationship also explains how maps can have different Resource Density:

> **The opening floor is guaranteed. Natural abundance above it supplies map character. The opening ceiling prevents that character from skipping progression.**

A Resource-Light Default and a Resource-Rich Default can both be valid VerifiedMaps.

---

# Consolidated decisions

The six decisions fit together as one broader model:

1. **Map lifecycle:** the foundry produces hidden unique realizations; players draft broad classifications, not revealed worlds. Seeds are reusable inputs, not consumables.
2. **Certification:** the compiler certifies a map as valid/Verified, not empirically “balanced.”
3. **Objectives:** objectives are finite defensive systems toppled by exhausting their ability to defend the Fountain through combat, structural siege, signature attacks, or combinations thereof.
4. **Lair reward:** paired monsters physically perform exceptional structural/combat siege rather than granting an abstract damage buff.
5. **Lair encounters:** preserve and scale Minecraft-native monster behavior; let terrain, PvP, preparation, and player modification create much of the complexity.
6. **Opening ceiling:** guarantee fundamental verbs, preserve natural variation, and reject opportunities that immediately collapse intended progression.

Together these preserve a common project principle:

> **Prefer Minecraft-native world state, verbs, and emergent strategic consequences over parallel abstract MOBA systems when the Minecraft systems can carry the mechanic.**

# Explicitly open

This record does **not** decide:

- exact map-draft order/rules or class-draft relationship;
- exact rotation size;
- final names/bounds for Resource Density;
- final Normal/Large/Vast dimensional contracts;
- exact defender counts, wave counts, replenishment cadence, or defensive-capacity formula;
- exact structural-integrity/spawn-position algorithm;
- exact magnitude of signature attacks;
- exact monster siege magnitude/targeting/presentation;
- final Giant AI tuning;
- final Ghast/Dragon encounter tuning;
- exhaustive Default or cross-Map-Type opening-opportunity classification;
- final empirical balance thresholds.

Those should remain tuning, implementation, or later design questions rather than being inferred from this decision record.
