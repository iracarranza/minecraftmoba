# Archipelago search review — 14 October 2026

Review snapshot for the archipelago seed search, isolated from unrelated gameplay
and Default-catalogue work. **Four new basic shared-check candidates were banked;
none is approved for the visually sequential Hawaii-like shape or runtime READY.**

The bundle is approximately 5 MB. [manifest.json](manifest.json) records every
included source/evidence file, original source path, byte size and SHA-256.
[provenance.json](provenance.json) records native/compiler versions and exclusions.

## Start here

- [Nine-candidate overview and all 36 distances per layout](candidate-overview.json).
- [Original five maps](original-five/qualified-archipelagos.png), with the user's
  [design review](original-five/design-review.json).
- [New four maps](new-four/qualified-archipelagos.png) and independent
  [river-aware geography/spacing audit](new-four/independent-geographic-audit.json).
- [Native sequence prospects](shape-search/fine-sequence-prospects.png),
  [the first two actual terrain maps](shape-search/actual-sequence-targets.png),
  [5,000-seed search counts](shape-search/search-summary.json) and
  [all six actual-target verification outcomes](shape-search/verification-results.json).

Full saved compiler summaries are compressed in
[original-five/qualified-summary.json.gz](original-five/qualified-summary.json.gz)
and [new-four/qualified-summary.json.gz](new-four/qualified-summary.json.gz).
They preserve physical/opening/resource evidence and characterization. The word
`qualified` in those historical filenames means the **implemented shared stages**,
not approval for island roles, morphology, full boss volume or runtime readiness.

## Candidate status

Distances below are horizontal anchor-to-anchor blocks. Every saved passing layout
has 9 anchors and all 36 pair distances independently checked at >=175 blocks.

| Seed | Saved status | Minimum all-nine spacing | Fountain–Lair N / S |
|---|---|---:|---:|
| 890000020 | User rejected Type/readability | 214.7 | 1656 / 868 |
| 890001347 | Prior prospect | 177.6 | 1052 / 970 |
| 890001592 | User rejected Type/readability | 181.4 | 1703 / 1961 |
| 890001687 | Prior prospect | 179.8 | 1308 / 1761 |
| 890001809 | Prior prospect | 218.4 | 1694 / 897 |
| 890001380 | New basic shared-check pass | 186.8 | 1056 / 1300 |
| 890001757 | New basic shared-check pass | 178.9 | 594 / 560 |
| 890001967 | New basic shared-check pass | 241.9 | 928 / 898 |
| 890001993 | New basic shared-check pass | 193.7 | 1263 / 1260 |

The long Fountain-to-Lair distances are unresolved design concerns. Numerical
access parity does not establish reasonable discovery or boat travel time.
The original 890000020 and 890001592 passes remain historical; the user rejected
them for Type/readability. The other original three are prospects, not final maps.

## Review points and limitations

1. Island identity uses four-neighbour connectivity of the 8-block actual source
   grid, restoring river/frozen-river cells **only for geographical grouping**.
   Physical fit retains actual surface water. Earlier
   [890001993-r90-p901](rejected/890001993-r90-p901.json.gz) is explicitly rejected:
   dry-component labels had separated North Outpost and Bastion on the same
   geographic island. The saved replacement p1002 and the independent audit use
   the corrected definition.
2. Fountains on two substantial islands, three distinct defensive islands per
   team, a 135% modeled alternate-route corridor and Lair distance ranking are
   **Prototype/test search preferences**. No universal one-structure-per-island
   balance rule or final numerical Type thresholds were adopted.
3. Shared acceptance includes actual source Archipelago features, contested-land
   symmetry, accepted independent Homebase quality, compact hinterland, strict
   Outpost/Bastion/Spike advance order, own practical-cost half/rival reach caps,
   physical pad/column and levelling checks, Lair usable-ground/open-volume/
   approach/access proxies, opening ceilings/floors and resource validity. The
   search adds global 175-block spacing; it does not relax those acceptance gates.
4. The 5,000-seed follow-up retained 225 coarse morphology windows, 70 refined
   sequence prospects and 22 with approximate symmetry. Six actual vanilla
   targets failed to yield the tested endpoint-constrained layout. These biased
   counts are not an unbiased rarity estimate or a proof of impossibility.
5. A native ocean-biome raster does not certify actual dry land: rivers, lakes,
   narrow channels and actual terrain can materially change the islands. The
   straight-projection morphology rank can miss curls and reward zigzags. Visual
   review remains necessary; no Hawaii-pattern approvals are claimed.
