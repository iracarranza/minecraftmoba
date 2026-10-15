"""Broad, resumable native Default-ish catalogue; no recognition or qualification.

Every measured window and orientation is retained, including failed preferences.
The raw native grid makes future, looser requirements possible without rescanning.
This does not generate Minecraft chunks or write playable worlds.
"""
from __future__ import annotations

import argparse
import concurrent.futures
import fcntl
import gzip
import hashlib
import json
import os
from pathlib import Path
import signal
import sqlite3
import subprocess
import sys
import time

import numpy as np

ROOT = Path(__file__).resolve().parents[2]
OUT = ROOT / "implementation/worldgen/reports/default_seed_catalogue_2026-10-14"
ART = ROOT / "artifacts/worldgen/default_seed_catalogue_2026-10-14"
PROBE = ROOT / "artifacts/worldgen/pale_gameplay_2026-10-12/build/pale_gameplay_probe"
SCHEMA = "default_native_catalogue/1"
STEP, HALF = 64, 3072
SIZES = (16, 24, 32, 48)  # 1024, 1536, 2048, 3072 blocks.
OCEAN = (0, 10, 24, 44, 45, 46, 47, 48, 49, 50)
# IDs follow the local Cubiomes biomes.h used to build the existing probe.
FOREST = (4, 5, 18, 19, 21, 22, 23, 27, 28, 29, 30, 31, 32, 33,
          34, 132, 133, 149, 151, 155, 156, 157, 158, 160, 161, 162,
          168, 169, 178, 184, 185, 186)
COLD = (10, 11, 12, 13, 26, 30, 31, 46, 49, 50, 140, 158, 178, 179, 180, 181)
ALPINE = (3, 13, 20, 34, 131, 162, 177, 178, 179, 180, 181, 182)
OPEN = (1, 12, 35, 36, 129, 177)
ARID = (2, 17, 35, 36, 37, 38, 39, 130, 163, 164, 165, 166, 167)
WET = (6, 7, 11, 134, 184)
FAMILIES = {"ocean": OCEAN, "forest": FOREST, "cold": COLD,
            "alpine": ALPINE, "open": OPEN, "arid": ARID, "wet": WET,
            "river": (7, 11)}
UNKNOWN = ["actual_vanilla_terrain", "Homebase_Core_fit", "hinterlands_and_routes",
           "objective_placement", "all_nine_anchor_175_block_spacing", "Lair",
           "resources_and_opening_ceiling", "physical_column_fit", "runtime_READY"]


