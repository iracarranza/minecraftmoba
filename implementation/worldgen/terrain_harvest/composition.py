"""Composition manifests: references, transforms, overlap and seam records only.

This module places no blocks and sculpts no terrain. It validates that a
proposed composition is internally coherent and records what remains unresolved,
so that a composition can be discussed and reviewed before anything is built.

Three rules it will not bend:
  - Natural geography and authored systems stay separate layers.
  - A measurement inherited from a source volume goes stale the moment the
    geometry it described changes, and is marked stale rather than carried over.
  - A seam is an unresolved record, never an interpolation.
"""
from __future__ import annotations
import argparse
import json
from pathlib import Path
from .materialize import json_write
from .model import Mask, digest, loads
from .relocate import UnsupportedState, place_point

SCHEMA = 'terrain_composition/1'
SEAM_STATES = {'UNRESOLVED', 'OBSERVED_INCOMPATIBLE', 'OBSERVED_COMPATIBLE_PENDING_BUILD'}

def placed_bounds(volume, transform):
    """Axis-aligned bounds after rotation and translation. Rotation is exact."""
    b = volume['provenance']['source_bounds']
    quarter = transform['rotation'] // 90
    corners = [place_point((x, y, z), quarter, transform['translation'])
               for x in b['x'] for y in b['y'] for z in b['z']]
    return {'x': [min(c[0] for c in corners), max(c[0] for c in corners)],
            'y': [min(c[1] for c in corners), max(c[1] for c in corners)],
            'z': [min(c[2] for c in corners), max(c[2] for c in corners)]}

def boxes_overlap(a, b):
    return all(a[k][0] <= b[k][1] and b[k][0] <= a[k][1] for k in ('x', 'y', 'z'))

def overlap_box(a, b):
    return {k: [max(a[k][0], b[k][0]), min(a[k][1], b[k][1])] for k in ('x', 'y', 'z')}

def stale_measurements(volume, transform):
    """A measurement describes the source geometry. Moving it invalidates it."""
    moved = transform != {'rotation': 0, 'translation': [0, 0, 0]}
    out = []
    for m in volume.get('measurements', []):
        entry = dict(m)
        if moved and m.get('evidence_state') in ('DERIVED MEASUREMENT', 'RAW WORLD OBSERVATION'):
            entry['evidence_state'] = 'UNRESOLVED'
            entry['stale_reason'] = 'inherited from the unplaced source volume; geometry changed under placement'
        out.append(entry)
    return out

def validate_composition(doc, library):
    """Return the checked composition, or raise. Nothing here writes blocks."""
    if doc.get('schema') != SCHEMA: raise ValueError('unknown composition schema')
    policy = doc.get('overlap_policy')
    if policy not in ('forbid', 'record_only'): raise ValueError("overlap_policy must be 'forbid' or 'record_only'")
    placements = doc.get('placements') or []
    if not placements: raise ValueError('composition needs at least one placement')
    seen = set(); resolved = []
    for p in placements:
        ident = p['volume_id']
        if ident in seen: raise ValueError(f'volume placed twice: {ident}')
        seen.add(ident)
        if ident not in library: raise ValueError(f'unknown volume reference: {ident}')
        volume = library[ident]
        t = p.get('transform') or {'rotation': 0, 'translation': [0, 0, 0]}
        if t['rotation'] not in (0, 90, 180, 270): raise ValueError('rotation must be a quarter turn')
        if len(t['translation']) != 3 or any(type(n) is not int for n in t['translation']):
            raise ValueError('translation must be three integers')
        # Refuse a placement the relocator could not actually carry out.
        if t != {'rotation': 0, 'translation': [0, 0, 0]}:
            p.setdefault('placement_status', 'PLANNED_NOT_BUILDABLE')
            if p['placement_status'] != 'PLANNED_NOT_BUILDABLE':
                raise ValueError('physical relocation is not implemented; placement must stay PLANNED_NOT_BUILDABLE')
        resolved.append({'volume_id': ident, 'transform': t,
                         'source_bounds': volume['provenance']['source_bounds'],
                         'placed_bounds': placed_bounds(volume, t),
                         'classification': volume['classification']['kind'],
                         'placement_status': p.get('placement_status', 'IDENTITY_PLACEMENT'),
                         'measurements': stale_measurements(volume, t)})

    overlaps = []
    for i, a in enumerate(resolved):
        for b in resolved[i+1:]:
            if boxes_overlap(a['placed_bounds'], b['placed_bounds']):
                overlaps.append({'volumes': [a['volume_id'], b['volume_id']],
                                 'box': overlap_box(a['placed_bounds'], b['placed_bounds']),
                                 'note': 'bounding boxes intersect; actual mask intersection not computed'})
    if overlaps and policy == 'forbid':
        raise ValueError(f'overlap forbidden by policy: {overlaps[0]["volumes"]}')

    seams = []
    for s in doc.get('seams', []):
        if s.get('evidence_state') not in SEAM_STATES:
            raise ValueError(f'seam evidence_state must be one of {sorted(SEAM_STATES)}')
        if set(s.get('volumes', [])) - seen: raise ValueError('seam references an unplaced volume')
        if len(set(s.get('volumes', []))) != 2: raise ValueError('a seam joins exactly two placed volumes')
        if s['evidence_state'] == 'OBSERVED_COMPATIBLE_PENDING_BUILD' and not s.get('supporting_profiles'):
            raise ValueError('a compatibility claim requires supporting boundary profile ids')
        seams.append(dict(s))

    result = {'schema': SCHEMA, 'overlap_policy': policy, 'placements': resolved,
              'bounding_box_overlaps': overlaps, 'seams': seams,
              'authored_systems': doc.get('authored_systems', []),
              'layering': 'natural geography and authored systems are separate layers; '
                          'this document describes geography only',
              'builds_blocks': False,
              'unresolved': ['physical relocation of rotated or translated volumes',
                             'mask-level rather than bounding-box overlap',
                             'seam continuity, elevation matching and traversability',
                             'travel, opportunity and economic measurements after any geometry change',
                             'final map selection and acceptance'],
              'evidence_state': 'ANALYTICAL FIXTURE'}
    result['id'] = 'comp_' + digest(result)[:24]
    return result

def load_library(paths):
    library = {}
    for p in paths:
        for f in sorted(Path(p).glob('tv_*.json')):
            v = loads(f.read_text()); library[v['id']] = v
    return library

if __name__ == '__main__':
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('--composition', type=Path, required=True)
    p.add_argument('--library', type=Path, action='append', required=True)
    p.add_argument('--output', type=Path, required=True)
    a = p.parse_args()
    r = validate_composition(json.loads(a.composition.read_text()), load_library(a.library))
    json_write(a.output, r)
    print(r['id'], len(r['placements']), 'placements,', len(r['bounding_box_overlaps']), 'bounding-box overlaps,',
          len(r['seams']), 'seam records; no blocks placed')
