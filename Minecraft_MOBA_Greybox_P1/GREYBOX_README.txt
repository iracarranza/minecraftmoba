Minecraft MOBA — Greybox Prototype 1

PURPOSE
Authored traversal-friction blockout. P0's macro scale and route/POI composition are preserved while abstract rough terrain is added to test movement topology. No route speed bonus, objectives, loot, natural terrain, or class mechanics are implemented.

P1 AUTHORED TERRAIN LANGUAGE
Brown terraces      Low rolling ridges / uneven ground
Green pillars       Dense-forest proxy; weave around solid obstruction
Gray ridges         Rocky foothill obstruction and mountain approaches
Black trench        Ravine / underground cut
Cyan/blue inlet     Shallow/deeper water proxy
Light-gray crossings Deliberate ravine/water crossings

UNCHANGED GAMEPLAY MARKERS
Blue platform       North base
Red platform        South base
Light-blue outline  North local/safe-development area
Pink outline        South local/safe-development area
Yellow strips       Routes / route junctions
Orange cuboids      Villages
Purple forms        Major POIs / major opportunities
Lime cuboids        Minor resource/opportunity sites
Gray/black west     Western mountain/highland mass; darker/taller = deeper/high mountain
Cyan coast band     Coast
Blue recessed plane Eastern ocean
Magenta posts       Candidate early collision/observation areas
White studs         Sparse 50-block scale markers

DESIGN INTENT
- Terrain is deliberately authored, geometric, and ugly. Each obstacle exists to create a known movement decision.
- Straight diagonal sprint-jumping should no longer dominate every off-route trip.
- Routes stay visually obvious but are not mechanically buffed in this prototype.
- Several wilderness shortcuts remain possible; terrain should create choices, not corridors.
- Major POIs have multiple approach directions and are not enclosed arenas.
- The western highlands are materially harder to cross than the central lowlands.
- The eastern inlet makes the coast locally expensive without turning the whole east side into water.

INSTALL
Unzip this folder into the Java Edition saves directory, then open Minecraft MOBA Greybox P1 from Singleplayer.
This prototype is written in the Java 1.12.2 Anvil save format for simplicity. A modern Java client should convert/upgrade it when opened; make a copy if Minecraft warns that the world is from an older version.

NOT LOCKED
All terrain pieces, dimensions, route speed, route geometry, exact POI positions, village spacing, base separation, mountain footprint, ocean depth, and collision markers remain working-draft values.
