# Terrain harvest + gallery substrate

**Working / Prototype-test, 18 September 2026.** See the
[authority reconciliation](../../../docs/reconciliation/2026-09-18-terrain-harvest.md).
All tooling is committed/pushed before corpus outputs are generated. Python
standard library only. Use an offline source snapshot; never open source worlds
while exporting. Existing worlds are local ignored artifacts, not downloadable
from Git. The staged world-generation runner remains their reproduction source;
for exact snapshot reproduction retain the hashed world files as well as seeds.

## Reference representation

`terrain_volume/1` JSON has:

- `id`: SHA-256-derived reference identity over schema, provenance and boundary;
  not just the seed. Reclassification, added measurements and placement do not
  rename the source selection. Actual source-file hashes are pinned at export.
- `provenance`: integer seed, dimension, inclusive X/Y/Z source bounds, Minecraft
  version/DataVersion, known worldgen settings (explicit UNRESOLVED if not read),
  source candidate path and SHA-256. Export additionally records level.dat hash,
  complete actual worldgen settings and every consumed region/entity file hash.
- `classification`: `whole_map`, `near_miss_map`, `large_section`, `local_section`,
  geographic tags and named failed/unresolved criteria. Missing evidence never
  becomes a pass. Whole-map here is an existing finalist retention category.
- `transform`: clockwise quarter-turn about source block-coordinate origin,
  then integer XYZ translation. Forward/inverse geometry is implemented; the
  materializer deliberately rejects nonidentity placement rather than corrupt
  directional blocks, POIs, structure data or embedded entity positions.
- `boundary`: `geometry_type`, explicit parameters, versioned inclusion rule.
- `measurements`: separately labeled RAW WORLD OBSERVATION, DERIVED MEASUREMENT,
  ANALYTICAL FIXTURE, INTERPRETATION, UNRESOLVED. Imported candidate-wide metrics
  stay candidate-wide; they are not measurements of a clipped section.
- `connection_interfaces`: optional observational extension list, initially empty.
  Future entries can contain boundary segment, elevation/profile, width, medium
  (land/water/cave/mixed), natural context and evidence provenance. Existing surface
  samples can describe coarse elevation/water contacts; block decoding can later
  measure edge air/fluid/solid profiles. Neither proves compatible ports or seams.

## Finite selection geometries

All inclusion tests use integer block coordinates and inclusive bounds. Box
includes every cell in XYZ bounds. Ellipse uses the bounds' midpoint and half
widths in X/Z, constant through the explicit Y interval. Scoop uses the same
ellipse scaled by a piecewise-linear `radius_scale_by_y` curve. Scale values
are exact rational strings; ordered Y knots must cover the full interval, with
positive nondecreasing scales at most one. This produces a contained floor and
narrower deep footprint. It does not move any retained block.

The demonstration's [-64, 1/4], [0, 3/4], [48, 1], [319, 1] knots and 32/128-block
section half-widths are **inspection fixtures**, not map requirements or balance.
Masks are deterministic and replaceable later with versioned polygon/custom
adapters. Tests specifically exclude underground lateral points retained at
surface height.

## Retention and evidence

`catalog` imports all eight existing staged finalists and references their latest
Opportunity SHADOW fits. It preserves existing Stage C metrics, hard failures,
manual weaknesses and unresolved gameplay validation. A record with Stage C
hard failures becomes a near miss; no such failure is invented for current
finalists. Tests exercise near-miss retention with labeled synthetic fixtures.
The initial proof uses 930010639 and 930012642 as alternate whole candidates,
plus a scoop around the documented 930010639 alpine-highland centroid [1940,364].
An additional larger elliptical section stays reference-only. Any number of
explicit manifests can refer to the same source seed. Existing scores are quoted
only inside source evidence, never combined into a new harvest score.

## Materialization and containment

One source-coordinate-preserving volume per **void custom dimension** is cheaper
and safer than implementing arbitrary state relocation. All dimensions, including
unused vanilla hosts, have empty flat generators, so exploring outside copied
chunks cannot produce surrounding land/ocean. A source-derived level.dat creates
an independent gallery save. A small authored hub lives outside harvested terrain.

A one-cell Chebyshev-dilated exterior envelope encloses every side, corner, top
and bottom: visible bedrock on sides/floor, transparent unbreakable barrier on
roof. The inspection dimension has min Y=-80 and height=416, giving room outside
the full source interval [-64,319] for floor at -65 and roof at 320. Natural
blocks inside the mask, including original bedrock, remain untouched. Walking,
swimming, boats, lateral cave tunneling, digging down and building/climbing over
meet a closed block envelope. No 2D world border is used. Normal navigation sets
Adventure mode. Spectator and commands intentionally bypass containment; this is
not a security boundary against administrator actions or teleport exploits.

