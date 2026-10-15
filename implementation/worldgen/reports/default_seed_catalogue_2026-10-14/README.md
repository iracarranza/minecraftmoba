# Broad Default-ish native seed catalogue — 14 October 2026

This is a reusable **approximate measurement catalogue**, not a list of qualified
maps. Every seed, region and orientation measured by this lane is retained,
including all preference misses. Rare map types and full gameplay qualification
are deferred. This lane starts at seed **910000001**, separately from the older
900000001+ loose Archipelago search and the other parallel work.

The Default direction in `maps.md` motivates recording an eastward ocean
gradient, western elevation/cold geography, forest/open-land distribution and
possible natural Homebase space. These are descriptive vectors. No catalogue
preference becomes a new canonical requirement or discards a record.

## Coverage and counting

Each seed receives one native Cubiomes height/biome grid over a 6,144-block square
centred at X/Z 0, sampled every 64 blocks. The raw grid is retained.

- Four square region sizes: 1,024, 1,536, 2,048 and 3,072 blocks.
- Twenty-five offsets per size: a five-by-five placement grid inside the source.
- Four clockwise orientations per region: 0°, 90°, 180°, 270°.
- Therefore **100 distinct regions and 400 orientation records per unique seed**.

Regions overlap. Orientations, regions and unique seeds must be reported
separately; 400 orientations do not mean 400 seeds or independent geographies.

## Retained data

[checkpoint.json](checkpoint.json) records live progress, next seed, worker PID,
exact command, source hashes, disk/time bounds and singleton lock location.
[catalogue.sqlite](catalogue.sqlite) is the authoritative query index. A committed
seed has all 400 window measurements on disk before its index transaction commits.

`seed-index.jsonl` is a compact integration feed, one summary per committed seed.
`seeds/{seed}.json` contains the same summary. `windows/{seed}.jsonl.gz` contains
all 400 full records for that seed, including:

- Bounds, size, rotation, sampling resolution and stable window identity.
- Whole-region biome histogram and overlapping biome-family fractions.
- West/east regional vectors and a seven-strip west-to-east profile.
- Elevation quantiles and relief, ocean-biome share/gradient, forest, open-land,
  alpine, cold, arid, wet and river shares.
- Coarse flat **non-ocean** samples at both team ends and team-end non-ocean share.
- Team-axis raw mirror deviation/relief, measured on paired non-ocean samples.
- Every descriptive preference, its measured pass/miss, and every unknown check.

The raw `palegameplay 1` binary grids are under
`artifacts/worldgen/default_seed_catalogue_2026-10-14/probes/`. They retain
unclamped approximate heights and native biome IDs, so future looser or different
requirements can be measured without rerunning the probe. Analysed heights are
clamped to sea level 63. The first two smoke seeds predate the explicit river
family field; their complete raw biome grids retain those measurements.

The SQL `windows` table carries compact numeric columns plus `payload_file` and
one-based `payload_line`, pointing to each full compressed record. The `seeds`
table records counts and source paths. For example, this retrieves loose regional
prospects without reinterpreting the retained full evidence:

```sql
SELECT seed, xmin, zmin, size, rotation, preference_count,
       ocean_gradient, western_height_advantage, flat_north, flat_south,
       payload_file, payload_line
FROM windows
WHERE preference_count >= 3 AND flat_north >= 4 AND flat_south >= 4
ORDER BY preference_count DESC, MIN(flat_north, flat_south) DESC
LIMIT 100;
```

## What the data does not prove

**Ocean-biome share is not actual surface-water share.** Fields named `dry` or
`land` are retained compatibility names for a non-ocean-biome proxy. Rivers,
lakes and other inland surface water can be included. Coarse flat samples do not
prove actual dry pads, space for a Homebase Core, or constructible terrain.

[The paired vanilla calibration](../catalogue_proxy_calibration_2026-10-14/calibration.json)
measures this approximation against existing generated terrain. Native heights
and biome classifications guide later sampling, not final qualification.

Nothing here establishes the nine-anchor 175-block spacing requirement, viable
routes/hinterlands, defensive placement, Lair access/physical volume, resources,
opening ceilings, physical column fit or runtime readiness. All records carry
`qualified: false` and `ready: false`. No playable worlds are authored.

## Worker and continuation

The worker uses two native-analysis threads and a lifetime `fcntl.flock` on
`worker.lock`; a duplicate worker exits before touching the database. It commits
each pair of seeds and atomically replaces its checkpoint. Interrupted native
grids and measurement files use temporary paths. Fully written cached seed files
can be ingested after interruption without resampling.

The exact resumable command, from the repository root, is:

```sh
artifacts/worldgen/pale_mansion_search_2026-10-12/build/plot-env/bin/python \
  tools/analysis/catalogue_default_seeds.py \
  --start 910000001 --max-seeds 100000 --hours 4.5 --workers 2 --max-storage-gib 4
```

An existing checkpoint determines the next seed. The worker stops at the first of
4.5 hours, the seed limit, an aggregate 4 GiB disk budget, a `STOP` file or a
termination signal. Scheduled continuation should inspect the existing PID and
lock first, resume a stopped lane from this checkpoint, and report a storage stop
without silently removing the bound. Logs are in `worker.log`; the initial
detached launch is recorded in `worker-launch.json`.

[Smoke validation](smoke-validation.json) confirmed four unique seeds, 400 distinct
regions and 1,600 retained orientation records. Eight mirror measurements matched
the existing `terrain_harvest.scoop.symmetry` implementation, and all records
remained unqualified. The live checkpoint supersedes that initial count.
