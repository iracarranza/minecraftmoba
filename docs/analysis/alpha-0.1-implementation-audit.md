# Alpha 0.1 implementation gap audit

Status: **Audit. No fixes applied.**
Date: 20 September 2026
Amended: 20 September 2026 — three Alpha decisions recorded, see
[`alpha-0.1-decisions.json`](alpha-0.1-decisions.json). P0.3 and P0.7 are no
longer design-blocked; the P0 pass is held pending victory (ALPHA-D3).
Question: *What specifically prevents a fresh checkout from launching, completing, ending, resetting and replaying a solo Alpha 0.1 match?*

---

## 1. Executive finding

**There is no match.** There is a substantial, working sandbox of economic and
progression systems, and no code that starts, runs, ends or resets a contest
between two teams. The four missing pieces are teams, a match clock, a victory
condition and a reset path. Everything else in the lifecycle either exists or is
a small integration on top of something that exists.

Three findings reframe the work:

1. **The largest gap is unmerged, not unwritten.** `main` carries 20 plugin
   classes; `codex/phase1-plugin` carries 36 and 27 unmerged commits. Nearly
   every gameplay system — Renewables, Hud, Routes, InfraMode, TaskEffects,
   Durability, Contributions, HubLobby — exists only on the branch. Auditing
   `main` alone would report most of the game as absent when it is written,
   tested and stranded.

2. **The branch is safe to merge.** A trial merge of `codex/phase1-plugin` into
   `main` completes with **zero conflicts**: 42 files added, 8 modified, 4,164
   insertions, **2 deletions**. Every piece of main's newer worldgen work
   survives. See §5.

3. **Victory is a genuine design gap, not an implementation gap.** `objectives.md`
   says so itself: *"The exact transition from a disabled Fountain to final match
   victory also remains subject to objective-system development."* By contrast
   the match clock **is** fully specified and simply unimplemented.

---

## 2. Implementation inventory: main vs Phase 1

| | `main` | `codex/phase1-plugin` |
|---|---:|---:|
| Plugin classes | 20 | 36 |
| Unmerged commits | — | 27 |
| Datapack (advancements, dialogs) | absent | present |
| `config.yml` | absent | 152 lines |

**Only on the branch:** Sentinel, Renewables, RenewableAuthoring, RenewableKinds,
Hud, HealthDisplay, Minimap, Pings, Recall, InfraMode, Routes, HubLobby,
RewardAdvancements, TaskEffects, Durability, HungerRegen, LockedSlots,
MaterialCategories, Contributions, TestBed.

**On both:** MobaPlugin, PlayerData, PlayerDataCodec, Provenance, ProvenanceBits,
Settings, Rewards, RewardCatalog, Ability, AbilityInputs, PacketInputs,
TestAbilities, InventoryGuard, OffhandMap, Capacity, NativePacketQueue, and the
probe/fixture classes.

---

## 3. System classification

Classified from constructors, listener registration, command routing and state
ownership — not from class names. All file references are on
`codex/phase1-plugin` unless stated.

### RUNNABLE — reachable today through a gameplay or admin path

| System | Evidence |
|---|---|
| Player enrollment | `/moba join` → `MobaPlugin:210`, `PlayerData`, persisted via `PlayerDataCodec` + PDC |
| Class assignment | `/moba setclass` → `MobaPlugin:382` |
| Level / XP / rewards | `/moba setlevel`, `/moba xp` → `MobaPlugin:389,397`; `Rewards`, `RewardCatalog` |
| Task effects | `TaskEffects` as player attributes, registered `onEnable` |
| Durability model | `Durability`, listener-registered |
| Hunger/regen rules | `HungerRegen` |
| Locked inventory slots | `LockedSlots`, incl. death/respawn handling at `:96,:102` |
| Health display | `HealthDisplay` |
| HUD | `Hud`, listener-registered |
| Minimap | `Minimap` + `OffhandMap` (branch replaces `StubRenderer` — the only 2 deleted lines) |
| Pings | `Pings`, incl. sneak+pick infra-mode entry |
| Recall | `Recall`, offhand-slot channel |
| Infra mode | `InfraMode` + `/moba infra enter|exit|toggle|status` (`MobaPlugin:241-244`) |
| Route designation | `Routes` (banner A → travel → banner B) |
| Renewables | `Renewables`, `RenewableAuthoring`, `RenewableKinds`; acceptance-tested |
| Contribution fork | `Contributions` + `/moba contribution options|choose|capitalize|allocate` (`MobaPlugin:263-291`) |
| Sentinel item | `Sentinel` |
| Reward advancements | `RewardAdvancements` + datapack under `resources/datapack/` |
| Abilities | `Ability`, `AbilityInputs`, `PacketInputs`, `TestAbilities` |
| Test bed | `TestBed` |

