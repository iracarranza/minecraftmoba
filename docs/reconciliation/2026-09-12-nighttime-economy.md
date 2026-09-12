# Reconciliation — nighttime and combat economy handoff

Date: 2026-09-12

## Sources

1. **[WN]** WAMS Handoff — Nighttime, Combat Economy, Regenerative Sources, and
   World-System Mapping, sections 1–17. Working design direction; exact
   numbers and tables explicitly not finalized.
2. **[CP]** Minecraft MOBA Capacity Progression Curve Handoff, sections 1–14.
   Explicitly **NOT CANON**, with a standing instruction not to modify
   repository documentation as if its numbers are final.

The two were supplied together but are handled differently: [WN] is integrated
as Working direction, [CP] is recorded as a proposal and changes nothing.

## [WN] — integrated

| Section | Destination |
| --- | --- |
| 1 Day/night economic foundation | `objectives.md` §17A; manuscript §3.5 |
| 2 Infrastructure at night | `infrastructure.md` → Infrastructure at night; manuscript §3.5 |
| 3 Combat's economic role | `objectives.md` §17A; `classes.md` → Combat; manuscript §8.3 |
| 4 Nighttime Worksites | `objectives.md` §11 → Nighttime activation; manuscript §7.10 |
| 5 Worksite / archetype mapping | `objectives.md` §17C; manuscript §7.11 |
| 6 Regenerative resource system | `objectives.md` §17B; `maps.md` → depth gradient; manuscript §3.3 |
| 7–8 Crops, animal populations | `maps.md` → depth gradient |
| 9–11 Mob Swarms, depth, day/night | `objectives.md` §17B; `maps.md`; manuscript §3.3 |
| 12 Distance × day/night field | `objectives.md` §17B; manuscript §3.3 |
| 13 Two nighttime opportunity systems | `objectives.md` §17A; manuscript §7.10 |
| 14 Why no direct PvP bonus | `objectives.md` §17A; manuscript §3.5 |
| 15 Archetype economy | `objectives.md` §17C; manuscript §7.11 |
| 16 Unresolved questions | preserved as `[OPEN]` in place |
| 17 XP branch dependency | `objectives.md` §9; manuscript §5.7 |

### Ownership resolved

Manuscript §3.5 carried `[NEEDS CANONICAL OWNER]` on day/night. That is now
assigned: **`objectives.md` §17A owns the day/night economy**, and
**`infrastructure.md` owns the nighttime infrastructure penalty**. `maps.md`
owns the spatial gradient the regenerative buckets are generated against.

### Narrowed, not answered

`infrastructure.md`'s standing `[OPEN]` on Combat's Level 6 economic system is
narrowed: Combat should not require a dedicated Combat infrastructure system,
its economic role is securing value under threat, and its regenerative world
system is Mob Swarms. What, if anything, Combat receives at Level 6 is still
open. The `[OPEN]` on infrastructure night behaviour in the integration section
now points at the new night section for direction while keeping magnitudes open.

### Deliberately not derived

No XP values, no nighttime multipliers, no penalty percentages, no Worksite
counts, no distance tables, no Swarm compositions. [WN] §17 places XP
calibration downstream of these decisions, and that dependency is recorded in
`objectives.md` §9 and manuscript §5.7 rather than acted on.

The manuscript's existing illustrative 75–90% nighttime effectiveness range
remains marked as not a selected multiplier; [WN] does not select one.

## [CP] — recorded as a proposal, nothing changed

Full text preserved in `docs/proposals/2026-09-12-capacity-curve.md`. Pointers
added, each labelled `[PROPOSED — NOT CANON]`, in `classes.md` (capacity
progression), `implementation/datapacks/README.md` (implemented capacity
values), and manuscript §5.4.

Canon **unchanged** and explicitly still in force:

- 9 Health / 9 Hunger / 6 slots at Level 1, with +1 / +1 / +3 universal growth;
- +2 / +2 / +6 specialization at Levels 3, 18 and 24;
- the Inventory overflow conversion to +0.5 Health / +0.5 effective Hunger;
- effective Hunger above 20 via `exhaustion multiplier = 14 / (H − 6)`;
- the implemented Chunk 2 curve in the datapack.

[CP] says the last two are *probably* superseded if it is adopted. They are not
superseded now.

### One point settled within the proposal, same day

The owner settled the Hunger cadence: [CP]'s +1.5 per event is replaced by an
alternating **+1/+2** with the same start (9), endpoint (18), event levels and
event count. A half food point is a quarter drumstick — the project's Hunger
unit is the food point and a food point is already half an icon — and vanilla's
renderer has only full/half/empty sprites driven by an integer `foodLevel`, so
10.5 / 13.5 / 16.5 cannot be drawn and a fractional maximum has no observable
state. This settles a value *inside a proposal that remains unadopted*; it
promotes nothing to canon, and every current capacity value listed above is
still in force.

Recorded with it: the Level 3 expedition breakpoint in `maps.md` measured a
Hunger specialization as +50% sprint-capable reserve on a 10/12-food fixture.
[CP] as written makes that +22% and the adopted cadence makes it +25%; the
disturbance comes mostly from specialization I dropping to +1 and the Lv1 base
to 9, not from the cadence fix. That measurement is invalidated by the curve and
wants re-testing. This is an observation, not an acceptance bound.

The remaining item for whoever reviews the curve is the three Masteries, which
are new mechanisms rather than capacity numbers, each wanting its own
feasibility pass. The half-point representation question is closed.

## Incidental

A stray `O` prefix on the second `# 20. Relationship to Infrastructure` heading
in `objectives.md` was corrected. The duplicated section body around it is
pre-existing and was left alone.
