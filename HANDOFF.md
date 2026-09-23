# Handoff — spatial doctrine reconciled, 22 September 2026

**Complete: documentation/terminology/audit pass. No algorithms or fixtures
changed.** Read the [spatial audit](docs/audit/2026-09-22-spatial-doctrine.md),
[reconciliation](docs/reconciliation/2026-09-22-spatial-doctrine.md), and
[source spec](specs/auditworldgenrecoverspatialmodel.md).

Canonical maps.md now separates the functionally mirrored **Homebase Core /
interface**, bounded-integration **Homebase Socket**, natural non-mirrored
**supported region / hinterland**, and asymmetric **Wilderness**. The opening
region needs distributed useful opportunity and a concentration ceiling, not
identical terrain or species/counts. Strategic Depth and Regional Character are
separate axes; future Map Types own their macro-geographic search contracts.

Initial Route support primarily belongs at Core exits and in hinterlands.
Terrain-weighted paths, Practical Reach, physical corridor checks and Route
spillover measurements survive, without a deep-map convenience or equalization
mandate. Player-created Exploration Infrastructure can extend the network.

Regenerative Source/Kind/depletion/recovery/manifestation machinery survives.
`RenewableKinds` deliberately has no depth/value/region. Runtime regions and
lifecycle are implemented; generic depth/character richness authoring is not.
Source capacity/recovery and temporal/eligibility values remain fixtures or
Working calibration. Stale empty-table/lazy-only comments were corrected.

## Smallest next decision and later implementation

1. Define the shared Core footprint, immediate defenses and exit/interface
   contract, bounded Socket integration, and opening economic floor/ceiling.
   Do not invent thresholds or make terrain surgery more powerful.
2. Add a contract record and exact integration/readback evidence at existing
   siting/authoring seams. Old sampled homeland quality is not this contract.
3. Before another seed search, replace Alpha origins in both optimizer `HOMES`
   and `idw_reach_model` with explicit seed-specific inputs and provenance.
   Stored near-miss scores remain historical; they are not silently recomputed.
4. Only after supported scope is defined, re-scope initial Route targets and
   eventually add the two-axis richness policy outside Kind/runtime identity.

Exact bounds, species assignments, richness values, recovery curves and Map Type
recognizers remain Open. No implementation for them was added.

## 930010639 experiment remains pending

**0.3173 is a natural Wilderness metric difference, not a proven competitive
deficit.** The old rescue/compensate/reject interpretation is superseded.
Task A/B remains a Prototype/test proposal asking whether permitted Wilderness
asymmetry has a competitive consequence missing from the authored-opportunity
model, with no presumption of a harmed side.

The [fixture report](implementation/worldgen/reports/nearmiss_fixture_2026-09-22/REPORT.md)
still records a materialized, runtime-loadable 28-opportunity `resource_light`
fixture, with 10 sources and 8 Worksites. Its north Route endpoint ice readback
conflict and missing authored wheat for Task B remain blockers. Task A has no
completed accepted-fixture rehearsal. No worlds were regenerated, 930010639 was
not altered, and no 16-trial experiment or new telemetry was run. Existing
fixtures are not certified against the as-yet-unfinalized Core/Socket contract.

## Prior Hunger task and workspace

The user considers the Hunger issue solved. Audit/test commit **9bb5093** is
retained; no gameplay fix was made. Its 11 server assertions and 205 unit tests
passed before this doctrine pass. Do not resume that investigation without a
new symptom. See [Hunger audit](docs/audit/2026-09-22-hunger-authority.md).

Work began from pushed main **8199c13** plus the completed Hunger audit, in an
isolated worktree. Original dirty work and the live Alpha server were preserved.
This pass validates links and unchanged executable/config semantics; it does
not rerun gameplay tests or claim new gameplay outcomes.
