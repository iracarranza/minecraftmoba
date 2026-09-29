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

## Input form — the third axis

**Settled 28 September 2026.** An ability is described by three independent
things:

| Axis | Question | Owner |
| --- | --- | --- |
| **Target form** | what is aimed at | the ability |
| **Input form** | how the gesture is made | the ability |
| **Cast mode** | whether the aim is verified first | the **player** |

Only the third is a preference.

### Why input form is not a preference

Because an ability may use it to **select which output you get**.

Pantheon's Q is the reference case: a **tap** is a short spear stab, a **hold**
is a long spear throw. Two outputs of one ability, chosen by gesture. A
cast-mode preference that reshaped the gesture would take an output away from
the player.

| Form | | Modes it permits |
| --- | --- | --- |
| **Instant** | the press is the whole input — *the stab* | all three |
| **Channeled** | must be held to occur at all — *the throw* | Hold only |
| **Charged** | held, and **how long** changes the output — *the widening beam* | Hold only |

### Why the throw cannot be double cast

The obvious half is Quick: Quick is a bare press, and a bare press produces the
*stab*. There is no press that produces a throw.

The less obvious half is Double, and it is a **gesture** collision rather than
an intent one:

> Double needs a first press that does **not** commit, so a second can confirm.
> A held form has no such press — the hold *is* the activation — so a
> double-cast throw would have to be press, release, press, hold. Nobody makes
> that gesture.

Hold is the only mode whose shape the throw already is.

### Why the stab *can* be double cast, in the same ability

Because a **held** first press is still distinguishable from a **tapped** one.
Under Double: tap shows the indicator, a second tap stabs — and press-and-hold
still throws. The tap selector is suspended for verification; the hold selector
is not disturbed at all.

That is the property that lets one ability carry two outputs *and* honour a
preference on one of them.

### Charged is Channeled plus measurement

Everything Channeled cannot do, Charged cannot do, for the same reasons. The
only difference is that duration is **measured** rather than merely required —
which is what makes a widening beam different from a throw that merely needs
holding.

### The unit of description is an OUTPUT, not an ability

Tested against the real roster, the axes held and the *unit* did not. Pantheon's
Q has two input forms in one ability, so a single (target, input) pair cannot
describe it.

> **An ability is a set of one or more outputs. Each output has a target form
> and an input form. Where there is more than one, the input form is also the
> selector that chooses between them.**

Most abilities have exactly one output and the distinction never shows. It shows
the moment an ability wants a tap and a hold to do different things — which is
the case that forced it.

### What the roster exposed

Two values were missing, and both were found by listing what exists rather than
reasoning about what could:

- **Self** — Reconfiguratron swaps components in your own inventory; an aimless
  channel; Bounding. Three shipped abilities targeted nothing, and there was no
  label for it. An ability with no target has nothing to preview, which is what
  makes it ignore cast modes.
- **Passive** — the Utility Belt fires on incoming damage. It has no gesture, so
  it permits **no** cast mode. That is a different statement from "every mode
  applies", which is what leaving it out would have implied.

### And one thing that is not on these axes

**Charges.** A2's three charges on an eight-second recharge is a *resource*
question, orthogonal to target, input and mode alike. It is not a gap in the
categorisation; it is a fourth thing that the categorisation deliberately does
not reach.

### Fallback rather than refusal

A preference that cannot be honoured falls back to the form's own shape. A
player who prefers Quick still gets the throw when they hold; they simply do not
get a version that fires on a press, because no such version exists.

---

## Ability cast types — reference labels

**Settled 28 September 2026.** Four names for how an ability is aimed, so that
presenting a new ability does not require inventing machinery for it. Each label
says what the ability needs from the input layer and whether it gets it.

A vocabulary, not a hierarchy. An ability is described by one of these; nothing
ranks or nests them.

| Label | Aims at | Example | Today |
| --- | --- | --- | --- |
| **Unit targeted** | one thing, already identified by the click | wax a block · heal a teammate | **supported** |
| **Area targeted** | a set of blocks | set this grass patch alight · break this vein | **supported** |
| **Direction targeted** | a facing, then fires | lunge this way · observe who I am looking at | **supported** |
| **Vector targeted** | a start *and* an end | — | **not supported** |

### Unit targeted

The block and the entity are both **carried from the click**, not re-found.
"Used *on* that player" and "whatever my raycast finds now" are different
predicates, and a heal that re-found its target could heal whoever stepped into
the line afterwards.

They are separate because the client sends one interaction or the other, so an
ability reads whichever its form expects and **refuses cleanly** when neither is
there.

### Area targeted

This is exactly what an ability's preview returns. An area ability therefore
gets a targeting preview for free, and is the form cast modes were built around.

### Direction targeted

Facing is read at **resolution**, not at trigger — which is what lets a
multi-step machine point three different ways as the player moves the mouse. It
may preview the blocks along its line, or nothing at all when the line is the
obvious part.

### Vector targeted — named before it is needed

Nothing uses it. It is named anyway, because naming it now is what stops it
being reinvented badly the first time something does.

What it needs that the others do not is **two designations in one activation**.
A vector wants a first press that fixes the start and a second that fixes the
end — which is Double cast's shape used for *targeting* rather than for
*commitment*, on the same input.

> That collision is the design question to settle before building it, not an
> implementation detail.

Under Double cast a vector ability would need three presses. Under Hold it would
need a press, a drag and a release the input layer cannot see. Neither is
obviously right, and neither should be decided in a hurry by whoever needs the
first vector ability.

---

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

### A held aim is not a held cast

**The three modes are about targeting and verification, and nothing else.** They
decide whether a player sees what they are about to affect before it happens:

| Mode | |
| --- | --- |
| **Quick** | cast with no verification |
| **Hold** | hold the input, see the indicator, release to commit |
| **Double** | press to see the indicator, press again to confirm |

Whether an ability then *does something while held* is a **separate question
that belongs to the ability**. A persistent beam that runs until released is a
held **cast**. Its length has nothing to do with verification, and it can be
cast all three ways.

Both phases ask the input layer the identical question — *is the button still
down* — and are answered by one mechanism, because two heuristics could
disagree with each other. But they are different phases and mean different
things: the same signal means **commit** while aiming and **let go** while
sustaining.

### Which abilities a mode may reshape

**Settled 28 September 2026.** A cast mode is a preference, and some abilities
cannot honour every preference. The line is **not the ability's form**:

- A projectile that is merely **aimed** — all three modes.
- A **persistent projectile behaving like a beam** — all three modes. Its length
  is a held *cast*, which is the ability's business and has nothing to do with
  whether the aim was verified.
- A beam **that widens while being aimed** — **not Quick**.

> The question is whether the ability's output depends on how long the input is
> held **during the aim**.

In that third case the hold does **two jobs at once**: it shows the indicator
*and* it grows the beam, and the release both ends the growth and commits. Quick
is the one mode with no held phase at all, so it measures nothing and would
resolve such an ability at **zero every time** — the minimum, silently. That is
an ability broken by a preference, not a preference honoured.

**Quick is upgraded to Hold, not refused.** Refusing leaves a player unable to
cast because of a setting; a tap under Hold already behaves as Quick does, so
the upgrade costs the quick-caster almost nothing and the ability nothing at
all.

**Double survives, with a consequence.** It stays available and means what it
says — but the **second press must itself be holdable**: press to aim, press and
hold to run, release to end. A second press that fired and forgot would carry
the same zero-hold defect Quick has, one press later.

Hold-dependence is declared **per activation, with the branch in hand**, so a
projectile branch and its widening-beam branch answer differently for the same
ability.

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

**Stunned targets take the LATER expiry, not a refresh.** This is the one place
Stun's rule differs from Root's, and the difference is deliberate: refreshing
lets a short Stun *shorten* a long one applied a tick earlier — an incidental
Crater cutting Bloodmason's Anatomb completion Stun down to half a second.
Stacking is still refused, for Root's reason. Two 40-tick Stuns at the same
instant expire at 40, not 80.

[OPEN] Stun durations per source.

**Implemented 29 September 2026** as `Stun.java`, which also supplies the
enforcement **Root never had** — `ToolboxStatuses` had owned Root since the
Utility Belt shipped and nothing in the plugin ever read it, so the status was
set correctly, tested, and inert. Both are enforced behind one listener so that
cannot happen separately to each again. Movement is refused by rewriting the
destination's position while keeping the event's yaw and pitch, because
Minecraft carries a move and a look in the same event and cancelling it outright
would take both.

