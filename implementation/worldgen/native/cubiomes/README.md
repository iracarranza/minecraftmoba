# The off-server seed probe

`mobaprobe.c` samples Overworld biome and approximate surface height over the
candidate window using [cubiomes](https://github.com/Cubitect/cubiomes), which
reimplements Minecraft's worldgen as a pure function of the seed. No server, no
chunks, no world.

    git clone --depth 1 https://github.com/Cubitect/cubiomes.git
    cd cubiomes && make libcubiomes
    cp <this dir>/mobaprobe.c .
    cc -O2 -o mobaprobe mobaprobe.c libcubiomes.a -lm
    export MOBA_CUBIOMES_PROBE=$PWD/mobaprobe

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
