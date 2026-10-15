# Seed catalogue index — 14 October 2026

**Prototype/test.** Broad Default-ish measurements are the current search priority. This index also preserves the earlier archipelago lane and historical Default tutor evidence. It owns no worker output and can refresh while workers append.

Snapshot: 2026-10-15T12:07:08.368027+00:00. **5,648,018 evidence records across 15,457 distinct seeds**. The Default native lane contributes 5,566,400 orientation records from 1,391,600 distinct regions, stored by its worker. These are not counts of playable maps.

| Catalogue | Evidence | Records | Seeds | Excluded references |
|---|---|---:|---:|---:|
| default_seed_catalogue_2026-10-14 | native_proxy | 5,566,400 | 13,916 | 0 |
| default_actual_catalogue_2026-10-14 | actual_partial | 46,236 | 40 | 2,444 |
| loose_scoop_bank_2026-10-14 | actual_partial | 146 | 3 | 2 |
| loose_scoop_bank_2026-10-14 | native_proxy | 30,764 | 1,437 | 0 |
| default_scoop_bank_2026-10-11 | native_proxy | 4,463 | 64 | 0 |
| archipelago_chain_search_2026-10-12 | implemented_shared_checks | 4 | 4 | 0 |
| archipelago_qualified_search_2026-10-12 | implemented_shared_checks | 5 | 5 | 2 |

Native proxy records retain approximate height/biome measurements and screen outcomes. Actual partial records use existing vanilla terrain but stop before full gameplay qualification. `implemented_shared_checks` means an earlier layout passed the implemented shared pipeline through characterization; it does not establish runtime readiness or the desired island-chain shape.

A seed may supply many regions, sizes, team axes, directed orientations and placement variants. Counts across lanes overlap. Source record references remain authoritative for measurement conventions and experiment settings. A failed old tutor screen does not delete a measured prospect. Missing measurements remain unknown. User-rejected seeds 890000020 and 890001592 are retained as excluded historical references.

The future qualification minimum remains **175 blocks across all nine major anchors**. Fountain-only separation and native Homebase proxies do not prove it. Default's canonical regional grammar and shared Homebase, route, physical fit, Lair, resource and readiness requirements still require their own stages.

The index preserves every source-provided measured vector, check, failure and source reference. It cannot reconstruct windows or rejected native measurements that a historical worker never saved; native probe files and source manifests are therefore part of the reusable catalogue, too.

The Default native worker retains all 400 orientations per seed rather than a winning shortlist. Its gzip files and SQLite are federated here, with one seed summary and path per seed. They are not duplicated into `catalogue.jsonl`; that file contains normalized historical and partial evidence. This keeps refresh cost small while the worker grows the full bank.

Actual-lane regional-vector supplements enrich 35 existing observations by original producer job ID. They add no seeds, windows or evidence-observation counts and do not change original rejection or acceptance. Missing Homebase-dependent metrics remain unknown. The normalized record retains the supplemental file/line and provenance.

- [Browse overview](index.html)
- [Normalized evidence](catalogue.jsonl)
- [Counts and worker checkpoints](summary.json)
- [Schema and evidence levels](schema.md)
- [SQLite index](catalogue.sqlite3)

- [Native seed summaries and full-measurement paths](native-seed-feeds.jsonl)
- [Native source catalogue](../default_seed_catalogue_2026-10-14/README.md)
- [Actual partial source catalogue](../default_actual_catalogue_2026-10-14/README.md)
- [Native versus actual calibration](../catalogue_proxy_calibration_2026-10-14/calibration.json): paired measurement evidence; no additional candidates are counted. The source sample is mostly archipelago-biased.

- [Scheduled continuation handoff](../seed_catalogue_continuation_2026-10-14/README.md): root coordinator's instructions for resuming workers and refreshing this index.

Refresh from the repository root: `python3 tools/analysis/index_seed_catalogues.py`. Add `--watch-seconds 600 --interval 30` for a bounded refresh loop. `--ensure-watch --watch-seconds 16200 --interval 30` starts or retains one detached 4.5-hour watcher; `watcher.json` and `watcher.log` retain progress. A lifetime watch lock and per-refresh write lock allow scheduled one-off refreshes safely. The index appends new JSONL observations to its own SQLite cache, reimports changed JSON snapshots and atomically replaces only its own exported views. Incomplete trailing JSONL records wait for the next refresh.

Canonical sources: [maps.md](../../../../maps.md), especially “Describe, then label” and “Valley / Default”; [infrastructure.md](../../../../infrastructure.md). This index creates no playable worlds and makes no design reconciliation decision.
