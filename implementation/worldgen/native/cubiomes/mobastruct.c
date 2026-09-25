/* Locate Overworld structures off the seed and emit JSON.
 *
 *   mobastruct <seed> <half> [types]
 *
 * covers x,z in [-half, half]. `types` is a comma-separated subset of the
 * names below; omitted means all of them.
 *
 * WHY THIS IS CHEAP. Structure placement is a 48-bit LCG seeded per region
 * from the world seed, so a candidate position is arithmetic, not generation.
 *
 * WHY IT IS TWO STEPS. `getStructurePos` returns where a structure WOULD go
 * in its region; the structure only actually generates if the biome there
 * accepts it. Reporting step one alone gives false positives -- every region
 * of the world contains a Mansion candidate, and almost none contain a
 * Mansion. `isViableStructurePos` is the filter, and it needs the generator,
 * which is why this costs more than a pure LCG walk and still far less than
 * generating a chunk.
 */
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include "generator.h"
#include "finders.h"

struct Named { const char *name; int type; };
static const struct Named KINDS[] = {
    {"mansion", Mansion}, {"village", Village}, {"monument", Monument},
    {"jungle_temple", Jungle_Temple}, {"desert_pyramid", Desert_Pyramid},
    {"outpost", Outpost}, {"igloo", Igloo}, {"swamp_hut", Swamp_Hut},
    {"ruined_portal", Ruined_Portal}, {"shipwreck", Shipwreck},
    {"ocean_ruin", Ocean_Ruin}, {"ancient_city", Ancient_City},
    {"trail_ruins", Trail_Ruins},
    // The rest of what cubiomes can place. Omitting them was an oversight
    // rather than a decision: they cost nothing at 0.107s per seed, and
    // `mineshaft` and `trial_chambers` in particular are real Extraction
    // anchors that were invisible to every measurement until now.
    {"mineshaft", Mineshaft}, {"trial_chambers", Trial_Chambers},
    {"treasure", Treasure}, {"geode", Geode}, {"desert_well", Desert_Well},
    {"fortress", Fortress}, {"bastion", Bastion}, {"end_city", End_City},
};
static const int NKINDS = sizeof(KINDS) / sizeof(KINDS[0]);

int main(int argc, char **argv) {
    if (argc < 3) {
        fprintf(stderr, "usage: mobastruct <seed> <half> [type,type,...]\n");
        return 2;
    }
    uint64_t seed = strtoull(argv[1], NULL, 10);
    int half = atoi(argv[2]);
    const char *want = argc > 3 ? argv[3] : NULL;
    if (half <= 0) { fprintf(stderr, "half must be > 0\n"); return 2; }

    Generator g;
    setupGenerator(&g, MC_NEWEST, 0);
    applySeed(&g, DIM_OVERWORLD, seed);
    SurfaceNoise sn;
    initSurfaceNoise(&sn, DIM_OVERWORLD, seed);

    printf("{\"seed\":%llu,\"half\":%d,\"mc\":%d,\"found\":[",
           (unsigned long long)seed, half, MC_NEWEST);
    int first = 1, skipped = 0;
    for (int k = 0; k < NKINDS; k++) {
        if (want) {
            char pat[64];
            snprintf(pat, sizeof pat, "%s", KINDS[k].name);
            const char *hit = strstr(want, pat);
            if (!hit) continue;
        }
        StructureConfig sc;
        if (!getStructureConfig(KINDS[k].type, MC_NEWEST, &sc)) {
            skipped++;          /* not present in this version */
            continue;
        }
        int span = sc.regionSize * 16;
        int r0 = (-half) / span - 1, r1 = half / span + 1;
        for (int rz = r0; rz <= r1; rz++) {
            for (int rx = r0; rx <= r1; rx++) {
                Pos p;
                if (!getStructurePos(KINDS[k].type, MC_NEWEST, seed, rx, rz, &p))
                    continue;
                if (p.x < -half || p.x > half || p.z < -half || p.z > half)
                    continue;
                /* The filter. Without it every region reports a candidate. */
                if (!isViableStructurePos(KINDS[k].type, &g, p.x, p.z, 0))
                    continue;
                float y = 0; int ignored = 0;
                mapApproxHeight(&y, &ignored, &g, &sn, p.x >> 2, p.z >> 2, 1, 1);
                /* The SURFACE biome, not mapApproxHeight's id output.
                 * That output is the biome at the sampled column and can be a
                 * cave biome: three Mansions on seed 31337 reported 175
                 * (lush_caves), 29 and 175, when all three stand in
                 * dark_forest. `isViableStructurePos` had already confirmed
                 * that; only the reported field was wrong. */
                int id = getBiomeAt(&g, 1, p.x, 63, p.z);
                printf("%s{\"type\":\"%s\",\"x\":%d,\"z\":%d,\"y\":%.0f,\"biome\":%d}",
                       first ? "" : ",", KINDS[k].name, p.x, p.z, y, id);
                first = 0;
            }
        }
    }
    printf("],\"types_unavailable_in_version\":%d}\n", skipped);
    return 0;
}
