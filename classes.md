# Minecraft MOBA — Classes

Current design state: September 8, 2026

This document is the authoritative class-design reference for Minecraft MOBA.

It contains:
- the current archetype framework;
- current established and working class designs;
- class-specific progression where already defined;
- known class → world/resource dependencies;
- unresolved class-design questions.

Exact numerical balance, cooldowns, costs, durations, radii, and other implementation values remain open unless explicitly stated otherwise.

---

# 1. Class Design Philosophy

Minecraft is the base systemic language.

Classes should alter how players interact with recognizable Minecraft systems rather than replacing Minecraft with an unrelated MOBA ability vocabulary.

Ordinary Minecraft actions remain broadly available to everyone.

A player does not need the relevant archetype merely to:
- fight;
- travel;
- mine;
- build;
- craft;
- farm;
- carry resources.

Instead, classes and archetypes provide exceptional:
- efficiency;
- potency;
- systemic interaction;
- specialization.

## Efficiency

Perform approximately the same useful task for less:
- time;
- input;
- resource expenditure;
- risk;
- effort;
- logistical burden.

## Potency

Perform:
- more;
- a stronger version;
- a larger-scale version;
- or something an ordinary player could not normally accomplish through that system.

A specialist should substantially outperform a generic player in their domain without making the specialist mandatory for interacting with that domain at all.

---

# 2. The Seven Archetypes

The current archetypes are:

1. Combat
2. Exploration
3. Construction
4. Extraction
5. Production
6. Development
7. Logistics

Archetypes describe systemic domains, not rigid subclasses.

A class may combine multiple archetypes.

Classes do not need to express every possible mechanic associated with their archetype.

---

## Combat

Exceptional manipulation of:
- damage;
- health;
- survivability;
- mitigation;
- sustain;
- control;
- other combat efficiency and potency.

Universal Health progression does not invalidate Combat specialization.

---

## Exploration

Exceptional manipulation of:
- movement;
- traversal;
- expedition efficiency;
- sprinting;
- jumping;
- terrain interaction;
- mobility.

Early Exploration should generally favor movement efficiency over unconditional movement potency.

Raw movement speed, high jumps, and similar effects can create disproportionate strategic tempo and should therefore be introduced cautiously.

---

## Construction

Exceptional manipulation of:
- block placement;
- construction;
- world geometry;
- conversion of carried material into useful built world.

One current Construction scaling vocabulary concept is **Block Efficiency**: producing more useful placed geometry from a fixed amount of carried material.

Block Efficiency is not necessarily a universal passive shared by every Construction class.

---

## Extraction

Exceptional useful:
- removal;
- access;
- acquisition;
- detection;
- excavation

of world materials.

Extraction does not simply mean that blocks disappear.

Destroying an enemy structure is not inherently Extraction; the resource/access purpose matters.

---

## Production

Exceptional transformation of acquired inputs into useful outputs.

Possible expressions include:
- input efficiency;
- output quantity;
- output quality;
- processing speed;
- recipe flexibility;
- alternate production access;
- byproducts;
- batching or automation.

Production does not universally mean crafting refunds or Salvage.

Kitfighter's Salvage is one class-specific expression of Production.

---

## Development

Exceptional improvement or maturation of productive world states.

Potential domains include:
- crops;
- renewable systems;
- animal populations;
- productive terrain;
- sustainability;
- advanced cultivation;
- persistent growth/transformation.

A useful conceptual distinction is:

Production:
wheat → bread

Development:
crop/farmland system → more reliable/productive wheat

Development and Construction have adjacent vocabulary and will often interact or become colocated.

However:
- Construction does not require Development.
- Development does not require Construction.
- Development does not necessarily operate inside Constructs.

Their relationship is synergy, not prerequisite.

---

## Logistics

Exceptional manipulation of:
- carrying;
- storage;
- transport;
- transfer;
- distribution;
- resource availability.

The central Logistics question is:

> How effectively can the team's resources become available where and when they are needed?

