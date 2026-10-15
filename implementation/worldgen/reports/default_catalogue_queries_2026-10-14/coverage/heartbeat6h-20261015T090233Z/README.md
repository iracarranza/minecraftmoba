# Retained native policy coverage — 6-hour continuation

**Prototype/test; unqualified native approximations.** The unchanged native corpus contains 13,916 seeds, 1,391,600 region choices and 5,566,400 orientations. This report adds one grouped index read; no terrain scans or producer restarts.

| Scoop size | Stricter region choices | Looser region choices | Seeds covered, stricter | Seeds covered, looser | Required flat-end density, stricter / looser |
|---|---:|---:|---:|---:|---:|
| 1024² | 29,362 | 208,069 | 12,333 | 13,916 | 30.0% / 20.0% |
| 1536² | 58,530 | 268,290 | 13,752 | 13,916 | 12.5% / 8.3% |
| 2048² | 76,367 | 288,430 | 13,873 | 13,916 | 7.5% / 5.0% |
| 3072² | 88,941 | 299,272 | 13,852 | 13,916 | 3.1% / 2.1% |

Loosening these saved preferences adds **810,861 region choices** and **0 new unique seeds**. Both policies already cover every retained seed somewhere; their difference is flexibility within each seed.

The fixed absolute end-flat threshold is a size effect worth retaining: twelve cells require 30% of a 1024-block scoop's sampled end, but only 3.1% of a 3072-block scoop's sampled end. That is a descriptive consequence of the experimental query, not a balance decision or proof that a larger scoop is better.

Distinct region choices still overlap. Across seeds, the saved greedy pairwise-disjoint selections total 72,059 under the stricter policy and 221,695 under the looser policy; these are conservative packings, not exact maxima. Orientation totals remain separate from region and seed counts.

- [Full coverage and size-pattern summary](coverage.json)
- [Per-seed size aggregates](per-seed-size-counts.jsonl.gz)
- [Original saved policies and query outputs](/Users/iracarranza/minecraftmoba/implementation/worldgen/reports/default_catalogue_queries_2026-10-14/runs/five-hour-continuation-910013916-5566400-94dec1877099-075903675996/strict/policy.json)

Native non-ocean samples can include rivers/inland water. Flat-end supply, gradients and native mirror ratios are approximate measurements. Actual terrain, Homebase Core fit, routes, physical columns, all-nine-anchor 175 spacing, Lair, resources and runtime readiness remain separate.
