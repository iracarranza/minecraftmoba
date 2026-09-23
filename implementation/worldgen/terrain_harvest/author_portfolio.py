"""Instantiate an optimizer finalist portfolio block-exactly.

`tools/analysis/map_authoring_optimizer.py` searches complete portfolios --
founder crop patches, renewable manifestations, Worksites, POIs and Route
targets -- and writes a scenario frontier. This module takes one finalist from
that frontier and authors it into a world.

The difference from a count-based profile matters: the optimizer has already
chosen *which* 128x128 regions, with a declared crop or species per site, after
scoring them as a portfolio. So there is nothing to re-decide here. This module
places what the finalist says, at the centre the finalist gives, and reports
anything it could not place rather than substituting a nearby cell.

The optimizer explicitly defers exact siting to this step and requires a rescan
afterwards, because a 128x128 region qualifying on average does not mean its
centre block is buildable.
"""
from __future__ import annotations

import argparse
import hashlib
import json
import math
from pathlib import Path

from serialization.world import block

from serialization.nbt import plain
from serialization.region import read_region

from .build_structures import WorldEditor, clear_and_foundation
from .massing import crop_patch, pen, poi, worksite

SCHEMA = 'map_portfolio_authored/1'

# The optimizer names crops in the singular; vanilla blocks are plural.

# Renewable species the optimizer can select, mapped to the pen fence that
# reads as that kind of range. Animals themselves are spawned at runtime.
# Opportunities the map records but does not BUILD. See template_for.
# Renewable ranges and founder crops are NOT built as blocks. There used to be
# a CROP_BLOCK table and a SPECIES_FENCE table here, mapping each species to a
# fence type -- and the regenerative manifestation doctrine rejects fences,
# rectangular layouts and prepared farmland outright, because infrastructure
# implies the site has already been Developed. Once nothing is authored, the
# tables had no caller left; they are removed rather than kept as a trap.
UNAUTHORED = {'renewable_range', 'founder_crop'}


def cell_index(opportunity: dict) -> dict:
    """Per-cell surface height, coverage, and a centre known to be in-volume.

    The optimizer works in 128x128 cells and names each site by its cell's
    geometric centre. On a boundary cell that centre can lie outside the
    harvested volume, so prefer the centroid of the columns actually sampled.
    """
    return {tuple(c['cell']): {
                'surface_y': c.get('mean_surface_y'),
                'coverage': c.get('coverage'),
                'centroid': c.get('sampled_centroid'),
            } for c in opportunity['cells']}


def sites_of(configuration: dict):
    """Flatten a finalist configuration into authorable sites."""
    for f in configuration.get('founders', []):
        yield {'kind': 'founder_crop', 'detail': f.get('crop'), **f}
    for r in configuration.get('renewables', []):
        yield {'kind': 'renewable_range', 'detail': r.get('species'), **r}
    for w in configuration.get('worksites', []):
        yield {'kind': 'mining_worksite', 'detail': None, **w}
    for p in configuration.get('pois', []):
        yield {'kind': 'poi', 'detail': None, **p}
    for team, targets in (configuration.get('route_targets') or {}).items():
        for t in targets:
            yield {'kind': 'route_target', 'detail': team, **t}


def template_for(site: dict):
    """Blocks for a site, or None with a reason if it cannot be authored."""
    kind, detail = site['kind'], site['detail']
    if kind in UNAUTHORED:
        # Regenerative opportunities are NOT built. A pen is prebuilt
        # containment and an irrigated field is player production geography, and
        # both make the world's own manifestation indistinguishable from the
        # thing players are supposed to create. The opportunity survives as
        # authored geography -- the cell the optimizer chose, which the runtime
        # reads as an Opportunity Region -- and its current manifestation is
        # placed at match time on terrain that is eligible then.
        #
        # An empty template is deliberate rather than a failure: the placement
        # is still recorded, so the opportunity is still part of the map.
        return {}, None
    if kind == 'mining_worksite':
        return worksite(span=32), None
    if kind == 'poi':
        return poi(span=48), None
    if kind == 'route_target':
        # A Route target is a waypoint, not a building. A low cairn marks it
        # without implying authored architecture the optimizer never chose.
        marker = {}
        for dy in range(0, 3):
            marker[(0, dy, 0)] = block('chiseled_stone_bricks')
        marker[(0, 3, 0)] = block('lantern')
        return marker, None
    return None, f'unknown site kind {kind!r}'


