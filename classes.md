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

> **[SUPERSEDED — 26 September 2026.]** The Lv1–30 skeleton below, its universal
> capacity growth, and its player-chosen capacity specialization at Levels 3, 18
> and 24 are replaced by the **Growth and Task** model in
> [docs/design/CLASS_PROGRESSION_GROWTH_AND_TASK.md](docs/design/CLASS_PROGRESSION_GROWTH_AND_TASK.md).
>
> What changed: the clock now runs **Lv0–30** with the Passive at Lv0; capacity
> is no longer universal or chosen but **class-authored and automatic (Growth)**
> at Levels 3/6/9/12/15/18/21/24/27/30; player choice moves to **Task** at
> Levels 4/8/12/16/20/24/28, seven allocations across Yield, Efficiency and
> Slaying; and Health is expressed on a ×10 player-facing scale. The ability
> schedule is unchanged in substance — Passive, A1, A2, A1 branch at 5, A2 branch
> at 10, Ultimate at 15.
>
> Retained below for traceability. Do not implement from this section.

The current working match progression spans Levels 1–30.

The level curve intentionally contains distinct breakpoints rather than treating every level as an equivalent incremental increase. Some rewards establish class abilities, some accelerate ordinary Minecraft activity, some expand fundamental player capacity, and others open or deepen persistent world specialization.

Current working breakpoint structure:

| Level | Progression event |
| ---: | --- |
| 1 | Ability 1; begin at 9 Health, 9 Hunger, and 6 inventory slots |
| 2 | Ability 2; universal capacity growth |
| 3 | Capacity specialization I |
| 4 | Task choice; universal capacity growth |
| 5 | Ability 1 upgrade |
| 6 | Infrastructure specialization / recognition entry |
| 7 | Proof level; universal capacity growth |
| 8 | Task choice; universal capacity growth |
| 9 | Development III; universal capacity growth |
| 10 | Ability 2 upgrade |
| 12 | Development IV; Task choice |
| 14 | Universal capacity growth / quiet proof level |
| 15 | Ultimate unlock |
| 16 | Task choice; current working mobility breakpoint |
| 17 | Universal Health and Hunger floors reach 20; Hunger above 20 is represented through exhaustion efficiency |
| 18 | Capacity specialization II |
| 19 | Quiet late-mature-state proof |
| 20 | Task choice |
| 24 | Development VIII; Task choice; capacity specialization III |
| 25 | Quiet |
| 28 | Task choice |
| 30 | Development X / class completion; exact form unresolved |

Levels not listed here may still provide universal capacity growth according to the universal growth rules.

[HISTORICAL] The 12 September capacity curve proposal ([docs/proposals/2026-09-12-capacity-curve.md](docs/proposals/2026-09-12-capacity-curve.md)) was never adopted and is now moot: it reworked universal capacity and specialization, both of which the Growth model removes. Its one settled decision — that Hunger cadence must produce integral values, because a half food point is a quarter drumstick vanilla cannot draw — survives as a constraint on any Hunger curve, and Mole's +2 per Growth satisfies it.

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

Task-progression rewards can coexist with universal capacity growth. A Task
choice does not replace the universal growth scheduled on the same level.

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

Task progression condenses ordinary Minecraft economic and world-interaction time as the match advances. **Working direction, 25 September 2026:** this condensation is not a promise that every later tier is simply a larger version of the earlier tier. A later tier occurs in a different world state and should answer the strategic problem created by that state.

The current working domains are:

- Efficiency — the Efficiency enchantment family; improves ordinary block-breaking and worldwork rate.
- Yield — Fortune + Looting; improves the amount of useful material obtained from successful resource-producing or acquisition actions.
- Damage — Sharpness + Power; improves conventional melee and ranged damage.

Current working cadence:

- Levels 4, 8, 12, 16, 20, 24, and 28: choose one Task branch — Yield,
  Efficiency, or Slaying — and unlock that branch if it has not been chosen or
  advance it if it has.

Efficiency, Yield, and Damage currently have an intended generic maximum of Tier III. **This ceiling remains a provisional generic-balance constraint, not a settled statement that late specialization must stop at an ordinary enchantment tier.** Later Task rewards may express decisive, conditional capabilities that are not equivalent to another generic enchantment level, subject to balance and implementation review.

There is no universal Yield or Efficiency onboarding under this model. Every
Task opportunity is a meaningful commitment, and repeated choices can produce
specialization while mixed choices preserve breadth. A player may eventually
have, for example, Yield V / Efficiency II, Yield III / Efficiency III /
Slaying I, or Slaying IV / Efficiency III, subject to the final branch-depth
model.

The Tier III ceiling is intended to preserve room for class-specific amplification. Generic high-tier enchantment scaling should not make specialized class mechanics redundant or cause uncontrolled multiplication with them. Mole's Tunneling, which amplifies current digging-tool speed, is an important example.

Task advancement is an additive progression layer and may occur on the same
level as universal capacity growth. The branches are not assumed to have seven
literal enchantment ranks: after the useful generic tiers, further investment
may become qualitative mastery or capstone effects while preserving the same
choice grammar.

### Class curve template — Working direction, 25 September 2026

The class curve is not a second independent thirty-level ability tree. It fills
three coordinated clocks:

- **Development every three levels** develops the character engine and, later,
  the class's persistent world footprint;
- **Task every four levels** chooses or advances Yield, Efficiency, or Slaying;
- **Ability acquisition and branching** establishes a readable kit early.

Default class template:

| Level | Event | Authoring job |
| ---: | --- | --- |
| 0 | Passive / class engine | Establish identity |
| 1 | Active 1 | Acquire first verb |
| 2 | Active 2 | Acquire second verb |
| 3 | Development I | Establish body and engine direction |
| 5 | Ability 1 branch | First build commitment |
| 6 | Development II | Operationalize the engine |
| 9 | Development III | Expand capacity, reach, or complexity |
| 10 | Ability 2 branch | Second build commitment |
| 12 | Development IV + Task | Establish methodology |
| 15 | Development V + Ultimate | Integrate the complete kit |
| 18 | Development VI | Mature the personal/class state |
| 21 | Development VII | Begin explicitly supernormal world expression |
| 24 | Development VIII + Task | Master infrastructure or footprint |
| 27 | Development IX | Integrate and extend the footprint |
| 30 | Development X | Complete the class methodology |

The ×5 ability cadence is not permanent. After the Level 5 and Level 10
branches and Level 15 ultimate, abilities are considered mechanically complete
unless a future class design explicitly proves otherwise. Later power should
normally come from Development, Task specialization, and interaction with the
world rather than repeated mutations of already-readable abilities. An ultimate
is acquired as a complete ability, not deliberately left unfinished for later
levels.

**[CLARIFIED 27 September 2026] "Mechanically complete" means no ADDITIONS, not
no change.** The rule forbids a class gaining new things that happen after
Lv15 — a new effect, a new interaction, a new mode. It does not forbid an
existing number moving. Scaling is scaling: it invents nothing, and a player who
has read the ability still knows what it does.

So a passive whose cooldown shortens at Lv25 is permitted; a passive that starts
doing something new at Lv25 is not.

## Cast modes and targeting previews

**Settled 28 September 2026.** Block- and area-targeting abilities have a
problem the rest do not: until they fire, **nothing shows what they will
affect**. Mole's tunnelling is the live example — direction and the blocks it
will attempt are undefined until it starts moving and blocks start breaking,
which is to say until it is too late to have aimed.

A targeting preview answers that, and *when to commit* is a question about the
**player**, not the ability.

| Mode | Press | Then |
| --- | --- | --- |
| **Quick cast** | fires | — |
| **Hold to cast** | previews | release fires |
| **Double cast** | previews | same input fires; **any other ability input cancels** |

Quick is the default and is what every ability does today, so the setting adds
a capability without changing anyone's muscle memory until they ask for it.

### What a mode must never change

> A mode decides **when** the commit happens and whether a preview is drawn in
> between. It never changes what is cast, where, or for how much.

