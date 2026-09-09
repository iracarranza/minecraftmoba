"""Create a matching vanilla level.dat template with the official server jar."""

from __future__ import annotations

import shutil
import subprocess
import time
from pathlib import Path


def bootstrap_level_template(server_jar: Path,java: Path,workdir: Path,accept_eula: bool) -> Path:
    if not accept_eula: raise ValueError("Pass --accept-eula after reviewing https://aka.ms/MinecraftEULA")
    if workdir.exists(): shutil.rmtree(workdir)
    workdir.mkdir(parents=True)
    (workdir/"eula.txt").write_text("eula=true\n")
    (workdir/"server.properties").write_text("level-name=template_world\nlevel-seed=920261010\ngenerate-structures=false\ngamemode=creative\nallow-flight=true\nonline-mode=false\nspawn-protection=0\nview-distance=4\nsimulation-distance=4\n")
    process=subprocess.Popen([str(java),"-Xmx1G","-jar",str(server_jar),"--nogui"],cwd=workdir,stdin=subprocess.PIPE,stdout=subprocess.PIPE,stderr=subprocess.STDOUT,text=True,bufsize=1)
    log=[]; deadline=time.time()+150
    assert process.stdout and process.stdin
    for line in process.stdout:
        log.append(line)
        if "Done (" in line:
            process.stdin.write("stop\n"); process.stdin.flush()
        if time.time()>deadline:
            process.kill(); raise TimeoutError("server bootstrap timed out")
    code=process.wait(); (workdir/"bootstrap.log").write_text("".join(log))
    template=workdir/"template_world"/"level.dat"
    if code or not template.exists(): raise RuntimeError(f"server bootstrap failed ({code}); see {workdir/'bootstrap.log'}")
    return template
