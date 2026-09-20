# System status audit — 20 September 2026

Every system this project has touched, sorted by **what it would take to build
it**, not by how interesting it is. The point of the sort is that the first
band is work and everything below it is a decision.

Nine bands, including four beyond the seven originally proposed. The extra four
turned out to be where most of the real risk lives.

---

## A. Buildable now — canon fully specifies it

No design needed. Canon enumerates the rule; only balance values are open.

| System | State |
| --- | --- |
| Restricted material categories | **Implemented today.** Membership is enumerated, including the derivative rule. |
| Automatic task cadence (L4 Efficiency I, L7 Yield I) | **Implemented today.** Config declared it and nothing read it. |
| 6-food sprint cutoff | **Needs nothing.** It is vanilla behaviour; reimplementing it would be a mistake. |

## B. Buildable once one detail is supplied

The mechanism is settled; a single value or choice blocks it.

| System | Missing |
| --- | --- |
| Recall destination | Where a team recalls to. Presumably its Aether Fountain, but that is map selection. |
| Durability curve | The tier table. 15.1.1 exists as a NON-CANON candidate, ships commented out. |
| XP from resource attainment | objectives.md states the rule outright; per-material values are open. |
| Hub portal destinations | Which maps are offered. Ships empty on purpose. |
| Construction Block XP premium | A balance target, explicitly not canon. |
| Class × Infrastructure matrix | Eligibility is authored per class and mostly unwritten. |

## C. Outlined but not designed

Direction exists; the mechanism does not.

- **Monster Combat branch** — named as the Level 6 alternative, content `[OPEN]`.
- **Ability upgrade branches** — three mutually exclusive upgrades per ability, deliberately undesigned.
- **Shared Worksite opportunity advancement** — earning it is defined, improving it is not.
- **Route effect scaling** — slots, length, width, branching, strength are listed as axes with no curve.
- **Day/night Swarm composition** — the axis is canon, the tables are not.
- **Regenerative depth gradient** — forbidden to invent, so it stays outlined until measured.

## D. Explicitly outstanding

Marked `[OPEN]` in canon. Counted: **infrastructure.md 40, classes.md 45, manuscript 127, objectives.md 6, maps.md 2.**

The largest clusters are Flow Weight versus Capacity authority, Route geometry
retention and bidirectionality, Aether Fountain disable conditions, and
per-recipe Primary Material mapping.

## E. Implicitly outstanding

Nobody wrote `[OPEN]`, but the system cannot work as described.

- **Level-up reward choice content** — the GUI framework exists and ships an empty table.
- **Death and respawn policy for progression state** — capacity, task tiers and pending choices have no defined death behaviour. `deathInMode` covers mode state only.
- **Anti-farming** — XP for legitimate attainment implies a definition of illegitimate.
- **Provenance read ordering** — solved and documented in SPEC 7.1, but every future consumer inherits it.
- **Offhand contention** — resolved by the tome, and any future offhand feature reopens it.

## F. Belongs in a MOBA, entirely unmentioned

Searched the canonical documents. Occurrence counts:

| System | Mentions |
| --- | ---: |
| match start / match end | **0** |
| team assignment | **0** |
| spectating | **0** |
| queue | **0** |
| lobby | **0** |
| shop | **0** |
| fog of war | **0** |
| surrender / early end | **0** |
| comeback / bounty mechanics | **0** |
| ping (designed in conversation, not in canon) | 29 |
| vision | 20 |

**No match lifecycle exists at all.** Every system built so far accumulates
state with nothing to start, end or reset it. That is the single largest gap,
and it is structural rather than a missing value.

## G. Superseded but still present

`[HISTORICAL]` blocks: classes.md 7, infrastructure.md 7. Retained deliberately
for traceability, with a standing instruction that they must not be revived
silently. The live hazard is the Flow Weight note in infrastructure.md, which
predates Capacity superseding it and now sits beside a section declaring the
supersession.

## H. Decided but not implementable as written

From the capability audit. Three `[TECHNICAL RISK]` items in the Waxer kit
alone: recipes cannot be class-gated, sealed containers, and Amber against
players. These are not open questions — they are settled designs the engine
refuses.

## I. Implemented but unvalidated

Built, compiles, never observed working.

- Minimap renderer, recall channel, tome, health displays, hunger regen — all shipped today, none seen in game.
- Level 6 fork and Worksite opportunities — no live scenario.
- Structure builder — eight structures written to a world nobody has walked.
- Durability — ships vanilla, so its effect is untested by construction.

Today alone produced: config that would not parse, a duplicate key that silently
disabled task progression, an inert Efficiency modifier, and a null dereference
on an unenrolled player. Every one compiled. **"Implemented" and "working" are
different claims**, and this band exists to keep them apart.

---

## What the bands imply

Band A is empty again. Band F is the priority: match lifecycle is not a missing
value but a missing frame, and without it the other systems have no lifecycle
to sit inside. Band I is the discipline problem — the gap between building and
verifying is where today's defects lived.
