"""Independent exhaustive preservation audit of a materialized volume.

This does not call the exporter. Expected destination state is recomputed from
the source snapshot and the mask alone, so an exporter bug cannot excuse itself.
Coverage is reported literally: sections actually compared are counted, and
anything skipped for cost is named rather than implied to have passed.
"""
from __future__ import annotations
import argparse
import copy
import json
from collections import Counter
from pathlib import Path
from serialization.nbt import COMPOUND, LIST, plain
from serialization.region import read_region
from serialization.world import AIR, block
from vanilla_search.extract import _palette_value
from .model import Mask, loads
from .materialize import sha, json_write, state_tuple, entity_inside, read_entity_region

def fingerprint(t):
    """Typed canonical form; unlike plain() this keeps byte/int/list-kind distinctions."""
    if t.kind == COMPOUND: return (t.kind, tuple(sorted((k, fingerprint(v)) for k, v in t.value.items())))
    if t.kind == LIST: return (t.kind, t.list_kind, tuple(fingerprint(v) for v in t.value))
    if isinstance(t.value, (bytes, list, tuple)): return (t.kind, tuple(t.value))
    return (t.kind, t.value)

def without_light(section):
    s = copy.deepcopy(section)
    s.value.pop('SkyLight', None); s.value.pop('BlockLight', None)
    return s

def expected_state(container, mask, x, y, z, top):
    if mask.include_block(x, y, z):
        return state_tuple(_palette_value(container, (y & 15)*256 + (z & 15)*16 + (x & 15), 4)) if container else AIR
    if mask.envelope(x, y, z):
        return block('barrier') if y > top else block('bedrock')
    return AIR

def _sections(root):
    return {s.value['Y'].value: s for s in root.value['sections'].value} if root else {}

