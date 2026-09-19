"""Completion receipts, verified reuse, deterministic packaging and installation.

A materialized volume is expensive and a half-written one is dangerous, so a
volume counts as complete only when its receipt exists. The receipt is written
last and atomically, after every other file, and it pins the exporter version,
the volume manifest, the source files consumed and the output files produced.
Reuse is refused, with a named reason, whenever any of those no longer match.
"""
from __future__ import annotations
import argparse
import json
import os
import shutil
import zipfile
from pathlib import Path
from .materialize import json_write, sha
from .model import dumps, loads

# Bump whenever export semantics change; older receipts then stop being reusable.
EXPORTER_VERSION = 'terrain_harvest_exporter/1'
RECEIPT = 'RECEIPT.json'

def output_hashes(dest):
    return {str(p.relative_to(dest)): sha(p) for p in sorted(dest.rglob('*'))
            if p.is_file() and p.name != RECEIPT}

def write_receipt(dest, volume, record, source):
    """Written last and atomically: a crash leaves no receipt, never a false one."""
    receipt = {'schema': 'terrain_volume_receipt/1', 'exporter_version': EXPORTER_VERSION,
               'volume_id': volume['id'], 'volume_manifest_sha256': sha(dest/'terrain_volume.json'),
               'source_level_sha256': record['source_level_sha256'],
               'source_region_sha256': record['source_region_sha256'],
               'output_sha256': output_hashes(dest)}
    tmp = dest/(RECEIPT + '.tmp')
    tmp.write_text(json.dumps(receipt, sort_keys=True, indent=2) + '\n')
    os.replace(tmp, dest/RECEIPT)
    return receipt

def reuse_refusal(dest, volume, source):
    """Return a human-readable reason this volume may not be reused, or None."""
    path = dest/RECEIPT
    if not path.exists(): return 'no receipt; build was never completed'
    try: receipt = json.loads(path.read_text())
    except json.JSONDecodeError: return 'receipt is not valid JSON'
    if receipt.get('exporter_version') != EXPORTER_VERSION:
        return f"exporter version changed: {receipt.get('exporter_version')} -> {EXPORTER_VERSION}"
    if receipt.get('volume_id') != volume['id']: return 'receipt is for a different volume'
    manifest = dest/'terrain_volume.json'
    if not manifest.exists(): return 'volume manifest missing'
    if sha(manifest) != receipt.get('volume_manifest_sha256'): return 'volume manifest changed since export'
    try:
        if dumps(loads(manifest.read_text())) != manifest.read_text(): return 'volume manifest is not canonical'
    except ValueError as e: return f'volume manifest invalid: {e}'
    if sha(source/'level.dat') != receipt.get('source_level_sha256'): return 'source level.dat changed since export'
    for name, digest in receipt.get('source_region_sha256', {}).items():
        f = source/name
        if not f.exists(): return f'source file missing: {name}'
        if sha(f) != digest: return f'source file changed since export: {name}'
    current = output_hashes(dest)
    expected = receipt.get('output_sha256', {})
    if current != expected:
        missing = sorted(set(expected) - set(current)); extra = sorted(set(current) - set(expected))
        changed = sorted(k for k in set(current) & set(expected) if current[k] != expected[k])
        return f'output differs from receipt (missing {missing[:3]}, extra {extra[:3]}, changed {changed[:3]})'
    return None

def package(gallery, archive):
    """Deterministic archive: sorted names, fixed timestamps, fixed permissions."""
    if archive.exists(): raise FileExistsError(archive)
    archive.parent.mkdir(parents=True, exist_ok=True)
    files = sorted(p for p in gallery.rglob('*') if p.is_file())
    with zipfile.ZipFile(archive, 'w', zipfile.ZIP_DEFLATED, compresslevel=9) as z:
        for p in files:
            info = zipfile.ZipInfo(str(p.relative_to(gallery)), date_time=(1980, 1, 1, 0, 0, 0))
            info.external_attr = 0o644 << 16
            info.compress_type = zipfile.ZIP_DEFLATED
            z.writestr(info, p.read_bytes())
    return {'archive': str(archive), 'files': len(files), 'sha256': sha(archive)}

def install(gallery, saves, name='Terrain Harvest Gallery'):
    """Copy to a mutable save directory. The artifact stays the immutable record."""
    if not (gallery/'gallery.json').exists(): raise ValueError('incomplete gallery; refusing to install')
    dest = saves/name
    if dest.exists(): raise FileExistsError(dest)
    shutil.copytree(gallery, dest)
    # The installed copy is meant to be played and changed; the artifact is not.
    (dest/'INSTALLED.txt').write_text(
        'Mutable inspection copy. Opening this in Minecraft will modify it.\n'
        f'Immutable source artifact: {gallery}\n')
    return {'installed': str(dest), 'source': str(gallery)}

def audit_gallery(gallery, sources, output):
    manifest = json.loads((gallery/'gallery.json').read_text())
    rows = []
    for record in manifest['volumes']:
        dest = gallery/'dimensions/harvest'/record['volume_id']
        volume = loads((dest/'terrain_volume.json').read_text())
        source = sources[volume['provenance']['source_seed']]
        reason = reuse_refusal(dest, volume, source)
        rows.append({'volume_id': volume['id'], 'reusable': reason is None, 'refusal_reason': reason})
    result = {'schema': 'terrain_lifecycle_audit/1', 'exporter_version': EXPORTER_VERSION,
              'volumes': rows, 'pass': all(r['reusable'] for r in rows) and bool(rows)}
    json_write(output, result)
    return result

if __name__ == '__main__':
    p = argparse.ArgumentParser(description=__doc__)
    sub = p.add_subparsers(dest='command', required=True)
    a = sub.add_parser('audit'); a.add_argument('--gallery', type=Path, required=True)
    a.add_argument('--source', action='append', required=True, metavar='SEED=WORLD')
    a.add_argument('--output', type=Path, required=True)
    k = sub.add_parser('package'); k.add_argument('--gallery', type=Path, required=True); k.add_argument('--archive', type=Path, required=True)
    i = sub.add_parser('install'); i.add_argument('--gallery', type=Path, required=True); i.add_argument('--saves', type=Path, required=True)
    args = p.parse_args()
    if args.command == 'audit':
        sources = {int(s.split('=', 1)[0]): Path(s.split('=', 1)[1]) for s in args.source}
        r = audit_gallery(args.gallery.resolve(), sources, args.output.resolve())
        for row in r['volumes']: print(row['volume_id'], 'REUSABLE' if row['reusable'] else 'REFUSED: ' + row['refusal_reason'])
        raise SystemExit(0 if r['pass'] else 1)
    if args.command == 'package': print(json.dumps(package(args.gallery.resolve(), args.archive.resolve()), indent=2))
    else: print(json.dumps(install(args.gallery.resolve(), args.saves.resolve()), indent=2))
