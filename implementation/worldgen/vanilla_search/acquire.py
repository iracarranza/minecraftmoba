"""Generate bounded, unmodified vanilla chunks with the official server."""

from __future__ import annotations

import hashlib
import queue
import subprocess
import threading
import time
from pathlib import Path

from . import SERVER_SHA1, VERSION


class ServerConsole:
    def __init__(self, command, cwd):
        self.lines = queue.Queue()
        self.log = []
        self.process = subprocess.Popen(command, cwd=cwd, stdin=subprocess.PIPE, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True, bufsize=1)
        self.thread = threading.Thread(target=self._read, daemon=True)
        self.thread.start()

    def _read(self):
        for line in self.process.stdout:
            self.log.append(line.rstrip())
            self.lines.put(line)

    def send(self, command):
        self.process.stdin.write(command + "\n")
        self.process.stdin.flush()

    def wait_for(self, text, timeout=180):
        deadline = time.monotonic() + timeout
        while time.monotonic() < deadline:
            try:
                line = self.lines.get(timeout=min(1, deadline - time.monotonic()))
            except queue.Empty:
                if self.process.poll() is not None:
                    raise RuntimeError(f"server exited before {text!r}:\n" + "\n".join(self.log[-30:]))
                continue
            if text in line:
                return line.rstrip()
        raise TimeoutError(f"server did not emit {text!r}:\n" + "\n".join(self.log[-30:]))


def verify_server_jar(path):
    actual = hashlib.sha1(Path(path).read_bytes()).hexdigest()
    if actual != SERVER_SHA1:
        raise ValueError(f"expected official Minecraft Java {VERSION} server SHA-1 {SERVER_SHA1}, got {actual}")


def prepare_runtime(server_jar, java, runtime):
    runtime = Path(runtime)
    if (runtime / "versions" / VERSION / f"server-{VERSION}.jar").exists():
        return 0.0
    runtime.mkdir(parents=True, exist_ok=True)
    started = time.perf_counter()
    subprocess.run([str(java), "-Xmx512M", "-jar", str(server_jar), "--nogui"], cwd=runtime, stdout=subprocess.DEVNULL, stderr=subprocess.STDOUT, timeout=90)
    return time.perf_counter() - started


def generate_world(seed, server_jar, java, root, runtime, chunk_bounds, accept_eula=True, progress=None):
    if not accept_eula:
        raise ValueError("review the Minecraft EULA and pass --accept-eula to use the official server")
    root = Path(root)
    root.mkdir(parents=True, exist_ok=True)
    for name in ("libraries", "versions"):
        (root / name).symlink_to(Path(runtime, name).resolve(), target_is_directory=True)
    (root / "eula.txt").write_text("eula=true\n")
    (root / "server.properties").write_text(
        "level-name=world\n"
        f"level-seed={seed}\n"
        "generate-structures=true\ngamemode=creative\nonline-mode=false\n"
        "spawn-protection=0\nview-distance=2\nsimulation-distance=2\n"
        "max-tick-time=0\nsync-chunk-writes=true\npause-when-empty-seconds=-1\n"
    )
    command = [str(java), "-Xms512M", "-Xmx3G", "-jar", str(server_jar), "--nogui"]
    total_started = time.perf_counter()
    server = ServerConsole(command, root)
    server.wait_for("Done (", 120)
    startup_seconds = time.perf_counter() - total_started
    min_cx, max_cx, min_cz, max_cz = chunk_bounds
    batches = [(x, min(x + 15, max_cx), z, min(z + 15, max_cz)) for z in range(min_cz, max_cz + 1, 16) for x in range(min_cx, max_cx + 1, 16)]
    generation_started = time.perf_counter()
    for index, (cx0, cx1, cz0, cz1) in enumerate(batches, 1):
        server.send(f"forceload add {cx0 * 16} {cz0 * 16} {cx1 * 16 + 15} {cz1 * 16 + 15}")
        server.wait_for("to be force loaded", 240)
        server.send("forceload remove all")
        server.wait_for("Unmarked all force loaded chunks", 60)
        if index % 5 == 0 or index == len(batches):
            server.send("save-all flush")
            server.wait_for("Saved the game", 120)
        if progress:
            progress(index, len(batches))
    generation_seconds = time.perf_counter() - generation_started
    server.send("stop")
    try:
        server.wait_for("Stopping server", 60)
    finally:
        server.process.wait(timeout=120)
    (root / "acquisition.log").write_text("\n".join(server.log) + "\n")
    return root / "world", {"startup_seconds": round(startup_seconds, 3), "chunk_generation_and_save_seconds": round(generation_seconds, 3), "total_acquisition_seconds": round(time.perf_counter() - total_started, 3), "forceload_batches": len(batches)}
