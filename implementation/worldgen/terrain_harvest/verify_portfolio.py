import json, sys
from pathlib import Path
sys.path.insert(0, '.')
from serialization.nbt import plain
from serialization.region import read_region
from vanilla_search.extract import VanillaChunk

CROP = {'wheat': 'minecraft:wheat', 'carrot': 'minecraft:carrots',
        'potato': 'minecraft:potatoes', 'beetroot': 'minecraft:beetroots'}

tok = tf = 0
for profile in ('balanced_baseline', 'exploration_centric',
                'consolidative', 'resource_light'):
    world = Path(f'/tmp/profile-worlds/{profile}')
    rep = json.load(open(f'/tmp/portfolio-{profile}.json'))
    chunks = {}
    for f in sorted((world / 'region').glob('*.mca')):
        for cx, cz, _, root in read_region(f):
            chunks[(cx, cz)] = VanillaChunk(plain(root))

    def at(x, y, z):
        """Look the chunk up per coordinate; a footprint crosses chunk edges."""
        c = chunks.get((x >> 4, z >> 4))
        return c.block(x, y, z) if c else None

    ok = fail = 0
    seen = {}
    for p in rep['placements']:
        x, y, z = p['world_xyz']; k = p['kind']
        if (x, z) in seen:
            print(f'COLLISION {profile}: {k} and {seen[(x, z)]} at {x},{z}'); fail += 1
        seen[(x, z)] = k
        if k == 'founder_crop':
            checks = [(at(x, y - 1, z), 'minecraft:water'),
                      (at(x + 2, y - 1, z), 'minecraft:farmland'),
                      (at(x + 2, y, z), CROP[p['detail']]),
                      (at(x, y - 1, z + 3), 'minecraft:farmland')]
        elif k == 'renewable_range':
            checks = [(at(x, y - 1, z), 'minecraft:grass_block'),
                      (at(x + 20, y, z + 20), 'minecraft:oak_fence' if
                       p['detail'] in ('sheep', 'cow', 'pig') else None)]
            checks = [c for c in checks if c[1] is not None]
        elif k == 'mining_worksite':
            checks = [(at(x, y - 3, z), 'minecraft:air'),
                      (at(x, y, z + 3), 'minecraft:polished_deepslate')]
        elif k == 'poi':
            checks = [(at(x, y - 1, z), 'minecraft:dirt'),
                      (at(x + 24, y + 6, z + 24), 'minecraft:sea_lantern')]
        elif k == 'route_target':
            checks = [(at(x, y, z), 'minecraft:chiseled_stone_bricks'),
                      (at(x, y + 3, z), 'minecraft:lantern')]
        else:
            continue
        for got, want in checks:
            if got == want:
                ok += 1
            else:
                print(f'MISMATCH {profile} {k} {p["detail"]} @{x},{y},{z}: '
                      f'got {got} want {want}')
                fail += 1
    print(f'{profile:22s} {len(rep["placements"]):3d} placements  {ok:3d} ok  {fail} failed')
    tok += ok; tf += fail
print(f'\nTOTAL {tok} assertions, {tf} failures')
sys.exit(1 if tf else 0)
