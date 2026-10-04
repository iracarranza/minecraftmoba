"""Boot disposable inspection-world copies and check fountains in Minecraft."""
import argparse
import json
from pathlib import Path
import shutil
import sys

REPO = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(REPO / 'implementation/worldgen'))
from vanilla_search.acquire import ServerConsole, free_port, verify_server_jar
from serialization.nbt import load_gzip, plain


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('reports', type=Path)
    parser.add_argument('artifacts', type=Path)
    parser.add_argument('--java', type=Path, required=True)
    parser.add_argument('--server-jar', type=Path, required=True)
    parser.add_argument('--runtime', type=Path, required=True)
    args = parser.parse_args()
    verify_server_jar(args.server_jar)
    index = args.reports / 'server-smoke.json'
    results = json.loads(index.read_text()) if index.exists() else []
    for package in json.loads((args.reports / 'packages.json').read_text()):
        ident = package['map_id']
        if any(r['map_id'] == ident and r['passed'] for r in results):
            continue
        root = args.artifacts / 'build/inspection-smoke' / ident
        root.mkdir(parents=True)
        shutil.copytree(package['world'], root / 'world')
        for name in ('libraries', 'versions'):
            (root / name).symlink_to((args.runtime / name).resolve(), target_is_directory=True)
        (root / 'eula.txt').write_text('eula=true\n')
        (root / 'server.properties').write_text(
            f'level-name=world\nserver-ip=127.0.0.1\nserver-port={free_port()}\n'
            'online-mode=false\ngamemode=creative\nview-distance=2\n'
            'simulation-distance=2\nmax-tick-time=0\npause-when-empty-seconds=-1\n')
        data = plain(load_gzip(root / 'world/level.dat')[1])['Data']
        generator = data['WorldGenSettings']['dimensions']['minecraft:overworld']['generator']
        assert generator['type'] == 'minecraft:flat'
        assert generator['settings']['layers'] == []
        assert generator['settings']['structure_overrides'] == []
        inspection = json.loads((root / 'world/INSPECTION.json').read_text())
        server = ServerConsole([str(args.java), '-Xms256M', '-Xmx2G', '-jar',
                                str(args.server_jar.resolve()), '--nogui'], root)
        result = {'map_id': ident, 'passed': False, 'void_generator_verified': True}
        try:
            server.wait_for('Done (', 120)
            for team, (x, y, z) in inspection['runtime_bindings']['fountains'].items():
                server.send(f'forceload add {x} {z}')
                # Query a real loaded chunk rather than an unloaded coordinate.
                server.wait_for('to be force loaded', 60)
                for label, yy, state in [('PLINTH', y, 'minecraft:chiseled_quartz_block'),
                                         ('SOURCE', y+4, 'minecraft:water[level=0]')]:
                    token = f'TEST_{team.upper()}_{label}_OK'
                    server.send(f'execute if block {x} {yy} {z} {state} run say {token}')
                    server.wait_for(token, 30)
            result['passed'] = True
        except Exception as exc:
            result['error'] = f'{type(exc).__name__}: {exc}'
        finally:
            if server.process.poll() is None:
                server.send('stop')
                try:
                    server.process.wait(timeout=60)
                except Exception:
                    server.process.terminate()
                    server.process.wait(timeout=15)
            (args.reports / f'{ident}-server-smoke.log').write_text('\n'.join(server.log)+'\n')
        results.append(result)
        index.write_text(json.dumps(results, indent=2))
        print(result, flush=True)
    if any(not result['passed'] for result in results):
        raise SystemExit(1)


if __name__ == '__main__':
    main()
