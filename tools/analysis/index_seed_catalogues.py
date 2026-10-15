"""Incrementally index read-only seed catalogues without promoting evidence levels.

Owns only seed_catalogue_index_2026-10-14. Concurrent workers own every input.
Native proxies, actual partial compilations and implemented shared checks remain
separate. A missing measurement is unknown, never a failed geography test.
"""
from __future__ import annotations

import argparse
from collections import Counter
from datetime import datetime, timezone
import fcntl
import hashlib
import html
import json
import os
from pathlib import Path
import sqlite3
import subprocess
import sys
import time

ROOT = Path(__file__).resolve().parents[2]
REPORTS = ROOT / "implementation/worldgen/reports"
OUT = REPORTS / "seed_catalogue_index_2026-10-14"
SCHEMA = "seed_catalogue_evidence/1"
ADAPTER_VERSION = 4
USER_REJECTED = {890000020, 890001592}
LANES = {
    "default_seed_catalogue_2026-10-14": "Default native measurements",
    "default_actual_catalogue_2026-10-14": "Default existing actual terrain",
    "loose_scoop_bank_2026-10-14": "Earlier archipelago loose bank",
    "default_scoop_bank_2026-10-11": "Historical Default tutor bank",
    "archipelago_chain_search_2026-10-12": "Earlier archipelago shared checks",
    "archipelago_qualified_search_2026-10-12": "Earlier archipelago shared checks",
}


def compact(value):
    return json.dumps(value, separators=(",", ":"), sort_keys=True, allow_nan=False)


def save(path, value):
    temp = path.with_name(path.name + ".tmp")
    temp.write_text(json.dumps(value, indent=2, allow_nan=False) + "\n")
    temp.replace(path)


def clean(value):
    """Keep non-finite source floats explicit and JSON interoperable."""
    if isinstance(value, float) and (value != value or abs(value) == float("inf")):
        return {"non_finite_source_value": str(value)}
    if isinstance(value, dict):
        return {str(k): clean(v) for k, v in value.items()}
    if isinstance(value, list):
        return [clean(v) for v in value]
    return value


def first(*values):
    return next((v for v in values if v is not None), None)


