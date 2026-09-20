"""Instantiate a map profile block-exactly.

A profile is a variation in economic demand over the same map and the same
system rules: how many crop patches, how many livestock ranges, how many mining
worksites, and how far apart. Those are exactly the knobs `packing.CONSUMERS`
already exposes, so a profile is a declared override of that list plus an id --
not a new placement algorithm and not new geography.

This module runs the packing fit for a profile and then writes the resulting
placements into the world as greybox massing, reusing the same WorldEditor that
built the team structures, which refuses to invent terrain rather than
generating chunks that do not exist.

Scope: blocks only. Farmland, crops, water, fences and pen structure are
authored here because they are terrain. Animals are entities and are manifested
at runtime by the plugin's Renewables authoring, which already exists; writing
entity NBT offline would duplicate that with a second, unverified path.
"""
from __future__ import annotations

import argparse
import json
from pathlib import Path

from .build_structures import TEMPLATES as STRUCTURE_TEMPLATES
from serialization.world import block

from .build_structures import WorldEditor, clear_and_foundation
from .packing import CONSUMERS, classify, fit

SCHEMA = 'map_profile_authored/1'

# Crops that are legible from a distance and cheap to read in a screenshot.
CROPS = ('wheat', 'potatoes', 'carrots', 'beetroots')


def crop_patch(span=24, crop='wheat'):
    """A tilled field with a central water column, vanilla farm geometry."""
    r = span // 2
    out = {}
    for dx in range(-r, r + 1):
        for dz in range(-r, r + 1):
            if dx == 0 and dz == 0:
                out[(dx, -1, dz)] = block('water')
                continue
            # Vanilla hydration reaches 4 blocks; beyond that the field is dry
            # dirt, which is deliberate -- it shows the real irrigable area.
            if max(abs(dx), abs(dz)) <= 4:
                out[(dx, -1, dz)] = block('farmland', moisture='7')
                out[(dx, 0, dz)] = block(crop, age='7')
            else:
                out[(dx, -1, dz)] = block('dirt')
    return out


def pen(span=40, post='oak_fence'):
    """A fenced enclosure with a grass floor; animals are spawned at runtime."""
    r = span // 2
    out = {}
    for dx in range(-r, r + 1):
        for dz in range(-r, r + 1):
            out[(dx, -1, dz)] = block('grass_block')
            if max(abs(dx), abs(dz)) == r:
                out[(dx, 0, dz)] = block(post)
    # A gap per side, so the pen reads as enterable rather than sealed.
    for dx, dz in ((0, -r), (0, r), (-r, 0), (r, 0)):
        out.pop((dx, 0, dz), None)
    return out


def worksite(span=32):
    """A marked extraction pad: an open shaft head with a rim and a banner post."""
    r = span // 2
    out = {}
    for dx in range(-r, r + 1):
        for dz in range(-r, r + 1):
            d = max(abs(dx), abs(dz))
            if d <= 2:
                for dy in range(0, 4):
                    out[(dx, -dy, dz)] = block('air')       # the shaft head
            elif d == 3:
                out[(dx, 0, dz)] = block('polished_deepslate')
            elif d == r:
                out[(dx, 0, dz)] = block('cobblestone')
    for dy in range(1, 4):
        out[(0, dy, r)] = block('oak_fence')
    out[(0, 4, r)] = block('white_banner', rotation='8')
    return out


def poi(span=48, pillar='stone_bricks'):
    """A neutral landmark: a ring of pillars, readable from range and cheap."""
    r = span // 2
    out = {}
    for dx, dz in ((-r, -r), (-r, r), (r, -r), (r, r), (0, -r), (0, r), (-r, 0), (r, 0)):
        for dy in range(0, 6):
            out[(dx, dy, dz)] = block(pillar)
        out[(dx, 6, dz)] = block('sea_lantern')
    return out


def village(span=48):
    """Three greybox huts on a plaza; a stand-in for authored settlement."""
    out = {}
    r = span // 2
    for dx in range(-r, r + 1):
        for dz in range(-r, r + 1):
            if max(abs(dx), abs(dz)) <= r:
                out[(dx, -1, dz)] = block('dirt_path')
    for ox, oz in ((-8, -8), (8, -8), (0, 8)):
        for dx in range(-3, 4):
            for dz in range(-3, 4):
                edge = max(abs(dx), abs(dz)) == 3
                for dy in range(0, 4):
                    if edge and not (dx == 0 and dz == 3 and dy < 2):
                        out[(ox + dx, dy, oz + dz)] = block('oak_planks')
                out[(ox + dx, 4, oz + dz)] = block('oak_slab', type='bottom')
    return out


