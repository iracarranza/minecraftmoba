"""Make the Lair a recognizable place, and nothing more than that.

Doctrine is unusually specific about what this must NOT be. The Lair's encounter
space is the natural 3D terrain, so authoring here does not flatten, excavate,
enclose, force a circle or impose biome neutrality. The manifestation exists to
supply five things and stop:

    RECOGNIZABILITY        it reads as a place, from outside it
    LOCATION IDENTITY      it is the same place every Lair night
    RUNTIME ANCHORING      the plugin has a position to bind to
    SPAWN REGISTRATION     an occupant has somewhere valid to appear
    PERSISTENCE            it survives being fought over

Everything visual here is PROVISIONAL. Art direction is explicitly open, and a
provisional treatment must not become doctrine by being written down: the
standing stones below are a legible placeholder for "something ancient marks
this", not a decision that the Lair is a stone circle. They are deliberately
irregular, deliberately terrain-following, and deliberately sparse.

The one non-provisional part is the anchor: a small plinth at the socket centre
whose top is a known, valid, non-suffocating block position. That is what the
runtime binds to and spawns from, so it is load-bearing rather than decorative.
"""
from __future__ import annotations

import math

from serialization.world import block

# PROVISIONAL treatment. Kept small on purpose: doctrine says the terrain is the
# encounter, and a larger authored footprint would start to be the arena the
# spec forbids.
MARKER_COUNT = 9
MARKER_MIN_HEIGHT = 2
MARKER_MAX_HEIGHT = 5
ANCHOR_RADIUS = 2


def _irregular_angles(count, seed):
    """Angles that read as placed rather than as generated.

    Evenly spaced markers would make a circle, which is exactly the geometry the
    spec says not to force. These are jittered by a deterministic amount so the
    ring is recognisable without being regular, and the same site always
    produces the same arrangement.
    """
    out = []
    for i in range(count):
        base = 2 * math.pi * i / count
        jitter = ((seed >> (i * 3)) & 7) / 7.0 - 0.5      # +/- half a step
        out.append(base + jitter * (2 * math.pi / count))
    return out


def manifest(surface, centre_x, centre_z, *, radius=14, seed=0):
    """Blocks and anchor for one Lair, given a `surface(x, z) -> y` callable.

    Returns (blocks, anchor) where blocks maps absolute (x, y, z) to a state.
    Absolute rather than relative because every marker sits on its own terrain
    height -- that is what terrain-conforming means here, and a single origin
    plus offsets cannot express it.
    """
    blocks = {}
    markers = []
    for i, angle in enumerate(_irregular_angles(MARKER_COUNT, seed or 1)):
        # Distance varies too, so the markers do not describe a circle.
        reach = radius * (0.7 + 0.3 * (((seed >> (i * 5)) & 3) / 3.0))
        x = int(round(centre_x + math.cos(angle) * reach))
        z = int(round(centre_z + math.sin(angle) * reach))
        y = surface(x, z)
        if y is None:
            continue
        height = MARKER_MIN_HEIGHT + (((seed >> (i * 7)) & 3)
                                      % (MARKER_MAX_HEIGHT - MARKER_MIN_HEIGHT + 1))
        for dy in range(height):
            blocks[(x, y + 1 + dy, z)] = block(
                'cobbled_deepslate' if dy < height - 1 else 'cracked_deepslate_bricks')
        markers.append({'xz': [x, z], 'base_y': y, 'height': height})

    # The anchor. Load-bearing, unlike the markers.
    anchor_y = surface(centre_x, centre_z)
    if anchor_y is None:
        return {}, None
    for dx in range(-ANCHOR_RADIUS, ANCHOR_RADIUS + 1):
        for dz in range(-ANCHOR_RADIUS, ANCHOR_RADIUS + 1):
            if dx * dx + dz * dz > ANCHOR_RADIUS * ANCHOR_RADIUS + 1:
                continue
            y = surface(centre_x + dx, centre_z + dz)
            if y is None:
                continue
            # One course of plinth laid on the existing surface, following it
            # rather than levelling it.
            blocks[(centre_x + dx, y + 1, centre_z + dz)] = block(
                'polished_deepslate' if (dx or dz) else 'chiseled_deepslate')
    anchor = {
        'xyz': [centre_x, anchor_y + 2, centre_z],
        'surface_y': anchor_y,
        'note': 'spawn/binding anchor; the plinth top plus one, so an occupant '
                'appears in open air rather than inside the marker',
    }
    return blocks, {'anchor': anchor, 'markers': markers,
                    'treatment': 'PROVISIONAL: irregular standing stones and a '
                                 'plinth. Art direction is open; this is a legible '
                                 'placeholder, not a decision that the Lair is a '
                                 'stone circle.'}


def build(editor, surface, centre_x, centre_z, *, radius=14, seed=0):
    """Write the manifestation through a WorldEditor. Returns its record."""
    blocks, record = manifest(surface, centre_x, centre_z, radius=radius, seed=seed)
    if record is None:
        return None
    for (x, y, z), state in blocks.items():
        editor.set(x, y, z, state)
    record['blocks'] = len(blocks)
    record['centre_xz'] = [centre_x, centre_z]
    record['radius'] = radius
    return record
