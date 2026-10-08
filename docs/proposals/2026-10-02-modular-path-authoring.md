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
