# Trial-chamber exclusion experiment — Prototype/test

Minecraft Java **1.21.11**, data-pack format **94.1**.

Install before the first chunk is generated. The pack replaces
`minecraft:has_structure/trial_chambers` with an empty biome tag. It keeps other
vanilla structures and terrain generation enabled. It does not remove chambers
from an existing world.

This is a test-batch implementation of the existing pool publication exclusion,
not a new canonical design decision. The generated worlds must report the
override in provenance and must not claim to be unmodified vanilla worlds.
The batch scans every saved Overworld chunk for chamber starts and trial-spawner
palettes before harvesting, and the existing pool publisher repeats that check
on the authored world. Missing or failed validation is not a pass.

The pack is intentionally separate from the runtime MOBA datapacks.
