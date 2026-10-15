# Archipelago island-chain search

**Prototype/test, 12 October 2026.** Implements an exploratory search preference
in response to the Working archipelago direction in `maps.md`. No final
Type-specific balance limits or universal one-objective-per-island rule are adopted.

The source is official unmodified Java 1.21.11 terrain. No playable-map authoring,
objective manifestation, runtime installation or READY certification occurs.

## Search preferences

- Fit the two Homebases on two distinct substantial geographic islands. The current
  search trial uses at least half the area of the second-largest component;
  it does not require the two exact largest islands.
- Retain defensive pad candidates by island as well as practical advance and
  position; high-scoring mainland pads must not eliminate smaller islands
  from the candidate pool.
- Try three distinct defensive islands for each team. This is a conservative
  experiment aimed at avoiding the concentration identified by the user;
  failure does not establish that every multi-objective island is unbalanced.
- Reserve a viable Lair with modeled access disparity <=15%, choosing the
  smallest worst-team straight-line Fountain distance among those sites.
  Check 175-block spacing from every other major anchor. Report absolute
  distances and modeled costs; neither is calibrated discovery or travel time.
- Rank Core pairs by the number of islands with eligible defensive-pad
  proxies for each team before comparing accepted Homebase quality. Retain
  the minimum quality bound. Reject pairs whose complete sampled eligibility
  fields cannot supply three islands per team; this is an upper-bound screen,
  not proof that surviving pairs have physical or correctly ordered layouts.
- Include alternate routes in the local forward search: cells whose combined
  modeled reach from both Fountains is at most 135% of the shortest modeled
  Fountain-to-Fountain cost. The earlier 48- and 192-block single-path corridor
  experiments are retained in individual result files. The 35% detour allowance
  is a Prototype/test coverage parameter, not adopted balance or boat timing.
- Preserve the shared physical-fit, opening/resource, symmetry, ordering,
  own practical-cost half, rival-distance and forward-corridor gates.

## Evidence and limits

Island identity is four-neighbour connectivity of the 8-block actual surface
grid with river and frozen-river cells restored for geographic grouping only.
All physical-fit checks continue to use actual surface water. This correction
excludes variant 890001993-r90-p901, whose dry components falsely separated
two North structures on one geographic island. Narrow channels, land bridges, caves, boats and player construction
can change practical connectivity. The identity labels do not certify
chokepoints, defensibility, visual prominence, or balanced territorial control.
Chain shape remains a visual design assessment. The sampled Lair open-volume
proxy still does not certify actual Giant/Ghast/Dragon encounter geometry.

Raw terrain refinements and compiler results are retained in
`implementation/worldgen/reports/archipelago_qualified_search_2026-10-12/`.
The new shortlist and topology evidence are reported separately in
`implementation/worldgen/reports/archipelago_chain_search_2026-10-12/`.

## Window experiment

Also screen 1792-block subwindows within fully generated 2048-block source
windows. Selected windows are re-extracted from the vanilla chunks, with their
own full-chunk count, bounds, feature grid and characterization. No terrain is
moved or authored. Off-window connectivity and future map-boundary enforcement
remain unverified. Reports distinguish the selected window bounds.

## Sequential morphology follow-up — 13 October 2026

The clarified Working preference requires visible size/footprint progression,
not proximity alone. `screen_sequential_archipelagos.py` ranks two inward
centroid sequences between substantial endpoint islands, rewards small first
islands and growth, and penalizes long steps and lateral scatter. Growth is a
ranking preference; the score is not an acceptance threshold. Straight-axis
projection can miss good curls and reward awkward zigzags, so visual review is
required. Native biome grids approximate terrain and do not establish actual
water, physical pads, or gameplay qualification.

The new census, high-resolution approximate previews, and subsequent actual
world checks are retained separately in
`implementation/worldgen/reports/archipelago_sequence_search_2026-10-13/`.
