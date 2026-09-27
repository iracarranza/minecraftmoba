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

Flow reads like text: **left to right, top row down**, beginning at the
**top-left** of the inventory. At a row's right edge it wraps to the next
available row's left edge, and that wrap is ordinary adjacency — no pause, no
special timing.

**Counting begins at the top-left, not at the hotbar.** Bukkit's slot indices do
not match the screen, and the flow order follows the *screen*:

```
flow order        screen position        Bukkit
  1st   row A     top                     9–17
  2nd   row B                            18–26
  3rd   row C     directly above hotbar  27–35
  4th   hotbar    bottom                  0– 8
```

### The hotbar is the magazine

Flow runs until it reaches something that is not a circuit item, and **that item
is what Dispenser and Dropper take**. Because the hotbar is last in reading
order, an ordinary Toolbox ends up with its program in the storage rows and its
ammunition in the hotbar — which is where a sword, food and blocks already live.

This resolves the circuit/payload boundary without a rule about it: the boundary
is wherever the program stops, the player decides where that is, and whatever
they are carrying is what the machine fires.

It also explains the Lv0 problem exactly. At six slots only the hotbar exists,
so program and magazine are forced to share it. At Lv3 the first cells of row A
unlock, the program moves there, and the **entire hotbar** becomes magazine — a
larger jump in usability than +6 slots suggests.

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
| 36 | all four rows | 27 |

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

Approximately **one primary component every ~5 ticks** [PROTOTYPE], and
**Dust costs no time**: flow follows Dust to the next component instantly, then
waits the interval. The cadence is measured between *components*, so a circuit's
duration is its component count, not its cell count — wiring is paid for in
items, never in seconds.

That matters more than it sounds. A 27-cell board wired half in Dust is ten
components, so 50 ticks rather than 135, and a large board stays a burst rather
than becoming a near-continuous program that eats its own cooldown.

Long enough
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
| **Sticky Piston** | Pull opposite current facing, and Root (10 ticks) |
| **Repeater** | Timing; delays the next component. Also the parity shim (§2) and the manual-intervention window (§10C) |
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

Because Tripwire reads literal vertical alignment while flow moves one row down,
a hooked component is pre-fired and then reached *again* by ordinary flow 19
ticks later. Two physical Pistons can produce four Piston activations — four dropped
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

[CORRECTED 27 September 2026] Read that as **stagger, not economy**. Each
activation still drops its own item, so four firings cost four Pistons; what the
geometry buys is two of them arriving 19 ticks late while the program continues,
not two of them arriving free. See §10A.

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

## 10A. The board at each breakpoint

Toolbox's authored curve reaches 36 at Lv18 (`classes.md` §19). What the board
can *hold* at each step, with flow reading A → B → C → hotbar and Tripwire
reading one row down on screen:

| Lv | Slots | Rows available | Tripwire positions |
| ---: | ---: | --- | ---: |
| 0 | 6 | part of hotbar | 0 |
| 3 | 12 | A×3 + hotbar | 0 |
| 6 | 18 | A + hotbar | 0 |
| 9 | 21 | A + B×3 + hotbar | 3 |
| 12 | 24 | A + B×6 + hotbar | 6 |
| 15 | 27 | A + B + hotbar | 9 |
| 18 | 36 | A + B + C + hotbar | **27** |

**Tripwire always fires forward, never back.** It targets one row down, and flow
also moves one row down, so a hook always *pre-fires a component flow will reach
later*. There is no reaching backward into an already-executed row at any board
size — the hotbar has nothing beneath it, and every other row's target is
downstream.

**The gap is a uniform 19 ticks.** A hook's vertical fires at `5(i+1)+1` and
flow arrives at the same component 19 ticks later, whatever row it sits in. So a
hooked component runs twice, just under a second apart.

That 19 is one tick inside Observed's ~20, which is a knife-edge worth keeping:
a hooked **Observer** re-fires one tick before its own Observed would lapse, so
the lock is continuous — while a hooked **Piston** is simply two shoves a second
apart. Same mechanism, completely different instrument, decided by which
component sits under the hook.