Logistics should not be reduced to merely having a larger inventory.

---

# 3. Class Draft Format

Classes are generally documented through:

- Core idea
- Archetypes
- Passive
- Ability 1 + upgrade choices
- Ability 2 + upgrade choices
- Ultimate
- Build directions / modularity where useful
- World/resource dependencies
- Open questions

Not every class must use exactly the same upgrade structure.

Class-specific progression thresholds do not automatically establish a universal progression cadence.

---

# 4. Mole

## Status

Established core with unresolved secondary-archetype classification and balance details.

## Core idea

Uses the ground itself for movement, information, extraction, and combat.

## Primary archetype

**Extraction**

## Other possible archetypes

- Exploration
- Combat
- Logistics

These remain possible secondary classifications rather than finalized secondary archetypes.

---

## Passive — Sifth Sense

Detects players moving on sand and gravel.

Scales with level through:
1. increased detection radius;
2. increased detection rate;
3. increased location specificity.

Sifth Sense is an existing example of class-specific Extraction progression through unusual resource/environmental detection.

World systems such as Worksites should preserve meaningful specialist advantage for Sifth Sense without making Mole mandatory.

---

## Ability 1 — Tunneling

Toggle Mole into a dedicated digging mode.

While active:
- Mole cannot attack;
- Mole cannot build;
- current digging-tool speed is amplified;
- moving into a wall automatically excavates a 1×2 player passage;
- shifting directs tunneling downward;
- jumping directs tunneling upward.

### Upgrade choices

**Speed**
- Dig significantly faster.

**Width**
- Dig in a wider radius.

**Stability**
- Negate knockback while Tunneling is active.

---

## Ability 2 — Drill Rush

Allows Mole to walk into walls or the floor.

On reactivation, Mole emerges and deals damage in the direction they are facing.

### Upgrade choices

**Armored Emergence**
- Gain massive damage reduction when emerging.

**Undermine**
- Emerging from the floor stuns enemies.

**Burrow Chain**
- Gain speed when exiting a wall.
- Drill Rush's cooldown resets if Mole enters a new section of wall within a short window.
- Failing to do so causes the normal cooldown.

---

## Ultimate — Sinkhole

After a delay, open a massive hole in targeted ground.

- Block destruction is real and persistent rather than a temporary visual/state change.
- Enemies inside cannot build for several seconds.

The Ultimate currently has no upgrade branch.

---

## Possible build directions

Working labels only:

- Extraction / route creation
- Exploration / information
- Bruiser / engage
- Control
- Hybrid combinations

These are not fixed subclasses.

---

## World/resource dependencies

Mole currently creates meaningful worldgen demand for:
- sand;
- gravel;
- excavation opportunities;
- caves;
- geology;
- underground resources.

The world should contain enough physical geological variation for Mole's extraction and information mechanics to matter.

---

## Worksite interaction

Mining Worksites should follow the principle:

> The apparatus prospects/reveals. Players excavate.

Generic players must remain capable of exploiting a Mining Worksite.

Mole should substantially outperform generic players through:
- search;
- excavation;
- route selection;
- extraction efficiency;
- resource detection.

A Worksite system should not automatically reveal/excavate so much information or terrain that Sifth Sense and Tunneling lose their value.

---

## Open

- Final secondary archetype(s).
- Sifth Sense exact detection radius/rate/specificity progression.
- Tunneling amplification values.
- Tunneling material/tool interactions.
- Drill Rush timing, damage, range, and cooldown.
- Sinkhole size, delay, duration of build prevention, and counterplay.
- Exact interaction with protected/objective terrain.
- Exact Worksite interaction.

---

# 5. Gardener

## Status

Established core with working upgrade themes and unresolved secondary-archetype classification.

## Core idea

Cultivates plants that turn parts of the map into useful environments for allies, combining renewable development with exploratory/ecological utility.

## Primary archetype

**Development**

## Other possible archetypes

- Exploration
- Support / Combat

