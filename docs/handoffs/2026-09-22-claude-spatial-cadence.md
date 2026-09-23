# Continuation for Claude — superspatialdoctrinespec

## Authorization, branch and boundaries

The latest user explicitly requested a pause/handoff/push, so Codex stopped
rather than finishing the substantial migration. Resume only as the user's next
agent/task. Active source is [the full spec](../../specs/superspatialdoctrinespec.md).
It supersedes the earlier “documentation only” restriction: executable logic,
config, tests and necessary fixtures MAY change. It does not authorize invented
balance, expensive trials, blanket regeneration, new Map Types or unrelated
combat/class systems.

Branch `codex/spatial-cadence-migration`, isolated worktree
`/tmp/minecraftmoba-spatial-cadence`. Parent **03c5bea** includes the prior completed
spatial doctrine audit; its parent work includes Hunger audit **9bb5093**, based
on main **8199c13**. Main was read-only and not merged/advanced here.

Preserve `/Users/iracarranza/minecraftmoba` dirty files (opportunity_map.py,
caves.py, assorted pasted Markdown, artifacts, .DS_Store, user specs). Existing
near-miss work is in `/tmp/minecraftmoba-terrain-audit`. Never reset/clean these.
The live server `/private/tmp/alpha-server` is NOT a disposable target. Nothing
here has operated/redeployed it.

## What the new spec resolves

- Compact mirrored Core ends at explicit interfaces, not a radius. Independently
  acceptable natural Sockets need not resemble each other.
- Small Opening Hinterland supports beginning every fundamental verb without
  resolving it. Wilderness can exist beside AND behind/poleward of Homebase.
- Known opening ceilings: villages, carrots, equipment-sufficient accessible
  iron; NOT “no iron”. No unsupported quota. Serious violations reject sockets.
- Authored Strategic Depth is a spatial-role classification, not just Fountain
  distance/Practical Reach. Player infrastructure changes runtime reach only.
- Concurrent defensive objectives are Wilderness structures, ordinal along team
  axis: midline → Outpost → Bastion → End Spike → Fountain. Variable W–E and
  spacing; no prerequisite invulnerability chain.
- Forms: Outpost WATCHTOWER only; Bridge Bastion rampart/central body WITHOUT
  projecting bridge; End Spike is obsidian pillar + crystal (+ applicable cage),
  NOT End City/generic End Tower. Measure selected form dimensions, don't guess.
- One permanent conspicuous shared Lair, competitively central by BOTH teams'
  initial reach (not coordinate center). Terrain-shaped, modifiable, no new
  reveal schedule or blanket anti-cheese protections.
- Exceptional sequence: Worksite I → Giant → Worksite II → Ghast → Worksite III
  → Dragon. Alternate on successive existing opportunity nights.
- Boss survives dawn and Worksite nights, dies to leave dormant Lair, or is
  replaced on next Lair night. Missing/killing it doesn't stall clock.
- Pairings Giant/Outpost, Ghast/Bastion, Dragon/Spike are current. Exact siege
  advantage and boss/objective XP remain OPEN. Do not restore direct damage or
  toppling-method XP hierarchy.
- Worksite I anchors iron/coal, Blast Furnace/Smoker; II second-tier resources,
  Enchanting Table/Anvil; III stronger package OPEN. Preserve more detailed
  Mining Outpost / Industrial Enchanter capability and capitalization systems.
- Preserve Renewable Kind identity without depth/value/region. Keep lifecycle;
  wild crop manifestations are irregular resource occurrences, not prepared farms.

## Inspection completed BEFORE writing drafts

Read active spec (initial output truncated, then separately read §§15–22,
including exact pairings, persistence and alternation). Read current canonical
objective sections 2–8, 11, 16–17B, 21; prior spatial audit/reconciliation and
HANDOFF; current Match, MatchClock, Worksites; dependency search across production
Java; WorkPoints costs; Renewables/Recovery/WorldTerrain dependencies; relevant
config and MatchClock/WorksiteLifecycle/MatchReset tests; worldgen structure
siting/massing; historical wiki objective entries and initial objective commit;
14 September economic calibration record and relevant history.