def normalize(raw, source, position):
    raw = clean(raw)
    target = raw.get("target", {})
    evidence = raw.get("evidence", {})
    measured = first(raw.get("measured"), raw.get("measurements"), target.get("measured"), {})
    region = first(raw.get("region"), evidence.get("region"), {})
    orient = first(raw.get("orientation"), evidence.get("orientation"), {})
    seed = first(raw.get("seed"), target.get("seed"))
    if seed is None:
        return None
    seed = int(seed)
    lane = source.parent.name
    center = first(raw.get("centre"), raw.get("center"), target.get("centre"))
    size = first(raw.get("size"), raw.get("size_blocks"), measured.get("scoop_blocks"))
    bounds = first(raw.get("bounds"), raw.get("block_bounds"), region.get("block_bounds"), target.get("block_bounds"))
    bounds_convention = raw.get("bounds_convention", "source convention retained; inspect source")
    if bounds is None and center is not None and size is not None:
        if isinstance(size, (int, float)):
            size = [size, size]
        bounds = [center[0] - size[0] / 2, center[0] + size[0] / 2,
                  center[1] - size[1] / 2, center[1] + size[1] / 2]
        bounds_convention = "half-open bounds derived from center and size"
    if lane == "loose_scoop_bank_2026-10-14" and raw.get("bounds") is not None:
        bounds_convention = "inclusive actual block bounds"
    rotation = first(raw.get("rotation"), raw.get("rotation_degrees"), orient.get("rotation_degrees_clockwise"))
    axis = first(raw.get("axis"), raw.get("team_axis"), target.get("team_axis"), measured.get("team_axis"))
    reflection = first(orient.get("east_west_reflected"), raw.get("east_west_reflected"), raw.get("reflected"))
    identity = {"seed": seed, "bounds": bounds, "bounds_convention": bounds_convention,
                "center": center, "size": size,
                "orientation": {"axis": axis, "rotation_degrees_clockwise": rotation,
                                "east_west_reflected": reflection},
                "placement_variant": first(raw.get("core_pair_variant"), raw.get("placement_variant"))}
    # Different orientations and coordinate conventions must not be merged by guesswork.
    scoop_id = hashlib.sha256(compact(identity).encode()).hexdigest()[:24]
    source_rel = str(source.relative_to(ROOT))
    record_id = hashlib.sha256((source_rel + "\n" + compact(raw)).encode()).hexdigest()[:24]
    shared_pass = (raw.get("deepest_stage_reached") == "characterize" and
                   not raw.get("rejections") and raw.get("status") in
                   {"shared_checks_pass", "qualified", "pass"})
    is_actual = lane == "default_actual_catalogue_2026-10-14" or bool(raw.get("source_surface")) or bool(evidence)
    level = "implemented_shared_checks" if shared_pass else ("actual_partial" if is_actual else "native_proxy")
    reject_fields = ["rejections", "recognition_rejections", "homebase_rejections", "screen_rejections", "errors"]
    failures = {k: raw[k] for k in reject_fields if raw.get(k)}
    for key in ["failed", "failed_checks", "default_template_failures", "default_screen_failed", "rejection_codes"]:
        if raw.get(key):
            failures[key] = raw[key]
    source_status = first(raw.get("status"), raw.get("classification"), "measured; no status supplied")
    unknowns = first(raw.get("not_checked"), raw.get("unknown_checks"), raw.get("unknowns"), raw.get("unverified"), [])
    if not isinstance(unknowns, list):
        unknowns = [unknowns]
    unknowns = list(unknowns)
    unknowns += ["regional metric unavailable: " + key for key in raw.get("unavailable_metrics", [])]
    if level == "native_proxy":
        unknowns += ["actual vanilla surface terrain", "exact physical structure fit", "full gameplay qualification", "runtime readiness"]
    elif level == "actual_partial":
        unknowns += ["full gameplay qualification", "runtime readiness"]
    else:
        unknowns += ["runtime readiness", "full boss arena volume", "measured boat travel and discovery times"]
    if bounds is None:
        unknowns.append("window bounds not supplied")
    if rotation is None:
        unknowns.append("directed team rotation not supplied; axis does not determine assignment")
    review = raw.get("archipelago_type_review", {})
    excluded = seed in USER_REJECTED or raw.get("user_rejected_seed") is True or review.get("status") == "reject"
    exclusion_reason = "user-rejected reference seed retained as historical evidence" if seed in USER_REJECTED else review.get("reason")
    # Preserve measured vectors, checks and failure values verbatim; the source
    # reference retains complete detailed evidence without duplicating huge grids.
    metrics = dict(measured) if isinstance(measured, dict) else {"source_measured": measured}
    for key, value in raw.items():
        if key.startswith(("approximate_", "actual_")) or key.endswith(("_pass", "_checks")) or key in {
            "score", "sampled_islands", "island_area_proxies", "sample_spacing", "sample_spacing_blocks",
            "symmetry", "water_fraction", "relief_blocks", "homebase_quality", "fountains",
            "fountain_horizontal_separation", "partial_front_end_pass", "default_template_fit",
            "checks", "gates", "thresholds", "filter_snapshot", "type_matches", "types",
            "prospect_types", "types_matched", "minimum_distance", "minimum_anchor_distance",
            "spacing_audit", "independent_spacing_audit", "criteria", "metrics", "gate_results",
            "stage_c", "experimental_screen", "fitted_homelands", "recommended_bank_eligible",
            "unavailable_metrics", "measurement_scope", "provenance",
        }:
            metrics[key] = value
    if evidence:
        for key in ["metrics", "regional", "default_template_fit", "orientation", "prospect_types", "homelands", "fountains", "hinterland"]:
            if key in evidence:
                metrics[key] = evidence[key]
    return {
        "schema": SCHEMA, "record_id": record_id, "scoop_id": scoop_id,
        **identity, "catalogue": lane, "lane": LANES[lane], "evidence_level": level,
        "source_status": source_status, "source": {"path": source_rel, "position": position,
                                                     "job_id": raw.get("id"), "surface": raw.get("source_surface"),
                                                     "surface_sha256": raw.get("source_sha256"), "compiler_sha256": raw.get("compiler_sha256")},
        "measured": metrics, "failures": failures, "unknowns": sorted(set(map(str, unknowns))),
        "implemented_shared_checks_pass": shared_pass,
        "fully_gameplay_qualified": False, "runtime_ready": raw.get("ready") is True,
        "candidate_excluded": excluded, "exclusion_reason": exclusion_reason,
        "minimum_spacing_for_future_qualification_blocks": 175,
        "source_qualified_flag": raw.get("qualified"),
    }


