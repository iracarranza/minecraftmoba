# Reconciliation: a regenerative source's radius is authoritative over its region

## Decision (10 October 2026, stated by the project owner)

`renewables.migratedRegionHalfSpan: 48` was a **default**, not an authored extent. For a source
with no authored region cells, **the authored radius is authoritative** for where its wild
population lives.

## What it resolves

Two definitions of a source's extent coexisted: the Opportunity Region (where eligibility
places a manifestation) and the radius cube (what membership, the sweep and the crop-harvest
scan judge). A region migrated from origin and radius was 48 blocks each way (3x the radius when the
key was unset) against a radius of 20 in every Alpha source, so about 82% (89% at the 3x default) of
chosen sites put a manifestation outside the cube it was judged by, and the membership sweep revoked
those members within one 200-tick sample. Found by the opportunity bench; reproduced live.

## What changed

- A source with no `region` cells gets `square(origin, radius - margin)`, where the margin is the
  larger of `renewables.patch.spread` and half of `renewables.herd.cluster` (6 by default), so a
  site and the members spread around it are inside the cube. `Renewables.migratedRegion`,
  `Renewables.placementMargin`.
- `renewables.migratedRegionHalfSpan` is removed. The offline simulation
  (`simulate_regenerative.py`), which reads the same config because it reproduces the eligibility
  predicate, derives each source's region the same way (`placement_margin`).
- Runtime-created sources (`createRuntime`) previously had NO region, which `manifest` would have
  dereferenced; they now get the derived one.
- Authored `region:` cells still win where present.

## Unchanged and still open

- **Strolling.** Placement is inside the cube, but creatures wander. Whether to confine movement
  (a soft home range) is a separate design question; it interacts with capture by leading ("narrowest
  defensible reading" of capture is leaving the region) and with unresolved swarm behaviors.
- The historical reports under `implementation/worldgen/reports/` were generated with the old 48 and
  are left as they were.
