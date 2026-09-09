# Minecraft MOBA repository guidance

## Design authority

`classes.md`, `objectives.md`, and `maps.md` are the canonical design sources
for their respective domains. `chathandoff.txt` is a dated consolidation and
may preserve unincorporated context, but does not override a canonical source
without an explicit reconciliation decision.

`minecraft_moba_design_wiki/` is non-authoritative browser material. Treat it
as a stale snapshot unless it is deliberately resynchronized from canonical
Markdown.

## Design-state language

Preserve the labels used in the design documents:

- **Established**: do not change without an explicit design decision.
- **Working**: current direction, still revisable.
- **Prototype/test**: parameter for an experiment, not final balance.
- **Historical/superseded**: retain for traceability; do not revive silently.
- **Open**: record the question; do not invent an answer.

## Implementation guardrails

Do not begin a playable-map build until the target Java version and world
authoring/writing approach are explicitly chosen. Keep generated worlds,
datapacks, scripts, validation output, and design documentation in separate,
clearly named directories. Update the reconciliation record when resolving a
cross-document contradiction.
