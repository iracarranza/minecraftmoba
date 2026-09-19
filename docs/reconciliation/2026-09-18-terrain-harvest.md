# Terrain harvest and gallery — 18 September 2026

**Working / Prototype-test tooling decision. No final Default selection.**

The user's explicit terrain-harvest request authorizes replacing the inspection
object, not changing canonical selection criteria. `maps.md`, `objectives.md`,
`classes.md` and `infrastructure.md` remain unchanged. Natural geography becomes
a finite TerrainVolume reference, optionally materialized for inspection; required
game systems and economic validation remain downstream. One seed can supply
several whole candidates, near misses and sections. A seed is provenance, not a
permanent assembled-map identity.

## Authority reconciliation

- `maps.md`: actual Overworld geography, western highland/eastern coast grammar,
  continuous Wilderness, asymmetric natural geography, equivalent baseline
  opportunity. These requirements remain. Rejecting a region as a complete map
  does not require throwing away its useful sections.
- `specs/mapseedsearchspec1.md` §§2/5 defined candidates as seed + X/Z bounds +
  logical orientation and deferred final boundaries. This remains the historical
  search-record contract. **For new harvest inspection only**, the user's request
  replaces the interpretation of these records as places to visit in an infinite
  world with explicit finite 3D selections. No spec-1 metric is reweighted.
- Specs 2/3 and `vanilla_search/TASK_A_SEQUENCE.md` trace the homeland-relative,
  destination-first, local-continuation and network work. The current frozen
  selection is `results/default_local_continuation_2026-09-11/`; historical roads
  must not become solver input again.
- Spec 4, `vanilla_search/OPPORTUNITY_SHADOW.md` and
  `results/default_opportunity_shadow_2026-09-11/` are additive diagnostics.
  Handoff → Affordance → Reach → Payoff and Opening Opportunity Sets retain their
  conditional evidence states. Their task-scoped prohibition on materializing
  worlds governed that earlier analysis; this explicit new request authorizes
  separate harvest copies, never mutation of its source worlds or selections.
- `infrastructure.md` distinguishes Homeland Depth from Travel Cost, formal
  recognition from ordinary useful work, and path/endpoint proof by modality.
  Harvesting grants no recognition and changes none of those rules.
- The current September 14 manuscript supplements retain Work Stock, UAU Flow,
  opportunity frontier and undertaking-specific Practical Reach. The 256 WP/UAU
  value is a Working calibration hypothesis, not a terrain valuation unit.
- The pre-existing **untracked** local September 18 economic SHADOW tooling and
  report were read in the original checkout. They count real ores in bounded
  Handoff windows, retain the source hashes, and explicitly leave accessibility,
  time, yield, return burden and Practical Reach unresolved. They are not in the
  current remote branch and are not silently committed by this task. The local
  worldgen-balancing additions were likewise read and preserved. Their distinction
  between raw opportunity density, eligibility and realized income remains.
- Browser wiki and old numeric-ID generators remain non-authoritative. Task B
  authored greyboxes are distinct from the staged source snapshots used here.

## Repository and capability discovery

Required identity commands were run first in `/Users/iracarranza/minecraftmoba`:
`git remote -v`, `git rev-parse --show-toplevel`, `git status --short --branch`,
`git log -5 --oneline`. Remote is `https://github.com/iracarranza/minecraftmoba.git`;
local `main` started at `be06814fbcbe8bf8e62386ef591bf48e296faf17`, dirty and 38
commits behind. After fetch, work was isolated on
`terrain-harvest-gallery-2026-09-18` from
`9c85c915599f410cdadf26dbc76de9b7b9b55220`. Original local edits are untouched.

Existing tools: official vanilla generation with Cubiomes prefiltering;
eight-block surface/biome/structure extraction; frozen finalist and fitter JSON;
complete NBT codec and zlib Anvil writer/reader; previous authored serializers;
read-only economic ore observations; real staged worlds for both named seeds.
The older serializer synthesizes terrain and is **not** used to reconstruct
natural geography. Only its palette packing and existing NBT/Anvil codecs are
reused. Locally installed 1.21.11 client `version.json` and dimension-type data
confirm Java 21, DataVersion 4671 and datapack format 94.1.

**Explicit technical choice:** Minecraft Java **1.21.11**, Java **21**, direct
modern Anvil/NBT copying/masking with a source-derived `level.dat` and a void
inspection datapack. This is a terrain inspection build, not a playable MOBA.
The exact implementation contract and reproduction commands are in
[`terrain_harvest/README.md`](../../implementation/worldgen/terrain_harvest/README.md).

## Scope of reconciliation

New harvest manifests and materialization records are authority for extraction
provenance, not map fitness. A whole-map classification means retention of an
existing structurally surviving candidate; it does not mean complete gameplay
validation. Near misses require named failed/unresolved criteria. Specialist
section classification is a retention proposal and does not certify exceptional
quality. No new opaque quality score or winner is introduced.

Natural geography → selected/assembled geography → authored required game systems
→ economic / Practical Reach validation remains the downstream sequence. Future
composition can reference a base volume, inserted IDs, transforms, seams and
separate authored systems. No stitcher or seam-quality threshold is implemented.