An ability that behaved differently by cast mode would make the setting a
**power choice** rather than an input preference, and players would pick the
strongest rather than the one they can use. Double cast is already the slowest
and the only one that can be aborted; that is its whole trade.

Abilities that target nothing — a self-buff, a channel with no aim — **ignore
the mode** rather than growing a preview with nothing in it.

### A branch may change an ability's input form

**Settled 28 September 2026.** An ability that shoots a projectile may have a
branch that makes it a **held beam** instead. Those are different input shapes —
one commits and is done, the other runs while held — and a branch is allowed to
change that.

It needs no new machinery. The preview is asked for **per activation**, with the
branch in hand, so an ability presents one form per branch and a player who
switches branch changes input form with it.

The composition rule falls out of what an empty preview already means:

> **An ability with its own hold semantics returns no preview, and therefore
> ignores cast modes.**

That is exactly right for a beam. Hold cast commits on **release**, and a beam
needs the button still down to sustain — the two want the same button for
opposite purposes. A beam also has nothing to pre-commit to: it goes where you
look *while it runs*. So the form that carries its own hold opts out of the one
a cast mode would impose, by the same route a self-buff does.

[OPEN] Whether a branch may change which *slot* an ability occupies. It cannot
today, and given the A1/A2 hold asymmetry above, moving an ability between them
would change its feel as much as its binding.

### The preview is particles, on the affected blocks, for the caster alone

**Settled 28 September 2026.** An ability that targets returns the blocks it
would affect, and those blocks are marked with a particle each.

**Opponents do not see it.** `Player.spawnParticle` sends to one player;
`World.spawnParticle` sends to everyone. That one call is the whole of it — and
it is the right answer rather than a cautious one, because a preview is
**pre-commitment information** in a way a cast is not. Double cast can be
feinted for free, so a visible preview would make aiming a liability rather
than a convenience.

**Occluded blocks are marked anyway.** A tunnelling preview marks blocks inside
stone that nobody can see, and that is correct: the preview's job is to say
*which blocks*, not to be a picture. A run that vanished where it entered the
hill would answer worst exactly where the ability is most opaque.

One mark at each block's centre rather than an outline — twelve edges per block
turns a twenty-block run into a haze, while centres stay countable.

### Release is detected by absence, and that makes Hold a heuristic

**Bukkit has no button-up.** There is no release event, and `org.bukkit.Input` —
the 1.21.2 raw input API — carries movement keys only, not clicks.

What *can* be read is that a held button keeps arriving: the client repeats its
packets while aimed at a block. So release is **the input going quiet for longer
than that gap**.

> **Double cast is exact. Hold is a heuristic.**

A Hold cast that fires early was a dropped packet, and no amount of tuning makes
it a promise. This is worth knowing before either is blamed for a bug.

### The quiet window is also a tap's latency floor

The window is not only a tolerance for holding. It is the delay a player who
merely **tapped** pays before anything happens — and that is the wrong trade for
the case Hold exists to serve.

> A player hovering an Ultimate over a bridge *also* meets the enemy already
> standing on it. They should not pay a third of a second to get the same cast
> out.

**A tap under Hold should behave as Quick cast does.** The two requirements pull
opposite ways: the window must *exceed* the client's repeat gap or a genuine
hold fires between packets, and it must be *short* or every tap is late. The
repeat gap is the only thing that settles it.

And that gap **differs by input** — right-click is throttled to roughly four
ticks, left-click repeats far faster while held — so one number is wrong for
both. The window is per input.

[FIXTURE — expect to tune] Left 2, right 5, inferred from vanilla's use cadence
rather than measured. `abilities.aim.logInputGaps` logs the real gaps; set it
and hold each button.

### A1 and A2 do not hold identically, and that is a design input

Because the windows differ, **the slot an ability occupies changes how it
feels to hold**:

| | Window | A tap fires in | Holding is |
| --- | ---: | ---: | --- |
| **A1** (left click) | 2 ticks | ~0.1s | tolerant — the input repeats fast |
| **A2** (right click) | 5 ticks | ~0.25s | tighter — throttled to about 4 ticks |

**Whenever hold timing matters to an ability, this belongs in the A1/A2
decision.** An ability whose value is in releasing at an exact moment — an
Ultimate hovering over a bridge — wants A1, where a tap is nearly instant and a
hold is least likely to be broken by a dropped packet. An ability held for
duration rather than for a moment does not care, and can take A2.

This is not a balance knob to tune per ability; it is a property of the two
inputs that ability placement has to account for.

An aim nobody resolves hits a ceiling, and the two modes resolve it opposite
ways: **Hold fires** (the button was evidently still down) and **Double cancels**
(no second press ever came). Firing an unconfirmed Double cast at the ceiling
would cast something the player had decided against.

## The lobby carries no match state

**Settled 28 September 2026.** In the lobby there is **no MOBA state at all** —
no offhand map, no locked inventory slots, no vitals bars, no progression
scoreboard, no Work Point accrual.

This was not true, and the cause was that every subsystem gated on
*enrollment*, which is permanent. A player standing in the lobby was carrying a
match: **the lobby was a match with no opponents.**

The rule is the **world**, not a flag. MOBA state applies while standing in the
match instance; the lobby, the drafting colosseum and the hub are not it. That
is legible from where you are standing — which no boolean could be — and it
cannot drift out of sync with the match lifecycle because it *is* the match
lifecycle: the instance world is created per match and discarded with it.

Deliberately **not** "is a participant". A spectator or an admin standing in the
instance sees the same world state as everyone else in it.

### Settings live there, as items

Because the lobby inventory is now the player's own, it has room for something
that is not a menu. Settings are **immovable items** — the same device the
offhand map already uses — one slot per setting, clicked to cycle.

An item is **visible without being opened**, which a chest GUI is not: a player
who has never heard of cast modes still sees it. Adding a second setting is
adding a slot, which is the property that keeps this from becoming a menu.

**Preferences outlive matches.** Cast mode is stored separately from the
match-scoped class state that a reset clears — a statement about how a person
plays must survive a reset, a class change and a new match, or every match
begins by re-choosing an input scheme.

---

## Target-conditional abilities

**Settled 28 September 2026.** An ability activated in Ability Mode may resolve
differently depending on **what it was aimed at** and **what the player is
holding**, and the two conditions compose.

### The interaction is never entered

Ability Mode cancels `PlayerInteractEvent` unconditionally, *before* dispatching
the input. So using A1 or A2 on a chest does not open it, a lever does not flip,
a button does not press. The click is consumed by the ability.

That is the load-bearing half of this pattern, and it holds in both directions:

> An ability whose conditions are **unmet** is refused, or succeeds in a
> different state. It never falls through to the block's own behaviour.

A mode that sometimes passed clicks through would be a mode nobody could trust —
a player could not know, before clicking, whether they were about to cast or to
open a container.

### The block is carried, not re-found

`AbilityContext` carries the clicked block and face. An ability could instead ask
`getTargetBlockExact` when it runs, and would usually get the same answer — but
that re-raycasts from the eye at a range the ability picks, with its own handling
of fluids and passable blocks, so it disagrees at reach edges. **"Used *on* block
a" and "whatever I am looking at now" are different predicates that happen to
agree most of the time**, and an ability that states a precondition should be
able to check the one it means.

The block is **null** for an activation from air, from Swap Offhand, or from a
damage event. That is a real answer rather than a missing one: it is what lets an
ability refuse cleanly instead of silently acting on whatever was behind the
target.

### The shape

```
A1 / A2  +  held item  +  target
   |          |             |
   |          |             +-- block, face, or nothing
   |          +-- read from the main hand; Ability Mode only occupies the offhand
   +-- LEFT_CLICK / RIGHT_CLICK, already cancelled
```

**[ILLUSTRATION, not a commitment]** A Chef class with a *Spatula* passive:
holding a shovel or hoe, A1 is a strike inflicting some crowd control; used on a
**cooking block that contains food**, it instead increases that block's output
scaled by tool tier.

