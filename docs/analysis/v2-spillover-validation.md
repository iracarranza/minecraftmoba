# V2 Route-spillover model — exact-world validation

Status: **Measurement result. The spillover model does not yet predict what it was built to predict.**
Date: 20 September 2026

## What was asked

Whether the v2 optimizer "has actually learned the main failure exposed by the
first physical authoring pass" — that a Route changes reach to everything near
it, not just to its own target.

## Provenance first

The v2 frontier **reproduces exactly** from the committed optimizer and export:
all eight profiles match the handoff's `balanced_found` and
`effective_balance_asymmetry` to eight decimals. The 32-bit LCG resolved the
provenance divergence that made the v1 numbers unconfirmable. Everything below
rests on a chain that now verifies.

Four rank-0 worlds authored, zero skipped sites, **346 readback assertions, 0
failures**. Eight rescans: bare and authored for each profile.

## Method

`terrain_harvest/spillover_validation.py` imports the committed optimizer and
calls its own `spillover_reach`. Reimplementing it would have validated a copy
of the model rather than the model.

The model is judged on **savings**, not absolute reach:

    predicted saving = regional_raw - regional_spillover
    actual saving    = exact_bare  - exact_authored

A model can be wrong about how long a trip takes and still be right about how
much a corridor shortens it. Only the second affects balance.

## Result 1 — the aggregate magnitude is about right

| Profile | Predicted mean | Actual mean | Median error |
|---|---:|---:|---:|
| Balanced Baseline | +19.93 s | +13.29 s | +0.86 s |
| Exploration-Centric | +12.45 s | +18.99 s | −1.32 s |
| Consolidative | +14.99 s | +11.06 s | −0.09 s |
| Resource-Light | +12.96 s | +14.42 s | +0.00 s |

Median per-opportunity error is near zero everywhere. Taken alone this looks
like a calibrated model.

## Result 2 — it fires at the wrong opportunities

| Kind | n | Recall | Precision |
|---|---:|---:|---:|
| Route target | 48 | 55.2% | 55.2% |
| POI | 32 | 29.4% | 71.4% |
| Mining worksite | 76 | 23.7% | 40.9% |
| Renewable range | 60 | 21.2% | 46.7% |
| Founder crop | 32 | 15.4% | 28.6% |
| **Overall** | **248** | **30.0%** | **48.8%** |

Correlation between predicted and actual saving, over cases where either side
claims an effect: **0.09 to 0.12** across all four profiles. That is
approximately no relationship.

The model misses **70% of the opportunities a corridor actually helped**, and
about half of what it does flag was not affected. It is closest on Route
targets, which are the one case v1 already handled — its accuracy falls off
precisely on the collateral effects it was added to capture.

The near-zero median error in Result 1 is therefore two large errors cancelling,
not precision.

## Result 3 — it gets the direction of the balance change wrong

| Profile | Predicted Δ | Actual Δ | |
|---|---:|---:|---|
| Balanced Baseline | −0.0021 | +0.0115 | **wrong** |
| Exploration-Centric | +0.0049 | −0.0160 | **wrong** |
| Consolidative | −0.0279 | −0.0379 | correct |
| Resource-Light | +0.0056 | **−0.1053** | **wrong** |

**One of four.** On Resource-Light the model predicted authoring would make
balance slightly worse; it improved by 0.105, nineteen times the predicted
magnitude and the opposite sign.

For the filter's purposes this is the load-bearing failure. `effective_balance_
asymmetry = max(raw, spillover)` can only help if `spillover` moves the right
way, and here it usually does not.

## Assessment

The v2 model has the right **shape** — a distance-falloff saving along a
corridor, bounded below — and roughly the right total magnitude. It does not yet
have the right **incidence**.

The likely reason is visible in the geometry: `spillover_reach` awards a saving
by proximity to the straight segment between homeland and Route target, while an
authored corridor follows the terrain-weighted cheapest path, which can deviate
far from that segment through exactly the broken ground where detours are
expensive. Proximity to the straight line is a poor proxy for proximity to the
actual corridor.

Retuning `radius_blocks` or `scale` will not fix an incidence error — it would
rescale savings that are largely on the wrong opportunities.

## Recommendation

Run the corridor, not the segment. The optimizer already has the travel matrix
and the same terrain graph the authoring uses; computing each candidate Route's
actual path once per configuration and awarding spillover by distance to *that*
polyline would replace the geometric assumption with the real one. If that is
too costly inside the sampling loop, precomputing one path per Route-target
candidate — 63 paths, fixed across all configurations — would capture most of it.

Then re-measure with this same tool. Recall and direction accuracy are the
numbers to watch; the median error was already near zero and did not indicate a
working model.

## Where the maps stand

| Profile | Bare | Authored | Against 0.12 |
|---|---:|---:|---|
| Balanced Baseline | 0.1130 | 0.1245 | **fail** |
| Exploration-Centric | 0.1418 | 0.1259 | **fail** |
| Consolidative | 0.1460 | 0.1081 | pass |
| Resource-Light | 0.1726 | 0.0673 | pass |

Two of four pass on measured exact-world values. Every profile measures worse
bare than its regional estimate, as in v1. **Nothing should freeze.**

## Not covered

- whether this corridor geometry is the only sensible one; a different width or
  routing rule would move the actuals and is itself a design choice;
- hostile exposure, still **UNRESOLVED**;
- reach from anywhere but the two homelands;
- whether 0.12 remains the right threshold once measured values replace
  regional ones.