**Tripwire buys stagger, not economy.** Each activation still drops its own
item, so hooking does not make a component cheaper — it makes it *late as well
as early*. The value is temporal overlap while the main program continues.

### Dust is required, and it fights Tripwire

**Settled 27 September 2026.** Dust is required between every pair of
components. It costs items but no time (§3), and it is what keeps a full board's
duration inside its cooldown.

It also creates the geometry problem that makes Tripwire hard to use, which is
worth stating because it is not obvious and it invalidated three worked examples
before anyone noticed.

A nine-cell row packed as `C d C d C d C d C` puts components on **even**
positions and ends on a component. The wrap is ordinary adjacency, so the next
row must **begin with dust** or two components touch. That row is then
`d C d C d C d C d` — components on **odd** positions.

**Parity flips at every wrap, so every Tripwire misses:**

```
A  C d C d C d C d C     components at 0 2 4 6 8
B  d C d C d C d C d     components at 1 3 5 7
   ↑ a hook at A0 targets B0, which is dust
```

**The fix is a parity shim: two consecutive wiring cells instead of one.**

One wiring cell between components preserves parity; **two flip it**.

```
A  C d C d C d C d d     components at 0 2 4 6   ← double wiring at 7–8
B  C d C d C d C d d     components at 0 2 4 6   ← same
C  C d C d C d C d C     components at 0 2 4 6 8
```

Now every hook lands. The cost is one component per aligned boundary — a full
board goes from 14 components to **13** — which is cheap, but it must be
deliberate. **Tripwire is not free real estate; it is a layout investment paid
in a wasted cell.** A player who packs rows greedily gets no parallelism at all,
and nothing explains why.

#### This is what Repeater is for

Dust and **Repeater** are both wiring, so either can occupy a link cell. That
gives the shim two flavours and finally gives Repeater a structural job rather
than a floating "adds delay" description:

| Link | Parity | Time |
| --- | --- | --- |
| `d` | preserved | none |
| `Rep` | preserved | delay |
| `d d` | **flipped** | none |
| `d Rep` / `Rep Rep` | **flipped** | delay |

So a player aligns parity for free with double Dust, or **aligns it and buys a
deliberate pause** with a Repeater. The Repeater is not a worse Dust; it is the
shim that does something.

And the something matters, because the hook gap is a knife-edge. A hook fires
its target 19 ticks before flow reaches it, and Observed lasts ~20 — one tick of
overlap. A Repeater at the row boundary delays flow's arrival and **widens that
gap past the Observed window**, turning a continuously-held target into two
separate acquisitions. The same shim, placed for parity, is also the dial that
decides whether a hooked Observer keeps its lock.

[OPEN] Repeater's delay length, which now has a second consumer: it tunes the
hook gap, not only the next component.

### Worked boards

All boards below are parity-aligned where they use hooks.

**Lv0 — 6 cells.** Program and magazine collide in the hotbar.

```
H [C][d][P][d][Sp][sword]        3 components · 15 ticks · 5 items
```

**Lv3 — 12.** Program moves to row A; the whole hotbar becomes magazine.

```
A [C][d][P]
H  sword, food, blocks …          2 components · 10 ticks · 3 items
```

**Lv6 — 18. The relay.** Row A full, no parallelism available yet.

```
A [C][d][O][d][O][d][O][d][Di]    5 components · 25 ticks · 9 items
H  magazine — Di fires what you are carrying
```

**Lv9 — 21. First hook, and the shim that pays for it.**

```
A [Tw][d][C][d][O][d][Sp][d][d]   ← shim at 8
B [P ]
H  magazine
```

The hook at A0 pre-fires B's Piston; flow reaches the same Piston later. Two
shoves from one slot, 19 ticks apart.

**Lv15 — 27. Row A hooks row B.**

