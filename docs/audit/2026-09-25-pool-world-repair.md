# Pool world repair

The parallel foundry published raw generated worlds with separately authored build manifests. Repeated seed windows could also be confused by a seed-only lookup. Publication now uses the exact compilation job's build path.

The local pool had 28 entries. Twenty missing fountain chunks affected ten entries. Twenty authored builds were recovered from `/private/tmp/stockrun`, matched against both manifest fountain plinths and source-water blocks, and copied into the pool. Objective chunk presence was also checked. Original worlds and manifests remain beside each repaired entry as `world.before-authored-repair` and `map.before-authored-repair.json`. Fingerprints were updated. Eight entries could not be matched and were quarantined without deletion.

A subsequent scan found trial-chamber starts or trial-spawner section palettes in ALL twenty recovered builds. Those entries were also quarantined. This is conservative over the entire saved world, including generated halo chunks; it does not assert every detected chamber intersects playable bounds. No entry is currently certified exclusion-compliant.

Publication now rejects saved trial-chamber starts/spawners. Preview checks both fountain chunk headers, refuses quarantined entries, and uses a void generator with structure generation disabled, preventing absent terrain from silently becoming vanilla terrain. Preview reports the map ID and north fountain coordinates.

Map 2 (`120997530-f5b517f079`) has restored authored fountains at north (-2404,67,3500) and south (-1772,66,3444), but remains quarantined because its authored world contains trial chambers. Existing loaded preview copies were not rewritten or deleted.

Outstanding: regenerate/re-author exclusion-compliant worlds (or establish and enforce certified playable bounds excluding every forbidden structure), then rerun block-level certification before removing quarantine markers. This repair does not pretend restored authored worlds are exclusion-compliant.