def present_chunks(world: Path) -> set[tuple[int, int]]:
    """Chunks that actually exist in the world's region files.

    The harvested volume is a shape, not a rectangle, so a region file can exist
    while chunks inside it were never generated. This is the only authority on
    whether a block can be written; a bounds check would pass sites that the
    editor then refuses.
    """
    out = set()
    for f in sorted((world / 'region').glob('*.mca')):
        for cx, cz, _, _ in read_region(f):
            out.add((cx, cz))
    return out


def radius_of(template) -> int:
    """How far a placement reaches. An UNAUTHORED opportunity reaches nowhere.

    Zero is the honest answer for something that builds nothing: it needs no
    chunk to be writable, claims no slot against its neighbours, and levels no
    pad. Treating it as a structure with an invisible footprint would keep
    reserving ground for a pen that is never coming.
    """
    if not template:
        return 0
    return max(max(abs(dx), abs(dz)) for dx, _, dz in template) + 1

def footprint_chunks(x: int, z: int, template) -> set[tuple[int, int]]:
    """Every chunk a placement's blocks and its foundation pad would touch."""
    r = radius_of(template)
    return {(cx, cz)
            for cx in range((x - r) >> 4, ((x + r) >> 4) + 1)
            for cz in range((z - r) >> 4, ((z + r) >> 4) + 1)}


def unwritable(x: int, z: int, template, chunks) -> list:
    """Chunks a placement needs that the world does not have."""
    return sorted(footprint_chunks(x, z, template) - chunks)