These remain possible secondary classifications rather than finalized secondary archetypes.

"Support" is descriptive vocabulary here, not one of the seven current archetypes.

---

## Broad upgrade tendencies

Gardener currently has two emerging thematic modes plus generalist choices.

### Backyard

Familiar, renewable, sustainable plants.

### Exotic

Unusual plants with specialized effects.

### Generalist

Quantity, flexibility, or effects that do not need to belong to either mode.

These are not rigid subclasses.

Mixed builds are allowed.

---

## Passive — name TBD

Repeatedly planting the same plant increases the growth speed of that plant.

This is a direct Development expression:
repeated cultivation improves the productivity/maturation of a renewable world state.

---

## Ability 1 — Spread Vines

Grow a vine on the targeted block.

### Upgrade choices

**Grapevines / Backyard**
- Vines become renewable grapevines.
- Allies can interact with them to restore some hunger.

**Jungle Vines / Exotic**
- Allies gain Jump Boost and Speed after climbing them.

**Offensive Vine / name TBD**
- Enemies take damage over time while climbing the vines.

---

## Ability 2 — Flowerpot

Periodically summons flowers in the targeted area.

Teammates can interact with the flowers to consume them for a small amount of regeneration.

### Upgrade choices

**Double Bushes / Backyard**
- Regeneration lasts longer.

**Torchflowers / Exotic**
- Grant Night Vision and Absorption.

**Generalist / Charges**
- Add additional charges to Flowerpot.

---

## Ultimate — Glistening Greenhouse

Turn the nearby area into a greenhouse.

Allies receive periodic Instant Health while near allied plants inside the greenhouse.

Exact:
- duration;
- area;
- healing rate;
- plant persistence;
- counterplay

remain unresolved.

---

## World/resource dependencies

Gardener creates meaningful worldgen demand for:
- plants;
- crops;
- renewable resources;
- ecological variation;
- farmland;
- water;
- development space;
- unusual/ecology-specific plants where appropriate.

Default's world/resource design should contain enough renewable and ecological vocabulary for Development classes such as Gardener to interact with the actual Minecraft world rather than relying exclusively on ability-created resources.

---

## Development-system relationship

Gardener should remain independently functional.

Gardener does not require:
- a Construct;
- a Construction class;
- a predefined Development zone

in order to use Development mechanics.

Gardener may naturally become stronger or more strategically useful when allied infrastructure is colocated with developed plant systems, but that is synergy rather than a prerequisite.

---

## Open

- Passive name.
- Exact growth-speed scaling.
- What counts as "the same plant" for Passive tracking.
- Growth-speed cap.
- Grapevine Hunger values and renewability behavior.
- Jungle Vine buff duration/strength.
- Offensive Vine name and damage behavior.
- Flowerpot generation rate, area, charges, and persistence.
- Exact Double Bush / Torchflower values.
- Glistening Greenhouse duration, area, healing rate, persistence, and counterplay.
- Final secondary archetype classification.
- Exact relationship to generic Development systems.

---

# 6. Golem Master

## Status

Working draft.

## Primary archetype

**Construction**

## Secondary archetype

**Combat**

---

## Core idea

Golem Master converts terrain into commanded golems, then uses those golems to convert carried materials back into useful structures.

**Animate:**
world blocks → golem

**Assemble:**
inventory blocks + golem labor → structure

Iron, Snow, and Copper are upgraded independently across Animate and Assemble, allowing mixed builds rather than locking Golem Master into one form.

---

## Passive — [Unnamed]

- Pumpkins periodically spawn/grow nearby.
- Maximum commanded golems increases with level.

---

## Ability 1 — Animate

Target terrain and consume blocks from the area at a tool-dependent rate.

Once enough material has been consumed, reactivate and consume a Pumpkin to form the stored material into a temporary golem.

Golem:
- level;
- health;
- damage;
- other properties

scale from the amount and relevant vanilla metadata of the blocks consumed.

Relevant properties may include:
- hardness;
- explosion resistance;
- preferred tool.

