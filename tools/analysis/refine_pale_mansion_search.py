"""Refine broad pale/mansion search without gameplay filters; preserve raw grids."""
import argparse
import json
import math
import subprocess
from pathlib import Path

import numpy as np

ROOT = Path(__file__).resolve().parents[2]
REPORT = ROOT / 'implementation/worldgen/reports/pale_mansion_search_2026-10-12'
BIN = ROOT / 'artifacts/worldgen/pale_mansion_search_2026-10-12/build/pale_mansion_refine'


def label(mask):
    result = np.zeros(mask.shape, dtype=np.int32)
    count = 0
    height, width = mask.shape
    for y, x in zip(*np.where(mask)):
        if result[y, x]:
            continue
        count += 1
        stack = [(y, x)]
        result[y, x] = count
        while stack:
            j, i = stack.pop()
            for b, a in ((j-1, i), (j+1, i), (j, i-1), (j, i+1)):
                if 0 <= b < height and 0 <= a < width and mask[b, a] and not result[b, a]:
                    result[b, a] = count
                    stack.append((b, a))
    return result, count


def load(path):
    rows = []
    for line in path.read_text().splitlines():
        try:
            rows.append(json.loads(line))
        except json.JSONDecodeError:
            pass  # Running scanner may have an incomplete final buffered line.
    return rows


def detailed(row, step=32, radius=1536):
    output = subprocess.check_output([str(BIN), 'grid', str(row['seed']),
                                     str(row['x']), str(row['z']),
                                     str(step), str(radius)], text=True)
    metric, grid = [json.loads(l) for l in output.splitlines()]
    n = grid['n']
    b = np.array(grid['biomes']).reshape(n, n)
    mask = b == 186
    zz, xx = np.mgrid[:n, :n]
    dx, dz = (xx - n // 2) * step, (zz - n // 2) * step
    distances = np.hypot(dx, dz)
    metric['pale_share_by_square_width'] = {}
    metric['quadrant_shares_by_square_width'] = {}
    for half in (128, 256, 512, 1024):
        square = (np.abs(dx) <= half) & (np.abs(dz) <= half)
        metric['pale_share_by_square_width'][str(2 * half)] = float(mask[square].mean())
        shares = []
        for sx, sz in ((False, False), (True, False), (False, True), (True, True)):
            q = square & ((dx >= 0) == sx) & ((dz >= 0) == sz)
            shares.append(float(mask[q].mean()))
        metric['quadrant_shares_by_square_width'][str(2 * half)] = shares
    components, count = label(mask)
    forests = []
    for k in range(1, count + 1):
        sel = components == k
        ys, xs = np.where(sel)
        size = len(xs)
        area = size * step * step
        cx, cz = float(dx[sel].mean()), float(dz[sel].mean())
        forests.append({'samples': size, 'area_proxy_blocks2': area,
                        'equivalent_diameter_blocks': 2 * math.sqrt(area / math.pi),
                        'bbox': [int(row['x'] + dx[sel].min()), int(row['x'] + dx[sel].max()),
                                 int(row['z'] + dz[sel].min()), int(row['z'] + dz[sel].max())],
                        'centroid': [row['x'] + cx, row['z'] + cz],
                        'centroid_distance_blocks': math.hypot(cx, cz),
                        'centroid_offset_in_equivalent_radii': math.hypot(cx, cz) / math.sqrt(area / math.pi),
                        'nearest_distance_blocks': float(distances[sel].min()),
                        'touches_boundary': bool((xs == 0).any() or (ys == 0).any() or
                                                 (xs == n - 1).any() or (ys == n - 1).any())})
    forests.sort(key=lambda f: f['area_proxy_blocks2'], reverse=True)
    metric['components'] = forests[:10]
    metric['coarse'] = row
    return metric, grid


def main():
    p = argparse.ArgumentParser()
    p.add_argument('--coarse', type=Path, default=REPORT / 'coarse.jsonl')
    p.add_argument('--top', type=int, default=250)
    args = p.parse_args()
    rows = load(args.coarse)
    chosen = {}
    criteria = {
        'largest_connected_forest': lambda r: r['component_area_proxy'],
        'highest_local_coverage': lambda r: r['inner_pale_share'],
        'most_surrounding_coverage': lambda r: min(r['quadrant_shares']),
        'large_central_forest': lambda r: r['component_area_proxy'] if
        r['component_centroid_distance'] <= 256 and r['component_nearest_distance'] <= 256 else 0,
    }
    for key in criteria.values():
        for row in sorted(rows, key=key, reverse=True)[:args.top]:
            if row['pale_samples']:
                chosen[(row['seed'], row['x'], row['z'])] = row
    grids = REPORT / 'grids'
    grids.mkdir(exist_ok=True)
    out = REPORT / 'refined.jsonl'
    already = {(r['seed'], r['x'], r['z']) for r in load(out)} if out.exists() else set()
    with out.open('a') as fh:
        for key, row in chosen.items():
            if key in already:
                continue
            metric, grid = detailed(row)
            (grids / f"{row['seed']}_{row['x']}_{row['z']}.json").write_text(json.dumps(grid))
            fh.write(json.dumps(metric) + '\n')
            fh.flush()
            already.add(key)
            if len(already) % 25 == 0:
                print(f'Refined {len(already)}; current selection {len(chosen)}', flush=True)
    (REPORT / 'refinement-selection.json').write_text(json.dumps({
        'coarse_records': len(rows), 'criteria': list(criteria), 'top_per_criterion': args.top,
        'selected_unique': len(chosen), 'refined_cumulative': len(already),
        'step': 32, 'radius': 1536, 'connectivity': 'four-neighbour',
        'selection_note': 'Diverse refinement budgets, not gameplay eligibility filters. All coarse records retained.'}, indent=2) + '\n')


if __name__ == '__main__':
    main()
