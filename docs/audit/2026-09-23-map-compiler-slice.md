# Default-map compiler vertical slice — 23 September 2026

Branch `codex/spatial-cadence-migration`, over the 14cb46c cadence migration.
Spec: `specs/claudeversionsuperspatialdoctrinepsec.md`.

The target was one unseen seed compiled end to end into a PlayableMap. That was
not reached. What was: an unseen seed, 99887766, passing every analytical stage
-- recognition, both Homebase sockets, compact Hinterlands, an ordered objective
layout, and a Lair socket at A_L 0.068 -- and stopping at exactly one physical
blocker, the absence of an End Spike mesh.

Alongside it: a compiler that fails by name at a named stage, four real defects
found by running things rather than reading them, and the first Lair access
measurement the project has ever had.

---

## 1. Physical objective forms

All three were unmeasured, and `objective_forms.certify` failed closed for every
candidate. All three are now measured, from Minecraft itself.

| Form | Source | Span | Height | Ground contact | Interior |
|---|---|---|---|---|---|
| Pillager Outpost | `pillager_outpost/watchtower.nbt` | 15 | 21 | 125 columns | yes |
| Nether Bastion | `bastion/bridge/` entrance + ramparts | 32 | 32 | 506 columns | yes, multilevel |
| End Spike | generated vanilla End, measured from region files | 11 | 76–103 | ~89 columns | no |

The Outpost and Bastion come out of the client jar's own structure NBT, which
makes both exclusions **file-level cuts rather than judgements about one mesh**:
vanilla already ships the cages, tents, log piles and plates as separate
`feature_*.nbt`, and the projecting bridge and its legs as separate pieces from
the entrance body and ramparts.

The End Spike is not a structure file. Vanilla generates the arena spikes in
code, so nothing in the jar describes them. They were measured from a real
generated End dimension instead — a disposable Paper server with a probe
plugin, because Paper's console cannot switch into the End — then read out of
the produced region files. Ten spikes, radius 2–5, height 76–103, bedrock cap on
every one, **two of ten caged**, so "cage where applicable" is a measured
minority rather than a guess.

**The contracts are deliberately not one bounding box.** A single span number
would have hidden the constraint most likely to reject a socket: the Spike needs
up to 103 blocks of clear sky over an 11-block footprint and has no interior at
all, while the Bastion needs 506 columns of ground contact and a multilevel
interior. `site_requirements()` reports them separately.

**Remaining uncertainty.** The Spike's vertical clearance and the Bastion's
interior fit cannot be verified from an eight-block surface sample grid — there
is no column data in the committed feature grid. Recorded in
`structures.UNVERIFIABLE_FROM_SAMPLES` and surfaced as compiler evidence rather
than assumed away.

`end_tower` remains HISTORICAL and is still **not** renamed. Siting now uses an
`end_spike` layer because a layer is a siting slot, not a mesh; `build_structures`
deliberately does **not** register the old mesh under that name, so authoring
fails closed instead of siting a Spike and quietly building a Tower.

---

## 2. The Lair

### Functional socket contract

`terrain_harvest/lair_socket.py`. Physical viability filters; access parity is
measured and, for the first time, gated. The two are kept apart on purpose:
terrain contrast between the ends is permitted everywhere, and the single
indivisible shared objective is the one exception doctrine actually names.

Viability: usable ground for the Giant encounter and for players, an open-volume
proxy because the Dragon is the limiting case, several practical approaches, and
a keep-out radius so the Lair cannot land inside a compact opening Hinterland.

**The Dragon's air volume is approximated, and labelled.** An eight-block
surface grid has no column data, so openness is inferred from how far the
surrounding terrain stays below the rim — an open basin reads differently from a
slot canyon. Reported as `UNVERIFIED`, not asserted.

### A_L, measured at last

Best achievable asymmetry per seed, each over ~11.8k–12.3k considered cells:

| seed | viable sockets | best A_L |
|---|---|---|
| 930016664 | 206 | **0.001** |
| 930015734 | 195 | **0.050** |
| 930005557 | 432 | **0.073** |
| 930019528 | 595 | 0.226 |
| 930012642 | 412 | 0.294 |
| 930007222 | 51 | 0.415 |
| 930006815 | 308 | 0.678 |
| 930010639 | 147 | 0.787 |

It discriminates hard, and it disagrees with the metric it replaces.
**930010639 — the near-miss a previous pass spent an entire rescue test
authoring to a 0.0161 balance scalar — has the worst Lair access parity of all
eight.** The superseded measure said that seed had been rescued; the measure
doctrine endorses says it is the worst of the set.

### Lifecycle smoke test, and the defect it found

Disposable Paper server, real `Lair` and `LairLifecycle` driven by a probe
plugin through all six nights, with the Giant left alive through Worksite II and
the Ghast deliberately moved 300 blocks off its socket.

The Ghast broke it. The lifecycle reported ALIVE while the runtime could not
find the occupant at all: `Bukkit.getEntity` returns null for an entity in an
unloaded chunk, and the only fallback was to load the **socket** chunk, which is
the one place a boss that can fly is least likely to be. The tagged sweep then
scanned loaded worlds only, missed it, and reported success. A replaced Ghast
would have stayed in the world beside the Dragon that replaced it, and since
match state is memory-only nothing would ever have looked for it again.

