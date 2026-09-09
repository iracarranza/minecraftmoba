Minecraft MOBA — Greybox Prototype P2A

PURPOSE
Partial-generation experiment. P1's map scale, POIs, route topology, western macro-mountain, eastern ocean placeholder, bases, and south-side greybox are preserved. Only the NORTH team's three-route opening projection fan is naturalized with deterministic generated terrain.

P2A EXPERIMENT
- The north route fan is generated from seeded multi-scale height, moisture, forest, and rock fields.
- The wilderness BETWEEN the three routes and somewhat outside them is part of the generated mask.
- Routes themselves have protected clear corridors: generated trees, ponds, boulders, and abrupt terrain do not obstruct the road surface.
- POI placeholder clearings are also protected so terrain can be judged independently of final POI generation.
- The coastline is NOT naturalized in this pass. The existing cyan/blue eastern placeholder remains.
- The south half remains P1 authored greybox and functions as an in-world control.

GENERATED NORTH-FAN TERRAIN
Grass/dirt          Ordinary lowland surface
Stone/gravel        Rougher upland/resource-character patches
Sand/gravel flats   Drier low-ground variation
Water               Seeded shallow ponds
Oak logs/leaves     Seeded woodland clusters / navigation obstruction
Stone boulders      Seeded local roughness

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
The key question is not whether the generated patch looks finished. It is whether the area between route branches is spatially large enough for route travel and wilderness travel to specialize differently: routes should be legible/easy; wilderness should support real navigation choices, terrain friction, alternate paths, and spatially varying resource character without collapsing into narrow corridors.

INSTALL
Unzip this folder into the Java Edition saves directory, then open Minecraft MOBA Greybox P2A from Singleplayer.
This prototype uses the Java 1.12.2 Anvil save format so modern Java may prompt to upgrade it on first open. Make a copy if Minecraft warns about conversion.

NOT LOCKED
Generated seed, terrain roughness, tree density, route protection width, POI clearings, resource-character thresholds, route speed, exact map dimensions, and all final world-generation rules remain experimental.
