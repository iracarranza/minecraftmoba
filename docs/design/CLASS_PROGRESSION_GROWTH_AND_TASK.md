# Class Progression — Kit, Growth, Task, and the Level Clock

**Status:** Working canon
**Date:** 2026-09-26
**Scope:** How classes mature across Lv0–30: the shared level clock, the Growth
system, the Task specialization system and its technique vocabularies, the
player-facing numerical scale, and Mole as the first fully authored curve.

This record supersedes the universal capacity/specialization skeleton in
[`classes.md`](../../classes.md#current-universal-progression-skeleton). Numbers
here are first calibrations, not final balance.

---

## 1. Where specialization comes from

Classes preserve Minecraft's ordinary verbs and become unusually good at
strategically meaningful versions of them. Specialization does **not** come from
generic multipliers — "you mine ore 20% better", "+20% damage because you are
Combat". It comes from four separated sources:

| Source | What it supplies | Chosen or authored |
| --- | --- | --- |
| **Kit** | the unusual verbs the class can perform | branches are chosen |
| **Growth** | how the class's operating envelope expands as it matures | authored, automatic |
| **Task** | which ordinary Minecraft activities the player specializes in | chosen |
| **XP / level pacing** | when each becomes available | shared |

The governing distinction:

> **Methodology is choice. Growth expands what the chosen class is capable of
> doing in the first place.**

The chassis guarantees viability; the class determines supernormality.

Consequences held deliberately:

- Archetypes are **descriptive**, not mechanic-granting templates.
- A class owns its own Growth curve. Two classes sharing an archetype may mature
  very differently.
- Growth is generally automatic and authored — **not** a third choice tree.
  Player choice belongs to ability branches and Task allocations.
- Fundamental class functionality should be complete by **Lv15**.
- Late progression should be allowed to become supernormal.
- Noncombat classes must be able to become decisively powerful in their ideal
  strategic situations, not merely useful.

---

## 2. The universal level clock

**Level is information.** A player's level must let opponents and teammates
estimate how far that player is from a power spike. That requires equivalent
*categories* of progression at the same levels across the whole roster.

> The roster shares the clock. Classes determine what happens when it strikes.

| Lv | Event | Lv | Event |
| ---: | --- | ---: | --- |
| 0 | Passive | 16 | Task IV |
| 1 | Active 1 | 17 | quiet |
| 2 | Active 2 | 18 | Growth VI |
| 3 | Growth I | 19 | quiet / possible band transition |
| 4 | Task I | 20 | Task V |
| 5 | Active 1 branch | 21 | Growth VII |
| 6 | Growth II | 22 | quiet |
| 7 | quiet / proof | 23 | quiet / anticipation |
| 8 | Task II | 24 | Growth VIII + Task VI |
| 9 | Growth III | 25 | quiet |
| 10 | Active 2 branch | 26 | quiet |
| 11 | quiet | 27 | Growth IX |
| 12 | Growth IV + Task III | 28 | Task VII |
| 13 | quiet / proof | 29 | quiet |
| 14 | quiet / anticipation | 30 | Growth X |
| 15 | Growth V + Ultimate | | |

The shared intuitions this creates: Lv14 is one level from Ultimate and Growth V;
Lv15 completes the fundamental kit; Lv15→16 is one level from the first possible
advanced Task technique; Lv23 is one level from Growth VIII and Task VI; Lv27 is
one level from the final Task allocation; Lv30 completes the authored curve.

Quiet levels are not filler. They are realization, evaluation, proof, and
anticipation space before a spike.

### Universal versus class-authored

**Universal:** Growth levels 3/6/9/12/15/18/21/24/27/30; Task levels
4/8/12/16/20/24/28; the ability schedule (Passive 0, A1 at 1, A2 at 2, A1 branch
at 5, A2 branch at 10, Ultimate at 15); and the approximate importance of shared
breakpoints.

**Class-authored:** Health growth rate, Hunger growth, Inventory growth, other
physical attributes, when infrastructure first appears, which infrastructure
dimensions matter, infrastructure magnitude, methodology-specific capacities,
and whether a given Growth event is primarily personal, infrastructural or
mixed.

### Do not standardize Growth *content*

"Infrastructure begins at Lv18" is **not** a universal rule. Mole receives Route
infrastructure late because its curve does not need it early. Gardener needs
infrastructure early and could spend significant Lv6 Growth budget on
Development Zone Capacity.

> Every class receives Growth budget at the same levels. Each class spends that
> budget according to its own maturation curve.

Conceptually `Growth(class, level) = Physical + Capacity + Infrastructure +
Methodology-specific capacity`, with the allocation authored per class. A pure
combat class could spend almost the entire budget personally; a Production class
could spend it on the capacities its transformation methodology needs.

[CONFLICT] [`infrastructure.md`](../../infrastructure.md) records Level 6 as the
universal infrastructure recognition entry. Under this model infrastructure
timing is class-authored, and Level 6 is at most a common case. The two
statements are recorded together rather than silently merged; see the
reconciliation record.

---

## 3. Growth

**Growth** is the term for automatic class maturation. Not "Development", which
is already a strategic archetype; not "Advancement", which Minecraft already
uses.

Growth modifies attributes and capabilities.

**Personal dimensions:** Health, Hunger, Inventory, Natural Regeneration,
Saturated Regeneration, Exhaustion Efficiency, and class-resource capacity where
applicable.

**Infrastructure dimensions:** Capacity, Extent, Reach, Potency, Eligibility,
Concurrency, Projection, Throughput, Connectivity, Branching, Filtering/Routing,
Reliability, Integration.

Infrastructure is not an archetype entitlement. Classes receive only the
infrastructure access their actual methodology needs.

**The world is already persistent state.** A tunnel is a tunnel; a road is a
road; a changed landscape already records itself. Do not invent plugin-maintained
parallel state where the physical world already holds the result.

### Budgets need not be equal at every event

Lv15 already grants an Ultimate, so its Growth packet can be modest. Lv24 also
grants Task VI, so it can be restrained. Lv18/21/27 are cleaner Growth-only
levels and can carry more consequential packages.

The preferred general shape:

> regular continuous attribute growth **+** authored qualitative/discrete Growth
> spikes

A spike need not come from an irregular Health jump. Continuous attributes should
generally read as curves; discrete attributes — inventory slots, infrastructure
capacity, Route concurrency, new eligibility, methodology capacity — are where
spikes belong.

---

## 4. Player-facing numerical scale

Design discussion and the eventual custom HUD use approximately **10× vanilla
Minecraft HP**:

    1 Minecraft HP   = 10 displayed Health
    1 Minecraft heart = 20 displayed Health

So vanilla 20 HP reads as 200 Health, Mole's starting 10 HP as 100, and Mole's
Lv30 32 HP as 320.

Minecraft's health and damage already carry finer precision than the heart HUD
communicates. The ×10 scale is a clearer player-facing numerical language rather
than necessarily an engine change; the custom HUD exposes the precision that
already exists.

---

## 5. Task

Task is the shared player-choice specialization system, allocated at **Lv4, 8,
12, 16, 20, 24, 28** — seven allocations total. At each, choose **Yield**,
**Efficiency** or **Slaying**, or advance one already chosen.

    Y + E + S = 7

Because `4 + 4 > 7`, reaching IV in one tree forecloses IV in another. **IV is
inherently a commitment threshold.**

| Tier | Meaning |
| --- | --- |
| I–III | conventional competence |
| IV | commitment threshold; first advanced technique |
| V | specialist |
| VI | deep specialist |
| VII | exclusive / min-max specialist |

Typical shapes: 4/3/0 advanced hybrid; 5/1/1 specialist; 6/1/0 deep specialist;
7/0/0 exclusive.

### Tiers I–III

Passive **character** progression, not item enchantments: Yield is
Fortune/Looting I→III, Efficiency is Efficiency I→III, Slaying is
Sharpness/Power I→III. No class aptitude differences are currently intended at
these tiers.

### Tiers IV–VII

Advanced points need a universal quantitative floor so they are never dead:

    TaskTier = fractional continuation of the base property
             + eligible advanced Technique grant(s)

Do **not** simply grant full Fortune IV–VII or Sharpness IV–VII. [OPEN]
Fractional magnitudes are uncalibrated.

Techniques come from a **shared vocabulary**. Classes vary only by access,
threshold (IV/V/VI/VII), strength, and later Growth strengthening of the same
technique — `Grant = (Technique, Strength)`. Repeated access to a technique
should usually raise its Strength rather than stack copies. There are no bespoke
per-class Task trees.

---

## 6. Technique vocabularies

A technique should deepen the identity already latent in an implement through
that implement's unique application. Avoid arbitrary kill-proc stacks,
status-effect soup, cooldown-reset pseudo-spells, and anything that stops reading
as Minecraft equipment.

### Slaying

| Implement | Identity | Techniques |
| --- | --- | --- |
| Sword | sustained melee, area control, defense shredding | **Sweeping Edge** (area control), **Breach** (fractional armour penetration) |
| Axe | committed burst, defeating its counter | **Density** (fractional downward/fall-momentum damage), **Cleaving** (amplifies the axe's existing shield/guard-break relationship) |
| Shield | defensive scrap-fighting | **Stunning Strike** (mainhand shield attack applies Stun), **Quick Guard** [PROVISIONAL] |
| Spear | perimeter fighting, space control | **Lunge** (exceptional access to an otherwise-disabled mechanic), **Reach** |
| Bow | ranged burst, picks, covering pressure | **Multishot** (transplanted crossbow vocabulary), **Quick Charge** (reduced draw time to full-power) |

Stunning Strike matters particularly because ordinary classes do not freely use
the offhand. Quick Guard automatically guards the next X otherwise-blockable
attacks while an eligible shield is stowed and available, still obeying ordinary
guardability and direction rules. [OPEN] Whether Quick Guard requires the shield
anywhere in inventory or in a hotbar/ready slot.

Spear Lunge is **disabled at baseline** for exploitability and excessive
mobility; advanced Task access reintroduces it as an exceptional specialization.
Multishot must avoid enabling triple point-blank burst.

**Current combat equipment direction:** crossbows excluded as normal equipment;
mace excluded; spears allowed with baseline Lunge disabled; shields allowed; bows
allowed; the offhand generally unavailable unless a class kit grants it.

### Efficiency

Efficiency means improving how efficiently world resources and terrain are
converted into strategically usable form — not "the animation goes faster".

| Implement | Natural affinity | Techniques |
| --- | --- | --- |
| Pickaxe | Extraction | **Vein Mining** (connected eligible resource blocks mined together; Strength controls propagation/limit), **Excavation** (bounded spatial group of tool-appropriate blocks) |
| Axe | Construction | **Felling** (a qualifying tree/log structure processes additional connected logs), **Timber Processing** (stripped logs convert into additional usable construction material) |
| Shovel | Exploration | **Excavation** (shared spatial-volume technique on appropriate material), **Pathfinding** (personal out-of-combat movement speed on shovel-created path blocks) |

Pickaxe exploits resource connectivity and hard geology; axe exploits resource
structure and conversion into construction material; shovel exploits resource
volume and terrain access.

Construction can own the gathering and production of blocks whose primary
strategic purpose is world geometry — wood's useful identity is partly its
efficient conversion from world resource into usable construction geometry.

**Pathfinding is not a Route.** Pathfinding is personal, local and immediate; a
Route is team infrastructure with network and projection properties.

### Yield

Yield stays within the fields Fortune and Looting actually represent. It is not a
farming tree, an animal-economy tree, or a generic economic bonus tree. Tiers
I–III are Fortune and Looting.

| Line | Immediate | Renewal | Knowledge |
| --- | --- | --- | --- |
| Fortune / pickaxe | **Richness** — pickaxe-harvested Fortune resources have an additional chance of additional material | **Prospecting** — fully harvesting an eligible mineral opportunity gives it some chance to renew or become worth revisiting | **Deep Study** — thoroughly completing mineral opportunities grants increased XP |
| Looting / sword | **Butchering** — sword-killed Looting-eligible mobs produce additional ordinary drops | **Subsistence** — the hunting-renewal counterpart | [OPEN] name unresolved |

Prospecting should use the existing regenerative-resource lifecycle rather than
magical instant ore respawn; Subsistence should likewise interact with
regenerative animal and mob opportunities rather than resurrecting individual
mobs. Deep Study likely hooks into existing ore-vein-completion XP.

[OPEN] The third Looting technique is a hunting-related learning/XP concept
parallel to Deep Study. Candidate names discussed: Fieldcraft, Hunter's Lore,
Tracking, Gamekeeping, Huntsmanship.

The abstract parallel — immediate means more now, renewal means more future
opportunity, study means more progression — is organizing language, not
necessarily player-facing naming.

**Implement overlap is intentional.** Efficiency pickaxe is *how* you mine; Yield
pickaxe is *what you seek to get out of* mining. Slaying sword is how you fight;
Yield sword is what you seek to get out of killing.

---

## 7. Task and archetype

Task must not collapse into Extraction→Efficiency, Combat→Slaying, everyone
else→leftovers.

- **Extraction** — deep pickaxe Efficiency and Fortune Yield; may choose Slaying
  to become a contested/fighting extractor.
- **Construction** — axe Efficiency, especially Felling and Timber Processing.
- **Exploration** — shovel Efficiency, especially Pathfinding and Excavation.
- **Combat** — deep Slaying; Looting/sword Yield offers an alternate
  resource-economy axis.
- **Development** — productive-territory methodology stays class/Growth-owned;
  Task supplies ordinary personal competence around it.
- **Logistics** — network and material-movement methodology stays
  class/Growth-owned; Task supplies personal sourcing, combat and work
  competence.
- **Production** — tends **wide rather than tall**. Production means
  transforming acquired inputs into strategically useful outputs, and that
  methodology belongs to the class. Task determines what the producer does during
  downtime, how they source inputs, and how they exploit finished outputs.

Do not invent a generic fake "Craft Quality". Recipe exclusivity is technically
possible but a poor systemic solution: there are not enough valuable vanilla
items to reserve for Production classes without making everyone else worse at
Minecraft.

Kitfighter illustrates the width: an Efficiency build self-sources rapidly, a
Yield build seeks and extracts valuable inputs, a Slaying build leans on team
resource funnelling and personally exploits the combat equipment it produces.
"Wide rather than tall" is a tendency, **not** an archetype rule — do not
automatically grant every Production class more techniques.

---

## 8. Deterministic enchanting

The Enchanting Table applies a **preset authored package** per item type:

    input item + cost -> deterministic authored package

No random rolls, no reroll fishing, no bookshelf-optimization metagame as
character progression. This makes enchanting a Production transformation and a
commodity rather than a character-build system.

The separation: **Task** is character specialization, **enchanting** is
standardized produced quality, **class** is unusual methodology.

Preset packages should reinforce an item's baseline identity without consuming
identity reserved for advanced Task techniques — for example, do not place
Sweeping Edge in the ordinary deterministic sword package if Sweeping Edge is
meant to represent advanced Slaying specialization.

[OPEN] The packages themselves are undesigned.

---

## 9. Mole — the first authored curve

Mole is the **introductory calibration class**, so its Growth should stay unusually
understandable: Health rises constantly, Hunger matures early, Inventory expands
intermittently, Exhaustion replaces Hunger as the late endurance lever, and
Routes arrive only once the basic body is already mature. That simplicity is a
feature.

Archetypes: **Excavation / Combat**, with limited Route capability.

[CONFLICT] The kit handoffs and `classes.md` record Mole's primary archetype as
**Extraction**, and this handoff's own archetype-relationship section uses
Extraction. "Excavation" is reproduced as supplied; whether it is a rename or
loose usage is the owner's call.

> A physically durable, self-sufficient excavator who is difficult to force out of
> valuable ground, can turn excavation access into an initiation angle, and can
> spend non-contested periods establishing simple Routes that lead teammates
> through terrain it has opened.

### Full Lv0–30 curve

| Lv | Event | Health | Hunger | Inv | Other |
| ---: | --- | ---: | ---: | ---: | --- |
| 0 | Passive — Sifth Sense | 100 | 10 | 6 | exhaustion baseline; no Routes |
| 1 | Active 1 — Tunneling | | | | |
| 2 | Active 2 — Drill Rush | | | | |
| 3 | Growth I | 122 | 12 | 9 | basic expedition capacity |
| 4 | Task I | | | | |
| 5 | A1 branch | | | | [OPEN] branch set |
| 6 | Growth II | 144 | 14 | 9 | Exhaustion Efficiency I |
| 7 | quiet / proof | | | | |
| 8 | Task II | | | | |
| 9 | Growth III | 166 | 16 | 12 | carrying/work capacity |
| 10 | A2 branch | | | | [OPEN] branch set |
| 11 | quiet | | | | |
| 12 | Growth IV + Task III | 188 | 18 | 15 | Exhaustion Efficiency II |
| 13 | quiet / proof | | | | |
| 14 | quiet / anticipation | | | | |
| 15 | Growth V + Ultimate — Sinkhole | 210 | 20 | 15 | Hunger reaches maximum |
| 16 | Task IV | | | | first advanced technique |
| 17 | quiet | | | | |
| 18 | Growth VI | 232 | 20 | 18 | Routes: **Establish I**, Capacity 1 |
| 19 | quiet | | | | |
| 20 | Task V | | | | |
| 21 | Growth VII | 254 | 20 | 18 | Exhaustion Efficiency III; Route **Reach I** |
| 22 | quiet | | | | |
| 23 | quiet / anticipation | | | | |
| 24 | Growth VIII + Task VI | 276 | 20 | 21 | Route **Projection I** |
| 25–26 | quiet | | | | |
| 27 | Growth IX | 298 | 20 | 21 | Exhaustion Efficiency IV; Route Capacity **2** |
| 28 | Task VII | | | | |
| 29 | quiet | | | | |
| 30 | Growth X | 320 | 20 | 24 | Route **Reach II** |

By Lv15 the complete fundamental loop is available: sense → penetrate terrain →
initiate → seize/deny ground.

**Health: +22 every Growth, 100 → 320** (engine-scale 10 → 32 HP, 5 → 16 hearts).
Clearly supernormal without reaching a doubled vanilla bar; a true health-centric
Tank can exceed it substantially. The key property is **regularity** — Health is
the continuous curve, and spikes come from abilities, branches, Task thresholds,
discrete capacity, infrastructure and methodology instead.

**Hunger: +2 per Growth through Lv15, then stop at 20.** Early Growth raises fuel
capacity; late Growth raises the efficiency with which existing fuel is consumed.

**Inventory: 6 → 24**, discrete and intermittent rather than every Growth. The 24
endpoint deliberately leaves room above Mole for true inventory specialists at
30/36.

**Exhaustion Efficiency** is one quantitative attribute, not four unrelated
perks, improving at Lv6/12/21/27. [PROTOTYPE] Illustrative first-pass:
+6% / +12% / +18% / +24% efficiency, i.e. 94% / 88% / 82% / 76% of normal
consumption. **Not calibrated.** The cadence matters more than the magnitudes
right now.

### Durability model

Err toward durability. Minecraft damage scales upward substantially, baseline PvP
TTK can be low, and the game lacks ubiquitous gold-farm healing economies. Higher
Health is preferable to discovering later that late-game characters evaporate.

But Mole is not a conventional health tank. Its pattern is: avoid damage through
terrain and position → use moments of mitigation → emerge into dangerous space →
tolerate a short period of concentrated exposure → disengage through terrain.
Health is the **buffer for the exposed part of that cycle**.

The separation to preserve:

- **Growth:** "I have enough body to survive being exposed."
- **Kit:** "I have specific ways to avoid or mitigate damage."
- **Player skill:** "I decide when to expose that Health."
- **Slaying:** "I decide whether that exposed period is also threatening."

Momentary mitigation should come from Drill Rush branches, emergence and terrain
interactions, and displacement resistance — not from permanent Resistance in
Growth.

**No major regeneration Growth.** A Mole reduced from 320 to low Health should
care: it should need food, time, disengagement, allies and safe terrain. Earlier
ideas about a major late Saturated Regeneration upgrade are deprioritized and
removed from this curve. Do not combine enormous Health with enormous passive
recovery.

### Route curve

> Mole makes good corridors. Explorer makes good networks.

Mole can physically excavate tunnels long before formal Route access — the world
already records them. Formal Route Growth is the point where that physical access
becomes team movement infrastructure.

Strengths: establishment, respectable Reach, modest Projection, eventually two
concurrent corridors. Intentionally weak or absent: sophisticated Branching,
large network Capacity, exceptional Connectivity, broad Integration,
Logistics-style item movement, complex Routing, network-scale control.

Reach is likely Mole's strongest natural Route dimension because it complements
Tunneling: Mole opens a difficult corridor, and that corridor becomes a useful
long connection.

### Task aptitude — hypothesis

Efficiency: likely very deep (Vein Mining, Excavation). Fortune Yield: likely
strong (Richness, Prospecting, Deep Study). Looting Yield: probably shallow or
absent. Slaying: at least one meaningful advanced technique at IV, to preserve
Fighting Miner as a credible build, but probably not repeated qualitative
techniques at every later threshold — the fractional backbone continues
regardless. [OPEN] Thresholds and Strengths unassigned.

Build shapes to support: Efficiency VII extreme tunneler; Yield VI/VII powerfarming
gatherer; Efficiency III + Slaying IV Fighting Miner; Efficiency IV + Yield III
advanced mining hybrid; Efficiency IV + Slaying III excavation/combat hybrid;
Efficiency V + Yield I + Slaying I specialist with broad baseline competence.

---

## 10. Other classes under this model

Recorded as supplied, for Growth-calibration context. Kit detail remains owned by
[`classes.md`](../../classes.md).

**Gardener** needs infrastructure **early** and is the intended counterexample to
Mole — Lv6 can spend meaningful Growth budget on Development Zone Capacity. Do not
infer Mole's late infrastructure timing as a roster-wide rule. Its methodology is
Plant Material → Cultivar selection → manipulation → broader mastery, with
Torchflower, Sweet Berry Bush and Giant Bamboo settled as Cultivars.

[CONFLICT] This handoff names the Clip branches **Maturity / Proliferation /
Diversity**; the recorded kit in `classes.md` names them **Specimen /
Proliferation / Collection**. Only the middle branch agrees.

**Kitfighter** — Combat primary, Production secondary; Salvage is the core
Production methodology. Historical Salvage thresholds were 6/5/4/3 stacks at
Lv0/8/16/24, recovering 2 material; that timing **needs recalibration** against
this clock. [CONFLICT] The Offhander **Crossbow** branch needs revision because
crossbows are now excluded as normal equipment.

**Golem Master** — Builder/Combat/Production with strong Construction expression;
materials → animated entities and fabricated geometry. Copper Golem's combat
identity is swarm/harass/scatter hit-and-run using panic AI.

**Merchant** — employment/labour/workforce methodology: Work employs villagers on
Emerald payroll, useful work develops Mastery and contributes value. Production is
the clearest archetype. Do not force the older Exploration/Logistics
classification onto the current Merchant.

---

## 11. Principles to preserve

1. **Level is information** — it must communicate distance to major events across
   the roster.
2. **The roster shares the clock** — Growth, Tasks, branches and Ultimates happen
   at shared levels.
3. **Classes do not share Growth content** — Mole waits until Lv18 for Routes;
   Gardener can need Development Zone Capacity at Lv6.
4. **Growth is mostly automatic** — not a third choice tree.
5. **Continuous attributes read as curves** — Mole's +22 every Growth, not
   arbitrary +20/+30/+40 spikes, unless a class specifically requires non-linearity.
6. **Discrete attributes create spikes** — inventory, infrastructure capacity,
   Route concurrency, eligibility, methodology capacity.
7. **Kit branches create methodological choice** — Growth must not steal that role.
8. **Task creates ordinary-Minecraft specialization.**
9. **Advanced techniques use shared vocabulary** — classes vary by access,
   threshold and Strength; no bespoke per-class trees.
10. **Infrastructure is class-dependent** — neither universally early nor late.
11. **World state stays physical where possible.**
12. **Late power may be extreme** — Lv30 is not required to remain politely vanilla.
13. **High durability is desirable** given Minecraft damage; do not inherit
    vanilla PvP's low TTK by default. Mole's 320 is the first calibration point,
    not a universal maximum.
14. **High Health is not Tank** — a true Tank distinguishes itself through
    mitigation, protection, control, frontline presence and ally defense.
15. **Regeneration must not erase attrition.**
16. **Mole is the introductory calibration class**, and its legibility is a feature.

---

## 12. Open — next calibration

**Mole.** Exact Exhaustion Efficiency percentages; Route Reach I/II and Projection
I magnitudes; formal Route establishment rules; confirmation of 2 concurrent
Routes at Lv27; recovery/design of the Tunneling and Drill Rush branch sets;
whether either branch supplies Mole's momentary mitigation; advanced Task
thresholds and Strengths; testing 320 late Health against real armour,
Sharpness/Power, Slaying techniques, deterministic enchanting and expected late
burst; whether Inventory 24 is the right endpoint.

**Growth.** Approximate power budgets for Growth I–X without forcing identical
content; whether broad early/established/mature/late budget expectations are
useful, without converting them into category lockouts; building **Gardener as
the counterexample calibration class**; then comparing Mole and Gardener against
a highly Combat-centric and a highly Production-centric class.

**Task.** Calibrate the fractional IV–VII backbone; assign technique Strength
scales; finish the Quick Guard requirement; name and define Looting's
hunting-study technique; define Subsistence precisely; test Prospecting against
the regenerative-resource economy; determine actual class aptitude matrices.

**Combat / numerical.** Continue ×10 presentation; build the HUD around precise
Health rather than heart abstraction; establish expected TTK bands by match
phase; test high Health against Minecraft's nonlinear armour/toughness/damage
behaviour; determine the healing and food economy without assuming infinite
recovery loops.

**Enchanting / Production.** Author deterministic packages by item type; ensure
they do not consume advanced Task identities; recalibrate Kitfighter Salvage
against this clock.

**XP.** Establish how long players remain near each major breakpoint, and
evaluate whether Lv12, 15, 16, 18, 24, 28 and 30 have enough temporal separation
to be strategically legible in a ~25–35 minute match.

---

## One-sentence read

> Mole is an introductory Excavation/Combat class whose Growth turns a constrained
> geological scout into a 320-Health, high-endurance, high-capacity excavator that
> can briefly survive dangerous exposure, initiate through terrain, and eventually
> convert a small number of the corridors it opens into useful team Routes, while
> its actual mining and combat specialization remains determined by ability
> branches and the player's seven Yield/Efficiency/Slaying Task allocations.
