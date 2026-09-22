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

### Seed generalization — done far enough to hand over

- **`vanilla_search/coarse.c --regions`** replaces "ocean east, highland west"
  with a region-CHARACTER classifier. 20,000 seeds give 8,425 accepted across
  12 pairings, against one composition by construction. The staged screen is
  untouched and still reproducible.
  → `docs/analysis/2026-09-22-region-character-screen.md`
- **`terrain_harvest.compare_seeds`** then showed the balance machinery is
  composition-agnostic in practice: Task A's fit ran unmodified over eight
  candidates in six pairings. The current Alpha seed has the best homeland
  asymmetry of the eight (0.0645); forest/frozen (0.0886) and forest/open
  (0.0983) are close behind.
  → `docs/analysis/2026-09-22-cross-composition-balance.md`

**The next experiment is specified and blocked on one thing.** Seed 930015734
is the mesa-and-jungle case, homeland quality 0.950 north against 0.468 south —
asymmetry 0.3397, the worst of the eight. Authoring exists to make a near-miss
competitive, so the question is whether authored opportunities can close a 0.34
homeland gap. Run the optimizer over that seed to get a scenario frontier, then
`author_portfolio` and read `balance_asymmetry`, comparing against the current
map's 0.0685.

Blocked because the optimizer takes an `export.zip` per seed and only the Alpha
volume has one. That export is the whole of the remaining work; everything
downstream already runs.

If the gap closes, the screen can afford far more geography than it accepts. If
it does not, homeland asymmetry becomes a screening gate rather than an
authoring problem — which is worth knowing before broadening the search.

**One metric in that comparison is broken and labelled as such.**
`land_asymmetry` reads 0.0000 for every seed because land band counts are summed
over halves with equal sample counts by construction. Do not read it as balance.
Per-team land wants buildable fraction or depth-weighted land instead.

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
