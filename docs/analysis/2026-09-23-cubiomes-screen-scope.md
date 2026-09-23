# Scoping a server-free seed screen (cubiomes)

**Date:** 2026-09-23
**Status:** SPIKE RUN AND WIRED IN, same day. See
`terrain_harvest/seed_screen.py` and `native/cubiomes/`. Results at the foot of
this document; the scope below is kept as written, and was mostly right.
**Motivation:** the regional-shape gate rejects 82.5% of random seeds, and every
rejection currently pays a full world generation first. The lattice pre-screen
measured this afternoon is a *net loss* (19% more expensive; break-even needs a
49% discard against its measured 30%), because a chunk cannot reach FULL without
its neighbours. Sparser generation is the wrong primitive. The question is
whether screening can happen **without generating anything**.

---

## 1. What the screen has to decide

Six checks, with their source metrics:

| check | metric | derived from |
| --- | --- | --- |
| `western_relief_advantage` | `western_mean_elevation_advantage_y` ≥ 3 | surface height |
| `western_highland_depth` | `highland_depth_blocks` ≥ 150 | surface height |
| `eastern_ocean_present` | `east_actual_ocean_fraction` ≥ 0.10 | **biome** |
| `ocean_gradient_points_east` | `east_over_west_ocean_advantage` ≥ 0.10 | **biome** |
| `region_not_mostly_water` | `actual_surface_water_fraction` ≤ 0.75 | **water blocks** |
| `homelands_reasonably_viable` | `homeland_developability_bias` ≤ 0.35 | **block-level buildability** |

They are not equally reachable without a world, and that is the whole scoping
question:

- **Biome metrics — exact.** cubiomes reimplements biome generation faithfully;
  ocean fractions are a biome classification over a sample grid.
- **Elevation metrics — approximate.** cubiomes offers an approximate Overworld
  surface height at 1:4 horizontal scaling. Note the candidate grid already
  samples every **8** blocks, so 1:4 is *finer than what the real screen reads*.
  The library documents this as an approximation, and an open cubiomes-viewer
  issue is titled "'approximate' surface height is very generous" — the size and
  direction of that error is the single biggest unknown here and must be
  measured before anything is trusted.
- **Water-block and buildability metrics — not reachable.**
  `actual_surface_water_fraction` counts real water blocks including rivers and
  lakes, and lakes are placed features, not biome facts.
  `homeland_developability_bias` reads a buildable mask off block data. cubiomes
  does no block-level generation and says so.

**Consequence: a cubiomes screen can evaluate at most 4 of the 6 checks**, and
two of those only approximately. It cannot replace `recognize`; it can only
reject seeds that fail on the cheap facts.

That is enough to be worth it. The screen's job is not to certify — it is to
discard, with **zero false negatives**, before a world is generated.

## 2. What the discard ceiling actually is

From the 40 unseen seeds, individual pass rates were: relief 55%, highland
depth 75%, eastern ocean 62%, ocean gradient 60%, not-mostly-water 88%,
homelands viable 78%. If a screen could evaluate only the two biome checks
exactly, its ceiling is whatever fraction fails **eastern ocean OR ocean
gradient** — on this sample about **45%**.

That number matters because the lattice measurement established a break-even at
49% discard for a screen costing half a generation. A cubiomes screen costs
~nothing per seed, so **any** discard rate is a win — but 45% from biome checks
alone is already above the lattice's break-even, which is the clearest single
argument for this direction over the one measured today.

Adding the two approximate elevation checks could raise the ceiling toward the
observed 82.5%, *if* the approximation proves tight enough to use with a safety
margin.

## 3. Validation is already paid for

**69 seeds have been compiled today with ground-truth metrics on disk** — 29
re-harvested plus 40 fresh, each with its generated world and its evaluated
`metrics` block. That is a ready-made validation set requiring no new
generation:

1. compute the cubiomes-derived metrics for the same 69 seeds;
2. plot approximate against actual for each elevation metric;
3. choose a margin such that **no seed that actually passes is ever discarded**;
4. report the discard rate achieved at that margin.

