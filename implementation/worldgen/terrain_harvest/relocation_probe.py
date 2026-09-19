"""Check rotated block states against a real Minecraft server, not just our tests.

Our rotation tables are a claim about what vanilla considers a legal, correctly
oriented state. This builds a fixture world of oriented blocks, rotates it a
quarter turn, loads both copies in the pinned server, and asks the server itself
whether each rotated cell holds the predicted state. A round trip that agrees
with our own code proves only that our code is self-consistent.
"""
from __future__ import annotations
import argparse
import hashlib
import json
import queue
import shutil
import subprocess
import tempfile
import threading
import time
from pathlib import Path
from serialization.nbt import (COMPOUND, INT, byte, compound, dump_gzip, int_array, integer,
                               list_tag, long, string, float_tag)
from serialization.region import write_region
from serialization.world import AIR, _block_states_tag, block
from .gallery import DIMENSION_TYPE, VOID, nbt_json
from .materialize import DATA_VERSION, empty_chunk, export_volume, json_write
from .model import make_volume
from .navigation_probe import offline_uuid
from .relocate import place_point, relocate_volume, rotate_state
from .server_probe import SERVER_SHA1

SEED = 4242
BOUNDS = {'x': [0, 31], 'y': [0, 15], 'z': [0, 31]}
TRANSLATION = (1000, 0, 0)

# One representative of every rotation rule the tables implement.
SPECIMENS = [
    block('oak_stairs', facing='north', half='bottom', shape='straight', waterlogged='false'),
    block('oak_stairs', facing='east', half='top', shape='inner_left', waterlogged='false'),
    block('oak_log', axis='x'),
    block('oak_log', axis='z'),
    block('oak_log', axis='y'),
    block('rail', shape='north_south'),
    block('rail', shape='ascending_north'),
    block('rail', shape='north_east'),
    block('furnace', facing='west', lit='false'),
    block('observer', facing='up', powered='false'),
    block('observer', facing='south', powered='false'),
    block('crafter', orientation='down_east', crafting='false', triggered='false'),
    block('chest', facing='south', type='single', waterlogged='false'),
    block('ladder', facing='north', waterlogged='false'),
    block('end_rod', facing='east'),
    block('hopper', facing='north', enabled='true'),
]

