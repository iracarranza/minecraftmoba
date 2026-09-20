# Spillover as a predictive filter — verdict

Status: **NO branch. Abandon spillover as a predictive filter; exact validation is the selection stage.**
Date: 20 September 2026

## Two corrections to what I previously reported

**1. My V3 diagnosis was wrong.** I attributed the incidence error to corridors
deviating from the straight homeland→target segment. I precomputed all 63
candidate corridor polylines over the same terrain graph the authoring uses:
they deviate by a **median of 6 blocks, max 74**, and **none** exceed the
112-block spillover radius. Mean difference between straight-segment and
real-polyline distance, over 248 opportunities: **8.3 blocks**.

Tested directly against the measurements:

| Predictor | Correlation with actual saving |
|---|---:|
| v2 `predicted_saving` | 0.222 |
| −distance to straight segment | −0.0002 |
| −distance to real polyline | −0.0095 |

The polyline is not better. The V3 correction was geometrically sound and
practically inert, so the authorized V3 pass was not spent: it would have
answered a question already answered.

**2. My earlier measurements contained a confound.** An authored world differs
from a bare one by 32 site pads as well as by corridors, and every pad clears
and flattens terrain. I attributed that whole difference to Routes.

Isolated properly with a **sites-only** control world:

| Profile | Pad effect on balance | Route effect on balance |
|---|---:|---:|
| Balanced Baseline | +0.0023 | +0.0092 |
| Exploration-Centric | −0.0123 | −0.0036 |
| Consolidative | −0.0259 | −0.0120 |
| Resource-Light | −0.0147 | −0.0906 |

Pooled per-opportunity saving: **pads +6.89 s, Routes +7.32 s** — site pads were
roughly **half** of what I called the Route effect. The V1 figure of ±0.049 that
motivated the spillover model was sites-plus-Routes, not Routes.

## The corrected verdict on the model

Judged against the isolated Route effect, over 248 opportunities:

| | Confounded (reported earlier) | Corrected |
|---|---:|---:|
| Recall | 30.0% | **33.3%** |
| Precision | 48.8% | **16.2%** |
| Correlation | 0.09–0.12 | −0.04 to 0.21 |
| Magnitude | ≈ right | **over-predicts 2.1×** |

Removing the confound made the model look **worse**, not better. It now fires 67
false positives against 13 true ones: it predicts a saving at five opportunities
for every one where a corridor actually helped, and over-predicts the size of
the saving it does find by a factor of two.

Correlation with the measured effect remains approximately zero, and is negative
on two profiles.

## Conclusion

Per the agreed decision tree, this is the **NO** branch. Spillover should not be
retained as a predictive filter, and it should not be retuned: the failure is
not calibration, not geometry, and not the confound. The regional surrogate does
not carry the information.

`effective_balance_asymmetry = max(raw, spillover)` is currently a term that
moves the filter on essentially random incidence. It should be dropped or
explicitly demoted to a screening heuristic with no claim to predict exact-world
balance.

**Exact author→rescan is cheap enough to be the final selection stage**, which
was the standing fallback. Authoring and rescanning one profile takes a few
minutes and the tooling is committed and verified.

## Alpha selection, from exact-world values

| Profile | Measured balance | 0.12 filter |
|---|---:|---|
| **Resource-Light** | **0.0673** | pass |
| Consolidative | 0.1081 | pass |
| Balanced Baseline | 0.1245 | fail |
| Exploration-Centric | 0.1259 | fail |

Two of four pass on measured values, which supports the earlier observation that
the pipeline can already produce viable battlefields — the difficulty was only
ever choosing them prospectively.

**Recommended Alpha: Resource-Light**, on the clearest margin measured
(0.0673, well inside the envelope). The caveat is that its balance is partly a
consequence of its character: fewer worksites and renewables mean fewer
opportunities to be asymmetric about. If the first playtest wants a denser
economy, **Consolidative at 0.1081** is the alternative, with much less margin.

Both were authored at rank 0 from the reproducible v2 frontier, verified by
readback, and rescanned. Neither is frozen until you choose.

## Not covered

- hostile exposure, still **UNRESOLVED**; the zero-hostile snapshot is not
  evidence of safe caves;
- whether 0.12 remains the right threshold now that measured values, which run
  systematically higher than regional ones, are the input;
- whether the corridor geometry authored here is the right one — a different
  width or clearing rule would move the actuals and is a design choice, not a
  measurement.
