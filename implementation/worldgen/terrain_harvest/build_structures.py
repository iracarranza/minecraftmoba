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


def from_vanilla_nbt(entry, *, jar=None, centre=True, skip_air=True):
    """Place an actual vanilla structure, block for block, from its own NBT.

    Preferred over hand-modelled massing wherever Minecraft ships the thing.
    The greybox `pillager_outpost` below was a seven-by-seven dark oak box
    standing in for a fifteen-by-fifteen watchtower, which meant the compiler
    verified a site against the MEASURED contract and then authored something a
    different size onto it -- the verifier and the builder disagreeing about
    what was being built.

    `structure_void` is dropped as vanilla does (it means "leave what is
    there"), and air is dropped by default so a structure does not punch a
    rectangular hole in a hillside; the pad has already been cleared.
    """
    from .vanilla_assets import DEFAULT_JAR, read_structure
    doc = read_structure(Path(jar) if jar else DEFAULT_JAR, entry)
    palette = doc['palette']
    size = [int(v) for v in doc['size']]
    ox, oz = (size[0] // 2, size[2] // 2) if centre else (0, 0)
    out = {}
    for b in doc['blocks']:
        entry_state = palette[int(b['state'])]
        name = str(entry_state['Name'])
        if name == 'minecraft:structure_void':
            continue
        if skip_air and name in ('minecraft:air', 'minecraft:cave_air', 'minecraft:void_air'):
            continue
        props = {str(k): str(v) for k, v in (entry_state.get('Properties') or {}).items()}
        x, y, z = (int(v) for v in b['pos'])
        out[(x - ox, y, z - oz)] = block(name.removeprefix('minecraft:'), **props)
    return out


def pillager_outpost_watchtower(jar=None):
    """The vanilla watchtower, and only the watchtower.

    Vanilla ships the cages, tents, log piles, targets and plates as separate
    feature_*.nbt files, so "watchtower only" is a file-level cut rather than a
    judgement about one mesh.
    """
    return from_vanilla_nbt('data/minecraft/structure/pillager_outpost/watchtower.nbt', jar=jar)


def nether_bastion_body(jar=None):
    """The Bridge Bastion's central body: the entrance piece over its base.

    The long projecting bridge and the legs that carry it are separate vanilla
    pieces and are not placed. The ramparts are also separate, and vanilla
    assembles them by jigsaw connection, which is NOT reproduced here -- what is
    placed is the starting body, and the rampart ring is recorded as unbuilt
    rather than faked with a guessed offset.
    """
    base = from_vanilla_nbt(
        'data/minecraft/structure/bastion/bridge/starting_pieces/entrance_base.nbt', jar=jar)
    top = from_vanilla_nbt(
        'data/minecraft/structure/bastion/bridge/starting_pieces/entrance.nbt', jar=jar)
    lift = max(dy for _, dy, _ in base) + 1
    out = dict(base)
    for (dx, dy, dz), state in top.items():
        out[(dx, dy + lift, dz)] = state
    return out


# What the vanilla-native placements do NOT reproduce, stated rather than left
# to be discovered. Consumed by the compiler's evidence, so a map never claims
# more than was actually built.
VANILLA_PLACEMENT_GAPS = {
    'nether_bastion': 'the rampart ring is a separate set of jigsaw pieces; vanilla '
                      'connects them procedurally and that assembly is not reproduced. '
                      'The central body is placed; the ramparts are not.',
}


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


# The measured vanilla End Spike, reproduced rather than approximated.
#
# Every number here came out of a generated vanilla End dimension
# (reports/objective_forms_2026-09-23/), not out of a guess:
#
#   - Ten spikes, pillar radius 2-5, height 76-103 above the island surface.
#   - Radius and height co-vary. The ten measured spikes are exactly
#     (2, 76) (2, 79) (2, 82) (3, 85) (3, 88) (3, 91) (4, 94) (4, 97) (4, 100)
#     (5, 103) -- height = 76 + 3i for i = 0..9, radius = 2 + i // 3.
#   - The pillar is a disc under `dx^2 + dz^2 <= r^2 + 1`, which reproduces the
#     measured column counts 21 / 37 / 57 / 89 exactly for r = 2 / 3 / 4 / 5.
#     A plain `<= r^2` gives 13 / 29 / 49 / 81 and is wrong for every radius.
#   - A SINGLE bedrock block caps the centre at top + 1 -- not a bedrock layer.
#   - Caged spikes carry a 5x5 iron-bar box: walls at top+1..top+3, roof at
#     top+4. Two of the ten measured spikes were caged.
SPIKE_HEIGHTS = tuple(76 + 3 * i for i in range(10))
SPIKE_RADII = tuple(2 + i // 3 for i in range(10))


def spike_disc(radius):
    """Vanilla's pillar cross-section. See the note above for why `+ 1`."""
    return [(dx, dz)
            for dx in range(-radius, radius + 1)
            for dz in range(-radius, radius + 1)
            if dx * dx + dz * dz <= radius * radius + 1]


def end_spike(radius=3, height=88, caged=False):
    # The defaults are the MEDIAN of the ten measured spikes, not a preference:
    # radii run 2,2,2,3,3,3,4,4,4,5 and heights 76..103 in steps of three, so
    # (3, 88) is the middle of both. Vanilla varies them per spike; a map that
    # wants that variation passes it in. Choosing the median is a parameter
    # choice inside a measured range, not a new constant.
    """An Ender Dragon arena End Spike: obsidian pillar, bedrock cap, cage.

    The End Crystal itself is an ENTITY, not a block, so it is not authored
    here. That is a seam, not an omission: the runtime already spawns the Lair's
    occupant, and the crystal belongs to the same layer. `end_spike_entities`
    reports where it goes so nothing has to rediscover it.

    `height` is measured from the pillar's base upward, so the caller places the
    base on the ground and the spike rises from it, exactly as vanilla's do from
    the island surface.
    """
    if not 2 <= radius <= 5:
        raise ValueError(f'measured pillar radius is 2-5, got {radius}')
    if not 76 <= height <= 103:
        raise ValueError(f'measured spike height is 76-103, got {height}')
    out = {}
    disc = spike_disc(radius)
    for dy in range(height):
        for dx, dz in disc:
            out[(dx, dy, dz)] = block('obsidian')
    top = height - 1
    out[(0, top + 1, 0)] = block('bedrock')       # one block, the crystal's base
    if caged:
        for dy in range(top + 1, top + 4):        # three courses of wall
            for dx in range(-2, 3):
                for dz in range(-2, 3):
                    if abs(dx) == 2 or abs(dz) == 2:
                        out[(dx, dy, dz)] = block('iron_bars')
        for dx in range(-2, 3):                   # and a roof
            for dz in range(-2, 3):
                out[(dx, top + 4, dz)] = block('iron_bars')
    return out


def end_spike_entities(radius=3, height=88, caged=False):
    """Where the End Crystal goes, for whatever spawns entities.

    Reported rather than placed. Region-file authoring writes blocks; entities
    live elsewhere in the save and, in this project, are the plugin's job.
    """
    return [{'type': 'minecraft:end_crystal',
             'offset_xyz': [0.5, height + 1, 0.5],
             'note': 'sits on the bedrock cap; ShowBottom false in vanilla arenas'}]


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
    # Real vanilla geometry where Minecraft ships it. The hand-modelled
    # `pillager_outpost` and `nether_bastion` below are kept under GREYBOX
    # names: they are still useful for quick massing, and old artifacts that
    # reference them stay readable, but they are not what gets built.
    'pillager_outpost': pillager_outpost_watchtower,
    'nether_bastion': nether_bastion_body,
    # The measured arena spike. `end_tower` remains registered under its OWN
    # name only: it is historical geometry, it is not what the current contract
    # describes, and old artifacts that reference it stay readable.
    'end_spike': end_spike,
    'end_tower': end_tower,
    'pillager_outpost_greybox': pillager_outpost,
    'nether_bastion_greybox': nether_bastion,
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
                # Unreachable in the normal path: build() refuses a site with
                # standing structure before any writing starts. Kept as the
                # last line of defence for a caller that bypasses that, and it
                # still refuses to overwrite rather than silently skipping --
                # a skip is what embedded a village house in an objective.
                raise ValueError(
                    f'built structure {here} at {[x, y, z]} inside an authoring '
                    f'footprint; this site should have been refused, not cleared')
            if here is not None and is_leaf(here):
                # A PAD fells; a CORRIDOR brushes. The difference is measured.
                #
                # Brushing the branch sliced every overhanging canopy flat at
                # the pad boundary: a tree whose trunk stands outside the disc
                # lost exactly the part that reached in, so the authored Outpost
                # sat in a ring of trees cut in half.
                #
                # `routes.carve` deliberately does the OPPOSITE, and is right
                # to: felling for a leaf along a corridor "turned a corridor
                # through a forest into a clear-cut -- 186,226 blocks of one".
                #
                # So this is NOT the Route rule generalized, and an earlier
                # commit message here claiming it was is wrong. A pad is a
                # compact disc that is levelled anyway, where a felled tree
                # reads as clearing and a bisected one as damage. A corridor is
                # a thin line through standing forest, where felling everything
                # that overhangs destroys the forest the corridor runs through.
                # The shared rule is that authoring must look like work someone
                # did; what that implies differs by the shape of the work.
                fell(editor, column, x, y, z, AIR)
                continue
            if here is not None and is_log(here):
                fell(editor, column, x, y, z, AIR)   # a trunk comes down whole
                continue
            editor.set(x, y, z, AIR)
        editor.set(x, cy - 1, z, foundation)


def _extents(placements):
    """Footprint half-width and headroom per structure, from its own template."""
    out = {}
    for p in placements:
        kind = p['structure']
        template = TEMPLATES.get(kind)
        if template is None:
            continue
        blocks = template()
        half = max(abs(dx) for dx, _, _ in blocks) + 2
        headroom = max(dy for _, dy, _ in blocks) + 2
        out[kind] = (half, headroom)
    return out


def build(world: Path, placements, report: Path, dry_run=False):
    editor = WorldEditor(world)
    reader = _column_reader(world)

    # Refuse before writing, not around what is there.
    #
    # clear_and_foundation used to step over a built block as "not terrain",
    # which meant an objective sited on a village house was built AROUND the
    # house. column_scan.verify_site fails the same condition, so the builder
    # and the verifier held opposite policies and which applied depended on
    # which ran. The verifier is right. Every offending site is reported at
    # once and nothing is written, because a partial build leaves a world that
    # is neither the old one nor the new one.
    from .clearance import refuse_built_sites, evict_entities
    extents = _extents(placements)
    refusals = refuse_built_sites(reader, placements, extents)
    if refusals:
        return {'schema': 'team_structures_built/1',
                'evidence_state': 'RAW WORLD OBSERVATION',
                'world': str(world), 'dry_run': dry_run,
                'blocks_written': 0, 'structures': [], 'refused': refusals,
                'note': 'nothing was written: one or more sites stand on built '
                        'structure, and authoring refuses rather than absorbing it'}

    # Animals standing in a footprint are removed before the blocks land on
    # them. A sheep was entombed by the Bastion because nothing looked.
    evicted = {'removed': 0} if dry_run else evict_entities(Path(world), placements, extents)

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
              'world': str(world), 'dry_run': dry_run, 'refused': [],
              'entities_evicted': evicted,
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
