# Toolbox — Circuit Grammar

**Status:** Working design direction
**Date:** 2026-09-27
**Owner:** [`classes.md` §19](../../classes.md#19-toolbox) owns the class. This
record owns the circuit language: topology, timing, the component dictionary,
and the paths that were tried and rejected.

---

## 1. The core rule

> Inventory arrangement is circuit topology. Redstone components are both
> instructions and consumable machine parts. Signal progresses through physical
> inventory geometry over real time, while Toolbox pilots direction and
> targeting live.

Three mastery layers fall out of it, and they are the class's difficulty:

1. **Preparation** — component acquisition, arrangement, stack sizes, geometry,
   alternate configurations, and which vocabulary this map's resources permit.
2. **Live execution** — aim, facing, Observer refresh timing, block targeting,
   where Piston sends entities, responding to a target that moves while the
   circuit runs.
3. **World engineering** — traps and machines, ordinary Redstone, terrain, and
   eventually the Ultimate.

---

## 2. Topology

Flow is **row-major, left to right**. At a row's right edge it wraps to the next
available row's left edge, and that wrap is ordinary adjacency — no pause, no
special timing.

**Locked cells do not compress the board.** Unavailable slots keep their
physical geometry, so inventory capacity determines the *dimensions of a
programmable board* rather than merely how much Toolbox can carry.

### The unlock order, and what it means for Tripwire

Slots unlock in raw index order. Bukkit's indices do not match the screen:

```
screen top     row A = slots  9–17
               row B = slots 18–26
               row C = slots 27–35
screen bottom  hotbar = slots 0–8
```

So the hotbar frees first, then row A — the screen's **top** row — leaving a
visual gap. Consequences, which hold for **any** growth curve because the order
is independent of the rate:

| Unlocked | Board | Tripwire columns |
|---:|---|---:|
| 18 | two full 9-wide runs, not vertically adjacent | 0 |
| 21 | A partially above B | 3 |
| 27 | A fully above B | 9 |
| 36 | all four rows | 18 |

**18 is a horizontal milestone only.** Two full 9-wide runs with a clean wrap,
and no vertical correspondence whatsoever. The first vertical adjacency is 21;
the first full-width one is 27.

**[DECIDED] Tripwire arriving partway up the curve is intentional** — the
component becomes useful when the board can hold it. An earlier draft of this
record claimed 18 gave "two aligned rows and full vertical correspondence";
that was wrong about the geometry and is corrected here rather than fixed by
changing the unlock order.

---

## 3. Timing and resolution

Approximately **one primary component every ~5 ticks** [PROTOTYPE]. Long enough
that individual effects stay legible and the battlefield can change mid-circuit;
short enough that the whole machine still reads as a rapid mishmash.

> **Components inspect current world state when they resolve.**

Not at trigger. That covers Toolbox's facing and aim, target visibility, player
positions, inventory, block geometry and placement validity — and it is what
makes the circuit something Toolbox *pilots* rather than pre-programs.

Directional components therefore sample facing at their **own** resolution tick,
so `P-O-P-O-P` can push in three different directions as Toolbox moves the
mouse. The system permits weird uses rather than policing intended
directionality: facing away so Sticky Piston's opposite-pull behaves like a push
is legitimate.

---

## 4. Consumption

> Every resolved component or Dust drops one item.

This is the original "variety as durability" idea made mechanical. A well-stocked
Toolbox treats components as ammunition; a poor one literally sheds the machine
while being attacked, and can scramble to recover what fell.

**The same physical slot may fire more than once** if distinct legitimate signal
paths reach it — each firing costs another item. So geometry buys *execution
density per occupied coordinate*, never free resources.

---

## 5. Amplification

Three states only: **Base**, **+1**, **+2**. Granular 1–15 signal simulation is
[REJECTED] — it would make the machine incomprehensible for no gain.

Sources: Comparator, and A2 branch II. They stack to +2.

Grammar components need no scaled payload. Payload components should ideally
each have a meaningful axis across the three stages.

---

## 6. Observed and Illuminated

Both are **short real-time world statuses**, not circuit memory.

### Observed — Observer

> Acquire an enemy aimed at from the current circuit origin; apply Observed;
> make that target the circuit's projected origin.

Working duration ~20 ticks, roughly four component intervals. Real time rather
than "until next component" or "until circuit ends", so long machines must
**refresh** their target lock — a second Observer is a deliberate reacquisition
point, and a gap is fine provided no target-dependent component falls in it.

**Relay is permitted.** Because Observer searches from the current origin,
repeated Observers can leapfrog: `Toolbox → A → B → C`. Expensive in
Quartz-heavy components, drops, circuit time and live aim, and most of the
machine is spent on projection alone. That is the point.

**One current target/origin**, replaced by each successful Observer.
[REJECTED] Multiple simultaneous Observed targets causing Dispenser to
multicast — it would make Observer both targeting *and* multiplication.

[REJECTED] Observer freezing future direction. It changes origin; Toolbox's live
facing still supplies direction.

### Illuminated — Redstone Torch

> Illuminate the current circuit origin and apply Illuminated. The next relevant
> hit against an Illuminated target receives greatly increased knockback and
> consumes the status.

**Why this matters more than it looks.** An ordinary knockback trap tries to put
an opponent into a small receiving hole, and normally fails because the hole is
small, the opponent moves, and without Knockback II it takes several accurate
uninterrupted hits. Illuminated makes a marginal knockback source sufficient.

> Toolbox can make technically possible Minecraft engineering competitively
> executable.

Redstone Lamp has **no** Utility Belt instruction. It earns its place as Dropper
ammunition instead.

---

## 7. Component dictionary

The dictionary is deliberately small. **A component does not need a Utility Belt
effect to be a Toolbox tool** — several earn their place through Dropper or
through ordinary world Redstone.

| Component | Instruction |
|---|---|
| **Dust** | Wiring; defensive discharge when resolved as an instruction |
| **Redstone Torch** | Illuminate the current origin; apply Illuminated |
| **Piston** | Directional impulse in Toolbox's current facing, to eligible entities **including dropped items** |
| **Sticky Piston** | Pull opposite current facing, and Root |
| **Repeater** | Timing; delays the next component |
| **Comparator** | Convert triggering damage magnitude into amplification |
| **Tripwire Hook** | Activate exactly one slot directly beneath, +1 tick |
| **Observer** | Acquire, apply Observed, move the projected origin |
| **Dispenser** | Take the first item; vanilla Dispenser behaviour, else eject |
| **Dropper** | Take the first item and throw it; full blocks settle into terrain |
| **Hopper** | Vacuum nearby dropped items into inventory |
| **Note Block** | Play a note |
| Lamp, Target Block, Redstone Block, Daylight Detector, plates/buttons/levers | **No Utility Belt effect** |

### Dust

Two roles, and the boundary between them resolves a long-standing conflict
between "drop the whole stack for mitigation" and "every resolution drops one":

> **Dust traversed as wiring drops one. Dust resolved as an instruction
> discharges the stack**, mitigating damage in proportion to its size.

A Dust at the circuit head, or one not sitting between two components, is an
instruction. A fat Dust stack therefore becomes a deliberate defensive
investment rather than a grandfathered formula. Exact mitigation [OPEN].

Dust as the first resolved item performs its defensive behaviour; it does not
mean "skip me".

### Tripwire Hook

Its purpose is **parallel execution**, not vertical routing.

- Interacts with exactly **one** slot directly beneath. No downward search, no
  vertical wrap, no multiple cells.
- **The horizontal next component resolves first; the vertical one resolves one
  tick later.** So `[T][O]` over `[P]` gives O, then P — while `[T][Sp]` over
  `[O]` gives Sp, then O, and the Observer is too late to help that Sticky
  Piston. Secondary activation cannot retroactively affect the primary.
- [REJECTED] Tripwire's secondary establishing ordinary flow. It is exactly one
  parallel instruction, not a fork. A vertically activated Repeater or Dust does
  **not** start a second circuit — otherwise every Tripwire becomes an arbitrary
  branch and geometry stops meaning anything.

Because Tripwire reads literal vertical alignment while flow wraps horizontally,
a component can be activated vertically and then reached *again* by ordinary
flow. Two physical Pistons can produce four Piston activations — four dropped
items, not free duplication.

### Dispenser, Dropper, Hopper

These three form one vocabulary and none of them makes a strategic decision for
Toolbox. **Neither Dispenser nor Dropper filters for eligible items**; both take
the first item, whatever it is. Arranging the inventory so TNT is first *is* the
programming.

**Dispenser** — take the first item and perform its vanilla Dispenser behaviour
from the current origin; if there is none, eject it. `Di(diamond)` spits a
diamond, and the machine does not helpfully search past it.

- Arrow: fires from the projected origin, in Toolbox's **current** facing.
  Observer supplies origin, never homing — an arrow originating near an Observed
  target may immediately strike it, and that is acceptable.
- TNT: one ordinary primed TNT. Normal fuse, normal explosion, **no authored
  bonus damage**.
- **Block-interaction payloads require a valid block target inside Toolbox's own
  placement reach.** Observer does not extend block-targeting. This is why lava
  cannot be dropped on a remotely Observed enemy, and why the earlier
  "lava always double-dispenses" special case is [SUPERSEDED] — it existed to
  solve a problem this rule removes.

**Dropper** — take the first item, eject it from the current origin in Toolbox's
current facing, and give it physical item motion.

> If the item corresponds to a **full block**, it becomes that block when it
> comes to rest in a valid, empty, **eligible** placement. Otherwise it remains
> a dropped item.

- "Eligible" inherits every existing placement restriction, so a thrown block
  cannot settle inside a no-build volume. Dropper needs no clause of its own.
- It does **not** place on first collision. A launched block brushing a wall
  becoming terrain mid-flight would make ballistics impossible to reason about.
- Full blocks only, no exception list. Cobweb is **out**. Honey and Slime Blocks
  are in, and bring their ordinary world properties with them — no authored
  effect needed.
- The intuitive test: *could this land as an ordinary 1×1×1 cube?*
- Gravity blocks settle and then behave normally.

Because Piston applies its impulse to dropped items, `D-P` while looking upward
throws a block up and then launches the airborne item horizontally — a two-stage
block launcher whose usefulness depends on flight timing players should
discover, not on an authored combo.

`D(TNT)` and `Di(TNT)` became a coherent pair without being designed as one:
Dispenser primes, Dropper places inert. Remote placement and remote detonation
as separate instructions.

**Hopper** — immediately absorb nearby dropped items into Toolbox's inventory.
A real item vacuum, not only Utility Belt drops. Amplification enlarges the
radius; no second effect is needed.

It is a natural circuit terminator — *enough machine; recover what is still in
reach* — and carries a spatial cost, because components scattered by movement or
by earlier effects fall outside the radius.

> **Hopper recovers the objects. It does not reconstruct the program.**

Ordinary Minecraft pickup rules apply; items land wherever they land. Restoring
them to their original slots would erase the attrition that firing is supposed
to cost. [OPEN] A consequence: Hopper can place items *ahead* of the execution
pointer and change what the rest of the circuit does.

[SUPERSEDED] Dropper-as-mitigation. Dust handles sacrificial defence; Hopper
handles recovery. Cleaner vocabulary.

---

## 8. Dependency and map variation

Comparator, Observer and Daylight Detector need Nether Quartz; Sticky Piston
needs Slime. Some maps make these hard or absent.

> **Toolbox does not receive artificial Quartz access to guarantee every
> circuit.** Losing components changes the vocabulary available on that map, and
> that is desirable.

This makes Toolbox the roster's **OPTIONAL** dependency example, against a
hypothetical water-dependent class whose identity collapses entirely without its
resource. Never guarantee a class every optional dependency merely to preserve
build parity across maps.

---

## 9. Placement Reach

The lava rule made Toolbox's block interactions depend on how far Toolbox can
place, which suggests reach should be a scalable class attribute — **typed by
Minecraft verb rather than collapsed into a generic RPG "range"**:

- **Placement Reach** — placing and placement-like interactions.
- **Break Reach** — beginning to break a block.
- **Entity Reach** — attacking. Combat-sensitive; almost certainly *not*
  suitable as generic progression, and deliberately not proposed. Nobody should
  be critting from unusual distance as a side effect.
- **Use Reach** — containers and buttons. Probably too marginal to progress.

Placement Reach is the attractive one because it changes *which positions and
geometries are possible* rather than adding a percentage.

> Universal progression establishes ordinary Minecraft capability. Class
> progression can push selected Minecraft **verbs** beyond ordinary spatial
> limits.

[TERMINOLOGY] `classes.md` already uses **Reach** as an *infrastructure*
dimension. Typed player reach is a second meaning for the same word and needs
disambiguating before either is implemented.

It needs no new machinery: `GrowthPacket` takes a free-text `dimension` on a
`PERSONAL` effect, so `{ dimension: placement_reach, effect: reach, tier: 2 }`
loads and validates today.

---

## 10. Worked examples

**`C-P-O-Sp`** — a strong hit amplifies via Comparator; Piston displaces the
attacker; Observer reprojects them; Toolbox turns while the sequence runs, so
Sticky Piston pulls opposite the *new* facing and slams them downward.

**`P-O-P-O-P`** — push, reacquire, push elsewhere, reacquire, push again. Three
directions from one circuit, steered by the mouse.

**`C-O-O-O-Di`** — Comparator lengthens O1's reach; the Observers relay
Toolbox → A → B → C; Dispenser fires once at C. Most of the machine is
projection.

**`C-Torch-Di-Di-Di` beside a physical knockback trap** — the inventory circuit
amplifies, illuminates and supplies staggered TNT, while the *world* machine
creates the knockback geometry that puts the victim into a receiving hole, and
Illuminated is what makes the otherwise-unreliable knockback sufficient. Toolbox
can be standing on the pressure plate operating it. The hole needs to be only a
couple of blocks deep: the point is confinement around TNT, not the fall.

> inventory circuit + ordinary Minecraft machine + live player execution

**Offset Tripwire over a wrap** — because Tripwire reads literal vertical cells
while flow wraps, two physical Piston slots can fire four times. Four dropped
Pistons; greater execution density per occupied coordinate.

**`O-Sp-D(web)` → melee** — Observer establishes the remote origin, Sticky
Piston pulls the target in and Roots them, Dropper throws a cobweb whose landing
is now far more likely to matter, and Toolbox finishes with an ordinary sword.
Under the full-blocks-only rule the web no longer settles as terrain, so this
combo needs another mechanism — recorded because the *shape* is the class in
miniature: **the class supplies reliable primitives; the player authors the
combo**, and each stage solves the next stage's prerequisite.

A circuit needs no finisher component. Turning "enemy at inconvenient distance,
moving freely" into "enemy in front of me, rooted, while I have a sword" is a
successful machine.

---

## 11. Stress-testing posture

> Construct apparently outrageous circuits first, then ask whether their power
> is naturally constrained by resources, geometry, execution time, positioning,
> replenishment, aim and ordinary counterplay.

Do not answer a strong interaction with a cooldown cap, an effect cap or an
anti-combo clause before determining whether the machine is executable and what
it costs. Natural limiters already include Quartz and Slime availability,
component stack depletion, dropped-component recovery, circuit time, target
movement, live aim, block geometry, placement reach, build time, and the fact
that opponents can simply leave or destroy a machine.

---

## Rejected — do not silently restore

- Observer persisting for the whole circuit. **Superseded** by short real-time
  Observed.
- Observer freezing future directional effects. **Rejected.**
- Multiple Observed targets causing Dispenser multicast. **Rejected.**
- Tripwire creating secondary horizontal flow. **Rejected.**
- Vertically activated Dust or Repeater starting another circuit. **Rejected.**
- Comparator as Tripwire-flow recovery. **Superseded** by damage-based
  amplification.
- Granular 1–15 signal simulation. **Rejected.**
- Dispenser custom-placing cobweb. **Rejected** — vanilla semantics preserved.
- Lava appearing on a remotely Observed target. **Superseded** by the
  placement-reach rule.
- Lava always double-dispensing. **Superseded** by the same rule.
- A2 having a separate circuit. **Rejected** — the shared machine is the point.
- A2 skipping grammar when counting its budget. **Rejected.**
- Dropper as damage mitigation. **Superseded** by Hopper.
- Every Redstone component needing a class effect. **Rejected.**
- Guaranteed Quartz access on Quartz-poor maps. **Rejected.**
- Arbitrary ceilings on huge Ultimate machines. **Deferred** pending
  stress-testing.

---

## Open

**Utility Belt** — exact cadence; circuit-start discovery in all inventory
states; handling of empty and locked cells during flow; the self-damage-versus-A2
incentive recorded in `classes.md` §19.

**Dust** — exact mitigation formula.

**Amplification** — Comparator's damage thresholds; whether every payload
meaningfully supports +1 and +2.

**Observed** — exact duration, search geometry, relay implementation, and
replacement semantics when a target dies or disappears.

**Illuminated** — duration, knockback multiplier, whether Glowing is part of the
payload.

**Piston / Sticky** — displacement, collision handling, Root duration, amplified
values.

**Dispenser / Dropper** — the boundary between circuit components and payload
inventory, which both read as "the first item"; projected spawn geometry;
whether a settled Dropper block registers with `Provenance`, since Structural
Integrity and Construct designation cannot see what is not attributed.

**A1** — names; swap semantics with stack sizes and duplicates; cooldown
reduction and healing amounts.

**A2** — base and extended budgets; the precise definition of "payload effect"
for Short Circuit; its damage and radius.

**Ultimate** — duration; what constitutes one circuit; how powered components
are detected; repeated powering and clocks; and what counts as "deliberately
activates". `D(Redstone Block)` — throwing a power source at a pre-built machine
— is the case that will decide the last one.
