#!/usr/bin/env python3
"""Measure the exact Paper PDC object graph with JVM Instrumentation (Java 21)."""
import argparse
import os
from pathlib import Path
import subprocess
import tempfile

p = argparse.ArgumentParser()
p.add_argument('--java-home', type=Path, required=True)
p.add_argument('--server', type=Path, required=True, help='Paper 1.21.11 directory with libraries and versions populated')
p.add_argument('--output', type=Path, required=True)
p.add_argument('payload_bytes', nargs='+', type=int)
a = p.parse_args()
if any(n < 0 for n in a.payload_bytes):
    p.error('payload sizes must be nonnegative')
java = a.java_home / 'bin'
with tempfile.TemporaryDirectory(prefix='moba-pdc-memory-') as temporary:
    work = Path(temporary)
    source = Path(__file__).with_name('PaperMemoryProbe.java')
    subprocess.run([str(java / 'javac'), '-d', str(work), str(source)], check=True)
    manifest = work / 'MANIFEST.MF'
    manifest.write_text('Premain-Class: PaperMemoryProbe\n')
    agent = work / 'probe.jar'
    subprocess.run([str(java / 'jar'), 'cfm', str(agent), str(manifest), '-C', str(work), 'PaperMemoryProbe.class'], check=True)
    jars = sorted((a.server / 'libraries').rglob('*.jar'))
    jars.append(a.server / 'versions/1.21.11/paper-1.21.11.jar')
    classpath = os.pathsep.join(map(str, [work, *jars]))
    output = subprocess.check_output([str(java / 'java'), '-javaagent:' + str(agent),
        '--add-opens', 'java.base/java.util=ALL-UNNAMED', '--add-opens', 'java.base/java.lang=ALL-UNNAMED',
        '-cp', classpath, 'PaperMemoryProbe', *map(str, a.payload_bytes)], text=True)
    a.output.write_text(output)
    print(output, end='')