def connect():
    db = sqlite3.connect(OUT / "catalogue.sqlite3")
    db.execute("CREATE TABLE IF NOT EXISTS sources(path TEXT PRIMARY KEY, inode INT, size INT, mtime INT, offset INT, lines INT)")
    db.execute("CREATE TABLE IF NOT EXISTS records(record_id TEXT PRIMARY KEY, source TEXT, seed INT, scoop_id TEXT, catalogue TEXT, level TEXT, excluded INT, shared_pass INT, payload TEXT)")
    db.execute("CREATE INDEX IF NOT EXISTS records_seed ON records(seed)")
    db.execute("CREATE TABLE IF NOT EXISTS native_seeds(seed INT PRIMARY KEY, source TEXT, regions INT, orientations INT, payload TEXT)")
    db.execute("CREATE TABLE IF NOT EXISTS supplements(job_id TEXT PRIMARY KEY, source TEXT, payload TEXT)")
    db.execute("CREATE TABLE IF NOT EXISTS index_settings(key TEXT PRIMARY KEY, value TEXT)")
    previous_adapter = db.execute("SELECT value FROM index_settings WHERE key='adapter_version'").fetchone()
    if previous_adapter is None or previous_adapter[0] != str(ADAPTER_VERSION):
        for table in ["sources", "records", "native_seeds", "supplements"]:
            db.execute(f"DELETE FROM {table}")
        db.execute("INSERT OR REPLACE INTO index_settings VALUES('adapter_version',?)", (str(ADAPTER_VERSION),))
        db.commit()
    return db


def source_files():
    files = []
    for lane in LANES:
        directory = REPORTS / lane
        if not directory.exists():
            continue
        if lane == "loose_scoop_bank_2026-10-14":
            files += [(directory / name, "jsonl") for name in ["loose-scoops.jsonl", "partial-compile-index.jsonl"]]
        elif lane == "default_scoop_bank_2026-10-11":
            files.append((directory / "candidate-bank.json", "list"))
        elif lane.startswith("archipelago_"):
            files.append((directory / "qualified-summary.json", "qualified"))
        elif lane == "default_seed_catalogue_2026-10-14":
            files.append((directory / "seed-index.jsonl", "native_seed_feed"))
        else:
            # Each new worker owns its JSONL naming. Seed-level breadth and errors
            # are checkpoint metadata, not independent scoop evidence records.
            for path in sorted(directory.glob("*.jsonl")):
                if not any(term in path.stem for term in ["error", "seed-index", "events", "log"]):
                    files.append((path, "actual_supplement" if path.name == "regional-vector-supplements.jsonl" else "jsonl"))
    return [(p, kind) for p, kind in files if p.exists()]


