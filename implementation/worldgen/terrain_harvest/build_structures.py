"""Build the four team structures into a materialized world, as greybox massing.

These are **greybox**, not final architecture: legible shapes in vanilla-native
palettes so a site can be walked and judged. objectives.md leaves exposure,
disable conditions, obstruction behaviour, validation, reactivation and repair
unresolved, so nothing here encodes any of that — no redstone, no spawners, no
functional machinery. A structure here is a shape on the ground.

The Aether Fountain is built as a literal water fountain lined with glowstone,
which is the described intent rather than an invented one.

Writes blocks directly into an existing world's region files, in the same way
the harvest exporter does, and refuses to touch a world it was not pointed at.
"""
from __future__ import annotations
import argparse
import json
import math
from collections import defaultdict
from pathlib import Path

from serialization.nbt import COMPOUND, byte, compound, list_tag, plain, string
from serialization.region import read_region, write_region
from serialization.world import AIR, _block_states_tag, block
from vanilla_search.extract import _palette_value
from .materialize import state_tuple


class WorldEditor:
    """Buffers block writes and rewrites only the sections that changed."""

    def __init__(self, world: Path):
        self.world = world
        self.pending: dict[tuple[int, int], dict[tuple[int, int, int], tuple]] = defaultdict(dict)
        self.written = 0

    def set(self, x: int, y: int, z: int, state):
        self.pending[(x >> 4, z >> 4)][(x, y, z)] = state

    def flush(self):
        by_region = defaultdict(dict)
        for (cx, cz), cells in self.pending.items():
            by_region[(cx >> 5, cz >> 5)][(cx, cz)] = cells
        for (rx, rz), chunks in by_region.items():
            path = self.world / 'region' / f'r.{rx}.{rz}.mca'
            if not path.exists():
                raise FileNotFoundError(f'no region file for chunk area {rx},{rz}: {path}')
            roots = {(cx, cz): (name, root) for cx, cz, name, root in read_region(path)}
            for (cx, cz), cells in chunks.items():
                if (cx, cz) not in roots:
                    raise KeyError(f'chunk {cx},{cz} absent; refusing to invent terrain')
                self._apply(roots[(cx, cz)][1], cx, cz, cells)
            write_region(path, roots)
        self.pending.clear()

    def _apply(self, root, cx, cz, cells):
        sections = {s.value['Y'].value: s for s in root.value['sections'].value}
        by_section = defaultdict(dict)
        for (x, y, z), state in cells.items():
            by_section[y >> 4][(x, y, z)] = state
        for sy, group in by_section.items():
            section = sections.get(sy)
            if section is None:
                raise KeyError(f'section {sy} absent in chunk {cx},{cz}; refusing to invent terrain')
            container = plain(section).get('block_states')
            states = []
            for y in range(sy * 16, sy * 16 + 16):
                for zz in range(cz * 16, cz * 16 + 16):
                    for xx in range(cx * 16, cx * 16 + 16):
                        key = (xx, y, zz)
                        if key in group:
                            states.append(group[key]); self.written += 1
                        elif container:
                            states.append(state_tuple(_palette_value(
                                container, (y & 15) * 256 + (zz & 15) * 16 + (xx & 15), 4)))
                        else:
                            states.append(AIR)
            section.value['block_states'] = _block_states_tag(states)


# ---- greybox templates -------------------------------------------------------
# Each returns a dict of (dx, dy, dz) -> state, relative to the site centre at
# ground level. Shapes are deliberately simple and readable from a distance.

def _disc(radius):
    return [(dx, dz) for dx in range(-radius, radius + 1) for dz in range(-radius, radius + 1)
            if dx * dx + dz * dz <= radius * radius]


def aether_fountain(radius=6):
    """A literal water fountain lined with glowstone, tiered to a source above."""
    out = {}
    for dx, dz in _disc(radius):
        out[(dx, -1, dz)] = block('polished_diorite')           # basin floor
    for dx, dz in _disc(radius):
        edge = dx * dx + dz * dz > (radius - 1) ** 2
        if edge:
            out[(dx, 0, dz)] = block('glowstone')               # lined rim
            out[(dx, 1, dz)] = block('polished_diorite_slab', type='bottom')
        else:
            out[(dx, 0, dz)] = block('water', level='0')        # the pool
    for dx, dz in _disc(2):                                      # central plinth
        for dy in range(0, 4):
            out[(dx, dy, dz)] = block('chiseled_quartz_block')
    out[(0, 4, 0)] = block('water', level='0')                   # the source
    for dx, dz in [(-1, 0), (1, 0), (0, -1), (0, 1)]:
        out[(dx, 4, dz)] = block('glowstone')
    return out


