# Opportunity Relationship diagnostics — Working / shadow

Authority: [spec 4](../../../specs/mapseedsearchspec4.md). This is a diagnostic
implementation note, not an amendment to settled map design. The frozen active
fit is `results/default_local_continuation_2026-09-11/`. New results are separate:
`results/default_opportunity_shadow_2026-09-11/`.

## Terms

- **Opportunity Relationship:** homeland origin → Handoff → physical Affordance
  → appropriate Reach → observed or potential Payoff.
- **Handoff:** where provided Starter infrastructure stops. Its diagnostic
  **arrival envelope** is a connected nearby portion of the same detected
  feature, not a replacement endpoint or a newly fitted road.
- **Affordance:** physical capability/context: passage, barrier, crossing,
  corridor access, network access, threshold, direct interaction.
- **Reach:** modality-specific evidence. A terrestrial dead end is not proof of
  absent water reach. A village arrival need not have onward branches.
- **Payoff:** an observed gameplay-bearing structure or sufficiently evidenced
  geographic projection relationship; alternatively grounded structural
  potential. Different biomes and long travel alone do not establish value.
  Scarcity, ore yield, ecology, resource concentration and exact strategic value
  remain unmeasured unless actual supporting data exists.

The player-facing functions are independent evidence statements, not one label
per handoff: **Gateway** enters a sustained, differently accessible system;
**Junction** exposes multiple materially different onward relationships;
**Destination** reaches an actual gameplay-bearing location; **Transition**
changes traversal relationship toward a credible downstream payoff.

Strong/supported/weak/unsupported are evidence states, not ordinal quality
tiers. A directed Gateway is not penalized for lacking branches.

## Completion and authored responsibility

**Complete** means sampled Handoff, Affordance, appropriate Reach and observed
Payoff form a coherent relationship. This is not physical validation.

**Structurally Supported** means the geographic relationship is supported but
its actual payoff remains unresolved or potential. Future authored systems may
complete it without manufacturing the relationship.

**Incomplete** means the required affordance/reach or usable handoff has not
been established. Missing data is explicitly a limitation, not proof of physical
impossibility. A hypothetical POI cannot rescue generic, unexplained terrain.

Natural-world responsibility includes terrain, thresholds, water networks,
geographic systems, vanilla structures and traversal topology. Authored-map
responsibility includes Worksites, recurring animals, wild crop patches, lapis
silos, minor POIs and other strategic placement. This pass creates none of them;
every `payoff.authored` list remains empty and potential entries place no content.

**Starter Route infrastructure should stop at the point where further
pre-established infrastructure would begin solving the opportunity rather than
merely exposing it.** This principle is reported, never used to alter lengths.

## Shadow boundary

`task_a_opportunities.analyze_shadow(candidate, fit)` reads existing frozen
homelands, fields, regions, handoffs and current corridors. It never calls the
active analyzer/selector. It reconstructs only unchanged traversal utilities
and additive evidence indexes. `strip_shadow(result) == baseline` is asserted
for every finalist, covering all prior candidate fields, eligibility, utility,
local/deep metrics, geometry, topology and historical comparison data.

Historical geometry is never read by the shadow solver. Origins use current
destination-first corridors, or the current homeland-wide field for unselected
examples. Scrambling/deleting history and mocking the selector to fail are
regression tests.

All currently selected handoffs are analyzed. Up to twelve unselected records
per homeland are stratified across kinds, locally valid/dead-end examples and
same-opportunity-group alternatives, then filled by existing utility. This is a
bounded diagnostic sample, not a replacement ranking or exhaustive rejection
audit. Ineligible villages can be meaningful shadow Destinations without gaining
an active eligibility exception.

## Provisional computation, not balance

- Arrival envelopes reuse the existing 32-block analytical approach scale.
  They require same-feature dry/modest connectivity; visual equivalence is not
  proven. No path is refitted around them.
- Entered forest/highland systems reuse existing masks. A persistent core uses
  existing 24-sample formation support and four inward sample layers. This
  deliberately distinguishes a thin strip from an interior, but is a provisional
  diagnostic resolution, not a persistence eligibility rule. Core existence,
  local entry and increasing depth are separate observations.
