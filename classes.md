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


## Archetypes and strategic roles

Archetype and strategic role are separate design dimensions.

An archetype describes which Minecraft systems a class manipulates exceptionally.

A strategic role describes how that class converts its capabilities into useful team contribution during a match.

The seven archetypes therefore do not need to map one-to-one onto conventional MOBA roles, and every archetype does not need to support every possible role equally well.

Instead, each archetype should support multiple strategically legitimate forms of contribution. Individual classes author the intersection between their Minecraft-system archetypes and their intended match roles.

For example:

- Construction can support territorial, wilderness, or ally-enabling play through different uses of recognized construction;
- Development can support an economic carry pattern by scaling productive world states, or support allies by directing productive value toward them;
- Exploration can support roaming and resource discovery, but can also support allies through information, access, expedition efficiency, and rapid intervention;
- Logistics can create personal or team economic value through resource availability and distribution.

These are role-expression examples rather than requirements that every class belonging to those archetypes provide every listed playstyle.

## Infrastructure recognition and integration

**Working canon update, 10 September 2026:** [infrastructure.md](infrastructure.md) owns shared Infrastructure Mode, retroactive recognition, the extent/relation and constitutive/facilitative matrix, Construct spatial metrics, Development Zones, Route proof, and directional Supply Line proof/persistence. Class eligibility remains authored per class. See the [explicit reconciliation](docs/reconciliation/2026-09-10-infrastructure.md).

A useful current design model connects four archetypes to four forms of recognized infrastructure:

| Archetype | Strategic infrastructure expression | Primary question |
| --- | --- | --- |
| Construction | Constructs | How efficiently can the team maintain useful presence at this place? |
| Development | Development Zones | How productive or mature can this place become? |
| Exploration | Routes | How efficiently can players repeatedly traverse this connection? |
| Logistics | Supply Lines | How effectively can resources become available across this connection? |

This is a design model rather than a rigid taxonomy.

The adjacency between these archetypes is intentional rather than necessarily evidence that they should be merged.

Ordinary Minecraft behavior remains universal. Progression can instead gate **systemic recognition** and exceptional infrastructure effects.


### Infrastructure progression breakpoints

The current working infrastructure progression begins at Level 6.

**Level 6 — Infrastructure specialization / recognition entry**

Level 6 grants the class's first authored infrastructure specialization or recognition capability. Ordinary Minecraft actions remain available before this point; the unlock concerns systemic recognition and exceptional infrastructure effects rather than permission to build, travel, develop, store, or otherwise interact with the world.

Infrastructure eligibility is authored per class rather than automatically inherited from archetype tags.

The earlier infrastructure model used one recognized infrastructure slot at this initial unlock. One slot remains the current working starting capacity rather than a finalized balance value.

**Level 12 — Infrastructure role expression**

Level 12 develops the Level 6 infrastructure specialization through an authored branch defining how that infrastructure completes or reinforces the class's strategic playstyle and team role.

The purpose of this breakpoint is not merely to provide a generic numerical Infrastructure II upgrade. A Level 12 branch can change what strategic job the infrastructure is especially good at performing: for example, territorial control, wilderness operation, ally enablement, or economic/personal scaling.

Exact mechanics remain class-specific and must also respect the global rules of the relevant world and match systems.

Examples discussed during development, not established mechanics:

- a wilderness-oriented Construction branch could increase effective Construct radius when established in the Wilderness;
- a support-oriented Construction branch could sacrifice some of the owner's progression benefit to increase the progression benefit received by an ally using the Construct.

The exact definitions of Wilderness, infrastructure use, XP transfer or multiplication, and effective radius are not established by these examples.


An earlier working progression model placed generic infrastructure upgrades at Levels 6, 12, 21, and 27.

That generic Infrastructure I / II / III / IV cadence is superseded by the current authored progression model.

Current infrastructure-related progression is instead framed as:

- Level 6 — infrastructure specialization / recognition entry;
- Level 12 — infrastructure role expression;
- Levels 20 and 25 — advanced class-authored rewards that may, where appropriate, further transform or empower the class's existing infrastructure specialization.

Levels 21 and 27 currently have no established universal infrastructure reward.


### Advanced class-authored progression

Levels 20 and 25 are current working advanced-reward breakpoints.

These rewards are authored for the individual class rather than generated automatically from its archetype tags.

An advanced reward may provide:

- an exceptional personal capability;
- an exceptional form of an ordinary Minecraft action;
- a powerful extension of an existing class mechanic;
- a powerful extension of the class's existing infrastructure specialization.

Advanced progression does not automatically grant unrelated infrastructure systems. A class that receives Routes through its authored progression does not therefore gain Supply Lines merely because Logistics is also among its archetype classifications.

Vanilla-native effects such as status effects, enchantment behaviors, attributes, and other systemic capabilities remain candidate design vocabulary for these rewards, but individual advanced rewards are not yet universally assigned.

Level 30 remains the capstone breakpoint; its universal design grammar is not yet settled.

Infrastructure associations with archetypes are design vocabulary, not automatic progression inheritance.

A class's authored progression explicitly determines which infrastructure system, if any, it can recognize and develop. Two classes sharing an archetype do not therefore need to receive the same infrastructure system or infrastructure progression.

This avoids requiring the game to support every possible combination implied by a class's archetype tags. For example, an Exploration-associated class does not automatically receive Routes merely because Exploration is among its archetypes.

### Infrastructure integration

Recognized infrastructure should also reward **connection**.

The current working direction is that a Construct can act as a spatial anchor for an integrated infrastructure center.

A Construct alone does **not** automatically grant an XP multiplier.

Instead, distinct recognized infrastructure connected to or established within the relevant Construct area can contribute toward an **infrastructure-integration XP multiplier** on otherwise legitimate XP-generating activity performed there.

Potential contributing systems currently include:

- Development infrastructure;
- a connected Route;
- a connected Supply Line.

The purpose is to reward teams for turning independently useful infrastructure into a connected center of activity without requiring all useful play to occur at a base.

World opportunities naturally encourage dispersion.

Infrastructure integration creates a competing incentive toward intentional concentration.

Each infrastructure type should be capable of improving the strength of its own integration contribution through later progression rather than making Construction solely responsible for the strength of the network-wide reward.

Exact contribution values and progression thresholds remain unresolved.

A current first-pass balance shape for later testing is:

| Integration state | T1 infrastructure | T2 infrastructure |
| --- | ---: | ---: |
| One distinct connected system | ~1.025× XP | ~1.05× XP |
| Two distinct connected systems | ~1.055× XP | ~1.10× XP |
| Three distinct connected systems | ~1.10× XP | ~1.15× XP |

These are balance targets, not established final values.

The important intended shape is:

- no integration multiplier from a Construct alone;
- modest value from the first connection;
- increasing value from distinct-system integration;
- approximately 10% as an initial fully integrated T1 target;
- approximately 15% as an initial fully integrated T2 target.

The multiplier modifies legitimate XP-generating activity. It does not create passive or AFK XP.

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

