# Handoff — Minecraft MOBA, 22 September 2026

Continuation from `7cae5ae` / `faa6748`; this handoff supersedes their proposed
next step. Audit branch: `codex/terrain-gameplay-audit`. The live test server is
at `/private/tmp/alpha-server`; this audit did not operate or change it.

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

### Seed generalization, team-axis diagnosis, and the first rescue test

**Keep the axes straight.** W–E is the REGIONAL axis and contrast there is
intentional (forest/arid, frozen/open). N–S is the TEAM axis and must be
competitive. A pairing never means one team gets forest and the other arid.

- **`vanilla_search/coarse.c --regions`** — region-CHARACTER screen. 20,000
  seeds → 8,425 accepted across 12 pairings, against one composition before.
  → `docs/analysis/2026-09-22-region-character-screen.md`
- **`terrain_harvest.compare_seeds`** — diagnoses each seed's N–S deficit and
  names its kind by comparing components between teams. No absolute thresholds.
  → `docs/analysis/2026-09-22-team-axis-deficits-and-intervention.md`
- **First rescue test, seed 930010639 (`frozen/open`)** — full pipeline run.
  → `docs/analysis/2026-09-22-nearmiss-rescue-930010639.md`
  → artifacts in `implementation/worldgen/reports/nearmiss_930010639_2026-09-22/`

**The finding that should govern what happens next:**

> **A near-miss passes the balance filter without its deficit being repaired.**

930010639 went in with an accessible-land gap of 0.3173 (south short of
near-depth workable land) and came out at `balance_asymmetry` 0.0161 — better
than the map we are playing. The accessible-land gap is **still 0.3173**,
unchanged *by construction*: `balance_asymmetry` measures travel-cost equality
to the opportunities authoring places, while the deficit is which depth bands
the terrain has. Authoring places opportunities; it does not create depth bands.

Placement confirms it directly. South-biased share of placements is 0.38–0.42 on
the deficient seed against 0.36–0.41 on the balanced one — **no steering toward
the deficient team at all**.

**Do not read `balance_asymmetry` as evidence about a diagnosed deficit.** It is
a real property (equal access to what was placed) and it is not that one.

**Other results worth keeping:**

- Minimum successful intervention on the near-miss is **28 opportunities**
  (0.0333); best is 32 (0.0161). Spending 38 does worse. Balance is bought by
  placement, not quantity — consistent with the −0.21 cost/balance correlation
  measured over Alpha's 80 finalists.
- The near-miss is **43% as authorable**: 991 qualifying configurations against
  Alpha's 2,285 per 9,600 samples. The best-case scalar hides this entirely.
- Structure siting already equalises teams by construction (quality gaps
  0.0016–0.0058), so a team-axis deficit never lives in the team structures.
- `land_asymmetry` was degenerate (0.0000 everywhere) and is replaced by
  accessible land plus connected workable land. Do not resurrect it.

### Next step — measure a task before changing balance

**The 0.3173 gap is terrain evidence, not an established gameplay penalty.**
Do not change balance, authoring or screening logic. Neither deficit-aware
weighting nor a new rejection gate is justified by this audit.

Read [the gameplay/economic audit](docs/audit/2026-09-22-terrain-gameplay-comparison.md)
for the implementation inventory, exact protocol and observation requirements.
Its source-level qualifications supersede the shorthand above:

- `accessible_land` counts dry Wilderness samples in selected depth bands,
  excluding homelands; it does not establish usable production area.
- Alpha is seed **930012642**, volume `tv_ef56852eda10acc88342e5ee`;
  near-miss is **930010639**, volume `tv_51a79e3b1bee05ea7559e799`.
- The near-miss has **63 resource manifestations in nine scanned cells**.
  Its `structures-built.json` is a positional stub; the rescue analysis did
  **not** physically build a playable map.
- Extraction benchmarks pool cells, travel matrices model surface cost, and
  the runtime ledger records work. None measures a complete timed expedition.
  Existing provenance/renewable CSVs are aggregate diagnostics without team/task
  attribution. No new telemetry was implemented.

**Recommended implementation/experiment, in order:**

1. Pin both existing **resource_light** configurations (same 28-opportunity
   budget), plugin/config and world inputs. Use a disposable Paper 1.21.11 /
   Java 21 server and existing Anvil/diff pipeline. Materialize the missing
   near-miss with the existing portfolio → routes → structures modules and
   verification/export functions, driven by explicit seed-specific inputs. The
   `reauthor` wrapper hard-codes Alpha fountain coordinates: use the small external
   driver specified in the audit, preserving all checks. Verify fountains,
   Routes, sources, spawn coordinates and base/diff
   compatibility. Force the named configuration through the existing test config
   key; never compare random Alpha selection to a cherry-picked near-miss.
2. Rehearse two tasks with video and existing `/moba work` and `/moba debug`
   snapshots: **acquire/process six iron into an iron pickaxe and bucket, return
   both to homeland**; **use an authored wheat source to deliver three bread and
   establish four hydrated planted wheat blocks at homeland**. Preflight the
   shared wheat source; absence is a content mismatch, not permission to inject
   it. Exact kits, milestones and reset requirements are in the audit.
3. Run **16 trials**: two operators × two maps × N/S × two tasks, counterbalanced,
   fresh state per trial, 20-minute cap and five-minute checkpoints. Record actual
   acquisition/processing/delivery times, travel, all break/place actions by
   purpose, source utilization, stock and WP/level history. Retain failures.
4. Only if manual observation is insufficient, implement the bounded
   `TaskTrialRecorder` specified in the audit: participant/task markers,
   timestamped successful actions, sampled state, and snapshots of existing
   ledger results. Reuse membership and scoring seams. Do not build a survival
   bot, full simulator or alternate economic model.
5. Compare S minus N on each map, then the difference between those paired gaps.
   Lead with useful output and access burden, not WP alone. Replicate any delay
   on a second existing profile before attributing it to terrain. Two operators
   establish feasibility, not full competitive fairness or causality of 0.3173.

No physical-failure arm is added. Preserve the previous rescue result and its
artifacts for traceability; its balance/screening alternatives remain pending.

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
