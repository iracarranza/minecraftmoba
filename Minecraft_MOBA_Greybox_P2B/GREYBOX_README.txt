Minecraft MOBA — Greybox Prototype P2B

PURPOSE
Second partial-generation experiment. P1's map scale, POIs, route topology, western macro-mountain, eastern ocean placeholder, bases, and south-side greybox are preserved. Only the NORTH team's three-route opening projection fan is naturalized. P2B specifically tests whether the existing horizontal footprint can support genuine wilderness specialization once vertical/topological complexity is allowed to become much stronger.

P2B EXPERIMENT
- The north route fan uses seeded multi-scale fields plus generated discrete terrain events: mesas, shelves, hollows, gullies, pits, spires, boulder fields, water pockets, and exposed lava pockets.
- The wilderness BETWEEN the three routes and somewhat outside them is part of the generated mask.
- Routes themselves have protected clear corridors: generated trees, fluids, spires, gullies, and abrupt terrain do not obstruct the road surface. Terrain may become dramatic immediately outside the shoulder.
- POI placeholder clearings are also protected so terrain can be judged independently of final POI generation.
- The coastline is NOT naturalized in this pass. The existing cyan/blue eastern placeholder remains.
- The south half remains P1 authored greybox and functions as an in-world control.

GENERATED NORTH-FAN TERRAIN
Grass/dirt          Ordinary lowland surface
Stone/gravel        Rougher upland/resource-character patches
Sand/gravel flats   Drier low-ground variation
Water               Seeded shallow ponds
Oak logs/leaves     Broad transition woodland + dense forest interiors + tall canopy
Stone formations    Spires, low mesas, boulders, scarps, exposed open-country landmarks
Lava                 Rare exposed rocky-region hazard / traversal decision
Negative relief      Basins, trenches, gullies, pits and hollows

UNCHANGED GAMEPLAY MARKERS
Blue platform       North base
Red platform        South base
Yellow strips       Routes / route junctions
Orange cuboids      Villages
Purple forms        Major POIs / major opportunities
Lime cuboids        Minor resource/opportunity sites
Gray/black west     Existing western macro-mountain placeholder
Cyan/blue east      Existing coast/ocean placeholder; intentionally not generated yet
Magenta posts       Candidate early collision/observation areas

WHAT THIS PASS IS FOR
The key question is whether the SAME route spacing that looked too conservative in P2A can support wilderness with a real interior once generation uses strong positive and negative relief. Forests should read as regions with transition edges, deep canopy, internal clearings and terrain landmarks. Rocky/open country should preserve sightlines without becoming a flat material patch. The test is whether players can meaningfully choose around / over / through terrain rather than merely sprint diagonally across it.

INSTALL
Unzip this folder into the Java Edition saves directory, then open Minecraft MOBA Greybox P2B from Singleplayer.
This prototype uses the Java 1.12.2 Anvil save format so modern Java may prompt to upgrade it on first open. Make a copy if Minecraft warns about conversion.

NOT LOCKED
Generated seed, terrain roughness, tree density, route protection width, POI clearings, resource-character thresholds, route speed, exact map dimensions, and all final world-generation rules remain experimental.