def audit_volume(volume, source, dest, max_clipped_sections=None):
    mask = Mask(volume); b = mask.bounds
    xmin, xmax = (b['x'][0]-1)//16, (b['x'][1]+1)//16
    zmin, zmax = (b['z'][0]-1)//16, (b['z'][1]+1)//16
    failures = []; cov = Counter()
    # Seed every reported key so an absent counter is never mistaken for full coverage.
    for key in ('chunks_in_range', 'chunks_audited', 'chunks_absent_ok', 'sections_retained',
                'sections_clipped_compared', 'sections_clipped_skipped', 'cells_compared', 'cell_mismatches'):
        cov[key] += 0
    src_be = {}; src_ticks = {'block_ticks': {}, 'fluid_ticks': {}}
    dst_be = {}; dst_ticks = {'block_ticks': {}, 'fluid_ticks': {}}

    for rz in range(zmin//32, zmax//32+1):
        for rx in range(xmin//32, xmax//32+1):
            sf = source/'region'/f'r.{rx}.{rz}.mca'
            df = dest/'region'/f'r.{rx}.{rz}.mca'
            sroots = {(cx, cz): r for cx, cz, _, r in read_region(sf) if xmin <= cx <= xmax and zmin <= cz <= zmax} if sf.exists() else {}
            droots = {(cx, cz): r for cx, cz, _, r in read_region(df)} if df.exists() else {}
            for cz in range(max(zmin, rz*32), min(zmax, rz*32+31)+1):
                for cx in range(max(xmin, rx*32), min(xmax, rx*32+31)+1):
                    cov['chunks_in_range'] += 1
                    sroot = sroots.get((cx, cz)); droot = droots.get((cx, cz))
                    if droot is None:
                        # Absence is only legitimate where nothing was retained and no shell is owed.
                        if any(mask.include_block(x, y, z) or mask.envelope(x, y, z)
                               for y in range(b['y'][0]-1, b['y'][1]+2)
                               for z in range(cz*16, cz*16+16) for x in range(cx*16, cx*16+16)):
                            failures.append({'kind': 'missing_destination_chunk', 'chunk': [cx, cz]})
                        cov['chunks_absent_ok'] += 1
                        continue
                    cov['chunks_audited'] += 1
                    starts = plain(droot).get('structures', {}).get('starts', {})
                    if starts: failures.append({'kind': 'structure_starts_present', 'chunk': [cx, cz]})
                    ssec, dsec = _sections(sroot), _sections(droot)
                    for sy, dpart in sorted(dsec.items()):
                        origin = (cx*16, sy*16, cz*16)
                        spart = ssec.get(sy)
                        if spart is not None and mask.section_inside(*origin):
                            cov['sections_retained'] += 1
                            if fingerprint(without_light(spart)) != fingerprint(dpart):
                                failures.append({'kind': 'retained_section_differs', 'chunk': [cx, cz], 'section_y': sy})
                            else:
                                cov['cells_compared'] += 4096
                            continue
                        if max_clipped_sections is not None and cov['sections_clipped_compared'] >= max_clipped_sections:
                            cov['sections_clipped_skipped'] += 1
                            continue
                        cov['sections_clipped_compared'] += 1
                        container = plain(spart).get('block_states') if spart is not None else None
                        dcontainer = plain(dpart).get('block_states')
                        for y in range(sy*16, sy*16+16):
                            for z in range(cz*16, cz*16+16):
                                for x in range(cx*16, cx*16+16):
                                    cov['cells_compared'] += 1
                                    want = expected_state(container, mask, x, y, z, b['y'][1])
                                    got = state_tuple(_palette_value(dcontainer, (y & 15)*256 + (z & 15)*16 + (x & 15), 4)) if dcontainer else AIR
                                    if want != got:
                                        if len(failures) < 200:
                                            failures.append({'kind': 'cell_differs', 'pos': [x, y, z], 'expected': list(want), 'actual': list(got)})
                                        cov['cell_mismatches'] += 1
                    for key in ('block_entities', 'block_ticks', 'fluid_ticks'):
                        for root, side in ((sroot, 'src'), (droot, 'dst')):
                            if root is None or key not in root.value: continue
                            for t in root.value[key].value:
                                d = t.value
                                if not all(a in d for a in ('x', 'y', 'z')): continue
                                pos = tuple(d[a].value for a in ('x', 'y', 'z'))
                                # Source side is filtered by the mask; destination is taken whole,
                                # so an out-of-mask leak in the destination still shows up.
                                if side == 'src' and not mask.include_block(*pos): continue
                                if key == 'block_entities': target = src_be if side == 'src' else dst_be
                                else: target = (src_ticks if side == 'src' else dst_ticks)[key]
                                target[pos] = fingerprint(t)

    for label, s, d in (('block_entities', src_be, dst_be),
                        ('block_ticks', src_ticks['block_ticks'], dst_ticks['block_ticks']),
                        ('fluid_ticks', src_ticks['fluid_ticks'], dst_ticks['fluid_ticks'])):
        for pos in sorted(set(s) | set(d)):
            if s.get(pos) != d.get(pos):
                failures.append({'kind': label + '_differs', 'pos': list(pos),
                                 'in_source_mask': pos in s, 'in_destination': pos in d})

    ent = Counter(); ent_fail = []
    src_ent = {}; dst_ent = {}
    for f in sorted((source/'entities').glob('*.mca')):
        _, rx, rz = f.stem.split('.')
        if not (xmin//32 <= int(rx) <= xmax//32 and zmin//32 <= int(rz) <= zmax//32): continue
        for cx, cz, _, r in read_entity_region(f):
            if not (xmin <= cx <= xmax and zmin <= cz <= zmax): continue
            for t in r.value['Entities'].value:
                ent['source_roots'] += 1
                if entity_inside(t, mask):
                    src_ent[fingerprint(t)] = src_ent.get(fingerprint(t), 0) + 1; ent['expected_kept'] += 1
                elif 'Pos' in t.value and not mask.include_block(*(int(p.value // 1) for p in t.value['Pos'].value)):
                    ent['excluded_root_outside_mask'] += 1
                else:
                    ent['excluded_passenger_outside_mask'] += 1
    for f in sorted((dest/'entities').glob('*.mca')):
        for cx, cz, _, r in read_entity_region(f):
            for t in r.value['Entities'].value:
                ent['destination_roots'] += 1
                dst_ent[fingerprint(t)] = dst_ent.get(fingerprint(t), 0) + 1
    if src_ent != dst_ent:
        ent_fail.append({'kind': 'entity_multiset_differs',
                         'only_in_source_mask': sum(v for k, v in src_ent.items() if dst_ent.get(k, 0) < v),
                         'only_in_destination': sum(v for k, v in dst_ent.items() if src_ent.get(k, 0) < v)})
    failures.extend(ent_fail)

    return {'schema': 'terrain_preservation_audit/1', 'volume_id': volume['id'],
            'evidence_state': 'DERIVED MEASUREMENT',
            'method': 'destination recomputed from source snapshot and mask independently of the exporter',
            'coverage': dict(cov), 'entities': dict(ent),
            'clipped_section_limit': max_clipped_sections,
            'exhaustive': cov['sections_clipped_skipped'] == 0,
            'failures': failures[:200], 'failure_count': len(failures),
            'not_covered': ['lighting recomputation', 'structure/POI behaviour after load',
                            'simulation behaviour once unfrozen', 'client rendering'],
            'pass': not failures}

def audit_gallery(gallery, sources, output, max_clipped_sections=None, only=None):
    if output.exists(): raise FileExistsError(output)
    manifest = json.loads((gallery/'gallery.json').read_text())
    results = []
    for record in manifest['volumes']:
        if only and record['volume_id'] not in only: continue
        dest = gallery/'dimensions/harvest'/record['volume_id']
        volume = loads((dest/'terrain_volume.json').read_text())
        source = sources[volume['provenance']['source_seed']]
        pinned = dict(record['source_region_sha256']); pinned['level.dat'] = record['source_level_sha256']
        if any(sha(source/f) != h for f, h in pinned.items()): raise ValueError('source differs from pinned materialization snapshot')
        results.append(audit_volume(volume, source, dest, max_clipped_sections))
        if any(sha(source/f) != h for f, h in pinned.items()): raise ValueError('source changed during audit')
    result = {'schema': 'terrain_preservation_audits/1', 'volumes': results,
              'pass': all(r['pass'] for r in results) and bool(results)}
    json_write(output, result)
    return result

if __name__ == '__main__':
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('--gallery', type=Path, required=True)
    p.add_argument('--source', action='append', required=True, metavar='SEED=WORLD')
    p.add_argument('--output', type=Path, required=True)
    p.add_argument('--max-clipped-sections', type=int, default=None)
    p.add_argument('--id', action='append', help='audit exact volume ID, repeatable')
    a = p.parse_args()
    sources = {int(s.split('=', 1)[0]): Path(s.split('=', 1)[1]) for s in a.source}
    r = audit_gallery(a.gallery, sources, a.output, a.max_clipped_sections, set(a.id) if a.id else None)
    for v in r['volumes']:
        print(v['volume_id'], 'PASS' if v['pass'] else f"FAIL({v['failure_count']})",
              'exhaustive' if v['exhaustive'] else 'partial', dict(v['coverage']))
    if not r['pass']: raise SystemExit(1)
