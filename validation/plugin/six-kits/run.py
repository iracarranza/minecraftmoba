"""Build and run the kit fixture on a NEW disposable Paper server, never the user's server."""
from pathlib import Path
import argparse, shutil, subprocess, os
import re

p = argparse.ArgumentParser(description=__doc__)
p.add_argument('--runtime', type=Path, required=True)
p.add_argument('--server', type=Path, required=True)
p.add_argument('--results', type=Path, required=True)
p.add_argument('--port', type=int, default=25668)
p.add_argument('--java-home', type=Path, default=Path('/opt/homebrew/opt/openjdk@21'))
a = p.parse_args()
repo = Path(__file__).resolve().parents[3]
root = Path(__file__).resolve().parent
if a.server.exists():
    raise SystemExit('Refusing an existing server directory')
if 'eula=true' not in (a.runtime / 'eula.txt').read_text():
    raise SystemExit('Runtime has no existing accepted EULA to reuse')
a.results.mkdir(parents=True, exist_ok=True)
build = root / 'build/fixture-classes'
build.mkdir(parents=True, exist_ok=True)
jar = repo / 'implementation/plugin/build/libs/minecraft-moba-0.1.0-SNAPSHOT.jar'
cached = list((Path.home() / '.gradle/caches/modules-2/files-2.1').rglob('*.jar'))
cp = os.pathsep.join(map(str, [jar, *cached]))
subprocess.run([str(a.java_home/'bin/javac'), '--release', '21', '-proc:none', '-cp', cp, '-d', str(build),
                str(root/'src/kf/SixKitAcceptance.java')], check=True)
shutil.copyfile(root/'src/plugin.yml', build/'plugin.yml')
fixture = root / 'build/SixKitAcceptance.jar'
subprocess.run([str(a.java_home/'bin/jar'), 'cf', str(fixture), '-C', str(build), '.'], check=True)
a.server.mkdir(parents=True)
for name in ('paper.jar', 'eula.txt'):
    shutil.copyfile(a.runtime/name, a.server/('validation-runtime.jar' if name == 'paper.jar' else name))
for name in ('libraries', 'versions', 'cache'):
    shutil.copytree(a.runtime/name, a.server/name)
plugins = a.server/'plugins'
(plugins/'MinecraftMoba').mkdir(parents=True)
shutil.copyfile(jar, plugins/'minecraft-moba.jar')
shutil.copyfile(fixture, plugins/'SixKitAcceptance.jar')
cfg = (repo/'implementation/plugin/src/main/resources/config.yml').read_text()
cfg = re.sub(r'^  templatePath:.*$', '  templatePath: "' + str(repo/'artifacts/worldgen/alpha-0.1/base-terrain') + '"', cfg, flags=re.M)
start = cfg.index('  configurations:', cfg.index('alpha:'))
end = cfg.index('  lab:', start)
pool = cfg[start:end]
pool = re.sub(r'^    directory:.*$', '    directory: "' + str(repo/'artifacts/worldgen/alpha-0.1/maps') + '"', pool, flags=re.M)
cfg = cfg[:start] + pool + cfg[end:]
start = cfg.index('  resourcePack:')
end = cfg.index('\n  ', start+3)
# Only its explicit enabled leaf; the remainder of resource pack configuration is preserved.
segment = cfg[start:]
segment = segment.replace('    enabled: true', '    enabled: false', 1)
cfg = cfg[:start] + segment
(plugins/'MinecraftMoba/config.yml').write_text(cfg)
(a.server/'server.properties').write_text(f'server-ip=127.0.0.1\nserver-port={a.port}\nonline-mode=false\n'
    'level-name=six_kits_acceptance_boot\nlevel-type=minecraft:flat\ngenerate-structures=false\n'
    'spawn-protection=0\nview-distance=3\nsimulation-distance=3\npause-when-empty-seconds=-1\n')
with (a.results/'server.log').open('w') as log:
    proc = subprocess.Popen([str(a.java_home/'bin/java'), '-Xms512M', '-Xmx2G', '-jar', 'validation-runtime.jar', '--nogui'],
                            cwd=a.server, stdin=subprocess.PIPE, stdout=log, stderr=subprocess.STDOUT)
    try:
        proc.wait(timeout=180)
    except subprocess.TimeoutExpired:
        proc.stdin.write(b'stop\n'); proc.stdin.flush()
        try: proc.wait(timeout=20)
        except subprocess.TimeoutExpired: proc.kill(); proc.wait()
        raise SystemExit('Timed out; see server.log')
report = plugins/'SixKitAcceptance/report.txt'
if not report.exists():
    raise SystemExit(f'No fixture report (server exit {proc.returncode}); see server.log')
shutil.copyfile(report, a.results/'report.txt')
text = report.read_text()
print(text)
if 'FAIL ' in text or 'fail=0' not in text:
    raise SystemExit(f'Kit acceptance incomplete or failed (server exit {proc.returncode})')
