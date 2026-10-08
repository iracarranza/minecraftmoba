# Modular path authoring, drawing on vanilla village streets

**Review recorded 8 October 2026, from a review performed 2 October.** No
implementation. Prototype/test throughout; nothing here is an established
decision.

**This is a transcription.** The review was carried out in a session and its
findings lived only in that transcript. It is written down here because an
unrecorded review is a review that will be performed again — and because the
[route authoring audit](2026-09-22-route-authoring-audit.md) it builds on is in
the repository, so its successor should be too.

## Why this exists: the Routes are messy, and measurably so

The 22 September audit measured the frozen map's authored Routes:

| step between adjacent route columns | share |
|---|---|
| 0 blocks (walkable) | 59.2% |
| 1 block (requires a jump) | **33.1%** |
| 2 blocks | 4.4% |
| 3–34 blocks | **3.2%** |

**40.8% of steps required a jump and 7.6% could not be climbed at all**, with
individual steps of 16, 18 and 34 blocks where a corridor ran over a cliff edge
and laid path blocks down the face. Each column independently took its own
surface height, so the Route inherited terrain *noise* as well as terrain shape.

**That specific defect is fixed.** `routes.py` now fits a median-filtered,
step-limited `walkable_profile`, and columns take their height from the profile
rather than from themselves. What remains is the part the audit did not reach:
the corridor is still **uniform everywhere**, with no vocabulary for what a
path should *be* at a given place.

## What vanilla villages actually do

Inspected against the installed Java 1.21.11 assets.

- **Plains streets use separate straight and corner templates**, assembled by
  the jigsaw system rather than drawn as a line.
- Those templates use **`terrain_matching`**, so a piece adapts to the ground it
  lands on instead of flattening it.
- **Processor rules substitute materials by context** — planks replace path
  blocks where a street meets water, which is a rule about the *situation*
  rather than about the route.

The transferable idea is not the jigsaw machinery. It is that a street is
**assembled from pieces that declare what they can connect to and what ground
they tolerate**, rather than carved as a continuous surface.

## The proposed vocabulary

Straights, bends, junctions, stairs, landings, bridges and structure entrances
— each declaring:

- **width**
- **elevation connections** (what it can join at each end, and at what
  height delta)
- **terrain tolerances** (the ground it will accept without surgery)

And a pipeline:

> choose corridor → fit elevation → select compatible pieces → validate
> connections → preview → **measure traversal after placement**

The last step is the one vanilla does not need and this project does. Village
assembly guarantees pieces fit each other; it guarantees nothing about whether
the finished street *connects the places it was meant to* or improves getting
between them. That remains ours to check, and the audit exists because it was
not being checked.

## Two distinctions that must survive

**A physical path is not a recognized Route.** Placing a trail creates world
geometry. Infrastructure recognition follows the demonstration rules in
[`infrastructure.md`](../../infrastructure.md), and authoring a path must not
quietly confer recognition on it.

**Stairs are authored geometry; grading is not.** The profile makes a steep run
*walkable*; it does not build treads. Those are different jobs, and the
vocabulary above is where the second one would live.

## Recorded defect, now fixed

The review noted that `routes.py`'s report still claimed grading was absent.
It did: `not_covered` carried *"stairs or grading on steep steps; the corridor
follows terrain"*, which bundled three claims of which two had become false —
the corridor follows the **profile**, and a steep run **is** graded.

Corrected on 8 October. The report now separates stairs, which genuinely are
not covered, from grading, which is, and states what the profile guarantees.
Worth noting as a pattern rather than an incident: **the code was fixed and the
document describing it was not**, and nothing connected the two.

## Built 8 October 2026

`terrain_harvest/path_pieces.py`, pure and with no block writing, covering the
three stages the corridor work did not have: **segmentation into pieces**,
**connection validation**, and **traversal measurement**.

Two findings from building it:

- **Slope direction is part of a column's character, not a detail of it.**
  Without it a ridge reads as one unbroken run and the reversal at the top —
  the exact place a landing belongs — is invisible. The first implementation
  had this wrong and produced a single stairs piece climbing and descending.
- **A landing is for a reversal, not for every pair of stairs.** Flat ground at
  the top of a climb already provides the legal join, and inserting one anyway
  would be construction nobody asked for.

The landing is zero-length, occupying the boundary column the two runs share.
Giving it width would move every later piece and the pieces would stop
describing the profile they were cut from.

## Joined to `routes.py`, 8 October 2026

`carve()` now segments the fitted profile, validates the assembly, and records
both the piece counts and any faults per route. Faults are **recorded rather
than raised**: a Route that cannot be assembled cleanly is still the Route the
optimizer chose, and refusing to build it would lose the map rather than the
problem.

