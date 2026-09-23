# 930010639 resource_light experimental fixture — 22 September 2026

**Doctrine interpretation update, 22 September 2026:** this fixture tests the
existing prototype's physical/runtime machinery, not compliance with a finalized
Homebase Core/Socket contract. 0.3173 is a permitted Wilderness terrain gap, not a
proven competitive deficit. Existing structures and Routes have not been
relocated or re-scoped. The endpoint-readback and Task B wheat blockers below
remain. See [spatial doctrine audit](../../../../docs/audit/2026-09-22-spatial-doctrine.md).

**Prototype/test. Materialized and runtime-loadable; NOT cleared for the paired
experiment.** One existing block-readback check fails, and Task B's specified
wheat opportunity does not exist. No balance result, task rehearsal, new telemetry
or 16-trial comparison was produced.

`4244b39` was fast-forwarded to remote `main` before this work. This continuation
uses its HANDOFF/audit and the exact near-miss inputs from `faa6748`. The original
dirty main checkout and live Alpha server were left untouched; the server's
runtime binaries/properties were read/copied into isolated installations.

## Result by requirement

| Requirement | Result / evidence |
|---|---|
| Correct seed and axes | **Pass.** Seed 930010639, volume `tv_51a79e3b1bee05ea7559e799`; logical rotation 90°, no reflection. N/S labels are copied from the candidate/siting; raw Z alone does not define the teams. Saved runtime world seed is 930010639. |
| Homelands/fountains/spawns | **Pass for the selected siting.** Candidate analytical centers and selected fountain positions differ; both are retained below. All fountain template blocks match readback. Runtime N/S Survival spawn, supporting quartz, movement away, and respawn after finite lethal damage pass. |
| Forced configuration / budget | **Pass.** Only `resource_light` is in the disposable catalogue, and `force: resource_light`. Existing author-best selected rank **0**, complete, 28 placements, zero skipped. The loaded diff contains **71,148 changed blocks**. Reset reapplies the same forced configuration. |
| Routes | **Mixed; readiness blocked.** Six authored physical corridors, no skipped targets, zero unwritable columns, zero modelled unclimbable steps, worst step 1. Existing endpoint block readback fails at **north destination (1804, 308): ice at Y62**. No Route algorithm, endpoint, material or checker was changed to hide this. |
| Team structures | **Pass.** Eight structures; all template block identities match the final authored world. `structures.json` is a real build report, replacing the earlier analytical positional stub for this fixture only. |
| Source / Worksite registration | **Pass after the narrow integration repairs below.** Ten renewable sources: four potato crops and six animal sources (three rabbit, goat, pig, chicken). Eight Worksites registered. Initial source lifecycle is RECOVERING; registration is not evidence of mature manifested stock or harvested yield. |
| Task B wheat | **Absent.** All four founder entries are `potato`, registered as `potatoes`. There is no authored wheat source. Do not inject wheat or substitute potatoes/hay without an explicit task revision. |
| Pristine base / diff compatibility | **Pass within the stated snapshot scope.** Input/base region fingerprints match, stay unchanged after server runs, and differ from Alpha's base. Two fresh builds produced identical decompressed diff payloads. All 16 packaged files were checked against SHA-256 hashes and archive contents. |
| Disposable load / basic playability | **Pass for loading, spawning, local movement, finite-damage death/respawn, reset.** The final probe still returns failure because the static Route readback is not cleared. No full Route traversal, client rendering, resource expedition or stock delivery is claimed. |
| Task A feasibility | **Not established on an accepted fixture.** No acquisition/processing/delivery rehearsal was run because validation is not clear. Ore observations from prior work are not a substitute for that test. |

### Spatial identities (world blocks)

| Team | Candidate analytical homeland X,Z | Selected fountain X,Y,Z | Runtime spawn X,Y,Z |
|---|---|---|---|
| North | 1812, -244 | 1828, 67, -244 | 1830.5, 71, -243.5 |
| South | 2348, -20 | 2212, 78, 76 | 2214.5, 82, 76.5 |

The runtime config encodes `[x,z,y]`: north `[1830,-244,71]`, south
`[2214,76,82]`. These are standable columns at fountain local offset `[2,3,0]`,
with feet one block above. The template's central water source and upper
cross-shaped glowstone are avoided; headroom is checked. The existing
`reauthor.check_fountains` quartz-top check is called with these seed-specific
columns, rather than Alpha's hard-coded positions. Siting rank 0 remains
unchanged. **Do not re-label these actual spawn sites as the candidate centers**
when making a later geographic comparison: the analytical travel matrices start
at candidate centers, while players start at the selected fountains.

## What changed, and what did not

- `implementation/worldgen/experiments/nearmiss_fixture.py` is a thin driver of
  the existing portfolio → routes → structures → verification → diff/export
  modules. It requires explicit inputs, refuses an existing output world, copies
  the unplayed harvested volume and the seed's original `level.dat`, preserves
  the old fountain/step checks, and adds actual final block readback. It exports
  a **diagnostic** diff but exits nonzero on failed readiness. Nothing in the
  authoring, screening, ranking, objectives or balance code was edited.
