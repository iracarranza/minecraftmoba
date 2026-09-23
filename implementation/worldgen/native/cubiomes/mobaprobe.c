/* Sample Overworld biome + approximate surface height over the compiler's own
 * window, and emit JSON. One process per seed; no world is generated. */
#include <stdio.h>
#include <stdlib.h>
#include "generator.h"
#include "biomenoise.h"

int main(int argc, char **argv) {
    if (argc < 2) { fprintf(stderr, "usage: mobaprobe <seed> [step]\n"); return 2; }
    uint64_t seed = strtoull(argv[1], NULL, 10);
    int step = argc > 2 ? atoi(argv[2]) : 8;

    /* The candidate window: chunks -27..26 x, -33..32 z => blocks. */
    int x0 = -27*16, x1 = 26*16 + 15;
    int z0 = -33*16, z1 = 32*16 + 15;

    Generator g;
    setupGenerator(&g, MC_NEWEST, 0);
    applySeed(&g, DIM_OVERWORLD, seed);
    SurfaceNoise sn;
    initSurfaceNoise(&sn, DIM_OVERWORLD, seed);

    printf("{\"seed\":%llu,\"step\":%d,\"x0\":%d,\"z0\":%d,\"cells\":[",
           (unsigned long long)seed, step, x0, z0);
    int first = 1;
    /* mapApproxHeight works at 1:4, so ask for a 1x1 cell at each sample. */
    for (int z = z0; z <= z1; z += step) {
        for (int x = x0; x <= x1; x += step) {
            float y = 0; int id = 0;
            mapApproxHeight(&y, &id, &g, &sn, x >> 2, z >> 2, 1, 1);
            if (!first) printf(",");
            first = 0;
            printf("[%d,%d,%.1f,%d]", x, z, y, id);
        }
    }
    printf("]}\n");
    return 0;
}
