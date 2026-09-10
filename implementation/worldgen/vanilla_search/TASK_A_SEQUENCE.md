# Post-Task-A sequence: implementation and limitations

Authority: `specs/mapseedsearchspec3.md` (Working sequence); `maps.md` remains
authoritative for settled design. No canonical game-design decisions changed.

## Execution and stage state

```sh
python3 implementation/worldgen/fit_default_task_a.py --refine
python3 -m unittest discover -s implementation/worldgen/tests -v
```

A.0 is preserved in `results/default_task_a_2026-09-10/`. A.1 reuses all eight
committed A.0 fits and their original extracted feature grids. It does not search
for new bounds, move anchors, place manual Routes, regenerate terrain or tune
individual seeds. Geometry remains fixed so semantic changes are comparable.

Revised artifacts are in `results/default_task_a_1_2026-09-10/`:

- Eight `<seed>/fit.json` and self-contained `<seed>/greybox.svg` pairs.
- `comparison.json`: comparable refined metrics, no ranking or winner.
- `comparative_validation.md`: A.2 automated comparison and human review packet.
- `readiness_gate.json`: all nine A→B criteria for every candidate and sequence stop.
- `verification.json`: source, A.0 and output hashes; world immutability checks.

A.1 is executable and complete. A.2 automated comparison is complete; **human
comparative validation and selection are pending**. The gate cannot be passed
by this program. Therefore B.0 physical authoring, B.1 player-scale validation,
B.2 correction/freeze and C are not executed. No candidate is selected, physically
validated or frozen. The spec's three pre-A.1 interest candidates are not approvals.

## Refined semantics

The implementation extends the existing `Terrain`, RLE, distance, graph and SVG
framework; it does not introduce a new world reader or writer. All thresholds
are explicit in `task_a_refine.PARAMETERS` and serialized in every fit.

- **Conservative traversal:** dry samples with at most four blocks of rise per
  sampled edge; diagonals require traversable corner edges. Existing analytical
  edge costs remain unchanged. The buildable mask is *not* a hard travel filter:
  it contains an elevation cutoff that would incorrectly exclude highlands.
- **Interior:** central half of logical width/height, excluding homelands. Report
  connected clusters, shared area, team/Route representation, distinct approaches,
  Route-pair connectivity, W/E crossing, N/S interaction, accepted lateral links,
  and nearby clusters connected within a 32-block halo. Approaches require a
  terrain-cost connection within 32 effective blocks. Five spines sharing one
  central sample raise an over-centralization warning, not a score bonus. No
  mandatory crossing is asserted. The A.0 point remains legacy reference data,
  while SVG shows the new area and conservative surface mask.
- **Differentiation:** pairwise departure/Starter overlap, terminus spacing and
  angles; the next 110 effective blocks; complete continuation branches;
  encountered connected formation identities. At least 65% early and broad
  corridor overlap (one-sample tolerance) diagnoses collapse when both have at
  least 32 effective blocks of continuation. Short continuations are unproven,
  not automatically differentiated. Angles are evidence, not a mandatory fan.
- **Links:** enumerate Route contacts with observed connected highland, coast,
  bank, forest-gap/edge and open-region masks. Ordinary open-country paths remain
  incidental. A formation-supported connection needs at least 24 physical blocks,
  detour ratio at most 1.8, physical/effective efficiency at least .25 and at least
  half its samples off the existing spines. A strategic shortcut additionally
  joins disconnected branches or saves at least 32 effective blocks and 15% of
  established network travel. Exact duplicate paths are merged. There is no
  output quota. Undetected links are not declared physically nonexistent.
- **Homelands:** retain N/S quality and report every component disparity, plus
  independent usability checks. Absolute disparity classes end at .1 (Low), .3
  (Moderate), .5 (High), then Severe. Only Severe is a scalar failure; independent
  usability is separately flagged. Resource equivalence remains unavailable.
- **Depth:** run edge-origin travel on the conservative Wilderness graph for
  each team independently. Report connected land and formation overlap separately
  in the established secondary, tertiary and 250–350+ deep bands. A formation
  needs 24 samples globally and a connected four-sample intersection in a band.
  Open regions larger than 25% of the available surface are not distinctive
  geographic cores. This supports geographic evidence, not proof of interesting
  content. A.0 permissive depth fields remain available for contrast.
- **Starters/network:** retain edge-origin 58–76 soft handoff range and separate
  corridor quality; assess the entire supported Fountain-to-terminus path for
  canopy, water, maximum rise and circuity over 2.5. Collective opening choices
  are reported separately. Off-Route room, unsupported Wilderness, access,
  shared interaction and meaningful links remain distinct metrics, not one score.

## Validation and uncertainty

Focused tests cover determinism, fixed geometry, water corner cutting, highland
travel, Route collapse despite separated termini, distinct continuations, all
three link categories, homeland thresholds/usability, shared-area connectivity,
under-convergence, six-spoke over-centralization, traversable-versus-geographic
depth, full Starter burden and the mandatory human gate. Artifact tests check
all eight outputs, SVG XML, aggregate consistency, hashes and the blocked gate.
The runner hashes existing worlds before and after; it never opens them to write.

Unavailable: exact block walkability, construction volume, deep-water burden,
fords, caves/ravines/overhangs, sightlines, resource/ecology equivalence, actual
travel/Hunger cost and true mandatory chokepoints. Surface-mask formations do
not establish exact valleys, passes, saddles or human-perceived significance.
Dry-graph disconnection is a conservative evidence gap, not physical impossibility.
Coarse highland/coast access from A.0 is retained as corridor-target evidence,
not promoted to a verified walk. Prior staged review files provide analytical
and overhead context, not human acceptance of these revised overlays.

Human review must evaluate diagnostic validity as well as candidate suitability.
No missing qualitative judgment is replaced by a scalar score, and no physical
stage is used to bypass the A.2 readiness gate.