Runtime trace findings:

| Source / consumer | Current behavior and migration consequence |
|---|---|
| `Match.elapsed` | Sole match tick source; scheduled 1-tick callback. `skipMinutes` increments SAME counter, invoking sunset/sunrise boundaries. Preserve one source, ideally one shared boundary handler. |
| `MatchClock` | Vanilla 24,000-tick day/night cycle, sunset at 12,000. Current first sunset=10m, then 30/50/70/...; `sunsetOrdinal` is one-based. `Phase {DAY,NIGHT}` is lighting, NOT Overworld/Nether/End gating. Do not delete unrelated phase uses. |
| Old horizon | `MATCH_MINUTES=80` inferred from four Worksite sunsets. `Match.tick` still announces “48-minute” horizon whenever `pastHorizon` holds and increments elapsed again. This both spams and skips boundaries after horizon. Remove obsolete horizon behavior; no timed victory is canonical. Do not invent a new 120-minute match duration just because Dragon is at sunset 6. |
| Worksites | Every sunset calls `onSunset(ordinal)`. Config `activationsPerSunset: [2,2,3,3]` has stale 6/18/30/42 comment. Chooses DORMANT eligible sites randomly; ACTIVATED returns DORMANT at dawn; CAPITALIZED/EXPLOITED persist. No resource/facility packages currently generated. |
| Contributions | First Worksite capitalization awards shared team Infrastructure opportunity through existing Contributions; must survive migration. No automatic award on activation. |
| Objective runtime | No actual team-defensive-objective lifecycle existed beyond Fountain/physical greyboxes. No top-level Overworld/Nether/End enum actually controls them. Add concurrent identity/state seam; don't pretend a phase-gate was removed where none existed. |
| Fountain/victory | Existing match Fountain disabled state + survivor elimination; leave semantics intact. Repair/exposure remain OPEN. |
| World time | `Match` sets world time from elapsed; daylight cycle disabled while running. `WorldTerrain.isNight` reads world time. |
| Renewables | Scheduled `tickOpportunities` uses current world's night and `Recovery.Rates`; temporal `phaseTicks` is duration of DAY/NIGHT, not obsolete macro stages. Preserve its values and semantics. |
| Progression/WP | Per-level economic bands/costs, not driven by match elapsed. “phase” in WorkPoints cost comment is level band vocabulary. No automatic reward multiplier tied to sunset discovered. Preserve. |
| Persistence | Match/Worksite state is memory-only; startup does NOT resume a match. `WorldInstance.load` replaces stale on-disk instance from pristine base when loading after restart. Preserve this policy rather than invent mid-match recovery. Need explicit Lair cleanup on reset/end/disable and no orphan entities. |
| Commands/status | `/moba match status`, `skip`, start/reset; `/moba worksite status/list/capitalize/exploit`. Update output to opportunity stage/tier/Lair rather than ambiguous phase/horizon. |
| Ready maps | Existing architecture loads copies + selected block diff through `WorldInstance`, not a full READY→IN_USE→USED pool. Preserve target/provenance direction; don't claim implemented foundry. |

Dependency search was saved to `/tmp/cadence-dependencies.txt`; it is only an
inspection scratch file, not required artifact. No dependency audit document or
supersession matrix for this new spec has been written yet.

History recovered:

- `objectives.md` already retains Giant/Ghast/Dragon and pairings but frames them
  historically; it already explicitly rejects XP-by-toppling-method hierarchy.
- Initial commit `8b9de92:objectives.md` likewise has bosses; stale nested browser
  wiki `minecraft_moba_design_wiki/minecraft_moba_design_wiki/objectives.html`
  says direct damage and massive XP. Use as historical evidence, NOT authority.