**Every row above is branch-only.** On `main` the list reduces to enrollment,
class/level/XP, rewards, abilities and inventory guarding.

### IMPLEMENTED / UNINTEGRATED — substantive code, not driven by any match

| System | Why unintegrated |
|---|---|
| `HubLobby` | `build(sender, world)` is an admin construction command. Header says *"Presentation only."* No lobby exists at startup; portals run commands rather than joining a match. |
| `Renewables` | Manifests and recovers correctly, but nothing initializes a renewable population *for a match* or clears it between matches. |
| `Routes` | Designation works; no match owns or resets Route state. |
| `Contributions` | `capitalize(worksiteId, team)` takes a team as a bare `String`. Nothing assigns teams, and nothing ever activates a Worksite for it to capitalize. |
| `InfraMode` | Enter/exit works; no match phase gates it. |
| `TestBed` | Deliberately a test harness, not a match feature. |

### PARTIAL — reachable but does not satisfy current design

| System | Gap |
|---|---|
| Death / respawn | Handlers exist in `MobaPlugin:191,198`, `LockedSlots`, `InfraMode`, `AbilityInputs`, `OffhandMap` — all **per-system cleanup**. None is a match-state transition. No respawn-at-Fountain, no respawn denial when a Fountain is disabled. |
| Class selection | Admin-only (`/moba setclass`). No player-facing selection flow. |

### DOCUMENTED ONLY — fully specified, zero implementation

| System | Specification |
|---|---|
| Match clock & phases | `docs/manuscript/2026-09-14-Economic-Calibration-Supplement.md:27` — day 6 min, night 6 min, 12-min cycle, 48-min analytical match, phases Day 0–6 / Night 6–12 / … / Night 42–48 |
| Sunset pulses | same, `:29` — sunsets at **6, 18, 30, 42 minutes** |
| Worksite sunset activation | `objectives.md:414` — a limited number activate at sunset, count depends on match phase, chosen randomly from the eligible pool; close at sunrise |
| Phase vocabularies | same doc `:84` — first sunset Copper/Coal, second Iron/enchanting, third Gold/Lapis/Diamond, fourth Diamond/Ancient Debris |

These need no decisions. They need code. **Zero occurrences** of `getTime`,
`sunset`, `TimeSkipEvent` or any clock in the plugin.

### ABSENT / UNRESOLVED

| System | Evidence of absence |
|---|---|
| **Teams** | `PlayerData` (`:8-20`) has `classId`, `level`, `xp`, `choices`, `task`, `contribution`, `modeState` — **no team field**. `team` appears only as a method parameter in `Contributions`. Nothing assigns anyone to a side. |
| **Match state** | No match start, end or reset anywhere. No class owns match lifecycle. |
| **Match clock** | See above — nothing reads or drives world time. |
| **Worksite activation** | `Contributions:94` refers to *"an activated Worksite"*; nothing activates one. Dormant→Activated→Capitalized has only the third step. |
| **Objectives as code** | The 8 team structures exist as **terrain only** (`build_structures.py`). No plugin code references a Fountain, Bastion, Outpost or Tower. |
| **Victory** | **Zero occurrences** of `victory` in any source file. |
| **Alpha world loading** | No `getWorld("…")`, `WorldCreator` or `loadWorld` anywhere. The frozen map can only be used by being the server's default world, placed by hand. |

---

## 4. Lifecycle trace and exact breakpoints

