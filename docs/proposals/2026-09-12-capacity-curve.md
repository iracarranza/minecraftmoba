# Proposal — capacity progression curve rework

Status: **NEW WORKING MODEL / NOT CANON.** Date: 2026-09-12.

The source handoff states outright: *"DO NOT MODIFY REPOSITORY DOCUMENTATION AS
IF THESE NUMBERS ARE FINAL UNTIL THE CURVE IS REVIEWED."* This file therefore
records the proposal in full and changes nothing else. Canonical capacity
progression remains the schedule in
[classes.md](../../classes.md#progression-schedule), and the implemented curve
remains Chunk 2 in
[implementation/datapacks/README.md](../../implementation/datapacks/README.md).
Neither has been altered.

## Purpose

Rework universal Health / Hunger / Inventory progression so that the Level 1
player begins deliberately constrained, universal progression approaches normal
Minecraft capability by the late midgame, the three capacities need not share a
growth cadence, specialization accelerates or completes a chosen capacity,
repeated specialization eventually changes *how* a resource behaves rather than
pushing its number past vanilla norms, and vanilla-native representation is
preferred — in particular, no displayed Hunger above 20.

If adopted, this removes the need for the general Hunger-above-20 exhaustion
conversion system.

## Working universal endpoints

| | Lv1 | Universal endpoint | Vanilla |
| --- | ---: | ---: | ---: |
| Health | 8 | 18 | 20 |
| Hunger | 9 | 18 | 20 |
| Inventory | 6 | 36 | 36 |

Universal progression fully restores ordinary Inventory capacity; universal
Health and Hunger stop slightly below it, and specialization is what reaches 20.
Further dedicated specialization culminates in a qualitative Mastery rather than
raw capacity above 20.

8 starting Health is particularly preliminary and needs combat-breakpoint
testing. 9 starting Hunger stays close to the current model because early food
and expedition constraint already matters to map design. 6 starting Inventory is
unchanged.

## Universal curves

**Health** — frequent, small increases. +1 at Lv2, 4, 7, 8, 9, 11, 13, 14, 15, 17.

**Hunger** — less frequent, larger steps, alternating **+1 / +2** at Lv2, 7, 9,
13, 15, 17 (so +1 at Lv2, +2 at Lv7, +1 at Lv9, +2 at Lv13, +1 at Lv15, +2 at
Lv17). Six events, +9 total, endpoint 18, exactly as the source handoff
specified — but every intermediate value is an integer. See
[Hunger cadence: resolved](#hunger-cadence-resolved) below; the handoff's
original +1.5 per event is superseded.

**Inventory** — unchanged shape. +3 at Lv2, 4, 7, 8, 9, 11, 13, 14, 15, 16.

Combined, unspecialized:

| Lv | Health | Hunger | Inventory |
| ---: | ---: | ---: | ---: |
| 1 | 8 | 9 | 6 |
| 2 | 9 | 10 | 9 |
| 3 | 9 | 10 | 9 |
| 4 | 10 | 10 | 12 |
| 7 | 11 | 12 | 15 |
| 8 | 12 | 12 | 18 |
| 9 | 13 | 13 | 21 |
| 11 | 14 | 13 | 24 |
| 13 | 15 | 15 | 27 |
| 14 | 16 | 15 | 30 |
| 15 | 17 | 16 | 33 |
| 16 | 17 | 16 | 36 |
| 17 | 18 | 18 | 36 |

Each capacity gets a distinct rhythm: Health frequent +1, Hunger less frequent
+1/+2, Inventory frequent +3 to its hard maximum.

## Hunger cadence: resolved

**Decided 12 September 2026 by the design owner.** The handoff's +1.5 per event
is replaced by the alternating +1/+2 cadence above. Start, endpoint, event
levels and event count are unchanged; only the per-event split moves, by half a
food point at Lv2, Lv9 and Lv15.

The reason is representational. The Hunger unit in this project is the food
point, and a food point is already **half a drumstick** — `classes.md` states
"9 Hunger = 4.5 hunger icons." A half food point is therefore a *quarter*
drumstick. Vanilla's food renderer selects one of three sprites per icon — full,
half, empty — from an integer `foodLevel`, and a resource pack can only
retexture those three, not add a fourth quantization step. The values 10.5, 13.5
and 16.5 food points are 5.25, 6.75 and 8.25 drumsticks and cannot be drawn.

Two alternatives were considered and rejected. Storing the fractional design
value while flooring the enforced value (which `moba_capfood` / `moba_fenf`
already supports) makes the Lv2, Lv9 and Lv15 rewards invisible until the next
event tops them up. Spending the half point as exhaustion efficiency instead is
coherent, and uses the same currency as Hunger Mastery, but puts two mechanisms
in every Hunger event and partly spends the Mastery's identity early.

Worth stating plainly: vanilla has no maximum-hunger attribute, so a maximum is
only ever observable as the point at which the bar stops refilling. A fractional
maximum is not merely hard to draw — it has no observable state at all.

## Consequence for the Lv3 expedition breakpoint

`maps.md` → *Hunger as expedition capacity* measures the Level 3 Hunger choice as
reserve above the vanilla 6-food sprint cutoff, using a fixture of 10 food
unspecialized and 12 specialized: 4 versus 6 points of reserve, reported as
**+50%** sprint-capable reserve and "a meaningful early expedition/logistics
choice."

At 4 exhaustion per food point and vanilla's 0.1 exhaustion per metre sprinted,
one food point is about 40 m of sprint. Expeditions are evaluated round-trip
(`maps.md`), so that is roughly 20 m of radius.

| Lv3 state | Food, unspec / spec | Reserve | Specialization gain |
| --- | --- | --- | ---: |
| `maps.md` prototype fixture | 10 / 12 | 4 / 6 | +50% |
| this curve with +1.5 events | 10.5 / 11.5 | 4.5 / 5.5 | +22% |
| this curve as adopted (+1/+2) | 10 / 11 | 4 / 5 | +25% |

The integer cadence costs half a food point at this breakpoint, about 40 m of
sprint one-way. The larger disturbance comes from the curve itself — Hunger
specialization I dropping from +2 to +1, and the Lv1 base from 10 to 9 — not
from the cadence fix, and the integer cadence is marginally the better of the
two here because lowering the unspecialized baseline restores a little of the
specialization's relative value.

One convenient property: with the adopted cadence the unspecialized Lv3 player
sits at exactly 10 food and 4 reserve, which is the prototype's own L1 test
state, so one side of that comparison needs no new testing.

[OPEN] `maps.md` labels its Hunger values prototype test settings, so this curve
may change them — but the +50% figure is *measured evidence* that the Lv3 choice
is meaningful, and this curve invalidates that measurement. Whether +25% is
still a meaningful early choice is an open question requiring a new test.
Neither this curve nor the cadence fix establishes that it is.

## Specialization architecture

Specialization levels stay at Lv3 / Lv18 / Lv24, but the flat "+2 / +2 / +6 at
every milestone" model is reconsidered in favour of stages: Capacity I is
numerical specialization or acceleration, Capacity II is completion or deeper
specialization, Capacity III is a qualitative **Mastery**.

**Health.** I and II give +1 maximum Health each, so 18 → 19 → 20. III does not
raise maximum Health above 20 and instead unlocks **Health Mastery**: current
spitball is regenerating out-of-combat Absorption — after some period out of
combat the player regenerates Absorption up to a limited amount, giving
renewable readiness between engagements rather than more permanent raw Health.
Delay, amount, rate and the definition of combat state are all open. Trajectory:
fragility → normal durability → renewable readiness.

**Hunger.** I and II give +1 maximum Hunger each, so 18 → 19 → 20. III does not
raise displayed or effective maximum Hunger beyond 20 and instead unlocks
**Hunger Mastery**: current candidate is exhaustion efficiency at incoming
exhaustion × 0.875, i.e. 12.5% less exhaustion accumulation. Trajectory: short
expedition range → normal endurance → metabolic efficiency.

Why 0.875: under the previous curve, 22 effective Hunger meant 22 − 6 = 16 points
of usable reserve above the sprint cutoff, against 20 − 6 = 14 for a normal bar;
14 / 16 = 0.875. So ×0.875 exhaustion gives a 20-Hunger bar approximately the
endurance previously represented by 22 effective Hunger. This is a balance
reference, not a locked value.

[OPEN] Deliberately unresolved: the interaction between exhaustion efficiency and
the Hunger cost of natural health regeneration. Do not solve it as part of this
curve revision.

**Inventory.** Its ceiling is reached universally, so it behaves differently.
Inventory I remains +6 slots capped at 36; taken at Lv3 it reaches 36 at Lv14
instead of Lv16, which keeps its identity as an early acceleration benefit:

| Lv | 3 | 4 | 7 | 8 | 9 | 11 | 13 | 14 |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| Inventory I from Lv3 | 15 | 18 | 21 | 24 | 27 | 30 | 33 | 36 |

By Lv18 universal Inventory has already reached 36, so later Inventory
investment should not try to exceed it. Current candidates: Inventory II gives
about +2.5% movement speed, Inventory III / **Inventory Mastery** another ~+2.5%,
for roughly +5% total. Trajectory: tiny carrying capacity → complete inventory
sooner → more mobility while operating at complete carrying capacity.

## Relation to universal movement

A preliminary universal movement breakpoint of about +10% base speed at Lv16 is
already under consideration, landing at roughly the same point universal
Inventory reaches 36. If retained, Inventory specialization movement should be
additional but smaller: an ordinary Lv16+ player at 110%, Inventory II ~112.5%,
Inventory III ~115%. The universal increase means the population gets faster as
characters mature; the specialization increase is a modest logistical advantage
for a player dedicated to carrying capacity.

Do **not** make Inventory speed depend on how full the inventory is — that
creates junk-filling incentives and unstable movement behaviour.

## What this would supersede if adopted

**The Inventory overflow conversion.** The current rule replacing a capped
Inventory specialization with +0.5 Health and +0.5 Hunger
([classes.md](../../classes.md#progression-schedule)) exists to keep Lv18/Lv24
Inventory selections live after universal Inventory reaches 36. Staged
specialization solves that directly, so the patch is probably unnecessary — but
it is **not** superseded yet and remains canon until this curve is reviewed.

**The Hunger-above-20 model.** Effective Hunger above 20 represented through
`exhaustion multiplier = 14 / (H − 6)` should not be canonized as general Hunger
progression if this curve is adopted, since the curve exists specifically to
avoid needing Hunger above 20. The mathematics remain useful as a balance
reference for Hunger Mastery. It also remains canon until review.

## Status

Working / strong candidates: capacities should have distinct growth cadences;
Hunger keeps a similar starting value; Inventory remains 6 → 36; Inventory I
keeps its acceleration identity; Health and Hunger reach 20 through
specialization; maximum repeated investment culminates in a qualitative Mastery;
Hunger Mastery explores exhaustion efficiency; Health Mastery explores
regenerating out-of-combat Absorption; Inventory Mastery explores movement speed
above the universal increase.

Preliminary numbers: Lv1 Health 8; universal Health and Hunger endpoints 18;
Hunger +1/+2 alternating per event (settled); Health/Hunger specialization I and
II +1 each; Hunger
Mastery ×0.875 exhaustion; universal Lv16 movement +10%; Inventory II +2.5%
movement; Inventory III a further +2.5%.

[OPEN] Whether Lv1 Health should be 8; exact Health and Hunger growth event
levels; exact
Health Mastery behaviour and numbers; exact Hunger Mastery exhaustion value; the
regeneration interaction; exact Inventory II/III movement values; whether
Inventory II counts as mastery or wants an intermediate effect; whether +10%
universal movement at Lv16 remains the final movement breakpoint.

## Implementation notes, if reviewed and adopted

These are consequences for Chunk 2, not requests to change it now.

- Hunger values are now integral at every level, so no rounding rule, no
  half-point accounting and no change to the `moba_capfood` / `moba_fenf` split
  are required. This was the curve's one presentation blocker and it is closed.
- An 18-point universal Health endpoint is reachable natively; `max_health` is
  already set by attribute. No new mechanism is needed.
- The Masteries are all new mechanisms rather than capacity numbers: Absorption
  regeneration needs an out-of-combat state machine, exhaustion efficiency needs
  exhaustion to be readable or interceptable, and the movement bonuses need a
  movement-speed attribute modifier that coexists with the universal Lv16 one.
  Each is its own feasibility question.