Golems can incidentally destroy and collect blocks.

Copper specializes in doing so intentionally.

### Iron

- More expensive / less efficient to form.
- Additional health.
- Increased attack knockback.

### Snow

- Ranged projectile attack.
- Projectiles apply Slowness.

### Copper

- More efficient / cheaper to form.
- Smaller and faster.
- Can wield different weapons.
- Best at intentionally destroying and collecting blocks.

---

## Ability 2 — Assemble

Consume blocks from inventory and command nearby golems to construct a wall at the targeted location.

Construction speed scales with participating golem health and level.

Assemble always spends actual carried blocks.

### Iron Wall

- Reinforced.
- Grants nearby Resistance.
- Golems receive a stronger Resistance benefit.

### Snow Wall

- Projectiles fired from behind/near the wall apply additional Slowness.
- Can enhance player, Snow Golem, and weapon-bearing Copper Golem projectiles.

### Copper Wall

- Constructed significantly faster.
- Nearby allied golems gain Speed.
- Nearby allied players gain Haste.

---

## Ultimate — Wither Golem

Create a temporary Wither Golem.

The Wither Golem is non-destructive to blocks.

Exact duration and combat behavior remain open.

---

## Build modularity

Animate and Assemble forms are selected independently.

| Animate / Assemble | Iron Wall | Snow Wall | Copper Wall |
| --- | --- | --- | --- |
| **Iron Golem** | Fortress / maximum defensive presence | Durable frontline for a ranged-control position | Mobile heavy golems / bruiser workforce |
| **Snow Golem** | Protected ranged battery | Maximum ranged control | Mobile/kiting ranged force |
| **Copper Golem** | Mobile workers with defensive staying power | Mobile weapon users exploiting a slowing firing position | Maximum work/extraction/construction tempo |

These are possible synergies, not fixed subclasses.

---

## World/resource dependencies

Golem Master creates meaningful worldgen/resource demand for:
- iron;
- snow;
- copper;
- pumpkins;
- arbitrary building blocks/materials.

Default therefore guarantees meaningful map-level access to snow through the western high-altitude cold ecology.

Iron and copper are part of the physical ore geography.

Pumpkins and other relevant ecological resources must remain available enough for the class to function, although their exact generation treatment remains to be finalized.

Because Animate consumes actual terrain and Assemble consumes actual carried blocks, Golem Master's class economy should remain connected to the physical Minecraft world.

---

## Construction-system relationship

Golem Master is a Construction class but does not imply that all Construction classes use:
- golems;
- walls;
- Animate;
- Assemble;
- the same Block Efficiency system.

Likewise, generic Construction progression should not make Golem Master's class-specific construction identity redundant.

The class's exceptional ceiling comes from converting:
- terrain into combat/workforce;
- workforce + inventory material into rapid functional construction.

---

## Open

- Passive name.
- Pumpkin generation rate.
- Command-capacity progression.
- Animate minimum material.
- Block-property formulas.
- Exact Iron/Snow/Copper construction-efficiency differences.
- Golem duration.
- Handling of collected materials on golem death/expiry.
- Assemble wall dimensions.
- Targeting.
- Material cost.
- Wall permanence.
- Snow Wall Slowness behavior.
- Copper Wall Haste/Speed values and radius.
- Wither Golem duration.
- Wither Golem exact combat behavior.
- Interaction with generic Construction progression and recognized infrastructure.

---

# 7. Kitfighter

## Status

Current documented class direction.

## Primary archetype

**Combat**

## Secondary archetype

**Production**

Do not currently classify Kitfighter as Extraction.

---

## Core idea

Uses crafted equipment and deployable utility as a flexible combat kit, creating equipment more efficiently through Salvage and temporarily exceeding the normal sustainable equipment curve through Covered With Diamonds.

Kitfighter's Production identity is economic:

> convert ordinary material acquisition into combat equipment more efficiently than a generic player.