def ingest(db, path, kind, diagnostics):
    stat = path.stat()
    key = str(path.relative_to(ROOT))
    previous = db.execute("SELECT inode,size,mtime,offset,lines FROM sources WHERE path=?", (key,)).fetchone()
    if previous and previous[0] == stat.st_ino and previous[1] == stat.st_size and previous[2] == stat.st_mtime_ns:
        return 0
    is_jsonl = kind in {"jsonl", "native_seed_feed", "actual_supplement"}
    append_only = is_jsonl and previous and previous[0] == stat.st_ino and stat.st_size >= previous[3]
    offset, line_number = (previous[3], previous[4]) if append_only else (0, 0)
    if not append_only:
        db.execute("DELETE FROM records WHERE source=?", (key,))
        db.execute("DELETE FROM native_seeds WHERE source=?", (key,))
        db.execute("DELETE FROM supplements WHERE source=?", (key,))
    count = 0

    def add(raw, position):
        nonlocal count
        if not isinstance(raw, dict):
            diagnostics.append({"source": key, "position": position, "error": "record is not an object"})
            return
        if kind == "native_seed_feed":
            db.execute("INSERT OR REPLACE INTO native_seeds VALUES(?,?,?,?,?)",
                       (raw["seed"], key, raw["distinct_regions_measured"], raw["orientations_measured"], compact(clean(raw))))
            count += 1
            return
        if kind == "actual_supplement":
            if raw.get("supplemental_kind") != "raw_regional_measurement_backfill":
                raise ValueError("Unexpected supplemental evidence kind")
            item = {"source": {"path": key, "position": position}, "evidence": clean(raw)}
            db.execute("INSERT OR REPLACE INTO supplements VALUES(?,?,?)", (raw["id"], key, compact(item)))
            count += 1
            return
        record = normalize(raw, path, position)
        if record is None:
            diagnostics.append({"source": key, "position": position, "error": "record lacks seed"})
            return
        db.execute("INSERT OR REPLACE INTO records VALUES(?,?,?,?,?,?,?,?,?)",
                   (record["record_id"], key, record["seed"], record["scoop_id"], record["catalogue"],
                    record["evidence_level"], record["candidate_excluded"], record["implemented_shared_checks_pass"], compact(record)))
        count += 1

    if is_jsonl:
        with path.open("rb") as handle:
            handle.seek(offset)
            # Snapshot only initial byte length; never chase a concurrent writer.
            data = handle.read(max(0, stat.st_size - offset))
        boundary = data.rfind(b"\n") + 1
        for line in data[:boundary].splitlines():
            line_number += 1
            if not line.strip():
                continue
            try:
                add(json.loads(line), {"line": line_number})
            except (ValueError, TypeError, KeyError) as error:
                diagnostics.append({"source": key, "line": line_number, "error": repr(error)})
        offset += boundary
        if boundary < len(data):
            diagnostics.append({"source": key, "pending_bytes": len(data) - boundary,
                                "note": "incomplete final line deferred until next refresh"})
    else:
        try:
            document = json.loads(path.read_text())
            rows = document if kind == "list" else document.get("qualified", [])
            for index, raw in enumerate(rows):
                add(raw, {"array": "qualified" if kind == "qualified" else None, "index": index})
            offset = stat.st_size
        except (ValueError, TypeError, KeyError) as error:
            diagnostics.append({"source": key, "error": repr(error), "note": "input may be updating; retry next refresh"})
            db.rollback()
            return count
    db.execute("INSERT OR REPLACE INTO sources VALUES(?,?,?,?,?,?)", (key, stat.st_ino, stat.st_size, stat.st_mtime_ns, offset, line_number))
    db.commit()
    return count


def metadata():
    result = {}
    for lane in LANES:
        directory = REPORTS / lane
        if not directory.exists():
            result[lane] = {"status": "worker output not yet created"}
            continue
        snapshots = {}
        for path in sorted(directory.glob("*.json")):
            if path.stem in {"checkpoint", "summary", "inputs", "settings", "manifest", "config"} and path.stat().st_size < 100_000:
                try:
                    snapshots[path.name] = json.loads(path.read_text())
                except ValueError:
                    snapshots[path.name] = {"status": "concurrent update; unavailable this snapshot"}
        result[lane] = snapshots
    return result


