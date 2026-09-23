# Default-map compiler vertical slice — 23 September 2026

Branch `codex/spatial-cadence-migration`, over the 14cb46c cadence migration.
Spec: `specs/claudeversionsuperspatialdoctrinepsec.md`.

**Two unseen seeds now compile, author and physically verify into PlayableMaps**,
and a foundry publishes them into a READY pool the runtime claims from.

Getting there took finding and fixing seven real defects, every one of them
caught by running something rather than reading it: three by a column-level
verifier and a block-level readback, one by a lifecycle smoke test on a
disposable server, one by a concurrency test on the pool, and two by the
compiler's own stage model.

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

## 5. Vertical slice — two PlayableMaps

**Thirty seeds never previously screened. Two compiled, authored and verified
end to end with no intervention and no special-casing.**

| stage reached | seeds | rejection |
|---|---|---|
| recognize | 24 | `NO_DEFAULT_REGIONAL_SHAPE` |
| homebase | 3 | `SOCKET_NOT_INDEPENDENTLY_ACCEPTABLE` |
| lair | 1 | `LAIR_ACCESS_DISPARITY` |
| **verify** | **2** | **passed** |

Yield **6.7%**. Default regional shape is the dominant filter by a wide margin:
roughly one seed in five is Default geography at all, and one in fifteen
survives everything after it. That is the number a foundry has to plan around.

The two successes are independent, not one result twice:

| | 99887766 | 2718281 |
|---|---|---|
| homeland sockets | 0.559 / 0.832 | 0.737 / 0.850 |
| Lair A_L | 0.068 | 0.0001 |
| viable Lair sockets | 288 / 11,946 | 355 / 11,806 |
| authored blocks | 90,296 | 90,330 |
| block readback | verified | verified |

### The End Spike, reproduced rather than approximated

Every number came from the generated vanilla End, not from a guess. Radius and
height co-vary across exactly (2,76)…(5,103) in steps of three. The pillar is a
disc under `dx² + dz² ≤ r² + 1`, which reproduces the measured column counts
21 / 37 / 57 / 89 for r = 2…5 — a plain `≤ r²` gives 13 / 29 / 49 / 81 and is
wrong for every radius. A **single** bedrock block caps the centre, not a layer.
Caged spikes get a 5×5 iron-bar box, walls at top+1…top+3 and a roof at top+4,
which is what two of the ten measured spikes have. The End Crystal is an
entity, so it is a reported seam rather than a block.

The Outpost and Bastion are now placed from the real vanilla NBT as well. The
greybox Outpost was a 7×7 dark oak box standing in for a 15×15 watchtower, so a
site was verified against the measured contract and then authored to a different
size.

### Column-level verification, and the false positives it exposed

Three, each fixed generally rather than for the seed that surfaced it.

1. **Trees counted as terrain**, rejecting two of six objectives on oak leaves.
   Authoring already fells trees, and doctrine treats clearing vegetation as
   bounded work while reserving rejection for landscape surgery. Vegetation is
   now clearing cost, using `respect.is_tree` rather than a second definition of
   what a tree is. Standing structures fail hard instead: a village house is not
   terrain.
2. **Ground contact compared the terrain's flatness to the MESH's contact
   count** — a property of the ground against a property of the structure. Every
   site failed, which is the shape of a wrong comparison rather than of bad
   terrain. Replaced by levelling volume against the median datum, bounded at
   10.0 blocks per column: measured over 1800 random footprints in three worlds
   (median 2.20, p90 6.26, max 21.5), it rejects the worst 3% as terrain surgery.
3. **Footprint overlap was invisible to everything upstream.** Siting reasons in
   eight-block samples and put a Bastion and an Outpost eleven blocks apart; both
   passed every analytic check, and a block-level readback found the Bastion had
   overwritten the Outpost. Separation is now enforced at authored span in the
   joint search and re-checked in verification, and the candidate pool went
   12 → 24 because the two constraints together left one team no legal layout.

The contract is now derived from the authored template rather than a second
measurement pass — which is how the Bastion came to be verified at 32 blocks
tall and built at 64. The two paths independently agree on the Outpost (1156
blocks, 15×15×21) and on the Bastion's 506 contact columns.