Its Production mechanics do not imply that all Production classes should use crafting refunds.

---

## Passive — Salvage

Crafting equipment using at least **3 units of a Primary Material** grants:

**1 Salvage for that material.**

Salvage is material-specific rather than generic.

### Recovery thresholds

| Level | Salvage required |
| --- | ---: |
| 0 | 6 |
| 8 | 5 |
| 16 | 4 |
| 24 | 3 |

When the threshold is reached, consume the required Salvage to recover:

**2 units of that same Primary Material.**

These level thresholds are specific to Kitfighter.

They do **not** establish a universal:
- 0/8/16/24 progression cadence;
- 1/8/16/24/30 class cadence;
- global ability-upgrade schedule.

---

### Ability 1 — Offhander

Right-click to equip a bow in Kitfighter's offhand.

#### Upgrade choices

**Crossbow** — Replace the bow with a crossbow.

**Fishing Rod** — Replace the bow with a fishing rod.

**Golden Head** — Replace the bow with a Golden Head.

---

### Ability 2 — Hotswap

Deploy a web at the targeted block that breaks after a short duration.

#### Upgrade choices

**Cobweb** — The deployed web persists much longer.

**Lava** — Deploy lava instead of the temporary web.

**TNT** — Deploy TNT instead of the temporary web.

---

## Ultimate — Covered With Diamonds

Temporarily upgrades Kitfighter's armor/tools to Diamond.

When the Ultimate ends, the equipment reverts.

Current overdrive combat bonuses:
- knockback resistance;
- movement speed.

The exact Ultimate unlock level remains unresolved.

Its unlock timing should eventually be calibrated against:
- universal Health/Hunger/Inventory progression;
- the point at which players approach vanilla physical capability;
- ordinary iron-equipment availability;
- Diamond scarcity;
- Worksite/resource progression;
- overall match economy.

Do not lock the Ultimate to Level 16 merely because approximately Level 16 is currently used as the conceptual vanilla-capability landmark.

---

## Equipment economy

Current broader equipment direction relevant to Kitfighter:

Iron should be the highest combat-equipment tier that an ordinary player can routinely and sustainably attain.

This should arise from the resource economy rather than an arbitrary equipment restriction.

Diamond remains:
- legal;
- vanilla-powerful;
- obtainable;
- exceptional.

Permanent Diamond equipment should represent substantial strategic investment.

Diamond can compete with other uses such as:
- Worksite cores;
- infrastructure;
- tools;
- other strategic crafting.

Natural Diamond may remain possible but scarce.

Mining Worksites can provide reliable exceptional concentrations.

If ordinary matches consistently result in everyone wearing full Diamond, diagnose the resource economy before simply banning Diamond equipment.

This distinction is central to Kitfighter:

**Salvage**
- bends the economic cost of equipment production.

**Covered With Diamonds**
- temporarily accesses exceptional equipment without permanently injecting a free Diamond set into the economy.

---

## World/resource dependencies

Kitfighter cares about:
- ordinary equipment materials;
- iron;
- higher-value materials;
- crafting access;
- resource economy;
- exceptional material opportunities.

The world should therefore support material scarcity and opportunity strongly enough that Salvage creates meaningful economic advantage.

---

## Open

- Exact Offhander mechanics for Bow, Crossbow, Fishing Rod, and Golden Head.
- Exact Deployable Utility mechanics for Cobwebs, persistent webs, Lava, and TNT.
- Resource/ammunition relationships.
- Cooldowns and durations.
- Covered With Diamonds unlock level.
- Ultimate duration.
- Ultimate scaling after unlock.
- Exact knockback-resistance and movement-speed values.
- Interaction between temporary Diamond equipment and existing enchantments/durability.
- Exact definition of Primary Material for every eligible equipment recipe.
- Salvage edge cases and crafting-validation rules.

---

# 8. Class ↔ World Design Contract

The world should contain enough meaningful material and ecological vocabulary that:

