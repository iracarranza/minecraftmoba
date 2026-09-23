Before continuing seed-generalization or the 930010639 gameplay experiment, reconcile the worldgen documentation and current analytical assumptions with the following clarified map doctrine.

Do NOT redesign or retune the worldgen algorithms yet. This is primarily a doctrine, terminology, and audit pass. Preserve useful measurements and experiments even where their interpretation changes.

CORE SPATIAL DOCTRINE

A competitive map has three relevant spatial layers:

1. HOMEBASE CORE
   - A relatively small, heavily authored competitive symmetry kernel.
   - North and South receive functionally mirrored geometry here.
   - Contains the Aether Fountain, spawn/reconstruction space, required immediate defensive geometry, and explicit wilderness-facing interface/exit(s).
   - Natural terrain inside the Core may be replaced as necessary.
   - The Core should contain no accidental natural economic jackpot.

2. HOMEBASE-SUPPORTED REGION / HINTERLAND
   - Predominantly natural Minecraft terrain surrounding the Core.
   - NOT mirrored between teams.
   - Selected because it can accept the Core cleanly and provide a viable opening.
   - Should provide distributed low-richness/basic opportunities sufficient for players to begin useful activity.
   - Should NOT normally contain a single concentrated opportunity rich enough to make an immediate full-team expedition obviously dominant.
   - Initial/base-authored Route support belongs primarily here.
   - The transition from Core into this terrain should require only bounded, terrain-conforming authoring.

3. WILDERNESS
   - Natural Minecraft geography is allowed to be substantially asymmetric.
   - Do not normalize North/South canopy, elevation, flat land, caves, resources, biome composition, coastline, etc.
   - Strategic opportunities, Worksites, regenerative manifestations, POIs and player-created Infrastructure inhabit this terrain, but they do not exist to make the underlying wilderness statistically mirrored.
   - Wilderness may be larger, harsher and less conveniently traversable than prior analysis implicitly assumed.
   - A natural terrain difference is not a competitive deficit unless there is evidence of a relevant gameplay consequence.

HOMEBASE SOCKET

Introduce/record the concept of a Homebase Socket:

- Seedfinding does NOT require two naturally mirrored terrain areas.
- It requires two candidate areas capable of accepting the same standardized Homebase Core with low and bounded integration cost.
- A socket should be judged on:
  - physical footprint fit;
  - support/headroom;
  - limited cut/fill/clearing requirement;
  - viable exit/interface into natural terrain;
  - opening economic floor;
  - opening opportunity ceiling;
  - smooth transition into unrestricted wilderness.
- If a site would require major terrain surgery or a large procedural transition halo, reject the socket rather than making the authorer increasingly powerful.
- Prefer finding terrain that permits simple reliable authoring over authoring arbitrary terrain into compliance.

The symmetry guarantee ends at the Homebase Core/interface. Terrain beyond it is not required to match.

ROUTES

Reinterpret existing Route analysis rather than discarding it.

- Existing terrain-weighted path, Practical Reach, physical-corridor and Route-distance work remains useful.
- Initial/map-authored Route reach should primarily help players navigate Homebase exits and hinterlands.
- It should not routinely create convenient highways deep into unrestricted Wilderness or directly solve access to distant strategic opportunities.
- Player-created Exploration Infrastructure is what can subsequently extend useful Route networks deeper into the world.
- Audit where current analysis assumes Routes should equalize or substantially improve access across the entire map, and distinguish those assumptions from measurements that remain useful unchanged.
- Do not retune Route lengths yet.

REGENERATIVE SOURCES

Preserve the existing runtime Source/Kind/depletion/recovery/manifestation machinery.

The clarified authoring model is:

Strategic Depth answers:
    How rich/novel may an opportunity be?

Regional Character answers:
    What kinds of opportunity make ecological/geographic sense here?

Lower strategic depth should generally support less-rich regenerative opportunities in BOTH:
- quantity/concentration; and
- novelty/specialization.

Deeper Wilderness may support larger concentrations, more specialized kinds, or both.

The Homebase-supported region should provide useful distributed subsistence/opportunity without a nearby renewable becoming an obvious full-team opening objective.

Do not assume North and South need the same renewable species or identical source counts. Functional opening opportunity is the concern.

Audit current pushed implementation/documentation and state explicitly what is already implemented versus still only design doctrine. In particular, verify the historical intent that RenewableKinds deliberately carries no depth/value/region and that current configured capacities/recovery values are analytical fixtures rather than a generic depth-gradient implementation.

Do NOT invent exact depth tiers, richness values, species tables or recovery curves yet.

TERMINOLOGY

Avoid conflating two different axes:

- STRATEGIC DEPTH = outward relationship to a team's Homebase/opening space.
- REGIONAL CHARACTER = position within a Map Type's geographic composition.

For the emerging Default Map Type, regional character is approximately:
Ocean -> Coast -> Open Land -> central interior -> Forest -> Rugged Uplands -> Alpine/Frozen Peaks.

These are tendencies/gradients, not seven rectangular biome stripes.

BALANCE INTERPRETATION CORRECTION

Audit the recent sequence around:
- 544747f
- bb1feac
- 997a319
- 1787590
- faa6748
- 7cae5ae
- 4244b39
- 8199c13

The important correction is:

Natural N/S terrain asymmetry is not itself a defect.

In particular, 930010639's 0.3173 accessible-land asymmetry should not be described as a proven competitive deficit. It is evidence that the teams' wilderness differs according to that terrain metric.

The unresolved gameplay question is instead:

"Does this permitted wilderness asymmetry cause a competitive consequence that the authored-opportunity model fails to capture?"

Preserve the Task A/B experiment if it remains useful for answering that question, but correct its interpretation accordingly.

Similarly, do not conclude that lower "homeland asymmetry" automatically means a better map unless the measured property actually belongs to the controlled Homebase contract or has demonstrated gameplay consequences.

MAP-TYPE DIRECTION

Do not implement Map Types yet, but structure the doctrine so future seedfinding can work as:

Map Type macro-geography
    -> team topology
    -> viable Homebase socket pair
    -> wilderness viability
    -> authorability
    -> physical verification
    -> PlayableMap

The eventual Default recognizer should search for the intended Overworld regional gradient rather than requiring all maps to share that composition. Other Map Types will later define different macro-geographic search contracts.

DELIVERABLE

1. Inspect current canonical docs, worldgen reports, code comments and recent experiment documentation for assumptions that conflict with this clarified doctrine.
2. Update canonical design documentation so the Homebase Core / Socket / supported-region / Wilderness distinction is explicit.
3. Correct misleading interpretations of recent terrain-asymmetry work without deleting the measurements.
4. Produce a short audit table classifying current machinery as:
   - survives unchanged;
   - survives but is reinterpreted/re-scoped;
   - needs later modification;
   - obsolete/contradictory.
5. Identify the smallest concrete implementation changes that will eventually be required, but DO NOT implement them in this pass unless a change is purely terminology/documentation and cannot alter behavior.
6. Update HANDOFF with the resulting state and the next unresolved design/implementation decisions.

Do not change balance constants, regenerate worlds, alter 930010639, run the 16-trial experiment, or implement new Map Type recognizers in this pass.