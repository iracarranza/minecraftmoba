# SHADOW terrain harvest + gallery proof — 18 September 2026

**Working / Prototype-test. No Default winner, reselection, gameplay or balance change.**

The reference library **and real block-level world outputs exist**. Three finite
volumes were generated, independently decoded, and loaded in the official
Minecraft Java 1.21.11 server. **A graphical client walkthrough was not performed.**
The launcher warned that another session was active and continuing could lose
that session; the duplicate launch was canceled. Server probes are not represented
as a human inspection, traversal test, or proof of playable-map quality.

## Repository and delivery

Repository: `https://github.com/iracarranza/minecraftmoba.git`.
Original checkout: `/Users/iracarranza/minecraftmoba`, branch `main`, starting
commit `be06814fbcbe8bf8e62386ef591bf48e296faf17`, with pre-existing local edits.
The required remote/root/status/log checks preceded changes. Fetch confirmed
`origin/main` at `9c85c915599f410cdadf26dbc76de9b7b9b55220`.

Implementation branch: `terrain-harvest-gallery-2026-09-18`, based on that current
remote commit, in `/tmp/minecraftmoba-terrain-harvest`. The initial complete
methodology was pushed as `b8edaa4` before corpus outputs. Follow-up fixes were
pushed before regeneration; the successful proof uses tooling at `7608675`.
Outputs are committed separately after the tooling. Bulk worlds remain ignored
under the repository's established artifact policy.

Canonical documents, active fitter/selector code, frozen Handoffs, Opportunity
Relationships and historical analyses were not edited. The original checkout's
existing changes remain. In particular, its untracked economic SHADOW report
(SHA-256 `eb9ff93fbdcc1e48fe1b574065b42516fce3b58cb4fd7d6a243114209acb0ad3`)
and tooling were read and preserved, not silently imported into this branch.

See [authority reconciliation](../../../../docs/reconciliation/2026-09-18-terrain-harvest.md)
for the current/old source distinctions and
[tooling contract and exact reproduction commands](../../terrain_harvest/README.md).

## Capability, representation and retention

Existing official-server world generation, eight-block terrain extraction,
finalist/fitter JSON, NBT/Anvil codecs, palette packing, Task B preservation audits
and the local ore-observation SHADOW work were found. The natural worlds already
existed; the earlier synthetic world serializer was not used to regenerate them.

`terrain_volume/1` contains stable source-selection ID; seed/dimension/version,
XYZ bounds, settings status and hashed candidate provenance; classification and
named criteria; physical transform; deterministic boundary; labeled evidence;
and an optional observational connection-interface list. Full settings and
consumed source-file hashes are pinned in each export record. The copied world
is not the sole authority. Logical fitter orientation remains distinct from
physical rotation, which is zero in this proof.

Box, elliptical cylinder and depth-narrowing scoop masks are implemented. Bounds
are inclusive integer cells. The scoop's explicit rational radius/Y knots are
inspection fixtures, not design percentages. Inside the mask the exporter keeps
source block state rather than sculpting a bowl. Quarter-turn coordinate transforms
and their inverses are tested; physical state-aware rotation/translation remains
unsupported and fails explicitly.

The library contains ten references: all eight existing whole-map finalists,
a large elliptical highland-section proposal, and a smaller scoop. Whole-map
means retained structural candidate, not passed physical/economic validation.
Current finalists have no recorded Stage C hard failures, so this proof does not
invent a near-miss verdict. The reusable `reference` command retains arbitrary
reviewed sections and near misses with named criteria. Tests cover missing
Stage C evidence, real failure fields and complete-versus-near-miss retention.
No missing evidence is promoted to success and no new quality score is added.

## Materialized proof

| TerrainVolume ID | Source | Category / boundary | Written chunks | Block entities | Entity roots retained |
|---|---:|---|---:|---:|---:|
| `tv_51a79e3b1bee05ea7559e799` | 930010639 | whole-map candidate / box | 3,808 | 1,966 | 579 |
| `tv_ef56852eda10acc88342e5ee` | 930012642 | alternate whole-map candidate / box | 3,808 | 1,164 | 809 |
| `tv_49b22bb49e0684c3a1295570` | 930010639 | local highland section / scoop | 25 | 19 | 0 |

Whole-map source X/Z bounds are respectively `[1616,2479] × [-528,527]` and
`[-2480,-1617] × [-528,527]`. The scoop uses `[1908,1972] × [332,396]`, around the
documented alpine-highland centroid. All three retain source Y `[-64,319]`.
The larger ellipse `tv_e480a5d29b59fe9e319ad514` remains reference-only.

These are current staged inspection snapshots. Exact equivalence to pristine
worldgen is unproven. No new world generation, authored systems or terrain repair
was needed. Source-file hashes explain the actual extraction snapshot.

## Isolation, containment and water

Each materialized volume occupies a separate void custom dimension at original
source coordinates. Empty flat generators also replace unused host dimensions;
unrelated terrain or an ocean cannot continue beyond the harvested object.

A generated exterior bedrock shell closes lateral faces, curves and the bottom;
a transparent barrier roof closes the top. A 26-neighbor dilation seals corners.
Custom inspection dimensions extend from Y=-80 through 335 so full source-depth
terrain can retain its original floor and have containment outside it at -65 and
320. This is actual written block containment, not a 2D border or metadata-only
specification. Ordinary walking/swimming/boating/building/digging paths encounter
this envelope; no exhaustive live movement exploit test was performed. Spectator,
administrator commands and teleport exploits are explicitly outside that claim.