1. Every class can care about multiple world resources.
2. Every meaningful world resource ideally matters to at least one class or system.
3. Common resources should often matter to multiple systems/classes.
4. A class should not become nonfunctional because a fundamental required resource randomly failed to generate.
5. Guaranteeing a required resource does not mean making it abundant, mirrored, fixed, or close to spawn.

Known class/world relationships currently include:

| Class | Important world/resource vocabulary |
| --- | --- |
| Mole | Sand, gravel, excavation, caves, geology, underground resources |
| Gardener | Plants, crops, renewable resources, ecological development, farmland, water |
| Golem Master | Iron, snow, copper, pumpkins, arbitrary blocks/materials |
| Kitfighter | Equipment materials, iron, exceptional materials, crafting/resource economy |

These relationships should inform world generation without turning the map into a set of class-specific resource stations.

---

# 9. Class ↔ Universal Progression

Match progression currently runs from Level 1 to Level 30.

Broad power curve:

Early game:
- significantly constrained Minecraft character.

Midgame:
- approximately complete / vanilla-capable Minecraft player.

Late game:
- increasingly supernormal class/archetype expression.

Level 30:
- highly realized class.

Approximately Level 16 is currently a conceptual vanilla-capability landmark, not a locked threshold.

Universal progression currently considers:
- Health;
- Hunger;
- Inventory.

Generic progression choices may accelerate or specialize these capacities.

Class/archetype progression establishes the exceptional ceiling.

Therefore:

> Universal progression establishes the floor.
>
> Specialization creates differentiation above or ahead of that floor.
>
> Class/archetype progression establishes the exceptional ceiling.

A generic progression choice should not reproduce an entire class identity.

Examples:
- generic Health progression does not replace Combat;
- generic Hunger progression does not replace Exploration;
- generic Inventory progression does not replace Logistics;
- generic construction capability does not replace Construction classes.

---

# 10. Infrastructure and Classes

Progression can gate systemic recognition and exceptional infrastructure benefits without gating normal Minecraft behavior.

Anyone can:
- build;
- travel;
- place storage;
- farm;
- transport items.

Specialized systems may recognize:
- Constructs;
- Routes;
- Supply Lines;
- Development systems/areas;
- other infrastructure.

These systems may interact with classes, but they should not automatically become mandatory prerequisites for a class.

In particular:

- Development does not require Constructs.
- Development does not require Construction.
- Construction does not require Development.
- Routes and Supply Lines are distinct.
- Supply Lines are currently envisioned as start-storage → end-storage distribution connections.
- Player-recognized Routes are currently envisioned as being established through traversal.
- Constructs do not currently have a settled mandatory XP-multiplier mechanic.

---

# 11. Current Class Roster Status

## Current documented classes

### Mole
Primary:
- Extraction

Secondary classification:
- unresolved among Exploration / Combat / Logistics possibilities.

### Gardener
Primary:
- Development

Secondary classification:
- unresolved; Exploration and Support/Combat vocabulary currently appears in the draft.

### Golem Master
Primary:
- Construction

Secondary:
- Combat

### Kitfighter
Primary:
- Combat

Secondary:
- Production

Kitfighter should not currently be listed as Extraction.

---

# 12. Major Cross-Class Open Questions

- Whether every class ultimately has exactly one Primary and one Secondary archetype.
- How secondary archetypes are assigned when a class touches several domains.
- Whether archetype labels themselves provide shared mechanics or primarily classify class-specific mechanics.
- Exact class progression cadence.
- Relationship between universal level choices and class ability upgrades.
- Ultimate unlock timing.
- Whether every class uses upgrade branches in the same structural way.
- How much class progression should be automatic versus chosen.
- Exact shared vocabulary for archetype efficiency and potency.
- How generic infrastructure progression interacts with class-specific infrastructure.
- How class/world resource dependencies are guaranteed without making world generation deterministic or class-specific.
- How specialist superiority is preserved without making specialists mandatory.
- Production ↔ Extraction conceptual adjacency remains intentionally deferred.