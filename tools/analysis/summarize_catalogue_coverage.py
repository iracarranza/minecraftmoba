"""Size/overlap coverage from retained native indices and saved query outputs.

One new grouped read of cached measurements; no probe, generator, producer,
archive rescan or repeat of the full seed-ranking/strict-loose query pipeline.
"""
from __future__ import annotations

from collections import Counter, defaultdict
from datetime import datetime, timezone
import gzip
import json
from pathlib import Path
import sqlite3
import statistics
import time

from query_default_catalogue import OUT, SOURCE, predicate


def save(path, value):
    temporary = path.with_name(path.name + ".tmp")
    temporary.write_text(json.dumps(value, indent=2, allow_nan=False) + "\n")
    temporary.replace(path)


def quantiles(values):
    ordered = sorted(values)
    return {"min": ordered[0], "p10": ordered[int(.10 * (len(ordered) - 1))],
            "median": statistics.median(ordered), "p90": ordered[int(.90 * (len(ordered) - 1))], "max": ordered[-1]}


def main():
    latest = json.loads((OUT / "latest.json").read_text())
    saved_run = Path(latest["run"])
    policies = {name: json.loads((saved_run / name / "policy.json").read_text())["thresholds"] for name in ["strict", "looser"]}
    saved = {name: json.loads((saved_run / name / "summary.json").read_text()) for name in policies}
    clauses = {name: predicate(policy) for name, policy in policies.items()}
    strict_where, strict_parameters = clauses["strict"]
    loose_where, loose_parameters = clauses["looser"]
    sql = f"""SELECT seed,size,COUNT(*) measured_regions,SUM(measured_orientations) measured_orientations,
        SUM(strict_orientations>0) strict_regions,SUM(strict_orientations) strict_orientations,
        SUM(looser_orientations>0) looser_regions,SUM(looser_orientations) looser_orientations
        FROM (SELECT seed,xmin,zmin,size,COUNT(*) measured_orientations,
            SUM(CASE WHEN {strict_where} THEN 1 ELSE 0 END) strict_orientations,
            SUM(CASE WHEN {loose_where} THEN 1 ELSE 0 END) looser_orientations
            FROM windows GROUP BY seed,xmin,zmin,size)
        GROUP BY seed,size ORDER BY seed,size"""
    started = time.monotonic()
    db = sqlite3.connect(SOURCE.resolve().as_uri() + "?mode=ro", uri=True, timeout=10)
    db.row_factory = sqlite3.Row
    try:
        db.execute("PRAGMA query_only=ON")
        db.execute("BEGIN")
        source_seeds = db.execute("SELECT COUNT(*),MAX(seed) FROM seeds").fetchone()
        assert source_seeds[0] == saved["strict"]["snapshot"]["seeds"], "Saved native snapshot differs; create a new saved query explicitly first"
        assert source_seeds[1] == saved["strict"]["snapshot"]["maximum_seed"]
        rows = [dict(row) for row in db.execute(sql, strict_parameters + loose_parameters)]
        db.rollback()
    finally:
        db.close()
    elapsed = round(time.monotonic() - started, 3)
    measured_orientations = sum(row["measured_orientations"] for row in rows)
    measured_regions = sum(row["measured_regions"] for row in rows)
    assert measured_orientations == saved["strict"]["snapshot"]["orientations"]
    totals_by_size = {}
    for size in sorted({row["size"] for row in rows}):
        selected = [row for row in rows if row["size"] == size]
        n = size // 64
        end_cells = (n // 3) * (n - 2 * (n // 4))
        result = {"size_blocks": size, "measured_seeds": len(selected),
                  "measured_regions": sum(r["measured_regions"] for r in selected),
                  "measured_orientations": sum(r["measured_orientations"] for r in selected),
                  "sampled_cells_per_team_end": end_cells}
        for name, policy in policies.items():
            region_count = sum(r[name + "_regions"] for r in selected)
            orientation_count = sum(r[name + "_orientations"] for r in selected)
            result[name] = {"seeds_with_at_least_one_matching_region": sum(r[name + "_regions"] > 0 for r in selected),
                            "distinct_matching_regions": region_count,
                            "matching_orientations": orientation_count,
                            "fraction_of_measured_regions_matching": round(region_count / result["measured_regions"], 6),
                            "per_seed_matching_region_count_quantiles": quantiles([r[name + "_regions"] for r in selected]),
                            "flat_non_ocean_cells_required_at_each_team_end": policy["min_end_flat_cells"],
                            "required_fraction_of_sampled_team_end_cells": round(policy["min_end_flat_cells"] / end_cells, 6)}
        result["looser_minus_strict_distinct_regions"] = result["looser"]["distinct_matching_regions"] - result["strict"]["distinct_matching_regions"]
        totals_by_size[str(size)] = result
    coverage = {}
    for name in policies:
        region_count = sum(r[name + "_regions"] for r in rows)
        orientation_count = sum(r[name + "_orientations"] for r in rows)
        assert region_count == saved[name]["matching_distinct_regions"]
        assert orientation_count == saved[name]["matching_orientations"]
        patterns = defaultdict(list)
        for row in rows:
            if row[name + "_regions"]:
                patterns[row["seed"]].append(row["size"])
        counts = Counter("+".join(map(str, sizes)) for sizes in patterns.values())
        original = json.loads((saved_run / name / "all-ranked-seed-counts.json").read_text())
        greedy_total = sum(r["greedy_pairwise_disjoint_regions"] for r in original)
        coverage[name] = {
            "unique_seeds_with_any_matching_region": len(patterns),
            "distinct_matching_regions": region_count, "matching_orientations": orientation_count,
            "seed_counts_by_matching_size_pattern": dict(sorted(counts.items())),
            "seed_counts_by_number_of_matching_sizes": dict(sorted(Counter(len(sizes) for sizes in patterns.values()).items())),
            "seed_counts_by_smallest_matching_size": dict(sorted(Counter(min(sizes) for sizes in patterns.values()).items())),
            "saved_seed_breadth_quantiles": quantiles([r["distinct_matching_regions"] for r in original]),
            "saved_greedy_disjoint_count_quantiles": quantiles([r["greedy_pairwise_disjoint_regions"] for r in original]),
            "saved_sum_greedy_disjoint_regions": greedy_total,
            "region_choices_per_saved_greedy_disjoint_selection": round(region_count / greedy_total, 4),
            "overlap_note": "Region choices can overlap. Greedy disjoint counts are saved lower-bound packings, not exact maximum independent counts.",
        }
    captured = datetime.now(timezone.utc).isoformat()
    result = {
        "schema": "default_native_size_coverage/1", "design_status": "Prototype/test descriptive policy coverage",
        "captured_at_utc": captured, "source": str(SOURCE), "saved_query_run": str(saved_run),
        "native_seeds": source_seeds[0], "maximum_seed": source_seeds[1],
        "measured_regions": measured_regions, "measured_orientations": measured_orientations,
        "new_index_aggregation_seconds": elapsed, "policies": policies,
        "by_size": totals_by_size, "coverage": coverage,
        "new_unique_seeds_gained_by_looser_policy": coverage["looser"]["unique_seeds_with_any_matching_region"] - coverage["strict"]["unique_seeds_with_any_matching_region"],
        "additional_region_choices_from_looser_policy": coverage["looser"]["distinct_matching_regions"] - coverage["strict"]["distinct_matching_regions"],
        "validation": "Size aggregates sum exactly to the existing saved same-snapshot strict/looser region and orientation counts.",
        "native_search_restarted": False, "terrain_scans": 0, "producer_processes_started": 0,
        "qualified": False, "ready": False,
        "limits": ["Native non-ocean is not actual dry land; end-flat samples are not physical Core fits.",
                   "Region bounds and sizes define choices, not independent or playable maps. Rotations are counted separately.",
                   "The same absolute flat-cell threshold imposes a different sampled-end density at different scoop sizes.",
                   "No gameplay, all-nine-anchor 175-block, Lair, resources, actual column fit or runtime qualification is performed."],
    }
    directory = OUT / "coverage" / datetime.now(timezone.utc).strftime("heartbeat6h-%Y%m%dT%H%M%SZ")
    directory.mkdir(parents=True, exist_ok=True)
    save(directory / "coverage.json", result)
    save(OUT / "coverage-latest.json", {"run": str(directory), **result})
    with gzip.open(directory / "per-seed-size-counts.jsonl.gz", "wt", compresslevel=3) as handle:
        for row in rows:
            handle.write(json.dumps(row, separators=(",", ":")) + "\n")
    lines = ["# Retained native policy coverage — 6-hour continuation", "",
             f"**Prototype/test; unqualified native approximations.** The unchanged native corpus contains {source_seeds[0]:,} seeds, {measured_regions:,} region choices and {measured_orientations:,} orientations. This report adds one grouped index read; no terrain scans or producer restarts.", "",
             "| Scoop size | Stricter region choices | Looser region choices | Seeds covered, stricter | Seeds covered, looser | Required flat-end density, stricter / looser |",
             "|---|---:|---:|---:|---:|---:|"]
    for size, row in totals_by_size.items():
        strict, loose = row["strict"], row["looser"]
        lines.append(f"| {size}² | {strict['distinct_matching_regions']:,} | {loose['distinct_matching_regions']:,} | {strict['seeds_with_at_least_one_matching_region']:,} | {loose['seeds_with_at_least_one_matching_region']:,} | {strict['required_fraction_of_sampled_team_end_cells']:.1%} / {loose['required_fraction_of_sampled_team_end_cells']:.1%} |")
    lines += ["", f"Loosening these saved preferences adds **{result['additional_region_choices_from_looser_policy']:,} region choices** and **{result['new_unique_seeds_gained_by_looser_policy']:,} new unique seeds**. Both policies already cover every retained seed somewhere; their difference is flexibility within each seed.", "",
              "The fixed absolute end-flat threshold is a size effect worth retaining: twelve cells require 30% of a 1024-block scoop's sampled end, but only 3.1% of a 3072-block scoop's sampled end. That is a descriptive consequence of the experimental query, not a balance decision or proof that a larger scoop is better.", "",
              "Distinct region choices still overlap. Across seeds, the saved greedy pairwise-disjoint selections total " + f"{coverage['strict']['saved_sum_greedy_disjoint_regions']:,} under the stricter policy and {coverage['looser']['saved_sum_greedy_disjoint_regions']:,} under the looser policy; these are conservative packings, not exact maxima. Orientation totals remain separate from region and seed counts.", "",
              "- [Full coverage and size-pattern summary](coverage.json)", "- [Per-seed size aggregates](per-seed-size-counts.jsonl.gz)",
              f"- [Original saved policies and query outputs]({saved_run.as_posix()}/strict/policy.json)", "",
              "Native non-ocean samples can include rivers/inland water. Flat-end supply, gradients and native mirror ratios are approximate measurements. Actual terrain, Homebase Core fit, routes, physical columns, all-nine-anchor 175 spacing, Lair, resources and runtime readiness remain separate."]
    (directory / "README.md").write_text("\n".join(lines) + "\n")
    print(json.dumps({"report": str(directory), "native_seeds": source_seeds[0],
                      "additional_region_choices_from_looser_policy": result["additional_region_choices_from_looser_policy"],
                      "new_unique_seeds_gained_by_looser_policy": result["new_unique_seeds_gained_by_looser_policy"],
                      "aggregation_seconds": elapsed}), flush=True)


if __name__ == "__main__":
    main()