def allocate(sites, cell_size: int, chunks, margin: int = 4):
    """Give each site in a cell its own non-overlapping position.

    A portfolio may put several sites in one 128x128 cell, and the optimizer
    says nothing about where inside it they go -- it defers exact siting here.
    Placing them all at the cell centre silently overwrites all but the last,
    which a readback caught.

    So divide the cell into an n x n grid of slots and give each site one.
    Sizing the grid to the number of sites, rather than greedily packing from
    the centre outwards, is what makes three large sites fit: a greedy pass
    puts the first one in the middle and then has nowhere to put the others.
    """
    n = len(sites)
    if n == 0:
        return [], []
    k = math.ceil(math.sqrt(n))
    slot = cell_size // k
    half = slot // 2
    # Slot centres, ordered from the middle of the cell outwards, so a cell with
    # one site still places it centrally.
    origin = sites[0]['origin']
    centres = sorted(
        ((origin[0] + (a * slot) + half - cell_size // 2,
          origin[1] + (b * slot) + half - cell_size // 2)
         for a in range(k) for b in range(k)),
        key=lambda c: (abs(c[0] - origin[0]) + abs(c[1] - origin[1]), c))

    placed, rejected = [], []
    # Largest first, so the tightest fit gets the first pick of slots.
    for site in sorted(sites, key=lambda s: -radius_of(s['template'])):
        r = radius_of(site['template'])
        spot = None
        if r <= half:
            for x, z in centres:
                if any(max(abs(x - q['world_xz'][0]), abs(z - q['world_xz'][1]))
                       < r + q['radius'] + margin for q in placed):
                    continue
                if chunks is not None and unwritable(x, z, site['template'], chunks):
                    continue
                spot = (x, z)
                break
        if spot is None:
            rejected.append({**site, 'radius': r})
            continue
        placed.append({**site, 'world_xz': list(spot), 'radius': r,
                       'slot_half': half})
    return placed, rejected


def profiles_of(frontier: dict) -> dict:
    """Profiles from a frontier document, v1 flat or v2 wrapped.

    The v2 optimizer wraps its profiles under a "profiles" key alongside
    provenance. Accepting both keeps a historical frontier readable without a
    second code path.
    """
    return frontier.get('profiles', frontier)


def provenance(obj) -> str:
    """Stable digest of the frontier a world was authored from.

    Three different frontiers for this map now exist -- the handoff's reference
    run, a committed in-repository search, and the committed optimizer's own
    output -- and they disagree. Recording which one produced a world is the
    difference between a reproducible artefact and a plausible one.
    """
    return hashlib.sha256(
        json.dumps(obj, sort_keys=True, separators=(',', ':')).encode()).hexdigest()


def author(frontier: dict, profile: str, rank: int, opportunity: dict,
           world: Path | None, report: Path, dry_run: bool = True,
           chunks: set | None = None) -> dict:
    profiles = profiles_of(frontier)
    if profile not in profiles:
        raise KeyError(f'no profile {profile!r} in frontier '
                       f'(have: {sorted(profiles)})')
    finalists = profiles[profile]['finalists']
    if rank >= len(finalists):
        raise IndexError(f'{profile} has {len(finalists)} finalists, '
                         f'rank {rank} requested')
    finalist = finalists[rank]
    cells = cell_index(opportunity)

    cell_size = opportunity['cell_size_blocks']
    by_cell, skipped = {}, []
    for site in sites_of(finalist['configuration']):
        template, reason = template_for(site)
        if template is None:
            skipped.append({'kind': site['kind'], 'detail': site['detail'],
                            'cell': site['cell'], 'reason': reason})
            continue
        key = tuple(site['cell'])
        info = cells.get(key, {})
        y = info.get('surface_y')
        if y is None:
            skipped.append({'kind': site['kind'], 'detail': site['detail'],
                            'cell': site['cell'],
                            'reason': 'no measured surface height for cell'})
            continue
        # The optimizer's centre, unless the cell is partial and a measured
        # in-volume centroid is available.
        origin = list(site['center'])
        centroid, coverage = info.get('centroid'), info.get('coverage')
        if centroid and coverage is not None and coverage < 1.0:
            origin = list(centroid)
        by_cell.setdefault(key, []).append({
            'kind': site['kind'], 'detail': site['detail'],
            'cell': list(key), 'bias': site.get('bias'),
            'coverage': coverage, 'surface_y': y,
            'origin': origin, 'centre': list(site['center']),
            'template': template})

    placements = []
    for key, sites in sorted(by_cell.items()):
        ok, rejected = allocate(sites, cell_size, chunks)
        for site in ok:
            x, z = site['world_xz']
            placements.append({
                'kind': site['kind'], 'detail': site['detail'],
                'cell': site['cell'], 'bias': site['bias'],
                'coverage': site['coverage'],
                'recentred': [x, z] != site['centre'],
                'cell_neighbours': len(sites) - 1,
                # Carried so Route authoring can report corridor crossings.
                'radius': site['radius'],
                'world_xyz': [x, int(round(site['surface_y'])), z],
                'template': site['template']})
        for site in rejected:
            skipped.append({
                'kind': site['kind'], 'detail': site['detail'],
                'cell': site['cell'],
                'reason': 'no free, writable slot in its cell; '
                          f'{len(sites)} site(s) share it'})

    written = 0
    if world is not None:
        editor = WorldEditor(world)
        for p in placements:
            t = p['template']
            x, y, z = p['world_xyz']
            if not t:
                # Nothing to build, and nothing to level either: clearing a pad
                # for a structure that is not coming is exactly the decorative
                # spawn pad the regenerative model rejects.
                continue
            r = max(max(abs(dx), abs(dz)) for dx, _, dz in t) + 1
            h = max(dy for _, dy, _ in t) + 2
            clear_and_foundation(editor, x, y, z, r, h, block('dirt'))
            for (dx, dy, dz), state in t.items():
                editor.set(x + dx, y + dy, z + dz, state)
        if not dry_run:
            editor.flush()
        written = editor.written

    for p in placements:
        p['blocks'] = len(p.pop('template'))

    result = {
        'schema': SCHEMA,
        'evidence_state': 'RAW WORLD OBSERVATION' if (world and not dry_run)
                          else 'DERIVED MEASUREMENT',
        'profile': profile,
        'finalist_rank': rank,
        'frontier_sha256': provenance(frontier),
        'finalist_sha256': provenance(finalist),
        'objective': finalist['objective'],
        'metrics': finalist['metrics'],
        'placed': len(placements),
        'complete': not skipped,
        'skipped': skipped,
        'preflighted': chunks is not None,
        'placements': placements,
        'dry_run': dry_run,
        'world': str(world) if world else None,
        'blocks_written': written,
        'nature': 'greybox massing in vanilla-native palettes, not final architecture',
        'not_covered': [
            'animals, which are entities manifested at runtime by the plugin',
            'Route paths between targets; only the targets themselves are marked',
            'whether the exact centre block is buildable -- the optimizer scored '
            'a 128x128 region on average and defers exact siting to a rescan',
            'underground hostile exposure, which the optimizer records as '
            'UNRESOLVED and which zero-hostile snapshot counts cannot establish',
        ],
    }
    report.parent.mkdir(parents=True, exist_ok=True)
    report.write_text(json.dumps(result, indent=1, sort_keys=True) + '\n')
    return result


def author_best(frontier: dict, profile: str, opportunity: dict, world: Path,
                report: Path, dry_run: bool = True, max_rank: int | None = None):
    """Author the highest-ranked finalist that places completely.

    The optimizer's run doc asks for a fallback when exact terrain invalidates a
    preferred regional candidate. Ranks are already ordered by objective, so the
    fallback is simply the next one down -- and the rejected ranks are reported,
    because a profile whose best several finalists all fail is telling you
    something about the profile, not about the tooling.
    """
    chunks = present_chunks(world)
    n = len(profiles_of(frontier)[profile]['finalists'])
    limit = n if max_rank is None else min(n, max_rank + 1)
    rejected = []
    for rank in range(limit):
        result = author(frontier, profile, rank, opportunity, None, report,
                        dry_run=True, chunks=chunks)
        if result['complete']:
            final = author(frontier, profile, rank, opportunity, world, report,
                           dry_run=dry_run, chunks=chunks)
            final['rejected_ranks'] = rejected
            report.write_text(json.dumps(final, indent=1, sort_keys=True) + '\n')
            return final
        rejected.append({'rank': rank, 'skipped': result['skipped']})
    raise RuntimeError(
        f'{profile}: no finalist in ranks 0-{limit - 1} places completely; '
        f'{len(rejected)} rejected. This is a siting result, not a tooling '
        f'failure -- see the report for which sites fall outside the volume.')


def main(argv=None):
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('--frontier', type=Path, required=True)
    p.add_argument('--opportunity', type=Path, required=True)
    p.add_argument('--profile', required=True)
    p.add_argument('--rank', type=int, default=0)
    p.add_argument('--best', action='store_true',
                   help='use the highest-ranked finalist that places completely')
    p.add_argument('--world', type=Path)
    p.add_argument('--report', type=Path, required=True)
    p.add_argument('--apply', action='store_true')
    a = p.parse_args(argv)
    frontier = json.loads(a.frontier.read_text())
    opportunity = json.loads(a.opportunity.read_text())
    if a.best:
        if a.world is None:
            p.error('--best needs --world, because the preflight reads the '
                    'world\'s actual chunks')
        r = author_best(frontier, a.profile, opportunity, a.world, a.report,
                        dry_run=not a.apply)
        for bad in r.get('rejected_ranks', []):
            print(f"  rank {bad['rank']} rejected: "
                  f"{len(bad['skipped'])} site(s) outside the volume")
    else:
        chunks = present_chunks(a.world) if a.world else None
        r = author(frontier, a.profile, a.rank, opportunity, a.world, a.report,
                   dry_run=not a.apply, chunks=chunks)
    print(f"{a.profile} #{r['finalist_rank']}: placed={r['placed']} "
          f"skipped={len(r['skipped'])} "
          f"blocks={r['blocks_written']} "
          f"balance={r['metrics']['balance_asymmetry']:.4f}")
    for s in r['skipped']:
        print(f"  skipped {s['kind']} {s['detail']}: {s['reason']}")


if __name__ == '__main__':
    main()