```
A [Tw][d][Tw][d][Tw][d][Tw][d][d]   4 hooks + shim
B [P ][d][O ][d][Sp][d][Di][d][d]   4 payloads, each doubled
H  magazine
```

Eight components, twelve activations, 40 ticks.

**Lv18 — 36. The double-hook cascade.**

A hook whose target is *another hook* chains: the second hook fires its own
vertical one tick later, reaching a third row.

```
A [P ][d][P ][d][C ][d][Tw][d][d]   ← hook at A6
B [To][d][H ][d][To][d][Tw][d][d]   ← hook at B6, directly beneath
C [· ][d][· ][d][· ][d][O ][d][· ]  ← Observer at C6
H  magazine
```

Timing of the Observer at C6:

| | Path | t |
| --- | --- | ---: |
| 1 | A6 hook → B6 hook → C6 | **22** |
| 2 | flow reaches B6, its hook fires C6 | **41** |
| 3 | flow reaches C6 | **55** |

**Three activations of one slot, spread across 33 ticks**, from two hooks and a
shim. That is the strongest thing the geometry does, and it is the reason
Tripwire earns its awkwardness.

[OPEN] The cascade depends on a Tripwire activated *by another Tripwire* firing
its own vertical. §7 says a secondary activation activates exactly one component
and grants it no flow — activating a hook should therefore make it hook, but the
rule was written before anyone tried chaining and does not say so. If chains are
refused, the third row is unreachable and the shim is worth much less.

[OPEN] Row C can be a hook target **or** an A1 reserve, not comfortably both.
Hooks fire the component beneath regardless of whether flow would reach it, so
a hooked reserve is consumed by the circuit it was being kept apart from.

### Duty cycle, and what the cooldown actually buys

Cooldown on the ability clock (§*The three clocks*), [WORKING]:
**14 / 12 / 10 / 8 / 6 seconds** at Lv0 / 5 / 10 / 15 / 25.

Against parity-aligned boards, with the circuit in the storage rows:

| Lv | Slots | Components | Duration | Cooldown | Duty | Idle |
| ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| 0 | 6 | 3 | 0.75s | 14s | 5% | 13.3s |
| 10 | 21 | 6 | 1.50s | 10s | 15% | 8.5s |
| 15 | 27 | 8 | 2.00s | 8s | 25% | 6.0s |
| 18 | 36 | 13 | 3.25s | 8s | 41% | 4.8s |
| **25** | 36 | 13 | **3.25s** | **6s** | **54%** | **2.8s** |

At Lv25 the machine is executing more than half the time in sustained combat.

**But the cooldown does not buy power, it buys burn rate.** At ~30 items per
activation on a 6-second cycle that is five items a second; a thirty-second
engagement is five activations and roughly 150 components, of which the
expensive fraction is iron and quartz. Nobody supplies that. So a faster
cooldown means **the resource wall arrives sooner**: the full board runs twice,
then Toolbox is on a cut-down circuit whether it planned to be or not.

That is a coherent lever, but it is a different one than it appears to be, and
it makes Hopper load-bearing rather than optional.

[OPEN] **Does the cooldown start at trigger or at completion?** At trigger the
table holds. At completion the Lv25 cycle is 3.25 + 6 = 9.25s and duty falls to
35%. "A global Utility Belt cycle cooldown" does not answer it, and it is the
difference between a near-continuous machine and a bursty one.

[TECHNICAL RISK] **The 6-second cooldown is only safe because Dust is
required.** Twenty-seven cells wired half in Dust is 13 components at 3.25s. If
Dust ever became optional between adjacent components, the same board would be
27 components at 6.75s — longer than its own cooldown, and circuits would
overlap and multiply. Dust's requirement is load-bearing for the cooldown curve,
not only for the economy.

### Where the A1 alternate loadout lives

A1 swaps **unlinked** components into the circuit, and unlinked means "after
flow stopped". So the alternate can only occupy the tail, and the tail has two
possible homes — which is the real reason **a Toolbox cannot use all of its
space**.