- Canon objective §21 still says sequential objective gating/bypass is unresolved;
  new spec resolves that, so remove the apparent current contradiction.
- §17's Overworld→Nether→End→Aether thematic sequence must not remain top-level
  temporal doctrine. Themes may remain, but all defenses coexist.
- `docs/reconciliation/2026-09-14-economic-calibration-recovery.md` claims 6+6
  minute timing and four Worksite pulses. Mark that temporal portion superseded;
  retain unrelated material/economic fixtures and level-band semantics.
- Detailed Mining Outpost / Industrial Enchanter work is more specific than a
  generic tier reward bullet list. Don't replace those verbs with free rewards.

## Draft code JUST ADDED — not wired or validated

Four files under `implementation/plugin/src/main/java/com/minecraftmoba/plugin/`:

1. **OpportunityCadence.java**: pure sequence mapping night ordinal 1..6 to
   WORKSITE_I, GIANT, WORKSITE_II, GHAST, WORKSITE_III, DRAGON; 0 OPENING;
   later UNSCHEDULED (post-Dragon policy OPEN). WorksiteTier and Boss enums.
   `at(elapsedTicks)` derives from existing MatchClock; no independent timer.
2. **TeamObjectives.java**: all three defensive identities STANDING per team;
   no time/prerequisite argument to `attackable`; validated-toppling seam without
   implementing unresolved validation or XP; objective pairing and ordinal list.
3. **LairLifecycle.java**: pure lifecycle over an Encounters adapter. Replaces
   survivors on Lair nights, preserves on other nights, duplicate/stale night
   guards, death makes dormant, no automatic siege award. UNBOUND/BLOCKED are
   honest missing-site/spawn-failure states. Records last SiegeOpportunity with
   optional team attribution, resets/cleans current occupant.
4. **Lair.java**: DRAFT Bukkit binding, one config `alpha.lair.site: [x,y,z]` or
   empty (not yet added to config); optional site bound to match world, vanilla
   entity spawning, persistent tag, natural distance-despawn disabled, death
   listener logs unresolved siege effect. Nothing instantiates/registers this yet.

**Known draft weaknesses to fix/review:**

- `Lair` removal fallback reloads only the socket chunk. A flying boss could
  unload elsewhere; correct cleanup needs loaded/owned entity tracking or chunk
  load removal of stale encounter IDs (and restart policy). Do not claim robust
  replacement yet. Ender Dragon/parts and unloaded death need consideration.
- Default vanilla Giant has no designed encounter AI, Dragon in Overworld may
  have arena/flight assumptions. No encounter AI, boss tuning, arena protections
  or XP suppression decision was made. The spec permits explicit unresolved
  seams; test native entity binding in disposable world before claiming playable
  boss encounters. Do not invent AI/balance to hide missing design.
- `Lair.bind` resets lifecycle but assumes cleanup of the previous world first.
  Integration must call cleanup before restore/unload, then bind fresh UUID.
- Attribution currently uses killer participant/alive; review RUNNING/match-world
  checks and state lifecycle. Unknown kill credit stays unknown, never award a
  guessed team. Pairing is recorded, not applied as damage/debuff.
- Physical toppling remains a seam; new state registry must not claim existing
  greyboxes are correct watchtower/Bridge Bastion/End Spike or certified sockets.
- No tests or compile were run on these classes. They may need API corrections.

## Planned next implementation (NOT performed)

1. Finish pure cadence and lifecycle tests; refactor Match boundary dispatch so
   tick/skip use the same handlers, without obsolete horizon double-increment.
   Lighting cycle stays current baseline; 6 events occur at 10/30/50/70/90/110m
   as a consequence of current clock, NOT a new fixed match duration.
2. Worksites should activate only on cadence Worksite nights and record I/II/III
   on selected sites. Preserve eligible-pool/state/capitalization semantics.
   Replace sunset-count config with explicit tier-scoped fixture configuration;
   either migrate old counts transparently with warning or clearly deprecate.
   Don't silently carry the four-sunset last-value-repeat into post-Dragon time.
   Packages/facilities must expose unresolved content instead of granting stock.
