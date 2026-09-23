"""Create a NEW disposable Alpha Hunger test server; never alters its input runtime/base."""
import argparse
from pathlib import Path
import shutil
import yaml

p = argparse.ArgumentParser(description=__doc__)
for arg in ('server', 'runtime', 'base'):
    p.add_argument('--' + arg, type=Path, required=True)
a = p.parse_args()
repo = Path(__file__).resolve().parents[3]
server, runtime, base = (v.resolve() for v in (a.server, a.runtime, a.base))
if server.exists():
    raise SystemExit('Refusing existing server directory')
if not (base / 'region').is_dir():
    raise SystemExit('Expected pristine Alpha base terrain with region directory')
server.mkdir(parents=True)
for name in ('paper.jar', 'eula.txt'):
    shutil.copyfile(runtime / name, server / name)
for name in ('libraries', 'versions', 'cache', 'maps'):
    shutil.copytree(runtime / name, server / name)
plugins = server / 'plugins'
(plugins / 'MinecraftMoba').mkdir(parents=True)
shutil.copyfile(repo / 'implementation/plugin/build/libs/minecraft-moba-0.1.0-SNAPSHOT.jar', plugins / 'minecraft-moba.jar')
shutil.copyfile(repo / 'validation/plugin/server-fixture/build/libs/acceptance-fixture-0.1.0.jar', plugins / 'acceptance-fixture.jar')
cfg = yaml.safe_load((repo / 'implementation/plugin/src/main/resources/config.yml').read_text())
cfg['alpha']['templatePath'] = str(base)
cfg['alpha']['configurations']['directory'] = str(server / 'maps')
cfg['alpha']['configurations']['force'] = 'resource_light_v2'
# A protocol bot does not render a resource pack. No gameplay feature changes.
cfg['features']['resourcePack']['enabled'] = False
(plugins / 'MinecraftMoba/config.yml').write_text(yaml.safe_dump(cfg, sort_keys=False))
props = dict(line.split('=', 1) for line in (runtime / 'server.properties').read_text().splitlines()
             if line and not line.startswith('#') and '=' in line)
props.update({'server-ip': '127.0.0.1', 'server-port': '25640', 'enable-rcon': 'false',
              'enable-query': 'false', 'pause-when-empty-seconds': '-1', 'view-distance': '4',
              'simulation-distance': '4'})
(server / 'server.properties').write_text('\n'.join(k + '=' + v for k, v in props.items()) + '\n')
(server / '.hunger-disposable').write_text('Acceptance test only\n')
print(server)