Step 3 is the whole discipline: a false negative loses a good seed permanently
and silently, which is the expensive error. The existing screen's first version
had one — it discarded 99887766, which went on to compile — and that is why
`best_gradient` exists.

## 4. Shape of the work

| stage | what | risk |
| --- | --- | --- |
| A | Build cubiomes, expose biome-at-coordinate and surface-height over a sample grid | low — C library, documented API |
| B | Bind it: ctypes/cffi against a shared library, or a CLI emitting JSON | low, but it **is** a C dependency in a Python/Java project |
| C | Reimplement the 4 reachable metrics against that sampling | medium — must match the real screen's grid, orientation handling and masks |
| D | Validate against the 69 known seeds; fit margins for zero false negatives | **the gate.** If elevation error is large and biased, only the 2 biome checks survive |
| E | Wire into the foundry ahead of generation | low |

Stage D decides whether this is a 45% screen or an 80% screen. It is also the
stage that can fail outright, and it should be run before B is polished.

## 5. What this is not

- **Not a replacement for `recognize`.** Two checks cannot be evaluated without
  blocks at all. Every surviving seed still generates and still faces the full
  gate.
- **Not a fix for the conjunction.** 82.5% rejection is the product of six
  simultaneous requirements, each individually near the median of random
  terrain. A faster screen makes rejection cheap; it does not make maps
  commoner. Whether Default regional shape should require all six remains a
  doctrine question and is untouched by this.
- **Not MCSR's problem.** MCSR Ranked filters *structure and loot* predicates —
  bastion distance, chest contents, lava near village — which are pure functions
  of the lower 48 bits of the seed and need no terrain at all. Terrain-shape
  predicates are strictly harder, and the comparison should not be used to
  imply a similar throughput is available here.

## 6. Recommendation

Do stage A→D as a **spike against the existing 69 seeds**, and stop at the
measured discard rate. Do not wire anything into the foundry until the
false-negative rate is zero on that set and the discard rate is reported. The
lattice screen is exactly what happens when a plausible optimisation is adopted
on the strength of a rate that was never multiplied by its cost.


---

# Result, 23 September 2026

Built, measured, wired in.

**Speed.** 14,256 cells in **0.23 s** against 65-170 s to generate the same
window. Roughly 500x, and `mapApproxHeight` returns biome and height together.

**Surface height, 61,479 samples over 69 generated worlds.** Unbiased and
heavy-tailed:

    mean -0.27   stdev 6.43   median +0.5
    |err| <= 2   41.3%
    |err| <= 5   81.2%
    |err| <= 10  93.0%
    p0 -118.2    p100 +111.8

The open cubiomes-viewer issue titled "'approximate' surface height is very
generous" is **overstated for a mean and understated for a tail**. The worst
seed, 179190514, is 85% ocean and runs a mean of -6.7 with a p05 of -30.5 --
deep water is where it diverges, which is a bounded and explainable failure
rather than noise. **Elevation is therefore not screened on**, even though it
would raise the discard rate.

**False negatives: zero, at every margin tried.**

    margin   discard   false negatives
    0.00       26%           0
    0.02       25%           0
    0.05       20%           0
    0.08       16%           0

0.05 is shipped. Zero at margin 0.00 is the stronger result, but only 13 of the
69 seeds were positives, so the evidence for "never discards a good seed" rests
on 13 cases; four points of discard is cheap headroom against seeds that set has
not seen.

**What the scope got right:** that only the two ocean checks are exactly
reachable, that validation needed no new generation, and that stage D was the
gate. **What it got wrong:** the ceiling. It estimated ~45% discard from the
biome checks alone, on the basis that 45% of the 40 unseen seeds failed eastern
ocean *or* ocean gradient. The shipped screen requires **both** to miss, which
is the conservative construction, and it discards 20-26%.

**What has not changed.** Rejection is now nearly free; maps are not commoner.
82.5% of seeds still fail the real gate, on a conjunction of six requirements
each near the median of random terrain. This buys throughput, not yield.
