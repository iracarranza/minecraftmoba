# Requery retained Default measurements — 14 October 2026

**Prototype/test. No results are qualified maps.** This lane queries the existing native SQLite catalogue without generating terrain or rerunning measurements. All thresholds are configurable experimental selectors, separate from canonical Default recognition and shared gameplay checks.

Latest read-only WAL snapshot: 13,916 seeds / 5,566,400 orientation records. The query selected 253,200 distinct region choices / 263,076 orientations in 33.655 seconds.

A distinct region is `(seed, xmin, zmin, size)`. Four rotations of one region count once toward region breadth. Different bounds or sizes remain alternative choices, even when they overlap. They are **not independent pieces of geography**. Every ranked seed also reports a deterministic greedy pairwise-disjoint selection (a lower bound, not the maximum) and exact horizontal union area, counting overlaps only once. `--rank-by disjoint-regions` can prioritize that more conservative breadth measure.

| Seed | Distinct region choices | Passing orientations | Greedy disjoint regions |
|---|---:|---:|---:|
| 910003612 | 40 | 44 | 8 |
| 910012671 | 38 | 42 | 6 |
| 910000376 | 37 | 40 | 9 |
| 910002553 | 37 | 37 | 9 |
| 910011826 | 37 | 40 | 8 |
| 910013895 | 37 | 37 | 8 |
| 910011616 | 37 | 38 | 8 |
| 910009848 | 37 | 39 | 8 |
| 910007064 | 37 | 38 | 4 |
| 910005294 | 36 | 38 | 9 |
| 910004849 | 36 | 40 | 8 |
| 910005821 | 36 | 36 | 8 |
| 910001525 | 36 | 38 | 8 |
| 910004995 | 36 | 39 | 7 |
| 910001247 | 36 | 39 | 7 |

The nested looser query selected 1,064,061 region choices / 1,486,137 orientations on the **same transaction snapshot**. SQL set differences verify that no strict region or orientation is absent from the looser result. This avoids mistaking newly appended worker data for an effect of loosening thresholds.

- [Latest policy](runs/five-hour-continuation-910013916-5566400-94dec1877099-075903675996/strict/policy.json)
- [Top seeds](runs/five-hour-continuation-910013916-5566400-94dec1877099-075903675996/strict/top-seeds.json)
- [All ranked seed counts](runs/five-hour-continuation-910013916-5566400-94dec1877099-075903675996/strict/all-ranked-seed-counts.json)
- [Window and full-payload references](runs/five-hour-continuation-910013916-5566400-94dec1877099-075903675996/strict/window-references.jsonl)
- [Snapshot summary](runs/five-hour-continuation-910013916-5566400-94dec1877099-075903675996/strict/summary.json)
- [Nested-policy verification](runs/five-hour-continuation-910013916-5566400-94dec1877099-075903675996/nested-policy-verification.json)
- [Latest run pointer](latest.json)

Top-window references contain inclusive bounds, all passing rotations and exact gzip file/line references. Each region appears once per seed; one passing orientation is selected as a representative. References from the greedy disjoint selection are shown first. Every producer window and orientation remains available in the source database and compressed payloads, including observations that fail this particular policy.

`matching_distinct_regions` and `matching_orientations` describe all observations passing window thresholds, before `min_distinct_regions` filters the seed ranking. No threshold defaults are design decisions. Native non-ocean biome samples can include river/inland water, flat-end cells are coarse developability proxies, and the native mirror ratio is not the actual shared compiler's symmetry proof. Missing mirror values are excluded only when a maximum mirror ratio is requested; their snapshot count is reported.

Run a configurable query:

```sh
python3 tools/analysis/query_default_catalogue.py --min-ocean-gradient 0.1 --min-western-height-advantage 3 --max-ocean-fraction 0.75 --min-end-flat-cells 12 --max-mirror-ratio 0.3 --sizes 1024,1536,2048,3072 --compare-looser
```

Reapply or loosen a saved policy without terrain work:

```sh
python3 tools/analysis/query_default_catalogue.py --policy /absolute/path/to/policy.json --min-ocean-gradient 0 --min-western-height-advantage 0 --min-end-flat-cells 4 --max-mirror-ratio 0.7 --label looser
```

Available controls: `--min-ocean-fraction`, `--max-ocean-fraction`, `--min-ocean-gradient`, `--min-western-height-advantage`, `--min-end-flat-cells`, `--max-mirror-ratio`, `--sizes`, `--rotations`, `--min-distinct-regions`, `--rank-by`, `--top-seeds`, `--windows-per-seed`, `--label`, `--source` and `--policy`. Set `max_mirror_ratio: null` in a policy JSON to remove that selector. `--compare-looser` relaxes scalar thresholds while retaining the same sizes/rotations and tests exact set inclusion.

The source is opened using `mode=ro` and `PRAGMA query_only=ON`; one brief transaction supplies both policies and their set comparison. The worker can continue writing through WAL. Runs preserve their policy, snapshot bounds and window references rather than modifying producer output.

Actual terrain, Homebase Core fit, routes, physical columns, all-nine-anchor 175-block spacing, Lair, resource ceilings and runtime readiness remain unchecked here. [Native/actual calibration](../catalogue_proxy_calibration_2026-10-14/calibration.json) documents proxy limits. [Aggregate catalogue index](../seed_catalogue_index_2026-10-14/README.md) separates evidence levels. [Scheduled continuation handoff](../seed_catalogue_continuation_2026-10-14/README.md) owns search continuation.