3. Wire one Lair and TeamObjectives through plugin init/listeners, Match open,
   start, sunset dispatch, status, reset/end/disable. Bind only explicit site
   input. Existing maps have no certified Lair socket, so report UNCONFIGURED,
   never choose a center or author a stadium silently.
4. Add spatial contract/candidate-analysis seam (likely worldgen Python module)
   representing explicit compact Core/interface and Hinterland support, all
   remaining cells as Wilderness including poleward, ordered Wilderness
   objectives, singular Lair and both-team reach evaluation. No universal circle
   or invented ellipse radius. Separate authored depth from live reach.
   Known village/carrot ceiling observations can fail a candidate; iron sufficiency
   needs a declared equipment target/measurement and stays unknown if missing.
   Independently acceptable Socket evaluation must not demand terrain matching.
5. `compare_seeds.py` STILL emits `weaker_team`, `deficit_kind` by default despite
   prior comment disclaimer. New spec requires analytics to stop interpreting
   wilderness differences as default defects: migrate output/CLI schema to
   descriptive contrast + explicit unknown competitive consequence, preserving
   raw metrics and historical JSON. Inspect downstream users/tests before rename.
6. Structure tooling currently `LAYERS` and `build_structures.TEMPLATES` use
   `end_tower` (End stone/purpur shaft), and Bastion is generic greybox, not selected
   vanilla Bridge body. Add current form/topology metadata and measurement seam,
   preserve old artifacts explicitly as historical. Don't relabel an old mesh as
   End Spike. Exact selected vanilla dimensions need actual measurement; if not
   yet chosen, block current-contract certification with explicit missing evidence.
7. Canonical docs/maps/objectives/infrastructure + relevant progression temporal
   wording + audit/supersession/reconciliation + commands/config/fixture docs.
   Keep wiki non-authoritative; no need wholesale browser resync.
8. Validate focused Java tests, relevant worldgen tests and a small disposable
   runtime transition test using skip, not six nights of elapsed gameplay.
   Cover survivor replacement, killed dormant, no dawn despawn, non-stalling,
   reset/restart ownership, concurrent objectives, no prerequisite attack gating,
   compact Hinterland/poleward Wilderness, objective ordering/role, one Lair,
   both-team reach, ceiling evidence without made-up quotas.

## Tools / validation environment

Java 21 JDK (not discoverable via java_home):
`/Users/iracarranza/Library/Application Support/minecraft/runtime/java-runtime-delta/mac-os-arm64/java-runtime-delta/jre.bundle/Contents/Home`

From `implementation/plugin`, set JAVA_HOME above and run `./gradlew test jar`.
Gradle has Paper API 1.21.11, JUnit 5, Mockito 5.18, SnakeYAML. Existing tests
include pure functions and some source-string lifecycle checks; prefer meaningful
pure transition tests and real disposable wiring coverage where possible.

Existing locked Mineflayer dependencies:
`/tmp/minecraftmoba-terrain-audit/validation/plugin/protocol/node_modules`.
Existing PyYAML 6.0.3 venv: `/tmp/nearmiss-fixture-930010639/venv`.
Paper runtime may be read/copied from `/private/tmp/alpha-server`, never operated
there. Prior stopped disposable test roots `/tmp/hunger-acceptance` and
`/tmp/minecraftmoba-hunger-repro`; avoid overwriting their evidence. Use a NEW
named disposable directory and free localhost port. No runtime for this new
migration has been prepared yet.

## Remaining completion requirements

Everything beyond the four drafts is still outstanding. No claim of passing
migration tests, coherent new runtime, or completed new canonical reconciliation
is justified yet. Final requested deliverable must include full supersession
matrix, actual dependency migration, config/fixture/test changes, unresolved
seams and next empirical work, then commit/push working branch. User's current
request was only to pause and hand this partial state to Claude.
