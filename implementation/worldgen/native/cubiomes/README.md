# The off-server seed probe and scanner

Both use [cubiomes](https://github.com/Cubitect/cubiomes), which reimplements
Minecraft's worldgen as a pure function of the seed. No server, no chunks, no
world.

`mobaprobe.c` samples the compiler's fixed window and prints JSON -- fine for
one window, hopeless for a scan, where formatting costs more than generating.

`mobascan.c` covers a large square and writes a binary grid of height and
biome. This is what `terrain_harvest.scoop` searches over, and until it
existed every scoop measurement ran on `feature_grid.height_rle` from worlds
the old pipeline had ALREADY generated -- so the search could only re-read its
own past choices.

    git clone --depth 1 https://github.com/Cubitect/cubiomes.git
    cd cubiomes && make libcubiomes
    cp <this dir>/mobaprobe.c <this dir>/mobascan.c <this dir>/mobastruct.c .
    cc -O2 -o mobaprobe  mobaprobe.c  libcubiomes.a -lm
    cc -O2 -o mobascan   mobascan.c   libcubiomes.a -lm
    cc -O2 -o mobastruct mobastruct.c libcubiomes.a -lm
    export MOBA_CUBIOMES_PROBE=$PWD/mobaprobe
    export MOBA_CUBIOMES_SCAN=$PWD/mobascan
    export MOBA_CUBIOMES_STRUCT=$PWD/mobastruct

`mobastruct.c` locates Overworld structures. Placement is a 48-bit LCG seeded
per region, so a candidate position is arithmetic rather than generation.

It is TWO steps and the second is not optional. `getStructurePos` says where a
structure would go in its region; it only generates if the biome accepts it.
Every region of the world holds a Mansion candidate and almost none hold a
Mansion, so emitting step one alone is noise, not an approximation.
`isViableStructurePos` is applied before anything is printed.

The reported biome is `getBiomeAt` at the surface, NOT `mapApproxHeight`'s id
output. That output is the biome of the sampled column and can be a cave
biome: three Mansions on seed 31337 first reported 175 (lush_caves), 29 and
175 when all three stand in dark_forest (29).

## Cost

An 8192-block square at 32-block sampling -- 65,536 cells -- scans in about
one second. Searching 25,088 candidate scoops over it takes another 0.3s.
All thirteen structure kinds over the same square take 0.10s. Generating that
area on a server is hours.

## Failing open, and not

`seed_screen` keeps every seed when `mobaprobe` is absent: the screen is an
optimisation and skipping it costs throughput, never correctness.

`terrain_harvest.scan` does the opposite and raises. A scoop search with no
scan has nothing to search, and an empty result would read as "this seed has
no symmetric region" when it means "nothing looked".

## Is the approximate height good enough to rank symmetry?

Measured, not assumed. On seed 930015734 over the exact rectangle that seed's
generated candidate covers, the mirror measure reads 22.6 deviation / -17.3
tilt / 20.3 residual from the GENERATED world and 21.0 / -18.3 / 18.8 from
cubiomes -- within 8%. So the approximation does not systematically flatten
the measure, and searched scoops scoring far better than generated finalists
(3.9 to 6.5 against 13 to 27) is a real difference and not an artefact of
reading smoother terrain.

Sampling resolution was ruled out the same way: 32, 16 and 8-block steps over
the same seed give best deviations of 6.02, 5.26 and 5.52.

`terrain_harvest.seed_screen` finds it on `$MOBA_CUBIOMES_PROBE` or `$PATH`, and
**keeps every seed when it is absent**. Building this is an optimisation; not
building it changes throughput, never correctness.

## What it is trusted for

Biome only. Measured against 61,479 samples over 69 generated worlds, the
surface height is unbiased (mean -0.27) but heavy-tailed: 81% of cells within 5
blocks, 93% within 10, and outliers past 100 blocks where deep ocean diverges.
The height is emitted for diagnosis and is deliberately **not** screened on.

## Version

Built against cubiomes' `MC_NEWEST`, which is `MC_1_21_WD`. The worlds it was
validated against are Paper 1.21.11. Biome generation is believed unchanged
across that gap, and the zero false negatives over 69 seeds is the evidence for
it -- but it is a gap, and a future Minecraft that moves biome generation would
invalidate the screen silently. Re-run the validation when the server version
changes.
