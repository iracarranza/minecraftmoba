# Default-ish seed catalogue — review entry

**Prototype/test. The user paused collection on 15 October 2026.** Workers and index watcher are stopped, continuation automation is paused, and STOP files remain in place. Do not run the worker/coordinator commands while reviewing. No Default result in this package is a fully qualified map.

This branch adds a compact, immutable review snapshot to `codex/archipelago-seed-review`. The existing archipelago review remains separate at [ARCHIPELAGO_REVIEW.md](ARCHIPELAGO_REVIEW.md). The new commit contains only catalogue methods and evidence; concurrent changes to gameplay, design documents and the lab are not applied here.

## Read first

- [Snapshot and exact source hashes](implementation/worldgen/reports/default_catalogue_review_2026-10-15/snapshot.json)
- [Final actual-terrain QA](implementation/worldgen/reports/default_catalogue_paused_final_qa_2026-10-15/README.md) and [stage supply](implementation/worldgen/reports/default_catalogue_paused_final_qa_2026-10-15/paused-final-summary.json)
- [Native method and limits](implementation/worldgen/reports/default_seed_catalogue_2026-10-14/README.md), [complete native integrity audit](implementation/worldgen/reports/default_seed_catalogue_2026-10-14/final-health-2026-10-15.md)
- [Saved policies, rankings and selected region references](implementation/worldgen/reports/default_catalogue_queries_2026-10-14/README.md)
- [Size coverage and threshold sensitivity](implementation/worldgen/reports/default_catalogue_queries_2026-10-14/coverage/heartbeat6h-20261015T090233Z/README.md)
- [Native/actual calibration](implementation/worldgen/reports/catalogue_proxy_calibration_2026-10-14/calibration.json)

## What was collected

| Evidence | Unique seeds | Distinct regions/windows | Orientation records |
|---|---:|---:|---:|
| Native approximation | 13,916 | 1,391,600 | 5,566,400 |
| Existing vanilla surface grids, partial evaluations | 40 | 7,606 | 46,236 |

Native seeds run from **910000001 through 910013916**. Each has 100 overlapping square region choices at four sizes and 400 directed orientations. Native collection stopped at its unchanged 4 GiB storage budget; next seed is **910013917**. Its complete archive audit passed. The complete two-seed batch caused a documented 710,374-byte stopping-estimate overshoot; that is not a raised cap or permission to restart.

Actual evaluations reslice 46 previously extracted surface grids from 40 existing seeds. The queue has 60,848 jobs; **14,612 orientation jobs remain paused at next_job 46236**. All 7,606 distinct queued windows have some orientation represented. This adds regional/orientation alternatives, not 46,236 new seeds.

| Actual partial stage | Records | Distinct windows | Existing seeds |
|---|---:|---:|---:|
| Default regional screen | 1,593 | 908 | 31 |
| Homebase partial stage | 16,074 | 4,031 | 37 |
| Combined partial front end | 217 | 204 | 21 |

These are separate stage counts, not sequential survivor totals or accepted maps. Full gameplay qualification still requires all existing checks and **at least 175 blocks between all nine major anchors**, as well as Routes/hinterlands, Lair fit/access/discoverability, resources, physical columns and readiness. Partial Fountain-pair spacing alone does not prove that requirement. User-rejected seeds 890000020 and 890001592 remain negative evidence and are never recommended.

## Data included

The package contains all 13,916 native seed summaries, all saved seed rankings and policy/window references, **all 46,236 actual partial result records**, the complete 60,848-job manifest, and the 35 append-only regional-vector supplements. Supplements amend original records by ID and add zero observations. Original JSONL bytes are losslessly gzip-compressed; source-byte SHA-256 hashes are recorded in the snapshot.

For full native evidence, eight explicitly selected sample seeds include all 400 records and the raw 64-block probe each: 910000001, 910000002, 910000376, 910003612, 910006958, 910012671, 910013915 and 910013916. Selection covers early compatibility records, high-ranked examples, a middle seed and the stopping boundary; it is not random or a rarity estimate.

[Portable file inventory](implementation/worldgen/reports/default_catalogue_review_2026-10-15/file-manifest.json) hashes every packaged source/evidence file except itself and the generated validation output. Historical audit/worker metadata describes its capture time; it does not indicate running jobs in a cloned checkout.

The multi-gigabyte SQLite databases, remaining native full-record archives/raw grids, original extracted actual surface grids (820,703,488 bytes), logs, generated worlds, runtimes and server jars remain intact locally and are excluded from this branch. Historical report links to these absent bulk files may not resolve. The combined index's metadata is included as historical context, but its full database/normalized JSONL is not duplicated. This package supports evidence and code review; it is not a self-contained world-generation or remeasurement bundle.

## Read and verify without running collection

Use Python 3's standard library to verify hashes, gzip payloads, seed identities, exact actual manifest-prefix alignment, stage counts, supplements and unqualified flags:

```sh
python3 tools/analysis/verify_default_catalogue_review.py
```

To inspect a compressed result file without installing dependencies:

```sh
python3 -m gzip -dc implementation/worldgen/reports/default_actual_catalogue_2026-10-14/actual-partials.jsonl.gz
```

The native feed is `implementation/worldgen/reports/default_seed_catalogue_2026-10-14/seed-index.jsonl.gz`. Full rankings remain readable JSON under the saved query run directories. Original absolute source paths in records are provenance; resolve their repository-relative suffix only when the matching local bulk source is available. Do not materialize worker inputs over the paused live catalogue.

## Suggested Claude review

1. Check that unique seeds, overlapping regions, orientations, partial stages and fully qualified maps remain distinct throughout producer, query and index code.
2. Review retention of failed preferences/rejections/unknowns, native commit boundaries, singleton/PID protection and exact checkpoint continuation. Confirm durable pause is respected; do not execute `--ensure`.
3. Check the read-only same-snapshot stricter/looser queries and overlap accounting. All 13,916 seeds have some region under both saved experimental policies; looser selectors add 810,861 region choices, not new seed IDs. The fixed 12-cell flat-end selector is 30% of a 1024-block sampled team end versus 3.125% at 3072 blocks; this is a size effect to assess before future balance decisions.
4. Review the native-to-actual calibration and the actual source-selection bias (36 archipelago and 10 Pale grids). Native non-ocean biome share does not establish actual dry land, flat samples do not prove Core fit, and this selected corpus cannot estimate general seed rarity.
5. Review the no-Homebase measurement supplement: 1,503 records retain 24 independent regional values with unavailable homeland bias; the 35 legacy amendments preserve original acceptance. No threshold or shared acceptance formula was changed for that repair.

The actual records all identify compiler SHA-256 `4167ab54d9a928dd79371de032776f82554b6928323e9fd014c2b07e94aef133`. The exact measured compiler and evaluator sources are preserved under [source-snapshots](implementation/worldgen/reports/default_catalogue_review_2026-10-15/source-snapshots/implementation/worldgen) for comparison. They are archival files, not active implementation changes or new design authority. In particular, concurrent main-checkout compiler changes are preserved there rather than silently applied to this branch. The active collection code is under `tools/analysis`; its existing archipelago/evaluator dependencies are inherited from the base branch. Reproduction requires restoring the matching source inputs and dependencies; review hashes and snapshots before attempting it.
