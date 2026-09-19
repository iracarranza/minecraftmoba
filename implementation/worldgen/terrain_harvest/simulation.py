"""Containment and drift under simulation, measured on a disposable gallery copy.

Two separate server sessions, each from a fresh copy: one with ticks frozen and
no player movement, one with ticks running and a bot walking into the envelope.
Each session is stopped before its world is read, so the comparison sees a
clean save rather than a live directory.

Frozen is expected to change nothing. Resumed is expected to change something;
the point is to measure what, and to prove the envelope still holds afterwards.
"""
from __future__ import annotations
import argparse
import hashlib
import json
import os
import queue
import shutil
import subprocess
import tempfile
import threading
import time
from pathlib import Path
from serialization.nbt import plain
from serialization.region import read_region
from serialization.world import AIR, block
from vanilla_search.extract import _palette_value
from .materialize import json_write, state_tuple
from .model import Mask, loads
from .navigation_probe import offline_uuid
from .preservation import fingerprint
from .server_probe import SERVER_SHA1

HERE = Path(__file__).resolve().parent

def _sections(root):
    return {s.value['Y'].value: s for s in root.value['sections'].value}

def _subset(a, b):
    """True when one property map only adds keys to the other, with no conflicts."""
    small, large = (a, b) if len(a) <= len(b) else (b, a)
    return all(large.get(k) == v for k, v in small.items())

def _section_is_air(section):
    """A section with no container, or a single-entry air palette, holds no blocks."""
    container = plain(section).get('block_states')
    if not container: return True
    palette = container.get('palette') or []
    return len(palette) == 1 and palette[0].get('Name') == 'minecraft:air'

def diff_regions(before, after, mask, label):
    """Cell-level diff, descending only into sections whose containers differ."""
    top = mask.bounds['y'][1]
    counts = {'sections_compared': 0, 'sections_differing': 0, 'cells_changed': 0,
              'in_mask_changed': 0, 'envelope_changed': 0, 'outside_changed': 0,
              'envelope_breached': 0, 'chunks_generated_empty': 0, 'chunks_generated_nonempty': 0,
              'chunks_lost': 0, 'state_normalized': 0}
    samples = []
    for f in sorted((before/'region').glob('*.mca')):
        g = after/'region'/f.name
        if not g.exists():
            counts['cells_changed'] += 1
            samples.append({'kind': 'region_file_missing_after', 'file': f.name})
            continue
        a = {(cx, cz): r for cx, cz, _, r in read_region(f)}
        b = {(cx, cz): r for cx, cz, _, r in read_region(g)}
        for key in sorted(set(a) | set(b)):
            if key not in a:
                # The void generator fills in chunks around a loaded player. An empty
                # one is expected; a generated chunk with blocks in it is not.
                empty = all(_section_is_air(s) for s in _sections(b[key]).values())
                counts['chunks_generated_empty' if empty else 'chunks_generated_nonempty'] += 1
                if not empty and len(samples) < 40:
                    samples.append({'kind': 'generated_chunk_with_blocks', 'chunk': list(key)})
                continue
            if key not in b:
                counts['chunks_lost'] += 1
                if len(samples) < 40: samples.append({'kind': 'chunk_lost', 'chunk': list(key)})
                continue
            asec, bsec = _sections(a[key]), _sections(b[key])
            for sy in sorted(set(asec) | set(bsec)):
                counts['sections_compared'] += 1
                pa, pb = asec.get(sy), bsec.get(sy)
                if pa is not None and pb is not None and fingerprint(pa) == fingerprint(pb): continue
                counts['sections_differing'] += 1
                ca = plain(pa).get('block_states') if pa is not None else None
                cb = plain(pb).get('block_states') if pb is not None else None
                cx, cz = key
                for y in range(sy*16, sy*16+16):
                    for z in range(cz*16, cz*16+16):
                        for x in range(cx*16, cx*16+16):
                            i = (y & 15)*256 + (z & 15)*16 + (x & 15)
                            sa = state_tuple(_palette_value(ca, i, 4)) if ca else AIR
                            sb = state_tuple(_palette_value(cb, i, 4)) if cb else AIR
                            if sa == sb: continue
                            # The server writes blocks with their full default
                            # property set on load. Same block, more properties
                            # spelled out, is normalization and not drift.
                            if sa[0] == sb[0] and _subset(dict(sa[1]), dict(sb[1])):
                                counts['state_normalized'] += 1
                                continue
                            counts['cells_changed'] += 1
                            if mask.include_block(x, y, z): bucket = 'in_mask_changed'
                            elif mask.envelope(x, y, z):
                                bucket = 'envelope_changed'
                                # Containment depends on which block is there,
                                # not on its property spelling.
                                want = 'minecraft:barrier' if y > top else 'minecraft:bedrock'
                                if sb[0] != want: counts['envelope_breached'] += 1
                            else: bucket = 'outside_changed'
                            counts[bucket] += 1
                            if len(samples) < 40:
                                samples.append({'pos': [x, y, z], 'bucket': bucket,
                                                'before': list(sa), 'after': list(sb)})
    return {'label': label, 'counts': counts, 'samples': samples}

