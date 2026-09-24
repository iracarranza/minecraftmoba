# Describe, then label: the compiler's next shape

**Date:** 2026-09-23
**Status:** METHOD. Steps 1–3 implemented in the same pass; nothing else built.

---

## 1. The problem, measured

Generation is the only expensive step, by an order of magnitude:

| step | cost | runs on |
| --- | --- | --- |
| **Minecraft world generation** | **65–173 s** | every seed |
| harvest (read + evaluate) | 6.5 s | every seed |
| `recognize` … `lair` | **~0.00 s** | every seed |
| `verify` | ~13 s | survivors |
| `characterize` | 276 s | survivors of verify |

For a 40-seed batch that is ~4,800 s of generation against ~280 s of everything
else, and **33 of those 40 generations were spent on seeds rejected at
`recognize`** — a stage that costs nothing and answers in arithmetic.

The compiler's filters are not slow. **We pay Minecraft to answer a question we
could have asked first.**

## 2. The deeper cause: one Map Type

`recognize` does not ask "is this terrain good". It asks **"is this a Valley
map"** — western highlands, eastern coast, N–S opposition. `maps.md` names
Chasm and Archipelago as future types and says outright that they are *"future
Map Types, not implemented recognizers"* and that *"Default's regional gradient
is its own search contract, not a universal composition constraint."*

So an 82% rejection rate is not strictness. It is **the hit rate for one
archetype among several**, and every seed that is a good Chasm is discarded as a
failed Valley.

## 3. What is already there and being thrown away

Every candidate already carries **25 metrics** — coastline edge length,
headland/cove turns, forest region count and interior depth, water body count
and largest body, highland depth and fraction, local relief p90, steep fraction,
approach bands, snow cells, river mouths, buildable fraction.

`recognize` collapses all 25 into **six booleans**, keeps only the failures as
`warnings`, and discards the rest. A window with 4,792 blocks of coastline, 25
water bodies and 7 forest regions is recorded as `NO_DEFAULT_REGIONAL_SHAPE`.

The description layer already exists. The change is to **stop collapsing it.**

## 4. The new shape

    ANY seed
        -> window search (off-server, ~2 s)        rank places, do not reject seeds
        -> generate the ranked windows
        -> harvest                                  25 metrics, PERSISTED not collapsed
        -> characterize + cell grid                 ore, caves, depth, character
        -> composite description                    located sub-features
        -> fit against Map Type templates           ranked, with what did not fit
        -> discovered classification                scale, resource density

**Small features first, composite label last.** A description built from located
sub-features — "an N–S pair of homebase-viable regions, ocean at one end,
highland at the other" — can say *which part* failed. A single Default verdict
cannot, which is why `A_D` was invisible for so long: nothing recorded the parts.

## 5. Why this is more efficient than the previous model

It does **not** make generation cheaper per map. Three other things change:

1. **One description serves every Map Type.** Today each type would need its own
   search over its own contract. Adding a type becomes writing a template over
   metrics that already exist, not implementing a recognizer.
2. **Generation follows ranking rather than preceding rejection.** The window
   search orders places before anything is materialised, so generation is spent
   on the best candidates rather than on arbitrary origins.
3. **Rejections become data.** A ranked fit shows a window that is 80% Default;
   a boolean shows nothing. The distribution that every deferred threshold —
   A_D, intervention budget, Map Scale, Resource Density — is waiting on is
   exactly what this accumulates.

## 6. Reuse, rework, discard

**Reused unchanged:** `evaluate.py`'s 25 metrics and its 8-orientation search;
`window_search`; `seed_screen`; `characterize`; `cell_grid`; `column_scan`;
`opportunity_map`; `caves`; `routes`; `rescan`; the homebase socket search; the
objective ordinal; `lair_socket`; `readiness`; the foundry.

**Reworked:** `recognize` — from a boolean gate to a description plus a fit
score. The six checks survive as *Default's template*, not as the definition of
a usable map.

**Discarded:** nothing. `NO_DEFAULT_REGIONAL_SHAPE` stops being a hard
rejection and becomes "did not fit the only template we have", which is a
different and more honest claim.

**Unchanged by design:** the expensive measurements stay downstream of
generation. Ore, caves, buildability and water are carver and feature output;
no reordering moves them off-server.

## 7. Two risks worth stating

**The vocabulary decides what can ever be found.** The 25 metrics were written
to describe Default — `western_mean_elevation_advantage_y`,
`east_actual_ocean_fraction`. A vocabulary naming only Default's features will
only ever recognise Default, whatever the order. `actual_water_body_count` and
`forest_region_count` are already type-neutral; the rest should move that way,
and Chasm needs relief *discontinuity* while Archipelago needs land-body count
and separation.