def overlay_supplements(db, diagnostics):
    """Enrich an existing observation by producer job ID; never add an observation."""
    supplements = {job_id: json.loads(payload) for job_id, payload in db.execute("SELECT job_id,payload FROM supplements")}
    applied_ids = set()
    enriched_records = 0
    for record_id, payload in db.execute("SELECT record_id,payload FROM records WHERE catalogue='default_actual_catalogue_2026-10-14'").fetchall():
        record = json.loads(payload)
        job_id = record["source"].get("job_id")
        item = supplements.get(job_id)
        if item is None:
            continue
        extra = item["evidence"]
        if (extra["seed"] != record["seed"] or extra["bounds"] != record["bounds"] or
                extra["rotation"] != record["orientation"]["rotation_degrees_clockwise"] or
                extra["reflected"] != record["orientation"]["east_west_reflected"] or
                (record["source"].get("surface_sha256") and
                 extra.get("source_sha256") != record["source"]["surface_sha256"])):
            diagnostics.append({"source": item["source"], "job_id": job_id, "error": "supplement identity/source digest does not match original observation"})
            continue
        original_metrics = record["measured"].get("metrics", {})
        conflicting = [key for key, value in extra["metrics"].items() if key in original_metrics and original_metrics[key] != value]
        if conflicting:
            diagnostics.append({"source": item["source"], "job_id": job_id, "error": "supplement conflicts with retained raw metrics", "keys": conflicting})
            continue
        record["measured"]["metrics"] = {**original_metrics, **extra["metrics"]}
        record["regional_measurement_backfill"] = item
        record["unknowns"] = sorted(set(record["unknowns"] + ["regional metric unavailable: " + key for key in extra.get("unavailable_metrics", [])]))
        updated = compact(record)
        if updated != payload:
            db.execute("UPDATE records SET payload=? WHERE record_id=?", (updated, record_id))
        applied_ids.add(job_id)
        enriched_records += 1
    db.commit()
    return {"supplemental_job_ids": len(supplements), "applied_job_ids": len(applied_ids),
            "enriched_existing_observations": enriched_records,
            "pending_job_ids_without_matching_observation": sorted(set(supplements) - applied_ids),
            "new_observations_added": 0, "acceptance_changed": False}