Classification: **LIFECYCLE DEFECT** — blocks the pass, so fixed. Unreachable is
not gone. `Lair` now tracks the occupant's last known location, refreshes it
before any replacement decision, tries entity then that chunk then the sweep,
and when all three fail queues the id and removes it on `ChunkLoadEvent`.
Verified end to end: both the queue message and the deferred removal appear in
the log, and with every chunk a boss occupied force-loaded first, zero remain.

Other findings, recorded not designed around:

- **VANILLA-ENGINE**: Giant, Ghast and Dragon all spawn and persist in an
  Overworld socket, and the Dragon is killable outside the End. No arena
  assumption prevented it.
- **ENCOUNTER-DESIGN DEFICIENCY**: encounter quality was not tested and is not
  claimed. No players were present, so the vanilla Giant's lack of designed AI
  stands. Does not block the compiler.
- **Hard-coded Alpha assumption**: `MapConfigurations.reload` throws when an
  Alpha map diff named in config is absent, which took the whole plugin down on
  a clean server. The run proceeded with the catalogue disabled.

---

## 3. Optimizer migration

`balance_asymmetry` was the optimization target: `char - 5.5*b - 25*max(0,
b-0.14)`, with nothing kept unless `b <= 0.12`. Balance dominated every profile.

Every measurement is **kept and still reported**. Only the interpretation
changed, to **hard gates then optimization among viable candidates**.

The gate is **reachability, not equality**. An opportunity a team cannot attend
to inside the night that opens it is a structural failure; one merely nearer a
team is ordinary competitive geography. Rejections carry machine-readable codes.

Both constants are measured:

- `PRACTICAL_REACH_BOUND_SEC = 600` — across both realized maps, worst-team
  reach has p90 400s, p95 491s, max 864s, so 600 excludes 2.4% as pathological.
  Sensitivity: gates nothing on either real map, rejects 13% of sampled
  portfolios at 400s and 97% at 300s.
- `OPENING_WORKSITE_REACH_SEC = 200` — each team's nearest Worksite sits at
  73–168s across 800 sampled portfolios on both maps, so 200 fires only when a
  portfolio strands a team out of the Worksite night entirely.

Consequence worth stating: with balance no longer optimized, measured
`balance_asymmetry` on 930010639 rises from ~0.016 to 0.13–0.17, and the map is
viable anyway on structural grounds. The effort that used to go into the scalar
was buying the scalar.

---

## 4. Recognizer

`terrain_harvest/map_compiler.py`:
`MapCandidate → CompetitiveCandidate → AuthoredCandidate → PlayableMap`, with
every rejection carrying its stage and a code.

Three defects found by running it, each fixed rather than worked around:

1. **The recognizer rejected every candidate ever screened.** It inherited
   `six_corridors_connect`, which fires on all eight finalists and is the
   straight-line corridor proxy an earlier pass had already shown to be a false
   positive — corridors follow terrain. Now recorded as deferred evidence; route
   fitting measures the real thing later.
2. **Objective siting chose the best site per layer independently**, and the
   results did not coexist: the End Spike, meant to sit inside the Bastion,
   systematically landed further toward the midline. The ordinal is a joint
   constraint, so the choice is now joint — exhaustive over a dozen candidates
   per objective. A greedy walk was tried first and starved. The Fountain is no
   longer one of the choices; it is Core geometry fixed by the homeland fit.
3. **Lair sockets had never been searched**, so A_L had never been computed.

Provisional constants and their basis:

| Constant | Value | Basis |
|---|---|---|
| `max_lair_access_asymmetry` | 0.15 | widest gap in the measured distribution (0.073→0.226); any value in [0.08, 0.22] selects the same three seeds |
| `min_homeland_developable_fraction` | 0.52 | over 28 fitted sockets, usable fraction runs 0.110-0.950 with a clear break: six cluster at 0.110-0.468, then nothing until 0.559. Any bound in [0.48, 0.55] rejects the same six |
| `max_hinterland_fraction_of_end` | 0.5 | a compact envelope cannot be the whole end. Has never fired: observed 1.4%-2.5%, so it is a guard, not yet evidence |
| `PRACTICAL_REACH_BOUND_SEC` | 600 | §3 above |
| `OPENING_WORKSITE_REACH_SEC` | 200 | §3 above |

All are labelled PROVISIONAL_ALPHA and none is doctrine.

---

## 5. Vertical slice

**An unseen seed reached the authoring stage and stopped at one physical
blocker. No PlayableMap was produced, and none is claimed.**

Six seeds never previously screened were generated locally (Paper 1.21.11,
4320 chunks each in ~67s including a margin ring), harvested, and compiled.

