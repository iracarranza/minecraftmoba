"""Validate a package by loading a disposable copy with Mojang's matching server."""

from __future__ import annotations

import hashlib
import json
import shutil
import subprocess
import tempfile
import time
from pathlib import Path

EXPECTED_SERVER_JAR_SHA1="64bb6d763bed0a9f1d632ec347938594144943ed"


def validate_with_server(world: Path,server_jar: Path,java: Path,accept_eula: bool):
    if not accept_eula: raise ValueError("--server-check requires --accept-eula after reviewing https://aka.ms/MinecraftEULA")
    with tempfile.TemporaryDirectory(prefix="minecraftmoba-server-check-") as temp:
        root=Path(temp); shutil.copytree(world,root/"world")
        (root/"eula.txt").write_text("eula=true\n")
        (root/"server.properties").write_text("level-name=world\ngamemode=creative\nallow-flight=true\nonline-mode=false\nspawn-protection=0\nview-distance=4\nsimulation-distance=4\ngenerate-structures=false\n")
        process=subprocess.Popen([str(java),"-Xmx1G","-jar",str(server_jar),"--nogui"],cwd=root,stdin=subprocess.PIPE,stdout=subprocess.PIPE,stderr=subprocess.STDOUT,text=True,bufsize=1)
        assert process.stdin and process.stdout
        lines=[]; ready=False; deadline=time.time()+150
        for line in process.stdout:
            lines.append(line)
            if "Done (" in line and not ready:
                ready=True; process.stdin.write("save-all\nstop\n"); process.stdin.flush()
            if time.time()>deadline:
                process.kill(); break
        exit_code=process.wait(); serious=[line.strip() for line in lines if any(word in line for word in ("ERROR","Exception","Failed to load chunk","Unknown block","mismatch"))]
    sha1=hashlib.sha1(server_jar.read_bytes()).hexdigest()
    result={"minecraft_java_version":"1.21.11","server_jar_sha1":sha1,"expected_server_jar_sha1":EXPECTED_SERVER_JAR_SHA1,"ready":ready,"clean_exit":exit_code==0,"serious_log_messages":serious,"pass":sha1==EXPECTED_SERVER_JAR_SHA1 and ready and exit_code==0 and not serious,"note":"Offline-mode warnings in the disposable localhost validation server are expected and are not package defects."}
    (world/"server_compatibility.json").write_text(json.dumps(result,indent=2)+"\n")
    return result