def export(db, added, diagnostics, supplement_status):
    now = datetime.now(timezone.utc).isoformat()
    counts = []
    for row in db.execute("SELECT catalogue,level,COUNT(*),COUNT(DISTINCT seed),COUNT(DISTINCT scoop_id),SUM(excluded),SUM(shared_pass) FROM records GROUP BY catalogue,level ORDER BY catalogue,level"):
        counts.append(dict(zip(["catalogue", "evidence_level", "records", "seeds", "distinct_scoop_identities", "excluded_records", "implemented_shared_pass_records"], row)))
    normalized_totals = db.execute("SELECT COUNT(*),COUNT(DISTINCT seed),COUNT(DISTINCT scoop_id),SUM(shared_pass),SUM(excluded) FROM records").fetchone()
    native = db.execute("SELECT COUNT(*),COALESCE(SUM(regions),0),COALESCE(SUM(orientations),0) FROM native_seeds").fetchone()
    if native[0]:
        counts.append({"catalogue": "default_seed_catalogue_2026-10-14", "evidence_level": "native_proxy",
                       "records": native[2], "seeds": native[0], "distinct_scoop_identities": native[2],
                       "distinct_regions": native[1], "excluded_records": 0, "implemented_shared_pass_records": 0,
                       "storage": "federated source-owned gzip and SQLite; one summary per seed indexed here"})
    lane_order = {lane: index for index, lane in enumerate(LANES)}
    counts.sort(key=lambda row: (lane_order[row["catalogue"]], row["evidence_level"]))
    union_seeds = db.execute("SELECT COUNT(*) FROM (SELECT seed FROM records UNION SELECT seed FROM native_seeds)").fetchone()[0]
    totals = (normalized_totals[0] + native[2], union_seeds, normalized_totals[2] + native[2],
              normalized_totals[3], normalized_totals[4])
    checkpoint = metadata()
    summary = {"schema": "seed_catalogue_index/1", "design_status": "Prototype/test measurement catalogue",
               "generated_at_utc": now, "new_or_updated_records_this_refresh": added,
               "records": totals[0], "distinct_seeds": totals[1], "source_scoop_identities": totals[2],
               "normalized_evidence_records": normalized_totals[0],
               "federated_native_records": native[2], "federated_native_regions": native[1],
               "implemented_shared_pass_records": totals[3] or 0, "excluded_records": totals[4] or 0,
               "new_fully_qualified_maps_claimed_by_index": 0, "lanes": counts,
               "worker_metadata": checkpoint, "read_diagnostics": diagnostics,
               "regional_measurement_supplements": supplement_status,
               "identity_warning": "Seed alone is not a map. Window bounds, coordinate convention, orientation and later placement variant matter. Axis-only records are not merged with directed rotations.",
               "count_warning": "Records are evidence observations. The same seed/window can occur at multiple evidence levels; broad screens, saved rows and partial passes are different counts."}
    summary["calibration"] = {
        "path": "implementation/worldgen/reports/catalogue_proxy_calibration_2026-10-14/calibration.json",
        "role": "Paired native-vs-actual measurement evidence only; adds no candidates or seeds.",
        "sampling_limit": "Existing actual source windows are mostly archipelago-biased; not a general prevalence estimate.",
    }
    save(OUT / "summary.json", summary)
    target = OUT / "catalogue.jsonl"
    temp = target.with_name(target.name + ".tmp")
    with temp.open("w") as handle:
        for (payload,) in db.execute("SELECT payload FROM records ORDER BY catalogue,seed,scoop_id,record_id"):
            handle.write(payload + "\n")
    temp.replace(target)
    native_target = OUT / "native-seed-feeds.jsonl"
    native_temp = native_target.with_name(native_target.name + ".tmp")
    with native_temp.open("w") as handle:
        for (payload,) in db.execute("SELECT payload FROM native_seeds ORDER BY seed"):
            handle.write(payload + "\n")
    native_temp.replace(native_target)
    lines = ["# Seed catalogue index — 14 October 2026", "",
             "**Prototype/test.** Broad Default-ish measurements are the current search priority. This index also preserves the earlier archipelago lane and historical Default tutor evidence. It owns no worker output and can refresh while workers append.", "",
             f"Snapshot: {now}. **{totals[0]:,} evidence records across {totals[1]:,} distinct seeds**. The Default native lane contributes {native[2]:,} orientation records from {native[1]:,} distinct regions, stored by its worker. These are not counts of playable maps.", "",
             "| Catalogue | Evidence | Records | Seeds | Excluded references |", "|---|---|---:|---:|---:|"]
    for row in counts:
        lines.append(f"| {row['catalogue']} | {row['evidence_level']} | {row['records']:,} | {row['seeds']:,} | {row['excluded_records']:,} |")
    lines += ["", "Native proxy records retain approximate height/biome measurements and screen outcomes. Actual partial records use existing vanilla terrain but stop before full gameplay qualification. `implemented_shared_checks` means an earlier layout passed the implemented shared pipeline through characterization; it does not establish runtime readiness or the desired island-chain shape.", "",
              "A seed may supply many regions, sizes, team axes, directed orientations and placement variants. Counts across lanes overlap. Source record references remain authoritative for measurement conventions and experiment settings. A failed old tutor screen does not delete a measured prospect. Missing measurements remain unknown. User-rejected seeds 890000020 and 890001592 are retained as excluded historical references.", "",
              "The future qualification minimum remains **175 blocks across all nine major anchors**. Fountain-only separation and native Homebase proxies do not prove it. Default's canonical regional grammar and shared Homebase, route, physical fit, Lair, resource and readiness requirements still require their own stages.", "",
              "The index preserves every source-provided measured vector, check, failure and source reference. It cannot reconstruct windows or rejected native measurements that a historical worker never saved; native probe files and source manifests are therefore part of the reusable catalogue, too.", "",
              "The Default native worker retains all 400 orientations per seed rather than a winning shortlist. Its gzip files and SQLite are federated here, with one seed summary and path per seed. They are not duplicated into `catalogue.jsonl`; that file contains normalized historical and partial evidence. This keeps refresh cost small while the worker grows the full bank.", "",
              f"Actual-lane regional-vector supplements enrich {supplement_status['enriched_existing_observations']} existing observations by original producer job ID. They add no seeds, windows or evidence-observation counts and do not change original rejection or acceptance. Missing Homebase-dependent metrics remain unknown. The normalized record retains the supplemental file/line and provenance.", "",
              "- [Browse overview](index.html)", "- [Normalized evidence](catalogue.jsonl)", "- [Counts and worker checkpoints](summary.json)", "- [Schema and evidence levels](schema.md)", "- [SQLite index](catalogue.sqlite3)", "",
              "- [Native seed summaries and full-measurement paths](native-seed-feeds.jsonl)",
              "- [Native source catalogue](../default_seed_catalogue_2026-10-14/README.md)",
              "- [Actual partial source catalogue](../default_actual_catalogue_2026-10-14/README.md)",
              "- [Native versus actual calibration](../catalogue_proxy_calibration_2026-10-14/calibration.json): paired measurement evidence; no additional candidates are counted. The source sample is mostly archipelago-biased.", "",
              "- [Scheduled continuation handoff](../seed_catalogue_continuation_2026-10-14/README.md): root coordinator's instructions for resuming workers and refreshing this index.", "",
              "Refresh from the repository root: `python3 tools/analysis/index_seed_catalogues.py`. Add `--watch-seconds 600 --interval 30` for a bounded refresh loop. `--ensure-watch --watch-seconds 16200 --interval 30` starts or retains one detached 4.5-hour watcher; `watcher.json` and `watcher.log` retain progress. A lifetime watch lock and per-refresh write lock allow scheduled one-off refreshes safely. The index appends new JSONL observations to its own SQLite cache, reimports changed JSON snapshots and atomically replaces only its own exported views. Incomplete trailing JSONL records wait for the next refresh.", "",
              "Canonical sources: [maps.md](../../../../maps.md), especially “Describe, then label” and “Valley / Default”; [infrastructure.md](../../../../infrastructure.md). This index creates no playable worlds and makes no design reconciliation decision."]
    (OUT / "README.md").write_text("\n".join(lines) + "\n")
    rows = []
    for row in counts:
        cells = [row["catalogue"], row["evidence_level"], f"{row['records']:,}", f"{row['seeds']:,}", f"{row['excluded_records']:,}"]
        rows.append("<tr>" + "".join("<td>" + html.escape(str(c)) + "</td>" for c in cells) + "</tr>")
    (OUT / "index.html").write_text("""<!doctype html><meta charset="utf-8"><title>Seed catalogue index</title>
<style>body{font:16px system-ui;background:#12202b;color:#e9f0f3;max-width:1150px;margin:45px auto;padding:0 24px}h1{font-size:34px}p{line-height:1.6;max-width:960px}a{color:#7be0cc}table{border-collapse:collapse;width:100%;margin:30px 0}td,th{text-align:left;border-bottom:1px solid #3c5262;padding:12px}small{color:#afc0cd}.numbers{font-size:25px;color:#89e7cf}.tag{color:#ffc78c}</style>
<h1>Broad seed catalogue</h1><p class="tag">Prototype/test measurements • No playable-map qualification implied</p>
""" + f'<p class="numbers">{totals[1]:,} seeds · {totals[0]:,} evidence records · {totals[2]:,} scoop identities</p>' +
        "<p>Current priority: many Default-ish seeds with reusable measurements. Earlier archipelago searches remain available as a separate lane. The same seed can provide many windows; saved records, measured windows and passing maps are different counts. Full Default native vectors remain in source-owned gzip/SQLite, linked through the seed feed.</p>" +
        "<table><thead><tr><th>Catalogue</th><th>Evidence</th><th>Records</th><th>Seeds</th><th>Excluded references</th></tr></thead><tbody>" + "".join(rows) + "</tbody></table>" +
        '<p><b>Native proxy:</b> approximate terrain and biome measurements. <b>Actual partial:</b> existing vanilla terrain plus only the reported stages. <b>Implemented shared checks:</b> earlier layouts through characterization; runtime readiness and shape approval remain separate.</p>' +
        '<p>Keep the 175-block all-nine-anchor spacing for future qualification. Measurements from a looser screen are reusable evidence, not a relaxation of that requirement.</p>' +
        '<p><a href="catalogue.jsonl">Normalized evidence JSONL</a> · <a href="native-seed-feeds.jsonl">Native seed feed and measurement paths</a> · <a href="summary.json">Counts and live worker checkpoints</a> · <a href="schema.md">Schema</a> · <a href="README.md">Notes and refresh command</a></p>' +
        '<p><a href="../catalogue_proxy_calibration_2026-10-14/calibration.json">Native versus actual calibration</a> helps interpret proxy measurements. It does not add seeds or prove general accuracy across all terrain.</p>' +
        '<p><a href="../seed_catalogue_continuation_2026-10-14/README.md">Scheduled continuation handoff</a></p>' +
        f'<small>Snapshot {html.escape(now)}. {len(diagnostics)} read diagnostics. Refresh this index after worker progress.</small>')
    return summary


