"""Export a map configuration as a block diff against the base terrain.

The runtime model is: keep one base world, and make a match's map a set of
authored additions applied to a copy of it. The additions have to reach the
server somehow, and there are only three ways.

Porting the authoring to Java would mean two implementations of every structure
in two languages, which is the defect this project has now shipped four times.
Baking a whole world per configuration costs 28MB each and puts us back to
migrating binaries. So the authoring stays here, in Python, where the terrain
tooling lives, and what ships is its RESULT: every block this configuration
changes about the base.

That also makes verification a single check rather than a per-match search. A
diff is computed against one specific base, so the only question at load is
whether the server holds that base -- which a fingerprint answers -- instead of
re-verifying candidate placements against live terrain every time.

Diffing is done section by section. A 16x16x16 section whose stored block_states
are byte-identical cannot contain a change, so only sections the authoring
actually touched are ever expanded.
"""
from __future__ import annotations

import argparse
import gzip
import hashlib
import json
from pathlib import Path

from serialization.nbt import plain
from serialization.region import read_region
from vanilla_search.extract import VanillaChunk, _palette_value

SCHEMA = 'moba_map_diff/1'


def state_of(section, index):
    """Full block state including properties, as Minecraft's own text form."""
    entry = _palette_value(section['block_states'], index, 4)
    name = entry['Name']
    props = entry.get('Properties')
    if not props:
        return name
    inner = ','.join(f'{k}={v}' for k, v in sorted(props.items()))
    return f'{name}[{inner}]'


def chunks_of(world: Path) -> dict:
    out = {}
    for f in sorted((world / 'region').glob('*.mca')):
        for cx, cz, _, root in read_region(f):
            out[(cx, cz)] = VanillaChunk(plain(root))
    return out


def fingerprint(world: Path) -> str:
    """Identity of a base world: its region files, by content.

    Cheap to compute and exact. A diff built against one base must never be
    applied to another, and this is the whole of that check.
    """
    h = hashlib.sha256()
    for f in sorted((world / 'region').glob('*.mca')):
        h.update(f.name.encode())
        h.update(hashlib.sha256(f.read_bytes()).digest())
    return h.hexdigest()


def diff(base: Path, authored: Path):
    """Every block the authored world changes about the base."""
    b, a = chunks_of(base), chunks_of(authored)
    palette, index_of, entries = [], {}, []
    expanded = skipped = 0

    for key in sorted(set(a) & set(b)):
        bc, ac = b[key], a[key]
        cx, cz = key
        for sy, section in sorted(ac.sections.items()):
            other = bc.sections.get(sy)
            if 'block_states' not in section:
                continue
            # Identical stored states cannot differ block by block.
            if other is not None and other.get('block_states') == section.get('block_states'):
                skipped += 1
                continue
            expanded += 1
            for i in range(4096):
                new = state_of(section, i)
                old = ('minecraft:air' if other is None or 'block_states' not in other
                       else state_of(other, i))
                if new == old:
                    continue
                x = cx * 16 + (i & 15)
                z = cz * 16 + ((i >> 4) & 15)
                y = sy * 16 + (i >> 8)
                if new not in index_of:
                    index_of[new] = len(palette); palette.append(new)
                entries.extend((x, y, z, index_of[new]))

    return palette, entries, {'sections_expanded': expanded, 'sections_skipped': skipped}


def export(base: Path, authored: Path, name: str, out: Path, note: str = ''):
    palette, entries, stats = diff(base, authored)
    payload = {
        'schema': SCHEMA,
        'name': name,
        'note': note,
        'base_fingerprint': fingerprint(base),
        'base': base.name,
        'authored_from': authored.name,
        'palette': palette,
        'blocks': len(entries) // 4,
        # Flat x,y,z,paletteIndex quadruples: compact, and streamable on the
        # other side without building a million small objects.
        'entries': entries,
    }
    out.parent.mkdir(parents=True, exist_ok=True)
    with gzip.open(out, 'wt') as f:
        json.dump(payload, f)
    return payload, stats


def main(argv=None):
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('--base', type=Path, required=True)
    p.add_argument('--authored', type=Path, required=True)
    p.add_argument('--name', required=True)
    p.add_argument('--out', type=Path, required=True)
    p.add_argument('--note', default='')
    a = p.parse_args(argv)
    payload, stats = export(a.base.resolve(), a.authored.resolve(), a.name, a.out.resolve(), a.note)
    size = a.out.stat().st_size
    print(f"{a.name}: {payload['blocks']} blocks, {len(payload['palette'])} states, "
          f"{size / 1024:.0f} KB gzipped")
    print(f"  sections expanded {stats['sections_expanded']}, skipped {stats['sections_skipped']}")
    print(f"  base fingerprint {payload['base_fingerprint'][:16]}...")


if __name__ == '__main__':
    main()
