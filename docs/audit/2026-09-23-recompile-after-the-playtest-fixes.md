# Recompiling 29 maps against the contract the playtest exposed

**Date:** 2026-09-23
**Inputs:** 29 previously generated worlds, re-harvested into candidates (the
originals were never kept) and run through the complete compiler with the
Fountain verification, Opening ceiling, structure refusal and entity eviction
added today.

## Result

**Yield 0 of 29.**

| deepest stage | n | |
| --- | --- | --- |
| recognize | 23 | `NO_DEFAULT_REGIONAL_SHAPE` |
| homebase | 3 | `SOCKET_NOT_INDEPENDENTLY_ACCEPTABLE` |
| lair | 1 | `LAIR_ACCESS_DISPARITY` |
| verify | 2 | see below |

The dominant filter is unchanged by today's work: **23 of 29 never reach a
Homebase**, on the regional-shape recogniser that has always been the first
gate. Nothing added today touches that, and it is where the yield is actually
decided.

## The two that reached verify

**99887766 — `OPENING_CEILING_VIOLATED`.** 18 sampled columns within 96 blocks
of the south Fountain stand on built structure. This is the map that was played,
and the check written this afternoon disqualifies it exactly as predicted from
the playtest. It is the clean confirmation that the gap is closed.

**2718281 — inconclusive, and the reason is a finding of its own.** Against the
pristine world it fails `AUTHORING_SITE_UNGENERATED`, because that generation
window does not cover the north Outpost's site. Against the fully generated copy
in the pool it fails `PHYSICAL_VERIFICATION_FAILED` — but that world **already
has its objectives authored into it**, so verification reads its own output back
as terrain obstruction.

> **A published pool map cannot be re-verified.** Authoring writes into the
> world, so the artifact that proves a map passed is also the artifact that
> makes the proof unrepeatable. Re-checking an existing map against a newer
> contract requires regenerating it from seed. Nothing records the pristine
> world, and nothing says it should — which is why this could not be answered
> today.

## What this does not say

It does not say the compiler got worse. Every check added today can only reject,
and one of the two previously playable maps is rejected for a documented,
demonstrated reason. The other cannot be judged from the worlds on disk.

It does not establish a yield for the current contract. A yield needs candidates
that reach the later stages, and 26 of 29 stop in the first two — so the sample
that could exercise the new checks is two maps, one of which is unmeasurable.

## Next, in order

1. **Record the pristine world, or the seed plus the generation window**, so a
   published map can be re-verified when the contract changes. This is the
   blocker on every question below.
2. **Generate against the recogniser, not past it.** 23 of 29 dying at
   `NO_DEFAULT_REGIONAL_SHAPE` means seed search, not compiler tuning, is where
   throughput comes from.
3. **Then** measure A_D across a population large enough to cut, and decide the
   objective-depth bound that `objectives.md` currently leaves OPEN.
