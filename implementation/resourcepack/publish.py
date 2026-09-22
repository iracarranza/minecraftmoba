"""Build the pack, zip it, and put it where the server can hand it to clients.

A resource pack the server pushes has to be a zip reachable over HTTP, and the
client verifies it against a SHA-1. That hash changes on every build, so the
hash and the zip have to be published together or clients silently keep a stale
pack -- which looks exactly like the pack not working.

So this does all of it in one step: build to a scratch directory, zip, hash,
copy into the served directory, and write the hash into the server's own config
so the plugin pushes the right one.
"""
from __future__ import annotations

import argparse
import hashlib
import re
import shutil
import tempfile
import zipfile
from pathlib import Path

from build_pack import build
import json

HERE = Path(__file__).resolve().parent


def publish(serve_dir: Path, config: Path | None, name: str = "moba-pack.zip"):
    registry = json.loads((HERE / "registry.json").read_text())
    with tempfile.TemporaryDirectory() as tmp:
        staged = Path(tmp) / "pack"
        build(registry, staged)
        zip_path = Path(tmp) / name
        with zipfile.ZipFile(zip_path, "w", zipfile.ZIP_DEFLATED) as z:
            for f in sorted(staged.rglob("*")):
                if f.is_file():
                    z.write(f, f.relative_to(staged).as_posix())
        digest = hashlib.sha1(zip_path.read_bytes()).hexdigest()
        serve_dir.mkdir(parents=True, exist_ok=True)
        shutil.copy2(zip_path, serve_dir / name)

    if config is not None and config.exists():
        text = config.read_text()
        updated, n = re.subn(r'(?m)^(\s*sha1:\s*).*$', rf'\g<1>"{digest}"', text, count=1)
        if n == 0:
            raise SystemExit(f"no features.resourcePack.sha1 key in {config}; add one first")
        config.write_text(updated)

    return digest, serve_dir / name


def main(argv=None):
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument("--serve-dir", type=Path, required=True)
    p.add_argument("--config", type=Path,
                   help="deployed config.yml whose features.resourcePack.sha1 to update")
    a = p.parse_args(argv)
    digest, path = publish(a.serve_dir.resolve(), a.config.resolve() if a.config else None)
    size = path.stat().st_size
    print(f"published {path} ({size} bytes)")
    print(f"sha1 {digest}")
    if a.config:
        print(f"wrote the hash into {a.config}")


if __name__ == "__main__":
    main()
