"""Drive gallery navigation functions over the real protocol on a disposable server.

Reuses the repository's pinned Mineflayer fixture. The gallery is copied first,
so the installed inspection save is never touched, and the server runs on its
own port with an explicit offline operator entry. This proves the datapack
functions work over the protocol; it is not a human client walkthrough.
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
import uuid
from pathlib import Path
from .materialize import json_write
from .server_probe import SERVER_SHA1

HERE = Path(__file__).resolve().parent

def offline_uuid(name):
    """Vanilla offline-mode UUID: MD5 of 'OfflinePlayer:<name>' as a version 3 UUID."""
    d = bytearray(hashlib.md5(('OfflinePlayer:' + name).encode()).digest())
    d[6] = (d[6] & 0x0f) | 0x30
    d[8] = (d[8] & 0x3f) | 0x80
    return str(uuid.UUID(bytes=bytes(d)))

def probe(world, jar, java, eula, node_modules, report, port, player='HarvestNav'):
    if hashlib.sha1(jar.read_bytes()).hexdigest() != SERVER_SHA1: raise ValueError('wrong server jar')
    if not any(l.strip() == 'eula=true' for l in eula.read_text().splitlines()): raise ValueError('existing accepted eula.txt required')
    if not (node_modules/'mineflayer').is_dir(): raise ValueError('pinned mineflayer fixture not found')
    manifest = world/'gallery.json'
    if not manifest.exists(): raise ValueError('gallery.json missing; incomplete build')

    lines = []; result_path = None; node_out = ''
    with tempfile.TemporaryDirectory(prefix='terrain-gallery-nav-') as tmp:
        root = Path(tmp)
        shutil.copytree(world, root/'world')
        shutil.copyfile(eula, root/'eula.txt')
        (root/'server.properties').write_text(
            f'level-name=world\nonline-mode=false\nserver-ip=127.0.0.1\nserver-port={port}\n'
            'view-distance=4\nsimulation-distance=4\nspawn-protection=0\nmax-tick-time=-1\n')
        (root/'ops.json').write_text(json.dumps([{ 'uuid': offline_uuid(player), 'name': player,
            'level': 4, 'bypassesPlayerLimit': True }]))
        result_path = root/'navigation.json'

        proc = subprocess.Popen([str(java), '-Xmx2G', '-jar', str(jar), '--nogui'], cwd=root,
            stdin=subprocess.PIPE, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True, bufsize=1)
        q = queue.Queue()
        threading.Thread(target=lambda: [q.put(l) for l in proc.stdout], daemon=True).start()
        ready = False; deadline = time.monotonic() + 300; code = None
        try:
            while time.monotonic() < deadline and proc.poll() is None and not ready:
                try: line = q.get(timeout=.5); lines.append(line)
                except queue.Empty: continue
                if 'Done (' in line: ready = True
            if not ready: raise RuntimeError('server never became ready')
            # Freeze before the bot joins so an exact teleport target cannot drift.
            proc.stdin.write('tick freeze\n'); proc.stdin.flush(); time.sleep(2)
            env = dict(os.environ, HARVEST_PORT=str(port), HARVEST_PLAYER=player,
                       HARVEST_MANIFEST=str(root/'world'/'gallery.json'), HARVEST_RESULT=str(result_path),
                       NODE_PATH=str(node_modules))
            node = subprocess.run(['node', str(HERE/'navigation'/'navigate.cjs')], env=env,
                                  capture_output=True, text=True, timeout=300)
            node_out = node.stdout + node.stderr
            code = node.returncode
            proc.stdin.write('stop\n'); proc.stdin.flush()
            try: proc.wait(timeout=60)
            except subprocess.TimeoutExpired: proc.kill()
            while not q.empty(): lines.append(q.get())
            navigation = json.loads(result_path.read_text()) if result_path.exists() else {'pass': False, 'checks': []}
        finally:
            if proc.poll() is None: proc.kill(); proc.wait()

    text = ''.join(lines)
    errors = [l.strip() for l in lines if any(w in l for w in ('ERROR', 'Exception', 'Failed to load', 'Unknown function', 'Parsing error'))]
    result = {'schema': 'terrain_gallery_navigation_probe/1', 'evidence_state': 'DERIVED MEASUREMENT',
              'server_sha1': SERVER_SHA1, 'node_exit_code': code, 'server_errors': errors,
              'navigation': navigation,
              'scope': 'disposable copy, frozen ticks, protocol-level function navigation with dimension/position/mode assertions',
              'not_covered': ['human client walkthrough', 'rendering', 'unfrozen simulation',
                              'containment against a moving player', 'teleport or operator exploits'],
              'pass': code == 0 and not errors and navigation.get('pass') is True}
    report.parent.mkdir(parents=True, exist_ok=True)
    report.with_suffix('.log').write_text(text + '\n----- node -----\n' + node_out)
    json_write(report, result)
    if not result['pass']: raise RuntimeError('navigation probe failed; see report')
    return result

if __name__ == '__main__':
    p = argparse.ArgumentParser(description=__doc__)
    for key in ('world', 'jar', 'java', 'eula', 'node-modules', 'report'):
        p.add_argument('--' + key, type=Path, required=True)
    p.add_argument('--port', type=int, default=25599)
    a = p.parse_args()
    r = probe(a.world.resolve(), a.jar.resolve(), a.java.resolve(), a.eula.resolve(),
              getattr(a, 'node_modules').resolve(), a.report.resolve(), a.port)
    n = r['navigation']
    print(f"PASS {sum(1 for c in n['checks'] if c['ok'])}/{len(n['checks'])} navigation assertions")
