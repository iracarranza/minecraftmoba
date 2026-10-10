"""What the in-world map inspection overlay draws, as data.

A compiled map is certified against measurements the player never sees: the two team cost
fields C_N and C_S over the analysis cells, the bands derived from them, and the field
points the compiler placed inside those bands. The overlay makes them visible in a launched
lab scoop, and this module is where that data is assembled, so the overlay (Java) only draws
and never re-derives.

Nothing here is a new measurement. Costs come from `cell_grid`, bands from `reach_fields`
(which reuses `portfolio`'s own rule rather than restating it, on purpose: two definitions of
"near" is the failure the compiler already paid for once), points from the runtime bindings.

The file is a SIDECAR, ``inspection.json`` beside ``map.json``. It is not part of the world,
so it does not touch the world fingerprint, and a scoop without one simply has no overlay.
"""
from __future__ import annotations

SCHEMA = 'moba_map_inspection/1'
CELL_SIZE = 64
OPENING_COST = 120.0


def _cell_row(cell: dict, bands: dict, opening_cost: float) -> dict:
    from . import reach_fields
    key = tuple(cell.get('cell') or ())
    return {
        'cell': list(cell['cell']),
        'origin': list(cell['world_origin']),
        'centroid': list(cell.get('centroid') or cell['world_origin']),
        'cost': dict(cell.get('strategic_depth_cost') or {}),
        'relation': reach_fields.relation(cell, opening_cost),
        'band': dict(bands.get(key) or {}),
        'surface_y': cell.get('mean_surface_y'),
    }


def build(evidence: dict, bindings: dict | None = None, *, opening_cost: float = OPENING_COST,
          cell_size: int = CELL_SIZE) -> dict | None:
    """The overlay's data, or None when the compilation carried no cell grid."""
    from . import reach_fields
    cells = ((evidence or {}).get('cell_grid') or {}).get('cells') or []
    if not cells:
        return None
    bands = reach_fields.bands(cells, opening_cost)
    rows = [_cell_row(c, bands, opening_cost) for c in cells]
    out = {
        'schema': SCHEMA,
        'cell_size': cell_size,
        'opening_cost': opening_cost,
        'units': (evidence.get('cell_grid') or {}).get('units'),
        'cells': rows,
        'fountains': {}, 'objectives': [], 'lair': None, 'worksites': [], 'points': [],
        'note': 'Costs are terrain-weighted traversal cost from each Fountain. Bands are an analytical '
                'grouping, not an authored polygon. Field points are what the compiler placed; an '
                'absent point is a real answer.',
    }
    b = bindings or {}
    out['fountains'] = {t: list(v) for t, v in (b.get('fountains') or {}).items()}
    for team, kinds in (b.get('objectives') or {}).items():
        for kind, xz in (kinds or {}).items():
            out['objectives'].append({'team': team, 'kind': kind, 'x': xz[0], 'z': xz[1]})
    lair = (b.get('lair') or {}).get('anchor') or {}
    if lair.get('xyz'):
        out['lair'] = list(lair['xyz'])
    for w in b.get('worksites') or []:
        xz = w.get('world_xz')
        if xz:
            out['worksites'].append({'id': w.get('id'), 'x': xz[0], 'z': xz[1], 'reach': w.get('reach')})
    for s in ((b.get('renewables') or {}).get('sources') or []):
        out['points'].append({
            'id': s.get('id'), 'kind': s.get('kind'), 'type': s.get('type'), 'band': s.get('band'),
            'near_team': s.get('near_team'), 'cell': s.get('cell'), 'x': s.get('x'), 'y': s.get('y'),
            'z': s.get('z'), 'radius': s.get('radius'), 'capacity': s.get('capacity'),
            'cost': s.get('strategic_depth_cost')})
    return out
