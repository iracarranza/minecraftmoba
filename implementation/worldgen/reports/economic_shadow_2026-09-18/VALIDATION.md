# Validation — 18 September 2026

- Repository preflight: `git remote -v`, `git rev-parse --show-toplevel`, `git status --short --branch`, `git log -5 --oneline`. Confirmed Minecraft MOBA before corpus analysis.
- `PYTHONPATH=implementation/worldgen python3 -m unittest discover -s implementation/worldgen/tests -p test_economic_shadow.py`: **9 passed**. Covers wrong-repository early stop, URL spoof rejection, renamed/reference-only corpus and untracked discovery, missing input/world handling, negative coordinates and clipping, component adjacency, unused palette entries, and homogeneous sections without yield multiplication. Fixtures are synthetic.
- `PYTHONPATH=implementation/worldgen python3 -m unittest discover -s implementation/worldgen/tests -p test_task_a_opportunities.py`: **19 passed**.
- `python3 implementation/worldgen/economic_shadow.py --output implementation/worldgen/reports/economic_shadow_2026-09-18`: both seeds `OBSERVED_CURRENT_SNAPSHOT`, six Handoffs each.
- Repeated the same exporter into a fresh temporary directory: **all 14 generated JSON files byte-identical** (inventory, measurements, twelve raw exports). Report and manually inspected capability catalog are separate editorial artifacts.
- Reconciled all twelve raw observation array lengths with summed per-material block counts: **passed**.
- Compared copied Opportunity Relationship sets and all twelve Handoff relationship objects to original fit JSON: **identical**.
- Rehashed every existing working-tree inventory input against its recorded hash after analysis: **unchanged**.
- Rehashed every consumed world region after the repeat run: **unchanged**. Exporter also checks region hashes and level.dat stability during each seed analysis and validates world seed identity.
- `git diff --check`: **passed**.

No test converted fixture output to seed evidence. No server was launched, no seed was generated, and no physical world, canonical source, fitter, selection or previous artifact was changed. Existing user modifications were not reverted. No commits, merges, pushes or VWM operations were performed.

Reproduction requires the local worlds recorded in `measurements.json`; they are not assumed portable or present in a clean clone. Missing worlds/chunks remain UNRESOLVED. The `origin/main` corpus is pinned by commit in `inventory.json`; future runs intentionally inventory the then-current local reference and may differ when inputs change.
