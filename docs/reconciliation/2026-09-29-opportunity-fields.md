# Reconciliation — Opportunity Fields, node economy versus ambient wealth, and swarm temporal tables

## Authority and scope

This record integrates the 29 September 2026 discussion of regenerative-resource
placement. `maps.md` remains canonical for spatial generation and
certification, `objectives.md` for the regenerative economy and Mob Swarms, and
`infrastructure.md` for spatial metrics. **This record is a reconciliation, not
an edit of those files.** Folding the doctrine into them is a separate step
that needs an explicit decision.

Everything below is **Working** direction unless labelled otherwise. No
numerical value is set, and no playable implementation is implied.

Grounding: the audit of the current implementation is in the session record of
29 September. In short: the runtime lifecycle (`Renewables`, `Eligibility`,
`Opportunity`, `Recovery`) exists on branch `integrate-2026-09-27`, not on
`main`; `portfolio.py` derives only animals and crops; swarms are hostile
entity types on an animal-shaped lifecycle, with no biome, time, behavior or
payload data.

## Integrated Working direction

### 1. Node economy versus ambient wealth

- **Regenerative-node sufficiency determines whether a map is playable.**
  Every playable map must meet a baseline regenerative opportunity *capacity*,
  with minimum redundancy, distribution and concurrency. Capacity, not raw node
  count, is the invariant: ten poor Coal opportunities are not eight good ones.
- **Ambient abundance characterizes a map.** Naturally generated, non-node
  resources are finite where vanilla makes them finite, and classify a map as
  resource-light, resource-neutral or resource-dense.
- Classification is relative to the population of recognized playable candidate
  maps, not to generic Minecraft worlds. It probably needs a weighted
  per-resource profile eventually, because +50% coal and -50% iron should not
  average to neutral.
- Ambient wealth and the node economy interact without being conflated: a
  resource-light map makes the same guaranteed nodes strategically weightier.

### 2. Ore

- Ore may participate in regenerative nodes. Ordinary generated ore stays
  finite and underlies the authored economy.
- **Coal and Copper are substantially renewable.**
- Increasing material value makes regenerative authorability progressively more
  restrictive (fewer bands, narrower eligibility, lower count, lower
  concurrency, slower recovery, lower yield) while raising discovery, access,
  exploitation, exposure and contestability burden. **No universal value
  threshold makes an ore categorically incapable of being a regenerative node.**
- A renewable ore node must preserve Extraction gameplay: walking to a marked
  spot, breaking exposed blocks and waiting is not acceptable.
- The resource-by-resource table is **Open**.

### 3. Opportunity Fields

- **Field** is the system term. A Ring is the most common topology for a
  map-wide field, not the definition of one.
- A Field has: **Domain** (map-wide, biome, region, strategic-depth range, or
  combinations), **Topology** (ring, ribbon, arc, compact loop,
  biome-following loop), **Geometry** (stretch, orientation, width,
  deformation, Practical-Reach conformity), **Sampling** (count, spacing,
  offset, jitter, concurrency) and **Eligibility** (biome/ecology, terrain,
  manifestation constraints, Commitment requirements).
- The map and biome geometry deform a field before individual points are
  realized. Compact biomes may produce conspicuously close nodes; this is
  identity, not a defect.
- Fields use a hierarchy of scales: map-wide, regional/biome, compact biome, and
  outer/deep (fragmentary arcs, clipped by biome).
- **Equal node spacing is not a balance goal.** The compiler measures resulting
  concentration and asks whether the whole map stays acceptable.
- RNG operates at several levels (orientation, stretch, offset, local
  deformation, slot count within bounds, jitter, choice of eligible contour)
  inside authoring constraints.

### 4. Ring axis is Practical Reach, not radius

- Euclidean radius from the midpoint is **not** the authoritative axis.
- Two team-relative cost fields, `C_N(x,z)` and `C_S(x,z)`, characterize a
  location: cheap for one side, expensive for the other, expensive for both,
  equally reachable, deep behind one side, or central despite odd geometry.
- Rings and bands are ordered opportunity structures embedded in
  Practical-Reach space. Cost determines depth; physical topology determines
  order along a band.
