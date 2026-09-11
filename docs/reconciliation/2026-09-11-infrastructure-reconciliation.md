# Reconciliation — infrastructure correction handoff

Date: 2026-09-11 (fourth integration; handoff dated "September 2026")

## Source

Minecraft MOBA Infrastructure Reconciliation / Correction Handoff `[WR]`,
sections 1–15. It responds specifically to the four conflicts recorded in
`docs/reconciliation/2026-09-11-wams-clarification.md`.

`[WR]` does **not** supersede `[WB]` wholesale. Several `[WC]` ideas are
retained; some `[WC]` conclusions are corrected against the earlier model.

## The four conflicts, now decided

**1. Supply Line start point — explicit start REINSTATED.** `[WC]`'s
Source-designation grammar stands, and reverses `[WB]`'s "do not prematurely
require marking an origin container first." The reversal is intentional: the
origin-first model was removed because designating an arbitrary storage
container was an unsolved interaction problem, and the archetype-native anchor
model solves it. Routes and Supply Lines now share one establishment grammar,
Banner Start → traversal → Banner End and Copper Chest Source → logistical
transport → Copper Chest Destination. Retrospective inference of the origin is
no longer preferred. Exact designate input remains `[OPEN]`; literal vanilla
right-click is deliberately not required.

**2. Flow Weight — property of the logistical method.** Authored per
vehicle/method, representing carrying efficiency and capacity; items per pulse.
Not inferred from one observed cargo sample, not a player-selected
infrastructure stat. Exact values `[OPEN]`.

**3. Item Rate — emergent, player authorship removed.** Item Rate reflects the
method's actual movement and delivery performance over the established
connection: carrier speed, connection length, traversability, terrain, rails,
paths, bridges, tunnels, shortcuts, movement-enhancing infrastructure,
method-specific traversal. The journey is not a benchmark run for estimating
stats; it is required because the method must actually establish the
connection. Environmental improvement raises Item Rate naturally, with no
"+X% Item Rate" rules. Distance is retained but only indirectly, through trip
duration, so no separate distance penalty is imposed.

This corrects `[WC]` §8's "derive both from demonstrated performance": the
demonstration does **not** determine both. The method carries its Flow Weight;
its actual movement determines the delivery frequency.

**4. Corridor vs endpoints — split by method category, not universal.**
Endpoints suffice as the player-intent model for **self-directed** methods
(Skeleton Crew worker, hypothetical bat/parrot carrier, future aquatic
carrier, other autonomous summoned workers). **Intrinsically directional,
path-authored** methods (rails, water channels) carry the corridor as part of
the logistical method, because the construction already encodes direction and
topology. Corridor information is therefore not globally deleted from Supply
Lines. The 11 September player-authored corridor section is scope-narrowed
rather than superseded. Output is normalized through Flow Weight and Item Rate
rather than forcing identical topology.

**5. Class ability firewall — reaffirmed.** Reinstating explicit Source
designation does not put Supply Line vocabulary inside class abilities. Class
creates behaviour; the system recognizes infrastructure. Skeleton Crew knows
"move cargo from this Source to this Destination," never "create Supply Line #4."
Violate only intentionally, for a class whose fantasy is explicitly
manipulating already-recognized infrastructure.

## Also integrated

- Revised Supply Line definition: persistent infrastructure that simulates
  repeated deliveries by the logistical method that established a connection
  between two designated Copper Chests.
- Execution: at each pulse transfer up to Flow Weight from Source to
  Destination; pulse interval represents Item Rate. After recognition the game
  does not keep physically simulating every carrier trip. Invisible Copper
  Golems are explicitly recorded as presentation only, not the authoritative
  clock — consistent with the existing datapack prototype finding.
- Shared connection grammar: start → actual world behaviour → end → recognized
  connection, tabulated for both connection infrastructures.
- Skeleton Crew Logistics baseline in `classes.md`: self-directed logistical
  workers with authored Flow Weight plus real movement; upgrades alter Crew
  behaviour rather than naming Supply Line stats.

## Left [OPEN] as instructed

Route corridor retention is explicitly **not decided**. Banners settle endpoint
legibility only; how much traversal geometry a Route keeps stays open, and is
not resolved by the fact that self-directed carriers need only endpoints.
Also still open: exact designate input, Flow Weight values, Item Rate units and
normalization, eligible cargo, arbitrary-container transfer feasibility, and
Route bidirectionality.

## Destinations

| `[WR]` § | Destination |
| --- | --- |
| 1–2 Explicit start | `infrastructure.md` Supply Lines, Copper Chest anchors; manuscript §6.21 |
| 3 Journey is not a stat test | `infrastructure.md` Supply Lines; manuscript §6.21.3 |
| 4–6 Flow Weight, Item Rate, distance | `infrastructure.md` Transport specific performance; manuscript §6.21.3 |
| 7 Revised definition | `infrastructure.md` Supply Lines; manuscript §6.21 |
| 8 Execution | `infrastructure.md` Recognized Supply Line execution; manuscript §6.21.4 |
| 9 Corridor vs endpoints | `infrastructure.md` Self-directed logistical entities, Player-authored corridor; manuscript §6.21.1 |
| 10 Route caution | `infrastructure.md` Banner endpoints; manuscript §6.21.6 |
| 11–12 Firewall, Skeleton Crew | `infrastructure.md` Class ability and infrastructure firewall; `classes.md`; manuscript §6.21.5 |
| 13, 15 Grammar and core principle | `infrastructure.md` Shared connection grammar; manuscript §6.21.6 |
| 14 Resolutions | this record |