PROFILE_TEMPLATES = {
    'crop_patch': crop_patch,
    'livestock_range': pen,
    'horse_range': lambda span=40: pen(span, 'spruce_fence'),
    'mining_worksite': worksite,
    'major_poi': poi,
    'minor_poi': lambda span=24: poi(span, 'deepslate_bricks'),
    'village': village,
}


def consumers_for(profile: dict) -> list[dict]:
    """Apply a profile's overrides to the baseline consumer demand."""
    base = {c['id']: dict(c) for c in CONSUMERS}
    for override in profile.get('consumers', []):
        cid = override['id']
        if cid not in base:
            raise ValueError(f'profile {profile["id"]}: unknown consumer {cid}')
        base[cid].update(override)
    return list(base.values())


def surface_y(opportunity: dict, cell) -> float | None:
    for c in opportunity['cells']:
        if c['cell'] == list(cell):
            return c.get('mean_surface_y')
    return None


def author(profile: dict, opportunity_path: Path, world: Path | None,
           report: Path, dry_run: bool = True) -> dict:
    opportunity = json.loads(opportunity_path.read_text())
    consumers = consumers_for(profile)
    cell_size = opportunity['cell_size_blocks']
    classified = {}
    for c in opportunity['cells']:
        info = {**classify(c, cell_size), 'world_origin': c['world_origin']}
        info['remaining_area'] = int(cell_size * cell_size * max(info['farmable_fraction'], 0.05))
        classified[tuple(c['cell'])] = info
    placed, unplaced = fit(classified, cell_size, consumers, clearance=8)

    placements, skipped = [], []
    for p in placed:
        template = PROFILE_TEMPLATES.get(p['id'])
        if template is None:
            # Team structures are built by build_structures, which already sites
            # them symmetrically. Authoring them again here would fight that.
            skipped.append({'id': p['id'], 'reason': 'no profile template; '
                            'handled by build_structures or has no massing'})
            continue
        y = surface_y(opportunity, p['cell'])
        if y is None:
            skipped.append({'id': p['id'], 'reason': 'no measured surface height for cell'})
            continue
        placements.append({'id': p['id'], 'instance': p['instance'],
                           'cell': p['cell'], 'world_xyz': [p['world_origin'][0],
                                                            int(round(y)),
                                                            p['world_origin'][1]]})

    written = 0
    if world is not None:
        editor = WorldEditor(world)
        for p in placements:
            t = PROFILE_TEMPLATES[p['id']]()
            x, y, z = p['world_xyz']
            r = max(max(abs(dx), abs(dz)) for dx, _, dz in t) + 1
            h = max(dy for _, dy, _ in t) + 2
            clear_and_foundation(editor, x, y, z, r, h, block('dirt'))
            for (dx, dy, dz), state in t.items():
                editor.set(x + dx, y + dy, z + dz, state)
        if not dry_run:
            editor.flush()
        written = editor.written

    result = {
        'schema': SCHEMA,
        'evidence_state': 'RAW WORLD OBSERVATION' if (world and not dry_run)
                          else 'DERIVED MEASUREMENT',
        'profile': {k: v for k, v in profile.items() if k != 'consumers'},
        'profile_source': profile.get('source', 'UNDECLARED'),
        'consumer_demand': consumers,
        'fits': not unplaced,
        'placed': len(placements),
        'placed_by_packing': len(placed),
        'unplaced': unplaced,
        'skipped': skipped,
        'placements': placements,
        'dry_run': dry_run,
        'world': str(world) if world else None,
        'blocks_written': written,
        'nature': 'greybox massing in vanilla-native palettes, not final architecture',
        'not_covered': [
            'animals, which are entities manifested at runtime by the plugin, '
            'not written into region files here',
            'terrain shape within a cell; packing treats a cell as uniform, so a '
            'placement can land on a slope the foundation then flattens',
            'Routes between placements, which no profile yet declares',
            'whether a profile is balanced; this authors what the profile asks for',
        ],
    }
    report.parent.mkdir(parents=True, exist_ok=True)
    report.write_text(json.dumps(result, indent=1, sort_keys=True) + '\n')
    return result


def main(argv=None):
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('--profile', type=Path, required=True)
    p.add_argument('--opportunity', type=Path, required=True)
    p.add_argument('--world', type=Path, help='omit to plan without touching a world')
    p.add_argument('--report', type=Path, required=True)
    p.add_argument('--apply', action='store_true', help='actually write blocks')
    a = p.parse_args(argv)
    profile = json.loads(a.profile.read_text())
    r = author(profile, a.opportunity, a.world, a.report, dry_run=not a.apply)
    print(f"{profile['id']}: fits={r['fits']} placed={r['placed']} "
          f"unplaced={len(r['unplaced'])} blocks={r['blocks_written']} "
          f"[{r['profile_source']}]")
    for s in r['skipped']:
        print(f"  skipped {s['id']}: {s['reason']}")


if __name__ == '__main__':
    main()
