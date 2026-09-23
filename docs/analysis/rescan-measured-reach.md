# Rescan — measured reach on the authored worlds

Status: **Measurement result. Nothing should freeze.**
Date: 20 September 2026

Step 2 of *Next physical validation*, and the handoff's `rescan` clause:

> Every exact authored placement and Route must be rescanned. Replace regional
> reach proxies with measured exact-world values before freezing the map.

`terrain_harvest/rescan.py` reads an authored world's own blocks, rebuilds the
traversal graph from them, and runs the same Dijkstra to every authored site.
Reach is reported in the optimizer's own units — weighted cost ÷ 5.612 — using
`vanilla_search.task_a.PARAMETERS` unchanged, so a measured value substitutes
directly into its balance metric. A test asserts the parameters are the shared
object rather than a copy, because a drifting local copy would make the
comparison meaningless while still looking like one.

## The control comes first

Run against a **bare, unauthored** world, the rescan reports the proxy's median
relative error as **7.1%** on the baseline site set (9.3–9.7% on the others) —
matching the documented 7–8% leave-one-out error. The rescan therefore
reproduces the model the proxy was fit against, and the differences below are
results rather than artefacts of a reimplementation.

## Results

| Profile | Proxy | Bare | Authored | Route effect | Verdict |
|---|---:|---:|---:|---:|---|
| Balanced Baseline | 0.0403 | 0.0919 | 0.1166 | +0.0247 | pass, no margin |
| Exploration-Centric | 0.0265 | 0.1456 | **0.1925** | +0.0468 | **FAIL** |
| Consolidative | 0.0685 | 0.1561 | 0.1068 | −0.0494 | fail → **pass** |
| Resource-Light | 0.0182 | 0.0996 | 0.0505 | −0.0491 | pass |

Against the 0.12 structural-balance filter, which remains a NON-CANON
ANALYTICAL FIXTURE.

### The proxy understates asymmetry, systematically

Every profile measures worse than its proxy, by 2–5× on bare terrain — before
any authoring. **Two of the four already fail the filter on bare terrain**,
including Consolidative at 0.1561, which the proxy scored as balanced at 0.0685.

That is the headline. The largest correction is not what authoring did; it is
what the interpolation could not see. Per-site proxy error reaches 204 seconds.

### Routes move balance in both directions

Isolating authoring as bare → authored, corridors **helped two profiles and hurt
two**, by almost the same magnitude (±0.049). They are not a uniform
improvement and they are not neutral.

They do work as intended. On Balanced Baseline, reach to Route targets improved
by up to 35 seconds (cell [3,4] north: 172.0 → 136.9) and the `route` balance
component more than halved, 0.0566 → 0.0229. But on the same profile the
`renewable` component doubled, 0.1582 → 0.3202, and overall balance worsened.

The mechanism is straightforward once measured: a corridor speeds travel to
everything near it, not only to its endpoint, and the north and south corridor
sets do not pass the same things. Consolidative is the mirror case — Routes took
it from failing to passing.

**So corridor placement is a balance lever, not just a reach improvement for its
own target.** The optimizer cannot currently see this: it scores Route targets
with a proxy fit before any corridor exists.

## What follows

- **Nothing freezes.** Three of four pass, one fails, and Balanced Baseline at
  0.1166 against 0.12 is not a margin.
- **Exploration-Centric goes back to the optimizer.** The fallback machinery
  exists; `--best` will take the next finalist that authors completely.
- **Route side-effects belong inside the evaluation.** Scoring a corridor only
  by its endpoint's reach misses an effect of ±0.049 on total balance, which is
  larger than the spread between several profiles.
- **The measured values should replace the proxy** in any further comparison of
  these four, per the handoff clause.

## Not covered

- reach from anywhere other than the two homelands;
- underground travel — this is a surface graph;
- hostile exposure, which is not reach and remains **UNRESOLVED**; the
  zero-hostile snapshot still must not be read as zero hazard;
- whether 0.12 is the right threshold. It is an analytical fixture, and this
  measurement changes what it selects.