### Foundry and READY pool

`terrain_harvest.foundry` publishes only verified maps, each with provenance —
seed, map type, the compiler's provisional constants, a world fingerprint, the
verification evidence, and what was **not** reproduced. `MapPool` claims
atomically via `CREATE_NEW`; a manifest write would be a read-modify-write and
would race in production rather than in a test. A twelve-thread test confirmed
no two matches take the same map, and it found a real precedence bug: a retired
map keeps its claim file, so checking claim before used reported it IN_USE
forever and would have leaked one map per match.

Verified on a live server: the plugin read a real pool, claimed both maps
distinctly, reported exhaustion, and retired one to USED. Disabled by default.

## 6. Failures

- **False positives found and corrected**: the corridor screen warning; the
  per-layer objective selection; the Fountain treated as a free siting slot.
- **Generation harness**: blocking the main thread for minutes trips Paper's
  watchdog, which kills the server mid-window and leaves a partially generated
  region that `extract_region` correctly refuses. Fixed by batching generation
  across ticks and adding a three-chunk margin ring, since a chunk cannot reach
  FULL until its neighbours exist.
- **Blockers resolved within this pass**, listed here because an earlier draft
  of this section recorded them as outstanding and that is no longer true:
  the End Spike mesh was authored from the measured contract; column-scan
  verification runs for the Spike's clearance and the Bastion's footprint; and
  physical authoring plus a block-level readback ran end to end. §5 is the
  current state and this section is subordinate to it.
- **Remaining blockers at the close of this pass**: the Lair was sited but not
  manifested, so a verified map could enter READY with no runtime Lair. That
  was the principal defect carried forward, and it is what
  [`2026-09-23-generated-map-lifecycle.md`](2026-09-23-generated-map-lifecycle.md)
  addresses.

---

## 7. Tests

- Java: **239 pass, 0 failures.**
- Worldgen: **388 pass, 0 failures.**

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

The compiler works and repeats. What stands between it and a running foundry:

1. **Yield is the operational constraint, not correctness.** 6.7% means ~15
   generated worlds per pool map, at roughly two minutes each — about half an
   hour of machine time per map. That is fine for a background foundry and
   hopeless for anything synchronous, which is exactly why the pool exists. If
   the pool is to stay stocked, the cheap win is screening regional shape from
   biome data before generating a world at all, since 80% of the cost is spent
   on seeds rejected at the first stage.
2. **Run the foundry as a job.** It currently runs by hand. It needs a target
   pool depth, a way to keep going until it reaches it, and a report.
3. **Widen the A_L sample.** Eight finalists plus thirty unseen seeds is now
   enough to revisit the 0.15 bound properly, rather than from one gap.
4. **The Bastion's rampart ring is not reproduced.** Vanilla assembles it by
   jigsaw connection; the central body is placed and the ramparts are not, and
   that travels with every published map. Reproducing the jigsaw assembly is the
   next real authoring increment.
5. **The Lair is sited but not manifested.** `alpha.lair.site` is still empty on
   published maps: the socket is chosen and measured, and nothing is built or
   bound there yet, so the cadence would report UNCONFIGURED on a claimed map.
   That is the clearest gap between "PlayableMap" as the physical contract
   defines it and a map a match can actually be played on.
6. **Nothing has been played.** Verification is physical, not competitive. Every
   competitive threshold here is PROVISIONAL_ALPHA, and the question the last
   two passes keep arriving at — whether any of these disparities are felt in
   play — is still unanswered and still not answerable by analysis.

Caveats that travel with these maps: `worldgen_truth.verified = false` (vanilla
terrain, but not the official-jar SHA-1 provenance the screened finalists have),
and the compact-Hinterland gate has never fired (observed 1.4%–2.5% against a
50% bound), so it is a guard rather than evidence.

Still open and untouched: siege advantage, boss and toppling XP, Worksite tier
packages, Giant/Ghast/Dragon encounter design, Lair art direction. The vanilla
Giant still has no designed AI; that is recorded, not designed around.
