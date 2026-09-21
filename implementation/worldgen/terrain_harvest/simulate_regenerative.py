"""Where each regenerative opportunity could actually manifest, on the frozen map.

Answers one question that has to be answerable before any re-freeze decision and
that a running server cannot answer about a world it has not loaded:

    Given the Alpha template as it stands, and the current Alpha eligibility
    fixtures, where could each opportunity manifest right now?

It also measures a claim the migration report currently only asserts -- that the
legacy pens and fields BIAS the query back onto themselves, because a flattened
grass pad is exactly what the predicate likes. That is reported per region as a
pad share rather than argued.

Exports, per opportunity: the region, every eligible locus, a simulated sequence
of manifestations, and a PNG of the region so the result can be looked at rather
than read.

Nothing here decides anything. The predicates are Alpha fixtures and this
reports what they currently imply.
"""
from __future__ import annotations

import argparse
import json
import re
import struct
import zlib
from pathlib import Path

from .regenerative import Region, Terrain, eligible, generations, rules_from_config
from .verify_portfolio import world_reader

CONFIG = Path(__file__).resolve().parents[2] / 'plugin' / 'src' / 'main' / 'resources' / 'config.yml'

# Legacy authored footprints, so their influence can be measured rather than assumed.
PEN_SPAN, FIELD_SPAN = 40, 24


class TemplateTerrain(Terrain):
    """The frozen template, read from its region files."""

    def __init__(self, world: Path):
        self._at, self._top = world_reader(world)

    def surface(self, x, z):
        return self._top(x, z)

    def block(self, x, y, z):
        return self._at(x, y, z) or 'minecraft:air'

    def player_placed(self, x, y, z):
        # The template is pristine: no match has been played on it, so nothing
        # in it is player-placed. Provenance only exists at runtime, which is
        # exactly why this export cannot model Development displacing a
        # manifestation -- it can only show the untouched map.
        return False


def sources_from_config(text: str) -> list[dict]:
    """The authored opportunities, parsed out of the plugin's own config."""
    body = text[text.index('\n  sources:'):]
    out, current = [], None
    for line in body.splitlines()[1:]:
        if line.strip().startswith('#') or not line.strip():
            continue
        indent = len(line) - len(line.lstrip())
        if indent <= 2 and line.strip().endswith(':') is False and ':' in line:
            break
        m = re.match(r'^\s{4}([A-Za-z0-9_]+):\s*$', line)
        if m:
            current = {'id': m.group(1)}
            out.append(current)
            continue
        m = re.match(r'^\s{6}([A-Za-z0-9_]+):\s*(.+?)\s*$', line)
        if m and current is not None:
            value = m.group(2)
            current[m.group(1)] = int(value) if re.fullmatch(r'-?\d+', value) else value
    return [s for s in out if 'x' in s]


def pad_span(kind_type: str) -> int:
    return FIELD_SPAN if kind_type == 'CROP' else PEN_SPAN


def simulate(world: Path, out_dir: Path, sequence: int, seed: int):
    text = CONFIG.read_text()
    rules = rules_from_config(CONFIG)
    half = int(re.search(r'migratedRegionHalfSpan:\s*(\d+)', text).group(1))
    terrain = TemplateTerrain(world)

    out_dir.mkdir(parents=True, exist_ok=True)
    report = {'schema': 'regenerative_simulation/1',
              'world': str(world),
              'note': 'Eligibility predicates are NON-CANON ALPHA FIXTURES. This reports '
                      'what they currently imply about the frozen template, not what the '
                      'design has settled. The template is pristine, so player-placed '
                      'rejection is not exercised.',
              'rules': {'headroom': rules.headroom, 'sampleStride': rules.sample_stride,
                        'minDisplacement': rules.min_displacement,
                        'playerExclusion': rules.player_exclusion,
                        'naturalGround': sorted(rules.ground),
                        'migratedRegionHalfSpan': half},
              'opportunities': []}

    for source in sources_from_config(text):
        region = Region.square(source['x'], source['z'], half)
        loci, chosen = generations(region, terrain, rules, sequence, seed)
        span = pad_span(source.get('type', 'ANIMAL'))
        on_pad = [l for l in loci
                  if abs(l[0] - source['x']) <= span // 2 and abs(l[2] - source['z']) <= span // 2]
        entry = {
            'id': source['id'],
            'type': source.get('type'),
            'kind': source.get('kind'),
            'authoredPoint': [source['x'], source['y'], source['z']],
            'region': {'cells': [{'minX': c[0], 'minZ': c[1], 'maxX': c[2], 'maxZ': c[3]}
                                 for c in region.cells], 'columns': region.area()},
            'sampledColumns': len(list(region.columns(rules.sample_stride))),
            'eligibleLoci': len(loci),
            'eligibleOnLegacyPad': len(on_pad),
            'padShare': round(len(on_pad) / len(loci), 4) if loci else None,
            'padShareIfUniform': round((span ** 2) / ((2 * half + 1) ** 2), 4),
            'manifestationSequence': [list(c) if c else None for c in chosen],
            'loci': [list(l) for l in loci],
        }
        report['opportunities'].append(entry)
        write_png(out_dir / f"{source['id']}.png", region, loci, chosen, source, span)

    (out_dir / 'regenerative-simulation.json').write_text(json.dumps(report, indent=1) + '\n')
    return report


def write_png(path: Path, region: Region, loci, chosen, source, span: int):
    """One pixel per column: ground, eligible, legacy pad, chosen manifestations."""
    a, b, c, d = region.cells[0]
    w, h = c - a + 1, d - b + 1
    px = [[(28, 30, 34)] * w for _ in range(h)]
    for x in range(a, c + 1):
        for z in range(b, d + 1):
            if abs(x - source['x']) <= span // 2 and abs(z - source['z']) <= span // 2:
                px[z - b][x - a] = (70, 55, 40)          # legacy authored footprint
    for x, _, z in loci:
        px[z - b][x - a] = (120, 220, 140)               # currently eligible
    for i, pick in enumerate([p for p in chosen if p]):
        for dx in range(-2, 3):
            for dz in range(-2, 3):
                ix, iz = pick[0] - a + dx, pick[2] - b + dz
                if 0 <= ix < w and 0 <= iz < h:
                    px[iz][ix] = (255, 210 - i * 40, 90) # successive manifestations
    raw = b''.join(b'\x00' + b''.join(bytes(p) for p in row) for row in px)

    def chunk(kind, data):
        return (struct.pack('>I', len(data)) + kind + data
                + struct.pack('>I', zlib.crc32(kind + data) & 0xFFFFFFFF))

    path.write_bytes(b'\x89PNG\r\n\x1a\n'
                     + chunk(b'IHDR', struct.pack('>IIBBBBB', w, h, 8, 2, 0, 0, 0))
                     + chunk(b'IDAT', zlib.compress(raw, 9))
                     + chunk(b'IEND', b''))


def main(argv=None):
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('--world', type=Path, required=True)
    p.add_argument('--output', type=Path, required=True)
    p.add_argument('--generations', type=int, default=4)
    p.add_argument('--seed', type=int, default=20260921)
    a = p.parse_args(argv)
    report = simulate(a.world.resolve(), a.output.resolve(), a.generations, a.seed)
    for o in report['opportunities']:
        print(f"{o['id']:<14} {o['type']:<6} eligible {o['eligibleLoci']:>5}"
              f"   on legacy pad {o['eligibleOnLegacyPad']:>4}"
              f"   pad share {o['padShare']} (uniform {o['padShareIfUniform']})")
    print(f"\nwrote {a.output}")


if __name__ == '__main__':
    main()
