# Seed reevaluation and test maps — 2 October 2026

**Prototype/test.** Uses the current compiler and its existing provisional
thresholds. Passing means the encoded physical and runtime-binding contracts
passed; competitive play balance and encounter quality remain untested.

**Completed:** three accepted maps from eight generated locations, across
sixteen screened seeds. All three inspection ZIPs passed independent server
boots and Fountain block checks. See [RESULTS.md](RESULTS.md) for downloads,
the terrain comparison, rejected seeds, and verification limits.

## Evidence

- `tests.log`: 617 worldgen tests passed before this batch.
- `tests-after-fixes.log`: 620 tests passed after the exact-counting and
  optional-entity-region repairs; focused test logs are retained separately.
- `prospects/`: complete off-server measurements for 16 seeds, including six
  familiar seeds and ten reproducible fresh seeds (RNG seed `20261002`).
- `candidates/`: harvested, oriented eight-block feature grids with source
  coordinates and generation provenance.
- `compilations/`: stage reached, explicit rejection codes, physical readback,
  opening constraints, resource portfolio, and runtime bindings per attempt.
- `summary.json`: checkpointed batch status and successful publication records.
- `run.log`: generation progress and compilation outcomes.
- `resume.log`: resumed evaluation using saved worlds after the tooling fixes.
- `packages.json`: final inspection ZIPs and their coordinates, when packaged.
- `*-overview.svg`: sampled terrain and authored objective/Lair locations;
  analytical plan views, not in-game screenshots.
- `server-smoke.json` and `*-server-smoke.log`: official-server boots and
  plinth/source-water checks for each inspection copy.
- `evaluation-source-hashes.json`: source hashes of the evaluation code and
  test-generation datapack, including the uncommitted changes.

The first sandbox attempt could not bind local server sockets. Its evidence is
retained in `sandbox-attempt/`; those are infrastructure failures, not seed
rejections. The actual generation run uses isolated localhost servers.

## World authoring and separation

Minecraft Java **1.21.11**, Java **21**, official server SHA-1
`64bb6d763bed0a9f1d632ec347938594144943ed`. The existing server generates
selected vanilla terrain windows, and the existing compiler authors structures
through its Anvil/NBT writer. The generated test worlds also use
`implementation/datapacks/moba_test_no_trial_chambers/` **before generation**.
That test override disables the trial-chamber biome tag; it does not disable
all structures, remove existing structures, or represent unmodified vanilla
generation. Every saved Overworld chunk is checked for chamber starts and
trial-spawner palettes; publication repeats that check on the authored build.

Scripts: `tools/worldgen/reevaluate_test_maps.py` and
`tools/worldgen/package_test_maps.py`. The batch retains source worlds and
authored builds under `artifacts/worldgen/seed_reevaluation_2026-10-02/attempts/`.
Its isolated pool is `artifacts/worldgen/seed_reevaluation_2026-10-02/test-pool/`.
Inspection copies and ZIPs use separate `inspection-worlds/` and `packages/`
directories. Nothing is installed in the live server or user's Minecraft saves.

Inspection copies spawn above the north Fountain in creative mode with commands
enabled. Their remaining generation is void with structures disabled, so absent
terrain cannot silently become fresh vanilla terrain. They contain coordinate
manifests. Actual MOBA matches require the plugin and the pool runtime bindings.

Historical September 24 yields are not current certification: the September 25
repair audit quarantined the old pool for excluded trial chambers. This batch
does not remove any old quarantine marker or relax the publication checks.