| Stage | State | Break / owner |
|---|---|---|
| fresh checkout | OK | — |
| **build** | **BREAK** | **ENVIRONMENT.** No JDK present (only Minecraft's bundled JRE; `/usr/bin/javac` is Apple's stub). `build.gradle.kts` declares **no `jvmToolchain`**, so the build silently depends on an ambient JDK. §6. |
| server/plugin startup | OK | `MobaPlugin.onEnable` registers every listener cleanly |
| **Alpha world load** | **BREAK** | **CODE.** Nothing loads a named world. The frozen Consolidative world must be installed manually as the server's default. |
| **lobby** | **BREAK** | **INTEGRATION.** `HubLobby` exists but is admin-triggered and presentation-only. |
| player participation | OK | `/moba join` |
| **team assignment** | **BREAK** | **CODE.** No team concept in player state. |
| class selection | PARTIAL | admin command only |
| **match initialization** | **BREAK** | **CODE.** No match object. |
| **homeland spawn** | **BREAK** | **CODE + CONTENT.** Homeland coordinates exist in worldgen artifacts; no plugin code consumes them. |
| **match clock** | **BREAK** | **CODE.** Spec exists (§3), no implementation. |
| **6m day / 6m night** | **BREAK** | **CODE.** Same. |
| **sunsets at 6/18/30/42** | **BREAK** | **CODE.** Same. |
| resource economy | RUNNABLE | `Renewables` — but not match-scoped |
| useful work / progression | RUNNABLE | `/moba xp`, `Rewards`, `TaskEffects` |
| level breakpoints / rewards | RUNNABLE | `RewardCatalog`, `RewardAdvancements` |
| class mechanics | RUNNABLE | `Ability`, `AbilityInputs`, `PacketInputs` |
| renewable systems | RUNNABLE | as above |
| **Worksite activation** | **BREAK** | **CODE.** Rules specified; nothing activates. |
| Worksite capitalization | IMPLEMENTED / UNINTEGRATED | `Contributions.capitalize` — unreachable without activation and teams |
| Infrastructure | IMPLEMENTED / UNINTEGRATED | `InfraMode`, `Routes` |
| **objectives** | **BREAK** | **CODE.** Terrain only. |
| PvP / death / respawn | PARTIAL | cleanup only, no match semantics |
| **victory condition** | **BREAK** | **DESIGN DECISION.** §8. |
| **match end** | **BREAK** | **CODE**, blocked on victory |
| **cleanup / reset** | **BREAK** | **CODE + DESIGN DECISION.** What resets — world, player state, renewables, Routes — is unspecified. |
| **second match start** | **BREAK** | depends on all of the above |

---

## 5. Branch reconciliation assessment

**Verdict: safe to merge, in the direction Phase 1 → main.**

- Trial merge (`git merge --no-commit --no-ff`): *"Automatic merge went well."*
  **0 conflicts.** 42 added, 8 modified. Aborted cleanly.
- Main's newer work survives the merge, verified file-by-file:
  `map_authoring_optimizer.py`, `vanilla_search/structures.py`,
  `expedition/benchmark.py`, `terrain_harvest/rescan.py`,
  `alpha-0.1-map-freeze.json` all present.
- Only **2 deleted lines**, both removing `StubRenderer` from `OffhandMap` —
  an intentional supersession by the real `Minimap` renderer, not a loss.
- Only **one** `main` commit has touched `implementation/plugin` since the
  merge-base (`c811002`, itself a merge). There is no competing plugin work on
  main to reconcile.

**Direction matters.** The branch is far *behind* main outside the plugin —
472k deletions' worth, including the entire worldgen optimizer, expedition
tooling, terrain tests and the Alpha map freeze. Merging main → branch, or
working on the branch, would discard all of it. Merge the branch into main.

**No branch implementation contradicts current canon** as far as this audit
established. Two branch files carry explicit self-declared limits rather than
contradictions: `Routes.java:23` (movement axis not implemented) and
`Contributions.java:29` (class×contribution matrix unresolved in canon).

**Gate the merge on:** the 9 existing plugin unit tests (`ConfigYamlTest`,
`InventoryGuardTest`, `PlayerDataTest`, `NativePacketQueueTest`,
`MaterialCategoriesTest`, `ProvenanceEventsTest`, `RewardsTest`,
`AbilityInputsTest`, `ProvenanceBitsTest`), a successful `./gradlew build`, and
the worldgen suite (46 tests) to prove nothing on main regressed.

---

## 6. Build environment assessment

**Not reproducible today.**

- No JDK on this machine. `/usr/bin/java` and `/usr/bin/javac` are Apple's
  "Unable to locate a Java Runtime" stubs. The only real Java is Minecraft's
  bundled **JRE** (`java-runtime-delta`), which cannot compile.