def _server(root, jar, java, port, feed, settle=300):
    proc = subprocess.Popen([str(java), '-Xmx2G', '-jar', str(jar), '--nogui'], cwd=root,
        stdin=subprocess.PIPE, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True, bufsize=1)
    q = queue.Queue(); lines = []
    threading.Thread(target=lambda: [q.put(l) for l in proc.stdout], daemon=True).start()
    ready = False; deadline = time.monotonic() + settle
    while time.monotonic() < deadline and proc.poll() is None and not ready:
        try: line = q.get(timeout=.5); lines.append(line)
        except queue.Empty: continue
        if 'Done (' in line: ready = True
    if not ready:
        proc.kill(); raise RuntimeError('server never became ready')
    try:
        feed(proc, q, lines)
        proc.stdin.write('save-all flush\nstop\n'); proc.stdin.flush()
        try: proc.wait(timeout=120)
        except subprocess.TimeoutExpired: proc.kill()
    finally:
        if proc.poll() is None: proc.kill(); proc.wait()
    while not q.empty(): lines.append(q.get())
    return ''.join(lines)

def _prepare(world, root, eula, port, player):
    shutil.copytree(world, root/'world')
    shutil.copyfile(eula, root/'eula.txt')
    (root/'server.properties').write_text(
        f'level-name=world\nonline-mode=false\nserver-ip=127.0.0.1\nserver-port={port}\n'
        'view-distance=4\nsimulation-distance=4\nspawn-protection=0\nmax-tick-time=-1\n')
    (root/'ops.json').write_text(json.dumps([{'uuid': offline_uuid(player), 'name': player,
                                              'level': 4, 'bypassesPlayerLimit': True}]))

FORCELOAD_MAX_CHUNKS = 256

