# Review before implementation

Authority: `specs/mapseedsearchspec1.md`; settled design remains in `maps.md`.
No cross-document design contradiction was resolved and no design was changed.

The prior search generated ten origin windows before screening (about 69 seconds
generation plus 8.5 seconds extraction each). Preserve `acquire.py`, the pinned
official 1.21.11 server / Java 21 approach, Anvil/NBT extraction, orientation
transforms, shortest-path utility, compact grid serialization, and inspection
world configuration. Preserve all prior results, especially 002 and 003.

Retain actual water, height, relief, forest extent, coastal composition and
homeland site analysis. Demote exact structures/resources/ecology to descriptive
bonuses or future authorship. Do not use the old aggregate score or warning list
as the new acceptance contract. Old highland depth counts noncontiguous columns
across the entire map; old open-buildable share includes forest; paths connect
because all cells have finite cost, so connectivity alone proves little. New
analysis must describe these limits rather than inherit misleading labels.

Add native Cubiomes biome opportunity screening and orientation ranking before
generation. The pinned library exposes MC_1_21 Winter Drop, not an explicit
1.21.11 contract: use it as an approximate proxy, validate against existing and
new official chunks, and never call its output ground truth. Stage A rejects
obvious absence of coast/highland opportunity and ocean domination. Stage B
ranks open/forest/highland/coast relationships and homeland land opportunity.
Sampled absence can miss small features; false negatives are a reported limit.
No native height approximation is needed.

Stage C extracts actual canopy surface blocks, connected dry land, genuine
open ground and western cold/highland evidence. Stage D fits homeland footprints,
six provisional corridors, POI opportunities and crossing warnings. No block
terrain is authored. Traversal at block scale, sightlines, cave entrances,
mountain internal geography, ecological portfolios, and competitive equivalence
remain manual or later implementation criteria. Inspectors receive explicit
bounds/directions, labeled references and clean worlds.

Budget: benchmark native filtering, then screen tens of thousands of windows;
generate an initial eight diverse survivors, extending only if actual evidence
does not yield about five to ten useful finalists. Reuse extraction checkpoints.
Measure stage counts, wall time, acquisition time, chunk totals and disk cost.

## Findings during actual validation

Snow on leaves required a ground scan rather than trusting the no-leaves
heightmap or top block alone. This was fixed in extraction while preserving
legacy behavior for the historical POC. The initial fixed eastern analytical
waypoint could land in ocean; flexible mainland endpoints and uncapped
relief/water costs now expose plausible coastal approaches. Homeland fitting
also penalizes relief across the complete footprint and overhead canopy.

The first eight actual worlds exposed excessive snowy-coast representation.
The search therefore resampled 5,000 already-screened windows cheaply to
distinguish temperate open interior from snowy plains, selecting only three
additional official generations. Documented review exclusions replace three
weaker initial variants; no world was regenerated or terrain repaired.
This is a search-ranking refinement, not a change to canonical design.
