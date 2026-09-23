# Handoff — map compiler, PlayableMaps, and the READY pool

**23 September 2026. Branch `codex/spatial-cadence-migration`.** Audit:
[`docs/audit/2026-09-23-map-compiler-slice.md`](docs/audit/2026-09-23-map-compiler-slice.md).
The cadence migration's audit
([`2026-09-22-spatial-cadence-migration.md`](docs/audit/2026-09-22-spatial-cadence-migration.md))
still holds for the runtime.

Not merged to main. Base 03c5bea over 9bb5093 over main 8199c13. Nothing here
touched `/Users/iracarranza/minecraftmoba` or the live `/private/tmp/alpha-server`;
all server work used disposable directories.

## Where it got to

**Two unseen seeds compile, author and physically verify into PlayableMaps**, and
a foundry publishes them into a READY pool the plugin claims from. Verified on a
live server: it read a real pool, claimed both maps distinctly, reported
exhaustion, and retired one to USED.

Thirty unseen seeds, 6.7% yield:

| stage reached | seeds | rejection |
|---|---|---|
| recognize | 24 | `NO_DEFAULT_REGIONAL_SHAPE` |
| homebase | 3 | `SOCKET_NOT_INDEPENDENTLY_ACCEPTABLE` |
| lair | 1 | `LAIR_ACCESS_DISPARITY` |
| verify | 2 | passed |

The two successes are independent: sockets 0.559/0.832 against 0.737/0.850, Lair
A_L 0.068 against 0.0001, different objective sites.

## Seven defects, all found by running something

1. **The Lair lost its own occupant** — a Ghast in an unloaded chunk read as
   ALIVE-but-unreachable. Last-known-location tracking plus a `ChunkLoadEvent`
   queue.
2. **The recognizer rejected every candidate ever screened**, on a corridor proxy
   already known to be a false positive.
3. **Objective siting chose the best site per layer independently** and they did
   not coexist. Joint exhaustive search.
4. **Trees counted as terrain** in column verification. Authoring fells them;
   vegetation is clearing cost. Standing structures now fail hard instead.
5. **Ground contact compared terrain flatness to mesh contact** — two different
   quantities, so every site failed. Replaced with levelling volume.
6. **Footprint overlap was invisible upstream.** A Bastion and an Outpost eleven
   blocks apart both passed every analytic check; the readback found the Bastion
   had overwritten the Outpost.
7. **The pool leaked a map per match.** A retired map keeps its claim file, and
   `entries()` checked claim before used, so it reported IN_USE forever.

## What is measured now

- **All three objective forms**, from Minecraft. Outpost and Bastion from the
  client jar's NBT; End Spike from a generated vanilla End — radius 2–5, height
  76–103, disc rule `dx²+dz² ≤ r²+1` (reproduces column counts 21/37/57/89), one
  bedrock cap block, two of ten caged.
- **Lair access parity**, bound 0.15 from the widest gap in the distribution.
- **Homeland socket quality**, 28 sockets, bound 0.52 at the break.
- **Levelling cost**, 1800 footprints across three worlds, bound 10.0/column.

Contracts are derived from the authored template, so the verifier and the
builder cannot drift — which is how the Bastion was once verified at 32 blocks
tall and built at 64.

## Next step

1. **Yield is the operational constraint.** 6.7% is ~15 generated worlds per pool
   map, ~2 minutes each. Screening regional shape from biome data *before*
   generating a world is the cheap win: 80% of the cost goes on seeds rejected at
   the first stage.
2. **Run the foundry as a job** with a target pool depth, rather than by hand.
3. **The Lair is sited but not manifested.** `alpha.lair.site` is still empty on
   published maps, so the cadence would report UNCONFIGURED on a claimed map.
   This is the clearest gap between the physical contract's "PlayableMap" and a
   map a match can actually be played on.
4. **The Bastion's rampart ring is not reproduced** — vanilla assembles it by
   jigsaw; the central body is placed and the ramparts are not. This travels with
   every published map.
5. **Widen the A_L sample** and revisit the 0.15 bound properly.
6. **Nothing has been played.** Verification is physical, not competitive. Every
   threshold is PROVISIONAL_ALPHA, and whether any of these disparities are felt
   in play is still unanswered and still not answerable by analysis.

Caveats travelling with these maps: `worldgen_truth.verified = false` (vanilla
terrain, not the official-jar SHA-1 provenance), and the compact-Hinterland gate
has never fired (1.4%–2.5% against a 50% bound), so it is a guard, not evidence.

Still open and untouched: siege advantage, boss and toppling XP, Worksite tier
packages, Giant/Ghast/Dragon encounter design, Lair art direction. The vanilla
Giant still has no designed AI; recorded, not designed around.

Tests: Java 239, worldgen 388, all passing.
