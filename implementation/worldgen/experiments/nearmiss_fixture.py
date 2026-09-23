"""Explicit-input experimental fixture driver; delegates all authoring rules."""
import argparse
import hashlib
import json
import shutil
from pathlib import Path
from terrain_harvest.reauthor import run, check_fountains, check_routes
from terrain_harvest.map_diff import export, fingerprint
from terrain_harvest.verify_portfolio import world_reader, check_portfolio, check_routes as readback_routes
from terrain_harvest.build_structures import TEMPLATES


def write(path, data):
    path.write_text(json.dumps(data, indent=2, sort_keys=True) + '\n')


def main():
    p = argparse.ArgumentParser(description=__doc__)
    for key in ('source', 'source-world', 'candidate', 'siting', 'frontier', 'opportunity', 'output', 'report'):
        p.add_argument('--' + key, type=Path, required=True)
    a = p.parse_args()
    for key, value in vars(a).items():
        setattr(a, key, value.resolve())
    a.output.mkdir(parents=True, exist_ok=True)
    a.report.mkdir(parents=True, exist_ok=True)
    base, world = a.output/'base-terrain', a.output/'authored'
    if base.exists() or world.exists():
        raise SystemExit('Use a fresh output directory; refusing to replace a fixture')
    candidate = json.loads(a.candidate.read_text())
    siting = json.loads(a.siting.read_text())
    volume = json.loads((a.source/'terrain_volume.json').read_text())
    assert candidate['seed'] == volume['provenance']['source_seed'] == 930010639
    before = fingerprint(a.source)
    shutil.copytree(a.source, base)
    from serialization.nbt import load_gzip, plain
    level = a.source_world/'level.dat'
    assert plain(load_gzip(level)[1])['Data']['WorldGenSettings']['seed'] == candidate['seed']
    shutil.copyfile(level, base/'level.dat')
    shutil.copytree(base, world)
    portfolio, routes, structures = [a.report/f'{n}.json' for n in ('portfolio', 'routes', 'structures')]
    run('terrain_harvest.author_portfolio', '--frontier', str(a.frontier),
        '--opportunity', str(a.opportunity), '--profile', 'resource_light', '--best',
        '--world', str(world), '--report', str(portfolio), '--apply')
    port = json.loads(portfolio.read_text())
    assert port['complete'] and port['placed'] == 28 and not port['skipped']
    run('terrain_harvest.routes', '--candidate', str(a.candidate), '--frontier', str(a.frontier),
        '--profile', 'resource_light', '--world', str(world), '--report', str(routes),
        '--placements', str(portfolio), '--apply')
    run('terrain_harvest.build_structures', '--world', str(world), '--sites', str(a.siting),
        '--report', str(structures))
    # Same quartz-top check as reauthor; choose a standable plinth column from
    # the selected fountain template, not Alpha's hard-coded spawn coordinates.
    homes = {}
    for team, layers in siting['teams'].items():
        x, y, z = layers['aether_fountain']['candidates'][0]['world_xyz']
        homes[team] = {'candidate_center_xz': candidate['homelands'][team]['raw_world'],
                       'fountain_xyz': [x, y, z], 'spawn_xzy': [x + 2, z, y + 4]}
    failures = check_fountains(world, {t: h['spawn_xzy'][:2] for t, h in homes.items()})
    failures += check_routes(routes)
    at, top = world_reader(world)
    for team, h in homes.items():
        x, z, y = h['spawn_xzy']
        if any(at(x, y + dy, z) != 'minecraft:air' for dy in (0, 1)):
            failures.append(f'{team}: spawn lacks two-block headroom')
    route_data = json.loads(routes.read_text())
    physical = {**port, 'placements': [v for v in port['placements'] if v['blocks'] > 0]}
    checks, readback_failures = check_portfolio(at, physical,
        {tuple(c['world_xz']) for c in route_data.get('crossings', [])})
    n, problems = readback_routes(at, top, route_data)
    checks += n
    readback_failures += problems
    structure_checks = []
    for st in json.loads(structures.read_text())['structures']:
        x, y, z = st['world_xyz']
        bad = []
        for (dx, dy, dz), state in TEMPLATES[st['structure']]().items():
            got = at(x+dx, y+dy, z+dz)
            if got != state[0]:
                bad.append([x+dx, y+dy, z+dz, state[0], got])
        structure_checks.append({'team': st['team'], 'structure': st['structure'], 'mismatches': bad})
    readback = {'physical_portfolio_checks': checks, 'physical_failures': readback_failures,
                'structures': structure_checks,
                'not_block_authored': [v for v in port['placements'] if not v['blocks']]}
    write(a.report/'physical-readback.json', readback)
    result = {'seed': candidate['seed'], 'volume_id': volume['id'], 'profile': 'resource_light',
              'homelands': homes, 'failures': failures, 'readback_failures': readback_failures,
              'ready_for_playtest': not failures and not readback_failures and not any(st['mismatches'] for st in structure_checks), 'placed': port['placed'],
              'finalist_rank': port['finalist_rank'], 'inputs': {'source_level': {'path': str(level), 'sha256': hashlib.sha256(level.read_bytes()).hexdigest()}},
              'source_fingerprint': before, 'base_fingerprint': fingerprint(base)}
    for key in ('candidate', 'siting', 'frontier', 'opportunity'):
        f = getattr(a, key)
        result['inputs'][key] = {'path': str(f), 'sha256': hashlib.sha256(f.read_bytes()).hexdigest()}
    assert before == fingerprint(a.source) == fingerprint(base)
    write(a.report/'build.json', result)
    if failures:
        raise SystemExit('Existing verification failed: ' + repr(failures))
    payload, stats = export(base, world, 'resource_light', a.report/'resource_light.json.gz',
                            '930010639 experimental fixture; no balance inference')
    result['diff'] = {k: v for k, v in payload.items() if k not in ('entries', 'palette')}
    result['diff_stats'] = stats
    write(a.report/'build.json', result)
    print(json.dumps(result, indent=2))
    if not result['ready_for_playtest']:
        raise SystemExit('Diagnostic diff exported; fixture readback failed. Do not playtest.')


if __name__ == '__main__':
    main()