def pillager_outpost(height=12):
    """Dark oak tower massing, matching the vanilla outpost silhouette."""
    out = {}
    for dx in range(-3, 4):
        for dz in range(-3, 4):
            out[(dx, -1, dz)] = block('cobblestone')
    for dy in range(0, height):
        for dx in range(-2, 3):
            for dz in range(-2, 3):
                edge = abs(dx) == 2 or abs(dz) == 2
                if edge:
                    out[(dx, dy, dz)] = block('dark_oak_log', axis='y') if abs(dx) == 2 and abs(dz) == 2 \
                        else block('dark_oak_planks')
    for dx in range(-3, 4):                                      # overhanging platform
        for dz in range(-3, 4):
            out[(dx, height, dz)] = block('dark_oak_planks')
    for dx in range(-3, 4):
        for dz in range(-3, 4):
            if abs(dx) == 3 or abs(dz) == 3:
                out[(dx, height + 1, dz)] = block('dark_oak_fence')
    return out


def nether_bastion(height=9):
    """Blackstone massing with a flat fighting deck."""
    out = {}
    for dx in range(-6, 7):
        for dz in range(-6, 7):
            out[(dx, -1, dz)] = block('polished_blackstone_bricks')
    for dy in range(0, height):
        for dx in range(-5, 6):
            for dz in range(-5, 6):
                if abs(dx) == 5 or abs(dz) == 5:
                    out[(dx, dy, dz)] = block('blackstone') if (dx + dz + dy) % 7 else \
                        block('gilded_blackstone')
    for dx in range(-5, 6):
        for dz in range(-5, 6):
            out[(dx, height, dz)] = block('polished_blackstone')
    for dx in range(-5, 6):
        for dz in range(-5, 6):
            if abs(dx) == 5 or abs(dz) == 5:
                out[(dx, height + 1, dz)] = block('polished_blackstone_wall')
    return out


def end_tower(height=18):
    """End stone shaft with an obsidian core and a purpur crown.

    HISTORICAL. The current form is the End Spike from the Ender Dragon arena:
    an obsidian pillar with an End Crystal, and a cage where applicable. This is
    the generic End Tower geometry the spec names and rejects. It still builds,
    under its own name, so existing maps and their artifacts stay readable --
    but `terrain_harvest.objective_forms.certify` will not pass a placement that
    uses it, and no End Spike has been measured to replace it with.
    """
    out = {}
    for dx, dz in _disc(5):
        out[(dx, -1, dz)] = block('end_stone_bricks')
    for dy in range(0, height):
        for dx, dz in _disc(4):
            if dx * dx + dz * dz > 9:
                out[(dx, dy, dz)] = block('end_stone_bricks')
        out[(0, dy, 0)] = block('obsidian')
    for dx, dz in _disc(5):
        out[(dx, height, dz)] = block('purpur_block')
    for dx, dz in _disc(5):
        if dx * dx + dz * dz > 16:
            out[(dx, height + 1, dz)] = block('purpur_pillar', axis='y')
    return out


TEMPLATES = {
    'aether_fountain': aether_fountain,
    'pillager_outpost': pillager_outpost,
    'nether_bastion': nether_bastion,
    # Historical; see objective_forms.HISTORICAL. Not renamed to 'end_spike',
    # and deliberately NOT registered under that key either: siting now uses the
    # `end_spike` layer, and if this table answered to that name the compiler
    # would site an End Spike and silently build an End Tower. There is no End
    # Spike mesh yet, so authoring fails closed instead.
    'end_tower': end_tower,
}