Water, waterlogged properties and scheduled fluid ticks are copied inside the
mask. The exports retain 14,304 fluid ticks and 418,800 block ticks in total.
Run `/tick freeze` in the hub **before visiting** for snapshot inspection; `/tick
unfreeze` allows a disposable traversal/simulation session. Random ticks and mob
spawning are disabled. Fluids, gravity blocks, leaves and other neighbor-sensitive
state may evolve at clipped boundaries when simulation resumes.

## Preservation limits

Retained source palette states include caves/air, ores, geology, vegetation,
fluids and structure blocks. In-mask block-entity NBT and wholly contained entity
/passenger trees are copied. Excluded entity-root counts are 14, 56 and 5 for the
table's rows; these include out-of-mask roots, not just crossing passengers.
Zero-byte optional entity files are hashed and separately listed. Nonempty
truncated regions, wrong versions and missing/non-full source chunks fail.

Structure starts/references and POI registries are omitted; structure-specific
spawns, village POI behavior, cross-boundary structures, external UUID/leash/brain
links and embedded dimension references are not fully preserved. Retained entity
counts prove serialized records, not behavioral correctness. Biomes retain their
native quart resolution. Lighting/heightmaps are recalculated by Minecraft.
Source adapters currently support Overworld, Java 1.21.11 and zlib Anvil. Physical
rotation/translation and arbitrary organic masks are not implemented.

## Gallery and local outputs

Completed pristine artifact, approximately 55 MB:

`/Users/iracarranza/minecraftmoba/artifacts/worldgen/terrain_harvest_2026-09-18/worlds/TerrainGallery`

A separate copy is installed at:

`/Users/iracarranza/Library/Application Support/minecraft/saves/TerrainHarvestGallery_2026-09-18`

Open **Terrain Harvest Gallery** using Java **1.21.11**, then:

```text
/tick freeze
/function harvest:hub
/function harvest:index
/function harvest:next
/function harvest:previous
/function harvest:visit/tv_51a79e3b1bee05ea7559e799
/function harvest:overview/tv_49b22bb49e0684c3a1295570
```

Visits set Adventure mode and use actual standing targets inside the mask;
overview explicitly sets Spectator mode. Index entries are clickable and expose
ID, seed, bounds, physical rotation, classification, recorded Stage C failures,
unresolved acceptance and provenance path. Detailed evidence stays in the manifest.
Interior targets are `[2032,77,-16]`, `[-2056,65,-16]` and `[1936,114,352]` for the
table's rows. No gameplay Routes, Fountains, resource balancing or other authored
game systems were placed.

The complete build command, source bindings, reference command, verifier and
server-probe invocation are preserved in the tooling README linked above. Use a
fresh output directory when reproducing. The source snapshots are local ignored
artifacts; another machine must supply the pinned snapshots or regenerate and
record different snapshot hashes. The installed save is a mutable inspection
copy; opening it is not expected to preserve the pristine artifact hashes.

## Verification and commands run

- `PYTHONPATH=implementation/worldgen python3 -m unittest discover -s implementation/worldgen/tests`:
  **142 passed**, including 16 harvest tests, in the final run.
- Original checkout's `test_economic_shadow.py`: **9 passed**; no source modifications.
- `harvest_terrain.py catalog`, `gallery --proof`, `verify`: completed.
- [Readback audit](readback.json): **38,192 probes passed**, including source-state,
  generated shell and exterior-air checks; every whole-map source chunk has geological,
  surface and sky probes. All consumed source hashes remained unchanged. This is
  sampled readback, not exhaustive equality of every voxel.
- [Official-server probe](server.json), with [log](server.log): clean load/exit,
  no errors, all **six actual floor/roof markers** observed across three dimensions.
  Navigation functions parsed; player-driven next/previous/click behavior remains
  unverified in a graphical client.
- Independent scoop export versus full gallery scoop:
  `diff -rq artifacts/worldgen/terrain_harvest_2026-09-18/worlds/TerrainGallery/dimensions/harvest/tv_49b22bb49e0684c3a1295570 artifacts/worldgen/terrain_harvest_2026-09-18/worlds/ScoopCheckFixed/dimensions/harvest/tv_49b22bb49e0684c3a1295570`
  returned exit 0: identical serialized files.
- `git diff --check` passed. Reference IDs/library regeneration matched saved JSON.
- The [initial failed server check](scoop-server-initial.json) is retained for
  traceability: missing required DragonFight field and an unauthorized datapack
  `tick` command were corrected. [Corrected scoop check](scoop-server-fixed.json)
  passed. Incomplete interrupted/failed world folders are not delivered as proofs.

## Files changed and next step

Source: `harvest_terrain.py`; `terrain_harvest/{model,library,materialize,gallery,
verify,server_probe,__init__}.py`; `tests/test_terrain_harvest.py`.
Documentation: worldgen README, terrain-harvest README, dated reconciliation.
Outputs: this report directory, ten library references/index, materialization
manifest, readback hashes and server reports/logs. Bulk world outputs are local.

The data-model port extension is observational only. Existing coarse surface,
water-network and region evidence can support preliminary edge profiles, and
block decoding can measure actual solid/air/fluid cross-sections. Neither proves
stitch compatibility. The smallest justified next step is a read-only interface
profile for a human-chosen pair, followed by tiny state-aware relocation fixtures.
Future composition can reference base/inserted volumes, transforms, seams and
separate authored game elements. Recompute travel, opportunity and economic /
Practical Reach analyses after assembly; no automatic stitcher or selection
promotion is included here.
