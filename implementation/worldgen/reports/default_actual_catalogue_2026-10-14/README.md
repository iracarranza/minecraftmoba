# Actual-surface Default catalogue — Prototype/test, 14 October 2026

Read-only reslicing of complete cached official-vanilla surface grids. Default regional recognition and the shared Homebase fitter retain their existing thresholds. Every orientation retains measurements and rejection reasons so later policies can be evaluated without rediscovering the seed.

`actual-partials.jsonl` is the append-only catalogue; `manifest.json` lists exact source files, SHA-256 fingerprints and pending window/orientation jobs; `checkpoint.json` is resumable. Create `STOP` to stop after the current job. Resume with the same script and an explicit duration.

Safe coordinator entry: `python tools/analysis/catalogue_actual_scoops.py --ensure --hours 4.5`; read-only status: `python tools/analysis/catalogue_actual_scoops.py --status`. New workers hold `worker.lock` for their entire lifetime and publish PID, command, start time and checkpoint in `worker.json`. Existing live PIDs, including sandbox-protected PIDs, prevent a duplicate writer. The initial PID 20533 predates the lock; its metadata bridge must be honored until it exits. Resume continues `manifest.json` at `checkpoint.next_job`, appending to existing JSONL and logs. A deliberate `STOP` file is respected.

`partial_front_end_pass` means Default regional recognition, independently acceptable sampled Homebases, two available Fountain anchors separated by at least 175 blocks, and the shared sampled symmetry bound. It does **not** establish spacing among all nine anchors, defensive ordering, Routes, Lair fit, exact columns, resources, manual balance or readiness. `recommended_bank_eligible` additionally excludes user-rejected seeds; rejected seeds remain negative evidence.

The cached source collection is biased toward earlier archipelago and pale-forest searches. This lane creates more actual regional windows, not new world seeds. Partial results and rejection vectors are useful even when the strict Default shape fails. No source worlds, canonical compiler constants or gameplay design documents are changed.

Every NO_COARSE_HOME_PAIR case also retains 24 Homebase-independent regional metrics from the unchanged evaluator formulas in a private diagnostic context. Homebase bias is explicitly unavailable; no placeholder Homebase or acceptance is retained. Older missing vectors are supplemented by original job ID in append-only `regional-vector-supplements.jsonl`; these are amendments, not new observations. `--backfill-regional` resumes that supplementation without rewriting original rows.
