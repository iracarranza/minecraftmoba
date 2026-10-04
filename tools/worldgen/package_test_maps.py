"""Create creative inspection ZIPs and terrain overviews from a test batch."""
import argparse
import html
import json
from pathlib import Path
import shutil
import sys

REPO = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(REPO / 'implementation/worldgen'))
from serialization.nbt import (byte, compound, dump_gzip, float_tag, int_array,
                               integer, load_gzip, long, string)
from successor.grid import unrle
from terrain_harvest import foundry
from terrain_harvest.column_scan import World
from terrain_harvest.gallery import nbt_json
from vanilla_search.evaluate import orient


def overview(candidate, manifest, output):
    grid = candidate['feature_grid']
    w, h = grid['width'], grid['height']
    heights = unrle(grid['height_rle'])
    water = unrle(grid['actual_surface_water_rle'])
    forest = unrle(grid['forest_mask_rle'])
    bounds = candidate['region']['block_bounds']
    s = grid['sample_spacing_blocks']
    raw = [[(x, z) for x in range(bounds[0]+s//2, bounds[1]+1, s)]
           for z in range(bounds[2]+s//2, bounds[3]+1, s)]
    o = candidate['orientation']
    coords = sum(orient(raw, o['rotation_degrees_clockwise'],
                        o['east_west_reflected']), [])
    shapes = []
    low, high = min(heights), max(heights)
    for i, height in enumerate(heights):
        t = (height - low) / max(1, high - low)
        color = '#3e8eb5' if water[i] else (
            f'rgb({int(42+80*t)},{int(93+75*t)},{int(58+55*t)})' if forest[i]
            else f'rgb({int(120+100*t)},{int(143+76*t)},{int(91+115*t)})')
        shapes.append(f'<rect x="{i%w}" y="{i//w}" width="1" height="1" fill="{color}"/>')

    def mark(x, z, label, color):
        i = min(range(len(coords)), key=lambda i:
                (coords[i][0]-x)**2+(coords[i][1]-z)**2)
        px, py = i % w + .5, i // w + .5
        shapes.append(f'<circle cx="{px}" cy="{py}" r="1.8" fill="{color}" stroke="#111" stroke-width=".4"/>')
        shapes.append(f'<text x="{px+2}" y="{py-2}" font-size="3.4" fill="white" stroke="#111" stroke-width=".25" paint-order="stroke">{html.escape(label)}</text>')

    bindings = manifest['runtime_bindings']
    for team, xyz in bindings['fountains'].items():
        mark(xyz[0], xyz[2], f'{team} fountain', '#faf3a5')
    for team, objectives in bindings['objectives'].items():
        for name, xz in objectives.items():
            mark(xz[0], xz[1], f'{team} {name}', '#ed916d')
    lair = bindings['lair']['centre_xz']
    mark(*lair, 'Lair', '#d879ef')
    output.write_text(f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 {w} {h}" width="{w*6}" height="{h*6}">' + ''.join(shapes) + '</svg>')


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('reports', type=Path)
    parser.add_argument('artifacts', type=Path)
    args = parser.parse_args()
    report = json.loads((args.reports / 'summary.json').read_text())
    package_index = args.reports / 'packages.json'
    existing = {p['map_id']: p for p in json.loads(package_index.read_text())} if package_index.exists() else {}
    packages = []
    for manifest in report['published']:
        ident = manifest['map_id']
        if ident in existing:
            packages.append(existing[ident])
            continue
        source = args.artifacts / 'test-pool' / ident / 'world'
        assert foundry._fingerprint(source) == manifest['world_fingerprint']
        reader = World(source)
        fountains = {}
        for team, (x, y, z) in manifest['runtime_bindings']['fountains'].items():
            observed = {'plinth': reader.block(x, y, z),
                        'source': reader.block(x, y+4, z)}
            assert observed == {'plinth': 'minecraft:chiseled_quartz_block',
                                'source': 'minecraft:water'}, observed
            fountains[team] = observed
        name = f'MOBA_Test_{ident}'
        destination = args.artifacts / 'inspection-worlds' / name
        shutil.copytree(source, destination)
        root_name, root = load_gzip(destination / 'level.dat')
        data = root.value['Data'].value
        x, y, z = manifest['runtime_bindings']['fountains']['north']
        spawn = [x, y+6, z]
        data['LevelName'] = string(name)
        data['GameType'] = integer(1)
        data['allowCommands'] = byte(1)
        data['spawn'] = compound(pos=int_array(spawn), pitch=float_tag(0),
                                  yaw=float_tag(0), dimension=string('minecraft:overworld'))
        data['DayTime'] = long(6000)
        data.pop('Player', None)
        settings = data['WorldGenSettings'].value
        settings['generate_features'] = byte(0)
        settings['dimensions'].value['minecraft:overworld'].value['generator'] = nbt_json({
            'type': 'minecraft:flat', 'settings': {'biome': 'minecraft:the_void',
            'layers': [], 'features': False, 'lakes': False, 'structure_overrides': []}})
        dump_gzip(destination / 'level.dat', root_name, root)
        inspection = {'design_status': 'Prototype/test', 'map_id': ident,
                      'spawn': spawn, 'source_pool_fingerprint_verified': True,
                      'fountain_readback': fountains,
                      'outside_saved_chunks': 'void; structure generation disabled',
                      'runtime_bindings': manifest['runtime_bindings'],
                      'provenance': manifest['provenance'],
                      'caveat': 'Creative inspection world. A MOBA match requires the plugin; competitive balance is untested.'}
        (destination / 'INSPECTION.json').write_text(json.dumps(inspection, indent=2))
        (destination / 'README.txt').write_text(
            f'Minecraft Java 1.21.11 | Prototype/test\n{name}\n'
            f'Extract this folder into Minecraft saves. Creative mode, commands enabled.\n'
            f'North fountain spawn: {spawn}\n'
            'Outside saved terrain is void. Gameplay needs the MOBA plugin.\n'
            'See INSPECTION.json for teleport coordinates and provenance.\n')
        attempt = next(a for a in report['attempts'] if a.get('ready') and
                       a['seed'] == manifest['provenance']['seed'] and
                       foundry.map_id(a['seed'], json.loads((args.reports /
                       'compilations' / f"{a['seed']}_{a['index']}.json").read_text())['runs'][0]) == ident)
        candidate = json.loads(Path(attempt['candidate']).read_text())
        picture = args.reports / f'{ident}-overview.svg'
        overview(candidate, manifest, picture)
        archives = args.artifacts / 'packages'
        archives.mkdir(parents=True, exist_ok=True)
        archive = shutil.make_archive(str(archives / name), 'zip',
                                     root_dir=destination.parent, base_dir=name)
        packages.append({'map_id': ident, 'seed': manifest['provenance']['seed'],
                         'types': attempt['types'], 'zip': str(Path(archive).resolve()),
                         'world': str(destination.resolve()),
                         'overview': str(picture.resolve()), 'spawn': spawn,
                         'lair_access_asymmetry': manifest['runtime_bindings']['lair']['access_asymmetry'],
                         'renewable_sources': manifest['runtime_bindings']['renewables']['source_count']})
        print(f'packaged {ident}', flush=True)
    (args.reports / 'packages.json').write_text(json.dumps(packages, indent=2))


if __name__ == '__main__':
    main()
