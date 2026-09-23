# Claude handoff — spatial doctrine / cadence migration PAUSED

**User-requested pause, 22 September 2026. This spec is NOT complete.**
The user asked: “after next command/tooluse/etc, pause, create a handoff for
claude explaining your progress on the given spec, and push”. Implementation
stopped. Continue on **codex/spatial-cadence-migration**, worktree
`/tmp/minecraftmoba-spatial-cadence` (real path `/private/tmp/...`).

## Read first

1. [Full active spec](specs/superspatialdoctrinespec.md), all 36 sections.
2. [Detailed continuation handoff](docs/handoffs/2026-09-22-claude-spatial-cadence.md).
3. [Completed previous spatial audit](docs/audit/2026-09-22-spatial-doctrine.md).

This branch starts at **03c5bea** (completed doctrine-only audit), which includes
**9bb5093** (completed Hunger audit), atop main **8199c13**. Remote main was still
8199c13 at the start. Neither prior branch nor this work has been merged to main
by this continuation. Do not reset/clean the original dirty checkout.

## Current state

- Traced the actual runtime clock and immediate consumers; recovered existing
  objective / Worksite documentation and relevant older history.
- Copied the full supplied spec into this branch.
- Added FOUR **draft, unwired, uncompiled and untested** Java classes:
  `OpportunityCadence`, `TeamObjectives`, `LairLifecycle`, `Lair`.
- No existing runtime class, config, test, map fixture or canonical doctrine has
  yet been migrated for the new spec. The current plugin still behaves as before.
- No server was run, no world regenerated, no live deployment, no gameplay trial.
- Draft code requires review, especially Lair entity unload/replacement cleanup,
  physical encounter behavior and attribution; see detailed handoff.

The next coherent implementation step is to finish the clock/Worksite/Lair
integration with focused tests, then implement the spatial contract data seam
and reconcile canonical docs. Do not mistake the presence of new classes for a
finished migration. Do not invent unresolved siege rewards, tier quantities,
Core/Lair dimensions, opening iron quota or post-Dragon cadence.

The 930010639 fixture remains unchanged and blocked by its Route endpoint ice
readback and missing Task B wheat. No 16-trial experiment is authorized by this
continuation. The user considers the earlier Hunger issue solved.
