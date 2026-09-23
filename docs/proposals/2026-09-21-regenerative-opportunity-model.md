# Regenerative Opportunity Relationships — audit and model

Status: **audit and proposal**. Sections A–E are reporting. Implementation is
deliberately partial; §F states exactly where it stops and why.

---

## A. Current-state audit

### A.1 Canon already contains the three-level structure

This is the most important finding, and it changes what this work is. The
Opportunity Region / Eligible Sites / Current Manifestation split is **not new
design** — it is already canon, and the implementation collapsed it.

`maps.md:694`:

> Regenerative opportunity is a known recurring world possibility whose
> **current manifestation must still be sought, accessed, and exploited** by
> humans. Exploitation reduces local availability; renewal or ecological
> conditions later restore opportunity. **Known ecology does not imply exact
> current location**, readiness, convenient access, ownership, or guaranteed
> successful acquisition. Crop Patches, Herds, and Mob Swarms illustrate the
> recurring-opportunity concept without settling their complete population
> rules.

"Known ecology does not imply exact current location" *is* the region/
manifestation distinction. And `maps.md:672–676` already names the middle
layer:

> **Candidate Density** is how many candidate resource opportunities the
> geography supports… **Regenerative Eligibility** identifies which of those
> opportunities can participate in regeneration. **Regenerative Vocabulary** is
> the set of resource or encounter kinds eligible to recur in that geography.
> … Identify real candidate opportunities, evaluate their regional conditions,
> and observe the eligible subset; **do not sprinkle** enough regenerative
> nodes onto empty cells to satisfy a percentage.

`maps.md:696` supplies the production/regeneration separation:

> **Regeneration restores world opportunity, not player income.** … A
> repeatable Production process consuming acquired inputs, an ordinary
> replanted farm, an objective-generated finite deposit, and a naturally
> recurring opportunity remain distinguishable behaviors.

So the four CANON items in the brief are all already written down. The paste's
contribution is the **lifecycle and ownership machinery** that makes them
implementable.

### A.2 Existing documents

| Document | What it establishes |
|---|---|
| `maps.md:672–696, 805–817` | Candidate density, regenerative eligibility, vocabulary, depth/specificity gradient, finite vs regenerative, "restores opportunity not income" |
| `objectives.md §17b` | Economic framing of regenerative sources |
| `docs/proposals/2026-09-19-regenerative-sources-seam.md` | The current plugin seam: `Source` shape, lazy recovery, the invariant "renewal grants nothing to anyone", and an explicit list of what ships empty |
| `docs/reconciliation/2026-09-12-nighttime-economy.md` | Hostile-swarm/time-of-day economy |
| `docs/analysis/authored-strategic-opportunity-optimizer.md` | How renewable sites were selected during map authoring |

### A.3 Existing implementation

**`Renewables.java`** — the whole runtime model.

```
Source { id, type ∈ {CROP, ANIMAL, SWARM}, world, kind, x, y, z,
         radius, capacity, available, recoveringUntil }
```

- `harvest(s)` decrements `available`; at zero sets `recoveringUntil = now + recoverTicks`.
- `available(s)` settles recovery lazily on access; on recovery calls `manifest(s)`.
- `manifest(s)` = `count()` what exists, top up to `available - present`.
- `count()` — CROP: scan the `(2r+1)³` volume for matching blocks. ANIMAL/SWARM:
  `getNearbyEntities` of matching `EntityType`.
- `placeCrops` — walk the volume, place crop blocks on farmland/grass/dirt.
- `spawnFauna` — random offsets within `radius`, snapped to `getHighestBlockYAt`.
- `onBreak` — **provenance-gated**, correct: a player-placed crop is not a wild harvest.
- `onDeath` — **not** ownership-gated: any player kill of a matching `EntityType`
  inside the radius decrements.
- Persistence: chunk PDC, matching `Provenance`.
- `grantedByRenewal` counter asserts the "grants nothing" invariant. Still zero.

**`RenewableKinds.java`** — the vocabulary (kind → type + qualifying blocks/entities),
with `FORBIDDEN_SWARM`. Assigns no depth, value or region, per canon.

**`RenewableAuthoring.java`** — `/moba renew` runtime create/remove/manifest.

**`RenewableMarkers.java`** (new today) — glow for entity members, particles for
crop blocks, gated on availability and player proximity.

**`Match.reset` → `resetRenewables()`** — clears all state and rebuilds from
config after the world is restored. Already conceptually "discard manifestations,
restore pristine world".

**`Provenance.java`** — chunk-PDC bitset of player-placed blocks, read at
`HIGHEST`. This is the existing ownership primitive, and it covers blocks only.

**`WorkPoints`** — `breed(EntityBreedEvent)` and `harvest(mature crop)` are the
Development interactions. The crop harvest is deliberately *not* provenance-gated,
because a player growing a crop is the point.

### A.4 Config