**In the hotbar.** Natural, since the hotbar is last in reading order. But it is
already carrying a sword, food, blocks and the magazine, so realistically two or
three cells survive — exactly A1 base (two) and A1-II Overhaul (three). Enough
for the ability as written and nothing more.

**In a storage row, by breaking the circuit early.** A non-component at row C's
first cell stops flow there; C's remaining eight cells hold unlinked components.
A far deeper reserve, bought by giving up row C as circuit.

| Row C used as | Circuit components | Alternate depth |
| --- | ---: | ---: |
| circuit | 13 | 2–3, hotbar only |
| reserve | 8–9 | up to 8 |

**A1 is therefore a rotation, not a toggle.** Each use swaps the last two
unlinked with the first two linked, so a deep reserve is not one alternate
configuration but a *magazine of configurations*, fed two at a time and gated by
A1's own cooldown. A1-III Redundancy reads cleanly against this: filling the
tail with duplicates of the opening spends that space on sustain instead of
versatility.

**And running dry rewrites the machine.** The break item that stops flow is also
the first item, so Dispenser eats it. A single item there vanishes on the first
activation, flow runs past the stop next time, and the unlinked reserve silently
becomes part of the circuit. The player-side answer is natural — make the break
a **stack**, so Di takes one and the rest hold the line — but it means ammunition
and circuit terminator are the same object, and **exhausting your ammunition
changes your program**. Keep that rather than designing it out: it is the class's
premise, and it punishes precisely what the class should be punished for.

### Combos, against the escape test

**"The Chute" — funnel and knockback, Lv15.** Walls first, Root last, because
Root is only two components long.

```
A [O ][d][Dr][d][Dr][d][To][d][d]   acquire · wall left · wall right · Illuminated
B [Sp][d][P ][d][P ][d][H ][d][d]   Root, then two shoves inside its window
H  honey ×64 · …
```

| Escape | Closed by | Open? |
| --- | --- | --- |
| Run | Root | |
| Jump | honey walls reduce it | |
| Steer out of the shove | Root | |
| Water clutch | — | **open** |
| Mine out | — | **open** (honey is soft) |
| Class mobility | — | **open** |

Both shoves land inside the 10 ticks, so the target travels the full run with
no ability to redirect. The honey walls do the aiming and Illuminated does the
distance — Toolbox solving a knockback trap's reliability problem from both
ends at once, which no other class can do.

**"The Box" — confinement, Lv18, hard ground only.**

```
A [O ][d][Sp][d][Dr][d][Dr][d][d]   acquire · Root · two walls inside the window
B [Dr][d][Dr][d][Dr][d][Di][d][d]   remaining walls and ceiling, then lava
C [· ][d][· ][d][· ][d][H ][d][· ]  recover
H  obsidian ×64 · lava bucket · …
```

Only the first two Droppers land during Root; the rest depend on the target
having nowhere to go, which is why this one **requires hard ground**. On dirt
they are gone before the ceiling exists.

| Escape | Closed by | Open? |
| --- | --- | --- |
| Run, jump, steer | Root, then walls | |
| Mine out sideways | obsidian, hardness 50 | |
| Climb or pearl up | ceiling | |
| **Tunnel down** | — | **open unless the ground is hard** |
| Class mobility | — | **open** |

[CORRECTED] An earlier draft of the same idea — "The Pen" — placed four walls
during a single Root and does not work. Root is ten ticks; four Droppers are
twenty.

### Does any of it break?

Per §11 the test is cost, not instinct, and the answer is no — by a wide margin.

| Board | Components | Duration | Items/proc | Cooldown |
| ---: | ---: | ---: | ---: | ---: |
| 6 | 3 | 0.75s | 5 | 20s |
| 18 | 5 | 1.25s | 9 | 16s |
| 27 | 10 | 2.5s | 23 | 14s |
| 36 | 15 | 3.75s | ~32 | 10s |