Water and waterlogged properties inside the mask are copied just like rock;
offshore ocean stays inside the volume, with void beyond the envelope. There is
no host ocean. Source scheduled block/fluid ticks are retained where in bounds;
source lighting is removed for recalculation. Run `/tick freeze` in the hub **before visiting** for a static snapshot. The
command requires administrator permission and cannot run in the default
permission-level-2 load function. Random ticks and mob spawning are disabled. `/tick unfreeze` enables simulation for a disposable traversal
session. Fluid flow, leaves, gravity, neighbor updates and structure behavior may
then diverge at clipped edges. No guarantee of long-term fluid equilibrium.

Preserved: exact in-mask palette states/properties, air/caves, ores, vegetation,
structure blocks, biomes at native quart resolution, block-entity NBT and wholly
contained entity/passenger trees. Missing/non-full source chunks fail rather than
becoming air. Clipped block entities and scheduled ticks are filtered by position.
Entity roots with an out-of-mask passenger are excluded and counted. External
UUID/leash/brain references and embedded dimension references are not repaired.
Zero-byte optional entity-region placeholders contain no serialized records and
are recorded separately; nonempty truncated regions still fail. Structure starts/references and POI
registries are omitted: associated spawning rules and villager POI behavior are
not preserved. Partial structures may be cut. Cross-dimension entity identities
may duplicate between overlapping selections; volumes are isolated inspection
copies, not a composable entity pool. Non-overworld source adapters and compressed
external Anvil chunks are unsupported. The existing codec supports zlib regions.

## Reproduce (repository root)

First run tests and commit/push all methodology; then generate outputs:

```sh
PYTHONPATH=implementation/worldgen python3 -m unittest discover -s implementation/worldgen/tests -p test_terrain_harvest.py
python3 implementation/worldgen/harvest_terrain.py catalog --output implementation/worldgen/reports/terrain_harvest_2026-09-18/library
python3 implementation/worldgen/harvest_terrain.py gallery \
  --library implementation/worldgen/reports/terrain_harvest_2026-09-18/library --proof \
  --source 930010639=/Users/iracarranza/minecraftmoba/artifacts/worldgen/staged_default_2026-09-09/worlds/Default_930010639_2048_0 \
  --source 930012642=/Users/iracarranza/minecraftmoba/artifacts/worldgen/staged_default_2026-09-09/worlds/Default_930012642_-2048_0 \
  --output artifacts/worldgen/terrain_harvest_2026-09-18/worlds/TerrainGallery
python3 implementation/worldgen/harvest_terrain.py verify \
  --gallery artifacts/worldgen/terrain_harvest_2026-09-18/worlds/TerrainGallery \
  --source 930010639=/Users/iracarranza/minecraftmoba/artifacts/worldgen/staged_default_2026-09-09/worlds/Default_930010639_2048_0 \
  --source 930012642=/Users/iracarranza/minecraftmoba/artifacts/worldgen/staged_default_2026-09-09/worlds/Default_930012642_-2048_0 \
  --report implementation/worldgen/reports/terrain_harvest_2026-09-18/readback.json
```

Paths after `--source` are local bindings, not reference IDs. Supply other offline
copies on another computer. Export to a fresh directory: overwrite is rejected.
Use repeated `--id tv_...` instead of `--proof` for on-demand selection. No implicit
full-library export. Failed builds retain `INCOMPLETE` and lack a completed gallery
index. Materialization status is separate from immutable library references.

Optional matching-server test (requires existing EULA acceptance):

```sh
curl -L https://piston-data.mojang.com/v1/objects/64bb6d763bed0a9f1d632ec347938594144943ed/server.jar -o /tmp/terrain-server-1.21.11.jar
PYTHONPATH=implementation/worldgen python3 -m terrain_harvest.server_probe \
  --world artifacts/worldgen/terrain_harvest_2026-09-18/worlds/TerrainGallery \
  --jar /tmp/terrain-server-1.21.11.jar \
  --java '/Users/iracarranza/Library/Application Support/minecraft/runtime/java-runtime-delta/mac-os-arm64/java-runtime-delta/jre.bundle/Contents/Home/bin/java' \
  --eula /Users/iracarranza/minecraftmoba/artifacts/worldgen/staged_default_2026-09-09/build/930010639_2048_0/eula.txt \
  --report implementation/worldgen/reports/terrain_harvest_2026-09-18/server.json
```

This probes floor/roof blocks in each dimension and validates function parsing in
a disposable world copy. It is not evidence of a human client walkthrough.

## Open and navigate

