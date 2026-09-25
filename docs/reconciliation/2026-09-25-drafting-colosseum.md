# Drafting colosseum reconciliation

User-approved direction, 25 September 2026:

- The standalone draft hall is superseded by a circular colosseum.
- A separate catalogue instance is available outside matches. Clicking toggles a persistent personal shortlist, initially five entries. Sorting is deferred.
- Sixty class slots is a prototype fixture; unimplemented slots are visibly unavailable.
- Class bans are simultaneous, one per player, with a 30-second deadline. First valid strike wins; duplicate attempts do not spend a ban.
- Physical class interactions commit decisions. Chat is preview/status only.
- Picks can use the outer ring or the active player's shortlist podium. Shortlist information is restricted to teammates.
- Six numbered map portals replace the arena centre. Entering a portal commits a strike; the final pick requires the selecting team's unanimous portal votes. Traversal returns players to the centre.
- Catalogue shortlists persist independently of match progression.
- Debug terrain visits use disposable pool-world copies without claims.

Implementation limits still requiring completion/validation:

- Pool manifests inspected contain unmeasured scale/density and no measured symmetry band. Do not invent classifiers to populate these fields.
- The required Level-4 terrain thumbnails remain outstanding; portals currently have property Text Displays only.
- The six-map board currently selects distinct entries, not certified distinct profiles.
- Map timeout policy and disconnected-player consensus behavior remain unimplemented.
- World geometry and physical interactions require an in-game multiplayer playtest.

Java 21 and Paper's Bukkit world/block authoring APIs are the implementation target. This builds presentation worlds, not a new playable-map generator.
