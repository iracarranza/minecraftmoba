# Handoff — 930010639 resource_light fixture, 22 September 2026

**Materialized and loadable, but NOT cleared for the paired experiment.**
Continue from the fixture report below. Do not run the 16 trials yet, add
telemetry, or change balance, screening or authoring semantics.

The earlier audit commit **4244b39 was fast-forwarded to remote main** before
this continuation. The original dirty checkout and the live Alpha server at
`/private/tmp/alpha-server` were preserved. Every fixture server was disposable
and has been stopped.

## Read first

- [Fixture report and exact reproduction commands](implementation/worldgen/reports/nearmiss_fixture_2026-09-22/REPORT.md)
- [Audit and proposed paired tasks](docs/audit/2026-09-22-terrain-gameplay-comparison.md)
- [Alpha server runbook](docs/alpha-server-runbook.md)

The report supersedes the audit's fixture assumptions; the audit remains the
experiment proposal. This task produced no gameplay/economic balance finding.

## What succeeded

- Built seed **930010639**, volume `tv_51a79e3b1bee05ea7559e799`, using the
  near-miss candidate, siting, opportunity map and frontier from `faa6748`.
- Existing portfolio → routes → structures → verification → diff/export
  machinery; **resource_light rank 0, 28 placements, zero skipped**. Six physical
  Route corridors and eight team structures. All structure template block
  identities match final readback. No terrain ranking or placement rule changed.
- Runtime catalogue contains only **resource_light**, forced by the existing
  config key. **71,148 diff blocks** apply successfully; reset reapplies it.
- **Ten sources and eight Worksites registered**. Sources: four potatoes,
  three rabbit, one goat, one pig, one chicken. Runtime `goat` → `GOAT` identity
  mapping was missing and is now added, with no tuning or authoring change.
- Both teams pass Survival spawn, quartz support, movement away from the
  fountain, and respawn after finite lethal damage. No gathering, processing,
  delivery, full-route traversal or task rehearsal was run.
- Base fingerprint unchanged through builds/server use; archive file hashes
  verified; two fresh builds produce identical decompressed diff payloads.
  Runtime seed is confirmed as 930010639. Plugin build: **205 tests pass**.

### Coordinates — keep the three meanings distinct

| Team | Candidate homeland X,Z | Selected fountain X,Y,Z | Runtime spawn X,Y,Z |
|---|---|---|---|
| North | 1812, -244 | 1828, 67, -244 | 1830.5, 71, -243.5 |
| South | 2348, -20 | 2212, 78, 76 | 2214.5, 82, 76.5 |

Siting rank 0 is preserved. Spawn is on a standable fountain plinth column with
headroom. Config order is **[x,z,y]**, not XYZ. The logical orientation is rotated
90°; raw-world Z does not by itself identify north/south. Prior travel matrices
start at candidate centers, not these runtime fountain positions.

## Remaining blockers / limits

1. **Existing Route endpoint verifier fails.** At north destination
   **(1804, 308)** the top is **ice at Y62**, while
   `verify_portfolio.check_routes` requires dirt path or oak planks. The writer
   deliberately surfaces selectively and may retain natural ground. Its six
   modeled profiles all report zero unclimbable steps / worst step 1, but that
   does not waive a failed final-block check. Neither authoring nor the verifier
   was changed. `build.json.ready_for_playtest` and `preflight.json.pass` remain
   **false**, and the build/probe correctly exit 1.
2. **Task B as specified is impossible here:** no authored wheat source.
   All four founder crops are potatoes. Do not add wheat or silently substitute
   potatoes or village hay. A common-resource task must be selected explicitly
   against both configurations before this experiment is run.
3. **Task A feasibility remains unestablished on an accepted fixture.** No
   resource-to-useful-output undertaking was attempted. Registration alone also
   does not prove manifested supply or harvestable yield; normal initial source
   delay is unchanged.
4. An initial `/kill` probe did not confirm death. The corrected finite-damage
   probe confirms death events and proper N/S respawns. Preserve this as an
   administrative-command limitation; do not change vitals/balance to fix the
   fixture test. Rendering and full-match playability are not certified.

## Smallest next step

Reconcile **that endpoint verification contract** with the current selective
surfacing semantics while retaining real surface/headroom/traversal safety
checks. Re-run final readback and a short traversal at the failed endpoint;
do not pave/move it or loosen checks to manufacture success. Then explicitly
revise Task B to a shared authored resource present in both fixed configurations,
and perform only the audit's minimal Task A/B feasibility rehearsal. The 16-trial
comparison and telemetry remain later work.

The land gap **0.3173 remains a terrain descriptor**, not a demonstrated economic
penalty. Do not introduce deficit weighting or a screening gate from these
fixture results.

## Implementation / artifacts

- `implementation/worldgen/experiments/nearmiss_fixture.py`: explicit-input
  build driver. Existing core fountain/step checks plus final block readback;
  exports a diagnostic diff and fails readiness when checks fail.
- `implementation/plugin/experiments/prepare_nearmiss_server.py`: isolated
  server/config packager. **Bukkit merges bundled defaults**: an external source
  table alone resurrected Alpha entries. This packages the same generated config
  in the disposable jar and external file, preserving all other jar bytes.
  Do not reuse Alpha's `deploy.sh` for this seed.
- `implementation/plugin/experiments/nearmiss_preflight.cjs`: load, registration,
  spawn, local movement, finite-damage respawn and reset acceptance probe.
  No economic task recorder.
- `implementation/worldgen/reports/nearmiss_fixture_2026-09-22/`: reports,
  configuration diff, generated server config, hashes, logs, pass/failure evidence.
- `artifacts/worldgen/nearmiss_fixture_2026-09-22/packages/base-terrain.tar.gz`:
  published experimental base snapshot. Reproduction no longer depends on an
  untracked harvest existing on another machine. Not a playable-map release.

Base fingerprint:
`5729f9396dbb0dc4d1e1b94d81b3d8f40cf3e0dcc56fa168273d58db5ea3486a`.
**Not compatible with Alpha's base.** Pristine means unchanged retained harvest
for this fixture, not independently proven untouched native generation.

Last disposable server: `/tmp/nearmiss-fixture-930010639/final/server`, port
25639, stopped. Source worlds under its sibling `worlds/` are separate from
reports and implementation. Use a fresh directory when replaying the commands.

## Preserved operating constraints

- Never copy a plugin jar into a running server. No live redeploy occurred here.
- Keep balance/authoring/screening frozen; no new telemetry and no 16-trial run.
- Runtime recognized Routes still require player designation/traversal. Authored
  corridors do not automatically grant recognized Route benefits.
- Do not re-freeze Alpha or revive the retired `land_asymmetry` score.
- Natural regen stays governed by existing config; Silk Touch harvest semantics,
  Logistics/Combat WP and class-selection GUI remain unresolved/parked as before.
- Prefer one implementation of a rule, and preserve all steps of the existing
  build pipeline. The fixture adapter supplies inputs; it does not redefine rules.
