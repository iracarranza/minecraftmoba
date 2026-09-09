# Minecraft MOBA — Default-map generation contract

This is the validator-facing form of the Default-map requirements established through P0–P2D. It separates **what must remain true** from any one successful geography.

## Hard structural invariants

- Two opposed team homelands organized north/south.
- Baseline homeland opportunity is mirrored/comparable.
- One continuous shared wilderness connects the teams.
- Three major authored route branches emerge from each homeland.
- Routes are visible, reliable movement infrastructure; the live game may add their speed advantage independently of terrain generation.
- Players can leave routes freely; wilderness remains a legitimate movement/exploration layer.
- Default macro geography contains a western mountain/high-difficulty region and an eastern ocean/coastal region, but their exact shapes are seed-variable.
- Wilderness contains multiple distributed opportunities, with a current working target of 3–5 villages and at least 3 villages.
- High-value wilderness opportunities are not required to mirror at equal coordinates.
- Visible terrain should remain recognizably Minecraft/Overworld-derived rather than revealing generator primitives.

## Spatial / strategic invariants

- Off-route wilderness has enough connected area and navigational depth to function as territory rather than residual space between routes.
- Routes specialize in reliable fast access; wilderness specializes in exploration, resources, concealment, alternate approaches, and terrain decisions.
- Purely obstructive terrain should generally remain off authored routes.
- Early collision is plausible through obvious infrastructure and POIs but not mandatory.
- POIs form a network with variable discovery order and approach geometry.
- Geography changes effective distance; raw block distance is not the only travel cost.
- Player-created infrastructure should be capable of compressing effective distance over a match.

## Terrain-generation invariants

- Competitive topology may be authored underneath the map, but the mathematical construction of that topology should not be visibly legible.
- Forests use irregular stands, variable spacing, canopy height, clearings/edges, and plausible substrate relationships.
- Terrain uses both positive and negative verticality: hills/ridges plus gullies, ravines, basins, cave mouths, and cuts.
- Fluids occupy topographically connected catchments or visibly spill/continue downhill; they are not closed geometric stamps disconnected from adjacent elevations.
- Regional transitions are progressive rather than hard theme boundaries.
- Difficulty should produce choices (around/over/through, surface/cave, route/shortcut) rather than repetitive jump/input taxes.
- Large regions need learnable generated landmarks.

## High-difficulty-region contract

Every high-difficulty region, including the western mountain, must satisfy:

1. **Intentionality** — entering/continuing represents visible commitment; accidental traversal is generally unattractive.
2. **Value** — distinctive resources/opportunities justify the commitment.
3. **Depth** — sustained gameplay can occur inside the region; it is not just one obstacle or summit objective.
4. **Legibility** — internal geography can be learned, navigated, and communicated.
5. **Contestability** — multiple portions/approaches can be fought over; one doorway does not trivially control the whole region.
6. **Transformability** — mining/building can materially improve traversal, extraction, or occupation.
7. **Logistics** — provisioning and extracting value create meaningful costs and decisions.

Supporting patterns include multiple useful approaches, an internal traversal hierarchy, spatially differentiated resources, reasons to remain, meaningful reinforcement depth, recognizable landmarks, and opportunities to domesticate hostile geography with Minecraft construction.

## Current automated acceptance gates

The 50-variant solution-space run operationalized the contract with the following cheap mechanical gates before visual review:

- 3–5 villages.
- Minimum village spacing ≥ 90 blocks.
- Largest connected ordinary-wilderness component ≥ 250,000 surface blocks.
- Mountain footprint ≥ 65,000 surface blocks and depth ≥ 45 blocks.
- Mountain vertical range ≥ 34 blocks and median deep-region elevation at least 16 blocks above its approach belt.
- No widespread heightmap walls: ≤ 800 adjacent mountain edges over 8 blocks and ≤ 20 over 12 blocks.
- Internally walkable mountain network ≥ 18,000 cells.
- At least 2 useful mountain approaches.
- At least 4 generated mountain landmarks.
- At least 5 differentiated mountain resource regions with mean resource-to-authored-route distance ≥ 55 blocks.
- Forest footprint ≥ 9,000 cells and interior depth ≥ 12 blocks.
- Authored routes: 95th-percentile adjacent height step ≤ 2 and no >8-block adjacent jumps.
- At least one topographically connected surface-water feature.

These are **prototype validator thresholds, not final balance constants**. Passing them does not prove gameplay quality; it rejects obvious violations cheaply before visual/pathfinding/playtest review.

## Deliberately non-invariant details

A valid Default map does **not** require P2D's particular basin, lake, mountain height, three exact entrances, forest placement, route curvature, POI coordinates, rocky region, peak count, resource concentrations, or sequence of westward terrain. The solution-space experiment explicitly varies these while preserving the contracts above.