def specimen_positions():
    """One specimen per column, on a stone floor, spaced so none touch."""
    for i, state in enumerate(SPECIMENS):
        yield (2 + (i % 8) * 3, 2, 2 + (i // 8) * 3), state

def build_fixture(root):
    (root/'region').mkdir(parents=True, exist_ok=True)
    (root/'entities').mkdir(parents=True, exist_ok=True)
    dump_gzip(root/'level.dat', '', compound(Data=compound(
        DataVersion=integer(DATA_VERSION), WorldGenSettings=compound(seed=long(SEED)))))
    placed = dict(specimen_positions())
    dest = {}
    for cz in range(-1, 3):
        for cx in range(-1, 3):
            chunk = empty_chunk(cx, cz)
            states = []
            for y in range(0, 16):
                for z in range(cz*16, cz*16+16):
                    for x in range(cx*16, cx*16+16):
                        if y == 1: states.append(block('stone'))
                        elif (x, y, z) in placed: states.append(placed[(x, y, z)])
                        else: states.append(AIR)
            chunk.value['sections'] = list_tag(COMPOUND, [compound(Y=byte(0),
                block_states=_block_states_tag(states),
                biomes=compound(palette=list_tag(8, [string('minecraft:plains')])))])
            dest[(cx, cz)] = ('', chunk)
    write_region(root/'region'/'r.0.0.mca', {k: v for k, v in dest.items() if k[0] >= 0 and k[1] >= 0})
    write_region(root/'region'/'r.-1.-1.mca', {k: v for k, v in dest.items() if k[0] < 0 or k[1] < 0})
    return root

def state_string(state):
    name, props = state
    if not props: return name
    return name + '[' + ','.join(f'{k}={v}' for k, v in props) + ']'

def build_world(root, identity_dir, rotated_dir):
    world = root/'world'
    (world/'region').mkdir(parents=True, exist_ok=True)
    for src in (identity_dir, rotated_dir):
        for f in (src/'region').glob('*.mca'):
            target = world/'region'/f.name
            if target.exists(): raise RuntimeError('region collision between identity and rotated copies')
            shutil.copyfile(f, target)
    pack = world/'datapacks/relocation_probe'
    json_write(pack/'pack.mcmeta', {'pack': {'min_format': [94, 1], 'max_format': [94, 1],
                                             'description': 'relocation probe'}})
    json_write(pack/'data/harvest/dimension_type/inspection.json', DIMENSION_TYPE)
    for dim in ('overworld', 'the_nether', 'the_end'):
        json_write(pack/f'data/minecraft/dimension/{dim}.json', VOID)
    data = compound(
        DataVersion=integer(DATA_VERSION), LevelName=string('Relocation Probe'),
        GameType=integer(2), allowCommands=byte(1), Difficulty=byte(0),
        SpawnX=integer(0), SpawnY=integer(8), SpawnZ=integer(0), Time=long(0), DayTime=long(0),
        LastPlayed=long(0), version=integer(19133),
        Version=compound(Id=integer(DATA_VERSION), Name=string('1.21.11'), Snapshot=byte(0)),
        initialized=byte(1), raining=byte(0), thundering=byte(0),
        # The server logs an ERROR and falls back if this is absent, even though
        # the end dimension is a void here and no dragon can exist.
        DragonFight=compound(Gateways=list_tag(INT, []), DragonKilled=byte(1),
                             PreviouslyKilled=byte(1), NeedsStateScanning=byte(0)),
        spawn=compound(pos=int_array([0, 8, 0]), dimension=string('minecraft:overworld'),
                       yaw=float_tag(0), pitch=float_tag(0)),
        DataPacks=compound(Enabled=list_tag(8, [string('vanilla'), string('file/relocation_probe')]),
                           Disabled=list_tag(8, [])),
        WorldGenSettings=compound(seed=long(0), generate_features=byte(0), bonus_chest=byte(0),
            dimensions=compound(**{'minecraft:' + d: nbt_json(VOID) for d in ('overworld', 'the_nether', 'the_end')})),
        game_rules=compound(**{'minecraft:spawn_mobs': byte(0), 'minecraft:advance_time': byte(0),
                               'minecraft:advance_weather': byte(0), 'minecraft:random_tick_speed': integer(0)}))
    dump_gzip(world/'level.dat', '', compound(Data=data))
    return world

def probe(jar, java, eula, report, quarter=1):
    if hashlib.sha1(jar.read_bytes()).hexdigest() != SERVER_SHA1: raise ValueError('wrong server jar')
    if not any(l.strip() == 'eula=true' for l in eula.read_text().splitlines()):
        raise ValueError('existing accepted eula.txt required')

    expectations = []
    with tempfile.TemporaryDirectory(prefix='relocation-probe-') as tmp:
        root = Path(tmp)
        source = build_fixture(root/'source')
        volume = make_volume({'source_seed': SEED, 'source_dimension': 'minecraft:overworld',
            'source_bounds': BOUNDS, 'minecraft_version': '1.21.11', 'data_version': DATA_VERSION,
            'worldgen_settings': {'status': 'UNRESOLVED'},
            'source_analysis_record': {'path': 'relocation fixture', 'sha256': 'fixture'}},
            'local_section')
        identity = root/'identity'
        export_volume(volume, source, identity)
        rotated = root/'rotated'
        moved = relocate_volume(volume, source, rotated, quarter, TRANSLATION)

        for pos, state in specimen_positions():
            expectations.append({'kind': 'identity', 'pos': list(pos), 'state': state_string(state)})
            expectations.append({'kind': 'rotated',
                                 'pos': list(place_point(pos, quarter, TRANSLATION)),
                                 'state': state_string(rotate_state(state, quarter)),
                                 'from': list(pos), 'from_state': state_string(state)})

        world = build_world(root, identity, rotated)
        shutil.copyfile(eula, root/'eula.txt')
        (root/'server.properties').write_text(
            'level-name=world\nonline-mode=false\nserver-ip=127.0.0.1\nserver-port=25601\n'
            'view-distance=4\nsimulation-distance=4\nspawn-protection=0\nmax-tick-time=-1\n')
        (root/'ops.json').write_text(json.dumps([]))

        commands = ['tick freeze']
        for i, e in enumerate(expectations):
            x, y, z = e['pos']
            commands.append(f'forceload add {x} {z}')
            commands.append(f'execute if block {x} {y} {z} {e["state"]} run say RELOC_OK_{i}')
            e['marker'] = f'RELOC_OK_{i}'

        proc = subprocess.Popen([str(java), '-Xmx2G', '-jar', str(jar), '--nogui'], cwd=root,
            stdin=subprocess.PIPE, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True, bufsize=1)
        q = queue.Queue(); lines = []
        threading.Thread(target=lambda: [q.put(l) for l in proc.stdout], daemon=True).start()
        ready = False; deadline = time.monotonic() + 300
        try:
            while time.monotonic() < deadline and proc.poll() is None and not ready:
                try: line = q.get(timeout=.5); lines.append(line)
                except queue.Empty: continue
                if 'Done (' in line: ready = True
            if not ready: raise RuntimeError('server never became ready')
            proc.stdin.write('\n'.join(commands) + '\nsave-all flush\nstop\n'); proc.stdin.flush()
            try: proc.wait(timeout=180)
            except subprocess.TimeoutExpired: proc.kill()
            while not q.empty(): lines.append(q.get())
        finally:
            if proc.poll() is None: proc.kill(); proc.wait()

    text = ''.join(lines)
    for e in expectations: e['observed'] = e['marker'] in text
    errors = [l.strip() for l in lines if any(w in l for w in
              ('ERROR', 'Exception', 'Unknown block', 'Parsing error', 'Failed to load'))]
    rotated_checks = [e for e in expectations if e['kind'] == 'rotated']
    result = {'schema': 'terrain_relocation_probe/1', 'evidence_state': 'DERIVED MEASUREMENT',
              'quarter_turns': quarter, 'translation': list(TRANSLATION), 'moved': moved,
              'server_sha1': SERVER_SHA1, 'server_errors': errors,
              'method': 'the server is asked whether each rotated cell holds the predicted state, '
                        'so agreement is with vanilla rather than with our own rotation tables',
              'specimens': len(SPECIMENS), 'expectations': expectations,
              'identity_confirmed': sum(1 for e in expectations if e['kind'] == 'identity' and e['observed']),
              'rotated_confirmed': sum(1 for e in rotated_checks if e['observed']),
              'rotated_total': len(rotated_checks),
              'not_covered': ['block entities, scheduled ticks and entities under rotation',
                              'blocks outside the specimen list', 'rendering and human inspection',
                              'corpus relocation, which remains disabled in the exporter'],
              'pass': not errors and all(e['observed'] for e in expectations)}
    report.parent.mkdir(parents=True, exist_ok=True)
    report.with_suffix('.log').write_text(text)
    json_write(report, result)
    if not result['pass']: raise RuntimeError('relocation probe failed; see report')
    return result

if __name__ == '__main__':
    p = argparse.ArgumentParser(description=__doc__)
    for key in ('jar', 'java', 'eula', 'report'): p.add_argument('--' + key, type=Path, required=True)
    p.add_argument('--quarter', type=int, default=1)
    a = p.parse_args()
    r = probe(a.jar.resolve(), a.java.resolve(), a.eula.resolve(), a.report.resolve(), a.quarter)
    print(f"identity {r['identity_confirmed']}/{r['specimens']} | "
          f"rotated {r['rotated_confirmed']}/{r['rotated_total']} confirmed by the server")
