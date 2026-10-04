#!/usr/bin/env python3
"""Export current foundry forms for the Paper lab; uses installed Java 1.21.11 assets."""
import gzip
import hashlib
import json
import sys
from pathlib import Path

REPO = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(REPO / "implementation/worldgen"))
from terrain_harvest import build_structures as forms
from terrain_harvest.vanilla_assets import DEFAULT_JAR


def main():
    destination = REPO / "implementation/plugin/src/main/resources/lab-authoring"
    destination.mkdir(parents=True, exist_ok=True)
    templates = {"fountain": forms.aether_fountain(),
                 "outpost": forms.pillager_outpost_watchtower(),
                 "bastion": forms.nether_bastion_body(), "spike": forms.end_spike()}
    jar_hash = hashlib.sha256(DEFAULT_JAR.read_bytes()).hexdigest()
    for name, blocks in templates.items():
        payload = {"name": name, "source": "terrain_harvest.build_structures; Java 1.21.11",
                   "asset_jar_sha256": jar_hash,
                   "blocks": [[x, y, z, material +
                               ("[" + ",".join(k + "=" + v for k, v in properties) + "]" if properties else "")]
                              for (x, y, z), (material, properties) in sorted(blocks.items())]}
        encoded = json.dumps(payload, separators=(",", ":")).encode()
        (destination / (name + ".json.gz")).write_bytes(gzip.compress(encoded, mtime=0))
        print(f"{name}: {len(blocks)} blocks")


if __name__ == "__main__":
    main()
