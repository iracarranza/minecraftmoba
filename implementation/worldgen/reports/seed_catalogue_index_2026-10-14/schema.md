# Seed catalogue evidence schema — Prototype/test

The index records evidence, not map acceptance. A seed alone does not identify a map. Reusable identity includes the seed, region bounds and coordinate convention, team orientation, reflection and, for a compiled layout, its placement variant/source record. A native axis-only record cannot be silently merged with a directed team rotation. Source measurements and source compiler settings remain authoritative.

## Storage and counts

- `catalogue.jsonl`: normalized historical native observations, actual partial compilations and earlier shared-check results. Each row uses `seed_catalogue_evidence/1`.
- `native-seed-feeds.jsonl`: one summary per seed from the new Default native worker. The worker's `measurements_path` points to all 400 measured orientations for that seed. `raw_native_grid` points to the retained biome/height probe. Full regional/biome vectors remain in those source-owned gzip files and source `catalogue.sqlite` rather than being copied into this index.
- `summary.json`: source lane counts plus worker checkpoint snapshots, read diagnostics and calibration links. `records` includes federated native orientation counts; `normalized_evidence_records` counts only rows in `catalogue.jsonl`. Distinct seed totals use a union across lanes. `source_scoop_identities` retains distinctions across source conventions and is not a claim of cross-source geometric deduplication.
- `catalogue.sqlite3`: this index's incremental cache. `records` contains normalized observations; `native_seeds` contains federated seed summaries; `sources` contains read offsets. This is distinct from either worker's SQLite files.
- Actual-lane `regional-vector-supplements.jsonl` is an amendment feed, never new observations. `supplements` stores those amendments; the index overlays their raw regional metrics by original job ID only after checking seed, bounds, rotation, reflection and source digest. Original rejection/acceptance and record identity remain unchanged. `regional_measurement_backfill` retains the amendment's source position and provenance. Missing Homebase-dependent metrics remain explicitly unavailable.

The Default native worker measures 100 regions at four directed rotations per seed. A region, an orientation record, a seed, a front-end pass and a fully qualified map are different units. Earlier archipelago native searches counted every screen pass but saved only the top 40 per seed; old counts cannot be converted into complete measurement rows after the fact. Their retained raw native grids permit later remeasurement.

## Normalized record fields

| Field | Meaning |
|---|---|
| `record_id` | Hash of source path and source payload; duplicate source observations do not inflate counts. |
| `scoop_id` | Hash of explicit seed/window/orientation metadata. Coordinate and direction ambiguity is retained. |
| `seed`, `bounds`, `bounds_convention`, `center`, `size` | Source region identity; unavailable values are null. Bounds derived from center/size are explicitly half open. |
| `orientation` | Axis, clockwise rotation and east/west reflection. Null means not supplied, never false. |
| `catalogue`, `lane`, `source` | Producer and exact relative path plus JSONL line or JSON array position. |
| `evidence_level` | `native_proxy`, `actual_partial` or `implemented_shared_checks`. |
| `measured` | Source-provided vectors, check outcomes and scalar measurements. These remain measurements even when a tutor screen rejects the region. |
| `failures` | Source rejection codes/details and failed checks. An evaluation error is not geography absence. |
| `unknowns` | Explicit unperformed stages and missing identity inputs. Missing is never converted to a zero or failure. |
| `implemented_shared_checks_pass` | Earlier layout reached characterization with no shared rejections. No runtime-ready claim follows. |
| `fully_gameplay_qualified` | Always false in this index; full gameplay/arena/runtime approval is outside its measurement scope. |
| `runtime_ready` | Only true if explicitly reported by the source; partial workers report false. |
| `candidate_excluded`, `exclusion_reason` | Historical user-rejected references remain visible but excluded. Seeds 890000020 and 890001592 are excluded references. |
| `minimum_spacing_for_future_qualification_blocks` | 175, across all nine major anchors when later qualified. It is not a statement that a partial measurement passed that check. |
| `source_qualified_flag` | Original source flag if supplied, retained without interpreting native/partial evidence as qualified. |

The normalized `source` object also retains the original producer job ID, terrain-surface path/digest and compiler digest when supplied. Inline `unavailable_metrics`, measurement scope and raw-measurement provenance remain available in `measured`, with explicit unavailable names repeated in `unknowns`. Sidecar enrichment does not increase seed, region, orientation or evidence-observation counts.

The actual Default lane retains every sampled partial result, including regional and Homebase failures. Its `partial_front_end_pass` requires the unchanged Default regional recognition, sampled Homebase acceptance, actual sampled symmetry and at least 175 blocks between its two Fountains. That checks only the Fountain pair. Defensive placement, all-nine-anchor spacing, routes/hinterland, exact physical columns, Lair, resources and runtime readiness remain separate.

The native Default lane's six experimental preferences are descriptive tutors. Their values, misses and underlying measurements are retained, including regions that meet none. They do not establish canonical Default recognition or shared gameplay acceptance. Full native payloads include west/east and seven regional bands, biome histogram, whole-window features, Homebase-end dry/flat proxies and approximate symmetry. First two smoke seeds predate an explicit river-family field; their raw biome grids remain available for remeasurement.

## Calibration and continuation

[Native/actual paired calibration](../catalogue_proxy_calibration_2026-10-14/calibration.json) supplies empirical error evidence for native proxies. It adds no catalogue seeds or candidate records. The paired actual sources are predominantly archipelago-biased, so error observations are not general terrain prevalence estimates. Native height, non-ocean biome classification and actual surface water are separate measurements.

Run `python3 tools/analysis/index_seed_catalogues.py` from the repository root. It uses only Python's standard library. JSONL reads stop at the initial file-size snapshot and defer incomplete trailing lines. Repeating the command imports only appended complete observations; changed JSON snapshots replace their own prior observations. A nonblocking process lock prevents concurrent index writers. Source workers can keep appending; no worker file is modified.

`--watch-seconds SECONDS --interval SECONDS` provides bounded periodic refresh. Long-running search workers and scheduled continuation are owned by the root coordinator, not this index. The live checkpoint metadata in `summary.json` records their last available state; the index does not infer process liveness from an old `status: running` value.

`python3 tools/analysis/index_seed_catalogues.py --ensure-watch --watch-seconds 16200 --interval 30` starts a detached watcher or retains the existing one. A lifetime `watch.lock` prevents duplicate watchers, while `refresh.lock` serializes each individual refresh with scheduled one-off invocations. `watcher.json` and `watcher.log` record the process, deadline and latest snapshot; a stale JSON status is not a process-liveness proof. The [scheduled continuation handoff](../seed_catalogue_continuation_2026-10-14/README.md) owns broader worker continuation.

This catalogue follows the canonical “describe, then label” direction in [maps.md](../../../../maps.md). It makes no change to design requirements or established thresholds and authors no playable worlds.

[Saved native catalogue queries](../default_catalogue_queries_2026-10-14/README.md) rank seeds under configurable looser or stricter experimental policies without regenerating terrain. They count distinct region choices separately from rotations, report conservative disjoint selections and union area for overlapping choices, and verify nested-policy inclusion on one read-only SQLite WAL snapshot.
