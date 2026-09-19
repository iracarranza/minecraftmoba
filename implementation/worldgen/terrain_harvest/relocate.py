"""State-aware relocation: translation and quarter-turn rotation, fail-closed.

Nothing here is wired into corpus export. The exporter still refuses non-identity
placement; this module exists so relocation can be proven on fixtures first.

The rule is reject, never guess. A block property is rotated only if it appears
in ROTATABLE, and passed through only if it appears in ORIENTATION_FREE. Anything
else raises UnsupportedState, because a property nobody has classified is exactly
the one that will be silently corrupted by a rotation.
"""
from __future__ import annotations
import copy
import math
from serialization.nbt import COMPOUND, DOUBLE, FLOAT, compound, double, float_tag, integer, list_tag

class UnsupportedState(ValueError):
    """Raised instead of writing state this module cannot rotate correctly."""

CARDINALS = ['north', 'east', 'south', 'west']

# Properties whose value encodes a direction or orientation and which this
# module knows how to rotate. Anything coordinate-bearing and absent from here
# must be rejected, not passed through.
ROTATABLE = {'facing', 'axis', 'rotation', 'shape', 'orientation', 'hinge',
             'north', 'east', 'south', 'west', 'attachment'}

# Properties that carry no horizontal orientation. Vertical and scalar state.
ORIENTATION_FREE = {
    'waterlogged', 'level', 'age', 'power', 'powered', 'lit', 'open', 'occupied',
    'half', 'type', 'layers', 'bites', 'delay', 'distance', 'persistent', 'snowy',
    'stage', 'moisture', 'honey_level', 'hatch', 'eggs', 'pickles', 'candles',
    'charges', 'note', 'instrument', 'mode', 'conditional', 'inverted', 'triggered',
    'extended', 'short', 'has_record', 'has_book', 'has_bottle_0', 'has_bottle_1',
    'has_bottle_2', 'signal_fire', 'in_wall', 'attached', 'disarmed', 'unstable',
    'drag', 'bloom', 'berries', 'tilt', 'thickness', 'vertical_direction',
    'up', 'down', 'down_', 'leaves', 'tip', 'slot_0_occupied', 'slot_1_occupied',
    'slot_2_occupied', 'slot_3_occupied', 'slot_4_occupied', 'slot_5_occupied',
    'crafting', 'cracked', 'flower_amount', 'segment_amount', 'active', 'natural',
    'blocked', 'can_summon', 'shrieking', 'ominous', 'trial_spawner_state',
    'vault_state', 'creaking', 'dusted', 'light_level',
}

RAIL_SHAPES = {'north_south': 'east_west', 'east_west': 'north_south',
               'ascending_north': 'ascending_east', 'ascending_east': 'ascending_south',
               'ascending_south': 'ascending_west', 'ascending_west': 'ascending_north',
               'north_east': 'south_east', 'south_east': 'south_west',
               'south_west': 'north_west', 'north_west': 'north_east'}
# Stair and door shapes are expressed relative to facing, so rotating facing is enough.
RELATIVE_SHAPES = {'straight', 'inner_left', 'inner_right', 'outer_left', 'outer_right'}

def rotate_direction(value, quarter):
    if value in ('up', 'down'): return value
    if value not in CARDINALS: raise UnsupportedState(f'unrotatable direction {value!r}')
    return CARDINALS[(CARDINALS.index(value) + quarter) % 4]

def rotate_properties(name, properties, quarter):
    quarter %= 4
    out = {}
    for key, value in properties.items():
        if quarter == 0:
            out[key] = value; continue
        if key in ORIENTATION_FREE: out[key] = value; continue
        if key not in ROTATABLE:
            raise UnsupportedState(f'{name}: unclassified property {key!r}; classify it before rotating')
        if key == 'facing': out[key] = rotate_direction(value, quarter)
        elif key == 'axis':
            if value == 'y': out[key] = value
            elif value in ('x', 'z'): out[key] = {'x': 'z', 'z': 'x'}[value] if quarter % 2 else value
            else: raise UnsupportedState(f'{name}: unknown axis {value!r}')
        elif key == 'rotation':
            out[key] = str((int(value) + 4 * quarter) % 16)
        elif key == 'shape':
            if value in RELATIVE_SHAPES: out[key] = value
            elif value in RAIL_SHAPES:
                v = value
                for _ in range(quarter): v = RAIL_SHAPES[v]
                out[key] = v
            else: raise UnsupportedState(f'{name}: unknown shape {value!r}')
        elif key == 'hinge': out[key] = value  # left/right is relative to facing
        elif key == 'orientation':
            # Crafter and jigsaw: "<vertical>_<cardinal>" or "<cardinal>_<vertical>".
            parts = value.split('_')
            if len(parts) != 2: raise UnsupportedState(f'{name}: unknown orientation {value!r}')
            out[key] = '_'.join(p if p in ('up', 'down') else rotate_direction(p, quarter) for p in parts)
        elif key == 'attachment': out[key] = value  # floor/ceiling/wall, vertical only
        elif key in CARDINALS:
            continue  # handled below as a group
        else: raise UnsupportedState(f'{name}: unhandled rotatable property {key!r}')
    present = [k for k in CARDINALS if k in properties]
    if present and quarter:
        for key in present:
            out[CARDINALS[(CARDINALS.index(key) + quarter) % 4]] = properties[key]
    return out