Duration stays comfortably inside the cooldown at every size, because Dust costs
no time. What limits the full board is **material throughput, not power**: ~32
items per activation, of which the expensive fraction — Pistons, Observers,
Comparators, Dispensers — is iron and quartz. Thirty activations in a match is
roughly 150 Pistons, which no team supplies.

So the full board is affordable a handful of times per match and the rest of the
time Toolbox runs a cut-down version. **Board size is not how strong Toolbox is;
it is how strong Toolbox can afford to be right now.** Inventory is capacity,
components are ammunition, and the two are separate resources.

Hopper is what makes the large board sustainable at all — and it only recovers
what is within radius, so a machine that uses Piston on *Toolbox* moves its
owner away from their own droppings. The sustainable machine is the one that
stays put.

## 10B. What a trap actually is

**Settled 27 September 2026**, from the trapping corpus rather than from
first principles.

> A trap is not a damage source. It is an **enumeration of the victim's
> escapes, with each one closed.**

That is the design test for any Toolbox combo: list the outs, show which the
circuit closes, and **name the one it does not**. A combo with no open row is
either wrong or broken.

### Movement denial and interaction denial are different problems

`classes.md` § *Movement and input statuses* settles Root as **movement input
only** — WASD, sneak, jump — with physics and all interaction surviving it.

| Escape | Root | Needs |
| --- | :---: | --- |
| Run | ✓ | |
| Jump out | ✓ | |
| Steer mid-air | ✓ | |
| Voluntary crawl | ✓ | |
| Water-bucket clutch | ✗ | geometry |
| Block clutch, pearl | ✗ | geometry |
| Mine out | ✗ | hard blocks |
| Attack back | ✗ | Stun, or killing them |

**So Root alone traps nobody**, and no stronger status is the answer. The
corpus solves interaction denial with *geometry* — shafts of signs that break
a water clutch, crafting tables that swallow a block placement by opening their
UI instead. **The class supplies movement denial; the world supplies
interaction denial.** Neither half is a trap.

Two properties of Root that circuits should exploit rather than work around:

- **Root does not stop external displacement.** A rooted target still takes
  knockback and cannot steer or resist it, so `Sp` then `P` delivers them
  precisely. Root is a *delivery* tool as much as a holding one.
- **Root drops people.** Rooted mid-launch they stop for one tick and then
  fall, uncontrolled, because gravity is not an input. Launch, Root, and the
  fall does the work.

### Ten ticks is two components

Sticky's Root is 10 ticks — **two component activations**. That is the real
budget, and it invalidates any combo that assumed a target stays put while a
structure goes up. You cannot build a box during a Root. You can place two
blocks, or land two shoves.

Spending amplification on Root duration instead of on Piston distance is
therefore a genuine circuit decision, not a default.

### Suffocation is weak; confinement is not

[CORRECTED] An earlier version of this record called a Root-plus-sand
suffocation column the one combo likely to be genuinely broken. It is the
weakest thing in the set. Sand is hardness **0.5** and is cleared in under a
second — often faster than the suffocation resolves. Obsidian is **50**.

But obsidian cannot be delivered the same way. **Block placement is refused
inside an entity's hitbox**; gravity blocks are the exception, which is exactly
why the suffocation family is built on soft blocks and exactly why it is weak.
Pushing a hard block into someone needs a piston crusher, and Toolbox's Piston
moves *entities*, not blocks.

So Toolbox's strong version is **confinement, not suffocation**: Root, then
obsidian walls and a ceiling. The box does not deal damage — it makes the
situation outlast the fight, which is the class's "damage is systemic" premise
working as intended.

### Every Toolbox box is open at the bottom

Dropper settles blocks in **empty** space, and the cell under a standing
player's feet is already terrain. So Toolbox can build walls and a ceiling and
**never a floor**. The sixth side is whatever ground the fight is on:

| Ground | Time to escape downward |
| --- | --- |
| Dirt, sand, gravel | about a second — the box is decorative |
| Stone, deepslate | a few seconds with a decent pick |
| Obsidian, bedrock, hard terrain | none |