def save_json(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    tmp = path.with_suffix(path.suffix + ".tmp")
    tmp.write_text(json.dumps(value, indent=2) + "\n")
    tmp.replace(path)


def rounded(value):
    return round(float(value), 5)


def percentile(values, q):
    """Same order statistic as terrain_harvest.scoop, rather than interpolation."""
    values = np.asarray(values).ravel()
    if not values.size:
        return 0.0
    k = min(len(values) - 1, int(q * len(values)))
    return float(np.partition(values, k)[k])


def describe(height, masks, grade):
    land = ~masks["ocean"]
    result = {name: rounded(mask.mean()) for name, mask in masks.items()}
    result.update(height_quantiles=[percentile(height, q) for q in (.05, .25, .5, .75, .95)],
                  dry_relief_p95_p05=percentile(height[land], .95) - percentile(height[land], .05),
                  flat_dry_fraction=rounded((land & (grade <= 8)).mean()),
                  sampled_dry_cells=int(land.sum()))
    return result


def mirror(height, land):
    half = len(height) // 2
    north, south = height[:half], height[-half:][::-1]
    eligible = land[:half] & land[-half:][::-1]
    n = int(eligible.sum())
    relief = percentile(north[land[:half]], .95) - percentile(north[land[:half]], .05)
    raw = float(np.abs(north - south)[eligible].mean()) if n else None
    return {"paired_dry_samples": n, "mirror_mean_absolute_y": None if raw is None else rounded(raw),
            "north_dry_relief_p95_p05": relief,
            "deviation_over_relief": None if raw is None else rounded(raw / (relief or 1.0))}


def oriented_record(seed, i, j, size, rotation, height, biome, masks, grade):
    # Rotation uses np.rot90 clockwise, matching the explicit orientation metadata.
    hh = np.rot90(height, -rotation // 90)
    bb = np.rot90(biome, -rotation // 90)
    mm = {k: np.rot90(v, -rotation // 90) for k, v in masks.items()}
    gg = np.rot90(grade, -rotation // 90)
    third = max(1, size // 3)
    west = {k: rounded(v[:, :third].mean()) for k, v in mm.items()}
    east = {k: rounded(v[:, -third:].mean()) for k, v in mm.items()}
    land = ~mm["ocean"]
    # Coarse developability proxies, not physical socket fits.
    middle = slice(size // 4, size - size // 4)
    core_north = land[:third, middle] & (gg[:third, middle] <= 8)
    core_south = land[-third:, middle] & (gg[-third:, middle] <= 8)
    sockets = [int(core_north.sum()), int(core_south.sum())]
    north_land = rounded(land[:third, middle].mean())
    south_land = rounded(land[-third:, middle].mean())
    west_mean = float(hh[:, :third].mean())
    centre_mean = float(hh[:, third:-third].mean())
    ocean_gradient = east["ocean"] - west["ocean"]
    altitude_gradient = west_mean - centre_mean
    # Only descriptive experimental preferences. None discard a row.
    checks = {
        "eastern_ocean_present_proxy": east["ocean"] >= .10,
        "ocean_increases_eastward_proxy": ocean_gradient >= .10,
        "western_elevation_advantage_proxy": altitude_gradient >= 3,
        "western_cold_samples_present_proxy": bool(mm["cold"][:, :third].any()),
        "some_flat_dry_samples_at_both_team_ends_proxy": min(sockets) >= 4,
        "not_mostly_ocean_proxy": float(mm["ocean"].mean()) <= .75,
    }
    count = sum(checks.values())
    profile = []
    # Seven cross-map strips retain the gradient, without inventing biome stripes.
    for columns in np.array_split(np.arange(size), 7):
        profile.append(describe(hh[:, columns], {k: v[:, columns] for k, v in mm.items()}, gg[:, columns]))
    unique, counts = np.unique(bb, return_counts=True)
    bounds = [-HALF + i * STEP, -HALF + (i + size) * STEP - 1,
              -HALF + j * STEP, -HALF + (j + size) * STEP - 1]
    return {
        "schema": SCHEMA, "seed": seed, "bounds": bounds, "rotation": rotation,
        "size": [size * STEP, size * STEP], "sample_spacing": STEP,
        "status": "approximate_native_measurements", "qualified": False, "ready": False,
        "land_proxy_definition": "Non-ocean biome, not actual dry surface; inland and river water may be included",
        "window_id": f"{seed}:{bounds[0]}:{bounds[2]}:{size * STEP}:r{rotation}",
        "regional_vector": {"west": west, "east": east, "west_to_east_seven_bands": profile},
        "biome_histogram": {str(int(k)): int(v) for k, v in zip(unique, counts)},
        "whole_window": describe(hh, mm, gg),
        "team_end_flat_dry_cells": sockets,
        "team_end_dry_fraction": [north_land, south_land],
        "ocean_east_minus_west": rounded(ocean_gradient),
        "elevation_west_minus_centre": rounded(altitude_gradient),
        "approximate_team_axis_mirror": mirror(hh, land),
        "experimental_preferences": checks,
        "preference_count": count,
        "preference_misses": [k for k, value in checks.items() if not value],
        "unknown_checks": UNKNOWN,
    }


def read_probe(seed):
    path = ART / "probes" / f"{seed}-64.bin"
    if not path.exists():
        tmp = path.with_suffix(".tmp")
        subprocess.run([str(PROBE), str(seed), "0", "0", str(HALF), str(STEP), str(tmp)],
                       check=True, timeout=120, stdout=subprocess.DEVNULL)
        tmp.replace(path)
    with path.open("rb") as stream:
        header = stream.readline().decode().split()
        values = np.frombuffer(stream.read(), dtype=np.int16)
    n = HALF * 2 // STEP
    if header[:2] != ["palegameplay", "1"] or int(header[2]) != seed or values.size != n * n * 2:
        raise ValueError(f"Invalid native probe {path}")
    return values.reshape(n, n, 2), path


def screen(seed):
    started = time.monotonic()
    path = OUT / "windows" / f"{seed}.jsonl.gz"
    summary_path = OUT / "seeds" / f"{seed}.json"
    if path.exists() and summary_path.exists():
        return json.loads(summary_path.read_text())
    grid, probe_path = read_probe(seed)
    n = grid.shape[0]
    regional_count = orientation_count = loose_count = gradient_count = 0
    best = None
    tmp = path.with_suffix(path.suffix + ".tmp")
    with gzip.open(tmp, "wt", compresslevel=3) as stream:
        for size in SIZES:
            starts = sorted(set(int(round(x)) for x in np.linspace(0, n - size, 5)))
            for j in starts:
                for i in starts:
                    regional_count += 1
                    height = np.maximum(grid[j:j+size, i:i+size, 0].astype(float), 63)
                    biome = grid[j:j+size, i:i+size, 1]
                    masks = {k: np.isin(biome, ids) for k, ids in FAMILIES.items()}
                    dz = np.abs(np.diff(height, axis=0, append=height[-1:]))
                    dx = np.abs(np.diff(height, axis=1, append=height[:, -1:]))
                    grade = np.maximum(dx, dz)
                    for rotation in (0, 90, 180, 270):
                        record = oriented_record(seed, i, j, size, rotation, height, biome, masks, grade)
                        stream.write(json.dumps(record, separators=(",", ":")) + "\n")
                        orientation_count += 1
                        # A loose candidate just has sampled workable land at both ends.
                        if record["experimental_preferences"]["some_flat_dry_samples_at_both_team_ends_proxy"]:
                            loose_count += 1
                        if record["preference_count"] == 6:
                            gradient_count += 1
                        rank = (record["preference_count"], min(record["team_end_flat_dry_cells"]),
                                record["ocean_east_minus_west"])
                        if best is None or rank > best[0]:
                            best = rank, {k: record[k] for k in ("seed", "bounds", "rotation", "size", "window_id",
                                         "preference_count", "preference_misses", "ocean_east_minus_west",
                                         "elevation_west_minus_centre", "team_end_flat_dry_cells")}
    tmp.replace(path)
    summary = {"schema": SCHEMA, "seed": seed, "status": "approximate_native_measurements",
               "qualified": False, "ready": False, "distinct_regions_measured": regional_count,
               "orientations_measured": orientation_count, "loose_end_land_orientations": loose_count,
               "all_six_experimental_preferences_orientations": gradient_count,
               "best_descriptive_window": best[1], "measurements_path": str(path),
               "raw_native_grid": str(probe_path), "elapsed_seconds": round(time.monotonic() - started, 3),
               "unknown_checks": UNKNOWN}
    save_json(summary_path, summary)
    return summary


def open_index():
    db = sqlite3.connect(OUT / "catalogue.sqlite")
    db.execute("PRAGMA journal_mode=WAL")
    db.execute("CREATE TABLE IF NOT EXISTS seeds(seed INTEGER PRIMARY KEY, regions INTEGER, orientations INTEGER, loose_orientations INTEGER, gradient_orientations INTEGER, summary_path TEXT, measurements_path TEXT, elapsed_seconds REAL)")
    db.execute("CREATE TABLE IF NOT EXISTS windows(seed INTEGER, xmin INTEGER, zmin INTEGER, size INTEGER, rotation INTEGER, preference_count INTEGER, ocean_fraction REAL, ocean_gradient REAL, western_height_advantage REAL, flat_north INTEGER, flat_south INTEGER, mirror_ratio REAL, payload_file TEXT, payload_line INTEGER, PRIMARY KEY(seed,xmin,zmin,size,rotation))")
    db.execute("CREATE INDEX IF NOT EXISTS windows_preferences ON windows(preference_count, ocean_gradient)")
    db.commit()
    return db


def ingest(db, summary):
    seed = summary["seed"]
    if db.execute("SELECT 1 FROM seeds WHERE seed=?", (seed,)).fetchone():
        return False
    path = summary["measurements_path"]
    with gzip.open(path, "rt") as stream:
        rows = []
        for line_no, line in enumerate(stream, 1):
            row = json.loads(line)
            rows.append((seed, row["bounds"][0], row["bounds"][2], row["size"][0], row["rotation"],
                         row["preference_count"], row["whole_window"]["ocean"], row["ocean_east_minus_west"],
                         row["elevation_west_minus_centre"], *row["team_end_flat_dry_cells"],
                         row["approximate_team_axis_mirror"]["deviation_over_relief"], path, line_no))
    with db:
        db.executemany("INSERT OR REPLACE INTO windows VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?)", rows)
        db.execute("INSERT INTO seeds VALUES(?,?,?,?,?,?,?,?)", (seed, summary["distinct_regions_measured"],
                   summary["orientations_measured"], summary["loose_end_land_orientations"],
                   summary["all_six_experimental_preferences_orientations"],
                   str(OUT / "seeds" / f"{seed}.json"), path, summary["elapsed_seconds"]))
    # An append-only, compact integration feed. SQLite remains authoritative after interruption.
    with (OUT / "seed-index.jsonl").open("a") as stream:
        stream.write(json.dumps(summary, separators=(",", ":")) + "\n")
    return True


def checkpoint(db, state):
    counts = db.execute("SELECT COUNT(*),COALESCE(SUM(regions),0),COALESCE(SUM(orientations),0),COALESCE(SUM(loose_orientations),0),COALESCE(SUM(gradient_orientations),0),MAX(seed) FROM seeds").fetchone()
    state.update(unique_seeds_measured=counts[0], distinct_regions_measured=counts[1],
                 orientations_measured=counts[2], loose_end_land_orientations=counts[3],
                 all_six_experimental_preferences_orientations=counts[4],
                 next_seed=(counts[5] + 1) if counts[5] is not None else state["requested_start"],
                 updated_unix=time.time(), qualified_seeds=0)
    save_json(OUT / "checkpoint.json", state)
    return state


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--start", type=int, default=910000001)
    parser.add_argument("--max-seeds", type=int, default=100000)
    parser.add_argument("--hours", type=float, default=4.5)
    parser.add_argument("--workers", type=int, default=2, choices=(1, 2))
    parser.add_argument("--max-storage-gib", type=float, default=4.0)
    args = parser.parse_args()
    for folder in (OUT, OUT / "seeds", OUT / "windows", ART / "probes"):
        folder.mkdir(parents=True, exist_ok=True)
    lock = (OUT / "worker.lock").open("w")
    try:
        fcntl.flock(lock, fcntl.LOCK_EX | fcntl.LOCK_NB)
    except BlockingIOError:
        raise SystemExit("A Default catalogue worker is already running")
    state_path = OUT / "checkpoint.json"
    state = json.loads(state_path.read_text()) if state_path.exists() else {"requested_start": args.start}
    db = open_index()
    stop = OUT / "STOP"
    state.update(schema=SCHEMA, status="running", worker_pid=os.getpid(), started_unix=time.time(),
                 worker_argv=[sys.executable, str(Path(__file__).resolve()), *sys.argv[1:]],
                 worker_cwd=str(ROOT), lifetime_lock=str(OUT / "worker.lock"),
                 stop_by_unix=time.time() + args.hours * 3600, stop_file=str(stop), workers=args.workers,
                 qualified=False, source_grid_half_extent=HALF, sample_spacing=STEP,
                 minecraft_target="Java 1.21.11; local Cubiomes reimplementation, not generated chunks",
                 measurement_notes=["Native heights are approximate; analysis clamps them to sea level 63",
                                    "Raw grids retain unclamped heights and surface biome IDs",
                                    "Fields named dry/land mean non-ocean biome proxy; actual inland/river water is unknown",
                                    "Biomes are sampled by the native probe at quart scale and Y=319",
                                    "Biome-family masks overlap; neither preference count nor mirror ratio is a qualification gate"],
                 window_sizes_blocks=[n * STEP for n in SIZES],
                 retention="Every measured orientation, preference miss, regional vector and native grid retained",
                 script_sha256=hashlib.sha256(Path(__file__).read_bytes()).hexdigest(),
                 probe_sha256=hashlib.sha256(PROBE.read_bytes()).hexdigest())
    state.pop("stopped_unix", None)
    state.pop("failure", None)
    retained_bytes = sum(p.stat().st_size for folder in (OUT / "windows", OUT / "seeds", ART / "probes")
                         for p in folder.iterdir() if p.is_file())
    state["max_storage_bytes"] = int(args.max_storage_gib * 1024**3)
    def storage_estimate():
        index_bytes = sum(p.stat().st_size for p in OUT.glob("catalogue.sqlite*") if p.is_file())
        feed = OUT / "seed-index.jsonl"
        return retained_bytes + index_bytes + (feed.stat().st_size if feed.exists() else 0)
    state["retained_bytes_estimate"] = storage_estimate()
    checkpoint(db, state)
    halted = [False]
    def halt(signum, frame):
        halted[0] = True
    signal.signal(signal.SIGTERM, halt)
    signal.signal(signal.SIGINT, halt)
    processed = 0
    try:
        with concurrent.futures.ThreadPoolExecutor(max_workers=args.workers) as pool:
            while (processed < args.max_seeds and time.time() < state["stop_by_unix"]
                   and state["retained_bytes_estimate"] < state["max_storage_bytes"]
                   and not stop.exists() and not halted[0]):
                first = state["next_seed"]
                seeds = range(first, first + min(args.workers, args.max_seeds - processed))
                futures = {seed: pool.submit(screen, seed) for seed in seeds}
                for seed, future in futures.items():
                    try:
                        summary = future.result()
                        if ingest(db, summary):
                            retained_bytes += sum(Path(p).stat().st_size for p in
                                                  (summary["measurements_path"], summary["raw_native_grid"],
                                                   str(OUT / "seeds" / f"{seed}.json")))
                        processed += 1
                    except Exception as error:
                        with (OUT / "errors.jsonl").open("a") as stream:
                            stream.write(json.dumps({"seed": seed, "error": repr(error), "time": time.time()}) + "\n")
                        # Stop at a recoverable checkpoint; never skip an unmeasured seed.
                        raise
                state["retained_bytes_estimate"] = storage_estimate()
                checkpoint(db, state)
                print(json.dumps({k: state[k] for k in ("unique_seeds_measured", "distinct_regions_measured",
                                  "orientations_measured", "loose_end_land_orientations", "next_seed")}), flush=True)
    except Exception as error:
        state["failure"] = repr(error)
        raise
    finally:
        if "failure" in state:
            reason = "failure"
        elif stop.exists():
            reason = "STOP_file"
        elif halted[0]:
            reason = "signal"
        elif state["retained_bytes_estimate"] >= state["max_storage_bytes"]:
            reason = "storage_budget"
        elif processed >= args.max_seeds:
            reason = "seed_budget"
        else:
            reason = "time_budget"
        state.update(status="failed_at_checkpoint" if "failure" in state else "stopped_at_checkpoint",
                     stopped_unix=time.time(), stop_reason=reason)
        checkpoint(db, state)
        db.close()


if __name__ == "__main__":
    main()
