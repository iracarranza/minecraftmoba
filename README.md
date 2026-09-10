# Minecraft MOBA

Minecraft MOBA is a Minecraft-first competitive-game design project. The
Minecraft world is intended to be the strategic board: players discover,
traverse, extract, build, develop, connect, supply, and contest it.

## Source hierarchy

The root Markdown documents are the canonical design sources for their
domains:

- [Classes](classes.md)
- [Objectives](objectives.md)
- [Maps](maps.md)
- [Infrastructure](infrastructure.md)

The [design manuscript](docs/manuscript/Minecraft-MOBA-Design.md) and its editable Word companion synthesize these sources. The [10 September infrastructure reconciliation](docs/reconciliation/2026-09-10-infrastructure.md) records the latest shared infrastructure update.

[chathandoff.txt](chathandoff.txt) is a September 8, 2026 handoff snapshot.
It is useful context and preserves material not yet incorporated into a
canonical document, but it does not silently override the canonical documents.

The browser wiki under `minecraft_moba_design_wiki/` is a local, generated-style
design workspace. It is not authoritative and currently contains stale,
superseded material; see the reconciliation report before using it as a source.

## Repository layout

- `docs/` — repository guidance and reconciliation records
- `implementation/` — reserved for future implementation artifacts; empty by
  design during the documentation bootstrap
- `minecraft_moba_design_wiki/` — non-authoritative local browser wiki

## Current state

This repository contains design material only. No Minecraft Java world,
datapack, generator, validation tooling, or prior greybox artifact referenced
by the design documents is presently checked in.

Start with [the reconciliation report](docs/reconciliation/2026-09-08.md) for
the established/working/prototype/historical/open classification and the next
recommended milestone.
