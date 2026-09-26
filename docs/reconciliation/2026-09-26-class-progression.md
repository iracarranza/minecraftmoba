# Reconciliation — class progression, Growth and Task handoff

Date: 2026-09-26. Repo state examined: `main` at `0a8e77f`.

## Source

Class Progression / Task / Growth handoff, sections I–XXVIII, including the
first fully authored Mole Lv0–30 curve. Incorporated as
[`docs/design/CLASS_PROGRESSION_GROWTH_AND_TASK.md`](../design/CLASS_PROGRESSION_GROWTH_AND_TASK.md).

## What the latest codex work already covers

`main` now carries the physical draft built on top of the pre-match spec: a
dedicated draft hall, a six-option map-draft state machine, the 2-3-2-3-3-1 snake
after a coinflip, both phases in one hall, per-player stands, a revealed Type
set, demand-driven allocation, and a pool/foundry the runtime claims from. None
of that is touched here. The progression handoff and the draft work do not
overlap: one decides what a class becomes over thirty levels, the other how a
class is chosen before level one.

One point of contact worth stating: the draft selects **the class and only the
class**, which the progression model independently requires — branches land at
Lv5 and Lv10 and Tasks at Lv4 onward, so there is nothing else for a pre-match
draft to select.

## Superseded

**The universal progression skeleton** in `classes.md` — the Lv1–30 table, its
universal capacity growth, and player-chosen capacity specialization at Levels 3,
18 and 24. Replaced by a Lv0–30 clock in which capacity is class-authored and
automatic (**Growth**, at 3/6/9/12/15/18/21/24/27/30) and player choice moves to
**Task** (at 4/8/12/16/20/24/28, seven allocations across Yield, Efficiency and
Slaying). Marked superseded in place and retained for traceability.

**The 12 September capacity curve proposal** is now moot — it reworked universal
capacity and specialization, and the Growth model removes both. Its one settled
decision survives as a standing constraint: Hunger values must be integral,
because a half food point is a quarter drumstick and vanilla cannot draw one.
Mole's +2 per Growth satisfies it.

**The ability schedule is not superseded.** Passive, A1, A2, A1 branch at 5, A2
branch at 10, Ultimate at 15 is unchanged in substance; the Passive is now
explicitly Lv0.

## Confirmed rather than changed

The handoff asks whether its recalled XP bands match the repository. **They do.**
`classes.md` carries exactly 1.000 / 1.350 / 1.875 / 2.575 / 3.250 across
boundaries 6→7, 12→13, 19→20, 24→25. That open question is closed. The band
*rationale* text did need updating, because it justified boundaries by events the
new clock removes (Efficiency I at Lv4, Yield I at Lv7, infrastructure at Lv6);
it now cites Task I, Growth II and the Lv12 Growth/Task collision instead. The
obsolete linear 80–900 table stays dead.

The handoff's recollection of "a ~385 early-ish XP value" has no source in the
repository and was not reconstructed.

## Resolved by the owner, same day

1. **Mole is Extraction.** "Excavation" is loose usage for the activity; no
   archetype of that name exists. Prior accepted naming stands.
2. **Infrastructure has no universal level.** A class unlocks access when its own
   Growth curve grants it. Level 6 is not a floor, gate or default — it is where
   some curves happen to spend budget. `infrastructure.md`'s breakpoint is marked
   superseded; Infrastructure Mode, per-class eligibility and the four
   recognitions are unaffected.
3. **Gardener's Clip branches are Specimen / Proliferation / Collection.** The
   handoff's "Maturity / Diversity" is inaccurate recall of the same branches.
4. **Kitfighter's Crossbow branch stands.** Equipment exclusions are baseline
   rules that class kits are licensed to break — the exclusion is what gives the
   branch its value, and the same principle is already stated for the offhand.
5. **Spears are accepted**, with Lunge gated behind heavy Task investment rather
   than the implement being excluded. Gating a breaking capability behind a
   commitment threshold keeps the implement's identity and turns an exploit into
   a build. What a spear *is* in the target version stays open as an
   implementation question.

## Originally recorded as [CONFLICT]

1. **Mole's archetypes.** The handoff says "Excavation / Combat"; `classes.md`,
   the kit handoffs, and the handoff's *own* archetype-relationship section all
   say **Extraction**. Whether "Excavation" is a rename or loose usage is the
   owner's call. Flagged in both places.
2. **Infrastructure timing.** `infrastructure.md` records Level 6 as the
   universal infrastructure recognition entry. The Growth model makes timing
   class-authored — Mole at Lv18, Gardener possibly at Lv6 — so Level 6 is at
   most a common case. Flagged at the head of `infrastructure.md`. This is the
   most load-bearing conflict here, since Infrastructure Mode's eligibility rules
   are written against a universal breakpoint.
3. **Gardener's Clip branches.** The handoff names them Maturity / Proliferation
   / Diversity; the recorded kit names them Specimen / Proliferation /
   Collection. Only the middle branch agrees.
4. **Kitfighter's Crossbow branch** depends on crossbows, which the handoff
   excludes as normal equipment. The handoff flags this itself.

## Flagged for verification

The combat equipment direction admits **spears** with baseline Lunge disabled.
Spears are not part of the vanilla equipment set this project's audits were
written against. Before Slaying's spear techniques are built on, confirm what a
spear is here — a vanilla item in the target version, or an authored one.

## Numbers

The handoff's numbers are recorded at the status it gives them. Mole's Health
curve (100 → 320, +22 per Growth) is a **first calibration point**, not a
universal maximum. Exhaustion Efficiency percentages are marked [PROTOTYPE] and
explicitly uncalibrated; the handoff says the cadence matters more than the
magnitudes, and that is preserved. Task fractional magnitudes, technique
Strengths, Route Reach/Projection magnitudes and Growth I–X budgets are all left
[OPEN].

Nothing was calibrated, fitted or invented to fill a gap.

## Next calibration, per the handoff

Build **Gardener** as the counterexample class — infrastructure early where Mole
has it late — then compare both against a highly Combat-centric and a highly
Production-centric class. That comparison is what turns "Growth budget" from a
word into a number.
