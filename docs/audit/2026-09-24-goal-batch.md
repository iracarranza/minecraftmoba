# The goal batch: 33 READY maps, each carrying a portfolio it derived

**24 September 2026.** 52 seeds, 95 targets, 2.03 hours, unattended.

| | |
|---|---|
| generated | 93 of 95 (2 failures) |
| **PLAYABLE** | **33 (35%)** |
| **READY/hour** | **16.28** |
| READY maps with a certified portfolio | **33** |
| ...without | **0** |
| sources per map | min 4, median 18, max 28 (604 total) |
| worlds deleted | 186 |

## Step 1's target was met by step 2, not by step 1

This reverses a result recorded two batches ago. Parallelism alone produced
**8.48 READY/hour** against a target of 15, and that was recorded as a miss
with a structural reason: Paper already generates chunks on worker threads, so
concurrent servers divide the same cores and generation only reaches 1.73x.
More parallelism could not have closed it.

The parallel batch's own analysis said the remaining gap was **yield, not
throughput**. That turned out to be exactly right. Per-Type allocation plus
the land-sited Homebase fix moved the playable rate 17% -> 22% -> **35%**, and
throughput crossed 15/hour with no further parallelism at all.

The target was real; the diagnosis of why it was missed was also real; and the
fix lived in a different step. Recording that as a sequence rather than
quietly marking step 1 green.

## Yield by stage

| rejection | count |
|---|---|
| `SOCKET_NOT_INDEPENDENTLY_ACCEPTABLE` | 44 |
| `OPENING_CEILING_VIOLATED` | 16 |
| `LAIR_ACCESS_DISPARITY` | 5 |
| `NO_ORDERED_OBJECTIVE_LAYOUT` | 3 |
| `OPENING_FLOOR_VERB_BLOCKED` | 2 |
| `LAIR_PHYSICALLY_INVALID` | 1 |
| `PHYSICAL_VERIFICATION_FAILED` | 1 |
| `AUTHORING_SITE_BUILT_ON` | 1 |

Homebase siting is still the dominant failure at 44, even after the land fix
and after allocation starved `shattered_coast`. It is now the clear next
target, and unlike the others it is a *siting* problem rather than a
*selection* one: the windows being chosen have land at both ends and the
compiler still cannot develop two independent sockets on them.

`OPENING_CEILING_VIOLATED` at 16 is the village check working, and is a real
doctrine rejection rather than a defect.

## Cost profile

Generation 60% of wall, compilation 39%. Generation has reclaimed the
majority as the pass rate rose, because a candidate that passes every stage
does all the authoring the rejected ones skip. Cost per attempt remains
strongly bimodal and mean cost per candidate is still a misleading planning
figure.

## What this does and does not establish

**Established.** A pool refills at ~16 READY maps per hour unattended, on one
machine, with every map certified against the six seams and carrying a
regenerative portfolio derived from its own geography. No map can certify
without one, because `renewables` is in `readiness.REQUIRED`.

**Not established.** That a match starts on one. The runtime can now read a
portfolio -- `MapBindings.renewables` parses it and `Renewables.bind` builds
this map's own sources -- and that path is verified in tests against a real
derived portfolio, not in play. Until a match runs on one of these 33 maps and
a herd or crop patch manifests, the loop is closed in code and not in fact.
