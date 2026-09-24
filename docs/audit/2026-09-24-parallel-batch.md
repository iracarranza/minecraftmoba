# Parallel batch: 29 targets, and the bottleneck flips back to generation

**24 September 2026.** Sixteen seeds, two targets each, 35.4 minutes, 3
generation workers and 6 compile workers.

## Result

| | |
|---|---|
| targets | 29 generated, **0 failures** |
| PLAYABLE | 5 / 29 (17%) |
| READY/hour | **8.48** (serial 5.2, **1.63x**) |
| worlds deleted | 58 |
| peak memory | 69% at 3 concurrent servers |

**The ≥15 READY/hour target is NOT met.** 8.48 is the measured figure.

## The two halves parallelise completely differently

| phase | wall share | per target | speedup |
|---|---|---|---|
| generation | 1592.6s (**75%**) | 54.9s | **1.73x** on 3 workers |
| compilation | 488.8s (23%) | 16.9s | **6.35x** on 6 workers |

Compilation parallelised near-linearly, as predicted. Generation did not, and
the reason is structural: **Paper already generates chunks on worker threads**,
so concurrent servers divide the same cores rather than adding throughput.
Three at once took 130-212s each against ~90s alone.

So the bottleneck has flipped back. Generation was 91% of cost, then briefly
less than compilation, and is now 75% again — because compilation got 6.35x
faster and generation only 1.73x. **More generation workers will not fix
this**; the cores are already busy.

## What this says about the next step

The remaining gap to 15/hour is a **yield** problem, not a throughput one. At
17% playable, 29 generations produced 5 maps. Raising yield is now strictly
more valuable than raising parallelism, and that is step 2 of the goal.

`SOCKET_NOT_INDEPENDENTLY_ACCEPTABLE` is **26 of 32 rejections**. One failure
mode dominates everything else: windows are being selected whose two ends
cannot both host a developable Homebase. Either the symmetry gate is selecting
for the wrong thing, or Homebase siting needs Type-specific rules, or both.

Other codes: `OPENING_CEILING_VIOLATED` x3 (villages, working as written),
`LAIR_ACCESS_DISPARITY` x2, `NO_ORDERED_OBJECTIVE_LAYOUT` x1.

## Status against the goal

Step 1 delivered parallelism, unique ports, world cleanup and zero crashes
over a 29-target run — but **not** the throughput target. Recorded as measured
rather than claimed. The honest conclusion is that step 1 is finished as a
piece of engineering and its headline number is 8.48, and that step 2 is where
the remaining 2x lives.
