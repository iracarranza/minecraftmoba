#!/usr/bin/env python3
"""Install certified reusable scoops and optionally stage a Paper plugin update."""
import argparse
import hashlib
import json
import shutil
from pathlib import Path


def fingerprint(world):
    combined = hashlib.sha256()
    for region in sorted((world / "region").glob("*.mca")):
        combined.update(region.name.encode())
        combined.update(hashlib.sha256(region.read_bytes()).digest())
    return combined.hexdigest()


def main():
    repo = Path(__file__).resolve().parents[2]
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("server", type=Path)
    parser.add_argument("--source", type=Path, default=repo / "artifacts/worldgen/seed_reevaluation_2026-10-02/test-pool")
    parser.add_argument("--maps-directory", default="lab-maps")
    parser.add_argument("--stage-plugin", action="store_true")
    args = parser.parse_args()
    entries = sorted(args.source.glob("*/map.json"))
    if not entries:
        raise SystemExit("No lab scoops found")
    destination = args.server / args.maps_directory
    # Validate every entry before making any installation changes.
    for manifest in entries:
        doc = json.loads(manifest.read_text())
        if fingerprint(manifest.parent / "world") != doc.get("world_fingerprint"):
            raise SystemExit(f"Certification fingerprint mismatch: {manifest}")
        target = destination / manifest.parent.name
        if target.exists() and (not (target / "map.json").exists()
                                or json.loads((target / "map.json").read_text()) != doc
                                or fingerprint(target / "world") != doc["world_fingerprint"]):
            raise SystemExit(f"Refusing to overwrite a different existing scoop: {target}")
    jar = repo / "implementation/plugin/build/libs/minecraft-moba-0.1.0-SNAPSHOT.jar"
    if args.stage_plugin and not jar.is_file():
        raise SystemExit("Build the plugin before staging it")
    for manifest in entries:
        target = destination / manifest.parent.name
        if not target.exists():
            shutil.copytree(manifest.parent, target)
        print(f"Reusable scoop: {target}")
    if args.stage_plugin:
        update = args.server / "plugins/update"
        update.mkdir(parents=True, exist_ok=True)
        shutil.copy2(jar, update / jar.name)
        print(f"Plugin staged for next Paper restart: {update / jar.name}")
    print("Existing server configuration and active plugin jar preserved.")


if __name__ == "__main__":
    main()
