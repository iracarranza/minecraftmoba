"""Load a disposable gallery copy in the pinned official server and probe blocks.
Requires an existing accepted eula.txt; never accepts an agreement on your behalf.
"""
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
from .materialize import json_write

SERVER_SHA1='64bb6d763bed0a9f1d632ec347938594144943ed'

def probe(world,jar,java,eula,report):
    if hashlib.sha1(jar.read_bytes()).hexdigest()!=SERVER_SHA1:raise ValueError('wrong server jar')
    if not any(l.strip()=='eula=true' for l in eula.read_text().splitlines()):raise ValueError('existing accepted eula.txt required')
    info=json.loads((world/'gallery.json').read_text());lines=[];expected=[];commands=[]
    with tempfile.TemporaryDirectory(prefix='terrain-gallery-probe-') as tmp:
        root=Path(tmp);shutil.copytree(world,root/'world');shutil.copyfile(eula,root/'eula.txt')
        (root/'server.properties').write_text('level-name=world\nonline-mode=false\nserver-ip=127.0.0.1\nserver-port=0\nview-distance=2\nsimulation-distance=2\nspawn-protection=0\n')
        for r in info['volumes']:
            ident=r['volume_id'];v=json.loads((world/f'dimensions/harvest/{ident}/terrain_volume.json').read_text());b=v['provenance']['source_bounds'];x=sum(b['x'])//2;z=sum(b['z'])//2
            commands.append(f'execute in harvest:{ident} run forceload add {x} {z}')
            for label,y,block in [('floor',b['y'][0]-1,'bedrock'),('roof',b['y'][1]+1,'barrier')]:
                marker=f'HARVEST_OK_{ident}_{label}';expected.append(marker)
                commands.append(f'execute in harvest:{ident} if block {x} {y} {z} minecraft:{block} run say {marker}')
        proc=subprocess.Popen([str(java),'-Xmx2G','-jar',str(jar),'--nogui'],cwd=root,stdin=subprocess.PIPE,stdout=subprocess.PIPE,stderr=subprocess.STDOUT,text=True,bufsize=1)
        q=queue.Queue()
        def read():
            for line in proc.stdout:q.put(line)
        threading.Thread(target=read,daemon=True).start();ready=False;sent=False;deadline=time.monotonic()+180
        try:
            while time.monotonic()<deadline and proc.poll() is None:
                try:line=q.get(timeout=.2);lines.append(line)
                except queue.Empty:continue
                if 'Done (' in line and not ready:
                    ready=True;proc.stdin.write('tick freeze\n'+ '\n'.join(c for c in commands if 'forceload' in c)+'\n');proc.stdin.flush();check_at=time.monotonic()+8
                if ready and not sent and time.monotonic()>=check_at:
                    proc.stdin.write('\n'.join(c for c in commands if 'forceload' not in c)+'\nsave-all\nstop\n');proc.stdin.flush();sent=True
                # If server is silent after readiness, the queue timeout path must still dispatch.
                if ready and not sent:
                    while time.monotonic()<check_at:
                        try:lines.append(q.get(timeout=.2))
                        except queue.Empty:pass
                    proc.stdin.write('\n'.join(c for c in commands if 'forceload' not in c)+'\nsave-all\nstop\n');proc.stdin.flush();sent=True
            if proc.poll() is None:proc.kill()
            code=proc.wait(timeout=10)
            while not q.empty():lines.append(q.get())
        finally:
            if proc.poll() is None:proc.kill();proc.wait()
    text=''.join(lines);errors=[l.strip() for l in lines if any(w in l for w in ('ERROR','Exception','Failed to load','Unknown block','Unknown function','Parsing error'))]
    result={'ready':ready,'exit_code':code,'expected_markers':expected,'observed_markers':[m for m in expected if m in text],
            'errors':errors,'pass':ready and code==0 and not errors and all(m in text for m in expected),
            'scope':'disposable server load, datapack parse, actual floor/roof block probes; no player navigation or client walkthrough',
            'server_sha1':SERVER_SHA1,'commands':commands}
    report.parent.mkdir(parents=True,exist_ok=True);report.with_suffix('.log').write_text(text);json_write(report,result)
    if not result['pass']:raise RuntimeError('server probe failed; see report')

if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__)
    for key in ('world','jar','java','eula','report'):p.add_argument('--'+key,type=Path,required=True)
    a=p.parse_args();probe(a.world.resolve(),a.jar.resolve(),a.java.resolve(),a.eula.resolve(),a.report.resolve())