- The existing `near_team` band logic in `portfolio.py` is an early, simple form
  of this measurement, to be preserved and generalized rather than replaced.

### 5. Relationships are emergent, not declared

- Fields are **not** generally related pairwise. There is no `coal <-> sheep =
  aligned` metadata, and no rule requiring any particular alignment.
- Fields are authored on **compatible spatial grammars** (slot frequencies,
  bands, topology, permissible offsets) so that independently generated fields
  *can* align, alternate, coincide harmonically, intersect or converge.
- Map generation realizes each field independently. Relationships are outputs:
  the compiler may measure and record them as map descriptors ("opportunity
  topology"), and must not require one unless playtesting later establishes
  that a relationship is needed for balance.
- Three levels of knowledge result: system, resource and match. The last
  cannot be memorized.

### 6. Swarm temporal tables

- **Opportunity identity is spatial and ecological; manifestation eligibility
  is temporal.** One swarm opportunity owns its day table and its night table.
- The current temporal table applies when the opportunity next manifests. Empty
  or ready at sunset uses the night table; recovering through sunset uses the
  table current when it becomes ready; a living daytime manifestation is **not**
  despawned or replaced at sunset.
- An individual swarm definition may later override this to permit a dramatic
  transformation.
- The composition of each table, and the biome portfolio of swarms, remain
  **Open**. The `swarmNightRate` value of 1.0 remains a placeholder asserting
  nothing.

### 7. Commitment Profile is a vector, not a score

- Discovery, Access, Exploitation, Return, Infrastructure dependency, Exposure
  and Contestability are an **authoring and diagnostic vector**.
- Compiler certification uses only what is measurable (route cost informs
  Access and Return; Exploitation is fairly measurable for ore). The rest is
  instrumented and validated during alpha. **No scalar Commitment formula is
  adopted.**
- The commitment an opportunity demands determines its authorability; Practical
  Reach is one input and is not the sole proxy for difficulty.

## Models superseded by this reconciliation

1. **Finite ore is never renewable** (`authorability.py`,
   `not_closable_by_renewables`, "finite ore, which renewables do not
   produce"; and the renewable-only-copper list). Superseded as design
   doctrine. The code is unchanged and now disagrees with this record.
2. **A swarm source has a fixed kind for its lifetime** (`Renewables.manifest`
   reads `s.kind`; `RenewableKinds.swarm`). Superseded as doctrine; not yet
   as code.
3. **Radial or literal ring geometry** as the placement axis. maps.md already
   states "not literal circular rings"; this makes the cost-space reading
   explicit.
4. **Pairwise relationship declarations** between resource fields.

## Preserved

- Ordinary vanilla incidence survives beneath deliberate manifestations
  (`portfolio.py`).
- A cell hosts a kind only where its measured ecology supports it; an absent
  opportunity is a real answer. Projection of nominal field points to terrain
  must stay gated by that rule, or it becomes the node-sprinkling maps.md
  forbids.
- Symmetry of opportunity, not of terrain. No species floor mirrored across
  teams.
- Renewal grants nothing (`grantedByRenewal` remains zero).

## Explicitly not decided

Exact ring or field counts; node floors; concurrency; recovery rates;
resource-light/dense thresholds; slot frequencies and offsets; the renewable-ore
table; any Commitment formula; swarm compositions and biome portfolios;
whether protected-region rules apply to block nodes.

## Vocabulary caution

"Phase" already means the Temporal Phase P and the Worksite tier in the plugin.
Use "offset" or "rotation" for field alignment. "Portfolio" already means a
map's derived source set; a per-biome swarm table needs a different name.

## Follow-through, none of it done here

- Canonical edits to `maps.md` (Fields, cost-space axis, emergent
  relationships, node floor versus ambient) and `objectives.md` (swarm temporal
  tables, ore).
- `authorability.py`'s finite-ore statement, and `RenewableKinds`' comment
  calling ore regeneration a design claim, now disagree with this record.
- Swarm work needs a definition schema (weighted composition, biomes, day and
  night tables, behavior id, payload id) and a `SWARM` vocabulary in the
  compiler. None exists.
- The regenerative runtime is on `integrate-2026-09-27`, not `main`.
