"""Where Overworld structures are, computed from the seed rather than generated.

Structure placement is a 48-bit LCG seeded per region from the world seed, so
a candidate position is arithmetic. An 8192-block square costs about 0.07s.

TWO STEPS, AND THE SECOND IS NOT OPTIONAL. `getStructurePos` says where a
structure WOULD go in its region; it only generates if the biome accepts it.
Every region of the world holds a Mansion candidate and almost none hold a
Mansion, so reporting step one alone is not an approximation, it is noise.
`mobastruct` applies `isViableStructurePos` before emitting anything.

WHAT THIS UNLOCKS. A Map Type is a (feature, LOCATION) pair, and until now the
location half had nothing to attach a named feature to. `central` and
`contained` below are the location terms: "a Mansion somewhere in this scoop"
and "a Mansion at the middle of this scoop" are different maps, and only the
second is Pale Forest.

TRUSTED FOR: position and type exactly. The reported `y` is the approximate
surface height and carries the same heavy tail as the rest of the scan.
"""
from __future__ import annotations

import json
import os
import shutil
import subprocess

STRUCT_ENV = 'MOBA_CUBIOMES_STRUCT'

KINDS = ('mansion', 'village', 'monument', 'jungle_temple', 'desert_pyramid',
         'outpost', 'igloo', 'swamp_hut', 'ruined_portal', 'shipwreck',
         'ocean_ruin', 'ancient_city', 'trail_ruins')


def finder_path() -> str | None:
    explicit = os.environ.get(STRUCT_ENV)
    if explicit and os.path.exists(explicit):
        return explicit
    return shutil.which('mobastruct')


def find(seed: int, *, half: int = 4096, kinds=None,
         finder: str | None = None, timeout: int = 300) -> dict:
    finder = finder or finder_path()
    if not finder:
        raise RuntimeError(
            f'no mobastruct on $PATH or ${STRUCT_ENV}; see '
            f'native/cubiomes/README.md. Returning "no structures" without '
            f'looking would make every structural Map Type unsatisfiable and '
            f'look like a property of the seed.')
    unknown = set(kinds or ()) - set(KINDS)
    if unknown:
        raise ValueError(f'unknown structure kinds: {sorted(unknown)}')
    cmd = [finder, str(seed), str(half)]
    if kinds:
        cmd.append(','.join(kinds))
    out = subprocess.run(cmd, check=True, capture_output=True, timeout=timeout)
    return json.loads(out.stdout.decode())


def within(found, x0: int, x1: int, z0: int, z1: int, kind: str | None = None):
    """Structures inside a block rectangle, optionally of one kind."""
    return [f for f in found
            if x0 <= f['x'] <= x1 and z0 <= f['z'] <= z1
            and (kind is None or f['type'] == kind)]


def contained(found, kind: str, bounds) -> int:
    """How many of `kind` fall inside the scoop at all."""
    return len(within(found, *bounds, kind=kind))


def central(found, kind: str, bounds, *, fraction: float = 0.34):
    """Structures of `kind` in the middle `fraction` of the scoop, by area.

    The location half of a (feature, location) Type term. A Mansion at the
    scoop edge belongs to one team; a Mansion at the middle is contested, and
    only the second arrangement is the Pale Forest premise. NON-CANON FIXTURE:
    a third is a shape choice with no measurement behind it yet.
    """
    x0, x1, z0, z1 = bounds
    mx, mz = (x0 + x1) / 2, (z0 + z1) / 2
    hw, hd = (x1 - x0) * fraction / 2, (z1 - z0) * fraction / 2
    return within(found, int(mx - hw), int(mx + hw),
                  int(mz - hd), int(mz + hd), kind=kind)


def offset_from_centre(found_one, bounds) -> dict:
    """How far off centre one structure sits, as a fraction of the scoop."""
    x0, x1, z0, z1 = bounds
    mx, mz = (x0 + x1) / 2, (z0 + z1) / 2
    w, d = max(1, x1 - x0), max(1, z1 - z0)
    return {'x_offset': round((found_one['x'] - mx) / w, 4),
            'z_offset': round((found_one['z'] - mz) / d, 4),
            'blocks_from_centre': round(
                ((found_one['x'] - mx) ** 2 + (found_one['z'] - mz) ** 2) ** 0.5, 1)}