- The physical readback calls existing `verify_portfolio` checks on the 18
  placements with nonempty templates. The ten zero-block regenerative
  placements are checked through registration instead: current authoring
  deliberately does not build pens/fields. Pretending those placements must have
  old irrigated-field blocks would revive superseded authoring. The endpoint
  check itself is preserved unchanged and its ice failure remains visible.
- `implementation/plugin/experiments/prepare_nearmiss_server.py` builds the
  source and Worksite tables from the **actual** portfolio; no Alpha coordinates
  or source IDs are retained. Type-specific radius/capacity/recoverTicks and
  eligibility/temporal coefficients are copied unchanged from shipped config.
  The existing migrated-region behavior is retained; no new region shape is
  invented. `server-preparation.json` contains every config difference.
- Bukkit's `Settings.load` calls `copyDefaults(true)`. A new external source
  table alone therefore resurrects the jar's Alpha sources. The fixture packager
  embeds the same generated config in the **disposable copy** of the plugin jar
  and its external config. It verifies every non-config jar entry is byte
  identical. This avoids changing Settings/default-merging behavior globally.
- `RenewableKinds.java` adds only `animals("goat", EntityType.GOAT)`. The selected
  portfolio already authored a goat; the runtime vocabulary previously refused
  it. This is an identity mapping, with no new placement, parameter, yield,
  timing, eligibility or progression rule. The first load stopped registration
  at this missing mapping; the final load binds all ten sources.
- `nearmiss_preflight.cjs` is a disposable acceptance probe, not telemetry. It
  uses existing commands and a protocol client for basic spawn/death/reset checks.
  It does not collect economic tasks, implement strategies or alter gameplay
  coefficients. No live deployment occurred.

## Existing verification conflict — not silently waived

`physical-readback.json` records 58 successful physical checks and one endpoint
failure; all eight structure templates match. The unchanged Route writer
selectively surfaces an already-matching terrain profile (`routes.py::carve`),
so some centerline ground remains natural. The unchanged
`verify_portfolio.check_routes` requires endpoint tops to be dirt path or oak
planks, and therefore rejects this ice endpoint. This is a concrete
writer/verifier contract mismatch, **not proof of an impassable route**. The
profile's zero unclimbable steps is also not sufficient evidence to waive a
failed final-block check. No one walked the complete corridor in this task.

`build.json.ready_for_playtest` is false and both the build driver and final
acceptance probe return exit 1. The artifact is intentionally marked diagnostic.
The smallest next implementation is to reconcile the endpoint verifier with the
already-established selective-surfacing behavior **while preserving actual
surface/headroom/traversal safety checks**, then rerun physical readback and a
short traversal of that endpoint. Do not pave it, move it, or weaken a gate just
to get a green report. This task leaves that verification change explicit and
unimplemented under the instruction to preserve existing checks.

## Runtime evidence and limits

The final installation is `/tmp/nearmiss-fixture-930010639/final/server`, bound
to `127.0.0.1:25639`; all disposable JVMs are stopped after validation. Paper and
plugin SHA-256 hashes are in `server-preparation.json`. Java is the installed
Minecraft Java 21 runtime. Difficulty easy and gameplay server properties came
from the existing Alpha installation. The copied seed metadata has ordinary
random-tick speed 3 and mob spawning enabled; Match applies its normal running
clock/regen settings. No inspection tick-freeze rules were injected.

`preflight.json` and `server.log` are the final run. Committed text logs have
console control escapes and trailing whitespace removed for readability. The first source failure and
its Alpha-default warnings remain in `first-load-failed.log`; the isolated
config/goat-registration failure is in `registration-failed.*`. After the goat
mapping, `registration-passed.json` records successful binding before the later
spawn check. These historical attempts are not pooled as task trials.

`kill-command-probe.*` records an earlier `/kill` check that did not produce a
confirmed death/respawn despite command feedback. The final probe uses finite
`damage NearPreflight 100 minecraft:generic`, records actual client death and
respawn events, and checks both team locations. Finite-damage respawn works;
the `/kill` behavior is an unresolved administrative-command limitation, not a
reason to alter vitals balance in this task.

Authored corridors are physical paths. Runtime `Routes` recognition still
requires the existing player designation/traversal procedure; no free recognized
Route or movement benefit was registered. Similarly, source registration does
not prove an available manifestation: the normal initial delay is unchanged and
no source harvest or task rehearsal was performed. Client rendering and full
survival undertaking feasibility remain untested.

## Artifacts and identity

- `build.json`, `portfolio.json`, `routes.json`, `structures.json`,
  `physical-readback.json`: exact selected/build results and failed gates.
- `resource_light.json.gz`: diagnostic configuration diff, reproducible by the
  existing exporter; same payload across two fresh builds.
- `server-config.yml`, `server-preparation.json`: generated fixture config,
  registration identities, code/binary hashes and config changes. Paths are the
  recorded run's absolute paths; regenerate for a different machine.
