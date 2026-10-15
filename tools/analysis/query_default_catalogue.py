"""Query retained Default native measurements; every result remains unqualified.

Reads one consistent transaction from the producer's live SQLite WAL database.
Thresholds are user-configurable experimental queries, never design acceptance.
No terrain generation, measurement rerun, producer mutation or world authoring.
"""
from __future__ import annotations

import argparse
from collections import defaultdict
from datetime import datetime, timezone
import hashlib
import json
from pathlib import Path
import sqlite3
import time

ROOT = Path(__file__).resolve().parents[2]
SOURCE = ROOT / "implementation/worldgen/reports/default_seed_catalogue_2026-10-14/catalogue.sqlite"
OUT = ROOT / "implementation/worldgen/reports/default_catalogue_queries_2026-10-14"
DEFAULT_POLICY = {
    "min_ocean_fraction": 0.0, "max_ocean_fraction": 0.75,
    "min_ocean_gradient": 0.10, "min_western_height_advantage": 3.0,
    "min_end_flat_cells": 4, "max_mirror_ratio": None,
    "sizes": [1024, 1536, 2048, 3072], "rotations": [0, 90, 180, 270],
    "min_distinct_regions": 1, "rank_by": "distinct-regions",
}


def save(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    temporary = path.with_name(path.name + ".tmp")
    temporary.write_text(json.dumps(value, indent=2, allow_nan=False) + "\n")
    temporary.replace(path)


def canonical(value):
    return json.dumps(value, sort_keys=True, separators=(",", ":"), allow_nan=False)


def validate(policy):
    unknown = set(policy) - set(DEFAULT_POLICY)
    if unknown:
        raise ValueError(f"Unknown policy keys: {sorted(unknown)}")
    if not 0 <= policy["min_ocean_fraction"] <= policy["max_ocean_fraction"] <= 1:
        raise ValueError("Ocean fraction must satisfy 0 <= min <= max <= 1")
    if not -1 <= policy["min_ocean_gradient"] <= 1:
        raise ValueError("Ocean gradient must be between -1 and 1")
    if policy["min_end_flat_cells"] < 0 or policy["min_distinct_regions"] < 1:
        raise ValueError("End flat cells must be nonnegative and minimum distinct regions positive")
    if policy["max_mirror_ratio"] is not None and policy["max_mirror_ratio"] < 0:
        raise ValueError("Mirror bound must be nonnegative or null")
    if not policy["sizes"] or any(int(n) != n or n <= 0 for n in policy["sizes"]):
        raise ValueError("At least one positive integer size is required")
    if not policy["rotations"] or set(policy["rotations"]) - {0, 90, 180, 270}:
        raise ValueError("Rotations must be chosen from 0,90,180,270")
    if policy["rank_by"] not in {"distinct-regions", "disjoint-regions"}:
        raise ValueError("rank_by must be distinct-regions or disjoint-regions")
    return policy


def predicate(policy):
    clauses = ["ocean_fraction>=?", "ocean_fraction<=?", "ocean_gradient>=?",
               "western_height_advantage>=?", "flat_north>=?", "flat_south>=?"]
    parameters = [policy["min_ocean_fraction"], policy["max_ocean_fraction"], policy["min_ocean_gradient"],
                  policy["min_western_height_advantage"], policy["min_end_flat_cells"], policy["min_end_flat_cells"]]
    if policy["max_mirror_ratio"] is not None:
        clauses += ["mirror_ratio IS NOT NULL", "mirror_ratio<=?"]
        parameters.append(policy["max_mirror_ratio"])
    for column, values in [("size", policy["sizes"]), ("rotation", policy["rotations"])]:
        clauses.append(column + " IN (" + ",".join("?" for _ in values) + ")")
        parameters.extend(values)
    return " AND ".join(clauses), parameters


def region_key(row):
    return row["seed"], row["xmin"], row["zmin"], row["size"]


def overlaps(a, b):
    return (a["xmin"] < b["xmin"] + b["size"] and b["xmin"] < a["xmin"] + a["size"] and
            a["zmin"] < b["zmin"] + b["size"] and b["zmin"] < a["zmin"] + a["size"])


def greedy_disjoint(regions):
    """A deterministic lower-bound packing, not an exact maximum."""
    chosen = []
    for row in sorted(regions, key=lambda r: (r["size"], r["xmin"], r["zmin"])):
        if not any(overlaps(row, other) for other in chosen):
            chosen.append(row)
    return chosen


def union_area(regions):
    """Exact horizontal area of the union; overlap is counted only once."""
    xs = sorted({x for r in regions for x in [r["xmin"], r["xmin"] + r["size"]]})
    total = 0
    for left, right in zip(xs, xs[1:]):
        intervals = sorted((r["zmin"], r["zmin"] + r["size"]) for r in regions
                           if r["xmin"] < right and r["xmin"] + r["size"] > left)
        covered = 0
        end = None
        for low, high in intervals:
            if end is None or low > end:
                covered += high - low
            elif high > end:
                covered += high - end
            end = max(end, high) if end is not None else high
        total += (right - left) * covered
    return total


def summarize(db, policy, top_count, windows_per_seed):
    where, parameters = predicate(policy)
    region_rows = db.execute(f"SELECT seed,xmin,zmin,size,COUNT(*) passing_orientations FROM windows WHERE {where} GROUP BY seed,xmin,zmin,size ORDER BY seed,xmin,zmin,size", parameters)
    by_seed = defaultdict(list)
    for row in region_rows:
        by_seed[row["seed"]].append(dict(row))
    ranked = []
    all_orientations = all_regions = 0
    for seed, regions in by_seed.items():
        count = len(regions)
        orientation_count = sum(r["passing_orientations"] for r in regions)
        all_regions += count
        all_orientations += orientation_count
        if count < policy["min_distinct_regions"]:
            continue
        disjoint = greedy_disjoint(regions)
        ranked.append({"seed": seed, "distinct_matching_regions": count,
                       "matching_orientations": orientation_count,
                       "greedy_pairwise_disjoint_regions": len(disjoint),
                       "union_horizontal_area_blocks2": union_area(regions),
                       "greedy_disjoint_region_keys": [[r["xmin"], r["zmin"], r["size"]] for r in disjoint],
                       "qualified": False, "ready": False})
    primary = "distinct_matching_regions" if policy["rank_by"] == "distinct-regions" else "greedy_pairwise_disjoint_regions"
    secondary = "greedy_pairwise_disjoint_regions" if policy["rank_by"] == "distinct-regions" else "distinct_matching_regions"
    ranked.sort(key=lambda r: (-r[primary], -r[secondary], -r["union_horizontal_area_blocks2"], r["seed"]))
    top = ranked[:top_count]
    references = []
    if top:
        seed_clause = ",".join("?" for _ in top)
        columns = "seed,xmin,zmin,size,rotation,preference_count,ocean_fraction,ocean_gradient,western_height_advantage,flat_north,flat_south,mirror_ratio,payload_file,payload_line"
        rows = db.execute(f"SELECT {columns} FROM windows WHERE {where} AND seed IN ({seed_clause})", parameters + [r["seed"] for r in top])
        grouped = defaultdict(list)
        for row in rows:
            grouped[region_key(row)].append(dict(row))
        per_seed = defaultdict(list)
        for key, variants in grouped.items():
            representative = min(variants, key=lambda r: (-min(r["flat_north"], r["flat_south"]), -r["ocean_gradient"], -r["western_height_advantage"],
                                                        float("inf") if r["mirror_ratio"] is None else r["mirror_ratio"], r["rotation"]))
            seed, x, z, size = key
            entry = {"seed": seed, "bounds_inclusive": [x, x + size - 1, z, z + size - 1],
                     "size": size, "passing_rotations": sorted(r["rotation"] for r in variants),
                     "matching_orientations": len(variants), "representative": representative,
                     "orientation_payload_references": [{"rotation": r["rotation"], "gzip_path": r["payload_file"], "line": r["payload_line"]} for r in sorted(variants, key=lambda r: r["rotation"])],
                     "qualified": False, "ready": False}
            per_seed[seed].append(entry)
        for seed_rank in top:
            seed = seed_rank["seed"]
            disjoint_keys = {tuple(k) for k in seed_rank["greedy_disjoint_region_keys"]}
            entries = per_seed[seed]
            entries.sort(key=lambda r: ((r["bounds_inclusive"][0], r["bounds_inclusive"][2], r["size"]) not in disjoint_keys,
                                        -min(r["representative"]["flat_north"], r["representative"]["flat_south"]),
                                        r["size"], r["bounds_inclusive"][0], r["bounds_inclusive"][2]))
            for entry in entries[:windows_per_seed]:
                entry["in_greedy_disjoint_selection"] = (entry["bounds_inclusive"][0], entry["bounds_inclusive"][2], entry["size"]) in disjoint_keys
                references.append(entry)
    return {"matching_seeds_before_minimum_breadth": len(by_seed), "ranked_seeds": len(ranked),
            "matching_distinct_regions": all_regions, "matching_orientations": all_orientations,
            "top_seeds": top, "window_references": references,
            "all_ranked_seed_counts": [{k: v for k, v in row.items() if k != "greedy_disjoint_region_keys"} for row in ranked]}


def loosen(policy):
    result = dict(policy)
    result.update(min_ocean_fraction=max(0, policy["min_ocean_fraction"] - .1),
                  max_ocean_fraction=min(1, policy["max_ocean_fraction"] + .15),
                  min_ocean_gradient=max(-1, policy["min_ocean_gradient"] - .1),
                  min_western_height_advantage=policy["min_western_height_advantage"] - 3,
                  min_end_flat_cells=max(0, policy["min_end_flat_cells"] - 4),
                  max_mirror_ratio=None if policy["max_mirror_ratio"] is None else policy["max_mirror_ratio"] * 2 + .1,
                  min_distinct_regions=1)
    return result


def compare(db, strict, loose):
    sw, sp = predicate(strict)
    lw, lp = predicate(loose)
    result = {}
    for name, columns in [("orientations", "seed,xmin,zmin,size,rotation"), ("regions", "seed,xmin,zmin,size")]:
        sql = f"SELECT COUNT(*) FROM (SELECT DISTINCT {columns} FROM windows WHERE {sw} EXCEPT SELECT DISTINCT {columns} FROM windows WHERE {lw})"
        result["strict_" + name + "_missing_from_loose"] = db.execute(sql, sp + lp).fetchone()[0]
    result["status"] = "pass" if not any(result.values()) else "fail"
    if result["status"] != "pass":
        raise AssertionError(result)
    return result


def query(source, policy, top_count=50, windows_per_seed=10, compare_looser=False):
    db = sqlite3.connect(source.resolve().as_uri() + "?mode=ro", uri=True, timeout=10)
    db.row_factory = sqlite3.Row
    started = time.monotonic()
    try:
        db.execute("PRAGMA query_only=ON")
        db.execute("PRAGMA temp_store=MEMORY")
        db.execute("BEGIN")
        seed_counts = dict(db.execute("SELECT COUNT(*) seeds,MAX(seed) maximum_seed FROM seeds").fetchone())
        counts = dict(db.execute("SELECT COUNT(*) orientations,COUNT(DISTINCT seed) seeds_with_windows,SUM(mirror_ratio IS NULL) unknown_mirror_orientations FROM windows").fetchone())
        snapshot = {"source": str(source.resolve()), "captured_at_utc": datetime.now(timezone.utc).isoformat(),
                    "journal_mode": db.execute("PRAGMA journal_mode").fetchone()[0],
                    "read_only": True, "consistent_transaction": True,
                    "query_script_sha256": hashlib.sha256(Path(__file__).read_bytes()).hexdigest(), **seed_counts, **counts}
        strict_result = summarize(db, policy, top_count, windows_per_seed)
        loose_policy = loosen(policy) if compare_looser else None
        loose_result = summarize(db, loose_policy, top_count, windows_per_seed) if loose_policy else None
        validation = compare(db, policy, loose_policy) if loose_policy else None
        db.rollback()
    finally:
        db.close()
    return snapshot, strict_result, loose_policy, loose_result, validation, round(time.monotonic() - started, 3)


def export_policy(directory, policy, result, snapshot):
    save(directory / "policy.json", {"schema": "default_catalogue_query_policy/1", "design_status": "Prototype/test query", "thresholds": policy,
                                     "qualification": "No result is qualified; thresholds only select retained native proxy observations."})
    summary = {"schema": "default_catalogue_query/1", "snapshot": snapshot, "policy": policy,
               **{k: v for k, v in result.items() if k not in {"top_seeds", "window_references", "all_ranked_seed_counts"}},
               "qualified": False, "ready": False}
    save(directory / "summary.json", summary)
    save(directory / "top-seeds.json", result["top_seeds"])
    save(directory / "all-ranked-seed-counts.json", result["all_ranked_seed_counts"])
    with (directory / "window-references.jsonl").open("w") as handle:
        for row in result["window_references"]:
            handle.write(canonical(row) + "\n")


def csv_ints(value):
    return sorted(set(int(part) for part in value.split(",")))


def overview(run, latest, result, loose_result):
    relative = run.relative_to(OUT)
    snapshot = latest["snapshot"]
    lines = ["# Requery retained Default measurements — 14 October 2026", "",
             "**Prototype/test. No results are qualified maps.** This lane queries the existing native SQLite catalogue without generating terrain or rerunning measurements. All thresholds are configurable experimental selectors, separate from canonical Default recognition and shared gameplay checks.", "",
             f"Latest read-only WAL snapshot: {snapshot['seeds']:,} seeds / {snapshot['orientations']:,} orientation records. The query selected {result['matching_distinct_regions']:,} distinct region choices / {result['matching_orientations']:,} orientations in {latest['elapsed_seconds']} seconds.", "",
             "A distinct region is `(seed, xmin, zmin, size)`. Four rotations of one region count once toward region breadth. Different bounds or sizes remain alternative choices, even when they overlap. They are **not independent pieces of geography**. Every ranked seed also reports a deterministic greedy pairwise-disjoint selection (a lower bound, not the maximum) and exact horizontal union area, counting overlaps only once. `--rank-by disjoint-regions` can prioritize that more conservative breadth measure.", "",
             "| Seed | Distinct region choices | Passing orientations | Greedy disjoint regions |", "|---|---:|---:|---:|"]
    for row in result["top_seeds"][:15]:
        lines.append(f"| {row['seed']} | {row['distinct_matching_regions']} | {row['matching_orientations']} | {row['greedy_pairwise_disjoint_regions']} |")
    if loose_result:
        lines += ["", f"The nested looser query selected {loose_result['matching_distinct_regions']:,} region choices / {loose_result['matching_orientations']:,} orientations on the **same transaction snapshot**. SQL set differences verify that no strict region or orientation is absent from the looser result. This avoids mistaking newly appended worker data for an effect of loosening thresholds."]
    lines += ["", f"- [Latest policy]({relative}/strict/policy.json)", f"- [Top seeds]({relative}/strict/top-seeds.json)",
              f"- [All ranked seed counts]({relative}/strict/all-ranked-seed-counts.json)",
              f"- [Window and full-payload references]({relative}/strict/window-references.jsonl)",
              f"- [Snapshot summary]({relative}/strict/summary.json)", "- [Latest run pointer](latest.json)", "",
              "Top-window references contain inclusive bounds, all passing rotations and exact gzip file/line references. Each region appears once per seed; one passing orientation is selected as a representative. References from the greedy disjoint selection are shown first. Every producer window and orientation remains available in the source database and compressed payloads, including observations that fail this particular policy.", "",
              "`matching_distinct_regions` and `matching_orientations` describe all observations passing window thresholds, before `min_distinct_regions` filters the seed ranking. No threshold defaults are design decisions. Native non-ocean biome samples can include river/inland water, flat-end cells are coarse developability proxies, and the native mirror ratio is not the actual shared compiler's symmetry proof. Missing mirror values are excluded only when a maximum mirror ratio is requested; their snapshot count is reported.", "",
              "Run a configurable query:", "", "```sh",
              "python3 tools/analysis/query_default_catalogue.py --min-ocean-gradient 0.1 --min-western-height-advantage 3 --max-ocean-fraction 0.75 --min-end-flat-cells 12 --max-mirror-ratio 0.3 --sizes 1024,1536,2048,3072 --compare-looser",
              "```", "", "Reapply or loosen a saved policy without terrain work:", "", "```sh",
              "python3 tools/analysis/query_default_catalogue.py --policy /absolute/path/to/policy.json --min-ocean-gradient 0 --min-western-height-advantage 0 --min-end-flat-cells 4 --max-mirror-ratio 0.7 --label looser",
              "```", "",
              "Available controls: `--min-ocean-fraction`, `--max-ocean-fraction`, `--min-ocean-gradient`, `--min-western-height-advantage`, `--min-end-flat-cells`, `--max-mirror-ratio`, `--sizes`, `--rotations`, `--min-distinct-regions`, `--rank-by`, `--top-seeds`, `--windows-per-seed`, `--label`, `--source` and `--policy`. Set `max_mirror_ratio: null` in a policy JSON to remove that selector. `--compare-looser` relaxes scalar thresholds while retaining the same sizes/rotations and tests exact set inclusion.", "",
              "The source is opened using `mode=ro` and `PRAGMA query_only=ON`; one brief transaction supplies both policies and their set comparison. The worker can continue writing through WAL. Runs preserve their policy, snapshot bounds and window references rather than modifying producer output.", "",
              "Actual terrain, Homebase Core fit, routes, physical columns, all-nine-anchor 175-block spacing, Lair, resource ceilings and runtime readiness remain unchecked here. [Native/actual calibration](../catalogue_proxy_calibration_2026-10-14/calibration.json) documents proxy limits. [Aggregate catalogue index](../seed_catalogue_index_2026-10-14/README.md) separates evidence levels. [Scheduled continuation handoff](../seed_catalogue_continuation_2026-10-14/README.md) owns search continuation."]
    if loose_result:
        lines.insert(lines.index("- [Latest run pointer](latest.json)"), f"- [Nested-policy verification]({relative}/nested-policy-verification.json)")
    (OUT / "README.md").write_text("\n".join(lines) + "\n")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--source", type=Path, default=SOURCE)
    parser.add_argument("--policy", type=Path, help="Saved policy JSON; may contain a thresholds object")
    for name in ["min-ocean-fraction", "max-ocean-fraction", "min-ocean-gradient", "min-western-height-advantage", "max-mirror-ratio"]:
        parser.add_argument("--" + name, type=float)
    parser.add_argument("--min-end-flat-cells", type=int)
    parser.add_argument("--min-distinct-regions", type=int)
    parser.add_argument("--sizes", type=csv_ints)
    parser.add_argument("--rotations", type=csv_ints)
    parser.add_argument("--rank-by", choices=["distinct-regions", "disjoint-regions"])
    parser.add_argument("--top-seeds", type=int, default=50)
    parser.add_argument("--windows-per-seed", type=int, default=10)
    parser.add_argument("--compare-looser", action="store_true", help="Verify nested looser policy on the exact same read snapshot")
    parser.add_argument("--label", default="default-breadth")
    args = parser.parse_args()
    policy = dict(DEFAULT_POLICY)
    if args.policy:
        document = json.loads(args.policy.read_text())
        policy.update(document.get("thresholds", document))
    for key in DEFAULT_POLICY:
        value = getattr(args, key, None)
        if value is not None:
            policy[key] = value
    validate(policy)
    if not 1 <= args.top_seeds <= 1000 or not 1 <= args.windows_per_seed <= 100:
        parser.error("top-seeds must be 1..1000 and windows-per-seed 1..100")
    if any(c not in "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789-_" for c in args.label) or not args.label:
        parser.error("label must contain letters, numbers, hyphens or underscores")
    snapshot, result, loose_policy, loose_result, validation, elapsed = query(args.source, policy, args.top_seeds, args.windows_per_seed, args.compare_looser)
    policy_hash = hashlib.sha256(canonical(policy).encode()).hexdigest()[:12]
    stamp = datetime.now(timezone.utc).strftime("%H%M%S%f")
    run = OUT / "runs" / f"{args.label}-{snapshot['maximum_seed']}-{snapshot['orientations']}-{policy_hash}-{stamp}"
    export_policy(run / "strict", policy, result, snapshot)
    if loose_policy:
        export_policy(run / "looser", loose_policy, loose_result, snapshot)
        save(run / "nested-policy-verification.json", {"snapshot": snapshot, "verification": validation,
                                                      "strict_regions": result["matching_distinct_regions"], "looser_regions": loose_result["matching_distinct_regions"],
                                                      "strict_orientations": result["matching_orientations"], "looser_orientations": loose_result["matching_orientations"]})
    latest = {"run": str(run), "snapshot": snapshot, "elapsed_seconds": elapsed,
              "matching_seeds": result["ranked_seeds"], "matching_distinct_regions": result["matching_distinct_regions"],
              "matching_orientations": result["matching_orientations"], "qualified": False}
    save(OUT / "latest.json", latest)
    overview(run, latest, result, loose_result)
    print(json.dumps(latest), flush=True)


if __name__ == "__main__":
    main()