`renewables.sources`: 12 entries — **8 ANIMAL, 4 CROP, 0 SWARM** — each
`{world, type, kind, x, y, z, radius: 20, capacity: 6 or 24, recoverTicks: 2400}`.
Marked NON-CANON ANALYTICAL FIXTURES.

### A.5 Map / worldgen artifacts

`implementation/worldgen/terrain_harvest/author_portfolio.py` turns optimizer
output into authored sites:

- `founder_crop` → `massing.crop_patch(span=24)`: a **tilled, irrigated field** —
  farmland with moisture 7, a central water column, mature crops.
- `renewable_range` → `massing.pen(span=40, post=SPECIES_FENCE[species])`: a
  **fenced enclosure** with a grass floor and one gap per side.

`SPECIES_FENCE` maps species → fence material. `publish_world.py` gives both
kinds map colours; `rescan.py`, `verify_portfolio.py` and `spillover_validation.py`
all reference the two kind names.

---

## B. Conflicts with the model

| # | Conflict | Where |
|---|---|---|
| 1 | **Three spatial concepts collapsed into one.** `origin + radius` is simultaneously the region, the eligible area, the manifestation site, and the harvest-detection volume. There is no representation in which they could differ. | `Renewables.Source` |
| 2 | **The map pre-builds containment.** `pen()` fences the animals in; `crop_patch()` builds a *player-style irrigated farm* with farmland and a water column. Both are exactly the "prebuilt facility" the model rejects, and the second also blurs world regeneration into player production. | `massing.py`, `author_portfolio.py` |
| 3 | **Regeneration stacks onto an uncleared manifestation.** `manifest()` tops up to `available - present`, so a partly-harvested herd is refilled in place rather than replaced. | `Renewables.manifest` |
| 4 | **Entity membership is positional, not owned.** Any entity of the right type inside the radius *is* a member. A player's bred sheep becomes wild; a wild sheep that wanders 25 blocks stops being wild; an ordinary naturally-spawned zombie inside a Swarm radius counts as a Swarm member. | `count`, `onDeath` |
| 5 | **No captured-vs-wild distinction for entities at all.** Ownership case C exists for blocks (via `Provenance`) and not for entities. | — |
| 6 | **The manifestation never moves.** Recovery re-manifests at the same origin, which makes the region a camp coordinate. | `manifest`, `spawnFauna` |
| 7 | **Crop `count()` ignores provenance.** `onBreak` checks it but `count()` does not, so a player farm built inside the region counts as the wild patch being intact and suppresses re-manifestation. | `Renewables.count` |
| 8 | **Markers mark by type-in-radius**, so they glow player livestock too. | `RenewableMarkers.markFauna` |
| 9 | **Recovery is a per-source hidden timer** (`recoverTicks`), which the paste characterises as invisible MMO machinery. Flagged, not resolved. | `Source.recoverTicks` |
| 10 | **`SWARM` is vocabulary only** — zero configured sources, and `onDeath` would classify any `Monster` death in an ANIMAL region incorrectly were the types ever mixed. | config, `onDeath` |

Conflicts 1–8 are defects against the model. 9–10 are gaps.

---

## C. Proposed unified lifecycle and data model

One system, three manifestation behaviours. Nothing below is per-kind bespoke.

```
RegenerativeOpportunity            (map design, persistent)
    id
    form        ∈ { HERD, SWARM, PATCH }      -- manifestation behaviour
    kind                                       -- vocabulary entry (sheep, carrots…)
    region                                      -- persistent authored geography
    eligibility                                 -- predicate, evaluated on demand
    population                                  -- how much manifests  [FIXTURE]
    depletionThreshold                          -- when it counts as spent [FIXTURE]
    recoveryTrigger                             -- when a new one may appear [OPEN]
    state       ∈ { MANIFESTED, DEPLETED, RECOVERING }
    current     : Manifestation?                -- at most one

Manifestation                      (match state, disposable)
    site                                        -- the chosen eligible position
    members     : set of member handles         -- entities by UUID, blocks by position
    remainingWild : int
```

Four points of structure that follow from the brief and are not free choices:

**Eligible sites are a query, not a table.** The brief says not to assume they
are precomputed. Evaluating `eligibility` over the region at manifestation time
is also the only version that stays correct when players change the terrain —
which is required for "Development can shift the wild manifestation elsewhere".

**Membership is explicit and system-owned.** An entity is a member because the
system marked it one, never because of where it is standing. Blocks reuse
`Provenance`: a manifestation block is one the *system* placed, so player-placed
is automatically excluded, which is the primitive already in the repo and the
least brittle option available.

**One manifestation at a time.** `MANIFESTED → DEPLETED` happens by membership
loss; a new manifestation is created only from `RECOVERING`, never as a top-up.
This is what stops an ignored opportunity from being an animal printer.

**Membership can be revoked without deleting anything.** A captured animal stops
being a wild member and *remains an entity*. Revocation reduces `remainingWild`;
it never despawns. This is the mechanism that lets a wild opportunity seed
permanent player Development.

Three forms differ only in two operations:

| | manifest | deplete |
|---|---|---|
| HERD | spawn passive entities, marked | member killed **or** membership revoked (left the region) |
| SWARM | spawn hostile entities, marked | member killed (no capture concept needed) |
| PATCH | place blocks, system-placed so provenance-clean | member block broken |

---

## D. Decisions required before implementation vs. safe Alpha fixtures

### D.1 Must be decided — these determine architecture

| Decision | Why it is architectural |
|---|---|
| **Region geometry** | Whether a region is a radius, an authored cell set, or a biome/terrain query changes the map format, the authoring tool, and the eligibility scan. Everything downstream reads it. |
| **Eligibility predicates** | Whether eligibility is "terrain test at a candidate point" or "precomputed authored site list" decides whether the map tool or the plugin owns site selection. The *predicates themselves* are content, but the **locus of the decision** is architecture. |
| **Recovery trigger** | A world event (sunrise/sunset) and a per-source timer are different control flows: event-driven vs. lazily-settled-on-access. The current code is the latter. |
| **When moved livestock cease to be wild** | If the answer is "left the region" the region must be queryable per-tick per-member; if it is "named/leashed/fenced" it is event-driven; if it is "distance from site" it needs the site retained. These are different data. |

### D.2 Safe as labelled Alpha fixtures

Manifestation population per kind; depletion threshold; recovery cadence value;
site-selection weighting; minimum displacement from the previous site; player
proximity exclusion radius; species/resource tables; distance/value scaling;
marker appearance.

---

## E. Migration plan for the authored Alpha map

Per-opportunity reinterpretation of the 12 existing sources:

| Source | Geography | Reinterpretable as a region? |
|---|---|---|
| 8 × ANIMAL (`renewable_range`) | 40-span fenced pen with a flattened grass floor, on optimizer-selected cells | **Partly.** The *cell* the optimizer chose is legitimate authored geography and should be kept as the region seed. The *pen* is rejected presentation and must go. The flattened grass floor is a real problem: it is authored terrain, so removing the fence leaves an artificial clearing that will read as the "decorative spawn pad" the model rejects. |
| 4 × CROP (`founder_crop`) | 24-span tilled field, farmland moisture 7, central water column | **No, as built.** This is a player-style farm, not a wild patch, and it is the stronger conflict of the two: it does not merely contain the resource, it *is* player production geography. |
| 0 × SWARM | — | Nothing to migrate; no authored swarm geography exists. |

**Recommended sequencing, not executed here:**

1. Keep the optimizer's chosen **cells** as region seeds. That data is the
   product of real analysis (`authored-strategic-opportunity-optimizer.md`) and
   nothing in this model invalidates it.
2. Retire `pen()` and `crop_patch()` from the authoring path.
3. Re-author the frozen Consolidative template **once**, as a deliberate
   re-freeze, not incrementally.

**Blocker:** the Alpha template is frozen (`alpha-0.1-map-freeze.json`) and is
the base of every match instance. Re-authoring it is a re-freeze decision with
its own validation pass, and it is explicitly out of scope for a model proposal.
Until then the pens and tilled fields remain in the world as legacy geography.

**Flagged as genuinely unsupportable:** the four `founder_crop` fields cannot be
reinterpreted as wild Patches without re-authoring, because irrigated farmland
with a water column is player-production geography by construction. A wild patch
manifesting *inside* one would be indistinguishable from a farm.

---

## F. What is implemented now, and where this stops

**Implemented** — unambiguous, and independent of every D.1 decision:

1. **Explicit manifestation membership for entities.** Members are marked with a
   PDC key naming their opportunity. `count`, `onDeath` and the markers resolve
   membership by that mark, never by position-and-type. This closes conflicts 4,
   5, 8 and 10, and it is the ownership fix the brief asks for.
2. **Membership revocation without deletion.** A member that leaves its region
   stops being wild and stays alive. Threshold is a labelled fixture; the
   *rule* for what counts as leaving is D.1 and the shipped behaviour is the
   narrowest defensible one.
3. **One manifestation at a time.** Manifesting is no longer a top-up; it
   happens on the transition into a new manifestation. Closes conflict 3.
4. **Provenance-correct crop membership.** `count()` now excludes player-placed
   blocks, so a player farm inside a region no longer suppresses regeneration.
   Closes conflict 7.
5. **Explicit lifecycle state** on the source, replacing inference from two ints.

**NOT implemented, stopped deliberately at the boundary:**

- **Site selection.** Everything about "manifest somewhere eligible rather than
  at the authored origin" waits on region geometry and the locus of eligibility
  (D.1). Until then a new manifestation appears at the authored origin, which is
  the rejected fixed-pad behaviour and is marked as such in the code so it
  cannot be mistaken for the intended design.
- **Recovery trigger.** Left as the existing per-source timer. Moving to a world
  event is a control-flow change and the trigger is explicitly unresolved.
- **Map re-authoring.** §E. Blocked on a re-freeze decision.
