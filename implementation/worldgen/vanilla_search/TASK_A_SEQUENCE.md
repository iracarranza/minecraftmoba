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

## Subsequent human selection and gate recheck

The user subsequently completed A.2 selection: **930010639 primary** and
**930012642 comparison/fallback**. The earlier A.1 output and stage-state text
above are historical snapshots, preserved rather than rewritten.

The decision is recorded in `decisions/task_b_selection_2026-09-10.json`.
Run `python3 implementation/worldgen/audit_task_b_readiness.py` from the repo root
to reproduce `results/default_task_b_gate_2026-09-10/`. Selection now satisfies
the human-selection criterion. Remaining central/lateral-connectivity and
Starter-handoff evidence requires resolution before B.0 under section 16.
Neither candidate has a measured physical pass/failure, correction or freeze.

The user then answered **yes continue** to the explicit remaining-gate acceptance
question. `decisions/task_b_gate_acceptance_2026-09-10.json` supersedes the blocked
gate state. Both B.0 greyboxes and partial B.1 block validation are now recorded in
`results/default_task_b_2026-09-10/REPORT.md`. The full player-scale walk remains
incomplete, so B.2/freeze/C remain gated; see that report for the current state.

Human review must evaluate diagnostic validity as well as candidate suitability.
No missing qualitative judgment is replaced by a scalar score, and no physical
stage is used to bypass the A.2 readiness gate.

## Destination-anchored analytical revision — 2026-09-10

Subsequent human B.1 inspection reported arbitrary open-terrain Starter ends,
particularly in 930010639. The user authorized a bounded analytical objective
revision, not seed rejection, world editing or Task C. The confirmed principle
is now recorded in spec2 §11 and spec3 §9.

Run `python3 implementation/worldgen/fit_default_task_a_destinations.py`.
Outputs: `results/default_task_a_destinations_2026-09-10/` (eight JSON/SVG pairs,
`comparison.json`, focused `REPORT.md`, preservation `verification.json`).
The fitter retains bounds, homelands, Fountains, departures and deeper targets;
it scores terrain-interface destinations jointly and recomputes network analysis.
Unavailable replacements are explicitly unresolved, never successful arbitrary
distance termini. Search bounds and evidence/scoring assumptions are prototype
parameters, not new balance rules. Earlier A/B artifacts remain unchanged.

Tests: `python3 -m unittest discover -s implementation/worldgen/tests -v`.
No new physical greybox is authorized until human review accepts the analytical
refit. Prior physical selection and partial block validation do not validate the
new Starter paths. No skeleton freeze or Task C occurred in this revision.

Completed verification: all eight fits ran in 13.685 seconds of fitting time
(world hashing separate); 34/48 handoffs are anchored and 14 remain explicitly
unresolved under the bounded search. Primary 930010639 has 4/6 anchored;
comparison 930012642 has 5/6. These counts are not physical pass/fail decisions.
The full suite passed **79 tests** in 30.662 seconds, including deterministic
reproduction of all eight results, XML parsing and output/provenance hashes.
The run verified **651 unchanged files across all ten existing worlds** and
**67 unchanged prior input/artifact files**. No world was unavailable.
Quick Look render inspection covered both focused candidates; long feature
labels were moved to the SVG sidebar to keep homeland handoffs readable.
`git diff --check` passed. Existing user `.DS_Store` changes are outside this work.

## Destination-first terrain-network analysis — subsequent revision

The next user-directed analytical pass supersedes the historical-spine search
constraints above. It retains existing candidate bounds, orientation, homelands
and Fountains, but **no historical Route geometry enters the solver**. Historical
Routes are preserved in comparison/debug output only.

Run `python3 implementation/worldgen/fit_default_task_a_network.py`.
Outputs: `results/default_destination_first_2026-09-10/`, with eight JSON/SVG pairs,
aggregate `comparison.json`, focused `REPORT.md` (930010639, 930012642, 930005557),
and input/world preservation hashes in `verification.json`.

The pass separates geometric Homeland Depth from terrain-weighted Travel Cost,
compresses coarse geographic components, groups inland-water systems, evaluates
homeland-wide destinations and forward/backtracking continuation, selects up to
three destinations jointly, then finalizes exits and Starter corridors. Same-team
lateral and opposing convergence matrices use sample-level reach rather than
mathematical-center or large-region-overlap assumptions. Region/branch counts
and convergence depths are descriptive, not desired topology targets.

Natural/modest/major labels are sampled dependency proxies. Water crossings are
conditional; the major sensitivity graph also contains **unverified water
transport**, not proof that a wide-water connection needs major earthwork.
Unresolved analysis of the manually promising 930010639 remains an analyzer
limitation, not an automatic seed rejection. No physical greybox or Task C is
authorized by this pass. Run the full test suite using the command above.