**The join changes one behaviour, and it is the one worth changing.** Filling
was unbounded — each column saw only its own deviation, so a corridor crossing
a twenty-block gully raised a twenty-block pillar of coarse dirt in every
column of its width. A run that deep is a *crossing*, and a crossing is decked.
Water was already decked; now so is ground that cannot carry a walker.

The two modules have to agree at exactly one number, and a test pins it:
`treatment()` calls a column constructed above `ASSIMILATE`, and segmentation
calls a run a span at `SPAN_DEVIATION`. **A span begins exactly where
assimilation stops.** If those drift apart a crossing is decked in some columns
and filled in others, which is worse than either alone.

[OPEN] **Bends are not synthesised.** A bend needs the corridor's *waypoints*,
and by the time `carve()` runs the centreline is densified to one column per
step, where almost every column differs in direction from its neighbour.
Generating bends from that would label the whole path a bend. The caller holds
the geometry that would answer this.

## Terrain tolerance, 8 October 2026

The `terrain_matching` half. Each piece declares what ground it will accept,
and the asymmetry turned out to be the content:

> **Grounded pieces are limited by vertical deviation. A spanning piece is
> limited by horizontal length.**

A straight sitting three blocks above the ground is the wrong piece whatever
its length. A bridge is high *by definition* — height says nothing about
whether it is the right piece, and **length says everything**. A sixty-column
deck is not a crossing, it is a viaduct, and it almost always means the
centreline was routed through something it should have gone around. So
`bridge` has no vertical tolerance and a `MAX_SPAN` instead, and exceeding it
reports a fault **about the corridor rather than about the bridge**.

A single tolerance field would have hidden that the two are different
quantities.

**Terrain faults are reported apart from connection faults**, because a path
can be perfectly assembled and resting on nothing; one combined verdict would
make "sound" mean two different things.

Most of this passes by construction, since segmentation derives pieces from
the same numbers. It earns its place on what segmentation cannot see: the
**landing**, which is synthesised at a slope reversal and never checked against
the ground beneath it — on a ridge standing over a drop, this is the only thing
that would notice — and any piece supplied by a caller rather than cut from a
profile.

**Unmeasured columns are counted, not blamed.** An unknown is not a defect, and
folding it into the faults would make a well-built path over unsurveyed ground
indistinguishable from a badly built one.

## Structure entrances, 8 October 2026

The `portal` end kind had been in the connection table since the vocabulary was
written and nothing produced one. Building the piece exposed a gap rather than
adding a refinement:

> **`traverse()` measures the walk *along* a corridor. Nothing measured the
> step at the end of it.**

So a Route could report a flawless traversal — zero jumps, nothing unclimbable
— and finish at a wall five blocks under the door, with the corridor's own
statistics calling that a success. That is the "awkward, buried or floating
build" the 2 October review warned a good-looking placement could still
produce, and it was invisible to every measurement the project had.

An entrance has **tolerance zero**, alone among the pieces. That is the reason
it is a piece at all rather than the last straight: a straight may float two
blocks over its ground, but a doorway is a specific height, and "close enough"
is exactly how a path ends above a door.

The check runs **from both sides**. An entrance with no threshold is an
entrance to nothing; a threshold with no entrance piece is the same defect read
from the other side — the path was built, the building is there, and they do
not meet. Checking only the pieces would miss the second case entirely.

Failures name their direction, because a path above its door needs lowering and
a path below it needs raising, and the fix differs.

### A limit, recorded rather than special-cased

Two entrances back to back **pass** connection validation: `portal` meets
`flat`, because that is also how a path *leaves* a building, and the end-kind
vocabulary cannot tell an arrival from a departure. Encoding that difference
would mean giving pieces **roles** rather than ends, which is the special-casing
the table's shortness exists to avoid. `entrance_faults`, which knows about
thresholds, is where the information to object actually lives.

## Where this would start

The review's recommendation, preserved: the strongest first prototype is a
**disposable lab experiment** combining one structure, its entrance transition
and a path to a chosen endpoint, with before-and-after terrain and team-access
measurements — exercising both the authoring question and the symmetry question
without expanding the authorer to every element first.

The lab now has the authoring and undo workflow that experiment needs
(`LabAuthoring`, `LabUndo`, `LAB_AUTHORING.md`), which did not exist when this
review was written.

## Open

- Whether piece selection is worth its complexity against simply extending the
  profile-fitting with a few treatments.
- What a "connection" is, precisely, for validation purposes.
- Whether terrain tolerance is a per-piece declaration or a single global
  budget, as the structure authorer's cut/fill limit currently is.
- How a path interacts with the authored sites its corridor crosses; today
  crossings are recorded rather than avoided, deliberately.
