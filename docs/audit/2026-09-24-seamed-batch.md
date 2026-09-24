# Second end-to-end batch: seams wired, two targets per seed

**24 September 2026.** Eight seeds, two generation targets each, 57 minutes
wall clock. All six seams running inside `characterize`.

## Result

| | |
|---|---|
| PLAYABLE | **5 / 16 (31%)** |
| crashes | 0 |
| generation | 26.3 min |
| compilation | 28.6 min |
| READY per hour of wall clock | **~5.2** |

Against the first batch — 1 of 8, ~2.5/hour, one crash — this is **double the
rate with strictly more checking**, which was not the expected direction.

## The finding: per-Type yield differs by 5x

| Map Type | playable |
|---|---|
| `landmass` | **4 / 6 (67%)** |
| `shattered_coast` | **1 / 10 (10%)** |

`SOCKET_NOT_INDEPENDENTLY_ACCEPTABLE` accounts for 10 of the 13 rejections and
almost all of them are `shattered_coast`: fragmented coastline does not offer
two independently developable Homebase sockets.

**The budget is allocated equally across Types with very different hit rates.**
Ten `shattered_coast` windows were generated at ~95s each to yield one map,
while six `landmass` windows yielded four. That is the clearest calibration
target available: allocate generation budget by measured per-Type yield.

Whether the predicate is wrong or `shattered_coast` genuinely needs different
Homebase siting rules than Default is **[OPEN]** — this batch cannot tell
those apart, and it is exactly the kind of question the describe-then-label
discipline exists to keep open.

## Other rejections

`OPENING_CEILING_VIOLATED` × 1 — the village check, working as written.
`NO_ORDERED_OBJECTIVE_LAYOUT` × 1. `LAIR_ACCESS_DISPARITY` × 1, the only
bounded symmetry metric in the compiler.

Zero rejections from the two seams that *can* reject. `resource_validity`
cannot reject on ore until it has an accessibility-filtered count, and
`opening_floor` blocked nothing once Combat was marked unevidenced.

## Cost

Generation 26.3 min against compilation 28.6 min — compilation now costs MORE
than generation, reversing the long-standing 91% figure. That is a consequence
of a higher pass rate: candidates that pass do all the authoring work the
rejected ones skip. Cost per attempt remains strongly bimodal and mean cost
per candidate is still a misleading planning figure.

## Status

Proof of concept: confirmed twice, now with certification. A pool at ~5/hour
single-process is a real refill rate. The next gains are cheap and known:
per-Type budget allocation, and parallelism, of which there is currently none.
