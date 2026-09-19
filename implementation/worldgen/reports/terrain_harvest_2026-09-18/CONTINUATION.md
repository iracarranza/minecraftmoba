# Terrain harvest continuation — stages 1–7 complete

Working / Prototype-test. Branch `terrain-harvest-gallery-2026-09-18`.
Every stage of [`NEXT_STEPS.md`](../../terrain_harvest/NEXT_STEPS.md) has been
executed. 225 tests pass. Tooling was pushed before each stage's outputs.

## Results

| Stage | Result |
| --- | --- |
| 1 Boundary profiles | 300 cells across three volumes. Regenerated from scratch after deleting the prior output: **byte-identical**. |
| 2 Preservation audit | **813,637,632 cells**, zero mismatches, exhaustive, 3m05s. Zero block-entity, tick or entity differences. |
| 3 Navigation | **19/19** protocol assertions, zero server errors. |
| 4 Containment and drift | Frozen changes **0** cells. Resumed changes 712, all in-mask. **Envelope breached: 0.** Containment 6/6. |
| 5 Artifact lifecycle | Receipts, refused reuse, byte-identical packaging, separate install. 15 tests. |
| 6 State-aware relocation | Fail-closed rotation and translation on fixtures. 21 tests. Not wired into export. |
| 7 Composition manifests | Reference, transform, overlap and seam validation. 19 tests. Places no blocks. |

## What the evidence supports

Materialized terrain is a faithful copy of its source on disk, verified cell by
cell rather than sampled. The gallery loads in the pinned server and its
navigation functions work over the real protocol. Freezing holds the world
still; unfreezing produces only ordinary neighbour-dependent updates.

## What it does not support

No human client walkthrough has occurred. Nothing here proves the terrain looks
right, and a server load never implies graphical inspection. Rendering,
long-term fluid equilibrium, boats and other vehicles, and operator, spectator
or teleport exploits are all uncovered. Stage 4 probed the scoop volume only;
the two whole-map volumes were not simulated. One containment direction is
recorded **inconclusive** because the bot died in a cave, and respawn is not an
escape. Physical relocation remains unimplemented, so no volume can be placed
anywhere but its source coordinates.

## Probe defects found and fixed, not gallery defects

Stage 4 took four runs. Each failure was in the measurement:

- 3473 reported envelope breaches were barriers gaining a spelled-out
  `waterlogged=false` when the server rewrote them on load. The block never
  changed. Shell cells are now compared by name.
- `forceload` from the server console ran in the overworld, so the volume never
  loaded. Fixing it is what made the breach diagnosable: the frozen phase then
  reproduced the identical count, and nothing ticks while frozen.
- A bot death in a cave was scored as an escape from the volume.
- The fall test measured whether a 380-block drop is survivable rather than
  whether a floor exists.

Stage 3 had one too: dimension identity was read from a client library field
that reports the dimension *type*, which is `harvest:inspection` for every
dimension in this gallery and cannot distinguish volumes. Had the probe asserted
position alone it would have passed while never verifying a dimension change.

## Next, and who decides

Engineering that still needs no user input: simulate the two whole-map volumes;
probe boats and vehicles; implement relocation against a matching-server probe
on real blocks before enabling it for the corpus; compute mask-level rather than
bounding-box overlap.

Outside this queue, and yours alone: choosing a base map, an actual insertion
pair, a seam policy, final acceptance, and the human walkthrough that no amount
of protocol evidence substitutes for.
