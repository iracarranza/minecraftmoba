# Swarm definition schema

**29 September 2026. Working.** Implements the swarm half of
`docs/reconciliation/2026-09-29-opportunity-fields.md`. Values are fixtures, not
balance; `maps.md` and `objectives.md` keep composition, cadence, tables and
rewards **Open**.

## The split

| Layer | Owns | Persists |
|---|---|---|
| Opportunity (`Renewables.Source`) | place, region, recovery lifecycle, its `SwarmTable` | yes |
| `SwarmTable` | the definitions that may occupy it, across day and night | yes |
| `SwarmDefinition` | one swarm: biomes, time, composition, behavior, payload, value | content |
| Manifestation | the entities standing now | until resolved |

Opportunity identity is spatial and ecological. Manifestation eligibility is
temporal.

## Definition

`SwarmDefinition(id, members[type, weight], size, biomes, time, behavior, payload, value)`

- **biomes**: ecological group names (`mountain`, `desert`, `beach`, `jungle`,
  `mushroom`, from `BiomeGroups`) and/or literal biome ids. A group is a working
  fixture mirrored in `implementation/worldgen/fixtures/swarm-biome-groups.json`.
  Vanilla has one mushroom biome, `minecraft:mushroom_fields`.
- **time**: `DAY`, `NIGHT` or `ALL`, against the world's own phase.
- **members / size**: weighted draw of `size` entity types.
- **behavior, payload**: ids into registries that are **empty**. A definition
  naming one is refused, so nothing is accepted and silently inert.
- **value**: relative, unset by any balance decision.
- Boss-class entities (Warden, Dragon, Wither) are refused.

## Selection at manifestation

1. If no definition in the table matches the current time, stay READY and record
   no eligible locus. No terrain scan.
2. Run the ordinary `Eligibility.loci`, then keep loci where at least one
   definition is eligible for the biome under that locus. A biome the terrain
   view cannot report never qualifies.
3. `Eligibility.select` chooses among those loci, so displacement and player
   exclusion still apply.
4. Choose uniformly among the definitions eligible at that locus. Weighting is
   Open.
5. Spawn the composition. Members carry the opportunity marker and a
   `swarm_definition` marker naming the definition.

**Sunset.** The table is read only when the opportunity next manifests. An
empty or READY opportunity at sunset uses the night table; one recovering
through sunset uses whichever table is current when it becomes ready; a living
daytime swarm is not replaced. A definition may later opt into an explicit
sunset transformation.

## Implemented now

`mountain_ravager`: `mountain`, `DAY`, Ravager, size 1, standard behavior. The
compiler derives it (`terrain_harvest/portfolio.py`, `swarm_vocabulary.py`)
into the `deeper` band for cells with at least a quarter mountain-group biome
share, and swarms never count toward a team's opening floor.

## Specified, not implemented

**Sand Creeper.** `desert` + `beach`, `DAY`, Creeper. Behavior: sand affected by
its explosion yields Quartz instead of Sand. Function: renewable Quartz through
a combat interaction, combat plus resource access.

**Infested Slime.** `jungle` at `NIGHT`; `mushroom` `ALL`. Slime. Behavior:
splitting also produces Silverfish.

Both need a swarm-scoped listener keyed on the `swarm_definition` marker
(`EntityExplodeEvent`, `SlimeSplitEvent`), which does not exist yet.

## Open questions

- **What clears a splitting swarm.** A slime's death counts as a harvest, and
  Paper does not carry PDC keys onto split children, so the children are not
  members and the opportunity would read as cleared at the first kill. Options:
  stamp children in a split listener, or count only leaf mobs.
- **Explosive payloads and the renewal invariant.** Renewal grants nothing
  (`grantedByRenewal` stays zero). A payload that changes drops during combat is
  ordinary combat loot, but the line should be stated before Sand Creeper.
- **Whether `Time.ALL` swarms should re-roll at sunset** when the opportunity is
  READY, or hold until they resolve.
- **Weights** between eligible definitions, and **value** scale.
- **Night with an empty table.** A daytime-only opportunity currently idles at
  night. Whether that opportunity should be rewritten to carry a night table, or
  deliberately dark, is a content decision.