def rotate_point(point, quarter):
    """Clockwise about the origin in the XZ plane; Y is untouched."""
    x, y, z = point
    for _ in range(quarter % 4): x, z = -z, x
    return (x, y, z)

def place_point(point, quarter, translation):
    x, y, z = rotate_point(point, quarter)
    return (x + translation[0], y + translation[1], z + translation[2])

# Block entity fields that name a position and must move with the block.
BLOCK_ENTITY_POSITIONS = {'x': 'x', 'y': 'y', 'z': 'z'}
# Block entity fields known to carry a position this module does not relocate.
BLOCK_ENTITY_UNSUPPORTED = {
    'ExitPortal': 'end gateway destination',
    'FlowerPos': 'beehive flower position',
    'flower_pos': 'beehive flower position',
    'target_pos': 'jigsaw or lodestone target',
    'Target': 'lodestone target',
    'posX': 'structure block offset', 'posY': 'structure block offset', 'posZ': 'structure block offset',
    'source_pos': 'piston source', 'ExitPortalX': 'end gateway destination',
}

def relocate_block_entity(tag, quarter, translation):
    d = copy.deepcopy(tag).value
    for key, reason in BLOCK_ENTITY_UNSUPPORTED.items():
        if key in d: raise UnsupportedState(f'block entity carries {reason} ({key}); relocation not implemented')
    if not all(a in d for a in ('x', 'y', 'z')): raise UnsupportedState('block entity without position')
    x, y, z = place_point(tuple(d[a].value for a in ('x', 'y', 'z')), quarter, translation)
    d['x'], d['y'], d['z'] = integer(x), integer(y), integer(z)
    return compound(**d)

def relocate_tick(tag, quarter, translation):
    d = copy.deepcopy(tag).value
    x, y, z = place_point(tuple(d[a].value for a in ('x', 'y', 'z')), quarter, translation)
    d['x'], d['y'], d['z'] = integer(x), integer(y), integer(z)
    return compound(**d)

# Entity fields that name a position or direction beyond Pos/Motion/Rotation.
ENTITY_UNSUPPORTED = {
    'leash': 'leash anchor', 'Leash': 'leash anchor',
    'Brain': 'villager memories hold positions',
    'SleepingX': 'sleeping position', 'SleepingY': 'sleeping position', 'SleepingZ': 'sleeping position',
    'TileX': 'hanging entity anchor', 'TileY': 'hanging entity anchor', 'TileZ': 'hanging entity anchor',
    'block_pos': 'hanging entity anchor',
    'HomePosX': 'home position', 'PatrolTarget': 'patrol target',
    'BeamTarget': 'beam target', 'wander_target': 'wander target',
}

def relocate_entity(tag, quarter, translation):
    d = copy.deepcopy(tag).value
    for key, reason in ENTITY_UNSUPPORTED.items():
        if key in d: raise UnsupportedState(f'entity carries {reason} ({key}); relocation not implemented')
    if 'Pos' not in d: raise UnsupportedState('entity without Pos')
    px, py, pz = (p.value for p in d['Pos'].value)
    # Rotate about the block-coordinate origin in continuous space.
    for _ in range(quarter % 4): px, pz = -pz, px
    d['Pos'] = list_tag(DOUBLE, [double(px + translation[0]), double(py + translation[1]), double(pz + translation[2])])
    if 'Motion' in d:
        mx, my, mz = (m.value for m in d['Motion'].value)
        for _ in range(quarter % 4): mx, mz = -mz, mx
        d['Motion'] = list_tag(DOUBLE, [double(mx), double(my), double(mz)])
    if 'Rotation' in d:
        yaw, pitch = (r.value for r in d['Rotation'].value)
        d['Rotation'] = list_tag(FLOAT, [float_tag((yaw + 90.0 * (quarter % 4)) % 360.0), float_tag(pitch)])
    if 'Passengers' in d:
        d['Passengers'] = list_tag(COMPOUND, [relocate_entity(p, quarter, translation) for p in d['Passengers'].value])
    return compound(**d)