Completed run: all eight finalists, 65.666 seconds of analytical fitting
(preservation hashing separate), 46 provisional handoffs. One choice remains
unresolved for 930015734 South and 930016664 North. All three focused candidates
have three provisional handoffs per team, not a physical validation pass.
The final suite passed **92 tests** in 41.392 seconds, including history-removal
and corruption tests, reproduction of the real 930010639 fit without old Routes,
all-face perimeter coverage, depth/cost separation, water identity, noise
absorption, continuation/backtracking, sample-level convergence and observed
overlap-growth profiles. All 19 output files were verified; **651 world files**
and **86 prior input/artifact files** remained unchanged. Focused SVG overlays
were render-inspected; XML and provenance hashes passed. No worlds were missing.

## Bounded local continuation correction — 2026-09-11

The user-directed correction from `4ec2dc5` separates immediate unsupported
handoff geography from eventual network reach. The runner now writes to
`results/default_local_continuation_2026-09-11/`; the previous destination-first
directory remains preserved. Run:

```
python3 implementation/worldgen/fit_default_task_a_network.py
python3 implementation/worldgen/fit_default_task_a_network.py --verify
```

The second command runs the full unittest suite and seals a test receipt plus
output hashes. The report includes all eight candidates, every selected handoff,
all-eight lateral/convergence matrices, focused before/after comparisons and
the primary coastal handoff's small-backward-detour evidence.

`local_continuation` uses the next existing operational depth layer, a fixed
one-sample-diagonal backward floor, and an explicitly provisional physical-path
guard of twice the derived depth span. It removes the immediate sample
neighborhood, merges residual components that reconnect inside the horizon,
and rejects unsupported small pockets. Physical and effective extents are
observations, not balance targets. Local dead_end handoffs are ineligible for
every destination kind. Directed/branching/junction receive no shape bonus.

`deep_network_reach` preserves the old broad flood, interventions, region access,
and network participation. Old regional port counts/classes are explicitly
legacy diagnostics, never local eligibility. Whole-map topology remains a
deep-network measurement. Homeland fields, destination discovery, water-system
identity, terrain segmentation, map dimensions and general weights are unchanged.

No seed search, world authoring, Task C or physical readiness decision occurs.
Missing choices and uncertain local scale remain analyzer/evidence limitations,
not automatic grounds for rejecting a manually promising seed. This pass does
not retry lower-ranked feature anchors after a local rejection; that limitation
is explicit rather than silently broadening the destination-fitting revision.

Completed result: eight finalists fitted in 132.945 seconds (world hashing
separate), with 46 provisional handoffs: 39 directed and 7 branching. No selected
local junction or dead_end. The same two unresolved choices remain: 930015734
South and 930016664 North. Changed sets are limited to 930007222 South and
930016664 South; all three focused candidates retain their prior destinations.
930010639 North's coast survives as a locally directed handoff requiring an
allowed small backward detour, not a destination-type exception. Its strict
forward deep reach remains limited and is reported separately.

The suite passes 107 tests, including the retained historical-geometry
independence tests and new bounded-local branch, pocket, reconvergence, depth
floor, eligibility, equal-shape-value and deep-reach preservation regressions.
All 651 files across ten available worlds and 105 prior input/artifact files
remain unchanged. JSON/XML/hash verification passed; all three focused SVGs
were render-inspected (square-padded temporary QA copies avoid Quick Look's
rectangular thumbnail clipping). The report records guard contact in 43/46
physical budgets and the 57.9–137.9 physical / 90.4–479.5 effective extent range;
these are observed analyzer limits, not calibrated gameplay distances.

## Opportunity Relationship shadow pass — spec 4

The next pass consumes those frozen results without reselecting handoffs or
changing any active fitting logic. See `OPPORTUNITY_SHADOW.md` for terminology,
provisional heuristics, natural/authored responsibility and the stopping rule.

```
python3 implementation/worldgen/fit_default_task_a_opportunities.py
python3 implementation/worldgen/fit_default_task_a_opportunities.py --verify
```

Outputs: `results/default_opportunity_shadow_2026-09-11/`. All 46 selected
handoffs and 192 stratified unselected candidates receive additive diagnostics.
The final eight-candidate shadow run took 11.298 seconds of analysis (world
hashing separate). Completion observations are 7 Complete, 10 Structurally
Supported and 29 Incomplete selected relationships; 55 sampled unselected
relationships are Complete or Structurally Supported. These are conditional
evidence categories, not new quality scores, eligibility decisions or seed
verdicts.

930010639 North coastal/inland-water handoffs share a sampled water network and
village payoff; direct land access and existing homeland water contact leave
privileged-access advantage unresolved. N1's immediate highland entry is not
established by the modest proxy, but its local Junction structure is separate
evidence. The suspicious 930007222 forest handoff is not a demonstrated forest
Gateway; its structural interpretation comes instead from water access, with
payoff still unobserved. General slope/pass semantics remain an analyzer gap.

The full suite passes 126 tests. Verification includes exact equality with each
baseline after stripping only shadow fields, unchanged active implementation,
133 protected input/artifact/implementation files and 651 unchanged files
across all ten available worlds. Previous analytical outputs remain intact.
No new seed, physical world, authored payoff, reselection or Task C work occurs.
Manual shadow review and another explicit design/validation pass are required
before Opportunity Relationship evidence may influence selection.