6. Actual Giant/Ghast/Dragon encounter collision and flight volumes, calibrated
   boat/discovery times, wider territorial balance and equipment-sufficient
   accessible-iron ceilings remain Open/unverified. All records have READY false;
   these are unmodified official vanilla source worlds, not authored playable maps.

## Source and reproduction

`maps.md` contains only the Archipelago Working addition in this branch. The
audit and reconciliation records explain the design status. The 24 Python files
under `tools/analysis/` include archipelago entrypoints and their local helper
dependencies. Existing `vanilla_search`, `terrain_harvest` and serialization code
comes from the branch base. The unrelated local compiler runtime-bindings diff
is intentionally excluded; historical compiler hashes are retained in evidence.

Use Python with NumPy and Matplotlib, a C toolchain and Java 21. The native probe
requires Cubiomes pinned at `ca9fcb4e2b4f29a88b15a19fe1f00a73bf503ec2`.
Build its `libcubiomes.a`, then compile
`implementation/worldgen/native/cubiomes/pale_gameplay_probe.c` against that
library with `-O2`, its include directory, and `-lm`. Place the executable at
`artifacts/worldgen/pale_gameplay_2026-10-12/build/pale_gameplay_probe`.

The pipeline expects the official Java 1.21.11 dedicated-server jar at
`artifacts/worldgen/seed_reevaluation_2026-10-02/build/server-1.21.11.jar`, SHA-1
`64bb6d763bed0a9f1d632ec347938594144943ed`, and DataVersion 4671. Configure the
Java path in the analysis helper for your platform. The acquire helper prepares
the shared vanilla runtime. Generation must use Mojang's applicable EULA.

Run from the checkout root. These are deliberate work entrypoints, not tests:

```sh
python tools/analysis/search_archipelago_windows.py
python tools/analysis/census_sequential_archipelagos.py --start 890002001 --count 5000
python tools/analysis/search_sequential_windows.py
```

The first two commands recreate native caches; the third refines the shape
screen. To inspect archived full evidence without running world generation:

```sh
python -m gzip -d new-four/qualified-summary.json.gz
```

Run that decompression command from this bundle directory or supply its full
path. It writes an uncompressed copy; keep the tracked compressed original.
Actual generation/compilation requires the prospect JSON and source grid layout
expected by the scripts. The archived target `result.json` files retain selected
centres/bounds. Reconstruct a prospect list from their `screen` objects, then:

```sh
python tools/analysis/generate_archipelago_prospects.py --seeds 890002226 890002716 --prospects /path/to/reconstructed-prospects.json --generate-only
python tools/analysis/stock_archipelago_bank.py --seeds 890002226 890002716 --budget 6 --island-chains --shape-prospects /path/to/all-window-fine.json
```

The second command's endpoint constraints use the decompressed archived fine
shape JSON. Crop searches use `crop_archipelago_sources.py --seeds ... --extract`
and `stock_archipelago_bank.py --source-root ... --variant-offset 2000`.
Some historical analysis scripts execute their work at module import; review
them before importing. Import checks here target only side-effect-light helpers.

**Raw-data exclusions:** official jars/worlds, third-party libraries/binaries,
native raster caches, bulk surface/candidate grids and most intermediate trials
are intentionally omitted. The bundle is reviewable evidence and source code,
not a self-contained rerun of every world. Archived absolute local paths remain
for provenance and must be remapped on another computer. Historical report
snapshots may refer to excluded raw report trees; the links above are the curated
entrypoints. Existing full local archives are preserved.

## Validation

[Native/actual paired calibration](validation/native-actual-calibration.json)
compares 30 existing, mostly archipelago source grids at 30,720 identical X/Z
positions. Median absolute height error is 3Y overall and 2Y on jointly dry
samples; p90 overall error is 7Y. Of 12,509 native non-ocean samples, 3,474 are
actual surface water. The biome breakdown is retained. This selection-biased
calibration explains proxy uncertainty; it does not add candidate counts or
change acceptance thresholds. Large paired NPZ arrays are excluded.


[Exact packed-section equivalence](validation/exact-count-validation.json)
records 72 synthetic and 168 actual vanilla sections. The separate
[full-window comparison](validation/exact-count-full-window-validation.json)
confirmed the entire opportunity summary remained equal. This acceleration is
local to analysis; the shared compiler is not modified.

[Review-checks](validation/review-checks.json) records syntax/import/native C
checks in the isolated checkout, fresh synthetic-count comparisons against its
base, gzip/manifest checks and independent spacing reconstruction for all nine
saved layouts. Missing excluded worlds prevent a fresh full source-world rerun.