def _chunk_count(area):
    return ((area['x'][1]//16 - area['x'][0]//16) + 1) * ((area['z'][1]//16 - area['z'][0]//16) + 1)

def forceload_area(mask, target, radius):
    """Vanilla refuses more than 256 chunks per forceload, so a large volume gets
    a window that actually fits. The radius is shrunk until it does; asking for
    more silently loads nothing at all."""
    b = mask.bounds
    area = {'x': list(b['x']), 'z': list(b['z'])}
    if _chunk_count(area) <= FORCELOAD_MAX_CHUNKS:
        return area, _chunk_count(area), 'whole volume'
    while radius > 8:
        area = {'x': [max(b['x'][0], target[0] - radius), min(b['x'][1], target[0] + radius)],
                'z': [max(b['z'][0], target[2] - radius), min(b['z'][1], target[2] + radius)]}
        if _chunk_count(area) <= FORCELOAD_MAX_CHUNKS: break
        radius //= 2
    n = _chunk_count(area)
    if n > FORCELOAD_MAX_CHUNKS: raise ValueError(f'cannot fit a forceload window: {n} chunks')
    return area, n, (f'bounded window of +/-{radius} blocks around the inspection target; '
                     f'the rest of the volume ticks only near a player')

def probe(world, jar, java, eula, node_modules, report, port, volume_id, soak_seconds, player='HarvestSim', keep_world=None, forceload_radius=384):
    if hashlib.sha1(jar.read_bytes()).hexdigest() != SERVER_SHA1: raise ValueError('wrong server jar')
    if not any(l.strip() == 'eula=true' for l in eula.read_text().splitlines()): raise ValueError('existing accepted eula.txt required')
    dest = world/'dimensions/harvest'/volume_id
    volume = loads((dest/'terrain_volume.json').read_text()); mask = Mask(volume)
    target = json.loads((world/'gallery.json').read_text())['targets'][volume_id]
    area, forced_chunks, forced_note = forceload_area(mask, target, forceload_radius)
    results = {}; logs = {}; containment = None

    # Frozen: nothing runs, so nothing may change.
    with tempfile.TemporaryDirectory(prefix='terrain-sim-frozen-') as tmp:
        root = Path(tmp); _prepare(world, root, eula, port, player)
        def frozen(proc, q, lines):
            proc.stdin.write(f'tick freeze\nexecute in harvest:{volume_id} run forceload add {area["x"][0]} {area["z"][0]} {area["x"][1]} {area["z"][1]}\n')
            proc.stdin.flush(); time.sleep(soak_seconds)
        logs['frozen'] = _server(root, jar, java, port, frozen)
        results['frozen'] = diff_regions(dest, root/'world/dimensions/harvest'/volume_id, mask, 'frozen soak')

    # Resumed: ticks run and a bot walks into the envelope on purpose.
    with tempfile.TemporaryDirectory(prefix='terrain-sim-running-') as tmp:
        root = Path(tmp); _prepare(world, root, eula, port, player)
        result_path = root/'containment.json'
        def running(proc, q, lines):
            nonlocal containment
            proc.stdin.write(f'tick unfreeze\nexecute in harvest:{volume_id} run forceload add {area["x"][0]} {area["z"][0]} {area["x"][1]} {area["z"][1]}\n')
            proc.stdin.flush(); time.sleep(5)
            env = dict(os.environ, HARVEST_PORT=str(port), HARVEST_PLAYER=player,
                       HARVEST_VOLUME=volume_id, HARVEST_BOUNDS=json.dumps(mask.bounds),
                       HARVEST_TARGET=json.dumps(target),
                       HARVEST_RESULT=str(result_path), NODE_PATH=str(node_modules))
            node = subprocess.run(['node', str(HERE/'navigation'/'contain.cjs')], env=env,
                                  capture_output=True, text=True, timeout=600)
            logs['node'] = node.stdout + node.stderr
            containment = json.loads(result_path.read_text()) if result_path.exists() else {'pass': False, 'checks': []}
            time.sleep(soak_seconds)
        logs['running'] = _server(root, jar, java, port, running)
        after = root/'world/dimensions/harvest'/volume_id
        results['running'] = diff_regions(dest, after, mask, 'resumed soak with player')
        if keep_world:
            keep_world.mkdir(parents=True, exist_ok=True)
            shutil.copytree(after, keep_world/'after', dirs_exist_ok=True)

    def clean(r):
        c = r['counts']
        return c['cells_changed'] == 0 and c['chunks_generated_nonempty'] == 0 and c['chunks_lost'] == 0
    combined = logs.get('frozen', '') + logs.get('running', '')
    forceload_applied = 'Too many chunks in the specified area' not in combined
    frozen_clean = clean(results['frozen'])
    no_breach = results['running']['counts']['envelope_breached'] == 0
    result = {'schema': 'terrain_simulation_probe/1', 'evidence_state': 'DERIVED MEASUREMENT',
              'volume_id': volume_id, 'soak_seconds': soak_seconds, 'server_sha1': SERVER_SHA1,
              'forceload': {'area': area, 'chunks': forced_chunks, 'coverage': forced_note,
                            'applied': forceload_applied},
              'frozen': results['frozen'], 'running': results['running'],
              'containment': containment,
              'expectations': {'frozen changes nothing': frozen_clean,
                               'envelope intact after simulation': no_breach,
                               'player stayed inside source bounds': bool(containment and containment.get('pass')),
                               'forceload actually applied': forceload_applied},
              'not_covered': ['human client walkthrough', 'rendering', 'long-term fluid equilibrium',
                              'operator, spectator or teleport exploits', 'volumes other than the one probed',
                              'chunks outside the forceloaded window, which only tick near a player'],
              'pass': frozen_clean and no_breach and forceload_applied
                      and bool(containment and containment.get('pass'))}
    report.parent.mkdir(parents=True, exist_ok=True)
    report.with_suffix('.log').write_text('\n===== frozen =====\n' + logs.get('frozen', '')
        + '\n===== running =====\n' + logs.get('running', '') + '\n===== node =====\n' + logs.get('node', ''))
    json_write(report, result)
    if not result['pass']: raise RuntimeError('simulation probe failed; see report')
    return result

if __name__ == '__main__':
    p = argparse.ArgumentParser(description=__doc__)
    for key in ('world', 'jar', 'java', 'eula', 'node-modules', 'report'):
        p.add_argument('--' + key, type=Path, required=True)
    p.add_argument('--volume-id', required=True)
    p.add_argument('--port', type=int, default=25599)
    p.add_argument('--soak-seconds', type=int, default=60)
    p.add_argument('--keep-world', type=Path, default=None, help='copy the post-simulation dimension out for inspection')
    p.add_argument('--forceload-radius', type=int, default=384, help='half-width of the forceloaded window for volumes over 256 chunks')
    a = p.parse_args()
    r = probe(a.world.resolve(), a.jar.resolve(), a.java.resolve(), a.eula.resolve(),
              getattr(a, 'node_modules').resolve(), a.report.resolve(), a.port,
              a.volume_id, a.soak_seconds, keep_world=a.keep_world.resolve() if a.keep_world else None,
              forceload_radius=a.forceload_radius)
    print('forceload:', r['forceload']['chunks'], 'chunks -', r['forceload']['coverage'])
    print('frozen cells changed:', r['frozen']['counts']['cells_changed'])
    print('resumed cells changed:', r['running']['counts']['cells_changed'],
          '| envelope breached:', r['running']['counts']['envelope_breached'])
    print('containment:', sum(1 for c in r['containment']['checks'] if c['ok']), '/', len(r['containment']['checks']))