- A system touching outward homeland sources is conservatively marked already
  accessible before the handoff. This may understate the privileged access of
  a better entry into a large continuous system; it is not a complete access
  equivalence model.
- A mask can be physically adjacent while its entrance is not connected under
  the current modest-grade proxy. Raw component size/core and immediate entry
  are reported separately; no entry is not the same claim as no geography.
  General slope-foot, pass and non-cover regional Gateway semantics remain
  incomplete analyzer capabilities, never evidence to reject the seed.
- Water Reach uses cardinal open-water surface samples; ice and other recorded
  surfaces do not become boat links. The additional water-network index can join
  adjacent existing inland/ocean identities. It never rewrites active identities
  or terrain-region segmentation. Width, obstacles, waterfalls and boat clearance
  between samples remain unknown. Water-cell height can be seabed: it is never
  used to invent embarkation grades or ford depths.
- Shore ports are connected contacts within existing region identities, with
  four-sample support. Regional label difference alone is not value. Separate
  dry traversal components or persistent cover systems can provide projection
  context, but disconnected dry components are not necessarily separate islands.
  Separate dry components need the existing 24-sample formation support before
  becoming remote projection context. Distinct village shore contacts can expose
  network relationships even where one coarse shoreline region spans both ends;
  their water distance is measured to the village-side contact, not the nearest
  unrelated point of the whole shore region.
- Water extent uses a double-sweep shortest-path lower bound. The inherited
  32-block continuation resolution plus an explicitly provisional geometric
  elongation ratio distinguish corridor-like geometry. Multiple distinct contact
  contexts support network geometry. Neither substitutes for payoff evidence.
- A village near a reachable remote shore is conditional observed payoff;
  non-village structure boxes do not prove a meaningful accessible entrance.
  Maritime payoff can be distant from the starting handoff. Landfall proximity
  uses a modest-dry graph path within the existing approach scale, not mere
  Euclidean proximity or unlimited land reach from every shore.
  Villages already at shallower/equivalent opening access are not credited as
  remote payoff. Alternative homeland land cost is emitted; uncalibrated water
  path length cannot establish that maritime access is faster or superior.
- Local branches are inherited unchanged. Their actual local sample components
  are inspected for persistent systems/water/village relationships. Equivalent
  branches do not become a Junction merely because there are two paths.
- Commitment uses dependency/context descriptions, never fixed gameplay-distance
  bands. Water exploitation/return and exposure remain unresolved.

## Sets, convergence and scale

An **Opening Opportunity Set** describes the frozen choices jointly across
geography, affordance, payoff and commitment. Counterfactual removal records
which payoff identities would be lost. Same-team pairs can overlap, substitute,
complement or connect; geographic overlap alone does not prove strategic
interchangeability or productive synergy.

**Spatial convergence** comes directly from the existing sample-level topology.
**Opportunity convergence** is shared strategic payoff or movement-system
identity, possibly without terrestrial path overlap. Shared water alone is weak
opportunity evidence until the payoff is understood. Contested/exclusive labels
are prospective and limited to sampled evidence, not actual combat/access rules.

Differentiation → Networking → Contestation is an observed sequence to inspect,
not a desired-distance template. Deeper terrain depth is retained; deeper
resource-space value is unresolved. No map dimensions, hard quotas, weights or
healthy-separation targets are introduced.

## Run and stop

```
python3 implementation/worldgen/fit_default_task_a_opportunities.py
python3 implementation/worldgen/fit_default_task_a_opportunities.py --verify
```

Outputs include eight augmented `fit.json` files, `comparison.json`, `REPORT.md`,
`TEST_RESULTS.md`, and `verification.json`. The runner verifies all prior
artifacts and active implementation hashes plus available world-file snapshots.
No new SVG is needed: frozen geometry remains inspectable in the previous
local-continuation SVGs. No Minecraft world is created or modified.

Manual shadow review and a further explicit design/validation decision are
required before these diagnostics may affect selection. Stop before reselection,
physical greyboxing, authored placement or Task C.