def _column_reader(world: Path):
    """Read blocks back out of the world being written, for obstacle checks."""
    from serialization.nbt import plain
    from serialization.region import read_region
    from vanilla_search.extract import VanillaChunk
    chunks = {}
    for f in sorted((world / 'region').glob('*.mca')):
        for cx, cz, _, root in read_region(f):
            chunks[(cx, cz)] = VanillaChunk(plain(root))

    def at(x, y, z):
        c = chunks.get((x >> 4, z >> 4))
        if c is None:
            return None
        try:
            return c.block(x, y, z)
        except Exception:
            return None
    return at


def clear_and_foundation(editor, cx, cy, cz, radius, headroom, foundation, column=None):
    """Level a pad so the massing reads, without sculpting surrounding terrain.

    Clearing a fixed height used to cut every tree inside the pad off at that
    height and leave its canopy hanging in the air -- the trunk was in the
    cleared volume and the leaves were above it. A tree in the way is felled
    whole instead, so the site reads as cleared rather than as damaged.

    `column` is optional because the caller may not have a world reader; without
    it the old behaviour stands, and the floating leaves with it.
    """
    from .respect import fell, is_leaf, is_log, is_structure
    for dx, dz in _disc(radius):
        x, z = cx + dx, cz + dz
        for dy in range(0, headroom):
            y = cy + dy
            here = column(x, y, z) if column else None
            if here is not None and is_structure(here):
                continue          # a building is not terrain
            if here is not None and is_leaf(here):
                editor.set(x, y, z, AIR)     # brush the branch aside
                continue
            if here is not None and is_log(here):
                fell(editor, column, x, y, z, AIR)   # a trunk comes down whole
                continue
            editor.set(x, y, z, AIR)
        editor.set(x, cy - 1, z, foundation)


def build(world: Path, placements, report: Path, dry_run=False):
    editor = WorldEditor(world)
    reader = _column_reader(world)
    built = []
    for p in placements:
        kind = p['structure']
        if kind not in TEMPLATES:
            raise ValueError(f'unknown structure {kind} (known: {sorted(TEMPLATES)})')
        x, y, z = p['world_xyz']
        template = TEMPLATES[kind]()
        radius = max(abs(dx) for dx, _, _ in template) + 1
        height = max(dy for _, dy, _ in template) + 2
        clear_and_foundation(editor, x, y, z, radius + 1, height,
                             block('polished_deepslate'), column=reader)
        for (dx, dy, dz), state in template.items():
            editor.set(x + dx, y + dy, z + dz, state)
        built.append({'structure': kind, 'team': p.get('team'), 'world_xyz': [x, y, z],
                      'blocks': len(template), 'footprint_radius': radius, 'height': height})
    if not dry_run:
        editor.flush()
    result = {'schema': 'team_structures_built/1', 'evidence_state': 'RAW WORLD OBSERVATION',
              'world': str(world), 'dry_run': dry_run,
              'blocks_written': editor.written, 'structures': built,
              'nature': 'greybox massing in vanilla-native palettes, not final architecture',
              'not_covered': ['exposure, disable conditions, obstruction, validation, reactivation '
                              'and repair, all unresolved in objectives.md',
                              'interiors, defender placement and spawners',
                              'redstone or any functional machinery',
                              'terrain sculpting beyond a levelled pad under each structure']}
    report.parent.mkdir(parents=True, exist_ok=True)
    report.write_text(json.dumps(result, indent=2, sort_keys=True) + '\n')
    return result


if __name__ == '__main__':
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('--world', type=Path, required=True)
    p.add_argument('--sites', type=Path, required=True, help='output of vanilla_search.structures')
    p.add_argument('--report', type=Path, required=True)
    p.add_argument('--rank', type=int, default=0, help='which scored candidate to build, best = 0')
    p.add_argument('--dry-run', action='store_true')
    a = p.parse_args()
    sites = json.loads(a.sites.read_text())
    placements = []
    for team, layers in sites['teams'].items():
        for kind, entry in layers.items():
            if a.rank >= len(entry['candidates']):
                raise SystemExit(f'{team}/{kind} has no candidate at rank {a.rank}')
            placements.append({'structure': kind, 'team': team,
                               'world_xyz': entry['candidates'][a.rank]['world_xyz']})
    result = build(a.world.resolve(), placements, a.report.resolve(), a.dry_run)
    print(f"{'Would write' if a.dry_run else 'Wrote'} {result['blocks_written']} blocks "
          f"across {len(result['structures'])} structures")
