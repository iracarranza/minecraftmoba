"""Versioned reference-only volumes. Coordinates are inclusive integer block cells."""
from __future__ import annotations
import copy
import hashlib
import json
from fractions import Fraction

KINDS = {'whole_map', 'near_miss_map', 'large_section', 'local_section'}
STATES = {'RAW WORLD OBSERVATION', 'DERIVED MEASUREMENT', 'ANALYTICAL FIXTURE', 'INTERPRETATION', 'UNRESOLVED'}
RULE = 'integer_block_coordinates_inclusive_v1'

def canonical(value):
    return json.dumps(value, sort_keys=True, separators=(',', ':'), allow_nan=False)

def digest(value):
    return hashlib.sha256(canonical(value).encode()).hexdigest()

def identity(v):
    # Placement, classification and later measurements do not rename source terrain.
    return 'tv_' + digest({'schema': v['schema'], 'provenance': v['provenance'], 'boundary': v['boundary']})[:24]

def validate(v):
    if v['schema'] != 'terrain_volume/1': raise ValueError('unknown schema')
    p = v['provenance']
    for key in ('source_seed', 'source_dimension', 'source_bounds', 'minecraft_version', 'worldgen_settings', 'source_analysis_record'):
        if key not in p: raise ValueError('missing provenance: '+key)
    if type(p['source_seed']) is not int: raise ValueError('seed must be integer')
    b = p['source_bounds']
    for axis in ('x', 'y', 'z'):
        if len(b[axis]) != 2 or any(type(n) is not int for n in b[axis]) or b[axis][0] > b[axis][1]: raise ValueError('invalid bounds')
    if v['classification']['kind'] not in KINDS: raise ValueError('classification')
    if v['classification']['kind'] == 'near_miss_map' and not v['classification'].get('failed_or_unresolved_criteria'): raise ValueError('near miss requires actual criteria')
    t = v['transform']
    if t['rotation'] not in (0, 90, 180, 270) or len(t['translation']) != 3 or any(type(n) is not int for n in t['translation']): raise ValueError('transform')
    boundary = v['boundary']
    if boundary['inclusion_rule'] != RULE: raise ValueError('inclusion rule')
    if boundary['geometry_type'] not in ('box', 'ellipse', 'scoop'): raise ValueError('geometry')
    if boundary['geometry_type'] != 'box' and any(b[a][0] == b[a][1] for a in ('x', 'z')): raise ValueError('ellipse needs positive radii')
    if boundary['geometry_type'] == 'scoop':
        levels = boundary['parameters']['radius_scale_by_y']
        if len(levels) < 2 or levels[0][0] != b['y'][0] or levels[-1][0] != b['y'][1]: raise ValueError('scoop must span Y bounds')
        last_y = b['y'][0]-1; last_s = Fraction(0)
        for y, scale in levels:
            s = Fraction(str(scale))
            if type(y) is not int or y <= last_y or not last_s <= s <= 1 or s <= 0: raise ValueError('scoop requires ordered positive nondecreasing scales <= 1')
            last_y, last_s = y, s
    for m in v['measurements']:
        if m['evidence_state'] not in STATES: raise ValueError('evidence state')
    if v['id'] != identity(v): raise ValueError('identity mismatch')
    return v

def make_volume(provenance, kind, geometry='box', parameters=None, measurements=None, criteria=None, tags=None):
    v = {'schema':'terrain_volume/1', 'provenance':copy.deepcopy(provenance),
         'classification':{'kind':kind, 'geographic_tags':tags or [], 'failed_or_unresolved_criteria':criteria or []},
         'transform':{'rotation':0, 'translation':[0,0,0]},
         'boundary':{'geometry_type':geometry, 'parameters':parameters or {}, 'inclusion_rule':RULE},
         'measurements':measurements or [], 'connection_interfaces':[], 'materialization_status':'reference_only'}
    v['id'] = identity(v)
    return validate(v)

class Mask:
    def __init__(self, v):
        validate(v)
        self.bounds = v['provenance']['source_bounds']; self.kind = v['boundary']['geometry_type']
        self.scales = {}
        b = self.bounds
        for y in range(b['y'][0], b['y'][1]+1):
            s = Fraction(1)
            if self.kind == 'scoop':
                levels = v['boundary']['parameters']['radius_scale_by_y']
                for (ya,sa),(yb,sb) in zip(levels,levels[1:]):
                    if ya <= y <= yb:
                        s = Fraction(str(sa)) + (Fraction(str(sb))-Fraction(str(sa))) * Fraction(y-ya,yb-ya)
                        break
            self.scales[y] = s
        self.ax = b['x'][1]-b['x'][0]; self.az = b['z'][1]-b['z'][0]
        self.cx2 = sum(b['x']); self.cz2 = sum(b['z'])
    def include_block(self, x, y, z):
        b = self.bounds
        if not (b['x'][0]<=x<=b['x'][1] and b['y'][0]<=y<=b['y'][1] and b['z'][0]<=z<=b['z'][1]): return False
        if self.kind == 'box': return True
        # Exact rational comparison, with no platform-dependent edge rounding.
        s = self.scales[y]
        return (((2*x-self.cx2)*self.az)**2 + ((2*z-self.cz2)*self.ax)**2)*s.denominator**2 <= (self.ax*self.az*s.numerator)**2
    def envelope(self, x, y, z):
        # Chebyshev dilation seals face, edge and corner gaps, including floor/roof.
        b = self.bounds
        if not (b['x'][0]-1<=x<=b['x'][1]+1 and b['y'][0]-1<=y<=b['y'][1]+1 and b['z'][0]-1<=z<=b['z'][1]+1): return False
        if self.kind == 'box': return not self.include_block(x,y,z)
        return not self.include_block(x,y,z) and any(self.include_block(x+dx,y+dy,z+dz) for dx in (-1,0,1) for dy in (-1,0,1) for dz in (-1,0,1))
    def section_inside(self, x, y, z):
        return all(self.include_block(x+dx,y+dy,z+dz) for dx in (0,15) for dy in (0,15) for dz in (0,15))

def transform_point(v, point, inverse=False):
    x,y,z = point; dx,dy,dz = v['transform']['translation']; r = v['transform']['rotation']//90
    if inverse: x,y,z=x-dx,y-dy,z-dz; r=(-r)%4
    for _ in range(r): x,z=-z,x
    return (x,y,z) if inverse else (x+dx,y+dy,z+dz)

def loads(text): return validate(json.loads(text))
def dumps(v): validate(v); return json.dumps(v,sort_keys=True,indent=2,allow_nan=False)+'\n'