One ability, two resolutions, chosen by the target rather than by a mode the
player has to remember they are in. The held-item condition is what makes the
combat use a deliberate loadout choice rather than a free extra button, and the
tool tier gives the cooking use a progression axis that costs real material.

### Two things this pattern must not become

**A hidden third input.** If an ability resolves three or more ways by target,
the player is memorizing a table rather than reading a situation. Two
resolutions, with the fallback being the ordinary one, is the readable limit.

[OPEN] **Refusal legibility.** A condition that fails needs to say which one
failed — wrong tool, wrong block, empty block — and the refusal line carries one
message. Whether that is enough, or whether target-conditional abilities need
their own phrasing, is unsettled.

---

## Movement and input statuses

**Settled 27 September 2026.** Two distinct statuses, deliberately separated,
because collapsing them produces crowd control that removes a player from the
game rather than from a position.

### Root

> **Root negates all prior axis displacement for one tick, then prevents
> movement input — WASD, sneak and jump — for its duration. Nothing else.**

Three consequences, all intended:

- **It is not a freeze.** Physics keeps running. A player rooted mid-launch
  stops dead for one tick and then *falls* for the remainder, because gravity
  is not an input. Root cannot suspend someone in the air for a team to hit.
- **It does not stop external displacement.** Knockback, pistons and explosions
  still move a rooted player, and they cannot steer or resist — so a rooted
  target travels exactly where they are sent.
- **Interaction survives it.** A rooted player may still attack, aim, place
  blocks, mine, drink, bucket and pearl. Root closes running, jumping and
  air-steering; it closes **no** clutch.

That last point is the load-bearing one. **Root alone traps nobody.** A trap
needs movement denial *and* interaction denial, and Root supplies only the
first — the second comes from geometry, which is why real trapping builds shafts
out of signs and crafting tables rather than looking for a stronger status.

**Roots refresh; they do not stack.** Rooting an already-Rooted target resets
the remaining duration rather than adding to it, so no number of simultaneous
applications produces a hold longer than one Root.

That is deliberate. Under stacking, anything able to apply many Roots at once
produced absurd holds — a bank of thirty-two applications would have been 320
ticks from a single instant. Refreshing makes a long hold cost **one
application per duration**, so it must be paid for across time rather than
bought in a lump.

### Stun

> **Stun prevents all player input.**

The strictly stronger effect, and the one that does close clutches. It should
therefore be rarer, shorter, or more expensive than Root wherever it appears.

**Its current owner is Mole's Undermine branch** — "emerging upward from beneath
an enemy stuns them" ([section 4](#4-mole)). That is a good shape for the
roster's only Stun: it requires arriving from below, which costs a tunnel and
telegraphs itself.

[OPEN] Stun duration, and whether stacking rules differ from Root's.

### Durations are per source

Sticky Piston's Root is **10 ticks** — two component activations at Toolbox's
cadence — and is deliberately short. Other abilities may Root for longer or
shorter, and amplification (Comparator, A2-II Overcharge) extends it, which
makes "spend amplification on Root duration or on Piston distance" a real
circuit decision rather than a default.

## The three clocks

**Settled 27 September 2026.** Three separate cadences run across a class's
thirty levels, and each carries one kind of thing:

| Clock | Cadence | Carries | Never carries |
| --- | --- | --- | --- |
| **Ability** | ×5 — 0, 5, 10, 15, and beyond for scaling | the passive, actives, branches, the ultimate, and scaling of any of them | capacities |
| **Growth** | ×3 | capacities, infrastructure, reach, methodology | **abilities, or the development of an ability's output** |
| **Task** | ×4 | Yield / Efficiency / Slaying | either of the above |

**Growth does not touch abilities.** This is the firewall that was implicit in
the template above and is now explicit, because it was walked through twice
before being written down. It is the same shape as §15's prohibition on Task
mentioning infrastructure, and it has the same test:

> Does the Growth packet change an **ability**, or change something in the world
> or the character that an ability then acts through? The first is forbidden;
> the second is the design.

Improving a summoned unit's combat is developing an ability's output and is
therefore forbidden. Raising a class-resource **capacity** the ability draws
from is a capacity, and is allowed — provided the class has established that the
resource is a capacity rather than part of the kit.

The clocks intersect deliberately and those intersections are where a level
feels large: Lv12 and Lv24 are Growth ∩ Task, Lv15 and Lv30 are Growth ∩
Ability, Lv20 is Ability ∩ Task. The cadences stay predictable even though what
arrives at each breakpoint is class-authored.

A class may use ×5 levels beyond 15 for scaling only, and may skip them: a
schedule of 0/5/10/15/25 is irregular by design, in exactly the way an authored
inventory curve is, and should not be read as an oversight.

Development changes meaning over the curve:

- Levels 3–9: develop the person and establish the class engine;
- Levels 12–18: develop the class methodology and mature personal capability;
- Levels 21–30: develop the player's persistent footprint — capacity, extent,
  connectivity, throughput, simultaneous sites, influence, and sophistication.

This does not prohibit personal late power. It sets the preferred budget:
individual supernormality remains available where the class needs it, while the
largest late scaling should increasingly be team-facing and persistent. A
class's late development should answer, **how much of the world has this class
made ours?**

The template does not require filling quiet levels. A class should initially
specify only its engine, the three Development tracks, the two early ability
branches, the ultimate, and its Task interactions. A quiet level receives a
bespoke reward only when the class has a compelling reason.

### Mole application — Working qualitative curve

Mole's engine is **sense valuable underground space → create access → exploit
access → weaponize terrain and access**. Its four expressions are Sifth Sense,
Tunneling, Drill Rush, and Sinkhole.

| Level | Mole's Development job |
| ---: | --- |
| 3 | Read: improve the reliability, reach, precision, or persistence of Sifth Sense |
| 6 | Exploit: improve carrying, expedition sustain, and conversion of sensed sites into useful expeditions |
| 9 | Extend: support deeper or longer underground work |
| 12 | Connect: turn tunnels, entrances, shortcuts, and known spaces into an underground access methodology |
| 15 | Integrate: make Sinkhole express the established sensing/excavation/access logic |
| 18 | Mature: bring Mole to a developed personal and underground capability without requiring absurd generic stats |
| 21 | Constraint-break: begin an underground capability ordinary Minecraft excavation cannot reproduce |
| 24 | Complexity: manage a more sophisticated underground environment or network |
| 27 | Integration: make Sifth Sense, Tunneling, Drill Rush, Sinkhole, and infrastructure operate as one engine |
| 30 | Complete: fully realize Mole's terrain-changing relationship with the team and map |

Mole's kit can therefore be mechanically complete by Level 15: Sifth Sense,
branched Tunneling, branched Drill Rush, and Sinkhole. Levels 21–30 should
prefer expanding persistent underground access and infrastructure over simply
making Mole personally mine faster.

### Phase-conditioned Task progression — Working direction, 25 September 2026

Task progression is evaluated against three interlocking curves:

- **Player progression:** what the individual can do and how strongly they specialize;
- **World development:** how much of the map the team has discovered, established, connected, improved, and defended;
- **Match objective state:** which objectives have fallen, which approaches are exposed, and whether the match has entered a breach or conversion state.

The level curve therefore assumes broad ecological states rather than rigid clock gates:

| Level range | Expected world state | Progression question |
| --- | --- | --- |
| 1–6, Opening | Mostly natural world; scarce equipment; first expeditions | What can I begin doing? |
| 7–12, Establishment | Known opportunities, first infrastructure, emerging economy | How do I organize and specialize what we found? |
| 13–18, Midgame | Developed footprints, routes, worksites, recurring contests | How do I exploit and defend established systems? |
| 19–24, Late | Valuable world is known; networks overlap contested space; objectives may be breached | How do I connect, contest, repair, or dismantle established state? |
| 25–30, Endgame | World is heavily transformed; remaining assets and Fountain access dominate | How do I convert accumulated advantage into a decisive conclusion? |

These are balance assumptions, not unlock gates. Teams can diverge in level and objective state; an early objective victory can move the world into a later strategic condition before the nominal level band.