**Economic role, 12 September 2026.** Combat's economic role is **securing value under threat**: it lets a team operate economically where danger or opposition would otherwise make operating inefficient or impossible. This refines the earlier phrasing that Combat converts threats and contestation into value. Combat does not require a dedicated Combat infrastructure system; its regenerative world system is hostile Mob Swarms, shared with Development's renewable living-world systems. See [objectives.md](objectives.md#combats-economic-role).

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

### Infrastructure expression — Routes

**Working, late September 2026.** Banners are Exploration's native infrastructure anchor: a Route is designated between two Banner endpoints and evaluated from demonstrated traversal. See [infrastructure.md](infrastructure.md#banner-endpoints).


A current strategic infrastructure expression of Exploration is the **Route**.

Ordinary Minecraft travel and road-building remain available to every player. Progression may instead grant the ability and capacity to designate or recognize Routes that provide exceptional traversal benefits.

The current early Route direction follows the broader Exploration rule:

> movement efficiency before movement potency

A recognized Route should initially improve repeated traversal primarily through sprint/traversal efficiency rather than unconditional movement speed.

Possible mechanical axes available to authored class progression include:

- Route slots/capacity;
- maximum length;
- width;
- branching or network complexity;
- traversal-efficiency strength;
- eventually, more cautiously, movement potency.

The current working establishment concept is that the player physically traverses the desired path and that traversal defines the Route. Exact submission, validation, width, branching, and topology remain unresolved.

Routes are distinct from Supply Lines.

A Route answers:

> How efficiently can players repeatedly travel through this connection?

A Supply Line instead concerns the distribution of actual resources.

---

## Construction

Exceptional manipulation of:
- block placement;
- construction;
- world geometry;
- conversion of carried material into useful built world.

One current Construction scaling vocabulary concept is **Block Efficiency**: producing more useful placed geometry from a fixed amount of carried material.

Block Efficiency is not necessarily a universal passive shared by every Construction class.

### Infrastructure expression — Constructs

A current strategic infrastructure expression of Construction is the **Construct**.

Ordinary Minecraft building remains unrestricted. Construct progression does not determine whether a player is allowed to build; it determines whether and how much player-built world state can receive systemic recognition as Construction infrastructure.

Authored class progression may grant or improve:

- Construct designation;
- Construct slots/capacity;
- maximum recognized size;
- recognized influence area;
- intrinsic Construct benefits;
- other Construct-specific properties.

Because useful building can occur before Construct recognition is unlocked, the system should support retrospective designation of qualifying existing construction rather than requiring all recognized construction to be built after entering a special mode.

Construct Slots, Buildable Scale, and Operational Scale are separate constraints. Buildable Scale governs recognized physical extent/material; Operational Area governs where Development Zones, Route points, and Supply Line nodes may legally connect for integration. Recognition is architecturally permissive and evaluates current qualifying construction; exact boundaries and thresholds remain [OPEN]. See [infrastructure.md](infrastructure.md#constructs).

#### Intrinsic value

Constructs should provide useful strategic value independently of other infrastructure.

The current design direction is that Constructs improve some form of **occupation or sustainment efficiency**:

> How efficiently can the team maintain useful presence at this place?

The exact mechanic remains unresolved.

Possible expressions include recovery, provisioning, hunger/exhaustion, or other costs associated with maintaining presence. These are examples rather than established mechanics and should not cause Constructs to subsume Development or other archetype domains.

This intrinsic benefit is important because not every useful Construct should be an economic base.

A mid- or late-game player may establish a Construct at:

- a bridge;
- a chokepoint;
- a Worksite;
- a forward position;
- an objective approach;
- another temporary or strategically important location.

Such a Construct should be immediately useful even if it never becomes a fully developed economic center.

Additional Construct slots/capacity allow established home infrastructure and later strategic positions to coexist rather than forcing players to abandon earlier useful construction.

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

### Infrastructure expression — Development Zones

Development recognizes a **Development Zone** through designation and measurement of eligible developmental resources and their enabling conditions, independently of Constructs. The current-state and restoration rules are in [infrastructure.md](infrastructure.md#development-zones).

The current direction is localized improvement of eligible persistent Development processes, potentially including:

- crop growth;
- animal or renewable-resource maturation;
- productive terrain;
- other persistent growth or transformation systems.

This should not currently be documented as a generic increase to Minecraft random tick speed. The intended mechanic is targeted acceleration or improvement of eligible Development processes.

Development infrastructure must remain independently establishable and useful without a Construct or Construction teammate.

When Development infrastructure and a Construct are colocated, however, that relationship may count toward broader infrastructure integration.

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

**Working canon, 11 September 2026.** Logistics materially changes how items, resources, supplies, and other strategic assets are delivered and made available where they are needed. The foundational verb is delivery and availability, not connection. A connection may result from Logistics, but connection is not what defines the archetype.

This separates Logistics from Exploration more cleanly. Exploration improves how players traverse, discover, access, and project through geography. Logistics improves how resources are delivered through that geography and made available at useful destinations.

### Infrastructure expression — Supply Lines

**Working, late September 2026.** Copper Chests are Logistics' native infrastructure anchor and make a logistics node legible to both teams. See [infrastructure.md](infrastructure.md#copper-chest-anchors-and-designation).


The current leading team-facing infrastructure expression of Logistics is the **Supply Line**.

A Supply Line concerns actual resource distribution rather than merely improving the carrying capacity of the Logistics player.

Current model:

> demonstrate repeatable delivery to a useful destination → recognize the demonstrated capability as a Supply Line

**Reinstated, late September 2026.** Explicit origin designation returns: a Supply Line is established Copper Chest Source → actual logistical transport → Copper Chest Destination. [HISTORICAL] The intervening origin-free formulation, in which origin was inferred from observed cargo movement rather than designated, is superseded. See [infrastructure.md](infrastructure.md#supply-lines).

Actual items stored upstream gradually transfer or replenish downstream storage.

This allows Logistics contribution to persist for the team even while the Logistics player is elsewhere.

Potential mechanical axes available to authored class progression include:

- Supply Line count;
- range;
- throughput;
- transfer frequency;
- capacity;
- filtering;
- number of destinations or nodes;
- branching;
- network sophistication;
- reliability.

Later high-potency Logistics may eventually allow supplied infrastructure to replenish allied players directly. This is a future candidate rather than an established baseline capability.

A Supply Line is established by designating a Copper Chest Source, performing actual logistical transport, and designating a Copper Chest Destination; it then simulates repeated deliveries by that method. It is directional unless separately proved in reverse. Capacity is a property of the logistical method and sets cargo slots per delivery, measured in Minecraft inventory slots at the carried item's ordinary stack size; Item Rate emerges from that method's actual movement over the established connection and sets delivery frequency. The player authors neither. [HISTORICAL] Capacity supersedes Flow Weight. Committed Capacity cannot simultaneously be active in the world — see [infrastructure.md](infrastructure.md#committed-capacity). Exact values remain [OPEN]. Ongoing flow may be represented without rendering every item, but the required physical transport state must remain valid and periodically checked. See [infrastructure.md](infrastructure.md#supply-lines). No mandatory Route, universal continuous ground path, or single required carrier is imposed; exact topology validation remains [OPEN].

Routes and Supply Lines may physically overlap and synergize without either requiring the other.

A Supply Line answers:

> How effectively can the team's resources become available where they are needed?

---

## Restricted material categories

**Working canon, 10 September 2026.** Primary Materials are a deliberately restricted set of materials whose expenditure directly represents significant portable player capability. The current members are Iron, Gold, Diamond, Netherite, and Leather. Wood, Stone, Copper, Redstone, Lapis, Coal, Emerald, Quartz, Flint, and String are explicitly outside the category. Exclusion does not imply low economic importance; a material can be central to another system, class, or activity without becoming a Primary Material.

The distinction is directness of conversion. Iron becomes tools, weapons, armour, shields, and buckets with little intermediation, so spending it intensifies an already meaningful scarcity and opportunity cost. Redstone is excluded because its major value emerges through further components, arrangement, and functioning systems rather than direct portable capability. Class relevance alone does not grant Primary Material status, and the category should remain restrictive rather than expanding toward an exhaustive material taxonomy.

This supplies the category definition [Kitfighter's Salvage](#7-kitfighter) depends on. Per-recipe mapping remains open.

Construction Blocks are a separate restricted set whose acquisition and preparation chains are distinct enough that Construction can meaningfully specialize in bringing them into structural use. The current members are Bricks, Mud Bricks, Terracotta, Concrete, and Glass. They are not an exhaustive list of blocks suitable for building, not the best building blocks, not required for valid construction, and not required for Construct recognition. Ordinary blocks remain fully useful and contribute normally to valid Constructs.

Current exclusions include planks, logs, cobblestone and stone, stone bricks, deepslate variants, granite, diorite and andesite with their polished forms, sandstone, slabs, stairs, panes and comparable derivatives, and mineral storage blocks. Status does not propagate to specialized derivatives: Glass may qualify while Glass Panes do not automatically qualify, and Bricks may qualify while Brick Slabs do not.

The category is partly an economic intervention. Wood is already extraordinarily useful and naturally demanded, and polished rock largely adds a crafting step to abundant material, so neither creates new strategic demand. Clay, mud, concrete, and glass support distinctive construction-oriented acquisition and preparation processes whose competitive value would otherwise be weak in a compressed match. The intent is to reward meaningful construction-material activity rather than arbitrary extra crafting clicks.

[OPEN] The XP premium for legitimately incorporating Construction Blocks is a balance target, not canon. Per-recipe Primary Material mapping, derivative handling, and the interaction with anti-farming rules remain unresolved.

---

## Construction's material agency

**Working design direction, 10 September 2026.** Construction may hold exceptional agency over the acquisition, preparation, recovery, and structural use of specifically construction-oriented materials without becoming generic Extraction or generic Production. Extraction remains exceptional removal, access, acquisition, detection, and excavation of world materials generally. Production remains exceptional transformation of acquired inputs into useful outputs generally. Construction is exceptional creation and manipulation of persistent built world, and may possess class mechanics that improve material handling precisely because the material is destined for structural construction.

The governing principle is bounded on both sides. Construction may outperform Production at a transformation when the output's principal purpose is structural construction, while Production remains superior at transformation as a general capability; and Construction may hold advantages involving construction-specific feedstocks without becoming superior to Extraction at acquisition generally. These are legitimate class-design territories, not universal passive bonuses for every Construction player. Candidate expressions include construction-material yield, bulk preparation, on-site preparation, rapid hardening or conversion, construction recovery and recycling, efficient demolition and rebuilding, and structural material reuse.

This creates a real decision when preferred Construction Blocks are unavailable. Building immediately from ordinary or improvised material brings the structure online sooner and keeps the Construct valid while sacrificing the Construction Block progression premium; spending time obtaining and preparing Construction Blocks delays the structure but is more progression-efficient; coordinating with Extraction, Logistics, or Production lets those archetypes support the material chain while Construction spends more labor actually constructing. The correct choice should vary with strategic pressure rather than having a universally optimal answer.

---

# 3. Class Draft Format

## When is a class "settled"?

**Project standard, 11 September 2026.**

A class is conceptually settled when it has an identity (hook / core rule, and archetypes assigned descriptively from finished behaviour), a finalized passive, two actives each with a base ability and three branches, and a finalized ultimate.

[OPEN] may remain for exact numbers, cooldowns, ranges, durations, implementation details, balance, global-system dependencies, and technical feasibility. **[OPEN] must not conceal missing class design.**

Archetypes are **descriptive, not generative**. The design pipeline is Source → Rule → Consequences → Modes → Archetypes. Do not add an archetype because an ability happens to touch related content, and do not assign archetypes prospectively to satisfy coverage.

---


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

## Current universal progression skeleton

The current working match progression spans Levels 1–30.

The level curve intentionally contains distinct breakpoints rather than treating every level as an equivalent incremental increase. Some rewards establish class abilities, some accelerate ordinary Minecraft activity, some expand fundamental player capacity, and others open or deepen persistent world specialization.

Current working breakpoint structure:

| Level | Progression event |
| ---: | --- |
| 1 | Ability 1; begin at 9 Health, 9 Hunger, and 6 inventory slots |
| 2 | Ability 2; universal capacity growth |
| 3 | Capacity specialization I |
| 4 | Efficiency I; universal capacity growth |
| 5 | Ability 1 upgrade |
| 6 | Infrastructure specialization / recognition entry |
| 7 | Yield I; universal capacity growth |
| 8 | Passive scaling; universal capacity growth |
| 9 | Task specialization; universal capacity growth |
| 10 | Ability 2 upgrade |
| 12 | Infrastructure role-expression upgrade |
| 14 | Task advancement; universal capacity growth |
| 15 | Ultimate unlock |
| 16 | Passive scaling; current working mobility breakpoint |
| 17 | Universal Health and Hunger floors reach 20; Hunger above 20 is represented through exhaustion efficiency |
| 18 | Capacity specialization II |
| 19 | Task advancement |
| 20 | Advanced class-authored reward I |
| 24 | Capacity specialization III; passive scaling |
| 25 | Advanced class-authored reward II |
| 30 | Capstone, exact form unresolved |

Levels not listed here may still provide universal capacity growth according to the universal growth rules.

[PROPOSED — NOT CANON, 12 September 2026] A capacity curve rework is under review: distinct growth cadences per capacity, Lv1 Health 8, universal Health and Hunger endpoints of 18 with specialization reaching 20, and a third specialization that unlocks a qualitative Mastery instead of a higher number. It is recorded in full in [docs/proposals/2026-09-12-capacity-curve.md](docs/proposals/2026-09-12-capacity-curve.md) and deliberately changes nothing below until reviewed. Everything in this section remains canon.

Universal capacity progression currently begins at:

- 9 Health = 4.5 hearts;
- 9 Hunger = 4.5 hunger icons;
- 6 inventory slots.

On applicable universal-growth levels:

- +1 Health;
- +1 Hunger;
- +3 inventory slots.

Capacity specialization occurs at Levels 3, 18, and 24. Each specialization choice currently grants one of:

- +2 Health;
- +2 effective Hunger;
- +6 inventory slots.

Health and effective Hunger specialization may exceed 20. Inventory capacity has a hard maximum of 36 slots.

If an Inventory specialization is selected while the player's Inventory is already at the 36-slot hard maximum, its +6 Inventory effect is replaced by +0.5 Health and +0.5 effective Hunger.

This makes early Inventory specialization primarily an acceleration benefit: an Inventory specialization at Level 3 causes the player to reach the 36-slot maximum at Level 14 rather than Level 16. Once universal Inventory progression catches up, later Inventory specialization remains mechanically live through the replacement benefit rather than exceeding the 36-slot maximum.

If an Inventory specialization is selected while the player's Inventory is already at the 36-slot hard maximum, its +6 Inventory effect is replaced by +0.5 Health and +0.5 Hunger. This keeps later Inventory specialization choices mechanically live without allowing Inventory capacity to exceed 36 slots.

Task-progression rewards can coexist with universal capacity growth. In particular, Level 9 task specialization does not replace Level 9 universal growth.

Under the current schedule, the universal Inventory floor reaches its hard maximum of 36 slots at Level 16. The universal Health and effective Hunger floors reach 20 at Level 17.

Health specialization remains directly additive above 20.

Effective Hunger specialization also remains additive above 20, but Minecraft's displayed food level remains capped at 20. Effective Hunger above 20 is therefore represented mechanically through exhaustion efficiency rather than additional visible Hunger icons.

Inventory specialization cannot raise Inventory above 36; specialization selected at the cap instead uses the +0.5 Health / +0.5 effective Hunger replacement benefit described above.

### Effective Hunger above 20

Effective Hunger remains a progression stat above Minecraft's visible 20-point food limit.

Up to 20 effective Hunger, the player's maximum displayed Hunger directly represents the progression stat.

Above 20 effective Hunger:

- displayed Hunger remains capped at 20;
- the player's effective reserve above the 6-Hunger sprint cutoff continues to scale according to the existing Hunger progression curve;
- this additional capacity is represented through reduced exhaustion consumption rather than additional food icons.

The conversion preserves the amount of Hunger-consuming activity that the original effective-Hunger value would have allowed before reaching the 6-Hunger sprint cutoff.

For effective Hunger H above 20:

    exhaustion multiplier = 14 / (H - 6)

The value 14 is the ordinary reserve between 20 Hunger and the 6-Hunger sprint cutoff.

Current relevant conversions are:

| Effective Hunger | Exhaustion multiplier | Exhaustion reduction |
| ---: | ---: | ---: |
| 20 | 1.0000× | 0% |
| 21 | 0.9333× | ~6.67% |
| 22 | 0.8750× | 12.5% |
| 24 | 0.7778× | ~22.22% |
| 26 | 0.7000× | 30% |

This conversion changes the implementation of Hunger capacity above 20, not the underlying progression curve. A player with 24 effective Hunger still has 24 Hunger for progression and balance purposes even though the vanilla HUD displays at most 20.

The interaction between this exhaustion-efficiency model and Hunger expenditure associated with health regeneration is intentionally unresolved and should not currently alter these values.

A current working Level 16 phase-transition reward is approximately +10% universal movement speed.

Example — a player selecting Hunger specialization at Level 3:

| Level | Effective Hunger | Displayed maximum | Exhaustion multiplier once above 20 |
| ---: | ---: | ---: | ---: |
| 1 | 9 | 9 | — |
| 2 | 10 | 10 | — |
| 3 | 12 | 12 | — |
| 4 | 13 | 13 | — |
| 7 | 14 | 14 | — |
| 8 | 15 | 15 | — |
| 9 | 16 | 16 | — |
| 11 | 17 | 17 | — |
| 13 | 18 | 18 | — |
| 14 | 19 | 19 | — |
| 15 | 20 | 20 | 1.0000× |
| 16 | 21 | 20 | 0.9333× |
| 17 | 22 | 20 | 0.8750× |

Level 18 Hunger specialization:
22 → 24 effective Hunger
exhaustion multiplier → 0.7778×

Level 24 Hunger specialization:
24 → 26 effective Hunger
exhaustion multiplier → 0.7000×

This is intended as a match-condensing mid/late-game mobility breakpoint, not as an early-game Exploration reward. It therefore does not replace the current principle that early Exploration and early Routes should generally emphasize traversal efficiency before unconditional movement potency.

The exact Level 16 movement value and implementation remain subject to testing.

Exact XP requirements for reaching these levels are a separate match-progression question.


### Task progression

Task progression condenses ordinary Minecraft economic and world-interaction time as the match advances.

The current working domains are:

- Efficiency — the Efficiency enchantment family; improves ordinary block-breaking and worldwork rate.
- Yield — Fortune + Looting; improves the amount of useful material obtained from successful resource-producing or acquisition actions.
- Damage — Sharpness + Power; improves conventional melee and ranged damage.

Current working cadence:

- Level 4: Efficiency I universally.
- Level 7: Yield I universally.
- Level 9: choose an eligible task advancement.
- Level 14: choose an eligible task advancement.
- Level 19: choose an eligible task advancement.

Efficiency, Yield, and Damage currently have an intended generic maximum of Tier III.

At Level 9, the initial choice space therefore begins from a player who already has Efficiency I and Yield I, while Damage has not yet been universally granted.

The Tier III ceiling is intended to preserve room for class-specific amplification. Generic high-tier enchantment scaling should not make specialized class mechanics redundant or cause uncontrolled multiplication with them. Mole's Tunneling, which amplifies current digging-tool speed, is an important example.

Task advancement is an additive progression layer and may occur on the same level as universal capacity growth.

## Working XP requirement bands

Exact XP values remain unresolved, but the current working direction is that Levels 1–30 do not follow one uninterrupted smooth XP curve.

Instead, levels are grouped into broad **XP requirement bands** corresponding to changes in the productive economy available to players.

A new band represents a new category of XP requirement. Requirements may remain approximately flat or rise only modestly within a band, while major economic breakpoints can produce a more substantial increase between bands.

Current working structure:

| XP band | Levels | Working requirement index | Economic state |
| --- | ---: | ---: | --- |
| I — Bootstrap | 1–6 | ~1.000× | constrained player; base Minecraft XP economy |
| II — Established | 7–12 | ~1.350× | first infrastructure economy; Yield I; early task specialization |
| III — Developed | 13–19 | ~1.875× | mature T1 / emerging T2 infrastructure; repeated task advancement; universal capacity approaches vanilla completeness |
| IV — Advanced | 20–24 | ~2.575× | increasingly mature T2 infrastructure and advanced class-authored progression |
| V — Endgame | 25–30 | ~3.250× | mature late-game economy; progression increasingly emphasizes exceptional class expression and terminal power |

These indices are relative balance targets rather than final displayed XP values. `1.000×` represents the eventual baseline XP requirement chosen for the Bootstrap band.

The exact amount of XP represented by that baseline should be derived from the amount and type of legitimate Minecraft activity expected during Levels 1–6 rather than chosen arbitrarily.

### Relationship between breakpoints and XP bands

Not every progression breakpoint creates a new XP band.

Many breakpoint rewards instead increase player throughput **within** the current band or provide the economic tools needed to enter the next one.

The current working progression rhythm is:

- Levels 1–6 use the Bootstrap requirement band.
- Efficiency I at Level 4 begins accelerating productive activity within that band.
- Infrastructure specialization / recognition at Level 6 provides access to a new economic tool immediately before the first major XP-requirement transition.
- The Level 6 → 7 requirement is the current candidate entry into the Established band.
- Yield I at Level 7 further accelerates productive activity after that transition.
- Level 9 task specialization provides another within-band progression increase.
- Level 12 infrastructure role expression helps mature the first infrastructure economy before the next requirement band.
- The Level 12 → 13 requirement is the current candidate entry into the Developed band.
- Level 14 task advancement further increases productive capability within that band.
- Levels 16–17 mark the approximate completion of the universal physical-capacity transition: inventory reaches its 36-slot universal floor at Level 16, while Health and Hunger reach their vanilla universal floors at Level 17.
- Level 19 task advancement provides another late-Developed-band progression increase.
- The Level 19 → 20 requirement is the current candidate entry into the Advanced band, alongside Advanced class-authored reward I.
- The Level 24 → 25 requirement is the current candidate entry into the Endgame band, alongside Advanced class-authored reward II.
- Level 30 is terminal progression rather than preparation for another XP band.

This creates a general pacing relationship:

> major economic tools and throughput increases should arrive before, at, or within the XP-requirement bands whose larger requirements assume access to those tools.

### XP requirements should not cancel progression acceleration

Higher XP requirements are intended to absorb only part of the increased XP throughput created by progression.

Efficiency, Yield, task advancement, increased universal capacity, infrastructure, class abilities, and increasingly valuable world opportunities are deliberately capable of making later match progression faster.

Therefore, if a later economy can generate legitimate XP substantially faster than the Bootstrap economy, its XP requirements should generally increase by a smaller proportion.

The current requirement indices reflect this principle:

- Bootstrap: ~1.000×;
- Established: ~1.350×;
- Developed: ~1.875×;
- Advanced: ~2.575×;
- Endgame: ~3.250×.

These values should be recalibrated once actual XP yields and expected XP throughput can be measured.

### Relationship to infrastructure progression

Infrastructure is one contributor to the economic transitions represented by the XP bands, but it is not the sole means of remaining competitive with later XP requirements.

The current infrastructure-integration direction allows independently useful infrastructure systems to become connected around recognized centers of activity.

A Construct alone does not automatically grant an XP multiplier.

Distinct connected infrastructure systems can instead contribute toward a modest multiplier on otherwise legitimate XP-generating activity within the relevant integrated area.

Current first-pass integration targets are:

| Integration state | T1 infrastructure | T2 infrastructure |
| --- | ---: | ---: |
| One distinct connected system | ~1.025× XP | ~1.050× XP |
| Two distinct connected systems | ~1.055× XP | ~1.100× XP |
| Three distinct connected systems | ~1.100× XP | ~1.150× XP |

These are working balance targets rather than final values.

The important intended relationship is:

- a Construct alone does not create bonus XP;
- the first distinct infrastructure connection provides a modest progression benefit;
- additional distinct connections provide increasingly meaningful integration;
- mature T1 integration currently targets approximately +10% XP;
- mature T2 integration currently targets approximately +15% XP;
- individual infrastructure systems should be able to improve their own integration contribution through later progression.

Infrastructure integration should not determine XP requirements dynamically.

The XP requirement for a level remains fixed regardless of whether a player or team has established effective infrastructure. Strong infrastructure instead allows players to meet those requirements more efficiently.

This preserves meaningful consequences for preparation and world development without making infrastructure the only viable source of post-Bootstrap progression.

### Relationship to match economy

The XP-band model assumes that the world and the players both become capable of supporting larger-scale activity over time.

Later XP requirements should therefore be evaluated against the combined effect of:

- Efficiency progression;
- Yield progression;
- task advancement;
- increasing universal Health, Hunger, and inventory capacity;
- infrastructure recognition and integration;
- Development productivity;
- Route efficiency;
- Supply Line distribution;
- Construct benefits;
- class-authored progression;
- phase-appropriate Worksite opportunities;
- larger or more valuable naturally available opportunities;
- other phase-appropriate XP-generating accomplishments.

The intended calibration target is therefore not:

> later XP requirement = earlier XP requirement × infrastructure multiplier

but:

> later XP requirement is calibrated against expected legitimate XP throughput in that progression and match phase.

The current XP bands and requirement indices are a framework for that later bottom-up calibration, not final XP values.

---

# 4. Mole

## Status

**Conceptually settled** (11 September 2026). Numerical and implementation details remain [OPEN]. Kit restated 13 September 2026.

### Canonical kit — 13 September 2026

**Source: [K7] seven-class kit handoff.** Reproduced as supplied. Where the developed material below elaborates this kit they agree; where it differs, this block is the current statement.

Archetypes: Extraction · Combat · Exploration
Hook: The world beneath and inside terrain is as navigable to Mole as its surface is to everyone else.

**Passive — Sifth Sense.** While actively digging, Mole senses nearby sand and gravel. Excavating these materials can sift through connected falling material rather than allowing it to collapse normally.

**A1 — Tunneling.** Enter a digging mode that amplifies tool speed and automatically excavates a 1×2 passage while moving into terrain. Mole cannot attack or build while Tunneling and can steer vertically with jump and sneak.

- **Bore** — Tunnel substantially faster.
- **Gallery** — Excavate a wider, approximately 3×2 passage.
- **Dig In** — Become substantially harder to displace while actively excavating.

**A2 — Drill Rush.** Enter a wall or floor. Reactivate to burst from the terrain in the aimed direction, damaging enemies at the point of emergence.

- **Armored Emergence** — Gain major damage reduction upon emerging.
- **Undermine** — Emerging upward from beneath an enemy stuns them.
- **Burrow Chain** — Gain Speed after emerging from a wall; quickly entering another wall resets Drill Rush.

**Ultimate — Sinkhole.** Destabilize a large region of natural terrain. After a delay, it progressively collapses downward into a permanent jagged sinkhole and becomes temporarily unbuildable during the collapse. Player-built construction is unaffected.

---

## Core idea / hook

The world beneath and inside terrain is as navigable to Mole as its surface is to everyone else.

## Primary archetype

**Extraction**

## Secondary archetypes

- Combat
- Exploration

Mole is **conceptually settled**. Secondary archetypes are assigned descriptively from finished behaviour. Logistics is **not** an archetype for Mole and should not be listed.

---

## Passive — Sifth Sense

While actively digging, Mole senses nearby sand and gravel. Excavating sand or gravel can sift or clear connected falling material substantially more effectively instead of producing normal collapse behaviour.

[OPEN] Sense radius, sift volume, the exact relationship to vanilla falling-block behaviour, and progression scaling remain unresolved.

[HISTORICAL] The earlier Sifth Sense — detecting players moving on sand and gravel, scaling through detection radius, rate, and location specificity — is **superseded** by the sensing/sifting formulation above.

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

**Bore**
- Increased tunnelling speed.

**Gallery**
- Creates approximately 3×2 useful tunnels.

**Dig In**
- Substantially harder to displace while actively excavating.

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

Mole senses and targets an unstable natural-terrain region and incites a delayed collapse.

- Natural terrain only.
  - [TECHNICAL RISK] Distinguishing natural terrain from player construction requires block provenance, which vanilla does not record. See [the capability audit](docs/feasibility/2026-09-12-capability-audit.md).
- Collapse occurs downward in stages.
- Destroyed terrain produces only partial drops.
- The area becomes No-Build while actively collapsing.
- Leaves a permanent jagged sinkhole.
- Player-authored construction survives rather than being indiscriminately erased.

[OPEN] Delay, staging cadence, region size, instability criteria, drop fraction, No-Build duration, and counterplay remain unresolved.

The Ultimate has no upgrade branch.

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

### Canonical kit — 13 September 2026

**Source: [K7] seven-class kit handoff.** Reproduced as supplied. Where the developed material below elaborates this kit they agree; where it differs, this block is the current statement.

Archetypes: Development · Control · Support
Hook: Vegetation isn't just something to harvest: it is a landscape of productive sources worth harvesting, preserving, and reinvesting into.

[CONFLICT] **Control** and **Support** are not members of the Seven Archetypes defined in [section 2](#2-the-seven-archetypes), which are Combat, Construction, Development, Exploration, Extraction, Logistics and Production. The kit's archetype line is reproduced exactly as supplied rather than silently mapped onto existing archetypes or silently dropped. Whether Control and Support are new archetypes, descriptive role language, or a substitution for existing secondary archetypes is for the owner to decide; the roster in [section 11](#11-current-class-roster-status) still records Gardener's secondaries as deliberately unassigned.

**Passive — Plant Material.** Gardener accumulates Plant Material through their abilities. At full stacks, Plant Material is automatically consumed to grow a Cultivar nearby. Cultivars accelerate the growth of adjacent plants.

**A1 — Clip.** Clip a living plant without destroying or resetting it, generating Plant Material. Mature plants provide substantially more.

- **Specimen** — Rewards clipping individually mature plants.
- **Proliferation** — Rewards concentrations of plants from the same family.
- **Collection** — Rewards clipping varied plant species and families, with diminishing value from repetition.

**A2 — Cultivate.** Consume eligible plant items from inventory to generate Plant Material at lower efficiency than Clip. The selected upgrade determines Gardener's normal Cultivar.

- **Torchflower** — Cultivars provide light and protective Absorption.
- **Sweet Berry Bush** — Cultivars provide provisioning and create hostile defensive thickets.
- **Giant Bamboo** — Cultivars generate empowered bamboo offspring that extend growth acceleration outward without recursively reproducing themselves.

**Ultimate — Grafter's Handbook.** Gardener gains access to both Cultivar types they did not select through Cultivate, allowing all three forms to be grown sequentially without changing the original specialization.

---

## Status

**Conceptually settled** (11 September 2026). The kit below **supersedes** the earlier Spread Vines / Flowerpot / Glistening Greenhouse design.

## Core idea / hook

Everyone else sees vegetation primarily as things to harvest. Gardener sees a landscape of productive sources at different stages of development and is constantly deciding which ones are worth harvesting, preserving, or reinvesting into.

## Primary archetype

**Development**

## Secondary archetypes

Gardener is **conceptually settled** with Development as its primary archetype. Secondary archetypes are deliberately **not** forced; they should be assigned descriptively once the finished behaviour is evaluated.

## Core resource — Plant Material and Cultivars

**Plant Material** is a stacking proc/meter, **not** an inventory item. Reaching its threshold automatically consumes the accumulated Plant Material to grow a Cultivar nearby.

A **Cultivar** is a special developed plant created through Gardener's class systems. Cultivars accelerate the growth of adjacent plants.

[OPEN] Threshold value, accumulation rates, Cultivar placement rules, persistence, stacking limits, and enemy counterplay remain unresolved.

---

## Superseded material

[HISTORICAL] The earlier Gardener kit — Spread Vines (Grapevines / Jungle Vines / Offensive Vine), Flowerpot (Double Bushes / Torchflowers / Charges), the repeated-planting growth-speed passive, and the **Glistening Greenhouse** ultimate — is **superseded** by the Plant Material / Cultivar model above. It is retained here for traceability only and should not be revived silently. Backyard / Exotic / Generalist upgrade tendencies are likewise superseded by the Clip and Cultivate branch families.

---

## Passive — Plant Material

Accumulate Plant Material. At threshold, Plant Material is automatically consumed to grow the currently available Cultivar nearby. Cultivars accelerate adjacent plant growth.

---

## Ability 1 — Clip

Clip a living plant without destroying or resetting it to generate Plant Material.

- Growing crops are eligible.
- Mature plants and crops provide substantially greater value.

### Branches

**Maturity / Specimen**
- Rewards clipping individually mature, high-value specimens.

**Proliferation / Population**
- Rewards clipping where many plants of the same family are established locally.

**Diversity / Collection**
- Rewards varied species and families; repeated clipping of the same kinds becomes less valuable.

[OPEN] Branch names are conceptual labels retained from design discussion, not finalized titles.

---

## Ability 2 — Cultivate

Consume eligible real plant items from inventory to generate Plant Material. This is less efficient than Clip, but lets Gardener convert harvested plant inventory back into development.

The chosen Cultivate branch determines Gardener's normal Cultivar.

### Branches

**Torchflower Cultivar**
- Protection / sanctuary orientation. Provides light and Absorption-related value.

**Sweet Berry Bush Cultivar**
- Fortification / sustain orientation. Provides provisioning value and creates a hostile thicket effect.

**Giant Bamboo Cultivar**
- Expansive Development orientation. The original Giant Bamboo produces empowered bamboo offspring that themselves project growth acceleration.
- **No exponential recursion.** Empowered offspring do not recursively create further empowered generations without limit.

[OPEN] Conversion efficiency, eligible plant items, Cultivar effect magnitudes, and the offspring generation cap remain unresolved.

---

## Ultimate — Grafter's Handbook

At the appropriate progression point — currently Level 15 in design discussion — activation allows Gardener to grow **both** of the other upgraded Cultivar forms sequentially, one at a time.

This is not a respec or cycling system. It represents mastery over the other Cultivar forms.

[OPEN] Unlock level, sequencing, duration, and whether the granted Cultivars persist after the ultimate ends remain unresolved.

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

### Canonical kit — 13 September 2026

**Source: [K7] seven-class kit handoff.** Reproduced as supplied. Where the developed material below elaborates this kit they agree; where it differs, this block is the current statement.

Archetypes: Construction · Combat · Production
Hook: Building materials are also potential workers: the blocks Golem Master carries determine what kind of golems they can animate and what those golems can accomplish.

**Passive — Pumpkin Supply.** Pumpkins become periodically available to Golem Master, supplying the defining component needed to animate golems.

**A1 — Animate.** Consume four blocks and a pumpkin to animate a temporary golem. Its durability and physical properties derive from the material used to create it.

- **Iron Golem** — A durable fighter emphasizing health and knockback.
- **Snow Golem** — A ranged golem whose projectiles hinder enemy movement.
- **Copper Golem** — A smaller, faster worker and swarm fighter using evasive hit-and-run behavior.

**A2 — Assemble.** Consume carried materials to rapidly assemble a wall. Construction performance scales with Golem Master's available workforce/material properties.

- **Iron Wall** — Reinforced defensive construction that synergizes with Iron Golems.
- **Snow Wall** — Defensive construction whose attacks/projectiles slow and displace enemies.
- **Copper Wall** — Rapid construction that improves nearby Copper Golem movement and work speed.

**Ultimate — Wither Golem.** Animate a temporary Wither-derived golem whose combat power evokes the Wither without reproducing its uncontrolled terrain destruction.

**Audit flag (supplied with the kit):** This is still the least settled of the seven. The base Assemble rule, generic material-stat rules, and Wither Golem's actual behavior need one final conceptual pass. This exact wording is **not** promoted to canon.

---

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

## Passive — Pumpkin Supply

**Named 13 September 2026 by [K7].** Pumpkins become periodically available to Golem Master, supplying the defining component needed to animate golems.

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

## Copper behaviour and command principle

**Working canon, 11 September 2026.** Copper golems should behave as cheap worker drones outside combat, and as swarm/harass/scatter combat units when fighting.

The core command principle is:

> Player chooses intent; golem type determines execution.

Commands under consideration include Follow, Combat, Work, and Rally/Guard.

[OPEN] The Wither Golem ultimate and the relationship to generic Construction progression retain their existing open questions.

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

### Canonical kit — 13 September 2026

**Source: [K7] seven-class kit handoff.** Reproduced as supplied. Where the developed material below elaborates this kit they agree; where it differs, this block is the current statement.

Archetypes: Combat · Production
Hook: Ordinary equipment is raw material for a combat kit: Kitfighter crafts efficiently, carries unusual tools into battle, and temporarily exceeds Minecraft's normal equipment ceiling.

**Passive — Salvage.** Crafting equipment with at least three units of a Primary Material generates Salvage for that material. Accumulating enough material-specific Salvage automatically recovers two units of that material, with the required Salvage decreasing through progression.

**A1 — Offhander.** Equip a Bow in the offhand, allowing it to function alongside Kitfighter's normal mainhand equipment.

- **Crossbow** — Replace the Bow with a Crossbow.
- **Fishing Rod** — Replace it with a Fishing Rod.
- **Golden Head** — Replace it with a Golden Head.

**A2 — Hotswap.** Deploy a temporary web at the targeted block.

- **Cobweb** — Deploy a longer-lasting Cobweb.
- **Lava** — Deploy temporary Lava instead.
- **TNT** — Deploy temporary TNT instead.

**Ultimate — Covered With Diamonds.** Temporarily project Diamond-level protection wherever Kitfighter's current equipment is weaker, while gaining movement speed and knockback resistance. Its effects extend to Kitfighter's upgraded equipment options.

Open: Exact interaction with equipment already exceeding the projected Diamond state remains implementation/balance work.

---

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

**Restated 13 September 2026 by [K7].** Temporarily project Diamond-level protection wherever Kitfighter's current equipment is weaker, while gaining movement speed and knockback resistance. Its effects extend to Kitfighter's upgraded equipment options. This is a projection over weaker equipment rather than a blanket upgrade, and it now also carries movement speed and knockback resistance.

[OPEN] Exact interaction with equipment already exceeding the projected Diamond state remains implementation/balance work.

When the Ultimate ends, the equipment reverts.

Current overdrive combat bonuses:
- knockback resistance;
- movement speed.

Upgraded escalation includes a **Notch Apple** and an **Ender Crystal**.

[OPEN] Ultimate unlock timing is owned by progression documentation and is not resolved here.

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
- Exact mapping of Primary Material to every eligible equipment recipe. Category membership is now defined under [Restricted material categories](#restricted-material-categories); the per-recipe mapping remains open.
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

Matches progress from:

> **Level 1 → Level 30**

Level 30 intentionally references Minecraft's familiar enchanting threshold.

The exact relationship between MOBA levels and vanilla Minecraft XP/enchanting remains unresolved.

The intended broad power curve is:

Early game:
- significantly constrained Minecraft character;
- fundamental physical capacities matter;
- loadout, provisioning, expedition range, and survivability impose meaningful limits.

Midgame:
- approaches and reaches ordinary vanilla physical capability;
- specialization begins to distinguish players above or ahead of the universal floor.

Late game:
- increasingly supernormal class/archetype expression;
- exceptional power should increasingly come from specialization, class mechanics, archetype mechanics, efficiency, potency, and systemic recognition rather than indefinite generic physical growth.

Level 30:
- highly realized class.

---

## Universal capacities

The current preliminary universal capacities are:

- **Health**
- **Hunger**
- **Inventory Capacity**

At Level 1, every player begins with:

| Capacity | Level 1 value |
| --- | ---: |
| Health | **9 Health / 4.5 hearts** |
| Hunger | **9 Hunger / 4.5 hunger icons** |
| Inventory Capacity | **6 slots** |

These deliberately place the Level 1 player below ordinary vanilla physical capability.

Health limits early survivability.

Hunger functions as internal expedition/endurance capacity.

Inventory Capacity limits the number of distinct tools, supplies, materials, and resource categories a player can carry at once.

Hunger and Inventory intentionally overlap as expedition constraints:

> Hunger determines how long the player's body can operate between resupply.
>
> Inventory determines how much resupply, equipment, and return haul the player can carry.

---

## Automatic universal growth

Every level that does **not** present the player with a progression choice grants:

- **+1 Health**
- **+1 Hunger**
- **+3 Inventory Capacity**

Fixed class unlocks and automatic passive scaling do not prevent this growth.

A level forgoes automatic universal growth only when that level presents the player with a progression choice.

Automatic universal growth stops separately for each capacity when its vanilla maximum is reached:

| Capacity | Universal maximum |
| --- | ---: |
| Health | **20 Health / 10 hearts** |
| Hunger | **20 Hunger / 10 hunger icons** |
| Inventory Capacity | **36 slots** |

Under the current preliminary level schedule, a player who never specializes in any of these capacities reaches all three vanilla capacities at approximately **Level 19**.

Vanilla physical capability is therefore a midgame universal floor rather than the player's maximum possible power.

---

## Capacity specialization

At the current preliminary capacity-choice levels of:

- **Level 3**
- **Level 18**
- **Level 24**

the player may specialize in one universal capacity.

The choices are:

| Choice | Increase |
| --- | ---: |
| Health | **+2 Health / 1 heart** |
| Hunger | **+2 Hunger / 1 hunger icon** |
| Inventory Capacity | **+6 slots** |

Each capacity choice is therefore worth two automatic increments of that capacity.

Capacity specialization is additive to the universal floor.

Automatic progression does not erase or absorb an earlier specialization.

Health and Hunger specialization may raise maximum Health or Hunger **above their vanilla values**.

Inventory Capacity cannot exceed the vanilla **36-slot** inventory maximum. Once a player has reached 36 slots, Inventory should no longer be offered as a capacity specialization. Its replacement choice remains unresolved.

---

## Level 3 capacity choice

Immediately before the Level 3 choice, the Level 2 automatic increase has brought every player to:

- **10 Health / 5 hearts**
- **10 Hunger / 5 hunger icons**
- **9 Inventory slots**

The Level 3 capacity choice therefore produces:

| Choice | Before | After |
| --- | ---: | ---: |
| Health | 10 Health / 5 hearts | **12 Health / 6 hearts** |
| Hunger | 10 Hunger / 5 icons | **12 Hunger / 6 icons** |
| Inventory | 9 slots | **15 slots** |

The three choices use the same progression grammar but are not expected to produce identical percentage changes or strategic effects.

### Health

The Level 3 Health choice increases maximum Health from 10 to 12:

> **+20% maximum Health**

Its practical value should be evaluated against recognizable Minecraft survival thresholds, including combat, mobs, falls, environmental damage, and the ability to survive long enough to disengage.

### Hunger

Vanilla sprinting becomes unavailable at **6 Hunger or below**.

At Level 3, a player who does not choose Hunger has:

> 10 maximum Hunger − 6 sprint cutoff = **4 Hunger of pre-cutoff reserve**

A player who chooses Hunger has:

> 12 maximum Hunger − 6 sprint cutoff = **6 Hunger of pre-cutoff reserve**

The Level 3 Hunger choice therefore increases this simplified pre-cutoff sprint-capable reserve from 4 to 6:

> **+50%**

This does not mean exactly 50% more real-world travel distance. Saturation, exhaustion, food carried, terrain, combat, detours, and other actions affect actual expedition range.

The magnitude is nevertheless intentional. Hunger progression is coupled to Default-map scale, food availability, expedition distance, and return-trip constraints and should be tested against those systems.

Hunger specialization should increase endurance rather than directly grant movement speed.

### Inventory Capacity

The Level 3 Inventory choice increases carrying capacity from 9 to 15 slots:

> **+67% Inventory Capacity**

Inventory progression should be evaluated through meaningful loadout categories rather than slot count alone.

Relevant decisions include:

- tools vs food;
- blocks vs supplies;
- expedition provisioning vs return haul;
- multiple construction materials vs utility;
- extraction yield vs preparedness.

The intended effect is to make additional categories of equipment or material practical to carry, not merely to make inventory management more convenient.

---

## Relationship to archetypes and classes

Universal progression establishes the minimum physical capability available to everyone.

Capacity specialization differentiates players above or ahead of that floor.

Class and archetype progression establishes the exceptional ceiling.

Therefore:

> Universal progression establishes the floor.
>
> Specialization creates differentiation above or ahead of that floor.
>
> Class/archetype progression establishes the exceptional ceiling.

Generic universal progression should not reproduce an entire archetype or class identity.

Examples:

- generic Health progression does not replace Combat;
- generic Hunger progression does not replace Exploration;
- generic Inventory progression does not replace Logistics.

Everyone can become physically tougher, travel farther between resupply, and carry a vanilla-sized inventory.

Combat, Exploration, and Logistics classes remain exceptional because their archetype mechanics can manipulate the corresponding strategic domains in ways that generic capacity growth does not.

The same principle applies outside these three universal capacities: ordinary Minecraft capabilities remain broadly available, while class/archetype progression supplies exceptional efficiency, potency, systemic recognition, and higher-order interaction with the world.

---

## Current status

The numerical universal-capacity model above is the **current preliminary progression model**.

In particular, it supersedes earlier exploratory assumptions that:

- approximately Level 16 must be the universal vanilla-capability landmark;
- Level 1 Health should begin at 10 Health / 5 hearts;
- Level 1 Hunger should begin at 10 Hunger / 5 icons;
- starting Inventory Capacity should be 9 slots;
- Health, Hunger, and Inventory require different progression schedules merely because they represent different systems.

The current model instead uses a common progression grammar:

> **Health:** 9 start → +1 automatic → +2 specialization
>
> **Hunger:** 9 start → +1 automatic → +2 specialization
>
> **Inventory:** 6 start → +3 automatic → +6 specialization

with the universal component of all three reaching vanilla capability at approximately Level 19 under the current preliminary level schedule.

These values remain subject to playtesting, particularly:

- early-game lethality at 9 Health;
- Hunger against actual Default-map expedition distances, food availability, saturation, and exhaustion;
- whether 6 starting Inventory slots creates strategic loadout pressure without excessive item-management friction;
- the replacement for Inventory specialization once 36 slots have been reached.

---

# 10. Infrastructure and Classes

# INJECTION §10: clarify infrastructure progression

Progression gates access to exceptional/system-recognized infrastructure without gating ordinary Minecraft behavior.

Anyone can:
- build;
- travel;
- place storage;
- farm and develop productive terrain;
- transport items.

These ordinary actions form the universal Minecraft floor and do not require specialization.

Permanent progression/specialization choices can unlock systemic recognition of:
- a Construct;
- a Route;
- a Supply Line;
- a Development system/area;
- other empowered infrastructure.

The causal order is:

> progression unlocks recognition capability → the player establishes a qualifying world structure/system → the game recognizes and empowers it.

Simply building, traversing, farming, or connecting storage does not by itself grant the corresponding progression capability.

Further progression may improve the scale, capacity, efficiency, potency, or sophistication of infrastructure the player can have recognized.

The exact:
- first unlock level;
- upgrade cadence;
- number of available investments;
- mutual exclusivity or non-exclusivity of infrastructure investments;
- numerical scaling of each recognized system

remain unresolved pending the preliminary level model.

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

### Mole — settled
Primary:
- Extraction

Secondary:
- Combat, Exploration

Logistics is **not** a Mole archetype.

### Gardener — settled
Primary:
- Development

Secondary:
- Deliberately unassigned pending descriptive evaluation of finished behaviour.

[CONFLICT] The 13 September kit lists Gardener as "Development · Control · Support". Control and Support are not members of the Seven Archetypes. Recorded in [section 5](#5-gardener) rather than resolved; this roster entry is unchanged pending the owner's decision.

### Golem Master
Primary:
- Construction

Secondary:
- Combat

The 13 September kit lists Construction · Combat · Production and carries an explicit audit flag against promoting its wording to canon; Production is therefore recorded in [section 6](#6-golem-master) but not adopted here.

### Kitfighter
Primary:
- Combat

Secondary:
- Production

Confirmed by the 13 September kit: Combat · Production.

Kitfighter should not currently be listed as Extraction.

### Merchant — active design, not settled
Emergent archetype:
- Production

Do not assign further archetypes until finished behaviour is evaluated. See [section 13](#13-merchant--active-design).

### Settled beyond the original four
Skeleton Crew ([section 15](#15-skeleton-crew)) and Lightfooted ([section 16](#16-lightfooted)) completed conceptual passes on 12 September 2026. Skeleton Crew is Logistics and Combat; Lightfooted is Exploration primary with Combat secondary.

### Daredevil — kit supplied 13 September 2026
Exploration and Combat, per [K7]. The kit supplies the previously missing Ability 2 and ultimate. See [section 17](#17-daredevil). [OPEN] Whether this equals a completed conceptual pass is the owner's call.

### Unsettled drafts
Waxer is the only remaining draft with missing conceptual slots. See [section 14](#14-unsettled-class-drafts).

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

---

# 13. Merchant — Active Design

**ACTIVE DESIGN — not settled.** Merchant is the current active class-design subject. Structure below is recorded at the confidence level it actually holds: strong current structure, [FAINT CAUTION] for mechanics worth preserving but not conceptually settled, and [OPEN] for unresolved implementation, balance, or system questions.

Emergent archetype: **Production**. Archetypes are descriptive, not generative — do not assign Logistics, Exploration, or others until the finished behaviour is evaluated.

---

## Core fantasy

A fabulously wealthy travelling benefactor and opportunist whose economic success becomes spectacle. The presentation is fairytale and lavish: showering villagers with money, gaudy fanfare, pomp, patronage, prosperity, an entourage, procession, finery, and royal treatment.

Merchant is feast-or-famine, potentially enormous in both economic and combat utility, and an aloof, unserious, largely self-serving aristocrat. Merchant helps the team because the team happens to benefit from Merchant getting disgustingly rich.

Progression fantasy: a poor Merchant still has to get a job — farm, cut wood, gather requested resources, walk normally, personally craft. A successful Merchant increasingly escapes ordinary labour through capital and employees.

---

## Economic model

**Reputation** determines commercial relationship and willingness to work. Hiring should specifically require sufficient Trading Reputation / commercial relationship rather than merely any positive vanilla Reputation, and continued willingness to Work may depend on sufficiently positive overall Reputation. This lets curing and gratitude differ from actually establishing a business relationship, and means Swindle can eventually damage an employee relationship. [OPEN] Exact thresholds and vanilla-gossip implementation.

**Mastery** is the villager's vanilla-style progression: Novice, Apprentice, Journeyman, Expert, Master. Mastery determines productive capability and later Procession strength. Work itself advances Mastery.

**Payroll** is the Emerald cost/obligation of employed villagers. It is not a new currency.

**Employment**: sufficient commercial Reputation makes a villager willing to Work; Merchant then hires them onto Payroll. Do not automatically force every eligible villager onto Payroll. [OPEN] Firing/rehiring anti-exploit behaviour.

**Retinue** is the subset of employed workers physically mobilized around Merchant. Workforce is not the same as Retinue.

---

## Passive — Work

Villagers with sufficient commercial Trading Reputation toward Merchant become willing to Work for Emeralds. Merchant may employ willing villagers, placing them on Payroll. Performing Work advances Mastery, and higher Mastery makes Work more capable and materially efficient.

Work is a **Production** system: resources plus Emerald labour cost produce products. Merchant supplies the actual resources; workers do **not** generate missing raw resources.

Work is **not** profession-specific. Villager professions remain important to their ordinary vanilla trading relationships, but Work itself should use a universal paid-production abstraction.

Preferred interaction model: avoid a bespoke Merchant production UI or specialized input family if possible. Work should occur periodically and automatically from eligible supplied materials. Some RNG is acceptable and may be desirable. Available materials constrain the possible output pool — Primary Materials, Construction Blocks, and secondary materials — and the worker selects a valid vanilla-grounded transformation within their Mastery and Production Reach.

Initial obvious output families are equipment and processed/construction products. Do not permanently define Work as only tools and blocks; specialized products may later expand the vocabulary. Use vanilla recipes and transformations as the authoritative semantic vocabulary rather than mathematically inferring every possible product from arbitrary material categories.

Output RNG principle: uncertainty may determine **which** useful valid production occurs, while the system remains legible through the materials Merchant supplies.

---

## Mastery and the production curve

Work is **not** required to be economically superior at low Mastery. Poor employees may genuinely lose Merchant material. Mastery can provide a continuous production-efficiency curve rather than requiring a new mechanic at every tier.

| Mastery | Approximate conceptual behaviour |
| --- | --- |
| Novice | Potentially actively wasteful; may consume more material than equivalent ordinary crafting |
| Apprentice | Likely the realistic floor of Merchant employment, because the trading needed to establish a commercial relationship may already advance many villagers past Novice. Still inefficient or near-ordinary |
| Journeyman | Production becomes genuinely efficient |
| Expert | Meaningful preserved inputs, leftovers, or surplus |
| Master | Exceptional material utilization: primary craft, useful leftover material, and potentially additional secondary products from the same budget |

[OPEN] Tier breakpoints are not finalized. Do not spend equal design budget forcing Novice to matter; Novice employment may be a legitimate but uncommon edge case.

Mastery has two related dimensions. **Production Reach** is what transformations the worker can perform; **Production Efficiency** is how effectively they convert the supplied material budget. Production Reach should represent productive sophistication, depth, and complexity — not simply Novice equals wood and Master equals diamond. [OPEN] The exact Production Reach model.

---

## Emerald substitution

[FAINT CAUTION] Merchant may substitute an intentionally **exorbitant** quantity of Emeralds for some or all goods requested by a villager in a trade.

A poor Merchant must satisfy trade requests normally. An extremely rich Merchant increasingly decides whether acquiring a requested material is worth their time or whether to throw money at the problem. The exchange rate must be intentionally bad, and this should not make trading obsolete — trading remains important for commercial relationships and Reputation, villager progression, liquidity, Swindle opportunities, and workforce development.

Emerald substitution competes with Payroll for the same liquid capital. [OPEN] Placement in the passive or another system is unresolved.

---

## Ability 1 — Swindle

After trading with a villager, Merchant can strike them to Swindle them, converting or sacrificing some Reputation for bonus Emerald value from the completed trade. Once per villager per restock cycle; the villager is marked Swindled until a successful restock.

The economic tension is that Emeralds are liquid capital and Reputation is social/commercial capital: Merchant can finance their economy by damaging the relationships that allow the economy to exist.

### Branches

**Invisible Hand** — strong, near-settled. Swindling one eligible villager also Swindles other qualifying nearby villagers without physically hitting them, extracting bonus Emerald value from each while concentrating Reputation loss onto the struck villager. Nearby villagers must independently be eligible, traded, and not already Swindled.

**Credit Line** — [FAINT CAUTION] Commercial leverage. The intended question is whether Merchant can consume future relationship value now for immediate liquidity and convert that liquidity into enough acceleration before the exhausted relationship matters again. Exact mechanic **not locked**.

**Community Chest** — [FAINT CAUTION] Economic circulation and distributed value. The intended question is whether Merchant can keep enough value circulating through Work and commerce that redistribution beats retaining personal liquidity. Exact mechanic **not locked**.

Branch principle: every branch should be an economically irresponsible business model that becomes brilliant if Merchant correctly understands the economy they built. The failure state is "I built my business model around an economy that doesn't exist."

---

## Ability 2 — Retinue

Nearby employed workers respond to Merchant and accompany or follow them. Retinue mobilizes the existing workforce; it does not independently create employees.

Because Work has evolved away from profession-specific resource gathering, any older Retinue text assuming workers simply gather profession-specific materials should be re-evaluated.

### Branches

**Seize the Means** — [FAINT CAUTION / STRONG] Retinue sacrifices or pauses ordinary productive Work to target a designated area and **disable** enemy workstations and infrastructure — disable, not necessarily destroy. This naturally creates Splitpusher, Dismantler, Siege, and Disruptor behaviour if retained.

**Toll Patrol** — [FAINT CAUTION / STRONG] Retinue patrols and operates around Merchant in contested space. Ordinary productive Work is sacrificed or paused. Enemy presence and activity offset Payroll rather than directly generating arbitrary Emeralds.

**Overtime** — [FAINT CAUTION] Pay more or intensify labour to accelerate ordinary Work and/or Mastery. This is the **weakest** current branch concept and still needs to prove it creates a distinct playstyle rather than merely numbers going up.

---

## Ultimate — Procession / Silk Road

Merchant begins a moving procession in a chosen direction or toward a destination with nearby Retinue workers. Merchant leads by remaining within the procession radius and continuing to finance the workers. If Merchant abandons the procession or cannot meet its Emerald/Payroll requirement, the ultimate ends.

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

---

## Superseded Merchant material

[HISTORICAL] An earlier Merchant draft framed the hook as discovering, developing, exploiting, and connecting villages and economic locations, with a **Star Trading** passive granting bonus XP as villagers advanced through trade levels, and a **Silk Road** ultimate granting speed along established routes and transmitting allied enchantments and potion effects along the route. That design is **superseded** by the model above. The name Silk Road survives as an alternative title for Procession; the route-effect-transmission mechanic does not.

---

# 14. Unsettled Class Drafts

These are preserved as drafts. They are **not** settled classes, and their missing slots are missing design rather than gaps to be filled by invention.

**Waxer** is the only remaining draft. Skeleton Crew and Lightfooted were promoted to settled classes on 12 September 2026; see [section 15](#15-skeleton-crew) and [section 16](#16-lightfooted). Daredevil received a full kit on 13 September 2026 and moved to [section 17](#17-daredevil).

---


## Waxer

**Draft — not settled.** Primary Production; Combat currently plausible.

Core rule: Honeycomb can be repeatedly invested into existing products to preserve them against change; enough preservation becomes obstruction.

**Passive — Waxed Recipes.** Fill otherwise-empty crafting slots with Honeycomb to produce a Waxed version or output.

**Ability 1 — Wax-On.** Apply Waxed stacks to existing objects. Enough Wax on interactive blocks makes them Sealed; Sealed blocks cannot normally change or use their interactive state. The first Wax application to damaged unwaxed equipment restores durability. Branch concepts: **Amber** (faster or stronger sealing), **Restorative** (stronger initial restoration), **Sticky** (interaction with Waxed armour can punish attackers, for example Mining Fatigue while consuming Wax).

**Ability 2 — Wax-Off.** A thrown Honey Solution. The base ability needs to provide a repeatable route to Honeycomb acquisition. Branch seeds — **Enzymatic**, **Preserving**, **Floral** — are not equally developed and remain draft.

**Ultimate — Amber.** Complete preservation and stasis; delivery mechanism unresolved.

[OPEN] The earlier Seal proposal, eligibility, application, protection consumption, visible counterplay, ordinary removal, and implementation feasibility remain undeveloped. Do not create a global enemy-block-immunity rule merely to make Waxer work.

---

# 15. Skeleton Crew

### Canonical kit — 13 September 2026

**Source: [K7] seven-class kit handoff.** Reproduced as supplied. Where the developed material below elaborates this kit they agree; where it differs, this block is the current statement.

Archetypes: Logistics · Combat
Hook: Skeleton Crew wants the undead to find them: nighttime monster pressure becomes a workforce that can fight or move resources.

**Passive — Undead Affinity.** Hostile undead detect and pursue Skeleton Crew from substantially farther away than normal. Killing eligible hostile undead generates Crew, which can be spent to deploy Crew Members.

Each Crew Member can exist either actively in the world or be committed as Capacity to a Supply Line, never both.

**A1 — Graveyard Shift.** Target an enemy to Strike them for bonus damage and provoke active Crew against them. Otherwise, consume Crew to Raise a Crew Member.

- **Field Work** — Graveyard Shift becomes a projectile: striking enemies at range or Raising Crew where it hits valid terrain.
- **Hard Hat Zone** — Strike friendly Crew for reduced damage to temporarily equip them with armor, progressing through Helmet → Chestplate → Leggings → Boots with repeated Strikes.
- **Labored Union** — Reduce Strike's bonus damage; Crew automatically fight nearby mobs, and their kills generate Crew.

**A2 — Burning Out.** Ignite active Crew. Burning Crew move substantially faster while continuing to prioritize Combat → Logistics → self-preservation, seeking to extinguish themselves once higher-priority work ends.

- **Fire Drill** — Do not ignite Crew. Instead, they become Alarmed and urgently complete their current logistical journey or return to Skeleton Crew.
- **Deadline** — Crew that remain burning in combat long enough explode.
- **Water Break** — Successfully extinguishing restores Health based on time spent burning and briefly increases movement speed.

**Ultimate — A-Head of Schedule.** Summon the Headless Horseman and a temporary supernatural Crew. They commit to either Combat or Logistics according to Skeleton Crew's workforce activity and become exceptionally effective at that activity for the ultimate's duration.

---

**Conceptually settled, 12 September 2026.** Hook, core rule, passive, both actives with three mutually exclusive branches each, and the ultimate are decided. Remaining work is numbers, implementation, tags, radii, durations, and interaction with global systems. Do not reopen settled mechanics for lack of exact values.

Archetypes, assigned descriptively from the finished kit: **Logistics** and **Combat**.

### Core fantasy

Night and darkness produce undead pressure; Skeleton Crew attracts that pressure; fighting undead generates Crew; Crew becomes labour, logistical, and combat capacity.

> Skeleton Crew wants the undead to find them.

### Resource terms

**Crew** is the stored resource the class generates. **Crew Limit** is the maximum workforce simultaneously deployed or committed. Crew Limit is not Logistics Capacity and is not a universal summon cap; stored Crew may exceed deployed Crew Limit, and Crew Limit should not automatically scale with level. [OPEN] Exact values.

Each Crew Member is either physically active in the world **or** committed to Supply Line Capacity, never both. A Crew Member's physical cargo Capacity and its possible committed Supply Line Capacity are the same underlying capacity and must never be counted twice. Summoning is therefore withdrawing logistical capacity from your infrastructure. Capacity and Item Rate follow the global definitions in [infrastructure.md](infrastructure.md#committed-capacity); the superseded Flow Weight terminology is not used.

### Passive — Undead Affinity

Hostile undead detect and pursue Skeleton Crew from substantially farther away than normal. Killing eligible hostile undead generates Crew stacks. This deliberately converts nighttime and undead danger into economic opportunity. [OPEN] Detection radius, eligible undead, Crew generation values, Crew Limit.

### Base Crew behaviour

Active Crew Members are physical workers and combatants with the priority order **Combat → Logistics → self-preservation**. Empty-handed Crew follow Skeleton Crew. Cargo-carrying Crew seek the nearest valid Supply Line node, deposit, then follow. Provoked Crew fight, then resume previous behaviour. Crew likely retain cargo while fighting and spill it if killed. Ordinary sunlight can burn Crew like undead unless otherwise protected.

### Ability 1 — Graveyard Shift

Context-sensitive. Against a valid hostile target, the ability itself performs a **Strike** dealing bonus damage and provoking Crew against that target. Otherwise it consumes Crew to **Raise** a Crew Member. This is not an empowered next normal attack; the ability performs the Strike directly.

**Field Work.** Graveyard Shift becomes a projectile. Impact on an enemy or mob performs the Strike at the impact point; impact on valid terrain may consume Crew to Raise a Crew Member there. It does not both Strike and Raise from one impact. Remote raising plus ranged Strike.

**Hard Hat Zone.** Graveyard Shift may Strike friendly Crew for reduced damage. Successive friendly Strikes temporarily equip that Crew Member in order — helmet, chestplate, leggings, boots — and each additional Strike refreshes the timeout of all armour granted this way, including further hits at full armour. The helmet naturally protects against sunlight ignition. The costs are friendly Strike damage, spent A1 opportunities, and maintenance time; do not invent an arbitrary Capacity penalty. [OPEN] Armour material, timeout, friendly damage.

**Labored Union.** Graveyard Shift's hostile Strike deals reduced bonus damage. Crew automatically become Provoked by any nearby mob, and kills made by Crew Members generate Crew stacks. "Any" is intentional where technically feasible — enemy players, hostile mobs, cows, villagers — and Crew generation is deliberately **not** restricted to undead victims under this branch. The result is an autonomous, self-replenishing combat workforce. Withdrawal of infrastructure Capacity is an emergent consequence of the global allocation rule, not a branch-specific penalty.

### Ability 2 — Burning Out

Active Crew that are burning move substantially faster. The burning state may come from any legitimate source — Burning Out itself, sunlight, environmental fire — and Burning Out reliably sets affected active Crew on fire.

Burning does not override the priority hierarchy. When no higher-priority responsibility remains, burning Crew seek a valid way to extinguish themselves. Consequences worth stating: shade does not extinguish an already burning entity; helmets prevent sunlight ignition but not actual fire; vanilla burning melee contact already supplies its own fire interaction, so no redundant combat rider is added; faster physical traversal naturally raises observed Item Rate, and the ability must never say "+Item Rate"; and damaged Crew can be deliberately risked and replaced, though this is not an instant workforce reset.

**Fire Drill.** Burning Out no longer ignites affected Crew. They become **Alarmed** instead: they abandon combat and urgently move toward their current logistical destination, or urgently return to Skeleton Crew if they have none. This is emergency relocation and task completion, not teleportation, recall, or instant cargo banking. Alarmed is the conceptual counterpart to Provoked.

**Deadline.** Crew that remain in combat while burning for long enough explode violently. Leaving combat before the threshold prevents it, and merely being on fire is insufficient. This interacts with the hierarchy: a burning cargo carrier that meets an enemy fights because Combat outranks Logistics, and a sustained fight ends in an explosion that may spill cargo, while a fight that ends quickly returns the Crew Member to work still burning. An enemy can disengage to prevent the explosion, indirectly letting the worker continue. [OPEN] Threshold, conceptually around three seconds.

**Water Break.** Crew that successfully extinguish themselves after burning restore Health proportional to time spent on fire, then move faster briefly. The lifecycle is burn, overdrive through higher-priority responsibilities, seek extinguishing, heal, brief refreshed movement, resume work. Any legitimate burning source may qualify. [OPEN] What counts as successfully extinguishing — rain, another player, and similar cases are deliberately not over-specified yet. Hard Hat Zone sits in intentional soft tension with this branch: the helmet suppresses incidental sunlight ignition while Burning Out can still start a controlled burn.

### Ultimate — A-Head of Schedule

Summon the Headless Horseman and his own temporary Crew. Their specialization is determined by what the ordinary workforce is doing at activation or first meaningful state. If ordinary Crew are fighting, the Horseman workforce commits to Combat, fights until combat ends, and remains ready for further Combat rather than switching to Logistics. If ordinary Crew are performing Logistics, it commits to Logistics, works immediately, and continues even if ordinary Crew are later pulled into combat. If ordinary Crew are inactive, it waits and commits to the first new Combat or Logistics state they enter. Once selected the mode is fixed for the duration rather than continuously mirroring ordinary Crew, and the Horseman workforce is more effective at the selected activity.

Temporary Horseman Crew are separate from normal Crew Limit, may physically perform Logistics, but do **not** become persistent Supply Line Capacity: temporary labour cannot create permanent automated infrastructure. [OPEN] Horseman size, stats, duration, specialization strength.

The fantasy: the project is behind schedule, so Skeleton Crew brings in another supervisor and a specialized temporary shift.

---

# 16. Lightfooted

### Canonical kit — 13 September 2026

**Source: [K7] seven-class kit handoff.** Reproduced as supplied. Where the developed material below elaborates this kit they agree; where it differs, this block is the current statement.

Archetypes: Exploration · Combat
Hook: Wolves, foxes, and cats/ocelots are mobile resources whose presence changes Lightfooted's abilities, while Lightfooted lets the whole group traverse terrain in extraordinary ways.

**Passive — Animal Senses.** Nearby animals provide stacking species-specific bonuses to Lightfooted and nearby animals of that species:

- **Wolves** — Toughness: damage resistance.
- **Foxes** — Quickness: movement speed.
- **Cats/Ocelots** — Surefootedness: fall-damage reduction.

**A1 — Lunge.** Leap toward the aimed location, damaging an enemy struck during the leap.

- **Swarming Bite** — Wolf: Shortest extension, but nearby wolves enable more frequent Lunges.
- **Thieving Swipe** — Fox: Longer Lunge; striking an enemy temporarily disables their current mainhand item, with nearby foxes extending the disable.
- **Stalking Pounce** — Cat/Ocelot: Greatest Lunge. Enter a brief slowed stalking stance before automatically launching toward the current aim.

**A2 — Bounding.** Continuously traverse through a sequence of low Bounds. Each Bound commits to a direction until landing, where Lightfooted can redirect. Nearby animals Bound alongside.

- **Drift to Drift** — Fox: Taking off from snow grants Invisibility until the next Bound leaves the ground.
- **Branch to Branch** — Cat/Ocelot: Taking off from leaves launches higher and faster.
- **Track to Track** — Wolf: A normal Bound from dirt-family terrain grants one bonus Bound; bonus Bounds cannot generate additional ones.

**Ultimate — Rabbit's Lucky Foot.** Target an area. Nearby wolves, foxes, and cats/ocelots make enormous protected leaps toward it, followed shortly by Lightfooted. Participating animals are invincible during the forced leap and landing; landing on enemies deals damage, with Lightfooted's own landing dealing greater damage.

---

**Conceptually settled, 12 September 2026.** Hook, core rule, archetypes, passive, both actives with three mutually exclusive branches each, and the ultimate are decided. Remaining work is numbers, implementation, tags, radii, durations, and visual communication.

Archetypes: **Exploration** primary, **Combat** secondary. Development is not assigned merely because animals are managed, and Logistics is not assigned merely because animals are moved.

### Hook and core rule

Lightfooted treats wolves, foxes, and cats or ocelots as **mobile resources**, using their different vanilla lure methods to gather and keep them nearby. Their presence strengthens the group and changes Lightfooted's abilities, while their movement inspires Lightfooted's own extraordinary traversal.

> Lightfooted treats particular woodland animals as mobile resources that must be kept physically nearby.

This is deliberately **not** a generic companion-animal or tamed-animal system. The class cares specifically about wolves, foxes, and cats/ocelots, which avoids ownership and taming edge cases and prevents unrelated animals such as horses from qualifying. The animals need not be technically tamed in vanilla terms; physical proximity is the class state. Animals influence Lightfooted, and Lightfooted enables and empowers the animal group, so the player cares about finding the species, using their different lure relationships, keeping them nearby, and transporting the resulting living resource group through difficult terrain.

### Passive — Animal Senses

Nearby wolves, foxes, and cats/ocelots grant small stacking species-specific bonuses that apply both to Lightfooted and to nearby animals of those species. Each species caps independently.

| Species | Bonus | Effect |
| --- | --- | --- |
| Wolves | Toughness | incremental damage reduction |
| Foxes | Quickness | incremental movement speed |
| Cats / Ocelots | Surefootedness | incremental fall-damage reduction |

These must be granular **custom** bonuses. Do not grant vanilla Resistance or Speed tiers, which are far too coarse for one nearby animal. One animal is a small but meaningful contribution, several are a recognizable specialization, and the cap prevents unlimited accumulation from breaking stats. [OPEN] Percentages, radii, caps.

### Ability 1 — Lunge

Lightfooted leaps toward the aimed direction, damaging a struck target. The three branches form a deliberate spectrum from smallest leap and highest combat potency to largest leap and weakest direct combat payoff.

**Swarming Bite — wolf.** The smallest increase to the leap itself. Nearby or following wolves enable increasingly frequent or repeated Lunges. This is the combat-heavy branch: repeated pounces, pack pursuit, and the highest ability to stay on a target. [OPEN] Wolf scaling, cooldown or charge behaviour.

**Thieving Swipe — fox.** The Lunge travels moderately farther than base. Striking an enemy player temporarily disables their **current mainhand item**, with nearby or following foxes increasing the duration up to a cap. This is the middle branch: moderate mobility plus combat disruption. Do not revert to the older offhand-only concept. [OPEN] Duration, scaling, cap.

**Stalking Pounce — cat/ocelot.** The strongest movement improvement. Activation first pulls Lightfooted into a brief, heavily slowed stalking stance; after a short **fixed** windup they automatically launch toward the current aim direction at greatly increased velocity. There is no hold-and-release input, it is not a literal sneak state, the player may keep aiming during the windup, and it cannot be charged indefinitely. Bow-draw-like field-of-view language may communicate the physical coiling. Do not add another damage or crowd-control rider; this branch is dramatic movement and ambush positioning.

> Input-design lesson worth preserving: prefer press to a deterministic state sequence over hold and release, unless continuous charging is indispensable.

### Ability 2 — Bounding

Lightfooted begins a **continuous** sequence of low Bounds. Each Bound commits to its launch direction; the player may choose a new heading on landing but cannot substantially redirect a Bound in mid-air. The sequence cannot be paused or voluntarily held between Bounds — once it begins, the remaining Bounds must be taken.

Lightfooted **cannot attack** while Bounding but **can build**. Building access is intentional and important: it lets skilled players manipulate upcoming terrain, landings, and takeoffs while already moving. Animals travelling with Lightfooted Bound alongside them.

Bounding should feel closer to boat-like momentum over land than a series of independently aimed combat dashes: launch, committed trajectory, land, choose next heading, immediately launch again. The ability is about intentional movement across the whole sequence.

**Drift to Drift — fox, snow.** Landing a Bound on snow grants Invisibility until the next Bound leaves the ground. Because Bounding cannot pause, this is not stationary stealth: a visible arc, a landing in snow, a brief disappearance, then immediate emergence into the next committed Bound. The enemy knows where Lightfooted entered the drift but briefly loses certainty about the next heading. The previously considered high-jump effect is not part of this branch. Fox's branch is **hide between Bounds**. [OPEN] Snow-family definition, invisibility timing.

**Branch to Branch — cat/ocelot, leaves.** Bounds taking off from leaves launch higher and faster. This is the strongest individual-Bound specialization and supports canopy traversal. It is not an "extreme" leap; the ultimate owns the truly extreme vertical launch. Cat's branch is **stronger individual Bounds**. [OPEN] Leaf tags, velocity.

**Track to Track — wolf, dirt.** A **normal** Bound taking off from eligible dirt-family terrain grants a **bonus** Bound. Bonus Bounds cannot generate further bonus Bounds, which prevents self-sustaining infinite movement without an arbitrary low cap. A skilful sequence alternates normal dirt takeoff and bonus Bound repeatedly; long back-and-forth traversal is not inherently undesirable, and the restriction exists only to stop recursion. Dirt is the contextual anchor because the fantasy is tracking through trackable ground. Wolf's branch is **more Bounds**. [OPEN] Dirt-family block tag, base Bound count.

A2 branches deliberately do **not** scale with animal count. A1 already uses nearby animals as its resource; A2 uses terrain and movement execution as its resource. Do not force animal-count scaling onto A2 for symmetry.

### Ultimate — Rabbit's Lucky Foot

Target a location. Nearby wolves, foxes, and cats/ocelots leap in extremely high arcs toward the target area, and after a short delay Lightfooted launches after them. Participating animals are **invincible** throughout the forced leap and landing, because they are valuable class resources and the ultimate forcibly commits them into dangerous space. Each animal landing on an enemy player deals damage; Lightfooted's own landing deals greater bonus damage. The extreme high leap belongs here rather than to Branch to Branch.

After landing, ordinary protection ends and the group is physically concentrated near Lightfooted again, naturally re-establishing Animal Senses at the destination. [OPEN] Targeting geometry, animal spread, impact radius, animal damage, Lightfooted bonus damage, delay, invincibility end timing.

---

# 17. Daredevil

**Kit supplied 13 September 2026. Source: [K7].** Daredevil was previously a substantially incomplete draft with no Ability 2 and no ultimate ([section 14](#14-unsettled-class-drafts)). The kit below supplies both and is reproduced as given. [OPEN] Whether this constitutes a completed conceptual pass equivalent to Skeleton Crew's and Lightfooted's is the owner's call; it is not asserted here.

Archetypes: Exploration · Combat
Hook: Everyone else uses clutch techniques to cancel dangerous momentum. Daredevil converts dangerous momentum into something better.

**Passive — Skydiver.** Remaining airborne long enough grants extreme airborne movement, rewarding Daredevil for sustaining dangerous aerial traversal rather than returning safely to the ground.

**A1 — Runway.** Convert meaningful forward momentum into a forward-and-upward launch, turning an existing run into aerial traversal.

- **Pop Rocket** — Use Wind Burst/Wind Charge behavior for more explosive propulsion.
- **Trampoline** — Use slime-like rebound behavior to extend or redirect the stunt.
- **Suplex** — Activating Runway while touching an enemy subjects them to Runway's corresponding upward and downward velocity bursts.

**A2 — Crash Landing.** At sufficient velocity, deliberately Crash into terrain, abruptly ending movement. Daredevil takes a fixed amount of fall damage while nearby enemies take damage based on the velocity lost in the impact. Daredevil's next instance of fall damage is then negated.

- **Crater** — Take greater fixed fall damage and deal less impact damage, but stun affected enemies based on Crash velocity.
- **Combat Roll** — Take minimal fall damage and deal greatly reduced immediate impact damage; instead, empower the next attack based on Crash velocity.
- **Superhero** — Take no self-damage and retain normal impact damage, but become simultaneously invincible and unable to act while holding a superhero landing pose.

**Ultimate — Deathly Clutches.** Drop to near-death Health and negate the next N damage instances. During the ultimate, a limited number of successful attacks restore already-consumed damage-negation instances, never exceeding the initial maximum.

[HISTORICAL] The earlier draft's Runway branches — **Cannon Jump**, **Trampoline**, **Mach Headbutt** — are superseded by Pop Rocket, Trampoline and Suplex above. The earlier Passive wording, which specified more than one second airborne and granted extreme Speed, is superseded by the Skydiver wording above. The earlier tentative Extraction archetype is not carried forward; the kit assigns Exploration and Combat.

[OPEN] N in Deathly Clutches, the attack count that restores instances, Skydiver's airborne threshold, Runway's momentum requirement, Crash velocity thresholds, and all durations remain unresolved.

---