- A jar exists at `build/libs/minecraft-moba-0.1.0-SNAPSHOT.jar`, newer than all
  sources, and the Paper server is running with it. **That is not
  reproducibility** — it proves a JDK existed earlier, not that a fresh checkout
  can build.
- ~~`build.gradle.kts` declares no toolchain.~~ **CORRECTION, 20 September:** it
  does. `java { toolchain.languageVersion.set(JavaLanguageVersion.of(21)) }` and
  `options.release.set(21)` were already present; the original audit grepped for
  `jvmToolchain` and missed this spelling. The build was already pinned. The
  only real gap was the absent JDK.

**Required:** JDK 21 (Paper 1.21.11 / `paper-api:1.21.11-R0.1-SNAPSHOT`),
Gradle 8.14.3 via the committed wrapper.

**Expected reproducible command**, once a JDK 21 is installed and a
`jvmToolchain(21)` block is added:

    cd implementation/plugin && ./gradlew build

---

## 7. Alpha 0.1 critical path

### P0 — blocks a complete solo match or reproducible development

Dependency-ordered. The hypothesised ordering held, with two corrections noted.

| | Item | Kind | Depends on |
|---|---|---|---|
| **P0.0** | JDK 21 + pin `jvmToolchain(21)` in `build.gradle.kts` | ENVIRONMENT | — |
| **P0.1** | Merge `codex/phase1-plugin` → `main`, gated on tests | INTEGRATION | P0.0 |
| **P0.2** | **World instance lifecycle**: template → instance, load, unload, restore | CODE + CONTENT | P0.1 |
| **P0.3** | Authoritative match + team + participation state; team field on `PlayerData` | CODE | P0.1 |
| **P0.4** | Match bootstrap: `/moba match start`, homeland spawn from worldgen coordinates | CODE | P0.2, P0.3 |
| **P0.5** | Match clock: 6m day / 6m night, sunsets at 6/18/30/42 | CODE (spec exists) | P0.4 |
| **P0.6** | **Victory condition** | **DESIGN-BLOCKED** (ALPHA-D3), then CODE | P0.3 |
| **P0.7** | Match end + replay | CODE; end *trigger* blocked on P0.6 | P0.2, P0.5, P0.6 |

**Corrections to the hypothesis:**

- World loading (P0.2) must precede match bootstrap, since spawning into a
  homeland requires the Alpha world to be the loaded one.
- "Integration of existing systems with match state" is **not P0** — a solo
  match can complete without Worksites activating. It is P1.
- **ALPHA-D2 merges reset into world loading.** Because reset is defined as
  restoring a pristine template rather than rolling back blocks, loading a match
  and resetting one are the same operation: unload the instance, copy the
  template, load it. P0.7 is consequently much smaller than first estimated —
  it retains the match-scoped state clearing and the end trigger, but the world
  half of it is P0.2.

**Shortest dependency chain to a first complete solo match:**
P0.0 → P0.1 → P0.2 → P0.3 → P0.4 → P0.6 → P0.7.
Of these, only **P0.6 is design-blocked**; P0.0–P0.5 and the reset half of P0.7
are implementable as soon as the hold lifts.
P0.5 is not strictly required to *finish* a match, but without it the match has
no phases, so it is retained in P0 as the thing that makes the match a match.

### P1 — match completes, but a major Alpha system cannot be exercised

| Item | Note |
|---|---|
| Worksite activation at sunset | Rules specified in `objectives.md:414`; needs P0.5 |
| Worksite capitalization reachable | `Contributions.capitalize` needs activation + real teams |
| Objectives as code | Fountain/Bastion/Outpost/Tower are terrain only |
| Respawn semantics | respawn at Fountain; denial when disabled — needs victory design |
| Match-scoped renewables | initialize per match, clear on reset |
| Player-facing class selection | currently admin-only |
| Lobby as an entry point | `HubLobby` exists; wire it to match join |
| Match-scoped Routes / infra state | ownership and reset |

### P2 — after the first complete executable match

Phase vocabularies for sunset Worksites; resource-pack HUD/icons; ping wheel;
minimap polish; TestBed expansion; balance tuning of anything measured here;
Alpha map re-validation.

---

## 8. DESIGN DECISION REQUIRED