The intended semantic shift is:

| Domain | Early | Middle | Late / endgame |
| --- | --- | --- | --- |
| Efficiency | Perform ordinary work faster | Reduce friction in repeated developed activity | Reconfigure, repair, rebuild, traverse, and redeploy mature systems quickly |
| Yield | Extract more from scarce natural finds | Extract more from established opportunities and production chains | Convert controlled or contested high-value opportunities into strategic surplus |
| Damage / Slaying | Survive and defeat ordinary threats | Dominate dangerous sites and recurring contests | Become decisive in the specific fights that determine objective access, defense, or conversion |

Late specialization is intentionally more conditional and more powerful:

> later specialization = greater magnitude × narrower ideal condition

This is an opportunity-cost contract, not a universal power increase. A player who commits most Task choices to Slaying should be frighteningly valuable in a decisive fight, while a Yield or Efficiency specialist should have an equivalently decisive ideal situation in extraction, production, reconfiguration, or sustained operation. Late combat power is therefore not rejected; the requirement is parity of strategic decisiveness across viable specialization paths.

Task investment compounds with class investment. Task specialization answers **how much** the player has invested in a Minecraft verb; class development answers **what situations and methods** make that verb strategically important. A Slaying-heavy Mole may create underground initiations and terrain-access fights, while a Slaying-heavy Kitfighter may be a more conventional combat carry. A Yield-heavy Mole may instead turn rare underground access into exceptional extraction. These are examples of the interaction grammar, not finalized class balance.

Late power has at least three channels:

> late power = personal specialization + accumulated world investment + interaction between them

The interaction term is essential. A late upgrade should generally be tested against the question: **would this be equally valuable on an untouched map at minute zero?** If yes, it may be a valid personal-capability reward, but it is probably not yet expressing the strongest late-game world-state design space.

The seven archetypes should each support a decisive late-game claim, without requiring identical mechanics:

| Archetype | Ideal decisive situation | Claim |
| --- | --- | --- |
| Combat | Major fight, objective defense, or assault | We resolve this fight because our combat specialist can dominate it. |
| Extraction | High-value finite or contested resource window | We convert access into extraordinary material gain. |
| Production | Valuable inputs must become usable decisive output | Our stockpile becomes the equipment and consumables required now. |
| Construction | A critical location must be established, fortified, recovered, or exploited | This place becomes dramatically more valuable because it was developed correctly. |
| Development | Mature productive territory must sustain the next phase | Our established ecology produces the surplus that keeps the plan alive. |
| Exploration | Rapid projection across a transformed map is critical | We reach and exploit the opportunity before the enemy does. |
| Logistics | Multiple developed fronts require resources immediately | Our distributed economy functions as one system under pressure. |

These claims are not automatic archetype inheritance. Archetypes remain descriptive, and each class's authored progression determines which systems and interactions it can actually use.

### Rejected and superseded progression models

**[HISTORICAL / REJECTED] Linear magnitude ladder.** Treating every tier as “more blocks, more drops, more speed” — for example, 100 blocks, then 150, then 200, then 250 — fails because the world being acted upon changes. It produces larger numbers without giving late progression a distinct strategic purpose.

**[HISTORICAL / REJECTED] Late personal power as inherently undesirable.** The concern that late Slaying should avoid decisive damage spikes was overcorrected. A heavily combat-invested player must be able to be a critical factor in a late decisive fight. The actual design requirement is equivalent decisive claims for noncombat specializations, plus opportunity-cost and situation dependence.

**[WORKING, NOT YET CANON] Exact late mechanics.** Names, coefficients, trigger conditions, interaction rules, and the precise level at which each Task domain changes semantic mode remain open. This section establishes the design test and progression grammar, not final enchantment numbers or a complete ability list.

## Working XP requirement bands

Exact XP values remain unresolved, but the current working direction is that Levels 1–30 do not follow one uninterrupted smooth XP curve.

Instead, levels are grouped into broad **XP requirement bands** corresponding to changes in the productive economy available to players.

A new band represents a new category of XP requirement. Requirements may remain approximately flat or rise only modestly within a band, while major economic breakpoints can produce a more substantial increase between bands.

Current working structure:

| XP band | Levels | Working requirement index | Economic state |
| --- | ---: | ---: | --- |
| I — Bootstrap | 0–6 | ~1.000× | constrained player; base Minecraft XP economy |
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

- Levels 0–6 use the Bootstrap requirement band. Lv0 is the enrolled starting state, holding the Passive alone; Active 1 is earned at Lv1.
- The first Task allocation at Level 4 begins accelerating productive activity within that band.
- Growth II at Level 6 arrives immediately before the first major XP-requirement transition. For classes whose curve grants infrastructure early, that is also where a new economic tool appears; for classes like Mole it is personal endurance instead.
- The Level 6 → 7 requirement is the current candidate entry into the Established band.
- Task II at Level 8 further accelerates productive activity after that transition.
- Growth III at Level 9 provides another within-band progression increase.
- Level 12 collides Growth IV with Task III, maturing the player's economy before the next requirement band.

[VERIFIED 26 September 2026] The band indices and boundaries above are the current repository implementation, confirmed against the progression handoff's independently recalled values (1.000 / 1.350 / 1.875 / 2.575 / 3.250 across 6→7, 12→13, 19→20, 24→25). The handoff's note that this "still needs repo confirmation" is answered: these are it. The obsolete linear 80–900 XP table is not to be restored.
- The Level 12 → 13 requirement is the current candidate entry into the Developed band.
- Level 16 Task choice further increases specialized capability within that band.
- Levels 16–17 mark the approximate completion of the universal physical-capacity transition: inventory reaches its 36-slot universal floor at Level 16, while Health and Hunger reach their vanilla universal floors at Level 17.
- Level 20 Task choice is the first explicitly advanced specialization decision.
- The Level 24 Task choice coincides with the preferred late infrastructure and
  footprint expansion point.
- Level 28 Task choice is the final shared specialization decision before the
  Level 30 class completion.
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