def refresh():
    OUT.mkdir(parents=True, exist_ok=True)
    lock = (OUT / "refresh.lock").open("a")
    try:
        fcntl.flock(lock.fileno(), fcntl.LOCK_EX | fcntl.LOCK_NB)
    except BlockingIOError:
        lock.close()
        print(json.dumps({"status": "another index refresh is running; existing snapshot retained"}), flush=True)
        return None
    diagnostics = []
    db = connect()
    try:
        added = sum(ingest(db, path, kind, diagnostics) for path, kind in source_files())
        supplement_status = overlay_supplements(db, diagnostics)
        summary = export(db, added, diagnostics, supplement_status)
    finally:
        db.close()
        lock.close()
    print(json.dumps({k: summary[k] for k in ["generated_at_utc", "records", "distinct_seeds", "source_scoop_identities", "new_or_updated_records_this_refresh"]}), flush=True)
    return summary


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--watch-seconds", type=float, default=0)
    parser.add_argument("--interval", type=float, default=30)
    parser.add_argument("--ensure-watch", action="store_true", help="Start or retain one detached bounded index watcher")
    parser.add_argument("--watch-worker", action="store_true", help=argparse.SUPPRESS)
    args = parser.parse_args()
    OUT.mkdir(parents=True, exist_ok=True)
    if args.ensure_watch:
        with (OUT / "watch.lock").open("a") as probe:
            try:
                fcntl.flock(probe.fileno(), fcntl.LOCK_EX | fcntl.LOCK_NB)
            except BlockingIOError:
                print(json.dumps({"status": "existing index watcher retained", "checkpoint": str(OUT / "watcher.json")}), flush=True)
                return
            fcntl.flock(probe.fileno(), fcntl.LOCK_UN)
        duration = args.watch_seconds or 16200
        with (OUT / "watcher.log").open("a") as log:
            child = subprocess.Popen([sys.executable, str(Path(__file__).resolve()), "--watch-worker", "--watch-seconds", str(duration), "--interval", str(args.interval)],
                                     cwd=ROOT, stdin=subprocess.DEVNULL, stdout=log, stderr=subprocess.STDOUT, start_new_session=True)
        print(json.dumps({"status": "index watcher launched", "pid": child.pid, "bounded_seconds": duration,
                          "checkpoint": str(OUT / "watcher.json")}), flush=True)
        return
    watch_lock = None
    if args.watch_seconds > 0:
        watch_lock = (OUT / "watch.lock").open("a")
        try:
            fcntl.flock(watch_lock.fileno(), fcntl.LOCK_EX | fcntl.LOCK_NB)
        except BlockingIOError:
            watch_lock.close()
            print(json.dumps({"status": "existing index watcher retained"}), flush=True)
            return
    deadline = time.monotonic() + max(0, args.watch_seconds)
    watch_state = {"status": "running", "pid": os.getpid(), "started_unix": time.time(),
                   "stop_by_unix": time.time() + max(0, args.watch_seconds), "interval_seconds": args.interval}
    try:
        while True:
            snapshot = refresh()
            if watch_lock:
                watch_state.update(updated_unix=time.time(), latest_records=snapshot.get("records") if snapshot else None,
                                   latest_distinct_seeds=snapshot.get("distinct_seeds") if snapshot else None)
                save(OUT / "watcher.json", watch_state)
            remaining = deadline - time.monotonic()
            if remaining <= 0:
                break
            time.sleep(min(max(1, args.interval), remaining, 60))
    finally:
        if watch_lock:
            watch_state.update(status="stopped_at_checkpoint", updated_unix=time.time())
            save(OUT / "watcher.json", watch_state)
            watch_lock.close()


if __name__ == "__main__":
    main()
