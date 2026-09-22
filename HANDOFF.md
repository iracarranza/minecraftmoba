# Handoff — Minecraft MOBA, 22 September 2026

Branch `plugin-assess`, pushed to `main` on `github.com/iracarranza/minecraftmoba`.
Everything below is committed. The live test server is at `/private/tmp/alpha-server`.

Read [docs/alpha-server-runbook.md](docs/alpha-server-runbook.md) first — it has
the deploy cycle, the resource-pack host, map configurations and resetting.

---

## The one rule this codebase keeps breaking

**Two implementations of one idea.** It has shipped five times, always the same
way: a fix lands on one copy, the other stays wrong, and the symptom is always
"the thing you did had no effect."

| | what happened |
|---|---|
| config keys | plugin read `progression.work.*`, file defined `progression.extraction.*`; every value silently came from a Java fallback |
| world loading | `load()` reused a stale instance, `restore()` rebuilt — so `match open` handed back last session's world |
| reset | `/moba match reset` cleared everything, `/moba reset <player>` cleared almost nothing |
| hunger units | Capacity became a rate, but four call sites still used it as a ceiling |
| eligibility predicate | deliberately duplicated in Python; kept honest by a shared fixture both sides must reproduce |

Tests now pin the *property* rather than the behaviour: no command may rebuild
`PlayerData` by hand, nothing outside the seam may call `effectiveHunger`, and
`ConfigKeysDefinedTest` scans every class for config reads the file does not
define. **Prefer that style over asserting a value.**

Second recurring failure: **pipeline steps that exist and get skipped.**
Re-authoring once shipped maps with no Aether Fountains because building the
team structures is a separate command. `terrain_harvest.reauthor` now runs all
four steps and gates on the two failures that are silent.

---

## State of the work

### Live and working
- **Map configurations.** A match's world is base terrain plus one authored
  configuration, drawn at random on world load. Four in the catalogue. A
  configuration is a ~250KB block diff, not a 28MB world.
- **Vitals scaling.** Health and hunger bars are always twenty points; Capacity
  decides what a point is worth. Display normalization, not a balance change —
  `VitalsTest` proves fractions are preserved.
- **Lobby world** `moba_lobby`: void, built, peaceful, no exit.
- **Resource pack** pushed by the server on join, hosted on `127.0.0.1:25580`.
- **Regenerative opportunities** manifest at runtime on eligible terrain;
  membership is an explicit PDC mark, never position-and-type.
- **Routes** fitted to a walkable profile: 0% unclimbable along the centreline,
  against 7.7% in the frozen map.

### Decided but not built
- **Class-selection GUI** — answered in full, explicitly parked. Chest GUI plus
  glyph art; the right panel cannot update on hover without a client mod.
- **Map vote/ban** — the eventual intent. It already settles that map balance is
  scored per named configuration rather than per family.

### Seed generalization and team-axis diagnosis — current front

**Keep the axes straight.** W–E is the REGIONAL axis and contrast there is
intentional (forest/arid, peaks/valley). N–S is the TEAM axis and must be
competitive. A pairing never means one team gets forest and the other arid.

- **`vanilla_search/coarse.c --regions`** replaces "ocean east, highland west"
  with a region-CHARACTER classifier. 20,000 seeds → 8,425 accepted across 12
  pairings. The staged screen is untouched and reproducible.
  → `docs/analysis/2026-09-22-region-character-screen.md`
- **`terrain_harvest.compare_seeds`** diagnoses the N–S deficit of each finalist
  and names its kind by comparing components between teams — no absolute
  threshold, and none taken from 930015734.
  → `docs/analysis/2026-09-22-team-axis-deficits-and-intervention.md`

**Findings that should shape the next move:**

- **930015734 (forest/arid) is an authoring target, not a rejection.**
  `severe_grade_fraction` and `water_fraction` are 0.0 on both homelands; the
  south is under 51.9% canopy with a third less near-depth workable land.
- **Authoring cost does not buy balance.** Over 80 stored optimizer finalists,
  cost-vs-balance correlates −0.21; 28 opportunities achieves 0.0182 where 38
  achieves 0.0211. Balance comes from placement, not quantity.
- **`land_asymmetry` was degenerate and is replaced** by accessible land
  (near-depth bands, excluding `deep_core_350_plus`) plus connected workable
  land. Do not resurrect the old one.

**Two limits stated rather than hidden:**

- No seed in the sample diagnoses `physical`, because the staged screen gated on
  homeland buildability upstream. "Severe geography justifies rejection" is
  untested, not supported.
- The cost-vs-balance figures come from configurations that already passed the
  balance filter, on a volume that was well chosen. They do not show that
  authoring can lift a deficient seed to parity.

### The next step, and its blocker

Answer whether minimal authoring rescues a genuine N–S near-miss.

930015734's world was deleted — only server scaffolding remains and it is not in
the gallery. **Use `tv_51a79e3b1bee05ea7559e799` instead**: seed 930010639,
`frozen/open`, homeland gap 0.1370, accessible land gap 0.3173, deficit kind
`opportunity`, already harvested at full 3,808 chunks. Different regional
character from the Alpha map, same deficit kind, no world generation needed.

The optimizer takes a zip containing exactly `data/opportunity-map.json` and
`data/travel-matrix.json`.

1. `terrain_harvest.opportunity_map --gallery <TerrainGallery> --volume-id
   tv_51a79e3b1bee05ea7559e799 --source <that volume's dir> --output ...` —
   **runnable today**.
2. `expedition.travel` needs `--structures` and `--manifest`, and the
   structure-siting and map-manifest records exist only for the Alpha volume.
   **This is the gap.** Both are produced by existing modules that have not been
   run for this volume.
3. Then zip, run `tools/analysis/map_authoring_optimizer.py`, and read
   `balance_asymmetry` out of `author_portfolio`'s metrics — comparing against
   the Alpha map's 0.0685.

If minimal authoring closes a 0.32 accessible-land gap, the screen can accept
far more geography than it does. If it cannot, N–S deficit becomes a screening
gate rather than an authoring problem.

### Unresolved, do not decide silently
- Natural regeneration is held OFF (`features.vitalsScaling.naturalRegeneration`).
  Uncapping hunger would switch vanilla regen on for the first time; that is a
  design decision, not a side effect of a bar-length change.
- Silk Touch → H in the Extraction model.
- Logistics and Combat WP: candidates reported, none implemented.
- Whether Routes should curve around structures rather than merely spare them.
- Re-freezing the Alpha template. Not done, deliberately.

---

## Things that will bite you

- **Never copy the jar into a running server.** Paper opens it lazily; classes
  not yet loaded become permanently unloadable and the server keeps running,
  throwing `NoClassDefFoundError` from whichever path is touched first. It
  presents as a game bug. `deploy.sh` refuses.
- **The server has no console window.** `start-alpha.sh` pipes a file into
  stdin: `echo "moba maps" >> /private/tmp/alpha-server/cmds`.
- **Resource-pack hash.** Clients cache by it, so a rebuilt pack behind a stale
  hash is silently ignored. `publish.py` writes both together.
- **Artifacts are gitignored.** `artifacts/worldgen/alpha-0.1/` holds the base
  terrain, the authored maps and the configuration diffs; none of it is in git.
  `base-terrain` is the parent of everything and was verified pristine.
- **The measurement usually exists.** Before arguing about whether Routes are
  jagged or whether pads bias the eligibility query, check — both turned out to
  be computable, and both corrected an assertion of mine.