- `package.json`, `compatibility.json`, `repeatability.json`: file hashes,
  archive checks, runtime seed and base identity evidence.
- `artifacts/worldgen/nearmiss_fixture_2026-09-22/packages/base-terrain.tar.gz`
  (at repository root): 18,174,729-byte base snapshot archive. Generated world
  data is kept apart from code and this report. This is a deliberate experimental
  artifact publication, not a playable-map release.

Base region fingerprint:
`5729f9396dbb0dc4d1e1b94d81b3d8f40cf3e0dcc56fa168273d58db5ea3486a`.
Alpha's base is `d131aff3c5f9523a21424dee76f3722b52e85d6cf243eb84607a45887d52008d`.
They are incompatible; never install this diff over Alpha's base.
“Pristine” here means unchanged from the retained, unplayed harvested snapshot
for this build. It does not establish equivalence to untouched native generation;
the harvest's original provenance limitations still apply.

## Exact reproduction

From repository root, choose a fresh disposable directory. The committed archive
removes dependence on the original machine's untracked harvest; it contains both
terrain and the original seed-specific level metadata. Python build code is
stdlib-only. Server preparation additionally uses PyYAML 6.0.3; protocol modules
use the existing package lock. `RUNTIME` must point to an existing accepted-EULA
Paper 1.21.11 installation (only files are copied/read, never operated).

```sh
REPO="$PWD"
FIXTURE=/tmp/nearmiss-fixture-930010639-repro
REPORT="$FIXTURE/report"
RUNTIME=/private/tmp/alpha-server
export JAVA_HOME='/Users/iracarranza/Library/Application Support/minecraft/runtime/java-runtime-delta/mac-os-arm64/java-runtime-delta/jre.bundle/Contents/Home'
mkdir -p "$FIXTURE/input"
tar -xzf artifacts/worldgen/nearmiss_fixture_2026-09-22/packages/base-terrain.tar.gz -C "$FIXTURE/input"
PYTHONPATH=implementation/worldgen python3 -m experiments.nearmiss_fixture \
  --source "$FIXTURE/input/base-terrain" \
  --source-world "$FIXTURE/input/base-terrain" \
  --candidate implementation/worldgen/results/staged_default_2026-09-09/finalists/930010639/candidate.json \
  --siting implementation/worldgen/reports/nearmiss_930010639_2026-09-22/siting.json \
  --frontier implementation/worldgen/reports/nearmiss_930010639_2026-09-22/scenario-frontier.json \
  --opportunity implementation/worldgen/reports/nearmiss_930010639_2026-09-22/opportunity-map.json \
  --output "$FIXTURE/worlds" --report "$REPORT"
```

**Expected current result: exit 1**, diagnostic diff written, with the ice
endpoint failure. Inspect it; do not treat it as a cleared fixture. To reproduce
only the independent load/spawn/reset validation despite that known static
failure (not gathering/crafting or task rehearsal):

```sh
(cd implementation/plugin && ./gradlew build)
python3 -m venv "$FIXTURE/venv"
"$FIXTURE/venv/bin/pip" install PyYAML==6.0.3
"$FIXTURE/venv/bin/python" implementation/plugin/experiments/prepare_nearmiss_server.py \
  --server "$FIXTURE/server" --runtime "$RUNTIME" \
  --jar implementation/plugin/build/libs/minecraft-moba-0.1.0-SNAPSHOT.jar \
  --config implementation/plugin/src/main/resources/config.yml \
  --base "$FIXTURE/worlds/base-terrain" --report "$REPORT"
(cd validation/plugin/protocol && npm ci --ignore-scripts)
NODE_PATH="$REPO/validation/plugin/protocol/node_modules" node \
  implementation/plugin/experiments/nearmiss_preflight.cjs "$FIXTURE/server" "$REPORT"
```

The probe owns and stops its own server. Port 25639 must be free. Its current
expected exit is also 1 because static readback remains false; inspect individual
checks rather than calling that a server-load failure. No `deploy.sh` is used:
it contains Alpha-specific template/catalogue defaults. Normal source timing and
all gameplay coefficients remain the shipped values.

The actual final build used the original source paths recorded in `build.json`
and `/tmp/nearmiss-fixture-930010639/final/worlds`; archived replay preserves their
contents. A fresh replay may change diagnostic path strings, gzip headers and
runtime random state; compare decompressed diff payloads and base file hashes,
not timestamps. Do not present random source manifestations as deterministic.

## Validation and next step

Plugin build and **205 tests passed** (38 suites, zero failures/errors/skips),
including existing map configuration/reset/vitals coverage. Python compilation,
Node syntax and documentation/diff checks were run. Two independent builds gave
identical diff payloads; archive contents match all 16 original file hashes.
The failed endpoint check is intentionally retained, not counted as passing.

Next: resolve the specific endpoint verification contract and rerun that check
plus a short endpoint traversal. Then choose an explicit common authored-resource
Task B for both configurations (current wheat task is impossible here), and only
then conduct the audit's minimal Task A/B feasibility rehearsal. There is no
basis yet for the 16 trials or any balance/screening change.
