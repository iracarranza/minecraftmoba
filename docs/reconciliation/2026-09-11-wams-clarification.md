# Reconciliation — WAMS infrastructure clarification

Date: 2026-09-11 (third integration; handoff dated "September 2026")

## Source

WAMS Infrastructure Clarification Handoff `[WC]`, sections 1–18, from the
Skeleton Crew / Logistics discussion.

## Four recorded conflicts — NOT silently resolved

These are tensions with canon committed on 10–11 September. All are marked
`[CONFLICT]` in place and left for the owner to decide.

**1. Supply Line source designation.** `[WC]` §5 proposes: enter Infrastructure
Mode, **designate Copper Chest A as Source**, demonstrate movement, designate
Copper Chest B as Destination. `[WB]` (11 Sep) explicitly superseded the
origin-first model and instructed *"do not prematurely require marking an
origin container first"* and *"do not require every class ability to contain
explicit Supply Line interface."* The proposals are not identical — `[WC]`
anchors designation to a legible block type and still requires demonstrated
delivery — but it does reinstate explicit Source designation. **This is the
most significant conflict in this handoff.**

**2. Flow Weight authored vs derived.** `[WB]` states Flow Weight is an
*authored* property of the transport method, with only Item Rate derived.
`[WC]` §8 states both should *preferably be derived from demonstrated
logistical performance*, not assigned from class identity. The Flow Weight /
Item Rate *distinction* is unchanged and agreed; their *origin* is disputed.

**3. Player-authored corridor vs endpoints-only.** `[WB]` has the player
author an intended corridor which the carrier resolves. `[WC]` §6 states the
player need not specify any path and that Source→Destination suffices for
self-directed entities. Both agree carriers resolve traversal in their own
movement vocabulary. Recorded as a narrowing for self-directed entities;
corridor authoring remains documented for player-guided methods.

**4. Class abilities and explicit Supply Line interface.** Follows from 1.

## Integrated without conflict

| `[WC]` § | Destination |
| --- | --- |
| 1 Designation and evidence model | `infrastructure.md`; manuscript §6.17 |
| 2 Place vs connection | §6.17.1 — framed as an organizing view of the existing extent/relation axis, not a replacement |
| 3 Archetype-native anchors | §6.17.2; pointers added in `classes.md` |
| 4 Banner endpoints | §6.20 |
| 5 Copper Chest anchors | §6.21 |
| 6 Self-directed entities | §6.21.1 |
| 7 Pre-infrastructure Logistics | §6.21.2 |
| 8 Derived Flow Weight / Item Rate | §6.21.3 |
| 9 Execution model | §6.21.4 |
| 10 Construct designation | §6.18 |
| 11–13 Development Weight, normalization, dynamic | §6.19 |
| 14–16 Summary and rationale | distributed |
| 17 Feasibility | §6.22 |

## Preserved

The progression-gated philosophy and the chain *player action → demonstrated
capability → persistent capability* are retained verbatim in intent. The
four-identity constitutive/facilitative table is kept; place/connection is
layered onto it rather than replacing it. Structural Integrity, benefit
symmetry, payback horizon, control windows, authorship/recognition/control,
capture and team-relative integration are untouched.

## Left [OPEN] as instructed

Exact designate interaction (right-click deliberately not canonized); region
representation for Constructs; Development Weight values and entity
normalization formula; recalculation frequency; Route bidirectionality,
retained path geometry, and whether alternate demonstrations improve a Route;
arbitrary-container transfer feasibility; inventory-origin proof.

## Implementation note

`[WC]` §17's caution against relying on mob AI as the authoritative Supply Line
clock is consistent with the prototype finding already recorded in
`implementation/datapacks/README.md`: vanilla cannot transplant one mob's
navigation onto another. The preferred direct-inventory-transfer model avoids
that dependency entirely.
