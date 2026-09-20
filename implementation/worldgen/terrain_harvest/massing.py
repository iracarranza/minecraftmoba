"""Greybox massing templates for authored opportunity.

Each function returns a dict of (dx, dy, dz) -> block state, relative to a site
centre at ground level, in the same form `build_structures` uses for the team
structures. Shapes are deliberately simple and readable from a distance.

These are consumed by `author_portfolio`, which places what the optimizer's
scenario frontier selected. An earlier count-based profile authoring path lived
here and has been removed: the optimizer chooses complete portfolios of specific
regions, with a declared crop or species per site, so there was nothing for a
second profile model to decide.

Scope: blocks only. Farmland, crops, water, fences and pen structure are terrain
and are authored here. Animals are entities, manifested at runtime by the
plugin's Renewables authoring.
"""
from __future__ import annotations

from serialization.world import block

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


TEMPLATES = {
    'crop_patch': crop_patch,
    'livestock_range': pen,
    'horse_range': lambda span=40: pen(span, 'spruce_fence'),
    'mining_worksite': worksite,
    'major_poi': poi,
    'minor_poi': lambda span=24: poi(span, 'deepslate_bricks'),
    'village': village,
}