**Growth curve authored 26 September 2026.** Mole is the roster's introductory
calibration class and now has a full Lv0–30 curve — Health 100→320 on the ×10
scale (+22 every Growth), Hunger 10→20 maturing by Lv15, Inventory 6→24,
Exhaustion Efficiency at Lv6/12/21/27, and Route access beginning at Lv18. See
[docs/design/CLASS_PROGRESSION_GROWTH_AND_TASK.md](docs/design/CLASS_PROGRESSION_GROWTH_AND_TASK.md#9-mole--the-first-authored-curve).
[RESOLVED 26 September 2026] The handoff's "Excavation / Combat" is loose usage;
the archetype is **Extraction**, as recorded below.

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

**Revised 27 September 2026.** Mole targets a jagged area of ground, which opens
after a delay and closes again later.

- **Targets any blocks, including player construction.** The previous
  natural-terrain-only rule is superseded.
- All affected blocks are **highlighted to every nearby player** before anything
  breaks.
- After a delay the blocks are **destroyed**; after a further period they are
  **restored exactly as they were**.
- The volume where blocks were destroyed is **No-Build** for the duration.
- **Zero drops.**

### Why it restores rather than scarring

Temporary removal changes the ultimate from map editing into **zone control**,
which is what its combat classification already claims it is
([COMBAT_STATE.md](docs/design/COMBAT_STATE.md)): it is crowd control whose
medium happens to be terrain.

Against construction it becomes a **timed breach** — "your wall is open for
fifteen seconds" — which is a siege tool rather than a demolition, and far
easier to balance than an irreversible hole in a 25–35 minute match.

Mole does not lose its permanent mark on the world: **Tunneling** is the class's
permanent world change and it happens constantly. Tunnels are permanent and
constructive; the sinkhole is temporary and controlling.

[HISTORICAL] "Leaves a permanent jagged sinkhole" and "player-authored
construction survives" are superseded.

### Three rules that hold it together

**No-Build is the restore invariant, not a separate effect.** Because nothing
can be built inside the opened volume, the restore is guaranteed to destroy
nothing the players made. They are one mechanic: *the volume belongs to the
ultimate for its duration.*

**Zero drops, non-negotiable.** With restoration, any drops at all make Sinkhole
an infinite duplicator — and since it can now target construction, a duplicator
of whatever the enemy built. This is why the earlier "partial drops" is
superseded rather than reduced.

**Falling blocks must not duplicate.** Removing support under sand or gravel
drops it; restoring the original would leave both. Suppress falling inside the
volume or clear the fallen entities on restore.

### Timing comes from block hardness

The delay before blocks break is **how long it would take to mine the hardest
highlighted block**. Soft ground opens quickly, stone slowly, obsidian very
slowly.

This uses vanilla's own mining-speed maths, so Mole's tool tier and Efficiency
Task level improve the ultimate without any bespoke scaling rule — an incidental
consequence of Sinkhole being *implemented as mining*, not a general principle
that Efficiency scales abilities. See
[the progression record](docs/design/CLASS_PROGRESSION_GROWTH_AND_TASK.md#what-each-tree-means-across-the-roster).

It also answers fortification natively: hard materials resist Sinkhole by taking
longer to open. **Structural Integrity and Waxer's Sealed blocks feed the same
hardness calculation** rather than resisting outright or being excluded — a
reinforced or sealed wall takes correspondingly longer to open, and a
sufficiently fortified one becomes impractical to target at all.

That keeps every defensive system on one axis. Construction's intrinsic benefit
is not bypassed by an ultimate, and Waxer's preservation means what it says,
without either needing a special case written against Sinkhole.

### Gaps: the budget is blocks, not volume

A targeted area may contain gaps, and Sinkhole still breaks the solid blocks
below and around them. A one-block-thin floor covering a large cavern can
therefore be opened remotely — it costs little of the block budget, takes the
same time, and only the destroyed floor receives No-Build.

This makes the cost **blocks affected rather than volume covered**, which turns
terrain knowledge into power: knowing where a thin crust hides a void is exactly
what **Sifth Sense** reports. The ultimate rewards the passive's information.

**Being restored into suffocates.** An entity occupying a position when its
block returns takes suffocation damage; the blocks win. This is the ultimate's
teeth — the threat is not the opening but the closing, and anyone still standing
in the hole when time runs out is in a wall. It also means the No-Build volume
is a place enemies must *leave*, not merely a place they cannot build.

[OPEN] Maximum block budget, the delay and open durations, targeting geometry,
how the highlight is drawn, chunk-unload and restart persistence of the
snapshot, and liquid behaviour at the boundary when a hole is opened under water
or lava.

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

[RESOLVED 26 September 2026] The 26 September progression handoff recalled these
branches as "Maturity / Proliferation / Diversity". The names below stand;
that recall is inaccurate rather than a rename.

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

[RESOLVED 26 September 2026] Crossbows are excluded as *normal* equipment, and
the **Crossbow** branch below stands regardless. Kitfighter exists to defy the
equipment possibilities available to everyone else, so the exclusion is what
gives the branch its value. Equipment exclusions are baseline rules that class
kits are licensed to break — the same principle already stated for the offhand.

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

### Toolbox — grammar settled, numbers open
Primary:
- Construction

Secondary:
- Combat

Construction is earned by remote block placement and Placement Reach as a growth
axis, not by the Ultimate alone. See [section 19](#19-toolbox).

Confirmed by the 13 September kit: Combat · Production.

Kitfighter should not currently be listed as Extraction.

### Merchant — active design, not settled
Emergent archetype:
- Production

Do not assign further archetypes until finished behaviour is evaluated. The 13 September kit [K9] confirms "Production (others pending final classification)" and lists final archetype classification among its open items. See [section 13](#13-merchant--active-design).

### Waxer — kit supplied 13 September 2026
Production and Combat, working, per [K9]. The kit supplies the previously undeveloped Ability 2 branch tree and the ultimate's delivery. See [section 18](#18-waxer). [OPEN] Whether this equals a completed conceptual pass is the owner's call.

### Settled beyond the original four
Skeleton Crew ([section 15](#15-skeleton-crew)) and Lightfooted ([section 16](#16-lightfooted)) completed conceptual passes on 12 September 2026. Skeleton Crew is Logistics and Combat; Lightfooted is Exploration primary with Combat secondary.

### Daredevil — kit supplied 13 September 2026
Exploration and Combat, per [K7]. The kit supplies the previously missing Ability 2 and ultimate. See [section 17](#17-daredevil). [OPEN] Whether this equals a completed conceptual pass is the owner's call.

### Unsettled drafts
None. Waxer and Daredevil received full kits on 13 September 2026; Skeleton Crew and Lightfooted were settled on 12 September. See the [draft record](#14-class-draft-record) for how the roster reached that state. Promotion out of draft status is not a claim that every quantity is decided.

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

Emergent archetype: **Production**. Archetypes are descriptive, not generative — do not assign Logistics, Exploration, or others until the finished behaviour is evaluated. The 13 September kit repeats this as "Production (others pending final classification)".

---

### Canonical kit — 13 September 2026

**Source: [K9] Merchant and Waxer kit handoff.** Reproduced as supplied. Where the developed material below elaborates this kit they agree; where it differs, this block is the current statement.

Archetypes: Production (others pending final classification)
Hook: Merchant gets rich enough to stop doing ordinary Minecraft work, turning successful trade relationships into an increasingly capable paid workforce and eventually an extravagant traveling spectacle.

**Passive — Work.** Villagers with sufficient Trading Reputation toward Merchant become willing to Work for Emeralds. Merchant can employ willing villagers on Payroll, allowing them to process available materials into valid products.

Working advances a villager's Mastery from Novice → Apprentice → Journeyman → Expert → Master. Higher Mastery expands the sophistication of transformations they can perform and improves their material efficiency.

**A1 — Swindle.** After trading with a villager, strike them to sacrifice Reputation and extract bonus Emerald value from the completed trade. Each villager can be Swindled once per restock cycle.

- **Invisible Hand** — Swindling one villager also Swindles nearby independently eligible villagers while concentrating the Reputation loss on the struck villager.
- **Credit Line** — Convert future commercial/relationship value into immediate liquidity. [OPEN: exact mechanic]
- **Community Chest** — Redirect Swindle toward distributed/circulating economic value. [OPEN: exact mechanic]

**A2 — Retinue.** Call nearby employed villagers away from ordinary Work to accompany Merchant.

- **Seize the Means** — Retinue seek out and temporarily disable workstations/infrastructure in a designated area rather than destroying them.
- **Toll Patrol** — Retinue patrol a contested area; enemy activity there offsets Payroll costs while the workers sacrifice ordinary Work.
- **Overtime** — Increase expenditure to accelerate Work/Mastery. [OPEN: mechanic/replacement]

**Ultimate — Procession / Silk Road.** Begin an extravagant procession with nearby Retinue. Merchant must continue moving with the procession and paying its costs or the ultimate ends.

The procession repeatedly grants effects to allies around it. Retinue size determines the frequency of these procs; workforce Mastery determines their cumulative quality, progressing through effects such as Saturation, Speed, Regeneration, Absorption, and offensive damage.

Open: Credit Line · Community Chest · Overtime · exact Emerald-substitution placement · final ultimate name/effect package · final archetype classification.

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

# 14. Class Draft Record

**No class remains an unsettled draft as of 13 September 2026.** This section is retained for traceability of how the roster reached that state rather than deleted.

The drafts were Skeleton Crew, Lightfooted, Waxer and Daredevil. Skeleton Crew ([section 15](#15-skeleton-crew)) and Lightfooted ([section 16](#16-lightfooted)) were promoted to settled classes on 12 September 2026. Daredevil ([section 17](#17-daredevil)) and Waxer ([section 18](#18-waxer)) received full kits on 13 September 2026, supplying the slots that were previously missing.

The original rule still applies to anything that remains unspecified within those kits: **missing slots are missing design rather than gaps to be filled by invention.** Promotion out of draft status is not a claim that every quantity is decided; each class section carries its own [OPEN] items.

[OPEN] Whether Daredevil and Waxer have had conceptual passes equivalent to Skeleton Crew's and Lightfooted's — which were declared settled with explicit statements about what was decided — is the owner's call and is not asserted by the presence of a kit.

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

**Kit supplied 13 September 2026. Source: [K7].** Daredevil was previously a substantially incomplete draft with no Ability 2 and no ultimate ([section 14](#14-class-draft-record)). The kit below supplies both and is reproduced as given. [OPEN] Whether this constitutes a completed conceptual pass equivalent to Skeleton Crew's and Lightfooted's is the owner's call; it is not asserted here.

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

# 18. Waxer

**Kit supplied 13 September 2026. Source: [K9].** Waxer was previously a draft whose Ability 2 branch seeds were "not equally developed" and whose ultimate had no delivery mechanism ([section 14](#14-class-draft-record)). The kit below supplies both and is reproduced as given. [OPEN] Whether this constitutes a completed conceptual pass equivalent to Skeleton Crew's and Lightfooted's is the owner's call; it is not asserted here.

Archetypes: Production · Combat (working)
Hook: Wax isn't merely protective: Waxer can preserve useful things against change until preservation itself becomes an obstruction.

**Passive — Waxed Recipes.** When crafting, fill otherwise-empty crafting slots with Honeycomb to produce a Waxed version of the product.

Waxed products become the persistent substrate for Waxer's preservation mechanics.

**A1 — Wax-On.** Apply Wax to a target. Repeated Waxing increasingly preserves its current state.

Interactive blocks that accumulate enough Wax become Sealed, preventing their interactive state from changing. Applying the first Wax to damaged, previously unwaxed equipment also restores some durability.

- **Amber** — Wax targets more aggressively, allowing them to reach a Sealed state more readily.
- **Restorative** — Greatly increase the durability restored by the first application of Wax.
- **Sticky** — Attacking Waxed enemy armor can consume Wax from it to inflict Mining Fatigue.

**A2 — Wax-Off.** Splash an area with Honey Solution. Using Honey Solution around eligible working/production activity generates Honeycomb, giving Waxer a repeatable way to replenish the resource consumed by Waxed Recipes and Wax-On.

- **Enzymatic** — While enemy players remain within the Honey Solution, their equipment suffers additional durability wear.
- **Preserving** — While allied players remain within the Honey Solution, their equipment suffers reduced durability wear.
- **Floral** — Honeycomb generation expands beyond workstations: splashing flowers and beehives can also produce Honeycomb.

This gives the tree a clean three-way interpretation of the solution:

Enzymatic: break things down.
Preserving: prevent things from breaking down.
Floral: produce more wax.

**Ultimate — Amber.** Completely encase a target in Amber, preserving it in stasis and preventing its state from changing.

Open: Exact Amber delivery/targeting and the precise rules governing what an Ambered target can or cannot do.

---

## Notes carried forward from the draft

[HISTORICAL] The draft recorded the A2 branch seeds — **Enzymatic**, **Preserving**, **Floral** — as "not equally developed and remain draft". They are now defined above. The draft's A1 branch concepts described Amber as "faster or stronger sealing", Restorative as "stronger initial restoration", and Sticky as punishing attackers "for example Mining Fatigue while consuming Wax"; the kit's wording supersedes those sketches. The draft's core rule — Honeycomb repeatedly invested into existing products to preserve them against change, until enough preservation becomes obstruction — survives as the hook above.

The **Amber** name is used twice, for the A1 branch and for the ultimate. This is reproduced as supplied rather than renamed.

[OPEN] The earlier Seal proposal, eligibility, application, protection consumption, visible counterplay, and ordinary removal remain undeveloped. Do not create a global enemy-block-immunity rule merely to make Waxer work.

**Feasibility assessed 13 September 2026.** See [the capability audit](docs/feasibility/2026-09-12-capability-audit.md). Most of the kit is reachable: durability restore, per-item Wax counters, the Honey Solution area, Floral, and Amber against mobs all work, and Enzymatic and Preserving are reachable through the same detect-and-correct polling this project has already accepted for Structural Integrity. Three [TECHNICAL RISK] items do not work as written:

- **Recipes cannot be class-gated.** Waxed Recipes would be craftable by every player. Workarounds exist — inert components for non-Waxers, or detect-and-revert — and both are visible seams. "Fill otherwise-empty slots" is also one authored full-grid recipe per product, not a general rule.
- **Sealed containers are not reachable.** Block states such as doors and levers can be forced back by polling; a chest cannot be prevented from opening, because a datapack cannot cancel an interaction or close a screen.
- **Amber cannot hold a player in stasis.** It is exact against mobs. Against players, every available effect still leaves them able to look, attack, use items and place blocks. The audit's escape hatch is encasing the target in **actual Honey Blocks**, which is vanilla-native, legible to both teams, natively prevents jumping and slows movement, and gives enemies physical counterplay by digging the target out.

Sealed blocks also make Waxer the sixth system depending on **persistent per-position block data**, alongside anti-farming XP, Sinkhole, Construct designation, Structural Integrity and Development Zones. That dependency has no owner; see §0 of the audit.

---


---

# September 19, 2026 — Equipment and Infrastructure Reconciliation

## Copper equipment as the Stone-to-Iron buffer

**Working canon, 19 September 2026.** Copper is a **Primary Material**. This supersedes the 10 September restricted-material passage above that explicitly excluded Copper from the category.

Copper equipment occupies the durability space between disposable Stone equipment and stable Iron equipment. Its purpose is not merely to add another recipe tier: Copper gives the early equipment economy a legitimate, replaceable metal tier so that Iron can retain a materially more stable and reliable position.

Current qualitative equipment ladder:

> **Stone — disposable → Copper — replaceable / field-standard metal → Iron — stable / reliable → Diamond — high-value capital → Netherite — premier late-game capital**

This direction permits early durability to be compressed around Stone and Copper rather than forcing Iron to function as the first merely serviceable tier. Exact durability values remain **[OPEN]**. Earlier numerical durability fixtures are sensitivity tests, not canon, and must not be promoted by this reconciliation.

Copper's Primary Material status also means qualifying Copper equipment participates in Primary-Material systems such as Kitfighter Salvage according to their recipe rules. The universal full-team combat-tier recipe ledger remains a **gross recipe threshold**; Production/Salvage can reduce net resource expenditure without changing the gross recipe cost.

## Level 6 contribution fork

**Working canon, 19 September 2026.** The Level 6 infrastructure model is no longer best described as every eligible player receiving a fungible generic infrastructure slot.

At Level 6, a player's authored progression may offer a fork between:

- contributing **one class-compatible form of persistent Infrastructure** to the team; or
- **Monster Combat** specialization.

A player who chooses Infrastructure contributes one authored infrastructure form. Later individual progression may increase the **quantity and/or quality** of that player's contribution. Multiple teammates may choose the same infrastructure form; duplicate choices represent concentrated team depth rather than wasted unlocks.

Infrastructure eligibility is **partially class-authored and may be asymmetric**. A class may have several coherent Infrastructure options, one option, or none. There is no requirement that every class receive the same number of choices. Current discussion examples are deliberately incomplete: Mole may plausibly choose **Route or Construct**, while Kitfighter may be **Monster Combat only**. These examples do not establish the full Class × Infrastructure matrix; unresolved cells remain **[OPEN]**.

Archetype tags do not automatically grant Infrastructure eligibility. Ordinary Minecraft building, travel, transport, farming, storage, and combat remain available regardless of the Level 6 choice.

### Monster Combat is not Infrastructure

The earlier open search for a Combat-side Infrastructure analogue should not be answered by inventing a "Mob Slayer" Infrastructure type. The current direction places Smite, Bane of Arthropods, and related monster-facing specialization vocabulary on the **Monster Combat** side of the Level 6 fork. Exact Monster Combat branches and their relationship to Mob Swarms remain **[OPEN]**.

### Worksite-earned team opportunities

Personal Infrastructure contribution and Worksite-earned Infrastructure are distinct.

- **Personal contribution** is bound to the player's authored/class-compatible Level 6 choice and later personal upgrades.
- **Shared Worksite opportunity** belongs to the team collectively.

The first team to capitalize an activated Worksite earns **one bonus shared team Infrastructure opportunity**. It is not personal to the player who completed the capitalization. Current direction is that this shared opportunity can let the team broaden or reinforce its infrastructure portfolio beyond individual class restrictions; exact allocation/reallocation rules remain **[OPEN]**.

A Worksite reward is therefore persistent organizational capacity, not a flat XP payout.

---

# 19. Toolbox

**Active design, 27 September 2026.** Toolbox treats their inventory as a
programmable Redstone machine. Inventory arrangement is circuit topology;
components are both instructions and consumable parts; the circuit resolves over
real time while Toolbox pilots its targeting and direction live.

The detailed circuit grammar — flow topology, timing, Tripwire, Observer relay,
the component dictionary and the worked examples — lives in
[docs/design/TOOLBOX_CIRCUIT_GRAMMAR.md](docs/design/TOOLBOX_CIRCUIT_GRAMMAR.md).
This section records the class itself.

## Status

**Conceptually settled** in its grammar and ability skeleton. Nearly every
number is [OPEN]. The component dictionary has been deliberately pruned rather
than completed.

## Identity

The roster's engineer, and knowingly a bit of a nerd about it — goofy ability
names, extremely squishy, and **thrives on being hit once or twice** and
answering with engineered prejudice. The passive only procs on damage, so the
class genuinely wants the first exchange. Its counterplay is therefore **burst,
not poke**.

## Hook

> Toolbox organizes their inventory as a programmable Redstone circuit. Taking
> damage powers it reactively; Toolbox can reconfigure or deliberately power it
> themselves; components physically fall out as they execute; and their Ultimate
> lets actual constructed Redstone machines invoke the same vocabulary.

The test in [section 1](#1-class-design-philosophy) — that the weird thing about
playing Minecraft as this class is explicable without mentioning ability buttons
— is answered by the passive alone: *my inventory is a Redstone circuit*.

## Archetype

Primary:
- Construction

Secondary:
- Combat

**Why Construction is earned rather than themed.** Not because Pistons are
construction items, and not because an ability happens to place a block — §2's
firewall forbids both. Two things earn it:

- **Remote block placement.** Dropper ejects an item that becomes its block when
  it settles validly, so Toolbox can establish terrain at range and along a
  trajectory rather than at arm's length.
- **Placement Reach as a growth axis** (§30 of the grammar record), which
  changes *which geometries are buildable at all*.

That is a verb-level change of the kind [section 31 of the grammar
record](docs/design/TOOLBOX_CIRCUIT_GRAMMAR.md) requires of specialization, and
it is the same shape of argument by which Merchant is Production. The Ultimate
then extends the claim — actual built machines become ability execution — but
the archetype no longer rests on it alone.

## Canonical kit — 27 September 2026

### Passive — Utility Belt

Incoming damage, when the cooldown is ready, executes the configured inventory
circuit.

- Inventory geometry is the program; Redstone Dust links components.
- Flow reads like text — left to right, top row down — beginning at the
  **top-left**, so the order is row A, row B, row C, then the hotbar. Locked
  cells do **not** compress the board.
- **The hotbar is the magazine.** Flow stops at the first non-circuit item, and
  that item is what Dispenser and Dropper take.
- Approximately one primary component every ~5 ticks [PROTOTYPE]. **Dust is
  required between every pair of components**, costs an item, and costs no
  time — duration is component count, not cell count.
- **Tripwire needs a parity shim.** Parity flips at every row wrap, so hooks
  miss unless two consecutive wiring cells realign them. Repeater is wiring, so
  it can be that shim and buy a deliberate delay at the same time.
- Components inspect **current world state when they resolve**, not at trigger.
  Directional components read Toolbox's facing at their own resolution tick.
- Every resolved component or Dust **drops one item**. This is the "variety as
  durability" rule: a poorly supplied Toolbox sheds the machine while being
  attacked.
- Three amplification states only — Base, +1, +2. Granular 1–15 signal is
  [REJECTED].

**Cooldown [WORKING]:** 20 / 18 / 16 / 14 / 10 seconds across the level range.
Long deliberately: at ~5 ticks per component a long circuit takes seconds, and a
short cooldown would let executions overlap and multiply.

**Any damage procs it**, including mobs and environmental damage. This
deliberately diverges from
[COMBAT_STATE.md](docs/design/COMBAT_STATE.md)'s predicate, which excludes
environmental damage; the Utility Belt is a machine reacting to being hit, not a
combat state.

[OPEN] That divergence has a consequence: a passive proc runs the *whole*
circuit while A2 runs a limited opening, so self-inflicted chip damage is
strictly better than A2 for firing one's own machine. Candidate answers —
gate the proc on damage magnitude, run a reduced portion on self-inflicted or
environmental procs, or make A2 about *when* rather than *how much*.

### A1 — Reconfiguratron! (6s)

Swap the first two **components** in the Utility Belt circuit for the last two
components in the inventory.

- **I — Speedy Swap!!** Cooldown halved.
- **II — More Config!!** Swaps three components instead of two.
- **III — Spare Parts??** Components that are unchanged after the swap heal
  Toolbox for x% HP instead.

Constrained reconfiguration, deliberately not free inventory editing: inventory
preparation remains the class's first mastery layer.

**A1 is the A2 loadout editor.** It swaps the first two components; A2 fires the
first two components. So A1 does not reconfigure the circuit in general — it
rewrites the part A2 uses. That is the answer to "why reconfigure mid-fight",
and it is a design consequence rather than something the ability text should
say.

### A2 — Jumpstartinator! (3 charges, 8s recharge)

Manually activate the first two components of the **same** Utility Belt,
targeting Toolbox where applicable. There is no separate A2 circuit; that is
the point.

- **I — Overcharging!!** The second component activated is additionally
  empowered. With a Comparator this is the **only** route to +2 (see the
  grammar record § Comparator).
- **II — Super Circuit!!** Activates the first three components.
- **III — Short Circuit??** If fewer than two components activate, Toolbox
  deals x damage in an area around them.

Branches are mutually exclusive, as everywhere on the roster.

**Charges rather than a flat cooldown**, and the difference is large: a flat 2s
would be 30 uses a minute, while three charges on an 8s recharge is a **3-use
burst and 7.5 a minute sustained**. A2 becomes something spent and then absent,
rather than a rhythm to lean on — and it alternates naturally with the passive
at 6–16s instead of drowning it.

**The first two components therefore run far more often than anything else.**
A2 fires them every few seconds; the passive fires the whole circuit every
6–16. Circuit design is front-weighted: the opening is not "what happens
first", it is "what happens constantly". A Piston in slot one is a dash on a
charge timer, paid for one Piston at a time.

**Short Circuit is a state, not a build.** It is reached by deliberately
breaking the circuit down to one component or none — literally short-circuiting
it — which is done by **manual inventory clicking** mid-fight, a deliberate and
risky action in keeping with the class. A1 cannot do it, since A1 only moves
components. It is periodic, the same way A1 value is periodic; it does not
require permanently running a short circuit.

### Ultimate — Gizmo of Absurdity and Untold Destruction!!! (67s)

The next Redstone circuit Toolbox powers treats any activated **pistons
(including sticky), hoppers, droppers, dispensers and observers** as if they
were also activated by the Utility Belt, for **6 seconds and/or up to 32
activations**.

See the grammar record § *The Ultimate* for what "as if" resolves to, which
component list falls out of one principle, and why the two caps are paired.

## Growth profile — 27 September 2026

Split across the three clocks (§ *The three clocks*). Every number is [WORKING].

### Ability clock (×5) — scaling only past Lv15

| Lv | |
| ---: | --- |
| 0 | Utility Belt, cooldown **16s** |
| 1 | A1 Reconfiguration |
| 2 | A2 Manual Activation |
| 5 | A1 branch · cooldown **14s** |
| 10 | A2 branch · cooldown **12s** |
| 15 | Ultimate |
| 20 | cooldown **10s** |
| 25 | cooldown **6s** |

**Lv15 carries the Ultimate and no cooldown step**, and the fourth step sits at
20 instead. That follows §*Budgets need not be equal at every event*: a level
already granting an Ultimate can be modest, so stacking a cooldown improvement
on it would make Lv15 enormous and Lv20 empty.

At Lv25 a full parity-aligned board is 13 components — 3.25s against a 6s
cooldown, so the machine executes **54% of the time** in sustained combat. The
faster curve does not buy power; at ~30 items per activation it buys **burn
rate**, and the resource wall simply arrives sooner. See the grammar record
§10A.

20 and 30 carry nothing. Irregular by design.

**Toolbox claims the exception** the template allows. Its passive carries an
unusually large share of the class identity — the inventory *is* the machine —
so the Utility Belt's cooldown continues scaling past Lv15 where another class's
kit would be finished. It is scaling, not addition: nothing new starts happening
at Lv25.

### Growth clock (×3) — extreme Inventory, minimal Health

```
health: { start: 10, growthUnit: 1.4, cap: 24 }      # 1,000 -> 2,400
hunger: { start: 10, growthUnit: 2,   cap: 20 }      # standard, plateaus ~Lv15
slots:  { steps: [6, 12, 18, 21, 24, 27, 36, 36, 36, 36, 36], cap: 36 }
```

| Lv | Slots | Δ | Also | |
| ---: | ---: | ---: | --- | --- |
| 3 | 12 | +6 | | the board becomes usable at all |
| 6 | 18 | +6 | Constructs I | two 9-wide runs; wrap works |
| 9 | 21 | +3 | Placement Reach I | Tripwire online, 3 columns |
| 12 | 24 | +3 | Constructs II | passes the normal end-state |
| 15 | 27 | +3 | Buildable Scale I | full-width Tripwire |
| 18 | **36** | **+9** | | the spike — fourth row lands whole, 27 positions |
| 21 | — | | Constructs III, Placement Reach II | |
| 24 | — | | Constructs IV | |
| 27 | — | | Placement Reach III | |
| 30 | — | | *capstone* — **[DEFERRED]** | |

**The infrastructure spend is mostly Constructs, and otherwise Buildable
Scale.** Toolbox builds machines and wants them bigger; it does not want a
logistics network, so nothing goes to Routes, Lines or Zones. Within the
Construct type that means the **Count** axis first and the **Spatial** axis
second — how many, then how large — and never Operational Scale, which governs
what may *connect* to a Construct rather than what one may be.

Buildable Scale lands at Lv15 because that level otherwise carries only its
three slots, and Lv18 immediately after is the largest Growth event on the
curve. Per §*Budgets need not be equal at every event*, the modest level takes
the addition.

Placement Reach is **PERSONAL, not infrastructure**. It changes which positions
and geometries are possible — which is exactly what the lava rule made
Toolbox's block interactions depend on — rather than what may be recognized.

[DEFERRED] The Lv30 capstone. Its slot is left empty rather than filled, so
nothing reads as placeholder content later.

**Health is the lowest on the roster.** 2,400 sits at the top edge of the
three-netherite-hit and two-crit bands; 2,500 flips both. So it is the most
health obtainable while still dying in three, which is the intent — Toolbox is
limited in hits taken independently of the Utility Belt's cooldown.

**Inventory is the opposite extreme, and it has to be.** At 6 slots Toolbox has
no class: six is the *whole* inventory including hotbar, so after a weapon, food
and a tool there is no board. Every other class loses carrying capacity at low
level; Toolbox loses its abilities. Reaching 36 by Lv18 makes Toolbox the
roster's clearest capacity specialist, and under the useful-capacity model the
24→36 band is not situational hauling for this class — **27 is the requirement
for full-width Tripwire**, so the specialist band is core mechanical function.

[KNOWN COST] Toolbox is weakest-relative-to-roster at Lv0–6 in a way no other
class is, and the front-loaded curve is a partial answer rather than a fix.

### Task clock (×4)

Seven choices at Lv4, 8, 12, 16, 20, 24, 28; **cap IV per tree**. Reaching IV
costs four, leaving three, so **only one tree can ever reach IV** — spreads are
4/3/0, 4/2/1, 3/3/1 or 3/2/2. One sized upgrade, otherwise wide, enforced by the
cap and budget rather than by a rule.

Expect Yield and Efficiency to dominate: both feed the component loop directly,
while Slaying improves personal combat Toolbox mostly does not do. Slaying is
the deliberate choice of the Toolbox who wants to be the one finishing with the
sword.

## Scaling is emergent, not authored

**Settled 27 September 2026.** Toolbox is one of the only classes so far whose
kit **does not help it obtain its own gameplan.** Mole's tunnelling makes Mole's
mining easier; Skeleton Crew's crew gather for Skeleton Crew. Nothing in the
Utility Belt acquires Redstone components — the kit is a *language*, and the
class has to be supplied before it can speak.

Every input to Toolbox's power is therefore external to its abilities:

| Input | Comes from |
| --- | --- |
| Component supply | **team economy** |
| Board size | **Inventory capacity**, not the kit |
| Circuit quality | the player |
| Buildable space, hard ground | the map |

**Its early game is carried by a Capacity stat rather than by its kit.** The
front-loaded Inventory curve lets Toolbox mine deeper and build larger than
anyone before it owns a single Redstone component — that is what makes the
class playable at all before its abilities mean anything.

**Then it is held back only by component count, creativity and buildable
space** — and once the team's economy supplies those, it becomes a late-game
monster. Nothing in the kit grants that; the curve is entirely a function of
what it is given.

This is the roster's clearest example of the principle in
[section 9](#9-class--universal-progression): **specialization should change a
verb, not add a percentage.** Toolbox never reads "gains 20% more X at Lv25."
Its scaling is what its inventory and its team make possible.

**Two consequences worth stating as strategy rather than flavour.** Toolbox has
an unusually **high floor to clear** — a Toolbox on a resource-poor team is
nothing, so fielding one is a team investment and a draft-level decision, not
only a player-skill one. And it explains why the class's dependencies bite so
hard elsewhere: quartz availability (grammar components), hard ground (traps),
and useful capacity (the board) are all the same fact seen three times.

## Damage is systemic, not authored

Outside Short Circuit, Toolbox has almost no authored damage. It kills by
arranging ordinary Minecraft objects — TNT, arrows, lava, knockback, terrain,
confinement — so that **Minecraft** supplies the lethal consequence.

This is why the friendly-fire rule matters to Toolbox specifically: authored
damage does not cross teams, but **TNT, suffocation and other incidental damage
do**. The line is authored versus systemic, which is the same distinction this
class is built on.

## Open

- Inventory unlock order means the first vertically adjacent unlocked pair
  appears at **21** slots and the first full 9-wide one at **27** — not 18.
  Tripwire therefore arrives partway up the curve. **This is intentional**: the
  component becomes useful when the board can hold it.
- Toolbox's Inventory curve. Under the useful-capacity model most classes
  mature around **24** slots, and 24→36 is specialization — but Toolbox needs
  **27** for full-width Tripwire, so for this class the top band is not
  situational hauling capacity, it is core mechanical function. That is the
  justification for spending most of its Growth budget there, and it makes
  Toolbox the roster's clearest capacity specialist. `Capacity` supports
  per-class `steps` today, so the schedule is authorable without code.
- Dust's consumption rule: wiring drops one, instruction discharges the stack.
  Exact mitigation formula [OPEN].
- Nearly every magnitude: cadence, cooldown curve, Observed and Illuminated
  durations, amplification thresholds, displacement values, A2 budgets, Short
  Circuit radius, Ultimate duration.
- The boundary between circuit components and payload inventory, which both
  Dispenser and Dropper read as "the first item".
