# First end-to-end batch: seed to READY, unattended

**24 September 2026.** Eight seeds, one generation target each, 23.8 minutes
wall clock.

## Result

| | |
|---|---|
| PLAYABLE | **1** (seed 3141592) |
| crashed | 1 |
| deepest stage | recognize 3, homebase 2, objectives 1, ready 1 |
| generation | 10.4 min |
| compilation | 5.6 min |
| READY per hour of wall clock | **~2.5** |

The playable map: centre (2368, -1184), chunk bounds [121, 174, -107, -42],
team axis **x**, deviation/relief 0.191, relief 10 blocks, water 0.387, typed
`shattered_coast`. Generation 86.7s, harvest 8.3s, compilation 326.8s.

**This is the first map compiled from a seed with no human choosing the
window.** The previous comparable batch was 29 candidates, 0 playable, 23
rejected on regional shape.

## What the number is and is not

2.5 READY/hour is a **floor, measured on one target per seed**. Prospect finds
a median of 13 targets per seed and 9 inside the symmetry gate, so the obvious
next move is generating more than one of them per scan — the scan is already
paid for. It is also single-process; nothing here is parallel.

It is not a yield to plan a pool against yet. Six of the eight compiler seams
built on 24 September are **not wired into the stages**, and every one of them
rejects on grounds nothing currently checks. Wiring them will lower this number
before anything raises it.

## What each rejection says

**recognize × 3** — all three were `unlabelled` windows. `recognize` bypasses
Default's regional contract only when a window carries a *non-Default* Type,
so an unlabelled window is still judged against Default and always fails. That
is 90 seconds of generation spent on a window no template could have verified.
Prospect should not emit unlabelled targets for generation; it should still
describe them. **Fix identified, not yet applied.**

**homebase × 2** — `SOCKET_NOT_INDEPENDENTLY_ACCEPTABLE`. Real geography
verdicts on genuinely coastal windows.

**objectives × 1** — `NO_ORDERED_OBJECTIVE_LAYOUT`, three stages deeper than
anything the previous batch reached.

**crash × 1** — `AttributeError: 'dict' object has no attribute 'kind'` on
seed 1010101, inside the volume model. A crash is not a rejection and must not
be counted as one.

## Costs

Generation is 86–95s per window and still dominates, as expected. Compilation
of the map that succeeded took **326.8s** — five and a half minutes, three
times its generation — because a map that passes every stage does all the
authoring work the rejected ones skip. Cost per attempt is therefore strongly
bimodal, and mean cost per candidate is a misleading planning figure.

## Honest status

Proof of concept: **achieved**. A pool: **not yet**, and the gap is wiring the
seams and measuring what they cost in yield, not building more measurement.
