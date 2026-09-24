/* Scan Overworld approximate surface height + biome over a large square and
 * emit a binary grid. This is the input `terrain_harvest.scoop` searches over.
 *
 * `mobaprobe` samples the compiler's fixed window and prints JSON, which is
 * fine for one window and hopeless for a scan: a 2048-sample side is 4.2M
 * cells, and formatting those as text costs more than generating them. This
 * writes a fixed-width binary body after one text header line.
 *
 *   mobascan <seed> <half> <step> <out>
 *
 * covers x,z in [-half, half) at `step` blocks, so n = 2*half/step per side.
 *
 * FORMAT
 *   line 1, ASCII: "mobascan 2 <seed> <n> <half> <step>\n"
 *   body: n*n records, row-major with x fastest, z ascending
 *         int16 height (blocks, clamped)   little-endian
 *         int16 biome id                   little-endian
 *
 * Version 2. Version 1 emitted one uint8 of ocean/land per cell, which is all
 * `window_search` ever needed; height is what makes symmetry searchable, and
 * a bitmap could not carry it.
 *
 * TRUSTED FOR: biome exactly, height approximately. Against 61,479 samples
 * over 69 generated worlds the height is unbiased (mean -0.27) but
 * heavy-tailed -- 81% within 5 blocks, 93% within 10, outliers past 100 where
 * deep ocean diverges. Good enough to rank symmetry and locate flat ground,
 * NOT good enough to certify a build site.
 */
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include "generator.h"
#include "biomenoise.h"

int main(int argc, char **argv) {
    if (argc < 5) {
        fprintf(stderr, "usage: mobascan <seed> <half> <step> <out>\n");
        return 2;
    }
    uint64_t seed = strtoull(argv[1], NULL, 10);
    int half = atoi(argv[2]);
    int step = atoi(argv[3]);
    const char *out = argv[4];
    if (half <= 0 || step <= 0) { fprintf(stderr, "half and step must be > 0\n"); return 2; }
    int n = (2 * half) / step;
    if (n <= 0) { fprintf(stderr, "empty scan\n"); return 2; }

    Generator g;
    setupGenerator(&g, MC_NEWEST, 0);
    applySeed(&g, DIM_OVERWORLD, seed);
    SurfaceNoise sn;
    initSurfaceNoise(&sn, DIM_OVERWORLD, seed);

    int16_t *body = malloc((size_t)n * n * 2 * sizeof(int16_t));
    if (!body) { fprintf(stderr, "out of memory for %d x %d\n", n, n); return 1; }

    for (int j = 0; j < n; j++) {
        int z = -half + j * step;
        for (int i = 0; i < n; i++) {
            int x = -half + i * step;
            float y = 0; int id = 0;
            /* mapApproxHeight works at 1:4; ask for a 1x1 cell per sample. */
            mapApproxHeight(&y, &id, &g, &sn, x >> 2, z >> 2, 1, 1);
            if (y < -32768.f) y = -32768.f;
            if (y >  32767.f) y =  32767.f;
            size_t k = ((size_t)j * n + i) * 2;
            body[k]     = (int16_t)(y < 0 ? y - 0.5f : y + 0.5f);
            body[k + 1] = (int16_t)id;
        }
    }

    FILE *fh = fopen(out, "wb");
    if (!fh) { perror(out); free(body); return 1; }
    fprintf(fh, "mobascan 2 %llu %d %d %d\n",
            (unsigned long long)seed, n, half, step);
    size_t want = (size_t)n * n * 2;
    size_t got = fwrite(body, sizeof(int16_t), want, fh);
    fclose(fh);
    free(body);
    if (got != want) { fprintf(stderr, "short write\n"); return 1; }
    return 0;
}