Blocking implementation. **No mechanics are proposed here.**

1. **P0 — Victory condition. STILL OPEN (ALPHA-D3).** `objectives.md:233`: *"The
   exact transition from a disabled Fountain to final match victory also remains
   subject to objective-system development."* Must be decided before P0.6: what
   state ends a match, whether Fountain disable is sufficient or a precondition,
   whether elimination-with-no-respawn is terminal, and what happens at the
   48-minute horizon if neither side has won. **The P0 pass is held on this.**
2. ~~**P0 — Match reset scope.**~~ **DECIDED (ALPHA-D2).** Each match runs on a
   disposable, restorable instance of the frozen Alpha world; reset restores the
   pristine template and clears all match-scoped plugin and player state.
   Piecemeal block rollback is explicitly rejected.
3. ~~**P0 — Team assignment rule.**~~ **DECIDED (ALPHA-D1).** Every participating
   player holds exactly one North/South assignment, total and exclusive. Alpha
   requires only admin/debug assignment; automatic formation is out of scope.
4. **P1 — Respawn destination and denial.** `Recall.java:83` records the recall
   destination as unresolved; the same question governs respawn.
5. **P1 — Fountain disable mechanics.** `objectives.md:225-233` lists
   reactivation, repair and relationship to preceding objectives as unresolved.
6. **P2 — Class × contribution matrix.** `Contributions.java:29` records it as
   unresolved in canon.

---

## 9. Definition: "Alpha 0.1 solo-playable"

Operational, testable. The milestone is met when **all** hold:

1. A fresh checkout builds with `./gradlew build` on a pinned JDK 21, with no
   manual toolchain setup beyond installing that JDK.
2. A server starts with the plugin loaded and the **frozen Consolidative Alpha
   world** loaded by name, without hand-editing the repository or the world.
3. One human joins, is assigned a team, and selects a class — admin commands
   are acceptable for all three.
4. `/moba match start` (or equivalent) initializes a match and spawns the player
   at a homeland.
5. The match clock advances, day/night alternates on the 6/6 schedule, and
   sunset events fire at 6, 18, 30 and 42 minutes.
6. The player can exercise the major systems: gather from renewables, gain XP
   and levels, receive level rewards, use class abilities, designate a Route,
   enter infra mode.
7. The victory condition can be reached — admin-forced is acceptable for Alpha,
   provided the condition itself is implemented rather than simulated.
8. The match ends, state resets, and a second match starts **without repository
   or world surgery**.

Explicitly **not** required: 14 humans, bots, balance validation, final art, or
any system in P2.

---

## 10. Not covered

- Runtime behaviour of branch systems beyond what earlier acceptance scenarios
  exercised; this audit read code and registration, and did not re-run the
  server.
- The datapack's advancement graph beyond its presence.
- Whether the 48-minute analytical match is the intended *played* length; the
  manuscript marks match duration as a working target, not canon.
- Hostile exposure underground, still **UNRESOLVED** and untouched by this audit.

---

## Addendum — Reconstruction (2026-09-20)

This audit's "Respawn semantics — respawn at Fountain; denial when disabled —
needs victory design" understated what was already established. Respawning at
the Fountain is not a teleport that hands back a whole player: the established
mechanic is **Reconstruction**, now recorded as `ALPHA-D4` in
`alpha-0.1-decisions.json`.

A player who dies with a functioning friendly Fountain reappears there at once
at roughly 1 Health and 1 Hunger, keeping inventory, and then reconstructs at
**fixed absolute** rates for as long as they stay. They may leave part-built. A
living player may return and use the same reconstruction.

Two things this corrects in the implementation:

- respawn previously returned a player at full Health and Hunger, which removed
  the entire cost of dying;
- the first Fountain restoration pass topped up saturation alongside Hunger.
  Current authority specifies Health and Hunger only, so saturation is no longer
  touched and reconstructed Hunger drains like ordinary Hunger.

Fountain disablement stops reconstruction instantly but does not harm anyone
mid-reconstruction: they keep their partial values and their normal maxima, and
only their *next* death is permanent. That is the mechanism `ALPHA-D3` resolves
victory through.

Alpha's irreversible disablement remains a lifecycle simplification. The broader
design's distinction between temporary obstruction/repair and permanent physical
destruction stands and is not rewritten by it.