**Template-fitting with n=2 would repeat the Socket mistake.** Two maps have
ever compiled. Fitting "what Default looks like" to that manufactures a
classifier from noise. So: **describe everything, record the distribution, do
not fit templates yet.**

## 8. Steps taken in this pass

1. persist the full metric vector per compilation, pass or fail;
2. run the window search over a batch, keeping every window's vector rather
   than only the best;
3. report the distribution.

No template, no threshold, no new Map Type.


---

# Step 3 result, 23 September 2026

Window descriptions over the 40 unseen seeds, cheap tier only:

    40 seeds -> 32,480 windows in 26s (0.65s per seed)

    feature              min     p25   median    p75     max
    ocean_fraction     0.000   0.031    0.204  0.509   1.000
    best_edge_ocean    0.000   0.081    0.448  0.832   1.000
    best_gradient      0.000   0.051    0.263  0.522   1.000

    windows passing Default's ocean+land checks, per seed:
      seeds with at least one:  40 / 40
      min 310   median 398   max 482
      seeds with NONE anywhere: 0

**Every seed contains Default-shaped windows, a median of 398 each.** The 82%
rejection is not a property of seeds; it is a property of only ever examining
the origin.

The limit, stated plainly: these are the two OCEAN checks. Relief, highland
depth, surface water and homeland developability still require generation, so
"passes ocean+land" is not "is a Default map". But those two checks rejected
about 45% of seeds at the origin and reject **none** when placement is searched.

No template was fitted and no threshold was set. The distribution is the
deliverable.

---

# Revision, 24 September 2026: three corrections

## A. Seed selection was the wrong frame, and the seed screen is largely moot

The world is +/-29,999,984 blocks -- about 3.6e15 blocks -- and a window is
~9e5. That is roughly **four billion non-overlapping windows inside a single
seed**. Measured, 398 of 812 windows in a +/-4096 box satisfy Default's ocean
checks, and 40 of 40 seeds had some.

Minecraft terrain is stationary noise, so any configuration with non-zero
probability occurs somewhere in a sample that large. Near-certainly **every
seed contains satisfying windows, in quantity.**

Consequence: `seed_screen`, built the previous day, discards 19% of seeds with
zero false negatives -- and optimises a quantity that does not constrain
anything. Both the seeds it keeps and the seeds it discards contain thousands
of viable windows. Its validation was sound; its premise was not.

**The real constraint was never "find a good seed". It is how many windows can
be generated and verified**, because the cheap tier scales to billions and the
expensive tier costs ~120 seconds each.

## B. Bound the search, and copy the pattern that exists

`foundry.job` is already bounded, and says why:

> Bounded on purpose. A foundry that runs until it succeeds is a foundry that
> hangs when the geography will not cooperate, so it stops at a target, an
> attempt limit, or the end of its candidates, and says which.

Placement search wants the same: stop at N viable windows or T seconds, and
report which bound was hit. Given A, the budget should probably not be
per-seed at all.

## C. Prominence -- features described by how much they stand out

`highland_fraction_west` is 0.31 for one massif and 0.31 for fifty scattered
hillocks. The vocabulary can say how MUCH high ground exists and not whether it
is one thing.

`terrain_harvest/prominence.py` measures the standard topographic quantity: a
summit's height above the lowest saddle connecting it to higher ground,
computed by a descending union-find sweep over the candidate's existing
`feature_grid.height_rle`. **No threshold is involved** -- prominence is
defined per feature by where it merges, not by a cutoff. Running the sweep on
negated elevation gives pit prominence, so a chasm is described by the same
measurement that describes a highland.

Measured across 14 candidates, `highland_fraction_west` predicts top prominence
at **r = 0.46** -- weak, so prominence carries information the old metric
cannot. Three cases show why:

| seed | highland_fraction_west | top prominence | top extent |
| --- | --- | --- | --- |
| 1412888665 | 0.212 (low) | 87 (highest) | 64 blocks2 -- a spire |
| 1248940432 | 0.471 (highest) | 61 | 2,560 blocks2 -- scattered |
| 132112992 | 0.202 (low) | 49 | 603,072 blocks2 -- one massif |

The highest highland fraction has the least coherent high ground; the lowest
has a single enormous feature.

Cost: 0.05 s per candidate, from data already persisted.

**Magnitude, extent and isolation stay separate.** A massif, a plateau and a
ridge differ in which of the three is large, and collapsing them to one scalar
would repeat the 25-metrics-into-6-booleans mistake at smaller scale.
Isolation -- distance to the nearest higher summit -- is not yet computed.

Combined with the location reframing, a feature becomes:

    feature  x  location  x  (magnitude, extent, isolation)

which can describe a Chasm without having been told what one is. That is the
actual test of whether describe-then-label works.
