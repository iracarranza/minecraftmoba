"""Give an already-published scoop its overlay data.

Scoops published before the overlay existed carry no ``inspection.json``. The compilation
that certified them is not stored with them, so this recompiles the same harvested candidate,
picks the run that matches the published map (same Fountains), and writes the sidecar.

It compiles into a temporary directory, never into the harvest's own build, and it does not
touch the published world or ``map.json``.

    python -m terrain_harvest.inspection_sidecar <entry-dir> <harvest-root>

``harvest-root`` holds ``<seed>_<n>/candidate.json`` and ``<seed>_<n>/server/world``, as
`foundry_run` leaves them for a playable seed.
"""
from __future__ import annotations

import json
import sys
import tempfile
from pathlib import Path

from . import compile_batch, inspection


def regenerate(entry: Path, harvest_root: Path) -> Path:
    entry = Path(entry)
    manifest = json.loads((entry / 'map.json').read_text())
    seed = manifest['provenance']['seed']
    want = manifest['runtime_bindings']['fountains']
    tried = []
    for n in range(8):
        work = Path(harvest_root) / f'{seed}_{n}'
        candidate, world = work / 'candidate.json', work / 'server' / 'world'
        if not candidate.is_file() or not world.is_dir():
            continue
        with tempfile.TemporaryDirectory() as tmp:
            out = compile_batch.run([candidate], worlds={seed: world}, build_root=Path(tmp))
        for run in out.get('runs') or []:
            ev = run.get('evidence') or {}
            bindings = ev.get('runtime_bindings') or {}
            tried.append((n, bindings.get('fountains')))
            if run.get('playable') and bindings.get('fountains') == want:
                data = inspection.build(ev, bindings)
                if data is None:
                    raise RuntimeError(f'seed {seed}: the matching compilation has no cell grid')
                target = entry / 'inspection.json'
                target.write_text(json.dumps(data))
                return target
    raise RuntimeError(f'seed {seed}: no compilation matched the published Fountains {want}; tried {tried}')


def main(argv=None):
    argv = list(sys.argv[1:] if argv is None else argv)
    if len(argv) != 2:
        print(__doc__)
        return 2
    print(regenerate(Path(argv[0]), Path(argv[1])))
    return 0


if __name__ == '__main__':
    raise SystemExit(main())
