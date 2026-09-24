"""The compiler's front end: decide WHERE to generate, before generating.

This is the wiring that was missing. Every measurement built for scoops --
`scan`, `scoop`, `map_types`, `prominence` -- was reachable only by hand, and
the compiler still generated the fixed window at the origin and judged
whatever happened to be there. On the last real batch that produced 0 PLAYABLE
maps from 29 candidates, 23 of them rejected on `NO_DEFAULT_REGIONAL_SHAPE`.

WHAT CHANGES. A seed stops being the unit of work. Biome and approximate
height are pure functions of the seed, so the right window is SEARCHED for at
about 1s per 8192-block square, and only then is anything generated.
Generation was 91% of batch cost, so moving the decision in front of it is
where the economics change.

TUTORED, BECAUSE UNTUTORED SEARCH FINDS WATER. Ranked blind on symmetry the
best scoops are 97-99% open ocean -- featureless terrain mirrors itself
perfectly -- so budget is spent per Map Type. See `map_types`.

WHAT THIS DOES NOT DECIDE. Surface water, homeland developability, ore, caves,
buildability and every seam certification still need the window generated. A
prospect is a PLACE WORTH GENERATING, never a map.
"""
from __future__ import annotations

from . import map_types, prominence, scan as scan_mod, scoop, window_search

SEA_LEVEL = 63


def prospect(seed: int, *, half: int = 4096, step: int = 32,
             sizes=None, stride: int = 6, budget: int = 4, coarsen: int = 4,
             scanner: str | None = None, types=None) -> dict:
    """Window targets on one seed, ranked within Map Type.

    Returns chunk bounds ready for `harvest`/generation, plus the full
    measured vector for each, so a window that fits no Type is still described
    rather than discarded.
    """
    s = scan_mod.scan(seed, half=half, step=step, scanner=scanner)
    # Symmetry is read on the PLAYABLE surface: you walk on the sea, not the
    # seafloor. Water comes from biome, so clamping does not hide it.
    playable = [max(h, SEA_LEVEL) for h in s.height]
    found = scoop.search(playable, s.width, s.depth, spacing=s.step,
                         sizes=sizes, stride=stride, budget=budget,
                         coarsen=coarsen, probe_blocks=96,
                         biome=s.biome, sea_level=SEA_LEVEL,
                         types=types if types is not None else map_types.predicates())

    gate = map_types.symmetry_bound()
    predicates = types if types is not None else map_types.predicates()
    targets, water_only = [], 0
    for k in found['scoops']:
        # `separation_sign` is not produced by the search, so the two
        # `divided_by_*` predicates would read the default 0 and never match.
        # It is computed here for the handful of exact scoops -- `budget` of
        # them, not the 16,580 considered -- rather than left silently absent.
        i0, j0 = k['origin_sample']
        w0, d0 = [v // s.step for v in k['scoop_blocks']]
        sub = [playable[(j0 + jj) * s.n + i0 + ii]
               for jj in range(d0) for ii in range(w0)]
        # Merge SELECTIVELY. `relief_shape` also emits `relief_blocks`, as a
        # float, and blanket-updating clobbered `symmetry`'s per-half dict of
        # the same name -- two modules with one key meaning two things.
        shape = prominence.relief_shape(sub, w0, d0, spacing=s.step)
        for key in ('separation_sign', 'peak_mass', 'pit_mass', 'second_peak'):
            k[key] = shape[key]
        # `land_bodies` and `coastline_per_1k_blocks2` have the same problem:
        # `scoop.search` computes neither, so `archipelago` read land_bodies=1
        # by default and could never match, and `shattered_coast` read
        # coastline 0. Across 20 seeds those two Types plus both `divided_by_*`
        # matched NOTHING, which looked like a property of the seeds and was a
        # missing input. Computed per exact scoop, not per candidate.
        sb = [s.biome[(j0 + jj) * s.n + i0 + ii]
              for jj in range(d0) for ii in range(w0)]
        land = [b not in scan_mod.OCEAN_IDS for b in sb]
        water = scoop.water_structure(sub, w0, d0, SEA_LEVEL,
                                      spacing=s.step, land_mask=land)
        for key in ('land_bodies', 'water_bodies', 'largest_land_share',
                    'coastline_per_1k_blocks2'):
            k[key] = water[key]
        # `open_water` exists to NAME the sink a symmetry ranking falls into,
        # not to be generated. A scoop labelled only open_water is dropped
        # here: on seed 31337 two such scoops ranked ABOVE the one genuinely
        # dry target, so naming the sink and then emitting it anyway would
        # have spent generation on open sea.
        labels = [n for n, ok in predicates.items() if ok(k)]
        # open_water VETOES, it does not merely fail to qualify.
        #
        # The first version dropped a scoop only when open_water was its sole
        # label, which let ['archipelago', 'open_water'] through -- the
        # archipelago cut is water above p75 and open_water is above p90, so
        # they overlap heavily. The first end-to-end run generated 95%-water
        # ground for 95 seconds and rejected it at `homebase` with
        # SOCKET_NOT_INDEPENDENTLY_ACCEPTABLE, which is the right answer
        # arrived at far too late. Too much water to be a map is a veto
        # whatever else the scoop also qualifies as.
        if 'open_water' in labels:
            water_only += 1
            continue
        i, j = i0, j0
        cw, cd = w0, d0
        x0, z0 = s.block_at(i, j)
        x1, z1 = s.block_at(i + cw - 1, j + cd - 1)
        centre = ((x0 + x1) // 2, (z0 + z1) // 2)
        relief = max(k['relief_blocks'].values()) or 1.0
        ratio = round(k['mirror_deviation_blocks'] / relief, 4)
        targets.append({
            'seed': seed,
            'block_bounds': [x0, x1, z0, z1],
            'centre': list(centre),
            # `chunk_bounds_for` lives in window_search, not scoop. Guarding
            # with hasattr on the wrong module silently emitted None for every
            # target, i.e. a prospect with nothing to generate.
            'chunk_bounds': list(window_search.chunk_bounds_for(*centre)),
            'team_axis': k['team_axis'],
            'deviation_over_relief': ratio,
            'within_symmetry_gate': ratio <= gate['value'],
            'relief_blocks': relief,
            'mirror_tilt_blocks': k['mirror_tilt_blocks'],
            'water_fraction': k.get('water_fraction'),
            'types': labels,
            'homebase_max_separation_blocks': k.get('homebase_max_separation_blocks'),
            'measured': k,
        })
    # Rank on the ratio, which is what makes a 10-block and a 100-block feature
    # comparable. Relief is carried and NOT ranked on.
    targets.sort(key=lambda t: t['deviation_over_relief'])
    return {
        'seed': seed,
        'scanned_half': half,
        'step': step,
        'considered': found['considered'],
        'measured_exactly': found['measured_exactly'],
        'partitions': found.get('partitions') or {},
        'types_matching_nothing': found.get('types_matching_nothing') or [],
        'symmetry_gate': gate,
        'targets': targets,
        'within_gate': sum(1 for t in targets if t['within_symmetry_gate']),
        'dropped_open_water': water_only,
        'proves': 'biome and approximate height only. Surface water, '
                  'developability, ore, caves, buildability and every seam '
                  'certification need the window generated.',
    }