**Toolbox's trapping power is therefore terrain-dependent**, which is a
property to keep rather than fix. It turns map knowledge into class power, and
this project already classifies maps by exactly that kind of geological
character — a "Toolbox map" is one with hard exposed ground.

It also makes **Mole structurally immune**, permanently, which is a clean
counter rather than a balance accident. Class mobility generally is the escape
Toolbox cannot close: Lightfooted Bounds out, Daredevil has Runway. Trapping is
therefore a read on the enemy's cooldowns, not a guaranteed kill — which is a
much healthier place for the class than "unescapable".

## 10C. Fluids, and the one payload per run rule

### Fluids are the exception to the hitbox rule

§10B says block placement is refused inside an entity's hitbox. **Fluids are
not.** Lava and water can be bucketed onto a player, which is the only route by
which a hard block reaches someone's head:

> A **lava source** contacted by water becomes **obsidian**.
> **Flowing lava** contacted by water becomes **cobblestone**.

That distinction decides the whole technique, and it is why the target must
already be in a **1-wide hole**. The hole keeps the source block a source,
sitting in their head cell, and stops either fluid spreading somewhere
unhelpful. In the open the same two buckets produce a lavacast, not a coffin.

Both are Dispenser bucket payloads, so both are **block interactions** — they
use Toolbox's own placement reach and **Observer does not project them**. This
is the one kill in the class that cannot be performed remotely.

The gate is the hole, not the kill. Toolbox cannot build a 1-wide pit inside a
10-tick Root — four walls is twenty ticks — and has no excavation. So this is a
**terrain play**: a hole that already exists, or one the funnel drove them into.

It is also not invented here. It is the bedtrap's lava component plus
lavacast's conversion, both documented trapping techniques.

### One payload type per uninterrupted run

Dispenser and Dropper take **the first item**, with no search for a compatible
one (§7). The consequence is general and was not designed:

> A circuit gets **one payload type per uninterrupted run**. Repeating a
> payload is free; mixing two requires intervention.

`Di(TNT)` three times is free. `Dr(obsidian)` four times is free. Lava then
water is not, and neither is honey then obsidian. **Circuits therefore favour
repetition, and mixing payloads is the advanced play.**

Three ways to mix, in rising difficulty:

| | Method | Cost |
| --- | --- | --- |
| **Manual half** | Lava in slot 0 for the circuit, water **in hand** — the player right-clicks it themselves. "First item" is slot order, not what is held, so no switching is needed at all. | Hand and attention; no circuit cost |
| **Repeater window** | The circuit fires both while the player swaps slot 0 during a Repeater's delay | A component plus a shim; frees the hand |
| **Tick-perfect swap** | The same, inside the bare 5-tick gap | Likely not humanly reliable |

**This is Repeater's third job** — delay, parity shim (§2), and now the window
for manual intervention.

### The strongest combo is deliberately un-automatable

Follow that through and the most lethal thing in the class cannot be
pre-programmed and fired. Somebody has to operate the machine while it runs,
which is §1's third mastery layer being load-bearing for the best play rather
than decorative:

> Toolbox must pilot the machine rather than merely pre-program everything.

It also means the kill cannot be reduced to one button or muscle memory, which
for a class this mechanically deep is worth protecting.

[BLOCKING — was OPEN] **Bucket aftermath.** Vanilla dispensers leave an empty
bucket, and an empty bucket dispensed at a lava source **picks the lava back
up**. So if the emptied bucket remains first in order, the second `Di` undoes
the first and the combo reverses itself. Candidates: eject the emptied bucket
so the next `Di` reaches the water; leave it and require a third `Di`; or leave
it deliberately. This is no longer a loose end — it decides whether the
technique exists.

Worth keeping either way: **`Di(empty bucket)` is a lava-removal tool**, which
is real vanilla behaviour and gives Toolbox an answer to somebody else's lava.

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