[TECHNICAL RISK] **"Prevents all player input" is not reachable: a stunned
player can still look around.** `org.bukkit.Input` carries movement keys only,
there is no attack or use signal to suppress, and the client always sends the
click. Cancelling the resulting events — interact, attack, break, place,
consume, drop, and the ability cast itself — recovers every consequence except
the camera, which cannot be taken. This is the same wall the
[Waxer capability audit](#18-waxer) hit with Amber.

**Retained look is therefore a property of Stun here, not a defect awaiting a
fix.** It is arguably better for the long ones: Bloodmason's completion Stun
scales with missing Health and can run for seconds, and a player who can watch
what is happening to them is being told more than one whose camera is frozen.

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

### Bloodmason — proposed 28 September 2026
Primary:
- Construction

Secondary:
- Combat

Working, per the proposal. Combat emerges from how the constructions manipulate fights rather than from authored damage. The kit, both branch trees and the ultimate were supplied together; see [section 20](#20-bloodmason). [OPEN] Whether this equals a completed conceptual pass is the owner's call. Feasibility was assessed on 28 September 2026 in the same section: the physiology is nearly free, the Athanor Anatomb is the majority of the cost, and Stun must be built as a status before the class can be.

### Fungal Assassin — documented 29 September 2026
Primary:
- Development

Secondary:
- Combat

Difficulty 3/5. Previously carried a difficulty rating with no canonical kit. See [section 23](#23-fungal-assassin).

### Looming Talismaniac — documented 29 September 2026
Primary:
- Production

Secondary:
- Construction

Difficulty 4/5. Previously carried a difficulty rating with no canonical kit, and is **less finished than the short roster summaries implied**: a third Imprint branch, a third A2 branch and the ultimate's economy are all unresolved. See [section 24](#24-looming-talismaniac).

### Chef — proposed 29 September 2026
Primary:
- Production

Secondary:
- None. Chef is a deliberately pure single-archetype exemplar.

Difficulty 1/5, and the first of the seven introductory archetype exemplars. See [section 22](#22-chef) and [section 21](#21-class-difficulty).

### Paver — proposed 29 September 2026
Primary:
- Construction

Secondary:
- None. A deliberately pure single-archetype exemplar.

Difficulty 1/5. See [section 25](#25-paver).

### Chauffeur — proposed 29 September 2026
Primary:
- Logistics

Secondary:
- None. A deliberately pure single-archetype exemplar.

Difficulty 1/5. **Supersedes the placeholder name Stationmaster**, which never had a kit. See [section 26](#26-chauffeur).

### Difficulty as a second axis
Every class now also carries a difficulty rating; the scale, its six dimensions and the 13-class distribution live in [section 21](#21-class-difficulty). Four of the seven planned exemplars — Werewolf, Wayfinder, Quarryman and Groundskeeper — exist as names and archetype questions only. Two global world-system rules and the Difficulty-1 design principles live in [section 27](#27-global-world-system-rules).

### Unsettled drafts
None of the original roster. Waxer and Daredevil received full kits on 13 September 2026; Skeleton Crew and Lightfooted were settled on 12 September. See the [draft record](#14-class-draft-record) for how the roster reached that state. Promotion out of draft status is not a claim that every quantity is decided.

Bloodmason, Chef, Paver and Chauffeur are **new proposals** rather than promoted drafts, and are not yet settled. Fungal Assassin and Looming Talismaniac are **newly documented rather than new**: both predate this entry and were previously recorded only as rows in the difficulty table.

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

---

# 20. Bloodmason

**Proposed 28 September 2026.** Kit, both branch trees and the ultimate were
supplied together. Reproduced as given, with the feasibility assessment added
below rather than folded into the design.

Archetypes: Construction primary · Combat secondary (working)

Hook: Bloodmason externalizes their physiology into temporary physical
architecture. Hemostasis determines what form that anatomy can successfully
take.

Combat emerges from how the constructions manipulate fights rather than from
authored damage — the same authored-versus-systemic line
[section 19](#19-toolbox) draws for Toolbox.

## Passive — Hemostasis

Bloodmason has **0–6 Hemostasis, starting at 3**. A1 decreases it; A2 increases
it.

- Low Hemostasis increases knockback received.
- High Hemostasis decreases knockback received.

These are not strictly penalty and reward. Increased knockback can facilitate
escape; reduced knockback improves positional stability.

### Stabilization

Every **5 / 4 / 3 seconds at Lv5 / 15 / 25**, provided the stabilization timer
has not been reset:

```
0 → 1 → 2 → 3
4 ← 5 ← 6
```

3 and 4 are both stable, and natural stabilization never crosses between them.
Ability-driven Hemostasis changes restart the stabilization interval.

### Pathological endpoints

**P0 — Ischemic.** The triggering cast still occurs at full P0 efficacy;
Bloodmason self-Stuns; Hemostasis resets to 1.

**P6 — Embolic.** The triggering cast still occurs at full P6 efficacy;
Bloodmason self-Stuns; Hemostasis resets to 5.

While either Ischemic or Embolic persists, subsequently reaching either
endpoint self-Stuns again and deals substantial self-damage.

**0 and 6 are transient, never resting.** Both reset immediately, so no cast can
ever originate from them. This is why the A1 and A2 manifestation tables below
are short one entry at each end rather than incomplete: **P5A1 and P1A2 are
unreachable by construction.** See the feasibility note on holding this as a
tested invariant.

## A1 — Capillary Flow

Targets the block Bloodmason is looking at and creates a temporary vertical
Blood Construction. **Its manifestation uses post-cast Hemostasis.**

| P | Manifestation |
|---|---|
| P4A1 | 1×1×4 stationary downward flow. Dense, relatively weak; standard duration. |
| P3A1 | 1×1×4 upward moving column, travels upward 3 blocks for max reachable height 7. Standard duration and speed. |
| P2A1 | 3×3×4 upward moving column, travels 3 blocks, max 7. Standard duration and speed. |
| P1A1 | 3×3×4 fast upward column, travels 3, max 7, with faster propagation and entity lift. Shorter duration. |
| P0A1 | 3×3 stationary eruption, each column extending independently up to 10 blocks or ceiling. Violent upward propulsion and enemy damage. Short duration. **Triggers Ischemic.** |

Ordinary moving columns are **segments**: their tails disappear as their heads
rise. They do not leave permanent 7-block elevators. Ceilings **truncate** them
rather than being broken or circumvented.

The progression is therefore: downward/clotted → circulating → broad flow →
hemorrhagic → eruptive.

### A1 branches

**Nausea Wave** — enemy disruption. Enemies carried by Capillary Flow become
physiologically disrupted: riding or exposure drains Hunger and applies a brief
Slow. Minecraft's literal Nausea visual is **not** required. Stronger and
broader Flow manifestations naturally expose more enemies without special
P-value rules.

**Bloodtrail** — circulation network. Capillary Flow leaves a temporary
Bloodtrail on blocks it contacts, **visible only to Bloodmason**. Casting
another Capillary Flow through a Bloodtrail amplifies that Flow by one P-level
toward lower Hemostasis, **without changing the Hemostasis cost or state**.

At Hemostasis 4, an ordinary cast goes 4 → 3 producing P3A1; through a
Bloodtrail it still goes 4 → 3 but manifests as P2A1. Likewise P3 → effective
P2, P2 → effective P1, P1 → effective P0.

Trails follow the actual blocks touched by the moving column, not merely its
origin. P3's 1×1×4 segment rising three blocks traces its vertical route; P2's
broad Flow leaves a much larger 3×3 trail. The world records where Bloodmason
has circulated.

**P4 → P3 is particularly valuable**: Bloodtrail turns the otherwise weak
downward P4 Flow into the ordinary upward P3 Flow, a qualitative payoff for
having prepared that location.

**P0 amplification caps at P0.** There is no P−1 and no new manifestation is
invented.

[OPEN] Whether the trail written corresponds to the **nominal** or the
**effective** P-level. If effective, a trail-amplified broad Flow lays a
correspondingly broader trail, and prepared terrain becomes progressively
cheaper to escalate on. That compounding loop may be the intended territorial
fantasy, but it needs a number.

**Lifeblood** — allied sustain. Allies who remain within Capillary Flow for a
required exposure duration heal for a **flat N HP, not a percentage of maximum
Health**. Flat scaling prevents Lifeblood disproportionately favouring
high-Health classes and makes the Flow itself the important resource.

The exposure requirement is essential: clipping an ally with Flow is not a heal
button. They must ride or use the construction. This produces useful
differences naturally — P4 an awkward downward healing stream, P3 narrow and
precise, P2 broad and team-accessible, P1 broad but fast and short-lived so the
dwell is harder to achieve, P0 a potentially powerful emergency team lift and
heal whose violent geometry and Ischemic consequence make it inherently messy.

**Each individual Flow heals a given ally once**, rather than every N seconds
indefinitely. Otherwise P2 and P3 become stationary healing infrastructure
despite the ability fundamentally being moving circulation.

## A2 — Viscerwall

Creates solid anatomical Blood Construction. **Manifestation again uses
post-cast Hemostasis.**

**P2A2 — Blood floor.** The blood cannot support upright architecture. It
manifests horizontally as a floor, semi-solid, with players able to sink through
it somewhat like powder snow. Capillary Flow explicitly passes through it.

**P3A2 — Rudimentary wall.** Smaller, flatter, sparse bone supports,
predominantly soft tissue; imperfect, relatively crude anatomy.

**P4A2 — Mature Viscerwall.** Slightly curved, concave toward Bloodmason;
maximum central height about 5 blocks; clear bone and rib structure; organized
tissue stretched between supports. The normal healthy Viscerwall.

**P5A2 — Fortification.** The most principled and architecturally structured
Viscerwall: larger area, harder constituent blocks, a strong regular skeletal
frame, deliberate ribs, braces and arches, and intentionally designed defensible
choke points. P5 is not merely trying to seal maximum area — **its openings make
the terrain more defensible.**

**P6A2 — Pathological jaw.** Architecture becomes excessive and malformed. It
reverses curvature, becoming convex toward Bloodmason and opening toward
attackers, with Bloodmason standing approximately at its throat. Walls and
overhang form a mouth intended to trap attackers. Substantially more holey than
P5, with irregular fatty and organic masses and connective tissue; abundant bone
becomes irregular and aggressive rather than architecturally principled; red
pointed-dripstone-like teeth grow from bone supports. **Triggers Embolic.**

Upper teeth fall and crush once, as is their purpose; lower teeth remain.

The visual progression is therefore: collapsed tissue → crude anatomy → mature
anatomy → optimal architecture → pathological overgrowth.

**P6 does not obsolete P5.** P5 is the superior fortification; P6 is the extreme
battlefield intervention.

### A2 branches

**Render Flesh** — hazardous material. Some of Viscerwall's soft-tissue sections
are rendered into hazardous material: portions of flesh become Magma Blocks,
whose damage is physical and world-based and **does not discriminate between
allies and enemies**. The underlying geometry is unchanged; the branch changes
the consequences of contacting or traversing it. At P6A2 the pathological jaw
additionally develops flowing lava through portions of its structure. P5's
deliberate choke points naturally become dangerous places to contest.

**Fracture** — footprint and topology. Viscerwalls grow in **separated
anatomical sections** rather than one continuous structure, occupying a larger
overall footprint. Major sections split apart along generated fractures,
creating larger gaps and additional passages. **This is not random
perforation**: recognizable pieces of the wall are displaced from one another.
The tradeoff is greater territorial reach for less continuous enclosure and
integrity. The effect applies to the existing P-value geometries rather than
replacing them — even P5's principled fortification spreads farther, but its
otherwise deliberate defensive topology becomes less sealed.

**Adipose** — contact control. Viscerwall develops soft fatty tissue that is
easier to destroy but catches enemies that contact it. The wall becomes easier
to break, and an enemy touching its structure is briefly **Rooted**. Root
retains its existing systemic meaning per
[Movement and input statuses](#movement-and-input-statuses): the enemy loses
voluntary movement but can still attack, mine and otherwise act. Consequently a
caught enemy can respond by breaking the deliberately weakened structure holding
them. The branch's effectiveness depends strongly on the base wall geometry
rather than receiving an arbitrary magnitude bonus: it is difficult to force
meaningful contact with smaller or less enclosing walls, while P6A2's jaw,
overhang and trapping geometry make Adipose particularly dangerous.

### The six-way branch vocabulary

| | A1 — Capillary Flow | A2 — Viscerwall |
|---|---|---|
| | Nausea Wave — physiological disruption | Render Flesh — hazardous material |
| | Bloodtrail — circulation network | Fracture — expanded topology |
| | Lifeblood — allied sustain | Adipose — contact control |

**None is simply a bigger ability, and none requires abandoning the P-value
system.** The P-values supply the geometry; the branches change what that
geometry means when something interacts with it.

## Ultimate — Athanor Anatomb

On cast, Bloodmason checks the surrounding physical space and creates **the
largest architectural stage of the Anatomb that fits**. The architecture
consists of multiple increasingly tall stages; the full manifestation resembles
an upside-down Vitruvian Man expressed as a building, widening as it rises.

A smaller structure is easier to escape. A taller structure provides
progressively more capability to force the fight upward into larger, more remote
chambers and prevent it from returning downward.

Bloodmason **levitates** during the ultimate. The Anatomb lasts for a fixed
duration or until Bloodmason dies.

**Initial state.** The architectural shell manifests, but its interior begins
without its developed blood or bone reinforcement. Bloodmason completes that
anatomy during the fight.

**Allied protection.** When allies inside take damage, the structure physically
breaks down to support and heal them. Protection **visibly consumes** the
Anatomb rather than applying an abstract defensive modifier.

**Enemy damage → Bloodmason Health.** Damage dealt to enemies inside any Anatomb
room heals Bloodmason. This includes allied damage, making the trapped teamfight
itself the ultimate's metabolic fuel.

### A1 during Anatomb — Hemorrhage

Capillary Flow becomes Hemorrhage. **Hold to continuously channel.** Every 0.5
seconds held:

- costs Bloodmason Health;
- Hemostasis decreases by 1;
- all rooms simultaneously flood upward;
- enemies are damaged and drained;
- players are driven upward through the structure.

The damage feeds the enemy-damage → Bloodmason-healing loop. Its spatial purpose
is to drive the fight toward the increasingly difficult-to-escape upper Anatomb.

### A2 during Anatomb — Teratoma

Viscerwall becomes Teratoma. **Hold to continuously channel.** Every 0.5 seconds
held:

- costs Bloodmason Health;
- Hemostasis increases by 1;
- all rooms simultaneously grow bone and tissue inward;
- damaged architecture is reinforced;
- navigable interior volume physically narrows.

Hemorrhage pushes combatants up; Teratoma increasingly prevents them from
finding usable space or routes out. **Hemostasis endpoint consequences remain
active during both channels.**

### Anatomb completion

Hemorrhage and Teratoma globally contribute toward completing the initially
empty architecture. Their contributions are **additive**: Bloodmason can
complete it with different proportions of blood and structural tissue, with
Hemostasis itself making extreme one-sided construction dangerous.

Upon reaching the required total completion, **every enemy inside the Anatomb is
massively Stunned**, with duration scaling on Bloodmason's **missing Health
percentage**.

That creates the final risk: Bloodmason can deliberately continue spending
Health to push completion while increasing the eventual Stun duration — but
dying immediately ends the Anatomb.

**The Hemorrhage death spiral is intentional and is not unreactable.** The
player knows the abilities are dealing damage to them.

## What each component manipulates

| Component | Bloodmason manipulates |
|---|---|
| Hemostasis | their physiological equilibrium |
| Capillary Flow | circulation as world geometry |
| Viscerwall | anatomy as world geometry |
| Athanor Anatomb | an entire fight as functioning anatomy |

## Feasibility assessed 28 September 2026

The physiology is nearly free; the architecture is the whole cost. Assessed
against the Paper 1.21.11 plugin as built.

**Free.** Hemostasis, the stabilization clock and its reset-on-ability-change,
the 3/4 no-cross rule, the pathological endpoints, the completion meter,
additive blood/bone contribution, and Stun duration scaling off missing Health
are all counters and timers. Stabilization is a **fourth clock** alongside
Ability, Growth and Task, but the cheap kind.

**Two pieces land on machinery built in September 2026.** Hemorrhage and
Teratoma are `InputForm.CHANNELED` with a 0.5s tick — the first real consumer of
the hold and sustain path. And `Provenance` already tracks per-block ownership
in the chunk PDC, which is what "Capillary Flow passes through the blood floor"
and the whole-class reset both require. That is the single biggest thing already
solved.

**Two prerequisites.**

- ~~**Stun does not exist.**~~ **Built 29 September 2026** as `Stun.java`, with
  the enforcement Root had been missing since the Utility Belt shipped. Its
  combination rule is later-expiry-wins rather than refresh, specifically so an
  incidental Stun cannot shorten the Anatomb's completion Stun.
- [TECHNICAL RISK] **A stun cannot block mouse inputs.** `org.bukkit.Input`
  carries movement keys only, and the client always sends the click. Cancelling
  `PlayerInteractEvent`, the attack `EntityDamageByEntityEvent`,
  `BlockBreakEvent`, `BlockDamageEvent` and the cast in `AbilityInputs` recovers
  everything except **camera look**, which cannot be taken. This is the same
  wall the [Waxer audit](#18-waxer) hit with Amber. Retained look is probably
  acceptable and arguably more legible for a long stun, but it is a property of
  Stun that should be stated rather than discovered.

[TECHNICAL RISK] **Neither direction of the knockback axis is an attribute.**
`KNOCKBACK_RESISTANCE` only *reduces*, so amplification is manual velocity
applied after the hit resolves. Expressing the low end as increased damage taken
instead is **not** cheaper — there is no incoming-damage-multiplier attribute
either, so that is also event-time math, and it additionally lands on the
invulnerability-frame behaviour where the first 10 ticks apply only excess
damage. Decide this on design, not on cost. The original two-sided framing is
the more interesting one, and since A1 drives Hemostasis downward, making the
low end a pure penalty would have the kit punishing its own primary line.

**Blocks cannot be retextured or remodelled per instance.** There is no
CustomModelData for blocks and any retexture is map-global. The working rule is
therefore: **blocks for anything that must be stood on or blocked by, display
entities for everything that only needs to be seen.** Under that division the
structural skeleton needs vanilla blocks that read acceptably and all the
character — ribs, arches, fatty masses, teeth — is drawn. `TEXT_DISPLAY` is
already used in `DraftHallWorld` and `DraftingColosseum`, but `ITEM_DISPLAY` and
CustomModelData are a new resourcepack path. Display entities have **no
collision**, and entity count is a per-player packet budget that needs a number
before a stage is densely dressed.

**Teeth.** Falling pointed dripstone and falling anvils damage entities **on
landing only** — never on mid-air collision, and a `NoGravity` tooth deals
nothing. That suits upper teeth that drop once at summon, with damage scaling on
chamber height. Spawn the falling block already rendering as the intended block
rather than retrofitting, and use `setCancelDrop(true)` so nothing is left
behind to poison the reset path. [OPEN] Whether the teeth are custom-drawn or
literally dripstone; the exact implementation is deliberately left open.

**A1's upward push.** Water flows, so "which water is mine" is only hard if the
water is allowed to spread; cancelling `BlockFromToEvent` makes the authored set
the exact set. Drive the push off the **active volume**, not off block lookups,
so a broken block does not put a hole in it. `bubble_column[drag=false]` is
client-predicted and therefore smooth; per-tick `setVelocity` is tunable but not
predicted and will fight the client, especially on a swimming player. Set rather
than add, or Y compounds into a launch. `TunnelingAbility` is the existing
precedent for sustained server-driven movement, though it is itself untested.

[OPEN] **Blood cannot be red as a fluid.** Water tint is a *biome* property, so
the only per-region lever is writing a custom red-water biome per 4×4×4 cell with
a chunk resend, on every cast and every expiry. Building the blood out of
translucent display entities instead removes the fluid entirely — any authored
colour, no flow, no biome writes, and the rising column becomes client-side
interpolation rather than a marched block set. The cost is losing swimming,
buoyancy and drowning, none of which the spec asks for. P2A2's blood floor is
the exception: displays have no collision, so the one manifestation that is
explicitly solid-ish is the one that wants real blocks. Powder snow supplies the
sink natively; suppressing `freezeTicks` each tick removes the freezing, the
frost vignette and the freeze slowness together, and the sink **rate** is a
hardcoded constant and not a tuning knob.

**The Anatomb.** Two design decisions make this moderate rather than high.
**Preauthor the structure complete** — shell, full blood, full bone — per stage,
so runtime state is a subset selector over authored positions and never
generation; completion becomes a count, and breakdown draws from an authored,
art-directed vocabulary of what may break and in what order. And **replace room
membership with a contact tag**: tag allies and enemies on contact with
Hemorrhage blood or Teratoma tissue, with a decay, exactly as Observed and Root
work today. A tagged ally taking damage mitigates and consumes the structure; a
tagged enemy taking damage heals Bloodmason. That converts a per-tick geometry
test against every player into a status timer, and it reads better — the heal
becomes a consequence of Bloodmason working rather than of enemy geography.
Teratoma and Hemorrhage then **march** an ordered authored list, two ticks of
channel per step, which the artist orders rather than an algorithm.

What remains genuinely costly is the **fit-the-largest-stage** plan pass, which
must be read-only and complete before a single block is placed — a
half-constructed stage is an object nobody designed. Levitation is likely a held
velocity rather than the Levitation effect, which is a constant upward drift
inside a structure that must not be clipped through.

**Two decisions are nearly free today and expensive to retrofit.**

- **Build A1 plan-first.** Bloodtrail needs the column's route before it
  manifests, both to test whether it crosses a trail and to write the new trail
  along blocks actually touched. The same pass serves `preview()`.
- **Bake labelled sections into the wall authoring format.** Fracture displacing
  *recognizable pieces* is a per-section offset if the format carries sections,
  and intractable if it is a flat block list — where it degrades into exactly
  the random perforation the branch rules out. Decide before any P-level is
  drawn.

**Branch costs.** Render Flesh's magma is a palette substitution with zero
behavioural code, and Lifeblood and Adipose are free once the volume loop and
Root exist. Adipose's "easier to break" must be a *softer block in the palette*
rather than a modifier, since hardness is per block type — which is arguably
better, because the softness becomes legible to the enemy breaking out.
Bloodtrail is free given plan-first A1, and visible-only-to-Bloodmason is
already solved: `Player.spawnParticle` sends to one player, as `TargetPreview`
does today. P6's flowing lava needs the same flow containment as the blood plus
airtight reset registration; it is the one branch that can leave permanent
damage.

[OPEN] **Nausea Wave's Hunger drain is not a flavour effect.** Hunger is a
Growth-scaled capacity starting at 9, and `HungerRegen` gates health
regeneration on it — two empty drumsticks block regeneration entirely. Draining
Hunger therefore *shuts off an enemy's healing*. It also scales wrong: the regen
gate is relative but a flat drain is absolute, so the same drain is
proportionally brutal against a Lv3 player with 9 Hunger and mild against a
Hunger-specialized Lv25. Either express the drain as a fraction of effective
Hunger, or accept that the branch is an anti-healing tool aimed at the early
game and balance it as one.

**Invariant worth testing.** A1 never fires at Hemostasis 6 and A2 never fires
at 0, which is what makes P5A1 and P1A2 unreachable rather than undefined. Hold
it as an assertion, because the day a third Hemostasis mover is added is the day
it silently stops being true.

**Build order.** A1, A2 and the passive are a shippable class on their own; the
Anatomb is a second project roughly the size of the Toolbox circuit. That is
also the only possible order, since the ultimate's channels are re-expressions
of A1 and A2.

## Open

- Exact timings and numbers throughout.
- Exact generated geometry and block palette per P-level.
- Growth progression.
- Whether blood must be red, which decides whether the display-entity path is
  load-bearing or optional.
- Whether P6's teeth are a one-time drop or standing hazards; the answer picks
  the mechanism.
- Display-entity budget per Anatomb stage.
- Whether Bloodtrail writes nominal or effective P-level trails.
- Nausea Wave's Hunger drain scaling.

---

# 21. Class Difficulty

**Introduced 29 September 2026.** Difficulty is a second axis alongside
archetype. It is recorded here rather than per class so that the scale, its
dimensions and the roster distribution stay in one place.

## Why it exists

The roster was accumulating classes whose kits were interesting **precisely
because** they had unusual Minecraft-specific rules — Toolbox, Bloodmason,
Merchant, Skeleton Crew, Talismaniac. That created a roster-design problem
separate from whether those classes were good:

> How difficult is it for a new player to understand what this class wants them
> to do, and produce useful value with it?

The answer could not be "number of mechanics." A mechanically simple ability can
be difficult because knowing *when*, *where* or *why* to use it is difficult.
An ability with an elaborate implementation can be easy if its correct use is
obvious.

Conventional low-difficulty MOBA characters make the point. Garen, Master Yi and
Yuumi do not have empty kits; their **decision grammar** is unusually clear.
Garen sees enemies near him and spins; Yi wants to engage or avoid an attack and
presses Q; Yuumi has a hurt ally and heals them. The player spends little
attention deciphering their own kit, which leaves them free to learn the game
around it.

## What difficulty measures

An aggregate judgment across six related dimensions:

- **Mechanical** — how demanding is execution itself?
- **Technical** — how much class-specific machinery must the player understand and track?
- **Decision** — given the current situation, how obvious is the correct action?
- **Role** — how hard is it to understand and perform the strategic job the class exists to do?
- **Success floor** — how hard is it for an inexperienced player to produce meaningful value rather than merely operate the abilities?
- **Mastery ceiling** — how much class-specific expertise exists above basic competency?

**The last dimension does not automatically raise the rating.** A Difficulty-1
class can have substantial mastery potential if that mastery comes from becoming
better at Minecraft — positioning, timing, resource economics, terrain,
opponents, the archetype itself — rather than from deciphering increasingly
complicated class procedures.

The diagnostic that matters most:

> **Difficulty-1 classes are easy because their choices are always clear.**

## Difficulty 1 — clear choices

The target for the seven pure-archetype introductory classes. An ability should
usually have an obvious job, and where it has several, **the context should
choose the job for you**. Food Fight technically does several things; that does
not make Chef difficult, because the player is not choosing between three
competing Food Fight modes. The target tells them what Food Fight means.

Difficulty-1 design prefers low ambiguity of intent, distinct ability jobs,
obvious target relationships, mutually exclusive upgrade branches, quantitative
rather than categorical failure, no mandatory sequencing puzzle, no resource
system requiring constant optimization, no setup that one ability unexpectedly
consumes for another, and no multiple equally plausible buttons competing for
the same situation.

**A branch may still require optimization.** Rake the Coals rewards intelligent
fuel use and Broiler Rack rewards filling the cooker before pressing it. The
beginner still knows exactly what to do — food is cooking, press the cooking
button. Expertise improves the result rather than revealing what the button was
secretly supposed to be used for.

## Difficulty 2–4 — increasing decision burden

Not rigid buckets; broadly increasing combinations of execution, knowledge and
strategic ambiguity.

**2** — Straightforward kit with meaningful positioning, sequencing, targeting
or specialization knowledge. Readily understandable, but mistakes begin to come
from using an understandable tool incorrectly rather than executing it poorly.

**3** — Moderate class-specific learning burden. The player must understand
interactions among abilities, world state, setup, positioning or role
responsibilities to consistently get the intended value. Still reasonably
learnable from the individual abilities.

**4** — Substantial strategic or technical burden: persistent systems, prepared
world state, coordinated autonomous elements, consequential macro decisions, or
interactions not reducible to one obvious action per situation. **Basic
operation is substantially easier than fulfilling the role well.**

## Difficulty 5 — the class itself becomes a major game

Understanding and operating the class is itself a substantial part of the
player's workload. **Bloodmason** ([section 20](#20-bloodmason)) is the clearest
conventional example: Hemostasis, its stabilization behaviour and
directionality, post-cast manifestation levels, endpoint pathology, materially
different manifestations per P-value, spatial construction consequences, and an
ultimate converting health, architecture, Hemostasis and combat into one
another. A competent Bloodmason is not selecting the correct spell; they are
continually reasoning about what physical world state a cast at their current
Hemostasis will create.

## 5+ — open-ended and combinatorial

**Toolbox** ([section 19](#19-toolbox)) effectively broke the scale. It is not
merely a very complicated fixed kit: its inventory is a programmable Redstone
machine, and increasing inventory geometry, component vocabulary and
player-discovered combinations mean **its possibility space itself expands**.
That is qualitatively different from memorizing a large kit.

5/5 remains the useful displayed maximum; Toolbox is treated internally as
something like 5+ or 7/5.

> Bloodmason has a large language to learn. Toolbox lets the player write
> programs in its language.

That distinction matters more than the literal number.

## Current 13-class difficulty table

The developed classes under consideration, before the seven introductory
archetype exemplars.

| Class | Difficulty | Principal source of difficulty |
|---|---|---|
| Mole | 2/5 | Spatial judgment and initiation/trapping; individual verbs remain straightforward |
| Kitfighter | 2/5 | Equipment and situational combat choices, but direct goals and relatively obvious buttons |
| Gardener | 3/5 | Cultivar/plant interactions, territorial Development, deciding where ecological setup creates value |
| Golem Master | 3/5 | Managing constructed autonomous units and translating building into combat/production value |
| Lightfooted | 3/5 | Movement execution and target access, without a particularly complicated underlying system |
| Fungal Assassin | 3/5 | Territorial preparation, mold/mycelium state, knowing where preparation creates assassination opportunities |
| Merchant | 4/5 | Workforce and labour economy, payroll, useful-work Mastery, macroeconomic allocation |
| Daredevil | 4/5 | High-commitment traversal, trajectory judgment, severe consequences for poor engagement |
| Skeleton Crew | 4/5 | Undead workforce management, Logistics infrastructure, coordinating autonomous labour with Supply Lines |
| Waxer | 4/5 | Manipulating Minecraft system states through waxing/sealing/preservation, many contextual interactions |
| Looming Talismaniac | 4/5 | Linked banner networks, spatial relationships, distributed effects and traps |
| Bloodmason | 5/5 | Hemostasis management, manifestation state, architecture, pathological endpoints, state-dependent decisions |
| Toolbox | 5+/5 | Programmable inventory-board construction with combinatorial programs rather than a fixed solution vocabulary |

Distribution: **1 → 0 · 2 → 2 · 3 → 4 · 4 → 5 · 5+ → 2.**

**There was no genuine entry point.** And several of the easier existing classes
are not pure teachers of their archetypes either: Mole teaches a particular
disruptive interpretation of its roles, Kitfighter teaches equipment-centric
Combat/Production, Golem Master teaches Construction through animation and
autonomous creations.

## The seven Difficulty-1 archetype exemplars

Rather than simplifying the existing thirteen, the goal is **one deliberately
obvious expression of each fundamental archetype**.

| Archetype | Class | Fundamental question |
|---|---|---|
| Combat | Werewolf | How do I defeat enemies? |
| Construction | Paver | Where would adding blocks make this place more useful? |
| Exploration | Wayfinder | Where should I go, and what can I discover there? |
| Extraction | Quarryman | How do I efficiently remove valuable material from the world? |
| Development | Groundskeeper | How can I make this place more productive? |
| Production | Chef | How can I turn these inputs into more useful outputs? |
| Logistics | Chauffeur | What needs to move, and what can I drive to move it? |

These are **not beginner versions of more interesting classes**. They expose the
irreducible verb of the archetype, so that a player who does not understand
Production need not start with a hybrid class and infer what Production means
from it.

**Difficulty 1 does not require low systemic creativity.** It requires low
uncertainty about what the player should do next. Chef still throws pork into
furnaces, physically presses cooking progress forward, conserves fuel, scales
advancement with batch size, and forces enemies to attend a disastrous buffet.

**Three have kits**: Chef ([section 22](#22-chef)), Paver
([section 25](#25-paver)) and Chauffeur ([section 26](#26-chauffeur)).

[HISTORICAL] The Logistics exemplar was first named **Stationmaster**, with the
question "how do I get people and resources where they're needed?". It never had
a kit, and Chauffeur supersedes both the name and the question.

[OPEN] Werewolf, Wayfinder, Quarryman and Groundskeeper exist as names and
questions only.

The design principles these exemplars produced are recorded in
[section 27](#27-global-world-system-rules), not here, because they constrain
class design generally rather than only the difficulty scale.

## A note the scale deliberately does not measure

Difficulty rates the **player's** burden, correctly and by design. It says
nothing about implementation cost, and the two can diverge sharply. Chef is the
roster's clearest case: a Difficulty-1 class whose Flip and Press carry-over and
Buffet inventory custody make it one of the more expensive classes to build. A
low rating is not a scheduling estimate.

---

# 22. Chef

**Proposed 29 September 2026.** The first of the seven Difficulty-1 archetype
exemplars ([section 21](#21-class-difficulty)). Reproduced as supplied, with the
feasibility assessment kept separate below.

Primary archetype: **Production**
Difficulty: **1/5**

Core fantasy: turn ordinary inputs into useful outputs, quickly and
efficiently.

Chef is the deliberately simple, pure expression of Production. The class
teaches the archetype through ordinary Minecraft transformation processes rather
than through an abstract Production statistic. Chef gets more value from food
because the kit lets them **start** processes, **accelerate** them, **conserve**
their inputs, **increase** their throughput, and **weaponize** their outputs.

Chef's combat is intentionally straightforward and secondary. Food can be thrown
at enemies, the Spatula can flip an approaching enemy, and the ultimate creates a
large teamfight opportunity. Chef is not intended to win through elaborate combat
sequencing. **Their team should want Chef because giving Chef things to process
is useful.**

## Mechanical abstraction

Chef's Production verbs are **Load → Process → Improve → Serve**. Food Fight
loads an appropriate cooking process; Flip and Press accelerates an active one;
its branches specialize combat, input efficiency or throughput; Irresistible
Buffet converts stored output into battlefield control.

Chef never receives an abstract effect like "produced items are 20% better."
Specialization emerges from how the class interacts with actual Minecraft
resources and production machinery.

## Passive — Trusty Spatula

Chef's **highest-tier shovel** is treated as their Spatula. Holding the Spatula
empowers Chef's active abilities where specified.

The Spatula does **not** introduce a separate farming or melee subsystem. It does
not make Chef better at harvesting crops, does not itself modify food output, and
does not turn shovel material tiers into another ability-scaling system.

Its purpose is much smaller: Chef has a signature tool, and when you are doing
Chef things you hold it. Because abilities such as Food Fight draw food directly
from Chef's inventory, Chef never has to stop holding the Spatula to use them.

Abstraction: **equipment identity.**

## A1 — Food Fight

Chef flings the **first eligible food in their inventory** as a projectile. Chef
does not need to hold that food. Selection follows deterministic inventory order,
so Chef determines what Food Fight will use simply by arranging their inventory.

The projectile's effect is immediately apparent from what Chef throws it at:

- **Enemy** — damages the enemy.
- **Appropriate cooking source** — appropriate uncooked food enters the cooking process.
- **Neither** — the food is simply thrown.

While holding the Spatula, Food Fight travels farther.

The exact eligible-food list and supported cooking targets are implementation
details rather than additional player-facing decisions.

### A1 branches

Mutually exclusive. None creates a new operating mode; each makes one
already-correct Food Fight target more rewarding.

**Super Nutritious** — Food Fight heals allies it hits. Hurt ally → throw food at
them. Abstraction: output as sustain.

**Extra Spicy** — Food Fight sets enemies it hits on fire. Enemy → throw food at
them. Abstraction: output as offense.

**Reckless Rationing** — When Food Fight throws raw food into an active
compatible cooking station, an **additional raw copy is left on top of the
station**. Visibly: throw 1 raw food → 1 enters the cooker + 1 raw copy appears
on top. Abstraction: yield efficiency.

The copy is a distinct item, **"Recklessly Rationed ___"**. It stacks with
itself but **not** with the ordinary food item, and it **cannot itself be
Recklessly Rationed** — stated as flavour text on the item so the rule is
self-documenting rather than invisible.

[HISTORICAL] Reckless Rationing was first written as "food successfully cooked
through Food Fight produces double output." That version required provenance to
survive the cooking process and special output-stack logic. The current version
places the bonus at cast time instead, the duplicate is a normal raw resource
another player can visibly collect, and it distinguishes the branch cleanly from
the A2 Production branches: **Reckless Rationing creates more input material,
Rake the Coals conserves fuel, Broiler Rack compresses processing time.**

## A2 — Flip and Press

Chef uses the Spatula to flip and press either an enemy or food already
undergoing a cooking process.

**Against an enemy.** Chef flips the targeted enemy upward, then presses and
slams them back down. The enemy is briefly **Stunned** at the end. This is Chef's
simple close-range defensive and control button.

**Against cooking.** Used on an actively cooking Furnace, Smoker, Campfire or
other supported process, Flip and Press instantly advances its active cooking by
**N seconds of productive time**. This is not a timed workstation buff — N
seconds of cooking happen immediately. If three seconds remain on the current
item and Flip and Press supplies eight, that item finishes and the remaining five
continue into the next eligible item in the stack.

The ability therefore does not conceptually accelerate "64 porkchops"
simultaneously. It advances the **active** production process, carrying excess
advancement forward where the workstation normally permits sequential
processing. The corresponding fuel expenditure occurs as appropriate: base Flip
and Press accelerates time rather than generating free furnace energy.

Holding the Spatula improves Flip and Press's normal effect; the exact numerical
empowerment remains calibration work.

### A2 branches

**Hibachi Skills** — Enemies struck are launched higher before Chef presses them
down and Stuns them. No new combo or condition; it exaggerates the combat
function already present, and the additional airtime gives Chef's teammates a
larger opportunity to respond. Abstraction: combat specialization.

**Rake the Coals** — Flip and Press conserves some of the fuel its instant
advancement would otherwise consume. It does not reset a coal indefinitely or
manufacture fuel: Chef gets the normal productive-time advancement while paying
less of its corresponding fuel-time cost. Its value increases when fuel itself is
strategically expensive. Abstraction: input efficiency.

**Broiler Rack** — Flip and Press advances cooking farther when more eligible
food is stored in the station, up to a cap. A small batch receives close to the
ordinary advancement; a heavily loaded cooker receives substantially more. The
additional advancement consumes the corresponding fuel where applicable. The
optimization is extremely apparent: **if you want a bigger press, load a bigger
batch.** No completion proc, persistent buff or recursive trigger is tracked.
Abstraction: throughput.

The latter two deliberately solve **opposite** Production problems:

| Branch | Optimizes | Economic expression |
|---|---|---|
| Rake the Coals | Inputs | Do the work for less fuel |
| Broiler Rack | Time | Do more work immediately |

Neither says "Chef gains +X% Production." One Chef becomes valuable when inputs
are scarce; the other when the team has accumulated large quantities that need
processing now.

## Ultimate — Irresistible Buffet

Chef rolls out an extremely long buffet table along a path. Enemies struck as the
table rolls out are forcibly invited to sit down and eat.

**Chef is not seated, Stunned, channeling or otherwise occupied** after
deploying the Buffet, and can immediately continue moving, attacking, throwing
food or using Flip and Press while the caught enemies deal with the table.

### Seating

A seated enemy's **real hotbar becomes temporarily inaccessible**. All nine
hotbar slots instead display servings of Chef's first stored food. These are
temporary Buffet servings rather than ordinary inventory replacement: **the
victim's actual items remain safely suspended and cannot be displaced, dropped,
rearranged or lost through the Buffet mechanic.**

The victim can eat the provided food. To resume normal combat they must
physically leave or jump out of the seating. When they leave, the temporary
hotbar disappears, their real hotbar is immediately restored, and they take minor
damage for every serving they left uneaten.

### Damage

Deliberately secondary. Taking the penalty from all nine uneaten servings should
hurt, but should not remotely function as a one-shot. Its purpose is to put a
small cost on immediately abandoning the meal, not to turn Buffet into an
execute.

The real power is the interval during which an enemy has been displaced into
seating, lost immediate access to weapons, lost immediate access to combat
consumables and utility, been given food instead, and been forced to spend an
action and movement window getting back out — while Chef's team is still
fighting them.

### Eating is legitimate counterplay

Buffet food is genuinely food and enemies are allowed to benefit from eating it.
The victim therefore has a continuum: **leave immediately** and regain weapons
quickly at the maximum minor penalty; **eat briefly** for some benefit and a
reduced eventual penalty while remaining without a normal hotbar longer; or
**keep eating** for substantial free nourishment and little or no penalty, at the
cost of voluntarily spending a large amount of combat time sitting at Chef's
table.

This produces the intended success condition:

> **A good Buffet is cast when the enemy does not have time to eat.**

If Chef's team is already collapsing on the table, the victims having been given
food is nearly irrelevant — they need their weapons now. Conversely, **a bad
Buffet caters the enemy team**: catching several opponents when nobody can
capitalize lets them consume Chef's resources, recover and leave. That is
intentional.

### What kind of CC is Buffet?

**Not a long Stun.** Victims can act within the restricted situation — they can
eat, and can attempt to leave. What they cannot do is participate normally in
combat until they escape and recover their real hotbar. Its control comes from
**physical displacement + inventory denial + action inconvenience**, using
Minecraft's own spatial and inventory language.

Abstraction: **Production as battlefield tempo.** Chef has prepared resources,
and the ultimate serves those resources to the opposing team at precisely the
moment when stopping to consume them is least useful.

## Class abstraction

Chef is fundamentally about **transformation**, not food. Food is the clearest
introductory Minecraft expression of Production: raw food → cooking process →
finished food → strategic output. Every part of the kit occupies a distinct part
of that chain.

| Mechanic | Production concept |
|---|---|
| Trusty Spatula | Production tool |
| Food Fight → cooker | Loading / input |
| Reckless Rationing | Yield |
| Flip and Press | Processing time |
| Rake the Coals | Input efficiency |
| Broiler Rack | Throughput |
| Food Fight → player | Using finished output |
| Irresistible Buffet | Distribution / serving |

This also establishes boundaries with the other archetypes. Chef does not make
crops grow better — that is **Development**. Chef does not acquire raw food more
effectively — that is **Extraction** or Development depending on the source. Chef
does not move supplies across the map — that is **Logistics**. Chef does not
create productive buildings — that is **Construction**.

Chef's question begins once inputs exist: *what useful thing can I turn this
into, and how efficiently can I do it?*

## Difficulty-1 abstraction

| I see… | I do… |
|---|---|
| Food that needs cooking | Food Fight it into the cooker |
| Food already cooking | Flip and Press the cooker |
| Enemy at range | Food Fight them |
| Enemy close to me | Flip and Press them |
| Several enemies in a punishable position | Irresistible Buffet them |
| I'm using Chef abilities | Hold my Spatula |

The branches preserve that grammar. They change what Chef is especially good at,
not what Chef has to figure out how to do. That makes Chef a useful baseline for
the seven entry-level exemplars: the kit contains genuinely Minecraft-specific
systemic depth while the person playing it almost never has to ask *which button
solves the problem in front of me?*

## Feasibility assessed 29 September 2026

**Chef is the roster's clearest case of a class that is easy to play and
expensive to build.** The 1/5 rates the player's burden and says nothing about
implementation; see the note at the end of
[section 21](#21-class-difficulty).

**Free.** Food Fight's projectile and its three target behaviours, Super
Nutritious, Extra Spicy, Hibachi Skills, the Spatula tier check and the range
bonus. Deterministic inventory order is the same convention Toolbox's circuit
`FLOW` already uses and should be stated as one rule for both.

**Reckless Rationing's item is free and the loop is closed.** Differently named
items do not stack with their base item natively, so "stacks with itself but not
with the real thing" needs no code. The item remains the same material, so it
still smelts into ordinary output and can still be eaten and thrown. The
no-re-rationing rule is one tag check.

[OPEN] Two consequences worth a decision. The copy creates a **second stack that
sits somewhere in Food Fight's deterministic order**, so taking this branch
changes what A1 selects unless the player manages it — the one place a
Difficulty-1 class asks about ordering. And it **costs an inventory slot**, which
under the Capacity curve means it is weakest at Lv1 with 6 slots and free later:
the branch is least rewarding exactly when a new player takes it.

**Flip and Press's carry-over is the fiddly part.** Advancing the current item is
a cook-time setter. Advancing *past* it and continuing the remainder means
simulating the workstation's own progression — decrementing the input stack,
emitting output, re-checking eligibility and spending the corresponding fuel —
and it differs per station, with campfires cooking four slots independently.
Doable, not cheap, and wants tests per station type.

[TECHNICAL RISK] **Buffet's item custody is the highest-consequence piece in the
class.** "Actual items remain safely suspended and cannot be displaced, dropped,
rearranged or lost" is a promise about the worst failure mode a MOBA can have.
`InventoryGuard` and `LockedSlots` are precedent, but the dangerous paths are
**death while seated** and **disconnect while seated**, where restoration runs on
code paths normal testing never exercises. Write those tests before the feature.

**Seating works natively and cheaply.** Seating means mounting the victim on an
entity, and a mounted player can always dismount — which is exactly "must
physically leave or jump out." The escape is not implemented, it is inherited and
then decorated. A mounted player can also still act, which is precisely why
Buffet is correctly classified as **not** a Stun: the control is entirely the
hotbar denial.

## Open

- N for Flip and Press, the Spatula empowerment values, Broiler Rack's cap and scaling, Rake the Coals' conservation fraction.
- The eligible-food list and the supported cooking-target list.
- Buffet's table length, catch geometry, per-serving damage and seating duration.
- Whether Reckless Rationing's copy should be skipped by Food Fight's selection order, or left in it.
- Growth progression.

---

# 23. Fungal Assassin

**Documented 29 September 2026.** Previously carried a difficulty rating with no
canonical kit. Reproduced as supplied, with the feasibility assessment kept
separate below.

Archetypes: **Development + Combat**
Difficulty: **3/5**

Core fantasy: cultivate fungal territory through combat, inhabit it to hunt, and
make it grow.

Fungal Assassin is an assassin whose **preparation is itself Development**.
Rather than merely putting traps somewhere before a fight, the class establishes
a persistent fungal ecology. Fighting enemies grows that ecology; the ecology
makes subsequent hunting stronger; successful use of the territory further
develops it.

> Establish fungal territory → infect prey → expand through prey → inhabit the
> network → assassinate → leave behind a stronger ecosystem.

This is why it is not simply a mushroom-themed Combat class. Its combat and
Development verbs are deliberately **circular**.

## Passive — Creeping Colony

Connected Mycelium forms **Mycelium Networks** — developed fungal territory with
several persistent properties:

- nearby mushrooms and crops grow faster;
- Huge Mushrooms broken within a Network always drop mushrooms;
- attacking enemies applies **Fungal Growth**;
- Fungal Growth lasts longer when applied from within a Network;
- damage dealt by Fungal Growth near a Network accelerates that Network's growth.

### Fungal Growth

The class's infection and damage-over-time state. It is simultaneously combat
attrition against prey, a marker consumed and manipulated by the active kit, and
a mechanism by which fighting contributes back into Development.

That last point is fundamental. Ordinary Development asks *how do I make this
productive place mature?* Fungal Assassin answers: **fight things in and around
it.** Combat activity literally cultivates the territory that enables future
combat.

Abstraction: **combat as cultivation.**

## A1 — Bursting Spore

Consumes Fungal Growth from an infected target to establish Mycelium around that
target, closing the passive loop: hit enemy → infect enemy → Burst infection →
create territory.

The target is not merely taking an assassin debuff. **They become a vector for
Development.** Fungal Assassin therefore does not always have to walk somewhere
first, prepare it and wait — an enemy can carry the fungus into a location and
then be used to establish the next piece of territory.

### A1 branches

**Submersive** — Bursting Spore gains an aquatic manifestation. Used underwater,
Mycelium establishes beneath the target, a waterlogged vine grows upward toward
the surface, and the manifestation damages nearby enemies. This lets the fungal
ecology colonize a medium that would otherwise interrupt its normal terrain
language. Abstraction: environmental adaptation.

**Airborne Spread** — Fungal Growth becomes capable of propagating to nearby
targets, and Bursting Spore receives a wider spread. Infection stops being purely
one-host-at-a-time and becomes an outbreak. Abstraction: propagation.

[OPEN] The recovered design alternates between **the Burst itself** becoming
wider and **the resulting Mycelium spread** becoming wider. Preserved as a design
clarification rather than silently resolved — see the feasibility note, since
these are different code paths rather than different numbers.

**Root Network** — Fungal Growth establishes a spatial relationship between
infected prey and its originating Network. If that enemy travels sufficiently far
away, **the Network pulls them back toward it**. This is not "Fungal Growth
Slows": the territory itself asserts control over prey that tries to leave. You
are not merely chasing someone through your territory — your territory helps
prevent your prey from escaping it. Abstraction: territorial retention.

## A2 — Myceliate

Fungal Assassin absorbs into nearby Mycelium and mushroom terrain and becomes
**temporarily intangible** while moving through the connected fungal structure.

This is the payoff for having developed a Network. Instead of *place fungus
because fungus gives +movement speed*, the class **physically inhabits the
organism it has cultivated**. Myceliate functions as approach, pursuit,
repositioning or escape depending on Network geometry, and its usefulness is
directly determined by previous Development: a poorly developed area offers
little useful fungal topology, while a mature Network becomes an assassin's
private movement environment.

Abstraction: **inhabit developed territory.**

### A2 branches

**Hardy** — Myceliating restores Health up to a cap. Rewards retreating and
repositioning through the fungal system and makes established territory safer for
repeated hunting, without turning the Network into generic passive regeneration:
the assassin has to use their fungal traversal. Abstraction: fungal refuge.

**Nutrient Cycle** — On emerging from Myceliate, the nearest appropriate small
mushroom grows immediately into a Huge Mushroom. Traversal therefore contributes
physical Development back into the Network. Combined with Creeping Colony's
guaranteed mushroom drops inside Networks, this gives the fungal ecosystem a
resource and productivity dimension rather than making Mycelium exclusively
combat terrain. Abstraction: traversal as maturation.

**Hyphae Lashers** — Creatures passed through while Myceliating are **Rooted**.
The movement path itself becomes control interaction, making Network geometry
matter considerably more: an experienced player chooses a subterranean route that
*intersects prey* rather than the shortest route to an endpoint. Abstraction:
predation through the organism.

## Ultimate — Destroying Angel's Blessing

Fungal Assassin plants a singular **Destroying Angel** mushroom. It must become
**Primed** before it becomes dangerous.

Recovered priming rules: it becomes Primed after maturation, **or** immediately
when incorporated into a sufficiently developed Mycelium Network. That creates a
strong Development shortcut — an undeveloped location must wait for the organism
to mature, while an established fungal ecosystem can make it dangerous
immediately.

A Primed Destroying Angel can then be destroyed or triggered. When triggered,
nearby small mushrooms erupt into Huge Mushrooms, enemies in the area suffer
massive AoE damage, and survivors receive Fungal Growth.

**Destroying it before it becomes Primed safely prevents the bloom.** The
ultimate is therefore not an assassin bomb disguised as a mushroom: its threat is
contingent on ecological maturity, and opponents can interact with it before that
threshold.

Abstraction: **Development reaches catastrophic maturity.** The ultimate
compresses the whole class into one event — Network development → mushroom
maturation → violent bloom → Fungal Growth → more fungal combat and development.

## Class abstraction

Development and Combat are not two halves of the kit. They are a feedback loop.

| Mechanic | Development / Combat relationship |
|---|---|
| Creeping Colony | Fungal terrain becomes productive territory |
| Fungal Growth | Fighting cultivates territory |
| Bursting Spore | Infected enemies become expansion vectors |
| Myceliate | Developed territory becomes assassin mobility |
| Nutrient Cycle | Using mobility further develops territory |
| Root Network | Territory controls prey |
| Destroying Angel | Mature Development becomes lethal Combat |
| Ultimate survivors | Combat creates new infection |

> The assassin grows the hunting ground, and the hunting grows the hunting
> ground.

**Why 3/5.** Individual inputs are not especially difficult. But unlike Chef,
Fungal Assassin can absolutely **press the correct ability in the wrong
ecological state** — Network location, connectivity, maturity, prey position and
future territorial usefulness all matter. A novice can infect and kill people; a
strong Fungal Assassin thinks several encounters ahead about where the fungus
should exist when the next fight occurs.

## Feasibility assessed 29 September 2026

[TECHNICAL RISK] **Myceliate's intangibility is the class's largest piece, and
harder than it appears.** Bukkit cannot give a player noclip. Spectator mode can,
but it also grants flight and see-through-walls scouting, which is an exploit
rather than an ability. The workable route is **per-tick teleportation along the
network path**, which bypasses collision without noclip, plus suffocation
suppression while inside blocks. `TunnelingAbility` is the existing precedent for
sustained forced movement — and it is untested, so it is a precedent rather than
a proof.

**Mycelium Networks should be expressed as a Zone, not as a new system.**
`infrastructure.md` is canonical for recognition, persistence and spatial
metrics, and its vocabulary already carries Zone as one of its four types.
Expressing Networks in that vocabulary inherits connectivity, persistence and
reset; inventing a parallel one inherits none of them and creates a
reconciliation problem later. `Provenance` supplies per-block ownership for
Mycelium the class places.

**The Development half runs into anti-farming.** Faster mushroom and crop growth
*plus* guaranteed Huge Mushroom drops inside a Network is, mechanically, a
renewable farm. The project has explicit anti-farming machinery and restricted
material categories; Creeping Colony must register with them rather than route
around them.

[TECHNICAL RISK] **Submersive's plant does not exist as written.** Twisting vines
cannot be waterlogged or placed in water — their only block state is `age`. Kelp
and sea pickle are the vanilla aquatic analogues. The branch is sound; the
species is not.

**Cheap once their prerequisites exist.** Fungal Growth is a status of the same
shape as Observed and Root. Hyphae Lashers reuses Root directly. Nutrient Cycle
is a bone-meal application with a space check. The Destroying Angel is an
ordinary mushroom block with a `Provenance` marker plus a Primed flag, and
"destroy it before priming" is its own counterplay with no extra machinery.

**Resolve Airborne Spread's ambiguity before implementation**, not after: burst
width is an entity-query radius and mycelium-spread width is a block-placement
volume. They are different code, not different constants.

## Open

- Every magnitude: Fungal Growth duration and damage, Network growth rates and thresholds, Burst radius, Myceliate range and duration, Hardy's cap, ultimate damage and radius.
- Whether Airborne Spread widens the Burst or the resulting spread.
- Submersive's aquatic species, given twisting vines cannot be waterlogged.
- Root Network's pull distance, strength and whether it is resistible.
- What "sufficiently developed" means for immediate priming.
- Growth progression.

---

# 24. Looming Talismaniac

**Documented 29 September 2026.** Previously carried a difficulty rating with no
canonical kit. Reproduced as supplied. **This class is less finished than the
short roster summaries implied**, and the unresolved pieces are recorded as
[OPEN] rather than invented.

Archetypes: **Production + Construction**
Difficulty: **4/5**

Core fantasy: manufacture symbolic objects whose shared designs establish
magical relationships between physically separated pieces of the world.

Talismaniac turns **banner designs into network identity**. Where Toolbox
programs through spatial inventory components, Talismaniac programs through
repeated symbols distributed through constructed space.

> Pattern = relationship. Banner = node. Matching pattern = network. Consuming
> the network = spell.

That grammar is much more settled than several individual abilities.

## Passive — Pattern Linking

Individual banner **patterns** establish links. Banners containing the same
pattern belong to the same corresponding magical network. Because a banner can
contain multiple patterns, **a single banner can participate in multiple
overlapping networks simultaneously**.

```
Banner A: patterns X + Y
Banner B: pattern X
Banner C: pattern Y
Banner D: pattern Z
```

A is simultaneously part of the X network and the Y network. This is where
Talismaniac gets much of its difficulty.

### Proc and propagation rule

**Settled.** Proccing a banner activates **every pattern on the initiating
banner**. Those activated patterns propagate to every banner containing any of
those patterns. Every banner reached this way is **consumed entirely**.

**Patterns found on reached banners do not recursively activate.**

```
A = X + Y
B = X + Z
C = Z

Proccing A activates X and Y.
B is reached through X and is consumed.
B's Z does NOT propagate onward to C.
```

That non-recursion rule is important: without it, overlapping designs could
unexpectedly detonate enormous chains through incidental pattern overlap.

Abstraction: **symbolic network topology.**

## A1 — Imprint

Imprint manipulates **complete banner designs**. Its base operation is cut and
paste: take the complete visible design from a banner, store that design, leave
the source blank, and apply or add the stored design to another banner.

This gives Talismaniac a direct tool for constructing and reorganizing Pattern
Linking networks without rebuilding every banner manually. **The important unit
is the complete design**, rather than individually editing layers through the
ability.

Abstraction: **manufacture and replication of symbolic infrastructure.**

### Recovered Imprint upgrades

Two established directions rather than a completed three-branch set.

**Copy** — Imprint preserves the source while acquiring its design. Instead of
*source loses design → Talismaniac stores it*, the operation becomes genuine
duplication. Abstraction: replication.

**Cut / Discharge** — The stored design can be triggered locally without ordinary
network propagation. Recovered wording varies between Cut/Proc and
Cut/Discharge; the established mechanical distinction is that the stored design's
effects can be activated **locally** rather than sending the normal Pattern
Linking activation through its distributed banner network. Abstraction: portable
talisman use.

[OPEN] **A finalized third Imprint branch was not recovered and is not
invented here.**

## A2 — current established core

**This is where the class becomes less complete.**

When the relevant banner or network is consumed, the block supporting the banner
**and a bounded contiguous region of matching material** become temporarily
cursed and reactive. Entities touching that affected construction receive
**Weakness**.

So a banner is not merely projecting a circular aura. Its effect enters the
physical construction underneath it: **banner → supporting block → contiguous
matching construction**.

That distinction is essential to the Construction half. The same banner placed on
a tiny stone pedestal, a long stone wall, a stone floor, or a deliberately
branching stone structure creates **materially different spell geometry**.

Abstraction: **enchant constructed material.**

[OPEN] Exact bounds and contiguity limits were never finalized. See the
feasibility note — this is a performance gate, not only a balance number.

### A2 branches

**Infestation** — The affected construction becomes fragile and introduces
silverfish. The curse changes a structure from stable architecture into something
infested and treacherous. Abstraction: corrupt construction from within.

**Explosive** — The affected construction becomes a delayed explosive trap. Again
the significant feature is that the spell **follows the prepared construction**
rather than creating a generic circular explosion around the banner.
Abstraction: construction as stored trap geometry.

[OPEN] **The third A2 branch is unresolved.** Current set: base
contact-reactive cursed construction → Weakness; Infestation for
fragility/silverfish; Explosive for delayed detonation. Third specialization not
yet designed.

## Ultimate — Talisman of Undying

A prepared banner/talisman network temporarily gives players within its resolved
affected locations an **extra life** — Totem-of-Undying-like protection.

This makes the ultimate the **positive mirror of A2**: A2 turns prepared symbolic
Construction into hostile cursed space; Talisman of Undying turns it into
life-preserving space.

[OPEN] Several mechanics were deliberately not finalized, and they **radically
change the ultimate's economy**, so they are not papered over:

- whether protection is a radius around each affected banner or follows blocks and constructed geometry;
- exactly which linked banners participate;
- whether activation consumes the entire relevant network immediately;
- whether a banner is consumed per protected player or per resurrection proc;
- whether each affected location has its own expenditure;
- exact propagation semantics when the ultimate uses overlapping Pattern Linking networks;
- what happens when a protected player dies while the network protecting them has already been consumed by A2.

A network consumed once to give five players temporary death protection is a very
different ability from one where individual resurrection events consume prepared
talismans.

**What is settled is the abstraction**: Talismaniac prepares physical symbolic
infrastructure ahead of time and later sacrifices that infrastructure to make
death temporarily fail within the prepared space.

## Class abstraction

**Production.** Talismaniac manufactures **encoded objects**. The value is not
the wool or the banner as raw material — it comes from its pattern, duplication
and manipulation of that pattern, which other produced banners share it, and what
effect that symbolic identity can invoke.

**Construction.** Those objects become meaningful because of **where and on what
they are installed**. A banner can establish a geographically distributed
network, bridge multiple pattern identities, attach an effect to constructed
material, and turn a deliberately shaped structure into spell geometry.

The loop is **design → reproduce → place → link → prepare → consume**, which is
considerably more strategically demanding than Chef's input → process → output.

**Why 4/5.** Mechanical execution is probably around 2/5 — placing banners and
activating abilities is not difficult. The **technical and planning** difficulty
is enormous: complete designs, individual patterns within them, which banners
share each pattern, banners in multiple networks, which networks an initiating
banner activates, the non-recursion rule, which physical structures banners are
attached to, contiguous-material geometry, geographical placement, and whether
consuming a network now destroys infrastructure needed later.

The characteristic Talismaniac mistake is not *I missed my skillshot*. It is **I
built the wrong relationships fifteen minutes ago.**

## Feasibility assessed 29 September 2026

**The core conceit is directly supported.** Banner patterns are fully readable
from the block state, including multi-layer designs, and settable — so Pattern
Linking and Imprint's cut-and-paste both work against the real API rather than
needing a parallel representation. This was the least certain part of the class
before checking, and it holds.

**The one genuinely new data structure is a global pattern index.** "Every banner
containing any of those patterns" is a query **by attribute, across the whole
world, persistently** — not by position. `Provenance` indexes by position and
cannot serve it. The index must be maintained on every banner place, break and
Imprint, and it is the piece most likely to drift out of sync with the world.
Design it deliberately rather than growing it.

The **non-recursion rule is also a performance property**, not only a design one:
one hop is bounded, while recursive activation over overlapping patterns is not.

[TECHNICAL RISK] **A2's contiguity limit is a performance gate.** An uncapped
flood fill by matching material, from a banner on a large stone structure, walks
thousands of blocks — and a map's natural stone is contiguous with very nearly
everything. That `[OPEN]` needs a number before A2 can be built at all, and
probably needs **player-placed material only** as well, or a banner set on
stone adjacent to the terrain curses half the map.

[TECHNICAL RISK] **Infestation is stone-only in vanilla.** Infested blocks exist
for stone, cobblestone, stone bricks and deepslate and nothing else. A banner on
an oak or copper structure cannot use them, so the branch either restricts itself
to stone construction or spawns silverfish manually without the infested-block
fiction. "Fragile" and "infested" arrive as one package in vanilla and as two
separate jobs if hand-built.

[TECHNICAL RISK] **Talisman of Undying cannot use the totem.** The vanilla effect
only fires from a totem held in hand. The extra life must be built manually —
intercept the fatal damage, cancel it, set health, apply the effects. Entirely
doable, but it means "Totem-like" is a **look, not a mechanism**, and every
interaction is authored rather than inherited.

**Cheap.** Imprint itself, Copy, Weakness application on contact, and Explosive's
delayed detonation are all straightforward once the index and the contiguity cap
exist.

## Open

- The third Imprint branch and the third A2 branch.
- All seven ultimate economy questions listed above.
- A2's contiguity bound, and whether it is restricted to player-placed material.
- Whether Infestation restricts itself to stone-family construction.
- Every magnitude: Weakness duration, curse duration, explosion delay and yield, ultimate duration and protection strength.
- Growth progression.

---

# 25. Paver

**Proposed 29 September 2026.** The Construction exemplar of the seven
Difficulty-1 archetype classes ([section 21](#21-class-difficulty)). Reproduced
as supplied, with the feasibility assessment kept separate below.

Primary archetype: **Construction**
Difficulty: **1/5**

Core fantasy: gather and compile Concrete, then use Minecraft's ordinary
block-placement freedom to construct terrain that is tougher, more traversable,
structurally unusual and combat-capable.

Fundamental question: **where would adding blocks make this place more useful?**

Core loop: **Gather → Compile → Construct → Exploit the construction → Reuse its
mass.**

Paver is the pure Construction exemplar. **The kit does not decide what Paver
should construct.** It makes construction itself more capable while preserving
the spatial decisions inherent to Minecraft building.

## Passive — Concrete Mixer

While carrying the ingredients required for Concrete Powder, Paver's movement
gradually converts those ingredients directly into Concrete. Conversion is
slightly faster when Paver is moving through water, or has a Water Bucket in
their inventory.

Paver therefore still participates in the material economy: Concrete has to come
from its actual ingredients. **The passive removes processing friction rather
than removing acquisition.** Water provides a simple optimization grounded in the
material's normal Minecraft behaviour.

Abstraction: **raw construction materials → construction-ready material.**

## A1 — Rebar Chain

**Passive.** Concrete placed by Paver is rebar-reinforced, slightly increasing
its hardness. Reinforcement marks Concrete as Paver's authored structural
material and is referenced by the rest of the kit.

**Active.** Paver attaches their Rebar Chain to a nearby Concrete block. While
attached, Paver gains climbing movement along other Concrete blocks within a
radius around the attachment point. Moving outside that radius breaks the Chain.

**The ability does not create climbing geometry.** Paver creates the geometry
through ordinary block placement, and Rebar Chain changes how Paver can traverse
it. A wall can be climbed; an overhang can become a route; a bridge can provide
recovery; a deliberately complex structure becomes correspondingly complex
movement terrain.

Abstraction: **construction as traversal.**

### A1 branches

**Galvanized** — Rebar-reinforced Concrete is even harder. The simplest
structural specialization: Paver's authored construction becomes more difficult
to destroy. Abstraction: durability.

**Beveled Edges** — Paver's movement speed is increased while attached with Rebar
Chain. Paver traverses their Concrete construction more rapidly without changing
what they need to build. Abstraction: traversal efficiency.

**Pitons** — Rebar Chain can be recast while attached to leave a **Piton**, a new
attachment point. Subsequent casts dash Paver to the furthest Piton. Pitons
establish persistent traversal anchors throughout the construction. Abstraction:
traversal topology.

[OPEN] **"Furthest Piton" must be made explicit** during implementation: distance
from Paver, distance from the original anchor, or position within an ordered
chain are three different abilities.

## A2 — Chamfering

**Passive.** Concrete Powder placed adjacent to Paver's rebar-reinforced Concrete
becomes **Chamfered**. Chamfered Concrete Powder **does not fall when it receives
block updates**.

This lets Paver's solid Concrete function as the structural skeleton for
otherwise impossible Concrete Powder geometry. **The player remains completely
responsible for deciding that geometry.**

**Active.** The next Chamfered Concrete Powder block Paver steps on loses its
Chamfered state, and adjacent Chamfered Concrete Powder loses Chamfering too.
Once released, those blocks again obey normal gravity.

The adjacency rule lets Paver **design how a collapse will propagate** through
their construction rather than selecting a predefined collapsing structure.

Abstraction: **conditional structural support.**

### A2 branches

**Steep Grading** — Players sneaking at the edges of Chamfered Concrete Powder
fall instead. Chamfered Powder defeats the normal edge protection sneaking
provides. **This applies to players generally, not only enemies**: it is a
physical property of the construction, not team-aware trap magic. It lets Paver
construct dangerous footing while leaving shape, location and intended use
entirely player-authored. Abstraction: precarious construction.

**Pozzolan Patch** — When Chamfered Concrete Powder becomes Concrete, adjacent
enemies are **Rooted**. The effect follows the actual material transition rather
than requiring a separate Paver action, giving Chamfered Powder two opposing
combat possibilities: release its support and it falls, or harden it and nearby
enemies are Rooted. Abstraction: hardening as control.

**Natural Mix** — Concrete Powder placed adjacent to **Grass Blocks** can also
become Chamfered, making grass an alternative structural starting point. Base
Paver construction follows terrain → reinforced Concrete → Chamfered Powder;
Natural Mix permits Grass → Chamfered Powder. It expands **where** Paver can
begin expressive Powder construction without prescribing what that construction
becomes. Abstraction: terrain integration.

## Ultimate — Concrete Shoes

Paver begins summoning a pair of massive Concrete Shoes over a targeted location.
The Shoes gather mass from two sources **in priority order**:

**1. Inventory Concrete** is consumed first and incorporated. This material is
**permanently committed**.

**2. Nearby reinforced Concrete**, if more is required, is **temporarily pulled**
from its positions and incorporated. These world blocks are only borrowed, and
**their original positions are reserved for them throughout the cast** — see
[section 27](#27-global-world-system-rules).

**Slam.** Once the Shoes have gathered their maximum possible mass, or all usable
Concrete is exhausted, they slam onto the target. The assembled amount scales
both effects: players caught directly beneath take increasing damage, and
grounded players affected receive increasing **upward** knockback.

> More Concrete → harder crush + greater launch.

**It does not fail for want of Concrete.** Whatever usable mass is available
determines the resulting strength.

**After impact** the two sources resolve differently. World-sourced Concrete
returns to its exact reserved original positions. Inventory-sourced Concrete does
**not** return to Paver's inventory: it remains permanently placed at the impact
location as the resulting Shoes.

> Carried Concrete is committed. Built Concrete is leveraged.

The resulting Shoes are **actual terrain** — climbable, buildable upon, usable as
cover, and subject to Paver's other Concrete mechanics where applicable.

Abstraction: **construction mass as combat force and new construction.**

## Paver's Construction grammar

Two related but mechanically distinct materials.

**Rebar-reinforced Concrete** — permanently player-placed; slightly harder than
ordinary Concrete; supports Rebar Chain traversal; supports Chamfered Concrete
Powder; can temporarily contribute mass to Concrete Shoes.

**Chamfered Concrete Powder** — freely player-arranged; supported through
adjacency rather than ordinary gravity; can deliberately have that support
released; retains the normal possibility of becoming Concrete; branches can make
its footing dangerous, its hardening controlling, or its support relationship
more terrain-flexible.

**Neither material dictates what structure Paver creates.**

## Why Paver remains Difficulty 1

| Situation | Paver's rule |
|---|---|
| Need Concrete | Gather ingredients and move |
| Want durable construction | Place Concrete |
| Want to traverse Concrete | Rebar Chain it |
| Want suspended Powder | Build it from reinforced Concrete |
| Want Powder to fall | Step on it with Chamfering active |
| Want a major fight impact | Concrete Shoes |

What remains difficult is **Construction itself** — what to build, where, how much
material to commit, how geometry interacts with terrain, how allies use it, how
enemies attack it, how to construct during combat, how to exploit gravity, and
whether Concrete is worth more in inventory, already constructed, or permanently
relocated by the ultimate.

**Those are not reasons to raise the rating. They are the skill expression of the
archetype.** See the first principle in
[section 27](#27-global-world-system-rules): low class difficulty must never come
at the expense of performing the underlying Minecraft verb well.

## Feasibility assessed 29 September 2026

**Free.** Concrete Mixer is an inventory scan on movement with a water check.
Pitons, Beveled Edges and the Shoes' mass-scales-potency rule are all
straightforward. Concrete and Concrete Powder are already distinguished in
`MaterialCategories` (`MaterialCategories.java:58` and `:69`), so the two
materials already have separate identities in the economy.

[TECHNICAL RISK] **"Slightly increasing its hardness" is not available.** Hardness
is a property of the block *type*, not the instance, and Bukkit exposes no
setter — the same constraint Bloodmason's Adipose hit
([section 20](#20-bloodmason)). Concrete cannot be substituted for a harder block
because its colour is the point. The workaround is detect-and-correct: apply
Mining Fatigue while a player targets reinforced Concrete, or track break
progress across `BlockDamageEvent`. This project has already accepted that
pattern for Structural Integrity, but it is a visible seam and **Galvanized
doubles down on it**, so the two branches share one risk.

[TECHNICAL RISK] **Climbing arbitrary blocks is not available either.** Vanilla
climbing belongs to ladders, vines and scaffolding; there is no climbable
attribute and no per-block override. Rebar Chain has to be per-tick upward
velocity while the player is against reinforced Concrete — which is **not
client-predicted**, so expect the same fighting-the-client behaviour described
for Bloodmason's A1. It is the class's largest movement risk and it sits on A1,
the most-used ability.

**Chamfering itself is cheap and the right shape.** Cancelling
`BlockPhysicsEvent` at the chamfered positions suppresses the gravity check
outright. **But `BlockPhysicsEvent` is one of the highest-frequency events on the
server**, so the listener must hit an in-memory position set before it touches
`Provenance` or a PDC. There is no `BlockPhysicsEvent` listener in the plugin
today, so this introduces one on a hot path.

**The release needs a cap, for the Talismaniac reason.** "Adjacent Chamfered
Powder also loses Chamfering" is an uncapped flood fill over a player-built
structure. A number is needed before A2 ships, not as balance but as a
performance bound.

**Steep Grading — answering the implementation question directly.** Yes, this can
be done cheaply, and without ticking or scanning Chamfered blocks. Sneak edge
protection manifests as the server clamping movement at the edge, so rather than
detecting the protection, **drive it from `PlayerMoveEvent`**, which fires only
when the player moves: if the player is sneaking, standing on Chamfered Powder,
and pressing toward an unsupported edge, apply the nudge that carries them over
it. The directional half is already solved — `org.bukkit.Input` (used today in
`AimState.java`) carries the movement keys, so "pressing toward" is readable
rather than inferred. Cost is one listener on an event that is already
player-driven, and zero cost for chamfered blocks nobody is standing on.

[OPEN] **Pozzolan Patch's trigger needs verification.** Concrete Powder becoming
Concrete on water contact may not fire an event a plugin can hear. If it does
not, the alternative is polling chamfered powder positions for the transition,
which for a large Paver structure is exactly the per-block ticking Steep Grading
was designed to avoid. **Confirm the event before designing around it** — this is
the one branch whose cost could change by an order of magnitude.

**Concrete Shoes needs the displacement registry** from
[section 27](#27-global-world-system-rules), which does not exist yet. Paver is
its first consumer but should not be its owner: Mole's Sinkhole and Bloodmason's
Anatomb are the others.

## Open

- Every magnitude: conversion rate and water bonus, Chain radius, climb speed, hardness deltas, Shoes' mass-to-damage and mass-to-knockback curves, Root duration.
- The definition of "furthest Piton".
- The adjacency cap on Chamfering release.
- Whether the Powder → Concrete transition is observable without polling.
- Growth progression.

---

# 26. Chauffeur

**Proposed 29 September 2026.** The Logistics exemplar of the seven Difficulty-1
archetype classes ([section 21](#21-class-difficulty)). **Supersedes the
placeholder name Stationmaster**, which was a name and a question with no kit.
Reproduced as supplied, with the feasibility assessment kept separate below.

Primary archetype: **Logistics**
Difficulty: **1/5**

Core fantasy: Chauffeur uses Minecraft's most legible driven vehicles
exceptionally well, bending some of their normal movement rules to move
themselves, teammates and resources through the world. **Summoned vehicles are
powerful but brief opportunities separated by meaningful downtime**; ordinary
vehicles and transportation infrastructure remain important.

Fundamental question: **what needs to move, and what can I drive to move it?**

Core loop: **Find or summon vehicle → choose what/whom to move → drive → park or
transition → operate through downtime → drive again.**

Chauffeur does **not** formalize deliveries or choose destinations. Sometimes
they are carrying resources home, sometimes moving a teammate, sometimes reaching
a location themselves is the logistical objective, sometimes they are positioning
a vehicle to support future infrastructure. **The class improves Chauffeur's
methodology for moving things, not the act of deciding what should move where.**

## Passive — Parking Spot

Chauffeur can have **one** Parking Spot at a time. Exiting a summoned Cargo Boat
or Muscle Cart places it at Chauffeur's current location, replacing the previous
one. Entering a summoned vehicle from the Parking Spot grants that vehicle a
brief speed boost.

The Parking Spot records **where did I last finish a summoned vehicle trip?** It
is not a selected waypoint, depot, Recall point or generated route. Its
strategic significance comes entirely from where Chauffeur actually chooses to
stop and exit — creating reasons to finish a trip somewhere that matters later:
an important developed position, an objective approach, a distant extraction
area, a defensible fallback, an aggressive forward position, a useful
vehicle-transfer location.

It is also the destination used by the ultimate.

## A1 — Cargo Boat

**Passive.** Chauffeur can drive Boats on land at increased speed. Boats can
already travel over land; Chauffeur makes that normally awkward behaviour
genuinely useful for transportation.

Cargo Boat should have **the more forgiving and versatile free-driving handling**
of Chauffeur's two summoned vehicles — correcting course readily and negotiating
improvised routes — while retaining the higher technical movement ceiling
associated with Minecraft boating under exceptional surfaces and conditions.

**Active.** Chauffeur temporarily summons a Cargo Boat, which retains the Boat's
extra passenger seat, **also** contains additional item storage, and can
therefore simultaneously move Chauffeur + passenger + cargo.

Its summon availability should be **relatively short** compared with its
relatively long independent cooldown. One activation should create an opportunity
for a meaningful logistical leg, not put Chauffeur permanently into a vehicle
state.

### A1 branches

**Jetski** — Cargo Boats gain increased **velocity** from Bubble Columns, making
them acceleration and launch features Chauffeur can incorporate into a route. The
important property is *gaining velocity*, not a flat movement-speed bonus while
inside the column.

**Freight Engine** — Cargo Boats move faster when their storage is full. Keep it
a **clear threshold** rather than a continuous fullness optimization meter: full
cargo → faster Cargo Boat.

**Front Propeller** — Cargo Boats deal additional damage and knockback when
striking enemies **with their front**. The directional requirement matters:
Chauffeur must line the Boat up and ram, not receive generic collision damage.

## A2 — Muscle Cart

**Passive.** Chauffeur can ride Minecart Variants, and **all rails Chauffeur
travels over in a Minecart are treated as Powered Rails**. This makes the entire
Minecart family part of Chauffeur's vocabulary rather than making ordinary
Minecarts obsolete once Muscle Cart exists — and Minecarts gain an additional
attraction relative to Boats because their variants retain their own specialized
characteristics.

**Active.** Chauffeur summons a Muscle Cart that can be freely driven across land
similarly to a Boat, with **distinctly more momentum-oriented handling**: greater
commitment to its current line, more deliberate turning, drift-like correction,
and rewards for maintaining momentum. It should remain a step below the extreme
technical handling of very high-speed ice boating.

Driving Muscle Cart into enemies **knocks them out of the way**. Its identity is
therefore **establish a line → build and carry momentum → force the line through
obstacles**, and rails provide an especially favourable prepared line because
Chauffeur treats them as Powered.

### A2 branches

**Pop the Hopper** — Striking an enemy with a Hopper Minecart sends them
extremely high. This is primarily **extreme vertical displacement**, not another
damage modifier, and it gives an existing variant a Chauffeur-specific driving
characteristic without summoning that variant for them.

**Diesel Engine** — An active Furnace Minecart gains speed based on the fuel
being used, preserving the actual Minecraft resource relationship rather than
introducing an abstract Chauffeur fuel meter. Fuel choice becomes part of
preparing a trip.

**Tactical Dismount** — Exiting a Minecart or Muscle Cart lets it continue driving
a short distance **without Chauffeur**, dealing bonus damage to enemies it
strikes; TNT Minecarts explode at the end of that movement. Dismounting becomes an
offensive continuation of the trajectory Chauffeur established: line it up →
dismount → unmanned continuation → explosion at endpoint. **The bonus damage
belongs to the unmanned continuation**, not to Chauffeur's Minecart collisions
generally.

## Ultimate — Highly Irresponsible Racing Line

Chauffeur draws a visible straight line from their current position to their
Parking Spot. Enemies within a **substantially wider corridor** around that line
become marked. While Chauffeur is driving a vehicle *toward* the Parking Spot
they gain bonus movement speed, and striking a marked enemy with the vehicle
grants additional bonus speed.

The narrow visible line communicates the **idealized** Racing Line; the wider
marking corridor allows Chauffeur to actually drive — steer around terrain,
drift, deviate to strike a marked enemy, correct course, and exploit the
vehicle's particular handling.

**The ultimate does not create a navigable path or ignore terrain.** Its
proposition is: *that is where you parked; get back there; anything between you
and it can become part of the racing line.*

This supports both directions. **Defensive:** leave the Parking Spot at a priority
position, venture elsewhere, then use the ultimate to aggressively recover it
through an enemy line. **Offensive:** establish the Parking Spot at an advanced
location and later break back through opposition toward it.

Marked-enemy collisions should be **discrete acceleration events**, not continuous
acceleration from maintaining hitbox contact.

## Vehicle handling doctrine

**Cargo Boat and Muscle Cart should overlap substantially on neutral terrain.
That is intentional.** The game should frequently permit *either vehicle works
here; which do I prefer driving?* Their distinction is primarily
**methodological** rather than a hard terrain counter system.

| | Cargo Boat | Muscle Cart / Minecarts |
|---|---|---|
| Neutral terrain | Good | Good |
| Free driving | More versatile, forgiving | More momentum-oriented |
| Water | Naturally excellent | Poorer context |
| Rails | No special benefit | Extremely favourable |
| Cargo | Excellent | Variant-dependent |
| Passenger + cargo | Yes | No equivalent baseline |
| Collision | Normal unless branched | Naturally clears enemies |
| Specialization | Highly capable single platform | Family of specialized variants |
| Technical ceiling | Higher free-movement ceiling | Line, momentum and infrastructure mastery |

This produces **preference without making preference absolute**. A Boat-preferring
Chauffeur puts Cargo Boat on cooldown more often, so Muscle Cart becomes
available for problems arising during that cooldown — alternation without an
explicit combo mechanic.

## Summon cadence and downtime

Cargo Boat and Muscle Cart have **independent cooldowns**, both with relatively
short exceptional availability and relatively long individual cooldowns. **The
target is emphatically not permanent vehicle uptime through alternating A1 and
A2**, and real periods where both are unavailable are desirable.

During those periods Chauffeur remains fully functional: travel normally,
transport items normally, use ordinary Boats, use ordinary Minecarts and
variants, exploit the passive vehicle bonuses, reposition vehicles, prepare
cargo, construct and use transportation infrastructure, and decide what the next
summoned trip should accomplish.

**The cooldown gates Chauffeur's exceptional vehicle opportunity, not the
Logistics archetype itself.** The eventual calibration question is therefore *how
many meaningful map connections can one summon solve?* rather than *what
percentage uptime does Chauffeur have?* One summon should generally solve a leg,
not an entire extended logistical operation.

## Vehicles and Supply Lines

Preserves the Logistics rule established with Skeleton Crew, and generalized in
[section 27](#27-global-world-system-rules): **if an entity-based logistical
method defines a Supply Line, that specific entity becomes tied to that Supply
Line.** The entity is not evidence that a Supply Line was established — the entity
is *part of* the Supply Line.

**Ordinary vehicles.** Chauffeur can use ordinary Boats, Minecarts and applicable
variants to establish Supply Lines, retaining Chauffeur's passive advantages
where applicable. A specific ordinary vehicle defining a Supply Line becomes
committed to it, which makes mundane vehicles valuable as **persistent logistical
capital** even though Chauffeur has exceptional summoned versions.

**Summoned vehicles.** Cargo Boat and Muscle Cart can likewise define Supply
Lines, and that particular summoned entity becomes committed. **Resummoning
replaces the previous instance and therefore breaks the Supply Line it was
defining.** Consequently *"Cargo Boat is available again" does not necessarily
mean "I should summon Cargo Boat"*: if the existing one is maintaining important
infrastructure, redeploying A1 has a logistical opportunity cost.

The useful distinction is: an **ordinary vehicle** is a persistent logistical
instrument that must actually be acquired; a **summoned vehicle** is an
immediately available exceptional instrument that can be redeployed, but
redeployment sacrifices anything dependent on its previous instance.

Tactical Dismount follows the same physical rule: if moving, destroying or
exploding a vehicle means it no longer satisfies its Supply Line's requirements,
**the Supply Line does not magically persist**.

## Feasibility assessed 29 September 2026

[TECHNICAL RISK] **Boat movement is client-authoritative, and the class's A1
passive depends on changing it.** The driving client simulates the boat and sends
its position; the server is largely a follower. Server-side velocity on a
player-driven boat is the least reliable movement work in Minecraft, and
"increased speed on land" is exactly that. This is the single largest risk in the
kit and it sits on the passive half of A1, so it affects every Chauffeur at all
times. **Jetski compounds it**, since bubble-column acceleration is the same
argument applied to the same entity.

**Minecarts are the opposite, and that is fortunate.** Minecart motion is
server-driven, so "every rail is Powered", free-driving Muscle Cart handling,
momentum and drift, enemy knock-aside, Pop the Hopper and Tactical Dismount's
riderless continuation are all reachable by applying velocity to an entity the
server already owns. `TNTMinecart` can be detonated directly. **Muscle Cart is
substantially more implementable than Cargo Boat**, which is worth knowing
before the two are balanced against each other as equals.

[TECHNICAL RISK] **Cargo Boat cannot be one vanilla entity.** A chest boat
carries storage but **loses the second seat**; an ordinary boat seats two but has
no storage. "Passenger + cargo simultaneously" is specified against a combination
vanilla does not offer. The cheap fix is an ordinary boat with a plugin-held
inventory opened on interact, which keeps both seats and gives storage that can
be sized deliberately rather than inherited.

[OPEN] **Diesel Engine's fuel table has to be authored.** Vanilla furnace
minecarts accept only coal and charcoal, so "speed based on the fuel being used"
has exactly two values unless a broader table is written. That is a small piece of
work but it is invention, not preservation, so it should be an explicit decision
rather than a surprise.

**Cheap.** Parking Spot is a single stored location. The Racing Line is a
distance-to-segment test for the corridor, particles for the line, and a
dot-product test for "toward" — all ordinary. Discrete rather than continuous
acceleration on marked-enemy collisions is a cooldown per enemy, and is the
cheaper of the two readings as well as the better one.

[TECHNICAL RISK] **Supply Lines do not exist yet.** They appear in `GrowthPacket`
and `TestBed` as commentary and endpoint fixtures, with no implementation. The
entity-commitment rule is therefore not a constraint on an existing system but a
requirement on one not yet built — and Chauffeur is the class that makes it
urgent, because summoned-vehicle redeployment is the mechanic that makes
commitment *cost* something.

## Open

- Every magnitude: summon durations, both cooldowns, speed deltas, corridor width, knockback and damage values, Parking Spot boost.
- Whether Cargo Boat's storage is a chest-boat-sized inventory or authored separately.
- The Diesel Engine fuel table.
- Muscle Cart's handling model: how much momentum commitment, and how drift correction is expressed.
- Growth progression.

---

# 27. Global world-system rules

**Recorded 29 September 2026.** Two rules and one design principle set that
emerged from Paver and Chauffeur but are **not class-specific**. They are
recorded here so they travel independently of the classes that produced them.

## Temporary block displacement reserves its origin

> Whenever an ability temporarily consumes, removes, relocates, hides or
> otherwise takes a block out of its original world position **with the
> intention of restoring it**, that original position remains **reserved** for
> the displaced block until restoration.

A reserved position cannot be built into or otherwise occupied by another block
in a way that would prevent the displaced block from returning. When the
temporary displacement ends, the original block returns to its exact original
position and state.

The distinction is: **temporary displacement reserves the origin; permanent
displacement releases it as ordinary usable space.**

**Implemented 29 September 2026** as `Displacement.java`, with its bookkeeping
in `DisplacementLedger`. Shared from the start rather than owned by its first
caller: Paver's Concrete Shoes ([section 25](#25-paver)), Mole's Sinkhole
([section 4](#4-mole)) and Bloodmason's Athanor Anatomb
([section 20](#20-bloodmason)) all displace with intent to restore, and three
private versions would disagree about who owes what to which hole.

What it enforces, and what it deliberately does not:

- **A position may be reserved once.** A second ability is refused rather than
  overwriting, because overwriting loses the first block entirely — its payload
  is the only record that it existed, and its position is already empty.
- **Taking and restoring both suppress physics**, which is what makes "returns
  to its exact original state" true and makes restore order irrelevant.
- **Provenance travels with the block.** A borrowed player-placed block is
  still player-placed when it comes back; forgetting that would quietly convert
  built material into a resource opportunity.
- **Reservations are enforced against** placement, fluid flow, falling blocks,
  pistons, and the form/spread/grow family. Explosions have their block list
  filtered rather than being cancelled, since there is nothing at a reserved
  position to destroy — only debris to keep out of it.
- **A piston push is refused whole**, never trimmed. Bukkit cannot move some of
  a push's blocks and not others, and Minecraft cannot express a partial push.
- **Release is complete.** A token's positions are freed as they are handed
  back, so a caller that fails mid-restore cannot leave a position reserved
  against a block nobody is going to return. Losing a block is bad; a
  permanently unbuildable hole that outlives the ability is worse.

[TECHNICAL RISK] **Reservations live in memory**, unlike `Provenance`, which
persists in the chunk PDC. A server stopped mid-displacement loses the held
blocks and their reservations together. Survivable because displacement is
within-a-cast state and matches are ephemeral — but **do not build a
displacement meant to outlast a cast on this.**

## An entity-defined Supply Line is committed to that entity

> If an entity is the physical logistical method defining a Supply Line, **that
> specific entity is committed to the Supply Line.**

The entity is not *evidence* that a Supply Line was established. The entity is
*part of* the Supply Line. Moving, replacing, recalling, destroying, consuming or
otherwise invalidating the defining entity invalidates the Supply Line, at the
point where it no longer satisfies the line's requirements.

Established with Skeleton Crew ([section 15](#15-skeleton-crew)) and generalized
by Chauffeur ([section 26](#26-chauffeur)), where summoned-vehicle redeployment
is what gives commitment a cost.

Together these two rules express a broader principle:

> **Persistent game systems remain physically accountable to the world objects
> that create them.**

Paver's displaced block has somewhere it belongs. Chauffeur's Supply Line has a
vehicle supporting it. The abstraction does not silently detach itself from the
Minecraft world once established.

## Difficulty-1 design principles

Sharpened by Paver and Chauffeur together. These extend
[section 21](#21-class-difficulty) rather than restating it.

**1. Difficulty measures what the class adds.** A class does not earn a high
rating merely because its archetype is hard to perform well. Construction
inherently involves difficult spatial decisions; Logistics inherently involves
difficult routing, timing and allocation. Those are the archetypes. **Difficulty
measures the additional procedural, interpretive and mechanical burden the class
imposes on top of performing its role well.**

**2. Simplify access to the verb, not the verb.** A Difficulty-1 class must not
achieve simplicity by deleting the decisions that make its archetype meaningful.

| | Bad easy version | The exemplar |
|---|---|---|
| Construction | Press "Build Bridge", receive the correct bridge | **Paver**: here are unusually capable construction materials; you decide what to build |
| Logistics | Select origin and destination; resources transfer automatically | **Chauffeur**: here are unusually capable transportation instruments; you decide what, whom, where and how to move |

**3. Make the methodology clearer and easier to manipulate — not necessarily
easier to execute.** A simple class can make its archetype legible without making
success trivial. *"Boats and Minecarts are my tools"* is clear; driving the right
vehicle through difficult terrain, managing momentum, preserving a Supply Line and
hitting the Racing Line are not. *"Concrete is my construction material"* is
clear; good Minecraft construction is not automatic.

**4. Preserve the world's existing vocabulary.** Chef builds on food, furnaces and
cooking; Paver on Concrete, Concrete Powder, gravity and Grass; Chauffeur on
Boats, Minecarts, rails, variants, Bubble Columns and fuel. **A class may break
rules, but the broken rule should remain intelligible relative to the original
object.** "My Minecart treats every rail as Powered" is easier to understand than
an abstract transportation statistic, and it creates more Minecraft-specific
consequences.

**5. Enrich a method rather than replace it.** Chef is ordinary resource decisions
plus extraordinary processing interactions; Paver ordinary construction decisions
plus extraordinary material properties; Chauffeur ordinary logistical decisions
plus extraordinary transportation instruments. **The player remains responsible
for the archetype's central decision.**

**6. Difficulty 1 can have a high mastery ceiling.** A simple kit generates
mastery through terrain, world state, Minecraft mechanics, opponents, timing,
resource economy, physical execution, coordination, infrastructure, and
interactions between simple rules. Chauffeur is the clearest case: the beginner
understanding is genuinely *"Boat carries lots. Cart hits things and likes rails.
Where I park matters. Ult makes me drive back there really fast."* The expert
thinks about vehicle staging, Supply Line commitment, cooldown asymmetry, future
Parking Spot value, cargo preparation, variant acquisition, Bubble Column
approaches, fuel selection, rail interception, momentum and drifting, Tactical
Dismount trajectories, whether redeploying a summon is worth destroying existing
infrastructure, whether a Racing Line is physically traversable, and deliberately
using enemies as acceleration points. **Low interpretive floor and high systemic
ceiling coexist.**

**7. Preference is allowed to be strategically meaningful.** Not every pair of
options needs a deterministic correct context. Cargo Boat and Muscle Cart
deliberately overlap on neutral terrain, and a player may simply prefer one
handling model. That preference changes which ability spends more time on
cooldown, which creates new reasons to use the other: **preference → usage →
availability changes → reconsideration**, producing variety without hard counters
or forced rotations.

**8. Downtime creates decision space.** Constant access to a class's exceptional
method can *reduce* strategic thought. Chauffeur deliberately has intervals
without a summoned vehicle, so the player has time and reason to prepare,
reposition, use ordinary systems, preserve infrastructure and decide what the
next exceptional opportunity is worth. **A cooldown is not merely an
action-frequency limiter; it can make the world outside the ability matter
again.**