Copy `TerrainGallery` into Minecraft's `saves` directory, open with **Java 1.21.11**
and commands enabled. First run `/tick freeze`, then `/function harvest:hub`, `/function harvest:index`,
`/function harvest:next`, `/function harvest:previous`, or
`/function harvest:visit/<TerrainVolume ID>`. Index messages offer clickable visits
and show seed, source bounds, classification, rotation and evidence path. Detailed
pass/fail/unknown evidence is retained in each dimension's `terrain_volume.json`.
`gallery.json` contains actual interior standing targets, found from copied blocks.
`/function harvest:overview/<ID>` enables external spectator inspection; `visit`
restores normal Adventure inspection. No polished UI or gameplay systems.

## Independent preservation audit

`preservation.py` recomputes expected destination state from the source snapshot
and the mask alone. It never calls the exporter, so an exporter defect cannot
certify itself. Fully retained sections are compared as whole typed NBT
containers; clipped sections are compared cell by cell. Block entities and
scheduled ticks are compared by position and typed identity; entity roots by
typed-identity multiset, with each exclusion attributed to the root or to a
passenger leaving the mask. `fingerprint()` preserves NBT type and list-kind
distinctions that `plain()` collapses.

Coverage is reported literally rather than implied. Counters are pre-seeded so a
missing key cannot read as full coverage, and `exhaustive` is false whenever
`--max-clipped-sections` skipped anything. The audit does **not** cover lighting
recomputation, structure/POI behaviour after load, simulation once unfrozen, or
client rendering; every report lists these under `not_covered`.

```sh
PYTHONPATH=implementation/worldgen python3 -m terrain_harvest.preservation \
  --gallery artifacts/worldgen/terrain_harvest_2026-09-18/worlds/TerrainGallery \
  --source 930010639=/Users/iracarranza/minecraftmoba/artifacts/worldgen/staged_default_2026-09-09/worlds/Default_930010639_2048_0 \
  --source 930012642=/Users/iracarranza/minecraftmoba/artifacts/worldgen/staged_default_2026-09-09/worlds/Default_930012642_-2048_0 \
  --output implementation/worldgen/reports/terrain_harvest_2026-09-18/preservation-audit.json
```

Add repeated `--id tv_...` to audit selected volumes. Exit status is nonzero on
any failure, and failing runs still write the report.

## Remaining work

Manifest validity, deterministic masks, copied world output, server loading and
client inspection are distinct evidence stages. A report must state which were
actually achieved. The smallest next composition step is a read-only boundary
profile export on a human-chosen pair, retaining raw solid/air/water/elevation
observations and unresolved compatibility. Then implement and test state-aware
relocation on tiny fixtures before attempting any stitching. Recompute derived
travel/economic evidence after geometry changes; never transplant candidate-wide
fitness onto a cropped section or composite.

Arbitrary retained sections and near misses can be added without changing the
corpus adapter. For example (near-miss criteria must be real review findings,
not an invented score):

```sh
python3 implementation/worldgen/harvest_terrain.py reference \
  --candidate implementation/worldgen/results/staged_default_2026-09-09/finalists/930010639/candidate.json \
  --bounds 1908 1972 -64 319 332 396 --classification local_section \
  --geometry scoop --scoop-knots '[[-64,"1/4"],[0,"3/4"],[48,"1"],[319,"1"]]' \
  --tag western_highland --output /tmp/my-terrain-library
```

Use `--classification near_miss_map --criterion 'specific failed/unresolved criterion'`
for reviewed near misses, or `large_section` for a large subsystem. The command
refuses selections outside the candidate's observed bounds, and whole-map
retention without explicit Stage C evidence. `gallery` discovers all `tv_*.json`
references in its input library, including individually added references.

## Read-only boundary transects

The [autonomous follow-up plan](NEXT_STEPS.md) records the full engineering queue.
The first tool observes four cardinal vertical transects in the pinned **natural
source**, never the artificial gallery shell. The default 16-block vertical step
is an explicit sampling fixture; top and bottom are included. Exact block
properties are retained. No collision, cave connection, port width, medium
traversability or seam score is inferred. Source hashes must match the existing
materialization record before and after observation. Missing observations fail.

```sh
PYTHONPATH=implementation/worldgen python3 -m terrain_harvest.profiles \
  --gallery artifacts/worldgen/terrain_harvest_2026-09-18/worlds/TerrainGallery \
  --source 930010639=/Users/iracarranza/minecraftmoba/artifacts/worldgen/staged_default_2026-09-09/worlds/Default_930010639_2048_0 \
  --source 930012642=/Users/iracarranza/minecraftmoba/artifacts/worldgen/staged_default_2026-09-09/worlds/Default_930012642_-2048_0 \
  --vertical-step 16 \
  --output implementation/worldgen/reports/terrain_harvest_2026-09-18/boundary-profiles.json
```

Use a fresh output path for repeats. Profiles are observations and candidate
interface groundwork, not a recommendation to join the sampled volumes.
