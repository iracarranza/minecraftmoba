# Prototype/test: disposable placement and path authoring

Start a lab scoop with `/moba lab start`, choose a class and scoop, then Launch.
Only the launching administrator can author, and only while standing in that
disposable world. The first author command switches the tester to Creative,
pauses the match clock and holds random ticks, physics, fluid flow and griefing
for reversible placement experiments. `/moba lab end` discards the world and
returns to setup. This mode lasts until the session ends.

Aim at a real block within 64 blocks; there is no silent player-position fallback.

```
/moba lab author inspect
/moba lab author preview fountain
/moba lab author preview outpost rigid
/moba lab author preview outpost follow
/moba lab author apply
/moba lab undo
/moba lab author cancel
```

Templates are `fountain`, `outpost`, `bastion`, and `spike`. They are exported
from the same foundry forms by `tools/worldgen/export_lab_templates.py`, using
installed Java 1.21.11 assets where applicable. Bastion means the current central
body; the omitted rampart assembly is still omitted. Spike is block geometry;
this experiment does not create its End Crystal.

Preview sends client-only proposed blocks (maximum 12000 shown), with temporary
cyan particles marking the original ground. Large previews explicitly report
how much was shown. Cancel restores the current real blocks to the viewer.
Chunk refreshes can replace these client-only previews; rerun preview if needed.

Physical reports include relief, total cut/fill and worst-column cut/fill.
The planner uses actual template columns, clears their occupied vertical volume
and fills beneath them. It refuses existing authored blocks, player-placed
blocks and block entities; geometry from previous lab operations can be joined
or edited, but block entities remain protected. Trees may be clipped within
the footprint rather than felled whole: inspect forest placement accordingly.

Prototype gates are mean cut/fill <=10 blocks per footprint column and maximum
local cut/fill <=12. These are experimental integration limits, not established
balance. `follow` projects columns onto terrain; uneven projection deforms rigid
buildings, so it is explicitly rejected for application but remains previewable.
Rigid placement uses the targeted column's surface elevation. Maximum edit size
is 60000 blocks. The target selects X/Z; terrain determines Y.

## Modular paths

```
/moba lab author path from
# Aim at the other endpoint.
/moba lab author path to
/moba lab author apply
/moba lab author access
/moba lab undo
```

The bounded planner searches a cardinal terrain-cost corridor, fits an elevation
profile while preserving endpoint heights, and selects landing, straight,
corner, stair and bridge modules. The first prototype is a one-block-wide trail:
dirt path, directional stone-brick stairs and plank water decks. Extending from
previous lab geometry is permitted. Its endpoint limit is 192 Manhattan blocks;
choose intermediate waypoints for longer routes. Abrupt transitions, one-column
valleys needing incompatible stairs and cut/fill exceeding 2 blocks are rejected.
It does not construct switchbacks, bridge supports, branching junctions or
register recognized player Routes. Width, materials and thresholds are fixtures.

## Competitive diagnostics

Preview and `access` scan terrain in small batches, loading only existing scoop
chunks. The 8-block graph reports north/south terrain-weighted costs and
`abs(north-south)/mean(north,south)` access asymmetry. Its edges penalize elevation
and water and reject sampled rises above 8 blocks. Unknown/blocked samples are
excluded. A missing connection is explicitly UNMEASURED/UNREACHED, not a parity
pass. Changes or undo invalidate a pending scan; rerun to compare before/after.
The scan's rectangle includes both fountains, the target and a 64-block margin;
routes outside that rectangle are not searched. Maximum scan is 40000 samples.

These are descriptive diagnostics, not a universal symmetry threshold or full
player traversal proof. An 8-block grid can miss thin barriers or a narrow new
trail. Placement PASS only means the physical prototype gates passed. Current
match bindings (respawn, objectives, Lair, Worksites, renewables) retain their
certified positions; this tool does not relocate them or re-certify a map.
No template, production pool entry or canonical design document is mutated.

## Encounter fixtures and undo

```
/moba lab author swarm mountain_ravager
/moba lab author fauna sheep 3
/moba lab undo
```

Swarm spawning checks the registered definition's biome/time eligibility.
Fauna accepts registered animal kinds, with 1..12 members. Both search eligible
natural ground with 3-block headroom within 3 blocks of the target. These are
one-off fixtures, not registered recurring opportunities. Unsupported swarm
definitions are refused; Sand Creeper and Infested Slime are not implemented.
Authored encounter deaths yield no ordinary drops/XP, and undo removes surviving
spawned members by UUID.

Undo snapshots complete pre-edit block states, uses reverse write order with
physics disabled, and removes spawned members. Overlapping edits undo in LIFO
order. If a restore fails, the operation remains available to retry. Application
re-evaluates current terrain, refuses stale previews and rolls back a failed
write. History holds the latest eight operations, capped at 120000 saved block
states, and is discarded at lab end/restart. Manual Creative edits and unrelated
gameplay/player-state changes are not journaled. Never use these commands as a
production map publisher; permanent export/re-certification remains future work.
