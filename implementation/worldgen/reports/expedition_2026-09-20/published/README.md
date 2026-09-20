# Authored world, published for non-Minecraft readers

`balanced_baseline`, finalist rank 0, authored from the committed optimizer's
frontier (`frontier_sha256` recorded inside the grid file).

The region files themselves are ~28 MB and only Minecraft can read them. These
two artefacts carry the same surface in forms anything can open.

## `balanced_baseline-map.png`

432×528 px top-down render of the surface, sampled every 4 blocks, 2 px per
sample. Relief-shaded by height, so it reads as terrain rather than a flat map.

- tan lines — the six authored Route corridors
- white squares — the two homelands
- yellow — founder crop patches · green — renewable ranges
- orange — mining worksites · purple — POIs · pink — Route targets

Exact colours are in the grid file under `render.class_colours` and
`render.site_colours`, so nothing has to be guessed from the image.

## `balanced_baseline-grid.json`

The same sample grid as data, 216 × 264 = 57,024 samples, run-length encoded by
row. Each sample is `[y, class_index]` or `null` outside the volume;
`surface_classes` gives the index order. `origin_xz` and
`sample_spacing_blocks` convert a sample index to world coordinates:

    x = origin_xz[0] + i * sample_spacing_blocks
    z = origin_xz[1] + j * sample_spacing_blocks

Also carries every authored site with its world position, and every Route with
its endpoints, column count, terrain-weighted cost, and which authored sites its
corridor crosses.

This is the file to compute against — distances, terrain along a corridor, what
sits near a worksite — without a Minecraft client.

## What neither shows

- anything below the top block; both are surface projections
- entities, which are not in the region files
- block-exact detail between 4-block samples

The render is a sampled orthographic projection, not a screenshot. For measured
reach and balance on this world, use `../rescan/rescan-balanced_baseline.json`.

## Regenerating, or publishing another profile

    python3 -m terrain_harvest.publish_world \
      --world <authored world> \
      --candidate results/staged_default_2026-09-09/finalists/930012642/candidate.json \
      --portfolio <portfolio report> --routes <routes report> \
      --outdir <dir> --name <profile> --spacing 4 --scale 2