| seed | score | stage reached | rejection |
|---|---|---|---|
| 20260923 | 61.8 | recognize | `NO_DEFAULT_REGIONAL_SHAPE` |
| 135791113 | 40.4 | recognize | `NO_DEFAULT_REGIONAL_SHAPE` |
| 20240617 | 63.4 | recognize | `NO_DEFAULT_REGIONAL_SHAPE` |
| 4242424 | 56.6 | homebase | `SOCKET_NOT_INDEPENDENTLY_ACCEPTABLE` (north 0.398) |
| 771113 | 68.0 | objectives | `NO_ORDERED_OBJECTIVE_LAYOUT` (south) |
| **99887766** | **90.3** | **author** | **`OBJECTIVE_MESH_MISSING`** |

**99887766** passes Default recognition, both Homebase sockets (0.559 / 0.832),
compact Hinterlands (1.8% / 2.5% of each end — nowhere near consuming it), an
ordered objective layout for both teams, and a Lair socket: 288 viable out of
11,946 cells considered, best **A_L = 0.068**, reach 1351s north / 1446s south,
usable fraction 0.55, open-volume proxy 0.81, three practical approaches.

Then it stops, for the same reason the best screened finalist does.

**The exact blocker: there is no End Spike mesh.** Siting uses the measured
`end_spike` contract; the repository has `aether_fountain`, `nether_bastion`,
`pillager_outpost` and the historical `end_tower`, and registering that last one
under the Spike's name is precisely the masquerade the physical contract
forbids. So the compiler fails closed rather than building the wrong structure.

Screening rate on unseen seeds: **3 of 6** have Default regional shape, **2 of
6** clear Homebase, **1 of 6** reaches authoring. Over the eight screened
finalists the distribution is 1 recognize, 3 homebase, 3 lair, 1 author.

Two honest caveats. These candidates carry `worldgen_truth.verified = false`:
terrain generation is vanilla, but they do not have the official-jar SHA-1
provenance the screened finalists do, and they should not be mistaken for
screened finalists. And the compact-Hinterland gate does not currently
discriminate — observed opening fractions are 1.4%–2.5% against a 50% bound, so
it has never fired and is not yet evidence of anything.

## 6. Failures

- **False positives found and corrected**: the corridor screen warning; the
  per-layer objective selection; the Fountain treated as a free siting slot.
- **Generation harness**: blocking the main thread for minutes trips Paper's
  watchdog, which kills the server mid-window and leaves a partially generated
  region that `extract_region` correctly refuses. Fixed by batching generation
  across ticks and adding a three-chunk margin ring, since a chunk cannot reach
  FULL until its neighbours exist.
- **Remaining blockers**: the End Spike mesh; column-scan verification for the
  Spike's vertical clearance and the Bastion's interior; no physical authoring
  or verification stage has been exercised end to end.

---

## 7. Tests

- Java: **231 pass, 0 failures.**
- Worldgen: **365 pass, 0 failures**, including 14 new compiler tests.

Two `test_structures` tests were migrated as a consequence of using measured
geometry rather than placeholders. One referenced the `end_tower` siting layer,
now `end_spike`. The other asserted that a narrow board crowds out a layer, and
it stopped holding because the Spike's footprint came down from a placeholder
radius of 2 to the measured radius of 1 -- an eleven-block arena pillar
genuinely needs less clearance than the greybox tower did, so the board had to
narrow for the same crowding to occur.

**The two pre-existing `test_author_portfolio` failures are fixed**, and
classified as the spec asks:

- `test_unmapped_species_is_a_reported_gap_not_a_default` — **B**, a stale
  assertion tied to a superseded model. It asserted every species mapped to a
  FENCE, and the regenerative doctrine rejects fences outright as implying the
  site is already Developed. Once nothing is authored, `CROP_BLOCK` and
  `SPECIES_FENCE` had no caller left; they are removed rather than kept as a
  trap, and the test now asserts that nothing is built.
- `test_effective_balance_is_worst_raw_or_spillover_case` — **C**, a stale
  fixture missing the `cell` key `metrics()` counts. It now also pins the real
  change: two portfolios differing only in balance score identically.

---

## 8. Next step

Between here and MAP FOUNDRY → READY MAP POOL → `/moba match start`:

1. **Author an End Spike mesh** to the measured contract. This is the single
   blocker on the deepest candidate, and it is now a specification rather than a
   design question.
2. **Column-scan verification** for the Spike's vertical clearance and the
   Bastion's interior fit — the two facts an 8-block surface grid cannot prove,
   both already named in `UNVERIFIABLE_FROM_SAMPLES`.
3. **Exercise author and verify** on 930005557 in a disposable world, feeding
   any false positive back into the recognizer rather than normalizing terrain.
4. **Widen the A_L sample.** Eight seeds is enough to see a gap; it is not
   enough to fix a threshold. The bound should be revisited once the sample is
   large enough to show a shape rather than a gap.
5. Only then the pool: the compiler is what makes a foundry possible, and it
   now reports per-stage rejections, which is what a foundry needs to know why
   it threw a seed away.

Still open and deliberately untouched: siege advantage, boss and toppling XP,
Worksite tier packages, Giant/Ghast/Dragon encounter design, Lair art direction,
and every competitive threshold after Alpha testing.
